package com.military.doc.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Primary LlmClient bean that delegates to the runtime-selected provider.
 * At startup the provider is read from llm.provider config; at runtime it can
 * be toggled via the {@link LlmProviderService}.
 */
@Slf4j
@Primary
@Component
public class DelegatingLlmClient implements LlmClient {

    private final LlmProviderService providerService;

    public DelegatingLlmClient(LlmProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return providerService.getActiveClient().chat(systemPrompt, userPrompt);
    }

    @Override
    public void chatStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        providerService.getActiveClient().chatStream(systemPrompt, userPrompt, onChunk);
    }
}
