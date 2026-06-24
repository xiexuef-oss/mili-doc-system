# 军工文档系统 V2.0 — 实施计划

> 基于需求文档 requirements-v2.0.md（12章）
> 规划日期：2026-06-21
> 当前版本基线：V1.0（commit 62ac9e6）

---

## 总览

```
Phase 0: 基础设施     P0   2周     模板体系 + 数据注入
Phase 1: B档计算引擎  P0   3周     预计/分配报告生成能力
Phase 2: 工作流导航   P0   2周     依赖检查 + 项目向导
Phase 3: 质量体系     P1   2周     评分 + 一致性检查
Phase 4: 对话式写作   P1   3周     对话引擎 + 决策沉淀
Phase 5: 学习进化     P2   3周     终稿对比 + 企业基线
Phase 6: 扩展能力     P2   2周     导入/导出/权限/审计
```

---

## Phase 0：基础设施改造（P0，2周）

> 目标：把 prompt 模板从"自然语言建议"改造为"结构化约束生成"，主数据和环境参数真正注入到生成流程。

### T0.1 定义章节约束数据结构

**Backend：新建实体类**

```java
// backend/src/main/java/com/military/doc/modules/template/entity/DocChapterConstraint.java

@TableName("doc_chapter_constraint")
@Data
public class DocChapterConstraint {
    private Long id;
    private Long templateId;          // 关联 DocTemplate
    private Integer chapterOrder;      // 章节序号（1,2,3...）
    private String chapterTitle;       // 章节标题
    private Boolean required;          // 是否必填
    private Integer minWords;          // 最小字数下限
    private String requiredFields;     // JSON: 必含字段列表
    private String dataMapping;        // JSON: 与主数据的映射 {"mtbf": "masterData.mtbfValue"}
    private String domain;             // 领域：ELECTRONIC
}
```

**DB 迁移**：
```sql
-- V3__chapter_constraints.sql
CREATE TABLE doc_chapter_constraint (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES doc_template(id),
    chapter_order INTEGER NOT NULL,
    chapter_title VARCHAR(200) NOT NULL,
    required BOOLEAN DEFAULT TRUE,
    min_words INTEGER DEFAULT 300,
    required_fields JSONB,
    data_mapping JSONB,
    domain VARCHAR(32) DEFAULT 'ELECTRONIC',
    created_at TIMESTAMP DEFAULT NOW()
);
```

### T0.2 重写 Prompt 模板

**修改文件**：`backend/src/main/resources/prompts/reliability-outline.md`

旧模板 → 新模板结构：
1. 角色定义（保留）
2. **必填章节定义**（新增，结构化）
3. **已知信息区**（新增，{{injected_context}} 包含主数据+环境参数）
4. **未知信息区**（新增，明确列出缺失数据）
5. **生成约束**（强化：锁定已知值、禁止占位符）
6. **AI_META 输出要求**（新增，每章末尾输出）
7. 项目上下文（保留）

同样改造其他 prompt：
- `reliability-derating.md`
- `reliability-prediction-report.md`
- `reliability-allocation-report.md`
- `draft-generation.md`（通用草稿）
- `chapter-generation.md`（章节生成）

### T0.3 实现 ContextAssemblyService 注入逻辑

**修改文件**：`backend/src/main/java/com/military/doc/ai/context/ContextAssemblyService.java`

当前 {{context}} 只注入项目基本信息。改造后：

```java
public String assembleContext(Long projectId) {
    ContextBuilder ctx = new ContextBuilder();
    
    // 1. 主数据（已提取的）
    MasterData md = masterDataService.getByProject(projectId);
    ctx.add("productName", md.getProductName());
    ctx.add("productType", md.getProductType());
    ctx.add("mtbfValue", md.getMtbfValue());
    // ... 更多主数据字段
    
    // 2. 环境参数（从技术要求提取）
    EnvParams env = envParamService.getByProject(projectId);
    ctx.add("tempRange", env.getTempRange());
    ctx.add("vibration", env.getVibration());
    ctx.add("humidity", env.getHumidity());
    // ...
    
    // 3. 平台参数（根据项目选择的平台）
    Platform platform = projectService.getPlatform(projectId);
    ctx.add("gjb299dEnvCategory", platform.getEnvCategory());  // ML/A/N/G
    
    // 4. 缺失数据清单
    ctx.add("missingFields", detectMissingFields(md, env));
    
    return ctx.build();  // 生成结构化的上下文文本
}
```

