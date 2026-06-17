<template>
  <div class="batch-generate">
    <div class="bg-toolbar">
      <el-select v-model="selectedEquipmentType" placeholder="产品类型" @change="loadChecklist" style="width: 160px">
        <el-option v-for="et in equipmentTypes" :key="et.label" :label="et.label" :value="et.label">
          <span>{{ et.label }}</span>
          <span style="float:right;color:var(--el-text-color-secondary);font-size:12px">{{ et.stageFlow }}</span>
        </el-option>
      </el-select>
      <el-select v-model="selectedStageId" placeholder="选择阶段" @change="loadChecklist" style="width: 240px">
        <el-option v-for="s in stages" :key="s.id" :label="s.stageName" :value="s.id" />
      </el-select>
      <el-button type="primary" :loading="generating" @click="startBatch" :disabled="!selectedStageId">
        <el-icon><VideoPlay /></el-icon>
        {{ generating ? '生成中...' : '开始批量生成' }}
      </el-button>
      <el-button v-if="generating" type="danger" @click="cancelBatch" plain>
        <el-icon><Close /></el-icon>取消
      </el-button>
    </div>

    <!-- Phase progress -->
    <div v-if="generating" class="progress-section">
      <GenerationProgressBar
        :current-phase="currentPhase"
        :current="completedDocs"
        :total="totalDocs"
        :done-count="doneCount"
        :error-count="errorCount"
        :current-doc-name="currentDoc"
        :eta-seconds="etaSeconds"
      />
    </div>

    <!-- Results table -->
    <el-table :data="checklistItems" v-loading="loadingChecklist" stripe max-height="calc(100vh - 280px)">
      <el-table-column prop="docCode" label="编号" width="100" />
      <el-table-column prop="docName" label="文档名称" min-width="200" />
      <el-table-column prop="category" label="类别" width="120" />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag v-if="resultMap[row.templateId]?.status === 'done'" type="success" size="small">已生成</el-tag>
          <el-tag v-else-if="resultMap[row.templateId]?.status === 'error'" type="danger" size="small">失败</el-tag>
          <el-tag v-else-if="resultMap[row.templateId]?.status === 'generating'" type="warning" size="small" effect="dark">生成中</el-tag>
          <el-tag v-else type="info" size="small">待生成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="resultMap[row.templateId]?.docLedgerId" link type="primary" size="small"
            @click="$router.push({ name: 'DocAssembly', params: { projectId, docLedgerId: resultMap[row.templateId].docLedgerId } })">
            查看
          </el-button>
          <span v-if="resultMap[row.templateId]?.status === 'error'" class="error-msg" :title="resultMap[row.templateId]?.message">
            {{ resultMap[row.templateId]?.message?.substring(0, 30) }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoPlay, Close } from '@element-plus/icons-vue'
import { getProjectStages, type ProjectStageItem as ProjectStage } from '@/api/project-stage'
import { getChecklist, getEquipmentTypes, type ProjectDocChecklistItem as ProjectDocChecklist, type EquipmentType } from '@/api/checklist'
import { streamBatchGenerate, cancelBatchGenerate, type BatchEvent } from '@/api/ai'
import GenerationProgressBar from '@/components/GenerationProgressBar.vue'

const route = useRoute()
const projectId = computed(() => Number(route.params.projectId))

const stages = ref<ProjectStage[]>([])
const selectedStageId = ref<number | null>(null)
const equipmentTypes = ref<EquipmentType[]>([])
const selectedEquipmentType = ref<string | undefined>(undefined)
const checklistItems = ref<ProjectDocChecklist[]>([])
const loadingChecklist = ref(false)
const generating = ref(false)
const currentDoc = ref('')
const totalDocs = ref(0)
const completedDocs = ref(0)
const doneCount = ref(0)
const errorCount = ref(0)
const currentPhase = ref('')
const etaSeconds = ref(0)
let abortController: AbortController | null = null

