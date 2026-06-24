package com.military.doc.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_document_section", autoResultMap = true)
public class AiDocumentSection {
    @TableId(type = IdType.AUTO) private Long id;
    private Long documentId;
    private Long parentId;
    private String title;
    private Integer level;
    private Integer sortOrder;
    private String content;
    @TableField(typeHandler = JsonbTypeHandler.class) private String contentJson;
    private String status;
    private Long createdBy;
    private Long updatedBy;
    @TableField(typeHandler = JsonbTypeHandler.class) private String metadata;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}
