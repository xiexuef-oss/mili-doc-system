-- ============================================================
-- V2 种子数据: 可靠性设计文件类别注册
-- 在 GJB 5882 15 大类基础上新增第 16 类: 可靠性设计文件
-- ============================================================

-- ===== 1. 阶段文档清单模板 =====
-- docCode 命名规则: REL-{TYPE}-{SEQ}
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num, spec_type, equipment_types)
VALUES
-- A档: 纯文本生成
('REL-OUTLINE-001', '可靠性大纲', '可靠性设计文件', 'REL', 'F,C,S', 'F', '可靠性设计师', true, 'GJB 450B-2021', '依据GJB 450B装备可靠性工作通用要求，定义装备可靠性工作项目、指标要求、验证方法和组织管理', '可靠性大纲,可靠性工作项目,GJB450B,301-313', 10, 'SYSTEM_SPEC', null),
('REL-DERATING-001', '降额设计报告', '可靠性设计文件', 'REL', 'C', 'C', '硬件设计师', true, 'GJB/Z 35', '依据GJB/Z 35元器件降额准则，对BOM清单中各器件进行降额审核并出具报告', '降额设计,元器件降额,GJB/Z35,电应力,热应力', 11, 'DEV_SPEC', null),
-- B档: 计算+文本混合
('REL-PRED-001', '可靠性预计报告', '可靠性设计文件', 'REL', 'C,S', 'C', '可靠性设计师', true, 'GJB/Z 299D-2024', '基于GJB/Z 299D元器件应力分析法，导入BOM逐器件查表计算失效率，输出MTBF预计结果', '可靠性预计,MTBF,失效率,GJB299D,应力分析法', 12, 'DEV_SPEC', null),
('REL-ALLOC-001', '可靠性分配报告', '可靠性设计文件', 'REL', 'F,C', 'F', '可靠性设计师', true, 'GJB 450B-2021 302', '将系统级可靠性指标逐级分配到分系统/设备/LRU，包含分配方法、参数和结果', '可靠性分配,AGREE,评分分配,指标分解', 13, 'SYSTEM_SPEC', null),
-- C档: 数据驱动（预留，本轮只注册不实现生成逻辑）
('REL-FMECA-001', 'FMECA报告', '可靠性设计文件', 'REL', 'C,S', 'C', '可靠性设计师', false, 'GJB/Z 1391', '故障模式影响及危害性分析报告，含功能FMECA和硬件FMECA', 'FMECA,故障模式,危害度,RPN,GJB1391', 14, 'DEV_SPEC', null),
('REL-FTA-001', '故障树分析报告', '可靠性设计文件', 'REL', 'C,S', 'C', '可靠性设计师', false, 'GJB/Z 768A', '故障树分析报告，含顶事件定义、故障树图、最小割集、顶事件概率', 'FTA,故障树,最小割集,MOCUS,GJB768A', 15, 'DEV_SPEC', null),
('REL-MODEL-001', '可靠性建模报告', '可靠性设计文件', 'REL', 'F,C', 'F', '可靠性设计师', false, 'GJB 813', '可靠性框图(RBD)建模报告，含可靠性数学模型和框图', '可靠性建模,RBD,可靠性框图,GJB813,串联,并联,表决', 16, 'SYSTEM_SPEC', null),
('REL-CRITICAL-001', '可靠性关键产品清单', '可靠性设计文件', 'REL', 'C,S', 'C', '可靠性设计师', false, 'GJB 450B-2021 310', '根据FMECA和预计结果综合识别可靠性关键产品，输出关键产品清单及控制措施', '可靠性关键产品,关键件,重要件,GJB450B', 17, 'SYSTEM_SPEC', null);

-- ===== 2. 文档模板 V2 =====
-- 为可立即生成的4份文档注册模板
INSERT INTO doc_template_v2 (category_id, template_code, template_name, template_type, applicable_stage_codes, applicable_project_type, gjb_standard_ref, document_class, status, version_no)
VALUES
(null, 'TPL-REL-OUTLINE', '可靠性大纲模板', 'RELIABILITY', 'F,C,S', null, 'GJB 450B-2021', '可靠性设计文件', 'ACTIVE', 1),
(null, 'TPL-REL-DERATING', '降额设计报告模板', 'RELIABILITY', 'C', null, 'GJB/Z 35', '可靠性设计文件', 'ACTIVE', 1),
(null, 'TPL-REL-PRED', '可靠性预计报告模板', 'RELIABILITY', 'C,S', null, 'GJB/Z 299D-2024', '可靠性设计文件', 'ACTIVE', 1),
(null, 'TPL-REL-ALLOC', '可靠性分配报告模板', 'RELIABILITY', 'F,C', null, 'GJB 450B-2021', '可靠性设计文件', 'ACTIVE', 1);

