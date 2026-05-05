package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_version")
public class DocVersion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docFileId;
    private String versionNo;
    private String sourceType;
    private Long baseVersionId;
    private Long fileObjectId;
    private String versionStatus;
    private Integer optimisticVersion;
    private Long submitUserId;
    private LocalDateTime submitTime;
    private String changeSummary;

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