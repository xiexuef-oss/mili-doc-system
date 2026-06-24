-- ============================================================
-- V4: AI 文档画布系统数据表
-- ============================================================

-- 1. AI 文档主表
CREATE TABLE IF NOT EXISTS ai_document (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT,
    user_id         BIGINT        NOT NULL,
    title           VARCHAR(256)  NOT NULL,
    description     TEXT,
    document_type   VARCHAR(64),
    source_prompt   TEXT,
    status          VARCHAR(32)   DEFAULT 'draft',
    metadata        JSONB,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ai_doc_user_id   ON ai_document(user_id)   WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_doc_project   ON ai_document(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_doc_status    ON ai_document(status)    WHERE deleted = 0;

-- 2. AI 文档章节表
CREATE TABLE IF NOT EXISTS ai_document_section (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT        NOT NULL REFERENCES ai_document(id) ON DELETE CASCADE,
    parent_id       BIGINT,
    title           VARCHAR(256)  NOT NULL,
    level           INT           DEFAULT 1,
    sort_order      INT           DEFAULT 0,
    content         TEXT,
    content_json    JSONB,
    status          VARCHAR(32)   DEFAULT 'empty',
    created_by      BIGINT,
    updated_by      BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT      DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ai_sec_doc_id  ON ai_document_section(document_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_sec_sort    ON ai_document_section(document_id, sort_order) WHERE deleted = 0;

-- 3. AI 文档块表（预留）
CREATE TABLE IF NOT EXISTS ai_document_block (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT        NOT NULL REFERENCES ai_document(id) ON DELETE CASCADE,
    section_id      BIGINT        REFERENCES ai_document_section(id) ON DELETE CASCADE,
    type            VARCHAR(32)   NOT NULL,
    content         TEXT          NOT NULL,
    sort_order      INT           DEFAULT 0,
    created_by      BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_blk_sec ON ai_document_block(section_id);

-- 4. AI 对话会话表
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT        NOT NULL REFERENCES ai_document(id) ON DELETE CASCADE,
    user_id         BIGINT        NOT NULL,
    title           VARCHAR(256),
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_chat_s_doc ON ai_chat_session(document_id);

-- 5. AI 对话消息表
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT        NOT NULL REFERENCES ai_chat_session(id) ON DELETE CASCADE,
    role            VARCHAR(16)   NOT NULL,
    content         TEXT          NOT NULL,
    action_type     VARCHAR(64),
    metadata        JSONB,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_chat_m_sess ON ai_chat_message(session_id);

-- 6. AI 画布操作日志表
CREATE TABLE IF NOT EXISTS ai_canvas_operation (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT        NOT NULL REFERENCES ai_document(id) ON DELETE CASCADE,
    user_id         BIGINT        NOT NULL,
    operation_type  VARCHAR(64)   NOT NULL,
    patches         JSONB,
    before_snapshot JSONB,
    after_snapshot  JSONB,
    status          VARCHAR(32)   DEFAULT 'success',
    error_message   TEXT,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_op_doc     ON ai_canvas_operation(document_id);
CREATE INDEX IF NOT EXISTS idx_ai_op_created ON ai_canvas_operation(created_at DESC);

-- 7. AI 文档版本快照表
CREATE TABLE IF NOT EXISTS ai_document_version (
    id              BIGSERIAL PRIMARY KEY,
    document_id     BIGINT        NOT NULL REFERENCES ai_document(id) ON DELETE CASCADE,
    user_id         BIGINT        NOT NULL,
    title           VARCHAR(256),
    reason          VARCHAR(256),
    snapshot        JSONB         NOT NULL,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_ver_doc ON ai_document_version(document_id, created_at DESC);
