<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h3>项目阶段路线图</h3>
        <span class="subtitle">GJB 3206B 技术状态管理</span>
      </div>
      <div class="header-actions">
        <el-tag type="info" size="large">阶段在项目创建时已选定</el-tag>
      </div>
    </div>

    <div v-loading="loading" class="roadmap">
      <div v-for="(stage, idx) in items" :key="stage.id" class="stage-card-wrapper">
        <div class="stage-card" :class="'status-' + (stage.status || 'NOT_STARTED').toLowerCase()">
          <div class="stage-header">
            <span class="stage-order">{{ String(idx + 1).padStart(2, '0') }}</span>
            <el-tag :type="statusType(stage.status)" size="small" effect="dark">{{ statusLabel(stage.status) }}</el-tag>
            <el-tag v-if="stage.isCurrent" type="primary" size="small" effect="plain">当前</el-tag>
          </div>
          <div class="stage-body">
            <h4>{{ stage.stageName }}</h4>
            <p class="stage-code">{{ stage.stageCode }}</p>
            <div class="stage-dates">
              <span>{{ stage.startDate || '待定' }}</span>
              <span class="arrow">→</span>
              <span>{{ stage.endDate || '待定' }}</span>
            </div>
            <p class="stage-goal" v-if="stage.stageGoal">{{ stage.stageGoal }}</p>
          </div>
          <div class="stage-actions">
            <el-button v-if="canTransition(stage)"
              type="primary" size="small" @click="handleRequestTransition(stage)">
              申请转阶段
            </el-button>
            <el-button v-if="canSuspend(stage)"
              size="small" type="warning" @click="handleSuspend(stage)">暂停</el-button>
            <el-button v-if="stage.status !== 'COMPLETED' && stage.status !== 'TERMINATED'"
              size="small" type="danger" @click="handleTerminate(stage)">终止</el-button>
            <el-button size="small" @click="showEditDialog(stage)">编辑</el-button>
          </div>

          <!-- Document catalog section -->
          <div class="stage-catalog">
            <div class="catalog-toggle" @click="toggleCatalog(stage.id!)">
              <el-icon v-if="catalogsExpanded[stage.id!]"><ArrowDown /></el-icon>
              <el-icon v-else><ArrowRight /></el-icon>
              <span>文档清单</span>
              <el-tag size="small" type="info" style="margin-left:6px">{{ (catalogsMap[stage.id!] || []).length }} 项</el-tag>
            </div>
            <div v-show="catalogsExpanded[stage.id!]" class="catalog-list">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;flex-wrap:wrap">
                <el-button size="small" type="success" :loading="catalogsLoading[stage.id!]"
                  @click.stop="handleGenerateByStage(stage)">
                  <el-icon><Plus /></el-icon>GJB 5882 规则生成
                </el-button>
                <el-button size="small" type="primary" link :loading="catalogsLoading[stage.id!]"
                  @click.stop="handleGenerateCatalog(stage.id!)">
                  AI 补充生成
                </el-button>
                <el-tooltip :content="aiOnline ? `大模型已连接 (${aiModel})` : '大模型未连接，请确认 Ollama 已启动'" placement="top">
                  <span :style="{display:'inline-flex',alignItems:'center',gap:'3px',fontSize:'11px',color:aiOnline?'#67c23a':'#f56c6c',cursor:'pointer'}" @click.stop="handleCheckHealth">
                    <span :style="{width:'8px',height:'8px',borderRadius:'50%',background:aiOnline?'#67c23a':'#f56c6c',display:'inline-block'}"></span>
                    {{ aiOnline ? 'AI 在线' : 'AI 离线' }}
                  </span>
                </el-tooltip>
              </div>
              <div v-if="(catalogsMap[stage.id!] || []).length === 0" class="catalog-empty">暂无文档清单，点击上方按钮由AI生成</div>
              <div v-for="cat in catalogsMap[stage.id!]" :key="cat.id" class="catalog-item">
                <el-tag :type="cat.requiredFlag ? 'danger' : 'info'" size="small" effect="plain">
                  {{ cat.requiredFlag ? '必' : '选' }}
                </el-tag>
                <span class="catalog-code">{{ cat.docCode }}</span>
                <span class="catalog-name">{{ cat.docName }}</span>
                <el-tag v-if="cat.docCategory" size="small" type="success">{{ cat.docCategory }}</el-tag>
                <el-tag size="small" type="">{{ docTypeLabel(cat.docType) }}</el-tag>
              </div>
            </div>
          </div>
        </div>
        <div v-if="idx < items.length - 1" class="stage-connector">
          <span class="arrow-down">▼</span>
        </div>
      </div>

      <el-empty v-if="!loading && items.length === 0" description="该项目未设置阶段，请在创建项目时选择适用阶段" />
    </div>

    <!-- 转阶段确认弹窗 -->
    <el-dialog v-model="transitionVisible" title="申请转阶段" width="520px">
      <div v-if="gateResult">
        <el-alert :title="gateResult.passed ? '准入检查通过，可以转阶段' : '准入检查未通过'" :type="gateResult.passed ? 'success' : 'error'" :closable="false" style="margin-bottom:16px" />
        <div v-if="gateResult.blockers && gateResult.blockers.length > 0">
          <h4 style="color:#f56c6c">阻断项 ({{ gateResult.blockers.length }})</h4>
          <ul><li v-for="b in gateResult.blockers" :key="b.type">{{ b.description }}</li></ul>
        </div>
        <div v-if="gateResult.warnings && gateResult.warnings.length > 0">
          <h4 style="color:#e6a23c">警告 ({{ gateResult.warnings.length }})</h4>
          <ul><li v-for="w in gateResult.warnings" :key="w.type">{{ w.description }}</li></ul>
        </div>
        <p v-if="gateResult.passed" style="color:#67c23a;font-weight:bold">
          将从「{{ transitioningStage?.stageName }}」转至下一阶段，确认后不可撤销。
        </p>
      </div>
      <el-alert v-else title="正在执行准入检查..." type="info" :closable="false" />
      <template #footer>
        <el-button @click="transitionVisible = false">取消</el-button>
        <el-button v-if="gateResult?.passed" type="primary" :loading="saving" @click="handleConfirmTransition">确认转阶段</el-button>
      </template>
    </el-dialog>

    <!-- 编辑阶段弹窗 -->
    <el-dialog v-model="dialogVisible" :title="'编辑阶段'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="阶段名称"><el-input v-model="form.stageName" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%">
            <el-option label="未开始" value="NOT_STARTED" />
            <el-option label="规划中" value="PLANNING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="评审中" value="REVIEWING" />
            <el-option label="整改中" value="RECTIFYING" />
            <el-option label="基线建立中" value="BASELINING" />
            <el-option label="转阶段检查中" value="GATE_CHECKING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已暂停" value="SUSPENDED" />
            <el-option label="已终止" value="TERMINATED" />
          </el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="阶段负责人"><el-input-number v-model="form.stageManagerId" :min="0" style="width:100%" placeholder="用户ID" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="技术负责人"><el-input-number v-model="form.technicalManagerId" :min="0" style="width:100%" placeholder="用户ID" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="阶段目标"><el-input v-model="form.stageGoal" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="准入条件"><el-input v-model="form.entryCriteria" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="准出条件"><el-input v-model="form.exitCriteria" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowRight, Plus } from '@element-plus/icons-vue'
