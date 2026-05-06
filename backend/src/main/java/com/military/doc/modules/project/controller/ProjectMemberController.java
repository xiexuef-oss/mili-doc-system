package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.project.entity.ProjectMember;
import com.military.doc.modules.project.mapper.ProjectMemberMapper;
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
    private ProjectMemberMapper projectMemberMapper;

    @PostMapping
    @Operation(summary = "添加项目成员")
    public Result<ProjectMember> create(@RequestBody ProjectMember member, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        member.setCreatedBy(userId);
        member.setUpdatedBy(userId);
        if (member.getStatus() == null) member.setStatus("ACTIVE");
        projectMemberMapper.insert(member);
        return Result.success(member);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目成员列表")
    public Result<List<ProjectMember>> listByProject(@PathVariable Long projectId) {
        List<ProjectMember> members = projectMemberMapper.selectList(
            new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getCreatedAt)
        );
        return Result.success(members);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新成员信息")
    public Result<ProjectMember> update(@PathVariable Long id, @RequestBody ProjectMember member, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        member.setId(id);
        member.setUpdatedBy(userId);
        projectMemberMapper.updateById(member);
        return Result.success(projectMemberMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "移除项目成员")
    public Result<Void> delete(@PathVariable Long id) {
        projectMemberMapper.deleteById(id);
        return Result.success();
    }
}
