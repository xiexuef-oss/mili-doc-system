<template>
  <div class="ai-chat-float">
    <!-- Floating button -->
    <div v-if="!open" class="ai-chat-btn" @click="open = true" title="AI 工程助手">
      <div class="btn-pulse"></div>
      <el-icon :size="22"><ChatDotRound /></el-icon>
    </div>

    <!-- Chat panel -->
    <transition name="slide-up">
      <div v-if="open" class="ai-chat-panel">
        <div class="chat-header">
          <div class="chat-header-title">
            <div class="header-icon">
              <el-icon :size="16"><Cpu /></el-icon>
            </div>
            <span>AI 工程助手</span>
            <span class="header-dot" :class="{ online: healthOk }"></span>
            <span class="header-status">{{ healthOk ? '在线' : '离线' }}</span>
          </div>
          <div class="chat-header-actions">
            <el-button link size="small" @click="clearChat">清空</el-button>
            <el-button link size="small" @click="open = false">
              <el-icon :size="16"><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="chat-body" ref="chatBodyRef">
          <div v-if="messages.length === 0" class="chat-welcome">
            <div class="welcome-icon">
              <el-icon :size="40"><Cpu /></el-icon>
            </div>
            <p class="welcome-title">军工项目 AI 工程助手</p>
            <p class="welcome-desc">可以向我咨询：</p>
            <div class="welcome-tags">
              <span class="welcome-tag">GJB 标准解读</span>
              <span class="welcome-tag">文档编写规范</span>
              <span class="welcome-tag">方案分析建议</span>
              <span class="welcome-tag">项目管理咨询</span>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-msg', msg.role]">
            <div class="chat-msg-avatar">
              <el-icon v-if="msg.role === 'assistant'" :size="16"><Cpu /></el-icon>
              <el-icon v-else :size="16"><User /></el-icon>
            </div>
            <div class="chat-msg-bubble">
              <div class="chat-msg-text">{{ msg.content }}</div>
              <div v-if="msg.role === 'assistant' && idx === messages.length - 1 && streaming" class="typing-indicator">
                <div v-if="progressText" class="progress-hint">{{ progressText }}</div>
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
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button
            type="primary"
            :disabled="!userInput.trim() || streaming"
            :loading="streaming"
            @click="handleSend"
            class="send-btn"
          >
            {{ streaming ? 'AI 思考中...' : '发 送' }}
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
let chatAbortController: AbortController | null = null

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

async function handleSend() {
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
  bottom: 28px;
  right: 28px;
  z-index: 2000;
}

/* Floating Button */
.ai-chat-btn {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1E3A5F, #2C5F9E);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(30, 58, 95, 0.35);
  transition: all 0.25s ease;
  position: relative;
}

.ai-chat-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(30, 58, 95, 0.45);
}

.btn-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 20px;
  background: transparent;
  border: 2px solid rgba(30, 58, 95, 0.2);
  animation: pulse-ring 2s ease-out infinite;
}

@keyframes pulse-ring {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.2); opacity: 0; }
}

/* Chat Panel */
.ai-chat-panel {
  width: 420px;
  height: 560px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.15), 0 2px 8px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.slide-up-enter-active, .slide-up-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-up-enter-from, .slide-up-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}

/* Header */
.chat-header {
  padding: 14px 18px;
  background: linear-gradient(135deg, #1E3A5F, #2C5F9E);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.chat-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.header-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(255,255,255,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #EF4444;
  margin-left: 4px;
}

.header-dot.online {
  background: #22C55E;
  box-shadow: 0 0 6px rgba(34,197,94,0.5);
}

.header-status {
  font-size: 11px;
  font-weight: 500;
  color: rgba(255,255,255,0.7);
}

.chat-header-actions .el-button {
  color: rgba(255,255,255,0.8) !important;
}
.chat-header-actions .el-button:hover {
  color: #ffffff !important;
}

/* Body */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #F8FAFC;
}

/* Welcome */
.chat-welcome {
  text-align: center;
  padding: 40px 20px;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #EFF4FA, #D6E3F3);
  color: #2C5F9E;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.welcome-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--md-gray-800);
  margin-bottom: 4px;
}

.welcome-desc {
  font-size: 13px;
  color: var(--md-gray-500);
  margin-bottom: 16px;
}

.welcome-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.welcome-tag {
  padding: 4px 12px;
  background: #ffffff;
  border: 1px solid var(--md-gray-200);
  border-radius: 20px;
  font-size: 12px;
  color: var(--md-gray-600);
  font-weight: 500;
}

/* Messages */
.chat-msg {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.chat-msg.user {
  flex-direction: row-reverse;
}

.chat-msg-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--md-gray-200);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--md-gray-500);
}

.chat-msg.user .chat-msg-avatar {
  background: linear-gradient(135deg, #2C5F9E, #3B82F6);
  color: #fff;
  border: none;
}

.chat-msg.assistant .chat-msg-avatar {
  background: #fff;
  color: #2C5F9E;
  border-color: var(--md-gray-200);
}

.chat-msg-bubble {
  max-width: 290px;
}

.chat-msg-text {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-msg.user .chat-msg-text {
  background: linear-gradient(135deg, #2C5F9E, #1E3A5F);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-msg.assistant .chat-msg-text {
  background: #fff;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  color: var(--md-gray-800);
}

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 6px 0 2px;
}

.progress-hint {
  font-size: 11px;
  color: var(--md-gray-400);
  margin-bottom: 4px;
}

.typing-indicator .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--md-gray-300);
  animation: blink 1.4s infinite both;
}

.typing-indicator .dot:nth-child(2) { animation-delay: .2s; }
.typing-indicator .dot:nth-child(3) { animation-delay: .4s; }

@keyframes blink {
  0% { opacity: .2; }
  20% { opacity: 1; }
  100% { opacity: .2; }
}

/* Footer */
.chat-footer {
  padding: 12px 14px;
  border-top: 1px solid var(--md-gray-200);
  background: #fff;
  flex-shrink: 0;
}

.chat-footer :deep(.el-textarea__inner) {
  font-size: 13px;
  border-radius: 10px;
  background: var(--md-gray-50);
  border-color: var(--md-gray-200);
  resize: none;
}

.chat-footer :deep(.el-textarea__inner:focus) {
  background: #fff;
}

.send-btn {
  width: 100%;
  margin-top: 8px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 13px;
}
</style>
