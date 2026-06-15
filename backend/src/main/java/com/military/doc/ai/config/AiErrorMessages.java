package com.military.doc.ai.config;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI 错误信息映射表：将技术异常转换为用户友好的中文错误信息。
 */
@Component
public class AiErrorMessages {

    public record ErrorMapping(String friendlyMessage, String suggestedAction, String category) {}

    private final Map<Pattern, ErrorMapping> patterns = new LinkedHashMap<>();

    public AiErrorMessages() {
        // 连接错误
        add("ConnectException|Connection refused|Failed to connect|connect timed out",
            "无法连接到 AI 模型服务",
            "请检查 AI 服务是否启动（Ollama 本地服务或 DeepSeek 网络连接），或联系管理员检查网络配置",
            "CONNECTION");

        // 超时
        add("timeout|TimeoutException|timed out|read timeout",
            "AI 模型响应超时",
            "文档内容较多时生成时间较长，请稍后重试。如持续超时，请联系管理员调整超时配置或切换到更快的模型",
            "TIMEOUT");

        // 取消
        add("SSE_ABORT|AbortError|aborted|cancelled",
            "生成已被取消",
            "",
            "CANCELLED");

        // API 密钥
        add("401|Unauthorized|Authentication|Invalid API key|Invalid token",
            "API 密钥无效或认证失败",
            "请在系统配置中检查 API 密钥设置，确认密钥未过期",
            "AUTH");

        // 频率限制
        add("429|rate.?limit|too many requests",
            "API 调用频率过高，触发了限流",
            "请稍后重试（建议等待 30 秒），或联系管理员降低并发调用数",
            "RATE_LIMIT");

        // 上下文超长
        add("context.?length|maximum context length|context_length_exceeded|too long",
            "输入内容超出模型处理限制",
            "系统已自动截断部分内容。如仍失败，请减少输入文件或切换支持更长上下文的模型",
            "CONTEXT");

        // JSON 解析错误
        add("JSON|JsonParseException|parse error|Failed to parse|MalformedJson",
            "AI 返回格式异常，解析失败",
            "系统将自动重试。如持续失败，请检查 AI 模型是否正常运行",
            "PARSE");

        // 空响应
        add("empty response|null response|blank response",
            "AI 返回了空响应",
            "请检查 AI 模型是否正常加载（Ollama 模型列表），或重试请求",
            "EMPTY");

        // 内存不足
        add("OutOfMemoryError|Java heap space|heap",
            "系统资源不足",
            "请联系管理员增加服务器内存配置或减少并发任务数",
            "RESOURCE");

        // Ollama 特定错误
        add("model not found|model.*not loaded|ollama.*error",
            "Ollama 模型未加载或不存在",
            "请在服务器上运行 'ollama pull <模型名>' 下载所需模型。常用模型: qwen2.5:7b, deepseek-r1:7b",
            "OLLAMA");

        // 通用 AI 错误
        add("AI.*fail|生成失败|LLM.*error",
            "AI 处理失败",
            "请稍后重试。如问题持续，请联系管理员",
            "GENERAL");
    }

    private void add(String regex, String friendly, String action, String category) {
        patterns.put(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), new ErrorMapping(friendly, action, category));
    }

    /**
     * 根据异常信息查找匹配的友好错误消息。
     */
    public ErrorMapping findMapping(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return new ErrorMapping("AI 服务出现未知错误", "请稍后重试，或联系管理员", "UNKNOWN");
        }
        for (var entry : patterns.entrySet()) {
            if (entry.getKey().matcher(errorMessage).find()) {
                return entry.getValue();
            }
        }
        return new ErrorMapping("AI 服务出现错误: " + truncate(errorMessage, 100),
            "请稍后重试，或联系管理员", "UNKNOWN");
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
