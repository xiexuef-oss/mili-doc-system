package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.service.DocCatalogService;
import com.military.doc.modules.document.service.DocFileService;
import com.military.doc.modules.document.service.StageCatalogTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/doc-catalogs")
@Tag(name = "文档目录管理")
public class DocCatalogController {

    @Autowired
    private DocCatalogService docCatalogService;

    @Autowired
    private StageCatalogTemplateService stageCatalogTemplateService;

    @Autowired
    private DocFileService docFileService;

    private boolean hasDraft(Long catalogId) {
        Long count = docFileService.count(
            new LambdaQueryWrapper<DocFile>().eq(DocFile::getCatalogId, catalogId));
        return count != null && count > 0;
    }

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
    @Operation(summary = "更新文档目录（已生成初稿的条目不可修改名称）")
    public Result<DocCatalog> update(@PathVariable Long id, @RequestBody DocCatalog catalog, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        DocCatalog existing = docCatalogService.getById(id);
        if (existing == null) return Result.error("NOT_FOUND", "目录条目不存在");

        if (hasDraft(id)) {
            return Result.error("HAS_DRAFT", "该目录条目已生成文档初稿，不可修改。如需修改，请先删除关联的文档初稿");
        }

        catalog.setId(id);
        catalog.setUpdatedBy(userId);
        docCatalogService.updateById(catalog);
        return Result.success(docCatalogService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档目录条目（已生成初稿的条目不可删除）")
    public Result<Void> delete(@PathVariable Long id) {
        if (hasDraft(id)) {
            return Result.error("HAS_DRAFT", "该目录条目已生成文档初稿，不可删除。请先删除关联的文档初稿");
        }
        docCatalogService.removeById(id);
        return Result.success();
    }

    @GetMapping("/draft-status")
    @Operation(summary = "批量查询目录条目是否已有初稿，返回 Map<catalogId, hasDraft>")
    public Result<Map<Long, Boolean>> draftStatus(@RequestParam Long projectId) {
        List<DocCatalog> catalogs = docCatalogService.list(
            new LambdaQueryWrapper<DocCatalog>().eq(DocCatalog::getProjectId, projectId));

        List<Long> catalogIds = catalogs.stream().map(DocCatalog::getId).toList();
        Set<Long> draftCatalogIds = new HashSet<>();
        if (!catalogIds.isEmpty()) {
            draftCatalogIds = docFileService.list(new LambdaQueryWrapper<DocFile>()
                    .in(DocFile::getCatalogId, catalogIds)
                    .select(DocFile::getCatalogId))
                .stream().map(DocFile::getCatalogId).collect(java.util.stream.Collectors.toSet());
        }

        Map<Long, Boolean> status = new java.util.LinkedHashMap<>();
        for (DocCatalog c : catalogs) {
            status.put(c.getId(), draftCatalogIds.contains(c.getId()));
        }
        return Result.success(status);
    }

    @PostMapping("/generate-by-stage")
    @Operation(summary = "按 GJB 5882 阶段模板生成文档目录（规则化，非AI）")
    public Result<List<DocCatalog>> generateByStage(@RequestBody Map<String, Object> body,
                                                     Authentication authentication) {
        Long projectId = toLong(body.get("projectId"));
        Long stageId = toLong(body.get("stageId"));
        String stageCode = (String) body.get("stageCode");
        boolean overwrite = Boolean.TRUE.equals(body.get("overwrite"));
        Long userId = (Long) authentication.getPrincipal();

        if (projectId == null || stageId == null || stageCode == null || stageCode.isEmpty()) {
            return Result.error("PARAM_ERROR", "projectId, stageId and stageCode are required");
        }

        List<DocCatalog> catalogs = stageCatalogTemplateService.generateByStage(
            projectId, stageId, stageCode, userId, overwrite);
        return Result.success(catalogs);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        try { return Long.parseLong(value.toString()); }
        catch (NumberFormatException e) { return null; }
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
