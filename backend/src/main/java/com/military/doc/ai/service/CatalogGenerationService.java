package com.military.doc.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.config.LlmProperties;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CatalogGenerationService {

    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final DocCatalogMapper docCatalogMapper;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
        "\\[\\s*\\{.*?}\\s*]", Pattern.DOTALL);

    public CatalogGenerationService(ContextAssemblyService contextAssemblyService,
                                     PromptTemplateService promptTemplateService,
                                     LlmClient llmClient,
                                     LlmProperties llmProperties,
                                     DocCatalogMapper docCatalogMapper,
                                     ObjectMapper objectMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.docCatalogMapper = docCatalogMapper;
        this.objectMapper = objectMapper;
    }

    public List<DocCatalog> generate(Long projectId, Long stageId, Long userId, boolean overwrite) {
        // 1. Assemble context
        String context = contextAssemblyService.assembleContext(projectId);
        if (context.isEmpty()) {
            log.warn("Empty context for project {}", projectId);
            return List.of();
        }

        // 2. Render prompt
        String userPrompt = promptTemplateService.render("catalog-generation",
            Map.of("context", context));
        String systemPrompt = "你是一位军工文档策划专家。仅返回 JSON 数组，不包含任何其他文字。";

        log.info("Catalog generation prompt: {} chars system, {} chars user",
            systemPrompt.length(), userPrompt.length());

        // 3. Call LLM
        String response = llmClient.chat(systemPrompt, userPrompt);
        if (response == null || response.isBlank()) {
            log.warn("LLM returned empty response for catalog generation");
            return List.of();
        }
        log.info("LLM response for catalog generation: {} chars", response.length());

        // 4. Parse JSON from response
        List<CatalogItem> items = parseCatalogResponse(response);
        if (items.isEmpty()) {
            log.warn("Failed to parse catalog items from LLM response");
            return List.of();
        }

        // 5. Optionally clear existing catalog entries for this project
        if (overwrite) {
            docCatalogMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocCatalog>()
                    .eq(DocCatalog::getProjectId, projectId));
            log.info("Cleared existing catalog for project {}", projectId);
        }

        // 6. Insert new catalog entries
        List<DocCatalog> catalogs = new ArrayList<>();
        for (CatalogItem item : items) {
            DocCatalog catalog = new DocCatalog();
            catalog.setProjectId(projectId);
            catalog.setStageId(stageId);
            catalog.setDocCode(item.docCode);
            catalog.setDocName(item.docName);
            catalog.setDocType(item.docType);
            catalog.setRequiredFlag(item.requiredFlag != null ? item.requiredFlag : true);
            catalog.setStatus("DRAFT");
            catalog.setCreatedBy(userId);
            catalog.setCreatedAt(LocalDateTime.now());
            docCatalogMapper.insert(catalog);
            catalogs.add(catalog);
        }

        log.info("Generated {} catalog entries for project {}", catalogs.size(), projectId);
        return catalogs;
    }

    List<CatalogItem> parseCatalogResponse(String response) {
        // Try to extract JSON array from response
        Matcher m = JSON_ARRAY_PATTERN.matcher(response);
        String json = null;
        if (m.find()) {
            json = m.group();
        } else {
            // Try using the whole response
            json = response.trim();
            if (!json.startsWith("[")) {
                // Look for JSON array anywhere
                int start = json.indexOf('[');
                int end = json.lastIndexOf(']');
                if (start >= 0 && end > start) {
                    json = json.substring(start, end + 1);
                } else {
                    return List.of();
                }
            }
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<CatalogItem>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse catalog JSON: {}", e.getMessage());
            return List.of();
        }
    }

    public static class CatalogItem {
        public String docCode;
        public String docName;
        public String docType;
        public Boolean requiredFlag;
    }
}
