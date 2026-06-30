<template>
  <div class="ai-assistant">
    <!-- Catalog Generation Card -->
    <el-card class="feature-card">
      <template #header>
        <div class="card-header">
          <span>文档目录自动生成</span>
          <el-tag type="info" size="small">Phase 1</el-tag>
        </div>
      
  <PrerequisitesCheck
    v-model="prereqVisible"
    :project-id="projectId"
    :doc-type="prereqDocType"
    api="general"
    @proceed="onPrereqProceed"
  />
</template>

      <el-descriptions :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="项目名称">{{ project?.projectName }}</el-descriptions-item>
        <el-descriptions-item label="项目类型">{{ project?.projectType }}</el-descriptions-item>
        <el-descriptions-item label="适用标准" :span="2">
          {{ project?.applicableStandards || '未设置' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form label-width="100px" @submit.prevent>
        <el-form-item label="关联阶段">
          <el-select v-model="stageId" placeholder="选择项目阶段（可选）" clearable style="width: 240px">
            <el-option
              v-for="s in stages"
              :key="s.id"
              :label="s.stageName"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生成选项">
          <el-checkbox v-model="overwrite">覆盖已有目录条目（默认追加，勾选后需二次确认）</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            :loading="previewing"
            :disabled="!healthOk"
            @click="handlePreviewCatalog"
          >
            <el-icon><View /></el-icon>
            预览
          </el-button>
          <el-button
            type="primary"
            :loading="generating"
            :disabled="!healthOk"
            @click="handleGenerateCatalog"
          >
            <el-icon><MagicStick /></el-icon>
            开始生成
          </el-button>
          <el-tag v-if="!healthOk" type="danger" style="margin-left: 8px">
            本地大模型未连接
          </el-tag>
        </el-form-item>
      </el-form>

      <!-- Generation Result -->
      <div v-if="catalogResult.length > 0" style="margin-top: 16px">
        <el-alert
          :title="`已生成 ${catalogResult.length} 条文档目录`"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />
        <el-table :data="catalogResult" border stripe max-height="400">
          <el-table-column prop="docCode" label="文档编号" width="120" />
          <el-table-column prop="docName" label="文档名称" min-width="200" />
          <el-table-column prop="docType" label="文档类型" width="150">
            <template #default="{ row }">
              <el-tag size="small">{{ docTypeLabel(row.docType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requiredFlag" label="必须" width="80">
            <template #default="{ row }">
              <el-tag :type="row.requiredFlag ? 'danger' : 'info'" size="small">
                {{ row.requiredFlag ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'DRAFT' ? 'info' : 'success'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Draft Generation Card -->
    <el-card class="feature-card" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>文档初稿自动生成</span>
          <el-tag type="success" size="small">Phase 2</el-tag>
        </div>
      </template>

      <el-form label-width="100px" @submit.prevent>
        <el-form-item label="选择目录条目">
          <el-select
            v-model="selectedCatalogId"
            placeholder="选择要生成初稿的文档目录条目"
            filterable
            style="width: 100%"
            :disabled="streaming"
          >
            <el-option
              v-for="c in catalogEntries"
              :key="c.id"
              :label="`${c.docCode} - ${c.docName}`"
              :value="c.id!"
            >
              <span style="float: left">{{ c.docCode }}</span>
              <span style="float: right; color: var(--el-text-color-secondary); font-size: 13px">{{ c.docName }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="streaming"
            :disabled="!healthOk || !selectedCatalogId"
            @click="handleGenerateDraft"
          >
            <el-icon><EditPen /></el-icon>
            {{ streaming ? '生成中...' : '开始生成初稿' }}
          </el-button>
          <el-button
            v-if="streaming"
            type="danger"
            plain
            @click="handleStopStreaming"
          >
            停止
          </el-button>
          <el-button
            v-if="draftContent && !streaming"
            type="success"
            :loading="saving"
            @click="handleSaveDraft"
          >
            <el-icon><FolderAdd /></el-icon>
            保存为文档
          </el-button>
          <span v-if="streaming" class="streaming-status">
            <el-icon class="is-loading"><Loading /></el-icon>
            正在生成，已接收 {{ draftCharCount }} 字
          </span>
        </el-form-item>
      </el-form>

      <!-- Draft output area -->
      <div v-if="draftContent || streaming" class="draft-output">
        <div class="draft-output-header">
          <span>生成结果</span>
          <el-button link size="small" @click="draftContent = ''; draftCharCount = 0" :disabled="streaming">
            清空
          </el-button>
        </div>
        <div class="draft-content" ref="draftContentRef">
          <div v-if="!draftContent && streaming" class="draft-placeholder">
            等待模型响应...
          </div>
          <div class="markdown-body" v-html="renderMarkdown(draftContent)" />
        </div>
      </div>
    </el-card>

    <!-- 预览弹窗 -->
    <el-dialog v-model="showPreviewDialog" title="目录生成预览" width="700px">
      <div v-if="previewData">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="AI 生成">{{ previewData.totalGenerated }} 条</el-descriptions-item>
          <el-descriptions-item label="新增">{{ previewData.newCount }} 条</el-descriptions-item>
          <el-descriptions-item label="冲突(跳过)">{{ previewData.conflictCount }} 条</el-descriptions-item>
        </el-descriptions>
        <div v-if="previewData.newItems?.length" style="margin-top: 16px">
          <h4>将新增的目录项：</h4>
          <el-table :data="previewData.newItems" size="small" max-height="300">
            <el-table-column prop="docCode" label="文档编号" width="150" />
            <el-table-column prop="docName" label="文档名称" />
            <el-table-column prop="docType" label="类型" width="120" />
          </el-table>
        </div>
        <div v-if="previewData.conflictItems?.length" style="margin-top: 16px">
          <h4 style="color: #e6a23c">将跳过的重复条目：</h4>
          <el-table :data="previewData.conflictItems" size="small" max-height="200">
            <el-table-column prop="docCode" label="文档编号" width="150" />
            <el-table-column prop="docName" label="文档名称" />
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPreviewDialog = false">关闭</el-button>
        <el-button type="primary" @click="showPreviewDialog = false; handleGenerateCatalog()">
          确认生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 覆盖确认弹窗 -->
    <el-dialog v-model="showOverwriteConfirm" title="确认覆盖" width="450px">
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>
          覆盖模式将删除当前项目/阶段下所有已有目录条目，并重新生成。
        </template>
        此操作不可撤销！建议先使用"预览"功能查看将生成的内容。
      </el-alert>
      <template #footer>
        <el-button @click="showOverwriteConfirm = false">取消</el-button>
        <el-button type="danger" @click="handleGenerateCatalog()">
          确认覆盖
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { MagicStick, EditPen, FolderAdd, Loading, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getProject, type ProjectItem } from '@/api/project'
import { getProjectStages } from '@/api/project-stage'
import { getDocCatalogsByProject, type DocCatalogItem } from '@/api/doc-catalog'
import {
  generateCatalog,
  previewCatalog,
  checkAiHealth,
  streamDraft,
  saveDraft,
  type DocCatalog,
  type CatalogPreviewResult
} from '@/api/ai'
import PrerequisitesCheck from '@/components/PrerequisitesCheck.vue'
import { sanitizeHtml } from '@/utils/sanitize'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const projectId = computed(() => Number(route.params.projectId))

const project = ref<ProjectItem | null>(null)
const stages = ref<any[]>([])
const stageId = ref<number | undefined>()
const overwrite = ref(false)
const generating = ref(false)
const saving = ref(false)
const previewing = ref(false)
const previewData = ref<CatalogPreviewResult | null>(null)
const showPreviewDialog = ref(false)
const showOverwriteConfirm = ref(false)
const healthOk = ref(false)
const catalogResult = ref<DocCatalog[]>([])

// Draft generation state
const catalogEntries = ref<DocCatalogItem[]>([])
const selectedCatalogId = ref<number | null>(null)
const streaming = ref(false)
const draftContent = ref('')
const draftCharCount = ref(0)
const draftContentRef = ref<HTMLElement>()
let abortController: AbortController | null = null

function docTypeLabel(type: string) {
  const map: Record<string, string> = {
    DESIGN_DOC: '设计文档',
    TEST_DOC: '测试文档',
    MANAGEMENT_DOC: '管理文档',
    QUALITY_DOC: '质量文档',
    REVIEW_DOC: '评审文档'
  }
  return map[type] || type
}

async function loadData() {
  try {
    const [projRes, stagesRes, catRes] = await Promise.all([
      getProject(projectId.value),
      getProjectStages(projectId.value),
      getDocCatalogsByProject(projectId.value)
    ])
    project.value = projRes.data.data
    stages.value = stagesRes.data.data || []
    catalogEntries.value = (catRes.data.data || [])
  } catch { /* ignore */ }
}

async function checkHealth() {
  try {
    const res = await checkAiHealth()
    healthOk.value = res.data.data?.connected === true
    if (healthOk.value) {
      // Connection OK, silent
    }
  } catch {
    healthOk.value = false
  }
}

async function handlePreviewCatalog() {
  previewing.value = true
  try {
    const res = await previewCatalog({
      projectId: projectId.value,
      stageId: stageId.value
    })
    previewData.value = res.data.data as CatalogPreviewResult
    showPreviewDialog.value = true
  } catch {
    ElMessage.error('目录预览失败，请检查 AI 连接状态')
  } finally {
    previewing.value = false
  }
}

async function handleGenerateCatalog() {
  // 覆盖模式需要二次确认
  if (overwrite.value && !showOverwriteConfirm.value) {
    showOverwriteConfirm.value = true
    return
  }
  showOverwriteConfirm.value = false

  generating.value = true
  catalogResult.value = []
  try {
    const res = await generateCatalog({
      projectId: projectId.value,
      stageId: stageId.value,
      overwrite: overwrite.value
    })
    // 新格式: {catalogs, totalNew, mode}
    const data = res.data.data
    const catalogs = data?.catalogs || data || []
    const totalNew = data?.totalNew ?? (Array.isArray(catalogs) ? catalogs.length : 0)
    const mode = data?.mode || 'APPEND'

    catalogResult.value = Array.isArray(catalogs) ? catalogs : []
    if (catalogResult.value.length === 0) {
      ElMessage.warning('未能生成新目录，所有条目均已存在（追加模式）或项目输入文件不完整')
    } else if (mode === 'APPEND') {
      ElMessage.success(`成功新增 ${totalNew} 条文档目录`)
    } else {
      ElMessage.success(`成功生成 ${catalogResult.value.length} 条文档目录（已覆盖）`)
    }
    // Refresh catalog entries
    const catRes = await getDocCatalogsByProject(projectId.value)
    catalogEntries.value = (catRes.data.data || [])
  } catch {
    ElMessage.error('目录生成失败，请检查 AI 连接状态')
  } finally {
    generating.value = false
  }
}


// Prerequisites check
const prereqVisible = ref(false)
const prereqDocType = ref('')
const prereqCallback = ref(null as (() => void) | null)

function checkThenGenerate(docType: string, callback: () => void) {
  prereqDocType.value = docType
  prereqCallback.value = callback
  prereqVisible.value = true
}

function onPrereqProceed() {
  if (prereqCallback.value) prereqCallback.value()
}

function handleGenerateDraft() {
  if (!selectedCatalogId.value) {
    ElMessage.warning('请先选择要生成初稿的目录条目')
    return
  }
  if (!healthOk.value) {
    ElMessage.error('本地大模型未连接，无法生成初稿')
    return
  }

  streaming.value = true
  draftContent.value = ''
  draftCharCount.value = 0

  abortController = streamDraft(
    projectId.value,
    selectedCatalogId.value,
    null,
    (chunk: string) => {
      draftContent.value += chunk
      draftCharCount.value = draftContent.value.length
      nextTick(() => {
        if (draftContentRef.value) {
          draftContentRef.value.scrollTop = draftContentRef.value.scrollHeight
        }
      })
    },
    (fullText: string) => {
      draftContent.value = fullText
      draftCharCount.value = fullText.length
      streaming.value = false
      abortController = null
      ElMessage.success(`初稿生成完成，共 ${draftCharCount.value} 字`)
    },
    (err: Error) => {
      streaming.value = false
      abortController = null
      ElMessage.error(`生成失败: ${err.message}`)
    }
  )
}

function handleStopStreaming() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  streaming.value = false
  ElMessage.info('已停止生成')
}

async function handleSaveDraft() {
  if (!draftContent.value) return
  const selected = catalogEntries.value.find(c => c.id === selectedCatalogId.value)
  saving.value = true
  try {
    await saveDraft({
      projectId: projectId.value,
      catalogId: selectedCatalogId.value ?? undefined,
      stageId: stageId.value,
      docName: selected?.docName || 'AI 生成文档',
      docType: selected?.docType || 'MANAGEMENT_DOC',
      securityLevel: project.value?.securityLevel || '内部',
      content: draftContent.value
    })
    ElMessage.success('文档初稿已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
  // Defer health check to not block initial render
  setTimeout(() => checkHealth(), 1000)
})
</script>

<style scoped>
.ai-assistant {
  padding: 0;
}
.feature-card {
  border-radius: 8px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.streaming-status {
  margin-left: 12px;
  color: var(--el-color-warning);
  font-size: 13px;
}
.streaming-status .el-icon {
  margin-right: 4px;
}
.draft-output {
  margin-top: 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
}
.draft-output-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color);
  font-weight: 500;
  font-size: 14px;
}
.draft-content {
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
  background: #fff;
  font-size: 14px;
  line-height: 1.8;
}
.draft-placeholder {
  color: var(--el-text-color-placeholder);
  text-align: center;
  padding: 24px;
}
.markdown-body :deep(h1) {
  font-size: 20px;
  margin: 16px 0 8px;
  border-bottom: 1px solid var(--el-border-color);
  padding-bottom: 4px;
}
.markdown-body :deep(h2) {
  font-size: 18px;
  margin: 14px 0 6px;
}
.markdown-body :deep(h3) {
  font-size: 16px;
  margin: 12px 0 4px;
}
.markdown-body :deep(h4) {
  font-size: 15px;
  margin: 10px 0 4px;
}
.markdown-body :deep(p) {
  margin: 4px 0;
}
.markdown-body :deep(ul) {
  margin: 4px 0;
  padding-left: 24px;
}
.markdown-body :deep(li) {
  margin: 2px 0;
}
.markdown-body :deep(strong) {
  font-weight: 600;
}
</style>
