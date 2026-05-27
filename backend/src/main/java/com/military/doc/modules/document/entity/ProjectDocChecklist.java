package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("project_doc_checklist")
public class ProjectDocChecklist {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long stageId;
    private String stageCode;
    private Long templateId;
    private String docName;
    private String category;
    private String categoryCode;
    private String docStatus;
    private String responsiblePerson;
    private LocalDate plannedDate;
    private LocalDate completedDate;
    private Long fileId;
    private Integer sortOrder;
    private Boolean isCustom;
    private String notes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
