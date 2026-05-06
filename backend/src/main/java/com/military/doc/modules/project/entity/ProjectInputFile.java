package com.military.doc.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project_input_file")
public class ProjectInputFile {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private String fileName;
    private String fileObjectId;
    private Long fileSize;
    private String fileType;
    private String inputType;
    private String description;
    private Long uploadedBy;

    @TableField(fill = FieldFill.INSERT)
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
