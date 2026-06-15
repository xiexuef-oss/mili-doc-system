<template>
  <div class="ai-chat-float">
    <!-- Floating button -->
    <div v-if="!open" class="ai-chat-btn" @click="open = true" title="AI 工程助手">
      <el-icon :size="22"><ChatDotRound /></el-icon>
    </div>

    <!-- Chat panel -->
    <transition name="slide-up">
      <div v-if="open" class="ai-chat-panel">
        <div class="chat-header">
          <div class="chat-header-title">
            <el-icon><Cpu /></el-icon>
            <span>AI 工程助手</span>
            <el-tag size="small" :type="healthOk ? 'success' : 'danger'" effect="dark">{{ healthOk ? '在线' : '离线' }}</el-tag>
          </div>
          <div class="chat-header-actions">
            <el-button link size="small" @click="clearChat">清空对话</el-button>
            <el-button link size="small" @click="open = false"><el-icon><Close /></el-icon></el-button>
          </div>
        </div>

        <div class="chat-body" ref="chatBodyRef">
          <div v-if="messages.length === 0" class="chat-welcome">
            <el-icon :size="36"><Cpu /></el-icon>
            <p>你好！我是军工项目 AI 工程助手。</p>
            <p>可以向我咨询：</p>
            <ul>
              <li>GJB 标准条款解读</li>
              <li>技术文档编写规范</li>
              <li>技术方案分析建议</li>
              <li>项目管理相关问题</li>
            </ul>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-msg', msg.role]">
            <div class="chat-msg-avatar">
              <el-icon v-if="msg.role === 'assistant'" :size="18"><Cpu /></el-icon>
              <el-icon v-else :size="18"><User /></el-icon>
            </div>
            <div class="chat-msg-content">
              <div class="chat-msg-text">{{ msg.content }}</div>
              <div v-if="msg.role === 'assistant' && idx === messages.length - 1 && streaming" class="typing-indicator">
                <div v-if="progressText" style="font-size:12px;color:#909399;margin-bottom:4px">{{ progressText }}</div>
                <span class="dot" />
                <span class="dot" />
                <span class="dot" />
              </div>
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="2"
            placeholder="输入你的工程问题..."
            :disabled="streaming"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button
            type="primary"
            :disabled="!userInput.trim() || streaming"
            :loading="streaming"
            @click="sendMessage"
            style="margin-top:8px;width:100%"
          >
            {{ streaming ? 'AI 思考中...' : '发送' }}
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Cpu, User, Close } from '@element-plus/icons-vue'
import { checkAiHealth } from '@/api/ai'
import { sendMessage, pollTask } from '@/api/chat'

const route = useRoute()
const projectId = computed(() => {
  const id = Number(route.params.projectId)
  return id > 0 ? id : undefined
})

const open = ref(false)
const healthOk = ref(false)
const streaming = ref(false)
const userInput = ref('')
const messages = ref<{ role: string; content: string }[]>([])
const chatBodyRef = ref<HTMLElement>()
const progressText = ref('')

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const text = userInput.value.trim()
  if (!text || streaming.value) return

  messages.value.push({ role: 'user', content: text })
  userInput.value = ''
  scrollToBottom()

  streaming.value = true
  const assistantIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  progressText.value = '思考中...'

  try {
    if (!projectId.value) {
      messages.value[assistantIdx].content = '请先进入一个项目。'
      streaming.value = false
      return
    }
    const { taskId } = await sendMessage(projectId.value, text)
    let failed = 0
    while (failed < 30) {
      await new Promise(r => setTimeout(r, 1500))
      try {
        const task = await pollTask(taskId)
        failed = 0
        if (task.progress) progressText.value = task.progress
        if (task.status === 'done') {
          progressText.value = ''
          const r = task.result
          if (r?.response) {
            let t = r.response
            if (r.actions?.length) {
              for (const a of r.actions) {
                if (a.type === 'generate_doc' && a.success) t += '\n\n✅ ' + a.docName + ' 已生成 (' + a.contentSize + '字)'
              }
            }
            messages.value[assistantIdx].content = t
          }
          break
        }
        if (task.status === 'error') { messages.value[assistantIdx].content = '请求失败'; break }
      } catch { failed++ }
    }
  } catch {
    messages.value[assistantIdx].content = '抱歉，请求失败，请重试。'
  } finally {
    streaming.value = false
    progressText.value = ''
    scrollToBottom()
  }
}

function clearChat() {
  if (chatAbortController) chatAbortController.abort()
  messages.value = []
  streaming.value = false
}

async function initHealth() {
  try {
    const res = await checkAiHealth()
    healthOk.value = res.data.data?.connected === true
  } catch { healthOk.value = false }
}

watch(open, (val) => {
  if (val && messages.value.length === 0) initHealth()
})

onMounted(() => initHealth())
</script>

<style scoped>
.ai-chat-float {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 2000;
}

.ai-chat-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0,0,0,0.2);
  transition: transform .2s, box-shadow .2s;
}
.ai-chat-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(0,0,0,0.3);
}

.ai-chat-panel {
  width: 400px;
  height: 540px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.slide-up-enter-active, .slide-up-leave-active {
  transition: all .25s ease;
}
.slide-up-enter-from, .slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

.chat-header {
  padding: 12px 16px;
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 15px;
}
.chat-header-actions {
  display: flex;
  gap: 4px;
}
.chat-header-actions .el-button {
  color: rgba(255,255,255,0.85);
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--el-fill-color-lighter);
}

.chat-welcome {
  text-align: center;
  padding: 32px 16px;
  color: var(--el-text-color-secondary);
}
.chat-welcome p {
  margin: 8px 0;
}
.chat-welcome ul {
  display: inline-block;
  text-align: left;
  padding-left: 20px;
  font-size: 13px;
}
.chat-welcome li {
  margin: 4px 0;
}

.chat-msg {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.chat-msg.user {
  flex-direction: row-reverse;
}
.chat-msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--el-fill-color);
}
.chat-msg.user .chat-msg-avatar {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}
.chat-msg.assistant .chat-msg-avatar {
  background: var(--el-color-success-light-8);
  color: var(--el-color-success);
}
.chat-msg-content {
  max-width: 280px;
}
.chat-msg-text {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-msg.user .chat-msg-text {
  background: var(--el-color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-msg.assistant .chat-msg-text {
  background: #fff;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-indicator .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
  animation: blink 1.4s infinite both;
}
.typing-indicator .dot:nth-child(2) { animation-delay: .2s; }
.typing-indicator .dot:nth-child(3) { animation-delay: .4s; }
@keyframes blink {
  0% { opacity: .2; }
  20% { opacity: 1; }
  100% { opacity: .2; }
}

.chat-footer {
  padding: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: #fff;
}
.chat-footer :deep(.el-textarea__inner) {
  font-size: 13px;
}
</style>
