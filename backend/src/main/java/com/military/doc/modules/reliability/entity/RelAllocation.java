package com.military.doc.modules.reliability.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rel_allocation")
public class RelAllocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String allocationMethod;
    private Double systemMtbf;
    private Double systemLambda;
    private String unitName;
    private String unitLevel;
    private Long parentId;
    private Double complexityScore;
    private Double maturityScore;
    private Double dutyTimeScore;
    private Double environmentScore;
    private Double importanceFactor;
    private Double complexityFactor;
    private Double existingLambda;
    private Double allocatedLambda;
    private Double allocatedMtbf;
    private Double allocationRatio;
    private Boolean isVerified;
    private String notes;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
