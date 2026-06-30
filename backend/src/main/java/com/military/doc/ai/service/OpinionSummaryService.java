package com.military.doc.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.modules.review.entity.ReviewExpertOpinionFile;
import com.military.doc.modules.review.mapper.ReviewExpertOpinionFileMapper;
import com.military.doc.ai.util.LlmOutputCleaner;
import com.military.doc.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpinionSummaryService {

    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final ReviewExpertOpinionFileMapper opinionMapper;
    private final ObjectMapper objectMapper;

    public OpinionSummaryService(LlmClient llmClient, PromptTemplateService promptTemplateService,
                                  ReviewExpertOpinionFileMapper opinionMapper, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.promptTemplateService = promptTemplateService;
        this.opinionMapper = opinionMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> summarize(Long meetingId) {
        List<ReviewExpertOpinionFile> opinions = opinionMapper.selectList(
            new LambdaQueryWrapper<ReviewExpertOpinionFile>()
                .eq(ReviewExpertOpinionFile::getMeetingId, meetingId));

        if (opinions.isEmpty()) {
            return Map.of("message", "该会议暂无专家意见文件");
        }

        String opinionsText = opinions.stream()
            .map(o -> String.format("- 专家组: %s | 文档ID: %s | 问题级别: %s | 上传时间: %s",
                o.getExpertGroupName() != null ? o.getExpertGroupName() : "未指定",
                o.getDocFileId() != null ? o.getDocFileId().toString() : "未关联",
                o.getProblemLevel() != null ? o.getProblemLevel() : "未标注",
                o.getUploadedAt() != null ? o.getUploadedAt().toString() : "未知"))
            .collect(Collectors.joining("\n"));

        String userPrompt = promptTemplateService.renderString(
            loadPromptTemplate("opinion-summary"),
            Map.of("opinions", opinionsText));

        String systemPrompt = "你是一位军工项目评审秘书，精通 GJB 3206B 评审管理要求。请按问题严重程度（重大/一般/建议）和影响领域（技术方案/标准符合性/质量保证/文档规范性）分类汇总所有专家意见，形成 PASSED/CONDITIONAL/FAILED 的评审结论和具体整改行动项。返回 JSON 格式结果。";

        try {
            String response = llmClient.chat(systemPrompt, userPrompt);
            return parseResponse(response);
        } catch (RuntimeException e) {
            log.error("Opinion summary failed: {}", e.getMessage());
            throw BusinessException.serverError("AI 意见汇总服务不可用: " + e.getMessage());
        }
    }

    private String loadPromptTemplate(String name) {
        String tmpl = promptTemplateService.getTemplate(name);
        if (tmpl != null) return tmpl;
        return """
            ## 专家意见汇总请求

            ### 所有专家意见文件
            {{opinions}}

            请以JSON格式返回汇总结果，格式为:
            {"summaryConclusion": "评审结论建议(PASSED/CONDITIONAL/FAILED)", "keyIssues": [{"issue": "主要问题描述", "relatedDocs": ["相关文档ID"], "severity": "MAJOR/MINOR"}], "actionItems": [{"action": "待办事项", "responsible": "责任方建议"}], "overallOpinion": "总体评审意见概述"}
            """;
    }

    private Map<String, Object> parseResponse(String response) {
        if (response == null || response.isBlank()) {
            throw BusinessException.serverError("AI 返回为空");
        }
        try {
            return objectMapper.readValue(LlmOutputCleaner.extractJsonObject(response, false), LinkedHashMap.class);
        } catch (Exception e) {
            log.warn("Failed to parse opinion summary response as JSON: {}", e.getMessage());
            throw BusinessException.serverError("AI 意见汇总结果格式解析失败，请重试");
        }
    }

}
