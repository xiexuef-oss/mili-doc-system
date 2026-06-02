package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档输入参考关联 — 记录每个文档模板在编写时需要参考的上游文档、军用标准和知识卡片。
 * 串联四环节中的第2环节：文件 → 参考文档+军标。
 */
@Data
@TableName("doc_input_reference")
public class DocInputReference {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 stage_doc_checklist_template.id */
    private Long checklistTemplateId;

    /** 参考类型: UPSTREAM_DOC / STANDARD / KNOWLEDGE_CARD */
    private String refType;

    /** 关联ID（视ref_type而定） */
    private Long refId;

    /** 参考编号, e.g. "GP-12", "GJB 6387-2008" */
    private String refCode;

    /** 参考名称 */
    private String refName;

    /** 参考用途说明 */
    private String refUsage;

    /** 是否必须参考 */
    private Boolean isRequired;

    /** 排序号 */
    private Integer orderNum;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
