-- ============================================================
-- Phase 0: 模板结构种子数据
-- 基于 GJB 6387-2008, GJB/Z 170.4-2013, GJB 0.2, GJB 2993-1997
-- ============================================================

-- ============================================================
-- 1. 模板分类 (6大类)
-- ============================================================
INSERT INTO doc_template_category(category_code, category_name, parent_id, gjb_reference, description, order_num) VALUES
('A_SPEC',     'A类规范(系统规范)',       0, 'GJB 6387-2008 4.1', '针对整个武器系统，规定系统级要求、接口要求和验证要求', 1),
('B_SPEC',     'B类规范(研制规范)',       0, 'GJB 6387-2008 4.2', '针对系统以下级产品，规定功能、性能和验证要求', 2),
('C_SPEC',     'C类规范(产品规范)',       0, 'GJB 6387-2008 4.3', '针对最终交付产品，规定产品特性和接收标准', 3),
('D_SPEC',     'D类规范(工艺规范)',       0, 'GJB 6387-2008 4.4', '针对制造工艺，规定工艺参数和工艺控制要求', 4),
('E_SPEC',     'E类规范(材料规范)',       0, 'GJB 6387-2008 4.5', '针对原材料或半成品，规定材料性能和检验方法', 5),
('SOFTWARE',   '软件文档',               0, 'GJB 438B-2009',    '军用软件开发和保障文档', 6),
('MANAGEMENT', '管理文件',               0, 'GJB/Z 170-2013',   '定型文件、评审文件、质量文件等管理类文档', 7);

-- ============================================================
-- 2. 核心模板定义
-- ============================================================
INSERT INTO doc_template_v2(category_id, template_code, template_name, template_type, applicable_stage_codes, gjb_standard_ref, document_class, variables_schema, status) VALUES
-- 研制总结 (管理文件)
((SELECT id FROM doc_template_category WHERE category_code = 'MANAGEMENT'),
 'TPL-DEV-SUMMARY', '研制总结', 'MANAGEMENT',
 'S', 'GJB/Z 170.4-2013', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称","required":true,"sourceField":"project.name"},"equipmentModel":{"type":"string","label":"装备型号","required":true,"sourceField":"master_data.equipment_info.model"},"contractNo":{"type":"string","label":"合同编号","required":false,"sourceField":"input_file.contract"},"taskBookNo":{"type":"string","label":"任务书编号","required":true,"sourceField":"input_file.task_book"}}',
 'ACTIVE'),

-- 标准化大纲 (管理文件)
((SELECT id FROM doc_template_category WHERE category_code = 'MANAGEMENT'),
 'TPL-STANDARD-OUTLINE', '产品标准化大纲', 'MANAGEMENT',
 'F,C', 'GJB/Z 114A-2005', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称","required":true},"equipmentType":{"type":"string","label":"装备类型","required":true}}',
 'ACTIVE'),

-- 质量保证大纲 (管理文件)
((SELECT id FROM doc_template_category WHERE category_code = 'MANAGEMENT'),
 'TPL-QUALITY-PLAN', '产品质量保证大纲', 'MANAGEMENT',
 'F,C', 'GJB 1406A-2005', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称","required":true}}',
 'ACTIVE'),

-- 软件需求规格说明 (软件文档)
((SELECT id FROM doc_template_category WHERE category_code = 'SOFTWARE'),
 'TPL-SW-REQ-SPEC', '软件需求规格说明', 'SOFTWARE',
 'F,C', 'GJB 438B-2009', 'SOFTWARE',
 '{"softwareName":{"type":"string","label":"软件名称","required":true},"softwareLevel":{"type":"string","label":"软件等级","required":true}}',
 'ACTIVE'),

-- 系统规范 (A类规范)
((SELECT id FROM doc_template_category WHERE category_code = 'A_SPEC'),
 'TPL-A-SPEC', '系统规范(A类)', 'A_SPEC',
 'F', 'GJB 6387-2008', 'A_SPEC',
 '{"systemName":{"type":"string","label":"系统名称","required":true}}',
 'ACTIVE'),

