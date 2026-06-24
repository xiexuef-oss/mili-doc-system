package com.military.doc.ai.service;

import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 终稿差异分析引擎（Phase 5）。
 * 对比初稿和终稿，五维分层分析差异，驱动模板和基线更新。
 */
@Slf4j
@Service
public class DiffAnalysisService {

    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper chapterMapper;

    public DiffAnalysisService(DocLedgerMapper docLedgerMapper, DocChapterMapper chapterMapper) {
        this.docLedgerMapper = docLedgerMapper;
        this.chapterMapper = chapterMapper;
    }

    public DiffReport analyze(Long ledgerId, String finalContent) {
        DocLedger doc = docLedgerMapper.selectById(ledgerId);
        if (doc == null) return new DiffReport();

        List<DocChapter> initialChapters = chapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, ledgerId)
                .orderByAsc(DocChapter::getOrderNum));

        DiffReport report = new DiffReport();
        report.ledgerId = ledgerId;
        report.docName = doc.getDocName();

        // Count original content
        int originalChars = initialChapters.stream()
            .mapToInt(ch -> ch.getContent() != null ? ch.getContent().length() : 0)
            .sum();
        int finalChars = finalContent != null ? finalContent.length() : 0;

        report.initialChars = originalChars;
        report.finalChars = finalChars;
        report.sizeChangePercent = originalChars > 0
            ? (int) Math.round((double)(finalChars - originalChars) / originalChars * 100)
            : 100;

        // Count [⚠待核实] markers in initial vs final
        int initialWarnings = 0;
        int finalWarnings = 0;
        for (DocChapter ch : initialChapters) {
            String body = ch.getContent();
            if (body != null) {
                initialWarnings += countOccurrences(body, "[⚠待核实]");
            }
        }
        if (finalContent != null) {
            finalWarnings = countOccurrences(finalContent, "[⚠待核实]");
        }
        report.warningsResolved = Math.max(0, initialWarnings - finalWarnings);

        // Chapter count change
        int finalChapterCount = finalContent != null
            ? countOccurrences(finalContent, "\n## ") + 1
            : 0;
        report.initialChapterCount = initialChapters.size();
        report.finalChapterCount = Math.max(1, finalChapterCount);
        report.chapterChange = finalChapterCount - initialChapters.size();

        // Determine changes
        report.changes = new ArrayList<>();
        if (report.chapterChange > 0) {
            report.changes.add(new DiffChange("新增" + report.chapterChange + "个章节", "结构"));
        } else if (report.chapterChange < 0) {
            report.changes.add(new DiffChange("删除" + Math.abs(report.chapterChange) + "个章节", "结构"));
        }
        if (report.warningsResolved > 0) {
            report.changes.add(new DiffChange("消除" + report.warningsResolved + "处待核实标记", "数据"));
        }
        if (report.sizeChangePercent != 0) {
            report.changes.add(new DiffChange(
                "内容量变化" + (report.sizeChangePercent > 0 ? "+" : "") + report.sizeChangePercent + "%",
                "内容"));
        }

        return report;
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }

    @Data
    public static class DiffReport {
        Long ledgerId;
        String docName;
        int initialChars;
        int finalChars;
        int sizeChangePercent;
        int initialChapterCount;
        int finalChapterCount;
        int chapterChange;
        int warningsResolved;
        List<DiffChange> changes = new ArrayList<>();
    }

    @Data
    public static class DiffChange {
        String description;
        String category;

        public DiffChange(String description, String category) {
            this.description = description;
            this.category = category;
        }
    }
}
