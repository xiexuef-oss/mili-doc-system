package com.military.doc.modules.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.DelegatingLlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.util.LlmOutputCleaner;
import com.military.doc.modules.ai.entity.AiDocument;
import com.military.doc.modules.ai.entity.AiDocumentSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j @Service @RequiredArgsConstructor
public class AiDocumentAgentService {
    private final DelegatingLlmClient llmClient;
    private final PromptTemplateService promptService;
    private final ContextAssemblyService contextAssembly;
    private final AiDocumentSectionService sectionService;
    private final AiDocumentService docService;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[\\s*\\{.*?\\}\\s*\\]", Pattern.DOTALL);
    private static final Pattern JSON_OBJ_PATTERN = Pattern.compile("\\{\\s*\".*?\\}\\s*", Pattern.DOTALL);

    // ──────────────── Outline Generation ────────────────

    /** Generate structured outline from user prompt. */
    public List<CanvasPatch.SectionDTO> generateOutline(AiDocument doc) {
        String system = getPromptTemplate("ai-outline-generation",
            "你是文档大纲生成器。根据用户需求输出JSON数组。每个元素: {title,level,parentId:null}。只输出JSON数组，不要任何说明。");
        String context = "";
        try {
            context = contextAssembly.assembleBasicContext(doc.getProjectId());
        } catch (Exception e) {
            log.warn("Context assembly failed, proceeding without project context: {}", e.getMessage());
        }
        String prompt = "文档类型：" + nvl(doc.getDocumentType()) + "\n用户需求：" + doc.getSourcePrompt() + "\n\n项目背景：\n" + nvl(context);
        String raw = llmClient.chat(system, prompt);  // quickChat: no document context
        raw = cleanLlmOutput(raw);

        try {
            List<CanvasPatch.SectionDTO> outline = parseJsonArray(raw, CanvasPatch.SectionDTO.class);
            if (outline == null || outline.isEmpty()) {
                log.warn("Outline generation returned empty, retrying with stricter prompt");
                raw = llmClient.chatWithAudit(system + "\n必须输出至少5个章节的JSON数组。", prompt, "canvas-gen", doc.getProjectId());
                raw = cleanLlmOutput(raw);
                outline = parseJsonArray(raw, CanvasPatch.SectionDTO.class);
            }
            // Cap at 12 sections max (prevent runaway generation)
            int MAX_SECTIONS = 12;
            if (outline != null) {
                if (outline.size() > MAX_SECTIONS) {
                    log.warn("Outline too large ({}), truncating to {}", outline.size(), MAX_SECTIONS);
                    outline = outline.subList(0, MAX_SECTIONS);
                }
                for (int i = 0; i < outline.size(); i++) {
                    if (outline.get(i).getSortOrder() == 0) outline.get(i).setSortOrder(i + 1);
                    if (outline.get(i).getLevel() == 0) outline.get(i).setLevel(1);
                    outline.get(i).setStatus("empty");
                }
                return outline;
            }
        } catch (Exception e) {
            log.error("Outline parse failed: {}", e.getMessage());
        }
        return List.of();
    }

    // ──────────────── Section Content Generation ────────────────

    /** Generate content for a single section. */
    public String generateSection(AiDocument doc, AiDocumentSection section, List<AiDocumentSection> allSections) {
        String system = getPromptTemplate("ai-section-generation",
            "你是军工文档撰写专家。请为指定章节撰写正文。只输出正文内容，不要标题。使用规范的军工文档术语。");
        StringBuilder ctx = new StringBuilder();
        ctx.append("文档标题：").append(doc.getTitle()).append("\n");
        ctx.append("文档描述：").append(nvl(doc.getDescription())).append("\n");
        ctx.append("完整大纲：\n");
        for (AiDocumentSection s : allSections) {
            String indent = "  ".repeat(Math.max(0, s.getLevel() - 1));
            ctx.append(indent).append(s.getLevel()).append(". ").append(s.getTitle()).append("\n");
        }
        ctx.append("\n当前章节：").append(section.getTitle());
        ctx.append("\n请撰写本章节正文，篇幅300-800字。直接输出正文，不要任何解释。");

        String result = llmClient.chatWithAudit(system, ctx.toString(), "canvas-gen", doc.getProjectId());
        result = cleanLlmOutput(result);
        return (result != null && !result.isBlank()) ? result : "";
    }

