package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("configuration_status_accounting")
public class ConfigurationStatusAccounting {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long stageId;
    private Long ciId;
    private Long docLedgerId;
    private Long docVersionId;
    private String eventType;
    private String eventName;
    private String eventDescription;
    private String relatedObjectType;
    private Long relatedObjectId;
    private LocalDateTime eventTime;
    private Long operatorId;

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
