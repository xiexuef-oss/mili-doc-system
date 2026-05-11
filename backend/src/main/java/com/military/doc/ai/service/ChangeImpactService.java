package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.project.entity.ConfigurationBaseline;
import com.military.doc.modules.project.entity.ConfigurationBaselineItem;
import com.military.doc.modules.project.entity.ConfigurationChangeRequest;
import com.military.doc.modules.project.entity.ConfigurationItem;
import com.military.doc.modules.project.mapper.ConfigurationBaselineItemMapper;
import com.military.doc.modules.project.mapper.ConfigurationBaselineMapper;
import com.military.doc.modules.project.mapper.ConfigurationChangeRequestMapper;
import com.military.doc.modules.project.mapper.ConfigurationItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChangeImpactService {

    private final LlmClient llmClient;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final ConfigurationBaselineMapper baselineMapper;
    private final ConfigurationBaselineItemMapper baselineItemMapper;
    private final ConfigurationItemMapper ciMapper;
    private final ConfigurationChangeRequestMapper changeRequestMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final ObjectMapper objectMapper;

    public ChangeImpactService(LlmClient llmClient, ContextAssemblyService contextAssemblyService,
                                PromptTemplateService promptTemplateService,
                                ConfigurationBaselineMapper baselineMapper,
                                ConfigurationBaselineItemMapper baselineItemMapper,
                                ConfigurationItemMapper ciMapper,
                                ConfigurationChangeRequestMapper changeRequestMapper,
                                DocLedgerMapper docLedgerMapper, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.baselineMapper = baselineMapper;
        this.baselineItemMapper = baselineItemMapper;
        this.ciMapper = ciMapper;
        this.changeRequestMapper = changeRequestMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> analyze(Long projectId, String changeDescription, Long baselineId) {
        // Collect baseline items
        List<Map<String, String>> baselineItems = new ArrayList<>();
        if (baselineId != null) {
            ConfigurationBaseline baseline = baselineMapper.selectById(baselineId);
            if (baseline != null) {
                List<ConfigurationBaselineItem> items = baselineItemMapper.selectList(
                    new LambdaQueryWrapper<ConfigurationBaselineItem>()
                        .eq(ConfigurationBaselineItem::getBaselineId, baselineId));
                for (ConfigurationBaselineItem item : items) {
                    baselineItems.add(Map.of(
                        "itemCode", item.getItemCode() != null ? item.getItemCode() : "",
                        "itemName", item.getItemName() != null ? item.getItemName() : "",
                        "itemType", item.getItemType() != null ? item.getItemType() : "",
                        "itemVersion", item.getItemVersion() != null ? item.getItemVersion() : ""));
                }
            }
        }

        // Collect CI list
        List<ConfigurationItem> cis = ciMapper.selectList(new LambdaQueryWrapper<ConfigurationItem>()
                .eq(ConfigurationItem::getProjectId, projectId));
        List<String> ciList = cis.stream()
            .map(ci -> String.format("- CI %s: %s (类型: %s, 版本: %s)",
                ci.getCiCode(), ci.getCiName(), ci.getCiType(), ci.getCurrentVersion()))
            .collect(Collectors.toList());

        // Collect documents in the project
        List<DocLedger> docs = docLedgerMapper.selectList(new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId));
        List<String> docList = docs.stream()
            .map(d -> String.format("- 文档 %s: %s (类型: %s, 状态: %s)",
                d.getDocCode(), d.getDocName(), d.getDocType(), d.getLifecycleStatus()))
            .collect(Collectors.toList());

        // Open change requests
        Long openChanges = changeRequestMapper.selectCount(new LambdaQueryWrapper<ConfigurationChangeRequest>()
                .eq(ConfigurationChangeRequest::getProjectId, projectId)
                .notIn(ConfigurationChangeRequest::getStatus, "CLOSED", "REJECTED"));

        String context = contextAssemblyService.assembleContext(projectId);

        Map<String, String> templateVars = new LinkedHashMap<>();
        templateVars.put("changeDescription", changeDescription);
        templateVars.put("baselineItems", baselineItems.isEmpty() ? "未指定基线" :
            baselineItems.stream().map(b -> String.format("- %s %s (%s)", b.get("itemCode"), b.get("itemName"), b.get("itemVersion")))
                .collect(Collectors.joining("\n")));
        templateVars.put("ciList", ciList.isEmpty() ? "无技术状态项" : String.join("\n", ciList));
        templateVars.put("docList", docList.isEmpty() ? "无关联文档" : String.join("\n", docList));
        templateVars.put("openChangeRequests", String.valueOf(openChanges));
        templateVars.put("context", context.length() > 2000 ? context.substring(0, 2000) : context);

        String userPrompt = promptTemplateService.renderString(loadPromptTemplate("change-impact"), templateVars);

        String systemPrompt = "你是一位军工项目技术状态管理专家。请分析变更对相关文档和CI的影响范围，评估变更级别和风险。返回 JSON 格式结果。";

        String response = llmClient.chat(systemPrompt, userPrompt);
        Map<String, Object> aiResult = parseResponse(response);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aiAnalysis", aiResult);
        result.put("context", Map.of(
            "totalCis", cis.size(),
            "totalDocs", docs.size(),
            "openChangeRequests", openChanges,
            "assessedBaselineItemCount", baselineItems.size()
        ));
        return result;
    }

    private String loadPromptTemplate(String name) {
        String tmpl = promptTemplateService.getTemplate(name);
        if (tmpl != null) return tmpl;
        return """
            ## 变更影响分析请求

            ### 变更描述
            {{changeDescription}}

            ### 当前基线项
            {{baselineItems}}

            ### 技术状态项(CI)
            {{ciList}}

            ### 关联文档
            {{docList}}

            ### 项目背景
            {{context}}

            ### 当前待处理变更数
            {{openChangeRequests}}

            请以JSON格式返回变更影响分析结果，格式为:
            {"changeLevel": "建议变更级别(I类重大/II类一般/III类轻微)", "affectedCis": [{"ciCode": "技术状态项编号", "impactDegree": "HIGH/MEDIUM/LOW", "impactDescription": "影响描述"}], "affectedDocs": [{"docName": "文档名称", "impactDegree": "HIGH/MEDIUM/LOW", "impactDescription": "影响描述"}], "risks": [{"description": "风险描述", "probability": "HIGH/MEDIUM/LOW", "mitigation": "缓解措施"}], "estimatedEffort": "工作量估算", "recommendation": "综合建议(是否建议变更)"}
            """;
    }

    private Map<String, Object> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            return Map.of("error", "AI 返回为空");
        }
        try {
            return objectMapper.readValue(extractJson(response), LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("Failed to parse change impact response as JSON: {}", e.getMessage());
            return Map.of("raw", response);
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
