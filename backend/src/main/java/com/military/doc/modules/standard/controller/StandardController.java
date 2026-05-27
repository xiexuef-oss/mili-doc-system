package com.military.doc.modules.standard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.ai.context.VectorIndexService;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.standard.entity.Standard;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.service.StandardService;
import com.military.doc.modules.standard.service.StandardClauseService;
import com.military.doc.config.OcrProperties;
import com.military.doc.modules.standard.service.StandardParseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/standards")
@Tag(name = "标准库管理")
public class StandardController {

    @Autowired
    private StandardService standardService;

    @Autowired
    private StandardClauseService standardClauseService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StandardParseService standardParseService;

    @Autowired
    private OcrProperties ocrProperties;

    @Autowired
    private VectorIndexService vectorIndexService;

    // ---- Standard CRUD ----

    @PostMapping("/parse")
    @Operation(summary = "上传标准文件并自动解析元数据和条款")
    public Result<Map<String, Object>> parseFile(@RequestParam("file") MultipartFile file) {
        StandardParseService.StandardParseResult result = standardParseService.parse(file);
        return Result.success(Map.of(
            "standard", result.standard(),
            "extractedText", result.extractedText(),
            "extractedClauses", (Object) result.clauses(),
            "ocrUsed", result.ocrUsed(),
            "ocrStats", result.ocrStats() != null ? result.ocrStats() : ""
        ));
    }

    @PostMapping("/batch-upload")
    @Operation(summary = "批量上传标准文件并自动解析元数据和条款")
    public Result<List<Standard>> batchUpload(@RequestParam("files") List<MultipartFile> files) {
        List<Standard> standards = files.stream().map(file -> {
            StandardParseService.StandardParseResult result = standardParseService.parse(file);
            Standard standard = result.standard();
            standardService.save(standard);
            saveExtractedClauses(standard.getId(), result.clauses());
            return standard;
        }).toList();
        return Result.success(standards);
    }

    private void saveExtractedClauses(Long standardId, List<StandardParseService.StandardClauseExtract> extracts) {
        if (extracts == null || extracts.isEmpty()) return;

        List<StandardClause> clauses = new ArrayList<>();
        for (StandardParseService.StandardClauseExtract extract : extracts) {
            StandardClause clause = new StandardClause();
            clause.setStandardId(standardId);
            clause.setClauseNumber(extract.clauseNumber);
            clause.setClauseTitle(extract.clauseTitle);
            clause.setClauseContent(extract.clauseContent);
            clause.setOrderNum(extract.orderNum);
            clauses.add(clause);
        }
        standardClauseService.saveBatch(clauses);

        // Map clause number -> ID (IDs are populated after saveBatch)
        Map<String, Long> numberToId = new LinkedHashMap<>();
        for (int i = 0; i < extracts.size(); i++) {
            numberToId.put(extracts.get(i).clauseNumber, clauses.get(i).getId());
        }

        // Resolve parent IDs in batch
        List<StandardClause> needParentUpdate = new ArrayList<>();
        for (StandardParseService.StandardClauseExtract extract : extracts) {
            if (extract.parentClauseNumber != null) {
                Long childId = numberToId.get(extract.clauseNumber);
                Long parentId = numberToId.get(extract.parentClauseNumber);
                if (childId != null && parentId != null) {
                    StandardClause update = new StandardClause();
                    update.setId(childId);
                    update.setParentId(parentId);
                    needParentUpdate.add(update);
                }
            }
        }
        if (!needParentUpdate.isEmpty()) {
            standardClauseService.updateBatchById(needParentUpdate);
        }
    }

