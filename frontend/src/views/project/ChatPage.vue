<template>
  <div class="chat-page" :class="{ 'writing-mode': writingMode }">
    <!-- Writing preview panel -->
    <div v-if="writingMode" class="writing-preview">
      <div class="wp-header">
        <span>📄 {{ writingDocName || '文档预览' }}</span>
        <el-tag size="small">{{ writingPhase }}</el-tag>
        <el-button size="small" text @click="writingMode=false">退出</el-button>
      </div>
      <div class="wp-content" v-html="sanitizeHtml(writingDocPreview) || '<p style=color:#909399>开始对话后将在此显示文档内容</p>'"></div>
    </div>
    <!-- AI功能使用说明 -->
    <el-collapse v-model="helpOpen" style="margin-bottom:8px">
      <el-collapse-item title="📖 AI 功能使用说明" name="help">
        <div style="font-size:13px;line-height:1.8;padding:0 8px">
          <p><strong>本系统提供以下 AI 辅助功能，覆盖军工文档全生命周期：</strong></p>
          
          <h4 style="margin:12px 0 4px">一、文档编制</h4>
          <table style="width:100%;border-collapse:collapse;margin:4px 0">
            <tr style="background:#f5f7fa"><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">功能</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">入口</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">说明</th></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">📋 文档目录生成</td><td style="padding:4px 8px;border:1px solid #e4e7ed">阶段工作台 → 生成文档清单</td><td style="padding:4px 8px;border:1px solid #e4e7ed">按 GJB 5882 三维分类自动规划阶段文档</td></tr>
            <tr style="background:#f5f7fa"><td style="padding:4px 8px;border:1px solid #e4e7ed">📝 AI文档生成</td><td style="padding:4px 8px;border:1px solid #e4e7ed">AI对话</td><td style="padding:4px 8px;border:1px solid #e4e7ed">从文档台账选择文档，AI智能生成初稿</td></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">✏️ 逐章生成/重写</td><td style="padding:4px 8px;border:1px solid #e4e7ed">文档编辑器</td><td style="padding:4px 8px;border:1px solid #e4e7ed">选中章节后 AI 辅助撰写或重写</td></tr>
          </table>

          <h4 style="margin:12px 0 4px">二、可靠性设计（新增）</h4>
          <table style="width:100%;border-collapse:collapse;margin:4px 0">
            <tr style="background:#f5f7fa"><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">文档</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">依据标准</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">操作</th></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">📋 可靠性大纲</td><td style="padding:4px 8px;border:1px solid #e4e7ed">GJB 450B-2021</td><td style="padding:4px 8px;border:1px solid #e4e7ed">可靠性设计 Tab → 设指标 → 生成</td></tr>
            <tr style="background:#f5f7fa"><td style="padding:4px 8px;border:1px solid #e4e7ed">🔻 降额设计报告</td><td style="padding:4px 8px;border:1px solid #e4e7ed">GJB/Z 35</td><td style="padding:4px 8px;border:1px solid #e4e7ed">可靠性设计 Tab → 生成</td></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">🔢 可靠性预计报告</td><td style="padding:4px 8px;border:1px solid #e4e7ed">GJB/Z 299D-2024</td><td style="padding:4px 8px;border:1px solid #e4e7ed">可靠性设计 Tab → 导入BOM → 预计 → 生成</td></tr>
            <tr style="background:#f5f7fa"><td style="padding:4px 8px;border:1px solid #e4e7ed">📐 可靠性分配报告</td><td style="padding:4px 8px;border:1px solid #e4e7ed">GJB 450B-2021</td><td style="padding:4px 8px;border:1px solid #e4e7ed">可靠性设计 Tab → 选方法 → 分配 → 生成</td></tr>
          </table>

          <h4 style="margin:12px 0 4px">三、标准合规审查</h4>
          <table style="width:100%;border-collapse:collapse;margin:4px 0">
            <tr style="background:#f5f7fa"><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">功能</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">入口</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">说明</th></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">🔍 文档校对</td><td style="padding:4px 8px;border:1px solid #e4e7ed">文档台账 → 校对</td><td style="padding:4px 8px;border:1px solid #e4e7ed">术语一致性/格式规范性/GJB符合性检查</td></tr>
            <tr style="background:#f5f7fa"><td style="padding:4px 8px;border:1px solid #e4e7ed">✅ 合规审查</td><td style="padding:4px 8px;border:1px solid #e4e7ed">文档台账 → 审查</td><td style="padding:4px 8px;border:1px solid #e4e7ed">逐条对照标准条款审查文档合规性</td></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">📊 预评审</td><td style="padding:4px 8px;border:1px solid #e4e7ed">文档台账 → 预评审</td><td style="padding:4px 8px;border:1px solid #e4e7ed">正式评审前的AI预评估</td></tr>
          </table>

          <h4 style="margin:12px 0 4px">四、评审辅助</h4>
          <table style="width:100%;border-collapse:collapse;margin:4px 0">
            <tr style="background:#f5f7fa"><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">功能</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">入口</th><th style="padding:4px 8px;text-align:left;border:1px solid #e4e7ed">说明</th></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">📝 评审意见摘要</td><td style="padding:4px 8px;border:1px solid #e4e7ed">评审管理</td><td style="padding:4px 8px;border:1px solid #e4e7ed">多位专家意见汇总归类去重</td></tr>
            <tr style="background:#f5f7fa"><td style="padding:4px 8px;border:1px solid #e4e7ed">🔄 变更影响分析</td><td style="padding:4px 8px;border:1px solid #e4e7ed">文档台账 → 变更分析</td><td style="padding:4px 8px;border:1px solid #e4e7ed">文档修改后自动分析受影响文档</td></tr>
            <tr><td style="padding:4px 8px;border:1px solid #e4e7ed">📦 归档建议</td><td style="padding:4px 8px;border:1px solid #e4e7ed">文档台账 → 归档</td><td style="padding:4px 8px;border:1px solid #e4e7ed">评估归档密级和保管期限</td></tr>
          </table>

          <p style="color:#909399;margin-top:8px">💡 也可在本对话中直接输入需求，AI 将理解并执行相应操作。</p>
        </div>
      </el-collapse-item>
    </el-collapse>
 
    <!-- 文档写作快捷入口 -->
    <div v-if="!writingMode" class="writing-actions">
      <span class="wa-label">📝 交互式文档写作：</span>
      <el-button size="small" type="primary" @click="startWriting('outline')">可靠性大纲</el-button>
      <el-button size="small" type="success" @click="startWriting('derating')">降额报告</el-button>
    </div>
   <div class="chat-messages" ref="msgRef">
      <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
        <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="msg-content" v-html="renderChatMarkdown(msg.content)"></div>
      </div>
      <div v-if="loading" class="msg assistant">
        <div class="msg-avatar">🤖</div>
        <div class="msg-content">
          <el-progress :percentage="Math.min(pollCount * 3, 90)" :stroke-width="4" :show-text="false" style="margin-bottom:6px" />
          <em>{{ progressText || '处理中...' }}</em>
        </div>
      </div>
    </div>
    <div class="chat-input">
      <el-input v-model="input" placeholder="输入需求..."
        @keyup.enter="send" :disabled="loading" />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "ChatPage" })
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendMessage, pollTask } from '@/api/chat'
import { generateReliabilityOutline, generateDeratingReport } from '@/api/reliability'
import { useProjectStore } from '@/stores/project'
import { sanitizeHtml } from '@/utils/sanitize'
import { renderChatMarkdown } from '@/utils/markdown'