-- 产品规范 (C类规范)
((SELECT id FROM doc_template_category WHERE category_code = 'C_SPEC'),
 'TPL-C-SPEC', '产品规范(C类)', 'C_SPEC',
 'F,C', 'GJB 6387-2008', 'C_SPEC',
 '{"productName":{"type":"string","label":"产品名称","required":true}}',
 'ACTIVE');

-- ============================================================
-- 3. 研制总结 章节结构 (GJB/Z 170.4-2013, 共12章)
-- ============================================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT;
    ch6 BIGINT; ch7 BIGINT; ch8 BIGINT; ch9 BIGINT; ch10 BIGINT;
    ch11 BIGINT; ch12 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-DEV-SUMMARY';

    -- 第1章 研制任务概述
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '1', '研制任务概述', 1, 10, TRUE, 'GJB/Z 170.4 5.1', '说明项目研制背景、任务来源、研制目标', '简要概括项目立项依据和研制任务书的主要内容')
    RETURNING id INTO ch1;

    -- 1.1 研制依据
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch1, '1.1', '研制依据', 2, 11, TRUE, 'GJB/Z 170.4 5.1.1', '列出研制任务书、合同、研制总要求等依据文件', '逐项列出所有研制依据文件，注明文件编号和批准日期');

    -- 1.2 研制任务与目标
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch1, '1.2', '研制任务与目标', 2, 12, TRUE, 'GJB/Z 170.4 5.1.2', '明确研制任务内容和预期达到的目标', '对照研制总要求逐条说明任务与目标');

    -- 1.3 产品用途与组成
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch1, '1.3', '产品用途与组成', 2, 13, TRUE, 'GJB/Z 170.4 5.1.3', '说明产品的军事用途和系统组成', '列出产品组成框图，说明各组成部分的功能');

    -- 第2章 研制过程概述
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '2', '研制过程概述', 1, 20, TRUE, 'GJB/Z 170.4 5.2', '概述从方案到设计定型的全部研制活动', '按时间线叙述，突出关键节点和重大决策')
    RETURNING id INTO ch2;

    -- 2.1 方案阶段
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch2, '2.1', '方案阶段工作', 2, 21, TRUE, 'GJB/Z 170.4 5.2.1', '方案阶段的主要工作和成果', '包括方案设计、方案评审、关键技术攻关等');

    -- 2.2 工程研制阶段
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch2, '2.2', '工程研制阶段工作', 2, 22, TRUE, 'GJB/Z 170.4 5.2.2', '工程研制阶段的主要工作和成果', '包括详细设计、试制、试验验证等');

    -- 2.3 设计定型阶段
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, ch2, '2.3', '设计定型阶段工作', 2, 23, TRUE, 'GJB/Z 170.4 5.2.3', '设计定型阶段的主要工作和成果', '包括定型试验、定型审查等');

    -- 第3章 战术技术指标符合性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips, content_schema) VALUES
    (tpl_id, 0, '3', '战术技术指标符合性', 1, 30, TRUE, 'GJB/Z 170.4 5.3', '逐项对照研制总要求中的战术技术指标，说明达到情况', '用表格形式逐项列出指标要求值、实测值、判定结论',
     '[{"fieldName":"indicatorName","fieldLabel":"指标名称","fieldType":"text","required":true,"placeholder":"如：最大射程"},{"fieldName":"requirementValue","fieldLabel":"要求值","fieldType":"text","required":true,"placeholder":"研制总要求中的指标值"},{"fieldName":"measuredValue","fieldLabel":"实测值","fieldType":"text","required":true,"placeholder":"试验实测值"},{"fieldName":"testMethod","fieldLabel":"试验方法","fieldType":"text","required":true,"placeholder":"验证方法/标准"},{"fieldName":"conclusion","fieldLabel":"符合性结论","fieldType":"select","required":true,"options":["符合","基本符合","不符合"]},{"fieldName":"remark","fieldLabel":"备注","fieldType":"text","required":false}]')
    RETURNING id INTO ch3;

    -- 第4章 关键技术攻关
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips, content_schema) VALUES
    (tpl_id, 0, '4', '关键技术攻关情况', 1, 40, TRUE, 'GJB/Z 170.4 5.4', '列出研制中遇到的重大技术问题及解决情况', '每项技术问题单独叙述，包括问题描述、攻关过程、解决效果',
     '[{"fieldName":"problemName","fieldLabel":"技术问题名称","fieldType":"text","required":true},{"fieldName":"problemDesc","fieldLabel":"问题描述","fieldType":"richtext","required":true},{"fieldName":"solution","fieldLabel":"解决措施","fieldType":"richtext","required":true},{"fieldName":"result","fieldLabel":"解决效果","fieldType":"richtext","required":true},{"fieldName":"verification","fieldLabel":"验证情况","fieldType":"richtext","required":true}]')
    RETURNING id INTO ch4;

    -- 第5章 产品质量状况
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '5', '产品质量状况', 1, 50, TRUE, 'GJB/Z 170.4 5.5', '概述产品研制过程中的质量状况', '包括质量评审、不合格品处理、质量归零等情况')
    RETURNING id INTO ch5;

    -- 5.1 质量评审情况
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '质量评审情况', 2, 51, TRUE, 'GJB/Z 170.4 5.5.1', '列出设计评审、工艺评审、产品质量评审等结果');

    -- 5.2 质量问题归零
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, content_schema) VALUES
    (tpl_id, ch5, '5.2', '质量问题归零情况', 2, 52, TRUE, 'GJB/Z 170.4 5.5.2', '列出研制中发生的质量问题及其归零闭环情况',
     '[{"fieldName":"problemNo","fieldLabel":"问题编号","fieldType":"text","required":true},{"fieldName":"problemDesc","fieldLabel":"问题简述","fieldType":"text","required":true},{"fieldName":"severity","fieldLabel":"严重等级","fieldType":"select","required":true,"options":["一般","严重","重大"]},{"fieldName":"closeoutType","fieldLabel":"归零类型","fieldType":"select","required":true,"options":["技术归零","管理归零","双归零"]},{"fieldName":"closeoutStatus","fieldLabel":"归零状态","fieldType":"select","required":true,"options":["已完成","进行中","未开始"]}]');

    -- 第6章 可靠性维修性测试性保障性安全性
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '通用质量特性', 1, 60, TRUE, 'GJB/Z 170.4 5.6', '概述六性(可靠性/维修性/测试性/保障性/安全性/环境适应性)工作情况')
    RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.1', '可靠性', 2, 61, TRUE, 'GJB 450A', '可靠性工作项目完成情况及结果');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.2', '维修性', 2, 62, TRUE, 'GJB 368B', '维修性工作项目完成情况及结果');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.3', '测试性', 2, 63, TRUE, 'GJB 2547A', '测试性工作项目完成情况及结果');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.4', '保障性', 2, 64, TRUE, 'GJB 3872A', '保障性工作项目完成情况及结果');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.5', '安全性', 2, 65, TRUE, 'GJB 900A', '安全性工作项目完成情况及结果');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.6', '环境适应性', 2, 66, TRUE, 'GJB 4239', '环境适应性工作完成情况及结果');

    -- 第7章 标准化工作
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '标准化工作', 1, 70, TRUE, 'GJB/Z 170.4 5.7', '概述产品研制中的标准化工作情况')
    RETURNING id INTO ch7;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.1', '标准化大纲执行情况', 2, 71, TRUE, 'GJB/Z 114A', '标准化要求的落实情况');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.2', '"三化"工作情况', 2, 72, TRUE, 'GJB/Z 170.4 5.7.2', '通用化、系列化、组合化工作情况');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.3', '标准采用情况', 2, 73, TRUE, 'GJB/Z 170.4 5.7.3', '采用标准的种类和数量统计');

    -- 第8章 产品规范
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '8', '产品规范编制情况', 1, 80, TRUE, 'GJB/Z 170.4 5.8', '说明产品规范的编制情况和主要内容');

    -- 第9章 设计图样和技术文件
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '9', '设计图样和技术文件', 1, 90, TRUE, 'GJB/Z 170.4 5.9', '列出全套设计图样和技术文件的目录及完整性');

    -- 第10章 研制经验与教训
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '10', '研制经验与教训', 1, 100, TRUE, 'GJB/Z 170.4 5.10', '总结研制中的经验教训，为后续型号研制提供参考', '客观总结，避免空话套话，注重可借鉴性');

    -- 第11章 产品改进建议
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '11', '产品改进建议', 1, 110, FALSE, 'GJB/Z 170.4 5.11', '对后续生产和改进提出建议');

    -- 第12章 结论
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '12', '结论', 1, 120, TRUE, 'GJB/Z 170.4 5.12', '给出研制总结的总体结论，明确产品是否具备设计定型条件');
END $$;

