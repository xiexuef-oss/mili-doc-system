package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_chapter_field_mapping")
public class TemplateChapterFieldMapping {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateChapterId;
    private String masterDataPath;
    private String fieldLabel;
    private Boolean isRequired;
    private Integer orderNum;

    private LocalDateTime createdAt;
}
