package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.DocLedgerLog;
import com.military.doc.modules.document.mapper.DocLedgerLogMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.project.entity.ConfigurationStatusAccounting;
import com.military.doc.modules.project.mapper.ConfigurationStatusAccountingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DocLedgerServiceImpl extends ServiceImpl<DocLedgerMapper, DocLedger> implements DocLedgerService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        "PLANNED",    Set.of("DRAFTING"),
        "DRAFTING",   Set.of("CHECKING", "PLANNED"),
        "CHECKING",   Set.of("REVIEWING", "DRAFTING"),
        "REVIEWING",  Set.of("APPROVING", "DRAFTING"),
        "APPROVING",  Set.of("RELEASED", "DRAFTING"),
        "RELEASED",   Set.of("ARCHIVED", "DRAFTING")
    );

    @Autowired
    private DocLedgerLogMapper logMapper;

    @Autowired
    private ConfigurationStatusAccountingMapper accountingMapper;

    @Override
    @Transactional
    public DocLedger createLedger(DocLedger ledger, Long operatorId) {
        ledger.setLifecycleStatus("PLANNED");
        ledger.setCreatedBy(operatorId);
        ledger.setUpdatedBy(operatorId);
        save(ledger);

        DocLedgerLog log = new DocLedgerLog();
        log.setDocLedgerId(ledger.getId());
        log.setToStatus("PLANNED");
        log.setOperatorId(operatorId);
        log.setOperatedAt(LocalDateTime.now());
        log.setRemark("创建文档台账条目");
        logMapper.insert(log);

        return ledger;
    }

    @Override
    @Transactional
    public void transitionStatus(Long id, String targetStatus, Long operatorId, String remark) {
        DocLedger ledger = getById(id);
        if (ledger == null) {
            throw BusinessException.notFound("文档台账条目不存在: " + id);
        }

        String currentStatus = ledger.getLifecycleStatus();
        if (currentStatus == null) {
            throw BusinessException.validation("当前状态为空，无法转移");
        }

        Set<String> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw BusinessException.validation(
                String.format("不允许从 %s 转移到 %s", currentStatus, targetStatus));
        }

        ledger.setLifecycleStatus(targetStatus);
        ledger.setUpdatedBy(operatorId);
        updateById(ledger);

        DocLedgerLog log = new DocLedgerLog();
        log.setDocLedgerId(id);
        log.setFromStatus(currentStatus);
        log.setToStatus(targetStatus);
        log.setOperatorId(operatorId);
        log.setOperatedAt(LocalDateTime.now());
        log.setRemark(remark);
        logMapper.insert(log);

        // Write to configuration status accounting (GJB 3206B)
        ConfigurationStatusAccounting csa = new ConfigurationStatusAccounting();
        csa.setProjectId(ledger.getProjectId());
        csa.setStageId(ledger.getStageId());
        csa.setDocLedgerId(id);
        csa.setEventType("DOC_STATUS_CHANGE");
        csa.setEventName("文档状态变更: " + (ledger.getDocName() != null ? ledger.getDocName() : "") + " " + currentStatus + " → " + targetStatus);
        csa.setEventDescription(remark);
        csa.setRelatedObjectType("DOC_LEDGER");
        csa.setRelatedObjectId(id);
        csa.setEventTime(LocalDateTime.now());
        csa.setOperatorId(operatorId);
        accountingMapper.insert(csa);
    }

    @Override
    public List<DocLedger> findUnreleasedByStage(Long projectId, Long stageId) {
        return baseMapper.findUnreleasedByStage(projectId, stageId);
    }

    @Override
    public List<DocLedger> listByProject(Long projectId, Long stageId, String lifecycleStatus) {
        LambdaQueryWrapper<DocLedger> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocLedger::getProjectId, projectId);
        if (stageId != null) {
            wrapper.eq(DocLedger::getStageId, stageId);
        }
        if (lifecycleStatus != null && !lifecycleStatus.isBlank()) {
            wrapper.eq(DocLedger::getLifecycleStatus, lifecycleStatus);
        }
        wrapper.orderByAsc(DocLedger::getDocCode);
        return list(wrapper);
    }
}
