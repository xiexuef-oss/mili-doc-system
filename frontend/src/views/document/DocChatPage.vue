<template>
  <div class="doc-chat">
    <!-- Left: AI Chat -->
    <div class="dc-left">
      <div class="dc-header">
        <span class="dc-h-title">AI 文档助手</span>
      </div>
      <div class="dc-messages" ref="msgRef">
        <div v-if="messages.length===0 && !sending" class="dc-welcome">
          <p>👋 我是 AI 文档助手</p>
          <p>你可以说：</p>
          <ul>
            <li><b>"生成大纲"</b> — 规划章节结构</li>
            <li><b>"写全文"</b> — 逐章撰写正文</li>
            <li><b>"修改第3章"</b> — 定点修改</li>
            <li><b>"在XX后新增一章"</b> — 增删章节</li>
          </ul>
        </div>
        <div v-for="(msg,i) in messages" :key="i" :class="['dc-msg',msg.role]">
          <div class="dc-msg-content" v-html="renderMsg(msg.content)"/>
        </div>
        <div v-if="sending" class="dc-msg ai"><div class="dc-msg-content">处理中...</div></div>
      </div>
      <div class="dc-input">
        <el-input v-model="input" placeholder="输入指令..." @keyup.enter="send" :disabled="sending" />
        <el-button type="primary" @click="send" :disabled="sending||!input.trim()">发送</el-button>
      </div>
    </div>

    <!-- Right: Document Workbench -->
    <div class="dc-right">
      <DocumentToolbar :doc-name="ledgerName" :chapters="chapters" :refreshing="refreshing"
        @write-all="handleWriteAll" @refresh="loadData" @export="exportDocx" />
      <div class="dc-doc-body">
        <div v-if="loading" style="text-align:center;padding:40px;color:var(--el-text-color-secondary)">加载中...</div>
        <!-- General mode: no document selected -->
        <div v-else-if="mode==='general'" class="dc-general">
          <h4>可用文档</h4>
          <div v-if="allLedgers.length===0" style="color:var(--el-text-color-secondary);text-align:center;padding:20px">
            暂无文档，在左侧输入"生成大纲"开始
          </div>
          <div v-for="item in allLedgers" :key="item.id" class="dc-ledger-item" @click="openDocument(item)">
            <span>{{ item.docName }}</span>
            <el-tag size="small" :type="item.lifecycleStatus==='DRAFTING'?'success':'info'">{{ item.lifecycleStatus==='DRAFTING'?'起草':'策划' }}</el-tag>
          </div>
        </div>
        <!-- Document mode: empty chapters -->
        <div v-else-if="chapters.length===0" style="text-align:center;padding:60px 20px;color:var(--el-text-color-secondary)">
          <p>📝 文档暂无章节</p>
          <p>在左侧说<b>"生成大纲"</b>开始</p>
        </div>
        <!-- Document mode: outline + content -->
        <div v-else class="dc-doc-layout">
          <OutlineSidebar :chapters="chapters" @select="scrollToChapter" />
          <div class="dc-content" ref="contentRef">
            <SectionBlock v-for="ch in chapters" :key="ch.id" :section="ch" :active="activeChapterId===ch.id"
              @ai-edit="(action:string)=>handleQuickEdit(ch,action)"
              @save="(content:string)=>handleSaveContent(ch,content)" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDocLedger } from '@/api/doc-ledger'
import { getChaptersByLedger, updateChapterContent, generateChapter, aiEditChapter } from '@/api/doc-chapter'
import { getToken } from '@/utils/auth'
import DocumentToolbar from '@/components/DocumentToolbar.vue'
import OutlineSidebar from '@/components/OutlineSidebar.vue'
import SectionBlock from '@/components/SectionBlock.vue'

const route = useRoute()
const router = useRouter()
const docLedgerId = computed(() => Number(route.query.docLedgerId || 0))
const projectId = computed(() => Number(route.query.projectId || 0))
const mode = computed(() => docLedgerId.value > 0 ? 'document' : 'general')

const ledgerName = ref('AI 文档写作')
const chapters = ref<any[]>([])
const allLedgers = ref<any[]>([])
const loading = ref(true)
const refreshing = ref(false)
const sending = ref(false)
const messages = ref<{role:string,content:string}[]>([])
const input = ref('')
const msgRef = ref<HTMLElement>()
const contentRef = ref<HTMLElement>()
const activeChapterId = ref<number|null>(null)

