package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.common.result.Result;
import com.military.doc.common.security.ProjectAccessGuard;
import com.military.doc.modules.project.entity.ProjectMember;
import com.military.doc.modules.project.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/project-members")
@Tag(name = "项目成员管理")
public class ProjectMemberController {

    @Autowired
    private ProjectMemberService projectMemberService;
    @Autowired
    private ProjectAccessGuard accessGuard;

    @PostMapping
    @Operation(summary = "添加项目成员")
    public Result<ProjectMember> create(@RequestBody ProjectMember member, Authentication authentication) {
        accessGuard.requireMember(member.getProjectId(), authentication);
        Long userId = (Long) authentication.getPrincipal();
        member.setCreatedBy(userId);
        member.setUpdatedBy(userId);
        if (member.getStatus() == null) member.setStatus("ACTIVE");
        projectMemberService.save(member);
        return Result.success(member);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目成员列表")
    public Result<List<ProjectMember>> listByProject(@PathVariable Long projectId) {
        List<ProjectMember> members = projectMemberService.list(
            new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getCreatedAt)
        );
        return Result.success(members);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新成员信息")
    public Result<ProjectMember> update(@PathVariable Long id, @RequestBody ProjectMember member, Authentication authentication) {
        ProjectMember existing = projectMemberService.getById(id);
        if (existing == null) throw BusinessException.notFound("项目成员不存在: id=" + id);
        accessGuard.requireMember(existing.getProjectId(), authentication);
        Long userId = (Long) authentication.getPrincipal();
        member.setId(id);
        member.setProjectId(existing.getProjectId());
        member.setUpdatedBy(userId);
        projectMemberService.updateById(member);
        return Result.success(projectMemberService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "移除项目成员")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        ProjectMember existing = projectMemberService.getById(id);
        if (existing == null) throw BusinessException.notFound("项目成员不存在: id=" + id);
        accessGuard.requireMember(existing.getProjectId(), authentication);
        projectMemberService.removeById(id);
        return Result.success();
    }
}
