-- Migration V8: AI API Audit Logging
-- 创建 AI API 调用审计日志表，用于合规审计、成本分析和问题排查

CREATE TABLE IF NOT EXISTS ai_audit_log (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT,
    task_type           VARCHAR(64) NOT NULL,
    provider            VARCHAR(32) NOT NULL,
    model               VARCHAR(128),
    system_prompt_hash  VARCHAR(64),
    user_prompt_hash    VARCHAR(64),
    input_tokens        INTEGER,
    output_tokens       INTEGER,
    latency_ms          INTEGER,
    success             BOOLEAN DEFAULT TRUE,
    error_code          VARCHAR(64),
    error_message       TEXT,
    scrubbed_fields     JSONB DEFAULT '[]',
    locality            VARCHAR(32),
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_audit_log_project ON ai_audit_log(project_id);
CREATE INDEX IF NOT EXISTS idx_ai_audit_log_task ON ai_audit_log(task_type);
CREATE INDEX IF NOT EXISTS idx_ai_audit_log_created ON ai_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_audit_log_locality ON ai_audit_log(locality);
CREATE INDEX IF NOT EXISTS idx_ai_audit_log_success ON ai_audit_log(success);

COMMENT ON TABLE ai_audit_log IS 'AI API 调用审计日志';
COMMENT ON COLUMN ai_audit_log.task_type IS '任务类型: CATALOG_GENERATION/DRAFT_GENERATION/PROOFREAD/PRE_REVIEW/COMPLIANCE_CHECK/OPINION_SUMMARY/STAGE_READINESS/ARCHIVE_ADVICE/CHANGE_IMPACT/CHAT';
COMMENT ON COLUMN ai_audit_log.scrubbed_fields IS '被脱敏的字段类型列表 (JSONB 数组)';
COMMENT ON COLUMN ai_audit_log.locality IS '模型位置: OLLAMA_LOCAL/DEEPSEEK_CLOUD/UNKNOWN';
