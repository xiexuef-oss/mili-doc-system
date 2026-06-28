<template>
  <div class="canvas-page">
    <!-- Step 1: Select document from ledger -->
    <div v-if="workflowStep === 'select'" class="workflow-select">
      <div class="ws-toolbar">
        <div class="ws-toolbar-left">
          <el-select v-model="filterStatus" placeholder="筛选状态" clearable size="default" style="width:140px" @change="handleFilterChange">
            <el-option label="策划" value="PLANNED"/>
            <el-option label="起草中" value="DRAFTING"/>
            <el-option label="校对" value="CHECKING"/>
            <el-option label="评审中" value="REVIEWING"/>
            <el-option label="已批准" value="APPROVED"/>
            <el-option label="已发布" value="RELEASED"/>
            <el-option label="已归档" value="ARCHIVED"/>
          </el-select>
          <el-input v-model="searchText" placeholder="搜索文档编号/名称" clearable size="default" style="width:260px" @input="handleSearchChange"/>
          <span class="ws-count">共 {{ filteredDocs.length }} 份文档</span>
        </div>
        <div class="ws-toolbar-right">
          <el-button :icon="Refresh" size="default" @click="loadDocChecklist">刷新</el-button>
        </div>
      </div>
      <div v-if="docChecklist.length > 0" class="ws-table-wrapper">
        <el-table :data="pagedDocs" border stripe style="width:100%">
          <el-table-column prop="docCode" label="文档编号" width="120"/>
          <el-table-column prop="docName" label="文档名称" min-width="240" show-overflow-tooltip/>
          <el-table-column prop="docType" label="文档类型" width="150">
            <template #default="{ row }">
              <span>{{ docTypeLabel(row.docType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="生命周期状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.lifecycleStatus)" size="small" effect="light">
                {{ getStatusText(row.lifecycleStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="模板状态" width="200">
            <template #default="{ row }">
              <el-tag :type="row.hasTemplate ? 'success' : 'warning'" size="small" effect="plain">
                {{ row.hasTemplate ? row.templateName || '已匹配' : '待生成' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="selectDocument(row)">
                <el-icon><EditPen /></el-icon>
                <span>AI写作</span>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-if="docChecklist.length > 0" class="ws-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredDocs.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
      <div v-else-if="docChecklist.length === 0" class="ws-empty">
        <el-empty description="暂无文档台账，请先在【文档台账】中创建文档">
          <template #image>
            <el-icon :size="80" color="#dcdfe6"><Document /></el-icon>
          </template>
        </el-empty>
      </div>
      <div v-if="filteredDocs.length === 0 && docChecklist.length > 0" class="ws-empty-filter">
        <el-empty description="没有匹配的文档，请调整筛选条件"/>
      </div>
    </div>

    <!-- Step 2: Template confirmation -->
    <div v-else-if="workflowStep === 'template'" class="workflow-template">
      <div class="wt-header">
        <el-button @click="workflowStep = 'select'">← 返回选择</el-button>
        <h2>📝 {{ selectedDoc?.docName }} - 模板确认</h2>
        <span v-if="templateGenerated" class="wt-tag">AI生成模板</span>
      </div>
      <div class="wt-info">
        <p v-if="selectedDoc?.hasTemplate">系统已找到匹配模板，点击确认后AI将按模板结构写作</p>
        <p v-else>系统未找到匹配模板，AI将基于知识库生成标准模板结构</p>
      </div>
      <div class="wt-outline">
        <h3>章节结构</h3>
        <div v-for="(item, idx) in templateOutline" :key="idx" class="wt-outline-item">
          <el-input size="small" v-model="item.title" :style="{ paddingLeft: (item.level - 1) * 20 + 'px' }"/>
          <span v-if="item.description" class="wt-desc">{{ item.description }}</span>
        </div>
      </div>
      <div class="wt-actions">
        <el-button @click="regenerateTemplate">重新生成模板</el-button>
        <el-button type="primary" @click="confirmTemplateAndWrite">确认模板并开始写作</el-button>
      </div>
    </div>

    <!-- Step 3: Writing (original canvas) -->
    <template v-else>
      <div class="cp-left">
        <div class="cp-header">
          <span class="cp-title">AI 文档助手</span>
          <el-button size="small" type="primary" @click="workflowStep = 'select'">选择文档</el-button>
        </div>
        <div class="cp-messages" ref="msgRef">
          <div v-if="messages.length===0" class="cp-welcome">
            <p>👋 我是AI文档助手</p>
            <p>输入需求开始：</p>
            <ul><li>"写一份XX方案"</li><li>"生成XX大纲"</li></ul>
          </div>
          <div v-for="(m,i) in messages" :key="i" :class="['cp-msg',m.role]">
            <div class="cp-msg-content">{{ m.content }}</div>
          </div>
          <div v-if="store.generating" class="cp-msg ai"><div class="cp-msg-content">生成中...</div></div>
        </div>
        <div class="cp-input">
          <el-input v-model="input" placeholder="输入需求或编辑指令..." @keyup.enter="send" :disabled="store.generating"/>
          <el-button type="primary" @click="send" :disabled="store.generating||!input.trim()">发送</el-button>
        </div>
      </div>

      <div class="cp-right">
        <div v-if="!store.document && !store.loading" class="cp-empty">
          <p>📄 在左侧输入需求开始创建文档</p>
          <el-button type="primary" @click="workflowStep = 'select'">选择文档写作</el-button>
        </div>
        <template v-else-if="store.document">
          <div class="cp-toolbar">
            <div class="cp-toolbar-title-wrap">
              <span v-if="!editingTitle" class="cp-toolbar-title" @click="startEditTitle">📄 {{ store.document.title }}</span>
              <el-input v-else v-model="editTitleValue" size="small" style="width:260px" @blur="saveTitle" @keyup.enter="saveTitle" ref="titleInputRef"/>
            </div>
            <div class="cp-toolbar-center">
              <span class="cp-toolbar-stats">{{ store.saveStatus==='saving'?'保存中...':store.saveStatus==='unsaved'?'未保存':'已保存' }}</span>
              <el-button size="small" type="success" :disabled="store.generating" @click="handleGenerateAll">AI写全部</el-button>
              <el-dropdown trigger="click" @command="handleExport">
                <el-button size="small">导出</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="markdown">导出 Markdown</el-dropdown-item>
                    <el-dropdown-item command="html">导出 HTML</el-dropdown-item>
                    <el-dropdown-item command="text">导出纯文本</el-dropdown-item>
                    <el-dropdown-item command="json">导出 JSON</el-dropdown-item>
                    <el-dropdown-item command="docx" :disabled="true">导出 Word (待实现)</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="cp-toolbar-actions">
              <el-button size="small" :disabled="workflowStep !== 'writing'" @click="saveToLedger">保存到台账</el-button>
              <el-button size="small" @click="showVersionHistory=true">版本历史</el-button>
              <el-button size="small" @click="loadDoc">刷新</el-button>
            </div>
          </div>
          <div class="cp-doc-body">
            <div v-if="!store.contentJson || (store.contentJson.content||[]).length===0" style="text-align:center;padding:40px;color:var(--el-text-color-secondary)">
              文档已创建，正在生成内容...
            </div>
            <div v-else class="cp-doc-layout">
              <OutlineNavigator :items="store.outline" :active-id="store.activeHeadingId" @navigate="scrollToHeading" />
              <div class="cp-content" ref="contentRef">
                <CanvasEditor ref="canvasEditorRef" :content-json="store.contentJson"
                  @update:content-json="store.handleContentUpdate"
                  @selection-change="onSelectionChange"
                  @active-heading-change="(id:string|null)=>store.activeHeadingId=id" />
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Floating AI Menu (cursor position) -->
      <FloatingAiMenu ref="floatingMenuRef" :editor="canvasEditorRef?.editor"
        @action="onFloatingAction" />

      <!-- Quick AI bar -->
      <div v-if="selectionCtx && !showAiPreview" class="ai-quick-bar">
        <el-button size="small" @click="aiQuickAction('polish')">✨ 润色</el-button>
        <el-button size="small" @click="aiQuickAction('expand')">📝 扩写</el-button>
        <el-button size="small" @click="aiQuickAction('shorten')">📏 缩短</el-button>
        <el-button size="small" @click="aiQuickAction('rewrite')">🔄 重写</el-button>
        <el-button size="small" @click="aiQuickAction('summarize')">📊 总结</el-button>
        <el-button size="small" @click="aiQuickAction('translate')">🌐 翻译</el-button>
        <el-button size="small" @click="aiQuickAction('formal')">🎩 改正式</el-button>
        <el-button size="small" @click="aiQuickAction('casual')">💬 改口语</el-button>
        <el-input v-model="customInstruction" size="small" placeholder="自定义..." style="width:140px" @keyup.enter="aiQuickAction('custom')"/>
      </div>

      <!-- AI Edit Preview Panel -->
      <div v-if="showAiPreview" class="ai-preview-panel">
        <div class="ai-preview-header">AI 修改建议</div>
        <div class="ai-preview-content">
          <div class="ai-preview-original"><strong>原文：</strong>{{ aiPreviewOriginal }}</div>
          <div class="ai-preview-result"><strong>修改后：</strong>{{ aiPreviewResult }}</div>
        </div>
        <div class="ai-preview-actions">
          <el-button size="small" type="primary" @click="applyAiPreview('replace')">替换原文</el-button>
          <el-button size="small" @click="applyAiPreview('insert')">插入到下方</el-button>
          <el-button size="small" @click="aiQuickAction(aiPreviewMode, true)">重新生成</el-button>
          <el-button size="small" @click="cancelAiPreview">放弃修改</el-button>
        </div>
      </div>
    </template>

    <el-dialog v-model="showCreate" title="新建AI文档" width="500px">
      <el-form label-width="80px">
        <el-form-item label="需求描述" required>
          <el-input v-model="createPrompt" type="textarea" :rows="4" placeholder="例如：帮我写一份AI客服系统建设方案"/>
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="createDocType" placeholder="可选" clearable style="width:100%">
            <el-option label="方案" value="方案"/><el-option label="报告" value="报告"/>
            <el-option label="规范" value="规范"/><el-option label="其他" value="other"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate=false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建并生成</el-button>
      </template>
    </el-dialog>

    <VersionHistory v-if="store.document" v-model="showVersionHistory"
      :document-id="store.document.id" @restored="loadDoc" />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, EditPen, Document } from '@element-plus/icons-vue'
import { useDocumentCanvasStore } from '@/stores/documentCanvas'
import * as aiDocApi from '@/api/ai-document'
import { getKanbanData } from '@/api/doc-ledger'
import { getTemplates } from '@/api/template-v2'
import type { EditorSelectionContext } from '@/utils/editorPatch'
import CanvasEditor from '@/components/CanvasEditor.vue'
import OutlineNavigator from '@/components/OutlineNavigator.vue'
import VersionHistory from '@/components/VersionHistory.vue'
import FloatingAiMenu from '@/components/FloatingAiMenu.vue'
import { exportDocument } from '@/api/ai-document'
import { getToken } from '@/utils/auth'

const route = useRoute()
const store = useDocumentCanvasStore()
const projectId = Number(route.params.projectId || 0)

const messages = ref<{role:string,content:string}[]>([])
const input = ref('')
const msgRef = ref<HTMLElement>()
const contentRef = ref<HTMLElement>()
const showCreate = ref(false)
const creating = ref(false)
const createPrompt = ref('')
const createDocType = ref('')
const showVersionHistory = ref(false)
const selectionCtx = ref<EditorSelectionContext | null>(null)
const customInstruction = ref('')
let abortCtrl: AbortController | null = null

// AI Edit Preview state
const showAiPreview = ref(false)
const aiPreviewResult = ref('')
const aiPreviewOriginal = ref('')
const aiPreviewMode = ref('')
const aiPreviewPatch = ref<any>(null)
const aiPreviewCtx = ref<EditorSelectionContext | null>(null)

// Title editing
const editingTitle = ref(false)
const editTitleValue = ref('')
const titleInputRef = ref<any>(null)

// CanvasEditor ref for applying editor-level patches directly
const canvasEditorRef = ref<InstanceType<typeof CanvasEditor> | null>(null)
const floatingMenuRef = ref<InstanceType<typeof FloatingAiMenu> | null>(null)

// IntersectionObserver for tracking active heading during scroll
let headingObserver: IntersectionObserver | null = null

function loadDoc() { if (store.document?.id) store.loadDocument(store.document.id) }

async function handleCreate() {
  if (!createPrompt.value.trim()) { ElMessage.warning('请输入需求描述'); return }
  creating.value = true
  try {
    await store.createDocument(projectId, createPrompt.value, createDocType.value)
    showCreate.value = false
    messages.value.push({role:'user',content:createPrompt.value})
    messages.value.push({role:'assistant',content:'文档已创建，正在生成...'})
    handleGenerateAll()
  } catch { ElMessage.error('创建失败') }
  creating.value = false
}

function handleGenerateAll() {
  if (!store.document?.id) return
  startAsyncGeneration()
}

let pollTimer: ReturnType<typeof setInterval> | null = null

function startAsyncGeneration() {
  if (!store.document?.id) return
  const docId = store.document.id
  store.generating = true
  messages.value.push({ role: 'assistant', content: '开始按模板结构写作...' })

  if (pollTimer) clearInterval(pollTimer)

  aiDocApi.generateContentAsync(docId).then(() => {
    messages.value.push({ role: 'assistant', content: '后台生成已启动，请稍候...' })
    pollTimer = setInterval(async () => {
      try {
        const res = await aiDocApi.getGenerationStatus(docId)
        const task = res.data?.data || {}
        if (task.message && task.message !== messages.value[messages.value.length - 1]?.content) {
          messages.value.push({ role: 'assistant', content: task.message })
        }
        // Reload document on every poll so editor updates progressively as sections are written
        try {
          const docRes = await aiDocApi.getDocument(docId)
          const cj = docRes.data?.data?.contentJson
          store.contentJson = (cj && typeof cj === 'string') ? JSON.parse(cj) : (cj || { type: 'doc', content: [] })
          const doc = docRes.data?.data?.document
          if (doc) store.document = doc
          const secs = docRes.data?.data?.sections || []
          store.outline = secs.map((s: any) => ({ id: 'sec-' + s.id, text: s.title, level: s.level || 1, pos: s.sortOrder }))
        } catch {}
        if (task.status === 'done') {
          clearInterval(pollTimer!); pollTimer = null
          store.generating = false
          messages.value.push({ role: 'assistant', content: '文档生成完成！' })
        } else if (task.status === 'failed') {
          clearInterval(pollTimer!); pollTimer = null
          store.generating = false
          messages.value.push({ role: 'assistant', content: '生成失败：' + (task.message || '未知错误') })
        }
      } catch { /* ignore transient poll errors */ }
    }, 2000)
  }).catch(e => {
    store.generating = false
    messages.value.push({ role: 'assistant', content: '启动生成失败：' + (e?.message || '网络错误') })
  })
}

function startGenerateContent() {
  startAsyncGeneration()
}

async function send() {
  const msg = input.value.trim(); if (!msg || store.generating) return
  input.value = ''; messages.value.push({role:'user',content:msg})
  if (!store.document) { await store.createDocument(projectId, msg, ''); messages.value.push({role:'assistant',content:'文档已创建...'}); handleGenerateAll(); return }
  try {
    const res = await aiDocApi.chatWithDocument(store.document.id, msg)
    const reply = res.data?.data
    messages.value.push({role:'assistant',content:reply?.reply||'已处理'})
    if (reply?.patches) {
      const result = store.applyCanvasPatches(reply.patches)
      result.editorPatches.forEach(ep => {
        nextTick(() => canvasEditorRef.value?.applyPatch(ep))
      })
    }
  } catch { messages.value.push({role:'assistant',content:'处理失败'}) }
  nextTick(()=>{ if(msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
}

function onSelectionChange(ctx: EditorSelectionContext | null) { selectionCtx.value = ctx }

async function aiQuickAction(mode: string, regenerate = false) {
  if (!selectionCtx.value || !store.document?.id) return
  const ctx = selectionCtx.value
  aiPreviewCtx.value = ctx
  aiPreviewMode.value = mode
  aiPreviewOriginal.value = ctx.selectedText
  try {
    const instruction = mode === 'custom' ? (customInstruction.value || mode) : mode
    const res = await aiDocApi.aiEditSelection(store.document.id, ctx, instruction)
    const patch = res.data?.data
    aiPreviewPatch.value = patch
    if (patch) {
      const content = patch.newContent || patch.content || ''
      aiPreviewResult.value = typeof content === 'string' ? content : JSON.stringify(content)
    } else { aiPreviewResult.value = '' }
    showAiPreview.value = true
    customInstruction.value = ''
  } catch { ElMessage.error('AI操作失败') }
}

function applyAiPreview(action: 'replace' | 'insert') {
  const patch = aiPreviewPatch.value; const ctx = aiPreviewCtx.value; const ed = canvasEditorRef.value
  if (!patch || !ctx || !ed) { showAiPreview.value = false; return }
  if (action === 'replace') {
    nextTick(() => {
      const applied = ed.applyPatch({ type: 'replace_selection', blockId: ctx.blockId, newContent: aiPreviewResult.value })
      if (!applied) ed.editor?.chain().focus().insertContent(aiPreviewResult.value).run()
    })
  } else {
    nextTick(() => {
      const applied = ed.applyPatch({ type: 'insert_after', blockId: ctx.blockId, content: { type: 'paragraph', content: [{ type: 'text', text: aiPreviewResult.value }] } })
      if (!applied) ed.editor?.chain().focus().insertContent(aiPreviewResult.value).run()
    })
  }
  showAiPreview.value = false; aiPreviewPatch.value = null; aiPreviewCtx.value = null
}

function cancelAiPreview() { showAiPreview.value = false; aiPreviewPatch.value = null; aiPreviewCtx.value = null }

function scrollToHeading(id: string) {
  store.activeHeadingId = id
  canvasEditorRef.value?.scrollToHeading(id)
}

function setupHeadingObserver() {
  if (!contentRef.value) return
  headingObserver?.disconnect()
  headingObserver = new IntersectionObserver((entries) => {
    let bestEntry: IntersectionObserverEntry | null = null
    for (const entry of entries) {
      if (entry.isIntersecting && (!bestEntry || entry.intersectionRatio > bestEntry.intersectionRatio))
        bestEntry = entry
    }
    if (bestEntry) {
      const headingId = bestEntry.target.getAttribute('data-heading-id')
      if (headingId) store.activeHeadingId = headingId
    }
  }, { root: contentRef.value, threshold: [0, 0.25, 0.5, 0.75, 1] })
}

function observeHeadings() {
  if (!headingObserver) return
  headingObserver.disconnect()
  document.querySelectorAll('[data-heading-id]').forEach(el => headingObserver!.observe(el))
}

// Workflow state
const workflowStep = ref<'select' | 'template' | 'writing'>('select')
const selectedDoc = ref<any>(null)
const selectedCatalogId = ref<number | null>(null)
const selectedLedgerId = ref<number | null>(null)
const docChecklist = ref<any[]>([])
const templateOutline = ref<any[]>([])
const templateGenerated = ref(false)

// Filter state
const filterStatus = ref('')
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

const filteredDocs = computed(() => {
  let list = [...docChecklist.value]
  if (filterStatus.value) {
    list = list.filter(item => item.lifecycleStatus === filterStatus.value)
  }
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(item =>
      item.docCode?.toLowerCase().includes(kw) ||
      item.docName?.toLowerCase().includes(kw)
    )
  }
  return list
})

const pagedDocs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredDocs.value.slice(start, end)
})

function handleFilterChange() {
  currentPage.value = 1
}

function handleSearchChange() {
  currentPage.value = 1
}

function handleSizeChange() {
  currentPage.value = 1
}

function handleCurrentChange() {
  // Page change handler
}

function docTypeLabel(type: string): string {
  const map: Record<string, string> = {
    TECHNICAL_SPEC: '技术规范',
    REQUIREMENTS_SPEC: '需求规格说明',
    DESIGN_DESCRIPTION: '设计说明',
    TEST_PLAN: '测试计划',
    TEST_REPORT: '测试报告',
    USER_MANUAL: '用户手册',
    MAINTENANCE_MANUAL: '维护手册',
    SOFTWARE_REQUIREMENTS: '软件需求说明',
    SOFTWARE_DESIGN: '软件设计说明',
    SOFTWARE_TEST_PLAN: '软件测试计划',
    SOFTWARE_TEST_REPORT: '软件测试报告',
    RELIABILITY_REPORT: '可靠性报告',
    SAFETY_REPORT: '安全性报告'
  }
  return map[type] || type || '—'
}

onMounted(() => {
  setupHeadingObserver()
  loadDocChecklist()
})

function getStatusTagType(status: string): string {
  const types: Record<string, string> = {
    PLANNED: 'info',
    DRAFTING: 'warning',
    REVIEWING: '',
    APPROVED: 'success',
    RELEASED: 'success',
    ARCHIVED: 'info'
  }
  return types[status] || 'info'
}

function getStatusText(status: string): string {
  const texts: Record<string, string> = {
    PLANNED: '策划',
    DRAFTING: '起草中',
    REVIEWING: '评审中',
    APPROVED: '已批准',
    RELEASED: '已发布',
    ARCHIVED: '已归档'
  }
  return texts[status] || status
}

function normalizeText(text: string): string {
  return text.toLowerCase().replace(/[\s()（）【】《》<>]/g, '')
}

async function loadDocChecklist() {
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  try {
    const [kanbanRes, templatesRes] = await Promise.all([
      getKanbanData(projectId),
      getTemplates()
    ])
    
    const kanban = kanbanRes.data?.data || {}
    const templates = templatesRes.data?.data || []
    
    const allItems: any[] = []
    Object.values(kanban).forEach((list: any) => {
      if (Array.isArray(list)) {
        allItems.push(...list)
      }
    })
    
    const seen = new Set<number>()
    const uniqueItems: any[] = []
    for (const item of allItems) {
      if (item.id && !seen.has(item.id)) {
        seen.add(item.id)
        uniqueItems.push(item)
      }
    }
    
    docChecklist.value = uniqueItems.map((item: any) => {
      const itemDocType = item.docType || item.type || item.category || ''
      const itemName = normalizeText(item.docName || item.name || '')
      
      let hasTemplate = false
      let templateId = null
      let templateName = ''
      
      for (const t of templates) {
        const tName = normalizeText(t.templateName || '')
        const tCode = normalizeText(t.templateCode || '')
        const tType = normalizeText(t.templateType || '')
        
        if (tName.length >= 4 && (itemName.includes(tName) || tName.includes(itemName))) {
          hasTemplate = true
          templateId = t.id
          templateName = t.templateName
          break
        }
        if (tCode.length >= 2 && itemDocType && (normalizeText(itemDocType).includes(tCode) || tCode.includes(normalizeText(itemDocType)))) {
          hasTemplate = true
          templateId = t.id
          templateName = t.templateName
          break
        }
        if (tType.length >= 2 && itemDocType && (normalizeText(itemDocType).includes(tType) || tType.includes(normalizeText(itemDocType)))) {
          hasTemplate = true
          templateId = t.id
          templateName = t.templateName
          break
        }
      }
      
      return {
        ledgerId: item.id,
        catalogId: item.catalogId || item.parentId,
        docCode: item.docCode || item.code,
        docName: item.docName || item.name,
        docType: itemDocType,
        stageId: item.stageId,
        lifecycleStatus: item.lifecycleStatus || item.status,
        hasTemplate,
        templateId,
        templateName
      }
    })
  } catch (e) {
    console.error('加载文档台账失败:', e)
    ElMessage.error('加载文档台账失败')
  }
}

async function selectDocument(doc: any) {
  selectedDoc.value = doc
  selectedCatalogId.value = doc.catalogId || doc.parentId
  selectedLedgerId.value = doc.ledgerId
  workflowStep.value = 'template'

  try {
    console.log('[selectDocument] initializing doc:', { projectId, docName: doc.docName, docType: doc.docType, templateId: doc.templateId, ledgerId: doc.ledgerId })
    const res = await aiDocApi.initializeDocument({
      projectId,
      docName: doc.docName,
      docType: doc.docType || '',
      templateId: doc.hasTemplate ? doc.templateId : undefined,
      ledgerId: doc.ledgerId
    })
    const payload = res.data?.data
    console.log('[selectDocument] initialize response:', JSON.stringify(payload).substring(0, 200))
    if (!payload || !payload.document || !payload.document.id) {
      console.error('[selectDocument] Invalid response structure:', res.data)
      ElMessage.error('创建文档失败：服务器返回数据异常')
      workflowStep.value = 'select'
      return
    }
    store.document = payload.document
    templateOutline.value = payload.outline || []
    templateGenerated.value = payload.templateGenerated !== false
    console.log('[selectDocument] doc created:', payload.document.id, 'outline count:', templateOutline.value.length)
  } catch (e: any) {
    console.error('[selectDocument] initialize error:', e)
    const msg = e?.response?.data?.message || e?.message || String(e)
    ElMessage.error('创建文档失败：' + msg)
    workflowStep.value = 'select'
  }
}

async function regenerateTemplate() {
  if (!store.document?.id) return
  try {
    const res = await aiDocApi.generateTemplateFromKnowledge(store.document.id)
    templateOutline.value = res.data?.data?.outline || []
    templateGenerated.value = true
  } catch {
    ElMessage.error('重新生成模板失败')
  }
}

async function confirmTemplateAndWrite() {
  if (!store.document?.id) return
  try {
    await aiDocApi.confirmTemplate(store.document.id, templateOutline.value)
    workflowStep.value = 'writing'
    // Show outline in sidebar immediately (from template data)
    store.outline = templateOutline.value.map((item: any, i: number) => ({
      id: 'tpl-' + i, text: item.title || '', level: item.level || 1, pos: i
    }))
    // Set a placeholder contentJson with headings from outline
    const headingBlocks = templateOutline.value.map((item: any, idx: number) => ({
      type: 'heading',
      attrs: { level: item.level || 1, id: 'tpl-' + idx },
      content: [{ type: 'text', text: item.title || '' }]
    }))
    store.contentJson = { type: 'doc', content: headingBlocks }
    // Auto-start async generation — backend will write real content
    nextTick(() => { startAsyncGeneration() })
  } catch {
    ElMessage.error('确认模板失败')
  }
}

async function saveToLedger() {
  if (!store.document?.id || !selectedLedgerId.value) {
    ElMessage.warning('未关联文档台账')
    return
  }
  try {
    await aiDocApi.saveToDocLedger(store.document.id, selectedLedgerId.value)
    ElMessage.success('已保存到文档台账')
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '保存失败'
    ElMessage.error(msg)
  }
}

onUnmounted(() => {
  headingObserver?.disconnect()
  headingObserver = null
})

// Re-observe headings when outline changes
watch(() => store.outline, () => {
  nextTick(() => observeHeadings())
}, { deep: true })

function startEditTitle() {
  if (!store.document) return
  editTitleValue.value = store.document.title
  editingTitle.value = true
  nextTick(() => titleInputRef.value?.focus())
}

async function saveTitle() {
  if (!store.document || !editTitleValue.value.trim()) {
    editingTitle.value = false
    return
  }
  const newTitle = editTitleValue.value.trim()
  if (newTitle !== store.document.title) {
    store.document.title = newTitle
    try {
      await aiDocApi.updateDocumentTitle(store.document.id, newTitle)
      ElMessage.success('标题已更新')
    } catch {
      ElMessage.error('标题更新失败')
    }
  }
  editingTitle.value = false
}

async function onFloatingAction(type: string, context?: string) {
  const ed = canvasEditorRef.value?.editor
  if (!ed || !store.document?.id) return

  try {
    switch (type) {
      case 'continue': {
        const docText = ed.getText()
        const beforeText = docText.slice(-500)
        const res = await aiDocApi.aiInsertAtCursor(store.document.id, { context: beforeText, instruction: '继续写后续内容' })
        const patch = res.data?.data
        if (patch) ed.chain().focus().insertContent(patch.content || patch.newContent || '').run()
        break
      }
      case 'insert_paragraph':
        ed.chain().focus().insertContent({ type: 'paragraph', content: [{ type: 'text', text: '' }] }).run()
        break
      case 'insert_heading': {
        const level = 2
        const id = `h-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
        ed.chain().focus().insertContent({
          type: 'heading', attrs: { level, id }, content: [{ type: 'text', text: '新标题' }]
        }).run()
        break
      }
      case 'insert_list':
        ed.chain().focus().toggleBulletList().run()
        break
      case 'insert_table':
        ed.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
        break
      case 'insert_summary':
      case 'insert_action_items': {
        const docText = ed.getText()
        const instruction = type === 'insert_summary' ? '基于当前文档内容生成一段小结' : '基于当前文档内容生成行动项列表'
        const res = await aiDocApi.aiInsertAtCursor(store.document.id, { context: docText.slice(0, 2000), instruction })
        const patch = res.data?.data
        if (patch) {
          const content = patch.content || patch.newContent || ''
          if (typeof content === 'string') {
            ed.chain().focus().insertContent(content).run()
          } else {
            ed.chain().focus().insertContent(content).run()
          }
        }
        break
      }
      case 'custom': {
        if (!context) return
        const docText = ed.getText()
        const res = await aiDocApi.aiInsertAtCursor(store.document.id, { context: docText.slice(0, 2000), instruction: context })
        const patch = res.data?.data
        if (patch) ed.chain().focus().insertContent(patch.content || patch.newContent || '').run()
        break
      }
    }
  } catch {
    ElMessage.error('AI操作失败')
  }
}

// Export
async function handleExport(command: string | object) {
  const format = typeof command === 'string' ? command : 'markdown'
  if (format === 'docx') {
    if (store.document?.id) window.open(`/api/v1/docx/generate/${store.document.id}?includeCover=true`)
    return
  }
  if (!store.document?.id) return
  try {
    const res = await exportDocument(store.document.id, format)
    const data = res.data?.data
    if (data) {
      const blob = new Blob([data.content], { type: getMimeType(format) })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = data.filename || `document.${format}`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    }
  } catch {
    ElMessage.error('导出失败')
  }
}

function getMimeType(format: string): string {
  switch (format) {
    case 'html': return 'text/html;charset=utf-8'
    case 'markdown': return 'text/markdown;charset=utf-8'
    case 'text':
    case 'txt': return 'text/plain;charset=utf-8'
    case 'json': return 'application/json;charset=utf-8'
    default: return 'text/plain;charset=utf-8'
  }
}
</script>

<style scoped>
.canvas-page {
  display: flex;
  height: 100%;
  overflow: hidden;
  background: var(--el-bg-color-page);
}

/* ========== Workflow Select - 文档选择页面 ========== */
.workflow-select {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 12px;
  height: 100%;
  box-sizing: border-box;
}

.workflow-select > * {
  flex-shrink: 0;
}

.ws-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
}

.ws-toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ws-toolbar-right {
  display: flex;
  gap: 8px;
}

.ws-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-left: 8px;
}

.ws-table-wrapper {
  flex: 1 !important;
  flex-shrink: 1 !important;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
  overflow: auto;
  min-height: 200px;
}

.ws-table-wrapper :deep(.el-table) {
  border-radius: 0;
  border: none;
}

.ws-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.ws-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
}

.ws-empty-filter {
  padding: 60px 0;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
  margin-top: 12px;
}

/* ========== Workflow Template - 模板确认页面 ========== */
.workflow-template {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 12px;
  height: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.wt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.wt-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.wt-header h2 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--el-text-color-primary);
}

.wt-tag {
  font-size: 12px;
  padding: 2px 8px;
  background: #fff7e6;
  color: #d48806;
  border-radius: 4px;
}

.wt-info {
  padding: 12px 16px;
  background: #ecf5ff;
  border-radius: 6px;
  border: 1px solid #b3d8ff;
  flex-shrink: 0;
}

.wt-info p {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.wt-outline {
  flex: 1;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
  padding: 16px;
  overflow-y: auto;
  min-height: 0;
}

.wt-outline h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: var(--el-text-color-primary);
}

.wt-outline-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
  padding: 10px 14px;
  background: #fafafa;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  transition: all 0.2s;
}

.wt-outline-item:hover {
  background: #f5f7fa;
  border-color: var(--el-border-color-light);
}

.wt-outline-item .el-input {
  flex: 1;
}

.wt-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.wt-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

/* ========== Canvas Editor - 画布编辑页面 ========== */
.cp-left {
  width: 320px;
  min-width: 260px;
  max-width: 400px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid var(--el-border-color-light);
}

.cp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.cp-title {
  font-weight: 600;
  font-size: 14px;
}

.cp-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
}

.cp-welcome {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.8;
}

.cp-welcome ul {
  padding-left: 18px;
  margin: 4px 0;
}

.cp-msg {
  margin-bottom: 12px;
}

.cp-msg.user {
  text-align: right;
}

.cp-msg.user .cp-msg-content {
  display: inline-block;
  background: var(--el-color-primary-light-9);
  padding: 8px 14px;
  border-radius: 12px 4px 12px 12px;
  font-size: 13px;
}

.cp-msg.ai .cp-msg-content,
.cp-msg.assistant .cp-msg-content {
  background: var(--el-fill-color);
  padding: 10px 14px;
  border-radius: 4px 12px 12px 12px;
  font-size: 13px;
  line-height: 1.7;
}

.cp-input {
  display: flex;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.cp-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  min-width: 0;
  overflow: hidden;
}

.cp-empty {
  text-align: center;
  padding: 80px 20px;
  color: var(--el-text-color-secondary);
  flex: 1;
}

.cp-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
  gap: 8px;
}

.cp-toolbar-title {
  font-weight: 600;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cp-toolbar-center {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cp-toolbar-stats {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.cp-toolbar-actions {
  display: flex;
  gap: 6px;
}

.cp-doc-body {
  flex: 1;
  overflow: hidden;
  display: flex;
}

.cp-doc-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.cp-content {
  flex: 1;
  overflow-y: auto;
  position: relative;
}

.ai-quick-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 4px;
  padding: 6px 10px;
  background: rgba(15, 32, 56, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 14px;
  box-shadow: 0 4px 24px rgba(15, 32, 56, 0.3);
  z-index: 100;
  flex-wrap: wrap;
  max-width: 95vw;
  justify-content: center;
}

.ai-quick-bar .el-button {
  border: none;
  background: rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.85);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  padding: 5px 12px;
  transition: all 0.2s ease;
}

.ai-quick-bar .el-button:hover {
  background: rgba(255,255,255,0.18);
  color: #ffffff;
}

.ai-quick-bar .el-input {
  border-radius: 10px;
}

.ai-quick-bar .el-input :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.15);
  box-shadow: none !important;
  border-radius: 10px;
}
.ai-quick-bar .el-input :deep(.el-input__inner) {
  color: #fff;
}
.ai-quick-bar .el-input :deep(.el-input__inner::placeholder) {
  color: rgba(255,255,255,0.4);
}

.ai-preview-panel {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  width: 600px;
  max-width: 92vw;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.08);
  z-index: 110;
  overflow: hidden;
  animation: preview-slide-up 0.25s ease;
}

@keyframes preview-slide-up {
  from { opacity: 0; transform: translateX(-50%) translateY(12px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.ai-preview-header {
  font-weight: 600;
  font-size: 13px;
  padding: 12px 18px;
  color: #ffffff;
  background: linear-gradient(135deg, #1E3A5F, #2C5F9E);
}

.ai-preview-content {
  max-height: 240px;
  overflow-y: auto;
  padding: 14px 18px;
}

.ai-preview-original {
  font-size: 12px;
  color: var(--md-gray-600);
  margin-bottom: 10px;
  padding: 10px 14px;
  background: var(--md-gray-50);
  border-radius: 8px;
  border: 1px solid var(--md-gray-200);
  white-space: pre-wrap;
  line-height: 1.7;
}

.ai-preview-original strong {
  display: block;
  margin-bottom: 4px;
  color: var(--md-gray-400);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ai-preview-result {
  font-size: 12px;
  color: var(--md-gray-800);
  padding: 10px 14px;
  background: #EFF4FA;
  border-radius: 8px;
  border: 1px solid #B1C9E7;
  white-space: pre-wrap;
  line-height: 1.7;
}

.ai-preview-result strong {
  display: block;
  margin-bottom: 4px;
  color: var(--md-primary-500);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ai-preview-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding: 12px 18px;
  border-top: 1px solid var(--md-gray-100);
  background: var(--md-gray-50);
}
</style>
