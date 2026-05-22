package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_template_element")
public class DocTemplateElement {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String elementCode;
    private String elementName;
    private String elementType;
    private String elementCategory;
    private String contentJson;
    private String standardRefs;
    private String keywords;
    private String status;

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
