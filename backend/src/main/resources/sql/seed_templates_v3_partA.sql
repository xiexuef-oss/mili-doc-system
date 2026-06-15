-- ============================================================
-- 种子数据 v3 — 全阶段文档模板补全
-- 基于 GJB 2993-1997(研制阶段)、GJB 6387-2008(规范类型)、GJB 438B-2009(软件文档)
-- 包含: DocTemplateV2 + doc_template_chapter + checklist-template 关联
-- ============================================================

-- ========================================
-- Part A: 新增模板定义 (doc_template_v2)
-- ========================================

-- 先确保模板类别表有所有需要的类别
INSERT INTO doc_template_category(category_code, category_name, description) VALUES
('A_SPEC', '系统规范(A类)', 'GJB 6387 Ch6 系统级规范，功能基线FBL'),
('B_SPEC', '研制规范(B类)', 'GJB 6387 Ch6-7 分配基线ABL'),
('C_SPEC', '产品规范(C类)', 'GJB 6387 Ch6-7 产品基线PBL'),
('SOFTWARE', '软件文档', 'GJB 438B-2009 28种软件文档'),
('MANAGEMENT', '管理类文档', 'GJB 2993 项目管理文档'),
('QUALITY', '质量类文档', 'GJB 1406A 质量保证'),
('RELIABILITY', '可靠性文档', 'GJB 450A 可靠性'),
('SAFETY', '安全性文档', 'GJB 900A 安全性'),
('STANDARDIZATION', '标准化文档', 'GJB/Z 114A 标准化'),
('TEST', '试验类文档', 'GJB 1362A 定型试验'),
('PROCESS', '工艺类文档', 'GJB 6387 Ch6 D类规范')
ON CONFLICT (category_code) DO NOTHING;

-- 插入模板定义
INSERT INTO doc_template_v2 (category_id, template_code, template_name, template_type, 
    applicable_stage_codes, gjb_standard_ref, document_class, variables_schema, status) VALUES
-- ===== 方案阶段模板 =====
((SELECT id FROM doc_template_category WHERE category_code='MANAGEMENT'),
 'TPL-TECH-PROPOSAL', '研制方案', 'MANAGEMENT',
 'F', 'GJB 2993-1997 5.4', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称"},"projectCode":{"type":"string","label":"项目编号"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='MANAGEMENT'),
 'TPL-CM-PLAN', '技术状态管理计划', 'MANAGEMENT',
 'F,C,S', 'GJB 3206B-2022', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='RELIABILITY'),
 'TPL-RELIABILITY-PLAN', '可靠性大纲', 'RELIABILITY',
 'F,C', 'GJB 450A-2004', 'RELIABILITY',
 '{"projectName":{"type":"string","label":"项目名称"},"mtbfTarget":{"type":"string","label":"MTBF目标值"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='SAFETY'),
 'TPL-SAFETY-PLAN', '安全性大纲', 'SAFETY',
 'F,C', 'GJB 900A-2012', 'SAFETY',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='A_SPEC'),
 'TPL-A-SPEC', '系统规范(A类)', 'A_SPEC',
 'F', 'GJB 6387-2008 Ch6', 'A_SPEC',
 '{"systemName":{"type":"string","label":"系统名称"},"systemCode":{"type":"string","label":"系统代号"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='B_SPEC'),
 'TPL-B-SPEC', '研制规范(B类)', 'B_SPEC',
 'F,C', 'GJB 6387-2008 Ch6-7', 'B_SPEC',
 '{"ciName":{"type":"string","label":"技术状态项名称"},"ciCode":{"type":"string","label":"技术状态项代号"}}', 'ACTIVE'),

-- ===== 初样/正样阶段模板 =====
((SELECT id FROM doc_template_category WHERE category_code='C_SPEC'),
 'TPL-C-SPEC', '产品规范(C类)', 'C_SPEC',
 'C,S', 'GJB 6387-2008 Ch6-7', 'C_SPEC',
 '{"productName":{"type":"string","label":"产品名称"},"productCode":{"type":"string","label":"产品代号"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='MANAGEMENT'),
 'TPL-DETAIL-DESIGN', '详细设计', 'MANAGEMENT',
 'C,S', 'GJB 2993-1997 5.5.1', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='MANAGEMENT'),
 'TPL-DEV-TEST-PLAN', '研制试验大纲', 'MANAGEMENT',
 'C,S', 'GJB 1362A-2007', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='MANAGEMENT'),
 'TPL-CHAR-ANALYSIS', '特性分析报告', 'MANAGEMENT',
 'C,S', 'GJB 190-1986', 'MANAGEMENT',
 '{"productName":{"type":"string","label":"产品名称"}}', 'ACTIVE'),

-- ===== 定型阶段模板 =====
((SELECT id FROM doc_template_category WHERE category_code='MANAGEMENT'),
 'TPL-DEV-SUMMARY', '研制总结', 'MANAGEMENT',
 'D', 'GJB/Z 170.4-2013', 'MANAGEMENT',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='TEST'),
 'TPL-FINAL-TEST-PLAN', '设计定型试验大纲', 'TEST',
 'D', 'GJB 1362A-2007', 'TEST',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='TEST'),
 'TPL-FINAL-TEST-REPORT', '设计定型试验报告', 'TEST',
 'D', 'GJB 1362A-2007', 'TEST',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE'),

-- ===== 软件文档模板 =====
((SELECT id FROM doc_template_category WHERE category_code='SOFTWARE'),
 'TPL-SW-SRS', '软件需求规格说明(SRS)', 'SOFTWARE',
 'F,C', 'GJB 438B-2009', 'SOFTWARE',
 '{"csciName":{"type":"string","label":"CSCI名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='SOFTWARE'),
 'TPL-SW-SDP', '软件开发计划(SDP)', 'SOFTWARE',
 'F,C', 'GJB 438B-2009', 'SOFTWARE',
 '{"projectName":{"type":"string","label":"项目名称"},"csciName":{"type":"string","label":"CSCI名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='SOFTWARE'),
 'TPL-SW-SDD', '软件设计说明(SDD)', 'SOFTWARE',
 'C,S', 'GJB 438B-2009', 'SOFTWARE',
 '{"csciName":{"type":"string","label":"CSCI名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='SOFTWARE'),
 'TPL-SW-STP', '软件测试计划(STP)', 'SOFTWARE',
 'C,S,D', 'GJB 438B-2009', 'SOFTWARE',
 '{"csciName":{"type":"string","label":"CSCI名称"}}', 'ACTIVE'),

((SELECT id FROM doc_template_category WHERE category_code='SOFTWARE'),
 'TPL-SW-SCMP', '软件配置管理计划(SCMP)', 'SOFTWARE',
 'F,C', 'GJB 5235-2004', 'SOFTWARE',
 '{"projectName":{"type":"string","label":"项目名称"}}', 'ACTIVE')

ON CONFLICT (template_code) DO NOTHING;
