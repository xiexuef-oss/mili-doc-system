-- ============================================================
-- 种子数据补全 v2 — 基于 GJB 标准库原文提取
-- 补全内容:
--   Part A: 模板定义(batch 2) — 6个关键模板
--   Part B: 模板章节结构 — 质量保证大纲/标准化大纲/软件需求规格说明/研制方案/技术状态管理计划/可靠性大纲
--   Part C: 输入参考关联 — 扩展至23个高频文档
--   Part D: 知识卡片补充
-- ============================================================


-- Part A: 补充模板定义 (doc_template_v2)

INSERT INTO doc_template_v2(category_id, template_code, template_name, template_type, applicable_stage_codes, gjb_standard_ref, document_class, variables_schema, status) VALUES

-- B类规范 研制规范
((SELECT id FROM doc_template_category WHERE category_code = 'B_SPEC'),
 'TPL-B-SPEC', '研制规范(B类)', 'B_SPEC',
 'F,C', 'GJB 6387-2008', 'B_SPEC',
 '{"projectName":{"type":"string","label":"项目名称","required":true}}', 'ACTIVE'),

-- D类规范 工艺规范
((SELECT id FROM doc_template_category WHERE category_code = 'D_SPEC'),
 'TPL-D-SPEC', '工艺规范(D类)', 'D_SPEC',
 'C', 'GJB 6387-2008', 'D_SPEC',
 '{"productName":{"type":"string","label":"产品名称","required":true}}', 'ACTIVE'),

-- E类规范 材料规范
((SELECT id FROM doc_template_category WHERE category_code = 'E_SPEC'),
 'TPL-E-SPEC', '材料规范(E类)', 'E_SPEC',
 'C', 'GJB 6387-2008', 'E_SPEC',
 '{"materialName":{"type":"string","label":"材料名称","required":true}}', 'ACTIVE'),

-- 软件系统规格说明
((SELECT id FROM doc_template_category WHERE category_code = 'SOFTWARE'),
 'TPL-SW-SSS', '系统/子系统规格说明(SSS)', 'SOFTWARE',
 'F', 'GJB 438B-2009', 'SOFTWARE',
 '{"systemName":{"type":"string","label":"系统名称","required":true}}', 'ACTIVE'),

-- 软件产品规格说明
((SELECT id FROM doc_template_category WHERE category_code = 'SOFTWARE'),
 'TPL-SW-PROD-SPEC', '软件产品规格说明(SPS)', 'SOFTWARE',
 'C,S', 'GJB 438B-2009', 'SOFTWARE',
 '{"softwareName":{"type":"string","label":"软件名称","required":true}}', 'ACTIVE'),

