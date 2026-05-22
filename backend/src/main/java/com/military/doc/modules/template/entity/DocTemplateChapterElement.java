package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_template_chapter_element")
public class DocTemplateChapterElement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chapterId;
    private Long elementId;
    private Integer orderNum;
    private Boolean isRequired;

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
