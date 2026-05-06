package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.service.DocCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/doc-catalogs")
@Tag(name = "文档目录管理")
public class DocCatalogController {

    @Autowired
    private DocCatalogService docCatalogService;

    @PostMapping
    @Operation(summary = "创建文档目录条目")
    public Result<DocCatalog> create(@RequestBody DocCatalog catalog, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        catalog.setCreatedBy(userId);
        catalog.setUpdatedBy(userId);
        if (catalog.getStatus() == null) {
            catalog.setStatus("DRAFT");
        }
        docCatalogService.save(catalog);
        return Result.success(catalog);
    }

    @GetMapping
    @Operation(summary = "分页查询文档目录")
    public Result<Page<DocCatalog>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long stageId) {
        LambdaQueryWrapper<DocCatalog> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(DocCatalog::getProjectId, projectId);
        }
        if (stageId != null) {
            wrapper.eq(DocCatalog::getStageId, stageId);
        }
        wrapper.orderByAsc(DocCatalog::getDocCode);
        return Result.success(docCatalogService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目的完整文档目录列表")
    public Result<List<DocCatalog>> listByProject(@PathVariable Long projectId) {
        List<DocCatalog> list = docCatalogService.list(
            new LambdaQueryWrapper<DocCatalog>()
                .eq(DocCatalog::getProjectId, projectId)
                .orderByAsc(DocCatalog::getDocCode)
        );
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文档目录详情")
    public Result<DocCatalog> getById(@PathVariable Long id) {
        return Result.success(docCatalogService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文档目录")
    public Result<DocCatalog> update(@PathVariable Long id, @RequestBody DocCatalog catalog, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        catalog.setId(id);
        catalog.setUpdatedBy(userId);
        docCatalogService.updateById(catalog);
        return Result.success(docCatalogService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档目录条目")
    public Result<Void> delete(@PathVariable Long id) {
        docCatalogService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "下发文档目录（DRAFT → ISSUED）")
    public Result<DocCatalog> issue(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        DocCatalog catalog = docCatalogService.getById(id);
        if (catalog == null) return Result.error("NOT_FOUND", "目录条目不存在");
        if (!"DRAFT".equals(catalog.getStatus())) {
            return Result.error("STATUS_ERROR", "仅草稿状态的目录可下发");
        }
        catalog.setStatus("ISSUED");
        catalog.setUpdatedBy(userId);
        docCatalogService.updateById(catalog);
        return Result.success(catalog);
    }

    @PostMapping("/{id}/change")
    @Operation(summary = "变更已下发目录（ISSUED → CHANGED），需填写变更理由")
    public Result<DocCatalog> change(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        DocCatalog catalog = docCatalogService.getById(id);
        if (catalog == null) return Result.error("NOT_FOUND", "目录条目不存在");
        if (!"ISSUED".equals(catalog.getStatus())) {
            return Result.error("STATUS_ERROR", "仅已下发状态的目录可变更");
        }
        String reason = body.get("changeReason");
        if (reason == null || reason.isBlank()) {
            return Result.error("PARAM_ERROR", "变更理由不能为空");
        }
        catalog.setStatus("CHANGED");
        catalog.setChangeReason(reason);
        catalog.setUpdatedBy(userId);
        docCatalogService.updateById(catalog);
        return Result.success(catalog);
    }
}
