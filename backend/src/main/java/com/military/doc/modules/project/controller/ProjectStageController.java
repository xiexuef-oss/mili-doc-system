package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.project.constant.StageDefinition;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.service.ProjectStageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "阶段管理")
public class ProjectStageController {

    @Autowired
    private ProjectStageService stageService;

    @GetMapping("/stages/definitions")
    @Operation(summary = "获取预定义阶段列表（7个固定阶段）")
    public Result<List<StageDefinition>> definitions() {
        return Result.success(StageDefinition.ALL);
    }

    @PostMapping("/projects/{projectId}/stages/initialize")
    @Operation(summary = "初始化项目阶段（选择初始阶段，自动包含后续所有阶段）")
    public Result<List<ProjectStage>> initialize(@PathVariable Long projectId,
                                                  @RequestBody Map<String, Object> body,
                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String initialStageCode = (String) body.get("initialStageCode");
        if (initialStageCode == null || initialStageCode.isEmpty()) {
            return Result.error("PARAM_ERROR", "initialStageCode is required");
        }
        return Result.success(stageService.initializeProjectStages(projectId, initialStageCode, userId));
    }

    @GetMapping("/projects/{projectId}/stages")
    @Operation(summary = "获取项目阶段列表")
    public Result<List<ProjectStage>> listByProject(@PathVariable Long projectId) {
        List<ProjectStage> stages = stageService.list(
            new LambdaQueryWrapper<ProjectStage>()
                .eq(ProjectStage::getProjectId, projectId)
                .orderByAsc(ProjectStage::getStageOrder)
        );
        return Result.success(stages);
    }

    @PutMapping("/projects/{projectId}/stages/{stageId}")
    @Operation(summary = "更新阶段")
    public Result<ProjectStage> update(@PathVariable Long projectId, @PathVariable Long stageId, @RequestBody ProjectStage stage) {
        stage.setId(stageId);
        stage.setProjectId(projectId);
        stageService.updateById(stage);
        return Result.success(stageService.getById(stageId));
    }

    @PostMapping("/projects/{projectId}/stages/{stageId}/request-transition")
    @Operation(summary = "申请转阶段（执行准入检查，通过后自动完成当前阶段并启动下一阶段）")
    public Result<Map<String, Object>> requestTransition(@PathVariable Long projectId,
                                                          @PathVariable Long stageId,
                                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(stageService.requestTransition(projectId, stageId, userId));
    }

    @PostMapping("/projects/{projectId}/stages/{stageId}/suspend")
    @Operation(summary = "暂停阶段")
    public Result<String> suspend(@PathVariable Long projectId, @PathVariable Long stageId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        stageService.suspendStage(projectId, stageId, userId);
        return Result.success("阶段已暂停");
    }

    @PostMapping("/projects/{projectId}/stages/{stageId}/terminate")
    @Operation(summary = "终止阶段")
    public Result<String> terminate(@PathVariable Long projectId, @PathVariable Long stageId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        stageService.terminateStage(projectId, stageId, userId);
        return Result.success("阶段已终止");
    }

    @GetMapping("/projects/{projectId}/stages/{stageId}/workbench")
    @Operation(summary = "阶段工作台数据")
    public Result<Map<String, Object>> workbench(@PathVariable Long projectId, @PathVariable Long stageId) {
        return Result.success(stageService.getStageWorkbench(projectId, stageId));
    }

    @PostMapping("/projects/{projectId}/stages/{stageId}/gate-check")
    @Operation(summary = "执行转阶段准入检查")
    public Result<Map<String, Object>> gateCheck(@PathVariable Long projectId, @PathVariable Long stageId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(stageService.gateCheck(projectId, stageId, userId));
    }
}
