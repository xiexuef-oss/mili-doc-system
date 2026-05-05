package com.military.doc.modules.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("review_meeting")
public class ReviewMeeting {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long stageId;
    private String meetingCode;
    private String meetingName;
    private String meetingType;
    private LocalDate meetingDate;
    private String meetingLocation;
    private Long hostUserId;
    private String attendeeUsers;
    private String expertGroup;
    private String status;

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