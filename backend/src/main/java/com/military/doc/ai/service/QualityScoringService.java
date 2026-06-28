package com.military.doc.ai.service;

import com.military.doc.ai.util.AiMeta;
import com.military.doc.ai.util.AiMetaParser;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.entity.ProjectDocChecklist;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.mapper.ProjectDocChecklistMapper;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 文档质量五维评分引擎。
 * 基于 Phase 0 的 AI_META 元数据和章节约束数据进行评分。
 */
@Slf4j
@Service
public class QualityScoringService {

    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper chapterMapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final ProjectDocChecklistMapper checklistMapper;
    private final StageDocChecklistTemplateMapper stageChecklistTplMapper;

    public QualityScoringService(DocLedgerMapper docLedgerMapper,
                                  DocChapterMapper chapterMapper,
                                  DocTemplateChapterMapper templateChapterMapper,
                                  ProjectDocChecklistMapper checklistMapper,
                                  StageDocChecklistTemplateMapper stageChecklistTplMapper) {
        this.docLedgerMapper = docLedgerMapper;
        this.chapterMapper = chapterMapper;
        this.templateChapterMapper = templateChapterMapper;
        this.checklistMapper = checklistMapper;
        this.stageChecklistTplMapper = stageChecklistTplMapper;
    }

    /**
     * 对文档进行五维评分。
     */
    public QualityScore scoreDocument(Long ledgerId) {
        DocLedger doc = docLedgerMapper.selectById(ledgerId);
        if (doc == null) return QualityScore.empty();

        List<DocChapter> chapters = chapterMapper.selectList(
            new LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, ledgerId)
                .orderByAsc(DocChapter::getOrderNum));

        // 获取模板章节约束
        List<DocTemplateChapter> templateChapters = getTemplateChapters(doc);

        double chapterScore = calcChapterCompleteness(templateChapters, chapters);
        double coverageScore = calcContentCoverage(chapters);
        double accuracyScore = calcDataAccuracy(chapters);
        double formatScore = calcFormatCompliance(doc, chapters);
        double readabilityScore = calcReadability(chapters);

        double total = chapterScore * 0.30 + coverageScore * 0.25
            + accuracyScore * 0.20 + formatScore * 0.15 + readabilityScore * 0.10;

        QualityScore qs = new QualityScore();
        qs.ledgerId = ledgerId;
        qs.docName = doc.getDocName();
        qs.totalScore = (int) Math.round(total);
        qs.chapterCompleteness = (int) Math.round(chapterScore);
        qs.contentCoverage = (int) Math.round(coverageScore);
        qs.dataAccuracy = (int) Math.round(accuracyScore);
        qs.formatCompliance = (int) Math.round(formatScore);
        qs.readability = (int) Math.round(readabilityScore);
        qs.totalChapters = chapters.size();
        qs.aiMetaScores = extractMetaScores(chapters);

