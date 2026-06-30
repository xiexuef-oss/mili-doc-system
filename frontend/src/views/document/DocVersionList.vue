<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
        <h3 style="display:inline;margin-left:16px">文档版本管理</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建版本</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="versionNo" label="版本号" width="120" />
      <el-table-column prop="sourceType" label="来源类型" width="110" />
      <el-table-column prop="versionStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.versionStatus === 'PUBLISHED' ? 'success' : row.versionStatus === 'DRAFT' ? 'info' : 'warning'" size="small">{{ row.versionStatus }}</el-tag>
        
  <DiffViewer v-model="diffVisible" :ledger-id="diffLedgerId" />
</template>
      </el-table-column>
      <el-table-column prop="changeSummary" label="变更说明" min-width="220" show-overflow-tooltip />
      <el-table-column prop="submitTime" label="提交时间" width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑版本' : '创建版本'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="版本号"><el-input v-model="form.versionNo" /></el-form-item>
        <el-form-item label="来源类型"><el-select v-model="form.sourceType" style="width:100%">
          <el-option label="新建" value="NEW" /><el-option label="修改" value="MODIFY" /><el-option label="转换" value="CONVERT" />
        </el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.versionStatus" style="width:100%">
          <el-option label="草稿" value="DRAFT" /><el-option label="已提交" value="SUBMITTED" /><el-option label="已发布" value="PUBLISHED" />
        </el-select></el-form-item>
        <el-form-item label="变更说明"><el-input v-model="form.changeSummary" type="textarea" :rows="3" /></el-form-item>
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
import DiffViewer from '@/components/DiffViewer.vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getDocVersions, createDocVersion, updateDocVersion, type DocVersionItem } from '@/api/doc-version'

const route = useRoute()
const diffVisible = ref(false)
const diffLedgerId = ref(0)
const docFileId = Number(route.params.docFileId)

const loading = ref(false); const saving = ref(false)
const items = ref<DocVersionItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): DocVersionItem => ({ docFileId, versionNo: '', sourceType: 'MODIFY', baseVersionId: 0, fileObjectId: 0, versionStatus: 'DRAFT', optimisticVersion: 1, submitUserId: 0, submitTime: '', changeSummary: '' })
const form = reactive<DocVersionItem>(empty())

async function loadItems() {
  loading.value = true
  try { const res = await getDocVersions(docFileId); items.value = res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: DocVersionItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateDocVersion(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createDocVersion({ ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; loadItems()
  } finally { saving.value = false }
}
async function handleDelete(row: DocVersionItem) {
  await ElMessageBox.confirm('确定删除此版本吗？', '确认', { type: 'warning' })
  try { await updateDocVersion(row.id!, { ...row, versionStatus: 'DELETED' }); ElMessage.success('已标记删除'); loadItems() } catch { /* cancelled */ }
}
onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
