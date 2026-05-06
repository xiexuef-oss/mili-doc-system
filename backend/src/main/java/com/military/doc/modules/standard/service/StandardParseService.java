package com.military.doc.modules.standard.service;

import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.standard.entity.Standard;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StandardParseService {

    @Autowired
    private FileStorageService fileStorageService;

    // Common Chinese military/industrial standard code regex patterns
    private static final Pattern[] CODE_PATTERNS = {
        Pattern.compile("GJB/[A-Z]\\s*\\d+[-–—]\\d{4}"),
        Pattern.compile("GJB\\s*\\d+[A-Za-z]?[-–—]\\d{4}"),
        Pattern.compile("MIL-(?:STD|PRF|DTL|HDBK)-\\d+[A-Z]?"),
        Pattern.compile("GB/T\\s*\\d+\\.?\\d*[-–—]\\d{4}"),
        Pattern.compile("GB\\s*\\d+[-–—]\\d{4}"),
        Pattern.compile("QJ\\s*\\d+[A-Z]?[-–—]\\d{4}"),
        Pattern.compile("HB\\s*\\d+[-–—]\\d{4}"),
        Pattern.compile("SJ\\s*\\d+[-–—]\\d{4}"),
        Pattern.compile("CB\\s*\\d+[-–—]\\d{4}"),
        Pattern.compile("ISO\\s*\\d+[:：]\\d{4}"),
    };

    private static final Pattern[] NAME_PATTERNS = {
        Pattern.compile("(?:标准名称|名称|标准)[：:]\\s*(.+)"),
    };

    public StandardParseResult parse(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName).toLowerCase();
        String text;
        try {
            text = extractText(file, ext);
        } catch (IOException e) {
            // If text extraction fails, fall back to filename-only parsing
            text = "";
        }

        Standard standard = new Standard();

        // Try to extract standard code from text first, then filename
        String code = extractCode(text);
        if (code == null || code.isEmpty()) {
            code = extractCode(originalName);
        }
        standard.setStandardCode(code != null ? code : "");

        // Extract standard name
        String name = extractName(text);
        if (name == null || name.isEmpty()) {
            name = extractNameFromFilename(originalName);
        }
        standard.setStandardName(name != null ? name : "");

        // Determine standard type from code
        standard.setStandardType(determineType(standard.getStandardCode()));

        // Determine category (default to empty, user can set)
        standard.setCategory("");

        // Extract version/year from code
        standard.setVersion(extractVersion(standard.getStandardCode()));

        // Set status
        standard.setStatus("ACTIVE");

        // Upload file and set file info
        String objectId = fileStorageService.upload(file);
        standard.setFileObjectId(objectId);
        standard.setFileName(originalName);
        standard.setFileSize(file.getSize());
        standard.setFileType(ext);

        return new StandardParseResult(standard, text);
    }

    private String extractText(MultipartFile file, String ext) throws IOException {
        byte[] bytes = file.getBytes();

        return switch (ext) {
            case ".pdf" -> {
                try (var doc = Loader.loadPDF(bytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    // Only extract first 3 pages for performance
                    stripper.setEndPage(3);
                    yield stripper.getText(doc);
                }
            }
            case ".docx" -> {
                try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes));
                     var extractor = new XWPFWordExtractor(doc)) {
                    yield extractor.getText();
                }
            }
            default -> throw new IOException("Unsupported file type: " + ext);
        };
    }

    String extractCode(String text) {
        if (text == null || text.isEmpty()) return null;
        for (Pattern pattern : CODE_PATTERNS) {
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                return m.group().replaceAll("\\s+", " ").trim();
            }
        }
        return null;
    }

    String extractName(String text) {
        if (text == null || text.isEmpty()) return null;

        // Try explicit name label first
        for (Pattern pattern : NAME_PATTERNS) {
            Matcher m = pattern.matcher(text);
            if (m.find()) return m.group(1).trim();
        }

        // Use first meaningful line after the standard code
        String[] lines = text.split("\\r?\\n");
        boolean foundCode = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() < 2) continue;

            String lineCode = extractCode(trimmed);
            if (lineCode != null) {
                // The name might be on the same line after the code
                String after = trimmed.substring(trimmed.indexOf(lineCode) + lineCode.length()).trim();
                // Remove common separators
                after = after.replaceAll("^[\\s——–—]+", "").trim();
                if (after.length() > 2) return cleanName(after);
                foundCode = true;
                continue;
            }

            if (foundCode && trimmed.length() > 3 && !trimmed.matches(".*\\d{4}.*\\d{4}.*")) {
                return cleanName(trimmed);
            }
        }

        // Fallback: first non-empty, non-metadata line
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 5 && !trimmed.startsWith("ICS") && !trimmed.startsWith("备案号")
                && extractCode(trimmed) == null) {
                return cleanName(trimmed);
            }
        }
        return null;
    }

    private String cleanName(String name) {
        if (name == null) return null;
        return name.replaceAll("^[\\s\\p{Punct}]+|[\\s\\p{Punct}]+$", "").trim();
    }

    String extractNameFromFilename(String filename) {
        if (filename == null) return null;
        String name = filename;
        // Remove extension
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        // Remove the standard code if present
        String code = extractCode(name);
        if (code != null) {
            name = name.replace(code, "");
        }
        // Clean up separators
        name = name.replaceAll("[-–—_\\.]+", " ").trim();
        return name.isEmpty() ? null : name;
    }

    String determineType(String code) {
        if (code == null) return "";
        if (code.startsWith("GJB/Z")) return "GJB/Z";
        if (code.startsWith("GJB")) return "GJB";
        if (code.startsWith("MIL-STD") || code.startsWith("MIL-PRF") || code.startsWith("MIL-DTL")) return "MIL";
        if (code.startsWith("GB/T")) return "GB/T";
        if (code.startsWith("GB")) return "GB";
        if (code.startsWith("QJ")) return "QJ";
        if (code.startsWith("HB")) return "HB";
        if (code.startsWith("SJ")) return "SJ";
        if (code.startsWith("CB")) return "CB";
        if (code.startsWith("ISO")) return "ISO";
        return "";
    }

    String extractVersion(String code) {
        if (code == null) return "";
        Matcher m = Pattern.compile("(\\d{4})$").matcher(code);
        if (m.find()) return m.group(1);
        return "";
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }

    public record StandardParseResult(Standard standard, String extractedText) {}
}
