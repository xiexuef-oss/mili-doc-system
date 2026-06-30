package com.military.doc.modules.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.service.DraftGenerationService;
import com.military.doc.modules.chat.entity.*;
import com.military.doc.modules.chat.mapper.*;
import com.military.doc.modules.document.entity.*;
import com.military.doc.modules.document.mapper.*;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 对话式文档生成 Agent。
 * 四层记忆驱动的自主规划引擎。
 */
@Slf4j
@Service
public class ChatAgentService {

    private final LlmClient llmClient;
    private final com.military.doc.ai.prompt.PromptTemplateService promptTemplateService;
    private final com.military.doc.modules.template.mapper.DocTemplateChapterMapper tplChapterMapper;
    private final com.military.doc.modules.document.mapper.ProjectDocChecklistMapper pdcMapper;
    private final com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper sctMapper;
    private final ProjectMapper projectMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final DocChapterMapper docChapterMapper;
    private final StageDocChecklistTemplateMapper checklistTplMapper;
    private final ProjectDocChecklistMapper checklistMapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final ContextAssemblyService contextAssemblyService;
    private final DraftGenerationService draftGenerationService;
    private final ChatSessionMapper sessionMapper;
    private final ProjectFeedbackMapper feedbackMapper;
    private final ProjectTerminologyMapper terminologyMapper;
    private final GenerationExemplarMapper exemplarMapper;
    private final ObjectMapper objectMapper;

    private static final String INTENT_CLASSIFY =
        "分析用户意图，只输出JSON: {\"intent\":\"qa|generate|modify|list|check\",\"docName\":\"\"}。" +
        "generate=生成/编写/创建新文档。modify=修改/编辑/更新已有文档。list=列出/查看文档清单。check=检查质量/预检。qa=其他问题。";

    private static final String AGENT_PROMPT =
        "你是军工文档编制助手。用户要求生成或修改文档时，用行动块：" +
        "生成: ```action\n{\"type\":\"generate_doc\",\"docName\":\"文档名\"}\n```" +
        "修改: ```action\n{\"type\":\"modify_doc\",\"docName\":\"文档名\",\"chapter\":\"章节号\",\"instruction\":\"修改指令\"}\n```" +
        "其他问题直接回答。用中文，简洁专业。";

    private static final String QA_PROMPT =
        "你是军工文档编制助手，精通 GJB 军用标准体系。" +
        "根据提供的项目上下文（模板结构、标准条款、知识库文章、主数据等）回答用户问题。" +
        "引用具体标准编号和条款。用中文，专业但不啰嗦。";

