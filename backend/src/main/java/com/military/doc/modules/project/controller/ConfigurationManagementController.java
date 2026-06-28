package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.common.result.Result;
import com.military.doc.common.security.ProjectAccessGuard;
import com.military.doc.modules.project.entity.*;
import com.military.doc.modules.project.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "技术状态管理")
public class ConfigurationManagementController {

    @Autowired
    private ConfigurationManagementService configService;

    @Autowired
    private ProjectAccessGuard accessGuard;

    @Autowired
    private ConfigurationBaselineService baselineService;

    @Autowired
    private ConfigurationBaselineItemService baselineItemService;

    @Autowired
    private ConfigurationChangeRequestService changeRequestService;

    @Autowired
    private ConfigurationStatusAccountingService accountingService;

    @Autowired
    private ConfigurationAuditService auditService;

    @Autowired
    private ConfigurationItemService ciService;

    // ---- 技术状态项 ----

    @GetMapping("/projects/{projectId}/configuration-items")
    @Operation(summary = "查询项目技术状态项")
    public Result<List<ConfigurationItem>> listCis(@PathVariable Long projectId,
                                                    @RequestParam(required = false) Long stageId) {
        var qw = new LambdaQueryWrapper<ConfigurationItem>()
                .eq(ConfigurationItem::getProjectId, projectId)
                .orderByAsc(ConfigurationItem::getCiCode);
        if (stageId != null) {
            qw.eq(ConfigurationItem::getStageId, stageId);
        }
        return Result.success(ciService.list(qw));
    }

    @PostMapping("/projects/{projectId}/configuration-items")
    @Operation(summary = "创建技术状态项")
    public Result<ConfigurationItem> createCi(@PathVariable Long projectId,
                                               @RequestBody ConfigurationItem ci,
                                               Authentication auth) {
        accessGuard.requireMember(projectId, auth);
        Long userId = (Long) auth.getPrincipal();
        ci.setProjectId(projectId);
        ci.setCreatedBy(userId);
        ci.setUpdatedBy(userId);
        ciService.save(ci);
        return Result.success(ci);
    }

    @PutMapping("/configuration-items/{id}")
    @Operation(summary = "更新技术状态项")
    public Result<ConfigurationItem> updateCi(@PathVariable Long id, @RequestBody ConfigurationItem ci, Authentication auth) {
        ConfigurationItem existing = ciService.getById(id);
        if (existing == null) throw BusinessException.notFound("技术状态项不存在: id=" + id);
        accessGuard.requireMember(existing.getProjectId(), auth);
        ci.setId(id);
        ci.setProjectId(existing.getProjectId());
        ciService.updateById(ci);
        return Result.success(ciService.getById(id));
    }

    // ---- 基线管理 ----

    @PostMapping("/projects/{projectId}/stages/{stageId}/baselines")
    @Operation(summary = "创建阶段基线")
    public Result<ConfigurationBaseline> createBaseline(@PathVariable Long projectId,
                                                         @PathVariable Long stageId,
                                                         @RequestParam String baselineType,
                                                         Authentication auth) {
        accessGuard.requireMember(projectId, auth);
        Long userId = (Long) auth.getPrincipal();
        return Result.success(configService.createBaseline(projectId, stageId, baselineType, userId));
    }

    @GetMapping("/projects/{projectId}/stages/{stageId}/baselines")
    @Operation(summary = "查询阶段基线列表")
    public Result<List<ConfigurationBaseline>> listBaselines(@PathVariable Long projectId,
                                                              @PathVariable Long stageId) {
        return Result.success(baselineService.list(new LambdaQueryWrapper<ConfigurationBaseline>()
                .eq(ConfigurationBaseline::getProjectId, projectId)
                .eq(ConfigurationBaseline::getStageId, stageId)
                .orderByDesc(ConfigurationBaseline::getCreatedAt)));
    }

    @GetMapping("/baselines/{id}")
    @Operation(summary = "获取基线详情")
    public Result<ConfigurationBaseline> getBaseline(@PathVariable Long id) {
        return Result.success(baselineService.getById(id));
    }

    @GetMapping("/baselines/{id}/items")
    @Operation(summary = "获取基线项列表")
    public Result<List<ConfigurationBaselineItem>> listBaselineItems(@PathVariable Long id) {
        return Result.success(baselineItemService.list(new LambdaQueryWrapper<ConfigurationBaselineItem>()
                .eq(ConfigurationBaselineItem::getBaselineId, id)));
    }

    @PutMapping("/baselines/{id}/approve")
    @Operation(summary = "批准基线")
    public Result<String> approveBaseline(@PathVariable Long id, Authentication auth) {
        ConfigurationBaseline baseline = baselineService.getById(id);
        if (baseline == null) throw BusinessException.notFound("基线不存在: id=" + id);
        accessGuard.requireMember(baseline.getProjectId(), auth);
        Long userId = (Long) auth.getPrincipal();
        configService.approveBaseline(id, userId);
        return Result.success("基线已批准");
    }

