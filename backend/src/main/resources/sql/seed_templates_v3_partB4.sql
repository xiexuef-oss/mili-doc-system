-- ============================================================
-- Part B4: 研制规范B类 + 产品规范C类 + 详细设计 + 研制总结
-- ============================================================

-- ========================================
-- 产品规范 C类 (TPL-C-SPEC) — GJB 6387-2008 Ch6-7
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-C-SPEC';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 6387 4.2.3.2', '产品规范的适用范围，适用的产品名称和代号') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 6387 4.2.3.2', '本规范引用的所有标准和规范文件') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '3', '要求', 1, 30, TRUE, 'GJB 6387 4.2.3.2', '产品必须满足的全部功能特性、物理特性和验收要求',
     'C类规范面向制造验收，第3章是核心。每个要求必须是可检验的，使用"应"字句') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '产品概述', 2, 31, TRUE, 'GJB 6387', '产品的组成、工作原理和主要功能');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', '功能特性', 2, 32, TRUE, 'GJB 6387', '产品应具备的功能及其性能参数');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.3', '物理特性', 2, 33, TRUE, 'GJB 6387', '外形尺寸、重量、颜色、标识、材料等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.4', '接口要求', 2, 34, TRUE, 'GJB 6387', '机械接口、电气接口、数据接口等');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.5', '环境适应性', 2, 35, TRUE, 'GJB 150', '温度/湿度/振动/冲击/盐雾等环境条件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.6', '电磁兼容性', 2, 36, TRUE, 'GJB 151B/152B', '电磁发射和敏感度限值');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.7', '可靠性', 2, 37, TRUE, 'GJB 450A', 'MTBF等可靠性定量要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.8', '维修性', 2, 38, TRUE, 'GJB 368B', 'MTTR等维修性定量要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.9', '安全性', 2, 39, TRUE, 'GJB 900A', '产品安全性设计和使用要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.10', '制造要求', 2, 310, TRUE, 'GJB 6387', '关键工序、特殊过程、检验点设置等制造工艺要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.11', '标识和可追溯性', 2, 311, TRUE, 'GJB 726', '产品标识、检验状态标识和追溯要求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '验证', 1, 40, TRUE, 'GJB 6387 4.2.3.2', '各项要求的检验方法和验收准则') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '检验分类', 2, 41, TRUE, 'GJB 6387', '鉴定检验和质量一致性检验的分类和条件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '检验方法', 2, 42, TRUE, 'GJB 6387', '各检验项目的检验设备和检验步骤');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '检验矩阵', 2, 43, TRUE, 'GJB 6387', '要求条款→检验项目→检验方法的对应表');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '包装、运输与贮存', 1, 50, TRUE, 'GJB 6387', '防护包装、装箱、运输和贮存要求') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '说明事项', 1, 60, TRUE, 'GJB 6387', '预定用途、术语定义、订购文件中应明确的内容') RETURNING id INTO ch6;
END $$;

-- ========================================
-- 详细设计 (TPL-DETAIL-DESIGN) — GJB 2993 5.5.1
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT; ch7 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-DETAIL-DESIGN';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 2993 5.5.1', '本文档的适用范围和不适用范围') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '规范性引用文件', 1, 20, TRUE, 'GJB 2993', '引用的标准、规范和输入文件') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '术语和定义', 1, 30, FALSE, 'GJB 2993', '本文档使用的专有术语和缩略语') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '系统概述', 1, 40, TRUE, 'GJB 2993 5.5.1', '系统的总体组成、功能和工作原理',
     '参考研制方案和研制规范的描述，简述系统级设计') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '硬件设计', 1, 50, TRUE, 'GJB 2993 5.5.1', '各功能单元的硬件详细设计') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '硬件总体架构', 2, 51, TRUE, 'GJB 2993', '硬件总体框图和各模块说明');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', '各模块详细设计', 2, 52, TRUE, 'GJB 2993', '每个功能模块的电路原理、器件选型、参数计算');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.3', '结构设计', 2, 53, TRUE, 'GJB 2993', '结构外形、安装尺寸、材料选用、表面处理');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.4', '电磁兼容性设计', 2, 54, TRUE, 'GJB 151B', '电磁屏蔽、滤波、接地等EMC设计措施');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.5', '热设计', 2, 55, FALSE, 'GJB/Z 27', '散热方案、热仿真分析和验证');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '软件设计', 1, 60, TRUE, 'GJB 438B', '软件体系结构、模块划分和接口设计') RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.1', '软件架构', 2, 61, TRUE, 'GJB 438B', '软件分层架构、运行环境和开发平台');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.2', '模块设计', 2, 62, TRUE, 'GJB 438B', '各CSC的输入/输出/处理逻辑/数据设计');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.3', '接口设计', 2, 63, TRUE, 'GJB 438B', '软件内部接口和外部接口的详细定义');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '接口设计(系统级)', 1, 70, TRUE, 'GJB 2737', '系统内外部接口的详细定义') RETURNING id INTO ch7;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.1', '电气接口', 2, 71, TRUE, 'GJB 2737', '电源、信号、通信接口的电气特性定义');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.2', '机械接口', 2, 72, TRUE, 'GJB 2737', '安装尺寸、连接器型号、安装方式');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.3', '数据接口', 2, 73, TRUE, 'GJB 2737', '通信协议、数据格式、ICD接口控制文件引用');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '8', '通用质量特性设计', 1, 80, TRUE, 'GJB 2993', '可靠性/维修性/测试性/保障性/安全性/环境适应性') RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '8.1', '可靠性设计', 2, 81, TRUE, 'GJB 450A', '可靠性预计、降额设计、冗余设计措施');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '8.2', '测试性设计', 2, 82, TRUE, 'GJB 2547A', 'BIT设计、测试点设置、故障隔离方案');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '8.3', '安全性设计', 2, 83, TRUE, 'GJB 900A', '危险控制措施，安全装置和告警设计');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '8.4', '环境适应性设计', 2, 84, TRUE, 'GJB 150', '温度/振动/冲击/盐雾等环境防护设计');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '8.5', '电磁兼容性设计', 2, 85, TRUE, 'GJB 151B/152B', '电磁屏蔽/滤波/接地/布线等EMC设计');
