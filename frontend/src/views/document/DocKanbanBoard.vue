<template>
  <div class="kanban-page">
    <!-- Header -->
    <div class="kanban-header">
      <div class="header-left">
        <el-select v-model="selectedStageId" placeholder="筛选阶段" clearable style="width:220px" @change="loadKanban">
          <el-option v-for="s in stages" :key="s.id" :label="s.stageName" :value="s.id" />
        </el-select>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>新建文档条目
        </el-button>
      </div>
    </div>

    <!-- Kanban columns -->
    <div class="kanban-board">
      <div
        v-for="col in columns"
        :key="col.key"
        class="kanban-column"
        :class="{ 'drag-over': dragOverColumn === col.key }"
        @dragover.prevent="dragOverColumn = col.key"
        @dragleave="dragOverColumn = dragOverColumn === col.key ? null : dragOverColumn"
        @drop="handleDrop($event, col.key)"
      >
        <div class="column-header">
          <span class="column-title">{{ col.label }}</span>
          <el-tag size="small" :type="col.tagType">{{ getColumnCount(col.key) }}</el-tag>
        </div>
        <div class="column-body">
          <div
            v-for="item in getColumnItems(col.key)"
            :key="item.id"
            class="kanban-card"
            :draggable="canTransitionFrom(col.key)"
            @dragstart="handleDragStart($event, item)"
            @click="showDetail(item)"
          >
            <div class="card-code">{{ item.docCode || '—' }}</div>
            <div class="card-name">{{ item.docName }}</div>
            <div class="card-tags">
              <el-tag size="small" type="info">{{ item.docType || '-' }}</el-tag>
              <el-tag v-if="item.securityLevel" size="small" :type="item.securityLevel === 'TOP_SECRET' || item.securityLevel === 'SECRET' ? 'danger' : 'warning'">
                {{ securityLabel(item.securityLevel) }}
              </el-tag>
            </div>
            <!-- AI buttons per column -->
            <div v-if="col.key === 'PLANNED'" class="card-actions">
              <el-button size="small" type="primary" link @click.stop="generateDraft(item)">AI 生成初稿</el-button>
            </div>
            <div v-if="col.key === 'DRAFTING'" class="card-actions">
              <el-button size="small" type="warning" link @click.stop="aiProofread(item)">AI 校对</el-button>
            </div>
            <div v-if="col.key === 'CHECKING'" class="card-actions">
              <el-button size="small" type="warning" link @click.stop="aiPreReview(item)">AI 预评审</el-button>
            </div>
          </div>
          <div v-if="getColumnCount(col.key) === 0" class="column-empty">拖拽卡片至此列</div>
        </div>
      </div>
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建文档台账条目" width="480px" @closed="resetForm">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="文档名称" required>
          <el-input v-model="createForm.docName" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="文档编号">
          <el-input v-model="createForm.docCode" placeholder="如 GJB-XXX-001" />
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="createForm.docType" style="width:100%">
            <el-option label="方案论证报告" value="FEASIBILITY_REPORT" />
            <el-option label="设计说明书" value="DESIGN_SPEC" />
            <el-option label="测试大纲" value="TEST_OUTLINE" />
            <el-option label="试验报告" value="TEST_REPORT" />
            <el-option label="评审报告" value="REVIEW_REPORT" />
            <el-option label="综合保障方案" value="SUPPORT_PLAN" />
            <el-option label="管理文档" value="MANAGEMENT_DOC" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="密级">
          <el-select v-model="createForm.securityLevel" style="width:100%">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="秘密" value="SECRET" />
            <el-option label="机密" value="CONFIDENTIAL" />
            <el-option label="绝密" value="TOP_SECRET" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必需">
          <el-switch v-model="createForm.requiredFlag" />
        </el-form-item>
        <el-form-item label="所属阶段">
          <el-select v-model="createForm.stageId" style="width:100%" clearable placeholder="选择阶段">
            <el-option v-for="s in stages" :key="s.id" :label="s.stageName" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="showDrawer" title="文档详情" size="480px">
      <template v-if="selectedItem">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="ID">{{ selectedItem.id }}</el-descriptions-item>
          <el-descriptions-item label="文档编号">{{ selectedItem.docCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文档名称">{{ selectedItem.docName }}</el-descriptions-item>
          <el-descriptions-item label="文档类型">{{ selectedItem.docType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="密级">{{ securityLabel(selectedItem.securityLevel || '') }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTagType(selectedItem.lifecycleStatus!)">{{ statusLabel(selectedItem.lifecycleStatus!) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否必需">
            <el-tag :type="selectedItem.requiredFlag ? 'danger' : 'info'" size="small">{{ selectedItem.requiredFlag ? '必需' : '非必需' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="阶段ID">{{ selectedItem.stageId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ selectedItem.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Status transition -->
        <div style="margin-top:16px">
          <h4 style="margin-bottom:8px">状态转移</h4>
          <el-select v-model="targetStatus" placeholder="选择目标状态" style="width:100%">
            <el-option
              v-for="s in allowedTransitions(selectedItem.lifecycleStatus!)"
              :key="s"
              :label="statusLabel(s)"
              :value="s"
            />
          </el-select>
          <el-button
            type="primary"
            :disabled="!targetStatus"
            :loading="transitioning"
            style="margin-top:8px;width:100%"
            @click="handleTransition"
          >转移</el-button>
        </div>

        <!-- Logs timeline -->
        <div style="margin-top:24px">
          <h4 style="margin-bottom:8px">操作日志</h4>
          <el-timeline v-if="logs.length > 0">
            <el-timeline-item
              v-for="log in logs"
              :key="log.id"
              :timestamp="log.operatedAt"
              placement="top"
            >
              <span v-if="log.fromStatus">{{ statusLabel(log.fromStatus) }} → </span>
              {{ statusLabel(log.toStatus) }}
              <div v-if="log.remark" style="color:#999;font-size:12px">{{ log.remark }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作日志" :image-size="40" />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getKanbanData, createDocLedger, transitionStatus, getDocLedgerLogs,
  type DocLedgerItem, type DocLedgerLogItem
} from '@/api/doc-ledger'
import { getProjectStages, type ProjectStageItem } from '@/api/project-stage'

const route = useRoute()
const projectId = Number(route.params.projectId)

const columns = [
  { key: 'PLANNED',  label: '策划',     tagType: '' },
  { key: 'DRAFTING', label: '起草',     tagType: 'info' },
  { key: 'CHECKING', label: '校对',     tagType: 'warning' },
  { key: 'REVIEWING',label: '评审',     tagType: 'warning' },
  { key: 'APPROVING',label: '批准',     tagType: '' },
  { key: 'RELEASED', label: '已发布',   tagType: 'success' },
  { key: 'ARCHIVED', label: '已归档',   tagType: 'info' }
]

const TRANSITIONS: Record<string, string[]> = {
  PLANNED:  ['DRAFTING'],
  DRAFTING: ['CHECKING', 'PLANNED'],
  CHECKING: ['REVIEWING', 'DRAFTING'],
  REVIEWING:['APPROVING', 'DRAFTING'],
  APPROVING:['RELEASED', 'DRAFTING'],
  RELEASED: ['ARCHIVED', 'DRAFTING']
}

const kanbanData = ref<Record<string, DocLedgerItem[]>>({})
const stages = ref<ProjectStageItem[]>([])
const selectedStageId = ref<number | null>(null)
const showCreateDialog = ref(false)
const creating = ref(false)
const showDrawer = ref(false)
const selectedItem = ref<DocLedgerItem | null>(null)
const targetStatus = ref('')
const transitioning = ref(false)
const logs = ref<DocLedgerLogItem[]>([])
const dragOverColumn = ref<string | null>(null)
let draggedItem: DocLedgerItem | null = null

const createForm = reactive({
  docName: '', docCode: '', docType: 'MANAGEMENT_DOC',
  securityLevel: 'INTERNAL', requiredFlag: true, stageId: null as number | null
})

function getColumnItems(key: string) { return kanbanData.value[key] || [] }
function getColumnCount(key: string) { return (kanbanData.value[key] || []).length }

function securityLabel(s: string) {
  const map: Record<string, string> = {
    PUBLIC:'公开',INTERNAL:'内部',SECRET:'秘密',CONFIDENTIAL:'机密',TOP_SECRET:'绝密'
  }
  return map[s] || s
}
function statusLabel(s: string) {
  const map: Record<string, string> = {
    PLANNED:'策划',DRAFTING:'起草',CHECKING:'校对',REVIEWING:'评审',
    APPROVING:'批准',RELEASED:'已发布',ARCHIVED:'已归档'
  }
  return map[s] || s
}
function statusTagType(s: string) {
  const map: Record<string, string> = {
    PLANNED:'',DRAFTING:'info',CHECKING:'warning',REVIEWING:'warning',
    APPROVING:'',RELEASED:'success',ARCHIVED:'info'
  }
  return map[s] || 'info'
}
function allowedTransitions(current: string) { return TRANSITIONS[current] || [] }
function canTransitionFrom(key: string) { return !!(TRANSITIONS[key] && TRANSITIONS[key].length > 0) }

async function loadKanban() {
  try {
    const res = await getKanbanData(projectId, selectedStageId.value || undefined)
    kanbanData.value = res.data.data
  } catch { /* ignore */ }
}
async function loadStages() {
  try {
    const res = await getProjectStages(projectId)
    stages.value = res.data.data || []
  } catch { /* ignore */ }
}

function handleDragStart(ev: DragEvent, item: DocLedgerItem) { draggedItem = item }
function handleDrop(ev: DragEvent, targetColumn: string) {
  dragOverColumn.value = null
  if (!draggedItem || draggedItem.lifecycleStatus === targetColumn) return
  const allowed = allowedTransitions(draggedItem.lifecycleStatus!)
  if (!allowed.includes(targetColumn)) {
    ElMessage.warning(`不允许从 ${statusLabel(draggedItem.lifecycleStatus!)} 直接转移到 ${statusLabel(targetColumn)}`)
    return
  }
  doTransition(draggedItem.id!, targetColumn)
}
async function doTransition(id: number, target: string) {
  try {
    await transitionStatus(id, target)
    ElMessage.success('状态转移成功')
    loadKanban()
  } catch {
    ElMessage.error('状态转移失败')
  }
}

async function handleCreate() {
  if (!createForm.docName) { ElMessage.warning('请输入文档名称'); return }
  creating.value = true
  try {
    await createDocLedger({ ...createForm, projectId })
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    loadKanban()
  } catch {
    ElMessage.error('创建失败')
  }
  creating.value = false
}
function resetForm() {
  Object.assign(createForm, { docName:'',docCode:'',docType:'MANAGEMENT_DOC',securityLevel:'INTERNAL',requiredFlag:true,stageId:null })
}

async function showDetail(item: DocLedgerItem) {
  selectedItem.value = item
  targetStatus.value = ''
  showDrawer.value = true
  try {
    const res = await getDocLedgerLogs(item.id!)
    logs.value = res.data.data || []
  } catch { logs.value = [] }
}
async function handleTransition() {
  if (!selectedItem.value || !targetStatus.value) return
  transitioning.value = true
  try {
    await transitionStatus(selectedItem.value.id!, targetStatus.value)
    ElMessage.success('状态转移成功')
    selectedItem.value.lifecycleStatus = targetStatus.value
    targetStatus.value = ''
    loadKanban()
    // Refresh logs
    const res = await getDocLedgerLogs(selectedItem.value.id!)
    logs.value = res.data.data || []
  } catch {
    ElMessage.error('状态转移失败')
  }
  transitioning.value = false
}

// AI integration buttons
function generateDraft(item: DocLedgerItem) {
  ElMessage.info('AI 初稿生成功能：请前往 AI 辅助页面，选择对应目录条目生成初稿')
}
function aiProofread(item: DocLedgerItem) {
  ElMessage.info('AI 校对功能将在后续版本中推出')
}
function aiPreReview(item: DocLedgerItem) {
  ElMessage.info('AI 预评审功能将在后续版本中推出')
}

onMounted(() => { loadKanban(); loadStages() })
</script>

<style scoped>
.kanban-page { padding: 16px 0; }
.kanban-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }

.kanban-board { display:flex; gap:12px; overflow-x:auto; padding-bottom:8px; min-height:60vh; }
.kanban-column {
  flex:1; min-width:180px; max-width:260px;
  background:var(--el-fill-color-lighter); border-radius:8px;
  display:flex; flex-direction:column; transition: background .2s;
}
.kanban-column.drag-over { background:var(--el-color-primary-light-9); }
.column-header { padding:12px 12px 8px; display:flex; justify-content:space-between; align-items:center; }
.column-title { font-weight:600; font-size:14px; }
.column-body { padding:0 8px 8px; flex:1; overflow-y:auto; }
.column-empty { text-align:center; color:var(--el-text-color-placeholder); font-size:12px; padding:24px 0; }
.kanban-card {
  background:#fff; border-radius:6px; padding:10px; margin-bottom:8px;
  cursor:grab; box-shadow:0 1px 3px rgba(0,0,0,.08);
  transition: box-shadow .2s, transform .15s;
}
.kanban-card:hover { box-shadow:0 2px 8px rgba(0,0,0,.15); transform:translateY(-1px); }
.kanban-card:active { cursor:grabbing; }
.card-code { font-size:11px; color:var(--el-text-color-placeholder); margin-bottom:4px; }
.card-name { font-size:13px; font-weight:500; margin-bottom:6px; line-height:1.4; }
.card-tags { display:flex; gap:4px; flex-wrap:wrap; }
.card-actions { margin-top:8px; padding-top:6px; border-top:1px solid var(--el-border-color-lighter); }
.header-left, .header-right { display:flex; align-items:center; gap:8px; }
</style>
