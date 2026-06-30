package com.military.doc.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.util.LlmOutputCleaner;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class ArchiveAdvisorService {

    private final LlmClient llmClient;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final DocLedgerMapper docLedgerMapper;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ArchiveAdvisorService(LlmClient llmClient, ContextAssemblyService contextAssemblyService,
                                  PromptTemplateService promptTemplateService, DocLedgerMapper docLedgerMapper,
                                  FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.docLedgerMapper = docLedgerMapper;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> advise(Long docLedgerId) {
        DocLedger doc = docLedgerMapper.selectById(docLedgerId);
        if (doc == null) {
            throw BusinessException.notFound("文档不存在: " + docLedgerId);
        }

        String content = readDocContent(doc);
        String context = contextAssemblyService.assembleContext(doc.getProjectId());

        String userPrompt = promptTemplateService.renderString(
            loadPromptTemplate("archive-advice"),
            Map.of("docName", doc.getDocName() != null ? doc.getDocName() : "",
                   "docType", doc.getDocType() != null ? doc.getDocType() : "",
                   "currentSecurityLevel", doc.getSecurityLevel() != null ? doc.getSecurityLevel() : "内部",
                   "lifecycleStatus", doc.getLifecycleStatus() != null ? doc.getLifecycleStatus() : "DRAFT",
                   "content", content.length() > 5000 ? content.substring(0, 5000) : content,
                   "context", context.length() > 2000 ? context.substring(0, 2000) : context));

        String systemPrompt = "你是一位军工文档归档管理专家，精通《军队档案管理条例》、GJB 5882-2006 文档分类体系和军工产品技术文件归档规范。请从密级合理性、保管期限（永久/30年/10年/5年）、归档类别（按 GJB 5882 十五大类）、归档风险四个维度提供归档建议。返回 JSON 格式结果。";

        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            return parseResponse(response);
        } catch (RuntimeException e) {
            log.error("Archive advice failed: {}", e.getMessage());
            throw BusinessException.serverError("AI 归档建议服务不可用: " + e.getMessage());
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
            ## 文档归档建议请求
            文档名称: {{docName}}
            文档类型: {{docType}}
            当前密级: {{currentSecurityLevel}}
            文档状态: {{lifecycleStatus}}

            ### 文档内容
            {{content}}

            ### 项目背景
            {{context}}

            请以JSON格式返回归档建议，格式为:
            {"recommendedSecurityLevel": "推荐密级(公开/内部/秘密/机密/绝密)", "securityLevelRationale": "密级评估依据", "retentionPeriod": "建议保管期限(如:永久/30年/10年)", "retentionRationale": "保管期限依据", "archiveCategory": "归档类别(如:产品技术文件/质量管理文件/项目管理文件)", "risks": ["归档风险项"], "summary": "综合归档建议"}
            """;
    }

    private Map<String, Object> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            throw BusinessException.serverError("AI 返回为空");
        }
        try {
            return objectMapper.readValue(LlmOutputCleaner.extractJsonObject(response, false), LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("Failed to parse archive advice response as JSON: {}", e.getMessage());
            throw BusinessException.serverError("AI 归档建议结果格式解析失败，请重试");
        }
    }

}
