-- ============================================================
-- 四环节串联种子数据
-- 1. 更新 stage_doc_checklist_template 的 template_id 和 spec_type
-- 2. 创建 doc_input_reference 输入参考关联
-- ============================================================

-- ============================================================
-- Part 1: 更新清单模板 → 写作模板映射 + 规范类型
-- ============================================================

-- 系统规范(A类)
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-A-SPEC'),
    spec_type = 'SYSTEM_SPEC'
WHERE doc_code = 'GP-12';

-- 研制规范(B类)
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-B-SPEC'),
    spec_type = 'DEV_SPEC'
WHERE doc_code = 'GP-13';

-- 产品规范(C类)
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-C-SPEC'),
    spec_type = 'PRODUCT_SPEC'
WHERE doc_code = 'GP-29';

-- 工艺规范(D类)
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-D-SPEC'),
    spec_type = 'PROCESS_SPEC'
WHERE doc_code = 'PR-02';

-- 材料规范(E类)
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-E-SPEC'),
    spec_type = 'MATERIAL_SPEC'
WHERE doc_code = 'PR-03';

-- 软件系统规格说明
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SW-SSS'),
    spec_type = 'SOFTWARE_SPEC'
WHERE doc_code = 'SW-02';

-- 软件需求规格说明
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SW-REQ-SPEC'),
    spec_type = 'SOFTWARE_SPEC'
WHERE doc_code = 'SW-13';

-- 软件产品规格说明
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SW-PROD-SPEC'),
    spec_type = 'SOFTWARE_SPEC'
WHERE doc_code = 'SW-18';

-- 研制总结
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-DEV-SUMMARY')
WHERE doc_code = 'GP-44';

-- 标准化大纲
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-STANDARD-OUTLINE')
WHERE doc_code = 'ST-01';

-- 质量保证大纲
UPDATE stage_doc_checklist_template SET
    template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-QUALITY-PLAN')
WHERE doc_code = 'QA-01';

-- ============================================================
-- Part 2: doc_input_reference 输入参考关联数据
-- 为每个关键文档建立"上游文档 + 引用标准 + 知识卡片"的三维关联
-- ============================================================

-- 辅助函数：根据doc_code获取checklist template id
-- 使用DO块逐条插入

-- ========================================
-- GP-12 系统规范(A类) 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-12';

    -- 上游输入文档
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '系统规范的编制需依据立项论证报告确定的作战使命任务和初步总体方案', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总要求是系统规范中战术技术指标的根本依据，必须与之协调一致', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '可行性论证报告的技术方案作为系统规范第3章要求的参考输入', FALSE, 3
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-08';

    -- 引用标准
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '系统规范编写的根本依据，定义34要素结构', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '系统规范建立功能基线(FBL)，变更走正式更改控制流程', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 0.2-2001', '军用规范编写规定', '规范格式和编排要求', TRUE, 12),
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '第3.5条可靠性要求的编制依据', TRUE, 13),
    (tmpl_id, 'STANDARD', 'GJB 900A-2012', '装备安全性工作通用要求', '第3.10条安全性要求的编制依据', TRUE, 14),
    (tmpl_id, 'STANDARD', 'GJB 151B-2013', '军用设备和分系统电磁发射和敏感度要求', '第3.13条电磁兼容性要求的依据', FALSE, 15),
    (tmpl_id, 'STANDARD', 'GJB 1389A-2005', '系统电磁兼容性要求', '第3.13条系统级电磁兼容要求的依据', FALSE, 16),
    (tmpl_id, 'STANDARD', 'GJB 3872-1999', '装备综合保障通用要求', '第3.18条综合保障要求的编制依据', TRUE, 17),
    (tmpl_id, 'STANDARD', 'GJB 2873-1997', '军事装备和设施的人机工程设计准则', '第3.15条人机工程要求的编制依据', FALSE, 18);
END $$;

-- ========================================
-- GP-13 研制规范(B类) 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-13';

    -- 上游输入文档
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '系统规范是研制规范的顶层输入，研制规范需将系统要求分配到各技术状态项目', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-12';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总要求中的技术指标是研制规范性能要求的根本依据', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案确定的技术路线和总体设计是研制规范的技术基础', TRUE, 3
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    -- 引用标准
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '研制规范编写的根本依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '研制规范建立分配基线(ABL)', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 1909-1994', '装备可靠性维修性参数选择和指标确定要求', '第3.5/3.6条可靠性维修性参数的选择依据', TRUE, 12),
    (tmpl_id, 'STANDARD', 'GJB 2737-1996', '武器装备系统接口控制要求', '第3.19条接口要求的编制依据', TRUE, 13);
END $$;

-- ========================================
-- GP-29 产品规范(C类) 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-29';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制规范是产品规范的上游输入，产品规范需落实分配基线要求', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-13';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '详细设计是产品规范编制的基础技术资料', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-21';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '产品规范编写的根本依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '产品规范建立产品基线(PBL)', TRUE, 11);
END $$;

-- ========================================
-- GP-44 研制总结 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-44';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总要求是研制总结中指标符合性对照的根本依据', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案是研制总结中技术方案实施情况的参考', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '设计定型试验报告是研制总结中试验结论的输入', TRUE, 3
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-37';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '重大技术问题攻关报告是关键技术攻关情况的输入', TRUE, 4
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-39';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '质量分析报告是质量问题归零情况的输入', FALSE, 5
    FROM stage_doc_checklist_template WHERE doc_code = 'QA-02';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB/Z 170.4-2013', '军工产品设计定型文件编制指南 第4部分：研制总结', '研制总结编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '设计定型程序的总体要求', TRUE, 11);
END $$;

-- ========================================
-- GP-04 研制总要求 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '立项综合论证报告是研制总要求的论证基础', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '可行性论证报告为研制总要求提供技术可行性支撑', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-08';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', '中国人民解放军装备条例', '中国人民解放军装备条例', '装备研制立项和研制总要求两报两批制度的法规依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 4054-2000', '武器装备论证规范', '装备论证工作的标准依据', TRUE, 11);
END $$;

-- ========================================
-- SW-02 系统/子系统规格说明(SSS) 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SW-02';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '系统规范是软件系统规格说明的顶层输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-12';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '运行方案说明(OCD)是系统规格说明的用户需求输入', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 438B-2009', '军用软件开发文档通用要求', '软件系统规格说明编写的根本依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '附录B对软件规范各章要素的规定', TRUE, 11);
END $$;

-- ========================================
-- GP-17 技术状态管理计划 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-17';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '系统规范定义了功能基线，技术状态管理计划需覆盖该基线的管理', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-12';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '技术状态管理计划编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '三类基线(功能/分配/产品)定义和建立时机', TRUE, 11);
END $$;

-- ========================================
-- RE-04 可靠性工作计划(可靠性大纲) 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'RE-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '可靠性要求为可靠性工作计划提供定性定量要求输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'RE-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '可靠性大纲编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 1909-1994', '装备可靠性维修性参数选择和指标确定要求', '可靠性参数选择和指标确定的依据', TRUE, 11);
END $$;

-- ========================================
-- QA-01 质量保证大纲 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'QA-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '质量保证大纲需与研制方案和研制总要求协调', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1406A-2005', '产品质量保证大纲要求', '质量保证大纲编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 9001C-2017', '质量管理体系要求', '质量管理的顶层标准', TRUE, 11);
END $$;
