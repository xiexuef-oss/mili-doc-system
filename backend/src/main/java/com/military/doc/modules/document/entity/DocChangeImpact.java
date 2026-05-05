package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_change_impact")
public class DocChangeImpact {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long changeEventId;
    private Long impactedDocFileId;
    private String impactReason;
    private String suggestAction;
    private Long responsibleUserId;
    private String status;
    private LocalDateTime closedAt;

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