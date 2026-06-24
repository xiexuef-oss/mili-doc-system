package com.military.doc.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_chat_message", autoResultMap = true)
public class AiChatMessage {
    @TableId(type = IdType.AUTO) private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String actionType;
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class) private String metadata;
    @TableField(fill = FieldFill.INSERT) private java.time.LocalDateTime createdAt;
}
