package com.military.doc.ai.service;

import com.military.doc.ai.context.ChapterWritingContext;
import com.military.doc.ai.context.ChapterWritingContextService;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.document.service.DocInputReferenceService;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.common.util.Str;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
    private final DocLedgerMapper docLedgerMapper;
    private final ProjectMapper projectMapper;
    private final DocInputReferenceService inputReferenceService;
    private final StageDocChecklistTemplateMapper checklistTemplateMapper;
    private final com.military.doc.modules.document.mapper.ProjectDocChecklistMapper checklistItemMapper;
    private final com.military.doc.modules.template.mapper.DocTemplateChapterMapper tplChapterMapper;

    public DraftGenerationService(ContextAssemblyService contextAssemblyService,
                                   ChapterWritingContextService chapterContextService,
                                   PromptTemplateService promptTemplateService,
                                   LlmClient llmClient,
                                   DocCatalogMapper docCatalogMapper,
                                   DocChapterMapper docChapterMapper,
                                   DocLedgerMapper docLedgerMapper,
                                   ProjectMapper projectMapper,
                                   DocInputReferenceService inputReferenceService,
                                   StageDocChecklistTemplateMapper checklistTemplateMapper,
                                   com.military.doc.modules.document.mapper.ProjectDocChecklistMapper checklistItemMapper,
                                   com.military.doc.modules.template.mapper.DocTemplateChapterMapper tplChapterMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.chapterContextService = chapterContextService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.docCatalogMapper = docCatalogMapper;
        this.docChapterMapper = docChapterMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.projectMapper = projectMapper;
        this.inputReferenceService = inputReferenceService;
        this.checklistTemplateMapper = checklistTemplateMapper;
        this.checklistItemMapper = checklistItemMapper;
        this.tplChapterMapper = tplChapterMapper;
    }

    private static final int MAX_CONTEXT_TOKENS = 50000; // DeepSeek 64K window, leave 14K for response

    public String generate(Long projectId, Long catalogId, Long docLedgerId) {
        return generateOneShot(projectId, catalogId, docLedgerId);
    }

    private String generateOneShot(Long projectId, Long catalogId, Long docLedgerId) {
        String context = assembleDraftContext(projectId, catalogId, docLedgerId);
        if (context.isEmpty()) return "";
        if (Str.estimateTokens(context) > MAX_CONTEXT_TOKENS) {
            context = Str.truncateByTokens(context, MAX_CONTEXT_TOKENS);
        }
        String userPrompt = promptTemplateService.render("draft-generation", Map.of("context", context));
        String systemPrompt = promptTemplateService.getTemplate("system-draft-generation");
        if (systemPrompt.isEmpty()) systemPrompt = "你是一位军工文档撰写专家，请输出文档正文。";
        log.info("Draft generation: catalogId={}, docLedgerId={}, prompt {} chars", catalogId, docLedgerId, userPrompt.length());
        return llmClient.chat(systemPrompt, userPrompt);
    }

    public void generateStream(Long projectId, Long catalogId, Long docLedgerId, Consumer<String> onChunk) {
        String context = assembleDraftContext(projectId, catalogId, docLedgerId);
        if (context.isEmpty()) return;
        // Safety: ensure context fits within model window
        if (Str.estimateTokens(context) > MAX_CONTEXT_TOKENS) {
            context = Str.truncateByTokens(context, MAX_CONTEXT_TOKENS);
        }
        String userPrompt = promptTemplateService.render("draft-generation", Map.of("context", context));
        String systemPrompt = promptTemplateService.getTemplate("system-draft-generation");
        if (systemPrompt.isEmpty()) systemPrompt = "你是一位军工文档撰写专家，请输出文档正文内容。";
        log.info("Draft generation stream: catalogId={}, docLedgerId={}, prompt {} chars", catalogId, docLedgerId, userPrompt.length());
        llmClient.chatStream(systemPrompt, userPrompt, onChunk);
    }

    // === Per-chapter template-driven generation ===

    private Long findTemplateForLedger(Long docLedgerId) {
        if (docLedgerId == null) return null;
        try {
            DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
            if (ledger == null || ledger.getChecklistItemId() == null) return null;
            var pdc = checklistItemMapper.selectById(ledger.getChecklistItemId());
            if (pdc == null || pdc.getTemplateId() == null) return null;
            var sct = checklistTemplateMapper.selectById(pdc.getTemplateId());
            return sct != null ? sct.getTemplateId() : null;
        } catch (Exception e) { return null; }
    }

    private String generateByTemplate(Long projectId, Long templateId) {
        var chapters = new com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<>(
            tplChapterMapper)
            .eq(com.military.doc.modules.template.entity.DocTemplateChapter::getTemplateId, templateId)
            .orderByAsc(com.military.doc.modules.template.entity.DocTemplateChapter::getOrderNum).list();
        if (chapters == null || chapters.isEmpty()) return null;

        String projectCtx = contextAssemblyService.assembleContext(projectId);
        StringBuilder doc = new StringBuilder();
        int generated = 0;
        for (var ch : chapters) {
            if (!Boolean.TRUE.equals(ch.getIsRequired())) continue;
            try {
                String body = generateChapterByTemplate(projectId, ch, projectCtx);
                if (body != null && !body.isBlank()) {
                    int lv = Math.min(ch.getChapterLevel() != null ? ch.getChapterLevel() : 1, 5);
                    doc.append("#".repeat(lv)).append(" ").append(ch.getChapterNumber())
                       .append(" ").append(ch.getChapterTitle()).append("\n\n");
                    doc.append(body).append("\n\n");
                    generated++;
                }
            } catch (Exception e) { log.warn("Ch {} failed: {}", ch.getChapterTitle(), e.getMessage()); }
        }
        if (generated == 0) return null;
        return doc.toString();
    }

    /**
     * Generate by logical sections, batching large sections for speed.
     * Small sections (<=6 subs) → one call. Large sections → split into batches of 6.
     * All batches run in parallel via 4-thread pool.
     */
    private void generateByTemplateStream(Long projectId, Long templateId, Consumer<String> onChunk) {
        var all = tplChapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.military.doc.modules.template.entity.DocTemplateChapter>()
                .eq(com.military.doc.modules.template.entity.DocTemplateChapter::getTemplateId, templateId)
                .orderByAsc(com.military.doc.modules.template.entity.DocTemplateChapter::getOrderNum));
        if (all == null || all.isEmpty()) { onChunk.accept(""); return; }

        // Deduplicate by chapter_number
        var deduped = all.stream().collect(java.util.stream.Collectors.toMap(
            ch -> ch.getChapterNumber() != null ? ch.getChapterNumber() : (ch.getParentId() + "_" + ch.getChapterTitle()),
            ch -> ch, (a, b) -> a, java.util.LinkedHashMap::new)).values().stream()
            .filter(ch -> Boolean.TRUE.equals(ch.getIsRequired()))
            .sorted(Comparator.comparing(ch -> ch.getOrderNum() != null ? ch.getOrderNum() : 0))
            .toList();

        if (deduped.isEmpty()) { onChunk.accept(""); return; }

        final String projectCtx = contextAssemblyService.assembleContext(projectId);
        // Per-batch budget: reserve ~8K tokens for chapter instructions + response
        final int PER_BATCH_PROJECT_TOKENS = Math.max(4000, (MAX_CONTEXT_TOKENS - 8000) / Math.max(1, (deduped.size() + 5) / 6));
        final String ctx = Str.estimateTokens(projectCtx) > PER_BATCH_PROJECT_TOKENS
            ? Str.truncateByTokens(projectCtx, PER_BATCH_PROJECT_TOKENS)
            : projectCtx;
        String sp = promptTemplateService.getTemplate("system-draft-generation");
        final String systemPrompt = (!sp.isEmpty()) ? sp : "你是军工文档撰写专家。只输出正文内容，不要写标题。";

        // Batch size: 6 chapters per LLM call
        int BATCH = 6;
        var batches = new java.util.ArrayList<java.util.List<com.military.doc.modules.template.entity.DocTemplateChapter>>();
        for (int i = 0; i < deduped.size(); i += BATCH) {
            batches.add(deduped.subList(i, Math.min(i + BATCH, deduped.size())));
        }

        onChunk.accept("共 " + batches.size() + " 批次，并行生成中...\n\n");
        int[] completed = {0};
        var pool = java.util.concurrent.Executors.newFixedThreadPool(4);

        // Build prompts outside lambda, then run in parallel
        var prompts = new java.util.ArrayList<String>();
        for (var batch : batches) {
            StringBuilder prompt = new StringBuilder();
            prompt.append("## 项目\n").append(ctx).append("\n\n");
            prompt.append("## 撰写以下章节\n");
            for (var ch : batch) {
                int lv = Math.min(ch.getChapterLevel() != null ? ch.getChapterLevel() : 1, 5);
                prompt.append("#".repeat(lv)).append(" ").append(ch.getChapterNumber())
                    .append(" ").append(ch.getChapterTitle()).append("\n");
                if (ch.getDescription() != null) prompt.append("说明: ").append(ch.getDescription()).append("\n");
                if (ch.getWritingTips() != null) prompt.append("提示: ").append(ch.getWritingTips()).append("\n");
            }
            prompt.append("\n请逐一撰写以上章节的正文内容。不需要写标题行（系统会自动添加）。按 GJB 规范，主数据缺失填 XXX。不要输出开场白。");
            prompts.add(prompt.toString());
        }

        var futures = new java.util.ArrayList<java.util.concurrent.CompletableFuture<Void>>();
        for (int bi = 0; bi < prompts.size(); bi++) {
            final int idx = bi;
            final String prompt = prompts.get(bi);
            final var batch = batches.get(bi);
            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    String body = llmClient.chat(systemPrompt, prompt);
                    if (body != null && !body.isBlank()) {
                        // System prepends ## headings → Parser can split chapters reliably
                        StringBuilder output = new StringBuilder();
                        for (var ch : batch) {
                            int lv = Math.min(ch.getChapterLevel() != null ? ch.getChapterLevel() : 1, 5);
                            output.append("#".repeat(lv)).append(" ")
                                .append(ch.getChapterNumber()).append(" ")
                                .append(ch.getChapterTitle()).append("\n\n");
                        }
                        output.append(body);
                        onChunk.accept(output.toString() + "\n\n");
                    }
                } catch (Exception e) {
                    log.warn("Batch {} failed: {}", idx, e.getMessage());
                }
                synchronized (completed) { completed[0]++; }
            }, pool));
        }
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        pool.shutdown();
        log.info("Section gen: {} chapters in {} batches, {} completed",
            deduped.size(), batches.size(), completed[0]);
    }

    /** Build a prompt for one logical section (parent + children) and stream result. */
    private String buildSectionPrompt(com.military.doc.modules.template.entity.DocTemplateChapter root,
                                       java.util.List<com.military.doc.modules.template.entity.DocTemplateChapter> subs,
                                       String projectCtx) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("## 项目信息\n").append(projectCtx).append("\n\n");
        ctx.append("## 写作任务\n");
        ctx.append("请撰写《").append(root.getChapterNumber()).append(" ").append(root.getChapterTitle()).append("》章节");

        if (root.getDescription() != null) ctx.append("。").append(root.getDescription());
        if (root.getWritingTips() != null) ctx.append("。编写要求：").append(root.getWritingTips());
        ctx.append("\n\n");

        if (!subs.isEmpty()) {
            ctx.append("本章包含以下小节，请逐一撰写（使用 ## 小节标题）：\n");
            for (var sub : subs) {
                if (!Boolean.TRUE.equals(sub.getIsRequired())) continue;
                ctx.append("- **").append(sub.getChapterNumber()).append(" ").append(sub.getChapterTitle()).append("**");
                if (sub.getDescription() != null) ctx.append(": ").append(sub.getDescription());
                if (sub.getWritingTips() != null) ctx.append("（编写提示: ").append(sub.getWritingTips()).append("）");
                if (sub.getStandardClauseRef() != null) ctx.append(" [标准: ").append(sub.getStandardClauseRef()).append("]");
                ctx.append("\n");
            }
        }
        ctx.append("\n要求：输出完整的 Markdown 格式内容。小节标题用 ## 开头。主数据用实际值，缺失值填 XXX。不要输出开场白。");

        String sp = promptTemplateService.getTemplate("system-draft-generation");
        final String systemPrompt = (!sp.isEmpty()) ? sp : "你是军工文档撰写专家。只输出正文内容，不要写标题。";
        log.info("Section generation: {} {}, prompt {} chars", root.getChapterNumber(), root.getChapterTitle(), ctx.length());
        return llmClient.chat(systemPrompt, ctx.toString());
    }

    private String assembleDraftContext(Long projectId, Long catalogId, Long docLedgerId) {
        StringBuilder ctx = new StringBuilder();

        // Project-level context (input files, master data, etc.)
        String projectContext = contextAssemblyService.assembleContext(projectId);
        ctx.append(projectContext);

        // Determine document metadata: prefer catalog, fall back to ledger
        String docCode = null, docName = null, docType = null, docCategory = null;
        Boolean requiredFlag = null;

        if (catalogId != null) {
            DocCatalog catalog = docCatalogMapper.selectById(catalogId);
            if (catalog != null) {
                docCode = nullToEmpty(catalog.getDocCode());
                docName = nullToEmpty(catalog.getDocName());
                docType = nullToEmpty(catalog.getDocType());
                docCategory = nullToEmpty(catalog.getDocCategory());
                requiredFlag = catalog.getRequiredFlag();
            }
        }

        if (docName == null || docName.isEmpty()) {
            // Fallback: use docLedger metadata
            if (docLedgerId != null) {
                DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
                if (ledger != null) {
                    docCode = nullToEmpty(ledger.getDocCode());
                    docName = nullToEmpty(ledger.getDocName());
                    docType = nullToEmpty(ledger.getDocType());
                    docCategory = nullToEmpty(ledger.getDocCategory());
                }
            }
        }

        if (docName != null && !docName.isEmpty()) {
            ctx.append("\n## 待生成文档\n");
            ctx.append("- 文档编号: ").append(docCode).append("\n");
            ctx.append("- 文档名称: ").append(docName).append("\n");
            ctx.append("- 文档类型: ").append(docType).append("\n");
            ctx.append("- 是否必须: ").append(Boolean.TRUE.equals(requiredFlag) ? "是" : "否").append("\n");
        }

        // ========== 四环节上下文注入 ==========

        // 查找对应的清单模板 (通过docCode或docName)
        Long checklistTemplateId = null;
        if (docCode != null && !docCode.isEmpty()) {
            StageDocChecklistTemplate tmpl = checklistTemplateMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageDocChecklistTemplate>()
                    .eq(StageDocChecklistTemplate::getDocCode, docCode));
            if (tmpl != null) {
                checklistTemplateId = tmpl.getId();
            }
        }

        // Try lookup by docName if docCode didn't match
        if (checklistTemplateId == null && docName != null && !docName.isEmpty()) {
            StageDocChecklistTemplate tmpl = checklistTemplateMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageDocChecklistTemplate>()
                    .like(StageDocChecklistTemplate::getDocName, docName)
                    .last("LIMIT 1"));
            if (tmpl != null) {
                checklistTemplateId = tmpl.getId();
            }
        }

        // 注入四环节上下文：上游文档 + 标准 + 模板章节 + 知识卡片
        if (checklistTemplateId != null) {
            String docContext = inputReferenceService.assembleDocumentContext(checklistTemplateId, projectId);
            if (!docContext.isEmpty()) {
                ctx.append("\n---\n\n");
                ctx.append(docContext);
            }
        }

        Project project = projectMapper.selectById(projectId);
        if (project != null && project.getProjectType() != null && docName != null && !docName.isEmpty()) {
            ctx.append("\n## 撰写任务\n");
            ctx.append("请为「").append(project.getProjectName()).append("」项目撰写「")
                .append(docName).append("」(");
            if (!docCategory.isEmpty()) {
                ctx.append("文档类别: ").append(docCategory).append(", ");
            }
            ctx.append("文档类型: ").append(docType.isEmpty() ? "文档" : docType)
                .append(")的完整初稿。\n");
            ctx.append("请严格遵循上文【写作模板】中的章节结构进行撰写，\n");
            ctx.append("每个章节根据【说明】和【编写提示】的要求填充内容，\n");
            ctx.append("未获取到的主数据字段用XXX占位符标记。\n");
        }

        return ctx.toString();
    }

    // ========== Chapter-level generation ==========

    /**
     * Generate body content for a single template chapter.
     * Builds focused context: chapter description + writing tips + standard clauses +
     * input file excerpts + knowledge cards, then calls LLM for just this chapter.
     */
    public String generateChapterByTemplate(Long projectId,
                                             com.military.doc.modules.template.entity.DocTemplateChapter tplCh,
                                             String projectContext) {
        StringBuilder ctx = new StringBuilder();
        ctx.append(projectContext).append("\n");

        // Chapter metadata
        ctx.append("## 当前章节\n");
        ctx.append("- 章节编号: ").append(tplCh.getChapterNumber()).append("\n");
        ctx.append("- 章节标题: ").append(tplCh.getChapterTitle()).append("\n");
        ctx.append("- 章节层级: ").append(tplCh.getChapterLevel()).append("\n");
        if (tplCh.getDescription() != null) {
            ctx.append("- 内容说明: ").append(tplCh.getDescription()).append("\n");
        }
        if (tplCh.getWritingTips() != null) {
            ctx.append("- 编写提示: ").append(tplCh.getWritingTips()).append("\n");
        }
        if (tplCh.getStandardClauseRef() != null) {
            ctx.append("- 适用标准条款: ").append(tplCh.getStandardClauseRef()).append("\n");
        }
        ctx.append("\n");

        // Task instruction
        ctx.append("## 任务\n");
        ctx.append("请为上述章节撰写正文内容。\n");
        ctx.append("- 只写本章节内容，不要涉及其他章节\n");
        ctx.append("- 主数据字段有值的直接填入，未填写的保留 XXX 占位符\n");
        ctx.append("- 如果有适用标准条款，请在正文中引用\n");
        ctx.append("- 如果需要表格，使用 Markdown 表格格式\n");
        ctx.append("- 不要输出章节标题，不要使用 # 号\n");

        String systemPrompt = buildChapterSystemPrompt();
        log.info("Chapter generation: {} {}, context {} chars",
            tplCh.getChapterNumber(), tplCh.getChapterTitle(), ctx.length());
        return llmClient.chat(systemPrompt, ctx.toString());
    }

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
        String prompt = promptTemplateService.getTemplate("system-chapter-writing");
        return !prompt.isEmpty() ? prompt : "你是一位军工文档撰写专家，请按GJB标准撰写本章节内容。";
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
