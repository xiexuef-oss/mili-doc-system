package com.military.doc.modules.chat.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("generation_exemplar")
public class GenerationExemplar {
    @TableId(type = IdType.AUTO) private Long id;
    private Long templateId; private String chapterNumber;
    private String chapterTitle; private String content;
    private Integer qualityScore; private Integer usageCount;
    private String source; private Long projectId; private String tags;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
