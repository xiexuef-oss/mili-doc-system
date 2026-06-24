package com.military.doc.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.military.doc.ai.context.ContextAssemblyService;
import com.military.doc.ai.llm.LlmClient;
import com.military.doc.ai.prompt.PromptTemplateService;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.template.entity.DocTemplateChapter;
import com.military.doc.modules.template.entity.DocTemplateV2;
import com.military.doc.modules.template.mapper.DocTemplateChapterMapper;
import com.military.doc.modules.template.mapper.DocTemplateV2Mapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Generates a "prompt-ready" document structure in Markdown format.
 * The output serves dual purpose:
 * 1. As an AI prompt template (tells AI what chapters to write, with constraints)
 * 2. As a reusable system template (saved to DocTemplateV2 + DocTemplateChapter)
 *
 * This implements the flow where no matching template exists:
 * AI generates structure → user reviews/edits → saves as template → used for generation.
 */
@Slf4j
@Service
public class DocumentStructureService {

    private final ContextAssemblyService contextAssemblyService;
    private final PromptTemplateService promptTemplateService;
    private final LlmClient llmClient;
    private final DocLedgerMapper docLedgerMapper;
    private final DocTemplateV2Mapper templateV2Mapper;
    private final DocTemplateChapterMapper templateChapterMapper;
    private final ObjectMapper objectMapper;