import { getProjectStages, updateProjectStage, requestTransition, suspendStage, terminateStage, gateCheck, type ProjectStageItem } from '@/api/project-stage'
import { getDocCatalogs, generateCatalogByStage, type DocCatalogItem } from '@/api/doc-catalog'
import { generateCatalog, checkAiHealth } from '@/api/ai'
import { getDictItems, type DictItem } from '@/api/dict'

const route = useRoute()
const projectId = Number(route.params.projectId)

const loading = ref(false); const saving = ref(false)
const items = ref<ProjectStageItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const transitionVisible = ref(false)
const transitioningStage = ref<ProjectStageItem | null>(null)
const gateResult = ref<any>(null)

const form = reactive<ProjectStageItem>({} as ProjectStageItem)

// AI health
const aiOnline = ref(false)
const aiModel = ref('')

// Document catalog per stage
const catalogsMap = ref<Record<number, DocCatalogItem[]>>({})
const catalogsExpanded = ref<Record<number, boolean>>({})
const catalogsLoading = ref<Record<number, boolean>>({})

const statusType = (s: string) => {
  const map: Record<string, string> = {
    NOT_STARTED: 'info', PLANNING: '', IN_PROGRESS: 'warning',
    REVIEWING: '', RECTIFYING: 'danger', BASELINING: '',
    GATE_CHECKING: 'warning', COMPLETED: 'success',
    SUSPENDED: 'warning', TERMINATED: 'danger'
  }
  return map[s] || 'info'
}
const statusLabel = (s: string) => {
  const map: Record<string, string> = {
    NOT_STARTED: '未开始', PLANNING: '规划中', IN_PROGRESS: '进行中',
    REVIEWING: '评审中', RECTIFYING: '整改中', BASELINING: '基线建立中',
    GATE_CHECKING: '转阶段检查中', COMPLETED: '已完成',
    SUSPENDED: '已暂停', TERMINATED: '已终止'
  }
  return map[s] || s
}

