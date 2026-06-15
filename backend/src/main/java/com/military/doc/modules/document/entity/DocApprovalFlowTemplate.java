package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 签审流程模板 — 定义不同文档类型的签审步骤链。
 */
@Data
@TableName("doc_approval_flow_template")
public class DocApprovalFlowTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateName;
    /** 适用文档类型: TECH_DOC/MGMT_DOC/REVIEW_DOC */
    private String docType;
    /** JSONB: [{"step":"DESIGN","position":"主管设计师","line":"TECHNICAL","order":1},...] */
    private String approvalSteps;
    private Boolean isDefault;

    @TableLogic
    private Integer deleted;
}
