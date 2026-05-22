package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("completeness_check_result")
public class CompletenessCheckResult {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long docLedgerId;
    private Long stageId;
    private String checkType;
    private Integer totalItems;
    private Integer passedItems;
    private Integer warningItems;
    private Integer errorItems;
    private BigDecimal score;
    private String detailJson;
    private Long checkedBy;
    private LocalDateTime checkedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
