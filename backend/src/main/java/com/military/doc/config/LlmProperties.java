package com.military.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String provider = "ollama";
    private String baseUrl = "http://localhost:11434";
    private String model = "qwen2.5:7b";
    private double temperature = 0.7;
    private int maxTokens = 4096;
    private int timeoutSeconds = 300;
}
