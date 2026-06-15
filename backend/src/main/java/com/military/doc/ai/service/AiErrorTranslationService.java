package com.military.doc.ai.service;

import com.military.doc.ai.config.AiErrorMessages;
import com.military.doc.ai.config.AiErrorMessages.ErrorMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 错误翻译服务 — 将技术异常链转换为用户友好的错误信息。
 */
@Slf4j
@Service
public class AiErrorTranslationService {

    private final AiErrorMessages messages;

    public AiErrorTranslationService(AiErrorMessages messages) {
        this.messages = messages;
    }

    /**
     * 扫描异常链，查找匹配的友好错误消息。
     */
    public ErrorMapping translateError(Throwable e) {
        if (e == null) return messages.findMapping(null);

        // 扫描异常链：自己 → cause → suppressed
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && !msg.isBlank()) {
                ErrorMapping mapping = messages.findMapping(msg);
                if (!"UNKNOWN".equals(mapping.category())) {
                    log.debug("AI error translated: {} → {}", e.getClass().getSimpleName(), mapping.friendlyMessage());
                    return mapping;
                }
            }
            current = current.getCause();
        }

        // 也检查 suppressed 异常
        for (Throwable suppressed : e.getSuppressed()) {
            ErrorMapping mapping = translateError(suppressed);
            if (!"UNKNOWN".equals(mapping.category())) return mapping;
        }

        return messages.findMapping(e.getMessage());
    }

    /**
     * 包裹异常为包含友好消息的 RuntimeException。
     */
    public RuntimeException wrapWithFriendlyMessage(Throwable e, String taskDescription) {
        ErrorMapping mapping = translateError(e);
        String prefix = taskDescription != null ? taskDescription + "失败: " : "";
        String fullMessage = prefix + mapping.friendlyMessage();

        if (mapping.suggestedAction() != null && !mapping.suggestedAction().isBlank()) {
            fullMessage += " | " + mapping.suggestedAction();
        }

        log.warn("AI error wrapped: {} (original: {})", fullMessage, e.getMessage());
        return new AiBusinessException(fullMessage, mapping.suggestedAction(), e);
    }

    /** AI 业务异常，包含友好消息和建议操作 */
    public static class AiBusinessException extends RuntimeException {
        private final String suggestedAction;

        public AiBusinessException(String message, String suggestedAction, Throwable cause) {
            super(message, cause);
            this.suggestedAction = suggestedAction;
        }

        public String getSuggestedAction() {
            return suggestedAction;
        }
    }
}
