<template>
  <el-container class="layout">
    <!-- Sidebar -->
    <el-aside width="240px" class="sidebar">
      <!-- Logo -->
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 32 32" fill="none" width="28" height="28">
            <rect width="32" height="32" rx="6" fill="rgba(255,255,255,0.15)"/>
            <path d="M8 22V10l6 8 4-6 6 10" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-title">军用文档系统</span>
          <span class="brand-sub">MILI-DOC v2.0</span>
        </div>
      </div>

      <!-- Main Navigation -->
      <div class="sidebar-nav">
        <div class="nav-section-label">业务导航</div>
        <el-menu
          :default-active="activeMenu"
          router
          background-color="transparent"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#ffffff"
          class="sidebar-menu"
        >
          <el-menu-item index="/projects">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Folder /></el-icon>
                <span>项目管理</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/templates">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Files /></el-icon>
                <span>模版库</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/standards">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Collection /></el-icon>
                <span>标准库</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/knowledge">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Reading /></el-icon>
                <span>知识库</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/ai-training">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Cpu /></el-icon>
                <span>AI 模型训练</span>
              </div>
            </template>
          </el-menu-item>
        </el-menu>

        <div v-if="isAdmin()" class="nav-section-label">系统管理</div>
        <el-menu
          v-if="isAdmin()"
          :default-active="activeMenu"
          router
          background-color="transparent"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#ffffff"
          class="sidebar-menu"
        >
          <el-menu-item index="/users">
            <template #title>
              <div class="menu-item-content">
                <el-icon><User /></el-icon>
                <span>用户管理</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/roles">
            <template #title>
              <div class="menu-item-content">
                <el-icon><UserFilled /></el-icon>
                <span>角色管理</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/dicts">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Menu /></el-icon>
                <span>字典配置</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/permissions">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Lock /></el-icon>
                <span>权限管理</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/embedding">
            <template #title>
              <div class="menu-item-content">
                <el-icon><Monitor /></el-icon>
                <span>向量索引</span>
              </div>
            </template>
          </el-menu-item>
          <el-menu-item index="/ai-audit-logs">
            <template #title>
              <div class="menu-item-content">
                <el-icon><List /></el-icon>
                <span>AI 审计日志</span>
              </div>
            </template>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- User Footer -->
      <div class="sidebar-footer">
        <div class="user-avatar">
          {{ (user?.realName || user?.username || '?').charAt(0).toUpperCase() }}
        </div>
        <div class="user-info">
          <span class="user-name">{{ user?.realName || user?.username }}</span>
          <span class="user-role">{{ user?.roles?.[0] || '用户' }}</span>
        </div>
        <el-button class="logout-btn" text @click="handleLogout" title="退出登录">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </el-aside>

    <!-- Main Content -->
    <el-container class="main-container">
      <!-- Header -->
      <el-header class="header">
        <div class="header-left">
          <div class="header-breadcrumb">
            <el-icon class="breadcrumb-icon"><Position /></el-icon>
            <span class="breadcrumb-text">{{ $route.meta.title || '项目管理' }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-badge">
            <LocalityBadge />
          </div>
        </div>
      </el-header>

      <!-- Content -->
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
/* ---- Layout Structure ---- */
.layout {
  width: 100vw;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  position: fixed;
  top: 0;
  left: 0;
}

/* ---- Sidebar ---- */
.sidebar {
  width: 240px !important;
  min-width: 240px;
  max-width: 240px;
  background: linear-gradient(180deg, #0F2038 0%, #152238 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid rgba(255,255,255,0.06);
}

/* Brand Area */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 18px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}

.brand-icon {
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.brand-title {
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-sub {
  color: rgba(255,255,255,0.4);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 1px;
  text-transform: uppercase;
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
}

.nav-section-label {
  padding: 16px 24px 6px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(255,255,255,0.3);
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.sidebar-menu {
  border-right: none !important;
  padding: 0 8px;
}

.sidebar-menu .el-menu-item {
  height: 40px;
  line-height: 40px;
  margin: 2px 0;
  border-radius: 8px;
  padding: 0 16px !important;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  color: rgba(255,255,255,0.65) !important;
}

.sidebar-menu .el-menu-item:hover {
  background: rgba(255,255,255,0.08) !important;
  color: rgba(255,255,255,0.9) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background: rgba(44,95,158,0.3) !important;
  color: #ffffff !important;
  box-shadow: inset 3px 0 0 0 #3B82F6;
  font-weight: 600;
}

.menu-item-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.menu-item-content .el-icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* User Footer */
.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2C5F9E, #3B82F6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.user-name {
  color: rgba(255,255,255,0.9);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  color: rgba(255,255,255,0.4);
  font-size: 11px;
}

.logout-btn {
  color: rgba(255,255,255,0.35) !important;
  padding: 6px;
  flex-shrink: 0;
}

.logout-btn:hover {
  color: #EF4444 !important;
  background: rgba(239,68,68,0.1) !important;
  border-radius: 6px;
}

/* ---- Main Container ---- */
.main-container {
  flex-direction: column;
  overflow: hidden;
  height: 100%;
}

/* ---- Header ---- */
.header {
  height: 56px !important;
  background: #ffffff;
  border-bottom: 1px solid var(--md-gray-200);
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-icon {
  color: var(--md-gray-400);
  font-size: 16px;
}

.breadcrumb-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--md-gray-800);
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-badge {
  display: flex;
  align-items: center;
}

/* ---- Main Content ---- */
.main {
  --el-main-padding: 0;
  flex: 1;
  overflow: hidden;
  background: var(--md-gray-50);
  padding: 0;
}
</style>
