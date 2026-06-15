package com.military.doc.modules.chat.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("project_terminology")
public class ProjectTerminology {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId; private String term; private String preferred;
    private String source; private Integer usageCount;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