        return qs;
    }

    private double calcChapterCompleteness(List<DocTemplateChapter> templates, List<DocChapter> chapters) {
        if (templates.isEmpty()) return 60; // No template constraints, give baseline
        long requiredCount = templates.stream().filter(t -> t.getIsRequired() != null && t.getIsRequired()).count();
        if (requiredCount == 0) requiredCount = templates.size();
        
        Set<String> chapterTitles = new HashSet<>();
        for (DocChapter ch : chapters) {
            if (ch.getChapterTitle() != null) chapterTitles.add(ch.getChapterTitle().trim());
        }
        
        long matched = templates.stream()
            .filter(t -> chapterTitles.stream().anyMatch(ct -> ct.contains(t.getChapterTitle())))
            .count();
        
        return Math.min(100, Math.round((double) matched / requiredCount * 100));
    }

    private double calcContentCoverage(List<DocChapter> chapters) {
        if (chapters.isEmpty()) return 0;
        // Count chapters with substantial content (>100 chars, excluding AI_META)
        long validChapters = chapters.stream()
            .filter(ch -> {
                String body = AiMetaParser.stripMeta(ch.getContent() != null ? ch.getContent() : "");
                return body != null && body.trim().length() >= 100;
            })
            .count();
        return Math.min(100, Math.round((double) validChapters / chapters.size() * 100));
    }

    private double calcDataAccuracy(List<DocChapter> chapters) {
        // Based on AI_META: percentage of chapters with no [⚠待核实] markers
        if (chapters.isEmpty()) return 0;
        long cleanChapters = chapters.stream()
            .filter(ch -> {
                String body = ch.getContent();
                return body != null && !body.contains("[⚠待核实]");
            })
            .count();
        return Math.min(100, Math.round((double) cleanChapters / chapters.size() * 100));
    }

    private double calcFormatCompliance(DocLedger doc, List<DocChapter> chapters) {
        int score = 80;
        // Check for forbidden words in document content
        String[] forbidden = {"XXX", "TBD", "详见XX", "待补充（非⚠标记）"};
        for (DocChapter ch : chapters) {
            String body = ch.getContent();
            if (body == null) continue;
            for (String f : forbidden) {
                if (body.contains(f)) score -= 5;
            }
        }
        return Math.max(0, Math.min(100, score));
    }

    private double calcReadability(List<DocChapter> chapters) {
        if (chapters.isEmpty()) return 0;
        int score = 70;
        // Simple heuristic: check paragraph lengths
        for (DocChapter ch : chapters) {
            String body = AiMetaParser.stripMeta(ch.getContent() != null ? ch.getContent() : "");
            if (body == null) continue;
            String[] paragraphs = body.split("\n\n");
            for (String p : paragraphs) {
                if (p.trim().length() > 2000) score -= 2; // Overly long paragraph
                if (p.trim().length() < 20 && !p.trim().isEmpty()) score -= 1; // Too short
            }
        }
        return Math.max(0, Math.min(100, score));
    }

    private List<DocTemplateChapter> getTemplateChapters(DocLedger doc) {
        Long templateId = resolveTemplateId(doc);
        if (templateId == null) return List.of();
        return templateChapterMapper.selectList(
            new LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, templateId)
                .isNotNull(DocTemplateChapter::getChapterTitle));
    }

    /**
     * doc_ledger.checklistItemId → project_doc_checklist.template_id
     * → stage_doc_checklist_template.template_id (same chain as DocxGenerationService.findTemplate)
     */
    private Long resolveTemplateId(DocLedger doc) {
        if (doc.getChecklistItemId() == null) return null;
        try {
            ProjectDocChecklist pdc = checklistMapper.selectById(doc.getChecklistItemId());
            if (pdc == null || pdc.getTemplateId() == null) return null;
            StageDocChecklistTemplate sct = stageChecklistTplMapper.selectById(pdc.getTemplateId());
            return sct != null ? sct.getTemplateId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Integer> extractMetaScores(List<DocChapter> chapters) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (DocChapter ch : chapters) {
            String body = ch.getContent();
            if (body == null) continue;
            AiMeta meta = AiMetaParser.extract(body);
            if (meta.isParsed() && meta.getChapterName() != null) {
                scores.put(meta.getChapterName(), meta.getScore());
            }
        }
        return scores;
    }

    @Data
    public static class QualityScore {
        Long ledgerId;
        String docName;
        int totalScore;
        int chapterCompleteness;
        int contentCoverage;
        int dataAccuracy;
        int formatCompliance;
        int readability;
        int totalChapters;
        Map<String, Integer> aiMetaScores = new LinkedHashMap<>();

        public static QualityScore empty() {
            QualityScore q = new QualityScore();
            q.totalScore = 0;
            return q;
        }

        public String getGrade() {
            if (totalScore >= 80) return "良好";
            if (totalScore >= 60) return "合格";
            if (totalScore >= 40) return "待改进";
            return "不合格";
        }
    }
}