-- ============================================================
-- 4. 系统规范(A类) 章节结构 (GJB 6387-2008 第5章)
-- ============================================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-A-SPEC';

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387 5.1', '说明规范的主题内容、适用范围和分类')
    RETURNING id INTO ch1;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.1', '主题内容', 2, 11, TRUE, 'GJB 6387 5.1.1', '规范所覆盖的主要技术内容');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.2', '适用范围', 2, 12, TRUE, 'GJB 6387 5.1.2', '规范适用的产品范围');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch1, '1.3', '分类', 2, 13, FALSE, 'GJB 6387 5.1.3', '产品的分类方式（如适用）');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 6387 5.2', '列出规范中引用的所有标准文件')
    RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, content_schema) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387 5.3', '规定系统必须满足的各种要求',
     '[{"fieldName":"reqCategory","fieldLabel":"要求类别","fieldType":"select","required":true,"options":["功能","性能","接口","环境","可靠性","维修性","测试性","安全性","保障性","电磁兼容性","人机工程","结构","软件","标准化","其他"]},{"fieldName":"reqNumber","fieldLabel":"要求编号","fieldType":"text","required":true},{"fieldName":"reqContent","fieldLabel":"要求内容","fieldType":"richtext","required":true},{"fieldName":"verificationMethod","fieldLabel":"验证方法","fieldType":"select","required":true,"options":["试验","演示","检查","分析","仿真"]},{"fieldName":"verificationLevel","fieldLabel":"验证级别","fieldType":"text","required":false}]')
    RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387 5.4', '规定验证各项要求的方法和判据')
    RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '包装运输和贮存', 1, 50, FALSE, 'GJB 6387 5.5', '包装、运输和贮存要求')
    RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '说明事项', 1, 60, TRUE, 'GJB 6387 5.6', '预定用途、分类代号、合格判定等说明')
    RETURNING id INTO ch6;