### T0.4 实现环境参数提取

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/EnvParamExtractionService.java`

```
用户上传技术要求文件
       ↓
  AI 提取环境参数
    - 工作温度范围
    - 振动条件
    - 冲击条件
    - 湿度/盐雾
    - 电磁兼容要求
       ↓
  用户确认/修改
       ↓
  存入 env_params 表
       ↓
  ContextAssemblyService 调用
```

集成到主数据提取流程中（MasterDataExtractionService 已有 OCR 和文件读取逻辑，扩展即可）。

### T0.5 实现 AI_META 解析器

**新建工具类**：`backend/src/main/java/com/military/doc/ai/util/AiMetaParser.java`

```java
// 从 AI 生成内容末尾提取 AI_META 注释
// 解析出：章节名、完成度分数、待核实列表、缺项列表
public class AiMetaParser {
    public static AiMeta extract(String content) { ... }
}

@Data
public class AiMeta {
    private String chapterName;
    private Integer completionScore;  // 0-100
    private List<String> toVerify;    // 待核实
    private List<String> confirmed;   // 确定项
    private List<String> missing;     // 缺项
}
```

---

## Phase 1：B 档计算引擎（P0，3周）

> 目标：完成可靠性预计报告和分配报告的自动计算与生成。

### T1.1 GJB/Z 299D 数据导入

**现有代码**：`ReliabilityPredictor.java`、`Gjb299dDataImporter.java` 已有框架。

**任务**：
1. 使用 Python 脚本数字化 299D 标准中的失效率表格（器件类型 × 环境 × 质量等级）
2. 导入到 `rel_gjb299d_cache` 表
3. 验证数据完整性（抽样比对标准原文）

数据规模估算：~5000 行（器件类型 ~200 × 环境类别 ~5 × 质量等级 ~5）

### T1.2 可靠性预计计算引擎

**修改文件**：`backend/src/main/java/com/military/doc/modules/reliability/calc/ReliabilityPredictor.java`

实现 GJB/Z 299D 应力分析法：
```
λp = λb × πE × πQ × πT × πS × πC

其中：
  λb  = 基本失效率（查 299D 表）
  πE  = 环境系数（平台决定，ML/A/N/G）
  πQ  = 质量系数（军品/工业级，用户提供）
  πT  = 温度系数（结温计算）
  πS  = 电应力系数（S=工作应力/额定应力）
  πC  = 复杂度系数（集成电路适用）
```

输入：元器件清单（器件型号、类型、质量等级、工作应力）
输出：各器件失效率 + 系统总失效率 + MTBF

### T1.3 可靠性分配计算引擎

**修改文件**：`backend/src/main/java/com/military/doc/modules/reliability/calc/ReliabilityAllocator.java`

实现两种分配方法：

**方法一：评分分配法**
```
Ci = Π(评分因子)  // 复杂度、技术成熟度、工作时间、环境严酷度
λi = Ci/ΣCi × λs  // 按复杂度比例分配系统失效率
```

**方法二：AGREE 分配法**
```
λi = (ni × ti) / (N × T) × (-ln Rs)
// ni=第i单元器件数, ti=工作时间, N=总器件数, T=系统任务时间
```

### T1.4 预计报告生成

**新建 Prompt**：`backend/src/main/resources/prompts/reliability-prediction-report.md`

按结构化模板重写：
- 必填章节：概述、可靠性模型、元器件清单与预计、预计结果汇总、薄弱环节分析、结论
- 注入计算引擎输出（各器件失效率、系统 MTBF）
- AI 负责：撰写分析文字、标识薄弱环节、给出改进建议

**API**：`POST /api/v1/reliability/prediction/generate`

### T1.5 分配报告生成

**新建 Prompt**：`backend/src/main/resources/prompts/reliability-allocation-report.md`

- 必填章节：概述、分配方法选择、分配计算过程、分配结果、分配合理性分析
- 注入计算引擎输出（各单元分配指标）
- AI 负责：解释分配逻辑、合理性论证

**API**：`POST /api/v1/reliability/allocation/generate`

---

## Phase 2：工作流导航（P0，2周）

> 目标：用户打开项目后知道"先做什么、后做什么、还缺什么"。

### T2.1 文档依赖引擎

**新建 Service**：`backend/src/main/java/com/military/doc/modules/document/service/DocumentDependencyService.java`

```java
// 核心方法
public DependencyCheckResult checkPrerequisites(Long projectId, String docType);

