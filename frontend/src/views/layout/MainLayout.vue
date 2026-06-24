<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <h2>军工文档系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon>
          <span>项目管理</span>
        </el-menu-item>
        <el-menu-item index="/templates">
          <el-icon><Files /></el-icon>
          <span>模版库</span>
        </el-menu-item>
        <el-menu-item index="/standards">
          <el-icon><Collection /></el-icon>
          <span>标准库</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Reading /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/ai-training">
          <el-icon><Cpu /></el-icon>
          <span>模型训练</span>
        </el-menu-item>
        <el-sub-menu v-if="isAdmin()" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><UserFilled /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/dicts">
            <el-icon><Menu /></el-icon>
            <span>字典配置</span>
          </el-menu-item>
          <el-menu-item index="/permissions">
            <el-icon><Lock /></el-icon>
            <span>权限管理</span>
          </el-menu-item>
          <el-menu-item index="/embedding">
            <el-icon><Monitor /></el-icon>
            <span>向量索引</span>
          </el-menu-item>
          <el-menu-item index="/ai-audit-logs">
            <el-icon><List /></el-icon>
            <span>AI审计日志</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
      <div class="sidebar-footer">
        <span class="user-info">{{ user?.realName || user?.username }}</span>
        <el-button text type="danger" @click="handleLogout">退出</el-button>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <h3>{{ $route.meta.title || '军工项目文档策划与编制一体机' }}</h3>
        <div class="header-right">
          <LocalityBadge />
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUser, removeToken, isAdmin } from '@/utils/auth'
import LocalityBadge from '@/components/LocalityBadge.vue'

const router = useRouter()
const route = useRoute()
const user = getUser()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/projects')) return '/projects'
  if (path.startsWith('/templates')) return '/templates'
  if (path.startsWith('/standards')) return '/standards'
  if (path.startsWith('/knowledge')) return '/knowledge'
  if (path.startsWith('/users')) return '/users'
  if (path.startsWith('/roles')) return '/roles'
  if (path.startsWith('/dicts')) return '/dicts'
  if (path.startsWith('/permissions')) return '/permissions'
  if (path.startsWith('/ai-training')) return '/ai-training'
  if (path.startsWith('/ai-audit-logs')) return '/ai-audit-logs'
  return '/projects'
})

function handleLogout() {
  removeToken()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.sidebar {
  background-color: #304156;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.logo h2 { font-size: 16px; }
.sidebar-footer {
  margin-top: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  color: #bfcbd9;
  border-top: 1px solid rgba(255,255,255,0.1);
}
.header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  padding: 0 24px;
}
.header h3 { font-size: 16px; font-weight: 500; }
.header-right { margin-left: auto; display: flex; align-items: center; gap: 12px; }
.main {
  --el-main-padding: 0;
  overflow: hidden;
  box-sizing: border-box; 
  height: calc(100vh - 64px);
  background: #f0f2f5;
  padding: 0;
}
</style>
