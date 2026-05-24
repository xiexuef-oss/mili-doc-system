package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "project_master_data", autoResultMap = true)
public class ProjectMasterData {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String equipmentInfo;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String tacticalIndicators;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String productTree;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String teamMembers;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String milestones;

    @TableField(typeHandler = JsonbTypeHandler.class)
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
