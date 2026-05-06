<template>
  <div class="page">
    <div class="page-header">
      <h3>文档生效基线管理</h3>
      <div>
        <el-input v-model="filterProjectId" placeholder="项目ID筛选" style="width:150px;margin-right:12px" clearable @change="fetch" />
        <el-button type="primary" @click="showCreateDialog">创建基线</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="projectId" label="项目ID" width="100" />
      <el-table-column prop="docFileId" label="文档ID" width="100" />
      <el-table-column prop="effectiveVersionId" label="生效版本" width="100" />
      <el-table-column prop="finalVersionId" label="最终版本" width="100" />
      <el-table-column prop="baselineStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.baselineStatus === 'CONFIRMED' ? 'success' : 'info'" size="small">{{ row.baselineStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="confirmedBy" label="确认人" width="100" />
      <el-table-column prop="confirmedAt" label="确认时间" width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑基线' : '创建基线'" width="480px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="项目ID"><el-input-number v-model="form.projectId" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="阶段ID"><el-input-number v-model="form.stageId" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="文档ID"><el-input-number v-model="form.docFileId" :min="1" style="width:100%" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="生效版本"><el-input-number v-model="form.effectiveVersionId" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="最终版本"><el-input-number v-model="form.finalVersionId" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态"><el-select v-model="form.baselineStatus" style="width:100%">
          <el-option label="草稿" value="DRAFT" /><el-option label="已确认" value="CONFIRMED" /><el-option label="已作废" value="INACTIVE" />
        </el-select></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDocEffectiveBaselines, createDocEffectiveBaseline, updateDocEffectiveBaseline, deleteDocEffectiveBaseline, type DocEffectiveBaselineItem } from '@/api/doc-effective-baseline'

const loading = ref(false); const saving = ref(false)
const items = ref<DocEffectiveBaselineItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const filterProjectId = ref<number>()

const empty = (): DocEffectiveBaselineItem => ({ projectId: 0, stageId: 0, docFileId: 0, effectiveVersionId: 0, finalVersionId: 0, confirmedBy: 0, confirmedAt: '', baselineStatus: 'DRAFT' })
const form = reactive<DocEffectiveBaselineItem>(empty())

async function fetch() {
  loading.value = true
  try { const res = await getDocEffectiveBaselines(filterProjectId.value); items.value = res.data.data.records || res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: DocEffectiveBaselineItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateDocEffectiveBaseline(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createDocEffectiveBaseline({ ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: DocEffectiveBaselineItem) {
  await ElMessageBox.confirm('确定删除此基线吗？', '确认', { type: 'warning' })
  try { await deleteDocEffectiveBaseline(row.id!); ElMessage.success('已删除'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
