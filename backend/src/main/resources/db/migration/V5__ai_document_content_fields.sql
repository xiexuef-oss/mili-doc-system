-- ============================================================
-- V5: AI 文档画布 - 内容字段扩展
-- ============================================================

-- 1. 为 ai_document 添加内容相关字段
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS content_json JSONB;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS content_text TEXT;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS content_html TEXT;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS content_markdown TEXT;
ALTER TABLE ai_document ADD COLUMN IF NOT EXISTS outline_json JSONB;

-- 2. 迁移 metadata 中存储的内容到 content_json
UPDATE ai_document SET content_json = metadata WHERE metadata IS NOT NULL;

-- 3. 为 ai_canvas_operation 添加 token 统计和模型字段
ALTER TABLE ai_canvas_operation ADD COLUMN IF NOT EXISTS model_name VARCHAR(128);
ALTER TABLE ai_canvas_operation ADD COLUMN IF NOT EXISTS prompt_tokens INT;
ALTER TABLE ai_canvas_operation ADD COLUMN IF NOT EXISTS completion_tokens INT;
ALTER TABLE ai_canvas_operation ADD COLUMN IF NOT EXISTS total_tokens INT;

-- 4. 创建索引
CREATE INDEX IF NOT EXISTS idx_ai_doc_content_gin ON ai_document USING GIN(content_json);
CREATE INDEX IF NOT EXISTS idx_ai_doc_outline_gin ON ai_document USING GIN(outline_json);
