package com.military.doc.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Component
public class FileTextExtractor {

    private static final int MAX_TEXT_LENGTH = 5000;
    private static final int FULL_MAX_LENGTH = 500_000;

    /** Extract text for context assembly (truncated to 5000 chars). */
    public String extract(byte[] bytes, String extension) {
        return extract(bytes, extension, MAX_TEXT_LENGTH);
    }

    /** Extract full text for knowledge import (truncated to 500K chars). */
    public String extractFull(byte[] bytes, String extension) {
        return extract(bytes, extension, FULL_MAX_LENGTH);
    }

    private String extract(byte[] bytes, String extension, int maxLength) {
        if (bytes == null || bytes.length == 0) return "";
        try {
            String text = switch (extension.toLowerCase()) {
                case ".pdf" -> extractPdfFull(bytes, maxLength);
                case ".docx" -> extractDocx(bytes);
                case ".txt" -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                default -> "";
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

    private String extractPdfFull(byte[] bytes, int maxLength) throws IOException {
        try (var doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(false);
            // Read all pages for knowledge import
            int totalPages = doc.getNumberOfPages();
            int endPage = Math.min(totalPages, 50); // max 50 pages
            stripper.setEndPage(endPage);
            String text = stripper.getText(doc);
            return text;
        }
    }

    /** Alias for full extraction, used by knowledge upload. */
    public String extractAll(byte[] bytes, String extension) {
        return extractFull(bytes, extension);
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (var doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(false);
            stripper.setEndPage(10);
            return stripper.getText(doc);
        }
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