    // ──────────────── Streaming All Sections ────────────────

    /** Generate all sections with SSE streaming (sequential, ordered). */
    public void generateAllSections(AiDocument doc, Consumer<CanvasPatch.Patch> onPatch, Consumer<String> onMessage) {
        onMessage.accept("开始撰写文档...");
        docService.updateStatus(doc.getId(), "generating");
        onPatch.accept(patch("set_document_status", Map.of("status", "generating")));

        String system = getPromptTemplate("ai-section-generation",
            "你是军工文档撰写专家。根据文档主题和已写内容，输出下一章：先输出章节标题（## 开头），再输出正文。");

        // Build the initial context
        StringBuilder writtenSoFar = new StringBuilder();
        writtenSoFar.append("文档标题：").append(doc.getTitle()).append("\n");
        writtenSoFar.append("文档类型：").append(nvl(doc.getDocumentType())).append("\n");
        writtenSoFar.append("用户需求：").append(nvl(doc.getSourcePrompt())).append("\n\n");
        
        String context = "";
        try { context = contextAssembly.assembleBasicContext(doc.getProjectId()); } catch(Exception ignored) {}
        if (!context.isBlank()) writtenSoFar.append("项目背景：").append(context).append("\n\n");
        
        int MAX_CHAPTERS = 10;
        int sortOrder = 0;
        int completed = 0, failed = 0;
        
        for (int i = 0; i < MAX_CHAPTERS; i++) {
            onMessage.accept("正在写第" + (i+1) + "章...");
            
            // Build prompt: tell the LLM what's been written and ask for the next chapter
            String prompt;
            if (i == 0) {
                prompt = writtenSoFar.toString() + "请开始写第一章（一级标题），包含章节标题和正文。格式：\n## 章节标题\n正文内容...";
            } else {
                prompt = writtenSoFar.toString() + "\n\n以上是已写内容。请继续写下一章（一级标题），如果文档已经完整可以写## END 表示结束。\n格式：\n## 章节标题\n正文内容...";
            }
            
            String raw = llmClient.chat(system, prompt);  // quickChat: no document context
            raw = cleanLlmOutput(raw);
            if (raw == null || raw.isBlank() || raw.contains("## END")) {
                onMessage.accept("文档撰写完成");
                break;
            }
            
            // Parse: extract title (## line) and content (rest)
            String title = "";
            String content = raw;
            if (raw.startsWith("##")) {
                int nl = raw.indexOf("\n");
                if (nl > 0) {
                    title = raw.substring(2, nl).trim();
                    content = raw.substring(nl + 1).trim();
                } else {
                    title = raw.substring(2).trim();
                    content = "";
                }
            }
            if (title.isBlank()) title = "第" + (i+1) + "章";
            
            // Persist as section
            sortOrder++;
            AiDocumentSection sec = new AiDocumentSection();
            sec.setDocumentId(doc.getId());
            sec.setTitle(title);
            sec.setLevel(1);
            sec.setSortOrder(sortOrder);
            sec.setStatus("ready");
            sec.setContent(content);
            sec.setCreatedBy(doc.getUserId());
            sec.setUpdatedBy(doc.getUserId());
            sectionService.getByDocumentId(doc.getId()); // just to access mapper
            
            // Insert via raw approach
            try {
                // Use AiDocumentSectionService.addAfter if possible, else direct insert
                AiDocumentSection lastSec = null;
                List<AiDocumentSection> existing = sectionService.getByDocumentId(doc.getId());
                if (!existing.isEmpty()) lastSec = existing.get(existing.size()-1);
                sectionService.addAfter(doc.getId(), title, 1, lastSec != null ? lastSec.getId() : null, null, doc.getUserId());
                
                // Update content on the newly inserted section
                List<AiDocumentSection> updated = sectionService.getByDocumentId(doc.getId());
                AiDocumentSection inserted = updated.get(updated.size()-1);
                sectionService.updateContent(inserted.getId(), content, null);
                inserted.setContent(content);
                
                // Send patches
                onPatch.accept(patch("add_section", Map.of(
                    "section", Map.of(
                        "id", inserted.getId(),
                        "title", title,
                        "level", 1,
                        "sortOrder", sortOrder,
                        "content", content,
                        "status", "ready",
                        "documentId", doc.getId()
                    ))));
                
                // Append to written context
                writtenSoFar.append("## ").append(title).append("\n").append(content).append("\n\n");
                completed++;
            } catch (Exception e) {
                log.warn("Chapter {} failed: {}", i, e.getMessage());
                failed++;
            }
        }

        String finalStatus = completed == 0 ? "failed" : "ready";
        docService.updateStatus(doc.getId(), finalStatus);
        onPatch.accept(patch("set_document_status", Map.of("status", finalStatus)));
        onMessage.accept(String.format("生成完成：%d成功，%d失败", completed, failed));
    }

