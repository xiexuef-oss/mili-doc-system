package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_effective_baseline")
public class DocEffectiveBaseline {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long stageId;
    private Long docFileId;
    private Long effectiveVersionId;
    private Long finalVersionId;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private String baselineStatus;

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