package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_edit_lock")
public class DocEditLock {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docFileId;
    private Long lockedVersionId;
    private Long lockedBy;
    private String lockType;
    private String lockStatus;
    private LocalDateTime expireAt;

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