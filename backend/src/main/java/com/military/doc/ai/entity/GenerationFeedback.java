package com.military.doc.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 生成质量反馈 — 用户对生成结果的评分和意见。
 */
@Data
@TableName("ai_generation_feedback")
public class GenerationFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long docLedgerId;
    private String taskType;
    private Integer rating;       // 1-5 星级评分
    private String feedbackText;  // 文字反馈
    private String categories;    // JSONB: ["术语准确","格式规范","内容完整"]
    private String promptVersionUsed;
    private String modelUsed;
    private Integer latencyMs;
    private Boolean willUseAgain;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
