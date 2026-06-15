-- ============================================================
-- V5: 模板 DOCX 自动解析字段
-- 支持上传模板文件后自动提取章节结构、变量、表格信息
-- ============================================================

ALTER TABLE doc_template_chapter
    ADD COLUMN IF NOT EXISTS heading_style VARCHAR(32),
    ADD COLUMN IF NOT EXISTS numbering_format VARCHAR(64),
    ADD COLUMN IF NOT EXISTS has_table BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS table_json JSONB,
    ADD COLUMN IF NOT EXISTS variable_placeholders TEXT,
    ADD COLUMN IF NOT EXISTS font_emphasis VARCHAR(32);

-- 添加嵌入索引状态标记 (之前可能通过 application 代码检查但无列)
ALTER TABLE standard_clause
    ADD COLUMN IF NOT EXISTS embedding_indexed BOOLEAN DEFAULT FALSE;

ALTER TABLE knowledge_base
    ADD COLUMN IF NOT EXISTS embedding_indexed BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN doc_template_chapter.heading_style IS '原始Word样式名 (Heading1-9)';
COMMENT ON COLUMN doc_template_chapter.numbering_format IS '编号格式 (如 "3.1.2", "一、")';
COMMENT ON COLUMN doc_template_chapter.has_table IS '是否包含表格';
COMMENT ON COLUMN doc_template_chapter.table_json IS '表格结构JSON (headers, rowCount, columnCount)';
COMMENT ON COLUMN doc_template_chapter.variable_placeholders IS '检测到的变量占位符列表 (逗号分隔)';
COMMENT ON COLUMN doc_template_chapter.font_emphasis IS '字体强调类型 (bold/color/underline)';
COMMENT ON COLUMN standard_clause.embedding_indexed IS '是否已完成向量嵌入索引';
COMMENT ON COLUMN knowledge_base.embedding_indexed IS '是否已完成向量嵌入索引';
