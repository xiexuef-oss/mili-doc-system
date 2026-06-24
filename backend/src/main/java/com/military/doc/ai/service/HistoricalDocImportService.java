package com.military.doc.ai.service;

import com.military.doc.ai.util.FileTextExtractor;
import com.military.doc.common.storage.FileStorageService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

/**
 * 历史文档导入服务（Phase 6）。
 * 支持 Word/PDF 批量导入，AI 识别文档类型并拆分章节。
 */
@Slf4j
@Service
public class HistoricalDocImportService {

    private final FileStorageService fileStorageService;
    private final FileTextExtractor fileTextExtractor;

    public HistoricalDocImportService(FileStorageService fileStorageService,
                                       FileTextExtractor fileTextExtractor) {
        this.fileStorageService = fileStorageService;
        this.fileTextExtractor = fileTextExtractor;
    }

    /**
     * 导入单个历史文档。
     */
    public ImportResult importDocument(Long projectId, MultipartFile file) {
        ImportResult result = new ImportResult();
        result.fileName = file.getOriginalFilename();

        try (InputStream is = file.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            String ext = getExtension(file.getOriginalFilename());

            // Extract text (with OCR fallback)
            String text = fileTextExtractor.extract(bytes, ext);
            result.extractedChars = text != null ? text.length() : 0;

            if (text == null || text.isBlank()) {
                result.success = false;
                result.error = "无法提取文本内容";
                return result;
            }

            // Detect document type from content
            result.detectedType = detectDocumentType(text);

            // Count chapters
            result.chapterCount = countChapters(text);

            // Identify reusable sections
            result.reusableSections = identifyReusableSections(text);

            result.success = true;
            log.info("Imported historical doc: {} ({} chars, type={})",
                result.fileName, result.extractedChars, result.detectedType);

        } catch (Exception e) {
            log.error("Failed to import historical document: {}", file.getOriginalFilename(), e);
            result.success = false;
            result.error = e.getMessage();
        }

        return result;
    }

    private String detectDocumentType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("可靠性大纲") || lower.contains("可靠性保证大纲")) return "reliability_outline";
        if (lower.contains("降额")) return "derating";
        if (lower.contains("可靠性预计")) return "reliability_prediction";
        if (lower.contains("可靠性分配")) return "reliability_allocation";
        if (lower.contains("fmeca") || lower.contains("故障模式")) return "fmeca";
        if (lower.contains("维修性")) return "maintainability_outline";
        if (lower.contains("测试性")) return "testability_outline";
        return "unknown";
    }

    private int countChapters(String text) {
        int count = 0;
        for (String line : text.split("\n")) {
            if (line.trim().startsWith("#") && line.trim().length() > 1) count++;
        }
        return Math.max(1, count);
    }

    private List<String> identifyReusableSections(String text) {
        List<String> sections = new ArrayList<>();
        // Simple heuristic: sections mentioning standards are often reusable
        for (String line : text.split("\n")) {
            if (line.contains("GJB ") || line.contains("GJB/Z ")) {
                sections.add(line.trim());
            }
        }
        return sections;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }

    @Data
    public static class ImportResult {
        String fileName;
        boolean success;
        String error;
        int extractedChars;
        String detectedType;
        int chapterCount;
        List<String> reusableSections = new ArrayList<>();
    }
}
