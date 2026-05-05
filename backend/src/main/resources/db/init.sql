-- ============================================================
-- 军工项目文档策划与编制一体机 - 数据库初始化脚本
-- Database: PostgreSQL
-- ============================================================

-- 创建数据库 (需要超级用户权限，通常单独执行)
-- CREATE DATABASE "mili-doc" WITH ENCODING 'UTF8';

-- ============================================================
-- 系统模块 (System)
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password        VARCHAR(256) NOT NULL,
    real_name       VARCHAR(64),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    status          VARCHAR(32)  DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user(username) WHERE deleted = 0;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGSERIAL PRIMARY KEY,
    role_code       VARCHAR(64)  NOT NULL,
    role_name       VARCHAR(64)  NOT NULL,
    description     VARCHAR(256),
    order_num       INT          DEFAULT 0,
    status          VARCHAR(32)  DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_code ON sys_role(role_code) WHERE deleted = 0;

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(128) NOT NULL,
    resource_type   VARCHAR(32),
    path            VARCHAR(256),
    parent_id       BIGINT       DEFAULT 0,
    order_num       INT          DEFAULT 0,
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_perm_code ON sys_permission(permission_code) WHERE deleted = 0;

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_role ON sys_user_role(user_id, role_id);

-- ============================================================
-- 项目管理模块 (Project)
-- ============================================================

-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id                  BIGSERIAL PRIMARY KEY,
    project_code        VARCHAR(64)  NOT NULL,
    project_name        VARCHAR(256) NOT NULL,
    project_type        VARCHAR(64),
    security_level      VARCHAR(32),
    status              VARCHAR(32)  DEFAULT 'DRAFT',
    owner_user_id       VARCHAR(64),
    applicable_standards VARCHAR(512),
    start_date          DATE,
    end_date            DATE,
    description         TEXT,
    created_by          BIGINT,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);

-- 项目成员表
CREATE TABLE IF NOT EXISTS project_member (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    role_in_project VARCHAR(64),
    duties          VARCHAR(256),
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT    DEFAULT 0
);

-- 项目阶段表
CREATE TABLE IF NOT EXISTS project_stage (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    stage_code      VARCHAR(64),
    stage_name      VARCHAR(128),
    stage_order     INT,
    status          VARCHAR(32) DEFAULT 'NOT_STARTED',
    start_date      DATE,
    end_date        DATE,
    stage_goal      TEXT,
    entry_criteria  TEXT,
    exit_criteria   TEXT,
    created_by      BIGINT,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT    DEFAULT 0
);

-- 阶段转阶段检查表
CREATE TABLE IF NOT EXISTS stage_transition_check (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    from_stage_id   BIGINT,
    to_stage_id     BIGINT,
    check_status    VARCHAR(32),
    blocker_items   TEXT,
    check_result    TEXT,
    checked_by      BIGINT,
    checked_at      TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT    DEFAULT 0
);

-- ============================================================
-- 文档模块 (Document)
-- ============================================================

-- 文档目录(策划清单)表
CREATE TABLE IF NOT EXISTS doc_catalog (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    stage_id            BIGINT,
    doc_code            VARCHAR(64),
    doc_name            VARCHAR(256) NOT NULL,
    doc_type            VARCHAR(64),
    required_flag       BOOLEAN      DEFAULT TRUE,
    meeting_usage       VARCHAR(64),
    usage_source        VARCHAR(128),
    usage_adjust_reason VARCHAR(512),
    responsible_user_id BIGINT,
    status              VARCHAR(32)  DEFAULT 'DRAFT',
    created_by          BIGINT,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);

-- 文档文件表
CREATE TABLE IF NOT EXISTS doc_file (
    id              BIGSERIAL PRIMARY KEY,
    catalog_id      BIGINT,
    project_id      BIGINT NOT NULL,
    stage_id        BIGINT,
    doc_name        VARCHAR(256) NOT NULL,
    doc_type        VARCHAR(64),
    security_level  VARCHAR(32),
    status          VARCHAR(32)  DEFAULT 'DRAFT',
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);

-- 文档版本表
CREATE TABLE IF NOT EXISTS doc_version (
    id                  BIGSERIAL PRIMARY KEY,
    doc_file_id         BIGINT NOT NULL,
    version_no          VARCHAR(32) NOT NULL,
    source_type         VARCHAR(32),
    base_version_id     BIGINT,
    file_object_id      BIGINT,
    version_status      VARCHAR(32) DEFAULT 'DRAFT',
    optimistic_version  INT         DEFAULT 1,
    submit_user_id      BIGINT,
    submit_time         TIMESTAMP,
    change_summary      TEXT,
    created_by          BIGINT,
    created_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT    DEFAULT 0
);

-- 文档编辑会话表
CREATE TABLE IF NOT EXISTS doc_edit_session (
    id              BIGSERIAL PRIMARY KEY,
    doc_file_id     BIGINT NOT NULL,
    base_version_id BIGINT,
    draft_version_id BIGINT,
    editor_user_id  BIGINT,
    session_status  VARCHAR(32) DEFAULT 'OPEN',
    opened_at       TIMESTAMP,
    submitted_at    TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT    DEFAULT 0
);

-- 文档编辑锁定表
CREATE TABLE IF NOT EXISTS doc_edit_lock (
    id                  BIGSERIAL PRIMARY KEY,
    doc_file_id         BIGINT NOT NULL,
    locked_version_id   BIGINT,
    locked_by           BIGINT,
    lock_type           VARCHAR(32) DEFAULT 'EDIT',
    lock_status         VARCHAR(32) DEFAULT 'ACTIVE',
    expire_at           TIMESTAMP,
    created_by          BIGINT,
    created_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT    DEFAULT 0
);

