package com.military.doc.modules.standard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("standard")
public class Standard {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String standardCode;
    private String standardName;
    private String standardType;
    private String category;
    private String version;
    private LocalDate publishDate;
    private LocalDate effectiveDate;
    private String description;
    private String fileObjectId;
    private String fileName;
    private Long fileSize;
    private String fileType;
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
