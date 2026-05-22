package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("doc_chapter")
public class DocChapter {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long docLedgerId;
    private Long templateChapterId;
    private Long parentId;
    private String chapterNumber;
    private String chapterTitle;
    private Integer chapterLevel;
    private Integer orderNum;
    private String content;
    private String contentJson;
    private String fillStatus;
    private Integer fillPercentage;
    private String status;
    private Integer versionNo;

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
