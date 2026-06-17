-- ============================================================
-- 三库融合种子数据: 字段映射 + 知识卡片链接
-- ============================================================

-- ============================================================
-- Phase 1: 主数据字段映射 (各章节需要哪些主数据)
-- ============================================================

-- 研制总结 (template_id=1) 章节字段映射

-- 1. 研制任务概述
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (1, 'equipment.projectName', '项目名称', true, 1),
    (1, 'equipment.equipmentName', '装备名称', true, 2),
    (1, 'equipment.equipmentModel', '装备型号', true, 3),
    (1, 'equipment.developerUnit', '研制单位', true, 4),
    (1, 'equipment.chiefEngineer', '总师', true, 5)
ON CONFLICT DO NOTHING;

-- 1.1 研制依据
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (2, 'equipment.taskBookCode', '任务书编号', true, 1),
    (2, 'equipment.contractCode', '合同编号', true, 2)
ON CONFLICT DO NOTHING;

-- 1.2 研制任务与目标
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (3, 'equipment.equipmentName', '装备名称', true, 1),
    (3, 'tacticalIndicators', '战术技术指标', true, 2)
ON CONFLICT DO NOTHING;

-- 1.3 产品用途与组成
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (4, 'equipment.equipmentName', '装备名称', true, 1),
    (4, 'productTree', '产品组成树', true, 2)
ON CONFLICT DO NOTHING;

-- 2. 研制过程概述
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (5, 'milestones', '里程碑节点', true, 1)
ON CONFLICT DO NOTHING;

-- 2.1 方案阶段工作
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (6, 'milestones', '方案阶段节点', true, 1)
ON CONFLICT DO NOTHING;

-- 2.2 工程研制阶段工作
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (7, 'milestones', '工程研制节点', true, 1)
ON CONFLICT DO NOTHING;

-- 2.3 设计定型阶段工作
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (8, 'milestones', '设计定型节点', true, 1)
ON CONFLICT DO NOTHING;

-- 3. 战术技术指标符合性
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (9, 'tacticalIndicators', '战术技术指标列表', true, 1),
    (9, 'equipment.equipmentName', '装备名称', true, 2)
ON CONFLICT DO NOTHING;

-- 4. 关键技术攻关情况
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (10, 'equipment.equipmentName', '装备名称', true, 1)
ON CONFLICT DO NOTHING;

-- 5. 产品质量状况
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (11, 'equipment.equipmentName', '装备名称', true, 1)
ON CONFLICT DO NOTHING;

-- 6. 通用质量特性
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (14, 'equipment.equipmentName', '装备名称', true, 1),
    (14, 'tacticalIndicators', '通用质量特性指标', true, 2)
ON CONFLICT DO NOTHING;

-- 产品规范模板 (template_id=5) 章节字段映射

-- 3. 要求
INSERT INTO template_chapter_field_mapping (template_chapter_id, master_data_path, field_label, is_required, order_num)
VALUES
    (35, 'tacticalIndicators', '战术技术指标', true, 1),
    (35, 'equipment.equipmentName', '装备名称', true, 2)
ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 2: 知识卡片链接 (章节 ↔ 知识卡片)
-- ============================================================

-- 卡1 "为什么要写战术技术指标符合性" → 第3章
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (9, 1) ON CONFLICT DO NOTHING;

-- 卡2 "为什么要写关键技术攻关情况" → 第4章
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (10, 2) ON CONFLICT DO NOTHING;

-- 卡3 "为什么要写质量问题归零情况" → 第5.2章
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (13, 3) ON CONFLICT DO NOTHING;

-- 卡4 "为什么要写通用质量特性" → 第6章
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (14, 4) ON CONFLICT DO NOTHING;

-- 卡5 "五类规范区别" → 产品规范模板第8章
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (38, 5) ON CONFLICT DO NOTHING;

-- 卡6 "设计定型文件清单" → 研制总结2.3
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (8, 6) ON CONFLICT DO NOTHING;

-- 卡7 "FMECA分析" → 可靠性章节6.1
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (15, 7) ON CONFLICT DO NOTHING;

-- 卡8 "三基线体系" → 研制过程2
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (5, 8) ON CONFLICT DO NOTHING;

-- 卡9 "指标符合性表格技巧" → 第3章
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (9, 9) ON CONFLICT DO NOTHING;

-- 卡10 "故障问题汇总技巧" → 第5章
INSERT INTO chapter_knowledge_card_link (template_chapter_id, knowledge_card_id)
VALUES (11, 10) ON CONFLICT DO NOTHING;

-- ============================================================
-- Phase 3: 标准条款链接 (手动补充)
-- ============================================================

-- 产品规范模板(5)的章节手动链接到GJB/Z 170.4条款
-- These are done via the migration already, but we add explicit ones

-- 1. 范围 → GJB 6387 4.1 (组织环境)
INSERT INTO template_chapter_clause_link (template_chapter_id, standard_clause_id, link_type, relevance_note)
VALUES (30, 1, 'INFORMS', 'GJB 9001C 4.1 组织环境适用于文档范围定义')
ON CONFLICT DO NOTHING;

-- 3. 要求 → GJB 3206B 5.1 (技术状态标识)
INSERT INTO template_chapter_clause_link (template_chapter_id, standard_clause_id, link_type, relevance_note)
VALUES (35, 2, 'REFERENCES', '技术状态标识要求适用于产品规范中要求的编写')
ON CONFLICT DO NOTHING;
