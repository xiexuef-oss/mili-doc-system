package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_edit_session")
public class DocEditSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docFileId;
    private Long baseVersionId;
    private Long draftVersionId;
    private Long editorUserId;
    private String sessionStatus;
    private LocalDateTime openedAt;
    private LocalDateTime submittedAt;

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