    @GetMapping
    @Operation(summary = "分页查询标准")
    public Result<Page<Standard>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String standardType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
        if (standardType != null && !standardType.isEmpty()) {
            wrapper.eq(Standard::getStandardType, standardType);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Standard::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Standard::getStandardCode, keyword)
                .or().like(Standard::getStandardName, keyword)
                .or().like(Standard::getDescription, keyword));
        }
        wrapper.orderByAsc(Standard::getStandardCode);
        return Result.success(standardService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取标准详情")
    public Result<Standard> getById(@PathVariable Long id) {
        return Result.success(standardService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建标准(有文件时自动提取条款)")
    public Result<Standard> create(@RequestBody Standard standard) {
        if (standard.getStatus() == null) {
            standard.setStatus("ACTIVE");
        }
        standardService.save(standard);

        // If standard was created with a file (from parse flow), extract clauses
        if (standard.getFileObjectId() != null && !standard.getFileObjectId().isEmpty()) {
            try (InputStream is = fileStorageService.download(standard.getFileObjectId());
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                String fileName = standard.getFileName() != null ? standard.getFileName() : "standard.pdf";
                List<StandardParseService.StandardClauseExtract> extracts =
                    standardParseService.extractClausesFromBytes(baos.toByteArray(), fileName);
                saveExtractedClauses(standard.getId(), extracts);
            } catch (Exception ignored) {
                // Clause extraction is best-effort; don't fail the create
            }
        }

        return Result.success(standard);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标准")
    public Result<Standard> update(@PathVariable Long id, @RequestBody Standard standard) {
        standard.setId(id);
        standardService.updateById(standard);
        return Result.success(standardService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标准")
    public Result<Void> delete(@PathVariable Long id) {
        standardService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/upload")
    @Operation(summary = "上传标准文件并自动提取条款")
    public Result<Standard> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String objectId = fileStorageService.upload(file);
        Standard standard = standardService.getById(id);
        if (standard == null) {
            return Result.error("NOT_FOUND", "标准不存在");
        }
        if (standard.getFileObjectId() != null) {
            try { fileStorageService.delete(standard.getFileObjectId()); } catch (Exception ignored) {}
        }
        standard.setFileObjectId(objectId);
        standard.setFileName(file.getOriginalFilename());
        standard.setFileSize(file.getSize());
        standard.setFileType(getExtension(file.getOriginalFilename()));
        standardService.updateById(standard);

        // Extract and save clauses (use bytes to avoid double-upload)
        try {
            byte[] bytes = file.getBytes();
            List<StandardParseService.StandardClauseExtract> extracts =
                standardParseService.extractClausesFromBytes(bytes, file.getOriginalFilename());
            standardClauseService.remove(new LambdaQueryWrapper<StandardClause>().eq(StandardClause::getStandardId, id));
            saveExtractedClauses(id, extracts);
        } catch (Exception ignored) {
            // Clause extraction is best-effort
        }

        return Result.success(standardService.getById(id));
    }

    @PostMapping("/{id}/extract-clauses")
    @Operation(summary = "为已有标准重新提取条款")
    public Result<Map<String, Object>> extractClauses(@PathVariable Long id) {
        Standard standard = standardService.getById(id);
        if (standard == null) {
            return Result.error("NOT_FOUND", "标准不存在");
        }
        if (standard.getFileObjectId() == null) {
            return Result.error("NO_FILE", "请先上传标准文件");
        }

        try (InputStream is = fileStorageService.download(standard.getFileObjectId());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            byte[] fileBytes = baos.toByteArray();
            String fileName = standard.getFileName() != null ? standard.getFileName() : "standard.pdf";

            int textLen = standardParseService.extractTextLength(fileBytes, fileName);
            List<StandardParseService.StandardClauseExtract> extracts =
                standardParseService.extractClausesFromBytes(fileBytes, fileName);

            // Replace existing clauses
            standardClauseService.remove(new LambdaQueryWrapper<StandardClause>().eq(StandardClause::getStandardId, id));
            saveExtractedClauses(id, extracts);

            List<StandardClause> clauses = standardClauseService.list(
                new LambdaQueryWrapper<StandardClause>()
                    .eq(StandardClause::getStandardId, id)
                    .orderByAsc(StandardClause::getOrderNum)
            );

            String warning = null;
            if (extracts.isEmpty()) {
                if (textLen < 100) {
                    warning = ocrProperties.isEnabled()
                        ? "该文件可能是扫描件，OCR 识别也未能提取到有效文字。请确认 Tesseract 已安装中文语言包"
                        : "该文件可能是扫描件，无法提取文字内容。请在 application.yml 中启用 ocr.enabled 并安装 Tesseract OCR";
                } else {
                    warning = "未能识别到标准的条款结构，请手动添加或检查文件格式";
                }
            }

            return Result.success(Map.of(
                "clauses", (Object) clauses,
                "clauseCount", clauses.size(),
                "textLength", textLen,
                "warning", warning != null ? warning : ""
            ));
        } catch (Exception e) {
            String msg = "条款提取失败: " + e.getClass().getSimpleName() + " - " + (e.getMessage() != null ? e.getMessage() : "");
            return Result.error("PARSE_FAILED", msg);
        }
    }

    @PostMapping("/batch-extract-clauses")
    @Operation(summary = "批量重新提取所有标准的条款")
    public Result<Map<String, Object>> batchExtractClauses() {
        List<Standard> all = standardService.list();
        int success = 0, skipped = 0, failed = 0;
        int totalClauses = 0;
        for (Standard s : all) {
            if (s.getFileObjectId() == null || s.getFileObjectId().isBlank()) {
                skipped++;
                continue;
            }
            try (InputStream is = fileStorageService.download(s.getFileObjectId());
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) { baos.write(buf, 0, n); }
                byte[] fileBytes = baos.toByteArray();
                String fileName = s.getFileName() != null ? s.getFileName() : "standard.pdf";
                List<StandardParseService.StandardClauseExtract> extracts =
                    standardParseService.extractClausesFromBytes(fileBytes, fileName);
                standardClauseService.remove(new LambdaQueryWrapper<StandardClause>().eq(StandardClause::getStandardId, s.getId()));
                saveExtractedClauses(s.getId(), extracts);
                totalClauses += extracts.size();
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        return Result.success(Map.of(
            "total", all.size(),
            "success", success,
            "skipped", skipped,
            "failed", failed,
            "totalClauses", totalClauses
        ));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取标准文件下载地址")
    public Result<String> getDownloadUrl(@PathVariable Long id) {
        Standard standard = standardService.getById(id);
        if (standard == null || standard.getFileObjectId() == null) {
            return Result.error("NOT_FOUND", "文件不存在");
        }
        return Result.success(fileStorageService.getAccessUrl(standard.getFileObjectId()));
    }

    @GetMapping("/types")
    @Operation(summary = "获取所有标准类型")
    public Result<List<String>> listTypes() {
        List<String> types = standardService.list().stream()
            .map(Standard::getStandardType)
            .filter(Objects::nonNull)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .toList();
        return Result.success(types);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取所有标准分类")
    public Result<List<String>> listCategories() {
        List<String> categories = standardService.list().stream()
            .map(Standard::getCategory)
            .filter(Objects::nonNull)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .toList();
        return Result.success(categories);
    }

    // ---- StandardClause CRUD ----

    @PostMapping("/repair-metadata")
    @Operation(summary = "从文件名重新提取所有标准的编号和名称")
    public Result<Map<String, Object>> repairMetadata() {
        List<Standard> all = standardService.list();
        int fixedCode = 0, fixedName = 0, skipped = 0;
        List<Map<String, String>> details = new ArrayList<>();

        for (Standard s : all) {
            String fileName = s.getFileName();
            if (fileName == null || fileName.isBlank()) {
                skipped++;
                continue;
            }
            String oldCode = s.getStandardCode();
            String oldName = s.getStandardName();

            // Re-extract from filename
            String newCode = standardParseService.extractCode(fileName);
            String newName = standardParseService.extractNameFromFilename(fileName);

            boolean changed = false;
            if (newCode != null && !newCode.isEmpty() && !newCode.equals(oldCode)) {
                s.setStandardCode(newCode);
                s.setStandardType(standardParseService.determineType(newCode));
                s.setVersion(standardParseService.extractVersion(newCode));
                fixedCode++;
                changed = true;
            }
            if (newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
                s.setStandardName(newName);
                fixedName++;
                changed = true;
            }
            if (changed) {
                standardService.updateById(s);
                details.add(Map.of(
                    "id", String.valueOf(s.getId()),
                    "fileName", fileName,
                    "oldCode", oldCode != null ? oldCode : "",
                    "newCode", s.getStandardCode(),
                    "oldName", oldName != null ? oldName : "",
                    "newName", s.getStandardName()
                ));
            }
        }

        return Result.success(Map.of(
            "total", all.size(),
            "fixedCode", fixedCode,
            "fixedName", fixedName,
            "skipped", skipped,
            "details", (Object) details
        ));
    }

    // ---- StandardClause CRUD ----

    @GetMapping("/{standardId}/clauses")
    @Operation(summary = "获取标准的条款列表")
    public Result<List<StandardClause>> listClauses(@PathVariable Long standardId) {
        List<StandardClause> clauses = standardClauseService.list(
            new LambdaQueryWrapper<StandardClause>()
                .eq(StandardClause::getStandardId, standardId)
                .orderByAsc(StandardClause::getOrderNum)
        );
        return Result.success(clauses);
    }

    @PostMapping("/{standardId}/clauses")
    @Operation(summary = "创建条款")
    public Result<StandardClause> createClause(@PathVariable Long standardId, @RequestBody StandardClause clause) {
        clause.setStandardId(standardId);
        standardClauseService.save(clause);
        reindexClauseAsync(clause.getId());
        return Result.success(clause);
    }

    @PutMapping("/{standardId}/clauses/{clauseId}")
    @Operation(summary = "更新条款")
    public Result<StandardClause> updateClause(@PathVariable Long standardId, @PathVariable Long clauseId, @RequestBody StandardClause clause) {
        clause.setId(clauseId);
        clause.setStandardId(standardId);
        standardClauseService.updateById(clause);
        reindexClauseAsync(clauseId);
        return Result.success(standardClauseService.getById(clauseId));
    }

    @DeleteMapping("/{standardId}/clauses/{clauseId}")
    @Operation(summary = "删除条款")
    public Result<Void> deleteClause(@PathVariable Long standardId, @PathVariable Long clauseId) {
        standardClauseService.removeById(clauseId);
        // embedding is cascade-deleted by FK; no reindex needed
        return Result.success();
    }

    private void reindexClauseAsync(Long clauseId) {
        try {
            vectorIndexService.reindexClause(clauseId);
        } catch (Exception e) {
            // best-effort; don't fail the request
        }
    }

    @GetMapping("/{standardId}/clauses/search")
    @Operation(summary = "搜索条款(按关键字)")
    public Result<List<StandardClause>> searchClauses(@PathVariable Long standardId, @RequestParam String keyword) {
        List<StandardClause> clauses = standardClauseService.list(
            new LambdaQueryWrapper<StandardClause>()
                .eq(StandardClause::getStandardId, standardId)
                .and(w -> w.like(StandardClause::getClauseTitle, keyword)
                    .or().like(StandardClause::getClauseContent, keyword)
                    .or().like(StandardClause::getKeywords, keyword))
                .orderByAsc(StandardClause::getOrderNum)
        );
        return Result.success(clauses);
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
