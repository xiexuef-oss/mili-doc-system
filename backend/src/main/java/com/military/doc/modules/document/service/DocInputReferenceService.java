package com.military.doc.modules.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.modules.document.entity.DocInputReference;
import com.military.doc.modules.document.entity.StageDocChecklistTemplate;
import com.military.doc.modules.document.mapper.DocInputReferenceMapper;
import com.military.doc.modules.document.mapper.StageDocChecklistTemplateMapper;
import com.military.doc.modules.knowledge.entity.KnowledgeCard;
import com.military.doc.modules.knowledge.mapper.KnowledgeCardMapper;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.mapper.StandardClauseMapper;
import com.military.doc.modules.standard.mapper.StandardMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.DocTemplateV2;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.modules.template.mapper.DocTemplateV2Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档输入参考服务 — 串联四环节中的第2环节。
 * 根据文档清单模板，查询其输入参考（上游文档 + 引用标准 + 知识卡片）和写作模板。
 */
@Slf4j
@Service
public class DocInputReferenceService {

    private final DocInputReferenceMapper inputRefMapper;
    private final StageDocChecklistTemplateMapper checklistTemplateMapper;
    private final DocTemplateV2Mapper templateV2Mapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final StandardClauseMapper standardClauseMapper;
    private final StandardMapper standardMapper;
    private final KnowledgeCardMapper knowledgeCardMapper;

    public DocInputReferenceService(DocInputReferenceMapper inputRefMapper,
                                    StageDocChecklistTemplateMapper checklistTemplateMapper,
                                    DocTemplateV2Mapper templateV2Mapper,
                                    DocTemplateChapterMapper templateChapterMapper,
                                    StandardClauseMapper standardClauseMapper,
                                    StandardMapper standardMapper,
                                    KnowledgeCardMapper knowledgeCardMapper) {
        this.inputRefMapper = inputRefMapper;
        this.checklistTemplateMapper = checklistTemplateMapper;
        this.templateV2Mapper = templateV2Mapper;
        this.templateChapterMapper = templateChapterMapper;
        this.standardClauseMapper = standardClauseMapper;
        this.standardMapper = standardMapper;
        this.knowledgeCardMapper = knowledgeCardMapper;
    }

