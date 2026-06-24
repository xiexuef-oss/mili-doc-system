package com.military.doc.modules.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.modules.document.entity.DocApprovalFlowTemplate;
import com.military.doc.modules.document.entity.DocApprovalRecord;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocApprovalFlowTemplateMapper;
import com.military.doc.modules.document.mapper.DocApprovalRecordMapper;
import com.military.doc.modules.project.entity.ProjectMember;
import com.military.doc.modules.project.mapper.ProjectMemberMapper;
import lombok.extern.slf4j.Slf4j;
import com.military.doc.ai.service.DiffAnalysisService;
import com.military.doc.ai.service.EnterpriseBaselineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 签审流程服务 — 管理两师签审链的创建、推进和查询。
 */
@Slf4j
@Service
public class DocApprovalService {

    private final DocApprovalRecordMapper recordMapper;
    private final DocApprovalFlowTemplateMapper templateMapper;
    private final ProjectMemberMapper memberMapper;
    private final DocLedgerService docLedgerService;
    private final DiffAnalysisService diffAnalysisService;
    private final EnterpriseBaselineService enterpriseBaselineService;
    private final ObjectMapper objectMapper;
    private final com.military.doc.modules.document.mapper.DocLedgerMapper docLedgerMapper;
    private final com.military.doc.modules.document.mapper.DocChapterMapper docChapterMapper;

    public DocApprovalService(DocApprovalRecordMapper recordMapper,
                               DocApprovalFlowTemplateMapper templateMapper,
                               ProjectMemberMapper memberMapper,
                               DocLedgerService docLedgerService,
                               ObjectMapper objectMapper,
                              com.military.doc.modules.document.mapper.DocLedgerMapper docLedgerMapper,
                              com.military.doc.modules.document.mapper.DocChapterMapper docChapterMapper,
                              DiffAnalysisService diffAnalysisService,
                              EnterpriseBaselineService enterpriseBaselineService) {
        this.recordMapper = recordMapper;
        this.templateMapper = templateMapper;
        this.memberMapper = memberMapper;
        this.docLedgerService = docLedgerService;
        this.objectMapper = objectMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.docChapterMapper = docChapterMapper;
        this.diffAnalysisService = diffAnalysisService;
        this.enterpriseBaselineService = enterpriseBaselineService;
    }

    /**
     * 为文档台账初始化签审链（根据文档类型匹配签审模板）。
     */
    @Transactional
    public List<DocApprovalRecord> initApprovalFlow(Long docLedgerId, String docType) {
        // 1. 查找匹配的签审模板
        DocApprovalFlowTemplate template = findTemplate(docType);
        if (template == null) {
            log.warn("No approval template found for docType={}, using default", docType);
            template = findTemplate("TECH_DOC");
        }
        if (template == null) {
            log.warn("No approval template available");
            return List.of();
        }

        // 2. 解析签审步骤
        List<ApprovalStep> steps = parseSteps(template.getApprovalSteps());

        // 3. 删除旧签审记录
        recordMapper.delete(new LambdaQueryWrapper<DocApprovalRecord>()
            .eq(DocApprovalRecord::getDocLedgerId, docLedgerId));

        // 4. 创建新签审记录
        DocLedger ledger = docLedgerService.getById(docLedgerId);
        Long projectId = ledger != null ? ledger.getProjectId() : null;

        List<DocApprovalRecord> records = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            ApprovalStep step = steps.get(i);
            DocApprovalRecord record = new DocApprovalRecord();
            record.setDocLedgerId(docLedgerId);
            record.setApprovalStep(step.step);
            record.setApproverPosition(step.position);
            record.setSortOrder(i + 1);
            record.setCreatedAt(LocalDateTime.now());
            // 尝试自动匹配审批人
            Long approverId = findApproverByPosition(projectId, step.position, step.line);
            if (approverId != null) {
                record.setApproverId(approverId);
            }
            recordMapper.insert(record);
            records.add(record);
        }

        // 5. 更新台账状态到 CHECKING
        if (ledger != null && "DRAFTING".equals(ledger.getLifecycleStatus())) {
            docLedgerService.transitionStatus(docLedgerId, "CHECKING", null, "签审流程已启动");
        }