-- 研制方案模板
((SELECT id FROM doc_template_category WHERE category_code = 'MANAGEMENT'),
 'TPL-DEV-PLAN', '研制方案', 'MANAGEMENT',
 'F', 'GJB 2993-1997', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称","required":true}}', 'ACTIVE');


-- ========================================
-- Part B1: 质量保证大纲(TPL-QUALITY-PLAN) 章节结构 [GJB 1406A-2005 第5章]
-- ========================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT;
    ch6 BIGINT; ch7 BIGINT; ch8 BIGINT; ch9 BIGINT; ch10 BIGINT;
    ch11 BIGINT; ch12 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-QUALITY-PLAN';

    -- 第1章 范围
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 1406A 5.1', '大纲的适用范围：适用的产品或特殊限制、合同范围、研制或生产阶段')
    RETURNING id INTO ch1;

    -- 第2章 质量工作原则与质量目标
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '2', '质量工作原则与质量目标', 1, 20, TRUE, 'GJB 1406A 5.2', '制定质量工作总原则和质量目标，包括可靠性/维修性/保障性/安全性/测试性指标',
     '总原则应有针对性，不是抄标准模板。质量目标尽可能量化，如"关键过程合格率≥98%"、"质量问题归零率100%"')
    RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch2, '2.1', '质量工作总原则', 2, 21, TRUE, 'GJB 1406A 5.2.1', '产品技术应用借鉴程度、新技术采用比例、技术状态管理要求、设计的可制造性');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch2, '2.2', '质量目标', 2, 22, TRUE, 'GJB 1406A 5.2.2', '对产品合同质量特性满意程度、六性指标、顾客满意内容');

    -- 第3章 管理职责
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '管理职责', 1, 30, TRUE, 'GJB 1406A 5.3', '各级各类人员职责/权限/相互关系、职能部门质量职责和接口关系');

    -- 第4章 文件和记录控制
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '文件和记录控制', 1, 40, TRUE, 'GJB 1406A 5.4', '研制生产全过程文件和记录控制规定，成套资料应符合GJB 906')
    RETURNING id INTO ch4;

    -- 第5章 质量信息管理
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '5', '质量信息管理', 1, 50, TRUE, 'GJB 1406A 5.5', '质量信息的收集/分析/处理/反馈/贮存/报告，质量问题归零要求',
     '必须明确质量信息传递的渠道和责任人，质量问题归零执行"双五条"标准')
    RETURNING id INTO ch5;

    -- 第6章 技术状态管理
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '技术状态管理', 1, 60, TRUE, 'GJB 1406A 5.6', '按GJB 3206策划和实施技术状态管理：标识/控制/记实/审核')
    RETURNING id INTO ch6;

    -- 第7章 人员培训和资格考核
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '人员培训和资格考核', 1, 70, TRUE, 'GJB 1406A 5.7', '研制/生产/试验人员培训与资格考核要求，特殊工艺人员重新考核要求');

    -- 第8章 顾客沟通
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '8', '顾客沟通', 1, 80, TRUE, 'GJB 1406A 5.8', '与顾客沟通的内容和方法：产品信息/合同处理/顾客反馈');

    -- 第9章 设计过程质量控制 (核心大章，含16个子节)
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '9', '设计过程质量控制', 1, 90, TRUE, 'GJB 1406A 5.9', '从任务分析到设计更改控制的完整设计质量控制过程')
    RETURNING id INTO ch9;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.1', '任务分析', 2, 91, TRUE, 'GJB 1406A 5.9.1', '任务剖面分析，确定影响设计的任务阶段和综合环境，确定六性定量定性因素');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.2', '设计分析', 2, 92, TRUE, 'GJB 1406A 5.9.2', '三化设计原则，性能/质量/可靠性/费用/进度/风险综合权衡，优化设计');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.3', '设计输入', 2, 93, TRUE, 'GJB 1406A 5.9.3', '确定设计输入要求(功能/性能/六性/环境等)，形成文件并评审批准');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.4', '可靠性设计', 2, 94, TRUE, 'GJB 1406A 5.9.4', '按可靠性大纲实施可靠性设计工作项目');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.5', '维修性设计', 2, 95, TRUE, 'GJB 1406A 5.9.5', '按维修性大纲实施维修性设计工作项目');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.6', '保障性设计', 2, 96, TRUE, 'GJB 1406A 5.9.6', '按保障性大纲实施保障性设计工作项目');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.7', '安全性设计', 2, 97, TRUE, 'GJB 1406A 5.9.7', '按安全性大纲实施安全性设计工作项目');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.8', '元器件/零件/原材料的选择和使用', 2, 98, TRUE, 'GJB 1406A 5.9.8', '按GJB 450工作项目308/309选择和控制，降额设计按GJB/Z35');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.9', '软件设计', 2, 99, TRUE, 'GJB 1406A 5.9.9', '按GJB 437/438/439/2786/GJB 5000实施软件工程化管理');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.10', '人机工程设计', 2, 100, TRUE, 'GJB 1406A 5.9.10', '编制人机工程大纲，确保操作人员正常准确操作');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.11', '特性分析', 2, 101, TRUE, 'GJB 1406A 5.9.11 / GJB 190', '按GJB 190进行特性分析，确定关键件(特性)和重要件(特性)');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.12', '设计输出', 2, 102, TRUE, 'GJB 1406A 5.9.12', '满足设计输入/包含验收准则/采购生产信息/关键设计特性标识');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.13', '设计评审', 2, 103, TRUE, 'GJB 1406A 5.9.13 / GJB 1310A', '按GJB 1310分级分阶段设计评审，合同要求时顾客或其代表参加');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.14', '设计验证', 2, 104, TRUE, 'GJB 1406A 5.9.14', '设计验证项目及方法，转阶段和靶场试验前的复核复算等验证工作');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.15', '设计确认/定型(鉴定)', 2, 105, TRUE, 'GJB 1406A 5.9.15', '确认内容/方式/条件/确认点，需定型产品按GJB 1362完成定型');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch9, '9.16', '设计更改控制', 2, 106, TRUE, 'GJB 1406A 5.9.16', '按GJB 3206实施设计更改控制的要求');

    -- 第10章 试验控制
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '10', '试验控制', 1, 110, TRUE, 'GJB 1406A 5.10', '试验综合计划(含全部研制/生产/交付试验)，可靠性增长/鉴定/ESS/维修性验证试验')
    RETURNING id INTO ch10;

    -- 第11章 采购质量控制
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '11', '采购质量控制', 1, 120, TRUE, 'GJB 1406A 5.11', '采购品控制(GJB 939/GJB 1404/GJB/Z2)和外包过程控制')
    RETURNING id INTO ch11;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch11, '11.1', '采购品的控制', 2, 121, TRUE, 'GJB 1406A 5.11.1', '采购文件/供方评价/验证程序/新研制产品采购/供方确认');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch11, '11.2', '外包过程的控制', 2, 122, TRUE, 'GJB 1406A 5.11.2', '设计外包/试制外包/试验外包/生产外包的控制措施和验收准则');

    -- 第12章 试制和生产过程质量控制
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '12', '试制和生产过程质量控制', 1, 130, TRUE, 'GJB 1406A 5.12', '涵盖工艺准备到售后服务的全流程质量控制',
     '这是大纲最长的章节，共16个子节。所有子节的质量控制要点都来自具体GJB标准，引用标准编号不要漏')
    RETURNING id INTO ch12;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.1', '工艺准备', 2, 131, TRUE, 'GJB 1406A 5.12.1', '工艺总方案/特种工艺文件/关键过程标识/工艺更改控制/工艺评审(GJB 1269)');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.2', '元器件/零件和原材料控制', 2, 132, TRUE, 'GJB 1406A 5.12.2', '合格器材投产/外购复验/代用料审批/易老化品管理/元器件筛选');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.3', '基础设施和工作环境', 2, 133, TRUE, 'GJB 1406A 5.12.3', '特殊基础设施/工作环境要求(如洁净室/生物危害防护)');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.4', '关键过程控制', 2, 134, TRUE, 'GJB 1406A 5.12.4', '按工艺文件或专用质量控制程序对关键过程实施控制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.5', '特殊过程控制', 2, 135, TRUE, 'GJB 1406A 5.12.5', '过程评审批准/设备认可/人员资格鉴定/再确认');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.6', '关键件/重要件的控制', 2, 136, TRUE, 'GJB 1406A 5.12.6 / GJB 909', '按GJB 909对关键件和重要件实施质量控制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.7', '试制/生产准备状态检查', 2, 137, TRUE, 'GJB 1406A 5.12.7 / GJB 1710', '按GJB 1710进行试制和生产准备状态检查');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.8', '首件鉴定', 2, 138, TRUE, 'GJB 1406A 5.12.8 / GJB 908', '按GJB 908对首件进行鉴定');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.9', '产品质量评审', 2, 139, TRUE, 'GJB 1406A 5.12.9 / GJB 907', '按GJB 907进行产品质量评审，问题处理及记录保存');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.10', '装配质量控制', 2, 140, TRUE, 'GJB 1406A 5.12.10', '编写装配规程或作业指导书');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.11', '标识和可追溯性', 2, 141, TRUE, 'GJB 1406A 5.12.11 / GJB 726 / GJB 1330', '产品标识/批次管理(GJB 1330)/可追溯性控制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.12', '顾客财产', 2, 142, TRUE, 'GJB 1406A 5.12.12', '顾客提供产品的验证/不合格处置/保护维护/损坏丢失记录');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.13', '产品防护', 2, 143, TRUE, 'GJB 1406A 5.12.13 / GJB 1443', '搬运/贮存/包装/防护/交付控制，按GJB 1443');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.14', '监视和测量', 2, 144, TRUE, 'GJB 1406A 5.12.14', '过程检验/验收试验和检验/例行试验/无损检验(GJB 466/GJB 593)/试验和检验记录');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.15', '不合格品控制', 2, 145, TRUE, 'GJB 1406A 5.12.15 / GJB 571', '按GJB 571标识/评价/隔离/处置和记录不合格品');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch12, '12.16', '售后服务', 2, 146, FALSE, 'GJB 1406A 5.12.16', '合同要求时组织技术服务，指导安装调试使用维护');
