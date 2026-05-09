package com.military.doc.ai.llm;

import java.util.function.Consumer;

public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);
    void chatStream(String systemPrompt, String userPrompt, Consumer<String> onChunk);
}
