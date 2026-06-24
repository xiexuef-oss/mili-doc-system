package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "doc_template_chapter", autoResultMap = true)
public class DocTemplateChapter {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;
    private Long parentId;
    private String chapterNumber;
    private String chapterTitle;
    private Integer chapterLevel;
    private Integer orderNum;
    private Boolean isRequired;
    private String applicabilityRule;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String contentSchema;

    private String standardClauseRef;
    private String description;
    private String writingTips;
    private String sampleContent;
    private Boolean isReusableElement;

    /** 最小字数下限 */
    private Integer minWords;
    /** 完成度计算权重 (0.0-1.0)，默认1.0 */
    private Double completionWeight;

    // === Template DOCX auto-parsed fields ===
    /** 原始 Word 样式名 (如 Heading1, Heading2) */
    private String headingStyle;
    /** 编号格式 (如 "3.1.2", "一、", "a)") */
    private String numberingFormat;
    /** 是否包含表格 */
    private Boolean hasTable;
    /** 表格结构 JSON (headers + rowCount) */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tableJson;
    /** 该章节检测到的变量占位符列表 (逗号分隔) */
    private String variablePlaceholders;
    /** 字体强调类型 (bold/color/underline 组合) */
    private String fontEmphasis;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
