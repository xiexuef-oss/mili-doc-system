# 可靠性文档自动生成模块 — 实施计划（修订版）

> **For Hermes:** 按本计划逐任务实施。当前系统 AI 能力已远超 DEV_ROADMAP 标记状态。

**Goal:** 扩展军工文档系统，新增"可靠性设计文件"为第 16 个文档类别，实现 8 类可靠性文档的 AI 自动生成

**Architecture:** 复用现有模板驱动生成架构，为 B 档文档新增独立计算引擎（Java 实现，不依赖 LLM），LLM 只负责将计算结果写入规范报告

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus + PostgreSQL / Vue 3 + TypeScript / DeepSeek API

**Updated:** 2026-06-14 — 基于 PRD V1.4 / 架构 V1.5 / 数据库 V1.5 / 实际代码审计

---

## 当前系统真实状态（代码审计结论）

DEV_ROADMAP.md 标记为"未完成"但实际已实现的 AI 能力：

| 能力 | 代码证据 |
|------|---------|
| LLM 服务接入 | `DelegatingLlmClient`, `OpenAiCompatibleClient`, `LlmProviderService` — DeepSeek + Ollama 双通道 |
| Prompt 模板管理 | 14 套 Markdown 模板 (`classpath:prompts/*.md`) + `PromptTemplateService` 渲染引擎 |
| RAG 语义检索 | `VectorIndexService`, `EmbeddingClient`, `SemanticMatch`, `SmartTruncationService` |
| 文档自动生成 | `DraftGenerationService` (one-shot/stream/chapter), `CatalogGenerationService` (AI目录), `BatchGenerationService` (拓扑并行) |
| 合规审查 | `ComplianceCheckService` + `pre-review.md` + `compliance-check.md` |
| 文档校对 | `ProofreadingService` + `proofread.md` |
| 评审辅助 | `OpinionSummaryService` + `opinion-summary.md` |
| 前端 AI 面板 | `ProjectAiAssistant.vue`, `AiChatFloating.vue`, `ChapterWritingGuide.vue` |

**15 大文档类别（GJB 5882）已覆盖**，可靠性设计文件为新增第 16 类。

---

## Phase 1: 数据库 + 种子数据（估计 1h）

### Task 1.1: 建表 SQL

**文件:** Create `backend/src/main/resources/db/migration/V2__reliability_tables.sql`

5 张新表：
- `rel_requirement` — 可靠性指标要求（MTBF/R(t)/验证方法/来源）
- `rel_prediction` — 可靠性预计记录（方法/λ_total/MTBF/是否达标）
- `rel_prediction_item` — 预计明细器件级（λ_b + 各 π 系数 + λ_p）
- `rel_gjb299d_cache` — GJB/Z 299D-2024 查表缓存（JSONB 键值对索引）
- `rel_allocation` — 可靠性分配记录（方法/分解路径/分配比例/父子链）

### Task 1.2: 注册为新文档类别

**文件:** Create `backend/src/main/resources/db/seed/V2__reliability_seeds.sql`

- 在 `stage_doc_checklist_template` 注册 8 份可靠性文档
- 在 `doc_template_v2` 注册 2 个模板（可靠性大纲 V2、降额报告 V2）
- 在 `doc_template_chapter` 注册大纲章节结构（20+ 章节，含 writingTips/standardClauseRef）
- 在 `doc_input_reference` 注册四环节参考关联

---

## Phase 2: Prompt 模板（估计 0.5h）

### Task 2.1: 可靠性专用 Prompt

**文件:** Create 4 个 prompt markdown 文件：
- `backend/src/main/resources/prompts/reliability-outline.md` — 以 GJB 450B 可靠性设计师身份撰写大纲
- `backend/src/main/resources/prompts/reliability-derating.md` — 以 GJB/Z 35 降额设计专家身份撰写报告
- `backend/src/main/resources/prompts/reliability-prediction-report.md` — 将计算结果撰写为 GJB 813 格式预计报告
- `backend/src/main/resources/prompts/reliability-allocation-report.md` — 将分配结果撰写为分配报告

---

## Phase 3: 生成服务（估计 1.5h）

### Task 3.1: ReliabilityGenerationService

**文件:** Create `backend/src/main/java/com/military/doc/ai/service/ReliabilityGenerationService.java`

核心方法（全部复用 DraftGenerationService 逐章生成能力）：
- `generateReliabilityOutline(projectId, docLedgerId)` → 可靠性大纲
- `generateDeratingReport(projectId, docLedgerId)` → 降额设计报告

实现：注入 DraftGenerationService，查找可靠性模板 → 调用 generateByTemplate → 返回 Markdown。与现有 DraftGenerationController 共用 API 模式。

---

## Phase 4: 计算引擎（B档核心，估计 3h）

### Task 4.1: 299D 数据导入器

**文件:** Create `backend/src/main/java/com/military/doc/modules/reliability/calc/Gjb299dDataImporter.java`

从已修正的 DOCX 读取 834 个表格 → 结构化写入 `rel_gjb299d_cache`。

### Task 4.2: ReliabilityPredictor

**文件:** Create `backend/src/main/java/com/military/doc/modules/reliability/calc/ReliabilityPredictor.java`

```
输入: List<PredictionInputItem> + environment
处理: 逐器件查299D缓存 → λ_b × π_E × π_Q × π_T × π_S × π_L × π_C...
输出: PredictionResult(totalFailureRate, mtbf, items明细, compliant)
```

### Task 4.3: ReliabilityAllocator

**文件:** Create `backend/src/main/java/com/military/doc/modules/reliability/calc/ReliabilityAllocator.java`

5 种方法：等分配 / 评分分配 / AGREE / ARINC / 比例分配

### Task 4.4: 扩展生成服务支持 B 档

在 ReliabilityGenerationService 新增：
- `generatePredictionReport()` — 调 Predictor → 组装结果 → LLM 写报告
- `generateAllocationReport()` — 调 Allocator → 组装结果 → LLM 写报告

---

## Phase 5: API + 前端（估计 2h）

### Task 5.1: ReliabilityDocController

**文件:** Create `backend/src/main/java/com/military/doc/modules/reliability/controller/ReliabilityDocController.java`

REST 端点（复用现有 `/api/ai/draft/generate` 模式，增加可靠性专用路由）

### Task 5.2: 前端可靠性工作台

**文件:** 
- Create: `frontend/src/views/project/ReliabilityWorkbench.vue`
- Create: `frontend/src/api/reliability.ts`
- Modify: `frontend/src/router/index.ts`

---

## 执行顺序

```
Phase 1 (DB+种子) → Phase 2 (Prompt) → Phase 3 (生成服务) → Phase 4 (计算引擎) → Phase 5 (API+前端)
```

**Phase 1-3 完成后即可交付 A 档（可靠性大纲 + 降额报告）。**
**Phase 4 完成后交付 B 档（预计报告 + 分配报告）。**
**Phase 5 提供完整前端交互。**

---

## Phase 1-3 代码详见各 Task 展开