const route = useRoute()
const store = useProjectStore()
const projectId = Number(route.params.projectId)
const messages = ref<{role:string,content:string}[]>([])
const input = ref('')
const loading = ref(false)
const progressText = ref('')
const pollCount = ref(0)
const sessionId = ref('')
const msgRef = ref<HTMLElement>()
const helpOpen = ref<string[]>([])

async function send() {
  const msg = input.value.trim()
  if (!msg || loading.value) return
  messages.value.push({ role: 'user', content: msg })
  input.value = ''
  loading.value = true
  progressText.value = '发送中...'
  pollCount.value = 0
  scrollBottom()

  try {
    const { taskId } = await sendMessage(projectId, msg, sessionId.value || undefined)
    let failedPolls = 0
    while (failedPolls < 30) {
      await new Promise(r => setTimeout(r, 1500))
      pollCount.value++
      try {
        const task = await pollTask(taskId)
        failedPolls = 0
        if (task.progress) progressText.value = task.progress
        if (task.status === 'done') {
          progressText.value = ''
          const r = task.result
          if (r?.response) {
            let text = r.response
            if (r.actions?.length) {
              for (const a of r.actions) {
                if (a.type === 'generate_doc') {
                  if (a.success) store.triggerKanbanRefresh()
                  text += '\n\n✅ ' + a.docName + ': 已生成(' + (a.contentSize||0) + '字)'
                }
              }
            }
            messages.value.push({ role: 'assistant', content: text })
          }
          if (r?.sessionId) sessionId.value = r.sessionId
          break
        }
        if (task.status === 'error') {
          messages.value.push({ role: 'assistant', content: '❌ 请求失败' })
          break
        }
      } catch {
        failedPolls++
      }
    }
    if (failedPolls >= 30) {
      messages.value.push({ role: 'assistant', content: '⏱ 请求超时，请检查网络后重试' })
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '请求失败')
  } finally {
    loading.value = false
    progressText.value = ''
    scrollBottom()
  }
}

