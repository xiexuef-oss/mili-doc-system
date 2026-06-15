package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project_member")
public class ProjectMember {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long userId;
    private String roleInProject;
    /** 指挥线: TECHNICAL/ADMINISTRATIVE/QUALITY/CRAFT */
    private String memberLine;
    /** 岗位编码: CHIEF_DESIGNER/CHIEF_COMMANDER/... */
    private String memberPosition;
    /** 上级 member_id，构建指挥链 */
    private Long supervisorId;
    private Integer sortOrder;
    private String duties;
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