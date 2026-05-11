package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("configuration_baseline_item")
public class ConfigurationBaselineItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long baselineId;
    private String itemType;
    private Long itemId;
    private Long itemVersionId;
    private String itemCode;
    private String itemName;
    private String itemVersion;

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
