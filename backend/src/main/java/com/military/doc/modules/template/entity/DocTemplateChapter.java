package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_template_chapter")
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
    private String contentSchema;
    private String standardClauseRef;
    private String description;
    private String writingTips;
    private String sampleContent;
    private Boolean isReusableElement;

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
