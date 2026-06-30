package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.util.LlmOutputCleaner;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.project.entity.ConfigurationAudit;
import com.military.doc.modules.project.entity.ConfigurationBaseline;
import com.military.doc.modules.project.entity.ConfigurationChangeRequest;
import com.military.doc.modules.project.entity.ProjectStage;
import com.military.doc.modules.project.mapper.ConfigurationAuditMapper;
import com.military.doc.modules.project.mapper.ConfigurationBaselineMapper;
import com.military.doc.modules.project.mapper.ConfigurationChangeRequestMapper;
import com.military.doc.modules.project.mapper.ProjectStageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StageReadinessService {

    private final LlmClient llmClient;
    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final DocLedgerMapper docLedgerMapper;
    private final ProjectStageMapper stageMapper;
    private final ConfigurationBaselineMapper baselineMapper;
    private final ConfigurationChangeRequestMapper changeRequestMapper;
    private final ConfigurationAuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    public StageReadinessService(LlmClient llmClient, ContextAssemblyService contextAssemblyService,
                                  PromptTemplateService promptTemplateService,
                                  DocLedgerMapper docLedgerMapper, ProjectStageMapper stageMapper,
                                  ConfigurationBaselineMapper baselineMapper,
                                  ConfigurationChangeRequestMapper changeRequestMapper,
                                  ConfigurationAuditMapper auditMapper, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.docLedgerMapper = docLedgerMapper;
        this.stageMapper = stageMapper;
        this.baselineMapper = baselineMapper;
        this.changeRequestMapper = changeRequestMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> assess(Long projectId, Long stageId) {
        ProjectStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw BusinessException.notFound("阶段不存在: " + stageId);
        }

        // Collect metrics
        List<DocLedger> docs = docLedgerMapper.selectList(new LambdaQueryWrapper<DocLedger>()
                .eq(DocLedger::getProjectId, projectId)
                .eq(DocLedger::getStageId, stageId));
        long totalDocs = docs.size();
        long releasedDocs = docs.stream().filter(d -> "RELEASED".equals(d.getLifecycleStatus()) || "ARCHIVED".equals(d.getLifecycleStatus())).count();

        List<ConfigurationBaseline> baselines = baselineMapper.selectList(new LambdaQueryWrapper<ConfigurationBaseline>()
                .eq(ConfigurationBaseline::getProjectId, projectId)
                .eq(ConfigurationBaseline::getStageId, stageId));
        boolean hasEffectiveBaseline = baselines.stream().anyMatch(b -> "EFFECTIVE".equals(b.getBaselineStatus()));

        Long openChanges = changeRequestMapper.selectCount(new LambdaQueryWrapper<ConfigurationChangeRequest>()
                .eq(ConfigurationChangeRequest::getProjectId, projectId)
                .eq(ConfigurationChangeRequest::getStageId, stageId)
                .notIn(ConfigurationChangeRequest::getStatus, "CLOSED", "REJECTED"));

        List<ConfigurationAudit> audits = auditMapper.selectList(new LambdaQueryWrapper<ConfigurationAudit>()
                .eq(ConfigurationAudit::getProjectId, projectId)
                .eq(ConfigurationAudit::getStageId, stageId));
        boolean hasCompletedAudit = audits.stream().anyMatch(a -> "COMPLETED".equals(a.getAuditStatus()) && "PASSED".equals(a.getAuditResult()));

        String context = contextAssemblyService.assembleContext(projectId);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("stageName", stage.getStageName());
        metrics.put("stageStatus", stage.getStatus());
        metrics.put("totalDocs", totalDocs);
        metrics.put("releasedDocs", releasedDocs);
        metrics.put("docCompletionRate", totalDocs > 0 ? Math.round(100.0 * releasedDocs / totalDocs) : 0);
        metrics.put("hasEffectiveBaseline", hasEffectiveBaseline);
        metrics.put("baselineCount", baselines.size());
        metrics.put("openChangeRequests", openChanges);
        metrics.put("hasCompletedAudit", hasCompletedAudit);
        metrics.put("auditCount", audits.size());

        // Build prompts for AI assessment
        String userPrompt = promptTemplateService.renderString(
            loadPromptTemplate("stage-readiness"),
            Map.of("metrics", metrics.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n")),
                "context", context.length() > 2000 ? context.substring(0, 2000) : context));

        String systemPrompt = "你是一位军工项目管理专家，精通 GJB 3206B 技术状态管理和军工产品研制七阶段（L/F/C/S/D/P/N）管理要求。请从文档齐套性、基线状态、更改控制、技术状态审核、风险状态、阶段特有要求六个维度评估转阶段准备度。返回 JSON 格式结果。";

        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            Map<String, Object> aiResult = parseResponse(response);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("metrics", metrics);
            result.put("aiAssessment", aiResult);
            return result;
        } catch (RuntimeException e) {
            log.error("Stage readiness assessment failed: {}", e.getMessage());
            throw BusinessException.serverError("AI 评估服务不可用: " + e.getMessage());
        }
    }

    private String loadPromptTemplate(String name) {
        String tmpl = promptTemplateService.getTemplate(name);
        if (tmpl != null) return tmpl;
        return """
            ## 转阶段准备度评估请求

            ### 阶段指标
            {{metrics}}

            ### 项目背景
            {{context}}

            请以JSON格式返回评估结果，格式为:
            {"readiness": "READY/CONDITIONAL/NOT_READY", "score": 0-100, "blockers": [{"description": "阻塞项描述", "suggestion": "解决方案"}], "risks": [{"risk": "风险描述", "probability": "HIGH/MEDIUM/LOW", "mitigation": "缓解措施"}], "recommendation": "综合建议"}
            """;
    }

    private Map<String, Object> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            throw BusinessException.serverError("AI 返回为空");
        }
        try {
            return objectMapper.readValue(LlmOutputCleaner.extractJsonObject(response, false), LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("Failed to parse readiness response as JSON: {}", e.getMessage());
            throw BusinessException.serverError("AI 转阶段评估结果格式解析失败，请重试");
        }
    }

}
