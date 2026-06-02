package com.military.doc.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.config.EmbeddingProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EmbeddingClient {

    private final OkHttpClient httpClient;
    private final EmbeddingProperties embeddingProperties;
    private final ObjectMapper objectMapper;

    public static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    public EmbeddingClient(OkHttpClient httpClient,
                           EmbeddingProperties embeddingProperties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.embeddingProperties = embeddingProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void ensureModelAvailable() {
        try {
            String url = embeddingProperties.getBaseUrl() + "/api/tags";
            Request request = new Request.Builder().url(url).get().build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode node = objectMapper.readTree(response.body().string());
                    var models = node.get("models");
                    if (models != null && models.isArray()) {
                        for (var m : models) {
                            String name = m.has("name") ? m.get("name").asText() : "";
                            if (name.equals(embeddingProperties.getModel())
                                || name.startsWith(embeddingProperties.getModel() + ":")) {
                                log.info("Embedding model '{}' is available in Ollama", embeddingProperties.getModel());
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not check embedding model availability: {}", e.getMessage());
        }
        log.warn("Embedding model '{}' NOT found in Ollama. Pull it with: ollama pull {}",
            embeddingProperties.getModel(), embeddingProperties.getModel());
    }

    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        if (results.isEmpty()) {
            throw new RuntimeException("Embedding returned empty result");
        }
        return results.get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        if (!embeddingProperties.isEnabled()) {
            throw new RuntimeException("Embedding is disabled in configuration");
        }
        try {
            Map<String, Object> body = Map.of(
                "model", embeddingProperties.getModel(),
                "input", texts
            );
            String json = objectMapper.writeValueAsString(body);
            Request request = new Request.Builder()
                .url(embeddingProperties.getBaseUrl() + "/api/embed")
                .post(RequestBody.create(json, JSON_MEDIA))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String respBody = response.body() != null ? response.body().string() : "(no body)";
                    String msg = String.format("Ollama embed API error: %d %s, body: %s", response.code(), response.message(), respBody.length() > 500 ? respBody.substring(0, 500) : respBody);
                    log.error(msg);
                    throw new RuntimeException(msg);
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) throw new RuntimeException("Ollama embed returned empty response");

                JsonNode root = objectMapper.readTree(responseBody.string());
                JsonNode embeddings = root.get("embeddings");
                if (embeddings == null || !embeddings.isArray()) {
                    throw new RuntimeException("Ollama embed response missing 'embeddings' array");
                }
                List<float[]> results = new ArrayList<>();
                for (JsonNode emb : embeddings) {
                    float[] vec = new float[emb.size()];
                    for (int i = 0; i < emb.size(); i++) {
                        vec[i] = emb.get(i).floatValue();
                    }
                    results.add(vec);
                }
                return results;
            }
        } catch (IOException e) {
            log.error("Ollama embed failed: {}", e.getMessage());
            throw new RuntimeException("Ollama embedding service unavailable: " + e.getMessage(), e);
        }
    }
}
