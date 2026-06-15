-- ============================================================
-- V7: 四层记忆架构表
-- L2: chat_session - 会话记忆
-- L3: project_feedback + project_terminology - 项目记忆
-- L4: generation_exemplar - 领域知识范例
-- ============================================================

-- L2: 会话记忆
CREATE TABLE IF NOT EXISTS chat_session (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT,
    session_id      VARCHAR(64) NOT NULL,
    title            VARCHAR(256),
    messages        JSONB DEFAULT '[]'::jsonb,  -- [{role,content,timestamp}]
    generated_docs  JSONB DEFAULT '[]'::jsonb,  -- [{docLedgerId,docName,status}]
    context_snapshot TEXT,                       -- 生成时的项目上下文快照
    status          VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE/ARCHIVED
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cs_session ON chat_session(session_id);
CREATE INDEX IF NOT EXISTS idx_cs_project ON chat_session(project_id);

-- L3: 项目记忆 - 用户反馈（AI从中学修改模式）
CREATE TABLE IF NOT EXISTS project_feedback (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    doc_ledger_id   BIGINT,
    chapter_number  VARCHAR(32),
    original_content TEXT,
    corrected_content TEXT,
    feedback_type   VARCHAR(32),   -- TERM_FIX / FORMAT_FIX / CONTENT_ADD / CONTENT_DELETE
    tags            VARCHAR(256),  -- 逗号分隔关键词
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pf_project ON project_feedback(project_id);
CREATE INDEX IF NOT EXISTS idx_pf_type ON project_feedback(feedback_type);

-- L3: 项目术语表（用户修改的术语偏好）
CREATE TABLE IF NOT EXISTS project_terminology (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    term            VARCHAR(128) NOT NULL,
    preferred       VARCHAR(128) NOT NULL,  -- "应" 替代 "必须"
    source          VARCHAR(64) DEFAULT 'USER_FEEDBACK', -- USER_FEEDBACK / GJB_STANDARD
    usage_count     INT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, term)
);
CREATE INDEX IF NOT EXISTS idx_pt_project ON project_terminology(project_id);

-- L4: 领域知识 - 高质量范例
CREATE TABLE IF NOT EXISTS generation_exemplar (
    id              BIGSERIAL PRIMARY KEY,
    template_id     BIGINT,
    chapter_number  VARCHAR(32),
    chapter_title   VARCHAR(256),
    content         TEXT NOT NULL,
    quality_score   INT DEFAULT 0,   -- 用户评分(1-5)或审批通过(5)
    usage_count     INT DEFAULT 0,   -- 被引用次数
    source          VARCHAR(64) DEFAULT 'AI_GENERATED', -- AI_GENERATED / MANUAL / APPROVED
    project_id      BIGINT,          -- 来源项目（用于脱敏）
    tags            VARCHAR(256),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ge_template ON generation_exemplar(template_id);
CREATE INDEX IF NOT EXISTS idx_ge_score ON generation_exemplar(quality_score DESC);
