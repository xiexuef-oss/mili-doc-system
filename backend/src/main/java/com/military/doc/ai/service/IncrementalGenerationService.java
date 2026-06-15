package com.military.doc.ai.service;

import com.military.doc.ai.context.ChapterWritingContextService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.service.DocChapterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 增量生成服务 — 支持单章重新生成和选中文本改写。
 */
@Slf4j
@Service
public class IncrementalGenerationService {

    private final LlmClient llmClient;
    private final ChapterWritingContextService chapterWritingContextService;
    private final DraftGenerationService draftGenerationService;
    private final DocChapterService docChapterService;

    public IncrementalGenerationService(LlmClient llmClient,
                                         ChapterWritingContextService chapterWritingContextService,
                                         DraftGenerationService draftGenerationService,
                                         DocChapterService docChapterService) {
        this.llmClient = llmClient;
        this.chapterWritingContextService = chapterWritingContextService;
        this.draftGenerationService = draftGenerationService;
        this.docChapterService = docChapterService;
    }

    /**
     * 重新生成单个章节。
     */
    public String regenerateChapter(Long docChapterId, Long projectId) {
        DocChapter chapter = docChapterService.getById(docChapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在: " + docChapterId);
        }

        log.info("Regenerating chapter {} ({}) for project {}", docChapterId, chapter.getChapterTitle(), projectId);
        String content = draftGenerationService.generateChapter(docChapterId, projectId);

        if (content != null && !content.isBlank()) {
            chapter.setContent(content);
            chapter.setFillStatus("FILLED");
            chapter.setFillPercentage(100);
            docChapterService.updateById(chapter);
        }

        return content;
    }

    /**
     * AI 改写选中文本。
     *
     * @param chapterContent 章节全文（用于上下文）
     * @param selectedText   用户选中的文本
     * @param instruction    改写指令（如"更简洁"、"更正式"、"补充技术细节"）
     * @param projectId      项目ID
     * @return 改写后的文本
     */
    public String rewriteSelection(String chapterContent, String selectedText,
                                    String instruction, Long projectId) {
        String systemPrompt = "你是一位军工文档编辑专家。请根据指令改写用户选中的文本。只输出改写后的文本，保留原有 Markdown 格式。不要添加任何解释。";

        String userPrompt = String.format("""
            请根据以下指令改写选中文本：

            指令：%s

            原文上下文（整章内容，仅供参考格式和风格）：
            %s

            需要改写的选中文本：
            %s

            改写后文本：""", instruction, chapterContent, selectedText);

        log.info("Rewrite selection: instruction='{}', text length={}", instruction, selectedText.length());
        try {
            String rewritten = llmClient.chat(systemPrompt, userPrompt);
            log.info("Rewrite result: {} chars", rewritten != null ? rewritten.length() : 0);
            return rewritten != null ? rewritten : selectedText;
        } catch (Exception e) {
            log.warn("Rewrite selection failed: {}", e.getMessage());
            throw new RuntimeException("AI 改写失败: " + e.getMessage(), e);
        }
    }
}
