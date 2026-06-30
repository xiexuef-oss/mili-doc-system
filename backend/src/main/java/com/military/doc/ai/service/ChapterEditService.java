package com.military.doc.ai.service;

import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.util.LlmOutputCleaner;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI-powered chapter editing: rewrite, expand, shorten, polish.
 */
@Slf4j
@Service
public class ChapterEditService {

    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final DocChapterMapper chapterMapper;
    private final DocLedgerMapper docLedgerMapper;

    public ChapterEditService(PromptTemplateService promptTemplateService,
                               LlmClient llmClient,
                               DocChapterMapper chapterMapper,
                               DocLedgerMapper docLedgerMapper) {
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.chapterMapper = chapterMapper;
        this.docLedgerMapper = docLedgerMapper;
    }

    public enum EditAction { rewrite, expand, shorten, polish }

    /**
     * AI-edit a chapter's content.
     */
    public String edit(Long chapterId, EditAction action, String instruction) {
        DocChapter ch = chapterMapper.selectById(chapterId);
        if (ch == null) throw new RuntimeException("章节不存在: " + chapterId);

        DocLedger ledger = docLedgerMapper.selectById(ch.getDocLedgerId());
        String docName = ledger != null ? ledger.getDocName() : "文档";

        String template = promptTemplateService.getTemplate("chapter-edit");
        if (template == null || template.isEmpty()) {
            template = "你是一位文档编辑专家。请对以下章节内容执行" + action + "操作。\n\n当前内容：\n{{currentContent}}\n\n只输出编辑后的正文。";
        }

        String prompt = template
            .replace("{{docName}}", docName)
            .replace("{{chapterNumber}}", ch.getChapterNumber() != null ? ch.getChapterNumber() : "")
            .replace("{{chapterTitle}}", ch.getChapterTitle() != null ? ch.getChapterTitle() : "")
            .replace("{{currentContent}}", ch.getContent() != null ? ch.getContent() : "（空）")
            .replace("{{action}}", action.name())
            .replace("{{instruction}}", instruction != null ? instruction : "");

        log.info("Chapter edit: chapterId={}, action={}, instruction={}", chapterId, action, instruction);
        String result = llmClient.chat("你是一位军工文档编辑专家。", prompt);
        if (result != null && result.startsWith("null")) result = LlmOutputCleaner.stripLeadingNull(result);
        return result;
    }
}
