package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stage_doc_checklist_template")
public class StageDocChecklistTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String docCode;
    private String docName;
    private String category;
    private String categoryCode;
    private String applicableStages;
    private String primaryStage;
    private String responsibility;
    private Boolean requiredFlag;
    private String gjbReference;
    private String description;
    private String keywords;
    private Integer orderNum;

    /** 关联 doc_template_v2.id，指向该文件的写作模板 */
    private Long templateId;

    /** GJB6387规范类型: SYSTEM_SPEC/DEV_SPEC/PRODUCT_SPEC/SOFTWARE_SPEC/MATERIAL_SPEC/PROCESS_SPEC */
    private String specType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
