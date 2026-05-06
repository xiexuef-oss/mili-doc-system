<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回会议详情</el-button>
        <h3 style="display:inline;margin-left:16px">专家意见管理</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加专家意见</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="expertUserId" label="专家用户" width="100" />
      <el-table-column prop="expertGroupName" label="专家组" width="140" />
      <el-table-column prop="docFileId" label="关联文档" width="100" />
      <el-table-column prop="problemLevel" label="问题等级" width="100">
        <template #default="{ row }">
          <el-tag :type="levelType(row.problemLevel)" size="small">{{ row.problemLevel }}</el-tag>
        </template>
      </el-table-column>
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
        <el-form-item label="专家用户"><el-input-number v-model="form.expertUserId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="专家组"><el-input v-model="form.expertGroupName" /></el-form-item>
        <el-form-item label="关联文档"><el-input-number v-model="form.docFileId" style="width:100%" /></el-form-item>
        <el-form-item label="问题等级"><el-select v-model="form.problemLevel" style="width:100%">
          <el-option label="严重" value="CRITICAL" /><el-option label="重要" value="MAJOR" /><el-option label="一般" value="MINOR" /><el-option label="建议" value="SUGGESTION" />
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
import { getReviewExpertOpinions, createReviewExpertOpinion, updateReviewExpertOpinion, deleteReviewExpertOpinion, type ReviewExpertOpinionItem } from '@/api/review-expert-opinion'

const route = useRoute()
const meetingId = Number(route.params.meetingId)

const loading = ref(false); const saving = ref(false)
const items = ref<ReviewExpertOpinionItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): ReviewExpertOpinionItem => ({ meetingId, expertUserId: 0, expertGroupName: '', docFileId: 0, fileObjectId: 0, problemLevel: 'MINOR', uploadedAt: '' })
const form = reactive<ReviewExpertOpinionItem>(empty())

const levelType = (l: string) => ({ CRITICAL: 'danger', MAJOR: 'warning', MINOR: 'info', SUGGESTION: 'success' }[l] || 'info')

async function fetch() {
  loading.value = true
  try { const res = await getReviewExpertOpinions(meetingId); items.value = res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: ReviewExpertOpinionItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateReviewExpertOpinion(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createReviewExpertOpinion({ ...form }); ElMessage.success('添加成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: ReviewExpertOpinionItem) {
  await ElMessageBox.confirm('确定删除此意见吗？', '确认', { type: 'warning' })
  try { await deleteReviewExpertOpinion(row.id!); ElMessage.success('已删除'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