-- 文档变更影响表
CREATE TABLE IF NOT EXISTS doc_change_impact (
    id                  BIGSERIAL PRIMARY KEY,
    change_event_id     BIGINT NOT NULL,
    impacted_doc_file_id BIGINT NOT NULL,
    impact_reason       TEXT,
    suggest_action      TEXT,
    responsible_user_id BIGINT,
    status              VARCHAR(32) DEFAULT 'PENDING',
    closed_at           TIMESTAMP,
    created_by          BIGINT,
    created_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT    DEFAULT 0
);

-- 文档生效基线表
CREATE TABLE IF NOT EXISTS doc_effective_baseline (
    id                    BIGSERIAL PRIMARY KEY,
    project_id            BIGINT NOT NULL,
    stage_id              BIGINT,
    doc_file_id           BIGINT NOT NULL,
    effective_version_id  BIGINT,
    final_version_id      BIGINT,
    confirmed_by          BIGINT,
    confirmed_at          TIMESTAMP,
    baseline_status       VARCHAR(32) DEFAULT 'DRAFT',
    created_by            BIGINT,
    created_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by            BIGINT,
    updated_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted               SMALLINT    DEFAULT 0
);

-- ============================================================
-- 评审模块 (Review)
-- ============================================================

-- 评审会议表
CREATE TABLE IF NOT EXISTS review_meeting (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    stage_id        BIGINT,
    meeting_code    VARCHAR(64),
    meeting_name    VARCHAR(256) NOT NULL,
    meeting_type    VARCHAR(64),
    meeting_date    DATE,
    meeting_location VARCHAR(256),
    host_user_id    BIGINT,
    attendee_users  TEXT,
    expert_group    VARCHAR(256),
    status          VARCHAR(32)  DEFAULT 'DRAFT',
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);

-- 评审会议-文档关联表
CREATE TABLE IF NOT EXISTS review_meeting_document (
    id                      BIGSERIAL PRIMARY KEY,
    meeting_id              BIGINT NOT NULL,
    doc_file_id             BIGINT NOT NULL,
    doc_version_id          BIGINT,
    review_result           VARCHAR(64),
    material_complete_flag  BOOLEAN DEFAULT FALSE,
    closed_flag             BOOLEAN DEFAULT FALSE,
    created_by              BIGINT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted                 SMALLINT  DEFAULT 0
);

-- 专家意见文件表
CREATE TABLE IF NOT EXISTS review_expert_opinion_file (
    id                BIGSERIAL PRIMARY KEY,
    meeting_id        BIGINT NOT NULL,
    expert_user_id    BIGINT,
    expert_group_name VARCHAR(128),
    doc_file_id       BIGINT,
    file_object_id    BIGINT,
    problem_level     VARCHAR(32),
    uploaded_at       TIMESTAMP,
    created_by        BIGINT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT  DEFAULT 0
);

-- 会议意见汇总表
CREATE TABLE IF NOT EXISTS review_meeting_opinion_file (
    id              BIGSERIAL PRIMARY KEY,
    meeting_id      BIGINT NOT NULL,
    doc_file_id     BIGINT,
    opinion_type    VARCHAR(32),
    file_object_id  BIGINT,
    status          VARCHAR(32) DEFAULT 'DRAFT',
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT    DEFAULT 0
);

-- ============================================================
-- 索引
-- ============================================================

-- 项目相关索引
CREATE INDEX IF NOT EXISTS idx_project_status ON project(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_project_member_project ON project_member(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_project_stage_project ON project_stage(project_id) WHERE deleted = 0;

-- 文档相关索引
CREATE INDEX IF NOT EXISTS idx_doc_catalog_project ON doc_catalog(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_file_project ON doc_file(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_file_catalog ON doc_file(catalog_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_version_file ON doc_version(doc_file_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_edit_session_file ON doc_edit_session(doc_file_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_edit_lock_file ON doc_edit_lock(doc_file_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_baseline_project ON doc_effective_baseline(project_id) WHERE deleted = 0;

-- 评审相关索引
CREATE INDEX IF NOT EXISTS idx_review_meeting_project ON review_meeting(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_review_meeting_doc_meeting ON review_meeting_document(meeting_id) WHERE deleted = 0;

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认管理员用户 (密码: 303319, BCrypt加密)
INSERT INTO sys_user (id, username, password, real_name, email, status, created_by, created_at, updated_by, updated_at)
VALUES (1, 'admin', '$2b$12$sLNAHfFWOJkCDCUVCPwN7uabFwhA8u63OftRNcAMv5x0ZD/xNmCh.', '系统管理员', 'admin@military-doc.local', 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 默认角色
INSERT INTO sys_role (id, role_code, role_name, description, order_num, status, created_by, created_at, updated_by, updated_at)
VALUES
    (1, 'ADMIN',     '系统管理员', '拥有全部权限',   1, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (2, 'PM',        '项目经理',   '管理项目和执行',  2, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (3, 'EDITOR',    '文档编制员', '编制和修改文档',  3, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (4, 'REVIEWER',  '评审专家',   '参与文档评审',    4, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (5, 'READONLY',  '只读用户',   '查看权限',        5, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 管理员角色关联
INSERT INTO sys_user_role (user_id, role_id, created_by, created_at)
VALUES (1, 1, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