-- ===== 3. 模板章节结构（可靠性大纲 — 示例 20 章） =====
-- 先找到刚插入的模板ID (PostgreSQL 用 currval)
-- 使用子查询方式
INSERT INTO doc_template_chapter (template_id, parent_id, chapter_number, chapter_title, chapter_level, order_num, is_required, description, writing_tips, standard_clause_ref)
SELECT t.id, null, '1', '总则', 1, 1, true,
    '明确大纲的目的、适用范围和依据文件',
    '简述大纲编制目的，列出适用的装备型号/项目名称，引用GJB 450B、GJB 451A等依据标准',
    'GJB 450B-2021 4.1'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '2', '引用文件', 1, 2, true,
    '列出可靠性大纲引用的所有标准和文件',
    '按GJB 450B、GJB 451A、GJB 899A、GJB 841A等顺序列出，注明标准编号和名称',
    'GJB 450B-2021 3'
UNION ALL SELECT t.id, null, '3', '术语与定义', 1, 3, true,
    '定义大纲中使用的可靠性术语',
    '引用GJB 451A术语定义，补充项目特有术语，如MTBF、MTBCF、可靠度等',
    'GJB 451A'
UNION ALL SELECT t.id, null, '4', '可靠性要求', 1, 4, true,
    '明确装备的可靠性定量和定性要求',
    '列出MTBF目标值、可靠度R(t)、使用寿命、贮存可靠度、故障判据等具体指标',
    'GJB 450B-2021 3.2'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '5', '可靠性工作项目', 1, 5, true,
    '按GJB 450B 表1选定适用的可靠性工作项目',
    '根据装备类型和研制阶段，从100/200/300/400/500系列中选定工作项目，列表说明',
    'GJB 450B-2021 表1'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6', '可靠性设计与分析（300系列）', 1, 6, true,
    '详细描述各可靠性设计分析工作项目的实施方法',
    '逐项展开301可靠性建模、302可靠性分配、303可靠性预计、304 FMECA、305 FTA等',
    'GJB 450B-2021 300系列'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6.1', '可靠性建模（301）', 2, 7, true,
    '建立装备的可靠性模型（RBD框图+数学模型）',
    '描述模型建立方法、约定层次划分、可靠性逻辑关系。串联/并联/表决/旁联模型的选择依据',
    'GJB 450B-2021 301, GJB 813'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6.2', '可靠性分配（302）', 2, 8, true,
    '将系统级可靠性指标分配到下级产品',
    '说明选用的分配方法（等分配/评分分配/AGREE/ARINC），列出分配输入参数和各级结果',
    'GJB 450B-2021 302'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6.3', '可靠性预计（303）', 2, 9, true,
    '预计装备的可靠性水平，验证是否满足指标要求',
    '说明预计方法（应力分析法/计数法/相似产品法），列出预计结果和达标分析',
    'GJB 450B-2021 303, GJB/Z 299D-2024'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6.4', 'FMECA（304）', 2, 10, true,
    '故障模式影响及危害性分析实施计划',
    '说明FMECA的范围、约定层次、严重度类别定义、危害性分析方法（定量/定性）',
    'GJB 450B-2021 304, GJB/Z 1391'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6.5', 'FTA（305）', 2, 11, false,
    '故障树分析实施计划（按需）',
    '说明FTA分析范围、顶事件选取原则、分析方法',
    'GJB 450B-2021 305, GJB/Z 768A'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6.6', '降额设计（310）', 2, 12, true,
    '元器件降额设计准则和实施要求',
    '说明降额等级（Ⅰ/Ⅱ/Ⅲ级）、各类器件的降额参数和降额因子',
    'GJB 450B-2021 310, GJB/Z 35'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '6.7', '可靠性关键产品识别（313）', 2, 13, true,
    '识别并列出可靠性关键产品',
    '说明识别准则（故障后果严重/复杂度高/新技术/历史故障多），列出关键产品清单',
    'GJB 450B-2021 313'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '7', '可靠性试验与评价（400系列）', 1, 14, true,
    '可靠性试验计划和评价方法',
    '说明环境应力筛选(ESS)、可靠性增长试验、可靠性鉴定试验、可靠性验收试验的计划',
    'GJB 450B-2021 400系列, GJB 899A'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '8', '可靠性管理（100系列）', 1, 15, true,
    '可靠性工作组织管理、进度安排和经费保障',
    '明确可靠性工作组织架构、各阶段工作安排、FRACAS故障闭环管理、供应商可靠性管理',
    'GJB 450B-2021 100系列, GJB 841A'
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE'
UNION ALL SELECT t.id, null, '9', '附录', 1, 16, false,
    '补充材料',
    '可包含：可靠性工作项目剪裁说明、术语索引、引用标准清单等',
    null
