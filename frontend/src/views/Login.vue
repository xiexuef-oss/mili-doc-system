<template>
  <div class="login-page">
    <!-- Left Brand Panel -->
    <div class="login-hero">
      <div class="hero-content">
        <div class="hero-icon">
          <svg viewBox="0 0 64 64" fill="none" width="64" height="64">
            <rect x="4" y="4" width="56" height="56" rx="12" fill="rgba(255,255,255,0.1)" stroke="rgba(255,255,255,0.2)" stroke-width="1.5"/>
            <path d="M16 40V22l12 14 8-10 12 18" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="46" cy="18" r="3" fill="#60A5FA"/>
          </svg>
        </div>
        <h1 class="hero-title">军工项目文档</h1>
        <h2 class="hero-subtitle">策划与编制一体机</h2>
        <p class="hero-desc">
          基于 AI 的军工文档全生命周期管理平台<br/>
          从策划、编写、评审到归档，一站式完成
        </p>
        <div class="hero-features">
          <div class="hero-feature">
            <span class="feature-dot"></span>
            AI 智能写作辅助
          </div>
          <div class="hero-feature">
            <span class="feature-dot"></span>
            文档完整性检查
          </div>
          <div class="hero-feature">
            <span class="feature-dot"></span>
            多级签审流程
          </div>
        </div>
      </div>
      <div class="hero-footer">
        <span>© 2026 MILI-DOC System v2.0</span>
      </div>
    </div>

    <!-- Right Login Panel -->
    <div class="login-panel">
      <div class="login-card">
        <div class="login-header">
          <h3>欢迎回来</h3>
          <p>请登录您的账户以继续</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="0"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              class="login-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              class="login-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>演示账户：admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, InfoFilled } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { setToken, setUser } from '@/utils/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  loading.value = true
  try {
    const res = await login(form)
    const data = res.data.data
    setToken(data.token)
    setUser({
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      token: data.token,
      roles: data.roles || []
    })
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ---- Layout ---- */
.login-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ---- Hero (Left) ---- */
.login-hero {
  flex: 1;
  background: linear-gradient(135deg, #0F2038 0%, #152D4A 40%, #1E3A5F 70%, #1B3A5C 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(59,130,246,0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(44,95,158,0.1) 0%, transparent 50%);
  pointer-events: none;
}

.login-hero::after {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255,255,255,0.03) 0%, transparent 70%);
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 0 60px;
}

.hero-icon {
  margin-bottom: 28px;
  display: inline-block;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 1px;
  margin-bottom: 4px;
}

.hero-subtitle {
  font-size: 18px;
  font-weight: 500;
  color: rgba(255,255,255,0.6);
  letter-spacing: 2px;
  margin-bottom: 28px;
}

.hero-desc {
  font-size: 14px;
  color: rgba(255,255,255,0.45);
  line-height: 1.8;
  margin-bottom: 36px;
}

.hero-features {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.hero-feature {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255,255,255,0.55);
  font-size: 13px;
  font-weight: 500;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3B82F6;
  box-shadow: 0 0 8px rgba(59,130,246,0.5);
}

.hero-footer {
  position: absolute;
  bottom: 24px;
  color: rgba(255,255,255,0.25);
  font-size: 12px;
  z-index: 1;
}

/* ---- Login Panel (Right) ---- */
.login-panel {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 380px;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h3 {
  font-size: 24px;
  font-weight: 700;
  color: var(--md-gray-900);
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: var(--md-gray-500);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-input :deep(.el-input__wrapper) {
  padding: 12px 16px;
  background: var(--md-gray-50);
  border-radius: var(--md-radius-md);
}

.login-input :deep(.el-input__wrapper:hover) {
  background: #ffffff;
}

.login-input :deep(.el-input__prefix) {
  color: var(--md-gray-400);
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: var(--md-radius-md);
  margin-top: 8px;
}

.login-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
  padding: 10px 16px;
  background: var(--md-gray-50);
  border-radius: var(--md-radius-sm);
  font-size: 12px;
  color: var(--md-gray-500);
}

.login-hint .el-icon {
  color: var(--md-gray-400);
  font-size: 14px;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .login-hero {
    display: none;
  }
  .login-panel {
    width: 100%;
  }
}
</style>
