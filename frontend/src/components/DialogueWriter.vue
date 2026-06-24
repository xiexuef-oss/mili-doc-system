<template>
  <div class="dw-container">
    <!-- Left: Document Preview -->
    <div class="dw-preview">
      <div class="dw-preview-header">
        <span>文档预览</span>
        <el-tag size="small">{{ session?.currentPhase || '策划中' }}</el-tag>
      </div>
      <div class="dw-preview-content" v-html="renderedPreview"/>
    </div>

    <!-- Center: Dialogue -->
    <div class="dw-chat">
      <div class="dw-messages" ref="msgRef">
        <div v-for="(msg, i) in messages" :key="i" :class="['dw-msg', msg.role]">
          <div class="dw-msg-avatar">{{ msg.role === 'AI' ? 'AI' : '我' }}</div>
          <div class="dw-msg-bubble">{{ msg.content }}</div>
        </div>
        <div v-if="sending" class="dw-msg AI"><div class="dw-msg-avatar">AI</div><div class="dw-msg-bubble">...</div></div>
      </div>
      <div class="dw-input">
        <el-input v-model="input" placeholder="输入消息..." @keyup.enter="send" :disabled="sending"/>
        <el-button type="primary" @click="send" :disabled="sending || !input.trim()">发送</el-button>
      </div>
    </div>

    <!-- Right: Status -->
    <div class="dw-status">
      <div class="dw-status-section">
        <div class="dw-status-title">会话状态</div>
        <div class="dw-status-item">阶段: {{ session?.currentPhase || '-' }}</div>
        <div class="dw-status-item">消息: {{ messages.length }}</div>
        <div class="dw-status-item">文档: {{ session?.docName || '-' }}</div>
      </div>
      <div class="dw-status-section">
        <el-button size="small" @click="handleExport">导出为草稿</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import request from '@/api/index'
import { ElMessage } from 'element-plus'

const props = defineProps<{ projectId: number; docType: string; docName?: string }>()

interface Message { role: string; content: string }

const session = ref<any>(null)
const messages = ref<Message[]>([])
const input = ref('')
const sending = ref(false)
const msgRef = ref<HTMLElement>()

const renderedPreview = computed(() => {
  if (!session.value?.docContent) return '<p style="color:#909399">对话开始后将在此显示文档预览</p>'
  return session.value.docContent.replace(/\n/g, '<br>')
})

async function init() {
  try {
    const res = await request.post('/reliability/dialogue/sessions', null, {
      params: { projectId: props.projectId, docType: props.docType, docName: props.docName || '文档' }
    })
    session.value = res.data?.data
    if (session.value?.lastAiMessage) {
      messages.value.push({ role: 'AI', content: session.value.lastAiMessage })
    }
  } catch (e: any) {
    ElMessage.error('启动对话失败: ' + (e?.message || '未知错误'))
  }
}

async function send() {
  if (!input.value.trim() || !session.value?.sessionId) return
  const msg = input.value.trim()
  input.value = ''
  messages.value.push({ role: 'USER', content: msg })
  sending.value = true
  try {
    const res = await request.post(`/reliability/dialogue/sessions/${session.value.sessionId}/message`, { message: msg })
    const data = res.data?.data
    messages.value.push({ role: 'AI', content: data.aiMessage })
    session.value = { ...session.value, docContent: data.docPreview, currentPhase: data.currentPhase }
  } catch (e: any) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
    nextTick(() => { if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
  }
}

function handleExport() {
  if (session.value?.docContent) {
    const blob = new Blob([session.value.docContent], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = `${session.value.docName || '文档'}_草稿.md`
    a.click(); URL.revokeObjectURL(url)
  }
}

onMounted(init)
</script>

<style scoped>
.dw-container { display: flex; height: calc(100vh - 120px); gap: 12px; }
.dw-preview { flex: 1; border: 1px solid #ebeef5; border-radius: 8px; display: flex; flex-direction: column; }
.dw-preview-header { padding: 10px 14px; border-bottom: 1px solid #ebeef5; display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.dw-preview-content { flex: 1; overflow-y: auto; padding: 14px; font-size: 13px; line-height: 1.8; }
.dw-chat { flex: 1.5; border: 1px solid #ebeef5; border-radius: 8px; display: flex; flex-direction: column; }
.dw-messages { flex: 1; overflow-y: auto; padding: 12px; }
.dw-msg { margin-bottom: 12px; display: flex; gap: 8px; }
.dw-msg.USER { flex-direction: row-reverse; }
.dw-msg-avatar { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 600; flex-shrink: 0; }
.dw-msg.AI .dw-msg-avatar { background: #409eff; color: #fff; }
.dw-msg.USER .dw-msg-avatar { background: #67c23a; color: #fff; }
.dw-msg-bubble { max-width: 70%; padding: 8px 12px; border-radius: 8px; font-size: 13px; line-height: 1.6; white-space: pre-wrap; }
.dw-msg.AI .dw-msg-bubble { background: #f0f2f5; }
.dw-msg.USER .dw-msg-bubble { background: #ecf5ff; }
.dw-input { display: flex; gap: 8px; padding: 10px; border-top: 1px solid #ebeef5; }
.dw-status { width: 200px; border: 1px solid #ebeef5; border-radius: 8px; padding: 12px; }
.dw-status-section { margin-bottom: 16px; }
.dw-status-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.dw-status-item { font-size: 12px; color: #606266; padding: 3px 0; }
</style>