    /**
     * 查询文档清单模板的所有输入参考，按类型分组返回。
     */
    public Map<String, List<DocInputReference>> getInputReferencesByType(Long checklistTemplateId) {
        List<DocInputReference> all = inputRefMapper.selectList(
            new LambdaQueryWrapper<DocInputReference>()
                .eq(DocInputReference::getChecklistTemplateId, checklistTemplateId)
                .orderByAsc(DocInputReference::getOrderNum));

        return all.stream()
            .collect(Collectors.groupingBy(DocInputReference::getRefType,
                LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 根据 doc_code 查询清单模板，再查输入参考。
     */
    public Map<String, List<DocInputReference>> getInputReferencesByDocCode(String docCode) {
        StageDocChecklistTemplate tmpl = checklistTemplateMapper.selectOne(
            new LambdaQueryWrapper<StageDocChecklistTemplate>()
                .eq(StageDocChecklistTemplate::getDocCode, docCode));
        if (tmpl == null) return Collections.emptyMap();
        return getInputReferencesByType(tmpl.getId());
    }

    /**
     * 组装文档写作的完整上下文 — 供 AI 初稿生成使用。
     * 包括：上游文档清单 + 引用标准条款 + 写作模板章节结构 + 知识卡片。
     */
    public String assembleDocumentContext(Long checklistTemplateId, Long projectId) {
        StringBuilder ctx = new StringBuilder();

        StageDocChecklistTemplate checklistTmpl = checklistTemplateMapper.selectById(checklistTemplateId);
        if (checklistTmpl == null) {
            log.warn("Checklist template not found: {}", checklistTemplateId);
            return "";
        }

        Map<String, List<DocInputReference>> refs = getInputReferencesByType(checklistTemplateId);

        // 1. GJB6387 规范类型说明
        if (checklistTmpl.getSpecType() != null) {
            ctx.append("## 规范类型\n");
            ctx.append("- 类型: ").append(getSpecTypeLabel(checklistTmpl.getSpecType())).append("\n");
            ctx.append("- 说明: ").append(getSpecTypeDescription(checklistTmpl.getSpecType())).append("\n");
            ctx.append("- 引用标准: ").append(nullToEmpty(checklistTmpl.getGjbReference())).append("\n\n");
        }

        // 2. 上游输入文档
        List<DocInputReference> upstreamDocs = refs.getOrDefault("UPSTREAM_DOC", Collections.emptyList());
        if (!upstreamDocs.isEmpty()) {
            ctx.append("## 上游输入文档（本文档编写前需参考的文档）\n");
            for (DocInputReference ref : upstreamDocs) {
                ctx.append("- **").append(nullToEmpty(ref.getRefCode())).append("** ")
                    .append(nullToEmpty(ref.getRefName())).append("\n");
                if (ref.getRefUsage() != null && !ref.getRefUsage().isBlank()) {
                    ctx.append("  用途: ").append(ref.getRefUsage()).append("\n");
                }
                if (Boolean.FALSE.equals(ref.getIsRequired())) {
                    ctx.append("  (可选参考)\n");
                }
            }
            ctx.append("\n");
        }

        // 3. 引用军用标准
        List<DocInputReference> standards = refs.getOrDefault("STANDARD", Collections.emptyList());
        if (!standards.isEmpty()) {
            ctx.append("## 引用军用标准\n");
            for (DocInputReference ref : standards) {
                ctx.append("- **").append(nullToEmpty(ref.getRefCode())).append("**")
                    .append(": ").append(nullToEmpty(ref.getRefName())).append("\n");
                if (ref.getRefUsage() != null && !ref.getRefUsage().isBlank()) {
                    ctx.append("  用途: ").append(ref.getRefUsage()).append("\n");
                }
            }
            ctx.append("\n");
        }

        // 4. 写作模板章节结构
        if (checklistTmpl.getTemplateId() != null) {
            DocTemplateV2 template = templateV2Mapper.selectById(checklistTmpl.getTemplateId());
            if (template != null) {
                ctx.append("## 写作模板: ").append(template.getTemplateName()).append("\n");
                ctx.append("- 模板标准: ").append(nullToEmpty(template.getGjbStandardRef())).append("\n\n");

                // Load ALL template chapters (not just level 1) with full context
            List<DocTemplateChapter> allTplChapters = templateChapterMapper.selectList(
                    new LambdaQueryWrapper<DocTemplateChapter>()
                        .eq(DocTemplateChapter::getTemplateId, template.getId())
                        .orderByAsc(DocTemplateChapter::getOrderNum));

                // Render complete template structure with descriptions and writing tips
                Map<Long, List<DocTemplateChapter>> byParent = new java.util.LinkedHashMap<>();
                for (DocTemplateChapter ch : allTplChapters) {
                    byParent.computeIfAbsent(ch.getParentId() != null ? ch.getParentId() : 0L,
                        k -> new ArrayList<>()).add(ch);
                }
                List<DocTemplateChapter> roots = byParent.getOrDefault(0L, List.of());
                ctx.append("### 章节结构（必须严格遵循此结构撰写）\n\n");
                renderTemplateChapters(roots, byParent, ctx, 0);
            }
        }

        // 5. 知识卡片
        List<DocInputReference> cards = refs.getOrDefault("KNOWLEDGE_CARD", Collections.emptyList());
        if (!cards.isEmpty()) {
            ctx.append("## 相关知识卡片\n");
            for (DocInputReference ref : cards) {
                if (ref.getRefId() != null) {
                    KnowledgeCard card = knowledgeCardMapper.selectById(ref.getRefId());
                    if (card != null) {
                        ctx.append("- **").append(card.getTitle()).append("**\n");
                        if (card.getPlainLanguage() != null) {
                            ctx.append("  ").append(card.getPlainLanguage()).append("\n");
                        }
                    }
                } else {
                    ctx.append("- **").append(nullToEmpty(ref.getRefName())).append("**\n");
                    if (ref.getRefUsage() != null) {
                        ctx.append("  ").append(ref.getRefUsage()).append("\n");
                    }
                }
            }
            ctx.append("\n");
        }

        String result = ctx.toString();
        log.info("Assembled doc context: checklistTmpl={}, {} chars", checklistTemplateId, result.length());
        return result;
    }

    private String getSpecTypeLabel(String specType) {
        return switch (specType) {
            case "SYSTEM_SPEC" -> "系统规范(A类) — 功能基线(FBL)";
            case "DEV_SPEC" -> "研制规范(B类) — 分配基线(ABL)";
            case "PRODUCT_SPEC" -> "产品规范(C类) — 产品基线(PBL)";
            case "SOFTWARE_SPEC" -> "软件规范";
            case "MATERIAL_SPEC" -> "材料规范(E类)";
            case "PROCESS_SPEC" -> "工艺规范(D类)";
            default -> specType;
        };
    }

    private String getSpecTypeDescription(String specType) {
        return switch (specType) {
            case "SYSTEM_SPEC" -> "针对整个武器系统，规定系统级功能特性、接口要求和验证要求。从论证阶段开始编制，方案阶段结束前批准定稿。建立功能基线。";
            case "DEV_SPEC" -> "针对系统级之下技术状态项目，规定功能特性、接口要求和验证要求。从方案阶段开始编制，详细设计开始前批准定稿。建立分配基线。";
            case "PRODUCT_SPEC" -> "针对最终交付产品，规定功能特性、物理特性和验证要求。从工程研制早期开始编制，正式生产前批准定稿。建立产品基线。";
            case "SOFTWARE_SPEC" -> "包括系统规格说明、软件需求规格说明、软件产品规格说明。按GJB 438B-2009规定编制。";
            case "MATERIAL_SPEC" -> "针对原材料、混合物或半成品，规定材料性能、形状和试验要求。";
            case "PROCESS_SPEC" -> "针对产品或材料制造的专用工艺，规定所需材料、设备及加工控制要求。";
            default -> "";
        };
    }

    /** Recursively render template chapters with descriptions, writing tips, and clause references. */
    private void renderTemplateChapters(List<DocTemplateChapter> chapters,
                                         Map<Long, List<DocTemplateChapter>> byParent,
                                         StringBuilder ctx, int depth) {
        for (DocTemplateChapter ch : chapters) {
            String indent = "  ".repeat(depth);
            String prefix = depth == 0 ? "## " : depth == 1 ? "### " : "- ";
            ctx.append(indent).append(prefix).append(ch.getChapterNumber())
                .append(" ").append(ch.getChapterTitle())
                .append(Boolean.TRUE.equals(ch.getIsRequired()) ? "（必写）" : "（可选）").append("\n");
            if (ch.getDescription() != null && !ch.getDescription().isBlank()) {
                ctx.append(indent).append("  **说明**: ").append(ch.getDescription()).append("\n");
            }
            if (ch.getWritingTips() != null && !ch.getWritingTips().isBlank()) {
                ctx.append(indent).append("  **编写提示**: ").append(ch.getWritingTips()).append("\n");
            }
            if (ch.getStandardClauseRef() != null && !ch.getStandardClauseRef().isBlank()) {
                ctx.append(indent).append("  **适用标准**: ").append(ch.getStandardClauseRef()).append("\n");
            }
            List<DocTemplateChapter> children = byParent.getOrDefault(ch.getId(), List.of());
            if (!children.isEmpty()) {
                renderTemplateChapters(children, byParent, ctx, depth + 1);
            }
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
