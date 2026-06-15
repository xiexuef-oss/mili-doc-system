-- ============================================================
-- Part B1: 方案阶段核心模板章节结构
-- ============================================================

-- ========================================
-- 技术状态管理计划 (TPL-CM-PLAN) — 依据 GJB 3206B-2022
-- ========================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT;
    ch6 BIGINT; ch7 BIGINT; ch8 BIGINT; ch9 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-CM-PLAN';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 3206B 4.1', '说明本计划的适用范围，包括适用的项目阶段、技术状态项范围',
     '明确列出计划覆盖的技术状态项(CI)清单，说明从哪个阶段开始、哪个阶段结束') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 3206B 4.2', '列出本计划引用的所有标准和规范文件');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '术语和定义', 1, 30, TRUE, 'GJB 3206B 4.3', '定义本计划使用的专有术语：技术状态项(CI)、基线、技术状态文件等');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '技术状态标识', 1, 40, TRUE, 'GJB 3206B 5',
     '技术状态项的选择、技术状态文件的确定、编码体系、基线的建立',
     '必须列出所有技术状态项的清单（名称+编号），明确三种基线(功能基线/分配基线/产品基线)的建立时机和内容') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '技术状态项选择', 2, 41, TRUE, 'GJB 3206B 5.2', '技术状态项的选择准则和清单');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '技术状态文件', 2, 42, TRUE, 'GJB 3206B 5.3', '三类技术状态文件的确定：功能技术状态文件/分配技术状态文件/产品技术状态文件');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '编码体系', 2, 43, TRUE, 'GJB 3206B 5.4', '文档编号、版本号、技术状态项编号规则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '基线建立', 2, 44, TRUE, 'GJB 3206B 5.5', '功能基线、分配基线、产品基线的建立时机、批准程序和内容');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '技术状态控制', 1, 50, TRUE, 'GJB 3206B 6',
     '更改分类(0/I/II/III类)、更改申请→评估→审批→实施→确认的完整流程') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '更改分类', 2, 51, TRUE, 'GJB 3206B 6.2', '特殊类(0类)/I类/II类/III类更改的定义和判别准则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', '更改控制程序', 2, 52, TRUE, 'GJB 3206B 6.3', '判定需求→提交申请→评估评审→审批决策→编制通知→实施更改→确认完成');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.3', 'CCB组织与运行', 2, 53, TRUE, 'GJB 3206B 6.4', '配置控制委员会的组成、职责、会议制度和决策机制');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '技术状态记实', 1, 60, TRUE, 'GJB 3206B 7', '技术状态信息的记录、报告和归档要求');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '7', '技术状态审核', 1, 70, TRUE, 'GJB 3206B 8',
     '功能技术状态审核(FCA)和物理技术状态审核(PCA)的计划和要求') RETURNING id INTO ch7;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.1', '功能技术状态审核(FCA)', 2, 71, TRUE, 'GJB 3206B 8.2', '审核目的、时机、内容和通过准则');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch7, '7.2', '物理技术状态审核(PCA)', 2, 72, TRUE, 'GJB 3206B 8.3', '审核目的、时机、内容和通过准则');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '8', '组织和职责', 1, 80, TRUE, 'GJB 3206B 9', '技术状态管理的组织架构、各级职责和接口关系');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '9', '工具和资源', 1, 90, FALSE, 'GJB 3206B 10', '使用的技术状态管理软件工具、数据库和资源保障');
END $$;
