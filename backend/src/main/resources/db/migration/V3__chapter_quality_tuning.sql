-- ============================================================
-- V3: 章节质量约束增强 — 最小字数、完成度权重
-- ============================================================

ALTER TABLE doc_template_chapter
    ADD COLUMN IF NOT EXISTS min_words INTEGER DEFAULT 300;

ALTER TABLE doc_template_chapter
    ADD COLUMN IF NOT EXISTS completion_weight NUMERIC(3,2) DEFAULT 1.00;

COMMENT ON COLUMN doc_template_chapter.min_words IS '最小字数下限，低于此值章节视为未完成';
COMMENT ON COLUMN doc_template_chapter.completion_weight IS '完成度计算权重，某些章节可设为0.5';
