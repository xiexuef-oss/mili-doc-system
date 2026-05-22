package com.military.doc.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_card")
public class KnowledgeCard {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String cardType;
    private String targetTable;
    private Long targetId;
    private String title;
    private String plainLanguage;
    private String gjbReference;
    private String tags;
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
