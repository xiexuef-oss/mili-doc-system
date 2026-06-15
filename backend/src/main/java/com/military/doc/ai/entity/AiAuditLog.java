package com.military.doc.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI API 调用审计日志，记录每次 LLM 调用的关键信息，
 * 用于合规审计、成本分析和问题排查。
 */
@Data
@TableName(value = "ai_audit_log", autoResultMap = true)
public class AiAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联项目ID */
    private Long projectId;

    /** 任务类型: CATALOG_GENERATION, DRAFT_GENERATION, PROOFREAD, PRE_REVIEW, COMPLIANCE_CHECK, etc. */
    private String taskType;

    /** LLM 提供商: ollama / deepseek */
    private String provider;

    /** 模型名称 */
    private String model;

    /** 系统提示词 SHA-256 哈希 */
    private String systemPromptHash;

    /** 用户提示词 SHA-256 哈希 */
    private String userPromptHash;

    /** 输入 token 估算数 */
    private Integer inputTokens;

    /** 输出 token 估算数 */
    private Integer outputTokens;

    /** 调用耗时(毫秒) */
    private Integer latencyMs;

    /** 是否成功 */
    private Boolean success;

    /** 错误码(失败时) */
    private String errorCode;

    /** 错误信息(失败时) */
    private String errorMessage;

    /** 脱敏字段类型列表(JSONB数组) */
    @TableField(typeHandler = com.military.doc.common.mybatis.JsonbTypeHandler.class)
    private String scrubbedFields;

    /** 模型位置: OLLAMA_LOCAL / DEEPSEEK_CLOUD / UNKNOWN */
    private String locality;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
