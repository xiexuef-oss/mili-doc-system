<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回项目详情</el-button>
        <h3 style="display:inline;margin-left:16px">阶段转阶段检查</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建检查</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="fromStageId" label="源阶段" width="100" />
      <el-table-column prop="toStageId" label="目标阶段" width="100" />
      <el-table-column prop="checkStatus" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.checkStatus)" size="small">{{ row.checkStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="blockerItems" label="阻塞项" min-width="200" show-overflow-tooltip />
      <el-table-column prop="checkResult" label="检查结果" min-width="200" show-overflow-tooltip />
      <el-table-column prop="checkedAt" label="检查时间" width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑检查' : '创建检查'" width="520px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="源阶段ID"><el-input-number v-model="form.fromStageId" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="目标阶段ID"><el-input-number v-model="form.toStageId" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态"><el-select v-model="form.checkStatus" style="width:100%">
          <el-option label="待检查" value="PENDING" /><el-option label="通过" value="PASSED" />
          <el-option label="不通过" value="FAILED" /><el-option label="豁免" value="WAIVED" />
        </el-select></el-form-item>
        <el-form-item label="阻塞项"><el-input v-model="form.blockerItems" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="检查结果"><el-input v-model="form.checkResult" type="textarea" :rows="3" /></el-form-item>
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
import { getStageTransitions, createStageTransition, updateStageTransition, deleteStageTransition, type StageTransitionItem } from '@/api/stage-transition'

const route = useRoute()
const projectId = Number(route.params.projectId)

const loading = ref(false); const saving = ref(false)
const items = ref<StageTransitionItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): StageTransitionItem => ({ projectId, fromStageId: 0, toStageId: 0, checkStatus: 'PENDING', blockerItems: '', checkResult: '', checkedBy: 0, checkedAt: '' })
const form = reactive<StageTransitionItem>(empty())

const statusType = (s: string) => ({ PENDING: 'warning', PASSED: 'success', FAILED: 'danger', WAIVED: 'info' }[s] || 'info')

async function fetch() {
  loading.value = true
  try { const res = await getStageTransitions(projectId); items.value = res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: StageTransitionItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateStageTransition(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createStageTransition({ ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: StageTransitionItem) {
  await ElMessageBox.confirm('确定删除此检查吗？', '确认', { type: 'warning' })
  try { await deleteStageTransition(row.id!); ElMessage.success('删除成功'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
