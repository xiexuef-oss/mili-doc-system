package com.military.doc.modules.standard.service;

import com.military.doc.common.storage.FileStorageService;
import com.military.doc.config.OcrProperties;
import com.military.doc.modules.standard.entity.Standard;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class StandardParseService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private OcrProperties ocrProperties;

    private static final int OCR_FALLBACK_THRESHOLD = 100;

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

    public StandardParseResult parse(byte[] bytes, String originalFilename) {
        String ext = getExtension(originalFilename).toLowerCase();
        String text;
        try {
            text = extractText(bytes, ext);
        } catch (IOException e) {
            text = "";
        }

        Standard standard = new Standard();

        String code = extractCode(text);
        if (code == null || code.isEmpty()) {
            code = extractCode(originalFilename);
        }
        standard.setStandardCode(code != null ? code : "");

        String name = extractName(text);
        if (name == null || name.isEmpty()) {
            name = extractNameFromFilename(originalFilename);
        }
        standard.setStandardName(name != null ? name : "");

        standard.setStandardType(determineType(standard.getStandardCode()));
        standard.setCategory("");
        standard.setVersion(extractVersion(standard.getStandardCode()));
        standard.setStatus("ACTIVE");

        // Upload file to storage
        String objectId = fileStorageService.upload(bytes, originalFilename);
        standard.setFileObjectId(objectId);
        standard.setFileName(originalFilename);
        standard.setFileSize((long) bytes.length);
        standard.setFileType(ext);

        // Extract clauses from full text with OCR fallback
        // Strategy: extract via PDFBox, if clause extraction fails, try OCR
        String fullText;
        boolean ocrUsed = false;
        String ocrStats = "";
        try {
            fullText = extractTextLayer(bytes, ext);
        } catch (IOException e) {
            fullText = text;
        }
        List<StandardClauseExtract> clauses = extractClauses(fullText);
        // If PDFBox text yields no clauses and it's a PDF, try OCR
        if (clauses.isEmpty() && ext.equals(".pdf") && ocrService.isEnabled()) {
            log.info("PDFBox text ({} chars) yielded no clauses; attempting OCR", fullText.trim().length());
            OcrService.OcrResult ocrResult = ocrService.ocrPdf(bytes, 50, ocrProperties.getLanguage());
            if (ocrResult != null && !ocrResult.text().isBlank()) {
                fullText = ocrResult.text();
                ocrUsed = true;
                ocrStats = ocrResult.pagesProcessed() + " pages, " + (ocrResult.elapsedMs() / 1000.0) + "s";
                clauses = extractClauses(fullText);
            }
        }

        return new StandardParseResult(standard, text, clauses, ocrUsed, ocrStats);
    }

    public StandardParseResult parse(MultipartFile file) {
        try {
            return parse(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }
    }

    /**
     * Extract clauses from file bytes without re-uploading (for re-parsing existing files).
     */
    public List<StandardClauseExtract> extractClausesFromBytes(byte[] bytes, String filename) {
        if (bytes == null || bytes.length == 0) return List.of();
        String ext = getExtension(filename).toLowerCase();
        String fullText;
        try {
            fullText = extractTextLayer(bytes, ext);
        } catch (Exception e) {
            return List.of();
        }
        if (fullText == null || fullText.isBlank()) return List.of();
        List<StandardClauseExtract> clauses = extractClauses(fullText);
        if (clauses.isEmpty() && ext.equals(".pdf") && ocrService.isEnabled()) {
            log.info("No clauses extracted from PDF text ({} chars), retrying with OCR", fullText.trim().length());
            OcrService.OcrResult ocrResult = ocrService.ocrPdf(bytes, 50, ocrProperties.getLanguage());
            if (ocrResult != null && !ocrResult.text().isBlank()) {
                clauses = extractClauses(ocrResult.text());
            }
        }
        return clauses;
    }

    private String extractText(byte[] bytes, String ext) throws IOException {
        String text = switch (ext) {
            case ".pdf" -> {
                try (var doc = Loader.loadPDF(bytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
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

        // OCR fallback for metadata (first 3 pages only)
        if (needsOcrFallback(text) && ext.equals(".pdf") && ocrService.isEnabled()) {
            OcrService.OcrResult ocrResult = ocrService.ocrPdf(bytes, 3, ocrProperties.getLanguage());
            if (ocrResult != null && !ocrResult.text().isBlank()) {
                return ocrResult.text();
            }
        }
        return text;
    }

    private boolean needsOcrFallback(String text) {
        return text == null || text.trim().length() < OCR_FALLBACK_THRESHOLD;
    }

    private String extractFullText(byte[] bytes, String ext) throws IOException {
        String text = switch (ext) {
            case ".pdf" -> {
                try (var doc = Loader.loadPDF(bytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    stripper.setAddMoreFormatting(false);
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

        // OCR fallback for full text extraction
        if (needsOcrFallback(text) && ext.equals(".pdf") && ocrService.isEnabled()) {
            OcrService.OcrResult ocrResult = ocrService.ocrPdf(bytes, 50, ocrProperties.getLanguage());
            if (ocrResult != null && !ocrResult.text().isBlank()) {
                return ocrResult.text();
            }
        }
        return text;
    }

    // Raw PDF text extraction without OCR fallback
    private String extractTextLayer(byte[] bytes, String ext) throws IOException {
        return switch (ext) {
            case ".pdf" -> {
                try (var doc = Loader.loadPDF(bytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    stripper.setAddMoreFormatting(false);
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

    // Pattern for GJB-style clause numbering: "1", "1.2", "3.1.2", "A.1", etc.
    private static final Pattern CLAUSE_HEADER = Pattern.compile(
        "^\\s*([A-Z]?\\d+(?:\\.\\d+)*)\\s+(.+?)(?:\\s*$)"
    );
    // Chinese numbering: 一、二、... 十、
    private static final Pattern CHINESE_NUM_HEADER = Pattern.compile(
        "^\\s*([一二三四五六七八九十]+)[、，,]\\s*(.+)"
    );
    // Appendix pattern
    private static final Pattern APPENDIX_HEADER = Pattern.compile(
        "^\\s*(附录\\s*[A-Z]?)\\s*(.*)"
    );

    List<StandardClauseExtract> extractClauses(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<StandardClauseExtract> clauses = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        StandardClauseExtract currentClause = null;
        StringBuilder currentContent = new StringBuilder();
        int orderNum = 0;

        // Filter noise lines
        Set<String> noiseLines = Set.of("", " ", "ICS", "备案号", "发布日期", "实施日期", "发布", "中华人民共和国");
        Pattern pageNoPattern = Pattern.compile("^\\s*[IVXLCDMivxlcdm]+\\s*$");
        Pattern punctuationOnly = Pattern.compile("^[\\s\\p{Punct}]+$");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Skip noise
            boolean isNoise = false;
            for (String noise : noiseLines) {
                if (trimmed.startsWith(noise)) { isNoise = true; break; }
            }
            if (isNoise) continue;
            if (pageNoPattern.matcher(trimmed).matches()) continue;
            if (punctuationOnly.matcher(trimmed).matches()) continue;
            if (trimmed.length() < 2) continue;

            // Try GJB-style clause header
            Matcher m = CLAUSE_HEADER.matcher(trimmed);
            if (m.matches()) {
                String clauseNum = m.group(1);
                String clauseTitle = m.group(2).trim();

                // Skip if it looks like a date or page ref rather than a real clause
                if (clauseTitle.matches("\\d{4}.*\\d{4}") || clauseTitle.length() > 120) continue;

                // Save previous clause
                if (currentClause != null) {
                    currentClause.clauseContent = currentContent.toString().trim();
                    clauses.add(currentClause);
                }
                currentContent.setLength(0);

                currentClause = new StandardClauseExtract();
                currentClause.clauseNumber = clauseNum;
                currentClause.clauseTitle = clauseTitle;
                currentClause.orderNum = orderNum++;
                continue;
            }

            // Try Chinese numbering
            m = CHINESE_NUM_HEADER.matcher(trimmed);
            if (m.matches()) {
                if (currentClause != null) {
                    currentClause.clauseContent = currentContent.toString().trim();
                    clauses.add(currentClause);
                }
                currentContent.setLength(0);

                currentClause = new StandardClauseExtract();
                currentClause.clauseNumber = chineseToArabic(m.group(1));
                currentClause.clauseTitle = m.group(2).trim();
                currentClause.orderNum = orderNum++;
                continue;
            }

            // Try appendix
            m = APPENDIX_HEADER.matcher(trimmed);
            if (m.matches()) {
                if (currentClause != null) {
                    currentClause.clauseContent = currentContent.toString().trim();
                    clauses.add(currentClause);
                }
                currentContent.setLength(0);

                currentClause = new StandardClauseExtract();
                currentClause.clauseNumber = m.group(1).trim();
                currentClause.clauseTitle = m.group(2).trim();
                currentClause.orderNum = orderNum++;
                continue;
            }

            // Accumulate content for current clause
            if (currentClause != null) {
                if (currentContent.length() > 0) currentContent.append(" ");
                currentContent.append(trimmed);
            }
        }

        // Save last clause
        if (currentClause != null) {
            currentClause.clauseContent = currentContent.toString().trim();
            clauses.add(currentClause);
        }

        // Resolve parent relationships by clause number hierarchy
        resolveParents(clauses);

        return clauses;
    }

    private void resolveParents(List<StandardClauseExtract> clauses) {
        for (int i = 0; i < clauses.size(); i++) {
            StandardClauseExtract clause = clauses.get(i);
            String num = clause.clauseNumber;
            if (num == null || !num.contains(".")) {
                clause.parentClauseNumber = null;
                continue;
            }
            // Parent is the clause with number = everything before the last dot
            String parentNum = num.substring(0, num.lastIndexOf('.'));
            // Find the closest preceding clause with this parent number
            for (int j = i - 1; j >= 0; j--) {
                if (parentNum.equals(clauses.get(j).clauseNumber)) {
                    clause.parentClauseNumber = parentNum;
                    break;
                }
            }
        }
    }

    private String chineseToArabic(String chinese) {
        Map<Character, Integer> map = Map.ofEntries(
            Map.entry('一', 1), Map.entry('二', 2), Map.entry('三', 3),
            Map.entry('四', 4), Map.entry('五', 5), Map.entry('六', 6),
            Map.entry('七', 7), Map.entry('八', 8), Map.entry('九', 9),
            Map.entry('十', 10)
        );
        int result = 0;
        int temp = 0;
        for (char c : chinese.toCharArray()) {
            Integer val = map.get(c);
            if (val == null) continue;
            if (val == 10) {
                if (temp == 0) temp = 1;
                result += temp * 10;
                temp = 0;
            } else {
                temp = val;
            }
        }
        result += temp;
        return String.valueOf(result);
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

    public int extractTextLength(byte[] bytes, String filename) {
        String ext = getExtension(filename).toLowerCase();
        try {
            String text = extractFullText(bytes, ext);
            return text != null ? text.trim().length() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public record StandardParseResult(Standard standard, String extractedText, List<StandardClauseExtract> clauses,
                                      boolean ocrUsed, String ocrStats) {}

    public static class StandardClauseExtract {
        public String clauseNumber;
        public String clauseTitle;
        public String clauseContent;
        public String parentClauseNumber;
        public int orderNum;
    }
}
