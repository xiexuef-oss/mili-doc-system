package com.military.doc.modules.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.military.doc.common.mybatis.JsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "doc_chapter", autoResultMap = true)
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

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String contentJson;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String contentBlocks; // DDXML block array: [{"id":1,"type":"title","content":"..."},...]

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
