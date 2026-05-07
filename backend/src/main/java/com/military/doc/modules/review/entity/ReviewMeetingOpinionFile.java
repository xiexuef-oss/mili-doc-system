package com.military.doc.modules.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_meeting_opinion_file")
public class ReviewMeetingOpinionFile {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long meetingId;
    private Long docFileId;
    private String opinionType;
    private String fileObjectId;
    private String status;
    private Long uploadedBy;
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