const canTransition = (stage: ProjectStageItem) =>
  stage.isCurrent && (stage.status === 'IN_PROGRESS' || stage.status === 'REVIEWING' || stage.status === 'RECTIFYING' || stage.status === 'BASELINING' || stage.status === 'GATE_CHECKING')

const canSuspend = (stage: ProjectStageItem) =>
  stage.status === 'IN_PROGRESS' || stage.status === 'REVIEWING' || stage.status === 'RECTIFYING' || stage.status === 'BASELINING' || stage.status === 'GATE_CHECKING'

async function fetch() {
  loading.value = true
  try {
    const res = await getProjectStages(projectId); items.value = res.data.data || []
    await fetchAllCatalogs()
  } finally { loading.value = false }
}

async function fetchAllCatalogs() {
  for (const stage of items.value) {
    if (!stage.id) continue
    try {
      const res = await getDocCatalogs({ projectId, stageId: stage.id })
      catalogsMap.value[stage.id] = res.data.data?.records || []
    } catch { catalogsMap.value[stage.id] = [] }
  }
}

async function handleGenerateCatalog(stageId: number) {
  catalogsLoading.value[stageId] = true
  try {
    await generateCatalog({ projectId, stageId, overwrite: true })
    const res = await getDocCatalogs({ projectId, stageId })
    const records = res.data.data?.records || []
    catalogsMap.value[stageId] = records
    catalogsExpanded.value[stageId] = true
    ElMessage.success(`AI 生成完成，共 ${records.length} 项`)
  } catch {
    ElMessage.error('AI 生成失败，请确认 Ollama 服务是否正常运行')
  } finally { catalogsLoading.value[stageId] = false }
}

function toggleCatalog(stageId: number) {
  catalogsExpanded.value[stageId] = !catalogsExpanded.value[stageId]
}

const typeNameMap = ref<Record<string, string>>({})

function docTypeLabel(t?: string) {
  if (!t) return '-'
  return typeNameMap.value[t] || t
}

async function loadDocTypeNames() {
  try {
    const res = await getDictItems('DOC_TYPE')
    const types = res.data.data || []
    for (const t of types) {
      typeNameMap.value[t.dictCode] = t.dictName
    }
  } catch { /* ignore */ }
}

async function handleGenerateByStage(stage: ProjectStageItem) {
  if (!stage.id || !stage.stageCode) {
    ElMessage.warning('阶段缺少 stageCode，无法按模板生成')
    return
  }
  catalogsLoading.value[stage.id] = true
  try {
    await generateCatalogByStage(projectId, stage.id, stage.stageCode, true)
    const res = await getDocCatalogs({ projectId, stageId: stage.id })
    const records = res.data.data?.records || []
    catalogsMap.value[stage.id] = records
    catalogsExpanded.value[stage.id] = true
    ElMessage.success(`GJB 5882 模板生成完成，共 ${records.length} 项`)
  } catch {
    ElMessage.error('模板生成失败')
  } finally { catalogsLoading.value[stage.id] = false }
}

async function handleRequestTransition(stage: ProjectStageItem) {
  transitioningStage.value = stage
  gateResult.value = null
  transitionVisible.value = true
  try {
    const res = await gateCheck(projectId, stage.id!)
    gateResult.value = res.data.data
  } catch { /* handled */ }
}

async function handleConfirmTransition() {
  saving.value = true
  try {
    const res = await requestTransition(projectId, transitioningStage.value!.id!)
    ElMessage.success(res.data.data.message || '转阶段成功')
    transitionVisible.value = false
    fetch()
  } catch { /* handled */ } finally { saving.value = false }
}

function showEditDialog(row: ProjectStageItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    await updateProjectStage(projectId, editingId.value!, { ...form })
    ElMessage.success('更新成功')
    dialogVisible.value = false; fetch()
  } catch { /* handled */ } finally { saving.value = false }
}