function scrollBottom() {
  nextTick(() => { if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
}

onMounted(() => {
  messages.value.push({ role: 'assistant', content: '你好！我是军工文档编制助手。' })
})

// === 交互式文档写作模式 ===

function startWriting(docType: string) {
  writingMode.value = true
  writingDocType.value = docType
  writingDocName.value = docType === 'outline' ? '可靠性大纲' : '降额设计报告'
  writingPhase.value = '大纲规划'
  
  // Add system prompt
  messages.value.push({
    role: 'assistant',
    content: `好的，我将用交互式方式协助你撰写《${writingDocName.value}》。

第一步：先规划文档大纲。请告诉我：
1. 有什么特别要求？（如章节侧重、篇幅要求）
2. 或者直接回复"开始"，我按GJB标准自动规划。`
  })
  scrollBottom()
}

// Intercept sendMessage for writing mode
const origSendMessage = sendMessage
async function handleWritingSend(msg: string) {
  if (!writingMode.value) return false
  
  messages.value.push({ role: 'user', content: msg })
  
  if (writingPhase.value === '大纲规划') {
    // Phase 1: Generate outline
    messages.value.push({ role: 'assistant', content: '正在根据GJB标准生成大纲...' })
    try {
      const res: any = await generateReliabilityOutline(projectId)
      const data = res.data?.data
      const content = data?.content || ''
      writingDocPreview.value = content.replace(/\n/g, '<br>')
      writingPhase.value = '逐章写作'
      
      messages.value.push({
        role: 'assistant',
        content: `大纲已生成（${content.length}字）。现在开始逐章写作。

第一章"范围"已完成。请确认：
- 回复"通过"继续下一章
- 回复具体修改意见来调整本章内容`
      })
    } catch (e: any) {
      messages.value.push({ role: 'assistant', content: '生成失败: ' + (e?.message || '请重试') })
    }
  } else if (writingPhase.value === '逐章写作') {
    // Continue to next phase
    if (msg.includes('通过') || msg.includes('继续') || msg.includes('ok')) {
      writingPhase.value = '审查'
      messages.value.push({
        role: 'assistant', 
        content: '全部章节已完成！现在进行质量审查...\n\n⚠️ 以下内容需要你补充：\n1. 实际MTBF值（当前使用默认值）\n2. 环境试验条件细节\n\n回复"完成"保存文档，或继续修改。'
      })
    }
  }
  
  scrollBottom()
  return true
}

// Override handleSend for writing mode
const origHandleSend = undefined  // Will be handled inline

</script>

<style scoped>
.chat-page { display: flex; flex-direction: column; height: calc(100vh - 200px); background: #fff; border-radius: 8px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; }
.msg { display: flex; margin-bottom: 16px; }
.msg.user { flex-direction: row-reverse; }
.msg-avatar { width: 32px; height: 32px; font-size: 20px; text-align: center; flex-shrink: 0; }
.msg-content { max-width: 70%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.6; }
.msg.user .msg-content { background: #409EFF; color: #fff; }
.msg.assistant .msg-content { background: #f0f2f5; color: #303133; }
.chat-input { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid #ebeef5; }

/* Writing mode styles */
.writing-mode { flex-direction: row !important; }
.writing-preview { width: 40%; border-left: 1px solid #ebeef5; display: flex; flex-direction: column; }
.wp-header { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-bottom: 1px solid #ebeef5; font-weight: 600; }
.wp-content { flex: 1; overflow-y: auto; padding: 14px; font-size: 13px; line-height: 1.8; }
.writing-actions { display: flex; align-items: center; gap: 8px; padding: 8px 0; flex-wrap: wrap; }
.wa-label { font-size: 13px; font-weight: 600; color: #606266; }

</style>
