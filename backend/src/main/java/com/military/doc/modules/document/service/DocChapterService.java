package com.military.doc.modules.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.military.doc.modules.document.entity.DocChapter;

import java.util.List;
import java.util.Map;

public interface DocChapterService extends IService<DocChapter> {

    /** Initialize chapters for a document from a template */
    List<DocChapter> initFromTemplate(Long docLedgerId, Long templateId, Long operatorId);

    /** Get all chapters for a document */
    List<DocChapter> listByDocLedger(Long docLedgerId);

    /** Get chapter tree (for frontend sidebar) */
    List<Map<String, Object>> getChapterTree(Long docLedgerId);

    /** Update chapter content */
    DocChapter updateContent(Long chapterId, String content, String contentJson, Long operatorId);

    /** Update fill status */
    DocChapter updateFillStatus(Long chapterId, String fillStatus, Integer fillPercentage);

    /** Get completion summary for a document */
    Map<String, Object> getCompletionSummary(Long docLedgerId);

    /** Get completion summaries for multiple documents (batch) */
    Map<Long, Map<String, Object>> getCompletionSummaryBatch(List<Long> docLedgerIds);

    /** Get chapter by id */
    DocChapter getById(Long chapterId);

    /** Validate chapter structure: sequential numbering, proper nesting, required chapters */
    ChapterStructureValidation validateStructure(Long docLedgerId);

    /** Auto-fix chapter numbering and hierarchy based on template or level-based inference */
    ChapterStructureValidation fixStructure(Long docLedgerId);

    /** Validation result */
    record ChapterStructureValidation(
        boolean valid,
        int totalChapters,
        int issuesFound,
        List<StructureIssue> issues,
        String summary
    ) {}

    record StructureIssue(
        String level,       // ERROR or WARNING
        String chapterRef,  // e.g., "3.2" or "章节#7510"
        String description
    ) {}
}
