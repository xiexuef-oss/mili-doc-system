package com.military.doc.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_session")
public class AiChatSession {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId;
    private Long userId;
    private String title;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
