package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocEffectiveBaseline;
import com.military.doc.modules.document.service.DocEffectiveBaselineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doc-baselines")
@Tag(name = "文档基线管理")
public class DocEffectiveBaselineController {

    @Autowired
    private DocEffectiveBaselineService baselineService;

    @PostMapping
    @Operation(summary = "创建基线记录")
    public Result<DocEffectiveBaseline> create(@RequestBody DocEffectiveBaseline baseline, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        baseline.setCreatedBy(userId);
        baseline.setUpdatedBy(userId);
        if (baseline.getBaselineStatus() == null) {
            baseline.setBaselineStatus("DRAFT");
        }
        baselineService.save(baseline);
        return Result.success(baseline);
    }

    @GetMapping
    @Operation(summary = "分页查询基线记录")
    public Result<Page<DocEffectiveBaseline>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long stageId) {
        LambdaQueryWrapper<DocEffectiveBaseline> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(DocEffectiveBaseline::getProjectId, projectId);
        }
        if (stageId != null) {
            wrapper.eq(DocEffectiveBaseline::getStageId, stageId);
        }
        wrapper.orderByDesc(DocEffectiveBaseline::getCreatedAt);
        return Result.success(baselineService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目的基线列表")
    public Result<List<DocEffectiveBaseline>> listByProject(@PathVariable Long projectId) {
        List<DocEffectiveBaseline> baselines = baselineService.list(
            new LambdaQueryWrapper<DocEffectiveBaseline>()
                .eq(DocEffectiveBaseline::getProjectId, projectId)
                .orderByAsc(DocEffectiveBaseline::getDocFileId)
        );
        return Result.success(baselines);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取基线详情")
    public Result<DocEffectiveBaseline> getById(@PathVariable Long id) {
        return Result.success(baselineService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新基线记录")
    public Result<DocEffectiveBaseline> update(@PathVariable Long id, @RequestBody DocEffectiveBaseline baseline, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        baseline.setId(id);
        baseline.setUpdatedBy(userId);
        baselineService.updateById(baseline);
        return Result.success(baselineService.getById(id));
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认基线生效")
    public Result<DocEffectiveBaseline> confirm(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        DocEffectiveBaseline baseline = baselineService.getById(id);
        if (baseline == null) {
            return Result.error("NOT_FOUND", "基线记录不存在");
        }
        baseline.setBaselineStatus("CONFIRMED");
        baseline.setConfirmedBy(userId);
        baseline.setConfirmedAt(LocalDateTime.now());
        baseline.setUpdatedBy(userId);
        baselineService.updateById(baseline);
        return Result.success(baseline);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除基线记录")
    public Result<Void> delete(@PathVariable Long id) {
        baselineService.removeById(id);
        return Result.success();
    }
}
