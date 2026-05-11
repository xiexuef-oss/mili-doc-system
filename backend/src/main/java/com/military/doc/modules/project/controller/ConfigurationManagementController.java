package com.military.doc.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.result.Result;
import com.military.doc.modules.project.entity.*;
import com.military.doc.modules.project.mapper.*;
import com.military.doc.modules.project.service.ConfigurationManagementService;
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
    private ConfigurationBaselineMapper baselineMapper;

    @Autowired
    private ConfigurationBaselineItemMapper baselineItemMapper;

    @Autowired
    private ConfigurationChangeRequestMapper changeRequestMapper;

    @Autowired
    private ConfigurationStatusAccountingMapper accountingMapper;

    @Autowired
    private ConfigurationAuditMapper auditMapper;

    @Autowired
    private ConfigurationItemMapper ciMapper;

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
        return Result.success(ciMapper.selectList(qw));
    }

    @PostMapping("/projects/{projectId}/configuration-items")
    @Operation(summary = "创建技术状态项")
    public Result<ConfigurationItem> createCi(@PathVariable Long projectId,
                                               @RequestBody ConfigurationItem ci,
                                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ci.setProjectId(projectId);
        ci.setCreatedBy(userId);
        ci.setUpdatedBy(userId);
        ciMapper.insert(ci);
        return Result.success(ci);
    }

    @PutMapping("/configuration-items/{id}")
    @Operation(summary = "更新技术状态项")
    public Result<ConfigurationItem> updateCi(@PathVariable Long id, @RequestBody ConfigurationItem ci) {
        ci.setId(id);
        ciMapper.updateById(ci);
        return Result.success(ciMapper.selectById(id));
    }

    // ---- 基线管理 ----

    @PostMapping("/projects/{projectId}/stages/{stageId}/baselines")
    @Operation(summary = "创建阶段基线")
    public Result<ConfigurationBaseline> createBaseline(@PathVariable Long projectId,
                                                         @PathVariable Long stageId,
                                                         @RequestParam String baselineType,
                                                         Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.success(configService.createBaseline(projectId, stageId, baselineType, userId));
    }

    @GetMapping("/projects/{projectId}/stages/{stageId}/baselines")
    @Operation(summary = "查询阶段基线列表")
    public Result<List<ConfigurationBaseline>> listBaselines(@PathVariable Long projectId,
                                                              @PathVariable Long stageId) {
        return Result.success(baselineMapper.selectList(new LambdaQueryWrapper<ConfigurationBaseline>()
                .eq(ConfigurationBaseline::getProjectId, projectId)
                .eq(ConfigurationBaseline::getStageId, stageId)
                .orderByDesc(ConfigurationBaseline::getCreatedAt)));
    }

    @GetMapping("/baselines/{id}")
    @Operation(summary = "获取基线详情")
    public Result<ConfigurationBaseline> getBaseline(@PathVariable Long id) {
        return Result.success(baselineMapper.selectById(id));
    }

    @GetMapping("/baselines/{id}/items")
    @Operation(summary = "获取基线项列表")
    public Result<List<ConfigurationBaselineItem>> listBaselineItems(@PathVariable Long id) {
        return Result.success(baselineItemMapper.selectList(new LambdaQueryWrapper<ConfigurationBaselineItem>()
                .eq(ConfigurationBaselineItem::getBaselineId, id)));
    }

    @PutMapping("/baselines/{id}/approve")
    @Operation(summary = "批准基线")
    public Result<String> approveBaseline(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        configService.approveBaseline(id, userId);
        return Result.success("基线已批准");
    }

    @PutMapping("/baselines/{id}/effective")
    @Operation(summary = "设置基线为当前有效")
    public Result<String> setBaselineEffective(@PathVariable Long id, Authentication auth) {
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
        return Result.success(changeRequestMapper.selectList(qw));
    }

    @PutMapping("/change-requests/{id}/process")
    @Operation(summary = "处理更改申请")
    public Result<String> processChangeRequest(@PathVariable Long id,
                                                @RequestParam String action,
                                                Authentication auth) {
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
        return Result.success(accountingMapper.selectList(qw));
    }

    // ---- 技术状态审核 ----

    @PostMapping("/projects/{projectId}/stages/{stageId}/audits")
    @Operation(summary = "创建技术状态审核")
    public Result<ConfigurationAudit> createAudit(@PathVariable Long projectId,
                                                    @PathVariable Long stageId,
                                                    @RequestParam String auditType,
                                                    Authentication auth) {
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
        return Result.success(auditMapper.selectList(qw));
    }

    @PutMapping("/audits/{id}/complete")
    @Operation(summary = "完成审核")
    public Result<String> completeAudit(@PathVariable Long id,
                                         @RequestParam String auditResult,
                                         @RequestParam(required = false) String auditOpinion,
                                         Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        configService.completeAudit(id, auditResult, auditOpinion != null ? auditOpinion : "", userId);
        return Result.success("审核已完成");
    }
}