@Data
public class DependencyCheckResult {
    private boolean allHardMet;          // 硬依赖全部满足？
    private List<DepStatus> hardDeps;    // 硬依赖状态
    private List<DepStatus> softDeps;    // 软依赖状态
    private List<ConflictWarning> conflicts;  // 冲突警告
    private String suggestion;           // 建议下一步操作
}

@Data
public class DepStatus {
    private String docType;       // 依赖的文档类型
    private String docName;       // 文档名称
    private boolean satisfied;    // 是否满足
    private String detail;        // 如："大纲已存在，MTBF=500h 可提取"
}
```

**配置依赖规则**：`backend/src/main/resources/dependencies.yml`

```yaml
reliability_prediction:
  hard:
    - doc: reliability_outline
      fields: [mtbf_requirement, reliability_model, mission_profile]
    - doc: master_data
      fields: [product_type, component_list]
  soft:
    - doc: gjb299d_data
      fields: [failure_rates]
reliability_allocation:
  hard:
    - doc: reliability_prediction
      fields: [unit_failure_rates, system_mtbf]
fmeca:
  hard:
    - doc: reliability_outline
    - doc: reliability_prediction
```

### T2.2 前置检查 API

**Controller**：已有 `ReliabilityDocController.java`，添加端点：

```
GET  /api/v1/projects/{projectId}/documents/{docType}/prerequisites
返回：DependencyCheckResult
```

### T2.3 项目创建向导改造

**修改文件**：已有 `frontend/src/views/project/ProjectCreateWizard.vue`

增加步骤：
```
步骤1：项目基本信息（名称、代号）
步骤2：领域与平台选择（电子信息 + 弹载/机载/舰载/车载）
步骤3：上传输入文件（合同/任务书/技术规格书）← 触发 AI 提取
步骤4：系统自动规划文档清单 → 用户确认/调整
步骤5：开始工作
```

### T2.4 工作流导航 UI

**修改文件**：`frontend/src/views/project/ProjectDetail.vue`

在项目详情页增加"文档策划"区域，显示：

```
⬜ 主数据提取 ← 从这里开始
    ↓
⬜ 可靠性大纲 ← 可写，前置已满足
    ↓
🔒 可靠性预计报告 ← 被锁定，需先完成大纲
    ↓
🔒 可靠性分配报告 ← 被锁定，需先完成预计
    ↓
🔒 FMECA ← 被锁定
```

点击文档跳转到对应的工作台或对话式写作页面。

### T2.5 前置检查 UI 组件

**新建组件**：`frontend/src/components/PrerequisitesCheck.vue`

用户点击"生成"按钮时，先弹出前置检查面板：

```
┌─────────────────────────────────┐
│  前置检查：可靠性预计报告          │
│                                 │
│  ✅ 可靠性大纲 V0.5              │
│     可提取：MTBF=500h, 串联模型   │
│  ✅ 主数据                      │
│     产品类型、元器件清单已就绪      │
│  ⚠️ GJB/Z 299D 数据            │
│     未导入，将影响计算精度         │
│                                 │
│  [继续生成] [先解决依赖]          │
└─────────────────────────────────┘
```

---

## Phase 3：质量体系（P1，2周）

> 目标：每次生成都有明确的质量分数和待办清单。

### T3.1 质量评分引擎

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/QualityScoringService.java`