    public ChatAgentService(LlmClient llmClient, ProjectMapper projectMapper,
                             DocLedgerMapper docLedgerMapper, DocChapterMapper docChapterMapper,
                             StageDocChecklistTemplateMapper checklistTplMapper,
                             ProjectDocChecklistMapper checklistMapper,
                             DocTemplateChapterMapper templateChapterMapper,
                             ContextAssemblyService contextAssemblyService,
                             com.military.doc.ai.prompt.PromptTemplateService promptTemplateService,
                             com.military.doc.modules.template.mapper.DocTemplateChapterMapper tplChapterMapper,
                             com.military.doc.modules.document.mapper.ProjectDocChecklistMapper pdcMapper,
                             com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper sctMapper,
                             DraftGenerationService draftGenerationService,
                             ChatSessionMapper sessionMapper,
                             ProjectFeedbackMapper feedbackMapper,
                             ProjectTerminologyMapper terminologyMapper,
                             GenerationExemplarMapper exemplarMapper,
                             ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.projectMapper = projectMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.docChapterMapper = docChapterMapper;
        this.checklistTplMapper = checklistTplMapper;
        this.checklistMapper = checklistMapper;
        this.templateChapterMapper = templateChapterMapper;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.tplChapterMapper = tplChapterMapper;
        this.pdcMapper = pdcMapper;
        this.sctMapper = sctMapper;
        this.draftGenerationService = draftGenerationService;
        this.sessionMapper = sessionMapper;
        this.feedbackMapper = feedbackMapper;
        this.terminologyMapper = terminologyMapper;
        this.exemplarMapper = exemplarMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Process a user message and return agent response.
     * This is the main entry point for the chat-based document generation.
     */
    public Map<String, Object> chat(Long projectId, String sessionId, String userMessage,
                                     Long userId, Consumer<String> onProgress) {
        // L1: Load project basics (lightweight — just name and type)
        Project project = projectMapper.selectById(projectId);
        if (project == null) return Map.of("error", "项目不存在: " + projectId);

        // L2: Load session history
        ChatSession session = loadOrCreateSession(projectId, sessionId, userId);

        // Semantic intent classification via lightweight LLM call
        // Fast intent detection: match document names from checklist + keywords
        boolean isGen = userMessage.matches(".*(生成|编写|创建|Generate|generate|创建|起草|写一份|写一个).*");
        if (!isGen) {
            // Check if message contains a document name from the checklist
            var checklist = checklistMapper.selectList(
                new LambdaQueryWrapper<ProjectDocChecklist>()
                    .eq(ProjectDocChecklist::getProjectId, projectId)
                    .eq(ProjectDocChecklist::getIsCustom, false));
            for (var item : checklist) {
                if (item.getDocName() != null && userMessage.contains(item.getDocName())) {
                    isGen = true; break;
                }
            }
        }

        if (isGen) {
            return handleGenerateRequest(projectId, project, session, userMessage, userId, onProgress);
        } else {
            return handleQA(projectId, project, session, userMessage, userId, onProgress);
        }
    }

    private Map<String, Object> handleQA(Long projectId, Project project, ChatSession session,
                                          String userMessage, Long userId, Consumer<String> onProgress) {
        // 注入完整三库上下文：模板+标准+知识库+主数据
        String projectContext = contextAssemblyService.assembleContext(projectId);
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 当前项目\n");
        prompt.append("- 名称: ").append(project.getProjectName()).append("\n");
        prompt.append("- 类型: ").append(project.getProjectType()).append("\n\n");
        if (projectContext != null && !projectContext.isBlank()) {
            prompt.append(projectContext).append("\n\n");
        }
        prompt.append("## 用户问题\n").append(userMessage);
        onProgress.accept("AI 正在思考...");
        var buf = new StringBuilder();
        llmClient.chatStream(QA_PROMPT, prompt.toString(), chunk -> { buf.append(chunk); onProgress.accept(chunk); });
        String response = buf.toString();

        appendMessage(session, "user", userMessage);
        appendMessage(session, "assistant", response);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("response", response);
        result.put("sessionId", session.getSessionId());
        result.put("actions", List.of());
        return result;
    }

    private Map<String, Object> handleGenerateRequest(Long projectId, Project project,
                                                        ChatSession session, String userMessage,
                                                        Long userId, Consumer<String> onProgress) {
        // Full context for generation
        String projectContext = contextAssemblyService.assembleContext(projectId);
        List<DocLedger> existingDocs = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>().eq(DocLedger::getProjectId, projectId)
                .orderByAsc(DocLedger::getDocName));
        List<ProjectDocChecklist> checklist = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getIsCustom, false));
        List<ProjectFeedback> feedbacks = feedbackMapper.selectList(
            new LambdaQueryWrapper<ProjectFeedback>().eq(ProjectFeedback::getProjectId, projectId)
                .orderByDesc(ProjectFeedback::getCreatedAt).last("LIMIT 5"));
        List<ProjectTerminology> terms = terminologyMapper.selectList(
            new LambdaQueryWrapper<ProjectTerminology>().eq(ProjectTerminology::getProjectId, projectId));
        List<GenerationExemplar> exemplars = exemplarMapper.selectList(
            new LambdaQueryWrapper<GenerationExemplar>().orderByDesc(GenerationExemplar::getQualityScore)
                .last("LIMIT 3"));

        onProgress.accept("正在分析项目上下文...");
        String prompt = buildAgentPrompt(project, projectContext, existingDocs, checklist,
            session, feedbacks, terms, exemplars, userMessage);

        onProgress.accept("AI 正在思考...");
        var responseBuf = new StringBuilder();
        llmClient.chatStream(AGENT_PROMPT, prompt, chunk -> {
            responseBuf.append(chunk);
            onProgress.accept(chunk);
        });
        String response = responseBuf.toString();

        Map<String, Object> result = parseAndExecute(projectId, session, response, userId, onProgress);

        appendMessage(session, "user", userMessage);
        appendMessage(session, "assistant", response);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return result;
    }

    /**
     * One-click generate all required documents for a project stage.
     */
    public Map<String, Object> generateAllDocs(Long projectId, Long stageId, Long userId) {
        List<ProjectDocChecklist> items = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getStageId, stageId)
                .eq(ProjectDocChecklist::getIsCustom, false));
        if (items.isEmpty()) return Map.of("error", "该阶段无文档清单");

        // Find top-priority docs (必编 + 在最早阶段编制的)
        List<ProjectDocChecklist> priority = items.stream()
            .filter(i -> i.getTemplateId() != null)
            .limit(5).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", items.size());
        result.put("generating", priority.size());
        List<Map<String, Object>> generated = new ArrayList<>();

        // Batch: collect all template IDs
        Set<Long> tplIds = new HashSet<>();
        for (var item : priority) {
            if (item.getTemplateId() != null) {
                tplIds.add(item.getTemplateId());
            }
        }
        Map<Long, StageDocChecklistTemplate> tplMap = new HashMap<>();
        if (!tplIds.isEmpty()) {
            List<StageDocChecklistTemplate> allTmpls = checklistTplMapper.selectBatchIds(tplIds);
            for (StageDocChecklistTemplate tmpl : allTmpls) {
                if (tmpl != null) tplMap.put(tmpl.getId(), tmpl);
            }
        }

        for (var item : priority) {
            var sct = tplMap.get(item.getTemplateId());
            if (sct == null || sct.getTemplateId() == null) continue;
            // Find or create ledger
            DocLedger ledger = findOrCreateLedger(projectId, stageId, item, userId);
            // Generate content
            String content = draftGenerationService.generate(projectId, null, ledger.getId());
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("docLedgerId", ledger.getId());
            doc.put("docName", item.getDocName());
            doc.put("status", content != null && !content.isBlank() ? "generated" : "failed");
            doc.put("chars", content != null ? content.length() : 0);
            generated.add(doc);
        }
        result.put("docs", generated);
        return result;
    }

    /**
     * Quality check report for a project.
     */
    public Map<String, Object> qualityReport(Long projectId, Long stageId) {
        List<DocLedger> ledgers = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(stageId != null, DocLedger::getStageId, stageId));

        int total = ledgers.size();
        int withContent = 0, xxxCount = 0;
        List<Map<String, Object>> issues = new ArrayList<>();

        for (var l : ledgers) {
            if (l.getContentSize() != null && l.getContentSize() > 0) withContent++;
            var chapters = docChapterMapper.selectList(
                new LambdaQueryWrapper<DocChapter>()
                    .eq(DocChapter::getDocLedgerId, l.getId()).eq(DocChapter::getDeleted, 0));
            for (var ch : chapters) {
                if (ch.getContent() != null) {
                    int xxx = countOccurrences(ch.getContent(), "XXX");
                    xxxCount += xxx;
                    if (xxx > 0) {
                        issues.add(Map.of("docName", l.getDocName(), "chapter", ch.getChapterTitle(),
                            "type", "XXX_PLACEHOLDER", "count", xxx));
                    }
                } else {
                    issues.add(Map.of("docName", l.getDocName(), "chapter", ch.getChapterTitle(),
                        "type", "EMPTY_CHAPTER", "count", 1));
                }
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalDocs", total);
        report.put("docsWithContent", withContent);
        report.put("completionRate", total > 0 ? Math.round(100.0 * withContent / total) : 0);
        report.put("xxxPlaceholders", xxxCount);
        report.put("issues", issues.stream().limit(20).toList());
        return report;
    }

    // === Private helpers ===

    private ChatSession loadOrCreateSession(Long projectId, String sessionId, Long userId) {
        var existing = sessionMapper.selectList(
            new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId));
        if (!existing.isEmpty()) return existing.get(0);
        ChatSession s = new ChatSession();
        s.setProjectId(projectId); s.setSessionId(sessionId);
        s.setTitle("新会话"); s.setMessages("[]"); s.setGeneratedDocs("[]");
        s.setStatus("ACTIVE"); s.setCreatedBy(userId);
        s.setCreatedAt(LocalDateTime.now()); s.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(s);
        return s;
    }

    private void appendMessage(ChatSession session, String role, String content) {
        try {
            var msgs = objectMapper.readValue(session.getMessages(), new TypeReference<List<Map<String,Object>>>(){});
            msgs.add(Map.of("role", role, "content", content, "timestamp", LocalDateTime.now().toString()));
            // Keep last 50 messages
            if (msgs.size() > 50) msgs = msgs.subList(msgs.size() - 50, msgs.size());
            session.setMessages(objectMapper.writeValueAsString(msgs));
        } catch (Exception e) { log.warn("Failed to append message: {}", e.getMessage()); }
    }

    private String buildAgentPrompt(Project project, String projectContext,
                                     List<DocLedger> existingDocs,
                                     List<ProjectDocChecklist> checklist,
                                     ChatSession session,
                                     List<ProjectFeedback> feedbacks,
                                     List<ProjectTerminology> terms,
                                     List<GenerationExemplar> exemplars,
                                     String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 当前项目\n");
        sb.append("- 名称: ").append(project.getProjectName()).append("\n");
        sb.append("- 类型: ").append(project.getProjectType()).append("\n");

        // 项目输入文件 + 标准条款 + 知识库 + 主数据（全量传递，依赖 LLM 长上下文窗口）
        if (projectContext != null && !projectContext.isBlank()) {
            sb.append("\n## 项目上下文（模板/标准/知识库/主数据）\n");
            sb.append(projectContext).append("\n");
        }

        // Existing documents
        if (!existingDocs.isEmpty()) {
            sb.append("\n## 已生成/已有的文档\n");
            for (var doc : existingDocs) {
                sb.append("- ").append(doc.getDocName())
                    .append(" [").append(doc.getLifecycleStatus() != null ? doc.getLifecycleStatus() : "?").append("]");
                if (doc.getContentSize() != null && doc.getContentSize() > 0)
                    sb.append(" (").append(doc.getContentSize()).append("字)");
                sb.append("\n");
            }
        }

        // Document checklist
        if (!checklist.isEmpty()) {
            sb.append("\n## 文档清单（前20条）\n");
            checklist.stream().limit(20).forEach(c ->
                sb.append("- ").append(c.getDocName())
                    .append(" [").append(c.getCategoryCode()).append("]\n"));
            if (checklist.size() > 20) sb.append("...还有 ").append(checklist.size() - 20).append(" 条\n");
        }

        // L3: Recent feedback
        if (!feedbacks.isEmpty()) {
            sb.append("\n## 历史修改记录（请学习这些修改模式）\n");
            for (var f : feedbacks) {
                sb.append("- 用户修改了 '").append(f.getOriginalContent() != null ? f.getOriginalContent().substring(0, Math.min(50, f.getOriginalContent().length())) : "").append("...' → '")
                    .append(f.getCorrectedContent() != null ? f.getCorrectedContent().substring(0, Math.min(50, f.getCorrectedContent().length())) : "").append("...' (")
                    .append(f.getFeedbackType()).append(")\n");
            }
        }

        // L3: Terminology
        if (!terms.isEmpty()) {
            sb.append("\n## 项目术语偏好\n");
            for (var t : terms) {
                sb.append("- '").append(t.getTerm()).append("' → '").append(t.getPreferred()).append("'\n");
            }
        }

        // L4: Exemplars
        if (!exemplars.isEmpty()) {
            sb.append("\n## 优质范例参考\n");
            for (var ex : exemplars) {
                sb.append("- ").append(ex.getChapterTitle()).append(": ")
                    .append(ex.getContent().substring(0, Math.min(100, ex.getContent().length())))
                    .append("...\n");
            }
        }

        sb.append("\n## 用户消息\n").append(userMessage);
        return sb.toString();
    }

    private Map<String, Object> parseAndExecute(Long projectId, ChatSession session,
                                                   String response, Long userId,
                                                   Consumer<String> onProgress) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getSessionId());

        // Extract action blocks
        var actions = extractActions(response);
        List<Map<String, Object>> results = new ArrayList<>();

        for (var action : actions) {
            String type = (String) action.getOrDefault("type", "");
            String docName = (String) action.getOrDefault("docName", "");
            Map<String, Object> actionResult = new LinkedHashMap<>();
            actionResult.put("type", type);

            try {
                switch (type) {
                    case "generate_doc":
                        if (onProgress != null) onProgress.accept("正在为「" + docName + "」生成初稿...");
                        var doc = executeGenerateDoc(projectId, docName, userId,
                            step -> { if (onProgress != null) onProgress.accept(step); });
                        if (doc != null && onProgress != null) onProgress.accept("「" + docName + "」生成完成");
                        actionResult.put("success", doc != null);
                        if (doc != null) {
                            actionResult.put("docLedgerId", doc.getId());
                            actionResult.put("docName", doc.getDocName());
                            actionResult.put("status", doc.getLifecycleStatus());
                            actionResult.put("contentSize", doc.getContentSize());
                            // Track in session
                            appendGeneratedDoc(session, doc);
                        }
                        break;
                    case "list_docs":
                        var docs = docLedgerMapper.selectList(
                            new LambdaQueryWrapper<DocLedger>()
                                .eq(DocLedger::getProjectId, projectId).last("LIMIT 20"));
                        actionResult.put("count", docs.size());
                        actionResult.put("docs", docs.stream().map(d -> Map.of(
                            "id", d.getId(), "name", d.getDocName(),
                            "status", d.getLifecycleStatus())).toList());
                        break;
                    case "quality_check":
                        actionResult.putAll(qualityReport(projectId, null));
                        break;
                    default:
                        actionResult.put("success", false);
                        actionResult.put("error", "Unknown action: " + type);
                }
            } catch (Exception e) {
                actionResult.put("success", false);
                actionResult.put("error", e.getMessage());
            }
            results.add(actionResult);
        }

        result.put("response", cleanResponse(response));
        result.put("actions", results);
        return result;
    }

    /** Parse ```action JSON blocks from agent response. */
    private List<Map<String, Object>> extractActions(String response) {
        List<Map<String, Object>> actions = new ArrayList<>();
        int idx = 0;
        while ((idx = response.indexOf("```action", idx)) != -1) {
            int start = response.indexOf("\n", idx) + 1;
            int end = response.indexOf("```", start);
            if (end == -1) break;
            String json = response.substring(start, end).trim();
            try {
                @SuppressWarnings("unchecked")
                var map = (Map<String, Object>) objectMapper.readValue(json, Map.class);
                actions.add(map);
            } catch (Exception e) { log.warn("Failed to parse action: {}", json); }
            idx = end + 3;
        }
        return actions;
    }

    /** Remove action blocks from display text. */
    private String cleanResponse(String response) {
        return response.replaceAll("```action\\n.*?```\\n?", "").trim();
    }

    /** Actually generate a document by finding it in the checklist, creating ledger, and calling AI. */
    private DocLedger executeGenerateDoc(Long projectId, String docName, Long userId) {
        return executeGenerateDoc(projectId, docName, userId, null);
    }

    private DocLedger executeGenerateDoc(Long projectId, String docName, Long userId,
                                          Consumer<String> onProgress) {
        // Find in checklist — must exist in current stage
        var items = checklistMapper.selectList(
            new LambdaQueryWrapper<ProjectDocChecklist>()
                .eq(ProjectDocChecklist::getProjectId, projectId)
                .eq(ProjectDocChecklist::getIsCustom, false));
        ProjectDocChecklist matched = null;
        for (var item : items) {
            if (item.getDocName() != null && item.getDocName().contains(docName)) {
                matched = item; break;
            }
        }
        if (matched == null && !items.isEmpty()) {
            // Fuzzy match
            for (var item : items) {
                if (item.getDocName() != null && docName.contains(item.getDocName().substring(0, Math.min(4, item.getDocName().length())))) {
                    matched = item; break;
                }
            }
        }
        if (matched == null) {
            log.warn("Doc '{}' not in checklist for project {}", docName, projectId);
            if (onProgress != null) onProgress.accept("文档「" + docName + "」不在当前阶段清单中，请确认文档名称或先添加到清单。");
            return null;
        }
        Long stageId = matched.getStageId();
        Long checklistItemId = matched.getId();

        // Find or create ledger
        DocLedger ledger;
        var existing = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .like(DocLedger::getDocName, docName).last("LIMIT 1"));
        if (!existing.isEmpty()) {
            ledger = existing.get(0);
        } else {
            ledger = new DocLedger();
            ledger.setProjectId(projectId);
            ledger.setStageId(stageId);
            ledger.setDocName(docName);
            ledger.setDocType("GENERATED");
            ledger.setLifecycleStatus("DRAFTING");
            ledger.setRequiredFlag(true);
            ledger.setChecklistItemId(checklistItemId);
            ledger.setCreatedBy(userId); ledger.setUpdatedBy(userId);
            ledger.setCreatedAt(LocalDateTime.now()); ledger.setUpdatedAt(LocalDateTime.now());
            docLedgerMapper.insert(ledger);
        }

        // Use streaming generation (reliable, original approach)
        if (onProgress != null) onProgress.accept("正在生成「" + docName + "」...");
        var contentBuf = new StringBuilder();
        draftGenerationService.generateStream(projectId, null, ledger.getId(), chunk -> {
            contentBuf.append(chunk);
            if (onProgress != null && contentBuf.length() % 200 < 100) {
                onProgress.accept("已生成 " + contentBuf.length() + " 字...");
            }
        }, (c, t, title, chars) -> {});
        String content = contentBuf.toString();
        if (onProgress != null && content != null && !content.isEmpty()) {
            onProgress.accept("生成完成，" + content.length() + " 字，正在保存...");
        }
        if (content != null && !content.isBlank()) {
            ledger.setContentSize((long) content.length());
            ledger.setLifecycleStatus("DRAFTING");
            ledger.setUpdatedAt(LocalDateTime.now());
            docLedgerMapper.updateById(ledger);

            // Parse and save chapters
            if (onProgress != null) onProgress.accept("正在拆分章节...");
            splitContentIntoChapters(ledger.getId(), content);
        }
        log.info("Chat generated doc: {} (ledger={}, {} chars)", docName, ledger.getId(),
            content != null ? content.length() : 0);
        return ledger;
    }

    /** Deduplicate chapters by chapter_number, keeping only the first occurrence. */
    private void deduplicateChapters(Long ledgerId) {
        var all = docChapterMapper.selectList(
            new LambdaQueryWrapper<DocChapter>().eq(DocChapter::getDocLedgerId, ledgerId).eq(DocChapter::getDeleted, 0)
                .orderByAsc(DocChapter::getOrderNum));
        var seen = new java.util.HashSet<String>();
        for (var ch : all) {
            String key = ch.getChapterNumber();
            if (!seen.add(key)) {
                ch.setDeleted(1);
                docChapterMapper.updateById(ch);
            }
        }
        log.info("Deduplicated chapters for ledger {}: {} -> {} unique", ledgerId, all.size(), seen.size());
    }

    /** Create chapters from template structure + AI content mapping. */
    private void splitContentIntoChapters(Long ledgerId, String content) {
        try {
            long count = docChapterMapper.selectCount(
                new LambdaQueryWrapper<DocChapter>().eq(DocChapter::getDocLedgerId, ledgerId).eq(DocChapter::getDeleted, 0));
            if (count > 0) return;

            // Parse AI output into sections
            var roots = com.military.doc.ai.util.MarkdownChapterParser.parse(content);
            // Build lookup: chapter_number → AI body text
            java.util.Map<String, String> aiContent = new java.util.LinkedHashMap<>();
            if (!roots.isEmpty()) {
                for (var fs : com.military.doc.ai.util.MarkdownChapterParser.flatten(roots)) {
                    String num = fs.section().number();
                    if (num != null && fs.section().content() != null && !fs.section().content().isBlank()) {
                        aiContent.put(num, fs.section().content());
                    }
                }
            }

            // Find template chapters
            Long templateId = findTemplateForLedger(ledgerId);
            java.util.List<com.military.doc.modules.template.entity.DocTemplateChapter> tplChapters;
            if (templateId != null) {
                tplChapters = tplChapterMapper.selectList(
                    new LambdaQueryWrapper<com.military.doc.modules.template.entity.DocTemplateChapter>()
                        .eq(com.military.doc.modules.template.entity.DocTemplateChapter::getTemplateId, templateId)
                        .eq(com.military.doc.modules.template.entity.DocTemplateChapter::getIsRequired, true)
                        .orderByAsc(com.military.doc.modules.template.entity.DocTemplateChapter::getOrderNum));
            } else {
                tplChapters = java.util.List.of();
            }

            if (tplChapters == null || tplChapters.isEmpty()) {
                // No template — use AI output directly
                if (!aiContent.isEmpty()) {
                    int[] ord = {0};
                    for (var e : aiContent.entrySet()) {
                        DocChapter dc = new DocChapter();
                        dc.setDocLedgerId(ledgerId); dc.setChapterNumber(e.getKey()); dc.setChapterTitle(e.getKey());
                        dc.setChapterLevel(1); dc.setOrderNum(++ord[0]); dc.setParentId(0L);
                        dc.setContent(e.getValue()); dc.setFillStatus("FILLED");
                        dc.setCreatedAt(LocalDateTime.now()); dc.setUpdatedAt(LocalDateTime.now());
                        docChapterMapper.insert(dc);
                    }
                } else {
                    DocChapter ch = new DocChapter();
                    ch.setDocLedgerId(ledgerId); ch.setChapterNumber("1"); ch.setChapterTitle("全文内容");
                    ch.setChapterLevel(1); ch.setOrderNum(1); ch.setParentId(0L);
                    ch.setContent(content.length() > 50000 ? content.substring(0, 50000) : content);
                    ch.setFillStatus("FILLED"); ch.setCreatedAt(LocalDateTime.now()); ch.setUpdatedAt(LocalDateTime.now());
                    docChapterMapper.insert(ch);
                }
            } else {
                // Deduplicate and sort template chapters by chapter_number numerically
                var dedupedTpl = new java.util.LinkedHashMap<String, com.military.doc.modules.template.entity.DocTemplateChapter>();
                for (var tpl : tplChapters) {
                    dedupedTpl.putIfAbsent(tpl.getChapterNumber() != null ? tpl.getChapterNumber() : tpl.getChapterTitle(), tpl);
                }
                // Sort by numeric chapter number (3.1 < 3.2 < 3.10)
                var sortedTpl = new java.util.ArrayList<>(dedupedTpl.values());
                sortedTpl.sort((a, b) -> {
                    String na = a.getChapterNumber() != null ? a.getChapterNumber() : "";
                    String nb = b.getChapterNumber() != null ? b.getChapterNumber() : "";
                    String[] pa = na.split("\\."); String[] pb = nb.split("\\.");
                    for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
                        int va = i < pa.length ? Integer.parseInt(pa[i]) : 0;
                        int vb = i < pb.length ? Integer.parseInt(pb[i]) : 0;
                        if (va != vb) return va - vb;
                    }
                    return 0;
                });
                int[] ord = {0};
                for (var tpl : sortedTpl) {
                    String num = tpl.getChapterNumber();
                    DocChapter dc = new DocChapter();
                    dc.setDocLedgerId(ledgerId); dc.setParentId(0L);
                    dc.setChapterNumber(num);
                    dc.setChapterTitle(tpl.getChapterTitle());
                    dc.setChapterLevel(tpl.getChapterLevel() != null ? tpl.getChapterLevel() : 1);
                    dc.setOrderNum(++ord[0]);
                    // Try to match AI content by chapter_number
                    String body = aiContent.get(num);
                    if (body == null) {
                        // Fuzzy match
                        for (var e : aiContent.entrySet()) {
                            if (num.equals(e.getKey()) || (tpl.getChapterTitle() != null
                                && e.getKey().contains(tpl.getChapterTitle()))) {
                                body = e.getValue(); break;
                            }
                        }
                    }
                    dc.setContent(body); dc.setFillStatus(body != null ? "FILLED" : "DRAFT");
                    dc.setCreatedAt(LocalDateTime.now()); dc.setUpdatedAt(LocalDateTime.now());
                    docChapterMapper.insert(dc);
                }
                log.info("Created {} template chapters for doc {}, {} with AI content",
                    tplChapters.size(), ledgerId, aiContent.size());
            }
        } catch (Exception e) { log.warn("Chapter split failed: {}", e.getMessage()); }
    }

    private Long findTemplateForLedger(Long ledgerId) {
        if (ledgerId == null) return null;
        try {
            DocLedger l = docLedgerMapper.selectById(ledgerId);
            if (l == null || l.getChecklistItemId() == null) return null;
            var pdc = pdcMapper.selectById(l.getChecklistItemId());
            if (pdc == null || pdc.getTemplateId() == null) return null;
            var sct = sctMapper.selectById(pdc.getTemplateId());
            return sct != null ? sct.getTemplateId() : null;
        } catch (Exception e) { return null; }
    }

    private void appendGeneratedDoc(ChatSession session, DocLedger doc) {
        try {
            var docs = objectMapper.readValue(session.getGeneratedDocs(), new TypeReference<List<Map<String,Object>>>(){});
            docs.add(Map.of("docLedgerId", doc.getId(), "docName", doc.getDocName(),
                "status", doc.getLifecycleStatus(), "timestamp", LocalDateTime.now().toString()));
            session.setGeneratedDocs(objectMapper.writeValueAsString(docs));
        } catch (Exception e) { log.warn("Failed to track doc: {}", e.getMessage()); }
    }

    private DocLedger findOrCreateLedger(Long projectId, Long stageId, ProjectDocChecklist item, Long userId) {
        var existing = docLedgerMapper.selectList(
            new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId)
                .eq(DocLedger::getDocName, item.getDocName())
                .last("LIMIT 1"));
        if (!existing.isEmpty()) return existing.get(0);
        DocLedger l = new DocLedger();
        l.setProjectId(projectId); l.setStageId(stageId);
        l.setDocName(item.getDocName()); l.setDocType(item.getCategory());
        l.setDocCategory(item.getCategoryCode()); l.setLifecycleStatus("PLANNED");
        l.setRequiredFlag(true); l.setChecklistItemId(item.getId());
        l.setCreatedBy(userId); l.setUpdatedBy(userId);
        l.setCreatedAt(LocalDateTime.now()); l.setUpdatedAt(LocalDateTime.now());
        docLedgerMapper.insert(l);
        return l;
    }

    private int countOccurrences(String text, String pattern) {
        if (text == null) return 0;
        int count = 0, idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) { count++; idx += pattern.length(); }
        return count;
    }
}
