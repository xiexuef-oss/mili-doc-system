<template>
  <div class="page">
    <el-page-header @back="$router.push('/projects')">
      <template #content>
        <span>{{ project?.projectName || '项目详情' }}</span>
      </template>
    </el-page-header>

    <el-descriptions v-if="project" :column="2" border class="info-card">
      <el-descriptions-item label="项目编号">{{ project.projectCode }}</el-descriptions-item>
      <el-descriptions-item label="项目类型">{{ typeLabel(project.projectType) }}</el-descriptions-item>
      <el-descriptions-item label="项目名称">{{ project.projectName }}</el-descriptions-item>
      <el-descriptions-item label="密级">
        <el-tag :type="project.securityLevel === 'TOP_SECRET' ? 'danger' : 'warning'" size="small">
          {{ securityLabel(project.securityLevel) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag size="small">{{ statusLabel(project.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="负责人">{{ project.ownerUserId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="初始阶段">
        <el-tag type="primary" size="small">{{ initialStageName || project?.initialStageCode || '-' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="当前阶段">
        <el-tag :type="currentStage?.isCurrent ? 'success' : 'info'" size="small">
          {{ currentStage?.stageName || '-' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="开始日期">{{ project.startDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="结束日期">{{ project.endDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="适用标准" :span="2">{{ project.applicableStandards || '-' }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ project.description || '-' }}</el-descriptions-item>
    </el-descriptions>

    <!-- Master Data Summary -->
    <el-card v-if="masterData" shadow="never" class="info-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>项目主数据概览</span>
          <el-button size="small" @click="handleConsistencyCheck" :icon="CircleCheck">一致性检查</el-button>
        <el-button size="small" type="primary" @click="$router.push({ name: 'ProjectMasterData', params: { projectId: $route.params.projectId } })">
            编辑主数据
          </el-button>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="8" v-if="masterData.equipmentInfo">
          <h4 class="md-section-title">装备信息</h4>
          <p v-if="masterData.equipmentInfo.equipmentName">名称: {{ masterData.equipmentInfo.equipmentName }}</p>
          <p v-if="masterData.equipmentInfo.equipmentType">类型: {{ masterData.equipmentInfo.equipmentType }}</p>
          <p v-if="masterData.equipmentInfo.model">型号: {{ masterData.equipmentInfo.model }}</p>
        </el-col>
        <el-col :span="8" v-if="masterData.tacticalIndicators">
          <h4 class="md-section-title">战术技术指标</h4>
          <p>{{ (masterData.tacticalIndicators || []).length }} 项指标</p>
        </el-col>
        <el-col :span="8" v-if="masterData.productTree">
          <h4 class="md-section-title">产品结构</h4>
          <p>{{ (masterData.productTree || []).length }} 个节点</p>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top:12px">
        <el-col :span="8" v-if="masterData.milestones">
          <h4 class="md-section-title">里程碑</h4>
          <p>{{ (masterData.milestones || []).length }} 个节点</p>
        </el-col>
        <el-col :span="8">
          <div v-if="!masterData.equipmentInfo && !masterData.tacticalIndicators" style="color:#999;font-size:13px;padding:12px 0">
            尚未填写主数据，点击"编辑主数据"开始填写
          </div>
        </el-col>
      </el-row>
    </el-card>

    <div v-if="!project" style="text-align:center;padding:80px 0;color:#999">加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProject, type ProjectItem } from '@/api/project'
import { getProjectStages, type ProjectStageItem } from '@/api/project-stage'
import { getStageDefinitions, type StageDefinitionItem } from '@/api/project-stage'
import { getMasterData } from '@/api/project-master-data'

const route = useRoute()
const project = ref<ProjectItem | null>(null)
const masterData = ref<any>(null)
const stages = ref<ProjectStageItem[]>([])
const stageDefs = ref<StageDefinitionItem[]>([])

// Resolve stage name from code using either loaded stages or stage definitions
function stageNameByCode(code: string): string | null {
  const fromStages = stages.value.find(s => s.stageCode === code)
  if (fromStages) return fromStages.stageName
  const fromDefs = stageDefs.value.find(d => d.code === code)
  if (fromDefs) return fromDefs.name
  return null
}

const initialStageName = computed(() => {
  if (!project.value?.initialStageCode) return null
  return stageNameByCode(project.value.initialStageCode) || project.value.initialStageCode
})

const currentStage = computed(() => {
  return stages.value.find(s => s.isCurrent) || null
})

function typeLabel(type: string) {
  const map: Record<string, string> = {
    MODEL: '型号项目', PRE_RESEARCH: '预研项目', TECH_IMPROVE: '技改项目'
  }
  return map[type] || type
}

function securityLabel(level: string) {
  const map: Record<string, string> = {
    PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密', TOP_SECRET: '绝密'
  }
  return map[level] || level
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿', IN_PROGRESS: '进行中', COMPLETED: '已完成', ARCHIVED: '已归档'
  }
  return map[status] || status
}

async function loadStages() {
  try {
    const [stagesRes, defsRes] = await Promise.all([
      getProjectStages(Number(route.params.projectId)),
      getStageDefinitions()
    ])
    stages.value = stagesRes.data.data || []
    stageDefs.value = defsRes.data.data || []
  } catch { /* ignore */ }
}

async function loadMasterData() {
  try {
    const res = await getMasterData(Number(route.params.projectId))
    masterData.value = res.data.data
  } catch { /* ignore */ }
}

onMounted(async () => {
  const id = Number(route.params.projectId)
  try {
    const res = await getProject(id)
    project.value = res.data.data
    await Promise.all([loadStages(), loadMasterData()])
  } catch {
    // error handled
  }
})

async function handleConsistencyCheck() {
  try {
    const res = await request.get(`/reliability/projects/${projectId.value}/consistency-check`)
    const data = res.data?.data
    if (data?.issues?.length) {
      const text = data.issues.map((i: any) => `${i.severity === 'error' ? '❌' : '⚠️'} ${i.description}`).join('\n')
      ElMessageBox.alert(text, `一致性检查 (${data.totalIssues}个问题)`, { confirmButtonText: '知道了' })
    } else {
      ElMessage.success('所有文档一致性检查通过')
    }
  } catch { ElMessage.error('检查失败') }
}
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.info-card { margin-top: 24px; }
</style>
