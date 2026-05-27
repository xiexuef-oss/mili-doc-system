package com.military.doc.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chapter_knowledge_card_link")
public class ChapterKnowledgeCardLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateChapterId;
    private Long knowledgeCardId;

    private LocalDateTime createdAt;
}
