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
public class ProofreadingService {

    private final LlmClient llmClient;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final DocLedgerMapper docLedgerMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ProofreadingService(LlmClient llmClient, ContextAssemblyService contextAssemblyService,
                                PromptTemplateService promptTemplateService, DocLedgerMapper docLedgerMapper,
                                FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.docLedgerMapper = docLedgerMapper;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> proofread(Long docLedgerId) {
        DocLedger doc = docLedgerMapper.selectById(docLedgerId);
        if (doc == null) {
            return Map.of("error", "文档不存在: " + docLedgerId);
        }

        String content = readDocContent(doc);
        String context = contextAssemblyService.assembleContext(doc.getProjectId());

        String userPrompt = promptTemplateService.renderString(
            loadPromptTemplate("proofread"),
            Map.of("docName", doc.getDocName() != null ? doc.getDocName() : "",
                   "docType", doc.getDocType() != null ? doc.getDocType() : "",
                   "content", content.length() > 8000 ? content.substring(0, 8000) : content,
                   "context", context.length() > 2000 ? context.substring(0, 2000) : context));

        String systemPrompt = "你是一位军工文档标准化审查专家。请对文档进行校对，检查格式、术语、标准条款引用等方面的合规性。返回 JSON 格式结果。";

        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            return parseResponse(response);
        } catch (RuntimeException e) {
            log.error("Proofreading failed: {}", e.getMessage());
            return Map.of("error", "AI 校对服务不可用: " + e.getMessage());
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
            ## 文档校对请求
            文档名称: {{docName}}
            文档类型: {{docType}}

            ### 文档内容
            {{content}}

            ### 项目背景
            {{context}}

            请以JSON格式返回校对结果，格式为: {"issues": [{"severity": "ERROR/WARNING/INFO", "location": "定位", "description": "问题描述", "suggestion": "修改建议"}], "summary": "总体评价"}
            """;
    }

    private Map<String, Object> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            return Map.of("error", "AI 返回为空");
        }
        try {
            return objectMapper.readValue(extractJson(response), LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("Failed to parse proofread response as JSON: {}", e.getMessage());
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
