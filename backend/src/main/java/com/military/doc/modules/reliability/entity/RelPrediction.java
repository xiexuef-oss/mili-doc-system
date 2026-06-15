package com.military.doc.modules.reliability.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rel_prediction")
public class RelPrediction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String predictionMethod;
    private String environmentCategory;
    private Double totalFailureRate;
    private Double mtbfResult;
    private Boolean isCompliant;
    private Double targetMtbf;
    private String notes;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
