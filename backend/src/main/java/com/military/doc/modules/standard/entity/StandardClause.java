package com.military.doc.modules.standard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("standard_clause")
public class StandardClause {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long standardId;
    private String clauseNumber;
    private String clauseTitle;
    private String clauseContent;
    private Long parentId;
    private Integer orderNum;
    private String keywords;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
