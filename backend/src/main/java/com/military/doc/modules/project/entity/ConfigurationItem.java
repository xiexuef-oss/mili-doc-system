package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("configuration_item")
public class ConfigurationItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long stageId;
    private String ciCode;
    private String ciName;
    private String ciType;
    private Long parentCiId;
    private Long responsibleUserId;
    private String currentVersion;
    private String status;
    private Boolean isControlled;
    private Boolean isKeyItem;
    private String description;

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