END $$;

-- ========================================
-- 研制总结 (TPL-DEV-SUMMARY) — GJB/Z 170.4-2013
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT;
    ch5 BIGINT; ch6 BIGINT; ch7 BIGINT; ch8 BIGINT; ch9 BIGINT;
    ch10 BIGINT; ch11 BIGINT; ch12 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-DEV-SUMMARY';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '研制任务来源', 1, 10, TRUE, 'GJB/Z 170.4 4.1',
     '立项批复、研制总要求下达时间/机关/文号') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '产品概述', 1, 20, TRUE, 'GJB/Z 170.4 4.2',
     '使命任务、产品组成及主要功能、研制任务分工') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '研制过程', 1, 30, TRUE, 'GJB/Z 170.4 4.3',
     '方案阶段→工程研制阶段(初样/试样)→设计定型阶段的研制工作概述') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '设计定型试验情况', 1, 40, TRUE, 'GJB/Z 170.4 4.4',
     '软件定型测评、基地试验、部队试验的简要情况', '用表格汇总试验项目/结论/是否满足指标') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '出现的技术问题及解决情况', 1, 50, TRUE, 'GJB/Z 170.4 4.5',
     '表格汇总+重大问题逐项详细说明(问题描述/原因分析/解决措施/验证情况)') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '主要配套产品定型情况及质量供货保障', 1, 60, TRUE, 'GJB/Z 170.4 4.6') RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '可靠性/维修性/测试性/保障性/安全性情况', 1, 70, TRUE, 'GJB/Z 170.4 4.7',
     '每项含: 要求→设计→试验→数据分析→评估结论') RETURNING id INTO ch7;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.1', '可靠性', 2, 71, TRUE, 'GJB/Z 170.4', '可靠性要求、设计措施、试验验证、评估结论');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.2', '维修性', 2, 72, TRUE, 'GJB/Z 170.4', '维修性要求、设计措施、验证评估');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.3', '测试性', 2, 73, TRUE, 'GJB/Z 170.4', '测试性要求、设计措施、验证评估');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.4', '安全性', 2, 74, TRUE, 'GJB/Z 170.4', '安全性要求、危险控制、验证评估');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '8', '贯彻标准化大纲情况', 1, 80, TRUE, 'GJB/Z 170.4 4.8',
     '标准实施情况、"三化"情况、标准化系数') RETURNING id INTO ch8;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '9', '产品质量/工艺性/经济性评价', 1, 90, TRUE, 'GJB/Z 170.4 4.9') RETURNING id INTO ch9;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '10', '产品达到的战术技术性能', 1, 100, TRUE, 'GJB/Z 170.4 4.10',
     '对照表(指标要求/实测值/数据来源/符合情况)', '这是最关键的章节，用表格形式列出每项指标的实际达到情况') RETURNING id INTO ch10;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '11', '产品尚存问题及解决措施', 1, 110, TRUE, 'GJB/Z 170.4 4.11',
     '问题描述/原因分析/影响程度/解决措施/时间节点/责任单位') RETURNING id INTO ch11;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '12', '对产品设计定型的意见', 1, 120, TRUE, 'GJB/Z 170.4 4.12',
     '按设计定型六条标准逐条叙述', '明确结论: a)达到研制总要求 b)符合体制和三化 c)图样文件完整 d)配套齐全 e)质量可靠 f)承制资格') RETURNING id INTO ch12;
END $$;