    // ──────────────── Section AI Edit ────────────────

    /** AI-edit a section with a specific mode. */
    public String editSection(Long sectionId, String mode, String instruction) {
        AiDocumentSection sec = findSectionById(sectionId);
        if (sec == null) return null;

        String system = getPromptTemplate("ai-section-edit",
            "你是文档编辑专家。根据用户指令编辑章节内容。\n操作模式：rewrite=完全重写 expand=扩充细节 shorten=精简 polish=优化表达 custom=按指令执行\n只输出编辑后的正文，不要任何解释。");

        StringBuilder prompt = new StringBuilder();
        prompt.append("章节标题：").append(sec.getTitle()).append("\n");
        prompt.append("操作模式：").append(mode).append("\n");
        prompt.append("编辑指令：").append(nvl(instruction)).append("\n\n");
        prompt.append("当前内容：\n").append(nvl(sec.getContent()));

        String result = llmClient.chat(system, prompt.toString());
        result = cleanLlmOutput(result);
        return result;
    }

    // ──────────────── Intent Recognition & Operation Planning ────────────────

    /** Handle chat instruction: intent recognition -> operation execution. */
    public List<CanvasPatch.Patch> handleChatInstruction(AiDocument doc, String message, Long userId) {
        List<AiDocumentSection> secs = sectionService.getByDocumentId(doc.getId());

        // Step 1: Intent recognition
        String system = getPromptTemplate("ai-intent-recognition",
            "分析用户对文档的操作意图。输出JSON(只输出JSON，不要任何说明):\n" +
            "{\"action\":\"<edit_section|add_section|delete_section|rename_section|rewrite_all|generate_outline|summarize|qa>\"," +
            "\"target\":\"目标章节标题或编号\"," +
            "\"instruction\":\"具体操作描述\"," +
            "\"reasoning\":\"简短推理\"}");

        String context = buildSectionContext(doc, secs);
        String raw = llmClient.chatWithAudit(system, context + "\n用户消息：" + message, "canvas-gen", doc.getProjectId());
        raw = cleanLlmOutput(raw);

        try {
            Map<String, Object> intent = parseJsonObject(raw);
            if (intent == null || !intent.containsKey("action")) {
                log.warn("Intent recognition returned null, raw: {}", raw.substring(0, Math.min(200, raw.length())));
                return List.of();
            }

            String action = (String) intent.get("action");
            String target = (String) intent.get("target");
            String instruction = (String) intent.getOrDefault("instruction", message);
            log.info("Intent: action={}, target={}", action, target);

            return executeAction(doc, secs, action, target, instruction, userId);

        } catch (Exception e) {
            log.error("Intent parse failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<CanvasPatch.Patch> executeAction(AiDocument doc, List<AiDocumentSection> secs,
                                                    String action, String target, String instruction, Long userId) {
        return switch (action) {
            case "edit_section" -> handleEditSection(doc, secs, target, instruction, userId);
            case "add_section" -> handleAddSection(doc, secs, target, instruction, userId);
            case "delete_section" -> handleDeleteSection(doc, secs, target, userId);
            case "rename_section" -> handleRenameSection(doc, secs, target, instruction);
            case "rewrite_all" -> handleRewriteAll(doc, userId);
            case "generate_outline" -> handleRegenerateOutline(doc, userId);
            case "summarize" -> handleSummarize(doc, secs);
            default -> handleQA(doc, secs, instruction);
        };
    }

    private List<CanvasPatch.Patch> handleEditSection(AiDocument doc, List<AiDocumentSection> secs,
                                                       String target, String instruction, Long userId) {
        Long sid = findSectionId(secs, target);
        if (sid == null) return List.of();
        docService.createVersion(doc.getId(), userId, "ai-edit");
        String content = editSection(sid, "custom", instruction);
        if (content != null && !content.isBlank()) {
            sectionService.updateContent(sid, content, null);
            return List.of(patch("update_section_content",
                Map.of("sectionId", sid, "content", content)));
        }
        return List.of();
    }

    private List<CanvasPatch.Patch> handleAddSection(AiDocument doc, List<AiDocumentSection> secs,
                                                       String target, String instruction, Long userId) {
        Long afterId = target != null ? findSectionId(secs, target) : null;
        String title = instruction != null && instruction.length() < 50 ? instruction : "新章节";
        AiDocumentSection newSec = sectionService.addAfter(doc.getId(), title, 1, afterId, null, userId);
        return List.of(patch("add_section", Map.of(
            "section", Map.of("id", newSec.getId(), "title", title, "level", 1,
                "sortOrder", newSec.getSortOrder(), "status", "empty", "documentId", doc.getId()))));
    }

    private List<CanvasPatch.Patch> handleDeleteSection(AiDocument doc, List<AiDocumentSection> secs,
                                                          String target, Long userId) {
        Long sid = findSectionId(secs, target);
        if (sid == null) return List.of();
        docService.createVersion(doc.getId(), userId, "delete-section");
        sectionService.delete(sid);
        return List.of(patch("delete_section", Map.of("sectionId", sid)));
    }

    private List<CanvasPatch.Patch> handleRenameSection(AiDocument doc, List<AiDocumentSection> secs,
                                                          String target, String instruction) {
        Long sid = findSectionId(secs, target);
        if (sid == null || instruction == null) return List.of();
        sectionService.rename(sid, instruction);
        return List.of(patch("rename_section", Map.of("sectionId", sid, "title", instruction)));
    }

    private List<CanvasPatch.Patch> handleRewriteAll(AiDocument doc, Long userId) {
        docService.createVersion(doc.getId(), userId, "rewrite-all");
        // Clear all content markers
        List<AiDocumentSection> secs = sectionService.getByDocumentId(doc.getId());
        for (AiDocumentSection s : secs) {
            sectionService.updateStatus(s.getId(), "empty");
        }
        docService.updateStatus(doc.getId(), "generating");
        return List.of(patch("set_document_status", Map.of("status", "generating")));
    }

    private List<CanvasPatch.Patch> handleRegenerateOutline(AiDocument doc, Long userId) {
        docService.createVersion(doc.getId(), userId, "regenerate-outline");
        return List.of(patch("set_document_status", Map.of("status", "generating")));
    }

    private List<CanvasPatch.Patch> handleSummarize(AiDocument doc, List<AiDocumentSection> secs) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 文档摘要：《").append(doc.getTitle()).append("》\n\n");
        for (AiDocumentSection s : secs) {
            if (s.getContent() != null && !s.getContent().isBlank()) {
                sb.append("### ").append(s.getTitle()).append("\n");
                String summary = s.getContent().length() > 200
                    ? s.getContent().substring(0, 200) + "..."
                    : s.getContent();
                sb.append(summary).append("\n\n");
            }
        }
        String system = "你是文档摘要专家。为以下文档生成简洁摘要（100-300字）。";
        String result = llmClient.chatWithAudit(system, sb.toString(), "canvas-gen", doc.getProjectId());
        return List.of(patch("set_document_title", Map.of("title", doc.getTitle())),
            patch("set_outline", Map.of("sections", List.of(
                Map.of("id", -1L, "title", "摘要", "level", 1, "sortOrder", 0,
                    "content", cleanLlmOutput(result), "status", "ready", "documentId", doc.getId())))));
    }

    private List<CanvasPatch.Patch> handleQA(AiDocument doc, List<AiDocumentSection> secs, String instruction) {
        // General Q&A - no patch, just return empty (handled by controller for message)
        return List.of();
    }

    // ──────────────── Helpers ────────────────

    private AiDocumentSection findSectionById(Long id) {
        if (id == null) return null;
        return sectionService.getByDocumentId(id).stream()
            .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    private Long findSectionId(List<AiDocumentSection> secs, String target) {
        if (target == null || target.isBlank()) return null;
        // Exact title match
        for (AiDocumentSection s : secs) {
            if (s.getTitle().equals(target)) return s.getId();
        }
        // Substring match
        for (AiDocumentSection s : secs) {
            if (s.getTitle().contains(target) || target.contains(s.getTitle())) return s.getId();
        }
        // Numeric index: "第一章" -> index 0
        try {
            String digits = target.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                int n = Integer.parseInt(digits);
                if (n > 0 && n <= secs.size()) return secs.get(n - 1).getId();
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private String buildSectionContext(AiDocument doc, List<AiDocumentSection> secs) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("文档标题：").append(doc.getTitle()).append("\n");
        ctx.append("文档状态：").append(nvl(doc.getStatus())).append("\n");
        ctx.append("章节列表（共").append(secs.size()).append("章）：\n");
        for (AiDocumentSection s : secs) {
            ctx.append("  [").append(s.getId()).append("] Level").append(s.getLevel())
               .append(" ").append(s.getTitle())
               .append(" (status:").append(nvl(s.getStatus())).append(")");
            if (s.getContent() != null && !s.getContent().isBlank())
                ctx.append(" [已填写]");
            ctx.append("\n");
        }
        return ctx.toString();
    }

    // ──────────────── JSON Parsing ────────────────

    /** Parse LLM output to a typed list, with multiple fallback strategies. */
    private <T> List<T> parseJsonArray(String raw, Class<T> itemClass) {
        if (raw == null || raw.isBlank()) return null;
        try {
            // Strategy 1: Try direct parse if it's already clean JSON
            String json = LlmOutputCleaner.extractBetween(raw, '[', ']', true);
            return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, itemClass));
        } catch (Exception e1) {
            try {
                // Strategy 2: Try as a JSON object with "sections" or "outline" key
                String json = LlmOutputCleaner.extractBetween(raw, '{', '}', true);
                Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                Object list = map.getOrDefault("sections", map.getOrDefault("outline", map.get("chapters")));
                if (list instanceof List<?> l) {
                    return l.stream().map(item -> objectMapper.convertValue(item, itemClass)).toList();
                }
            } catch (Exception e2) {
                log.warn("JSON array parse fallback failed: {}", e2.getMessage());
            }
            return null;
        }
    }

    /** Parse LLM output to a Map. */
    private Map<String, Object> parseJsonObject(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String json = LlmOutputCleaner.extractBetween(raw, '{', '}', true);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON object parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String cleanLlmOutput(String raw) {
        return LlmOutputCleaner.clean(raw);
    }

    private CanvasPatch.Patch patch(String type, Object payload) {
        CanvasPatch.Patch p = new CanvasPatch.Patch();
        p.setType(type);
        p.setPayload(payload);
        return p;
    }

    private String getPromptTemplate(String name, String fallback) {
        String tpl = promptService.getTemplate(name);
        return (tpl != null && !tpl.isBlank()) ? tpl : fallback;
    }

    /** Quick chat without document context (for selection edits). */
    public String quickChat(String system, String prompt) {
        String raw = llmClient.chat(system, prompt);  // quickChat: no document context
        return cleanLlmOutput(raw);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