END $$;

-- ============================================================
-- 5. 知识卡片种子数据 (核心GJB条款白话解释)
-- ============================================================
INSERT INTO knowledge_card(card_type, target_table, target_id, title, plain_language, gjb_reference, tags) VALUES
('GJB_CLAUSE', 'doc_template_chapter', NULL,
 '为什么要写"战术技术指标符合性"？',
 '这是研制总结中最关键的一章。设计定型审查时，审查组会逐项核对每一项战术技术指标是否达到了研制总要求中的规定值。如果某项指标不满足要求，需要说明原因和补救措施。这章的内容可以直接从系统中"项目主数据"的战术技术指标表中自动提取。',
 'GJB/Z 170.4-2013 5.3',
 '研制总结,战术技术指标,设计定型'),

('GJB_CLAUSE', 'doc_template_chapter', NULL,
 '为什么要写"关键技术攻关情况"？',
 '研制过程中遇到的技术难题及其解决过程，是评价项目技术水平的重要依据。评审专家通过这部分内容判断：1)技术风险是否得到有效控制；2)攻关方法是否科学合理；3)是否还有遗留问题。重大技术攻关是设计定型审查的必查项目。',
 'GJB/Z 170.4-2013 5.4',
 '研制总结,技术攻关,设计定型'),

('GJB_CLAUSE', 'doc_template_chapter', NULL,
 '为什么要写"质量问题归零情况"？',
 '"归零"是军工质量管理最核心的概念。技术归零要求做到"定位准确、机理清楚、问题复现、措施有效、举一反三"五条；管理归零要求做到"过程清楚、责任明确、措施落实、严肃处理、完善规章"五条。研制总结中必须有归零情况的汇总，这是判断产品是否成熟的关键依据。',
 'GJB/Z 194-2021',
 '归零,质量问题,FRACAS,双五条'),