实现五维评分模型：

```java
public QualityScore scoreDocument(Long ledgerId) {
    DocLedger doc = docLedgerMapper.selectById(ledgerId);
    List<DocChapter> chapters = chapterMapper.selectByDocId(ledgerId);
    
    double chapterCompleteness = calcChapterScore(doc, chapters);  // ×30%
    double contentCoverage = calcCoverageScore(chapters);           // ×25%
    double dataAccuracy = calcAccuracyScore(doc, chapters);         // ×20%
    double formatCompliance = calcFormatScore(doc);                 // ×15%
    double readability = calcReadabilityScore(doc);                 // ×10%
    
    return new QualityScore(
        chapterCompleteness * 0.30
        + contentCoverage * 0.25
        + dataAccuracy * 0.20
        + formatCompliance * 0.15
        + readability * 0.10
    );
}
```

评分细则：
- 章节完整性：必填章节数 / 应有数
- 内容覆盖度：有效章节数 / 总章节（排除 <100 字的"假章节"）
- 数据准确性：主数据在文档中被正确引用的比例
- 格式规范性：通过格式检查项 / 总检查项
- 可读性：段落长度、术语密度、禁用词扫描

### T3.2 自动审查报告

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/AutoReviewService.java`

生成后自动跑审查，输出：

```
┌─────────────────────────────────┐
│  文档质量审查报告                  │
│  可靠性大纲 V0.1  完成度：62%      │
├─────────────────────────────────┤
│  ✅ 章节完整性：9/9 通过          │
│  ✅ 格式规范：通过                 │
│  ⚠️ 数据完整：3项主数据未引用     │
│  ❌ 引用文件：缺 GJB 899A        │
│                                 │
│  人工需补充（共7项）：             │
│  🔴 补充 MTBF 验证方法（3.1节）   │
│  🟡 补充环境试验条件细节          │
│  🟡 确认冗余设计措施              │
│  ...                            │
└─────────────────────────────────┘
```

### T3.3 跨文档一致性检查

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/ConsistencyCheckService.java`

```java
// 检查规则（可配置）
public List<ConsistencyIssue> checkCrossDocument(Long projectId) {
    List<ConsistencyIssue> issues = new ArrayList<>();
    
    // 规则1：大纲MTBF == 预计报告MTBF
    // 规则2：大纲可靠性模型 == 预计报告模型
    // 规则3：大纲环境条件 == 分配报告环境条件
    // 规则4：同一术语在不同文档中的表述一致
    // ...
    
    return issues;
}
```

### T3.4 前端质量面板

**新建组件**：`frontend/src/components/QualityPanel.vue`

文档生成后在右侧展示质量分数 + 审查报告 + 待办清单。

---

## Phase 4：对话式写作引擎（P1，3周）

> 目标：从"一键生成"转变为"结构化引导对话"。

### T4.1 对话引擎后端

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/DialogueWritingService.java`

四阶段对话流程：

```
阶段1：策划对话 — 确认文档类型、约束、数据来源
阶段2：大纲对话 — 生成大纲 → 用户确认/调整
阶段3：逐章对话 — 生成单章 → 标注不确定性 → 用户确认 → 下一章
阶段4：审查对话 — 自动检查 → 输出报告 → 用户逐项处理
```

```java
@Service
public class DialogueWritingService {
    
    // 启动写作对话
    public DialogueSession startSession(Long projectId, String docType);
    
    // 处理用户消息，返回 AI 回复 + 文档更新
    public DialogueResponse processMessage(String sessionId, String userMessage);
    
    // 生成当前阶段的 AI 初始消息
    public DialogueResponse startPhase(String sessionId, WritingPhase phase);
    
    // 获取当前文档预览内容
    public String getDocumentPreview(String sessionId);
}

