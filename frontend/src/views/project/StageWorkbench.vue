<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回阶段列表</el-button>
        <h3 style="display:inline;margin-left:16px">阶段工作台</h3>
        <el-tag v-if="stage" :type="statusType(stage.status)" size="small" style="margin-left:12px">{{ statusLabel(stage.status) }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button v-if="canTransition" type="primary" @click="handleRequestTransition" :loading="acting">申请转阶段</el-button>
        <el-button @click="handleGateCheck" :loading="acting">转阶段准入检查</el-button>
        <el-button @click="handleAiReadiness" :loading="aiLoading">AI 转阶段评估</el-button>
      </div>
    </div>

    <el-row :gutter="16" v-if="workbench">
      <!-- 左侧：指标卡片 -->
      <el-col :span="8">
        <el-card shadow="never" class="metric-card">
          <template #header><span>文档完成度</span></template>
          <el-progress :percentage="workbench.docCompletionRate" :color="progressColor(workbench.docCompletionRate)" :stroke-width="16" />
          <p class="metric-detail">{{ workbench.releasedDocs }} / {{ workbench.totalDocs }} 已发布</p>
        </el-card>

        <el-card shadow="never" class="metric-card">
          <template #header><span>基线状态</span></template>
          <div v-if="workbench.baselines && workbench.baselines.length > 0">
            <div v-for="bl in workbench.baselines" :key="bl.id" class="baseline-row">
              <el-tag :type="baselineStatusType(bl.baselineStatus)" size="small">{{ bl.baselineStatus }}</el-tag>
              <span>{{ bl.baselineName }}</span>
            </div>
          </div>
          <p v-else class="no-data">尚未建立基线</p>
          <el-button size="small" style="margin-top:8px" @click="showCreateBaseline = true">创建基线</el-button>
        </el-card>

        <el-card shadow="never" class="metric-card">
          <template #header><span>变更状态</span></template>
          <p class="metric-detail">待处理变更: {{ workbench.openChangeRequests || 0 }}</p>
          <p class="metric-detail">评审中文档: {{ workbench.reviewingDocs || 0 }}</p>
        </el-card>
      </el-col>

      <!-- 右侧：基线 & 记实 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span>技术状态基线</span>
            <el-button size="small" type="primary" link style="float:right" @click="fetchBaselines">刷新</el-button>
          </template>
          <el-table :data="baselines" v-loading="blLoading" size="small">
            <el-table-column prop="baselineCode" label="基线编号" width="130" />
            <el-table-column prop="baselineName" label="基线名称" min-width="160" />
            <el-table-column prop="baselineType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ basetypeLabel(row.baselineType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="baselineVersion" label="版本" width="80" />
            <el-table-column prop="baselineStatus" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="baselineStatusType(row.baselineStatus)" size="small">{{ row.baselineStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button v-if="row.baselineStatus === 'DRAFT' || row.baselineStatus === 'REVIEWING'" link type="primary" size="small" @click="handleApprove(row)">批准</el-button>
                <el-button v-if="row.baselineStatus === 'APPROVED'" link type="success" size="small" @click="handleEffective(row)">生效</el-button>
                <el-button link size="small" @click="viewBaselineItems(row)">查看项</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" style="margin-top:16px">
          <template #header><span>技术状态记实</span></template>
          <el-timeline v-if="events.length > 0">
            <el-timeline-item v-for="e in events" :key="e.id" :timestamp="e.eventTime" placement="top" size="small">
              <strong>{{ e.eventName }}</strong>
              <p v-if="e.eventDescription" style="margin:4px 0 0;color:#909399">{{ e.eventDescription }}</p>
            </el-timeline-item>
          </el-timeline>
          <p v-else class="no-data">暂无记实</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建基线弹窗 -->
    <el-dialog v-model="showCreateBaseline" title="创建基线" width="400px">
      <el-form :model="baselineForm" label-width="90px">
        <el-form-item label="基线类型">
          <el-select v-model="baselineForm.baselineType" style="width:100%">
            <el-option label="功能基线 (FBL)" value="FUNCTIONAL_BASELINE" />
            <el-option label="分配基线 (ABL)" value="ALLOCATED_BASELINE" />
            <el-option label="产品基线 (PBL)" value="PRODUCT_BASELINE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateBaseline = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="handleCreateBaseline">创建</el-button>
      </template>
    </el-dialog>

    <!-- 基线项查看弹窗 -->
    <el-dialog v-model="showBaselineItems" title="基线项列表" width="600px">
      <el-table :data="baselineItems" size="small">
        <el-table-column prop="itemCode" label="编号" width="120" />
        <el-table-column prop="itemName" label="名称" min-width="200" />
        <el-table-column prop="itemType" label="类型" width="100" />
        <el-table-column prop="itemVersion" label="版本" width="80">
          <template #default="{ row }">{{ row.itemVersion || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getStageWorkbench, requestTransition, gateCheck, type ProjectStageItem } from '@/api/project-stage'
import { createBaseline, getBaselines, getBaselineItems, approveBaseline, setBaselineEffective, getStatusAccounting, type ConfigurationBaselineVO, type ConfigurationBaselineItemVO, type ConfigurationStatusAccountingVO } from '@/api/configuration-management'
import { stageReadiness } from '@/api/ai'

const route = useRoute()
const stageId = Number(route.params.stageId)
const projectId = Number(route.params.projectId)

const acting = ref(false)
const aiLoading = ref(false)
const blLoading = ref(false)

const stage = ref<ProjectStageItem | null>(null)
const workbench = ref<any>(null)
const baselines = ref<ConfigurationBaselineVO[]>([])
const events = ref<ConfigurationStatusAccountingVO[]>([])
const baselineItems = ref<ConfigurationBaselineItemVO[]>([])

const showCreateBaseline = ref(false)
const showBaselineItems = ref(false)
const baselineForm = ref({ baselineType: 'FUNCTIONAL_BASELINE' })

const canTransition = computed(() => stage.value && stage.value.isCurrent && (stage.value.status === 'IN_PROGRESS' || stage.value.status === 'REVIEWING' || stage.value.status === 'RECTIFYING' || stage.value.status === 'BASELINING' || stage.value.status === 'GATE_CHECKING'))

const statusType = (s: string) => {
  const map: Record<string, string> = { NOT_STARTED: 'info', PLANNING: '', IN_PROGRESS: 'warning', REVIEWING: '', RECTIFYING: 'danger', BASELINING: '', GATE_CHECKING: 'warning', COMPLETED: 'success', SUSPENDED: 'warning', TERMINATED: 'danger' }
  return map[s] || 'info'
}
const statusLabel = (s: string) => {
  const map: Record<string, string> = { NOT_STARTED: '未开始', PLANNING: '规划中', IN_PROGRESS: '进行中', REVIEWING: '评审中', RECTIFYING: '整改中', BASELINING: '基线建立中', GATE_CHECKING: '转阶段检查中', COMPLETED: '已完成', SUSPENDED: '已暂停', TERMINATED: '已终止' }
  return map[s] || s
}
const basetypeLabel = (t: string) => ({ FUNCTIONAL_BASELINE: '功能基线', ALLOCATED_BASELINE: '分配基线', PRODUCT_BASELINE: '产品基线' }[t] || t)
const baselineStatusType = (s: string) => ({ DRAFT: 'info', REVIEWING: 'warning', APPROVED: 'primary', EFFECTIVE: 'success', SUPERSEDED: '', ARCHIVED: 'info' }[s] || 'info')
const progressColor = (p: number) => p >= 100 ? '#67c23a' : p >= 60 ? '#409eff' : '#e6a23c'

async function loadWorkbench() {
  try {
    const res = await getStageWorkbench(projectId, stageId)
    workbench.value = res.data.data
    stage.value = workbench.value.stage
  } catch { /* ignore */ }
}

async function fetchBaselines() {
  blLoading.value = true
  try {
    const res = await getBaselines(projectId, stageId)
    baselines.value = res.data.data || []
  } finally { blLoading.value = false }
}

async function fetchEvents() {
  try {
    const res = await getStatusAccounting(projectId, stageId)
    events.value = res.data.data || []
  } catch { /* ignore */ }
}

async function handleRequestTransition() {
  try {
    // First run gate check
    const checkRes = await gateCheck(projectId, stageId)
    const checkData = checkRes.data.data
    const lines = [
      checkData.passed ? '✓ 准入检查通过' : '✗ 准入检查未通过',
      '阻断项: ' + (checkData.blockers || []).length + '个',
      ...(checkData.blockers || []).map((b: any) => '  - ' + b.description),
      '警告: ' + (checkData.warnings || []).length + '个',
      ...(checkData.warnings || []).map((w: any) => '  - ' + w.description)
    ]
    if (!checkData.passed) {
      await ElMessageBox.alert(lines.join('\n'), '无法转阶段 — 准入检查未通过', { confirmButtonText: '知道了', type: 'warning' })
      return
    }
    await ElMessageBox.confirm(lines.join('\n') + '\n\n确认转阶段？完成后当前阶段将被标记为已完成并自动启动下一阶段。', '确认转阶段', { confirmButtonText: '确认转阶段', type: 'success' })
    acting.value = true
    const res = await requestTransition(projectId, stageId)
    ElMessage.success(res.data.data.message || '转阶段成功')
    loadWorkbench()
  } catch { /* cancelled */ } finally { acting.value = false }
}

async function handleCreateBaseline() {
  acting.value = true
  try {
    await createBaseline(projectId, stageId, baselineForm.value.baselineType)
    ElMessage.success('基线创建成功')
    showCreateBaseline.value = false
    fetchBaselines()
    loadWorkbench()
  } catch { /* ignore */ } finally { acting.value = false }
}

async function handleApprove(row: ConfigurationBaselineVO) {
  try {
    await approveBaseline(row.id!)
    ElMessage.success('基线已批准')
    fetchBaselines()
  } catch { /* ignore */ }
}

async function handleEffective(row: ConfigurationBaselineVO) {
  try {
    await setBaselineEffective(row.id!)
    ElMessage.success('基线已生效')
    fetchBaselines()
    loadWorkbench()
  } catch { /* ignore */ }
}

async function viewBaselineItems(row: ConfigurationBaselineVO) {
  try {
    const res = await getBaselineItems(row.id!)
    baselineItems.value = res.data.data || []
    showBaselineItems.value = true
  } catch { /* ignore */ }
}

async function handleGateCheck() {
  acting.value = true
  try {
    const res = await gateCheck(projectId, stageId)
    const data = res.data.data
    const passed = data.passed
    const blockers = data.blockers || []
    const warnings = data.warnings || []
    const lines = [
      passed ? '✓ 准入检查通过' : '✗ 准入检查未通过',
      '阻断项 (' + blockers.length + '): ' + (blockers.length > 0 ? '' : '无'),
      ...blockers.map((b: any) => '  - ' + b.description),
      '警告 (' + warnings.length + '): ' + (warnings.length > 0 ? '' : '无'),
      ...warnings.map((w: any) => '  - ' + w.description)
    ]
    await ElMessageBox.alert(lines.join('\n'), '转阶段准入检查', {
      confirmButtonText: '知道了',
      type: passed ? 'success' : 'warning'
    })
  } catch { /* cancelled */ } finally { acting.value = false }
}

async function handleAiReadiness() {
  aiLoading.value = true
  try {
    const res = await stageReadiness(projectId, stageId)
    const data = res.data.data
    const ai = data.aiAssessment || {}
    const metrics = data.metrics || {}
    const info = [
      '文档完成率: ' + metrics.docCompletionRate + '%',
      '基线是否有效: ' + (metrics.hasEffectiveBaseline ? '是' : '否'),
      '待处理变更: ' + metrics.openChangeRequests,
      '',
      'AI 评估: ' + (ai.readiness || 'N/A'),
      '评分: ' + (ai.score || 'N/A'),
      '建议: ' + (ai.recommendation || 'N/A')
    ].join('\n')
    await ElMessageBox.alert(info, 'AI 转阶段准备度评估', { confirmButtonText: '知道了', type: 'info' })
  } catch { /* ignore */ } finally { aiLoading.value = false }
}

onMounted(() => {
  loadWorkbench()
  fetchBaselines()
  fetchEvents()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-actions { display: flex; gap: 8px; }

.metric-card { margin-bottom: 16px; }
.metric-detail { margin: 8px 0 0; font-size: 13px; color: #606266; }
.no-data { color: #c0c4cc; font-size: 13px; text-align: center; padding: 16px 0; }

.baseline-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; }
</style>
