package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.ai.service.CatalogGenerationService;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.document.service.StageDocChecklistService;
import com.military.doc.modules.project.constant.StageDefinition;
import com.military.doc.modules.project.entity.*;
import com.military.doc.modules.project.mapper.*;
import com.military.doc.modules.project.service.ProjectStageService;
import com.military.doc.modules.review.entity.ReviewMeeting;
import com.military.doc.modules.review.mapper.ReviewMeetingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ProjectStageServiceImpl extends ServiceImpl<ProjectStageMapper, ProjectStage> implements ProjectStageService {

    @Autowired
    private DocLedgerMapper docLedgerMapper;

    @Autowired
    private DocLedgerService docLedgerService;

    @Autowired
    private DocCatalogMapper docCatalogMapper;

    @Autowired
    private ConfigurationBaselineMapper baselineMapper;

    @Autowired
    private ConfigurationChangeRequestMapper changeRequestMapper;

    @Autowired
    private ConfigurationStatusAccountingMapper accountingMapper;

    @Autowired
    private ReviewMeetingMapper meetingMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private CatalogGenerationService catalogGenerationService;

    @Autowired
    private StageDocChecklistService checklistService;

    private static final Set<String> IN_PROGRESS_LIKE = Set.of("IN_PROGRESS", "REVIEWING", "RECTIFYING", "BASELINING", "GATE_CHECKING");
    private static final Set<String> COMPLETABLE_STATUSES = Set.of("COMPLETED", "TERMINATED");

    @Override
    @Transactional
    public void startStage(Long projectId, Long stageId, Long operatorId) {
        ProjectStage stage = getById(stageId);
        if (stage == null) {
            throw BusinessException.notFound("阶段不存在: " + stageId);
        }
        if (!"NOT_STARTED".equals(stage.getStatus()) && !"PLANNING".equals(stage.getStatus())
                && !"SUSPENDED".equals(stage.getStatus())) {
            throw BusinessException.validation("只有未开始、规划中或已暂停的阶段才能启动");
        }

        // check previous stages are completed
        List<ProjectStage> stages = list(new LambdaQueryWrapper<ProjectStage>()
                .eq(ProjectStage::getProjectId, projectId)
                .orderByAsc(ProjectStage::getStageOrder));
        for (ProjectStage s : stages) {
            if (s.getId().equals(stageId)) break;
            if (!COMPLETABLE_STATUSES.contains(s.getStatus())) {
                throw BusinessException.validation("前置阶段 '" + s.getStageName() + "' 尚未完成，无法启动当前阶段");
            }
        }

        stage.setStatus("IN_PROGRESS");
        stage.setIsCurrent(true);
        stage.setUpdatedBy(operatorId);
        updateById(stage);

        // clear is_current on other stages
        List<ProjectStage> toClear = stages.stream()
            .filter(s -> !s.getId().equals(stageId) && Boolean.TRUE.equals(s.getIsCurrent()))
            .peek(s -> { s.setIsCurrent(false); s.setUpdatedBy(operatorId); })
            .toList();
        if (!toClear.isEmpty()) {
            updateBatchById(toClear);
        }

        recordEvent(projectId, stageId, null, "STAGE_START", "启动阶段: " + stage.getStageName(), "PROJECT_STAGE", stageId, operatorId);

        // 从文档目录同步创建台账条目
        int synced = docLedgerService.syncFromCatalog(projectId, stageId, operatorId);
        log.info("Stage {} started: synced {} ledger entries from catalog", stageId, synced);
    }

    @Override
    @Transactional
    public void completeStage(Long projectId, Long stageId, Long operatorId) {
        ProjectStage stage = getById(stageId);
        if (stage == null) {
            throw BusinessException.notFound("阶段不存在: " + stageId);
        }
        if (!IN_PROGRESS_LIKE.contains(stage.getStatus())) {
            throw BusinessException.validation("当前阶段状态不允许完成操作");
        }

        // Run gate check
        Map<String, Object> gateResult = gateCheck(projectId, stageId, operatorId);
        boolean passed = Boolean.TRUE.equals(gateResult.get("passed"));
        if (!passed) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blockers = (List<Map<String, Object>>) gateResult.getOrDefault("blockers", List.of());
            StringBuilder sb = new StringBuilder("转阶段准入检查未通过:\n");
            for (Map<String, Object> b : blockers) {
                sb.append("- ").append(b.getOrDefault("description", "")).append("\n");
            }
            throw BusinessException.validation(sb.toString().trim());
        }

        stage.setStatus("COMPLETED");
        stage.setIsCurrent(false);
        stage.setUpdatedBy(operatorId);
        updateById(stage);

        recordEvent(projectId, stageId, null, "STAGE_COMPLETE", "完成阶段: " + stage.getStageName(), "PROJECT_STAGE", stageId, operatorId);
    }

    @Override
    @Transactional
    public void suspendStage(Long projectId, Long stageId, Long operatorId) {
        ProjectStage stage = getById(stageId);
        if (stage == null) {
            throw BusinessException.notFound("阶段不存在: " + stageId);
        }
        if (!IN_PROGRESS_LIKE.contains(stage.getStatus())) {
            throw BusinessException.validation("只有活动状态的阶段才能暂停");
        }
        stage.setStatus("SUSPENDED");
        stage.setUpdatedBy(operatorId);
        updateById(stage);
        recordEvent(projectId, stageId, null, "STAGE_SUSPEND", "暂停阶段: " + stage.getStageName(), "PROJECT_STAGE", stageId, operatorId);
    }

    @Override
    @Transactional
    public void terminateStage(Long projectId, Long stageId, Long operatorId) {
        ProjectStage stage = getById(stageId);
        if (stage == null) {
            throw BusinessException.notFound("阶段不存在: " + stageId);
        }
        if ("COMPLETED".equals(stage.getStatus())) {
            throw BusinessException.validation("已完成的阶段不能终止");
        }
        stage.setStatus("TERMINATED");
        stage.setIsCurrent(false);
        stage.setUpdatedBy(operatorId);
        updateById(stage);
        recordEvent(projectId, stageId, null, "STAGE_TERMINATE", "终止阶段: " + stage.getStageName(), "PROJECT_STAGE", stageId, operatorId);
    }

    @Override
    public Map<String, Object> getStageWorkbench(Long projectId, Long stageId) {
        ProjectStage stage = getById(stageId);
        if (stage == null) {
            throw BusinessException.notFound("阶段不存在: " + stageId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stage", stage);

        List<DocLedger> allDocs = docLedgerMapper.selectList(new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId));
        long totalDocs = allDocs.size();
        long releasedDocs = allDocs.stream().filter(d -> "RELEASED".equals(d.getLifecycleStatus()) || "ARCHIVED".equals(d.getLifecycleStatus())).count();
        result.put("totalDocs", totalDocs);
        result.put("releasedDocs", releasedDocs);
        result.put("docCompletionRate", totalDocs > 0 ? Math.round(100.0 * releasedDocs / totalDocs) : 0);

        List<ConfigurationBaseline> baselines = baselineMapper.selectList(new LambdaQueryWrapper<ConfigurationBaseline>()
                .eq(ConfigurationBaseline::getProjectId, projectId)
                .eq(ConfigurationBaseline::getStageId, stageId));
        result.put("baselines", baselines);
        result.put("hasEffectiveBaseline", baselines.stream().anyMatch(b -> "EFFECTIVE".equals(b.getBaselineStatus())));

        Long openChanges = changeRequestMapper.selectCount(new LambdaQueryWrapper<ConfigurationChangeRequest>()
                .eq(ConfigurationChangeRequest::getProjectId, projectId)
                .eq(ConfigurationChangeRequest::getStageId, stageId)
                .notIn(ConfigurationChangeRequest::getStatus, "CLOSED", "REJECTED"));
        result.put("openChangeRequests", openChanges);

        long reviewingDocs = allDocs.stream().filter(d -> "REVIEWING".equals(d.getLifecycleStatus())).count();
        result.put("reviewingDocs", reviewingDocs);

        // review meeting stats
        List<ReviewMeeting> meetings = meetingMapper.selectList(new LambdaQueryWrapper<ReviewMeeting>()
                .eq(ReviewMeeting::getProjectId, projectId)
                .eq(ReviewMeeting::getStageId, stageId));
        long openMeetings = meetings.stream().filter(m -> !"CLOSED".equals(m.getStatus()) && !"COMPLETED".equals(m.getStatus())).count();
        result.put("totalMeetings", meetings.size());
        result.put("openMeetings", openMeetings);
        result.put("reviewClosureRate", meetings.isEmpty() ? 100 : Math.round(100.0 * (meetings.size() - openMeetings) / meetings.size()));

        return result;
    }

    @Override
    public Map<String, Object> gateCheck(Long projectId, Long stageId, Long operatorId) {
        List<Map<String, Object>> blockers = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();

        // 1. Document completeness check
        List<DocLedger> unreleased = docLedgerMapper.findUnreleasedByStage(projectId, stageId);
        if (!unreleased.isEmpty()) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("type", "DOC_INCOMPLETE");
            b.put("description", "存在 " + unreleased.size() + " 个未归档文档");
            b.put("count", unreleased.size());
            b.put("details", unreleased.stream().map(d -> Map.of("docCode", d.getDocCode() != null ? d.getDocCode() : "", "docName", d.getDocName() != null ? d.getDocName() : "", "status", d.getLifecycleStatus() != null ? d.getLifecycleStatus() : "")).toList());
            blockers.add(b);
        }

        // 2. Review closure check
        List<ReviewMeeting> meetings = meetingMapper.selectList(new LambdaQueryWrapper<ReviewMeeting>()
                .eq(ReviewMeeting::getProjectId, projectId)
                .eq(ReviewMeeting::getStageId, stageId));
        List<ReviewMeeting> openMeetings = meetings.stream()
                .filter(m -> !"CLOSED".equals(m.getStatus()) && !"COMPLETED".equals(m.getStatus()))
                .toList();
        if (!openMeetings.isEmpty()) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("type", "REVIEW_OPEN");
            b.put("description", "存在 " + openMeetings.size() + " 个未关闭的评审会议");
            b.put("count", openMeetings.size());
            b.put("details", openMeetings.stream().map(m -> Map.of("meetingName", m.getMeetingName() != null ? m.getMeetingName() : "", "status", m.getStatus() != null ? m.getStatus() : "")).toList());
            blockers.add(b);
        }

        // 3. Change request closure check
        List<ConfigurationChangeRequest> openChanges = changeRequestMapper.selectList(new LambdaQueryWrapper<ConfigurationChangeRequest>()
                .eq(ConfigurationChangeRequest::getProjectId, projectId)
                .eq(ConfigurationChangeRequest::getStageId, stageId)
                .notIn(ConfigurationChangeRequest::getStatus, "CLOSED", "REJECTED"));
        if (!openChanges.isEmpty()) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("type", "CHANGE_OPEN");
            b.put("description", "存在 " + openChanges.size() + " 个未关闭的技术状态更改单");
            b.put("count", openChanges.size());
            b.put("details", openChanges.stream().map(c -> Map.of("changeTitle", c.getChangeTitle() != null ? c.getChangeTitle() : "", "status", c.getStatus() != null ? c.getStatus() : "")).toList());
            blockers.add(b);
        }

        // 4. Baseline effectiveness check
        List<ConfigurationBaseline> baselines = baselineMapper.selectList(new LambdaQueryWrapper<ConfigurationBaseline>()
                .eq(ConfigurationBaseline::getProjectId, projectId)
                .eq(ConfigurationBaseline::getStageId, stageId));
        boolean hasEffective = baselines.stream().anyMatch(b -> "EFFECTIVE".equals(b.getBaselineStatus()));
        boolean hasAny = !baselines.isEmpty();
        if (hasAny && !hasEffective) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("type", "BASELINE_NOT_EFFECTIVE");
            b.put("description", "存在基线但无当前有效基线，请设置有效基线");
            b.put("count", baselines.size());
            blockers.add(b);
        }
        if (!hasAny) {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("type", "NO_BASELINE");
            w.put("description", "尚未建立阶段技术状态基线");
            warnings.add(w);
        }

        // 5. Document review status check
        List<DocLedger> inReview = docLedgerMapper.selectList(new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId)
                .eq(DocLedger::getLifecycleStatus, "REVIEWING"));
        if (!inReview.isEmpty()) {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("type", "DOCS_IN_REVIEW");
            w.put("description", "存在 " + inReview.size() + " 个正在评审中的文档");
            w.put("count", inReview.size());
            warnings.add(w);
        }

        boolean passed = blockers.isEmpty();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", passed);
        result.put("blockers", blockers);
        result.put("warnings", warnings);
        result.put("checkedAt", LocalDateTime.now().toString());

        // Record gate check event
        recordEvent(projectId, stageId, null, "GATE_CHECK",
                "转阶段准入检查: " + (passed ? "通过" : "未通过") + " (阻断项: " + blockers.size() + ", 警告: " + warnings.size() + ")",
                "PROJECT_STAGE", stageId, operatorId);

        return result;
    }

    @Override
    @Transactional
    public List<ProjectStage> initializeProjectStages(Long projectId, String initialStageCode, Long operatorId) {
        StageDefinition initialDef = StageDefinition.findByCode(initialStageCode);
        if (initialDef == null) {
            throw BusinessException.validation("未知的初始阶段: " + initialStageCode);
        }

        // Persist the initial stage code on the project
        Project project = projectMapper.selectById(projectId);
        if (project != null) {
            project.setInitialStageCode(initialStageCode);
            project.setUpdatedBy(operatorId);
            projectMapper.updateById(project);
        }

        // Create all stages from the initial stage onwards
        List<ProjectStage> created = new ArrayList<>();
        for (StageDefinition def : StageDefinition.ALL) {
            if (def.order() < initialDef.order()) continue;
            ProjectStage stage = new ProjectStage();
            stage.setProjectId(projectId);
            stage.setStageCode(def.code());
            stage.setStageName(def.name());
            stage.setStageOrder(def.order());
            stage.setStatus("NOT_STARTED");
            stage.setStageGoal(def.description());
            stage.setCreatedBy(operatorId);
            stage.setUpdatedBy(operatorId);
            save(stage);
            created.add(stage);
        }

        // Set the first stage (initial stage) as current and IN_PROGRESS
        if (!created.isEmpty()) {
            ProjectStage first = created.stream().min(Comparator.comparingInt(ProjectStage::getStageOrder)).orElse(null);
            if (first != null) {
                first.setStatus("IN_PROGRESS");
                first.setIsCurrent(true);
                first.setUpdatedBy(operatorId);
                updateById(first);
                recordEvent(projectId, first.getId(), null, "STAGE_START", "项目创建，自动启动初始阶段: " + first.getStageName(), "PROJECT_STAGE", first.getId(), operatorId);

                // AI catalog generation is triggered manually via the stage UI to avoid blocking project creation

                // Auto-generate document checklist from GJB template library
                try {
                    checklistService.autoGenerateForStage(projectId, first.getId(), first.getStageCode());
                } catch (Exception e) {
                    log.warn("Failed to auto-generate checklist for stage {}: {}", first.getId(), e.getMessage());
                }
            }
        }
        return created;
    }

    @Override
    @Transactional
    public Map<String, Object> requestTransition(Long projectId, Long currentStageId, Long operatorId) {
        ProjectStage currentStage = getById(currentStageId);
        if (currentStage == null) {
            throw BusinessException.notFound("当前阶段不存在: " + currentStageId);
        }
        if (!IN_PROGRESS_LIKE.contains(currentStage.getStatus())) {
            throw BusinessException.validation("当前阶段状态不允许申请转阶段");
        }

        // Find next stage by order
        List<ProjectStage> stages = list(new LambdaQueryWrapper<ProjectStage>()
                .eq(ProjectStage::getProjectId, projectId)
                .orderByAsc(ProjectStage::getStageOrder));
        ProjectStage nextStage = null;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).getId().equals(currentStageId) && i + 1 < stages.size()) {
                nextStage = stages.get(i + 1);
                break;
            }
        }
        if (nextStage == null) {
            throw BusinessException.validation("已是最后一个阶段，无法转阶段");
        }

        // Run gate check
        Map<String, Object> gateResult = gateCheck(projectId, currentStageId, operatorId);
        boolean passed = Boolean.TRUE.equals(gateResult.get("passed"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gateCheck", gateResult);

        if (passed) {
            // Complete current stage
            currentStage.setStatus("COMPLETED");
            currentStage.setIsCurrent(false);
            currentStage.setUpdatedBy(operatorId);
            updateById(currentStage);
            recordEvent(projectId, currentStageId, null, "STAGE_COMPLETE", "转阶段完成: " + currentStage.getStageName(), "PROJECT_STAGE", currentStageId, operatorId);

            // Start next stage
            nextStage.setStatus("IN_PROGRESS");
            nextStage.setIsCurrent(true);
            nextStage.setUpdatedBy(operatorId);
            updateById(nextStage);
            recordEvent(projectId, nextStage.getId(), null, "STAGE_START", "转阶段启动: " + nextStage.getStageName(), "PROJECT_STAGE", nextStage.getId(), operatorId);

            int synced = docLedgerService.syncFromCatalog(projectId, nextStage.getId(), operatorId);
            log.info("Transition to stage {}: synced {} ledger entries from catalog", nextStage.getId(), synced);

            // Auto-generate document checklist for the next stage
            try {
                checklistService.autoGenerateForStage(projectId, nextStage.getId(), nextStage.getStageCode());
            } catch (Exception e) {
                log.warn("Failed to auto-generate checklist for stage {}: {}", nextStage.getId(), e.getMessage());
            }

            result.put("transitioned", true);
            result.put("fromStage", currentStage.getStageName());
            result.put("toStage", nextStage.getStageName());
            result.put("nextStageId", nextStage.getId());
            result.put("message", "转阶段成功: " + currentStage.getStageName() + " → " + nextStage.getStageName());
        } else {
            result.put("transitioned", false);
            result.put("message", "转阶段准入检查未通过，存在" + ((List<?>) gateResult.getOrDefault("blockers", List.of())).size() + "个阻断项");
        }

        return result;
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
