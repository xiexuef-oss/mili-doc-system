package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.project.entity.StageTransitionCheck;
import com.military.doc.modules.project.mapper.StageTransitionCheckMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stage-transitions")
@Tag(name = "阶段转阶段检查")
public class StageTransitionCheckController {

    @Autowired
    private StageTransitionCheckMapper checkMapper;

    @Autowired
    private DocLedgerService docLedgerService;

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

    @PostMapping("/check-documents")
    @Operation(summary = "检查当前阶段文档是否全部归档")
    public Result<Map<String, Object>> checkDocuments(@RequestBody DocumentCheckRequest request) {
        List<DocLedger> unreleased = docLedgerService.findUnreleasedByStage(
            request.getProjectId(), request.getFromStageId());

        boolean passed = unreleased.isEmpty();
        long totalRequired = docLedgerService.listByProject(
            request.getProjectId(), request.getFromStageId(), null).stream()
            .filter(d -> d.getRequiredFlag() == null || d.getRequiredFlag())
            .count();

        List<Map<String, Object>> blockers = unreleased.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("docCode", d.getDocCode());
            m.put("docName", d.getDocName());
            m.put("lifecycleStatus", d.getLifecycleStatus());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", passed);
        result.put("totalRequired", totalRequired);
        result.put("unreleasedCount", unreleased.size());
        result.put("blockers", blockers);
        result.put("checkResult", passed ? "文档齐套检查通过" : "存在未归档文档");
        return Result.success(result);
    }

    public static class DocumentCheckRequest {
        private Long projectId;
        private Long fromStageId;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public Long getFromStageId() { return fromStageId; }
        public void setFromStageId(Long fromStageId) { this.fromStageId = fromStageId; }
    }
}
