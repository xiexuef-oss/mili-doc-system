-- ============================================================
-- 种子数据补全 v2 Batch 3 — 10份关键文档的输入参考关联
-- 库中标准引用: 27项，其中26项库中已有
-- 缺失标准: GJB 1452(大型试验质量管理要求) — 仅标注引用
-- ============================================================

-- ========================================
-- GP-19 试验与评定总计划
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-19';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案中的试验要求是试验总计划的直接输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '试验总计划的阶段划分和总体要求依据', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 1452', '大型试验质量管理要求', '大型试验质量管理的规范要求（库中缺原文，建议补充）', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '可靠性研制/鉴定/增长试验的规划要求(400系列)', TRUE, 12);
END $$;

-- ========================================
-- GP-21 详细设计
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-21';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制方案确定的技术基线是详细设计的顶层输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-16';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制规范(B类)定义了技术状态项的性能指标要求', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-13';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 3206B-2022', '技术状态管理', '详细设计阶段是分配基线建立的关键阶段', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 6387-2008', '武器装备研制项目专用规范编写规定', '详细设计输出应符合规范格式要求', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 1310A-2004', '设计评审', '详细设计完成后需进行分级分阶段设计评审', TRUE, 12);
END $$;

-- ========================================
-- GP-37 设计定型试验报告(基地试验)
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-37';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '设计定型试验大纲是试验报告的执行依据，报告逐条回应大纲中的试验项目', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-34';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '设计定型试验报告的总体编制要求', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB/Z 170.6-2013', '第6部分：设计定型基地试验报告编制指南', '基地试验报告编制指南 ✓库中有', TRUE, 11);
END $$;

-- ========================================
-- GP-40 质量问题报告
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-40';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制试验中发现的故障和问题是质量问题报告的主要来源', FALSE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-26';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '质量保证大纲中的问题报告和处理要求(第5章)', FALSE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'QA-01';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 5711-2006', '装备质量问题处理通用要求', '质量问题报告的分类(技术问题/管理问题)和处理流程', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 841A-2024', '通用质量特性问题报告分析和纠正措施系统(FRACAS)', 'FRACAS问题报告格式和闭环流程 ✓库中有', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJBZ 194-2021', '装备质量问题归零实施指南', '技术归零(五条)和管理归零(五条)的要求 ✓库中有', TRUE, 12);
END $$;

-- ========================================
-- GP-48 设计定型申请报告
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-48';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总结是设计定型申请的核心支撑文件', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-44';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '设计定型试验报告中的试验结论是申请定型的直接证据', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-37';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '军事代表机构对设计定型的意见是申请的前置条件', TRUE, 3
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-47';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '设计定型申请的程序和要求', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB/Z 170.3-2013', '第3部分：设计定型申请编制指南', '设计定型申请报告编制指南 ✓库中有', TRUE, 11);
END $$;

-- ========================================
-- GP-49 设计定型审查意见书
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'GP-49';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '设计定型申请报告是审查意见书的查阅对象', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-48';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '研制总结为审查提供全面的技术信息', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-44';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 1362A-2007', '军工产品定型程序和要求', '设计定型审查的程序和要求', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB/Z 170.2-2013', '第2部分：设计定型审查意见书编制指南', '设计定型审查意见书编制指南 ✓库中有', TRUE, 11);
END $$;

-- ========================================
-- SW-08 软件配置管理计划(SCMP)
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SW-08';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '软件开发计划(SDP)是软件配置管理计划的上游文件', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-07';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 5235-2004', '军用软件配置管理', '软件配置管理的直接依据：五大活动(标识/控制/记实/评价/发行) ✓库中有', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 438B-2009', '军用软件开发文档通用要求', '软件配置管理计划的文档格式要求', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 2786A-2009', '军用软件开发通用要求', '软件开发活动中配置管理活动(支持活动1)的要求', TRUE, 12);
END $$;

-- ========================================
-- SW-12 软件测试计划(STP)
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SW-12';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '软件需求规格说明(SRS)是测试计划中测试项的来源', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-13';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '软件设计说明(SDD)是集成测试计划的输入', FALSE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-14';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 438B-2009', '军用软件开发文档通用要求', '软件测试计划的文档格式要求', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 5234-2004', '军用软件验证和确认', '软件验证和确认的流程要求 ✓库中有', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 2786A-2009', '军用软件开发通用要求', 'CSCI合格性测试和系统合格性测试的要求', TRUE, 12);
END $$;

-- ========================================
-- RE-08 故障模式影响及危害性分析(FMECA)
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'RE-08';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '可靠性大纲(工作计划)中规定FMECA为300系列必做工作项目', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'RE-04';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '详细设计是硬件FMECA的基本输入(需分析到元器件级别)', TRUE, 2
    FROM stage_doc_checklist_template WHERE doc_code = 'GP-21';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB/Z 1391-2006', '故障模式影响及危害性分析指南', 'FMECA的直接依据 ✓库中有', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 450A-2004', '装备可靠性工作通用要求', '工作项目304(FMECA)的要求', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB/Z 768A-1998', '故障树分析指南', 'FTA作为FMECA的补充分析方法 ✓库中有', FALSE, 12);
END $$;

-- ========================================
-- SW-09 软件质量保证计划(SQAP)
-- ========================================
DO $$
DECLARE tmpl_id BIGINT;
BEGIN
    SELECT id INTO tmpl_id FROM stage_doc_checklist_template WHERE doc_code = 'SW-09';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_id, ref_code, ref_name, ref_usage, is_required, order_num)
    SELECT tmpl_id, 'UPSTREAM_DOC', id, doc_code, doc_name, '软件开发计划(SDP)是SQAP的顶层输入', TRUE, 1
    FROM stage_doc_checklist_template WHERE doc_code = 'SW-07';

    INSERT INTO doc_input_reference(checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num) VALUES
    (tmpl_id, 'STANDARD', 'GJB 438B-2009', '军用软件开发文档通用要求', '软件质量保证计划的文档格式要求', TRUE, 10),
    (tmpl_id, 'STANDARD', 'GJB 5234-2004', '军用软件验证和确认', '软件验证和确认的依据 ✓库中有', TRUE, 11),
    (tmpl_id, 'STANDARD', 'GJB 2786A-2009', '军用软件开发通用要求', '软件质量保证活动(支持活动3)的要求', TRUE, 12),
    (tmpl_id, 'STANDARD', 'GJB 5000A-2008', '军用软件研制能力成熟度模型', '软件过程质量保证(PPQA)的参考', FALSE, 13);
END $$;

