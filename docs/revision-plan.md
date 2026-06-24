# 系统层面修订方案

> 2026-06-21
> 基于需求文档 requirements-v2.0.md 的完整审查

---

## 现状：后端服务已完成，前端大量未接线

已完成后端服务（8个新增）：

| 服务 | 状态 | API 端点 |
|------|------|------|
| QualityScoringService | OK | GET /api/v1/reliability/documents/{id}/quality-score |
| AutoReviewService | OK | GET /api/v1/reliability/documents/{id}/auto-review |
| ConsistencyCheckService | OK | GET /api/v1/reliability/projects/{id}/consistency-check |
| DialogueWritingService | OK | POST /api/v1/reliability/dialogue/sessions |
| DiffAnalysisService | OK | POST /api/v1/reliability/documents/{id}/diff-analysis |
| EnterpriseBaselineService | OK | GET /api/v1/reliability/baseline/summary |
| GjbExportService | OK | GET /api/v1/reliability/documents/{id}/export |
| HistoricalDocImportService | OK | 需前端上传入口 |
| DocumentDependencyService | OK | 可靠性工作台 + AI助手 已接入 |

**核心问题：8个新服务全部就绪，但用户一个都看不到。**

---

## 修订计划（按用户可见性排序）

### 阶段 A：让已有能力可见（P0）

**A1. QualityPanel — 文档质量面板**
- 文件: 新建 frontend/src/components/QualityPanel.vue
- 位置: 文档台账详情页
- 内容: 五维评分 + 审查报告 + 待补充清单
- 用户看到: 每个文档都有一个完成度分数和待办

**A2. 文档导出按钮**
- 位置: 文档台账操作栏
- 用户看到: 点击导出 -> 下载GJB格式文档

**A3. 项目一致性检查**
- 位置: 项目详情页
- 用户看到: 一键检查全项目文档一致性

### 阶段 B：核心体验（P1）

**B1. DialogueWriter — 对话式写作界面**
- 文件: 新建 frontend/src/views/project/DialogueWriter.vue
- 三栏布局: 文档预览 | 对话区 | 状态面板

**B2. 项目创建向导增强**
- 修改 ProjectCreateWizard.vue
- 新增: 领域选择 + 平台选择 + 环境参数自动提取

**B3. DiffViewer — 初稿终稿对比**
- 入口: 文档版本历史
- 内容: 五维差异分析展示

### 阶段 C：扩展能力（P2）

**C1. 历史文档导入入口**
**C2. 模板定制编辑器**
**C3. 企业基线面板**

---

## 下一步：立即开始阶段 A
