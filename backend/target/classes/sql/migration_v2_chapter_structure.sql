-- ============================================================
-- Phase 0: 分章节编辑+系统组装 核心表结构
-- 所有新表均为增量添加，不影响现有表
-- ============================================================

-- ============================================================
-- P0-1: 项目主数据表
-- ============================================================
CREATE TABLE IF NOT EXISTS project_master_data (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    equipment_info      JSONB,          -- 装备基本信息
    tactical_indicators JSONB,           -- 战术技术指标体系
    product_tree        JSONB,          -- 产品分解结构(WBS)
    team_members        JSONB,          -- 研制团队
    milestones          JSONB,          -- 关键里程碑
    extended_fields     JSONB,          -- 扩展字段(不同装备类型)
    version_no          INT DEFAULT 1,
    status              VARCHAR(32) DEFAULT 'DRAFT',
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pmd_project ON project_master_data(project_id) WHERE deleted = 0;

-- ============================================================
-- P0-2: 文档模板分类 (A/B/C/D/E+软件)
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_template_category (
    id                  BIGSERIAL PRIMARY KEY,
    category_code       VARCHAR(32) NOT NULL,
    category_name       VARCHAR(128) NOT NULL,
    parent_id           BIGINT DEFAULT 0,
    gjb_reference       VARCHAR(256),
    description         TEXT,
    order_num           INT DEFAULT 0,
    status              VARCHAR(32) DEFAULT 'ACTIVE',
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dtc_code ON doc_template_category(category_code) WHERE deleted = 0;

-- ============================================================
-- P0-3: 增强版文档模板 V2
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_template_v2 (
    id                      BIGSERIAL PRIMARY KEY,
    category_id             BIGINT NOT NULL,
    template_code           VARCHAR(64) NOT NULL,
    template_name           VARCHAR(256) NOT NULL,
    template_type           VARCHAR(64),
    applicable_stage_codes  VARCHAR(256),
    applicable_project_type VARCHAR(128),
    gjb_standard_ref        VARCHAR(256),
    document_class          VARCHAR(32),
    file_object_id          VARCHAR(256),
    file_name               VARCHAR(256),
    file_size               BIGINT,
    file_type               VARCHAR(32),
    variables_schema        JSONB,
    status                  VARCHAR(32) DEFAULT 'ACTIVE',
    version_no              INT DEFAULT 1,
    created_by              BIGINT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_dtv2_category ON doc_template_v2(category_id) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_dtv2_code ON doc_template_v2(template_code) WHERE deleted = 0;

-- ============================================================
-- P0-4: 模板章节树
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_template_chapter (
    id                  BIGSERIAL PRIMARY KEY,
    template_id         BIGINT NOT NULL,
    parent_id           BIGINT DEFAULT 0,
    chapter_number      VARCHAR(32),
    chapter_title       VARCHAR(256) NOT NULL,
    chapter_level       INT DEFAULT 1,
    order_num           INT DEFAULT 0,
    is_required         BOOLEAN DEFAULT TRUE,
    applicability_rule  VARCHAR(512),
    content_schema      JSONB,
    standard_clause_ref VARCHAR(256),
    description         TEXT,
    writing_tips        TEXT,
    sample_content      TEXT,
    is_reusable_element BOOLEAN DEFAULT FALSE,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_dtch_template ON doc_template_chapter(template_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dtch_parent ON doc_template_chapter(parent_id) WHERE deleted = 0;

-- ============================================================
-- P0-5: 可复用标准元素
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_template_element (
    id                  BIGSERIAL PRIMARY KEY,
    element_code        VARCHAR(64) NOT NULL,
    element_name        VARCHAR(256) NOT NULL,
    element_type        VARCHAR(32),
    element_category    VARCHAR(64),
    content_json        JSONB,
    standard_refs       VARCHAR(512),
    keywords            VARCHAR(512),
    status              VARCHAR(32) DEFAULT 'ACTIVE',
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dte_code ON doc_template_element(element_code) WHERE deleted = 0;

-- ============================================================
-- P0-6: 章节-元素关联
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_template_chapter_element (
    id              BIGSERIAL PRIMARY KEY,
    chapter_id      BIGINT NOT NULL,
    element_id      BIGINT NOT NULL,
    order_num       INT DEFAULT 0,
    is_required     BOOLEAN DEFAULT FALSE
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dtce_pair ON doc_template_chapter_element(chapter_id, element_id);

-- ============================================================
-- P0-7: 文档实例章节内容
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_chapter (
    id                  BIGSERIAL PRIMARY KEY,
    doc_ledger_id       BIGINT NOT NULL,
    template_chapter_id BIGINT,
    parent_id           BIGINT DEFAULT 0,
    chapter_number      VARCHAR(32),
    chapter_title       VARCHAR(256) NOT NULL,
    chapter_level       INT DEFAULT 1,
    order_num           INT DEFAULT 0,
    content             TEXT,
    content_json        JSONB,
    fill_status         VARCHAR(32) DEFAULT 'EMPTY',
    fill_percentage     INT DEFAULT 0,
    status              VARCHAR(32) DEFAULT 'DRAFT',
    version_no          INT DEFAULT 1,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_dc_ledger ON doc_chapter(doc_ledger_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dc_parent ON doc_chapter(parent_id) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_dc_ledger_chapter ON doc_chapter(doc_ledger_id, template_chapter_id) WHERE deleted = 0;

-- ============================================================
-- P0-8: 完整性检查结果
-- ============================================================
CREATE TABLE IF NOT EXISTS completeness_check_result (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    doc_ledger_id       BIGINT,
    stage_id            BIGINT,
    check_type          VARCHAR(32) DEFAULT 'CHAPTER',
    total_items         INT DEFAULT 0,
    passed_items        INT DEFAULT 0,
    warning_items       INT DEFAULT 0,
    error_items         INT DEFAULT 0,
    score               DECIMAL(5,2) DEFAULT 0,
    detail_json         JSONB,
    checked_by          BIGINT,
    checked_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ccr_project ON completeness_check_result(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ccr_ledger ON completeness_check_result(doc_ledger_id) WHERE deleted = 0;

-- ============================================================
-- P0-9: 知识卡片
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_card (
    id                  BIGSERIAL PRIMARY KEY,
    card_type           VARCHAR(32) DEFAULT 'GJB_CLAUSE',
    target_table        VARCHAR(64),
    target_id           BIGINT,
    title               VARCHAR(256) NOT NULL,
    plain_language      TEXT,
    gjb_reference       VARCHAR(256),
    tags                VARCHAR(512),
    status              VARCHAR(32) DEFAULT 'ACTIVE',
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_kc_target ON knowledge_card(target_table, target_id) WHERE deleted = 0;
