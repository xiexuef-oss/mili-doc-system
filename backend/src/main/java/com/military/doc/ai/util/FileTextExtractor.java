package com.military.doc.ai.util;

import com.military.doc.modules.standard.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FileTextExtractor {

    // 内网部署，不截断文本
    private static final int FULL_MAX_LENGTH = 2_000_000;
    // PDF文本层字符数低于此阈值时自动触发OCR
    private static final int PDF_OCR_THRESHOLD = 100;
    // OCR最大页数
    private static final int OCR_MAX_PAGES = 50;
    // OCR语言
    private static final String OCR_LANG = "chi_sim+eng";

    private final OcrService ocrService;

    public FileTextExtractor(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /** Extract text for context assembly (no truncation for intranet deployment). */
    public String extract(byte[] bytes, String extension) {
        return extract(bytes, extension, FULL_MAX_LENGTH);
    }

    /** Extract full text for knowledge import. */
    public String extractFull(byte[] bytes, String extension) {
        return extract(bytes, extension, FULL_MAX_LENGTH);
    }

    private String extract(byte[] bytes, String extension, int maxLength) {
        if (bytes == null || bytes.length == 0) return "";
        try {
            String text = switch (extension.toLowerCase()) {
                case ".pdf" -> extractPdfWithOcr(bytes, maxLength);
                case ".docx" -> extractDocx(bytes);
                case ".txt", ".md", ".markdown", ".json", ".xml", ".csv" ->
                    new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                default -> {
                    String s = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    yield s.length() > 10 ? s : "";
                }
            };
            if (text.length() > maxLength) {
                text = text.substring(0, maxLength) + "\n...(已截断)";
            }
            return text;
        } catch (IOException e) {
            log.warn("Failed to extract text from file: {}", e.getMessage());
            return "";
        }
    }

    /**
     * PDF文本提取，内置OCR回退。
     * 先用PDFBox提取文字层，若字符数不足则自动调用Tesseract OCR。
     */
    private String extractPdfWithOcr(byte[] bytes, int maxLength) throws IOException {
        // Step 1: PDFBox direct text extraction
        String text = extractPdfFull(bytes, maxLength);

        // Step 2: If text layer is sparse, try OCR
        if (text.trim().length() < PDF_OCR_THRESHOLD && ocrService.isEnabled()) {
            log.info("PDF text layer is sparse ({} chars), falling back to OCR...",
                text.trim().length());
            try {
                var ocrResult = ocrService.ocrPdf(bytes, OCR_MAX_PAGES, OCR_LANG);
                if (ocrResult != null && ocrResult.text() != null && !ocrResult.text().isBlank()) {
                    log.info("OCR extracted {} chars ({} pages, {}ms), replacing PDFBox result",
                        ocrResult.text().length(), ocrResult.pagesProcessed(), ocrResult.elapsedMs());
                    return ocrResult.text();
                }
            } catch (Exception e) {
                log.warn("OCR fallback failed: {}", e.getMessage());
            }
        }
        return text;
    }

    private String extractPdfFull(byte[] bytes, int maxLength) throws IOException {
        try (var doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(false);
            int totalPages = doc.getNumberOfPages();
            int endPage = Math.min(totalPages, 100);
            stripper.setEndPage(endPage);
            return stripper.getText(doc);
        }
    }

    /** Alias for full extraction, used by knowledge upload. */
    public String extractAll(byte[] bytes, String extension) {
        return extractFull(bytes, extension);
    }

    private String extractPdf(byte[] bytes) throws IOException {
        return extractPdfWithOcr(bytes, FULL_MAX_LENGTH);
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    // ============================================================
    // Multi-modal: Image extraction
    // ============================================================

    public List<ImageDescriptor> extractPdfImages(byte[] pdfBytes) {
        List<ImageDescriptor> images = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            for (int pageIdx = 0; pageIdx < doc.getNumberOfPages(); pageIdx++) {
                PDPage page = doc.getPage(pageIdx);
                PDResources resources = page.getResources();
                if (resources == null) continue;

                for (var name : resources.getXObjectNames()) {
                    if (resources.isImageXObject(name)) {
                        try {
                            PDImageXObject img = (PDImageXObject) resources.getXObject(name);
                            ImageDescriptor desc = new ImageDescriptor();
                            desc.setPageNumber(pageIdx + 1);
                            desc.setImageName(name.getName());
                            desc.setWidth(img.getWidth());
                            desc.setHeight(img.getHeight());
                            desc.setColorSpace(img.getColorSpace() != null
                                ? img.getColorSpace().getName() : "unknown");

                            BufferedImage buffered = img.getImage();
                            if (buffered != null) {
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                ImageIO.write(buffered, "png", bos);
                                desc.setImageBytes(bos.toByteArray());
                                desc.setImageSizeBytes(bos.size());
                            }

                            desc.setDescription(generateImageDescription(desc));
                            images.add(desc);
                        } catch (Exception e) {
                            log.debug("Skipping non-image XObject: {}", name.getName());
                        }
                    }
                }
            }
            log.info("Extracted {} images from PDF ({} pages)", images.size(), doc.getNumberOfPages());
        } catch (Exception e) {
            log.warn("Failed to extract PDF images: {}", e.getMessage());
        }
        return images;
    }

    public List<ImageDescriptor> extractDocxImages(byte[] docxBytes) {
        List<ImageDescriptor> images = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            List<XWPFPictureData> pictures = doc.getAllPictures();
            for (XWPFPictureData pic : pictures) {
                ImageDescriptor desc = new ImageDescriptor();
                desc.setImageName(pic.getFileName());
                desc.setImageBytes(pic.getData());
                desc.setImageSizeBytes(pic.getData().length);
                desc.setMimeType(pic.getPackagePart().getContentType());

                try {
                    BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(pic.getData()));
                    if (buffered != null) {
                        desc.setWidth(buffered.getWidth());
                        desc.setHeight(buffered.getHeight());
                    }
                } catch (Exception ignored) {}

                desc.setDescription(generateImageDescription(desc));
                images.add(desc);
            }
            log.info("Extracted {} images from DOCX", images.size());
        } catch (Exception e) {
            log.warn("Failed to extract DOCX images: {}", e.getMessage());
        }
        return images;
    }

    public List<ImageDescriptor> extractImages(byte[] bytes, String extension) {
        if (extension == null) return List.of();
        return switch (extension.toLowerCase()) {
            case ".pdf" -> extractPdfImages(bytes);
            case ".docx" -> extractDocxImages(bytes);
            default -> List.of();
        };
    }

    private String generateImageDescription(ImageDescriptor img) {
        StringBuilder desc = new StringBuilder();
        desc.append("[图片]");
        if (img.getPageNumber() > 0) {
            desc.append(" 页码:").append(img.getPageNumber());
        }
        if (img.getWidth() > 0 && img.getHeight() > 0) {
            desc.append(" 尺寸:").append(img.getWidth()).append("x").append(img.getHeight()).append("px");
        }
        if (img.getMimeType() != null) {
            desc.append(" 格式:").append(img.getMimeType());
        }
        if (img.getImageSizeBytes() > 0) {
            desc.append(" 大小:").append(formatBytes(img.getImageSizeBytes()));
        }
        if (img.getImageName() != null) {
            desc.append(" 名称:").append(img.getImageName());
        }
        return desc.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
    }

    @lombok.Data
    public static class ImageDescriptor {
        private String imageName;
        private int pageNumber;
        private int width;
        private int height;
        private String colorSpace;
        private String mimeType;
        private long imageSizeBytes;
        private byte[] imageBytes;
        private String description;
    }
}
