package com.military.doc.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_document_block", autoResultMap = true)
public class AiDocumentBlock {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId;
    private Long sectionId;
    private String type;
    private String content;
    private Integer sortOrder;
    private Long createdBy;
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class) private String metadata;
    @TableField(fill = FieldFill.INSERT) private java.time.LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private java.time.LocalDateTime updatedAt;
}
