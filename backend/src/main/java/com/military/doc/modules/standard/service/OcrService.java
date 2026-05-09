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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OcrService {

    private static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_OCR_PAGES = 50;

    // CJK Unicode ranges
    private static final Pattern CJK_CHAR = Pattern.compile(
        "[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF\\u2F800-\\u2FA1F]");
    // Remove space between CJK characters, and between CJK char + CJK punctuation
    private static final Pattern CJK_SPACING = Pattern.compile(
        "([" +
        "\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF\\u2F800-\\u2FA1F" +
        "\\u3000-\\u303F\\uFF00-\\uFFEF" +
        "])\\s+([" +
        "\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF\\u2F800-\\u2FA1F" +
        "\\u3000-\\u303F\\uFF00-\\uFFEF" +
        "])");
    // Tesseract diagnostic noise that leaks into stdout
    private static final Pattern TESSERACT_NOISE = Pattern.compile(
        "(?i)(Image too small|Line cannot be recognized|no best words|"
        + "Empty page|Warning:|Error during processing).*");

    private final OcrProperties properties;
    private boolean tesseractAvailable;

    public OcrService(OcrProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void checkTesseract() {
        if (!properties.isEnabled()) {
            log.info("OCR is disabled (ocr.enabled=false)");
            tesseractAvailable = false;
            return;
        }
        try {
            Process p = new ProcessBuilder(properties.getTesseractPath(), "--version")
                .redirectErrorStream(true)
                .start();
            boolean finished = p.waitFor(10, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0) {
                tesseractAvailable = true;
                log.info("Tesseract OCR detected at: {}", properties.getTesseractPath());
            } else {
                tesseractAvailable = false;
                log.warn("Tesseract not found at: {}. OCR will be skipped.", properties.getTesseractPath());
            }
        } catch (Exception e) {
            tesseractAvailable = false;
            log.warn("Failed to check Tesseract at {}: {}", properties.getTesseractPath(), e.getMessage());
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled() && tesseractAvailable;
    }

    public boolean needsOcr(byte[] pdfBytes) {
        if (!isEnabled() || pdfBytes == null || pdfBytes.length == 0) return false;
        try {
            String text = extractTextLayer(pdfBytes);
            return text == null || text.trim().length() < MIN_TEXT_LENGTH;
        } catch (Exception e) {
            return true; // If can't even extract, likely needs OCR
        }
    }

    private String extractTextLayer(byte[] pdfBytes) throws IOException {
        try (var doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(3, doc.getNumberOfPages()));
            return stripper.getText(doc);
        }
    }

    public OcrResult ocrPdf(byte[] pdfBytes, int maxPages, String language) {
        if (!isEnabled() || pdfBytes == null) {
            return new OcrResult("", 0, 0, false);
        }

        long startTime = System.currentTimeMillis();
        Path tempDir = null;
        int pagesProcessed = 0;
        boolean timedOut = false;
        StringBuilder fullText = new StringBuilder();

        try {
            PDDocument doc = Loader.loadPDF(pdfBytes);
            int totalPages = doc.getNumberOfPages();
            int pages = Math.min(totalPages, Math.min(maxPages, MAX_OCR_PAGES));
            String lang = (language != null && !language.isEmpty()) ? language : properties.getLanguage();
            PDFRenderer renderer = new PDFRenderer(doc);

            tempDir = Files.createTempDirectory("ocr_");

            for (int i = 0; i < pages; i++) {
                Path imagePath = null;
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, properties.getDpi());
                    imagePath = tempDir.resolve("page_" + i + ".png");
                    ImageIO.write(image, "png", imagePath.toFile());

                    String pageText = ocrPage(imagePath, lang, i);
                    if (pageText != null && !pageText.isBlank()) {
                        if (fullText.length() > 0) fullText.append("\n\n");
                        fullText.append(pageText);
                    }
                    pagesProcessed++;
                } catch (Exception e) {
                    log.warn("OCR failed for page {}: {}", i, e.getMessage());
                } finally {
                    if (imagePath != null) {
                        try { Files.deleteIfExists(imagePath); } catch (IOException ignored) {}
                    }
                }
            }

            doc.close();
        } catch (IOException e) {
            log.warn("OCR PDF rendering failed: {}", e.getMessage());
        } finally {
            cleanupTempDir(tempDir);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        String result = normalizeCjkSpacing(fullText.toString().trim());
        log.info("OCR completed: {} pages, {} chars, {}ms, timedOut={}",
            pagesProcessed, result.length(), elapsed, timedOut);

        return new OcrResult(result, pagesProcessed, elapsed, timedOut);
    }

    // Clean up OCR output: remove Tesseract diagnostic noise and fix CJK spacing
    static String normalizeCjkSpacing(String text) {
        if (text == null || text.isEmpty()) return text;
        // Remove Tesseract diagnostic lines
        text = TESSERACT_NOISE.matcher(text).replaceAll("");
        // Remove spaces between CJK characters
        String result = CJK_SPACING.matcher(text).replaceAll("$1$2");
        for (int i = 0; i < 3; i++) {
            String prev = result;
            result = CJK_SPACING.matcher(result).replaceAll("$1$2");
            if (result.equals(prev)) break;
        }
        return result;
    }

    private String ocrPage(Path imagePath, String language, int pageNum) {
        List<String> command = List.of(
            properties.getTesseractPath(),
            imagePath.toAbsolutePath().toString(),
            "stdout",
            "-l", language,
            "--psm", "3"
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            // Don't merge stderr — Tesseract diagnostics on stderr would pollute OCR text
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();

            boolean finished = p.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                log.warn("OCR timeout on page {} after {}s", pageNum, properties.getTimeoutSeconds());
                return "";
            }

            if (p.exitValue() != 0) {
                log.warn("Tesseract exited with code {} on page {}", p.exitValue(), pageNum);
                return "";
            }

            byte[] outBytes = p.getInputStream().readAllBytes();
            // Try UTF-8 first, fall back to GBK for Windows Chinese locale
            try {
                return new String(outBytes, StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                return new String(outBytes, Charset.forName("GBK")).trim();
            }
        } catch (IOException | InterruptedException e) {
            log.warn("OCR process error on page {}: {}", pageNum, e.getMessage());
            return "";
        }
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
