package com.military.doc.modules.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.military.doc.common.result.Result;
import com.military.doc.common.security.ProjectAccessGuard;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.DocLedgerLog;
import com.military.doc.modules.document.service.DocLedgerLogService;
import com.military.doc.modules.document.service.DocLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/doc-ledgers")
@Tag(name = "文档台账")
public class DocLedgerController {

    @Autowired
    private DocLedgerService docLedgerService;

    @Autowired
    private DocLedgerLogService logService;

    @Autowired
    private ProjectAccessGuard accessGuard;

    @PostMapping
    @Operation(summary = "创建台账条目")
    public Result<DocLedger> create(@RequestBody DocLedger ledger, Authentication authentication) {
        accessGuard.requireMember(ledger.getProjectId(), authentication);
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(docLedgerService.createLedger(ledger, userId));
    }

    @GetMapping
    @Operation(summary = "分页查询台账")
    public Result<Page<DocLedger>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam Long projectId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String lifecycleStatus) {
        LambdaQueryWrapper<DocLedger> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocLedger::getProjectId, projectId);
        if (stageId != null) {
            wrapper.eq(DocLedger::getStageId, stageId);
        }
        if (lifecycleStatus != null && !lifecycleStatus.isBlank()) {
            wrapper.eq(DocLedger::getLifecycleStatus, lifecycleStatus);
        }
        wrapper.orderByAsc(DocLedger::getDocCode);
        return Result.success(docLedgerService.page(new Page<>(pageNo, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取台账详情")
    public Result<DocLedger> getById(@PathVariable Long id) {
        return Result.success(docLedgerService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新台账元数据")
    public Result<DocLedger> update(@PathVariable Long id, @RequestBody DocLedger ledger, Authentication authentication) {
        accessGuard.requireMemberForLedger(id, authentication);
        Long userId = (Long) authentication.getPrincipal();
        ledger.setId(id);
        ledger.setUpdatedBy(userId);
        docLedgerService.updateById(ledger);
        return Result.success(docLedgerService.getById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "状态转移")
    public Result<DocLedger> transitionStatus(
            @PathVariable Long id,
            @RequestBody StatusTransitionRequest request,
            Authentication authentication) {
        accessGuard.requireMemberForLedger(id, authentication);
        Long userId = (Long) authentication.getPrincipal();
        docLedgerService.transitionStatus(id, request.getTargetStatus(), userId, request.getRemark());
        return Result.success(docLedgerService.getById(id));
    }

    @GetMapping("/kanban")
    @Operation(summary = "看板数据（按状态分组）")
    public Result<Map<String, List<DocLedger>>> kanban(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long stageId) {
        List<DocLedger> all = docLedgerService.listByProject(projectId, stageId, null);

        String[] statuses = {"PLANNED", "DRAFTING", "CHECKING", "REVIEWING", "APPROVING", "RELEASED"};
        Map<String, List<DocLedger>> kanban = new LinkedHashMap<>();
        for (String status : statuses) {
            kanban.put(status, new ArrayList<>());
        }

        for (DocLedger doc : all) {
            List<DocLedger> bucket = kanban.get(doc.getLifecycleStatus());
            if (bucket != null) {
                bucket.add(doc);
            }
        }
        return Result.success(kanban);
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "操作日志")
    public Result<List<DocLedgerLog>> logs(@PathVariable Long id) {
        List<DocLedgerLog> logs = logService.list(
            new LambdaQueryWrapper<DocLedgerLog>()
                .eq(DocLedgerLog::getDocLedgerId, id)
                .orderByDesc(DocLedgerLog::getOperatedAt));
        return Result.success(logs);
    }

    @PostMapping("/sync-from-catalog")
    @Operation(summary = "从文档目录同步创建台账条目")
    public Result<Map<String, Object>> syncFromCatalog(
            @RequestParam Long projectId,
            @RequestParam Long stageId,
            Authentication authentication) {
        accessGuard.requireMember(projectId, authentication);
        Long userId = (Long) authentication.getPrincipal();
        int created = docLedgerService.syncFromCatalog(projectId, stageId, userId);
        return Result.success(Map.of("syncedCount", created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "级联删除文档台账及其关联数据")
    public Result<?> delete(@PathVariable Long id, Authentication authentication) {
        accessGuard.requireMemberForLedger(id, authentication);
        docLedgerService.deleteLedger(id);
        return Result.success();
    }

    public static class StatusTransitionRequest {
        private String targetStatus;
        private String remark;

        public String getTargetStatus() { return targetStatus; }
        public void setTargetStatus(String targetStatus) { this.targetStatus = targetStatus; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
