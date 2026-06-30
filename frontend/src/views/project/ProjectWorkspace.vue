<template>
  <div class="workspace">
    <div class="workspace-header">
      <div class="header-left">
        <el-button link @click="$router.push('/projects')">
          <el-icon><ArrowLeft /></el-icon>返回项目列表
        </el-button>
        <h3 v-if="project">{{ project.projectName }}</h3>
      </div>
      <div class="header-right">
        <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="workspace-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="概览" name="overview" />
      <el-tab-pane label="输入文件" name="input-files" />
      <el-tab-pane label="阶段管理" name="stages" />
      <el-tab-pane label="文档台账" name="doc-ledger" />
      <el-tab-pane label="AI对话" name="ai-chat" />
      <el-tab-pane label="主数据" name="master-data" />
      <el-tab-pane label="评审管理" name="reviews" />
      <el-tab-pane label="完整性检查" name="completeness" />
      <el-tab-pane label="可靠性设计" name="reliability" />
      <el-tab-pane label="项目成员" name="members" />
    </el-tabs>

    <div class="workspace-content">
      <router-view v-slot="{ Component }">
        <keep-alive :max="8">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'
import { projectStatusLabel, projectStatusTagType } from '@/utils/labels'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.projectId))
const store = useProjectStore()

const project = computed(() => store.project)
const activeTab = ref('overview')

onMounted(async () => {
  await store.loadProject(projectId.value)
  store.loadStages()
})

const statusTagType = computed(() => projectStatusTagType(project.value?.status))
const statusLabel = computed(() => projectStatusLabel(project.value?.status))

function handleTabChange(name: string) {
  const t0 = performance.now()
  console.log('[TabSwitch] start:', name)
  const routeName = name === 'overview' ? 'ProjectOverview' :
    name === 'stages' ? 'ProjectStages' :
    name === 'doc-ledger' ? 'ProjectDocLedger' :
    name === 'ai-chat' ? 'ChatPage' :
    name === 'master-data' ? 'ProjectMasterData' :
    name === 'reviews' ? 'ProjectReviews' :
    name === 'completeness' ? 'CompletenessCheck' :
    name === 'reliability' ? 'ReliabilityWorkbench' :
    name === 'members' ? 'ProjectMembers' :
    'ProjectInputFiles'
  router.replace({ name: routeName, params: { projectId: projectId.value } })
}

watch(() => route.name, (name) => {
  const map: Record<string, string> = {
    ProjectOverview: 'overview',
    ProjectStages: 'stages',
    ProjectDocLedger: 'doc-ledger',
    ChatPage: 'ai-chat',
    ProjectMasterData: 'master-data',
    ProjectReviews: 'reviews',
    CompletenessCheck: 'completeness',
    ReliabilityWorkbench: 'reliability',
    ProjectMembers: 'members',
    ProjectInputFiles: 'input-files'
  }
  if (name && map[String(name)]) {
    activeTab.value = map[String(name)]
  }
}, { immediate: true })
</script>

<style scoped>
.workspace {
  background: #fff;
  border-radius: 4px;
  min-height: calc(100vh - 140px);
}
.workspace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px 0;
  border-bottom: 1px solid #ebeef5;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-left h3 {
  margin: 0;
  font-size: 16px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.workspace-tabs {
  padding: 0 24px;
}
.workspace-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.workspace-content {
  padding: 0;
  height: calc(100vh - 220px);
  overflow: hidden;
}
</style>
