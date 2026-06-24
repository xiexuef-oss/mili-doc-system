package com.military.doc.ai.service;

import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.common.util.Str;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话式文档写作引擎（Phase 4）。
 * 支持四阶段结构化对话：策划 -> 大纲 -> 逐章写作 -> 审查。
 */
@Slf4j
@Service
public class DialogueWritingService {

    private final LlmClient llmClient;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;

    // In-memory session store (production should use DB/Redis)
    private final Map<String, DialogueSession> sessions = new ConcurrentHashMap<>();

    public DialogueWritingService(LlmClient llmClient,
                                   ContextAssemblyService contextAssemblyService,
                                   PromptTemplateService promptTemplateService) {
        this.llmClient = llmClient;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
    }

    public DialogueSession startSession(Long projectId, String docType, String docName) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        DialogueSession session = new DialogueSession();
        session.sessionId = sessionId;
        session.projectId = projectId;
        session.docType = docType;
        session.docName = docName;
        session.currentPhase = "PLANNING";
        session.status = "ACTIVE";
        session.createdAt = LocalDateTime.now();
        session.messages = new ArrayList<>();
        session.docContent = new StringBuilder();

        // Load project context
        session.projectContext = contextAssemblyService.assembleContext(projectId);

        // AI initial message
        String initMsg = buildInitialMessage(session);
        session.messages.add(new DialogueMessage("AI", initMsg, LocalDateTime.now()));
        session.lastAiMessage = initMsg;

        sessions.put(sessionId, session);
        log.info("Dialogue session started: {} for docType={}", sessionId, docType);
        return session;
    }

    public DialogueResponse processMessage(String sessionId, String userMessage) {
        DialogueSession session = sessions.get(sessionId);
        if (session == null) throw new IllegalArgumentException("Session not found: " + sessionId);

        session.messages.add(new DialogueMessage("USER", userMessage, LocalDateTime.now()));

        // Build prompt with conversation history
        String systemPrompt = buildSystemPrompt(session);
        String userPrompt = buildUserPrompt(session, userMessage);

        String aiResponse = llmClient.chat(systemPrompt, userPrompt);
        session.messages.add(new DialogueMessage("AI", aiResponse, LocalDateTime.now()));
        session.lastAiMessage = aiResponse;

        // Update document content (append AI response)
        session.docContent.append(aiResponse).append("\n\n");

        DialogueResponse resp = new DialogueResponse();
        resp.sessionId = sessionId;
        resp.aiMessage = aiResponse;
        resp.currentPhase = session.currentPhase;
        resp.docPreview = session.docContent.toString();
        resp.messageCount = session.messages.size();

        return resp;
    }

    public DialogueSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    private String buildInitialMessage(DialogueSession session) {
        return String.format(
            "你好！我将协助你撰写《%s》。" + System.lineSeparator() + System.lineSeparator() +
            "当前项目上下文已加载。请告诉我：" + System.lineSeparator() +
            "1. 你希望从哪个章节开始？" + System.lineSeparator() +
            "2. 有什么特别的要求或约束？" + System.lineSeparator() + System.lineSeparator() +
            "或者直接说\"开始写大纲\"，我来规划文档结构。",
            session.docName
        );
    }

    private String buildSystemPrompt(DialogueSession session) {
        return String.format(
            "你是军工文档撰写专家。正在协助用户撰写《%s》。" + System.lineSeparator() +
            "项目上下文：" + System.lineSeparator() + "%s" + System.lineSeparator() + System.lineSeparator() +
            "规则：" + System.lineSeparator() +
            "1. 锁定已知数据，原样使用" + System.lineSeparator() +
            "2. 不确定的内容用 [⚠待核实] 标记" + System.lineSeparator() +
            "3. 禁止XXX、待补充等占位符" + System.lineSeparator() +
            "4. 每段内容末尾输出 AI_META",
            session.docName,
            session.projectContext != null ? Str.truncate(session.projectContext, 3000) : ""
        );
    }

    private String buildUserPrompt(DialogueSession session, String latestMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("对话历史：\n");
        int start = Math.max(0, session.messages.size() - 6);
        for (int i = start; i < session.messages.size(); i++) {
            DialogueMessage msg = session.messages.get(i);
            sb.append(msg.role).append(": ").append(msg.content).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator()).append("当前用户消息: ").append(latestMessage);
        return sb.toString();
    }

    @Data
    public static class DialogueSession {
        String sessionId;
        Long projectId;
        String docType;
        String docName;
        String currentPhase;
        String status;
        LocalDateTime createdAt;
        String projectContext;
        String lastAiMessage;
        List<DialogueMessage> messages;
        StringBuilder docContent;
    }

    @Data
    public static class DialogueMessage {
        String role;
        String content;
        LocalDateTime timestamp;

        public DialogueMessage(String role, String content, LocalDateTime timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    @Data
    public static class DialogueResponse {
        String sessionId;
        String aiMessage;
        String currentPhase;
        String docPreview;
        int messageCount;
    }
}
