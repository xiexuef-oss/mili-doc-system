package com.military.doc.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_canvas_operation", autoResultMap = true)
public class AiCanvasOperation {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId;
    private Long userId;
    private String operationType;
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class) private String patches;
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class) private String beforeSnapshot;
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class) private String afterSnapshot;
    private String status;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT) private java.time.LocalDateTime createdAt;
}
