package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("project_stage")
public class ProjectStage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String stageGoal;
    private String entryCriteria;
    private String exitCriteria;
    private Long stageManagerId;
    private Long technicalManagerId;
    private Long qualityManagerId;
    private Boolean isCurrent;
    private Boolean allowParallel;

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