package com.military.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String provider = "ollama";
    private String baseUrl = "http://localhost:11434";
    private String apiKey = "";
    private String model = "qwen2.5:7b";
    private double temperature = 0.7;
    private int maxTokens = 4096;
    private int timeoutSeconds = 300;

    /** Per-provider overrides. Key: provider name (ollama, deepseek). */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    /** Per-task model routing: catalog, draft, proofread, etc. → model name */
    private Map<String, String> taskModels = new LinkedHashMap<>();

    /** Smart context truncation max chars */
    private int maxContextChars = 40000;

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
    }

    /**
     * Apply the per-provider config for the given provider name to the top-level fields.
     */
    public void applyProvider(String name) {
        ProviderConfig cfg = providers != null ? providers.get(name) : null;
        if (cfg == null) return;
        if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) this.baseUrl = cfg.getBaseUrl();
        if (cfg.getApiKey() != null) this.apiKey = cfg.getApiKey();
        if (cfg.getModel() != null && !cfg.getModel().isBlank()) this.model = cfg.getModel();
    }

    /**
     * 获取指定任务类型应使用的模型。
     * 如果 taskModels 中有配置则返回配置值，否则返回默认 model。
     */
    public String getModelForTask(String taskType) {
        if (taskType != null && taskModels != null && taskModels.containsKey(taskType)) {
            return taskModels.get(taskType);
        }
        return model;
    }
}
