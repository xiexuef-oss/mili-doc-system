package com.military.doc.ai.service;

import com.military.doc.ai.context.ChapterWritingContext;
import com.military.doc.ai.context.ChapterWritingContextService;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.service.AiChapterStructureService;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.document.service.DocChapterService;
import com.military.doc.modules.document.service.DocInputReferenceService;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.common.util.Str;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private final AiChapterStructureService aiStructureService;
    private final DocChapterService docChapterService;
    private final DocumentStructureService documentStructureService;

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
                                   com.military.doc.modules.template.mapper.DocTemplateChapterMapper tplChapterMapper,
                                   AiChapterStructureService aiStructureService,
                                   DocChapterService docChapterService,
                                   DocumentStructureService documentStructureService) {
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
        this.aiStructureService = aiStructureService;
        this.docChapterService = docChapterService;
        this.documentStructureService = documentStructureService;
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
        String result = llmClient.chat(systemPrompt, userPrompt);
        if (result != null && !result.isEmpty()) {
            String cleaned = result.replaceFirst("^(null)+", "");
            if (cleaned.length() < result.length()) {
                log.info("Stripped {} leading null chars from AI response", result.length() - cleaned.length());
            }
            result = cleaned;
        }
        return result;
    }

    /** Progress callback: (current, total, chapterTitle, charsGenerated) */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int current, int total, String chapterTitle, int chars);
    }

    /**
     * Stream-generate document content chapter by chapter.
     * Each chapter is saved to DB immediately after generation.
     * Progress events are sent via onProgress callback.
     */
    public void generateStream(Long projectId, Long catalogId, Long docLedgerId,
                                Consumer<String> onChunk, ProgressCallback onProgress) {
        List<DocChapter> chapters = loadExistingChapters(docLedgerId);

        if (chapters.isEmpty()) {
            // No chapters — auto-generate structure using AI + GJB standards
            onChunk.accept("正在根据项目数据和GJB标准自动编排章节结构...\n\n");
            // No chapters: try matching template first, else return structure for user review
            Long tplId = findMatchingTemplate(projectId, docLedgerId, catalogId);
            if (tplId != null) {
                try { chapters = docChapterService.initFromTemplate(docLedgerId, tplId, 0L); }
                catch (Exception e) { log.warn("Template init failed: {}", e.getMessage()); }
            }
            if (chapters.isEmpty()) {
                returnStructureForFrontend(projectId, docLedgerId, onChunk);
                return;
            }
            if (chapters.isEmpty()) {
                onChunk.accept("（自动编排失败，请手动从模板初始化章节）");
                return;
            }
        }

        String projectContext = contextAssemblyService.assembleContext(projectId);
        int total = chapters.size();
        int threads = Math.min(6, total);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<String[]>>();

        for (DocChapter ch : chapters) {
            futures.add(pool.submit(() -> {
                try {
                    String heading = buildHeading(ch);
                    String body = generateSingleChapterContent(projectContext, ch);
                    if (body != null && !body.isBlank()) {
                        // Save content to DB immediately
                        ch.setContent(truncate(body, 50000));
                        ch.setFillStatus("FILLED");
                        ch.setFillPercentage(100);
                        docChapterMapper.updateById(ch);
                        return new String[]{heading, body, ch.getChapterTitle()};
                    }
                } catch (Exception e) {
                    log.warn("Chapter {} {} failed: {}", ch.getChapterNumber(), ch.getChapterTitle(), e.getMessage());
                }
                return null;
            }));
        }

        // Collect and stream results in original chapter order
        int generated = 0;
        int totalChars = 0;
        for (int i = 0; i < total; i++) {
            String chTitle = chapters.get(i).getChapterTitle();
            try {
                String[] result = futures.get(i).get(180, java.util.concurrent.TimeUnit.SECONDS);
                if (result != null) {
                    String heading = result[0];
                    String body = result[1];
                    onChunk.accept(heading + "\n\n");
                    onChunk.accept(body + "\n\n");
                    generated++;
                    totalChars += body.length();
                }
            } catch (Exception e) {
                log.warn("Chapter {} timed out: {}", chTitle, e.getMessage());
                onChunk.accept("（章节「" + chTitle + "」生成超时）\n\n");
            }
            // Progress update after each chapter
            onProgress.onProgress(i + 1, total, chTitle, totalChars);
        }
        pool.shutdown();
        log.info("Per-chapter stream done: {}/{} chapters, {} total chars", generated, total, totalChars);
    }

    /** Send generated structure to frontend for user review (does NOT auto-apply). */
    private void returnStructureForFrontend(Long projectId, Long docLedgerId,
                                             Consumer<String> onChunk) {
        onChunk.accept("未找到匹配模板，正在通过AI生成文档结构供您审核...\n");
        try {
            var structure = documentStructureService.generate(projectId, docLedgerId);
            if (structure != null && !structure.chapters().isEmpty()) {
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(structure);
                onChunk.accept("__STRUCTURE__:" + json);
            } else {
                onChunk.accept("（AI未能生成有效的文档结构）");
            }
        } catch (Exception e) {
            log.error("Structure generation failed: {}", e.getMessage());
            onChunk.accept("（结构生成失败：" + e.getMessage() + "）");
        }
    }

    /** Apply a user-reviewed structure to the doc ledger, optionally saving as template. */
    public List<DocChapter> applyReviewedStructure(Long docLedgerId,
                                                    DocumentStructureService.GeneratedStructure structure,
                                                    boolean saveAsTemplate) {
        List<DocChapter> chapters = createChaptersFromStructure(docLedgerId, structure);
        if (saveAsTemplate && !chapters.isEmpty()) {
            try { documentStructureService.saveAsTemplate(structure, null, 0L); }
            catch (Exception e) { log.warn("Failed to save as template: {}", e.getMessage()); }
        }
        return chapters;
    }

    /**
     * Auto-generate chapter structure: try template matching first, then AI generation.
     * When AI generates structure, it saves as a reusable template for future use.
     */
    private List<DocChapter> autoGenerateStructure(Long projectId, Long docLedgerId,
                                                    Long catalogId, Consumer<String> onChunk) {
        // Step 1: Try to find matching template via checklist chain
        Long templateId = findMatchingTemplate(projectId, docLedgerId, catalogId);
        if (templateId != null) {
            onChunk.accept("已匹配到文档模板，正在初始化章节结构...\n");
            try {
                List<DocChapter> chapters = docChapterService.initFromTemplate(docLedgerId, templateId, 0L);
                if (!chapters.isEmpty()) {
                    onChunk.accept("从模板初始化了 " + chapters.size() + " 个章节。\n\n");
                    return chapters;
                }
            } catch (Exception e) {
                log.warn("Template init failed for ledger {}: {}", docLedgerId, e.getMessage());
            }
        }

        // Step 2: No matching template — use DocumentStructureService to generate MD structure
        // This generates a prompt-ready markdown that doubles as a reusable template
        onChunk.accept("未找到匹配模板，正在通过AI+GJB标准生成文档结构...\n");
        try {
            var structure = documentStructureService.generate(projectId, docLedgerId);
            if (structure == null || structure.chapters().isEmpty()) {
                onChunk.accept("（AI未能生成有效的文档结构）\n");
                return List.of();
            }

            // Apply the generated structure to the doc ledger
            List<DocChapter> chapters = createChaptersFromStructure(docLedgerId, structure);
            if (!chapters.isEmpty()) {
                // Save as template for future reuse
                try {
                    Long savedTemplateId = documentStructureService.saveAsTemplate(structure, null, 0L);
                    onChunk.accept("文档结构已生成（" + chapters.size() + "章），并保存为模板#" + savedTemplateId + "。\n\n");
                } catch (Exception e) {
                    onChunk.accept("文档结构已生成（" + chapters.size() + "章）。\n\n");
                    log.warn("Failed to save structure as template: {}", e.getMessage());
                }
                return chapters;
            }
        } catch (Exception e) {
            log.error("Structure generation failed: {}", e.getMessage());
            onChunk.accept("（结构生成失败：" + e.getMessage() + "）\n");
        }

        return List.of();
    }

    /** Create DocChapter records from a GeneratedStructure. */
    private List<DocChapter> createChaptersFromStructure(Long docLedgerId,
                                                          DocumentStructureService.GeneratedStructure structure) {
        List<DocChapter> chapters = new ArrayList<>();
        Map<Integer, Long> indexToId = new HashMap<>();

        for (int i = 0; i < structure.chapters().size(); i++) {
            var sc = structure.chapters().get(i);
            DocChapter dc = new DocChapter();
            dc.setDocLedgerId(docLedgerId);
            dc.setChapterNumber(sc.chapterNumber());
            dc.setChapterTitle(sc.chapterTitle());
            dc.setChapterLevel(sc.chapterLevel());
            dc.setOrderNum(sc.orderNum());
            dc.setParentId(0L);
            dc.setFillStatus("EMPTY");
            dc.setFillPercentage(0);
            dc.setCreatedBy(0L);
            dc.setUpdatedBy(0L);

            // Store writing tips in contentJson
            try {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("isRequired", sc.isRequired());
                if (sc.writingTips() != null && !sc.writingTips().isBlank()) {
                    meta.put("writingTips", sc.writingTips());
                }
                if (sc.description() != null && !sc.description().isBlank()) {
                    meta.put("description", sc.description());
                }
                dc.setContentJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(meta));
            } catch (Exception ignored) {}

            docChapterMapper.insert(dc);
            chapters.add(dc);
            indexToId.put(i, dc.getId());
        }

        // Resolve parent-child from chapter levels
        for (int i = 0; i < chapters.size(); i++) {
            DocChapter dc = chapters.get(i);
            if (dc.getChapterLevel() > 1) {
                // Find nearest prior chapter with lower level
                for (int j = i - 1; j >= 0; j--) {
                    if (chapters.get(j).getChapterLevel() < dc.getChapterLevel()) {
                        dc.setParentId(chapters.get(j).getId());
                        docChapterMapper.updateById(dc);
                        break;
                    }
                }
            }
        }

        return chapters;
    }

    /** Find matching template for a doc ledger via the checklist chain. */
    private Long findMatchingTemplate(Long projectId, Long docLedgerId, Long catalogId) {
        if (docLedgerId == null) return null;
        try {
            DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
            if (ledger == null) return null;

            // Try via checklist item → templateId
            if (ledger.getChecklistItemId() != null) {
                var checklistItem = checklistItemMapper.selectById(ledger.getChecklistItemId());
                if (checklistItem != null && checklistItem.getTemplateId() != null) {
                    return checklistItem.getTemplateId();
                }
            }
            // Try matching by specType from checklist template
            if (ledger.getSpecType() != null) {
                var templates = checklistTemplateMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StageDocChecklistTemplate>()
                        .eq(StageDocChecklistTemplate::getSpecType, ledger.getSpecType())
                        .isNotNull(StageDocChecklistTemplate::getTemplateId)
                        .last("LIMIT 1"));
                if (!templates.isEmpty() && templates.get(0).getTemplateId() != null) {
                    return templates.get(0).getTemplateId();
                }
            }
        } catch (Exception e) {
            log.debug("Template matching failed: {}", e.getMessage());
        }
        return null;
    }

    private String buildHeading(DocChapter ch) {
        int level = Math.min(ch.getChapterLevel() != null ? ch.getChapterLevel() : 1, 5);
        String hashes = "#".repeat(level + 1);
        return hashes + " " + (ch.getChapterNumber() != null ? ch.getChapterNumber() + " " : "") + ch.getChapterTitle();
    }

    /** Load existing chapters for a doc ledger, sorted by orderNum. */
    private List<DocChapter> loadExistingChapters(Long docLedgerId) {
        if (docLedgerId == null) return List.of();
        return docChapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocChapter>()
                .eq(DocChapter::getDocLedgerId, docLedgerId)
                .eq(DocChapter::getDeleted, 0)
                .orderByAsc(DocChapter::getOrderNum));
    }

    /** Generate content for a single chapter using its template metadata. */
    public String generateSingleChapterContent(String projectContext, DocChapter ch) {
        DocTemplateChapter tplCh = null;
        if (ch.getTemplateChapterId() != null) {
            tplCh = tplChapterMapper.selectById(ch.getTemplateChapterId());
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append(projectContext).append("\n");
        ctx.append("## 待撰写章节\n");
        ctx.append("- 章节编号: ").append(ch.getChapterNumber()).append("\n");
        ctx.append("- 章节标题: ").append(ch.getChapterTitle()).append("\n");
        if (tplCh != null) {
            if (tplCh.getDescription() != null) ctx.append("- 内容说明: ").append(tplCh.getDescription()).append("\n");
            if (tplCh.getWritingTips() != null) ctx.append("- 编写提示: ").append(tplCh.getWritingTips()).append("\n");
            if (tplCh.getStandardClauseRef() != null) ctx.append("- 适用标准条款: ").append(tplCh.getStandardClauseRef()).append("\n");
        }
        ctx.append("\n");
        ctx.append("撰写要求：\n");
        ctx.append("1. 撰写本章节的完整正文内容，篇幅不少于300字\n");
        ctx.append("2. 主数据中已有的具体数据直接填入，缺失的用 XXX 占位\n");
        ctx.append("3. 如需列表或表格，使用 Markdown 格式\n");
        ctx.append("4. 只写本章内容，不要写章节标题，不要提及前后章节\n");

        String systemPrompt = buildChapterSystemPrompt();
        log.debug("Generating chapter {} {} (context {} chars)", ch.getChapterNumber(), ch.getChapterTitle(), ctx.length());
        String result = llmClient.chat(systemPrompt, ctx.toString());
        if (result != null && result.startsWith("null")) {
            result = result.replaceFirst("^(null)+", "");
        }
        return result;
    }

    private void generateOneShotStream(Long projectId, Long catalogId, Long docLedgerId,
                                        Consumer<String> onChunk) {
        String context = assembleDraftContext(projectId, catalogId, docLedgerId);
        if (context.isEmpty()) return;
        if (Str.estimateTokens(context) > MAX_CONTEXT_TOKENS) {
            context = Str.truncateByTokens(context, MAX_CONTEXT_TOKENS);
        }
        String userPrompt = promptTemplateService.render("draft-generation", Map.of("context", context));
        String systemPrompt = promptTemplateService.getTemplate("system-draft-generation");
        if (systemPrompt.isEmpty()) systemPrompt = "你是一位军工文档撰写专家，请输出文档正文内容。";
        log.info("One-shot draft stream: prompt {} chars", userPrompt.length());

        var state = new Object() { boolean strippingNulls = true; int totalChars = 0; int nullsStripped = 0; };
        llmClient.chatStream(systemPrompt, userPrompt, chunk -> {
            if (state.strippingNulls) {
                String stripped = chunk.replaceAll("^null+", "");
                if (stripped.isEmpty() && chunk.length() <= 200) { state.nullsStripped += chunk.length(); return; }
                if (!stripped.isEmpty()) { state.strippingNulls = false; chunk = stripped; }
            }
            state.totalChars += chunk.length();
            onChunk.accept(chunk);
        });
        log.info("One-shot stream done: {} chars ({} nulls stripped)", state.totalChars, state.nullsStripped);
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
        // Debug: log if context contains null
        if (projectContext.contains("null")) {
            int idx = projectContext.indexOf("null");
            int s = Math.max(0, idx - 50);
            int e = Math.min(projectContext.length(), idx + 50);
            log.error("ASSEMBLED PROJECT CONTEXT contains 'null' at index {}: ...{}...", idx, projectContext.substring(s, e));
        }

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
                if (docContext.contains("null")) {
                    int idx = docContext.indexOf("null");
                    int s = Math.max(0, idx - 80);
                    int e = Math.min(docContext.length(), idx + 80);
                    log.error("DOC CONTEXT contains 'null' at index {}: ...{}...", idx, docContext.substring(s, e));
                }
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

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
