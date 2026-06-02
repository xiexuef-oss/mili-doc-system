-- ============================================================
-- V6: 文档输入参考关联 + 写作模板关联 + GJB6387规范类型
-- 串联四环节：阶段文件清单 → 参考文档/标准 → 写作模板 → AI初稿生成
-- ============================================================

-- V6.1: stage_doc_checklist_template 增加 template_id 和 spec_type
ALTER TABLE stage_doc_checklist_template ADD COLUMN IF NOT EXISTS template_id BIGINT;
ALTER TABLE stage_doc_checklist_template ADD COLUMN IF NOT EXISTS spec_type VARCHAR(32);

COMMENT ON COLUMN stage_doc_checklist_template.template_id IS '关联 doc_template_v2.id，指向该文件的写作模板';
COMMENT ON COLUMN stage_doc_checklist_template.spec_type IS 'GJB6387规范类型: SYSTEM_SPEC/DEV_SPEC/PRODUCT_SPEC/SOFTWARE_SPEC/MATERIAL_SPEC/PROCESS_SPEC';

CREATE INDEX IF NOT EXISTS idx_sdct_template ON stage_doc_checklist_template(template_id);
CREATE INDEX IF NOT EXISTS idx_sdct_spec_type ON stage_doc_checklist_template(spec_type);

-- V6.2: 文档输入参考关联表
-- 记录每个文档模板在编写时需要参考的上游文档、军用标准和知识卡片
CREATE TABLE IF NOT EXISTS doc_input_reference (
    id                      BIGSERIAL PRIMARY KEY,
    checklist_template_id   BIGINT NOT NULL,              -- 关联 stage_doc_checklist_template.id
    ref_type                VARCHAR(32) NOT NULL,         -- UPSTREAM_DOC / STANDARD / KNOWLEDGE_CARD
    ref_id                  BIGINT,                       -- 关联ID: checklist_template_id / standard_library.id / knowledge_card.id
    ref_code                VARCHAR(128),                 -- 参考编号, e.g. "GP-12", "GJB 6387-2008"
    ref_name                VARCHAR(256),                 -- 参考名称
    ref_usage               TEXT,                         -- 参考用途说明：该参考对当前文档的作用
    is_required             BOOLEAN DEFAULT TRUE,         -- 是否必须参考
    order_num               INT DEFAULT 0,                -- 排序号
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dir_template ON doc_input_reference(checklist_template_id);
CREATE INDEX IF NOT EXISTS idx_dir_ref_type ON doc_input_reference(ref_type);
CREATE INDEX IF NOT EXISTS idx_dir_ref_id ON doc_input_reference(ref_id);

-- V6.3: doc_ledger 增加来自清单的规范类型字段
ALTER TABLE doc_ledger ADD COLUMN IF NOT EXISTS spec_type VARCHAR(32);
COMMENT ON COLUMN doc_ledger.spec_type IS 'GJB6387规范类型，从stage_doc_checklist_template同步';