END $$;



-- ========================================
-- Part B2: 标准化大纲(TPL-STANDARD-OUTLINE) 章节结构 [GJB/Z 114A-2005 第5章]
-- ========================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT;
    ch6 BIGINT; ch7 BIGINT; ch8 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-STANDARD-OUTLINE';

    -- 第1章 概述
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '1', '概述', 1, 10, TRUE, 'GJB/Z 114A 5.1', '编制依据、适用范围，概略描述研制产品基本情况',
     '包括任务来源/产品用途/主要性能/研制类型和特点/产品组成/标准化要求/配套情况')
    RETURNING id INTO ch1;

    -- 第2章 标准化目标
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '2', '标准化目标', 1, 20, TRUE, 'GJB/Z 114A 5.2', '标准化目标：水平目标/效果目标/任务目标',
     '水平目标如标准化系数、"三化"程度；效果目标如节约经费、缩短周期；任务目标如标准数量')
    RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch2, '2.1', '水平目标', 2, 21, TRUE, 'GJB/Z 114A 5.2.2', '产品标准化期望达到的水平：标准实施水平/标准化系数/通用化-系列化-组合化程度');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch2, '2.2', '效果目标', 2, 22, TRUE, 'GJB/Z 114A 5.2.2', '预期的军事/技术/经济效果：装备效能提升/研制经费节约/周期缩短');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch2, '2.3', '任务目标', 2, 23, TRUE, 'GJB/Z 114A 5.2.2', '计划制定的型号标准化文件数量及贯彻实施标准任务');

    -- 第3章 标准实施要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '标准实施要求', 1, 30, TRUE, 'GJB/Z 114A 5.3', '包括一般要求/重大标准实施/标准选用范围/标准件元器件原材料选用范围')
    RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '一般要求', 2, 31, TRUE, 'GJB/Z 114A 5.3.2', '贯彻实施标准的原则/程序/审批/时效性/监督/问题处理');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', '重大标准实施要求', 2, 32, TRUE, 'GJB/Z 114A 5.3.3', '涉及面宽/投资大/协调复杂/影响指标/安全相关/"三化"重大影响标准的实施方案');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.3', '标准选用范围', 2, 33, TRUE, 'GJB/Z 114A 5.3.4', '产品研制过程中设计人员选用标准的推荐性目录，实施动态管理');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.4', '标准件/元器件/原材料选用范围', 2, 34, TRUE, 'GJB/Z 114A 5.3.5', '品种规格压缩/"三化"水平提升/动态管理/限用品种管控');

    -- 第4章 通用化/系列化/组合化("三化")要求和接口/互换性要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '通用化/系列化/组合化("三化")要求及接口/互换性要求', 1, 40, TRUE, 'GJB/Z 114A 5.4', '"三化"设计要求(含不同层次产品重点)+接口标准+互换性标准')
    RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '通用化/系列化/组合化设计要求', 2, 41, TRUE, 'GJB/Z 114A 5.4.1', '采用下级通用设备/纳入系列型谱/"三化"数据库应用/"三化"评审');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '接口/互换性要求', 2, 42, TRUE, 'GJB/Z 114A 5.4.2', '机械接口/电气接口/软件接口/信息格式/人机界面接口标准及互换性标准');

    -- 第5章 型号标准化文件体系
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '型号标准化文件体系要求', 1, 50, TRUE, 'GJB/Z 114A 5.5', '型号标准化文件体系表(完整性/动态性/协调性)及文件项目表')
    RETURNING id INTO ch5;

    -- 第6章 图样和技术文件要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '图样和技术文件要求', 1, 60, TRUE, 'GJB/Z 114A 5.6', '完整性/正确性/统一性要求 + 管理要求(借用件/更改/审批会签)')
    RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.1', '完整性/正确性/统一性要求', 2, 61, TRUE, 'GJB/Z 114A 5.6.2', '图样成套性项目表/正确性和协调性/CAD统一编号/统编格式');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.2', '图样和技术文件管理要求', 2, 62, TRUE, 'GJB/Z 114A 5.6.3', '借用件管理/更改管理(按技术状态管理标准)/审批会签');

    -- 第7章 标准化工作范围和研制各阶段主要工作
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '标准化工作范围和研制各阶段主要工作', 1, 70, TRUE, 'GJB/Z 114A 5.7', '工作方案→论证→工程研制→设计定型各阶段标准化工作内容(参见GJB/Z 114A表1)');

    -- 第8章 标准化工作协调要求
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '8', '标准化工作协调管理要求', 1, 80, TRUE, 'GJB/Z 114A 5.8', '协调原则/文件协调程序和传递路线/审批会签范围权限/更改传递');
END $$;



