package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.service.DocFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/doc-files")
@Tag(name = "文档文件管理")
public class DocFileController {

    @Autowired
    private DocFileService docFileService;

    @PostMapping
    @Operation(summary = "创建文档文件")
    public Result<DocFile> create(@RequestBody DocFile docFile, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        docFile.setCreatedBy(userId);
        docFile.setUpdatedBy(userId);
        if (docFile.getStatus() == null) {
            docFile.setStatus("DRAFT");
        }
        docFileService.save(docFile);
        return Result.success(docFile);
    }

    @GetMapping
    @Operation(summary = "分页查询文档文件")
    public Result<Page<DocFile>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<DocFile> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(DocFile::getProjectId, projectId);
        }
        if (catalogId != null) {
            wrapper.eq(DocFile::getCatalogId, catalogId);
        }
        if (status != null) {
            wrapper.eq(DocFile::getStatus, status);
        }
        wrapper.orderByDesc(DocFile::getCreatedAt);
        return Result.success(docFileService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文档文件详情")
    public Result<DocFile> getById(@PathVariable Long id) {
        return Result.success(docFileService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文档文件")
    public Result<DocFile> update(@PathVariable Long id, @RequestBody DocFile docFile, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        docFile.setId(id);
        docFile.setUpdatedBy(userId);
        docFileService.updateById(docFile);
        return Result.success(docFileService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档文件")
    public Result<Void> delete(@PathVariable Long id) {
        docFileService.removeById(id);
        return Result.success();
    }
}
