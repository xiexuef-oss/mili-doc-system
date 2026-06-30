<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回会议详情</el-button>
        <h3 style="display:inline;margin-left:16px">会议意见汇总</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加意见汇总</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="docFileId" label="关联文档" width="100" />
      <el-table-column prop="opinionType" label="意见类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.opinionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileObjectId" label="文件ID" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'FINAL' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploadedBy" label="上传人" width="100" />
      <el-table-column prop="uploadedAt" label="上传时间" width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑意见' : '添加意见'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="关联文档"><el-input-number v-model="form.docFileId" style="width:100%" /></el-form-item>
        <el-form-item label="意见类型"><el-select v-model="form.opinionType" style="width:100%">
          <el-option label="技术类" value="TECHNICAL" /><el-option label="格式类" value="FORMAT" />
          <el-option label="标准类" value="STANDARD" /><el-option label="其他" value="OTHER" />
        </el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" style="width:100%">
          <el-option label="草稿" value="DRAFT" /><el-option label="已提交" value="SUBMITTED" />
          <el-option label="已确认" value="CONFIRMED" /><el-option label="定稿" value="FINAL" />
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getReviewMeetingOpinions, createReviewMeetingOpinion, updateReviewMeetingOpinion, deleteReviewMeetingOpinion, type ReviewMeetingOpinionItem } from '@/api/review-meeting-opinion'

const route = useRoute()
const meetingId = Number(route.params.meetingId)

const loading = ref(false); const saving = ref(false)
const items = ref<ReviewMeetingOpinionItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): ReviewMeetingOpinionItem => ({ meetingId, docFileId: 0, opinionType: 'TECHNICAL', fileObjectId: 0, status: 'DRAFT', uploadedBy: 0, uploadedAt: '' })
const form = reactive<ReviewMeetingOpinionItem>(empty())

async function loadItems() {
  loading.value = true
  try { const res = await getReviewMeetingOpinions(meetingId); items.value = res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: ReviewMeetingOpinionItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateReviewMeetingOpinion(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createReviewMeetingOpinion({ ...form }); ElMessage.success('添加成功') }
    dialogVisible.value = false; loadItems()
  } finally { saving.value = false }
}
async function handleDelete(row: ReviewMeetingOpinionItem) {
  await ElMessageBox.confirm('确定删除吗？', '确认', { type: 'warning' })
  try { await deleteReviewMeetingOpinion(row.id!); ElMessage.success('已删除'); loadItems() } catch { /* cancelled */ }
}
onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
