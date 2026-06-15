-- Migration V10: 两师系统组织架构 + 签审流程
-- 实施军工型号研制的"技术指挥线+行政指挥线"两师体系

-- ============================================================
-- 1. 项目成员表增强
-- ============================================================
ALTER TABLE project_member
    ADD COLUMN IF NOT EXISTS member_line VARCHAR(32),           -- 指挥线: TECHNICAL/ADMINISTRATIVE/QUALITY/CRAFT
    ADD COLUMN IF NOT EXISTS member_position VARCHAR(64),       -- 岗位编码: CHIEF_DESIGNER/CHIEF_COMMANDER/...
    ADD COLUMN IF NOT EXISTS supervisor_id BIGINT,              -- 上级 member_id，构建指挥链
    ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;          -- 排序

CREATE INDEX IF NOT EXISTS idx_pm_line ON project_member(member_line);
CREATE INDEX IF NOT EXISTS idx_pm_supervisor ON project_member(supervisor_id);

-- ============================================================
-- 2. 用户表增强
-- ============================================================
ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS org_name VARCHAR(128),             -- 所在单位/部门
    ADD COLUMN IF NOT EXISTS title VARCHAR(64);                 -- 职称/军衔

-- ============================================================
-- 3. 阶段表增强 (两师签批)
-- ============================================================
ALTER TABLE project_stage
    ADD COLUMN IF NOT EXISTS chief_designer_confirmed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS chief_commander_confirmed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS chief_designer_confirmed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS chief_commander_confirmed_at TIMESTAMP;

