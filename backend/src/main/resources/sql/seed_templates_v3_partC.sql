-- ============================================================
-- Part B6: 试验类 + 特性分析
-- ============================================================

-- ========================================
-- 研制试验大纲 (TPL-DEV-TEST-PLAN)
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-DEV-TEST-PLAN';
    IF tpl_id IS NULL THEN RETURN; END IF;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 1362A', '试验大纲的适用范围、试验对象和试验阶段') RETURNING id INTO ch1;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 1362A', '引用的标准、试验规范和被测产品技术文件') RETURNING id INTO ch2;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '3', '试验目的', 1, 30, TRUE, 'GJB 1362A', '本次试验的目的、需要验证的技术指标和功能', '逐条列出试验需要验证的指标，与研制总要求/研制规范指标对照') RETURNING id INTO ch3;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '试验项目与方法', 1, 40, TRUE, 'GJB 1362A', '每项试验的具体内容、方法和合格判据') RETURNING id INTO ch4;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '功能性能试验', 2, 41, TRUE, 'GJB 1362A', '验证产品功能是否满足规范要求的试验项目和方法');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '环境试验', 2, 42, TRUE, 'GJB 150', '温度/湿度/振动/冲击/盐雾等环境适应性试验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '电磁兼容性试验', 2, 43, TRUE, 'GJB 151B/152B', '电磁发射和敏感度测量试验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '可靠性试验', 2, 44, TRUE, 'GJB 899A', '可靠性研制/鉴定试验的项目和方法');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.5', '安全性试验', 2, 45, FALSE, 'GJB 900A', '安全性验证试验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '试验组织与实施', 1, 50, TRUE, 'GJB 1362A', '试验的组织分工/试验条件/进度安排') RETURNING id INTO ch5;
END $$;

-- ========================================
-- 设计定型试验大纲 (TPL-FINAL-TEST-PLAN)
-- ========================================
DO $$
DECLARE tpl_id BIGINT; ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT; ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-FINAL-TEST-PLAN';
    IF tpl_id IS NULL THEN RETURN; END IF;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 1362A', '定型试验大纲的适用范围、试验对象') RETURNING id INTO ch1;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '2', '任务依据', 1, 20, TRUE, 'GJB 1362A', '编制本大纲的依据文件', '必须列出全部依据文件名称、文号和下达机关') RETURNING id INTO ch2;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '3', '试验目的和性质', 1, 30, TRUE, 'GJB 1362A', '定型试验的目的和考核范围') RETURNING id INTO ch3;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '4', '被试品/陪试品', 1, 40, TRUE, 'GJB 1362A', '被试品技术状态/数量/来源，陪试品要求') RETURNING id INTO ch4;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '试验项目与考核内容', 1, 50, TRUE, 'GJB 1362A', '每项试验的考核内容、指标要求和合格判据') RETURNING id INTO ch5;
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '战术技术性能试验', 2, 51, TRUE, 'GJB 1362A', '对研制总要求规定的战术技术指标进行试验考核');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', '环境适应性试验', 2, 52, TRUE, 'GJB 1362A', '自然环境和实验室环境试验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.3', '可靠性试验', 2, 53, TRUE, 'GJB 1362A', '可靠性鉴定试验和加速寿命试验');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.4', '部队试验', 2, 54, TRUE, 'GJB 6177', '部队适应性考核的试验项目和内容');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '试验组织与保障', 1, 60, TRUE, 'GJB 1362A', '组织分工/场地/设备/通信/安全保障') RETURNING id INTO ch6;
END $$;

-- ============================================================
-- Part C: 关联 stage_doc_checklist_template → doc_template_v2
-- ============================================================
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-CM-PLAN') WHERE doc_code = 'GP-17';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-RELIABILITY-PLAN') WHERE doc_code = 'RE-04';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SAFETY-PLAN') WHERE doc_code = 'SA-02';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-A-SPEC') WHERE doc_code = 'GP-12';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-B-SPEC') WHERE doc_code = 'GP-13';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-C-SPEC') WHERE doc_code = 'GP-29';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-DETAIL-DESIGN') WHERE doc_code = 'GP-21';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-DEV-SUMMARY') WHERE doc_code = 'GP-44';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-DEV-TEST-PLAN') WHERE doc_code = 'GP-25';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-FINAL-TEST-PLAN') WHERE doc_code = 'GP-34';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-FINAL-TEST-REPORT') WHERE doc_code = 'GP-37';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-CHAR-ANALYSIS') WHERE doc_code = 'GP-23';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SW-SDP') WHERE doc_code = 'SW-07';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SW-SDD') WHERE doc_code = 'SW-14';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SW-STP') WHERE doc_code = 'SW-12';
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-SW-SCMP') WHERE doc_code = 'SW-08';