FROM doc_template_v2 t WHERE t.template_code = 'TPL-REL-OUTLINE';

-- ===== 4. 四环节参考关联 =====
-- 先查出 checklist_template_id，再插入 doc_input_reference
DO $$
DECLARE
    v_outline_id BIGINT;
    v_derating_id BIGINT;
    v_pred_id BIGINT;
    v_alloc_id BIGINT;
BEGIN
    SELECT id INTO v_outline_id FROM stage_doc_checklist_template WHERE doc_code = 'REL-OUTLINE-001';
    SELECT id INTO v_derating_id FROM stage_doc_checklist_template WHERE doc_code = 'REL-DERATING-001';
    SELECT id INTO v_pred_id FROM stage_doc_checklist_template WHERE doc_code = 'REL-PRED-001';
    SELECT id INTO v_alloc_id FROM stage_doc_checklist_template WHERE doc_code = 'REL-ALLOC-001';

    -- 可靠性大纲 的参考（标准）
    IF v_outline_id IS NOT NULL THEN
        INSERT INTO doc_input_reference (checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num)
        VALUES
        (v_outline_id, 'STANDARD', 'GJB 450B-2021', '装备可靠性工作通用要求', '大纲编制依据，定义工作项目', true, 1),
        (v_outline_id, 'STANDARD', 'GJB 451A', '可靠性维修性保障性术语', '术语定义参考', true, 2),
        (v_outline_id, 'STANDARD', 'GJB 899A', '可靠性鉴定和验收试验', '试验方案参考', true, 3);
    END IF;

    -- 降额设计报告 的参考（标准 + 上游）
    IF v_derating_id IS NOT NULL THEN
        INSERT INTO doc_input_reference (checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num)
        VALUES
        (v_derating_id, 'STANDARD', 'GJB/Z 35', '元器件降额准则', '降额等级和降额因子依据', true, 1),
        (v_derating_id, 'UPSTREAM_DOC', 'REL-OUTLINE-001', '可靠性大纲', '获取降额设计要求和等级定义', true, 2);
    END IF;

    -- 可靠性预计报告 的参考（标准 + 上游）
    IF v_pred_id IS NOT NULL THEN
        INSERT INTO doc_input_reference (checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num)
        VALUES
        (v_pred_id, 'STANDARD', 'GJB/Z 299D-2024', '电子设备可靠性预计手册', '失效率数据来源和预计方法', true, 1),
        (v_pred_id, 'STANDARD', 'GJB 813', '可靠性模型的建立和可靠性预计', '预计报告格式要求', true, 2),
        (v_pred_id, 'UPSTREAM_DOC', 'REL-OUTLINE-001', '可靠性大纲', '获取MTBF目标值和环境条件', true, 3);
    END IF;

    -- 可靠性分配报告 的参考（标准 + 上游）
    IF v_alloc_id IS NOT NULL THEN
        INSERT INTO doc_input_reference (checklist_template_id, ref_type, ref_code, ref_name, ref_usage, is_required, order_num)
        VALUES
        (v_alloc_id, 'STANDARD', 'GJB 450B-2021 302', '装备可靠性工作通用要求-可靠性分配', '分配方法和要求', true, 1),
        (v_alloc_id, 'UPSTREAM_DOC', 'REL-OUTLINE-001', '可靠性大纲', '获取系统级MTBF指标', true, 2);
    END IF;
END $$;

-- ===== 5. 更新 stage_doc_checklist_template 关联 template_id =====
-- 将 checklist 条目关联到对应的 doc_template_v2
UPDATE stage_doc_checklist_template sct
SET template_id = t.id
FROM doc_template_v2 t
WHERE sct.doc_code = 'REL-OUTLINE-001' AND t.template_code = 'TPL-REL-OUTLINE';

UPDATE stage_doc_checklist_template sct
SET template_id = t.id
FROM doc_template_v2 t
WHERE sct.doc_code = 'REL-DERATING-001' AND t.template_code = 'TPL-REL-DERATING';

UPDATE stage_doc_checklist_template sct
SET template_id = t.id
FROM doc_template_v2 t
WHERE sct.doc_code = 'REL-PRED-001' AND t.template_code = 'TPL-REL-PRED';

UPDATE stage_doc_checklist_template sct
SET template_id = t.id
FROM doc_template_v2 t
WHERE sct.doc_code = 'REL-ALLOC-001' AND t.template_code = 'TPL-REL-ALLOC';
