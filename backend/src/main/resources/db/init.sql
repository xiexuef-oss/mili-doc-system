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

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_perm ON sys_role_permission(role_id, permission_id);

-- 字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id              BIGSERIAL PRIMARY KEY,
    dict_type       VARCHAR(64)  NOT NULL,
    dict_code       VARCHAR(128) NOT NULL,
    dict_name       VARCHAR(128) NOT NULL,
    order_num       INT          DEFAULT 0,
    status          VARCHAR(32)  DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dict_type_code ON sys_dict(dict_type, dict_code) WHERE deleted = 0;

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
    change_reason       VARCHAR(512),
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
    file_object_id      VARCHAR(256),
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
    file_object_id    VARCHAR(256),
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
    file_object_id  VARCHAR(256),
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
-- 模版库模块 (Template)
-- ============================================================

-- 文档模版表
CREATE TABLE IF NOT EXISTS doc_template (
    id                    BIGSERIAL PRIMARY KEY,
    template_name         VARCHAR(256) NOT NULL,
    template_code         VARCHAR(64),
    template_type         VARCHAR(64),
    applicable_project_type VARCHAR(128),
    description           TEXT,
    file_object_id        VARCHAR(256),
    file_name             VARCHAR(256),
    file_size             BIGINT,
    file_type             VARCHAR(32),
    variables             TEXT,
    status                VARCHAR(32) DEFAULT 'ACTIVE',
    created_by            BIGINT,
    created_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by            BIGINT,
    updated_at            TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted               SMALLINT    DEFAULT 0
);

-- ============================================================
-- 标准库模块 (Standard)
-- ============================================================

-- 标准表
CREATE TABLE IF NOT EXISTS standard (
    id              BIGSERIAL PRIMARY KEY,
    standard_code   VARCHAR(128) NOT NULL,
    standard_name   VARCHAR(256) NOT NULL,
    standard_type   VARCHAR(32),
    category        VARCHAR(64),
    version         VARCHAR(32),
    publish_date    DATE,
    effective_date  DATE,
    description     TEXT,
    file_object_id  VARCHAR(256),
    file_name       VARCHAR(256),
    file_size       BIGINT,
    file_type       VARCHAR(32),
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT    DEFAULT 0
);

