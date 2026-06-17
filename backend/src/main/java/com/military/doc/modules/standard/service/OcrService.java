package com.military.doc.modules.standard.service;

import com.military.doc.config.OcrProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OcrService {

    private static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_OCR_PAGES = 50;

    private static final Pattern CJK_CHAR = Pattern.compile(
        "[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF\\u2F800-\\u2FA1F]");
    private static final Pattern CJK_SPACING = Pattern.compile(
        "([" +
        "\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF\\u2F800-\\u2FA1F" +
        "\\u3000-\\u303F\\uFF00-\\uFFEF" +
        "])\\s+([" +
        "\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF\\u2F800-\\u2FA1F" +
        "\\u3000-\\u303F\\uFF00-\\uFFEF" +
        "])");
    private static final Pattern TESSERACT_NOISE = Pattern.compile(
        "(?i)(Image too small|Line cannot be recognized|no best words|"
        + "Empty page|Warning:|Error during processing).*");

    private final OcrProperties properties;
    private boolean tesseractAvailable;
    private boolean paddleocrAvailable;

    public OcrService(OcrProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.info("OCR is disabled (ocr.enabled=false)");
            tesseractAvailable = false;
            paddleocrAvailable = false;
            return;
        }

        // Check Tesseract
        try {
            Process p = new ProcessBuilder(properties.getTesseractPath(), "--version")
                .redirectErrorStream(true).start();
            if (p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0) {
                tesseractAvailable = true;
                log.info("Tesseract OCR: {} (v{})", properties.getTesseractPath(),
                    new String(p.getInputStream().readAllBytes()).split("\n")[0].trim());
            } else {
                log.warn("Tesseract not found at: {}", properties.getTesseractPath());
            }
        } catch (Exception e) {
            log.warn("Tesseract check failed: {}", e.getMessage());
        }

        // Check PaddleOCR
        try {
            Process p = new ProcessBuilder(
                properties.getPaddleocrPython(), "-c",
                "from paddleocr import PaddleOCR; print('OK')"
            ).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            if (p.waitFor(15, TimeUnit.SECONDS) && p.exitValue() == 0 && out.contains("OK")) {
                paddleocrAvailable = true;
                log.info("PaddleOCR: available (python={})", properties.getPaddleocrPython());
            } else {
                log.warn("PaddleOCR not available: {}", out);
            }
        } catch (Exception e) {
            log.info("PaddleOCR check skipped: {}", e.getMessage());
        }

        log.info("OCR engine: {} (tesseract={}, paddleocr={})",
            properties.getProvider(), tesseractAvailable, paddleocrAvailable);
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean needsOcr(byte[] pdfBytes) {
        if (!isEnabled() || pdfBytes == null || pdfBytes.length == 0) return false;
        try {
            String text = extractTextLayer(pdfBytes);
            return text == null || text.trim().length() < MIN_TEXT_LENGTH;
        } catch (Exception e) {
            return true;
        }
    }

    private String extractTextLayer(byte[] pdfBytes) throws IOException {
        try (var doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(3, doc.getNumberOfPages()));
            return stripper.getText(doc);
        }
    }

    /**
     * OCR PDF: 根据配置选择 Tesseract 或 PaddleOCR 引擎
     */
    public OcrResult ocrPdf(byte[] pdfBytes, int maxPages, String language) {
        if (!isEnabled() || pdfBytes == null) {
            return new OcrResult("", 0, 0, false);
        }

        if ("paddleocr".equalsIgnoreCase(properties.getProvider())) {
            if (paddleocrAvailable) {
                return ocrPdfWithPaddle(pdfBytes, maxPages, language);
            }
            log.warn("PaddleOCR configured but not available, falling back to Tesseract");
        }
        return ocrPdfWithTesseract(pdfBytes, maxPages, language);
    }

    // ============================================================
    // Tesseract OCR
    // ============================================================

    private OcrResult ocrPdfWithTesseract(byte[] pdfBytes, int maxPages, String language) {
        if (!tesseractAvailable) {
            log.warn("Tesseract not available");
            return new OcrResult("", 0, 0, false);
        }

        long startTime = System.currentTimeMillis();
        Path tempDir = null;
        int pagesProcessed = 0;
        StringBuilder fullText = new StringBuilder();

        try {
            PDDocument doc = Loader.loadPDF(pdfBytes);
            int totalPages = doc.getNumberOfPages();
            int pages = Math.min(totalPages, Math.min(maxPages, MAX_OCR_PAGES));
            String lang = (language != null && !language.isEmpty())
                ? language : properties.getLanguage();
            PDFRenderer renderer = new PDFRenderer(doc);
            tempDir = Files.createTempDirectory("ocr_");

            for (int i = 0; i < pages; i++) {
                Path imagePath = null;
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, properties.getDpi());
                    imagePath = tempDir.resolve("page_" + i + ".png");
                    ImageIO.write(image, "png", imagePath.toFile());
                    String pageText = ocrPageTesseract(imagePath, lang, i);
                    if (pageText != null && !pageText.isBlank()) {
                        if (fullText.length() > 0) fullText.append("\n\n");
                        fullText.append(pageText);
                    }
                    pagesProcessed++;
                } catch (Exception e) {
                    log.warn("Tesseract failed page {}: {}", i, e.getMessage());
                } finally {
                    if (imagePath != null) {
                        try { Files.deleteIfExists(imagePath); } catch (IOException ignored) {}
                    }
                }
            }
            doc.close();
        } catch (IOException e) {
            log.warn("PDF rendering failed: {}", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        String result = normalizeCjkSpacing(fullText.toString().trim());
        log.info("Tesseract OCR: {} pages, {} chars, {}ms",
            pagesProcessed, result.length(), elapsed);
        return new OcrResult(result, pagesProcessed, elapsed, false);
    }

    // ============================================================
    // PaddleOCR
    // ============================================================

    private OcrResult ocrPdfWithPaddle(byte[] pdfBytes, int maxPages, String language) {
        long startTime = System.currentTimeMillis();
        int pagesProcessed = 0;
        StringBuilder fullText = new StringBuilder();

        try {
            PDDocument doc = Loader.loadPDF(pdfBytes);
            int totalPages = doc.getNumberOfPages();
            int pages = Math.min(totalPages, Math.min(maxPages, MAX_OCR_PAGES));
            String lang = (language != null && !language.isEmpty())
                ? language : "ch";
            PDFRenderer renderer = new PDFRenderer(doc);

            for (int i = 0; i < pages; i++) {
                File tmpFile = null;
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, properties.getDpi());
                    tmpFile = File.createTempFile("paddleocr_page_", ".png");
                    ImageIO.write(image, "png", tmpFile);

                    String pageText = ocrPagePaddle(tmpFile, lang);
                    if (pageText != null && !pageText.isBlank()) {
                        if (fullText.length() > 0) fullText.append("\n\n");
                        fullText.append("--- 第 " + (i + 1) + " 页 ---\n");
                        fullText.append(pageText);
                    }
                    pagesProcessed++;

                    if (pages > 1) {
                        log.info("PaddleOCR page {}/{}: {} chars",
                            i + 1, pages, pageText != null ? pageText.length() : 0);
                    }
                } catch (Exception e) {
                    log.warn("PaddleOCR failed page {}: {}", i, e.getMessage());
                } finally {
                    if (tmpFile != null) tmpFile.delete();
                }
            }
            doc.close();
        } catch (IOException e) {
            log.error("PaddleOCR PDF rendering failed: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        String result = fullText.toString().trim();
        log.info("PaddleOCR: {} pages, {} chars, {}ms",
            pagesProcessed, result.length(), elapsed);
        return new OcrResult(result, pagesProcessed, elapsed, false);
    }

    private String ocrPagePaddle(File imageFile, String lang) throws IOException {
        String scriptPath = properties.getPaddleocrScript();
        // Resolve relative to working directory or use absolute
        File scriptFile = new File(scriptPath);
        if (!scriptFile.isAbsolute()) {
            // Try relative to current dir
            scriptFile = new File(System.getProperty("user.dir"), scriptPath);
        }

        List<String> command = new ArrayList<>();
        command.add(properties.getPaddleocrPython());
        command.add(scriptFile.getAbsolutePath());
        command.add(imageFile.getAbsolutePath());
        command.add("--lang");
        command.add(lang);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process p = pb.start();

        boolean finished;
        try {
            finished = p.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
            return "";
        }

        if (!finished) {
            p.destroyForcibly();
            log.warn("PaddleOCR timeout after {}s", properties.getTimeoutSeconds());
            return "";
        }

        // Read stderr for errors
        String errOutput = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!errOutput.isEmpty() && errOutput.contains("ERROR")) {
            log.warn("PaddleOCR error: {}", errOutput.substring(0, Math.min(200, errOutput.length())));
        }

        if (p.exitValue() != 0) {
            log.warn("PaddleOCR exited with code {}, stderr: {}",
                p.exitValue(), errOutput.substring(0, Math.min(200, errOutput.length())));
            return "";
        }

        byte[] outBytes = p.getInputStream().readAllBytes();
        try {
            return new String(outBytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return new String(outBytes, Charset.forName("GBK")).trim();
        }
    }

    // ============================================================
    // Tesseract page
    // ============================================================

    private String ocrPageTesseract(Path imagePath, String language, int pageNum) {
        List<String> command = List.of(
            properties.getTesseractPath(),
            imagePath.toAbsolutePath().toString(),
            "stdout",
            "-l", language,
            "--psm", "3"
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();

            boolean finished = p.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.warn("OCR timeout page {} after {}s", pageNum, properties.getTimeoutSeconds());
                return "";
            }

            if (p.exitValue() != 0) {
                log.warn("Tesseract exit code {} on page {}", p.exitValue(), pageNum);
                return "";
            }

            byte[] outBytes = p.getInputStream().readAllBytes();
            try {
                return new String(outBytes, StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                return new String(outBytes, Charset.forName("GBK")).trim();
            }
        } catch (IOException | InterruptedException e) {
            log.warn("OCR error page {}: {}", pageNum, e.getMessage());
            return "";
        }
    }

    // ============================================================
    // Utilities
    // ============================================================

    static String normalizeCjkSpacing(String text) {
        if (text == null || text.isEmpty()) return text;
        text = TESSERACT_NOISE.matcher(text).replaceAll("");
        String result = CJK_SPACING.matcher(text).replaceAll("$1$2");
        for (int i = 0; i < 3; i++) {
            String prev = result;
            result = CJK_SPACING.matcher(result).replaceAll("$1$2");
            if (result.equals(prev)) break;
        }
        return result;
    }

    private void cleanupTempDir(Path tempDir) {
        if (tempDir == null) return;
        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
    }

    public record OcrResult(String text, int pagesProcessed, long elapsedMs, boolean timedOut) {}
}