('GJB_CLAUSE', 'doc_template_chapter', NULL,
 '为什么要写"通用质量特性"？',
 '通用质量特性俗称"六性"：可靠性（MTBF）、维修性（MTTR）、测试性（FDR/FAR）、保障性（Ao）、安全性（RAC）、环境适应性。这六项是装备战斗力的基础保障。军方最关心的是"装备好不好用、容不容易坏、坏了好不好修"。六性的每一项都需要通过试验验证，研制总结中必须汇总所有六性试验结果。',
 'GJB 450A/368B/2547A/3872A/900A',
 '六性,可靠性,维修性,测试性,保障性,安全性'),

('GJB_CLAUSE', 'doc_template_chapter', NULL,
 'A/B/C/D/E五类规范有什么区别？',
 'GJB 6387规定了五类军用规范：A类(系统规范)——管整个武器系统，规定系统级"能干什么"；B类(研制规范)——管系统以下的各分系统/设备，规定"要达到什么指标"；C类(产品规范)——管最终交付的产品，规定"做出来是什么样"；D类(工艺规范)——管制造过程，规定"怎么做出来"；E类(材料规范)——管原材料，规定"用什么材料"。选对规范类型，就选对了文档框架。',
 'GJB 6387-2008 4.1-4.5',
 '规范分类,A类,B类,C类,D类,E类'),

('GJB_CLAUSE', 'doc_template_chapter', NULL,
 '设计定型需要准备哪些文件？',
 '根据GJB 1362A，设计定型至少需要23类文件：审查意见书、申请报告、军代表意见、试验大纲和报告、研制总结、研制总要求、研制合同、技术攻关报告、研制试验报告、标准化文件、质量分析报告、六性评估报告、设计计算报告、软件文档、全套图样、成本分析报告、产品规范、技术说明书等、配套表、影像资料等。系统中已预置了这些文件类型。',
 'GJB 1362A-2007 7.2.1',
 '设计定型,文件清单,23类文件'),

('GJB_CLAUSE', 'doc_template_chapter', NULL,
 'FMECA分析什么时候做？怎么做？',
 'FMECA(故障模式影响及危害性分析)分两步做：方案阶段做功能FMECA——分析系统功能图，找出可能的故障模式；工程研制阶段做硬件FMECA——分析到每个元器件。严酷度I类(灾难性)故障必须强制改进，RPN≥100的必须处理。这个分析结果是可靠性设计和安全性设计的基础。',
 'GJB/Z 1391-2006',
 'FMECA,故障分析,可靠性,安全性,RPN'),

('GJB_CLAUSE', 'doc_template_chapter', NULL,
 '什么是"三基线"体系？',
 '三基线是GJB 3206B技术状态管理的核心：功能基线(FBL)——方案阶段建立，规定系统"要干什么"；分配基线(ABL)——工程研制阶段建立，把系统要求分配到各分系统/设备；产品基线(PBL)——设计定型时建立，最终产品的全套技术文件。每一条基线建立后，变更必须走正式的更改控制流程。',
 'GJB 3206B-2022',
 '三基线,功能基线,分配基线,产品基线,技术状态管理'),

('WRITING_TIP', 'doc_template_chapter', NULL,
 '编写技巧：指标符合性表格',
 '战术技术指标符合性表是评审组最先查看的内容。建议：1)指标名称必须与研制总要求完全一致(不要自己改名)；2)要求值和实测值的单位必须一致；3)对于用文字描述的定性指标(如"应具有抗干扰能力")，结论栏填写具体的验证试验名称和结果；4)不符合项要单独标注并附说明。',
 'GJB/Z 170.4-2013 5.3',
 '编写技巧,指标表格,符合性'),

('WRITING_TIP', 'doc_template_chapter', NULL,
 '编写技巧：故障问题汇总',
 '研制总结中的故障问题汇总建议从FRACAS系统直接导出，确保数据一致性。如果在Word中手动整理，最容易出现的问题是：1)与FRACAS系统中的数据不一致；2)遗漏已归零但未在系统中更新的问题；3)编号不连续。建议先在系统中完成归零闭环，再导出汇总。',
 'GJB/Z 170.4-2013 5.5.2',
 '编写技巧,故障汇总,FRACAS,归零');
