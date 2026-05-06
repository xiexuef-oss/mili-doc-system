<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回项目详情</el-button>
        <h3 style="display:inline;margin-left:16px">项目阶段管理</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加阶段</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="stageCode" label="阶段编码" width="120" />
      <el-table-column prop="stageName" label="阶段名称" min-width="180" />
      <el-table-column prop="stageOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始" width="110" /><el-table-column prop="endDate" label="结束" width="110" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑阶段' : '添加阶段'" width="520px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="阶段编码"><el-input v-model="form.stageCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="阶段名称"><el-input v-model="form.stageName" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.stageOrder" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态"><el-select v-model="form.status" style="width:100%">
            <el-option label="未开始" value="NOT_STARTED" /><el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" /><el-option label="已暂停" value="PAUSED" />
          </el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="阶段目标"><el-input v-model="form.stageGoal" type="textarea" /></el-form-item>
        <el-form-item label="准入条件"><el-input v-model="form.entryCriteria" type="textarea" /></el-form-item>
        <el-form-item label="准出条件"><el-input v-model="form.exitCriteria" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getProjectStages, createProjectStage, updateProjectStage, type ProjectStageItem } from '@/api/project-stage'

const route = useRoute()
const projectId = Number(route.params.projectId)

const loading = ref(false); const saving = ref(false)
const items = ref<ProjectStageItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): ProjectStageItem => ({ projectId, stageCode: '', stageName: '', stageOrder: 0, status: 'NOT_STARTED', startDate: '', endDate: '', stageGoal: '', entryCriteria: '', exitCriteria: '' })
const form = reactive<ProjectStageItem>(empty())

const statusType = (s: string) => ({ NOT_STARTED: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', PAUSED: 'danger' }[s] || 'info')

async function fetch() {
  loading.value = true
  try { const res = await getProjectStages(projectId); items.value = res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: ProjectStageItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateProjectStage(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createProjectStage({ ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: ProjectStageItem) {
  await ElMessageBox.confirm('确定删除此阶段吗？', '确认', { type: 'warning' })
  try { await updateProjectStage(row.id!, { ...row, status: 'INACTIVE' } as any); ElMessage.success('已作废'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
