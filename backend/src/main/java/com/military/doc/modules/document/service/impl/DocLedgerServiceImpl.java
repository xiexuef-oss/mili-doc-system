package com.military.doc.modules.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.DocLedgerLog;
import com.military.doc.modules.document.mapper.DocLedgerLogMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.service.DocChapterService;
import com.military.doc.modules.document.service.DocLedgerLogService;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.mapper.ProjectDocChecklistMapper;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.project.entity.ConfigurationStatusAccounting;
import com.military.doc.modules.project.mapper.ConfigurationStatusAccountingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocLedgerServiceImpl extends ServiceImpl<DocLedgerMapper, DocLedger> implements DocLedgerService {

    /** Per-(projectId, catalogId) locks to prevent concurrent findOrCreateDraftLedger race. */
    private static final ConcurrentHashMap<String, Object> DRAFTING_LOCKS = new ConcurrentHashMap<>();

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
    private DocLedgerLogService logService;

    @Autowired
    private ConfigurationStatusAccountingMapper accountingMapper;

    @Autowired
    private DocCatalogMapper docCatalogMapper;

    @Autowired
    private ProjectDocChecklistMapper checklistMapper;

    @Autowired
    private StageDocChecklistTemplateMapper checklistTemplateMapper;

    @Autowired
    private DocChapterService docChapterService;

    @Autowired
    private com.military.doc.modules.document.mapper.CompletenessCheckResultMapper completenessCheckResultMapper;

    @Autowired
    private com.military.doc.modules.document.mapper.DocApprovalRecordMapper approvalRecordMapper;

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

    @Override
    @Transactional
    public int syncFromCatalog(Long projectId, Long stageId, Long operatorId) {
        List<DocCatalog> catalogs = docCatalogMapper.selectList(
            new LambdaQueryWrapper<DocCatalog>()
                .eq(DocCatalog::getProjectId, projectId)
                .eq(stageId != null, DocCatalog::getStageId, stageId)
                .eq(DocCatalog::getRequiredFlag, true)
        );

        Set<Long> existingCatalogIds = list(new LambdaQueryWrapper<DocLedger>()
            .eq(DocLedger::getProjectId, projectId)
            .eq(stageId != null, DocLedger::getStageId, stageId)
            .isNotNull(DocLedger::getCatalogId))
            .stream()
            .map(DocLedger::getCatalogId)
            .filter(cid -> cid != null)
            .collect(Collectors.toSet());

        // Build map of existing ledgers by catalogId for quick lookup
        Map<Long, DocLedger> existingByCatalog = list(new LambdaQueryWrapper<DocLedger>()
            .eq(DocLedger::getProjectId, projectId)
            .eq(stageId != null, DocLedger::getStageId, stageId)
            .isNotNull(DocLedger::getCatalogId))
            .stream()
            .filter(l -> l.getCatalogId() != null)
            .collect(Collectors.toMap(DocLedger::getCatalogId, l -> l, (a, b) -> a));

        List<DocLedger> newLedgers = new ArrayList<>();
        int updatedCount = 0;
        for (DocCatalog catalog : catalogs) {
            DocLedger existing = existingByCatalog.get(catalog.getId());
            if (existing != null) {
                // Overwrite metadata from catalog
                existing.setDocCode(catalog.getDocCode());
                existing.setDocName(catalog.getDocName());
                existing.setDocType(catalog.getDocType());
                existing.setRequiredFlag(catalog.getRequiredFlag());
                existing.setMeetingUsage(catalog.getMeetingUsage());
                existing.setUsageSource(catalog.getUsageSource());
                existing.setUsageAdjustReason(catalog.getUsageAdjustReason());
                existing.setChangeReason(catalog.getChangeReason());
                existing.setResponsibleUserId(catalog.getResponsibleUserId());
                existing.setUpdatedBy(operatorId);
                updateById(existing);
                updatedCount++;
                continue;
            }
            DocLedger ledger = new DocLedger();
            ledger.setProjectId(catalog.getProjectId());
            ledger.setStageId(catalog.getStageId());
            ledger.setCatalogId(catalog.getId());
            ledger.setDocCode(catalog.getDocCode());
            ledger.setDocName(catalog.getDocName());
            ledger.setDocType(catalog.getDocType());
            ledger.setRequiredFlag(catalog.getRequiredFlag());
            ledger.setMeetingUsage(catalog.getMeetingUsage());
            ledger.setUsageSource(catalog.getUsageSource());
            ledger.setUsageAdjustReason(catalog.getUsageAdjustReason());
            ledger.setChangeReason(catalog.getChangeReason());
            ledger.setResponsibleUserId(catalog.getResponsibleUserId());
            ledger.setLifecycleStatus("PLANNED");
            ledger.setCreatedBy(operatorId);
            ledger.setUpdatedBy(operatorId);
            newLedgers.add(ledger);
        }

        if (!newLedgers.isEmpty()) {
            saveBatch(newLedgers);

            List<DocLedgerLog> logs = new ArrayList<>();
            for (DocLedger ledger : newLedgers) {
                DocLedgerLog log = new DocLedgerLog();
                log.setDocLedgerId(ledger.getId());
                log.setToStatus("PLANNED");
                log.setOperatorId(operatorId);
                log.setOperatedAt(LocalDateTime.now());
                log.setRemark("从文档目录自动创建 (catalogId=" + ledger.getCatalogId() + ")");
                logs.add(log);
            }
            logService.saveBatch(logs);
        }

        return newLedgers.size() + updatedCount;
    }

    @Override
    @Transactional
    public int syncFromChecklist(Long projectId, Long stageId, Long operatorId) {
        List<ProjectDocChecklist> items = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId)
        );

        Set<Long> existingItemIds = list(new LambdaQueryWrapper<DocLedger>()
            .eq(DocLedger::getProjectId, projectId)
            .eq(DocLedger::getStageId, stageId)
            .isNotNull(DocLedger::getChecklistItemId))
            .stream()
            .map(DocLedger::getChecklistItemId)
            .filter(cid -> cid != null)
            .collect(Collectors.toSet());

        // Batch: collect all template IDs
        Set<Long> tplIdSet = new HashSet<>();
        for (ProjectDocChecklist item : items) {
            if (item.getTemplateId() != null) {
                tplIdSet.add(item.getTemplateId());
            }
        }
        Map<Long, StageDocChecklistTemplate> tplMap = new HashMap<>();
        if (!tplIdSet.isEmpty()) {
            List<StageDocChecklistTemplate> allTmpls = checklistTemplateMapper.selectBatchIds(tplIdSet);
            for (StageDocChecklistTemplate tmpl : allTmpls) {
                if (tmpl != null) tplMap.put(tmpl.getId(), tmpl);
            }
        }

        List<DocLedger> newLedgers = new ArrayList<>();
        for (ProjectDocChecklist item : items) {
            if (existingItemIds.contains(item.getId())) {
                continue;
            }
            DocLedger ledger = new DocLedger();
            ledger.setProjectId(item.getProjectId());
            ledger.setStageId(item.getStageId());
            ledger.setStageCode(item.getStageCode());
            ledger.setChecklistItemId(item.getId());
            ledger.setDocName(item.getDocName());
            ledger.setDocCategory(item.getCategory());
            ledger.setLifecycleStatus("PLANNED");
            ledger.setCreatedBy(operatorId);
            ledger.setUpdatedBy(operatorId);

            // 从模板同步specType
            if (item.getTemplateId() != null) {
                StageDocChecklistTemplate tmpl = tplMap.get(item.getTemplateId());
                if (tmpl != null && tmpl.getSpecType() != null) {
                    ledger.setSpecType(tmpl.getSpecType());
                }
            }

            newLedgers.add(ledger);
        }

        if (!newLedgers.isEmpty()) {
            saveBatch(newLedgers);

            List<DocLedgerLog> logs = new ArrayList<>();
            for (DocLedger ledger : newLedgers) {
                DocLedgerLog log = new DocLedgerLog();
                log.setDocLedgerId(ledger.getId());
                log.setToStatus("PLANNED");
                log.setOperatorId(operatorId);
                log.setOperatedAt(LocalDateTime.now());
                log.setRemark("从阶段文档清单自动创建 (checklistItemId=" + ledger.getChecklistItemId() + ")");
                logs.add(log);
            }
            logService.saveBatch(logs);
        }

        return newLedgers.size();
    }

    @Override
    @Transactional
    public void initChaptersFromTemplate(Long docLedgerId, Long templateId, Long operatorId) {
        DocLedger ledger = getById(docLedgerId);
        if (ledger == null) throw BusinessException.notFound("文档台账条目不存在: " + docLedgerId);
        docChapterService.initFromTemplate(docLedgerId, templateId, operatorId);
        ledger.setLifecycleStatus("DRAFTING");
        ledger.setUpdatedBy(operatorId);
        updateById(ledger);
    }

    @Override
    @Transactional
    public void deleteLedger(Long id) {
        DocLedger ledger = getById(id);
        if (ledger == null) throw BusinessException.notFound("文档台账条目不存在: " + id);

        // 1. Delete chapters
        docChapterService.remove(new LambdaQueryWrapper<com.military.doc.modules.document.entity.DocChapter>()
            .eq(com.military.doc.modules.document.entity.DocChapter::getDocLedgerId, id));

        // 2. Delete completeness check results
        completenessCheckResultMapper.delete(new LambdaQueryWrapper<com.military.doc.modules.document.entity.CompletenessCheckResult>()
            .eq(com.military.doc.modules.document.entity.CompletenessCheckResult::getDocLedgerId, id));

        // 3. Delete configuration status accounting records
        accountingMapper.delete(new LambdaQueryWrapper<ConfigurationStatusAccounting>()
            .eq(ConfigurationStatusAccounting::getDocLedgerId, id));

        // 4. Delete approval records
        approvalRecordMapper.delete(new LambdaQueryWrapper<com.military.doc.modules.document.entity.DocApprovalRecord>()
            .eq(com.military.doc.modules.document.entity.DocApprovalRecord::getDocLedgerId, id));

        // 5. Delete status transition logs
        logMapper.delete(new LambdaQueryWrapper<DocLedgerLog>()
            .eq(DocLedgerLog::getDocLedgerId, id));

        // 6. Delete the ledger itself
        removeById(id);
    }

    @Override
    @Transactional
    public int deleteByChecklistItemId(Long checklistItemId) {
        List<DocLedger> ledgers = list(new LambdaQueryWrapper<DocLedger>()
            .eq(DocLedger::getChecklistItemId, checklistItemId));
        for (DocLedger ledger : ledgers) {
            deleteLedger(ledger.getId());
        }
        return ledgers.size();
    }

    @Override
    @Transactional
    public DocLedger findOrCreateDraftLedger(Long projectId, Long stageId, Long catalogId,
                                              String docName, String docType, Long operatorId) {
        // Serialize per (projectId, catalogId) to prevent concurrent duplicate DRAFTING ledgers
        String lockKey = projectId + ":" + (catalogId != null ? catalogId : "NONE");
        Object lock = DRAFTING_LOCKS.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            try {
                // Try to find existing DRAFTING ledger by catalogId
                if (catalogId != null) {
                    List<DocLedger> existing = list(new LambdaQueryWrapper<DocLedger>()
                        .eq(DocLedger::getProjectId, projectId)
                        .eq(DocLedger::getCatalogId, catalogId)
                        .eq(DocLedger::getLifecycleStatus, "DRAFTING")
                        .orderByDesc(DocLedger::getId)
                        .last("LIMIT 1"));
                    if (!existing.isEmpty()) {
                        DocLedger ledger = existing.get(0);
                        if (docName != null && !docName.equals(ledger.getDocName())) ledger.setDocName(docName);
                        if (docType != null) ledger.setDocType(docType);
                        ledger.setStageId(stageId);
                        ledger.setUpdatedBy(operatorId);
                        ledger.setUpdatedAt(LocalDateTime.now());
                        updateById(ledger);
                        return ledger;
                    }
                }

                // Create new
                DocLedger ledger = new DocLedger();
                ledger.setProjectId(projectId);
                ledger.setStageId(stageId);
                ledger.setCatalogId(catalogId);
                ledger.setDocName(docName != null ? docName : "AI 生成文档");
                ledger.setDocType(docType != null ? docType : "MANAGEMENT_DOC");
                ledger.setLifecycleStatus("DRAFTING");
                ledger.setRequiredFlag(true);
                ledger.setCreatedBy(operatorId);
                ledger.setUpdatedBy(operatorId);
                ledger.setCreatedAt(LocalDateTime.now());
                ledger.setUpdatedAt(LocalDateTime.now());
                save(ledger);
                return ledger;
            } finally {
                // Clean up lock entries to prevent memory leak
                DRAFTING_LOCKS.remove(lockKey);
            }
        }
    }
}
