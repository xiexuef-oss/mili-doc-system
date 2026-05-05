CREATE TABLE IF NOT EXISTS sys_dict (
    id              BIGSERIAL PRIMARY KEY,
    dict_type       VARCHAR(64)  NOT NULL,
    dict_code       VARCHAR(64)  NOT NULL,
    dict_name       VARCHAR(128) NOT NULL,
    order_num       INT          DEFAULT 0,
    status          VARCHAR(32)  DEFAULT 'ACTIVE',
    created_by      BIGINT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT     DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sys_dict_type ON sys_dict(dict_type) WHERE deleted = 0;

INSERT INTO sys_dict (dict_type, dict_code, dict_name, order_num, status, created_by, created_at, updated_by, updated_at)
VALUES
    ('PROJECT_TYPE', 'MODEL',         '型号项目', 1, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('PROJECT_TYPE', 'PRE_RESEARCH',  '预研项目', 2, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('PROJECT_TYPE', 'TECH_IMPROVE',  '技改项目', 3, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('PROJECT_TYPE', 'PROD_SUPPORT',  '批产保障', 4, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    ('PROJECT_TYPE', 'OTHER',         '其他',     5, 'ACTIVE', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