interface ResultEntry {
  status: string
  docLedgerId?: number
  message?: string
}
const resultMap = ref<Record<number, ResultEntry>>({})

const progress = computed(() => {
  if (totalDocs.value === 0) return 0
  return completedDocs.value / totalDocs.value
})

onMounted(async () => {
  try {
    const [stageRes, etRes] = await Promise.all([
      getProjectStages(projectId.value),
      getEquipmentTypes()
    ])
    stages.value = stageRes.data.data || []
    equipmentTypes.value = etRes.data.data || []
    if (equipmentTypes.value.length > 0) {
      selectedEquipmentType.value = equipmentTypes.value[0].label
    }
    if (stages.value.length > 0) {
      selectedStageId.value = stages.value[0].id
      loadChecklist()
    }
  } catch { /* ignore */ }
})

async function loadChecklist() {
  if (!selectedStageId.value) return
  loadingChecklist.value = true
  resultMap.value = {}
  try {
    const res = await getChecklist(projectId.value, selectedStageId.value)
    checklistItems.value = res.data.data || []
  } catch { /* ignore */ }
  finally { loadingChecklist.value = false }
}

// Convert SSE event.current (1-based index) to checklist item's templateId
// so resultMap keys align with table template lookups (row.templateId)
function getTemplateId(idx: number): number | undefined {
  if (idx > 0 && idx <= checklistItems.value.length) {
    return checklistItems.value[idx - 1].templateId
  }
  return undefined
}

function startBatch() {
  if (!selectedStageId.value) return
  generating.value = true
  completedDocs.value = 0
  doneCount.value = 0
  errorCount.value = 0
  totalDocs.value = checklistItems.value.length
  currentDoc.value = ''
  currentPhase.value = ''
  etaSeconds.value = 0
  resultMap.value = {}

  abortController = streamBatchGenerate(
    projectId.value,
    selectedStageId.value,
    (event: BatchEvent) => {
      const templateId = getTemplateId(event.current)
      if (event.type === 'phase_start') {
        currentPhase.value = event.phase || ''
        if (event.estimatedTotalSeconds) etaSeconds.value = event.estimatedTotalSeconds
      } else if (event.type === 'doc_start') {
        currentDoc.value = event.docName || ''
        if (event.docLedgerId && templateId) {
          resultMap.value[templateId] = { status: 'generating', docLedgerId: event.docLedgerId }
        }
      } else if (event.type === 'doc_done') {
        completedDocs.value = event.current
        doneCount.value++
        if (event.docLedgerId && templateId) {
          resultMap.value[templateId] = { status: 'done', docLedgerId: event.docLedgerId, message: event.message }
        }
        if (event.estimatedTotalSeconds) etaSeconds.value = event.estimatedTotalSeconds
      } else if (event.type === 'doc_error') {
        completedDocs.value = event.current
        errorCount.value++
        if (templateId) {
          resultMap.value[templateId] = { status: 'error', docLedgerId: event.docLedgerId || undefined, message: event.message }
        }
      } else if (event.type === 'batch_complete' || event.type === 'batch_cancelled') {
        generating.value = false
        if (event.type === 'batch_cancelled') {
          ElMessage.info(event.message || '批量生成已取消')
        } else {
          ElMessage.success(event.message || '批量生成完成')
        }
      }
    },
    () => {
      generating.value = false
    },
    (err: Error) => {
      generating.value = false
      ElMessage.error('生成失败: ' + err.message)
    }
  )
}

function cancelBatch() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  if (selectedStageId.value) {
    cancelBatchGenerate(projectId.value, selectedStageId.value).catch(() => {})
  }
  generating.value = false
}
</script>

<style scoped>
.batch-generate { padding: 16px 24px; }
.bg-toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.progress-section { margin-bottom: 16px; }
.current-doc { margin-top: 8px; color: #606266; font-size: 13px; }
.error-msg { color: #f56c6c; font-size: 12px; }
</style>