    @PutMapping("/baselines/{id}/effective")
    @Operation(summary = "设置基线为当前有效")
    public Result<String> setBaselineEffective(@PathVariable Long id, Authentication auth) {
        ConfigurationBaseline baseline = baselineService.getById(id);
        if (baseline == null) throw BusinessException.notFound("基线不存在: id=" + id);
        accessGuard.requireMember(baseline.getProjectId(), auth);
        Long userId = (Long) auth.getPrincipal();
        configService.setBaselineEffective(id, userId);
        return Result.success("基线已生效");
    }

    // ---- 技术状态更改控制 ----

    @PostMapping("/projects/{projectId}/change-requests")
    @Operation(summary = "创建技术状态更改申请")
    public Result<ConfigurationChangeRequest> createChangeRequest(@PathVariable Long projectId,
                                                                    @RequestBody ConfigurationChangeRequest request,
                                                                    Authentication auth) {
        accessGuard.requireMember(projectId, auth);
        Long userId = (Long) auth.getPrincipal();
        request.setProjectId(projectId);
        return Result.success(configService.createChangeRequest(request, userId));
    }

    @GetMapping("/projects/{projectId}/change-requests")
    @Operation(summary = "查询更改申请列表")
    public Result<List<ConfigurationChangeRequest>> listChangeRequests(@PathVariable Long projectId,
                                                                        @RequestParam(required = false) Long stageId) {
        var qw = new LambdaQueryWrapper<ConfigurationChangeRequest>()
                .eq(ConfigurationChangeRequest::getProjectId, projectId)
                .orderByDesc(ConfigurationChangeRequest::getCreatedAt);
        if (stageId != null) {
            qw.eq(ConfigurationChangeRequest::getStageId, stageId);
        }
        return Result.success(changeRequestService.list(qw));
    }

    @PutMapping("/change-requests/{id}/process")
    @Operation(summary = "处理更改申请")
    public Result<String> processChangeRequest(@PathVariable Long id,
                                                @RequestParam String action,
                                                Authentication auth) {
        ConfigurationChangeRequest request = changeRequestService.getById(id);
        if (request == null) throw BusinessException.notFound("更改申请不存在: id=" + id);
        accessGuard.requireMember(request.getProjectId(), auth);
        Long userId = (Long) auth.getPrincipal();
        configService.processChangeRequest(id, action, userId);
        return Result.success("操作成功");
    }

    // ---- 技术状态记实 ----

    @GetMapping("/projects/{projectId}/status-accounting")
    @Operation(summary = "查询技术状态记实")
    public Result<List<ConfigurationStatusAccounting>> listStatusAccounting(@PathVariable Long projectId,
                                                                             @RequestParam(required = false) Long stageId) {
        var qw = new LambdaQueryWrapper<ConfigurationStatusAccounting>()
                .eq(ConfigurationStatusAccounting::getProjectId, projectId)
                .orderByDesc(ConfigurationStatusAccounting::getEventTime);
        if (stageId != null) {
            qw.eq(ConfigurationStatusAccounting::getStageId, stageId);
        }
        return Result.success(accountingService.list(qw));
    }

    // ---- 技术状态审核 ----

    @PostMapping("/projects/{projectId}/stages/{stageId}/audits")
    @Operation(summary = "创建技术状态审核")
    public Result<ConfigurationAudit> createAudit(@PathVariable Long projectId,
                                                    @PathVariable Long stageId,
                                                    @RequestParam String auditType,
                                                    Authentication auth) {
        accessGuard.requireMember(projectId, auth);
        Long userId = (Long) auth.getPrincipal();
        return Result.success(configService.conductAudit(projectId, stageId, auditType, userId));
    }

    @GetMapping("/projects/{projectId}/audits")
    @Operation(summary = "查询技术状态审核")
    public Result<List<ConfigurationAudit>> listAudits(@PathVariable Long projectId,
                                                        @RequestParam(required = false) Long stageId) {
        var qw = new LambdaQueryWrapper<ConfigurationAudit>()
                .eq(ConfigurationAudit::getProjectId, projectId)
                .orderByDesc(ConfigurationAudit::getCreatedAt);
        if (stageId != null) {
            qw.eq(ConfigurationAudit::getStageId, stageId);
        }
        return Result.success(auditService.list(qw));
    }

    @PutMapping("/audits/{id}/complete")
    @Operation(summary = "完成审核")
    public Result<String> completeAudit(@PathVariable Long id,
                                         @RequestParam String auditResult,
                                         @RequestParam(required = false) String auditOpinion,
                                         Authentication auth) {
        ConfigurationAudit audit = auditService.getById(id);
        if (audit == null) throw BusinessException.notFound("审核记录不存在: id=" + id);
        accessGuard.requireMember(audit.getProjectId(), auth);
        Long userId = (Long) auth.getPrincipal();
        configService.completeAudit(id, auditResult, auditOpinion != null ? auditOpinion : "", userId);
        return Result.success("审核已完成");
    }
}
