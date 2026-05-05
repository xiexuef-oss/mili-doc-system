<template>
  <div class="page">
    <el-page-header @back="$router.push('/projects')">
      <template #content>
        <span>{{ project?.projectName || '项目详情' }}</span>
      </template>
    </el-page-header>

    <el-descriptions v-if="project" :column="2" border class="info-card">
      <el-descriptions-item label="项目编号">{{ project.projectCode }}</el-descriptions-item>
      <el-descriptions-item label="项目类型">{{ project.projectType }}</el-descriptions-item>
      <el-descriptions-item label="项目名称">{{ project.projectName }}</el-descriptions-item>
      <el-descriptions-item label="密级">
        <el-tag :type="project.securityLevel === 'TOP_SECRET' ? 'danger' : 'warning'" size="small">
          {{ project.securityLevel }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag size="small">{{ project.status }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="负责人">{{ project.ownerUserId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="开始日期">{{ project.startDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="结束日期">{{ project.endDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="适用标准" :span="2">{{ project.applicableStandards || '-' }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ project.description || '-' }}</el-descriptions-item>
    </el-descriptions>

    <div v-if="!project" style="text-align:center;padding:80px 0;color:#999">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProject, type ProjectItem } from '@/api/project'

const route = useRoute()
const project = ref<ProjectItem | null>(null)

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await getProject(id)
    project.value = res.data.data
  } catch {
    // error handled
  }
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.info-card { margin-top: 24px; }
</style>
