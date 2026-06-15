package com.military.doc.modules.reliability.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rel_requirement")
public class RelRequirement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Double mtbfHours;
    private Double mtbcfHours;
    private Double reliabilityAtTime;
    private Double reliabilityTimeHours;
    private Double serviceLifeYears;
    private Double storageReliability;
    private String failureCriteria;
    private String verificationMethod;
    private String requirementSource;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
