package com.military.doc.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("embedding_index_task")
public class EmbeddingIndexTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private String targetTable;
    private Integer totalCount;
    private Integer completedCount;
    private Integer failedCount;
    private String status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
