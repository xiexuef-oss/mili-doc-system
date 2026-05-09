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

    public String extract(byte[] bytes, String extension) {
        if (bytes == null || bytes.length == 0) return "";
        try {
            String text = switch (extension.toLowerCase()) {
                case ".pdf" -> extractPdf(bytes);
                case ".docx" -> extractDocx(bytes);
                case ".txt" -> new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                default -> "";
            };
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH) + "\n...(已截断)";
            }
            return text;
        } catch (IOException e) {
            log.warn("Failed to extract text from file: {}", e.getMessage());
            return "";
        }
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
