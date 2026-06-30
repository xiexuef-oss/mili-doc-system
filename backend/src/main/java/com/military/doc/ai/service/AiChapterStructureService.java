package com.military.doc.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.ai.util.LlmOutputCleaner;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.config.LlmProperties;
import com.military.doc.modules.document.entity.DocChapter;
import com.military.doc.modules.document.mapper.DocChapterMapper;
import com.military.doc.modules.document.service.DocChapterService;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.DocTemplateV2;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.modules.template.mapper.DocTemplateV2Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class AiChapterStructureService {

    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final DocChapterService docChapterService;
    private final DocChapterMapper docChapterMapper;
    private final DocTemplateV2Mapper templateV2Mapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final ObjectMapper objectMapper;

    public AiChapterStructureService(ContextAssemblyService contextAssemblyService,
                                      PromptTemplateService promptTemplateService,
                                      LlmClient llmClient,
                                      LlmProperties llmProperties,
                                      DocChapterService docChapterService,
                                      DocChapterMapper docChapterMapper,
                                      DocTemplateV2Mapper templateV2Mapper,
                                      DocTemplateChapterMapper templateChapterMapper,
                                      ObjectMapper objectMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
        this.docChapterService = docChapterService;
        this.docChapterMapper = docChapterMapper;
        this.templateV2Mapper = templateV2Mapper;
        this.templateChapterMapper = templateChapterMapper;
        this.objectMapper = objectMapper;
    }

    /** AI-generated chapter tree node (matches prompt output format) */
    public static class AiChapterNode {
        public String chapterNumber;
        public String chapterTitle;
        public Integer chapterLevel;
        public Integer orderNum;
        public Boolean isRequired;
        public String writingTips;
        public Map<String, Object> contentSchema;
        public List<AiChapterNode> children;
    }

    /** Request to generate/optimize chapter structure */
    public static class StructureRequest {
        public Long projectId;
        public Long docLedgerId;
        public Long templateId;         // optional: reference template
        public String docType;          // document type hint
        public String additionalPrompt; // user's extra instructions
        public boolean optimize;        // true = optimize existing, false = generate new
    }

    /** Response with generated structure preview */
    public static class StructureResponse {
        public List<AiChapterNode> chapters;
        public int totalChapters;
        public String summary;
    }

    /**
     * Generate chapter structure preview using AI.
     * Does NOT persist — returns preview for user review.
     */
    public StructureResponse generatePreview(StructureRequest req) {
        String systemPrompt = promptTemplateService.getTemplate("chapter-structure-generation");
        if (systemPrompt == null) {
            throw BusinessException.serverError("AI prompt template 'chapter-structure-generation' not found");
        }

        String context = buildContext(req);
        log.info("AI chapter structure generation: projectId={}, docLedgerId={}, optimize={}",
                req.projectId, req.docLedgerId, req.optimize);

        String response = llmClient.chat(systemPrompt, context);
        List<AiChapterNode> chapters = parseResponse(response);

        if (chapters.isEmpty()) {
            throw BusinessException.serverError("AI 未能生成有效的章节结构，请尝试提供更详细的项目信息");
        }

        StructureResponse result = new StructureResponse();
        result.chapters = chapters;
        result.totalChapters = countAllChapters(chapters);
        result.summary = buildSummary(chapters, req);
        return result;
    }

    /**
     * Apply AI-generated chapter structure to a doc ledger.
     * Deletes existing chapters and replaces with AI-generated ones.
     */
    @Transactional
    public List<DocChapter> applyStructure(Long docLedgerId, List<AiChapterNode> nodes, Long operatorId) {
        // 1. Soft-delete existing chapters
        List<DocChapter> existing = docChapterMapper.selectList(
                new LambdaQueryWrapper<DocChapter>()
                        .eq(DocChapter::getDocLedgerId, docLedgerId));
        if (!existing.isEmpty()) {
            existing.forEach(dc -> dc.setDeleted(1));
            // Batch update deleted flag
            for (DocChapter dc : existing) {
                docChapterMapper.updateById(dc);
            }
            log.info("Soft-deleted {} existing chapters for docLedgerId={}", existing.size(), docLedgerId);
        }

        // 2. Create new chapters from AI tree (flatten with parent tracking)
        List<DocChapter> result = new ArrayList<>();
        Map<AiChapterNode, Long> nodeToId = new IdentityHashMap<>();
        flattenAndCreate(nodes, 0L, docLedgerId, operatorId, result, nodeToId);

        log.info("Applied AI-generated structure: {} chapters for docLedgerId={}", result.size(), docLedgerId);
        return result;
    }

    private void flattenAndCreate(List<AiChapterNode> nodes, Long parentId, Long docLedgerId,
                                   Long operatorId, List<DocChapter> result,
                                   Map<AiChapterNode, Long> nodeToId) {
        int order = 1;
        for (AiChapterNode node : nodes) {
            DocChapter dc = new DocChapter();
            dc.setDocLedgerId(docLedgerId);
            dc.setParentId(parentId);
            dc.setChapterNumber(node.chapterNumber);
            dc.setChapterTitle(node.chapterTitle);
            dc.setChapterLevel(node.chapterLevel != null ? node.chapterLevel : 1);
            dc.setOrderNum(node.orderNum != null ? node.orderNum : order);
            dc.setFillStatus("EMPTY");
            dc.setFillPercentage(0);
            dc.setCreatedBy(operatorId);
            dc.setUpdatedBy(operatorId);

            // Store writing tips and content schema in contentJson
            Map<String, Object> meta = new LinkedHashMap<>();
            if (node.isRequired != null) meta.put("isRequired", node.isRequired);
            if (node.writingTips != null) meta.put("writingTips", node.writingTips);
            if (node.contentSchema != null) meta.put("contentSchema", node.contentSchema);
            if (!meta.isEmpty()) {
                try {
                    dc.setContentJson(objectMapper.writeValueAsString(meta));
                } catch (Exception e) {
                    log.warn("Failed to serialize chapter meta for {}", node.chapterNumber, e);
                }
            }

            docChapterMapper.insert(dc);
            result.add(dc);
            nodeToId.put(node, dc.getId());

            // Recursively create children
            if (node.children != null && !node.children.isEmpty()) {
                flattenAndCreate(node.children, dc.getId(), docLedgerId, operatorId,
                        result, nodeToId);
            }
            order++;
        }
    }

    private String buildContext(StructureRequest req) {
        StringBuilder sb = new StringBuilder();

        // 1. Core task description
        sb.append("## 任务\n");
        if (req.optimize) {
            sb.append("请优化以下文档的现有章节结构，补充缺失的必要章节，调整不合理的层级关系，完善编写提示。\n\n");
        } else {
            sb.append("请为以下文档生成完整的章节结构。\n\n");
        }

        // 2. Project context
        if (req.projectId != null) {
            String projectContext = contextAssemblyService.assembleContext(req.projectId, null);
            // Truncate if too long to avoid overwhelming the model
            if (projectContext.length() > 12000) {
                projectContext = projectContext.substring(0, 12000) + "\n...[上下文已截断]";
            }
            sb.append("## 项目背景\n").append(projectContext).append("\n\n");
        }

        // 3. Document type hint
        if (req.docType != null) {
            sb.append("## 文档类型\n").append(req.docType).append("\n\n");
        }

        // 4. Reference template (if specified)
        if (req.templateId != null) {
            DocTemplateV2 template = templateV2Mapper.selectById(req.templateId);
            if (template != null) {
                sb.append("## 参考模板\n");
                sb.append("模板名称：").append(template.getTemplateName()).append("\n");
                sb.append("适用标准：").append(
                        template.getGjbStandardRef() != null ? template.getGjbStandardRef() : "GJB 通用").append("\n");

                List<DocTemplateChapter> tplChapters = templateChapterMapper.selectList(
                        new LambdaQueryWrapper<DocTemplateChapter>()
                                .eq(DocTemplateChapter::getTemplateId, req.templateId)
                                .orderByAsc(DocTemplateChapter::getOrderNum));
                if (!tplChapters.isEmpty()) {
                    sb.append("现有模板章节：\n");
                    for (DocTemplateChapter tc : tplChapters) {
                        sb.append("  ").append(tc.getChapterNumber()).append(" ")
                                .append(tc.getChapterTitle())
                                .append(" (层级").append(tc.getChapterLevel()).append(")")
                                .append(tc.getParentId() != null && tc.getParentId() > 0 ? " [子章节]" : "")
                                .append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        // 5. Existing chapters (for optimize mode)
        if (req.optimize && req.docLedgerId != null) {
            List<DocChapter> existing = docChapterMapper.selectList(
                    new LambdaQueryWrapper<DocChapter>()
                            .eq(DocChapter::getDocLedgerId, req.docLedgerId)
                            .orderByAsc(DocChapter::getOrderNum));
            if (!existing.isEmpty()) {
                sb.append("## 现有章节结构（需要优化）\n");
                for (DocChapter dc : existing) {
                    sb.append("  ").append(dc.getChapterNumber()).append(" ")
                            .append(dc.getChapterTitle())
                            .append(" [fillStatus=").append(dc.getFillStatus()).append("]")
                            .append(dc.getParentId() != null && dc.getParentId() > 0 ? " [子章节，parentId=" + dc.getParentId() + "]" : "")
                            .append("\n");
                }
                sb.append("\n");
            }
        }

        // 6. User's additional instructions
        if (req.additionalPrompt != null && !req.additionalPrompt.isBlank()) {
            sb.append("## 用户额外要求\n").append(req.additionalPrompt).append("\n\n");
        }

        return sb.toString();
    }

    private List<AiChapterNode> parseResponse(String response) {
        // Log truncated response for debugging
        String preview = response.length() > 500 ? response.substring(0, 500) + "..." : response;
        log.info("Parsing AI response ({} chars): {}", response.length(), preview);

        String json = extractJson(response);
        if (json == null || json.equals(response)) {
            log.error("No JSON array found in AI response. Raw: {}", preview);
            throw BusinessException.serverError("AI 未返回有效的章节结构数据，请重试并确认文档类型描述准确");
        }

        try {
            List<AiChapterNode> nodes = objectMapper.readValue(json, new TypeReference<List<AiChapterNode>>() {});
            // Post-process: ensure children is never null
            nodes.forEach(this::sanitizeNode);
            return nodes;
        } catch (Exception e) {
            log.error("Failed to parse AI response as chapter nodes. Error: {}", e.getMessage());
            log.error("Raw JSON (first 2000 chars): {}", json.substring(0, Math.min(2000, json.length())));

            // Fallback: try parsing as generic List of Maps and convert
            try {
                List<Map<String, Object>> raw = objectMapper.readValue(json,
                        new TypeReference<List<Map<String, Object>>>() {});
                List<AiChapterNode> nodes = new ArrayList<>();
                for (Map<String, Object> m : raw) {
                    nodes.add(convertFromMap(m));
                }
                log.info("Fallback parse succeeded: {} nodes", nodes.size());
                return nodes;
            } catch (Exception e2) {
                log.error("Fallback parse also failed: {}", e2.getMessage());
            }

            throw BusinessException.serverError("AI 返回的章节结构格式无法解析，请重试");
        }
    }

    private String extractJson(String response) {
        return LlmOutputCleaner.extractJsonArray(response, true);
    }

    private void sanitizeNode(AiChapterNode node) {
        if (node.children == null) node.children = List.of();
        for (AiChapterNode child : node.children) {
            sanitizeNode(child);
        }
    }

    @SuppressWarnings("unchecked")
    private AiChapterNode convertFromMap(Map<String, Object> map) {
        AiChapterNode node = new AiChapterNode();
        node.chapterNumber = (String) map.get("chapterNumber");
        node.chapterTitle = (String) map.get("chapterTitle");
        node.chapterLevel = map.get("chapterLevel") instanceof Number n ? n.intValue() : 1;
        node.orderNum = map.get("orderNum") instanceof Number n ? n.intValue() : 1;
        node.isRequired = map.get("isRequired") instanceof Boolean b ? b : false;
        node.writingTips = (String) map.get("writingTips");
        node.contentSchema = (Map<String, Object>) map.get("contentSchema");
        List<Map<String, Object>> rawChildren = (List<Map<String, Object>>) map.get("children");
        node.children = new ArrayList<>();
        if (rawChildren != null) {
            for (Map<String, Object> childMap : rawChildren) {
                node.children.add(convertFromMap(childMap));
            }
        }
        return node;
    }

    private int countAllChapters(List<AiChapterNode> nodes) {
        int count = 0;
        for (AiChapterNode node : nodes) {
            count++;
            if (node.children != null && !node.children.isEmpty()) {
                count += countAllChapters(node.children);
            }
        }
        return count;
    }

    private String buildSummary(List<AiChapterNode> chapters, StructureRequest req) {
        int total = countAllChapters(chapters);
        int maxDepth = maxDepth(chapters, 0);
        int requiredCount = countRequired(chapters);

        StringBuilder sb = new StringBuilder();
        sb.append("AI 已生成 ").append(total).append(" 个章节");
        if (maxDepth > 1) {
            sb.append("（最大层级 ").append(maxDepth).append(" 级）");
        }
        if (requiredCount > 0) {
            sb.append("，其中 ").append(requiredCount).append(" 个为必填项");
        }
        sb.append("。");
        if (req.optimize) {
            sb.append("请预览后确认是否应用。");
        }
        return sb.toString();
    }

    private int maxDepth(List<AiChapterNode> nodes, int currentDepth) {
        int max = currentDepth + 1;
        for (AiChapterNode node : nodes) {
            if (node.children != null && !node.children.isEmpty()) {
                max = Math.max(max, maxDepth(node.children, currentDepth + 1));
            }
        }
        return max;
    }

    private int countRequired(List<AiChapterNode> nodes) {
        int count = 0;
        for (AiChapterNode node : nodes) {
            if (node.isRequired != null && node.isRequired) count++;
            if (node.children != null && !node.children.isEmpty()) {
                count += countRequired(node.children);
            }
        }
        return count;
    }
}