enum WritingPhase {
    PLANNING,     // 策划
    OUTLINING,    // 大纲
    CHAPTERING,   // 逐章写作
    REVIEWING     // 审查
}
```

**API**：
```
POST /api/v1/dialogue/sessions             创建对话会话
POST /api/v1/dialogue/sessions/{id}/message 发送消息（SSE 流式返回）
GET  /api/v1/dialogue/sessions/{id}/preview  获取文档预览
```

### T4.2 对话 UI 前端

**新建组件**：`frontend/src/views/project/DialogueWriter.vue`

三栏布局：
```
┌──────────────┐ ┌──────────────────┐ ┌──────────────┐
│ 文档预览      │ │ 对话区            │ │ 状态面板      │
│ (实时渲染)    │ │                  │ │ 完成度：62%   │
│              │ │ [AI] 开始第1章... │ │ ⚠️ 3处待核实  │
│ ✅ 1. 范围   │ │                  │ │              │
│ ✅ 2. 引用   │ │ [用户] MTBF改800 │ │ 待办：        │
│ ⬜ 3. 术语   │ │                  │ │ □ 补充验证方法│
│ ⬜ 4. 要求   │ │ [AI] 已更新...   │ │ □ 确认振动条件│
│              │ │                  │ │              │
│              │ │ [输入框________] │ │              │
└──────────────┘ └──────────────────┘ └──────────────┘
```

### T4.3 对话决策数据持久化

**DB 迁移**：
```sql
-- V4__dialogue_tables.sql
CREATE TABLE dialogue_session (
    id VARCHAR(64) PRIMARY KEY,
    project_id BIGINT NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    doc_ledger_id BIGINT,
    current_phase VARCHAR(20) DEFAULT 'PLANNING',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE dialogue_decision (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES dialogue_session(id),
    step VARCHAR(200) NOT NULL,          -- 决策步骤描述
    options JSONB,                       -- 可选方案列表
    chosen VARCHAR(500),                 -- 用户选择的方案
    reason TEXT,                         -- 选择原因
    before_score NUMERIC(5,2),           -- 决策前完成度
    after_score NUMERIC(5,2),            -- 决策后完成度
    created_at TIMESTAMP DEFAULT NOW()
);
```

### T4.4 集成到现有可靠性模块

在 ReliabilityWorkbench.vue 中，将"一键生成"按钮改为"对话式生成"按钮，点击后进入 DialogueWriter.vue。

保留"一键生成"作为快捷方式（给熟悉系统的老用户）。

---

## Phase 5：学习进化（P2，3周）

> 目标：终稿对比学习 + 企业基线建立。

### T5.1 终稿差异分析引擎

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/DiffAnalysisService.java`

终稿提交时自动触发：

```java
public DiffReport analyze(Long projectId, Long ledgerId, String finalVersionContent) {
    // 1. 获取初稿版本
    DocVersion initialDraft = versionMapper.getFirstDraft(ledgerId);
    
    // 2. 五维差异分析
    DiffReport report = new DiffReport();
    report.setStructuralChanges(analyzeStructureChange(initialDraft, finalVersionContent));
    report.setMetricChanges(analyzeMetricChange(initialDraft, finalVersionContent));
    report.setContentChanges(analyzeContentChange(initialDraft, finalVersionContent));
    report.setFormatChanges(analyzeFormatChange(initialDraft, finalVersionContent));
    report.setAddRemoveChanges(analyzeAddRemove(initialDraft, finalVersionContent));
    
    // 3. 更新企业基线
    baselineService.updateFromDiff(projectId, report);
    
    // 4. 更新模板建议
    templateService.suggestImprovements(projectId, report);
    
    return report;
}
```

### T5.2 企业基线系统

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/EnterpriseBaselineService.java`

```
基线数据结构（按 领域 → 产品类型 → 文档类型 三级存储）：
{
  "domain": "ELECTRONIC",
  "productType": "弹载电子设备",
  "docType": "reliability_outline",
  "metrics": {
    "mtbf": {"min": 500, "max": 800, "avg": 650, "samples": 5},
    "reliability_model": {"串联": 5},
    "verification_method": {"定时截尾": 5}
  },
  "stableChapters": [
    {"chapter": "引用文件", "reuseRate": 0.98},
    {"chapter": "术语和定义", "reuseRate": 0.85}
  ],
  "termPreferences": {
    "可靠性预计": "可靠性摸底",
    "大纲": "可靠性保证大纲"
  }
}
```

**基线更新触发**：
- 每个项目终稿提交后自动更新
- 最低 3 个项目后才生成基线（避免小样本偏差）

### T5.3 模板定制 UI

**新建组件**：`frontend/src/views/admin/TemplateEditor.vue`

```
- 查看标准模板章节结构
- 增加/删除/调整章节
- 修改章节约束（字数下限、必填字段）
- 保存为"企业自定义模板"
- 预览生成效果
- 恢复标准模板
```

### T5.4 前端对比展示

**新建组件**：`frontend/src/components/DiffViewer.vue`

```
┌──────────────────────────────────┐
│  文档进化分析                      │
│  初稿 V0.1 → 终版 V1.0  变化率32% │
├──────────────────────────────────┤
│  📊 结构调整：1处（新增质保章节）    │
│  📊 指标修正：3处（MTBF 500→800） │
│  📊 内容重写：5段                  │
│  📊 占位符消除：12处 → 10处填充     │
│  📊 新增内容：+25%                 │
└──────────────────────────────────┘
```

---

## Phase 6：扩展能力（P2，2周）

### T6.1 历史文档导入

**新建 Service**：`backend/src/main/java/com/military/doc/ai/service/HistoricalDocImportService.java`

- 支持 Word (.docx) 和 PDF 拖拽批量上传
- OCR + 文本提取（复用现有 FileTextExtractor + OcrService）
- AI 识别文档类型、拆分章节
- 提取可复用段落（跨文档去重后的"模板化内容"）
- 导入成功后自动建企业基线

**API**：`POST /api/v1/projects/{projectId}/import/historical`

### T6.2 GJB 格式导出

**新建 Service**：`backend/src/main/java/com/military/doc/common/export/GjbExportService.java`

```
- 封面：项目代号、文档名称、编制单位、日期
- 审批页：设计师签字栏、总师/副总师签字栏
- 正文：A4、标准页边距、宋体标题、仿宋正文
- 密级水印："内部" 或 "秘密" 或 "机密"
- 导出格式：Word (.docx) 可编辑 + PDF 防篡改
```

**API**：`GET /api/v1/documents/{ledgerId}/export?format=docx|pdf`

### T6.3 权限体系

**DB 迁移**：
```sql
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,  -- WRITER/REVIEWER/APPROVER/PM/ADMIN
    name VARCHAR(50) NOT NULL
);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 默认角色
INSERT INTO sys_role (code, name) VALUES
('WRITER', '编写员'),
('REVIEWER', '校对员'),
('APPROVER', '审批员'),
('PM', '项目管理员'),
('ADMIN', '系统管理员');
```

### T6.4 审计日志扩展

扩展现有 `ai_audit_log` 为通用操作日志：

```sql
CREATE TABLE sys_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(100),
    action VARCHAR(50),        -- CREATE/UPDATE/DELETE/GENERATE/APPROVE
    target_type VARCHAR(50),   -- DOCUMENT/CHAPTER/PROJECT/TEMPLATE
    target_id BIGINT,
    detail JSONB,              -- 变更详情
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 附录 A：文件改动清单

