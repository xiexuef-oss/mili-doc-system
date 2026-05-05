<template>
  <div class="login-container">
    <div class="login-card">
      <h2>军工项目文档策划与编制一体机</h2>
      <p class="subtitle">用户登录</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
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
      token: data.token
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
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a2a6c, #304156, #1a2a6c);
}
.login-card {
  width: 420px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.2);
}
.login-card h2 {
  text-align: center;
  font-size: 20px;
  margin-bottom: 8px;
  color: #303133;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
}
</style>
