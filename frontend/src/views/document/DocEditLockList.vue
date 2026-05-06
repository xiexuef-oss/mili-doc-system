<template>
  <div class="page">
    <div class="page-header">
      <h3>文档编辑锁定管理</h3>
      <div>
        <el-input v-model="filterDocFileId" placeholder="文档ID筛选" style="width:150px;margin-right:12px" clearable @change="fetch" />
        <el-button type="primary" @click="showCreateDialog">创建锁定</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="docFileId" label="文档ID" width="100" />
      <el-table-column prop="lockedBy" label="锁定用户" width="100" />
      <el-table-column prop="lockType" label="类型" width="90" />
      <el-table-column prop="lockStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.lockStatus === 'ACTIVE' ? 'danger' : 'info'" size="small">{{ row.lockStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lockedVersionId" label="锁定版本" width="100" />
      <el-table-column prop="expireAt" label="过期时间" width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">解锁</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑锁定' : '创建锁定'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="文档ID"><el-input-number v-model="form.docFileId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="锁定用户"><el-input-number v-model="form.lockedBy" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.lockType" style="width:100%">
          <el-option label="编辑" value="EDIT" /><el-option label="审批" value="REVIEW" />
        </el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.lockStatus" style="width:100%">
          <el-option label="激活" value="ACTIVE" /><el-option label="已释放" value="RELEASED" /><el-option label="过期" value="EXPIRED" />
        </el-select></el-form-item>
        <el-form-item label="过期时间"><el-date-picker v-model="form.expireAt" type="datetime" style="width:100%" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
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
import { getDocEditLocks, createDocEditLock, updateDocEditLock, deleteDocEditLock, type DocEditLockItem } from '@/api/doc-edit-lock'

const loading = ref(false); const saving = ref(false)
const items = ref<DocEditLockItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const filterDocFileId = ref<number>()

const empty = (): DocEditLockItem => ({ docFileId: 0, lockedVersionId: 0, lockedBy: 0, lockType: 'EDIT', lockStatus: 'ACTIVE', expireAt: '' })
const form = reactive<DocEditLockItem>(empty())

async function fetch() {
  loading.value = true
  try { const res = await getDocEditLocks(filterDocFileId.value); items.value = res.data.data.records || res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: DocEditLockItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateDocEditLock(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createDocEditLock({ ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: DocEditLockItem) {
  await ElMessageBox.confirm('确定解锁吗？', '确认', { type: 'warning' })
  try { await deleteDocEditLock(row.id!); ElMessage.success('已解锁'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
