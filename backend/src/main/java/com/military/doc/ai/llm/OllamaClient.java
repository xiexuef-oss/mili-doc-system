package com.military.doc.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.config.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component("ollamaClient")
public class OllamaClient implements LlmClient {

    private final OkHttpClient httpClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final LlmProviderService providerService;

    public static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    public OllamaClient(OkHttpClient httpClient, LlmProperties properties,
                         ObjectMapper objectMapper, LlmProviderService providerService) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.providerService = providerService;
        this.providerService.registerClient("ollama", this);
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt, boolean stream) {
        return Map.of(
            "model", properties.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "stream", stream,
            "options", Map.of(
                "temperature", properties.getTemperature(),
                "num_predict", properties.getMaxTokens()
            )
        );
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            String json = objectMapper.writeValueAsString(buildRequestBody(systemPrompt, userPrompt, false));
            Request request = new Request.Builder()
                .url(properties.getBaseUrl() + "/api/chat")
                .post(RequestBody.create(json, JSON_MEDIA))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String msg = String.format("Ollama API error: %d %s", response.code(), response.message());
                    log.error(msg);
                    throw new RuntimeException(msg);
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) throw new RuntimeException("Ollama 返回空响应");
                JsonNode node = objectMapper.readTree(responseBody.string());
                JsonNode msgNode = node.get("message");
                if (msgNode != null && msgNode.has("content")) {
                    return msgNode.get("content").asText();
                }
                return "";
            }
        } catch (IOException e) {
            log.error("Ollama chat failed: {}", e.getMessage());
            throw new RuntimeException("Ollama 服务不可用: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        try {
            String json = objectMapper.writeValueAsString(buildRequestBody(systemPrompt, userPrompt, true));
            Request request = new Request.Builder()
                .url(properties.getBaseUrl() + "/api/chat")
                .post(RequestBody.create(json, JSON_MEDIA))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String msg = String.format("Ollama stream API error: %d %s", response.code(), response.message());
                    log.error(msg);
                    throw new RuntimeException(msg);
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) throw new RuntimeException("Ollama 流式返回空响应");

                BufferedSource source = responseBody.source();
                String line;
                while ((line = source.readUtf8Line()) != null) {
                    if (line.isEmpty()) continue;
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        if (node.has("done") && node.get("done").asBoolean()) {
                            break;
                        }
                        JsonNode msg = node.get("message");
                        if (msg != null && msg.has("content")) {
                            onChunk.accept(msg.get("content").asText());
                        }
                    } catch (Exception e) {
                        log.debug("Skipping non-JSON stream line: {}", line.substring(0, Math.min(80, line.length())));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Ollama chat stream failed: {}", e.getMessage());
            throw new RuntimeException("Ollama 流式服务不可用: " + e.getMessage(), e);
        }
    }
}
