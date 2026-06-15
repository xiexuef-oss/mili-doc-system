-- ============================================================
-- Part B2: 可靠性大纲 + 安全性大纲 + 系统规范A类 + 研制规范B类
-- ============================================================

-- ========================================
-- 可靠性大纲 (TPL-RELIABILITY-PLAN) — 依据 GJB 450A-2004
-- ========================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT;
    ch5 BIGINT; ch6 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-RELIABILITY-PLAN';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 450A 4.1', '本大纲的适用范围、适用产品、适用阶段',
     '明确说明大纲覆盖的产品层次(系统/分系统/设备)和研制阶段') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 450A 4.2', '列出引用的标准和规范文件');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '3', '可靠性要求', 1, 30, TRUE, 'GJB 450A 100系列',
     '可靠性定量要求和定性要求，包括MTBF/MTBCF/可靠度等参数及其目标值',
     '定量指标必须给出具体数值，定性要求必须可验证。如"MTBF≥500小时"、"任务可靠度≥0.95"') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.1', '基本可靠性要求', 2, 31, TRUE, 'GJB 450A', 'MTBF(平均故障间隔时间)或MTBM(平均维修间隔时间)');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.2', '任务可靠性要求', 2, 32, TRUE, 'GJB 450A', 'MTBCF(致命性故障间隔任务时间)或任务可靠度R(t)');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch3, '3.3', '耐久性要求', 2, 33, FALSE, 'GJB 450A', '使用寿命、贮存寿命等耐久性参数');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '可靠性管理', 1, 40, TRUE, 'GJB 450A 200系列',
     '可靠性工作计划、评审、故障报告分析和纠正措施系统(FRACAS)、故障审查组织',
     'FRACAS必须形成闭环: 故障报告→核实→分析→纠正措施→验证→关闭') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '可靠性工作计划', 2, 41, TRUE, 'GJB 450A 201', '可靠性工作的任务分解、进度安排和资源配置');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', 'FRACAS', 2, 42, TRUE, 'GJB 841', '故障报告、分析和纠正措施系统的建立和运行');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '可靠性评审', 2, 43, TRUE, 'GJB 450A 203-204', '可靠性设计评审和专题评审的节点和内容');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '5', '可靠性设计与分析', 1, 50, TRUE, 'GJB 450A 300系列',
     '可靠性建模、分配、预计、FMEA/FMECA、FTA、降额设计、冗余设计等',
     '每一项分析工作都要说明采用的方法/工具和预期成果。FMEA建议按GJB/Z 1391执行') RETURNING id INTO ch5;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.1', '可靠性建模', 2, 51, TRUE, 'GJB 450A 301', '可靠性框图和数学模型建立方法');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.2', '可靠性分配', 2, 52, TRUE, 'GJB 450A 302', '将系统级可靠性指标分配到分系统和设备');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.3', '可靠性预计', 2, 53, TRUE, 'GJB 450A 303', '预计产品能达到的可靠性水平并评价是否满足要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.4', 'FMEA/FMECA', 2, 54, TRUE, 'GJB/Z 1391', '故障模式影响分析/故障模式影响及危害性分析');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.5', '故障树分析(FTA)', 2, 55, FALSE, 'GJB/Z 768A', '对关键故障事件进行故障树分析');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch5, '5.6', '可靠性设计准则', 2, 56, TRUE, 'GJB 450A 310-314', '降额设计、冗余设计、热设计、简化设计等准则');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '6', '可靠性试验与评价', 1, 60, TRUE, 'GJB 450A 400系列',
     '可靠性研制试验、鉴定试验、验收试验、环境应力筛选(ESS)、可靠性增长试验') RETURNING id INTO ch6;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.1', '环境应力筛选(ESS)', 2, 61, TRUE, 'GJB 1032', '对产品100%进行环境应力筛选，剔除早期故障');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.2', '可靠性研制试验', 2, 62, TRUE, 'GJB 450A 401', '通过试验发现设计缺陷，提高产品固有可靠性');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.3', '可靠性鉴定试验', 2, 63, TRUE, 'GJB 899A', '验证产品是否达到规定的可靠性要求');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.4', '可靠性验收试验', 2, 64, FALSE, 'GJB 899A', '批生产阶段验证产品可靠性的一致性');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch6, '6.5', '可靠性增长试验', 2, 65, FALSE, 'GJB 1407', '有计划地激发故障、分析改进，使可靠性逐步增长');
END $$;

-- ========================================
-- 安全性大纲 (TPL-SAFETY-PLAN) — 依据 GJB 900A-2012
-- ========================================
DO $$
DECLARE
    tpl_id BIGINT;
    ch1 BIGINT; ch2 BIGINT; ch3 BIGINT; ch4 BIGINT;
    ch5 BIGINT;
BEGIN
    SELECT id INTO tpl_id FROM doc_template_v2 WHERE template_code = 'TPL-SAFETY-PLAN';
    IF tpl_id IS NULL THEN RETURN; END IF;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '1', '范围', 1, 10, TRUE, 'GJB 900A 4.1', '适用产品、阶段和范围说明') RETURNING id INTO ch1;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '2', '引用文件', 1, 20, TRUE, 'GJB 900A 4.2', '引用的安全标准和规范');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '3', '安全性要求', 1, 30, TRUE, 'GJB 900A 5', '安全性定性/定量要求，危险严重性等级(I~IV级)，安全性关键项目判别准则',
     '必须量化安全性目标，如"I级危险发生的概率<1×10⁻⁹/飞行小时"。危险等级标准:I级=灾难,II级=严重,III级=轻度,IV级=轻微') RETURNING id INTO ch3;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description, writing_tips) VALUES
    (tpl_id, 0, '4', '安全性工作项目', 1, 40, TRUE, 'GJB 900A 6',
     '安全性工作的具体项目、实施方法和程序',
     '按安全性措施优先次序: ①最小风险设计(消除危险) ②安全装置(永久/自动防护) ③告警装置 ④专用规程和培训。I/II级危险不能用告警作为唯一方法') RETURNING id INTO ch4;

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.1', '初步危险分析(PHA)', 2, 41, TRUE, 'GJB 900A', '方案阶段的初步危险识别和评估');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.2', '系统危险分析(SHA)', 2, 42, TRUE, 'GJB 900A', '详细设计阶段的系统级危险识别和分析');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.3', '使用与保障危险分析(O&SHA)', 2, 43, TRUE, 'GJB 900A', '分析使用、维修、保障活动中的危险');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.4', '安全性验证', 2, 44, TRUE, 'GJB 900A', '通过试验/演示/分析验证安全性要求是否满足');
    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, ch4, '4.5', '安全性关键项目管理', 2, 45, TRUE, 'GJB 900A', '安全性关键项目的识别、控制和跟踪');

    INSERT INTO doc_template_chapter(template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, standard_clause_ref, description) VALUES
    (tpl_id, 0, '5', '组织与职责', 1, 50, TRUE, 'GJB 900A 7', '安全性工作组织架构、安全审查委员会职责') RETURNING id INTO ch5;
END $$;
