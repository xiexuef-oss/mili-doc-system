package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "项目管理")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @PostMapping
    @Operation(summary = "创建项目")
    public Result<Project> create(@RequestBody Project project, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        project.setCreatedBy(userId);
        project.setUpdatedBy(userId);
        project.setStatus("DRAFT");
        projectService.save(project);
        return Result.success(project);
    }

    @GetMapping
    @Operation(summary = "分页查询项目")
    public Result<Page<Project>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null) {
            wrapper.like(Project::getProjectName, keyword);
        }
        if (status != null) {
            wrapper.eq(Project::getStatus, status);
        }
        wrapper.orderByDesc(Project::getCreatedAt);
        return Result.success(projectService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取项目详情")
    public Result<Project> getById(@PathVariable Long id) {
        return Result.success(projectService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目")
    public Result<Project> update(@PathVariable Long id, @RequestBody Project project, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        project.setId(id);
        project.setUpdatedBy(userId);
        projectService.updateById(project);
        return Result.success(projectService.getById(id));
    }

    @PutMapping("/{id}/current-stage")
    @Operation(summary = "更新项目当前阶段")
    public Result<Project> updateCurrentStage(@PathVariable Long id, @RequestBody StageUpdateRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Project project = projectService.getById(id);
        if (project == null) {
            return Result.error("NOT_FOUND", "项目不存在");
        }
        project.setCurrentStageId(request.getStageId());
        project.setUpdatedBy(userId);
        projectService.updateById(project);
        return Result.success(projectService.getById(id));
    }

    public static class StageUpdateRequest {
        private Long stageId;
        public Long getStageId() { return stageId; }
        public void setStageId(Long stageId) { this.stageId = stageId; }
    }
}