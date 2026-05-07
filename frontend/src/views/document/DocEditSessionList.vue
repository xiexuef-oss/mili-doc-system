<template>
  <div class="page">
    <div class="page-header">
      <h3>文档编辑会话管理</h3>
      <div>
        <el-input v-model="filterDocFileId" placeholder="文档ID筛选" style="width:150px;margin-right:12px" clearable @change="fetch" />
        <el-button type="primary" @click="showCreateDialog">创建会话</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="docFileId" label="文档ID" width="100" />
      <el-table-column prop="editorUserId" label="编辑用户" width="100" />
      <el-table-column prop="sessionStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.sessionStatus === 'OPEN' ? 'success' : 'info'" size="small">{{ row.sessionStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="baseVersionId" label="基准版本" width="100" />
      <el-table-column prop="draftVersionId" label="草稿版本" width="100" />
      <el-table-column prop="openedAt" label="打开时间" width="160" />
      <el-table-column prop="submittedAt" label="提交时间" width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑会话' : '创建会话'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="文档ID"><el-input-number v-model="form.docFileId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="编辑用户"><el-input-number v-model="form.editorUserId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.sessionStatus" style="width:100%">
          <el-option label="打开" value="OPEN" /><el-option label="已提交" value="SUBMITTED" /><el-option label="已关闭" value="CLOSED" />
        </el-select></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="基准版本"><el-input-number v-model="form.baseVersionId" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="草稿版本"><el-input-number v-model="form.draftVersionId" :min="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
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
import { getDocEditSessions, createDocEditSession, submitDocEditSession, closeDocEditSession, type DocEditSessionItem } from '@/api/doc-edit-session'

const loading = ref(false); const saving = ref(false)
const items = ref<DocEditSessionItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const filterDocFileId = ref<number>()

const empty = (): DocEditSessionItem => ({ docFileId: 0, baseVersionId: 0, draftVersionId: 0, editorUserId: 0, sessionStatus: 'OPEN', openedAt: '', submittedAt: '' })
const form = reactive<DocEditSessionItem>(empty())

async function fetch() {
  loading.value = true
  try { const res = await getDocEditSessions(filterDocFileId.value); items.value = res.data.data.records || res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: DocEditSessionItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await submitDocEditSession(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createDocEditSession({ ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: DocEditSessionItem) {
  await ElMessageBox.confirm('确定删除吗？', '确认', { type: 'warning' })
  try { await closeDocEditSession(row.id!); ElMessage.success('已关闭'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
