<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回会议详情</el-button>
        <h3 style="display:inline;margin-left:16px">评审文档管理</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加评审文档</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="docFileId" label="文档ID" width="100" />
      <el-table-column prop="docVersionId" label="文档版本" width="100" />
      <el-table-column prop="reviewResult" label="评审结果" min-width="200" show-overflow-tooltip />
      <el-table-column label="材料完整" width="100">
        <template #default="{ row }">
          <el-tag :type="row.materialCompleteFlag ? 'success' : 'info'" size="small">{{ row.materialCompleteFlag ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="已关闭" width="80">
        <template #default="{ row }">
          <el-tag :type="row.closedFlag ? 'success' : 'warning'" size="small">{{ row.closedFlag ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑评审文档' : '添加评审文档'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="文档ID"><el-input-number v-model="form.docFileId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="文档版本"><el-input-number v-model="form.docVersionId" style="width:100%" /></el-form-item>
        <el-form-item label="评审结果"><el-input v-model="form.reviewResult" type="textarea" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="材料完整"><el-switch v-model="form.materialCompleteFlag" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="已关闭"><el-switch v-model="form.closedFlag" /></el-form-item></el-col>
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getReviewMeetingDocuments, createReviewMeetingDocument, updateReviewMeetingDocument, deleteReviewMeetingDocument, type ReviewMeetingDocumentItem } from '@/api/review-meeting-document'

const route = useRoute()
const meetingId = Number(route.params.meetingId)

const loading = ref(false); const saving = ref(false)
const items = ref<ReviewMeetingDocumentItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): ReviewMeetingDocumentItem => ({ meetingId, docFileId: 0, docVersionId: 0, reviewResult: '', materialCompleteFlag: false, closedFlag: false })
const form = reactive<ReviewMeetingDocumentItem>(empty())

async function loadItems() {
  loading.value = true
  try { const res = await getReviewMeetingDocuments(meetingId); items.value = res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: ReviewMeetingDocumentItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateReviewMeetingDocument(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createReviewMeetingDocument({ ...form }); ElMessage.success('添加成功') }
    dialogVisible.value = false; loadItems()
  } finally { saving.value = false }
}
async function handleDelete(row: ReviewMeetingDocumentItem) {
  await ElMessageBox.confirm('确定移除此文档吗？', '确认', { type: 'warning' })
  try { await deleteReviewMeetingDocument(row.id!); ElMessage.success('已移除'); loadItems() } catch { /* cancelled */ }
}
onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
