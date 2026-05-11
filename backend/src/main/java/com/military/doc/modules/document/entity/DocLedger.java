package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_ledger")
public class DocLedger {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long stageId;
    private String docCode;
    private String docName;
    private String docType;
    private Boolean requiredFlag;
    private String meetingUsage;
    private String usageSource;
    private String usageAdjustReason;
    private String changeReason;
    private Long responsibleUserId;
    private String securityLevel;
    private String lifecycleStatus;
    private String fileObjectId;
    private Long contentSize;

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
