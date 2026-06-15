package com.military.doc.ai.llm;

import com.military.doc.config.LlmProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LlmProviderService {

    private final LlmProperties properties;
    private final Map<String, LlmClient> clients = new ConcurrentHashMap<>();
    private volatile String activeProvider;

    public LlmProviderService(LlmProperties properties) {
        this.properties = properties;
        this.activeProvider = properties.getProvider();
    }

    @PostConstruct
    public void init() {
        properties.applyProvider(activeProvider);
        log.info("LLM provider initialized: {} (baseUrl={}, model={})",
            activeProvider, properties.getBaseUrl(), properties.getModel());
    }

    public void registerClient(String provider, LlmClient client) {
        clients.put(provider, client);
        log.info("Registered LLM client: {}", provider);
    }

    public String getActiveProvider() {
        return activeProvider;
    }

    public LlmClient getActiveClient() {
        LlmClient client = clients.get(activeProvider);
        if (client == null) {
            throw new IllegalStateException("No LLM client registered for provider: " + activeProvider);
        }
        return client;
    }

    public LlmClient switchProvider(String provider) {
        if (!clients.containsKey(provider)) {
            throw new IllegalArgumentException("Unknown LLM provider: " + provider + ". Available: " + clients.keySet());
        }
        String old = this.activeProvider;
        this.activeProvider = provider;
        properties.setProvider(provider);
        properties.applyProvider(provider);
        log.info("LLM provider switched: {} -> {} (baseUrl={}, model={})",
            old, provider, properties.getBaseUrl(), properties.getModel());
        return clients.get(provider);
    }

    public Map<String, Object> getStatus() {
        return Map.of(
            "provider", activeProvider,
            "availableProviders", clients.keySet().stream().sorted().toList(),
            "model", properties.getModel(),
            "baseUrl", properties.getBaseUrl(),
            "locality", getLocality(),
            "isLocal", isLocal()
        );
    }

    /**
     * 获取当前模型的位置标识。
     * @return OLLAMA_LOCAL / DEEPSEEK_CLOUD / UNKNOWN
     */
    public String getLocality() {
        if ("ollama".equals(activeProvider)) {
            return "OLLAMA_LOCAL";
        }
        String baseUrl = properties.getBaseUrl();
        if (baseUrl != null && (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1"))) {
            return "OLLAMA_LOCAL";
        }
        if ("deepseek".equals(activeProvider)) {
            return "DEEPSEEK_CLOUD";
        }
        return "UNKNOWN";
    }

    /** 当前模型是否运行在本地 */
    public boolean isLocal() {
        return "OLLAMA_LOCAL".equals(getLocality());
    }
}