-- ============================================================
-- 4. 签审记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_approval_record (
    id                  BIGSERIAL PRIMARY KEY,
    doc_ledger_id       BIGINT NOT NULL,
    approval_step       VARCHAR(32) NOT NULL,                   -- DESIGN/CHECK/REVIEW/APPROVE/COUNTERSIGN
    approver_id         BIGINT,                                 -- 审批人 sys_user.id
    approver_position   VARCHAR(64),                            -- 审批岗位
    approval_result     VARCHAR(32),                            -- APPROVED/REJECTED/RETURN
    approval_opinion    TEXT,                                   -- 审批意见
    approved_at         TIMESTAMP,
    sort_order          INT DEFAULT 0,                          -- 审批顺序
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dar_ledger ON doc_approval_record(doc_ledger_id);
CREATE INDEX IF NOT EXISTS idx_dar_approver ON doc_approval_record(approver_id);
CREATE INDEX IF NOT EXISTS idx_dar_step ON doc_approval_record(approval_step);

-- ============================================================
-- 5. 签审流程模板表
-- ============================================================
CREATE TABLE IF NOT EXISTS doc_approval_flow_template (
    id                  BIGSERIAL PRIMARY KEY,
    template_name       VARCHAR(128) NOT NULL,
    doc_type            VARCHAR(64),                            -- 适用文档类型
    approval_steps      JSONB NOT NULL DEFAULT '[]',            -- 签审步骤配置
    is_default          BOOLEAN DEFAULT FALSE,
    deleted             SMALLINT DEFAULT 0
);

-- ============================================================
-- 6. 两师岗位体系 字典种子数据
-- ============================================================
INSERT INTO sys_dict (dict_type, dict_code, dict_name, parent_code, order_num, status)
VALUES
    -- 技术指挥线
    ('MEMBER_LINE', 'TECHNICAL', '技术指挥线', NULL, 1, 'ACTIVE'),
    ('MEMBER_LINE', 'ADMINISTRATIVE', '行政指挥线', NULL, 2, 'ACTIVE'),
    ('MEMBER_LINE', 'QUALITY', '质量线', NULL, 3, 'ACTIVE'),
    ('MEMBER_LINE', 'CRAFT', '工艺线', NULL, 4, 'ACTIVE'),

    -- 技术线岗位
    ('MEMBER_POSITION', 'CHIEF_DESIGNER', '总设计师', 'TECHNICAL', 1, 'ACTIVE'),
    ('MEMBER_POSITION', 'DEPUTY_CHIEF_DESIGNER', '副总设计师', 'TECHNICAL', 2, 'ACTIVE'),
    ('MEMBER_POSITION', 'CHIEF_DESIGNER_ENGINEER', '主任设计师', 'TECHNICAL', 3, 'ACTIVE'),
    ('MEMBER_POSITION', 'LEAD_DESIGNER_ENGINEER', '主管设计师', 'TECHNICAL', 4, 'ACTIVE'),
    ('MEMBER_POSITION', 'DESIGNER_ENGINEER', '设计师', 'TECHNICAL', 5, 'ACTIVE'),

    -- 行政线岗位
    ('MEMBER_POSITION', 'CHIEF_COMMANDER', '总指挥', 'ADMINISTRATIVE', 1, 'ACTIVE'),
    ('MEMBER_POSITION', 'DEPUTY_CHIEF_COMMANDER', '副总指挥', 'ADMINISTRATIVE', 2, 'ACTIVE'),
    ('MEMBER_POSITION', 'PROJECT_OFFICE_DIRECTOR', '项目办主任', 'ADMINISTRATIVE', 3, 'ACTIVE'),
    ('MEMBER_POSITION', 'PLAN_SUPERVISOR', '计划主管', 'ADMINISTRATIVE', 4, 'ACTIVE'),
    ('MEMBER_POSITION', 'PROJECT_ASSISTANT', '项目助理', 'ADMINISTRATIVE', 5, 'ACTIVE'),

    -- 质量线岗位
    ('MEMBER_POSITION', 'CHIEF_QUALITY_ENGINEER', '总质量师', 'QUALITY', 1, 'ACTIVE'),
    ('MEMBER_POSITION', 'QUALITY_SUPERVISOR', '质量主管', 'QUALITY', 2, 'ACTIVE'),
    ('MEMBER_POSITION', 'QUALITY_ENGINEER', '质量师', 'QUALITY', 3, 'ACTIVE'),

    -- 工艺线岗位
    ('MEMBER_POSITION', 'CHIEF_PROCESS_ENGINEER', '总工艺师', 'CRAFT', 1, 'ACTIVE'),
    ('MEMBER_POSITION', 'PROCESS_ENGINEER', '工艺师', 'CRAFT', 2, 'ACTIVE')
ON CONFLICT (dict_type, dict_code) DO NOTHING;

-- ============================================================
-- 7. 系统角色补充 (军事特有角色)
-- ============================================================
INSERT INTO sys_role (id, role_code, role_name, description, order_num, status)
VALUES
    (6, 'CHIEF_DESIGNER', '总设计师', '技术决策权限', 6, 'ACTIVE'),
    (7, 'CHIEF_COMMANDER', '总指挥', '资源调配权限', 7, 'ACTIVE'),
    (8, 'QUALITY_MANAGER', '质量师', '质量审批权限', 8, 'ACTIVE'),
    (9, 'MILITARY_REP', '军代表', '军方监督权限', 9, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 8. 默认签审流程模板
-- ============================================================
INSERT INTO doc_approval_flow_template (template_name, doc_type, approval_steps, is_default)
VALUES
    ('技术文件标准签审链', 'TECH_DOC',
     '[
       {"step":"DESIGN","position":"DESIGNER_ENGINEER","label":"编制","line":"TECHNICAL","order":1},
       {"step":"CHECK","position":"LEAD_DESIGNER_ENGINEER","label":"校对","line":"TECHNICAL","order":2},
       {"step":"REVIEW","position":"CHIEF_DESIGNER_ENGINEER","label":"审核","line":"TECHNICAL","order":3},
       {"step":"COUNTERSIGN","position":"CHIEF_QUALITY_ENGINEER","label":"会签(质量)","line":"QUALITY","order":4},
       {"step":"APPROVE","position":"CHIEF_DESIGNER","label":"批准(总师)","line":"TECHNICAL","order":5}
     ]'::jsonb, TRUE),

    ('管理文件签审链', 'MGMT_DOC',
     '[
       {"step":"DESIGN","position":"PROJECT_ASSISTANT","label":"编制","line":"ADMINISTRATIVE","order":1},
       {"step":"REVIEW","position":"PROJECT_OFFICE_DIRECTOR","label":"审核","line":"ADMINISTRATIVE","order":2},
       {"step":"APPROVE","position":"CHIEF_COMMANDER","label":"批准(总指挥)","line":"ADMINISTRATIVE","order":3}
     ]'::jsonb, FALSE),

    ('评审文件签审链', 'REVIEW_DOC',
     '[
       {"step":"DESIGN","position":"DESIGNER_ENGINEER","label":"编制","line":"TECHNICAL","order":1},
       {"step":"REVIEW","position":"CHIEF_DESIGNER_ENGINEER","label":"审核","line":"TECHNICAL","order":2},
       {"step":"COUNTERSIGN","position":"MILITARY_REP","label":"会签(军代表)","line":"QUALITY","order":3},
       {"step":"APPROVE","position":"CHIEF_DESIGNER","label":"批准(总师)","line":"TECHNICAL","order":4},
       {"step":"APPROVE","position":"CHIEF_COMMANDER","label":"批准(总指挥)","line":"ADMINISTRATIVE","order":5}
     ]'::jsonb, FALSE)
ON CONFLICT DO NOTHING;
