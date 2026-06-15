package com.military.doc.ai.llm;

import com.military.doc.ai.entity.AiAuditLog;
import com.military.doc.ai.service.AiAuditService;
import com.military.doc.ai.service.AiErrorTranslationService;
import com.military.doc.ai.util.SensitiveDataScrubber;
import com.military.doc.config.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Primary LlmClient bean that delegates to the runtime-selected provider.
 * At startup the provider is read from llm.provider config; at runtime it can
 * be toggled via the {@link LlmProviderService}.
 *
 * <p>Adds desensitization, audit logging, and error translation around every LLM call.</p>
 */
@Slf4j
@Primary
@Component
public class DelegatingLlmClient implements LlmClient {

    private final LlmProviderService providerService;
    private final SensitiveDataScrubber scrubber;
    private final AiAuditService auditService;
    private final AiErrorTranslationService errorTranslationService;
    private final LlmProperties properties;

    public DelegatingLlmClient(LlmProviderService providerService,
                               SensitiveDataScrubber scrubber,
                               AiAuditService auditService,
                               AiErrorTranslationService errorTranslationService,
                               LlmProperties properties) {
        this.providerService = providerService;
        this.scrubber = scrubber;
        this.auditService = auditService;
        this.errorTranslationService = errorTranslationService;
        this.properties = properties;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return chatWithAudit(systemPrompt, userPrompt, null, null);
    }

    @Override
    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        chatStreamWithAudit(systemPrompt, userPrompt, null, null, onChunk);
    }

    /**
     * 带审计的同步调用。
     * @param taskType 任务类型(catalog/draft/proofread/...)，用于审计日志分类
     * @param projectId 关联项目ID
     */
    public String chatWithAudit(String systemPrompt, String userPrompt,
                                 String taskType, Long projectId) {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorCode = null;
        String errorMessage = null;

        // 1. 脱敏处理
        String scrubbedSystem = scrubber.scrub(systemPrompt);
        String scrubbedUser = scrubber.scrub(userPrompt);

        try {
            // 2. 调用 LLM
            String response = providerService.getActiveClient().chat(scrubbedSystem, scrubbedUser);

            // 3. 解敏恢复
            String descrubbed = scrubber.descrub(response);
            return descrubbed;
        } catch (Exception e) {
            success = false;
            errorCode = e.getClass().getSimpleName();
            errorMessage = e.getMessage();
            // 翻译为友好错误消息
            throw errorTranslationService.wrapWithFriendlyMessage(e,
                taskType != null ? taskTypeToDescription(taskType) : null);
        } finally {
            // 4. 记录审计日志
            try {
                logAudit(taskType, projectId, systemPrompt, userPrompt,
                    System.currentTimeMillis() - start, success, errorCode, errorMessage);
            } finally {
                scrubber.clear();
            }
        }
    }

    private static String taskTypeToDescription(String taskType) {
        return switch (taskType) {
            case "catalog" -> "目录生成";
            case "draft" -> "初稿生成";
            case "proofread" -> "AI校对";
            case "pre-review" -> "预评审";
            case "compliance" -> "合规检查";
            case "opinion-summary" -> "意见汇总";
            case "stage-readiness" -> "阶段评估";
            case "archive-advice" -> "归档建议";
            case "change-impact" -> "变更分析";
            default -> "AI处理";
        };
    }

    /**
     * 带审计的流式调用。
     * 注意：流式调用无法解敏（逐块返回），因此对外部云模型的流式调用应谨慎。
     * 建议本地模型使用流式，云模型使用非流式（chatWithAudit）。
     */
    public void chatStreamWithAudit(String systemPrompt, String userPrompt,
                                     String taskType, Long projectId,
                                     Consumer<String> onChunk) {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorCode = null;
        String errorMessage = null;

        // 流式调用先脱敏
        String scrubbedSystem = scrubber.scrub(systemPrompt);
        String scrubbedUser = scrubber.scrub(userPrompt);

        try {
            providerService.getActiveClient().chatStream(scrubbedSystem, scrubbedUser,
                chunk -> {
                    // 流式块不做解敏（单块太短无法准确匹配占位符）
                    onChunk.accept(chunk);
                });
        } catch (Exception e) {
            success = false;
            errorCode = e.getClass().getSimpleName();
            errorMessage = e.getMessage();
            throw errorTranslationService.wrapWithFriendlyMessage(e,
                taskType != null ? taskTypeToDescription(taskType) : "流式AI处理");
        } finally {
            try {
                logAudit(taskType, projectId, systemPrompt, userPrompt,
                    System.currentTimeMillis() - start, success, errorCode, errorMessage);
            } finally {
                scrubber.clear();
            }
        }
    }

    private void logAudit(String taskType, Long projectId,
                          String sysPrompt, String usrPrompt,
                          long latencyMs, boolean success,
                          String errorCode, String errorMessage) {
        try {
            AiAuditLog logEntry = new AiAuditLog();
            logEntry.setProjectId(projectId);
            logEntry.setTaskType(taskType != null ? taskType : "CHAT");
            logEntry.setProvider(providerService.getActiveProvider());
            logEntry.setModel(properties.getModel());
            logEntry.setSystemPromptHash(sha256(sysPrompt));
            logEntry.setUserPromptHash(sha256(usrPrompt));
            logEntry.setInputTokens(estimateTokens(sysPrompt) + estimateTokens(usrPrompt));
            logEntry.setLatencyMs((int) latencyMs);
            logEntry.setSuccess(success);
            logEntry.setErrorCode(errorCode);
            logEntry.setErrorMessage(errorMessage);
            logEntry.setScrubbedFields(
                scrubber.getScrubbedFieldTypes().isEmpty() ? "[]"
                    : scrubber.getScrubbedFieldTypes().toString());
            logEntry.setLocality(providerService.getLocality());
            logEntry.setCreatedAt(LocalDateTime.now());
            auditService.logCall(logEntry);
        } catch (Exception e) {
            log.debug("Failed to write audit log (non-fatal): {}", e.getMessage());
        }
    }

    /** 估算 token 数量（简化为字符数/2，适用于中英文混合） */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.length() / 2;
    }

    private String sha256(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
