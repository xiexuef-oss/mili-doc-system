package com.military.doc.ai.service;

import com.military.doc.ai.util.AiMeta;
import com.military.doc.ai.util.AiMetaParser;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 自动审查服务。生成后自动审查章节完整性、占位符、禁用词、数据标注等。
 */
@Slf4j
@Service
public class AutoReviewService {

    private final DocChapterMapper chapterMapper;
    private final QualityScoringService qualityScoringService;

    private static final String[] FORBIDDEN = {"XXX", "TBD", "详见XX", "待补充"};

    public AutoReviewService(DocChapterMapper chapterMapper,
                              QualityScoringService qualityScoringService) {
        this.chapterMapper = chapterMapper;
        this.qualityScoringService = qualityScoringService;
    }

    public ReviewReport review(Long ledgerId) {
        QualityScoringService.QualityScore qs = qualityScoringService.scoreDocument(ledgerId);
        List<DocChapter> chapters = chapterMapper.selectList(
            new LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, ledgerId)
                .orderByAsc(DocChapter::getOrderNum));

        ReviewReport report = new ReviewReport();
        report.ledgerId = ledgerId;
        report.docName = qs.docName;
        report.qualityScore = qs;

        // 1. Forbidden words scan
        for (DocChapter ch : chapters) {
            String body = ch.getContent();
            if (body == null) continue;
            for (String fw : FORBIDDEN) {
                if (body.contains(fw)) {
                    report.forbiddenWordIssues.add(
                        new Issue("占位符", "第" + (ch.getOrderNum() != null ? ch.getOrderNum() : "?") + "章包含禁用词: " + fw,
                            Issue.SEVERITY_ERROR));
                }
            }
        }

        // 2. Collect all AI_META warnings
        List<String> allToVerify = new ArrayList<>();
        List<String> allMissing = new ArrayList<>();
        for (DocChapter ch : chapters) {
            String body = ch.getContent();
            if (body == null) continue;
            AiMeta meta = AiMetaParser.extract(body);
            if (meta.isParsed()) {
                for (String v : meta.getToVerify()) {
                    allToVerify.add(ch.getChapterTitle() + ": " + v);
                }
                for (String m : meta.getMissing()) {
                    allMissing.add(ch.getChapterTitle() + ": " + m);
                }
            }
        }

        for (String v : allToVerify) {
            report.toVerifyItems.add(new Issue("待核实", v, Issue.SEVERITY_WARN));
        }
        for (String m : allMissing) {
            report.missingItems.add(new Issue("缺项", m, Issue.SEVERITY_INFO));
        }

        // 3. Summary
        report.totalIssues = report.forbiddenWordIssues.size() + report.toVerifyItems.size() + report.missingItems.size();
        if (report.forbiddenWordIssues.isEmpty() && report.toVerifyItems.isEmpty()) {
            report.overallStatus = "PASS";
        } else if (!report.forbiddenWordIssues.isEmpty()) {
            report.overallStatus = "FAIL";
        } else {
            report.overallStatus = "WARN";
        }

        return report;
    }

    @Data
    public static class ReviewReport {
        Long ledgerId;
        String docName;
        QualityScoringService.QualityScore qualityScore;
        String overallStatus = "PASS";
        int totalIssues;
        List<Issue> forbiddenWordIssues = new ArrayList<>();
        List<Issue> toVerifyItems = new ArrayList<>();
        List<Issue> missingItems = new ArrayList<>();
    }

    @Data
    public static class Issue {
        public static final String SEVERITY_ERROR = "error";
        public static final String SEVERITY_WARN = "warn";
        public static final String SEVERITY_INFO = "info";

        String category;
        String description;
        String severity;

        public Issue(String category, String description, String severity) {
            this.category = category;
            this.description = description;
            this.severity = severity;
        }
    }
}
