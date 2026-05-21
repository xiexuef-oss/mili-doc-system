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
      <el-tab-pane label="评审管理" name="reviews" />
      <el-tab-pane label="项目成员" name="members" />
    </el-tabs>

    <div class="workspace-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getProject, type ProjectItem } from '@/api/project'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.projectId))

const project = ref<ProjectItem | null>(null)

const activeTab = ref('overview')

const statusTagType = computed(() => {
  const map: Record<string, string> = {
    DRAFT: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', ARCHIVED: ''
  }
  return map[project.value?.status || ''] || 'info'
})

const statusLabel = computed(() => {
  const map: Record<string, string> = {
    DRAFT: '草稿', IN_PROGRESS: '进行中', COMPLETED: '已完成', ARCHIVED: '已归档'
  }
  return map[project.value?.status || ''] || project.value?.status || ''
})

function handleTabChange(name: string) {
  const routeName = name === 'overview' ? 'ProjectOverview' :
    name === 'stages' ? 'ProjectStages' :
    name === 'doc-ledger' ? 'ProjectDocLedger' :
    name === 'reviews' ? 'ProjectReviews' :
    name === 'members' ? 'ProjectMembers' :
    'ProjectInputFiles'
  router.replace({ name: routeName, params: { projectId: projectId.value } })
}

watch(() => route.name, (name) => {
  const map: Record<string, string> = {
    ProjectOverview: 'overview',
    ProjectStages: 'stages',
    ProjectDocLedger: 'doc-ledger',
    ProjectReviews: 'reviews',
    ProjectMembers: 'members',
    ProjectInputFiles: 'input-files'
  }
  if (name && map[String(name)]) {
    activeTab.value = map[String(name)]
  }
}, { immediate: true })

async function loadProject() {
  try {
    const res = await getProject(projectId.value)
    project.value = res.data.data
  } catch { /* ignore */ }
}

loadProject()
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
}
</style>
