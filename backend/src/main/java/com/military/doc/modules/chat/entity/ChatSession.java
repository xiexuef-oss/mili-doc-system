package com.military.doc.modules.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "chat_session", autoResultMap = true)
public class ChatSession {
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class)
    private String messages;
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class)
    private String generatedDocs;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String sessionId;
    private String title;
    private String contextSnapshot;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
