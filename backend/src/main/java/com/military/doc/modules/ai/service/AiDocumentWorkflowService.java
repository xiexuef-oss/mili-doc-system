package com.military.doc.modules.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.DelegatingLlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.ai.entity.AiDocument;
import com.military.doc.modules.ai.entity.AiDocumentSection;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.document.service.DocLedgerService;
import com.military.doc.modules.template.entity.DocTemplateV2;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.mapper.DocTemplateV2Mapper;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDocumentWorkflowService {

    private final AiDocumentService aiDocService;
    private final AiDocumentSectionService sectionService;
    private final AiDocumentAgentService agentService;
    private final DocLedgerService ledgerService;
    private final DocCatalogMapper catalogMapper;
    private final DocTemplateV2Mapper templateMapper;
    private final DocTemplateChapterMapper chapterMapper;
    private final DelegatingLlmClient llmClient;
    private final PromptTemplateService promptService;
    private final ContextAssemblyService contextAssembly;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[\\s*\\{.*?\\}\\s*\\]", Pattern.DOTALL);

    // ──────────────── 流程入口：从文档清单开始写作 ────────────────

    @Transactional
    public Map<String, Object> startDocumentFromCatalog(Long projectId, Long catalogId, String docName, String docType, Long ledgerId, Long userId) {
        log.info("startDocumentFromCatalog: projectId={}, catalogId={}, docName={}, docType={}, ledgerId={}, userId={}", 
            projectId, catalogId, docName, docType, ledgerId, userId);
        
        String finalDocName = docName != null && !docName.isBlank() ? docName : "未命名文档";
        String finalDocType = docType != null && !docType.isBlank() ? docType : "";
        
        AiDocument aiDoc = aiDocService.create(userId, projectId, finalDocName, finalDocType, "");
        aiDoc.setStatus("draft");
        aiDocService.updateById(aiDoc);

        Map<String, Object> result = new HashMap<>();
        result.put("document", aiDoc);
        result.put("catalog", null);
        result.put("hasTemplate", false);
        result.put("templateId", null);
        result.put("docLedger", null);
        result.put("ledgerId", ledgerId);

        log.info("startDocumentFromCatalog completed: docId={}, title={}", aiDoc.getId(), aiDoc.getTitle());
        return result;
    }

    // ──────────────── 检查是否有模板 ────────────────

    public boolean checkHasTemplate(DocCatalog catalog) {
        return findTemplateId(catalog) != null;
    }

    public Long findTemplateId(DocCatalog catalog) {
        List<DocTemplateV2> templates = templateMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateV2>()
            .eq(DocTemplateV2::getTemplateType, catalog.getDocType())
            .eq(DocTemplateV2::getStatus, "ACTIVE")
            .last("LIMIT 1"));
        return templates.isEmpty() ? null : templates.get(0).getId();
    }

    // ──────────────── 从模板加载大纲 ────────────────

    @Transactional
    public Map<String, Object> loadOutlineFromTemplate(Long documentId, Long templateId, Long userId) {
        AiDocument aiDoc = aiDocService.getById(documentId);
        if (aiDoc == null) throw BusinessException.notFound("文档不存在");

        DocTemplateV2 template = templateMapper.selectById(templateId);
        if (template == null) throw BusinessException.notFound("模板不存在");

        try {
            // Only load outline for preview — do NOT persist sections yet
            // (sections are persisted when user clicks "confirm" via confirmTemplateAndStartWriting)
            List<Map<String, Object>> outline = loadChaptersAsOutline(templateId);

            Map<String, Object> result = new HashMap<>();
            result.put("outline", outline);
            result.put("template", template);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("loadOutlineFromTemplate failed: docId={}, templateId={}", documentId, templateId, e);
            throw new BusinessException("TEMPLATE_LOAD_FAILED", "模板加载失败：" + e.getMessage());
        }
    }

    /** Load template chapters and convert to outline format. Deduplicates by title+level. */
    private List<Map<String, Object>> loadChaptersAsOutline(Long templateId) {
        List<DocTemplateChapter> chapters = chapterMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateChapter>()
                .eq(DocTemplateChapter::getTemplateId, templateId)
                .orderByAsc(DocTemplateChapter::getOrderNum));

        // Deduplicate by (title, level) + auto-number chapters
        Set<String> seen = new java.util.HashSet<>();
        List<Map<String, Object>> outline = new ArrayList<>();
        int sortOrder = 0;
        int[] counters = new int[6]; // support up to level 5
        for (DocTemplateChapter ch : chapters) {
            String key = (ch.getChapterTitle() != null ? ch.getChapterTitle() : "") + "|" + ch.getChapterLevel();
            if (!seen.add(key)) continue;

            int level = ch.getChapterLevel() != null ? ch.getChapterLevel() : 1;
            if (level < counters.length) {
                counters[level - 1]++;
                for (int i = level; i < counters.length; i++) counters[i] = 0;
            }
            // Build number string like "1.", "1.1", "1.1.1"
            StringBuilder num = new StringBuilder();
            for (int i = 0; i < level && i < counters.length; i++) {
                if (counters[i] > 0) {
                    if (num.length() > 0) num.append('.');
                    num.append(counters[i]);
                }
            }
            String numberedTitle = num + " " + (ch.getChapterTitle() != null ? ch.getChapterTitle() : "");

            sortOrder++;
            Map<String, Object> item = new HashMap<>();
            item.put("title", numberedTitle);
            item.put("level", level);
            item.put("sortOrder", sortOrder);
            item.put("status", "empty");
            item.put("description", ch.getDescription() != null ? ch.getDescription() : "");
            outline.add(item);
        }
        return outline;
    }

    @Transactional
    public void saveOutlineToDocument(Long documentId, List<Map<String, Object>> outline, Long userId) {
        sectionService.getBaseMapper().delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiDocumentSection>().eq(AiDocumentSection::getDocumentId, documentId));
        Long lastId = null;
        for (Map<String, Object> item : outline) {
            String title = (String) item.get("title");
            Integer level = (Integer) item.getOrDefault("level", 1);
            String status = (String) item.getOrDefault("status", "empty");
            AiDocumentSection sec = sectionService.addAfter(documentId, title, level, lastId, null, userId);
            sec.setStatus(status);
            sectionService.updateById(sec);
            lastId = sec.getId();
        }
    }

    // ──────────────── AI生成模板（无模板时） ────────────────

    public Map<String, Object> generateTemplateFromKnowledge(Long documentId, Long userId) {
        AiDocument aiDoc = aiDocService.getById(documentId);
        if (aiDoc == null) throw BusinessException.notFound("文档不存在");

        String system = promptService.getTemplate("template-generation") != null ? promptService.getTemplate("template-generation") : (
            "你是军工文档模板专家。根据文档类型和项目信息，生成一份标准的文档模板大纲。\n" +
            "输出JSON数组，每个元素: {title, level(1-3), description:该章节应包含的内容描述}\n" +
            "必须符合GJB 5882-2006标准。只输出JSON数组，不要任何说明。");

        // Try AI generation first, fall back to default outline on any error
        List<Map<String, Object>> outline = null;
        try {
            String context = "";
            try {
                context = contextAssembly.assembleBasicContext(aiDoc.getProjectId());
            } catch (Exception ignored) {}

            String prompt = String.format(
                "文档类型：%s\n文档标题：%s\n项目背景：\n%s\n\n请生成该文档的标准章节结构。",
                aiDoc.getDocumentType(), aiDoc.getTitle(), context);

            String raw = llmClient.chatWithAudit(system, prompt, "canvas-template", aiDoc.getProjectId());
            raw = cleanLlmOutput(raw);

            outline = parseJsonArray(raw);
        } catch (Exception e) {
            log.warn("AI template generation failed, using default outline: {}", e.getMessage());
        }
        if (outline == null || outline.isEmpty()) {
            outline = generateDefaultOutline(aiDoc.getDocumentType());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("outline", outline);
        result.put("generated", true);
        return result;
    }

    private List<Map<String, Object>> generateDefaultOutline(String docType) {
        List<Map<String, Object>> outline = new ArrayList<>();
        outline.add(Map.of("title", "范围", "level", 1, "description", "说明文档的适用范围"));
        outline.add(Map.of("title", "规范性引用文件", "level", 1, "description", "列出引用的标准文件"));
        outline.add(Map.of("title", "术语和定义", "level", 1, "description", "定义文档中使用的术语"));
        outline.add(Map.of("title", "总体方案", "level", 1, "description", "描述总体技术方案"));
        outline.add(Map.of("title", "详细设计", "level", 1, "description", "描述详细设计内容"));
        outline.add(Map.of("title", "验证与确认", "level", 1, "description", "描述验证方法和结果"));
        outline.add(Map.of("title", "质量保证", "level", 1, "description", "描述质量保证措施"));
        return outline;
    }

    // ──────────────── 确认模板后开始写作 ────────────────

    @Transactional
    public void confirmTemplateAndStartWriting(Long documentId, List<Map<String, Object>> outline, Long userId) {
        saveOutlineToDocument(documentId, outline, userId);
        AiDocument aiDoc = aiDocService.getById(documentId);
        aiDoc.setStatus("generating");
        aiDocService.updateById(aiDoc);
    }

    // ──────────────── 生成文档内容（基于模板大纲） ────────────────

    private static final java.util.concurrent.ExecutorService GEN_POOL =
        java.util.concurrent.Executors.newFixedThreadPool(8);

    public void generateContentFromOutline(Long documentId, java.util.function.Consumer<Map<String, Object>> onProgress) {
        AiDocument aiDoc = aiDocService.getById(documentId);
        if (aiDoc == null) throw BusinessException.notFound("文档不存在");

        List<AiDocumentSection> sections = sectionService.getByDocumentId(documentId);
        if (sections.isEmpty()) return;

        String system = "你是军工文档撰写专家。为以下章节写正文(300-800字)。只输出正文，不要标题。";

        // Collect sections that need generation
        List<AiDocumentSection> pending = new ArrayList<>();
        for (AiDocumentSection s : sections) {
            if (s.getContent() != null && !s.getContent().isBlank()) continue;
            pending.add(s);
        }

        java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
        int total = pending.size();
        if (total == 0) {
            onProgress.accept(Map.of("status", "completed", "message", "所有章节已有内容"));
            return;
        }

        // Process in batches of 8 for max parallel generation
        int batchSize = 8;
        for (int i = 0; i < pending.size(); i += batchSize) {
            int end = Math.min(i + batchSize, pending.size());
            List<AiDocumentSection> batch = pending.subList(i, end);
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(batch.size());

            for (AiDocumentSection section : batch) {
                GEN_POOL.submit(() -> {
                    try {
                        onProgress.accept(Map.of("status", "generating", "section", section.getTitle(),
                            "progress", completed.get() * 100 / total));

                        String prompt = "章节：" + section.getTitle() + "\n请写正文(300-800字)";

                        String content = null;
                        for (int attempt = 0; attempt < 2; attempt++) {
                            try {
                                content = llmClient.chat(system, prompt);
                                content = cleanLlmOutput(content);
                                if (content != null && !content.isBlank()) break;
                            } catch (Exception e) {
                                if (attempt == 0) log.warn("Section '{}' attempt {} failed, retrying", section.getTitle(), attempt + 1);
                            }
                        }

                        if (content != null && !content.isBlank()) {
                            sectionService.updateContent(section.getId(), content, null);
                        } else {
                            log.warn("Section '{}' has no content after 2 attempts", section.getTitle());
                        }
                    } finally {
                        int done = completed.incrementAndGet();
                        onProgress.accept(Map.of("status", "progress", "completed", done, "total", total));
                        latch.countDown();
                    }
                });
            }

            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }

            // Save contentJson after each batch (from main thread, no race conditions)
            try {
                List<AiDocumentSection> cur = sectionService.getByDocumentId(documentId);
                aiDocService.updateContent(documentId, buildContentJsonFromSections(cur));
            } catch (Exception e) { log.warn("Batch contentJson save failed: {}", e.getMessage()); }
        }

        // Final save
        try {
            List<AiDocumentSection> finalSections = sectionService.getByDocumentId(documentId);
            aiDocService.updateContent(documentId, buildContentJsonFromSections(finalSections));
            log.info("generateContentFromOutline: final contentJson saved");
        } catch (Exception e) { log.error("Final contentJson save failed", e); }

        aiDoc.setStatus("ready");
        aiDocService.updateById(aiDoc);
        onProgress.accept(Map.of("status", "completed", "message", "文档生成完成"));
    }

    // ──────────────── 关联到文档台账 ────────────────

    @Transactional
    public DocLedger linkToDocLedger(Long documentId, Long catalogId, Long userId) {
        AiDocument aiDoc = aiDocService.getById(documentId);
        if (aiDoc == null) throw BusinessException.notFound("文档不存在");

        return ledgerService.findOrCreateDraftLedger(
            aiDoc.getProjectId(), null, catalogId, aiDoc.getTitle(), aiDoc.getDocumentType(), userId);
    }

    // ──────────────── 保存到正式文档 ────────────────

    @Transactional
    public void saveToDocLedger(Long documentId, Long ledgerId, Long userId) {
        AiDocument aiDoc = aiDocService.getById(documentId);
        DocLedger ledger = ledgerService.getById(ledgerId);
        if (aiDoc == null || ledger == null) throw BusinessException.notFound("文档或台账不存在");

        List<AiDocumentSection> sections = sectionService.getByDocumentId(documentId);
        StringBuilder content = new StringBuilder();
        for (AiDocumentSection sec : sections) {
            content.append("#".repeat(sec.getLevel())).append(" ").append(sec.getTitle()).append("\n\n");
            if (sec.getContent() != null) content.append(sec.getContent()).append("\n\n");
        }

        // ledger.setDocContent(content.toString()); // TODO: add field to DocLedger
        ledger.setLifecycleStatus("DRAFTING");
        ledger.setUpdatedBy(userId);
        ledgerService.updateById(ledger);
    }

    // ──────────────── 获取项目文档清单（用于前端选择） ────────────────

    public List<Map<String, Object>> getProjectDocChecklist(Long projectId) {
        List<DocLedger> ledgers = ledgerService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocLedger>()
            .eq(DocLedger::getProjectId, projectId)
            .orderByAsc(DocLedger::getDocCode));

        List<Map<String, Object>> result = new ArrayList<>();
        for (DocLedger ledger : ledgers) {
            DocCatalog catalog = ledger.getCatalogId() != null ? catalogMapper.selectById(ledger.getCatalogId()) : null;
            
            Map<String, Object> item = new HashMap<>();
            item.put("ledgerId", ledger.getId());
            item.put("catalogId", ledger.getCatalogId());
            item.put("docCode", ledger.getDocCode());
            item.put("docName", ledger.getDocName());
            item.put("docType", ledger.getDocType());
            item.put("stageId", ledger.getStageId());
            item.put("lifecycleStatus", ledger.getLifecycleStatus());
            
            boolean hasTemplate = false;
            Long templateId = null;
            String templateName = null;
            if (catalog != null && catalog.getDocType() != null && !catalog.getDocType().isBlank()) {
                hasTemplate = checkHasTemplate(catalog);
                templateId = findTemplateId(catalog);
            } else if (ledger.getDocType() != null && !ledger.getDocType().isBlank()) {
                // Exact match by templateType = docType
                List<DocTemplateV2> templates = templateMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateV2>()
                    .eq(DocTemplateV2::getTemplateType, ledger.getDocType())
                    .eq(DocTemplateV2::getStatus, "ACTIVE")
                    .last("LIMIT 1"));
                hasTemplate = !templates.isEmpty();
                templateId = hasTemplate ? templates.get(0).getId() : null;
                templateName = hasTemplate ? templates.get(0).getTemplateName() : null;
            } else {
                // Fallback: match by document name against template names
                // e.g., "软件开发计划(SDP)" matches template "软件开发计划"
                DocTemplateV2 bestMatch = findTemplateByName(ledger.getDocName());
                hasTemplate = bestMatch != null;
                templateId = bestMatch != null ? bestMatch.getId() : null;
                templateName = bestMatch != null ? bestMatch.getTemplateName() : null;
            }

            item.put("hasTemplate", hasTemplate);
            item.put("templateId", templateId);
            item.put("templateName", templateName);
            result.add(item);
        }
        return result;
    }

    // ──────────────── Helper ────────────────

    /** Build TipTap/ProseMirror-compatible content JSON from generated sections */
    private String buildContentJsonFromSections(List<AiDocumentSection> sections) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (AiDocumentSection sec : sections) {
            // Heading block
            Map<String, Object> headingAttrs = new HashMap<>();
            headingAttrs.put("level", sec.getLevel() != null ? sec.getLevel() : 1);
            headingAttrs.put("id", "sec-" + sec.getId());
            Map<String, Object> headingBlock = new HashMap<>();
            headingBlock.put("type", "heading");
            headingBlock.put("attrs", headingAttrs);
            headingBlock.put("content", List.of(Map.of("type", "text", "text", sec.getTitle() != null ? sec.getTitle() : "")));
            blocks.add(headingBlock);

            // Content paragraphs (safely handle null content)
            String sectionContent = sec.getContent();
            if (sectionContent != null && !sectionContent.isBlank()) {
                for (String para : sectionContent.split("\\n\\s*\\n")) {
                    String trimmed = para.trim();
                    if (trimmed.isEmpty()) continue;
                    Map<String, Object> paraBlock = new HashMap<>();
                    paraBlock.put("type", "paragraph");
                    paraBlock.put("content", List.of(Map.of("type", "text", "text", trimmed)));
                    blocks.add(paraBlock);
                }
            }
        }
        Map<String, Object> doc = new HashMap<>();
        doc.put("type", "doc");
        doc.put("content", blocks);
        try {
            return objectMapper.writeValueAsString(doc);
        } catch (Exception e) {
            log.error("Failed to build contentJson", e);
            return "{\"type\":\"doc\",\"content\":[]}";
        }
    }

    /** Match document name against active template names.
     *  Only high-confidence matches: template name fully contained in doc name or vice versa.
     *  e.g. "软件开发计划(SDP)" matches template "软件开发计划"
     *       "详细设计" matches template "详细设计"
     *  Returns the best-matching template, or null for documents without a clear template match. */
    private DocTemplateV2 findTemplateByName(String docName) {
        if (docName == null || docName.isBlank()) return null;
        List<DocTemplateV2> all = templateMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocTemplateV2>()
            .eq(DocTemplateV2::getStatus, "ACTIVE"));
        if (all.isEmpty()) return null;

        String dn = docName.trim();
        // Remove parenthetical suffixes like (SSDD), (SDP), (B类规范)
        String dnClean = dn.replaceAll("\\s*\\(.*?\\)\\s*", "").trim();
        // Also try removing slash-separated prefixes like "系统/子系统"
        String dnShort = dnClean.contains("/") ? dnClean.substring(dnClean.lastIndexOf('/') + 1) : dnClean;

        DocTemplateV2 best = null;
        int bestScore = 0;
        for (DocTemplateV2 t : all) {
            String tn = (t.getTemplateName() != null ? t.getTemplateName() : "").trim();
            // Clean template name too
            String tnClean = tn.replaceAll("\\s*\\(.*?\\)\\s*", "").trim();
            if (tnClean.length() < 2) continue;

            int score = 0;
            // Exact match after cleaning
            if (dnClean.equals(tnClean) || dnShort.equals(tnClean)) {
                score = 100;
            } else if (dnClean.contains(tnClean) && tnClean.length() >= 4) {
                // Doc name contains full template name
                score = 50 + tnClean.length();
            } else if (tnClean.contains(dnClean) && dnClean.length() >= 4) {
                // Template name contains full doc name
                score = 40 + dnClean.length();
            }
            // No partial matching — only high-confidence matches to avoid false positives

            if (score > bestScore) {
                bestScore = score;
                best = t;
            }
        }
        // Require score >= 40 (full name containment) to consider it a match
        return bestScore >= 40 ? best : null;
    }

    private List<Map<String, Object>> parseJsonArray(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String json = raw.trim().replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("JSON parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String cleanLlmOutput(String raw) {
        if (raw == null) return "";
        String result = raw.trim();
        // Remove ALL markdown heading lines (LLM sometimes generates full doc instead of single section)
        result = result.replaceAll("(?m)^#{1,4}\\s+[^\\n]*\\n?", "").trim();
        // Strip leading "null" strings
        result = result.replaceFirst("^(null)+", "").trim();
        return result;
    }
}
