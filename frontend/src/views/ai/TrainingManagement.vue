<template>
  <div class="training-mgmt">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模型训练数据管理</span>
          <div>
            <el-button type="primary" @click="showCollectDialog = true">
              <el-icon><Plus /></el-icon>
              收集训练数据
            </el-button>
            <el-button type="success" :loading="exporting" @click="handleExport">
              <el-icon><Download /></el-icon>
              导出 JSONL
            </el-button>
          </div>
        </div>
      </template>

      <!-- Summary stats -->
      <el-row :gutter="16" style="margin-bottom: 16px">
        <el-col :span="6">
          <el-statistic title="总计" :value="totalCount" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="待审核" :value="pendingCount">
            <template #suffix>
              <el-tag type="warning" size="small">PENDING</el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="已批准" :value="approvedCount">
            <template #suffix>
              <el-tag type="success" size="small">APPROVED</el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="已驳回" :value="rejectedCount">
            <template #suffix>
              <el-tag type="danger" size="small">REJECTED</el-tag>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <!-- Quality filter -->
      <el-radio-group v-model="qualityFilter" @change="loadData" style="margin-bottom: 16px">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="PENDING_REVIEW">待审核</el-radio-button>
        <el-radio-button label="APPROVED">已批准</el-radio-button>
        <el-radio-button label="REJECTED">已驳回</el-radio-button>
      </el-radio-group>

      <!-- Training examples table -->
      <el-table :data="examples" border stripe v-loading="loading" max-height="500">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="projectId" label="项目ID" width="80" />
        <el-table-column label="提示词" min-width="200">
          <template #default="{ row }">
            <span class="text-preview">{{ truncateText(row.prompt, 120) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="生成结果" min-width="200">
          <template #default="{ row }">
            <span class="text-preview">{{ truncateText(row.completion, 120) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quality" label="质量" width="110">
          <template #default="{ row }">
            <el-tag :type="qualityTag(row.quality)" size="small">{{ qualityLabel(row.quality) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link size="small" @click="showPreview(row)">预览</el-button>
            <el-button
              v-if="row.quality === 'PENDING_REVIEW'"
              link size="small" type="success"
              @click="handleApprove(row.id)"
            >
              批准
            </el-button>
            <el-button
              v-if="row.quality === 'PENDING_REVIEW'"
              link size="small" type="danger"
              @click="handleReject(row.id)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="totalCount"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- Collect Dialog -->
    <el-dialog v-model="showCollectDialog" title="收集训练数据" width="500px" @closed="resetCollectForm">
      <el-form label-width="80px">
        <el-form-item label="选择项目">
          <el-select
            v-model="collectProjectId"
            placeholder="选择项目"
            filterable
            style="width: 100%"
            @change="onCollectProjectChange"
          >
            <el-option
              v-for="p in projects"
              :key="p.id"
              :label="p.projectName"
              :value="p.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文档">
          <el-select
            v-model="collectDocFileId"
            placeholder="选择已生成的文档"
            filterable
            style="width: 100%"
            :disabled="!collectProjectId"
          >
            <el-option
              v-for="f in projectDocs"
              :key="f.id"
              :label="`[${f.docType}] ${f.docName}`"
              :value="f.id!"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCollectDialog = false">取消</el-button>
        <el-button type="primary" :loading="collecting" :disabled="!collectDocFileId" @click="handleCollect">
          开始收集
        </el-button>
      </template>
    </el-dialog>

    <!-- Preview Dialog -->
    <el-dialog v-model="showPreviewDialog" title="训练示例预览" width="800px">
      <el-tabs>
        <el-tab-pane label="提示词 (Prompt)">
          <div class="preview-content"><pre>{{ previewPrompt }}</pre></div>
        </el-tab-pane>
        <el-tab-pane label="生成结果 (Completion)">
          <div class="preview-content"><pre>{{ previewCompletion }}</pre></div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { getProjects, type ProjectItem } from '@/api/project'
import { getDocFiles, type DocFileItem } from '@/api/doc-file'
import {
  getTrainingExamples,
  approveTrainingExample,
  rejectTrainingExample,
  collectTraining,
  exportTraining,
  type TrainingExampleItem
} from '@/api/ai'

const examples = ref<TrainingExampleItem[]>([])
const loading = ref(false)
const exporting = ref(false)
const collecting = ref(false)
const page = ref(1)
const size = ref(20)
const totalCount = ref(0)
const pendingCount = ref(0)
const approvedCount = ref(0)
const rejectedCount = ref(0)
const qualityFilter = ref('')

// Collect dialog
const showCollectDialog = ref(false)
const collectProjectId = ref<number | null>(null)
const collectDocFileId = ref<number | null>(null)
const projects = ref<ProjectItem[]>([])
const projectDocs = ref<DocFileItem[]>([])

// Preview dialog
const showPreviewDialog = ref(false)
const previewPrompt = ref('')
const previewCompletion = ref('')

function qualityTag(q: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[q] || 'info'
}

function qualityLabel(q: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: '待审核',
    APPROVED: '已批准',
    REJECTED: '已驳回'
  }
  return map[q] || q
}

function truncateText(text: string, len: number) {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTrainingExamples({
      quality: qualityFilter.value || undefined,
      page: page.value,
      size: size.value
    })
    const pageData = res.data.data
    examples.value = pageData.records || []
    totalCount.value = pageData.total || 0

    // Load counts per quality
    const [allRes, pendingRes, approvedRes, rejectedRes] = await Promise.all([
      getTrainingExamples({ size: 1 }),
      getTrainingExamples({ quality: 'PENDING_REVIEW', size: 1 }),
      getTrainingExamples({ quality: 'APPROVED', size: 1 }),
      getTrainingExamples({ quality: 'REJECTED', size: 1 })
    ])
    pendingCount.value = pendingRes.data.data?.total || 0
    approvedCount.value = approvedRes.data.data?.total || 0
    rejectedCount.value = rejectedRes.data.data?.total || 0
  } catch { /* ignore */ }
  loading.value = false
}

async function loadProjects() {
  try {
    const res = await getProjects({ pageNo: 1, pageSize: 100 })
    projects.value = res.data.data?.records || []
  } catch { /* ignore */ }
}

async function onCollectProjectChange(projectId: number) {
  projectDocs.value = []
  collectDocFileId.value = null
  try {
    const res = await getDocFiles({ projectId, pageNo: 1, pageSize: 100 })
    projectDocs.value = res.data.data?.records || []
  } catch { /* ignore */ }
}

async function handleCollect() {
  if (!collectDocFileId.value || !collectProjectId.value) return
  collecting.value = true
  try {
    await collectTraining({
      docFileId: collectDocFileId.value,
      projectId: collectProjectId.value
    })
    ElMessage.success('训练数据收集成功')
    showCollectDialog.value = false
    loadData()
  } catch {
    ElMessage.error('收集失败')
  }
  collecting.value = false
}

function resetCollectForm() {
  collectProjectId.value = null
  collectDocFileId.value = null
  projectDocs.value = []
}

async function handleApprove(id: number) {
  try {
    await approveTrainingExample(id)
    ElMessage.success('已批准')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleReject(id: number) {
  try {
    await ElMessageBox.confirm('确定驳回此训练示例？', '确认驳回', { type: 'warning' })
    await rejectTrainingExample(id)
    ElMessage.success('已驳回')
    loadData()
  } catch { /* cancelled */ }
}

function showPreview(row: TrainingExampleItem) {
  previewPrompt.value = row.prompt || '(无)'
  previewCompletion.value = row.completion || '(无)'
  showPreviewDialog.value = true
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await exportTraining('APPROVED')
    const data = res.data.data?.data
    if (data) {
      const blob = new Blob([data], { type: 'application/jsonl' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `training-${new Date().toISOString().slice(0, 10)}.jsonl`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('JSONL 已导出')
    } else {
      ElMessage.warning('没有已批准的训练数据可导出')
    }
  } catch {
    ElMessage.error('导出失败')
  }
  exporting.value = false
}

onMounted(() => {
  loadData()
  loadProjects()
})
</script>

<style scoped>
.training-mgmt {
  max-width: 1200px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.text-preview {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.preview-content {
  max-height: 400px;
  overflow-y: auto;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
  padding: 12px;
}
.preview-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
}
</style>
