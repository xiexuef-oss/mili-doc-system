INSERT INTO doc_template_v2 (category_id, template_code, template_name, template_type, applicable_stage_codes, gjb_standard_ref, document_class, status, version_no)
VALUES 
(11, 'TPL-REL-OUTLINE', '可靠性大纲模板', 'RELIABILITY', 'F,C,S', 'GJB 450B-2021', '可靠性设计文件', 'ACTIVE', 1),
(11, 'TPL-REL-DERATING', '降额设计报告模板', 'RELIABILITY', 'C', 'GJB/Z 35', '可靠性设计文件', 'ACTIVE', 1),
(11, 'TPL-REL-PRED', '可靠性预计报告模板', 'RELIABILITY', 'C,S', 'GJB/Z 299D-2024', '可靠性设计文件', 'ACTIVE', 1),
(11, 'TPL-REL-ALLOC', '可靠性分配报告模板', 'RELIABILITY', 'F,C', 'GJB 450B-2021', '可靠性设计文件', 'ACTIVE', 1)
ON CONFLICT DO NOTHING;

-- Update checklist templates to point to these (template_id was null from earlier)
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-REL-OUTLINE') WHERE doc_code = 'REL-OUTLINE-001' AND template_id IS NULL;
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-REL-DERATING') WHERE doc_code = 'REL-DERATING-001' AND template_id IS NULL;
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-REL-PRED') WHERE doc_code = 'REL-PRED-001' AND template_id IS NULL;
UPDATE stage_doc_checklist_template SET template_id = (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-REL-ALLOC') WHERE doc_code = 'REL-ALLOC-001' AND template_id IS NULL;

-- Insert chapters for outline template
WITH tpl AS (SELECT id FROM doc_template_v2 WHERE template_code = 'TPL-REL-OUTLINE')
INSERT INTO doc_template_chapter (template_id, chapter_number, chapter_title, chapter_level, order_num, is_required, description, writing_tips, standard_clause_ref)
SELECT tpl.id, v.c, v.t, v.l, v.o, v.r, v.d, v.w, v.s
FROM tpl,
(VALUES
 ('1','总则',1,1,true,'明确大纲的目的、适用范围和依据文件','简述大纲编制目的，列出适用装备型号/项目名称，引用GJB 450B、GJB 451A等依据标准','GJB 450B-2021 4.1'),
 ('2','引用文件',1,2,true,'列出可靠性大纲引用的所有标准和文件','按GJB 450B、GJB 451A、GJB 899A、GJB 841A等顺序列出，注明标准编号和名称','GJB 450B-2021 3'),
 ('3','术语与定义',1,3,true,'定义大纲中使用的可靠性术语','引用GJB 451A术语定义，补充项目特有术语','GJB 451A'),
 ('4','可靠性要求',1,4,true,'明确装备的可靠性定量和定性要求','列出MTBF目标值、可靠度R(t)、使用寿命、贮存可靠度、故障判据等','GJB 450B-2021 3.2'),
 ('5','可靠性工作项目',1,5,true,'按GJB 450B 表1选定适用的可靠性工作项目','根据装备类型和研制阶段，从100/200/300/400/500系列中选定工作项目，列表说明','GJB 450B-2021 表1'),
 ('6','可靠性设计与分析(300系列)',1,6,true,'详细描述各可靠性设计分析工作项目的实施方法','逐项展开301~313各工作项目','GJB 450B-2021 300系列'),
 ('6.1','可靠性建模(301)',2,7,true,'建立装备的可靠性模型(RBD框图+数学模型)','描述模型建立方法、约定层次划分、可靠性逻辑关系','GJB 450B-2021 301, GJB 813'),
 ('6.2','可靠性分配(302)',2,8,true,'将系统级可靠性指标分配到下级产品','说明选用的分配方法，列出分配输入参数和各级结果','GJB 450B-2021 302'),
 ('6.3','可靠性预计(303)',2,9,true,'预计装备的可靠性水平，验证是否满足指标要求','说明预计方法，列出预计结果和达标分析','GJB 450B-2021 303, GJB/Z 299D-2024'),
 ('6.4','FMECA(304)',2,10,true,'故障模式影响及危害性分析实施计划','说明FMECA的范围、约定层次、严重度类别定义','GJB 450B-2021 304, GJB/Z 1391'),
 ('6.5','FTA(305)',2,11,false,'故障树分析实施计划(按需)','说明FTA分析范围、顶事件选取原则','GJB 450B-2021 305, GJB/Z 768A'),
 ('6.6','降额设计(310)',2,12,true,'元器件降额设计准则和实施要求','说明降额等级(I/II/III级)、各类器件的降额参数','GJB 450B-2021 310, GJB/Z 35'),
 ('6.7','可靠性关键产品识别(313)',2,13,true,'识别并列出可靠性关键产品','说明识别准则，列出关键产品清单','GJB 450B-2021 313'),
 ('7','可靠性试验与评价(400系列)',1,14,true,'可靠性试验计划和评价方法','说明ESS、可靠性增长试验、鉴定试验、验收试验的计划','GJB 450B-2021 400系列, GJB 899A'),
 ('8','可靠性管理(100系列)',1,15,true,'可靠性工作组织管理、进度安排和经费保障','明确组织架构、FRACAS故障闭环管理、供应商管理','GJB 450B-2021 100系列, GJB 841A'),
 ('9','附录',1,16,false,'补充材料','可包含：可靠性工作项目剪裁说明、术语索引、引用标准清单等',null)
) AS v(c, t, l, o, r, d, w, s)
ON CONFLICT DO NOTHING;
