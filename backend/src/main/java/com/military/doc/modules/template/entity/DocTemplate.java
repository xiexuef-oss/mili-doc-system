package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_template")
public class DocTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateName;
    private String templateCode;
    private String templateType;
    private String applicableProjectType;
    private String description;
    private String fileObjectId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String variables;
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
