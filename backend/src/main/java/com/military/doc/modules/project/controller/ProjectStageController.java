package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.service.ProjectStageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stages")
@Tag(name = "阶段管理")
public class ProjectStageController {

    @Autowired
    private ProjectStageService stageService;

    @PostMapping
    @Operation(summary = "创建阶段")
    public Result<ProjectStage> create(@RequestBody ProjectStage stage, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        stage.setCreatedBy(userId);
        stage.setUpdatedBy(userId);
        stage.setStatus("NOT_STARTED");
        stageService.save(stage);
        return Result.success(stage);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目阶段列表")
    public Result<List<ProjectStage>> listByProject(@PathVariable Long projectId) {
        List<ProjectStage> stages = stageService.list(
            new LambdaQueryWrapper<ProjectStage>()
                .eq(ProjectStage::getProjectId, projectId)
                .orderByAsc(ProjectStage::getStageOrder)
        );
        return Result.success(stages);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新阶段")
    public Result<ProjectStage> update(@PathVariable Long id, @RequestBody ProjectStage stage) {
        stage.setId(id);
        stageService.updateById(stage);
        return Result.success(stageService.getById(id));
    }
}