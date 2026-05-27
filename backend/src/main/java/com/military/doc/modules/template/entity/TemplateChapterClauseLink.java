package com.military.doc.modules.template.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("template_chapter_clause_link")
public class TemplateChapterClauseLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateChapterId;
    private Long standardClauseId;
    private String linkType;
    private String relevanceNote;

    private LocalDateTime createdAt;
}
