package com.military.doc.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.config.LlmProperties;
import com.military.doc.modules.document.entity.DocCatalog;
import com.military.doc.modules.document.mapper.DocCatalogMapper;
import com.military.doc.modules.project.constant.StageDefinition;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final ProjectStageMapper projectStageMapper;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile(
        "\\[\\s*\\{.*?}\\s*]", Pattern.DOTALL);

    public CatalogGenerationService(ContextAssemblyService contextAssemblyService,
                                     PromptTemplateService promptTemplateService,
                                     LlmClient llmClient,
                                     LlmProperties llmProperties,
                                     DocCatalogMapper docCatalogMapper,
                                     ProjectStageMapper projectStageMapper,
                                     ObjectMapper objectMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.docCatalogMapper = docCatalogMapper;
        this.projectStageMapper = projectStageMapper;
        this.objectMapper = objectMapper;
    }

    public List<DocCatalog> generate(Long projectId, Long stageId, Long userId, boolean overwrite) {
        // 1. Assemble context
        String context = contextAssemblyService.assembleContext(projectId);
        if (context.isEmpty()) {
            log.warn("Empty context for project {}", projectId);
            throw new RuntimeException("无法获取项目上下文信息，请确认项目存在且包含输入文件或适用标准");
        }

        // 2. Build stage-specific context
        StringBuilder stageContext = new StringBuilder();
        if (stageId != null) {
            ProjectStage stage = projectStageMapper.selectById(stageId);
            if (stage != null) {
                stageContext.append("## 当前阶段\n");
                stageContext.append("- 阶段名称: ").append(nullToEmpty(stage.getStageName())).append("\n");
                stageContext.append("- 阶段代码: ").append(nullToEmpty(stage.getStageCode())).append("\n");
                if (stage.getStageGoal() != null) {
                    stageContext.append("- 阶段目标: ").append(stage.getStageGoal()).append("\n");
                }
                StageDefinition def = StageDefinition.findByCode(stage.getStageCode());
                if (def != null) {
                    stageContext.append("- 阶段说明: ").append(def.description()).append("\n");
                    stageContext.append("- 基线类型: ").append(def.defaultBaselineType() != null ? def.defaultBaselineType() : "无").append("\n");
                }
                stageContext.append("\n");
            }
        }

        // 3. Render prompt with stage info
        String userPrompt = promptTemplateService.render("catalog-generation",
            Map.of("context", stageContext + "\n" + context));
        String systemPrompt = "你是一位军工文档策划专家，精通 GJB 3206B 技术状态管理各阶段的文档需求。"
            + "请根据阶段特点生成该阶段特有的文档清单。不同阶段的文档应有明显差异。仅返回 JSON 数组，不包含任何其他文字。";

        log.info("Catalog generation for project={} stage={}: system {} chars, user {} chars",
            projectId, stageId, systemPrompt.length(), userPrompt.length());

        // 4. Call LLM
        String response;
        try {
            response = llmClient.chat(systemPrompt, userPrompt);
        } catch (RuntimeException e) {
            log.error("LLM call failed for catalog generation: {}", e.getMessage());
            throw new RuntimeException("AI 目录生成失败: " + e.getMessage(), e);
        }
        if (response == null || response.isBlank()) {
            log.warn("LLM returned empty response for catalog generation");
            throw new RuntimeException("AI 返回空响应，请检查 Ollama 模型是否正常加载");
        }
        log.info("LLM response for catalog generation: {} chars", response.length());

        // 5. Parse JSON from response
        List<CatalogItem> items = parseCatalogResponse(response);
        if (items.isEmpty()) {
            log.warn("Failed to parse catalog items from LLM response");
            return List.of();
        }

        // 6. Clear only this stage's catalog entries (not the whole project)
        if (overwrite) {
            var deleteWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocCatalog>()
                .eq(DocCatalog::getProjectId, projectId);
            if (stageId != null) {
                deleteWrapper.eq(DocCatalog::getStageId, stageId);
            }
            docCatalogMapper.delete(deleteWrapper);
            log.info("Cleared existing catalog for project={} stage={}", projectId, stageId);
        }

        // 7. Insert new catalog entries
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

        log.info("Generated {} catalog entries for project={} stage={}", catalogs.size(), projectId, stageId);
        return catalogs;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
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