async function handleSuspend(stage: ProjectStageItem) {
  try {
    await ElMessageBox.confirm('确定暂停阶段"' + stage.stageName + '"吗？', '确认暂停', { type: 'warning' })
    await suspendStage(projectId, stage.id!)
    ElMessage.success('阶段已暂停')
    fetch()
  } catch { /* cancelled */ }
}

async function handleTerminate(stage: ProjectStageItem) {
  try {
    await ElMessageBox.confirm('确定终止阶段"' + stage.stageName + '"吗？此操作不可逆。', '确认终止', { type: 'warning' })
    await terminateStage(projectId, stage.id!)
    ElMessage.success('阶段已终止')
    fetch()
  } catch { /* cancelled */ }
}

async function handleCheckHealth() {
  try {
    const res = await checkAiHealth()
    const data = res.data.data
    aiOnline.value = data?.modelLoaded === true && data?.connected === true
    aiModel.value = data?.model || ''
    if (aiOnline.value) {
      ElMessage.success(`大模型已连接 (${aiModel.value})`)
    } else if (data?.connected) {
      ElMessage.warning(`Ollama 已连接，但模型 ${aiModel.value} 未加载，请运行: ollama pull ${aiModel.value}`)
    } else {
      ElMessage.error('Ollama 服务未启动，请确认 Ollama 已安装并运行')
    }
  } catch {
    aiOnline.value = false
    ElMessage.error('无法连接 Ollama 服务')
  }
}

onMounted(() => { fetch(); handleCheckHealth(); loadDocTypeNames() })
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h3 { margin: 0; font-size: 18px; }
.subtitle { color: #999; font-size: 13px; margin-left: 12px; }
.header-actions { display: flex; gap: 8px; align-items: center; }

.roadmap { display: flex; flex-direction: column; align-items: center; gap: 0; }

.stage-card-wrapper { display: flex; flex-direction: column; align-items: center; width: 100%; max-width: 520px; }

.stage-card { width: 100%; border: 2px solid #e4e7ed; border-radius: 8px; padding: 16px; background: #fafafa; transition: all 0.3s; }
.stage-card.status-in_progress { border-color: #409eff; background: #ecf5ff; }
.stage-card.status-completed { border-color: #67c23a; background: #f0f9eb; }
.stage-card.status-rectifying { border-color: #f56c6c; background: #fef0f0; }
.stage-card.status-gate_checking { border-color: #e6a23c; background: #fdf6ec; }
.stage-card.status-baselining { border-color: #9b59b6; background: #f4ecf7; }
.stage-card.status-suspended { border-color: #e6a23c; background: #fdf6ec; opacity: 0.7; }
.stage-card.status-terminated { border-color: #909399; background: #f2f3f5; opacity: 0.6; }

.stage-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.stage-order { font-size: 24px; font-weight: 700; color: #ddd; }

.stage-body h4 { margin: 0 0 4px 0; font-size: 15px; color: #303133; }
.stage-code { color: #909399; font-size: 12px; margin: 0 0 8px 0; }
.stage-dates { font-size: 13px; color: #606266; margin-bottom: 4px; }
.stage-dates .arrow { margin: 0 8px; color: #c0c4cc; }
.stage-goal { font-size: 12px; color: #909399; margin: 4px 0 0 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 400px; }

.stage-actions { margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap; }

.stage-connector { padding: 4px 0; color: #c0c4cc; font-size: 12px; }

.stage-def-item { padding: 8px 0; border-bottom: 1px solid #f0f0f0; }

/* Catalog section */
.stage-catalog { margin-top: 12px; border-top: 1px solid #ebeef5; padding-top: 10px; }
.catalog-toggle { display: flex; align-items: center; gap: 4px; cursor: pointer; font-size: 13px; color: #606266; user-select: none; }
.catalog-toggle:hover { color: #409eff; }
.catalog-list { margin-top: 8px; padding-left: 4px; }
.catalog-empty { color: #c0c4cc; font-size: 12px; text-align: center; padding: 8px 0; }
.catalog-item { display: flex; align-items: center; gap: 6px; padding: 4px 0; font-size: 12px; }
.catalog-code { color: #909399; font-family: monospace; min-width: 40px; }
.catalog-name { flex: 1; color: #303133; }
.stage-def-item:last-child { border-bottom: none; }
.def-name { font-weight: 600; color: #303133; }
.def-desc { color: #909399; font-size: 12px; }
</style>
