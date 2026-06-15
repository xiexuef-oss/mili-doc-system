package com.military.doc.modules.chat.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("project_feedback")
public class ProjectFeedback {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId; private Long docLedgerId;
    private String chapterNumber; private String originalContent;
    private String correctedContent; private String feedbackType;
    private String tags; private Long createdBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
