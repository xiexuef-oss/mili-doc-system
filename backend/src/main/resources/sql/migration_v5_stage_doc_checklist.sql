-- ============================================================
-- V5: 阶段文档清单模板库 (Stage Document Checklist Template Library)
-- 参考: GJB 军工产品研制技术文件编写指南 + 编写说明
-- ============================================================

-- V5.1: Add checklist_item_id to doc_ledger for checklist→ledger sync
ALTER TABLE doc_ledger ADD COLUMN IF NOT EXISTS checklist_item_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_doc_ledger_checklist ON doc_ledger(checklist_item_id);

-- 阶段文档清单模板表
CREATE TABLE IF NOT EXISTS stage_doc_checklist_template (
    id              BIGSERIAL PRIMARY KEY,
    doc_code        VARCHAR(64)  NOT NULL,          -- 文档编号, e.g. "GP-01"
    doc_name        VARCHAR(256) NOT NULL,          -- 文档名称
    category        VARCHAR(64)  NOT NULL,          -- 15大类: 一般过程文件/软件文档/工艺文件/标准化文件/质量文件/风险管理文件/可靠性文件/维修性文件/测试性文件/保障性文件/安全性文件/环境适应性文件/电磁兼容性文件/人机工程文件/项目成果文件
    category_code   VARCHAR(32),                    -- 类别代码: GP/SW/PR/ST/QA/RM/RE/MA/TE/SU/SA/EN/EM/HE/PO
    applicable_stages VARCHAR(512) NOT NULL,        -- 适用阶段(JSON数组): ["ARGUMENTATION","SCHEME"]
    primary_stage   VARCHAR(64),                    -- 主要编制阶段
    responsibility  VARCHAR(64),                    -- 责任单位: 订购方/承研承制方/军事代表机构/承试方/试用部队/审查组/总体单位
    required_flag   BOOLEAN DEFAULT TRUE,           -- 是否必编(true=√, false=△根据需要)
    gjb_reference   VARCHAR(512),                   -- 参考标准, e.g. "GJB 1362A-2007"
    description     TEXT,                           -- 文件用途说明
    keywords        VARCHAR(512),                   -- 关键词(逗号分隔，用于搜索)
    order_num       INT DEFAULT 0,                  -- 排序号
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 项目文档清单表 (per-project instantiated from template)
CREATE TABLE IF NOT EXISTS project_doc_checklist (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    stage_id            BIGINT,                      -- 关联project_stage.id
    stage_code          VARCHAR(64),                 -- 阶段代码
    template_id         BIGINT,                      -- 关联stage_doc_checklist_template.id
    doc_name            VARCHAR(256) NOT NULL,
    category            VARCHAR(64),
    category_code       VARCHAR(32),
    doc_status          VARCHAR(32) DEFAULT 'NOT_STARTED',  -- NOT_STARTED/IN_PROGRESS/DRAFT/REVIEW/APPROVED
    responsible_person  VARCHAR(64),
    planned_date        DATE,
    completed_date      DATE,
    file_id             BIGINT,                      -- 关联doc_file.id
    sort_order          INT DEFAULT 0,
    is_custom           BOOLEAN DEFAULT FALSE,       -- 是否用户自定义添加
    notes               TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sdct_category ON stage_doc_checklist_template(category_code);
CREATE INDEX IF NOT EXISTS idx_pdc_project ON project_doc_checklist(project_id);
CREATE INDEX IF NOT EXISTS idx_pdc_stage ON project_doc_checklist(stage_id);

-- ============================================================
-- 种子数据: 从GJB编写指南提取的全部文档类型 (15大类, ~190+文档)
-- 适用阶段: ARGUMENTATION=论证, SCHEME=方案, PROTOTYPE=初样(工程研制),
--           FORMAL_SAMPLE=正样(工程研制), FINALIZATION=定型, PRODUCTION=生产, MAINTENANCE=使用维护
-- ============================================================

-- 阶段简写: ARG=ARGUMENTATION, SCH=SCHEME, PT=PROTOTYPE, FS=FORMAL_SAMPLE, FIN=FINALIZATION, PRD=PRODUCTION, MNT=MAINTENANCE

-- ========================================
-- 1. 一般过程文件 (GP) - 约66个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('GP-01', '研制立项综合论证报告', '一般过程文件', 'GP', '["ARGUMENTATION"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 4054-2000', '论证发展新型武器装备的必要性、作战使命任务、初步总体技术方案、主要作战使用性能指标、效能评估、进度要求、订购数量预测、寿命周期费用、风险分析', '论证,立项,需求分析,效能评估', 1),
('GP-02', '招标书', '一般过程文件', 'GP', '["ARGUMENTATION"]', 'ARGUMENTATION', '订购方', FALSE, '武器装备研制项目招标管理办法', '提出研制项目名称、内容要求、进度要求、成果形式、技术方案要求、投标报价构成细目、递标要求', '招标,投标,采购', 2),
('GP-03', '投标书', '一般过程文件', 'GP', '["ARGUMENTATION"]', 'ARGUMENTATION', '承研承制方', FALSE, '武器装备研制项目招标管理办法', '对招标书的应答,包括投标函、投标方案、投标报价、研制周期等技术管理内容', '投标,招标,方案', 3),
('GP-04', '研制总要求', '一般过程文件', 'GP', '["ARGUMENTATION"]', 'ARGUMENTATION', '订购方', TRUE, '中国人民解放军装备条例', '提出作战使命任务、主要作战使用性能指标、初步总体方案、研制周期和经费概算,是后续研制的基本依据', '研制总要求,战术技术指标,装备条例', 4),
('GP-05', '研制总要求论证工作报告', '一般过程文件', 'GP', '["ARGUMENTATION"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 4054-2000', '论证工作报告对研制总要求的论证过程和依据进行说明', '论证工作报告,论证依据', 5),
('GP-06', '技术风险评估报告', '一般过程文件', 'GP', '["ARGUMENTATION","SCHEME"]', 'ARGUMENTATION', '承研承制方', FALSE, 'GJB 5852-2006', '对产品研制的技术风险、进度风险和费用风险进行分析评估', '风险评估,技术风险,进度风险,费用风险', 6),
('GP-07', '经费概算报告', '一般过程文件', 'GP', '["ARGUMENTATION","SCHEME"]', 'ARGUMENTATION', '承研承制方', TRUE, '国防科研试制费管理规定', '对研制经费进行概算,包括设计费、材料费、设备费、外协费、试验费等', '经费概算,费用,预算', 7),
('GP-08', '可行性论证报告', '一般过程文件', 'GP', '["ARGUMENTATION"]', 'ARGUMENTATION', '承研承制方', TRUE, 'GJB 4054-2000', '对技术经济可行性进行研究及必要的验证试验,提出可达到的战术技术指标和初步总体方案', '可行性论证,技术可行性,经济可行性', 8),
('GP-09', '研制合同', '一般过程文件', 'GP', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '订购方+承研承制方', TRUE, '武器装备研制合同暂行办法', '明确研制工作范围、交付物、进度安排、经费、验收标准等', '合同,交付物,验收标准', 9),
('GP-10', '标准化大纲', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB/Z 114A-2005', '指导产品研制标准化工作的基本文件,规定标准化目标和要求', '标准化大纲,标准化目标', 10),
('GP-11', '质量保证大纲', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 1406A-2005', '明确为满足质量要求所开展的活动及其风险,对采购研制生产和售后服务的质量控制作出规定', '质量大纲,质量保证,质量控制', 11),
('GP-12', '系统规范(A类规范)', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 6387-2008', '规定武器装备系统的功能特性、性能特性、接口要求和验证要求,建立功能基线(FBL)', '系统规范,A类规范,功能基线', 12),
('GP-13', '研制规范(B类规范)', '一般过程文件', 'GP', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 6387-2008', '规定技术状态项的性能特性、物理特性和接口要求,建立分配基线(ABL)', '研制规范,B类规范,分配基线', 13),
('GP-14', '研制计划', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 2993-1997', '对产品研制过程中所有工作进行计划,明确责任界限和进度安排', '研制计划,项目管理,进度', 14),
('GP-15', '生产性分析报告(方案阶段)', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', FALSE, 'GJB 3363-1998', '对拟采取的研制方案进行生产性分析', '生产性分析,可生产性', 15),
('GP-16', '研制方案', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, '--', '为完成产品研制在技术上进行组织规划,指导工程研制工作', '研制方案,技术组织规划', 16),
('GP-17', '技术状态管理计划', '一般过程文件', 'GP', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 3206A-2010', '说明对技术状态项的功能特性和物理特性进行管理所采用的程序和方法', '技术状态管理,配置管理,基线', 17),
('GP-18', '接口控制文件', '一般过程文件', 'GP', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 2737-1996', '规定系统、分系统、设备之间功能和物理接口要求', '接口控制,接口要求', 18),
('GP-19', '试验与评定总计划', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, '--', '列出试验的目标、进度和所需资源,是关键的试验规划文件', '试验计划,评定,试验目标', 19),
('GP-20', '研制任务书', '一般过程文件', 'GP', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, '--', '为开展产品研制项目下达工作任务,是产品方案设计、工程研制和设计定型的依据', '研制任务书,工作任务', 20),
('GP-21', '详细设计', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, '--', '对产品进行详细设计的输出文件,提供设计评审', '详细设计,设计评审,工程研制', 21),
('GP-22', '设计计算报告', '一般过程文件', 'GP', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, '--', '对有关技术问题进行设计计算,提供方案可行性和样机性能数据', '设计计算,计算报告,性能数据', 22),
('GP-23', '特性分析报告', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 190-1986', '对产品特性实施分类,提高设计质量,实施质量控制和检查监督', '特性分析,特性分类,质量控制', 23),
('GP-24', '生产性分析报告(工程研制阶段)', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3363-1998', '标识所有硬件的关键特性,减少生产流程时间和成本,改善检验和试验程序', '生产性分析,生产流程,成本', 24),
('GP-25', '研制试验大纲', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 1362A-2007', '规范研制试验的项目、内容和方法等', '研制试验,试验大纲', 25),
('GP-26', '研制试验报告', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 1362A-2007', '报告研制试验情况和试验结果', '研制试验,试验报告,试验结果', 26),
('GP-27', '验收测试规范(ATS)', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, '--', '提出产品的测试环境、测试方法和测试项目,是编写验收测试程序(ATP)的基础', '验收测试,测试规范,ATS', 27),
('GP-28', '验收测试程序(ATP)', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, '--', '规定产品的测试环境、测试项目和测试步骤', '验收测试,测试程序,ATP', 28),
('GP-29', '产品规范(C类规范)', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 6387-2008', '规定系统级以下项目的主要功能要求、性能要求、制造要求和验收要求', '产品规范,C类规范,验收要求', 29),
('GP-30', '技术说明书', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 4771-1997', '为用户提供详细的、全面的产品信息,指导用户准确理解和正确使用维护产品', '技术说明书,使用维护', 30),
('GP-31', '使用维护说明书', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, '--', '为用户提供产品使用维护信息,指导正确使用维护产品', '使用维护说明书,操作手册', 31),
('GP-32', '改装方案', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 151A-1997', '为完成产品在平台上的加改装科研工作在技术上进行组织规划', '改装方案,加改装,电磁兼容', 32),
('GP-33', '设计定型试验申请报告', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承研承制方+军事代表机构', TRUE, 'GJB 1362A-2007', '向二级定委提出设计定型试验书面申请', '设计定型,试验申请', 33),
('GP-34', '设计定型试验大纲', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 1362A-2007', '规范设计定型试验的项目、内容和方法,全面考核战术技术指标和作战使用要求', '设计定型,试验大纲,考核指标', 34),
('GP-35', '设计定型试验大纲(部队试验)', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 6177-2007', '规范部队试验的项目、内容和方法,考核作战使用性能和部队适用性', '部队试验,试验大纲,作战使用', 35),
('GP-36', '设计定型试验大纲编制说明', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 1362A-2007', '说明编制依据、过程、确定主要内容的理由、试验项目剪裁依据和理由', '编制说明,剪裁依据,大纲', 36),
('GP-37', '设计定型试验报告', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 1362A-2007', '报告设计定型试验情况和试验结果,作为产品设计定型的依据', '试验报告,设计定型,试验结果', 37),
('GP-38', '设计定型试验报告(部队试验)', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '试用部队', TRUE, 'GJB 6178-2007', '反映部队试验执行情况,对产品作出科学、客观、公正、完整的评价', '部队试验报告,评价', 38),
('GP-39', '重大技术问题的技术攻关报告', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 1362A-2007', '对产品研制过程中出现的重大技术问题及其解决情况进行记录和报告', '技术攻关,重大技术问题,问题解决', 39),
('GP-40', '质量问题报告', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '军事代表机构', FALSE, 'GJB 1362A-2007', '报告产品研制过程中出现的重大技术问题及其解决情况', '质量问题,归零,改进', 40),
('GP-41', '价值工程和成本分析报告(设计定型)', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 1364-1992', '报告产品研制成本、生产成本估算和寿命周期费用分析结果', '价值工程,成本分析,寿命周期费用', 41),
('GP-42', '生产性分析报告(设计定型阶段)', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 3363-1998', '在设计定型阶段对设计图样和产品规范进行较大修改时进行生产性分析', '生产性分析,设计定型', 42),
('GP-43', '改装总结', '一般过程文件', 'GP', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 1362A-2007', '对产品科研改装全过程进行全面总结', '改装总结,科研改装', 43),
('GP-44', '研制总结(设计定型用)', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 1362A-2007', '对产品研制工作全过程进行系统综述、全面总结的结论性文件', '研制总结,设计定型,结论', 44),
('GP-45', '设计定型录像片解说词', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, '产品定型录像片制作要求', '定型录像片解说词文本,实事求是、简明扼要、通俗易懂', '录像片,解说词,定型', 45),
('GP-46', '总体单位对设计定型的意见', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '总体单位', TRUE, 'GJB 1362A-2007', '总体单位对军工产品能否设计定型提出明确意见', '总体单位,设计定型意见', 46),
('GP-47', '军事代表机构对设计定型的意见', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '军事代表机构', TRUE, 'GJB 1362A-2007', '军事代表机构对军工产品能否设计定型提出明确意见', '军事代表,设计定型意见', 47),
('GP-48', '设计定型申请报告', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '承研承制方+军事代表机构', TRUE, 'GJB 1362A-2007', '向二级定委提出设计定型书面申请', '申请报告,设计定型申请', 48),
('GP-49', '设计定型审查意见书', '一般过程文件', 'GP', '["FINALIZATION"]', 'FINALIZATION', '审查组', TRUE, 'GJB 1362A-2007', '全面评定产品是否符合设计定型标准和要求的总结性文件', '审查意见书,设计定型审查', 49),
('GP-50', '部队试用申请报告', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承研承制方+军事代表机构', FALSE, 'GJB 1362A-2007', '向上级机关提出对产品开展部队试用工作的申请', '部队试用,申请,生产定型', 50),
('GP-51', '部队试用大纲', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '试用部队', TRUE, 'GJB 1362A-2007', '规定部队试用工作内容,全面考核产品的作战使用要求和部队适应性', '部队试用大纲,适应性考核', 51),
('GP-52', '部队试用大纲编制说明', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '试用部队', TRUE, 'GJB 6177-2007', '说明编制依据、过程、确定主要内容的理由、试用项目剪裁依据', '编制说明,部队试用,剪裁', 52),
('GP-53', '部队试用报告', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '试用部队', TRUE, 'GJB 1362A-2007', '全面反映部队试用执行情况,对产品作出科学、客观、公正、完整的评价', '部队试用报告,评价', 53),
('GP-54', '技术状态更改建议', '一般过程文件', 'GP', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3206A-2010', '说明对技术状态项更改的范围、理由、内容、方案及其影响', '技术状态更改,更改控制', 54),
('GP-55', '偏离(超差)申请', '一般过程文件', 'GP', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3206A-2010', '说明偏离或超差的范围、理由、内容及其影响', '偏离申请,超差申请', 55),
('GP-56', '技术通报', '一般过程文件', 'GP', '["MAINTENANCE"]', 'MAINTENANCE', '承研承制方', FALSE, 'GJB 4757-1997', '用于对已出厂武器装备进行排故、检查、改装以及其他重要技术事项', '技术通报,排故,检查,改装', 56),
('GP-57', '生产定型试验申请报告', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承研承制方+军事代表机构', FALSE, 'GJB 1362A-2007', '向二级定委提出生产定型试验书面申请', '生产定型,试验申请', 57),
('GP-58', '生产定型试验大纲', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承试方', FALSE, 'GJB 1362A-2007', '规范生产定型试验的项目、内容和方法', '生产定型,试验大纲', 58),
('GP-59', '生产定型试验报告', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承试方', FALSE, 'GJB 1362A-2007', '报告生产定型试验情况和试验结果,作为产品生产定型的依据', '生产定型,试验报告', 59),
('GP-60', '价值工程分析和成本核算报告', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, 'GJB 1364-1992', '报告产品研制成本、生产成本核算和寿命周期费用分析结果', '价值工程,成本核算,寿命周期费用', 60),
('GP-61', '生产性分析报告(生产定型阶段)', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', FALSE, 'GJB 3363-1998', '在生产定型阶段对产品生产性进行全面分析', '生产性分析,生产定型', 61),
('GP-62', '试生产总结', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, 'GJB 1362A-2007', '对产品试生产工作全过程进行系统综述、全面总结', '试生产总结,生产定型', 62),
('GP-63', '生产定型录像片解说词', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, '产品定型录像片制作要求', '生产定型录像片解说词', '录像片,解说词,生产定型', 63),
('GP-64', '军事代表机构对生产定型的意见', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '军事代表机构', TRUE, 'GJB 1362A-2007', '军事代表机构对军工产品能否生产定型提出明确意见', '军事代表,生产定型意见', 64),
('GP-65', '生产定型申请报告', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '承研承制方+军事代表机构', TRUE, 'GJB 1362A-2007', '向二级定委申请生产定型', '申请报告,生产定型申请', 65),
('GP-66', '生产定型审查意见书', '一般过程文件', 'GP', '["PRODUCTION"]', 'PRODUCTION', '审查组', TRUE, 'GJB 1362A-2007', '全面评定产品是否符合生产定型标准和要求的总结性文件', '审查意见书,生产定型审查', 66);

-- ========================================
-- 2. 软件文档 (SW) - 30个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('SW-01', '运行方案说明(OCD)', '软件文档', 'SW', '["ARGUMENTATION","SCHEME"]', 'ARGUMENTATION', '承研承制方', TRUE, 'GJB 438B-2009', '描述系统应满足的用户需要、与现有系统或规程的关系以及使用方式', '运行方案,OCD,用户需求', 100),
('SW-02', '系统/子系统规格说明(SSS)', '软件文档', 'SW', '["ARGUMENTATION","SCHEME"]', 'ARGUMENTATION', '承研承制方', TRUE, 'GJB 438B-2009', '描述系统的需求以及确保满足各需求所使用的方法,构成系统设计与合格性测试的基础', '系统规格说明,SSS,需求分析', 101),
('SW-03', '接口需求规格说明(IRS)', '软件文档', 'SW', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 438B-2009', '描述作用于系统/子系统/配置项/人工操作之间的需求', '接口需求,IRS,接口规范', 102),
('SW-04', '系统/子系统设计说明(SSDD)', '软件文档', 'SW', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 438B-2009', '描述系统/子系统的系统级或子系统级设计决策与体系结构设计', '系统设计,SSDD,体系结构', 103),
('SW-05', '接口设计说明(IDD)', '软件文档', 'SW', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 438B-2009', '描述系统/子系统/配置项/人工操作之间的接口设计特性', '接口设计,IDD,接口特性', 104),
('SW-06', '软件研制任务书(SDTD)', '软件文档', 'SW', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 438B-2009', '描述软件开发的目的、目标、主要任务、功能及性能指标等要求', '软件研制任务书,SDTD', 105),
('SW-07', '软件开发计划(SDP)', '软件文档', 'SW', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 438B-2009', '描述实施软件开发工作的计划,包含新开发、修改、重用、再工程、维护等活动', '软件开发计划,SDP', 106),
('SW-08', '软件配置管理计划(SCMP)', '软件文档', 'SW', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 438B-2009', '描述在项目中如何实施软件配置管理', '配置管理,SCMP', 107),
('SW-09', '软件质量保证计划(SQAP)', '软件文档', 'SW', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 438B-2009', '描述在项目中采用的软件质量保证的措施、方法和步骤', '质量保证,SQAP', 108),
('SW-10', '软件安装计划(SIP)', '软件文档', 'SW', '["SCHEME","PROTOTYPE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 438B-2009', '描述在用户的现场安装软件的计划', '软件安装,SIP,部署', 109),
('SW-11', '软件移交计划(STrP)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 438B-2009', '描述开发方向保障机构移交应交付项的计划', '软件移交,STrP', 110),
('SW-12', '软件测试计划(STP)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 438B-2009', '描述对CSCI和软件系统或子系统进行合格性测试的计划', '软件测试计划,STP', 111),
('SW-13', '软件需求规格说明(SRS)', '软件文档', 'SW', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 438B-2009', '描述对CSCI的需求及确保满足每个需求所使用的方法,构成设计与测试基础', '软件需求,SRS,需求规格', 112),
('SW-14', '软件设计说明(SDD)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 438B-2009', '描述CSCI的设计,将软件需求转化为对软件结构、部件、接口和数据的描述', '软件设计,SDD,详细设计', 113),
('SW-15', '数据库设计说明(DBDD)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 438B-2009', '描述数据库的设计以及存取或操纵数据所使用的软件单元', '数据库设计,DBDD', 114),
('SW-16', '软件测试说明(STD)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 438B-2009', '描述执行合格性测试所需的测试准备、测试用例及测试过程', '软件测试说明,STD,测试用例', 115),
('SW-17', '软件测试报告(STR)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 438B-2009', '对软件进行合格性测试的记录,为纠正缺陷提供依据', '软件测试报告,STR,测试记录', 116),
('SW-18', '软件产品规格说明(SPS)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 438B-2009', '描述或引用可执行软件、源文件以及软件保障信息', '软件产品规格,SPS', 117),
('SW-19', '软件版本说明(SVD)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 438B-2009', '标识并描述由一个或多个CSCI组成的软件版本,用于发布、追踪和控制', '软件版本,SVD,版本管理', 118),
('SW-20', '软件用户手册(SUM)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 438B-2009', '描述操作该软件的用户如何安装和使用CSCI', '用户手册,SUM', 119),
('SW-21', '软件输入/输出手册(SIOM)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 438B-2009', '为集中式或网络化安装场所的软件系统而编制,用户通过终端访问系统', '输入输出手册,SIOM', 120),
('SW-22', '软件中心操作员手册(SCOM)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 438B-2009', '为计算机中心工作人员提供如何安装和操作软件系统的信息', '操作员手册,SCOM', 121),
('SW-23', '计算机编程手册(CPM)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 438B-2009', '为程序员描述对指定计算机进行编程所需要的信息', '编程手册,CPM', 122),
('SW-24', '计算机操作手册(COM)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 438B-2009', '描述操作指定的计算机及其外部设备所需的信息', '操作手册,COM', 123),
('SW-25', '固件保障手册(FSM)', '软件文档', 'SW', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 438B-2009', '描述对系统的固件设备进行编程和再编程所需的信息', '固件保障,FSM', 124),
('SW-26', '软件研制总结报告(SDSR)', '软件文档', 'SW', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 438B-2009', '描述软件整个研制/开发情况,是产品设计定型的依据之一', '软件研制总结,SDSR', 125),
('SW-27', '软件配置管理报告(SCMR)', '软件文档', 'SW', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 438B-2009', '描述软件整个研制/开发过程中软件配置管理情况', '配置管理报告,SCMR', 126),
('SW-28', '软件质量保证报告(SQAR)', '软件文档', 'SW', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 438B-2009', '描述软件整个研制/开发过程中软件质量保证情况', '质量保证报告,SQAR', 127),
('SW-29', '软件定型测评大纲', '软件文档', 'SW', '["FINALIZATION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 6921-2009', '规范软件定型测评的项目、内容和方法', '软件定型测评,测评大纲', 128),
('SW-30', '软件定型测评报告', '软件文档', 'SW', '["FINALIZATION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 6922-2009', '报告软件鉴定测评情况和测评结果,作为产品设计定型的依据', '软件定型测评,测评报告', 129);

-- ========================================
-- 3. 工艺文件 (PR) - 7个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('PR-01', '工艺总方案', '工艺文件', 'PR', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 1269A-2000', '产品工艺技术准备工作的重要依据和指导性工艺文件', '工艺总方案,工艺准备', 200),
('PR-02', '工艺规范(D类规范)', '工艺文件', 'PR', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 6387-2008', '规定制造时对产品或材料实施某种工艺作业的方法', '工艺规范,D类规范', 201),
('PR-03', '材料规范(E类规范)', '工艺文件', 'PR', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 6387-2008', '规定产品制造中使用的原材料、混合物或半成品', '材料规范,E类规范', 202),
('PR-04', '工艺设计工作总结', '工艺文件', 'PR', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, '--', '对产品的工艺设计工作进行全面和系统的总结,用于产品工艺评审', '工艺设计,工作总结', 203),
('PR-05', '工艺评审报告', '工艺文件', 'PR', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '评审组', TRUE, 'GJB 1269A-2000', '对产品研制生产采取的工艺进行评审把关,为批准工艺设计提供决策性咨询', '工艺评审,评审报告', 204),
('PR-06', '工艺总结', '工艺文件', 'PR', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, '--', '对研制过程中的工艺工作进行全面和系统的总结', '工艺总结', 205),
('PR-07', '工艺和生产条件考核报告', '工艺文件', 'PR', '["PRODUCTION"]', 'PRODUCTION', '订购方', TRUE, '--', '评估承研承制单位产品工艺和生产条件是否稳定并满足批量生产条件', '工艺考核,生产条件考核', 206);

-- ========================================
-- 4. 标准化文件 (ST) - 6个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('ST-01', '标准化大纲', '标准化文件', 'ST', '["SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB/Z 114A-2005', '指导产品研制标准化工作的基本文件,按系统/分系统/设备不同层次分别编制', '标准化大纲,标准化要求', 300),
('ST-02', '标准化工作报告', '标准化文件', 'ST', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB/Z 113-1998', '各研制阶段执行标准化工作规定和实施标准化大纲情况的总结性报告', '标准化工作报告,标准化评审', 301),
('ST-03', '标准化审查报告', '标准化文件', 'ST', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, '--', '反映和评价产品研制过程中标准化大纲的实施情况和产品标准化程度与水平', '标准化审查,标准化水平', 302),
('ST-04', '工艺标准化大纲(工艺标准化综合要求)', '标准化文件', 'ST', '["PROTOTYPE","FORMAL_SAMPLE","PRODUCTION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB/Z 106A-2005', '规定样机试制/生产定型工艺标准化要求,指导工艺标准化工作', '工艺标准化,标准化大纲', 303),
('ST-05', '工艺标准化工作报告', '标准化文件', 'ST', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB/Z 113-1998', '产品研制过程中各阶段执行标准化工作规定和实施工艺标准化大纲情况的总结', '工艺标准化,工作报告', 304),
('ST-06', '工艺标准化审查报告', '标准化文件', 'ST', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, '--', '反映和评价产品生产过程中工艺标准化大纲的实施情况和工艺标准化水平', '工艺标准化审查', 305);

-- ========================================
-- 5. 质量文件 (QA) - 4个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('QA-01', '质量保证大纲(质量计划)', '质量文件', 'QA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 1406A-2005', '明确为满足质量要求所开展的活动及风险,对采购研制生产和售后服务的质量控制作出规定', '质量保证大纲,质量计划', 400),
('QA-02', '质量分析报告', '质量文件', 'QA', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 907A-2006', '对研制产品的质量及其质量保证工作进行全面和系统的分析,用于产品质量评审', '质量分析,质量评审', 401),
('QA-03', '配套产品/原材料/元器件/检测设备质量和定点供应情况', '质量文件', 'QA', '["FINALIZATION","PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, '--', '报告配套产品、原材料、元器件及检测设备的质量和定点供应情况', '配套产品质量,定点供应', 402),
('QA-04', '质量管理报告', '质量文件', 'QA', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, 'GJB 907A-2006', '对产品的生产质量及其质量保证工作进行全面和系统的总结', '质量管理报告,质量总结', 403);

-- ========================================
-- 6. 风险管理文件 (RM) - 6个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('RM-01', '风险管理计划', '风险管理文件', 'RM', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'ARGUMENTATION', '承研承制方', TRUE, 'GJB 2993-1997', '具体规定为了满足订购方的要求所需要进行的风险管理工作', '风险管理,风险计划', 500),
('RM-02', '风险分析报告', '风险管理文件', 'RM', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 5852-2006', '对产品研制的技术风险、进度风险和费用风险等进行分析', '风险分析,技术风险,进度风险', 501),
('RM-03', '技术成熟度评价工作计划', '风险管理文件', 'RM', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', FALSE, '--', '具体规定技术成熟度评价工作的组织和计划进度', '技术成熟度,评价计划', 502),
('RM-04', '关键技术元素(初始候选)清单', '风险管理文件', 'RM', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', FALSE, '--', '列出技术成熟度评价的对象', '关键技术元素,技术成熟度', 503),
('RM-05', '技术成熟度评价报告', '风险管理文件', 'RM', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, '--', '展示项目技术成熟度评价过程和评价结果', '技术成熟度评价,成熟度等级', 504),
('RM-06', '技术成熟计划', '风险管理文件', 'RM', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, '--', '列出不成熟关键技术元素改进到所需技术成熟度等级时需要的活动', '技术成熟计划,风险降低', 505);

-- ========================================
-- 7. 可靠性文件 (RE) - 32个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('RE-01', '可靠性要求', '可靠性文件', 'RE', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '订购方', TRUE, 'GJB 450A-2004', '协调确定可靠性定量定性要求,以满足系统战备完好性和任务成功性要求', '可靠性要求,定性定量', 600),
('RE-02', '可靠性工作项目要求', '可靠性文件', 'RE', '["ARGUMENTATION","SCHEME"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 450A-2004', '选择并确定可靠性工作项目,以可接受的寿命周期费用实现规定的可靠性要求', '可靠性工作项目,工作选择', 601),
('RE-03', '可靠性计划', '可靠性文件', 'RE', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION","MAINTENANCE"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 450A-2004', '对可靠性工作提出总要求、做出总体安排,协调订购方和承制方的关系', '可靠性计划,总体安排', 602),
('RE-04', '可靠性工作计划(可靠性大纲)', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 450A-2004', '有计划地组织、指挥、协调、检查和控制全部可靠性工作', '可靠性大纲,工作计划', 603),
('RE-05', '可靠性模型', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 450A-2004', '建立产品的可靠性模型,用于定量分配、预计和评价产品的可靠性', '可靠性模型,定量分配', 604),
('RE-06', '可靠性分配', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 450A-2004', '将产品的可靠性指标逐级分配到规定的产品层次', '可靠性分配,指标分配', 605),
('RE-07', '可靠性预计', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 450A-2004', '预计产品的基本可靠性和任务可靠性,评价设计方案是否满足规定的可靠性定量要求', '可靠性预计,设计评价', 606),
('RE-08', '故障模式、影响及危害性分析(FMECA)', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB/Z 1391-2006', '找出潜在的硬件、软件的设计缺陷,以便采取改进措施', 'FMECA,故障模式,危害性分析', 607),
('RE-09', '故障树分析(FTA)', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB/Z 768A-1998', '运用演绎法逐级分析,寻找导致某种故障事件的各种可能原因', '故障树分析,FTA', 608),
('RE-10', '潜在通路分析', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 450A-2004', '发现通路中可能存在的潜在状态,保证通路安全可靠', '潜在通路分析', 609),
('RE-11', '电路容差分析', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB/Z 89-1997', '分析电路组成部分参数偏差和寄生参数对电路性能容差的影响', '电路容差分析,参数偏差', 610),
('RE-12', '可靠性设计准则', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 450A-2004', '将有助于保证、提高型号可靠性的一系列设计要求设计到产品中去', '可靠性设计准则,设计要求', 611),
('RE-13', '元器件、零部件和原材料选择与控制', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION","MAINTENANCE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 450A-2004', '减少品种,保持和提高产品的固有可靠性,降低保障费用', '元器件选择,原材料控制', 612),
('RE-14', '可靠性关键项目', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 450A-2004', '确定和控制对安全性、战备完好性、任务成功性和保障要求有重大影响的项目', '可靠性关键项目,风险控制', 613),
('RE-15', '测试/包装/贮存/装卸/运输和维修对可靠性影响', '可靠性文件', 'RE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 450A-2004', '通过测试与分析确定功能测试、包装、贮存、装卸、运输和维修对可靠性的影响', '可靠性影响,贮存运输', 614),
('RE-16', '有限元分析', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 450A-2004', '对产品的机械强度和热特性等进行分析和评价,发现承载结构和材料的薄弱环节', '有限元分析,机械强度,热特性', 615),
('RE-17', '耐久性分析', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 450A-2004', '发现可能过早发生耗损故障的零部件,确定故障根本原因和纠正措施', '耐久性分析,耗损故障', 616),
('RE-18', '环境应力筛选', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE","PRODUCTION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 450A-2004', '发现和排除不良元器件、制造工艺和其他原因引入的缺陷造成的早期故障', '环境应力筛选,早期故障', 617),
('RE-19', '可靠性增长试验大纲', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 1407-1992', '暴露产品潜在缺陷,找出薄弱环节,分析原因,采取纠正措施,使可靠性增长', '可靠性增长,试验大纲', 618),
('RE-20', '可靠性增长试验报告', '可靠性文件', 'RE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 1407-1992', '报告可靠性增长试验情况和试验结果', '可靠性增长,试验报告', 619),
('RE-21', '可靠性鉴定(验收)试验方案', '可靠性文件', 'RE', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 899A-2009', '全面规划装备的可靠性鉴定(验收)试验工作', '可靠性鉴定,验收试验方案', 620),
('RE-22', '可靠性鉴定(验收)试验大纲', '可靠性文件', 'RE', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 899A-2009', '规范可靠性鉴定(验收)试验的项目、内容和方法', '可靠性鉴定,试验大纲', 621),
('RE-23', '可靠性鉴定(验收)试验程序', '可靠性文件', 'RE', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 899A-2009', '规范可靠性鉴定(验收)试验的试验程序', '可靠性鉴定,试验程序', 622),
('RE-24', '可靠性鉴定(验收)试验报告', '可靠性文件', 'RE', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承试方', TRUE, 'GJB 899A-2009', '报告可靠性鉴定(验收)试验情况和试验结果', '可靠性鉴定,试验报告', 623),
('RE-25', '可靠性鉴定(验收)试验总结', '可靠性文件', 'RE', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 899A-2009', '对可靠性鉴定(验收)试验情况进行全面总结', '可靠性鉴定,试验总结', 624),
('RE-26', '可靠性分析评价', '可靠性文件', 'RE', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 450A-2004', '对整个研制过程中开展的可靠性工作情况和产品可靠性满足研制总要求情况的总结', '可靠性分析评价,总结', 625),
('RE-27', '使用期间可靠性信息收集计划', '可靠性文件', 'RE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 450A-2004', '规范装备使用期间可靠性信息收集的程序和要求', '使用可靠性,信息收集', 626),
('RE-28', '使用期间可靠性信息分类与编码', '可靠性文件', 'RE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 1775-1993', '按标准要求统一可靠性信息分类、信息单元、信息编码', '可靠性信息,分类编码', 627),
('RE-29', '使用期间可靠性评估计划', '可靠性文件', 'RE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 450A-2004', '规范使用期间可靠性评估各方的职责及评估内容、方法和程序', '使用可靠性评估,评估计划', 628),
('RE-30', '使用期间可靠性评估报告', '可靠性文件', 'RE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 450A-2004', '报告使用期间可靠性评估情况及结果', '使用可靠性评估,评估报告', 629),
('RE-31', '使用期间可靠性改进计划', '可靠性文件', 'RE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 450A-2004', '规范使用期间可靠性改进工作的组织和安排', '可靠性改进,改进计划', 630),
('RE-32', '使用期间可靠性改进项目报告', '可靠性文件', 'RE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 450A-2004', '报告使用期间可靠性改进情况和改进结果', '可靠性改进,项目报告', 631);

-- ========================================
-- 8. 维修性文件 (MA) - 25个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('MA-01', '维修性要求', '维修性文件', 'MA', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '订购方', TRUE, 'GJB 368B-2009', '协调并确定维修性定量定性要求,以满足系统战备完好性和任务成功性要求', '维修性要求,定性定量', 700),
('MA-02', '维修性工作项目要求', '维修性文件', 'MA', '["ARGUMENTATION","SCHEME"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 368B-2009', '选择并确定维修性工作项目', '维修性工作项目,工作选择', 701),
('MA-03', '维修性计划', '维修性文件', 'MA', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION","MAINTENANCE"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 368B-2009', '全面规划装备寿命周期的维修性工作', '维修性计划,全面规划', 702),
('MA-04', '维修性工作计划(维修性大纲)', '维修性文件', 'MA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 368B-2009', '制定并实施维修性工作计划,确保产品满足合同规定的维修性要求', '维修性大纲,工作计划', 703),
('MA-05', '维修性模型', '维修性文件', 'MA', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB/Z 145-2006', '建立产品的维修性模型,用于定量分配、预计与评定产品的维修性', '维修性模型,分配预计', 704),
('MA-06', '维修性分配', '维修性文件', 'MA', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB/Z 57-1994', '将产品的维修性指标由上到下逐级分配到规定的产品层次', '维修性分配,指标分配', 705),
('MA-07', '维修性预计', '维修性文件', 'MA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB/Z 57-1994', '估计产品的维修性,评价所提出的设计方案是否满足规定的维修性定量要求', '维修性预计,方案评价', 706),
('MA-08', '故障模式及影响分析--维修性信息', '维修性文件', 'MA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB/Z 1391-2006', '确定可能的故障模式及其对产品工作的影响,确定需要的维修性设计特征', 'FMEA-维修性,故障检测', 707),
('MA-09', '维修性分析', '维修性文件', 'MA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 368B-2009', '建立能够实现维修性要求的设计准则、对设计方案进行权衡、确定维修保障要求', '维修性分析,设计准则', 708),
('MA-10', '抢修性分析', '维修性文件', 'MA', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 368B-2009', '分析评价潜在战场损伤的抢修快捷性与资源要求', '抢修性分析,战场损伤', 709),
('MA-11', '维修性设计准则', '维修性文件', 'MA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 368B-2009', '将维修性的定量和定性要求及使用和保障约束转化为具体的产品设计准则', '维修性设计,设计准则', 710),
('MA-12', '维修保障计划和保障性分析的输入', '维修性文件', 'MA', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 368B-2009', '为制定详细的维修保障计划和进行保障性分析准备输入', '维修保障计划,保障性分析', 711),
('MA-13', '维修性核查方案', '维修性文件', 'MA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2072-1994', '对贯穿于整个研制过程的维修性试验与评定工作制定实施方案', '维修性核查,核查方案', 712),
('MA-14', '维修性核查报告', '维修性文件', 'MA', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2072-1994', '记录核查过程及结果', '维修性核查,核查报告', 713),
('MA-15', '维修性验证计划', '维修性文件', 'MA', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 368B-2009', '包括维修性验证目的、要求和程序,规范维修性验证工作', '维修性验证,验证计划', 714),
('MA-16', '维修性验证报告', '维修性文件', 'MA', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 2072-1994', '报告维修性验证试验情况和试验结果', '维修性验证,验证报告', 715),
('MA-17', '维修性分析评价方案', '维修性文件', 'MA', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 368B-2009', '通过综合各种信息评价产品是否满足合同规定的维修性要求', '维修性分析评价,评价方案', 716),
('MA-18', '维修性分析评价报告', '维修性文件', 'MA', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 368B-2009', '报告维修性评价情况和评价结果', '维修性分析评价,评价报告', 717),
('MA-19', '维修性评估报告', '维修性文件', 'MA', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 2072-1994', '对整个研制过程中维修性工作情况和产品维修性满足要求的总结性报告', '维修性评估,评估报告', 718),
('MA-20', '使用期间维修性信息收集计划', '维修性文件', 'MA', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 368B-2009', '规范装备使用期间维修性信息收集的程序和要求', '使用维修性,信息收集', 719),
('MA-21', '使用期间维修性信息分类和编码', '维修性文件', 'MA', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 1775-1993', '统一维修性信息分类、信息单元、信息编码', '维修性信息,分类编码', 720),
('MA-22', '使用期间维修性评价计划', '维修性文件', 'MA', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 368B-2009', '规范使用期间维修性评价各方的职责及评价内容、方法和程序', '使用维修性评价,计划', 721),
('MA-23', '使用期间维修性评价报告', '维修性文件', 'MA', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 368B-2009', '报告使用期间维修性评价情况和评价结果', '使用维修性评价,报告', 722),
('MA-24', '使用期间维修性改进计划', '维修性文件', 'MA', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 368B-2009', '规范使用期间维修性改进工作的组织和实施安排', '维修性改进,改进计划', 723),
('MA-25', '使用期间维修性改进报告', '维修性文件', 'MA', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 368B-2009', '报告使用期间维修性改进情况和改进结果', '维修性改进,改进报告', 724);

-- ========================================
-- 9. 测试性文件 (TE) - 25个文档 (关键条目)
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('TE-01', '诊断方案', '测试性文件', 'TE', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 2547A', '协调并确定装备诊断方案,以满足装备战备完好性、任务成功性和安全性要求', '诊断方案,测试性方案', 800),
('TE-02', '测试性要求', '测试性文件', 'TE', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '订购方', TRUE, 'GJB 2547A', '协调并确定装备测试性定量和定性要求', '测试性要求,定性定量', 801),
('TE-03', '测试性工作项目要求', '测试性文件', 'TE', '["ARGUMENTATION"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 2547A', '选择并确定测试性工作项目,以合理的费用实现规定的测试性要求', '测试性工作项目', 802),
('TE-04', '测试性计划', '测试性文件', 'TE', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION","MAINTENANCE"]', 'ARGUMENTATION', '订购方', TRUE, 'GJB 2547A', '全面规划装备寿命周期的测试性工作', '测试性计划,全面规划', 803),
('TE-05', '测试性工作计划', '测试性文件', 'TE', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 2547A', '明确并合理安排要求的测试性工作项目,确保满足合同规定的要求', '测试性工作计划', 804),
('TE-06', '测试性模型', '测试性文件', 'TE', '["ARGUMENTATION","SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', FALSE, 'GJB/Z 145-2006', '建立产品的测试性模型,用于分配、预计、设计和评价产品的测试性', '测试性模型', 805),
('TE-07', '测试性分配', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 2547A', '将产品的测试性定量要求逐层分配到规定的产品层次', '测试性分配', 806),
('TE-08', '测试性预计', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 2547A', '估计产品的测试性水平是否能满足规定的测试性定量要求', '测试性预计', 807),
('TE-09', 'FMECA--测试性信息', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB/Z 1391-2006', '进行故障模式影响及危害性分析,为测试性设计分析及试验评价提供信息', 'FMECA-测试性,故障分析', 808),
('TE-10', '测试性设计准则', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 2547A', '将测试性要求及使用和保障约束转化为具体的产品测试性设计准则', '测试性设计,设计准则', 809),
('TE-11', '固有测试性设计分析报告', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2547A', '分析记录系统或设备设计过程中固有测试性设计工作', '固有测试性,设计分析', 810),
('TE-12', '测试性设计准则符合性报告', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2547A', '对系统或设备的固有测试性进行分析,确定硬件是否有利于测试', '测试性符合性,分析报告', 811),
('TE-13', '诊断能力设计', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2547A', '进行嵌入式诊断设计和外部诊断设计', '诊断能力设计,嵌入式诊断', 812),
('TE-14', '测试要求文件(TRD)', '测试性文件', 'TE', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 3385-1998', '作为被测对象性能检验和诊断步骤的源文件', '测试要求文件,TRD', 813),
('TE-15', '测试性核查计划', '测试性文件', 'TE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2547A', '对贯穿于整个研制过程的测试性分析与评价工作制定实施计划', '测试性核查,核查计划', 814),
('TE-16', '测试性核查报告', '测试性文件', 'TE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2547A', '记录核查过程及结果', '测试性核查,核查报告', 815),
('TE-17', '测试性验证试验计划', '测试性文件', 'TE', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 2547A', '包括测试性验证目的、要求和程序,规范测试性验证试验工作', '测试性验证,验证计划', 816),
('TE-18', '测试性验证试验报告', '测试性文件', 'TE', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 1362A-2007', '报告测试性验证试验情况和试验结果', '测试性验证,验证报告', 817),
('TE-19', '测试性分析评价计划', '测试性文件', 'TE', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 2547A', '对综合分析评价工作提出要求、工作程序和有关安排', '测试性分析评价,计划', 818),
('TE-20', '测试性分析评价报告', '测试性文件', 'TE', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', FALSE, 'GJB 2547A', '报告测试性分析评价情况和评价结果', '测试性分析评价,报告', 819),
('TE-21', '测试性评估报告', '测试性文件', 'TE', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 2547A', '不断更新的文件,包含测试性设计的最新信息,设计定型阶段形成最终评估报告', '测试性评估,评估报告', 820),
('TE-22', '使用期间测试性信息收集计划', '测试性文件', 'TE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 2547A', '规范装备使用期间测试性信息收集的程序和要求', '使用测试性,信息收集', 821),
('TE-23', '使用期间测试性信息分类和编码', '测试性文件', 'TE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 1775-1993', '统一测试性信息分类、信息单元、信息编码', '测试性信息,分类编码', 822),
('TE-24', '使用期间测试性评价计划', '测试性文件', 'TE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 2547A', '规范使用期间测试性评价各方的职责及评价内容、方法和程序', '使用测试性评价,计划', 823),
('TE-25', '使用期间测试性评价报告', '测试性文件', 'TE', '["MAINTENANCE"]', 'MAINTENANCE', '订购方', FALSE, 'GJB 2547A', '报告使用期间测试性评价情况和评价结果', '使用测试性评价,报告', 824);

-- ========================================
-- 10. 保障性文件 (SU) - 约12个文档 (关键条目)
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('SU-01', '保障性要求', '保障性文件', 'SU', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '订购方', TRUE, 'GJB 3872-1999', '协调确定保障性定量定性要求,满足系统战备完好性和任务成功性要求', '保障性要求,定性定量', 900),
('SU-02', '综合保障计划', '保障性文件', 'SU', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 3872-1999', '全面规划装备的综合保障工作,协调保障资源与主装备的同步研制', '综合保障计划,保障资源', 901),
('SU-03', '综合保障工作计划', '保障性文件', 'SU', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 3872-1999', '制定并实施综合保障工作计划,确保各项保障工作落实', '综合保障,工作计划', 902),
('SU-04', '保障性分析', '保障性文件', 'SU', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 1371-1992', '进行系统级和各保障要素级的保障性分析', '保障性分析,保障要素', 903),
('SU-05', '维修保障方案', '保障性文件', 'SU', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 3872-1999', '确定装备维修保障的总体方案,包括维修级别、维修策略等', '维修保障方案,维修级别', 904),
('SU-06', '备件供应方案', '保障性文件', 'SU', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3872-1999', '确定备件供应规划,包括备件清单、供应渠道、库存策略等', '备件供应,备件清单', 905),
('SU-07', '保障设备方案', '保障性文件', 'SU', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3872-1999', '确定保障设备规划,包括测试设备、维修设备、培训设备等', '保障设备,测试设备', 906),
('SU-08', '人员与培训方案', '保障性文件', 'SU', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3872-1999', '确定人员编制、技能要求和培训方案', '人员培训,技能要求', 907),
('SU-09', '技术资料方案', '保障性文件', 'SU', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 3872-1999', '确定技术资料编制规划,包括技术说明书、使用维护说明书等', '技术资料,编制规划', 908),
('SU-10', '保障设施方案', '保障性文件', 'SU', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3872-1999', '确定保障设施规划,包括仓储设施、维修设施等', '保障设施,仓储,维修设施', 909),
('SU-11', '包装、装卸、贮存和运输方案', '保障性文件', 'SU', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 3872-1999', '确定包装、装卸、贮存和运输(PHS&T)方案', '包装,装卸,贮存,运输', 910),
('SU-12', '保障性评估报告', '保障性文件', 'SU', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 3872-1999', '对整个研制过程中保障性工作情况和产品保障性满足要求的总结性报告', '保障性评估,评估报告', 911);

-- ========================================
-- 11. 安全性文件 (SA) - 约8个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('SA-01', '安全性要求', '安全性文件', 'SA', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '订购方', TRUE, 'GJB 900A-2012', '协调确定安全性定量定性要求,将安全性设计到产品中去', '安全性要求,安全性设计', 1000),
('SA-02', '安全性大纲(工作计划)', '安全性文件', 'SA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 900A-2012', '有计划地组织、指挥、协调、检查和控制全部安全性工作', '安全性大纲,工作计划', 1001),
('SA-03', '初步危险分析(PHA)', '安全性文件', 'SA', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 900A-2012', '在方案阶段早期进行的初步危险分析,识别潜在危险', '初步危险分析,PHA,危险识别', 1002),
('SA-04', '系统危险分析(SHA)', '安全性文件', 'SA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 900A-2012', '在工程研制阶段识别系统接口和功能中的危险', '系统危险分析,SHA', 1003),
('SA-05', '使用与保障危险分析(O&SHA)', '安全性文件', 'SA', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 900A-2012', '识别使用和保障活动中的危险', '使用危险分析,保障危险分析', 1004),
('SA-06', '安全性评价报告', '安全性文件', 'SA', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 900A-2012', '对整个研制过程中安全性工作情况和产品安全性满足要求的总结性报告', '安全性评价,评价报告', 1005),
('SA-07', '安全性风险评估报告', '安全性文件', 'SA', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 900A-2012', '对识别出的危险进行风险评估,确定风险等级和可接受水平', '安全性风险,风险评估', 1006),
('SA-08', '危险材料清单', '安全性文件', 'SA', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 900A-2012', '列出产品中使用的危险材料及其处置要求', '危险材料,处置要求', 1007);

-- ========================================
-- 12. 环境适应性文件 (EN) - 约6个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('EN-01', '环境工程工作计划', '环境适应性文件', 'EN', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 4239-2001', '规定装备环境工程工作项目、实施计划和管理要求', '环境工程,工作计划', 1100),
('EN-02', '寿命期环境剖面', '环境适应性文件', 'EN', '["ARGUMENTATION","SCHEME"]', 'SCHEME', '承研承制方', TRUE, 'GJB 4239-2001', '描述装备在寿命期内经历的环境事件及相应的环境参数', '环境剖面,寿命期,环境参数', 1101),
('EN-03', '环境适应性设计准则', '环境适应性文件', 'EN', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 4239-2001', '将环境适应性要求转化为具体的设计准则,指导产品环境适应性设计', '环境适应性设计,设计准则', 1102),
('EN-04', '环境适应性设计分析报告', '环境适应性文件', 'EN', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 4239-2001', '分析产品设计方案满足环境适应性要求的程度', '环境适应性分析,设计分析', 1103),
('EN-05', '环境试验大纲', '环境适应性文件', 'EN', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 150A-2009', '规范环境试验的项目、内容和方法', '环境试验,试验大纲', 1104),
('EN-06', '环境试验报告', '环境适应性文件', 'EN', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 150A-2009', '报告环境试验情况和试验结果', '环境试验,试验报告', 1105);

-- ========================================
-- 13. 电磁兼容性文件 (EM) - 约6个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('EM-01', '电磁兼容性大纲', '电磁兼容性文件', 'EM', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'SCHEME', '承研承制方', TRUE, 'GJB 151A-1997', '规划电磁兼容性工作,确保系统内外电磁兼容', '电磁兼容性大纲,EMC', 1200),
('EM-02', '电磁兼容性控制计划', '电磁兼容性文件', 'EM', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 151A-1997', '制定电磁兼容性控制的具体计划和实施方案', '电磁兼容控制,实施计划', 1201),
('EM-03', '电磁环境效应分析报告', '电磁兼容性文件', 'EM', '["SCHEME","PROTOTYPE"]', 'SCHEME', '承研承制方', FALSE, 'GJB 1389A-2005', '分析系统可能遇到的电磁环境及系统可能产生的电磁效应', '电磁环境效应,分析报告', 1202),
('EM-04', '电磁兼容性设计准则', '电磁兼容性文件', 'EM', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 151A-1997', '将电磁兼容性要求转化为具体的设计准则', '电磁兼容设计,设计准则', 1203),
('EM-05', '电磁兼容性试验大纲', '电磁兼容性文件', 'EM', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 152A-1997', '规范电磁兼容性试验的项目、内容和方法', '电磁兼容试验,试验大纲', 1204),
('EM-06', '电磁兼容性试验报告', '电磁兼容性文件', 'EM', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 152A-1997', '报告电磁兼容性试验情况和试验结果', '电磁兼容试验,试验报告', 1205);

-- ========================================
-- 14. 人机工程文件 (HE) - 约4个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('HE-01', '人机工程大纲', '人机工程文件', 'HE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'SCHEME', '承研承制方', TRUE, 'GJB 2873-1997', '规划人机工程工作,确保人机系统设计良好', '人机工程大纲,人机系统', 1300),
('HE-02', '人机工程分析报告', '人机工程文件', 'HE', '["SCHEME","PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', TRUE, 'GJB 2873-1997', '对人机界面、操作环境和人员能力进行分析', '人机工程分析,人机界面', 1301),
('HE-03', '人机界面设计标准', '人机工程文件', 'HE', '["PROTOTYPE","FORMAL_SAMPLE"]', 'PROTOTYPE', '承研承制方', FALSE, 'GJB 2873-1997', '制定人机界面设计标准和规范', '人机界面,设计标准', 1302),
('HE-04', '人机工程评价报告', '人机工程文件', 'HE', '["FINALIZATION"]', 'FINALIZATION', '承研承制方', TRUE, 'GJB 2873-1997', '对人机工程实施情况进行评价总结', '人机工程评价,评价报告', 1303);

-- ========================================
-- 15. 项目成果文件 (PO) - 约5个文档
-- ========================================
INSERT INTO stage_doc_checklist_template (doc_code, doc_name, category, category_code, applicable_stages, primary_stage, responsibility, required_flag, gjb_reference, description, keywords, order_num) VALUES
('PO-01', '产品履历书', '项目成果文件', 'PO', '["PROTOTYPE","FORMAL_SAMPLE","FINALIZATION","PRODUCTION","MAINTENANCE"]', 'FINALIZATION', '承研承制方', TRUE, '--', '记录产品技术状态、使用维修、改装等全寿命信息', '履历书,技术状态,全寿命', 1400),
('PO-02', '产品证明书(合格证)', '项目成果文件', 'PO', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, '--', '证明产品经检验合格,准予出厂', '合格证,出厂证明', 1401),
('PO-03', '产品装箱单', '项目成果文件', 'PO', '["PRODUCTION"]', 'PRODUCTION', '承研承制方', TRUE, '--', '列出产品装箱清单,包括产品本体、备附件、工具、技术资料等', '装箱单,备附件', 1402),
('PO-04', '科技成果鉴定证书', '项目成果文件', 'PO', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '订购方', FALSE, '--', '对产品研制的科技成果进行鉴定', '科技成果鉴定,鉴定证书', 1403),
('PO-05', '研制工作总结报告', '项目成果文件', 'PO', '["FINALIZATION","PRODUCTION"]', 'FINALIZATION', '承研承制方', TRUE, '--', '对整个研制项目进行全面总结', '工作总结,研制总结,全面总结', 1404);
