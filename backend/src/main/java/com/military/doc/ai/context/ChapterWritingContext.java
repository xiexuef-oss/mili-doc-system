package com.military.doc.ai.context;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 融合三库信息的章节写作上下文。
 * 对于给定的文档章节，汇编：
 * 1. 模板指南 — 该写什么
 * 2. 标准条款 — 规矩是什么
 * 3. 知识卡片 — 怎么写好
 * 4. 主数据字段 — 自动填充值
 */
@Data
public class ChapterWritingContext {

    // 章节标识
    private Long docChapterId;
    private Long templateChapterId;
    private String chapterNumber;
    private String chapterTitle;
    private Integer chapterLevel;

    // 1. 模板指南 (WHAT)
    private String templateDescription;
    private String writingTips;
    private String sampleContent;
    private Object contentSchema;

    // 2. 适用标准条款 (RULES)
    private List<StandardClauseRef> applicableClauses;

    // 3. 相关知识卡片 (HOW)
    private List<KnowledgeCardRef> relevantCards;

    // 4. 关联主数据字段 (DATA)
    private List<MasterDataFieldRef> relevantFields;

    // ---- 内嵌类型 ----

    @Data
    public static class StandardClauseRef {
        private Long clauseId;
        private String standardCode;
        private String standardName;
        private String clauseNumber;
        private String clauseTitle;
        private String clauseContent;
        private String linkType;
    }

    @Data
    public static class KnowledgeCardRef {
        private Long cardId;
        private String title;
        private String plainLanguage;
        private String gjbReference;
        private String tags;
    }

    @Data
    public static class MasterDataFieldRef {
        private String masterDataPath;
        private String fieldLabel;
        private boolean required;
        private Object currentValue;
        private String valueStatus; // FILLED / EMPTY
    }

    /**
     * 将所有上下文拼装为LLM可消费的结构化文本
     */
    public String toPromptContext() {
        StringBuilder sb = new StringBuilder();

        sb.append("## 章节信息\n");
        sb.append("- 章节编号: ").append(nullToEmpty(chapterNumber)).append("\n");
        sb.append("- 章节标题: ").append(nullToEmpty(chapterTitle)).append("\n");
        sb.append("- 层级: ").append(chapterLevel != null ? chapterLevel : 1).append("\n\n");

        // 1. 编写指南
        boolean hasGuide = notBlank(templateDescription) || notBlank(writingTips) || notBlank(sampleContent);
        if (hasGuide) {
            sb.append("## 编写指南 (模板要求)\n");
            if (notBlank(templateDescription)) {
                sb.append("### 章节说明\n").append(templateDescription).append("\n\n");
            }
            if (notBlank(writingTips)) {
                sb.append("### 编写提示\n").append(writingTips).append("\n\n");
            }
            if (notBlank(sampleContent)) {
                sb.append("### 参考示例\n").append(sampleContent).append("\n\n");
            }
        }

        // 2. 标准条款
        if (applicableClauses != null && !applicableClauses.isEmpty()) {
            sb.append("## 适用标准条款 (必须遵守)\n");
            for (StandardClauseRef clause : applicableClauses) {
                sb.append("### ").append(nullToEmpty(clause.getStandardCode()))
                    .append(" ").append(nullToEmpty(clause.getClauseNumber()))
                    .append(" ").append(nullToEmpty(clause.getClauseTitle())).append("\n");
                if (notBlank(clause.getClauseContent())) {
                    String content = clause.getClauseContent();
                    if (content.length() > 500) {
                        content = content.substring(0, 500) + "...";
                    }
                    sb.append(content).append("\n\n");
                }
            }
        }

        // 3. 知识卡片
        if (relevantCards != null && !relevantCards.isEmpty()) {
            sb.append("## 编写知识与技巧\n");
            for (KnowledgeCardRef card : relevantCards) {
                sb.append("### ").append(nullToEmpty(card.getTitle())).append("\n");
                if (notBlank(card.getPlainLanguage())) {
                    sb.append(card.getPlainLanguage()).append("\n\n");
                }
                if (notBlank(card.getGjbReference())) {
                    sb.append("参考: ").append(card.getGjbReference()).append("\n\n");
                }
            }
        }

        // 4. 主数据
        if (relevantFields != null && !relevantFields.isEmpty()) {
            sb.append("## 关联主数据字段\n");
            for (MasterDataFieldRef field : relevantFields) {
                String status = "FILLED".equals(field.getValueStatus()) ? "✓" : "【缺失】";
                sb.append("- ").append(status).append(" ")
                    .append(nullToEmpty(field.getFieldLabel()))
                    .append(" (").append(nullToEmpty(field.getMasterDataPath())).append(")");
                if (field.getCurrentValue() != null) {
                    sb.append(" = ").append(field.getCurrentValue());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
