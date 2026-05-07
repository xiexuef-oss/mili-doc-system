package com.military.doc.modules.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_expert_opinion_file")
public class ReviewExpertOpinionFile {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;
    private Long expertUserId;
    private String expertGroupName;
    private Long docFileId;
    private String fileObjectId;
    private String problemLevel;
    private LocalDateTime uploadedAt;

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