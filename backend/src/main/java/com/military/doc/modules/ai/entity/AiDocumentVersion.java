package com.military.doc.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_document_version", autoResultMap = true)
public class AiDocumentVersion {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId;
    private Long userId;
    private String title;
    private String reason;
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class) private String snapshot;
    @TableField(fill = FieldFill.INSERT) private java.time.LocalDateTime createdAt;
}