    public DocumentStructureService(ContextAssemblyService contextAssemblyService,
                                     PromptTemplateService promptTemplateService,
                                     LlmClient llmClient,
                                     DocLedgerMapper docLedgerMapper,
                                     DocTemplateV2Mapper templateV2Mapper,
                                     DocTemplateChapterMapper templateChapterMapper,
                                     ObjectMapper objectMapper) {
        this.contextAssemblyService = contextAssemblyService;
        this.promptTemplateService = promptTemplateService;
        this.llmClient = llmClient;
        this.docLedgerMapper = docLedgerMapper;
        this.templateV2Mapper = templateV2Mapper;
        this.templateChapterMapper = templateChapterMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== Data types ====================

    public record GeneratedStructure(
        String markdownContent,     // The full MD document (prompt-ready)
        List<StructureChapter> chapters,  // Parsed chapter list
        String suggestedTemplateName,
        String gjbStandardRef,
        int totalChapters
    ) {}

    public record StructureChapter(
        String chapterNumber,
        String chapterTitle,
        int chapterLevel,
        int orderNum,
        boolean isRequired,
        String writingTips,
        String description
    ) {}

    // ==================== Generate Structure ====================

    /**
     * Generate a document structure as a markdown prompt template.
     * The AI acts as a GJB documentation expert and outputs a structured
     * chapter outline with writing tips, word count requirements, and standard references.
     */
    public GeneratedStructure generate(Long projectId, Long docLedgerId) {
        DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
        String docName = ledger != null ? ledger.getDocName() : "文档";

        String projectContext = contextAssemblyService.assembleContext(projectId);
        String systemPrompt = buildStructureSystemPrompt();
        String userPrompt = buildStructureUserPrompt(docName, projectContext);

        log.info("Generating document structure for: {} (project={})", docName, projectId);
        String response = llmClient.chat(systemPrompt, userPrompt);

        // Parse chapters from the markdown response
        List<StructureChapter> chapters = parseChaptersFromMd(response);
        String templateName = docName + "（AI生成模板）";
        String gjbRef = extractGjbRef(response);

        return new GeneratedStructure(response, chapters, templateName, gjbRef, chapters.size());
    }

    // ==================== Save as Template ====================

    /**
     * Save the generated structure as a reusable system template.
     * Creates DocTemplateV2 + DocTemplateChapter records.
     */
    @Transactional
    public Long saveAsTemplate(GeneratedStructure structure, Long categoryId, Long userId) {
        // Create template
        DocTemplateV2 template = new DocTemplateV2();
        template.setTemplateName(structure.suggestedTemplateName);
        template.setTemplateCode("TPL-AI-" + System.currentTimeMillis() % 100000);
        template.setTemplateType("AI_GENERATED");
        template.setGjbStandardRef(structure.gjbStandardRef);
        template.setCategoryId(categoryId);
        template.setStatus("ACTIVE");
        template.setVersionNo(1);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        template.setVariablesSchema("{}");
        templateV2Mapper.insert(template);

        // Create template chapters
        Map<Long, Long> tplToDbId = new LinkedHashMap<>();
        for (StructureChapter sc : structure.chapters) {
            DocTemplateChapter tc = new DocTemplateChapter();
            tc.setTemplateId(template.getId());
            tc.setChapterNumber(sc.chapterNumber);
            tc.setChapterTitle(sc.chapterTitle);
            tc.setChapterLevel(sc.chapterLevel);
            tc.setOrderNum(sc.orderNum);
            tc.setIsRequired(sc.isRequired);
            tc.setWritingTips(sc.writingTips);
            tc.setDescription(sc.description);
            tc.setParentId(0L); // First pass
            tc.setCreatedAt(LocalDateTime.now());
            tc.setUpdatedAt(LocalDateTime.now());
            templateChapterMapper.insert(tc);
            tplToDbId.put((long) sc.orderNum, tc.getId());
        }

        // Second pass: resolve parent-child relationships
        for (StructureChapter sc : structure.chapters) {
            if (sc.chapterLevel > 1) {
                // Find parent: the most recent chapter with level < current
                DocTemplateChapter childTc = templateChapterMapper.selectById(tplToDbId.get((long) sc.orderNum));
                if (childTc != null) {
                    Long parentIdx = null;
                    for (int i = sc.orderNum - 2; i >= 0; i--) {
                        if (structure.chapters.get(i).chapterLevel < sc.chapterLevel) {
                            parentIdx = (long) (i + 1);
                            break;
                        }
                    }
                    if (parentIdx != null) {
                        Long parentDbId = tplToDbId.get(parentIdx);
                        if (parentDbId != null) {
                            childTc.setParentId(parentDbId);
                            templateChapterMapper.updateById(childTc);
                        }
                    }
                }
            }
        }

        log.info("Saved AI-generated structure as template {}: {} chapters",
            template.getId(), structure.chapters.size());
        return template.getId();
    }

    // ==================== Internal ====================

    private String buildStructureSystemPrompt() {
        return """
            你是一位军工文档策划专家，精通 GJB 450B、GJB 6387、GJB 5882-2006、GJB/Z 299D 等国家军用标准。

            你的任务是为指定的文档类型生成一套完整的章节结构文档。

            ## 输出格式要求

            请直接输出 Markdown 格式的文档结构，包含以下要素：

            1. **文档元信息**（文档名称、适用标准、文档类别）
            2. **每章定义**：
               - 章节编号和标题
               - [必填/可选] 标记
               - 字数要求：>=XXX字
               - 内容说明：该章节应包含什么
               - 编写提示：具体怎么写
               - 适用标准条款：GJB XX-XXXX 第X章
               - 必含字段：列出必须出现的字段名（如产品名称、MTBF等）
            3. **禁止事项**：列出禁止使用的占位词
            4. **AI_META 输出规范**：每章末尾需输出的元数据格式

            ## 格式模板（参考）

            ```
            # [文档名称] - 文档结构

            > 适用标准：GJB XXXX-XXXX
            > 文档类别：XXX

            ## 1. 章节标题 [必填] [>=300字]

            **内容说明**：本章应包含...

            **编写提示**：建议从...角度撰写

            **适用标准条款**：GJB XXXX 第X.X条

            **必含字段**：产品名称、产品型号

            ---
            ```

            严格按照上述格式输出完整的文档结构。
            """;
    }

    private String buildStructureUserPrompt(String docName, String projectContext) {
        return String.format("""
            ## 任务
            请为以下文档生成完整的章节结构文档（Markdown 格式）：

            文档名称：%s

            ## 项目背景
            %s

            ## 要求
            1. 章节结构应符合 GJB 标准对该类文档的要求
            2. 每章需包含：内容说明、编写提示、字数要求、适用标准条款、必含字段
            3. 层级最多到 3 级（1, 1.1, 1.1.1）
            4. 用 [必填] 或 [可选] 标记每个章节
            5. 在文档末尾列出禁止事项
            6. 直接输出 Markdown，不要其他说明文字
            """, docName, projectContext);
    }

    private List<StructureChapter> parseChaptersFromMd(String md) {
        List<StructureChapter> result = new ArrayList<>();
        int orderNum = 1;

        for (String line : md.split("\n")) {
            line = line.trim();
            if (line.startsWith("## ")) {
                String content = line.substring(3).trim();
                var sc = parseChapterLine(content, orderNum);
                if (sc != null) {
                    result.add(sc);
                    orderNum++;
                }
            }
        }
        return result;
    }

    private StructureChapter parseChapterLine(String line, int orderNum) {
        // Pattern: "1. 章节标题 [必填] [>=300字]" or "1.1.1 子标题 [可选]"
        String num = null, title = null;
        int level = 1;
        boolean required = line.contains("[必填]");

        int firstSpace = line.indexOf(' ');
        if (firstSpace > 0) {
            num = line.substring(0, firstSpace).trim();
            title = line.substring(firstSpace + 1).trim()
                .replaceAll("\\[必填\\]", "").replaceAll("\\[可选\\]", "")
                .replaceAll("\\[>=\\d+字\\]", "").trim();
            level = num.split("\\.").length;
        } else {
            num = String.valueOf(orderNum);
            title = line.replaceAll("\\[必填\\]", "").replaceAll("\\[可选\\]", "").trim();
        }

        return new StructureChapter(num, title, level, orderNum, required, "", "");
    }

    private String extractGjbRef(String md) {
        // Extract GJB reference from the markdown
        for (String line : md.split("\n")) {
            if (line.contains("适用标准") || line.contains("GJB")) {
                String cleaned = line.replaceAll(".*?适用标准[：:]\\s*", "").trim();
                if (!cleaned.isEmpty()) return cleaned;
            }
        }
        return "GJB 通用";
    }
}