        log.info("Initialized approval flow for ledger {}: {} steps, template={}",
            docLedgerId, records.size(), template.getTemplateName());
        return records;
    }

    /**
     * 提交单个签审批复。
     */
    @Transactional
    public DocApprovalRecord submitApproval(Long recordId, Long approverId,
                                              String result, String opinion) {
        DocApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new IllegalArgumentException("签审记录不存在: " + recordId);

        record.setApproverId(approverId);
        record.setApprovalResult(result);
        record.setApprovalOpinion(opinion);
        record.setApprovedAt(LocalDateTime.now());
        recordMapper.updateById(record);

        Long ledgerId = record.getDocLedgerId();
        String stepLabel = stepLabel(record.getApprovalStep());

        if ("REJECTED".equals(result)) {
            // 驳回：退回编制
            docLedgerService.transitionStatus(ledgerId, "DRAFTING", approverId,
                "签审驳回(" + stepLabel + "): " + opinion);
            return record;
        }

        if ("RETURN".equals(result)) {
            // 退回修改：回到 DRAFTING 但保留已通过的签审记录
            docLedgerService.transitionStatus(ledgerId, "DRAFTING", approverId,
                "退回修改(" + stepLabel + "): " + opinion);
            return record;
        }

        // APPROVED: 检查是否所有步骤通过了
        List<DocApprovalRecord> all = getApprovalRecords(ledgerId);
        boolean allDone = all.stream().allMatch(r ->
            "APPROVED".equals(r.getApprovalResult()));

        if (allDone) {
            // 全部通过 → 发布
            docLedgerService.transitionStatus(ledgerId, "RELEASED", approverId,
                "全部签审通过，文档发布");

            // 学习闭环：终稿发布后自动触发差异分析和基线更新
            try {
                List<DocChapter> chapters = docChapterMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                        .eq(DocChapter::getDocLedgerId, ledgerId));
                StringBuilder contentBuilder = new StringBuilder();
                for (DocChapter ch : chapters) {
                    if (ch.getContent() != null) contentBuilder.append(ch.getContent()).append("\n");
                }
                String finalContent = contentBuilder.toString();
                if (!finalContent.isBlank()) {
                    var diff = diffAnalysisService.analyze(ledgerId, finalContent);
                    enterpriseBaselineService.updateFromDiff(
                        docLedgerMapper.selectById(ledgerId).getProjectId(), diff);
                    log.info("Learning loop triggered for ledgerId={}: changes={}, warningsResolved={}",
                        ledgerId, diff.getChanges().size(), diff.getWarningsResolved());
                }
            } catch (Exception e) {
                log.warn("Learning loop failed for ledgerId={}: {}", ledgerId, e.getMessage());
            }
        } else {
            // 推进到下一步
            advanceToNextStep(ledgerId);
        }

        log.info("Approval submitted: record={}, result={}, approver={}", recordId, result, approverId);
        return record;
    }

    /**
     * 获取文档的所有签审记录（按顺序）。
     */
    public List<DocApprovalRecord> getApprovalRecords(Long docLedgerId) {
        return recordMapper.selectList(new LambdaQueryWrapper<DocApprovalRecord>()
            .eq(DocApprovalRecord::getDocLedgerId, docLedgerId)
            .orderByAsc(DocApprovalRecord::getSortOrder));
    }

    /**
     * 获取当前待审批步骤。
     */
    public DocApprovalRecord getCurrentStep(Long docLedgerId) {
        var records = getApprovalRecords(docLedgerId);
        return records.stream()
            .filter(r -> r.getApprovalResult() == null)
            .findFirst().orElse(null);
    }

    // ---- private helpers ----

    private DocApprovalFlowTemplate findTemplate(String docType) {
        var qw = new LambdaQueryWrapper<DocApprovalFlowTemplate>()
            .eq(DocApprovalFlowTemplate::getDocType, docType)
            .eq(DocApprovalFlowTemplate::getIsDefault, true);
        DocApprovalFlowTemplate tmpl = templateMapper.selectOne(qw);
        if (tmpl == null) {
            tmpl = templateMapper.selectOne(new LambdaQueryWrapper<DocApprovalFlowTemplate>()
                .eq(DocApprovalFlowTemplate::getIsDefault, true));
        }
        return tmpl;
    }

    private List<ApprovalStep> parseSteps(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ApprovalStep>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse approval steps JSON", e);
            return List.of();
        }
    }

    private Long findApproverByPosition(Long projectId, String positionCode, String line) {
        if (projectId == null || positionCode == null) return null;
        var members = memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getMemberPosition, positionCode)
            .eq(ProjectMember::getStatus, "ACTIVE"));
        if (!members.isEmpty()) {
            return members.get(0).getUserId();
        }
        return null;
    }

    private void advanceToNextStep(Long docLedgerId) {
        var current = getCurrentStep(docLedgerId);
        if (current != null) {
            DocLedger ledger = docLedgerService.getById(docLedgerId);
            String stepLabel = switch (current.getApprovalStep()) {
                case "CHECK" -> "校对中";
                case "REVIEW" -> "审核中";
                case "COUNTERSIGN" -> "会签中";
                case "APPROVE" -> "批准中";
                default -> "签审中";
            };
            if (ledger != null && "CHECKING".equals(ledger.getLifecycleStatus())) {
                docLedgerService.transitionStatus(docLedgerId, "REVIEWING", null, stepLabel);
            }
        }
    }

    private static String stepLabel(String step) {
        return switch (step) {
            case "AUTHOR" -> "编制";
            case "REVIEW" -> "审核";
            case "COUNTERSIGN" -> "会签";
            case "QUALITY_APPROVE" -> "质量批准";
            case "APPROVE" -> "批准";
            case "CUSTOMER_COUNTERSIGN" -> "顾客会签";
            default -> step;
        };
    }

    private record ApprovalStep(String step, String position, String label, String line, int order, boolean required) {}
}
