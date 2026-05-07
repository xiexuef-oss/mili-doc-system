package com.military.doc.modules.standard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.standard.entity.Standard;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.mapper.StandardClauseMapper;
import com.military.doc.modules.standard.mapper.StandardMapper;
import com.military.doc.modules.standard.service.StandardParseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/standards")
@Tag(name = "标准库管理")
public class StandardController {

    @Autowired
    private StandardMapper standardMapper;

    @Autowired
    private StandardClauseMapper standardClauseMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StandardParseService standardParseService;

    // ---- Standard CRUD ----

    @PostMapping("/parse")
    @Operation(summary = "上传标准文件并自动解析元数据")
    public Result<Map<String, Object>> parseFile(@RequestParam("file") MultipartFile file) {
        StandardParseService.StandardParseResult result = standardParseService.parse(file);
        return Result.success(Map.of(
            "standard", result.standard(),
            "extractedText", result.extractedText()
        ));
    }

    @PostMapping("/batch-upload")
    @Operation(summary = "批量上传标准文件并自动解析")
    public Result<List<Standard>> batchUpload(@RequestParam("files") List<MultipartFile> files) {
        List<Standard> standards = files.stream().map(file -> {
            StandardParseService.StandardParseResult result = standardParseService.parse(file);
            Standard standard = result.standard();
            standardMapper.insert(standard);
            return standard;
        }).toList();
        return Result.success(standards);
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
        return Result.success(standardMapper.selectPage(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取标准详情")
    public Result<Standard> getById(@PathVariable Long id) {
        return Result.success(standardMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "创建标准")
    public Result<Standard> create(@RequestBody Standard standard) {
        if (standard.getStatus() == null) {
            standard.setStatus("ACTIVE");
        }
        standardMapper.insert(standard);
        return Result.success(standard);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标准")
    public Result<Standard> update(@PathVariable Long id, @RequestBody Standard standard) {
        standard.setId(id);
        standardMapper.updateById(standard);
        return Result.success(standardMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标准")
    public Result<Void> delete(@PathVariable Long id) {
        standardMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/upload")
    @Operation(summary = "上传标准文件")
    public Result<Standard> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String objectId = fileStorageService.upload(file);
        Standard standard = standardMapper.selectById(id);
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
        standardMapper.updateById(standard);
        return Result.success(standardMapper.selectById(id));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "获取标准文件下载地址")
    public Result<String> getDownloadUrl(@PathVariable Long id) {
        Standard standard = standardMapper.selectById(id);
        if (standard == null || standard.getFileObjectId() == null) {
            return Result.error("NOT_FOUND", "文件不存在");
        }
        return Result.success(fileStorageService.getAccessUrl(standard.getFileObjectId()));
    }

    @GetMapping("/types")
    @Operation(summary = "获取所有标准类型")
    public Result<List<String>> listTypes() {
        List<String> types = standardMapper.selectList(null).stream()
            .map(Standard::getStandardType)
            .distinct()
            .sorted()
            .toList();
        return Result.success(types);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取所有标准分类")
    public Result<List<String>> listCategories() {
        List<String> categories = standardMapper.selectList(null).stream()
            .map(Standard::getCategory)
            .distinct()
            .sorted()
            .toList();
        return Result.success(categories);
    }

    // ---- StandardClause CRUD ----

    @GetMapping("/{standardId}/clauses")
    @Operation(summary = "获取标准的条款列表")
    public Result<List<StandardClause>> listClauses(@PathVariable Long standardId) {
        List<StandardClause> clauses = standardClauseMapper.selectList(
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
        standardClauseMapper.insert(clause);
        return Result.success(clause);
    }

    @PutMapping("/{standardId}/clauses/{clauseId}")
    @Operation(summary = "更新条款")
    public Result<StandardClause> updateClause(@PathVariable Long standardId, @PathVariable Long clauseId, @RequestBody StandardClause clause) {
        clause.setId(clauseId);
        clause.setStandardId(standardId);
        standardClauseMapper.updateById(clause);
        return Result.success(standardClauseMapper.selectById(clauseId));
    }

    @DeleteMapping("/{standardId}/clauses/{clauseId}")
    @Operation(summary = "删除条款")
    public Result<Void> deleteClause(@PathVariable Long standardId, @PathVariable Long clauseId) {
        standardClauseMapper.deleteById(clauseId);
        return Result.success();
    }

    @GetMapping("/{standardId}/clauses/search")
    @Operation(summary = "搜索条款(按关键字)")
    public Result<List<StandardClause>> searchClauses(@PathVariable Long standardId, @RequestParam String keyword) {
        List<StandardClause> clauses = standardClauseMapper.selectList(
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
