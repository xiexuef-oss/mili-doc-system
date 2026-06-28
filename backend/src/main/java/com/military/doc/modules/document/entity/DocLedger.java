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
    private Long catalogId;
    private Long checklistItemId;
    private String docCode;
    private String docName;
    private String docCategory;
    private String docType;
    private String stageCode;
    private Boolean requiredFlag;
    private String meetingUsage;
    private String usageSource;
    private String usageAdjustReason;
    private String changeReason;
    private Long responsibleUserId;
    private String securityLevel;
    private String specType;
    private String lifecycleStatus;
    private String fileObjectId;
    private Long contentSize;
    private String docContent;  // AI生成的文档内容(Markdown格式)

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
