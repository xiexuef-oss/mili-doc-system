-- ============================================================
-- Part B5: 软件文档模板 — SDP + SDD + STP + SCMP (GJB 438B-2009)
-- ============================================================

-- ========================================
-- 软件开发计划 SDP (TPL-SW-SDP) — GJB 438B
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT;
    ch5 BIGINT; ch6 BIGINT; ch7 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-SW-SDP';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 438B', '本计划的适用范围、适用项目和软件配置项',
     '标识本计划适用的CSCI名称和代号') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 438B', '引用的标准、规范和其他文件') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '术语和缩略语', 1, 30, FALSE, 'GJB 438B', '本文档使用的专有术语和缩略语') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '项目概述', 1, 40, TRUE, 'GJB 438B', '项目背景/软件概述/开发目标和约束条件',
     '简述项目来源、软件的主要功能和性能指标') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '项目背景', 2, 41, TRUE, 'GJB 438B', '项目来源、项目目标和总体要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '软件概述', 2, 42, TRUE, 'GJB 438B', '软件的主要功能、运行环境和用户特点');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '可交付的软件产品', 2, 43, TRUE, 'GJB 438B', '计划交付的软件项清单及其交付形式和时间');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '软件开发过程', 1, 50, TRUE, 'GJB 438B', '采用的软件开发模型和各阶段活动') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '开发模型', 2, 51, TRUE, 'GJB 438B', '瀑布/迭代/敏捷等开发模型的选择和理由');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', '阶段划分与里程碑', 2, 52, TRUE, 'GJB 438B', '各开发阶段的起止时间、主要活动和里程碑');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.3', '开发方法和工具', 2, 53, TRUE, 'GJB 438B', '编程语言/开发环境/建模工具/测试工具等');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '项目管理和控制', 1, 60, TRUE, 'GJB 438B', '进度管理/风险管理/配置管理/质量保证') RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.1', '进度计划', 2, 61, TRUE, 'GJB 438B', 'WBS工作分解和甘特图/里程碑计划');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.2', '风险管理', 2, 62, TRUE, 'GJB 438B', '风险识别/评估/缓解措施和跟踪');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.3', '资源分配', 2, 63, TRUE, 'GJB 438B', '人员/设备/设施等资源需求和分配');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '组织和职责', 1, 70, TRUE, 'GJB 438B', '开发团队组织结构、角色职责和沟通机制') RETURNING id INTO ch7;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.1', '组织结构', 2, 71, TRUE, 'GJB 438B', '开发团队的组织架构图和人员配置');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.2', '角色职责', 2, 72, TRUE, 'GJB 438B', '项目经理/软件负责人/开发人员/测试人员等职责');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.3', '评审机制', 2, 73, TRUE, 'GJB 438B', '同行评审/里程碑评审/正式技术评审的计划');
END $$;

-- ========================================
-- 软件设计说明 SDD (TPL-SW-SDD) — GJB 438B
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-SW-SDD';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 438B', '本文档标识、所适用的系统和CSCI') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 438B', '引用的标准、需求文档、接口文档') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '3', 'CSCI级设计决策', 1, 30, TRUE, 'GJB 438B', 'CSCI级的设计决策及其理由，包括设计约束',
     '说明选择此设计的原因、弃选方案及其被否决的理由') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', 'CSCI体系结构设计', 1, 40, TRUE, 'GJB 438B', 'CSCI的软件单元划分、单元间的关系和接口') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', 'CSC划分', 2, 41, TRUE, 'GJB 438B', 'CSCI的CSC(计算机软件部件)清单和功能描述');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', 'CSU划分', 2, 42, TRUE, 'GJB 438B', '各CSC的CSU(计算机软件单元)清单和功能描述');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '执行流程', 2, 43, TRUE, 'GJB 438B', '软件的执行控制流程、并发和中断处理');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', 'CSCI详细设计', 1, 50, TRUE, 'GJB 438B', '每个CSC/CSU的详细设计描述') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', 'CSC-XXX详细设计', 2, 51, TRUE, 'GJB 438B', '输入/输出/处理逻辑/数据设计/接口设计(每个CSC一节)');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', '全局数据结构', 2, 52, TRUE, 'GJB 438B', 'CSCI级全局数据结构的定义和描述');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '需求可追踪性', 1, 60, TRUE, 'GJB 438B',
     'CSC/CSU到SRS中需求的追踪关系矩阵', '用表格列出每个需求→对应的设计单元→验证方法') RETURNING id INTO ch6;
