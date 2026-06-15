-- ============================================================
-- 种子数据补全 v2 Batch 2 — 输入参考关联 (doc_input_reference)
-- 基于 GJB 标准库原文，为高频/核心文档建立"上游文档→引用标准→知识卡片"三维关联
-- ============================================================



-- ========================================
-- GP-10/ST-01 标准化大纲 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'ST-01';

    -- 上游输入文档: 研制总要求 + 研制方案
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总要求中提出的标准化要求是编制标准化大纲的根本依据', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案是标准化大纲编制的基础技术资料', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    -- 引用标准
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB/Z 114A-2005', '产品标准化大纲编制指南', '标准化大纲编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB/Z 113', '标准化评审', '标准化大纲编制后评审的规范', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB/Z 69', '军用标准的选用和剪裁导则', '标准选用和剪裁的方法指导', TRUE, 12),
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '图样技术文件更改管理需符合技术状态管理要求', TRUE, 13),
    (tmpl_id, 'STANDARD', '武器装备研制生产标准化工作规定', '武器装备研制生产标准化工作规定(科工法[2004]176号)', '标准化工作的法规依据', TRUE, 14);
END $$;

-- ========================================
-- GP-11/QA-01 质量保证大纲 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'QA-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '质量保证大纲需与研制总要求协调，获取质量目标和六性指标要求', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案确定技术路线，是设计过程质量控制章节的输入', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '特性分析报告是确定关键件/重要件的依据，用于5.12.6', FALSE, 3
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-23';

    -- 引用标准
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1406A-2005', '产品质量保证大纲要求', '质量保证大纲编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 9001C-2017', '质量管理体系要求', '质量管理的顶层标准', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '第6章技术状态管理的依据', TRUE, 12),
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '可靠性设计/试验的依据(5.9.4/5.10)', TRUE, 13),
    (tmpl_id, 'STANDARD', 'GJB 900A-2012', '装备安全性工作通用要求', '安全性设计的依据(5.9.7)', TRUE, 14),
    (tmpl_id, 'STANDARD', 'GJB 368B-2009', '装备维修性工作通用要求', '维修性设计/验证的依据(5.9.5)', TRUE, 15),
    (tmpl_id, 'STANDARD', 'GJB 3872A-2022', '装备综合保障通用要求', '保障性设计的依据(5.9.6)', TRUE, 16),
    (tmpl_id, 'STANDARD', 'GJB 571A-2005', '不合格品管理', '不合格品控制的依据(5.12.15)', TRUE, 17),
    (tmpl_id, 'STANDARD', 'GJB 907A-2006', '产品质量评审', '产品质量评审的依据(5.12.9)', TRUE, 18),
    (tmpl_id, 'STANDARD', 'GJB 908A-2008', '首件鉴定', '首件鉴定的依据(5.12.8)', TRUE, 19),
    (tmpl_id, 'STANDARD', 'GJB 909A-2005', '关键件和重要件的质量控制', '关键件重要件控制的依据(5.12.6)', TRUE, 20),
    (tmpl_id, 'STANDARD', 'GJB 1710A-2004', '试制和生产准备状态检查', '试制准备状态检查的依据(5.12.7)', TRUE, 21),
    (tmpl_id, 'STANDARD', 'GJB 726A-2004', '产品标识和可追溯性要求', '标识和可追溯性的依据(5.12.11)', TRUE, 22),
    (tmpl_id, 'STANDARD', 'GJB 1442A-2006', '检验工作要求', '监视和测量/过程检验的依据(5.12.14)', TRUE, 23),
    (tmpl_id, 'STANDARD', 'GJB 1269A-2000', '工艺评审', '工艺评审的依据(5.12.1)', TRUE, 24);
END $$;

-- ========================================
-- GP-16 研制方案 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总要求是研制方案战术技术指标的根本依据', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '可行性论证报告的技术方案是研制方案的参考', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-08';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '技术风险评估报告为方案提供风险约束', FALSE, 3
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-06';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 2993-1997', '武器装备研制项目管理', '研制方案编写的项目管理依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '方案中需规划技术状态管理策略', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '方案中需明确可靠性设计思路', TRUE, 12),
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '方案应对后续规范编制进行规划', FALSE, 13);
END $$;

-- ========================================
-- GP-18 接口控制文件 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-18';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '系统规范定义了系统级接口要求', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-12';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制规范定义了各技术状态项之间的接口关系', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-13';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 2737-1996', '武器装备系统接口控制要求', '接口控制文件编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '规范中第3.19条接口要求的依据', TRUE, 11);
END $$;

-- ========================================
-- GP-20 研制任务书 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-20';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总要求是下达研制任务书的根本依据', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制合同明确研制工作范围和交付物', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-09';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案确定的技术路线是任务书的技术基础', TRUE, 3
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 2993-1997', '武器装备研制项目管理', '研制任务书的管理依据', TRUE, 10);
END $$;

