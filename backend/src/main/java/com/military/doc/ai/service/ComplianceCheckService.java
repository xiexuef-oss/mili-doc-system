package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.project.entity.ConfigurationBaseline;
import com.military.doc.modules.project.entity.ConfigurationBaselineItem;
import com.military.doc.modules.project.mapper.ConfigurationBaselineItemMapper;
import com.military.doc.modules.project.mapper.ConfigurationBaselineMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ComplianceCheckService {

    private final LlmClient llmClient;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final ConfigurationBaselineMapper baselineMapper;
    private final ConfigurationBaselineItemMapper baselineItemMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ComplianceCheckService(LlmClient llmClient, ContextAssemblyService contextAssemblyService,
                                   PromptTemplateService promptTemplateService,
                                   ConfigurationBaselineMapper baselineMapper,
                                   ConfigurationBaselineItemMapper baselineItemMapper,
                                   DocLedgerMapper docLedgerMapper,
                                   FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.baselineMapper = baselineMapper;
        this.baselineItemMapper = baselineItemMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> check(Long projectId, Long baselineId) {
        ConfigurationBaseline baseline = baselineMapper.selectById(baselineId);
        if (baseline == null) {
            return Map.of("error", "基线不存在: " + baselineId);
        }

        // Collect baseline item documents
        List<ConfigurationBaselineItem> items = baselineItemMapper.selectList(
            new LambdaQueryWrapper<ConfigurationBaselineItem>()
                .eq(ConfigurationBaselineItem::getBaselineId, baselineId));
        List<Map<String, String>> docContents = new ArrayList<>();
        for (ConfigurationBaselineItem item : items) {
            if ("DOCUMENT".equals(item.getItemType()) && item.getItemId() != null) {
                DocLedger doc = docLedgerMapper.selectById(item.getItemId());
                if (doc != null) {
                    String content = readDocContent(doc);
                    docContents.add(Map.of(
                        "docCode", item.getItemCode() != null ? item.getItemCode() : "",
                        "docName", item.getItemName() != null ? item.getItemName() : "",
                        "content", content.length() > 2000 ? content.substring(0, 2000) : content));
                }
            }
        }

        String context = contextAssemblyService.assembleContext(projectId);
        String docsSummary = docContents.stream()
            .map(d -> String.format("- %s %s\n内容摘要: %s", d.get("docCode"), d.get("docName"), d.get("content")))
            .collect(Collectors.joining("\n\n"));

        String userPrompt = promptTemplateService.renderString(
            loadPromptTemplate("compliance-check"),
            Map.of("baselineName", baseline.getBaselineName() != null ? baseline.getBaselineName() : "",
                   "baselineType", baseline.getBaselineType() != null ? baseline.getBaselineType() : "",
                   "docsSummary", docsSummary,
                   "context", context.length() > 2000 ? context.substring(0, 2000) : context));

        String systemPrompt = "你是一位军工标准合规审查专家。请评估基线文件与适用标准条款的符合性。返回 JSON 格式结果。";

        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            return parseResponse(response);
        } catch (RuntimeException e) {
            log.error("Compliance check failed: {}", e.getMessage());
            return Map.of("error", "AI 合规检查服务不可用: " + e.getMessage());
        }
    }

    private String readDocContent(DocLedger doc) {
        if (doc.getFileObjectId() == null) return "[文档内容为空]";
        try (var is = fileStorageService.download(doc.getFileObjectId())) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read doc content: {}", e.getMessage());
            return "[无法读取文档内容]";
        }
    }

    private String loadPromptTemplate(String name) {
        String tmpl = promptTemplateService.getTemplate(name);
        if (tmpl != null) return tmpl;
        return """
            ## 标准合规检查请求
            基线名称: {{baselineName}}
            基线类型: {{baselineType}}

            ### 基线文件
            {{docsSummary}}

            ### 适用标准
            {{context}}

            请以JSON格式返回合规检查结果，格式为:
            {"overallCompliance": "COMPLIANT/PARTIAL/NON_COMPLIANT", "score": 0-100, "items": [{"docCode": "文档编号", "compliance": "COMPLIANT/PARTIAL/NON_COMPLIANT", "issues": ["不符合项"], "suggestions": ["改进建议"]}], "summary": "总体合规评价"}
            """;
    }

    private Map<String, Object> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            return Map.of("error", "AI 返回为空");
        }
        try {
            return objectMapper.readValue(extractJson(response), LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("Failed to parse compliance check response as JSON: {}", e.getMessage());
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
