package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.common.security.ProjectAccessGuard;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.service.DocVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doc-versions")
@Tag(name = "文档版本管理")
public class DocVersionController {

    @Autowired
    private DocVersionService docVersionService;
    @Autowired
    private ProjectAccessGuard accessGuard;

    @PostMapping
    @Operation(summary = "创建文档版本")
    public Result<DocVersion> create(@RequestBody DocVersion version, Authentication authentication) {
        accessGuard.requireMemberForFile(version.getDocFileId(), authentication);
        Long userId = (Long) authentication.getPrincipal();
        version.setCreatedBy(userId);
        version.setUpdatedBy(userId);
        if (version.getVersionStatus() == null) {
            version.setVersionStatus("DRAFT");
        }
        docVersionService.save(version);
        return Result.success(version);
    }

    @GetMapping
    @Operation(summary = "分页查询文档版本")
    public Result<Page<DocVersion>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long docFileId) {
        LambdaQueryWrapper<DocVersion> wrapper = new LambdaQueryWrapper<>();
        if (docFileId != null) {
            wrapper.eq(DocVersion::getDocFileId, docFileId);
        }
        wrapper.orderByDesc(DocVersion::getCreatedAt);
        return Result.success(docVersionService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/doc-file/{docFileId}")
    @Operation(summary = "获取文档的所有版本历史")
    public Result<List<DocVersion>> listByDocFile(@PathVariable Long docFileId) {
        List<DocVersion> versions = docVersionService.list(
            new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocFileId, docFileId)
                .orderByDesc(DocVersion::getCreatedAt)
        );
        return Result.success(versions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取版本详情")
    public Result<DocVersion> getById(@PathVariable Long id) {
        return Result.success(docVersionService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新版本信息")
    public Result<DocVersion> update(@PathVariable Long id, @RequestBody DocVersion version, Authentication authentication) {
        DocVersion existing = docVersionService.getById(id);
        if (existing == null) throw com.military.doc.common.exception.BusinessException.notFound("文档版本不存在: id=" + id);
        accessGuard.requireMemberForFile(existing.getDocFileId(), authentication);
        Long userId = (Long) authentication.getPrincipal();
        version.setId(id);
        version.setDocFileId(existing.getDocFileId());
        version.setUpdatedBy(userId);
        docVersionService.updateById(version);
        return Result.success(docVersionService.getById(id));
    }
}