-- ========================================
-- GP-23 特性分析报告 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-23';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '详细设计是特性分析的基本输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-21';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '质量保证大纲中的特性分析要求(5.9.11)', FALSE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'QA-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 190-1986', '特性分析', '特性分析的直接依据，定义关键件/重要件判别准则', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 909A-2005', '关键件和重要件的质量控制', '特性分析后关键件/重要件的质量控制要求', TRUE, 11);
END $$;

-- ========================================
-- GP-25/26 研制试验大纲/报告 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-25';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案中的试验要求是试验大纲的技术输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '研制试验大纲编写的依据', TRUE, 10);
END $$;

DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-26';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制试验大纲是研制试验报告的执行依据', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-25';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '研制试验报告的编写依据', TRUE, 10);
END $$;

-- ========================================
-- GP-34 设计定型试验大纲 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-34';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总要求中的战术技术指标是考核的根本依据', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '设计定型试验大纲编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB/Z 170.5-2013', '军工产品设计定型文件编制指南 第5部分：设计定型基地试验大纲', '基地试验大纲编制指南', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 6177-2007', '军工产品设计定型部队试验大纲编制指南', '部队试验大纲编制依据', FALSE, 12);
END $$;

-- ========================================
-- GP-39 重大技术问题攻关报告 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-39';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '技术风险评估报告中识别的高风险技术是攻关的重点', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-06';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案中的关键技术清单', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB/Z 170.10-2013', '军工产品设计定型文件编制指南 第10部分：重大技术问题攻关报告', '重大技术问题攻关报告编写的直接依据', TRUE, 10);
END $$;

-- ========================================
-- RE-04 可靠性大纲(可靠性工作计划) 补充模板关联
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'RE-04';

    -- 上游文档: 可靠性要求 + 研制方案
    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '可靠性要求为可靠性工作计划提供定性定量要求输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'RE-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案确定的技术基线是可靠性设计分析的基础', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '可靠性大纲编写的直接依据(200/300/400/500系列工作项目)', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 451A-2005', '可靠性维修性保障性术语', '可靠性术语标准', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 841A-2024', '通用质量特性问题报告、分析和纠正措施系统', 'FRACAS系统的建立依据', TRUE, 12),
    (tmpl_id, 'STANDARD', 'GJB/Z 1391-2006', '故障模式、影响及危害性分析指南', '300系列FMECA工作项目的依据', TRUE, 13),
    (tmpl_id, 'STANDARD', 'GJB/Z 768A-1998', '故障树分析指南', '300系列FTA工作项目的依据', FALSE, 14);
END $$;

-- ========================================
-- SA-02 安全性大纲 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SA-02';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '安全性要求为安全性大纲提供工作项目要求', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'SA-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '初步危险分析(PHA)是安全性大纲中危险识别的输入', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'SA-03';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 900A-2012', '装备安全性工作通用要求', '安全性大纲编写的直接依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB/Z 1391-2006', '故障模式、影响及危害性分析指南', '危险识别和分析的工具参考', FALSE, 11);
END $$;

-- ========================================
-- MA-04 维修性大纲 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'MA-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '维修性要求为大纲提供指标要求', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'MA-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 368B-2009', '装备维修性工作通用要求', '维修性大纲编写的直接依据', TRUE, 10);
END $$;

-- ========================================
-- SW-06 软件研制任务书 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SW-06';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '系统/子系统规格说明(SSS)是软件研制任务书的系统级输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-02';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '运行方案说明(OCD)是任务书的用户需求来源', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 438B-2009', '军用软件开发文档通用要求', '软件研制任务书编写的依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 2786A-2009', '军用软件开发通用要求', '软件开发的顶层要求', TRUE, 11);
END $$;

-- ========================================
-- SW-07 软件开发计划 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SW-07';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '软件研制任务书是软件开发计划的输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-06';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '软件需求规格说明确定的工作量是计划编制的基础', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-13';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 438B-2009', '军用软件开发文档通用要求', '软件开发计划编写的依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 2786A-2009', '军用软件开发通用要求', '软件开发生命周期活动的总体要求', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 5000A-2008', '军用软件研制能力成熟度模型', '软件过程管理的参考', FALSE, 12);
END $$;

-- ========================================
-- GP-14 研制计划 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-14';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制合同明确交付物和进度安排', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-09';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案确定技术路线和阶段目标', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 2993-1997', '武器装备研制项目管理', '研制计划编写的直接依据，规定阶段划分和要求', TRUE, 10);
END $$;

-- ========================================
-- GP-22 设计计算报告 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-22';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '详细设计是设计计算的基础', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-21';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '可靠性预计(工作项目303)需要设计计算报告', FALSE, 10);
END $$;

-- ========================================
-- SU-02 综合保障计划 的输入参考
-- ========================================
DO $$
DECLARE
    tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SU-02';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '保障性要求为综合保障计划提供输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'SU-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 3872A-2022', '装备综合保障通用要求', '综合保障计划编写的直接依据', TRUE, 10);
END $$;

