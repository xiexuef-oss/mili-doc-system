package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocChangeImpact;
import com.military.doc.modules.document.service.DocChangeImpactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/doc-change-impacts")
@Tag(name = "文档变更影响管理")
public class DocChangeImpactController {

    @Autowired
    private DocChangeImpactService impactService;

    @PostMapping
    @Operation(summary = "创建变更影响记录")
    public Result<DocChangeImpact> create(@RequestBody DocChangeImpact impact, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        impact.setCreatedBy(userId);
        impact.setUpdatedBy(userId);
        if (impact.getStatus() == null) {
            impact.setStatus("PENDING");
        }
        impactService.save(impact);
        return Result.success(impact);
    }

    @GetMapping
    @Operation(summary = "分页查询变更影响记录")
    public Result<Page<DocChangeImpact>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long changeEventId,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<DocChangeImpact> wrapper = new LambdaQueryWrapper<>();
        if (changeEventId != null) {
            wrapper.eq(DocChangeImpact::getChangeEventId, changeEventId);
        }
        if (status != null) {
            wrapper.eq(DocChangeImpact::getStatus, status);
        }
        wrapper.orderByDesc(DocChangeImpact::getCreatedAt);
        return Result.success(impactService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取变更影响详情")
    public Result<DocChangeImpact> getById(@PathVariable Long id) {
        return Result.success(impactService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新变更影响记录")
    public Result<DocChangeImpact> update(@PathVariable Long id, @RequestBody DocChangeImpact impact, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        impact.setId(id);
        impact.setUpdatedBy(userId);
        impactService.updateById(impact);
        return Result.success(impactService.getById(id));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "关闭变更影响")
    public Result<DocChangeImpact> close(@PathVariable Long id, Authentication authentication) {
        DocChangeImpact impact = impactService.getById(id);
        if (impact == null) {
            return Result.error("NOT_FOUND", "变更影响记录不存在");
        }
        impact.setStatus("CLOSED");
        impact.setClosedAt(LocalDateTime.now());
        impact.setUpdatedBy((Long) authentication.getPrincipal());
        impactService.updateById(impact);
        return Result.success(impact);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除变更影响记录")
    public Result<Void> delete(@PathVariable Long id) {
        impactService.removeById(id);
        return Result.success();
    }
}
