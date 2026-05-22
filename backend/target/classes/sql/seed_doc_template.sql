-- ============================================================
-- 文档模板库种子数据：依据 GJB 5882-2006 标准文档结构
-- ============================================================

INSERT INTO doc_template (template_name, template_code, template_type, applicable_project_type, description, variables, status, created_at, updated_at) VALUES
(
  '技术方案报告模板',
  'TPL-F-PROCESS-001',
  'PROCESS',
  'MODEL_PROJECT',
  '# {{projectName}} 技术方案报告

## 1 范围
本文档规定了{{projectName}}的技术方案，适用于{{stageName}}技术方案评审。

## 2 规范性引用文件
- GJB 5882-2006 军工产品研制技术文件编写指南
- GJB 3206B 技术状态管理
- GJB 9001C-2017 质量管理体系要求
{{#each applicableStandards}}
- {{this}}
{{/each}}

## 3 术语和定义
- {{projectName}}：{{projectDescription}}
- 功能基线：经批准的战术技术指标文件
- 分配基线：经批准的技术方案文件

## 4 总体方案
### 4.1 系统组成
{{systemComposition}}

### 4.2 系统功能
{{systemFunctions}}

### 4.3 主要战术技术指标
| 序号 | 指标名称 | 指标要求 | 验证方法 |
|------|---------|---------|---------|
{{#each ttiItems}}
| {{@index}} | {{this.name}} | {{this.requirement}} | {{this.verificationMethod}} |
{{/each}}

## 5 分系统方案
{{#each subsystemItems}}
### 5.{{@index}} {{this.name}}
#### 5.{{@index}}.1 功能描述
{{this.functionDescription}}

#### 5.{{@index}}.2 技术途径
{{this.technicalApproach}}

#### 5.{{@index}}.3 关键器件选型
{{this.keyComponents}}
{{/each}}

## 6 关键技术
| 序号 | 关键技术名称 | 技术难点 | 解决途径 | 成熟度 |
|------|------------|---------|---------|--------|
{{#each keyTechnologies}}
| {{@index}} | {{this.name}} | {{this.difficulty}} | {{this.approach}} | {{this.maturity}} |
{{/each}}

## 7 标准化要求
### 7.1 标准化大纲要点
{{standardizationOutline}}

### 7.2 标准选用清单
{{#each standardList}}
- {{this.standardCode}} {{this.standardName}}
{{/each}}

## 8 可靠性初步分析
### 8.1 可靠性指标
MTBF ≥ {{mtbfRequirement}} 小时

### 8.2 可靠性设计措施
{{reliabilityDesign}}

## 9 安全性初步分析
### 9.1 危险源识别
{{#each hazardItems}}
- **{{this.name}}**：{{this.description}}（危险等级：{{this.level}}）
{{/each}}

### 9.2 安全性设计措施
{{safetyDesign}}

## 10 进度安排
| 阶段 | 时间 | 里程碑 | 输出文件 |
|------|------|--------|---------|
{{#each scheduleItems}}
| {{this.stage}} | {{this.period}} | {{this.milestone}} | {{this.deliverables}} |
{{/each}}

## 附录A 配套表
{{supportingTable}}

## 附录B 经费概算
{{budgetEstimate}}',
  '{"projectName": "项目名称", "stageName": "阶段名称", "projectDescription": "项目简要描述", "applicableStandards": ["GJB XXXX 标准名称"], "systemComposition": "系统组成描述", "systemFunctions": "系统功能列表", "ttiItems": [{"name": "指标名称", "requirement": "指标要求值", "verificationMethod": "分析方法/试验方法"}], "subsystemItems": [{"name": "分系统名称", "functionDescription": "功能描述", "technicalApproach": "技术途径", "keyComponents": "关键器件描述"}], "keyTechnologies": [{"name": "关键技术名", "difficulty": "技术难点", "approach": "解决途径", "maturity": "成熟度等级"}], "standardizationOutline": "标准化大纲要点", "standardList": [{"standardCode": "GJB XXXX", "standardName": "标准名称"}], "mtbfRequirement": "MTBF指标值", "reliabilityDesign": "可靠性设计措施", "hazardItems": [{"name": "危险源名称", "description": "危险描述", "level": "灾难性/严重/轻度/可忽略"}], "safetyDesign": "安全性设计措施", "scheduleItems": [{"stage": "阶段名", "period": "时间段", "milestone": "里程碑", "deliverables": "交付物"}], "supportingTable": "配套关系说明", "budgetEstimate": "经费概算说明"}',
  'ACTIVE', NOW(), NOW()
),
(
  '设计说明书模板',
  'TPL-C-PROCESS-002',
  'PROCESS',
  'MODEL_PROJECT',
  '# {{projectName}} 设计说明书

## 1 范围
本文档描述{{projectName}}的详细设计方案，适用于{{stageName}}设计评审。

## 2 规范性引用文件
- GJB 5882-2006 军工产品研制技术文件编写指南
- GJB 438C 软件文档编制规范
{{#each applicableStandards}}
- {{this}}
{{/each}}

## 3 设计依据
### 3.1 战术技术指标
{{ttiSummary}}

### 3.2 技术方案决策
{{technicalDecisions}}

## 4 系统设计
### 4.1 系统架构
{{systemArchitecture}}

### 4.2 工作原理
{{workingPrinciple}}

### 4.3 接口设计
| 接口编号 | 接口名称 | 接口类型 | 接口协议 | 数据格式 |
|---------|---------|---------|---------|---------|
{{#each interfaceItems}}
| {{this.id}} | {{this.name}} | {{this.type}} | {{this.protocol}} | {{this.dataFormat}} |
{{/each}}

## 5 硬件设计
{{#each hardwareItems}}
### 5.{{@index}} {{this.name}}
#### 5.{{@index}}.1 功能需求
{{this.functionRequirement}}

#### 5.{{@index}}.2 电路设计
{{this.circuitDesign}}

#### 5.{{@index}}.3 元器件选型
{{this.componentSelection}}

#### 5.{{@index}}.4 结构设计
{{this.structureDesign}}
{{/each}}

## 6 软件设计
{{#each softwareItems}}
### 6.{{@index}} {{this.name}}
#### 6.{{@index}}.1 需求规格
{{this.requirementSpec}}

#### 6.{{@index}}.2 概要设计
{{this.outlineDesign}}

#### 6.{{@index}}.3 详细设计
{{this.detailDesign}}

#### 6.{{@index}}.4 数据库设计
{{this.databaseDesign}}
{{/each}}

## 7 可靠性设计
### 7.1 可靠性分配
{{reliabilityAllocation}}

### 7.2 可靠性预计
{{reliabilityPrediction}}

### 7.3 FMEA 分析
| 功能 | 故障模式 | 故障影响 | 严酷度 | 检测方法 | 补偿措施 |
|------|---------|---------|--------|---------|---------|
{{#each fmeaItems}}
| {{this.function}} | {{this.failureMode}} | {{this.effect}} | {{this.severity}} | {{this.detection}} | {{this.compensation}} |
{{/each}}

## 8 电磁兼容性设计
### 8.1 电磁干扰控制措施
{{emiControl}}

### 8.2 电磁敏感度防护措施
{{emsProtection}}

## 9 环境适应性设计
{{#each environmentItems}}
### 9.{{@index}} {{this.factor}}
{{this.designMeasure}}
{{/each}}

## 10 设计验证
### 10.1 仿真分析
{{simulationAnalysis}}

### 10.2 原理样机验证
{{prototypeVerification}}

## 附录A 设计计算书
{{designCalculations}}

## 附录B 配套关系表
{{supportingRelations}}',
  '{"projectName": "项目名称", "stageName": "阶段名称", "ttiSummary": "战术技术指标摘要", "technicalDecisions": "技术方案决策说明", "systemArchitecture": "系统架构描述", "workingPrinciple": "工作原理说明", "interfaceItems": [{"id": "接口编号", "name": "接口名称", "type": "接口类型", "protocol": "协议", "dataFormat": "数据格式"}], "hardwareItems": [{"name": "硬件模块名", "functionRequirement": "功能需求", "circuitDesign": "电路设计说明", "componentSelection": "元器件选型说明", "structureDesign": "结构设计说明"}], "softwareItems": [{"name": "软件配置项名", "requirementSpec": "需求规格", "outlineDesign": "概要设计", "detailDesign": "详细设计", "databaseDesign": "数据库设计"}], "reliabilityAllocation": "可靠性分配说明", "reliabilityPrediction": "可靠性预计说明", "fmeaItems": [{"function": "功能", "failureMode": "故障模式", "effect": "影响", "severity": "严酷度", "detection": "检测方法", "compensation": "补偿措施"}], "emiControl": "EMI控制措施", "emsProtection": "EMS防护措施", "environmentItems": [{"factor": "环境因素", "designMeasure": "设计措施"}], "simulationAnalysis": "仿真分析说明", "prototypeVerification": "原理样机验证说明", "designCalculations": "设计计算过程", "supportingRelations": "配套关系说明"}',
  'ACTIVE', NOW(), NOW()
),
(
  '试验大纲模板',
  'TPL-C-TEST-001',
  'TEST_DOC',
  'MODEL_PROJECT',
  '# {{projectName}} {{testName}} 试验大纲

## 1 范围
本大纲规定了{{projectName}}{{testName}}的试验目的、项目、条件、方法和判据。

## 2 规范性引用文件
- GJB 150A 军用设备环境试验方法
- GJB 5882-2006 军工产品研制技术文件编写指南
{{#each applicableStandards}}
- {{this}}
{{/each}}

## 3 术语和定义
- 受试品：{{testArticleDescription}}
- 陪试品：{{supportEquipmentDescription}}

## 4 试验目的
{{testPurpose}}

## 5 试验依据
{{testBasis}}

## 6 试验项目与顺序
| 序号 | 试验项目 | 试验类型 | 试验条件概要 | 预计时间 |
|------|---------|---------|------------|---------|
{{#each testItems}}
| {{this.seq}} | {{this.name}} | {{this.type}} | {{this.condition}} | {{this.duration}} |
{{/each}}

## 7 受试品状态
### 7.1 技术状态
{{testArticleConfiguration}}

### 7.2 数量和编号
{{testArticleQuantity}}

## 8 试验条件
{{#each testConditionItems}}
### 8.{{@index}} {{this.itemName}}
#### 8.{{@index}}.1 试验设备
{{this.equipment}}

#### 8.{{@index}}.2 试验环境
{{this.environment}}

#### 8.{{@index}}.3 试验步骤
{{this.procedure}}

#### 8.{{@index}}.4 数据采集要求
{{this.dataCollection}}

#### 8.{{@index}}.5 试验判据
{{this.criteria}}
{{/each}}

## 9 测量与记录
### 9.1 测量参数与精度
{{measurementParameters}}

### 9.2 数据记录要求
{{recordingRequirements}}

## 10 安全要求
### 10.1 危险源识别
{{#each safetyHazards}}
- {{this}}
{{/each}}

### 10.2 安全防护措施
{{safetyMeasures}}

### 10.3 应急处置预案
{{emergencyPlan}}

## 11 质量保证
### 11.1 质量控制点
{{qualityControlPoints}}

### 11.2 记录和报告要求
{{reportingRequirements}}

## 附录A 试验件/陪试品清单
{{equipmentList}}

## 附录B 试验矩阵
{{testMatrix}}',
  '{"projectName": "项目名称", "testName": "试验名称", "testArticleDescription": "受试品描述", "supportEquipmentDescription": "陪试品描述", "testPurpose": "试验目的", "testBasis": "试验依据", "testItems": [{"seq": "序号", "name": "试验项目名", "type": "试验类型", "condition": "试验条件概要", "duration": "预计时间"}], "testArticleConfiguration": "受试品技术状态", "testArticleQuantity": "受试品数量和编号", "testConditionItems": [{"itemName": "试验项目名", "equipment": "试验设备", "environment": "试验环境", "procedure": "试验步骤", "dataCollection": "数据采集要求", "criteria": "试验判据"}], "measurementParameters": "测量参数与精度要求", "recordingRequirements": "数据记录要求", "safetyHazards": ["危险源描述"], "safetyMeasures": "安全防护措施", "emergencyPlan": "应急处置预案", "qualityControlPoints": "质量控制点", "reportingRequirements": "记录和报告要求", "equipmentList": "设备清单", "testMatrix": "试验矩阵"}',
  'ACTIVE', NOW(), NOW()
),
(
  '质量保证大纲模板',
  'TPL-F-QUALITY-001',
  'QUALITY',
  'MODEL_PROJECT',
  '# {{projectName}} 质量保证大纲

## 1 范围
本大纲规定了{{projectName}}研制全过程的质量保证要求。

## 2 规范性引用文件
- GJB 9001C-2017 质量管理体系要求
- GJB 3206B 技术状态管理
- GJB 5882-2006 军工产品研制技术文件编写指南

## 3 质量目标
### 3.1 产品质量目标
{{qualityObjectives}}

### 3.2 过程质量目标
{{processObjectives}}

## 4 质量管理组织与职责
### 4.1 质量保证组织
{{qualityOrganization}}

### 4.2 职责分配
| 角色 | 职责 | 权限 |
|------|------|------|
{{#each responsibilityItems}}
| {{this.role}} | {{this.responsibility}} | {{this.authority}} |
{{/each}}

## 5 文件控制
### 5.1 文件编制要求
{{documentRequirements}}

### 5.2 文件审批流程
{{approvalProcess}}

### 5.3 文件更改控制
{{changeControl}}

## 6 设计质量控制
### 6.1 设计输入控制
{{designInputControl}}

### 6.2 设计评审
{{#each designReviewItems}}
#### 6.2.{{@index}} {{this.reviewName}}
- 评审时机：{{this.timing}}
- 评审内容：{{this.content}}
- 参加人员：{{this.participants}}
{{/each}}

### 6.3 设计验证与确认
{{designVerification}}

## 7 采购质量控制
### 7.1 合格供方管理
{{supplierManagement}}

### 7.2 采购品检验
{{procurementInspection}}

## 8 生产过程质量控制
### 8.1 工艺评审
{{processReview}}

### 8.2 关键过程控制
{{#each keyProcessItems}}
- **{{this.processName}}**：控制参数 {{this.controlParameters}}，检测方法 {{this.inspectionMethod}}
{{/each}}

### 8.3 特殊过程控制
{{#each specialProcessItems}}
- **{{this.processName}}**：资质要求 {{this.qualification}}，过程确认 {{this.validation}}
{{/each}}

## 9 检验与试验
### 9.1 检验计划
| 检验阶段 | 检验项目 | 检验方法 | 抽样方案 | 验收准则 |
|---------|---------|---------|---------|---------|
{{#each inspectionItems}}
| {{this.stage}} | {{this.item}} | {{this.method}} | {{this.sampling}} | {{this.criteria}} |
{{/each}}

### 9.2 试验控制
{{testControl}}

## 10 不合格品控制
### 10.1 不合格品识别和标识
{{nonconformityIdentification}}

### 10.2 不合格品审理
{{nonconformityReview}}

### 10.3 纠正措施
{{correctiveActions}}

## 11 质量记录
### 11.1 记录清单
{{#each recordItems}}
- {{this.name}}（保存期限：{{this.retention}}）
{{/each}}

### 11.2 记录管理要求
{{recordManagement}}',
  '{"qualityObjectives": "产品质量目标", "processObjectives": "过程质量目标", "qualityOrganization": "质量保证组织描述", "responsibilityItems": [{"role": "角色", "responsibility": "职责", "authority": "权限"}], "documentRequirements": "文件编制要求", "approvalProcess": "审批流程", "changeControl": "更改控制", "designInputControl": "设计输入控制", "designReviewItems": [{"reviewName": "评审名称", "timing": "评审时机", "content": "评审内容", "participants": "参加人员"}], "designVerification": "设计验证说明", "supplierManagement": "供应商管理", "procurementInspection": "采购品检验", "processReview": "工艺评审说明", "keyProcessItems": [{"processName": "工序名", "controlParameters": "控制参数", "inspectionMethod": "检测方法"}], "specialProcessItems": [{"processName": "过程名", "qualification": "资质要求", "validation": "过程确认"}], "inspectionItems": [{"stage": "检验阶段", "item": "检验项目", "method": "检验方法", "sampling": "抽样方案", "criteria": "验收准则"}], "testControl": "试验控制说明", "nonconformityIdentification": "不合格品识别", "nonconformityReview": "不合格品审理", "correctiveActions": "纠正措施", "recordItems": [{"name": "记录名称", "retention": "保存期限"}], "recordManagement": "记录管理要求"}',
  'ACTIVE', NOW(), NOW()
),
(
  '标准化大纲模板',
  'TPL-F-STANDARDIZE-001',
  'STANDARDIZE',
  'MODEL_PROJECT',
  '# {{projectName}} 标准化大纲

## 1 范围
本大纲规定了{{projectName}}研制全过程的标准化工作要求。

## 2 规范性引用文件
- GJB 5882-2006 军工产品研制技术文件编写指南
- GJB 0.1 军用标准文件编制工作导则

## 3 标准化目标
### 3.1 总体目标
{{standardizationGoal}}

### 3.2 量化指标
| 指标 | 目标值 | 计算方法 |
|------|--------|---------|
| 标准化系数 | ≥ {{standardizationRate}}% | 标准件数量/零件总数 |
| 通用化系数 | ≥ {{commonalityRate}}% | 通用件数量/零件总数 |
| 系列化系数 | ≥ {{serializationRate}}% | 系列化件数量/零件总数 |

## 4 标准化要求
### 4.1 设计标准化
{{designStandardization}}

### 4.2 工艺标准化
{{processStandardization}}

### 4.3 文件标准化
{{documentStandardization}}

## 5 标准选用
### 5.1 标准选用原则
{{standardSelectionPrinciple}}

### 5.2 标准选用清单
| 序号 | 标准代号 | 标准名称 | 选用类别 | 适用阶段 |
|------|---------|---------|---------|---------|
{{#each standardList}}
| {{this.seq}} | {{this.code}} | {{this.name}} | {{this.category}} | {{this.stage}} |
{{/each}}

## 6 标准化审查
### 6.1 审查项目
{{#each reviewItems}}
#### 6.1.{{@index}} {{this.name}}
- 审查时机：{{this.timing}}
- 审查内容：{{this.content}}
- 审查依据：{{this.basis}}
{{/each}}

### 6.2 审查程序
{{reviewProcedure}}

### 6.3 不符合项处理
{{nonconformityHandling}}

## 7 标准实施
### 7.1 新标准宣贯
{{standardPromotion}}

### 7.2 标准实施检查
{{standardImplementationCheck}}

## 8 标准化效益评估
### 8.1 评估方法
{{evaluationMethod}}

### 8.2 预期效益
{{expectedBenefits}}',
  '{"standardizationGoal": "标准化总体目标", "standardizationRate": "标准化系数%", "commonalityRate": "通用化系数%", "serializationRate": "系列化系数%", "designStandardization": "设计标准化要求", "processStandardization": "工艺标准化要求", "documentStandardization": "文件标准化要求", "standardSelectionPrinciple": "标准选用原则", "standardList": [{"seq": "序号", "code": "标准代号", "name": "标准名称", "category": "规范性/资料性", "stage": "适用阶段"}], "reviewItems": [{"name": "审查项目名", "timing": "审查时机", "content": "审查内容", "basis": "审查依据"}], "reviewProcedure": "审查程序", "nonconformityHandling": "不符合项处理", "standardPromotion": "新标准宣贯计划", "standardImplementationCheck": "实施检查", "evaluationMethod": "评估方法", "expectedBenefits": "预期效益"}',
  'ACTIVE', NOW(), NOW()
),
(
  '风险管理计划模板',
  'TPL-F-RISK-001',
  'RISK',
  'MODEL_PROJECT',
  '# {{projectName}} 风险管理计划

## 1 范围
本计划规定了{{projectName}}研制全过程的风险识别、评估、控制和跟踪要求。

## 2 规范性引用文件
- GJB 5882-2006 军工产品研制技术文件编写指南
- GJB 9001C-2017 质量管理体系要求
{{#each applicableStandards}}
- {{this}}
{{/each}}

## 3 风险管理组织
### 3.1 风险管理小组
{{riskTeam}}

### 3.2 职责
| 角色 | 职责 |
|------|------|
{{#each teamRoles}}
| {{this.role}} | {{this.responsibility}} |
{{/each}}

## 4 风险识别
### 4.1 风险类别
{{#each riskCategories}}
- **{{this.name}}**：{{this.description}}
{{/each}}

### 4.2 风险识别方法
{{identificationMethods}}

### 4.3 风险识别清单
| 编号 | 风险名称 | 类别 | 描述 | 来源 |
|------|---------|------|------|------|
{{#each riskItems}}
| {{this.id}} | {{this.name}} | {{this.category}} | {{this.description}} | {{this.source}} |
{{/each}}

## 5 风险评估
### 5.1 评估准则
#### 5.1.1 可能性等级
| 等级 | 描述 | 概率范围 |
|------|------|---------|
| 1 | 极低 | < 5% |
| 2 | 低 | 5% ~ 20% |
| 3 | 中等 | 20% ~ 50% |
| 4 | 高 | 50% ~ 80% |
| 5 | 极高 | > 80% |

#### 5.1.2 影响等级
| 等级 | 描述 | 进度影响 | 成本影响 | 技术影响 |
|------|------|---------|---------|---------|
| 1 | 极小 | < 1周 | < 1万 | 不影响指标 |
| 2 | 小 | 1~2周 | 1~5万 | 影响非关键指标 |
| 3 | 中等 | 2~4周 | 5~20万 | 影响次要指标 |
| 4 | 大 | 1~2月 | 20~100万 | 影响主要指标 |
| 5 | 严重 | > 2月 | > 100万 | 影响关键指标 |

### 5.2 风险评估矩阵
| 编号 | 风险名称 | 可能性 | 影响 | 风险等级 | 优先级 |
|------|---------|--------|------|---------|--------|
{{#each riskAssessmentItems}}
| {{this.id}} | {{this.name}} | {{this.probability}} | {{this.impact}} | {{this.level}} | {{this.priority}} |
{{/each}}

## 6 风险控制
### 6.1 控制策略
{{#each controlItems}}
#### 6.1.{{@index}} {{this.riskName}}
- 控制策略：{{this.strategy}}（规避/减轻/转移/接受）
- 控制措施：{{this.measures}}
- 责任人：{{this.owner}}
- 完成时限：{{this.deadline}}
- 预期效果：{{this.expectedEffect}}
{{/each}}

### 6.2 应急方案
{{#each contingencyItems}}
#### 6.2.{{@index}} {{this.riskName}}
- 触发条件：{{this.trigger}}
- 应急措施：{{this.action}}
- 启动时间：{{this.timeline}}
{{/each}}

## 7 风险跟踪
### 7.1 跟踪周期
{{trackingPeriod}}

### 7.2 风险状态报告
{{statusReportRequirements}}

## 附录A 风险管理工具
{{riskManagementTools}}',
  '{"riskTeam": "风险管理小组描述", "teamRoles": [{"role": "角色", "responsibility": "职责"}], "riskCategories": [{"name": "类别名", "description": "类别描述"}], "identificationMethods": "风险识别方法", "riskItems": [{"id": "编号", "name": "风险名称", "category": "类别", "description": "描述", "source": "来源"}], "riskAssessmentItems": [{"id": "编号", "name": "风险名称", "probability": "可能性等级", "impact": "影响等级", "level": "风险等级", "priority": "优先级"}], "controlItems": [{"riskName": "风险名称", "strategy": "控制策略", "measures": "控制措施", "owner": "责任人", "deadline": "完成时限", "expectedEffect": "预期效果"}], "contingencyItems": [{"riskName": "风险名称", "trigger": "触发条件", "action": "应急措施", "timeline": "启动时间"}], "trackingPeriod": "跟踪周期", "statusReportRequirements": "状态报告要求", "riskManagementTools": "风险管理工具说明"}',
  'ACTIVE', NOW(), NOW()
),
(
  '可靠性保证大纲模板',
  'TPL-F-RELIABILITY-001',
  'RELIABILITY',
  'MODEL_PROJECT',
  '# {{projectName}} 可靠性保证大纲

## 1 范围
本大纲规定了{{projectName}}可靠性工作的目标、项目和要求。

## 2 规范性引用文件
- GJB 450B 可靠性工作通用要求
- GJB 899C 可靠性鉴定和验收试验
- GJB/Z 1391 故障模式、影响及危害性分析指南

## 3 可靠性目标
### 3.1 定量指标
| 指标名称 | 指标要求 | 置信度 |
|---------|---------|--------|
| MTBF（平均故障间隔时间） | ≥ {{mtbf}} 小时 | {{confidence}}% |
| 使用寿命 | ≥ {{serviceLife}} 年 | — |
| 任务成功率 | ≥ {{missionSuccessRate}}% | — |

### 3.2 定性要求
{{qualitativeRequirements}}

## 4 可靠性工作项目
| 序号 | 工作项目 | 工作内容 | 输出文件 | 完成阶段 |
|------|---------|---------|---------|---------|
{{#each workItems}}
| {{this.seq}} | {{this.name}} | {{this.content}} | {{this.output}} | {{this.stage}} |
{{/each}}

## 5 可靠性建模
### 5.1 可靠性框图
{{reliabilityBlockDiagram}}

### 5.2 可靠性数学模型
{{reliabilityModel}}

## 6 可靠性分配
| 产品层次 | 产品名称 | MTBF分配值 | 分配依据 |
|---------|---------|-----------|---------|
{{#each allocationItems}}
| {{this.level}} | {{this.name}} | {{this.mtbf}} 小时 | {{this.basis}} |
{{/each}}

## 7 可靠性预计
### 7.1 预计方法
{{predictionMethod}}

### 7.2 预计结果
{{predictionResults}}

## 8 故障模式影响及危害性分析(FMECA)
### 8.1 分析范围
{{fmecaScope}}

### 8.2 分析结果摘要
| 功能 | 故障模式 | 故障原因 | 局部影响 | 最终影响 | 严酷度 | 检测方法 |
|------|---------|---------|---------|---------|--------|---------|
{{#each fmecaItems}}
| {{this.function}} | {{this.mode}} | {{this.cause}} | {{this.localEffect}} | {{this.endEffect}} | {{this.severity}} | {{this.detection}} |
{{/each}}

## 9 可靠性试验
### 9.1 可靠性研制试验
{{developmentTest}}

### 9.2 可靠性鉴定试验
{{qualificationTest}}

### 9.3 可靠性验收试验
{{acceptanceTest}}

## 10 可靠性管理
### 10.1 数据收集与分析
{{dataCollection}}

### 10.2 评审要求
{{reviewRequirements}}

### 10.3 供方可靠性管理
{{supplierReliability}}',
  '{"mtbf": "MTBF指标值", "confidence": "置信度%", "serviceLife": "使用寿命年数", "missionSuccessRate": "任务成功率%", "qualitativeRequirements": "定性要求", "workItems": [{"seq": "序号", "name": "工作项目", "content": "工作内容", "output": "输出文件", "stage": "完成阶段"}], "reliabilityBlockDiagram": "可靠性框图说明", "reliabilityModel": "可靠性数学模型", "allocationItems": [{"level": "产品层次", "name": "产品名称", "mtbf": "MTBF分配值", "basis": "分配依据"}], "predictionMethod": "预计方法", "predictionResults": "预计结果", "fmecaScope": "FMECA分析范围", "fmecaItems": [{"function": "功能", "mode": "故障模式", "cause": "故障原因", "localEffect": "局部影响", "endEffect": "最终影响", "severity": "严酷度", "detection": "检测方法"}], "developmentTest": "研制试验说明", "qualificationTest": "鉴定试验说明", "acceptanceTest": "验收试验说明", "dataCollection": "数据收集与分析要求", "reviewRequirements": "评审要求", "supplierReliability": "供方可靠性管理"}',
  'ACTIVE', NOW(), NOW()
);