function renderMsg(text: string) { return text.replace(/\n/g,'<br>').replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>') }

async function loadData() {
  loading.value = true
  try {
    if (docLedgerId.value) {
      const res = await getDocLedger(docLedgerId.value)
      if (res.data?.data) ledgerName.value = res.data.data.docName || '文档'
      const listRes = await getChaptersByLedger(docLedgerId.value)
      chapters.value = (listRes.data?.data || []).sort((a:any,b:any)=>(a.orderNum||0)-(b.orderNum||0))
    } else {
      ledgerName.value = 'AI 文档写作'; chapters.value = []
      if (projectId.value) {
        const { getKanbanData } = await import('@/api/doc-ledger')
        const res = await getKanbanData(projectId.value)
        const data = res.data?.data || {}
        const all: any[] = []
        for (const items of Object.values(data) as any[]) for (const item of items as any[]) all.push(item)
        allLedgers.value = all
      }
    }
  } catch (e) { console.error('Load err:', e) }
  loading.value = false
}

function scrollToChapter(ch: any) {
  activeChapterId.value = ch.id
  const el = document.getElementById('section-'+ch.id)
  if (el) el.scrollIntoView({behavior:'smooth',block:'start'})
}

async function handleSaveContent(ch: any, content: string) {
  try {
    await updateChapterContent(ch.id, content, undefined, 1)
    ch.content = content
  } catch { ElMessage.error('保存失败') }
}

async function handleQuickEdit(ch: any, action: string) {
  messages.value.push({role:'USER',content:`${actionLabel(action)}「${ch.chapterTitle}」`})
  sending.value = true
  try {
    const res = await aiEditChapter(ch.id, action)
    const content = res.data?.data?.content
    if (content) { ch.content = content; await updateChapterContent(ch.id, content, undefined, 1) }
    messages.value.push({role:'AI',content:`已完成`})
  } catch { ElMessage.error('编辑失败') }
  sending.value = false
}

async function handleWriteAll() {
  const toWrite = chapters.value.filter(c => !c.content || c.content.length < 50)
  if (toWrite.length === 0) { ElMessage.info('所有章节已有内容'); return }
  messages.value.push({role:'AI',content:`开始逐章撰写（${toWrite.length}章待写）...`})
  for (let i = 0; i < toWrite.length; i++) {
    const ch = toWrite[i]
    try {
      const res = await generateChapter(ch.id, projectId.value)
      const content = res.data?.data
      if (content) { ch.content = content; await updateChapterContent(ch.id, content, undefined, 1) }
    } catch {}
    const last = messages.value[messages.value.length-1]
    if (last && last.role==='AI') last.content = `正在撰写：${i+1}/${toWrite.length} — ${ch.chapterTitle}`
  }
  await loadData()
  messages.value.push({role:'AI',content:`完成！${toWrite.length}章已撰写。`})
}

async function send() {
  const msg = input.value.trim()
  if (!msg || sending.value) return
  input.value = ''
  messages.value.push({role:'USER',content:msg})
  sending.value = true
  await handleCommand(msg)
  sending.value = false
  nextTick(()=>{ if(msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
}

async function handleCommand(cmd: string) {
  if (cmd.includes('大纲') || cmd.includes('结构')) await doGenerateOutline()
  else if (cmd.includes('写全') || cmd.includes('写所有')) await handleWriteAll()
  else if ((cmd.includes('修改')||cmd.includes('重写')) && /\d/.test(cmd)) await doEditChapter(cmd)
  else { messages.value.push({role:'AI',content:'收到。你说"生成大纲"规划章节，"写全文"撰写内容，"修改第X章"定点编辑。'}) }
}

async function doGenerateOutline() {
  messages.value.push({role:'AI',content:'正在生成文档结构...'})
  const token = getToken()
  const res = await fetch(`/api/v1/ai/draft/stream?projectId=${projectId.value}&docLedgerId=${docLedgerId.value}`,{
    headers:{'Authorization':token?`Bearer ${token}`:'','Accept':'text/event-stream'}
  })
  const reader = res.body?.getReader()
  if (!reader) { messages.value.push({role:'AI',content:'无法连接服务'}); return }
  const decoder = new TextDecoder(); let buf = ''
  while (true) {
    const {done,value} = await reader.read(); if (done) break
    buf += decoder.decode(value,{stream:true})
    const lines = buf.split('\n'); buf=lines.pop()||''
    for (const line of lines) {
      if (line.startsWith('data:') && line.includes('__STRUCTURE__:')) {
        const json = line.substring(line.indexOf('__STRUCTURE__:')+14)
        try {
          const s = JSON.parse(json)
          await fetch('/api/v1/ai/draft/apply-structure',{
            method:'POST',headers:{'Authorization':token?`Bearer ${token}`:'','Content-Type':'application/json'},
            body:JSON.stringify({docLedgerId:docLedgerId.value,projectId:projectId.value,chapters:s.chapters,markdownContent:s.markdownContent,suggestedTemplateName:s.suggestedTemplateName,saveAsTemplate:true})
          })
        } catch {}
      }
    }
  }
  await loadData()
  messages.value.push({role:'AI',content:`大纲已生成（${chapters.value.length}章）。说"写全文"开始撰写。`})
}

async function doEditChapter(cmd: string) {
  const num = cmd.match(/([\d.]+)/)?.[0]
  const ch = chapters.value.find(c => c.chapterNumber === num)
  if (!ch) { messages.value.push({role:'AI',content:'未找到章节'+num}); return }
  messages.value.push({role:'AI',content:'正在重写...'})
  const res = await aiEditChapter(ch.id, 'rewrite', cmd)
  const content = res.data?.data?.content
  if (content) { ch.content = content; await updateChapterContent(ch.id, content, undefined, 1) }
  messages.value.push({role:'AI',content:'已更新'})
}

function exportDocx() {
  if (!docLedgerId.value) return
  const token = getToken()
  fetch(`/api/v1/docx/generate/${docLedgerId.value}?includeCover=true`,{
    headers:{'Authorization':token?`Bearer ${token}`:''}
  }).then(r=>r.blob()).then(b=>{
    const a=document.createElement('a'); a.href=URL.createObjectURL(b); a.download='document.docx'; a.click()
  })
}

function openDocument(item: any) {
  router.push({name:'ChatPage',query:{projectId:projectId.value,docLedgerId:item.id}})
}

function actionLabel(a: string): string {
  return ({rewrite:'重写',expand:'扩写',shorten:'缩短',polish:'优化'})[a] || a
}

onMounted(() => loadData())
</script>

<style scoped>
.doc-chat { display:flex; height:calc(100vh - 60px); background:var(--el-bg-color-page); }
.dc-left { width:35%; min-width:280px; display:flex; flex-direction:column; background:#fff; border-right:1px solid var(--el-border-color-light); }
.dc-header { padding:10px 14px; border-bottom:1px solid var(--el-border-color-light); flex-shrink:0; }
.dc-h-title { font-weight:600; font-size:14px; }
.dc-messages { flex:1; overflow-y:auto; padding:14px; }
.dc-welcome { color:var(--el-text-color-secondary); font-size:13px; line-height:1.8; }
.dc-welcome ul { padding-left:18px; margin:4px 0; }
.dc-msg { margin-bottom:12px; max-width:95%; }
.dc-msg.user { text-align:right; }
.dc-msg.user .dc-msg-content { display:inline-block; background:var(--el-color-primary-light-9); padding:8px 14px; border-radius:12px 4px 12px 12px; font-size:13px; }
.dc-msg.ai .dc-msg-content { background:var(--el-fill-color); padding:10px 14px; border-radius:4px 12px 12px 12px; font-size:13px; line-height:1.7; }
.dc-input { display:flex; gap:8px; padding:10px 14px; border-top:1px solid var(--el-border-color-light); flex-shrink:0; }
.dc-right { flex:1; display:flex; flex-direction:column; background:#fff; min-width:0; }
.dc-doc-body { flex:1; overflow:hidden; display:flex; }
.dc-general { padding:16px; overflow-y:auto; }
.dc-ledger-item { display:flex; justify-content:space-between; align-items:center; padding:8px 12px; cursor:pointer; border-radius:4px; margin-bottom:4px; }
.dc-ledger-item:hover { background:var(--el-fill-color-light); }
.dc-doc-layout { display:flex; flex:1; overflow:hidden; }
.dc-content { flex:1; overflow-y:auto; padding:12px; }
</style>
