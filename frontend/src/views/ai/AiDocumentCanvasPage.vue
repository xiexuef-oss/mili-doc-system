<template>
  <div class="canvas-page">
    <div class="cp-left">
      <div class="cp-header">
        <span class="cp-title">AI 文档助手</span>
        <el-button v-if="!store.document" size="small" type="primary" @click="showCreate=true">新建文档</el-button>
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
        <el-button type="primary" @click="showCreate=true">新建文档</el-button>
      </div>
      <template v-else-if="store.document">
        <div class="cp-toolbar">
          <span class="cp-toolbar-title">📄 {{ store.document.title }}</span>
          <div class="cp-toolbar-center">
            <span class="cp-toolbar-stats">{{ store.saveStatus==='saving'?'保存中...':store.saveStatus==='unsaved'?'未保存':'已保存' }}</span>
            <el-button size="small" type="success" :disabled="store.generating" @click="handleGenerateAll">AI写全部</el-button>
            <el-button size="small" @click="handleExport">导出</el-button>
          </div>
          <div class="cp-toolbar-actions">
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
              <CanvasEditor :content-json="store.contentJson"
                @update:content-json="store.handleContentUpdate"
                @selection-change="onSelectionChange"
                @active-heading-change="(id:string|null)=>store.activeHeadingId=id" />
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Quick AI bar -->
    <div v-if="selectionCtx" class="ai-quick-bar">
      <el-button size="small" @click="aiQuickAction('polish')">✨ 润色</el-button>
      <el-button size="small" @click="aiQuickAction('expand')">📝 扩写</el-button>
      <el-button size="small" @click="aiQuickAction('shorten')">📏 缩短</el-button>
      <el-button size="small" @click="aiQuickAction('rewrite')">🔄 重写</el-button>
      <el-input v-model="customInstruction" size="small" placeholder="自定义..." style="width:140px" @keyup.enter="aiQuickAction('custom')"/>
    </div>

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
import { ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDocumentCanvasStore } from '@/stores/documentCanvas'
import * as aiDocApi from '@/api/ai-document'
import type { EditorSelectionContext } from '@/utils/editorPatch'
import CanvasEditor from '@/components/CanvasEditor.vue'
import OutlineNavigator from '@/components/OutlineNavigator.vue'
import VersionHistory from '@/components/VersionHistory.vue'

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
  store.generating = true; messages.value.push({role:'assistant',content:'开始生成大纲和正文...'})
  abortCtrl = aiDocApi.streamGenerateDocument(store.document.id,
    (patch) => store.applyCanvasPatches([patch]),
    (msg) => messages.value.push({role:'assistant',content:msg}),
    () => { store.generating = false; messages.value.push({role:'assistant',content:'生成完成！'}) }
  )
}

async function send() {
  const msg = input.value.trim(); if (!msg || store.generating) return
  input.value = ''; messages.value.push({role:'user',content:msg})
  if (!store.document) { await store.createDocument(projectId, msg, ''); messages.value.push({role:'assistant',content:'文档已创建...'}); handleGenerateAll(); return }
  try {
    const res = await aiDocApi.chatWithDocument(store.document.id, msg)
    const reply = res.data?.data
    messages.value.push({role:'assistant',content:reply?.reply||'已处理'})
    if (reply?.patches) store.applyCanvasPatches(reply.patches)
  } catch { messages.value.push({role:'assistant',content:'处理失败'}) }
  nextTick(()=>{ if(msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
}

function onSelectionChange(ctx: EditorSelectionContext | null) { selectionCtx.value = ctx }

async function aiQuickAction(mode: string) {
  if (!selectionCtx.value || !store.document?.id) return
  const ctx = selectionCtx.value
  try {
    const instruction = mode === 'custom' ? (customInstruction.value || mode) : mode
    const res = await aiDocApi.aiEditSelection(store.document.id, ctx, instruction)
    const patch = res.data?.data
    if (patch) { store.applyCanvasPatches([patch]) }
    customInstruction.value = ''
  } catch { ElMessage.error('AI操作失败') }
}

function scrollToHeading(id: string) {
  store.activeHeadingId = id
  const el = document.querySelector(`[data-heading-id="${id}"]`)
  el?.scrollIntoView({behavior:'smooth',block:'start'})
}

function handleExport() { if(store.document?.id) window.open(`/api/v1/docx/generate/${store.document.id}?includeCover=true`) }
</script>

<style scoped>
.canvas-page { display:flex; height:100%; overflow:hidden; background:var(--el-bg-color-page); }
.cp-left { width:35%; min-width:280px; max-width:420px; height:100%; display:flex; flex-direction:column; background:#fff; border-right:1px solid var(--el-border-color-light); }
.cp-header { display:flex; justify-content:space-between; align-items:center; padding:10px 14px; border-bottom:1px solid var(--el-border-color-light); flex-shrink:0; }
.cp-title { font-weight:600; font-size:14px; }
.cp-messages { flex:1; min-height:0; overflow-y:auto; padding:14px; }
.cp-welcome { font-size:13px; color:var(--el-text-color-secondary); line-height:1.8; }
.cp-welcome ul { padding-left:18px; margin:4px 0; }
.cp-msg { margin-bottom:10px; } .cp-msg.user { text-align:right; }
.cp-msg.user .cp-msg-content { display:inline-block; background:var(--el-color-primary-light-9); padding:8px 14px; border-radius:12px 4px 12px 12px; font-size:13px; }
.cp-msg.ai .cp-msg-content,.cp-msg.assistant .cp-msg-content { background:var(--el-fill-color); padding:10px 14px; border-radius:4px 12px 12px 12px; font-size:13px; line-height:1.7; }
.cp-input { display:flex; gap:8px; padding:10px 14px; border-top:1px solid var(--el-border-color-light); flex-shrink:0; }
.cp-right { flex:1; display:flex; flex-direction:column; background:#fff; min-width:0; overflow:hidden; }
.cp-empty { text-align:center; padding:80px 20px; color:var(--el-text-color-secondary); flex:1; }
.cp-toolbar { display:flex; justify-content:space-between; align-items:center; padding:10px 14px; border-bottom:1px solid var(--el-border-color-light); flex-shrink:0; gap:8px; }
.cp-toolbar-title { font-weight:600; font-size:14px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.cp-toolbar-center { display:flex; align-items:center; gap:8px; }
.cp-toolbar-stats { font-size:11px; color:var(--el-text-color-secondary); white-space:nowrap; }
.cp-toolbar-actions { display:flex; gap:6px; }
.cp-doc-body { flex:1; overflow:hidden; display:flex; }
.cp-doc-layout { display:flex; flex:1; overflow:hidden; }
.cp-content { flex:1; overflow-y:auto; }
.ai-quick-bar { position:fixed; bottom:12px; left:50%; transform:translateX(-50%); display:flex; gap:6px; padding:8px 14px; background:#fff; border-radius:8px; box-shadow:0 2px 12px rgba(0,0,0,.12); z-index:100; }
</style>