END $$;

-- ========================================
-- 软件测试计划 STP (TPL-SW-STP) — GJB 438B
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT; ch7 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-SW-STP';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 438B', '测试计划的适用范围、受测CSCI标识') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 438B', '引用的标准、需求文档、设计文档') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '3', '测试环境', 1, 30, TRUE, 'GJB 438B', '测试所需的硬件、软件、测试工具',
     '分别说明：a)软件项测试环境 b)软件系统测试环境 c)软硬件集成测试环境') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '测试级别和类型', 1, 40, TRUE, 'GJB 438B',
     '单元测试→CSCI集成测试→CSCI合格性测试→系统测试') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '单元测试', 2, 41, TRUE, 'GJB 438B', '单元测试的范围、方法和通过准则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', 'CSCI集成测试', 2, 42, TRUE, 'GJB 438B', '集成策略(自顶向下/自底向上/混合)、测试用例设计方法');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', 'CSCI合格性测试', 2, 43, TRUE, 'GJB 438B', '功能/性能/接口/安全性等测试的覆盖范围和评价准则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '系统合格性测试', 2, 44, TRUE, 'GJB 438B', '软件与硬件/其他系统集成的测试方法');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '测试进度', 1, 50, TRUE, 'GJB 438B', '各测试活动的起止时间和资源安排') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '测试通过/失败准则', 1, 60, TRUE, 'GJB 438B',
     '每个测试级别和测试类型的通过标准和失败处理') RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '组织和职责', 1, 70, TRUE, 'GJB 438B', '测试团队组织结构、角色职责和沟通机制') RETURNING id INTO ch7;
END $$;

-- ========================================
-- 软件配置管理计划 SCMP (TPL-SW-SCMP) — GJB 5235-2004
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT; ch7 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-SW-SCMP';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 5235', '本计划的适用范围和适用项目') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 5235', '引用的标准、规范和相关文档') RETURNING id INTO ch2;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '术语和缩略语', 1, 30, FALSE, 'GJB 5235', '配置项(CI)/基线/CCB/配置审计等术语') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '配置标识', 1, 40, TRUE, 'GJB 5235', '配置项的选择和命名规则、基线定义',
     '列出所有纳入配置管理的软件项及其编号。说明分支策略和标记方法') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '配置项选择', 2, 41, TRUE, 'GJB 5235', '纳入配置管理的软件项清单和选择准则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '命名规则', 2, 42, TRUE, 'GJB 5235', '配置项的命名规则和版本号规则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '基线管理', 2, 43, TRUE, 'GJB 5235', '功能基线/分配基线/产品基线的定义和升级流程');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '配置控制', 1, 50, TRUE, 'GJB 5235', '更改控制流程/CCB组成和运行/版本控制') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '更改控制流程', 2, 51, TRUE, 'GJB 5235', '更改申请→评估→审批→实施→验证的完整流程');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', 'CCB组织', 2, 52, TRUE, 'GJB 5235', '配置控制委员会的成员、职责和决策机制');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.3', '版本控制', 2, 53, TRUE, 'GJB 5235', '使用的版本控制工具(如Git)和使用规范');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '配置状态记实', 1, 60, TRUE, 'GJB 5235', '配置状态的记录/报告/统计分析') RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '配置审计', 1, 70, TRUE, 'GJB 5235', '功能配置审计(FCA)和物理配置审计(PCA)') RETURNING id INTO ch7;
END $$;
