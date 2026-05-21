package com.military.doc.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.LinkedHashMap;

@Slf4j
@Service
public class PreReviewService {

    private final LlmClient llmClient;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final DocLedgerMapper docLedgerMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public PreReviewService(LlmClient llmClient, ContextAssemblyService contextAssemblyService,
                             PromptTemplateService promptTemplateService, DocLedgerMapper docLedgerMapper,
                             FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.docLedgerMapper = docLedgerMapper;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> preReview(Long docLedgerId) {
        DocLedger doc = docLedgerMapper.selectById(docLedgerId);
        if (doc == null) {
            return Map.of("error", "文档不存在: " + docLedgerId);
        }

        String content = readDocContent(doc);
        String context = contextAssemblyService.assembleContext(doc.getProjectId());

        String userPrompt = promptTemplateService.renderString(
            loadPromptTemplate("pre-review"),
            Map.of("docName", doc.getDocName() != null ? doc.getDocName() : "",
                   "docType", doc.getDocType() != null ? doc.getDocType() : "",
                   "content", content.length() > 8000 ? content.substring(0, 8000) : content,
                   "context", context.length() > 3000 ? context.substring(0, 3000) : context));

        String systemPrompt = "你是一位军工文档评审专家，精通 GJB 5882-2006 三维分类体系（阶段×内容×形式）、GJB 3206B 技术状态管理。请从标准符合性、阶段匹配度、文档间一致性、条款覆盖率四个维度进行预评审。返回 JSON 格式结果。";

        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            return parseResponse(response);
        } catch (RuntimeException e) {
            log.error("Pre-review failed: {}", e.getMessage());
            return Map.of("error", "AI 预评审服务不可用: " + e.getMessage());
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
            ## 文档预评审请求
            文档名称: {{docName}}
            文档类型: {{docType}}

            ### 文档内容
            {{content}}

            ### 适用标准
            {{context}}

            请以JSON格式返回预评审结果，格式为:
            {"complianceScore": 0-100, "completenessScore": 0-100, "issues": [{"clauseRef": "标准条款引用", "status": "COMPLIANT/PARTIAL/NON_COMPLIANT", "description": "评估说明"}], "recommendations": ["改进建议1", "改进建议2"], "summary": "总体评审意见"}
            """;
    }

    private Map<String, Object> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            return Map.of("error", "AI 返回为空");
        }
        try {
            return objectMapper.readValue(extractJson(response), LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("Failed to parse pre-review response as JSON: {}", e.getMessage());
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
