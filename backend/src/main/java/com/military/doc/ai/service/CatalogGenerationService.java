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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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

    /**
     * 生成文档目录。overwrite=false 时为追加模式（默认），跳过已存在的条目。
     * overwrite=true 时先删除再全部重新插入。
     */
    public List<DocCatalog> generate(Long projectId, Long stageId, Long userId, boolean overwrite) {
        // 1. 调用 LLM 获取目录项
        List<CatalogItem> items = generateItems(projectId, stageId);

        if (items.isEmpty()) {
            log.warn("No catalog items generated for project={} stage={}", projectId, stageId);
            return List.of();
        }

        // 2. 查询已有的目录条目
        var existingQuery = new LambdaQueryWrapper<DocCatalog>()
            .eq(DocCatalog::getProjectId, projectId);
        if (stageId != null) {
            existingQuery.eq(DocCatalog::getStageId, stageId);
        }
        List<DocCatalog> existing = docCatalogMapper.selectList(existingQuery);
        Set<String> existingCodes = new HashSet<>();
        for (var ex : existing) {
            existingCodes.add(ex.getDocCode());
        }

        // 3. 覆盖模式：先删除
        if (overwrite) {
            docCatalogMapper.delete(existingQuery);
            log.info("Cleared existing catalog for project={} stage={} (overwrite mode)", projectId, stageId);
            existingCodes.clear();
        }

        // 4. 插入新条目（跳过重复）
        List<DocCatalog> catalogs = new ArrayList<>();
        int skipped = 0;
        for (CatalogItem item : items) {
            if (existingCodes.contains(item.docCode)) {
                log.debug("Skipping duplicate catalog entry: {} (docCode={})", item.docName, item.docCode);
                skipped++;
                continue;
            }
            DocCatalog catalog = new DocCatalog();
            catalog.setProjectId(projectId);
            catalog.setStageId(stageId);
            catalog.setDocCode(item.docCode);
            catalog.setDocName(item.docName);
            catalog.setDocCategory(item.docCategory);
            catalog.setDocType(item.docType);
            catalog.setStageCode(item.stageCode);
            catalog.setRequiredFlag(item.requiredFlag != null ? item.requiredFlag : true);
            catalog.setStatus("DRAFT");
            catalog.setCreatedBy(userId);
            catalog.setCreatedAt(LocalDateTime.now());
            docCatalogMapper.insert(catalog);
            catalogs.add(catalog);
        }

        log.info("Generated {} new catalog entries, skipped {} duplicates for project={} stage={}",
            catalogs.size(), skipped, projectId, stageId);
        return catalogs;
    }

    /**
     * 预览模式：解析 LLM 返回的目录项并返回冲突信息，不写入数据库。
     * @return {items: [...], conflictCount: N, existingCount: M, newCount: K}
     */
    public Map<String, Object> generateDryRun(Long projectId, Long stageId) {
        List<CatalogItem> items = generateItems(projectId, stageId);

        // 查询已有条目
        var existingQuery = new LambdaQueryWrapper<DocCatalog>()
            .eq(DocCatalog::getProjectId, projectId);
        if (stageId != null) {
            existingQuery.eq(DocCatalog::getStageId, stageId);
        }
        List<DocCatalog> existing = docCatalogMapper.selectList(existingQuery);
        Set<String> existingCodes = new HashSet<>();
        for (var ex : existing) {
            existingCodes.add(ex.getDocCode());
        }

        // 分类：新条目 vs 冲突条目
        List<Map<String, Object>> newItems = new ArrayList<>();
        List<Map<String, Object>> conflictItems = new ArrayList<>();
        for (var item : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("docCode", item.docCode);
            m.put("docName", item.docName);
            m.put("docType", item.docType);
            m.put("requiredFlag", item.requiredFlag);
            if (existingCodes.contains(item.docCode)) {
                conflictItems.add(m);
            } else {
                newItems.add(m);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalGenerated", items.size());
        result.put("newCount", newItems.size());
        result.put("conflictCount", conflictItems.size());
        result.put("existingCount", existing.size());
        result.put("newItems", newItems);
        result.put("conflictItems", conflictItems);
        return result;
    }

    // ---- private helpers ----

    private List<CatalogItem> generateItems(Long projectId, Long stageId) {
        String context = contextAssemblyService.assembleContext(projectId);
        if (context.isEmpty()) {
            log.warn("Empty context for project {}", projectId);
            throw new RuntimeException("无法获取项目上下文信息，请确认项目存在且包含输入文件或适用标准");
        }

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

        String userPrompt = promptTemplateService.render("catalog-generation",
            Map.of("context", stageContext + "\n" + context));
        String systemPrompt = "你是一位军工文档策划专家，精通 GJB 5882-2006《军工产品研制技术文件编写指南》三维分类体系（7个阶段×15个内容类别）、GJB 3206B 技术状态管理各阶段的文档需求。"
            + "请根据阶段特点生成该阶段特有的文档清单，覆盖 GJB 5882 定义的十五大文档类别中该阶段适用的所有类别。不同阶段的文档应有明显差异。仅返回 JSON 数组，不包含任何其他文字。";

        log.info("Catalog generation for project={} stage={}: system {} chars, user {} chars",
            projectId, stageId, systemPrompt.length(), userPrompt.length());

        String response;
        try {
            response = llmClient.chat(systemPrompt, userPrompt);
        } catch (RuntimeException e) {
            log.error("LLM call failed for catalog generation: {}", e.getMessage());
            throw new RuntimeException("AI 目录生成失败: " + e.getMessage(), e);
        }
        if (response == null || response.isBlank()) {
            log.warn("LLM returned empty response for catalog generation");
            throw new RuntimeException("AI 返回空响应，请检查 AI 模型是否正常加载");
        }
        log.info("LLM response for catalog generation: {} chars", response.length());

        return parseCatalogResponse(response);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    List<CatalogItem> parseCatalogResponse(String response) {
        var pattern = java.util.regex.Pattern.compile("\\[\\s*\\{.*?}\\s*]", java.util.regex.Pattern.DOTALL);
        var m = pattern.matcher(response);
        String json;
        if (m.find()) {
            json = m.group();
        } else {
            json = response.trim();
            if (!json.startsWith("[")) {
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
        public String docCategory;
        public String docType;
        public String stageCode;
        public Boolean requiredFlag;
    }
}
