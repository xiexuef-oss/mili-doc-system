package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stage_transition_check")
public class StageTransitionCheck {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long fromStageId;
    private Long toStageId;
    private String checkStatus;
    private String blockerItems;
    private String checkResult;
    private Long checkedBy;
    private LocalDateTime checkedAt;

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