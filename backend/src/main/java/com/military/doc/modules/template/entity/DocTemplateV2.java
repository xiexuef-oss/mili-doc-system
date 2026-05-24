package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "doc_template_v2", autoResultMap = true)
public class DocTemplateV2 {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private String templateCode;
    private String templateName;
    private String templateType;
    private String applicableStageCodes;
    private String applicableProjectType;
    private String gjbStandardRef;
    private String documentClass;
    private String fileObjectId;
    private String fileName;
    private Long fileSize;
    private String fileType;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String variablesSchema;

    private String status;
    private Integer versionNo;

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
