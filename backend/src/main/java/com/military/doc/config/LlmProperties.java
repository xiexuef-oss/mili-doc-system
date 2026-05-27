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
}
