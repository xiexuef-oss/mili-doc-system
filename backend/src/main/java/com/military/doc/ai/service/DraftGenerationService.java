package com.military.doc.ai.service;

import com.military.doc.ai.context.ChapterWritingContext;
import com.military.doc.ai.context.ChapterWritingContextService;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class DraftGenerationService {

    private final ContextAssemblyService contextAssemblyService;
    private final ChapterWritingContextService chapterContextService;
    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final DocCatalogMapper docCatalogMapper;
    private final DocChapterMapper docChapterMapper;
    private final ProjectMapper projectMapper;

    public DraftGenerationService(ContextAssemblyService contextAssemblyService,
                                   ChapterWritingContextService chapterContextService,
                                   PromptTemplateService promptTemplateService,
                                   LlmClient llmClient,
                                   DocCatalogMapper docCatalogMapper,
                                   DocChapterMapper docChapterMapper,
                                   ProjectMapper projectMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.chapterContextService = chapterContextService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.docCatalogMapper = docCatalogMapper;
        this.docChapterMapper = docChapterMapper;
        this.projectMapper = projectMapper;
    }

    public String generate(Long projectId, Long catalogId) {
        String context = assembleDraftContext(projectId, catalogId);
        if (context.isEmpty()) return "";

        String userPrompt = promptTemplateService.render("draft-generation",
            Map.of("context", context));
        String systemPrompt = "你是一位军工文档撰写专家，精通 GJB 5882-2006《军工产品研制技术文件编写指南》、GJB 438C、GJB 9001C、GJB 3206B 等标准。请严格按照 GJB 5882 规定的文档结构和内容要求撰写，输出 Markdown 格式的完整文档正文。";

        log.info("Draft generation: catalogId={}, prompt {} chars", catalogId, userPrompt.length());
        return llmClient.chat(systemPrompt, userPrompt);
    }

    public void generateStream(Long projectId, Long catalogId, Consumer<String> onChunk) {
        String context = assembleDraftContext(projectId, catalogId);
        if (context.isEmpty()) return;

        String userPrompt = promptTemplateService.render("draft-generation",
            Map.of("context", context));
        String systemPrompt = "你是一位军工文档撰写专家，精通 GJB 5882-2006《军工产品研制技术文件编写指南》、GJB 438C、GJB 9001C、GJB 3206B 等标准。请严格按照 GJB 5882 规定的文档结构和内容要求撰写，输出 Markdown 格式的完整文档正文。";

        log.info("Draft generation stream: catalogId={}, prompt {} chars", catalogId, userPrompt.length());
        llmClient.chatStream(systemPrompt, userPrompt, onChunk);
    }

    private String assembleDraftContext(Long projectId, Long catalogId) {
        StringBuilder ctx = new StringBuilder();

        // Project-level context
        String projectContext = contextAssemblyService.assembleContext(projectId);
        ctx.append(projectContext);

        // Catalog entry details
        DocCatalog catalog = docCatalogMapper.selectById(catalogId);
        if (catalog != null) {
            ctx.append("\n## 待生成文档\n");
            ctx.append("- 文档编号: ").append(nullToEmpty(catalog.getDocCode())).append("\n");
            ctx.append("- 文档名称: ").append(nullToEmpty(catalog.getDocName())).append("\n");
            ctx.append("- 文档类型: ").append(nullToEmpty(catalog.getDocType())).append("\n");
            ctx.append("- 是否必须: ").append(Boolean.TRUE.equals(catalog.getRequiredFlag()) ? "是" : "否").append("\n");
        }

        Project project = projectMapper.selectById(projectId);
        if (project != null && project.getProjectType() != null) {
            String docTypeName = catalog != null ? nullToEmpty(catalog.getDocType()) : "";
            String docCategory = catalog != null ? nullToEmpty(catalog.getDocCategory()) : "";
            ctx.append("\n## 撰写任务\n");
            ctx.append("请为「").append(project.getProjectName()).append("」项目撰写「")
                .append(catalog != null ? nullToEmpty(catalog.getDocName()) : "")
                .append("」(");
            if (!docCategory.isEmpty()) {
                ctx.append("文档类别: ").append(docCategory).append(", ");
            }
            ctx.append("文档类型: ").append(docTypeName.isEmpty() ? "文档" : docTypeName)
                .append(")的完整初稿。\n");
        }

        return ctx.toString();
    }

    // ========== Chapter-level generation ==========

    /**
     * Generate content for a single chapter using three-library context.
     */
    public String generateChapter(Long docChapterId, Long projectId) {
        ChapterWritingContext ctx = chapterContextService.assembleForChapter(docChapterId, projectId);
        if (ctx == null) return "";

        String chapterContext = ctx.toPromptContext();
        String systemPrompt = buildChapterSystemPrompt();
        String userPrompt = "请撰写以下章节的完整内容，严格遵循编写指南、标准条款和知识卡片要求。\n\n" + chapterContext;

        log.info("Chapter generation: chapterId={}, context {} chars", docChapterId, chapterContext.length());
        return llmClient.chat(systemPrompt, userPrompt);
    }

    /**
     * Stream-generate content for a single chapter.
     */
    public void generateChapterStream(Long docChapterId, Long projectId, Consumer<String> onChunk) {
        ChapterWritingContext ctx = chapterContextService.assembleForChapter(docChapterId, projectId);
        if (ctx == null) return;

        String chapterContext = ctx.toPromptContext();
        String systemPrompt = buildChapterSystemPrompt();
        String userPrompt = "请撰写以下章节的完整内容，严格遵循编写指南、标准条款和知识卡片要求。\n\n" + chapterContext;

        log.info("Chapter generation stream: chapterId={}", docChapterId);
        llmClient.chatStream(systemPrompt, userPrompt, onChunk);
    }

    /**
     * Generate all chapters for a document ledger sequentially.
     */
    public int generateAllChapters(Long docLedgerId, Long projectId) {
        List<DocChapter> chapters = docChapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, docLedgerId)
                .eq(DocChapter::getDeleted, 0)
                .orderByAsc(DocChapter::getOrderNum)
        );

        int generated = 0;
        for (DocChapter chapter : chapters) {
            if (chapter.getContent() != null && !chapter.getContent().isBlank()) {
                log.info("Skipping chapter {} (already has content)", chapter.getId());
                continue;
            }
            try {
                String content = generateChapter(chapter.getId(), projectId);
                if (content != null && !content.isBlank()) {
                    chapter.setContent(content);
                    chapter.setFillStatus("DRAFT");
                    docChapterMapper.updateById(chapter);
                    generated++;
                    log.info("Generated chapter {}: {} chars", chapter.getId(), content.length());
                }
            } catch (Exception e) {
                log.error("Failed to generate chapter {}: {}", chapter.getId(), e.getMessage());
            }
        }
        log.info("Generated {} chapters for ledger {}", generated, docLedgerId);
        return generated;
    }

    private String buildChapterSystemPrompt() {
        return """
            你是一位军工文档撰写专家，精通 GJB 5882-2006《军工产品研制技术文件编写指南》、\
            GJB 438C、GJB 9001C、GJB 3206B 等军用标准。

            撰写要求:
            1. 只撰写本章节内容，不要包含其他章节
            2. 严格遵循编写指南中的章节说明和编写提示
            3. 遵守适用标准条款中的规定和要求
            4. 参考知识卡片中的编写技巧和范例
            5. 使用规范的军工文档术语和表达方式
            6. 主数据字段有值的直接填入，未填写的保留 ××× 占位符
            7. 输出 Markdown 格式，不添加章节标题(系统会自动添加)
            8. 不输出任何开场白或结束语，直接输出内容
            """;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
