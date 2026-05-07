package com.military.doc.modules.knowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.knowledge.entity.KnowledgeBase;
import com.military.doc.modules.knowledge.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@Tag(name = "知识库管理")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private FileStorageService fileStorageService;

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

    private String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
