package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.project.entity.StageTransitionCheck;
import com.military.doc.modules.project.mapper.StageTransitionCheckMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stage-transitions")
@Tag(name = "阶段转阶段检查")
public class StageTransitionCheckController {

    @Autowired
    private StageTransitionCheckMapper checkMapper;

    @PostMapping
    @Operation(summary = "创建转阶段检查")
    public Result<StageTransitionCheck> create(@RequestBody StageTransitionCheck check, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        check.setCreatedBy(userId);
        check.setUpdatedBy(userId);
        if (check.getCheckStatus() == null) check.setCheckStatus("PENDING");
        checkMapper.insert(check);
        return Result.success(check);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "获取项目的所有转阶段检查")
    public Result<List<StageTransitionCheck>> listByProject(@PathVariable Long projectId) {
        List<StageTransitionCheck> checks = checkMapper.selectList(
            new LambdaQueryWrapper<StageTransitionCheck>()
                .eq(StageTransitionCheck::getProjectId, projectId)
                .orderByDesc(StageTransitionCheck::getCreatedAt)
        );
        return Result.success(checks);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新检查结果")
    public Result<StageTransitionCheck> update(@PathVariable Long id, @RequestBody StageTransitionCheck check, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        check.setId(id);
        check.setUpdatedBy(userId);
        if (check.getCheckStatus() != null && check.getCheckedBy() == null) {
            check.setCheckedBy(userId);
            check.setCheckedAt(LocalDateTime.now());
        }
        checkMapper.updateById(check);
        return Result.success(checkMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除转阶段检查")
    public Result<Void> delete(@PathVariable Long id) {
        checkMapper.deleteById(id);
        return Result.success();
    }
}
