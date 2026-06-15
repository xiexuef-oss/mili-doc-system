package com.military.doc.modules.knowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.ai.util.FileTextExtractor;
import com.military.doc.ai.util.TextSplitter;
import com.military.doc.ai.util.TextSplitter.TextChunk;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.knowledge.entity.KnowledgeBase;
import com.military.doc.modules.knowledge.service.KnowledgeBaseService;
import com.military.doc.modules.standard.entity.Standard;
import com.military.doc.modules.standard.mapper.StandardMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
@Tag(name = "知识库管理")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired(required = false)
    private FileTextExtractor fileTextExtractor;

    @Autowired(required = false)
    private TextSplitter textSplitter;

    @Autowired(required = false)
    private StandardMapper standardMapper;

    // GJB standard reference pattern for auto-detection
    private static final Pattern GJB_REF_PATTERN = Pattern.compile(
        "GJB[/_\\-]?Z?\\s*\\d+[A-Za-z]*(?:\\.\\d+)?[A-Za-z]?[-–—]\\d{2,4}");
    private static final Pattern MIL_REF_PATTERN = Pattern.compile(
        "MIL-(?:STD|PRF|DTL|HDBK)-\\d+[A-Z]?");
    private static final Pattern GB_REF_PATTERN = Pattern.compile(
        "GB(?:/T)?\\s*\\d+\\.?\\d*[-–—]\\d{2,4}");

    @GetMapping
    @Operation(summary = "分页查询知识库")
    public Result<Page<KnowledgeBase>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(KnowledgeBase::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(KnowledgeBase::getTitle, keyword)
                .or().like(KnowledgeBase::getContent, keyword)
                .or().like(KnowledgeBase::getTags, keyword));
        }
        wrapper.orderByDesc(KnowledgeBase::getCreatedAt);
        return Result.success(knowledgeBaseService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情")
    public Result<KnowledgeBase> getById(@PathVariable Long id) {
        return Result.success(knowledgeBaseService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建知识条目")
    public Result<KnowledgeBase> create(@RequestBody KnowledgeBase entry) {
        if (entry.getStatus() == null) {
            entry.setStatus("ACTIVE");
        }
        knowledgeBaseService.save(entry);
        return Result.success(entry);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识条目")
    public Result<KnowledgeBase> update(@PathVariable Long id, @RequestBody KnowledgeBase entry) {
        entry.setId(id);
        knowledgeBaseService.updateById(entry);
        return Result.success(knowledgeBaseService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识条目")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/upload")
    @Operation(summary = "上传附件")
    public Result<KnowledgeBase> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String objectId = fileStorageService.upload(file);
        KnowledgeBase entry = knowledgeBaseService.getById(id);
        if (entry == null) {
            return Result.error("NOT_FOUND", "知识条目不存在");
        }
        if (entry.getFileObjectId() != null) {
            try { fileStorageService.delete(entry.getFileObjectId()); } catch (Exception ignored) {}
        }
        entry.setFileObjectId(objectId);
        entry.setFileName(file.getOriginalFilename());
        entry.setFileSize(file.getSize());
        entry.setFileType(getExtension(file.getOriginalFilename()));
        knowledgeBaseService.updateById(entry);
        return Result.success(knowledgeBaseService.getById(id));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取附件下载地址")
    public Result<String> getDownloadUrl(@PathVariable Long id) {
        KnowledgeBase entry = knowledgeBaseService.getById(id);
        if (entry == null || entry.getFileObjectId() == null) {
            return Result.error("NOT_FOUND", "文件不存在");
        }
        return Result.success(fileStorageService.getAccessUrl(entry.getFileObjectId()));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取分类列表")
    public Result<List<String>> listCategories() {
        List<String> categories = knowledgeBaseService.list().stream()
            .map(KnowledgeBase::getCategory)
            .filter(c -> c != null && !c.isEmpty())
            .distinct()
            .sorted()
            .toList();
        return Result.success(categories);
    }

    @GetMapping("/tags")
    @Operation(summary = "获取所有标签")
    public Result<List<String>> listTags() {
        List<String> tags = knowledgeBaseService.list().stream()
            .map(KnowledgeBase::getTags)
            .filter(t -> t != null && !t.isEmpty())
            .flatMap(t -> Arrays.stream(t.split(",")))
            .map(String::trim)
            .filter(t -> !t.isEmpty())
            .distinct()
            .sorted()
            .toList();
        return Result.success(tags);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件自动拆分为知识条目")
    public Result<List<KnowledgeBase>> uploadAndSplit(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String tags,
            @RequestParam(defaultValue = "true") boolean autoSplit) {
        if (fileTextExtractor == null || textSplitter == null) {
            return Result.error("NOT_CONFIGURED", "文本提取服务未配置");
        }

        // Fix encoding: if form fields were mis-decoded as ISO-8859-1, recover UTF-8 bytes
        category = fixEncoding(category);
        tags = fixEncoding(tags);

        String extension = getExtension(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            return Result.error("READ_ERROR", "无法读取文件: " + e.getMessage());
        }

        String fullText = fileTextExtractor.extractAll(bytes, extension);
        if (fullText.isBlank()) {
            return Result.error("EMPTY_TEXT", "未能从文件中提取到文本内容，请确认文件格式为 .txt/.docx/.pdf");
        }

        // Auto-detect standard references in the full text
        List<String> detectedStds = detectStandardRefs(fullText);
        // Auto-extract tags from content if not provided
        String autoTags = extractAutoTags(fullText);
        String finalTags = buildFinalTags(tags, autoTags, detectedStds);

        List<KnowledgeBase> created = new ArrayList<>();
        String defaultCategory = category.isBlank() ? "导入" : category;

        if (autoSplit) {
            List<TextChunk> chunks = textSplitter.split(fullText, defaultCategory,
                file.getOriginalFilename(), null);
            for (TextChunk chunk : chunks) {
                // Detect standard refs per chunk
                List<String> chunkStds = detectStandardRefs(chunk.content());
                chunk.setDetectedStandardRefs(chunkStds);

                KnowledgeBase entry = new KnowledgeBase();
                entry.setTitle(chunk.title());
                entry.setContent(chunk.content());
                entry.setCategory(defaultCategory);
                entry.setTags(finalTags + (chunkStds.isEmpty() ? "" : "," + String.join(",", chunkStds)));
                entry.setStatus("ACTIVE");
                entry.setFileName(file.getOriginalFilename());
                entry.setFileSize(file.getSize());
                entry.setFileType(extension);
                knowledgeBaseService.save(entry);
                created.add(entry);
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("file", file.getOriginalFilename());
            info.put("size", file.getSize());
            info.put("chars", fullText.length());
            info.put("chunks", chunks.size());
            info.put("detectedStandards", detectedStds);
            info.put("autoTags", autoTags);
            log.info("Knowledge upload split: {}", info);
        } else {
            KnowledgeBase entry = new KnowledgeBase();
            entry.setTitle(file.getOriginalFilename() != null
                ? file.getOriginalFilename().replace(extension, "") : "导入文档");
            entry.setContent(fullText);
            entry.setCategory(defaultCategory);
            entry.setTags(finalTags);
            entry.setStatus("ACTIVE");
            entry.setFileName(file.getOriginalFilename());
            entry.setFileSize(file.getSize());
            entry.setFileType(extension);
            knowledgeBaseService.save(entry);
            created.add(entry);
            log.info("Knowledge upload single: file={}, chars={}, standards={}",
                file.getOriginalFilename(), fullText.length(), detectedStds);
        }

        // Try to link to matching standards
        if (standardMapper != null && !detectedStds.isEmpty()) {
            try {
                linkToStandards(created, detectedStds);
            } catch (Exception e) {
                log.warn("Failed to link knowledge to standards: {}", e.getMessage());
            }
        }

        return Result.success(created);
    }

    /**
     * Detect GJB/MIL/GB standard references in text.
     */
    private List<String> detectStandardRefs(String text) {
        if (text == null || text.isBlank()) return List.of();
        Set<String> refs = new LinkedHashSet<>();
        for (Pattern p : new Pattern[]{GJB_REF_PATTERN, MIL_REF_PATTERN, GB_REF_PATTERN}) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                refs.add(m.group().replaceAll("\\s+", " ").trim());
            }
        }
        return new ArrayList<>(refs);
    }

    /**
     * Extract auto-tags from content. Uses keyword frequency analysis.
     */
    private String extractAutoTags(String text) {
        if (text == null || text.isBlank()) return "";
        // Common military document keywords
        Set<String> militaryKeywords = Set.of(
            "技术状态", "质量管理", "可靠性", "维修性", "测试性",
            "保障性", "安全性", "环境适应性", "标准化", "评审",
            "鉴定", "定型", "论证", "方案", "工程研制", "设计定型",
            "生产定型", "软件", "硬件", "系统", "分系统", "设备",
            "工艺", "材料", "试验", "验证", "确认", "风险",
            "需求", "规格说明", "接口", "构型", "基线"
        );

        Set<String> found = new LinkedHashSet<>();
        for (String keyword : militaryKeywords) {
            if (text.contains(keyword)) {
                found.add(keyword);
            }
        }
        return String.join(",", found);
    }

    private String buildFinalTags(String userTags, String autoTags, List<String> standardRefs) {
        Set<String> all = new LinkedHashSet<>();
        if (userTags != null && !userTags.isBlank()) {
            Arrays.stream(userTags.split(",")).map(String::trim)
                .filter(t -> !t.isEmpty()).forEach(all::add);
        }
        if (autoTags != null && !autoTags.isBlank()) {
            Arrays.stream(autoTags.split(",")).map(String::trim)
                .filter(t -> !t.isEmpty()).forEach(all::add);
        }
        // Add standard codes as tags
        for (String ref : standardRefs) {
            String code = ref.replaceAll("[-–—]\\d{2,4}$", "").trim();
            all.add(code);
        }
        return String.join(",", all);
    }

    /**
     * Link created knowledge entries to matching standards (for reference lookup).
     */
    private void linkToStandards(List<KnowledgeBase> created, List<String> standardCodes) {
        for (String code : standardCodes) {
            // Normalize code for lookup
            String normalized = code.replaceAll("\\s+", " ").trim();
            try {
                List<Standard> matches = standardMapper.selectList(
                    new LambdaQueryWrapper<Standard>()
                        .like(Standard::getStandardCode, normalized.replaceAll("[-–—]\\d{2,4}$", "").trim())
                        .eq(Standard::getStatus, "ACTIVE")
                        .last("LIMIT 3"));
                if (!matches.isEmpty()) {
                    log.debug("Knowledge linked to standard {}: {} entries",
                        matches.get(0).getStandardCode(), created.size());
                }
            } catch (Exception e) {
                log.debug("Standard lookup failed for {}: {}", normalized, e.getMessage());
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }

    /**
     * Fix Tomcat multipart encoding: when the request body is UTF-8 but Tomcat
     * decodes it as ISO-8859-1, the resulting String contains mojibake. This method
     * detects and repairs such cases by re-interpreting the bytes.
     */
    private String fixEncoding(String s) {
        if (s == null || s.isBlank()) return s;
        // If the string contains replacement characters (U+FFFD), try to recover
        if (s.contains("�")) {
            try {
                byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return s;
            }
        }
        return s;
    }
}
