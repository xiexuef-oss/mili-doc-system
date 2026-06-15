package com.military.doc.modules.reliability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;

@Data
@TableName(value = "rel_gjb299d_cache", autoResultMap = true)
public class RelGjb299dCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String partCategory;
    private String partSubtype;
    private String sectionRef;
    private String paramName;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String keyValues;
    private Double resultValue;
    private String tableRef;
    private Integer tableRowIndex;
    private String notes;
}
