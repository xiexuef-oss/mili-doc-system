package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档签审记录 — 记录两师签审链中每一步的审批结果。
 */
@Data
@TableName("doc_approval_record")
public class DocApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docLedgerId;
    /** 审批步骤: DESIGN/CHECK/REVIEW/APPROVE/COUNTERSIGN */
    private String approvalStep;
    private Long approverId;
    /** 审批岗位: CHIEF_DESIGNER/CHIEF_COMMANDER/... */
    private String approverPosition;
    /** 审批结果: APPROVED/REJECTED/RETURN */
    private String approvalResult;
    private String approvalOpinion;
    private LocalDateTime approvedAt;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
