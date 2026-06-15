package com.military.doc.modules.reliability.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("rel_prediction_item")
public class RelPredictionItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long predictionId;
    private String partName;
    private String partCategory;
    private String partSubtype;
    private String partSpec;
    private Integer quantity;
    private String qualityLevel;
    private Double lambdaB;
    private Double piE;
    private Double piQ;
    private Double piT;
    private Double piS;
    private Double piL;
    private Double piC;
    private Double piA;
    private Double piK;
    private Double piCvc;
    private Double lambdaP;
    private Double operatingTemp;
    private Double stressRatio;
    private String tableRef;
    private Integer orderNum;
}