-- ========================================
-- Part B3: 软件需求规格说明(TPL-SW-REQ-SPEC) 章节结构 [GJB 438B-2009]
-- ========================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-SW-REQ-SPEC';

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 438B', '标识要生产的软件产品，说明软件需求规格说明的目的和适用范围')
    RETURNING id INTO ch1;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.1', '标识', 2, 11, TRUE, 'GJB 438B', '软件标识号/标题/缩略语/版本号');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.2', '系统概述', 2, 12, TRUE, 'GJB 438B', '软件应用系统的简要描述');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.3', '文档概述', 2, 13, TRUE, 'GJB 438B', '文档的用途和内容摘要');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 438B', '列出引用的所有标准/规范/文件');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '需求', 1, 30, TRUE, 'GJB 438B', '全面规定CSCI必须满足的各项要求，是SRS的核心章节')
    RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '要求的状态和方式', 2, 31, TRUE, 'GJB 438B', '若CSCI以多种状态/方式运行，对每种状态/方式的要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', 'CSCI能力需求', 2, 32, TRUE, 'GJB 438B', 'CSCI应具备的每一项能力(功能)的详细描述');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.3', 'CSCI外部接口需求', 2, 33, TRUE, 'GJB 438B', '与其他CSCI/HWCI/系统的接口名称/标识/描述');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.4', 'CSCI内部接口需求', 2, 34, FALSE, 'GJB 438B', 'CSCI内部各组成单元之间的接口');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.5', 'CSCI内部数据需求', 2, 35, FALSE, 'GJB 438B', '数据库/数据文件/全局数据结构');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.6', '适应性需求', 2, 36, FALSE, 'GJB 438B', '目标计算机/操作系统的适应性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.7', '安全性需求', 2, 37, TRUE, 'GJB 438B', '保密性/完整性/可用性/抗抵赖性');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.8', '保密性需求', 2, 38, FALSE, 'GJB 438B', '信息保密要求/访问控制/加解密');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.9', 'CSCI环境需求', 2, 39, TRUE, 'GJB 438B', '运行所需的硬件/软件/通信环境');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.10', '计算机资源需求', 2, 40, TRUE, 'GJB 438B', 'CPU/内存/存储/网络带宽/外设');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.11', '软件质量因素', 2, 41, TRUE, 'GJB 438B', '可靠性/可用性/可维护性/可移植性/可扩展性/可测试性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.12', '设计和实现约束', 2, 42, FALSE, 'GJB 438B', '编程语言/编码标准/设计约束/数据库约束');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.13', '人员需求', 2, 43, FALSE, 'GJB 438B', '操作/维护人员技能/编制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.14', '培训需求', 2, 44, FALSE, 'GJB 438B', '使用/维护培训要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.15', '后勤保障需求', 2, 45, FALSE, 'GJB 438B', '部署/现场支持/升级维护');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.16', '其他需求', 2, 46, FALSE, 'GJB 438B', '以上未覆盖的其他需求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.17', '需求优先次序和关键程度', 2, 47, TRUE, 'GJB 438B', '按重要性和关键程度排列各项需求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '合格性需求', 1, 40, TRUE, 'GJB 438B', '对第3章各项需求的验证方法/验证环境/验证级别')
    RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '需求可追踪性', 1, 50, TRUE, 'GJB 438B', '每条需求与系统需求/上级文档的双向追溯关系');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '注解', 1, 60, FALSE, 'GJB 438B', '任何有助于理解本文档的补充信息');
END $$;

