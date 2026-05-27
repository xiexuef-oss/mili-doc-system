package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.project.entity.*;
import com.military.doc.modules.project.mapper.*;
import com.military.doc.modules.project.service.ConfigurationManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConfigurationManagementServiceImpl implements ConfigurationManagementService {

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
    private DocLedgerMapper docLedgerMapper;

    @Override
    @Transactional
    public ConfigurationBaseline createBaseline(Long projectId, Long stageId, String baselineType, Long operatorId) {
        ConfigurationBaseline baseline = new ConfigurationBaseline();
        baseline.setProjectId(projectId);
        baseline.setStageId(stageId);
        baseline.setBaselineCode("BL-" + projectId + "-" + stageId + "-" + System.currentTimeMillis() % 100000);
        baseline.setBaselineName(baselineType + " 基线");
        baseline.setBaselineType(baselineType);
        baseline.setBaselineVersion("V1.0");
        baseline.setBaselineStatus("DRAFT");
        baseline.setCreatedBy(operatorId);
        baseline.setUpdatedBy(operatorId);
        baselineMapper.insert(baseline);

        // auto-collect released/archived documents for this stage as baseline items
        var docs = docLedgerMapper.selectList(new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId)
                .in(DocLedger::getLifecycleStatus, "RELEASED", "ARCHIVED"));
        List<ConfigurationBaselineItem> items = new java.util.ArrayList<>();
        for (DocLedger doc : docs) {
            ConfigurationBaselineItem item = new ConfigurationBaselineItem();
            item.setBaselineId(baseline.getId());
            item.setItemType("DOCUMENT");
            item.setItemId(doc.getId());
            item.setItemCode(doc.getDocCode());
            item.setItemName(doc.getDocName());
            item.setCreatedBy(operatorId);
            item.setUpdatedBy(operatorId);
            items.add(item);
        }
        for (ConfigurationBaselineItem item : items) {
            baselineItemMapper.insert(item);
        }

        recordEvent(projectId, stageId, null, "BASELINE_CREATE",
                "创建基线: " + baseline.getBaselineName(), "CONFIGURATION_BASELINE", baseline.getId(), operatorId);

        return baseline;
    }

    @Override
    @Transactional
    public void approveBaseline(Long baselineId, Long operatorId) {
        ConfigurationBaseline baseline = baselineMapper.selectById(baselineId);
        if (baseline == null) {
            throw BusinessException.notFound("基线不存在: " + baselineId);
        }
        if (!"DRAFT".equals(baseline.getBaselineStatus()) && !"REVIEWING".equals(baseline.getBaselineStatus())) {
            throw BusinessException.validation("只有草稿或评审中的基线才能批准");
        }
        baseline.setBaselineStatus("APPROVED");
        baseline.setApproveUserId(operatorId);
        baseline.setApproveTime(LocalDateTime.now());
        baseline.setUpdatedBy(operatorId);
        baselineMapper.updateById(baseline);

        recordEvent(baseline.getProjectId(), baseline.getStageId(), null, "BASELINE_APPROVE",
                "批准基线: " + baseline.getBaselineName(), "CONFIGURATION_BASELINE", baselineId, operatorId);
    }

    @Override
    @Transactional
    public void setBaselineEffective(Long baselineId, Long operatorId) {
        ConfigurationBaseline baseline = baselineMapper.selectById(baselineId);
        if (baseline == null) {
            throw BusinessException.notFound("基线不存在: " + baselineId);
        }
        if (!"APPROVED".equals(baseline.getBaselineStatus())) {
            throw BusinessException.validation("只有已批准的基线才能生效");
        }

        // supersede any currently effective baseline for this stage+type
        ConfigurationBaseline supersedeUpdate = new ConfigurationBaseline();
        supersedeUpdate.setBaselineStatus("SUPERSEDED");
        supersedeUpdate.setUpdatedBy(operatorId);
        baselineMapper.update(supersedeUpdate, new LambdaUpdateWrapper<ConfigurationBaseline>()
                .eq(ConfigurationBaseline::getProjectId, baseline.getProjectId())
                .eq(ConfigurationBaseline::getStageId, baseline.getStageId())
                .eq(ConfigurationBaseline::getBaselineType, baseline.getBaselineType())
                .eq(ConfigurationBaseline::getBaselineStatus, "EFFECTIVE"));

        baseline.setBaselineStatus("EFFECTIVE");
        baseline.setEffectiveTime(LocalDateTime.now());
        baseline.setUpdatedBy(operatorId);
        baselineMapper.updateById(baseline);

        recordEvent(baseline.getProjectId(), baseline.getStageId(), null, "BASELINE_EFFECTIVE",
                "基线生效: " + baseline.getBaselineName(), "CONFIGURATION_BASELINE", baselineId, operatorId);
    }

    @Override
    @Transactional
    public ConfigurationChangeRequest createChangeRequest(ConfigurationChangeRequest request, Long operatorId) {
        request.setChangeCode("CR-" + request.getProjectId() + "-" + System.currentTimeMillis() % 100000);
        request.setStatus("SUBMITTED");
        request.setCreatedBy(operatorId);
        request.setUpdatedBy(operatorId);
        changeRequestMapper.insert(request);

        recordEvent(request.getProjectId(), request.getStageId(), null, "CHANGE_SUBMIT",
                "提交更改申请: " + request.getChangeTitle(), "CONFIGURATION_CHANGE_REQUEST", request.getId(), operatorId);

        return request;
    }

    @Override
    @Transactional
    public void processChangeRequest(Long id, String action, Long operatorId) {
        ConfigurationChangeRequest cr = changeRequestMapper.selectById(id);
        if (cr == null) {
            throw BusinessException.notFound("更改单不存在: " + id);
        }

        String currentStatus = cr.getStatus();
        String newStatus = switch (action) {
            case "analyze" -> "ANALYZING";
            case "review" -> "REVIEWING";
            case "approve" -> "APPROVED";
            case "reject" -> "REJECTED";
            case "implement" -> "IMPLEMENTED";
            case "close" -> "CLOSED";
            default -> throw BusinessException.validation("不支持的操作: " + action);
        };

        // validate transition
        boolean valid = switch (currentStatus) {
            case "SUBMITTED" -> "ANALYZING".equals(newStatus);
            case "ANALYZING" -> "REVIEWING".equals(newStatus) || "REJECTED".equals(newStatus);
            case "REVIEWING" -> "APPROVED".equals(newStatus) || "REJECTED".equals(newStatus);
            case "APPROVED" -> "IMPLEMENTED".equals(newStatus);
            case "IMPLEMENTED" -> "CLOSED".equals(newStatus);
            default -> false;
        };
        if (!valid) {
            throw BusinessException.validation(
                    String.format("不允许从 %s 执行 %s 操作", currentStatus, action));
        }

        cr.setStatus(newStatus);
        if ("APPROVED".equals(newStatus) || "REJECTED".equals(newStatus)) {
            cr.setApproveResult(newStatus);
            cr.setApproveTime(LocalDateTime.now());
        }
        cr.setUpdatedBy(operatorId);
        changeRequestMapper.updateById(cr);

        recordEvent(cr.getProjectId(), cr.getStageId(), null, "CHANGE_" + action.toUpperCase(),
                "更改单状态变更: " + cr.getChangeTitle() + " → " + newStatus,
                "CONFIGURATION_CHANGE_REQUEST", id, operatorId);
    }

    @Override
    @Transactional
    public ConfigurationAudit conductAudit(Long projectId, Long stageId, String auditType, Long operatorId) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setProjectId(projectId);
        audit.setStageId(stageId);
        audit.setAuditCode("AUDIT-" + projectId + "-" + stageId + "-" + System.currentTimeMillis() % 100000);
        audit.setAuditName(auditType + " 审核");
        audit.setAuditType(auditType);
        audit.setAuditStatus("PLANNED");
        audit.setCreatedBy(operatorId);
        audit.setUpdatedBy(operatorId);
        auditMapper.insert(audit);

        recordEvent(projectId, stageId, null, "AUDIT_CREATE",
                "创建审核: " + audit.getAuditName(), "CONFIGURATION_AUDIT", audit.getId(), operatorId);

        return audit;
    }

    @Override
    @Transactional
    public void completeAudit(Long auditId, String auditResult, String auditOpinion, Long operatorId) {
        ConfigurationAudit audit = auditMapper.selectById(auditId);
        if (audit == null) {
            throw BusinessException.notFound("审核记录不存在: " + auditId);
        }
        if (!"PLANNED".equals(audit.getAuditStatus()) && !"IN_PROGRESS".equals(audit.getAuditStatus())) {
            throw BusinessException.validation("只有计划中或进行中的审核才能完成");
        }
        audit.setAuditStatus("COMPLETED");
        audit.setAuditResult(auditResult);
        audit.setAuditOpinion(auditOpinion);
        audit.setAuditTime(LocalDateTime.now());
        audit.setUpdatedBy(operatorId);
        auditMapper.updateById(audit);

        recordEvent(audit.getProjectId(), audit.getStageId(), null, "AUDIT_COMPLETE",
                "完成审核: " + audit.getAuditName() + " - " + auditResult,
                "CONFIGURATION_AUDIT", auditId, operatorId);
    }

    private void recordEvent(Long projectId, Long stageId, Long ciId, String eventType, String eventName,
                             String relatedObjectType, Long relatedObjectId, Long operatorId) {
        ConfigurationStatusAccounting event = new ConfigurationStatusAccounting();
        event.setProjectId(projectId);
        event.setStageId(stageId);
        event.setCiId(ciId);
        event.setEventType(eventType);
        event.setEventName(eventName);
        event.setRelatedObjectType(relatedObjectType);
        event.setRelatedObjectId(relatedObjectId);
        event.setEventTime(LocalDateTime.now());
        event.setOperatorId(operatorId);
        accountingMapper.insert(event);
    }
}
