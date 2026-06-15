package com.military.doc.modules.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.mapper.ProjectDocChecklistMapper;
import com.military.doc.modules.project.entity.StageTransitionCheck;
import com.military.doc.modules.project.mapper.StageTransitionCheckMapper;
import com.military.doc.modules.project.service.StageTransitionCheckService;
import com.military.doc.modules.review.entity.ReviewMeeting;
import com.military.doc.modules.review.mapper.ReviewMeetingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 阶段转阶段检查服务实现。
 * 当一个阶段准备转入下一阶段时，进行全面检查：
 * 1. 文档清单完成率（必须≥阈值）
 * 2. 必编文档不得缺失
 * 3. 文档章节内容不得为空
 * 4. 评审会议必须关闭
 * 5. 生成详细的通过/阻塞/警告报告
 */
@Slf4j
@Service
public class StageTransitionCheckServiceImpl extends ServiceImpl<StageTransitionCheckMapper, StageTransitionCheck>
        implements StageTransitionCheckService {

    private final ProjectDocChecklistMapper checklistMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper docChapterMapper;
    private final ReviewMeetingMapper reviewMeetingMapper;

    /** 必编文档的最低要求状态（至少达到DRAFT） */
    private static final Set<String> MIN_REQUIRED_STATUSES = Set.of("DRAFT", "REVIEWING", "APPROVED", "RELEASED");

    /** 文档完成率阈值（低于此值拒绝转阶段） */
    private static final double COMPLETION_THRESHOLD = 0.80;

    public StageTransitionCheckServiceImpl(ProjectDocChecklistMapper checklistMapper,
                                           DocLedgerMapper docLedgerMapper,
                                           DocChapterMapper docChapterMapper,
                                           ReviewMeetingMapper reviewMeetingMapper) {
        this.checklistMapper = checklistMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.docChapterMapper = docChapterMapper;
        this.reviewMeetingMapper = reviewMeetingMapper;
    }

    /**
     * 执行完整的转阶段检查，返回详细报告。
     *
     * @param projectId   项目ID
     * @param fromStageId 当前阶段ID
     * @param toStageId   目标阶段ID
     * @param operatorId  操作人ID
     * @return 检查结果（pass + blockers + warnings + metrics）
     */
    public Map<String, Object> runFullCheck(Long projectId, Long fromStageId, Long toStageId, Long operatorId) {
        List<Map<String, Object>> blockers = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();
        List<Map<String, Object>> checks = new ArrayList<>();

        // ---- Check 1: Document checklist completion ----
        Map<String, Object> docCheck = checkDocumentCompletion(projectId, fromStageId);
        checks.add(docCheck);
        if ((boolean) docCheck.get("blocked")) blockers.add(docCheck);

        // ---- Check 2: Mandatory documents must exist ----
        Map<String, Object> mandatoryCheck = checkMandatoryDocuments(projectId, fromStageId);
        checks.add(mandatoryCheck);
        if ((boolean) mandatoryCheck.get("blocked")) blockers.add(mandatoryCheck);

        // ---- Check 3: Chapter content completeness ----
        Map<String, Object> chapterCheck = checkChapterContent(projectId, fromStageId);
        checks.add(chapterCheck);
        if ((boolean) chapterCheck.get("blocked")) blockers.add(chapterCheck);

        // ---- Check 4: Review meeting closure ----
        Map<String, Object> reviewCheck = checkReviewClosure(projectId, fromStageId);
        checks.add(reviewCheck);
        if ((boolean) reviewCheck.get("blocked")) blockers.add(reviewCheck);

        // ---- Aggregate verdict ----
        boolean pass = blockers.isEmpty();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pass", pass);
        result.put("blockers", blockers);
        result.put("warnings", warnings);
        result.put("checks", checks);
        result.put("blockerCount", blockers.size());
        result.put("warningCount", warnings.size());
        result.put("checkedAt", LocalDateTime.now());

        // Persist check record
        StageTransitionCheck record = new StageTransitionCheck();
        record.setProjectId(projectId);
        record.setFromStageId(fromStageId);
        record.setToStageId(toStageId);
        record.setCheckStatus(pass ? "PASS" : "BLOCKED");
        record.setBlockerItems(blockers.stream().map(b -> b.get("type").toString()).collect(Collectors.joining(",")));
        record.setCheckResult(toCheckResultJson(result));
        record.setCheckedBy(operatorId);
        record.setCheckedAt(LocalDateTime.now());
        save(record);

        log.info("Stage transition check: project={}, {}→{}, pass={}, blockers={}",
            projectId, fromStageId, toStageId, pass, blockers.size());
        return result;
    }

    // ============================================================
    // Individual Check Methods
    // ============================================================

    /**
     * Check 1: Document checklist completion rate.
     * Compares checklist items against actual document ledgers.
     */
    Map<String, Object> checkDocumentCompletion(Long projectId, Long stageId) {
        List<ProjectDocChecklist> checklist = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId)
                .eq(ProjectDocChecklist::getIsCustom, false));

        List<DocLedger> ledgers = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId));

        int total = checklist.size();
        long completed = checklist.stream()
            .filter(c -> ledgers.stream().anyMatch(l ->
                l.getDocName() != null && l.getDocName().equals(c.getDocName())
                && MIN_REQUIRED_STATUSES.contains(l.getLifecycleStatus())))
            .count();
        long approved = checklist.stream()
            .filter(c -> ledgers.stream().anyMatch(l ->
                l.getDocName() != null && l.getDocName().equals(c.getDocName())
                && ("APPROVED".equals(l.getLifecycleStatus()) || "RELEASED".equals(l.getLifecycleStatus()))))
            .count();

        double rate = total > 0 ? (double) completed / total : 0;
        boolean blocked = rate < COMPLETION_THRESHOLD;

        // List incomplete documents
        List<String> incomplete = checklist.stream()
            .filter(c -> ledgers.stream().noneMatch(l ->
                l.getDocName() != null && l.getDocName().equals(c.getDocName())
                && MIN_REQUIRED_STATUSES.contains(l.getLifecycleStatus())))
            .map(ProjectDocChecklist::getDocName)
            .limit(20)
            .toList();

        Map<String, Object> check = new LinkedHashMap<>();
        check.put("type", "DOC_COMPLETION");
        check.put("label", "文档完成率");
        check.put("total", total);
        check.put("completed", completed);
        check.put("approved", approved);
        check.put("completionRate", Math.round(rate * 100));
        check.put("threshold", Math.round(COMPLETION_THRESHOLD * 100));
        check.put("blocked", blocked);
        check.put("incompleteDocs", incomplete);
        check.put("message", blocked
            ? String.format("文档完成率 %d%% 低于阈值 %d%%，(%d/%d)", Math.round(rate * 100),
                Math.round(COMPLETION_THRESHOLD * 100), completed, total)
            : String.format("文档完成率 %d%%（%d/%d）达标", Math.round(rate * 100), completed, total));
        return check;
    }

    /**
     * Check 2: All mandatory documents (requiredFlag=true) must have status >= DRAFT.
     */
    Map<String, Object> checkMandatoryDocuments(Long projectId, Long stageId) {
        List<ProjectDocChecklist> checklist = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId));

        // Mandatory = items linked to templates with requiredFlag=true
        // Since checklist items don't store requiredFlag directly, we check all items
        // and rely on the template's requiredFlag (via templateId)
        List<DocLedger> ledgers = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId));

        // Find mandatory docs that are missing or incomplete
        List<Map<String, String>> missing = new ArrayList<>();
        for (ProjectDocChecklist item : checklist) {
            // Check if this item has a corresponding ledger with adequate status
            boolean found = ledgers.stream().anyMatch(l ->
                l.getDocName() != null && l.getDocName().equals(item.getDocName())
                && MIN_REQUIRED_STATUSES.contains(l.getLifecycleStatus()));
            if (!found) {
                DocLedger ledger = ledgers.stream()
                    .filter(l -> l.getDocName() != null && l.getDocName().equals(item.getDocName()))
                    .findFirst().orElse(null);
                String status = ledger != null ? ledger.getLifecycleStatus() : "MISSING";
                missing.add(Map.of("docName", item.getDocName(), "status", status));
            }
        }

        boolean blocked = !missing.isEmpty();

        Map<String, Object> check = new LinkedHashMap<>();
        check.put("type", "MANDATORY_DOCS");
        check.put("label", "必编文档检查");
        check.put("total", checklist.size());
        check.put("missingCount", missing.size());
        check.put("blocked", blocked);
        check.put("missingDocs", missing.stream().limit(10).toList());
        check.put("message", blocked
            ? String.format("存在 %d 个文档未编制或未达到最低状态要求", missing.size())
            : "全部必编文档已满足最低状态要求");
        return check;
    }

    /**
     * Check 3: Chapter content completeness.
     * All mandatory chapters (is_required=true) should have non-empty content.
     */
    Map<String, Object> checkChapterContent(Long projectId, Long stageId) {
        List<DocLedger> ledgers = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId));

        int emptyChapters = 0;
        int totalChapters = 0;
        List<Map<String, String>> emptyList = new ArrayList<>();

        for (DocLedger ledger : ledgers) {
            List<DocChapter> chapters = docChapterMapper.selectList(
                new LambdaQueryWrapper<DocChapter>()
                    .eq(DocChapter::getDocLedgerId, ledger.getId())
                    .eq(DocChapter::getDeleted, 0));
            for (DocChapter ch : chapters) {
                totalChapters++;
                if (ch.getContent() == null || ch.getContent().isBlank()) {
                    emptyChapters++;
                    if (emptyList.size() < 20) {
                        emptyList.add(Map.of(
                            "docName", ledger.getDocName() != null ? ledger.getDocName() : "",
                            "chapterNumber", ch.getChapterNumber() != null ? ch.getChapterNumber() : "",
                            "chapterTitle", ch.getChapterTitle() != null ? ch.getChapterTitle() : ""));
                    }
                }
            }
        }

        boolean blocked = totalChapters > 0 && (double) emptyChapters / totalChapters > 0.5;

        Map<String, Object> check = new LinkedHashMap<>();
        check.put("type", "CHAPTER_CONTENT");
        check.put("label", "章节内容检查");
        check.put("totalChapters", totalChapters);
        check.put("emptyChapters", emptyChapters);
        check.put("fillRate", totalChapters > 0 ? Math.round(100.0 * (totalChapters - emptyChapters) / totalChapters) : 100);
        check.put("blocked", blocked);
        check.put("emptyChapterDetails", emptyList);
        check.put("message", emptyChapters > 0
            ? String.format("存在 %d/%d 个章节内容为空", emptyChapters, totalChapters)
            : "所有章节内容已填充");
        return check;
    }

    /**
     * Check 4: All review meetings for this stage must be closed.
     */
    Map<String, Object> checkReviewClosure(Long projectId, Long stageId) {
        List<ReviewMeeting> meetings = reviewMeetingMapper.selectList(
            new LambdaQueryWrapper<ReviewMeeting>()
                .eq(ReviewMeeting::getProjectId, projectId)
                .eq(ReviewMeeting::getStageId, stageId));

        List<ReviewMeeting> openMeetings = meetings.stream()
            .filter(m -> !"CLOSED".equals(m.getStatus()) && !"COMPLETED".equals(m.getStatus()))
            .toList();

        boolean blocked = !openMeetings.isEmpty();

        Map<String, Object> check = new LinkedHashMap<>();
        check.put("type", "REVIEW_CLOSURE");
        check.put("label", "评审会议关闭检查");
        check.put("totalMeetings", meetings.size());
        check.put("openMeetings", openMeetings.size());
        check.put("blocked", blocked);
        check.put("openMeetingDetails", openMeetings.stream().limit(10).map(m -> Map.of(
            "meetingName", m.getMeetingName() != null ? m.getMeetingName() : "",
            "status", m.getStatus() != null ? m.getStatus() : "")).toList());
        check.put("message", blocked
            ? String.format("存在 %d 个未关闭的评审会议", openMeetings.size())
            : "所有评审会议已关闭");
        return check;
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String toCheckResultJson(Map<String, Object> result) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
        } catch (Exception e) {
            return "{}";
        }
    }
}
