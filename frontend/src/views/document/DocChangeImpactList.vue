<template>
  <div class="page">
    <div class="page-header">
      <h3>变更影响分析管理</h3>
      <div>
        <el-input v-model="filterChangeEventId" placeholder="变更事件ID筛选" style="width:150px;margin-right:12px" clearable @change="fetch" />
        <el-button type="primary" @click="showCreateDialog">创建影响分析</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="changeEventId" label="变更事件" width="100" />
      <el-table-column prop="impactedDocFileId" label="受影响文档" width="120" />
      <el-table-column prop="impactReason" label="影响原因" min-width="200" show-overflow-tooltip />
      <el-table-column prop="suggestAction" label="建议措施" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'CLOSED' ? 'info' : 'warning'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑影响分析' : '创建影响分析'" width="520px">
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="变更事件ID"><el-input-number v-model="form.changeEventId" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="受影响文档ID"><el-input-number v-model="form.impactedDocFileId" :min="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="影响原因"><el-input v-model="form.impactReason" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="建议措施"><el-input v-model="form.suggestAction" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" style="width:100%">
          <el-option label="待处理" value="PENDING" /><el-option label="处理中" value="IN_PROGRESS" /><el-option label="已关闭" value="CLOSED" />
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
import { getDocChangeImpacts, createDocChangeImpact, updateDocChangeImpact, deleteDocChangeImpact, type DocChangeImpactItem } from '@/api/doc-change-impact'

const loading = ref(false); const saving = ref(false)
const items = ref<DocChangeImpactItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const filterChangeEventId = ref<number>()

const empty = (): DocChangeImpactItem => ({ changeEventId: 0, impactedDocFileId: 0, impactReason: '', suggestAction: '', responsibleUserId: 0, status: 'PENDING', closedAt: '' })
const form = reactive<DocChangeImpactItem>(empty())

async function fetch() {
  loading.value = true
  try { const res = await getDocChangeImpacts(filterChangeEventId.value); items.value = res.data.data.records || res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: DocChangeImpactItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateDocChangeImpact(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createDocChangeImpact({ ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: DocChangeImpactItem) {
  await ElMessageBox.confirm('确定删除吗？', '确认', { type: 'warning' })
  try { await deleteDocChangeImpact(row.id!); ElMessage.success('已删除'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
