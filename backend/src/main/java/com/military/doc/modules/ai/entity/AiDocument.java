package com.military.doc.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_document", autoResultMap = true)
public class AiDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private String title;
    private String description;
    private String documentType;
    private String sourcePrompt;
    private String status;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String contentJson;
                @TableField(typeHandler = JsonbTypeHandler.class)
    private String outlineJson;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
