<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回台账</el-button>
        <h3 style="display:inline;margin-left:16px">文档汇编</h3>
        <el-tag v-if="ledger" size="small" style="margin-left:12px">{{ ledger.docName }}</el-tag>
      </div>
      <div class="header-actions">
        <el-select v-model="selectedTemplateId" placeholder="选择模板初始化章节" style="width:220px" @change="handleInitChapters">
          <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
        </el-select>
        <el-button type="success" @click="showAiDialog = true" :disabled="!ledger?.projectId">
          <el-icon><MagicStick /></el-icon>AI 生成结构
        </el-button>
        <el-button type="primary" @click="showExport = true">
          <el-icon><Download /></el-icon>导出 Word
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" v-if="ledger">
      <!-- Left: Chapter tree -->
      <el-col :xs="24" :sm="24" :md="5" :lg="5" :xl="5">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>章节结构</span>
              <el-tag size="small">{{ completionSummary.filled || 0 }}/{{ completionSummary.total || 0 }}</el-tag>
            </div>
          
    <div v-if="ledger?.id" class="quality-sidebar">
      <QualityPanel :ledger-id="ledger.id" />
    </div>
</template>
          <ChapterTreeViewer
            :tree-data="chapterTree"
            @node-click="selectChapter"
          />
          <div style="margin-top:8px;display:flex;gap:8px">
            <el-button size="small" :loading="validating" @click="handleValidate">
              <el-icon><CircleCheck /></el-icon>校验结构
            </el-button>
            <el-button v-if="validationResult && !validationResult.valid" size="small" type="warning" :loading="fixing" @click="handleFix">
              <el-icon><Tools /></el-icon>一键修复
            </el-button>
          </div>
          <el-alert v-if="validationResult" :title="validationResult.summary"
            :type="validationResult.valid ? 'success' : 'error'"
            :closable="false" show-icon style="margin-top:8px;font-size:12px" />
          <div v-if="validationResult && validationResult.issues.length > 0" style="margin-top:4px;max-height:150px;overflow-y:auto">
            <div v-for="(issue, i) in validationResult.issues" :key="i"
              style="font-size:11px;padding:2px 4px;margin:1px 0;border-radius:2px"
              :style="{background: issue.level==='ERROR'?'#fef0f0':'#fdf6ec'}">
              <el-tag size="small" :type="issue.level==='ERROR'?'danger':'warning'" style="font-size:10px">{{ issue.level }}</el-tag>
              <strong>{{ issue.chapterRef }}</strong> {{ issue.description }}
            </div>
          </div>
          <CompletenessProgressBar
            v-if="completionSummary.total > 0"
            :passed="completionSummary.filled || 0"
            :warnings="completionSummary.partial || 0"
            :errors="completionSummary.empty || 0"
            :total="completionSummary.total || 1"
            style="margin-top:12px"
          />
        </el-card>
      </el-col>

      <!-- Center: Chapter editor -->
      <el-col :xs="24" :sm="24" :md="13" :lg="13" :xl="13">
        <el-card shadow="never">
          <template #header>
            <span v-if="selectedChapter">
              编辑: {{ selectedChapter.chapterNumber }} {{ selectedChapter.chapterTitle }}
            </span>
            <span v-else>请从左侧选择章节</span>
          </template>

          <template v-if="selectedChapter">
            <!-- Three-library writing guide -->
            <ChapterWritingGuide :context="writingContext" />

            <div style="margin-bottom:12px;display:flex;align-items:center;gap:8px;flex-wrap:wrap">
              <el-tag size="small" :type="statusType(selectedChapter.fillStatus)">
                {{ statusLabel(selectedChapter.fillStatus) }}
              </el-tag>
              <el-select v-model="selectedChapter.fillStatus" size="small" style="width:120px" @change="handleStatusChange">
                <el-option label="已完成" value="FILLED" />
                <el-option label="部分完成" value="PARTIAL" />
                <el-option label="待填写" value="EMPTY" />
              </el-select>
              <el-input-number v-model="selectedChapter.fillPercentage" :min="0" :max="100" size="small" style="width:100px" placeholder="%" @change="handleFillPercentChange" />

              <KnowledgeCardPopover
                v-if="selectedChapter.chapterTitle"
                :keyword="selectedChapter.chapterTitle"
                :label="'GJB参考'"
              />

              <el-button v-if="ledger?.projectId" size="small" type="success" :loading="autoFilling" @click="handleAutoFill">
                自动填充
              </el-button>
              <el-button v-if="ledger?.projectId" size="small" type="warning" :loading="aiGenerating" @click="handleAiGenerate">
                AI生成本章
              </el-button>
            </div>

            <el-input
              v-model="selectedChapter.content"
              type="textarea"
              :rows="20"
              placeholder="在此输入章节内容..."
            />

            <div v-if="contentSchema" class="schema-fields">
              <el-divider content-position="left">结构化字段</el-divider>
              <div v-for="(field, key) in contentSchema" :key="key" style="margin-bottom:8px">
                <el-input v-model="contentJsonObj[key]" :placeholder="'输入: ' + (field as any).label || key" size="small" />
              </div>
              <el-button size="small" type="primary" style="margin-top:4px" @click="handleSaveContent">保存内容</el-button>
            </div>

            <div style="margin-top:16px;display:flex;gap:8px">
              <el-button type="primary" size="small" :loading="saving" @click="handleSaveContent">保存内容</el-button>
              <el-button size="small" @click="selectedChapter = null">取消</el-button>
            </div>
          </template>
        </el-card>
      </el-col>

      <!-- Right: Knowledge & completeness -->
      <el-col :xs="24" :sm="24" :md="6" :lg="6" :xl="6">
        <el-card shadow="never">
          <template #header><span>知识卡片</span></template>
          <div v-if="knowledgeCards.length > 0">
            <div v-for="card in knowledgeCards" :key="card.id" class="knowledge-item" @click="selectedCard = card">
              <h4>{{ card.title }}</h4>
              <p v-if="card.plainLanguage" class="plain-text">{{ card.plainLanguage }}</p>
              <el-tag v-if="card.gjbReference" size="small" type="danger">{{ card.gjbReference }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无相关卡片" :image-size="40" />
        </el-card>

        <el-card shadow="never" style="margin-top:12px" v-if="selectedCard">
          <template #header><span>{{ selectedCard.title }}</span></template>
          <div class="card-detail">
            <div v-if="selectedCard.plainLanguage" class="plain-box">
              <strong>白话解释：</strong>{{ selectedCard.plainLanguage }}
            </div>
            <div v-if="selectedCard.gjbReference">
              <strong>GJB原文参考：</strong>{{ selectedCard.gjbReference }}
            </div>
            <div v-if="selectedCard.tags" style="margin-top:4px">
              <el-tag v-for="tag in cardTags" :key="tag" size="small" type="info" style="margin-right:4px">{{ tag }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Export Dialog -->
    <DocxExportDialog
      v-if="ledger"
      v-model="showExport"
      :doc-ledger-id="ledger.id!"
      :doc-name="ledger.docName"
      :chapter-count="completionSummary.total"
      :fill-rate="completionSummary.total ? Math.round((completionSummary.filled || 0) / completionSummary.total * 100) : 0"
      @done="loadChapters"
    />

    <!-- AI Structure Dialog -->
    <el-dialog v-model="showAiDialog" title="AI 智能构建章节结构" width="800px" :close-on-click-modal="false" @closed="aiPreview = null; aiRequest.additionalPrompt = ''">
      <el-form label-width="100px" size="small">
        <el-form-item label="文档类型">
          <el-input v-model="aiRequest.docType" placeholder="如：研制总结、可靠性大纲、产品规范等" />
        </el-form-item>
        <el-form-item label="参考模板">
          <el-select v-model="aiRequest.templateId" placeholder="可选，选择已有模板作为参考" clearable style="width:100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充要求">
          <el-input v-model="aiRequest.additionalPrompt" type="textarea" :rows="3"
            placeholder="可输入额外的结构要求，如：'需要增加电磁兼容性试验章节'、'重点突出安全性分析'等" />
        </el-form-item>
      </el-form>

      <!-- Preview result -->
      <div v-if="aiPreview" class="ai-preview">
        <el-alert :title="aiPreview.summary" type="success" :closable="false" show-icon style="margin-bottom:12px" />
        <div class="ai-tree">
          <el-tree :data="aiPreview.chapters" :props="{ children: 'children', label: 'chapterTitle' }"
            node-key="chapterNumber" default-expand-all>
            <template #default="{ data }">
              <span style="display:flex;align-items:center;gap:8px">
                <el-tag size="small" type="info">{{ data.chapterNumber }}</el-tag>
                <span>{{ data.chapterTitle }}</span>
                <el-tag v-if="data.isRequired" size="small" type="danger">必填</el-tag>
                <el-tag v-if="data.writingTips" size="small" type="success">含提示</el-tag>
                <el-tag v-if="data.contentSchema" size="small" type="warning">结构化</el-tag>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <template #footer>
        <el-button @click="showAiDialog = false">关闭</el-button>
        <el-button type="warning" :loading="aiLoading" @click="handleAiOptimizeStructure" :disabled="chapterTree.length === 0">
          优化现有结构
        </el-button>
        <el-button type="primary" :loading="aiLoading" @click="handleAiGenerateStructure">
          {{ chapterTree.length > 0 ? '重新生成' : '智能生成' }}
        </el-button>
        <el-button v-if="aiPreview" type="success" :loading="aiApplying" @click="handleAiApplyStructure">
          应用此结构
        </el-button>
      </template>
    </el-dialog>

    <div v-if="!ledger" style="text-align:center;padding:80px 0;color:#999">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Download, MagicStick, CircleCheck, Tools, ChatDotRound } from '@element-plus/icons-vue'
import { getDocLedger, type DocLedgerItem } from '@/api/doc-ledger'
import { getChapterTree, getCompletionSummary, updateChapterContent, initFromTemplate,
  getChapterWritingContext, generateChapter, autoFillChapter, type ChapterWritingContext,
  previewChapterStructure, applyChapterStructure, optimizeChapterStructure,
  type AiStructureRequest, type AiStructureResponse, type AiChapterNode,
  validateChapterStructure, fixChapterStructure, type ChapterStructureValidation } from '@/api/doc-chapter'
import { getTemplates, type DocTemplateV2 } from '@/api/template-v2'
import { getKnowledgeCards, type KnowledgeCard } from '@/api/knowledge-card'
import ChapterTreeViewer from '@/components/ChapterTreeViewer.vue'
import CompletenessProgressBar from '@/components/CompletenessProgressBar.vue'
import QualityPanel from '@/components/QualityPanel.vue'
import KnowledgeCardPopover from '@/components/KnowledgeCardPopover.vue'
import ChapterWritingGuide from '@/components/ChapterWritingGuide.vue'
import DocxExportDialog from '@/components/DocxExportDialog.vue'

const route = useRoute()
const docLedgerId = Number(route.params.docLedgerId || 0)

const ledger = ref<DocLedgerItem | null>(null)
const chapterTree = ref<any[]>([])
const selectedChapter = ref<any>(null)
const saving = ref(false)
const showExport = ref(false)

const templates = ref<DocTemplateV2[]>([])
const selectedTemplateId = ref<number | null>(null)
const knowledgeCards = ref<KnowledgeCard[]>([])
const selectedCard = ref<KnowledgeCard | null>(null)

const completionSummary = ref<{ total: number; filled: number; partial: number; empty: number; score: number }>({ total: 0, filled: 0, partial: 0, empty: 0, score: 0 })

// Three-library fusion
const writingContext = ref<ChapterWritingContext | null>(null)
const aiGenerating = ref(false)
const autoFilling = ref(false)

const contentSchema = computed(() => {
  if (!selectedChapter.value?.contentSchema) return null
  try {
    const schema = typeof selectedChapter.value.contentSchema === 'string'
      ? JSON.parse(selectedChapter.value.contentSchema)
      : selectedChapter.value.contentSchema
    return schema.fields || schema
  } catch { return null }
})

const contentJsonObj: Record<string, any> = reactive({})

// ---- AI Chapter Structure ----
const showAiDialog = ref(false)
const aiLoading = ref(false)
const aiApplying = ref(false)
const aiPreview = ref<AiStructureResponse | null>(null)
const aiRequest = reactive<AiStructureRequest>({
  projectId: 0, docLedgerId: 0, docType: '', templateId: undefined, additionalPrompt: '', optimize: false
})

async function handleAiGenerateStructure() {
  if (!ledger.value?.projectId) { ElMessage.warning('缺少项目ID'); return }
  aiLoading.value = true; aiPreview.value = null
  aiRequest.projectId = ledger.value.projectId
  aiRequest.docLedgerId = ledger.value.id
  aiRequest.optimize = false
  try {
    const res = await previewChapterStructure({ ...aiRequest })
    aiPreview.value = res.data.data as AiStructureResponse
  } catch {
    ElMessage.error('AI 生成失败，请检查 AI 服务连接')
  }
  aiLoading.value = false
}

async function handleAiOptimizeStructure() {
  if (!ledger.value?.id) return
  aiLoading.value = true; aiPreview.value = null
  aiRequest.docLedgerId = ledger.value.id
  try {
    const res = await optimizeChapterStructure({ docLedgerId: ledger.value.id, additionalPrompt: aiRequest.additionalPrompt })
    aiPreview.value = res.data.data as AiStructureResponse
  } catch {
    ElMessage.error('AI 优化失败')
  }
  aiLoading.value = false
}

async function handleAiApplyStructure() {
  if (!aiPreview.value || !ledger.value?.id) return
  aiApplying.value = true
  try {
    await applyChapterStructure(ledger.value.id, aiPreview.value.chapters)
    ElMessage.success('AI 章节结构已应用')
    showAiDialog.value = false
    loadChapters(); loadSummary()
  } catch {
    ElMessage.error('应用失败')
  }
  aiApplying.value = false
}

// ---- Chapter Structure Validation ----
const validating = ref(false)
const fixing = ref(false)
const validationResult = ref<ChapterStructureValidation | null>(null)

async function handleValidate() {
  if (!ledger.value?.id) return
  validating.value = true
  try {
    const res = await validateChapterStructure(ledger.value.id)
    validationResult.value = res.data.data as ChapterStructureValidation
    if (validationResult.value?.valid) {
      ElMessage.success('章节结构校验通过')
    } else {
      ElMessage.warning('发现结构问题，请查看详情')
    }
  } catch {
    ElMessage.error('校验失败')
  }
  validating.value = false
}

async function handleFix() {
  if (!ledger.value?.id) return
  fixing.value = true
  try {
    const res = await fixChapterStructure(ledger.value.id)
    validationResult.value = res.data.data as ChapterStructureValidation
    if (validationResult.value?.valid) {
      ElMessage.success('章节结构已修复')
    } else {
      ElMessage.warning('部分问题已修复，剩余问题请手动调整')
    }
    loadChapters(); loadSummary()
  } catch {
    ElMessage.error('修复失败')
  }
  fixing.value = false
}

const cardTags = computed(() => {
  if (!selectedCard.value?.tags) return []
  return selectedCard.value.tags.split(',').map(t => t.trim()).filter(Boolean)
})

function statusType(s: string) {
  const map: Record<string, string> = { FILLED: 'success', PARTIAL: 'warning', EMPTY: 'info' }
  return map[s] || 'info'
}

function statusLabel(s: string) {
  const map: Record<string, string> = { FILLED: '已完成', PARTIAL: '部分完成', EMPTY: '待填写' }
  return map[s] || s
}

async function selectChapter(ch: any) {
  selectedChapter.value = ch
  // Reset
  writingContext.value = null
  // Load full chapter content
  try {
    const { getChapter } = await import('@/api/doc-chapter')
    const res = await getChapter(ch.id)
    selectedChapter.value = res.data.data || ch
    // Parse contentJson
    if (selectedChapter.value.contentJson) {
      try {
        Object.assign(contentJsonObj, JSON.parse(selectedChapter.value.contentJson))
      } catch { /* ignore */ }
    }
  } catch { /* ignore */ }
  // Load writing context (three-library fusion)
  if (ledger.value?.projectId) {
    try {
      const ctxRes = await getChapterWritingContext(ch.id, ledger.value.projectId)
      writingContext.value = ctxRes.data.data
    } catch { /* ignore */ }
  }
}

async function loadLedger() {
  try {
    const res = await getDocLedger(docLedgerId)
    ledger.value = res.data.data
  } catch { /* ignore */ }
}

async function loadChapters() {
  try {
    const res = await getChapterTree(docLedgerId)
    chapterTree.value = res.data.data || []
  } catch { /* ignore */ }
}

async function loadSummary() {
  try {
    const res = await getCompletionSummary(docLedgerId)
    completionSummary.value = res.data.data || { total: 0, filled: 0, partial: 0, empty: 0, score: 0 }
  } catch { /* ignore */ }
}

async function loadTemplates() {
  try {
    const res = await getTemplates()
    templates.value = res.data.data || []
  } catch { /* ignore */ }
}

async function loadKnowledgeCards() {
  try {
    const res = await getKnowledgeCards(undefined, 'doc_chapter', docLedgerId)
    knowledgeCards.value = res.data.data || []
  } catch { /* ignore */ }
}

async function handleInitChapters() {
  if (!selectedTemplateId.value) return
  try {
    await initFromTemplate(docLedgerId, selectedTemplateId.value, 1)
    ElMessage.success('章节结构已从模板初始化')
    loadChapters()
    loadSummary()
  } catch {
    ElMessage.error('初始化失败')
  }
}

async function handleSaveContent() {
  if (!selectedChapter.value?.id) return
  saving.value = true
  try {
    const contentJson = Object.keys(contentJsonObj).length > 0 ? JSON.stringify(contentJsonObj) : undefined
    await updateChapterContent(selectedChapter.value.id, selectedChapter.value.content || '', contentJson, 1)
    ElMessage.success('内容已保存')
    loadSummary()
  } catch { ElMessage.error('保存失败') }
  saving.value = false
}

async function handleStatusChange() {
  if (!selectedChapter.value?.id) return
  try {
    const { updateFillStatus } = await import('@/api/doc-chapter')
    await updateFillStatus(selectedChapter.value.id, selectedChapter.value.fillStatus, selectedChapter.value.fillPercentage)
    loadSummary()
  } catch { /* ignore */ }
}

async function handleFillPercentChange() {
  handleStatusChange()
}

async function handleAiGenerate() {
  if (!selectedChapter.value?.id || !ledger.value?.projectId) return
  aiGenerating.value = true
  try {
    const res = await generateChapter(selectedChapter.value.id, ledger.value.projectId)
    const generated = res.data.data
    if (generated) {
      selectedChapter.value.content = generated
      ElMessage.success('AI内容生成完成，请检查后保存')
    }
  } catch { ElMessage.error('AI生成失败') }
  aiGenerating.value = false
}

async function handleAutoFill() {
  if (!selectedChapter.value?.id || !ledger.value?.projectId) return
  autoFilling.value = true
  try {
    const res = await autoFillChapter(selectedChapter.value.id, ledger.value.projectId)
    const updated = res.data.data
    if (updated?.content) {
      selectedChapter.value.content = updated.content
      ElMessage.success('占位符自动填充完成')
    } else {
      ElMessage.info('未发现可填充的占位符')
    }
    // Refresh writing context to show updated field statuses
    const ctxRes = await getChapterWritingContext(selectedChapter.value.id, ledger.value.projectId)
    writingContext.value = ctxRes.data.data
  } catch { ElMessage.error('自动填充失败') }
  autoFilling.value = false
}

// Watch for route param changes (switch between documents)
watch(() => route.params.docLedgerId, (newId) => {
  if (newId) {
    loadLedger()
    loadChapters()
    loadSummary()
    loadTemplates()
    loadKnowledgeCards()
  }
})

onMounted(() => { loadLedger(); loadChapters(); loadSummary(); loadTemplates(); loadKnowledgeCards() })

function handleGjbExport() {
  if (!ledger.value?.id) return
  const url = `/api/v1/reliability/documents/${ledger.value.id}/export`
  fetch(url).then(r => r.text()).then(text => {
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${ledger.value?.docName || '文档'}.txt`
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('导出成功')
  }).catch(() => ElMessage.error('导出失败'))
}


function handleDialogue() {
  if (!ledger.value?.id) return
  const route = useRoute()
  const router = useRouter()
  // Navigate to dialogue writer
  window.open(`/dialogue-writer?projectId=${route.params.projectId}&docType=${ledger.value.docType || 'any_draft'}&docName=${ledger.value.docName || '文档'}&ledgerId=${ledger.value.id}`, '_blank')
}
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-actions { display: flex; gap: 8px; }
.knowledge-item {
  padding: 8px; margin-bottom: 6px; border: 1px solid #ebeef5; border-radius: 4px;
  cursor: pointer; transition: background .2s;
}
.knowledge-item:hover { background: #f5f7fa; }
.knowledge-item h4 { margin: 0 0 4px; font-size: 13px; }
.plain-text { font-size: 12px; color: #67c23a; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-detail { font-size: 13px; line-height: 1.8; }
.plain-box { padding: 8px; background: #f0f9eb; border-radius: 4px; margin-bottom: 8px; }
.schema-fields { margin-top: 12px; padding: 12px; background: var(--el-fill-color-lighter); border-radius: 4px; }
.ai-preview { margin-top: 16px; border: 1px solid var(--el-border-color); border-radius: 6px; padding: 12px; background: #fafbfc; }
.ai-tree { max-height: 400px; overflow-y: auto; }
.ai-tree :deep(.el-tree-node__content) { height: auto; padding: 3px 0; }
</style>