### Backend 新增文件

```
backend/src/main/java/com/military/doc/
├── modules/template/entity/DocChapterConstraint.java
├── modules/template/mapper/DocChapterConstraintMapper.java
├── ai/service/EnvParamExtractionService.java
├── ai/service/QualityScoringService.java
├── ai/service/AutoReviewService.java
├── ai/service/ConsistencyCheckService.java
├── ai/service/DialogueWritingService.java
├── ai/service/DiffAnalysisService.java
├── ai/service/EnterpriseBaselineService.java
├── ai/service/HistoricalDocImportService.java
├── ai/util/AiMetaParser.java
├── modules/document/service/DocumentDependencyService.java
├── common/export/GjbExportService.java
└── modules/reliability/calc/Gjb299dDataLoader.java
```

### Backend 需重写文件

```
backend/src/main/resources/prompts/reliability-outline.md
backend/src/main/resources/prompts/reliability-derating.md
backend/src/main/resources/prompts/reliability-prediction-report.md
backend/src/main/resources/prompts/reliability-allocation-report.md
backend/src/main/resources/prompts/draft-generation.md
backend/src/main/resources/prompts/chapter-generation.md
backend/src/main/resources/prompts/master-data-extraction.md
└── 新增约10个 prompt 文件
```

### Backend 需修改文件

