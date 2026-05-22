package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project_master_data")
public class ProjectMasterData {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String equipmentInfo;
    private String tacticalIndicators;
    private String productTree;
    private String teamMembers;
    private String milestones;
    private String extendedFields;
    private Integer versionNo;
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
