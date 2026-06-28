package com.military.doc.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.config.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component("openAiCompatibleClient")
public class OpenAiCompatibleClient implements LlmClient {

    private final OkHttpClient httpClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final LlmProviderService providerService;

    public static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 800;

    public OpenAiCompatibleClient(OkHttpClient httpClient, LlmProperties properties,
                                   ObjectMapper objectMapper, LlmProviderService providerService) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.providerService = providerService;
        this.providerService.registerClient("deepseek", this);
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userPrompt) {
        List<Map<String, String>> messages = new ArrayList<>(2);
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        return messages;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
            "model", properties.getModel(),
            "messages", buildMessages(systemPrompt, userPrompt),
            "temperature", properties.getTemperature(),
            "max_tokens", properties.getMaxTokens()
        );

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String json = objectMapper.writeValueAsString(body);
                Request request = new Request.Builder()
                    .url(properties.getBaseUrl() + "/v1/chat/completions")
                    .post(RequestBody.create(json, JSON_MEDIA))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.code() == 429 || response.code() >= 500) {
                        throw new IOException("Retryable status: " + response.code());
                    }
                    if (!response.isSuccessful()) {
                        String respBody = response.body() != null ? response.body().string() : "";
                        String msg = String.format("API error %d: %s", response.code(), respBody);
                        log.error(msg);
                        throw new RuntimeException(msg);
                    }
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) throw new RuntimeException("API 返回空响应");
                    JsonNode node = objectMapper.readTree(responseBody.string());
                    JsonNode choices = node.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode content = choices.get(0).get("message").get("content");
                        if (content != null) return content.asText();
                    }
                    return "";
                }
            } catch (IOException e) {
                if (attempt < MAX_RETRIES - 1) {
                    long delay = BASE_DELAY_MS * (1L << attempt); // 1s, 2s, 4s
                    log.warn("LLM API attempt {}/{} failed: {}. Retrying in {}ms...",
                        attempt + 1, MAX_RETRIES, e.getMessage(), delay);
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                } else {
                    log.error("LLM API failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                    throw new RuntimeException("API 服务不可用 (已重试" + MAX_RETRIES + "次): " + e.getMessage(), e);
                }
            }
        }
        throw new RuntimeException("API 服务不可用");
    }

    @Override
    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        Map<String, Object> body = Map.of(
            "model", properties.getModel(),
            "messages", buildMessages(systemPrompt, userPrompt),
            "temperature", properties.getTemperature(),
            "max_tokens", properties.getMaxTokens(),
            "stream", true
        );

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String json = objectMapper.writeValueAsString(body);
                Request request = new Request.Builder()
                    .url(properties.getBaseUrl() + "/v1/chat/completions")
                    .post(RequestBody.create(json, JSON_MEDIA))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Accept", "text/event-stream")
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String respBody = response.body() != null ? response.body().string() : "";
                        throw new IOException(String.format("API stream error %d: %s", response.code(), respBody));
                    }
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) throw new RuntimeException("API 流式返回空响应");

                    BufferedSource source = responseBody.source();
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        if (line.isEmpty() || !line.startsWith("data: ")) continue;
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode choices = node.get("choices");
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).get("delta");
                                if (delta != null && delta.has("content")) {
                                    String chunk = delta.get("content").asText();
                                    if (!chunk.isEmpty()) onChunk.accept(chunk);
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Skipping unparseable SSE line");
                        }
                    }
                    return; // success
                }
            } catch (IOException e) {
                if (attempt < MAX_RETRIES - 1) {
                    long delay = BASE_DELAY_MS * (1L << attempt);
                    log.warn("LLM stream attempt {}/{} failed: {}. Retrying in {}ms...",
                        attempt + 1, MAX_RETRIES, e.getMessage(), delay);
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                } else {
                    log.error("LLM stream failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                    throw new RuntimeException("API 流式服务不可用 (已重试" + MAX_RETRIES + "次): " + e.getMessage(), e);
                }
            }
        }
    }
}
