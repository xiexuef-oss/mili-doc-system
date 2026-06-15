-- Migration V9: AI Generation Feedback
-- 创建 AI 生成质量反馈表，支持用户评分和意见收集

CREATE TABLE IF NOT EXISTS ai_generation_feedback (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT,
    doc_ledger_id       BIGINT,
    task_type           VARCHAR(64),
    rating              SMALLINT CHECK (rating >= 1 AND rating <= 5),
    feedback_text       TEXT,
    categories          JSONB DEFAULT '[]',
    prompt_version_used VARCHAR(32),
    model_used          VARCHAR(128),
    latency_ms          INTEGER,
    will_use_again      BOOLEAN,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedback_project ON ai_generation_feedback(project_id);
CREATE INDEX IF NOT EXISTS idx_feedback_task ON ai_generation_feedback(task_type);
CREATE INDEX IF NOT EXISTS idx_feedback_rating ON ai_generation_feedback(rating);

COMMENT ON TABLE ai_generation_feedback IS 'AI 生成质量反馈';
COMMENT ON COLUMN ai_generation_feedback.categories IS '反馈分类 (JSONB 数组): ["术语准确","格式规范","内容完整","数据正确"]';