```
backend/src/main/java/com/military/doc/ai/context/ContextAssemblyService.java
backend/src/main/java/com/military/doc/ai/service/ReliabilityGenerationService.java
backend/src/main/java/com/military/doc/modules/reliability/calc/ReliabilityPredictor.java
backend/src/main/java/com/military/doc/modules/reliability/calc/ReliabilityAllocator.java
backend/src/main/java/com/military/doc/modules/reliability/controller/ReliabilityDocController.java
```

### Frontend 新增文件

```
frontend/src/components/PrerequisitesCheck.vue
frontend/src/components/QualityPanel.vue
frontend/src/components/DiffViewer.vue
frontend/src/views/project/DialogueWriter.vue
frontend/src/views/admin/TemplateEditor.vue
frontend/src/api/dialogue.ts
frontend/src/api/quality.ts
frontend/src/api/export.ts
```

### Frontend 需修改文件

```
frontend/src/views/project/ProjectCreateWizard.vue
frontend/src/views/project/ProjectDetail.vue
frontend/src/views/project/ReliabilityWorkbench.vue
frontend/src/api/reliability.ts
frontend/src/router/index.ts
```

### DB 迁移文件

```
backend/src/main/resources/db/migration/V3__chapter_constraints.sql
backend/src/main/resources/db/migration/V4__dialogue_tables.sql
backend/src/main/resources/db/migration/V5__baseline_tables.sql
backend/src/main/resources/db/migration/V6__audit_log_extension.sql
backend/src/main/resources/db/migration/V7__permission_system.sql
```

---

## 附录 B：里程碑与验收标准

| Phase | 验收标准 |
|-------|----------|
| P0-基础设施 | 1. 新 prompt 模板生成的大纲，完成度 ≥ 55%（旧模板 ~40%）<br>2. 主数据指标（MTBF等）在生成内容中正确引用<br>3. 环境参数从技术要求文件成功提取 |
| P0-B档计算 | 1. 预计报告：MTBF 计算值与人工核验偏差 < 10%<br>2. 分配报告：分配结果满足 Σλi ≤ λs<br>3. 299D 数据：抽样 50 条数据比对标准原文，准确率 100% |
| P0-工作流 | 1. 依赖检查：缺失前置文档时正确阻断并提示<br>2. 项目向导：5 步创建流程走通，15 分钟内完成 |
| P1-质量 | 1. 五维评分人工复检：评分误差 < 10 分<br>2. 跨文档一致性检查：覆盖 ≥ 5 条检查规则 |
| P1-对话式 | 1. 四阶段对话流程跑通<br>2. 逐章生成后用户可修改并重新生成单章<br>3. 决策数据正确持久化 |
| P2-学习 | 1. 终稿差异分析报告生成<br>2. 3 个项目后企业基线建立<br>3. 高复用段落被正确识别 |
| P2-扩展 | 1. 历史文档导入成功率 ≥ 80%<br>2. Word/PDF 导出格式符合 GJB 规范<br>3. 权限控制生效 |

---

> **下一步**：按 Phase 0 → 6 顺序依次实施。Phase 0 和 Phase 1 可并行（不同开发者）。
