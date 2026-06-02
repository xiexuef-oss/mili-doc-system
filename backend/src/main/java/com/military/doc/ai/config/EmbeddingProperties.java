package com.military.doc.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:11434";
    private String model = "nomic-embed-text";
    private int dimension = 768;
    private int batchSize = 10;
    private boolean semanticRagEnabled = false;
}
