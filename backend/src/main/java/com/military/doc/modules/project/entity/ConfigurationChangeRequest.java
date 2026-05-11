package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("configuration_change_request")
public class ConfigurationChangeRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long stageId;
    private String changeCode;
    private String changeTitle;
    private String changeType;
    private String changeLevel;
    private String changeReason;
    private String changeContent;
    private String impactAnalysis;
    private Long applicantId;
    private Long responsibleUserId;
    private String status;
    private Long ccbMeetingId;
    private String approveResult;
    private String approveOpinion;
    private LocalDateTime approveTime;

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