-- 标准条款表
CREATE TABLE IF NOT EXISTS standard_clause (
    id              BIGSERIAL PRIMARY KEY,
    standard_id     BIGINT NOT NULL,
    clause_number   VARCHAR(32),
    clause_title    VARCHAR(256),
    clause_content  TEXT,
    parent_id       BIGINT DEFAULT 0,
    order_num       INT    DEFAULT 0,
    keywords        VARCHAR(512),
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT  DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_standard_clause_sid ON standard_clause(standard_id) WHERE deleted = 0;

-- ============================================================
-- 索引
-- ============================================================

-- 项目相关索引
CREATE INDEX IF NOT EXISTS idx_project_status ON project(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_project_member_project ON project_member(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_project_stage_project ON project_stage(project_id) WHERE deleted = 0;

-- 项目输入文件表 (任务书/合同/质量程序等)
CREATE TABLE IF NOT EXISTS project_input_file (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    file_name       VARCHAR(256) NOT NULL,
    file_object_id  VARCHAR(256),
    file_size       BIGINT,
    file_type       VARCHAR(32),
    input_type      VARCHAR(64) NOT NULL,
    description     VARCHAR(512),
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT  DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pif_project ON project_input_file(project_id) WHERE deleted = 0;

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
VALUES (1, 'admin', '$2b$10$Xh58k8sRfL3NAEMSAboLZerwT2ym/T4Y3JjtIFYItGNr3w9zIBEY6', '系统管理员', 'admin@military-doc.local', 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
-- default password: admin123
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

-- 默认权限
INSERT INTO sys_permission (id, permission_code, permission_name, resource_type, path, parent_id, order_num, created_by, created_at, updated_by, updated_at)
VALUES
    (1,  'project',           '项目管理',   'MENU', '/projects',  0,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (2,  'project:create',    '创建项目',   'BTN',  NULL,         1,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (3,  'project:edit',      '编辑项目',   'BTN',  NULL,         1,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (4,  'project:delete',    '删除项目',   'BTN',  NULL,         1,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (5,  'document',          '文档管理',   'MENU', '/documents', 0,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (6,  'document:create',   '创建文档',   'BTN',  NULL,         5,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (7,  'document:edit',     '编辑文档',   'BTN',  NULL,         5,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (8,  'document:delete',   '删除文档',   'BTN',  NULL,         5,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (9,  'catalog',           '文档目录',   'MENU', '/catalogs',  0,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (10, 'catalog:create',    '创建目录',   'BTN',  NULL,         9,  1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (11, 'catalog:edit',      '编辑目录',   'BTN',  NULL,         9,  2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (12, 'catalog:delete',    '删除目录',   'BTN',  NULL,         9,  3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (13, 'meeting',           '评审会议',   'MENU', '/meetings',  0,  4, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (14, 'meeting:create',    '创建会议',   'BTN',  NULL,         13, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (15, 'meeting:edit',      '编辑会议',   'BTN',  NULL,         13, 2, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (16, 'meeting:delete',    '删除会议',   'BTN',  NULL,         13, 3, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (17, 'system:user',       '用户管理',   'MENU', '/users',     0,  5, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (18, 'system:user:crud',  '用户CRUD',   'BTN',  NULL,         17, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (19, 'system:role',       '角色管理',   'MENU', '/roles',     0,  6, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (20, 'system:role:crud',  '角色CRUD',   'BTN',  NULL,         19, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (21, 'system:dict',       '字典配置',   'MENU', '/dicts',     0,  7, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (22, 'system:dict:crud',  '字典CRUD',   'BTN',  NULL,         21, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (23, 'system:perm',       '权限管理',   'MENU', '/permissions',0, 8, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (24, 'system:perm:crud',  '权限CRUD',   'BTN',  NULL,         23, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (25, 'template',          '模版管理',   'MENU', '/templates',  0,  9, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (26, 'template:crud',     '模版CRUD',   'BTN',  NULL,         25, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (27, 'standard',          '标准库',     'MENU', '/standards',  0, 10, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (28, 'standard:crud',     '标准CRUD',   'BTN',  NULL,         27, 1, 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 知识库
CREATE TABLE IF NOT EXISTS knowledge_base (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(256) NOT NULL,
    content         TEXT,
    category        VARCHAR(64),
    tags            VARCHAR(512),
    file_object_id  VARCHAR(256),
    file_name       VARCHAR(256),
    file_size       BIGINT,
    file_type       VARCHAR(32),
    status          VARCHAR(32) DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER   DEFAULT 0
);
COMMENT ON TABLE knowledge_base IS '知识库';
COMMENT ON COLUMN knowledge_base.title IS '标题';
COMMENT ON COLUMN knowledge_base.content IS '内容';
COMMENT ON COLUMN knowledge_base.category IS '分类';
COMMENT ON COLUMN knowledge_base.tags IS '标签（逗号分隔）';
CREATE INDEX idx_kb_category ON knowledge_base(category);
CREATE INDEX idx_kb_status ON knowledge_base(status);

-- 初始字典数据
INSERT INTO sys_dict (id, dict_type, dict_code, dict_name, order_num, status, created_by, created_at, updated_by, updated_at)
VALUES
    (1,  'PROJECT_TYPE', 'MODEL',         '型号项目',   1, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (2,  'PROJECT_TYPE', 'PRE_RESEARCH',  '预研项目',   2, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (3,  'PROJECT_TYPE', 'TECH_IMPROVE',  '技改项目',   3, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (4,  'PROJECT_TYPE', 'BATCH_GUARANTEE','批产保障',  4, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (5,  'PROJECT_TYPE', 'OTHER',         '其他',       5, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (6,  'SECURITY_LEVEL', 'PUBLIC',      '公开',       1, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (7,  'SECURITY_LEVEL', 'INTERNAL',    '内部',       2, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (8,  'SECURITY_LEVEL', 'SECRET',      '秘密',       3, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (9,  'SECURITY_LEVEL', 'CONFIDENTIAL','机密',       4, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (10, 'SECURITY_LEVEL', 'TOP_SECRET',  '绝密',       5, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (11, 'PROJECT_STATUS', 'DRAFT',       '草稿',       1, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (12, 'PROJECT_STATUS', 'IN_PROGRESS', '进行中',     2, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (13, 'PROJECT_STATUS', 'COMPLETED',   '已完成',     3, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (14, 'PROJECT_STATUS', 'ARCHIVED',    '已归档',     4, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- AI 训练示例表
CREATE TABLE IF NOT EXISTS ai_training_example (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT,
    doc_file_id     BIGINT,
    catalog_id      BIGINT,
    prompt          TEXT,
    completion      TEXT,
    quality         VARCHAR(32)  DEFAULT 'PENDING_REVIEW',
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ai_training_quality ON ai_training_example(quality) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ai_training_project ON ai_training_example(project_id) WHERE deleted = 0;

-- 文档台账 (合并 doc_catalog + doc_file)
CREATE TABLE IF NOT EXISTS doc_ledger (
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
    change_reason       VARCHAR(512),
    responsible_user_id BIGINT,
    security_level      VARCHAR(32),
    lifecycle_status    VARCHAR(32)  DEFAULT 'PLANNED',
    file_object_id      VARCHAR(256),
    content_size        BIGINT,
    created_by          BIGINT,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_doc_ledger_project ON doc_ledger(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_ledger_stage ON doc_ledger(stage_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_doc_ledger_lifecycle ON doc_ledger(lifecycle_status) WHERE deleted = 0;

-- 文档台账操作日志
CREATE TABLE IF NOT EXISTS doc_ledger_log (
    id              BIGSERIAL PRIMARY KEY,
    doc_ledger_id   BIGINT NOT NULL,
    from_status     VARCHAR(32),
    to_status       VARCHAR(32) NOT NULL,
    operator_id     BIGINT,
    operated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512)
);
CREATE INDEX IF NOT EXISTS idx_doc_ledger_log_ledger ON doc_ledger_log(doc_ledger_id);

-- ============================================================
-- 技术状态管理模块 (GJB 3206B)
-- ============================================================

-- 技术状态项
CREATE TABLE IF NOT EXISTS configuration_item (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    stage_id            BIGINT,
    ci_code             VARCHAR(64),
    ci_name             VARCHAR(256) NOT NULL,
    ci_type             VARCHAR(64),
    parent_ci_id        BIGINT,
    responsible_user_id BIGINT,
    current_version     VARCHAR(32),
    status              VARCHAR(32)  DEFAULT 'ACTIVE',
    is_controlled       BOOLEAN      DEFAULT FALSE,
    is_key_item         BOOLEAN      DEFAULT FALSE,
    description         VARCHAR(1024),
    created_by          BIGINT,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ci_project ON configuration_item(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ci_stage ON configuration_item(stage_id) WHERE deleted = 0;

-- 技术状态基线
CREATE TABLE IF NOT EXISTS configuration_baseline (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    stage_id        BIGINT,
    baseline_code   VARCHAR(64),
    baseline_name   VARCHAR(256) NOT NULL,
    baseline_type   VARCHAR(64),
    baseline_version VARCHAR(32),
    baseline_status VARCHAR(32)  DEFAULT 'DRAFT',
    approve_user_id BIGINT,
    approve_time    TIMESTAMP,
    effective_time  TIMESTAMP,
    description     VARCHAR(1024),
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cb_project ON configuration_baseline(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_cb_stage ON configuration_baseline(stage_id) WHERE deleted = 0;

-- 基线项
CREATE TABLE IF NOT EXISTS configuration_baseline_item (
    id              BIGSERIAL PRIMARY KEY,
    baseline_id     BIGINT NOT NULL,
    item_type       VARCHAR(32),
    item_id         BIGINT,
    item_version_id BIGINT,
    item_code       VARCHAR(64),
    item_name       VARCHAR(256),
    item_version    VARCHAR(32),
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_cbi_baseline ON configuration_baseline_item(baseline_id) WHERE deleted = 0;

-- 技术状态更改申请
CREATE TABLE IF NOT EXISTS configuration_change_request (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    stage_id            BIGINT,
    change_code         VARCHAR(64),
    change_title        VARCHAR(256) NOT NULL,
    change_type         VARCHAR(64),
    change_level        VARCHAR(32),
    change_reason       VARCHAR(1024),
    change_content      TEXT,
    impact_analysis     TEXT,
    applicant_id        BIGINT,
    responsible_user_id BIGINT,
    status              VARCHAR(32)  DEFAULT 'DRAFT',
    ccb_meeting_id      BIGINT,
    approve_result      VARCHAR(32),
    approve_opinion     VARCHAR(1024),
    approve_time        TIMESTAMP,
    created_by          BIGINT,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ccr_project ON configuration_change_request(project_id) WHERE deleted = 0;

-- 技术状态记实
CREATE TABLE IF NOT EXISTS configuration_status_accounting (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    stage_id            BIGINT,
    ci_id               BIGINT,
    doc_ledger_id       BIGINT,
    doc_version_id      BIGINT,
    event_type          VARCHAR(64) NOT NULL,
    event_name          VARCHAR(256),
    event_description   VARCHAR(1024),
    related_object_type VARCHAR(32),
    related_object_id   BIGINT,
    event_time          TIMESTAMP,
    operator_id         BIGINT,
    created_by          BIGINT,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_csa_project ON configuration_status_accounting(project_id) WHERE deleted = 0;

-- 技术状态审核
CREATE TABLE IF NOT EXISTS configuration_audit (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    stage_id        BIGINT,
    baseline_id     BIGINT,
    audit_code      VARCHAR(64),
    audit_name      VARCHAR(256) NOT NULL,
    audit_type      VARCHAR(64),
    audit_status    VARCHAR(32)  DEFAULT 'PLANNED',
    meeting_id      BIGINT,
    audit_result    VARCHAR(32),
    audit_opinion   VARCHAR(1024),
    audit_time      TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ca_project ON configuration_audit(project_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_ca_stage ON configuration_audit(stage_id) WHERE deleted = 0;

-- 项目表增加当前阶段
ALTER TABLE project ADD COLUMN IF NOT EXISTS current_stage_id BIGINT;

-- 管理员角色授予所有权限
INSERT INTO sys_role_permission (role_id, permission_id, created_by, created_at)
SELECT 1, id, 1, CURRENT_TIMESTAMP FROM sys_permission
ON CONFLICT DO NOTHING;
