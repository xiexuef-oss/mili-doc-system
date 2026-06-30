<template>
  <div class="page">
    <div class="back-bar">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>返回会议列表
      </el-button>
    </div>

    <!-- Meeting Info Card -->
    <el-card v-loading="loading" class="info-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>{{ meeting?.meetingName }}</h2>
            <div class="meta">
              <el-tag size="small">{{ meetingTypeLabel(meeting?.meetingType) }}</el-tag>
              <el-tag :type="statusTag(meeting?.status)" size="small">{{ meeting?.status }}</el-tag>
              <span v-if="meeting?.meetingCode">编号: {{ meeting?.meetingCode }}</span>
            </div>
          </div>
          <div class="actions">
            <el-button
              v-if="meeting?.status === 'DRAFT'" type="success" size="small"
              @click="handleStatus('SCHEDULED')"
            >排期</el-button>
            <el-button
              v-if="meeting?.status === 'SCHEDULED'" type="warning" size="small"
              @click="handleStatus('IN_PROGRESS')"
            >开始会议</el-button>
            <el-button
              v-if="meeting?.status === 'IN_PROGRESS'" type="primary" size="small"
              @click="handleStatus('COMPLETED')"
            >完成会议</el-button>
            <el-button
              type="primary" size="small"
              :loading="aiLoading"
              @click="handleAiSummary"
              :disabled="meeting?.status !== 'IN_PROGRESS' && meeting?.status !== 'COMPLETED'"
            >
              <el-icon><MagicStick /></el-icon>
              AI 汇总意见
            </el-button>
            <el-button size="small" @click="showEditDialog">编辑</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="会议日期">{{ meeting?.meetingDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="会议地点">{{ meeting?.meetingLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主持人">{{ meeting?.hostUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参会人员">{{ meeting?.attendeeUsers || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专家组">{{ meeting?.expertGroup || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ meeting?.createdAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Tabs: Documents / Expert Opinions / AI Summary -->
    <el-card class="tabs-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="评审文档" name="docs">
          <div style="margin-bottom: 12px; text-align: right">
            <el-button type="primary" size="small" @click="showDocDialog()">添加文档</el-button>
          </div>
          <el-table :data="documents" v-loading="docsLoading" size="small">
            <el-table-column prop="docFileId" label="文档ID" width="100" />
            <el-table-column prop="docVersionId" label="版本ID" width="100" />
            <el-table-column prop="reviewResult" label="评审结果" min-width="200" show-overflow-tooltip />
            <el-table-column label="材料完整" width="90">
              <template #default="{ row }">
                <el-tag :type="row.materialCompleteFlag ? 'success' : 'info'" size="small">
                  {{ row.materialCompleteFlag ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link size="small" @click="showDocDialog(row)">编辑</el-button>
                <el-button link size="small" type="danger" @click="handleDeleteDoc(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="专家意见" name="opinions">
          <div style="margin-bottom: 12px; text-align: right">
            <el-button type="primary" size="small" @click="showOpinionDialog()">添加意见</el-button>
          </div>
          <el-table :data="opinions" v-loading="opinionsLoading" size="small">
            <el-table-column prop="expertUserId" label="专家ID" width="100" />
            <el-table-column prop="expertGroupName" label="专家组" width="140" />
            <el-table-column prop="docFileId" label="关联文档" width="100" />
            <el-table-column label="问题等级" width="100">
              <template #default="{ row }">
                <el-tag :type="levelType(row.problemLevel)" size="small">{{ row.problemLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="uploadedAt" label="上传时间" width="160" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link size="small" @click="showOpinionDialog(row)">编辑</el-button>
                <el-button link size="small" type="danger" @click="handleDeleteOpinion(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="AI 意见汇总" name="summary">
          <div v-if="aiSummary" class="summary-content">
            <div class="summary-text">{{ aiSummary }}</div>
          </div>
          <el-empty v-else description="点击「AI 汇总意见」按钮生成会议意见汇总" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Edit Meeting Dialog -->
    <el-dialog v-model="editVisible" title="编辑会议" width="600px">
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="会议名称" prop="meetingName">
          <el-input v-model="editForm.meetingName" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会议编号"><el-input v-model="editForm.meetingCode" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议类型">
              <el-select v-model="editForm.meetingType" style="width:100%">
                <el-option label="设计评审" value="DESIGN_REVIEW" />
                <el-option label="技术评审" value="TECH_REVIEW" />
                <el-option label="质量评审" value="QUALITY_REVIEW" />
                <el-option label="阶段评审" value="STAGE_REVIEW" />
                <el-option label="最终评审" value="FINAL_REVIEW" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会议日期">
              <el-date-picker v-model="editForm.meetingDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议地点"><el-input v-model="editForm.meetingLocation" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="主持人"><el-input v-model="editForm.hostUserId" /></el-form-item>
        <el-form-item label="参会人员"><el-input v-model="editForm.attendeeUsers" /></el-form-item>
        <el-form-item label="专家组"><el-input v-model="editForm.expertGroup" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleEditSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Document Dialog -->
    <el-dialog v-model="docDialogVisible" :title="editingDocId ? '编辑文档' : '添加文档'" width="480px">
      <el-form :model="docForm" label-width="90px">
        <el-form-item label="文档ID"><el-input-number v-model="docForm.docFileId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="版本ID"><el-input-number v-model="docForm.docVersionId" style="width:100%" /></el-form-item>
        <el-form-item label="评审结果"><el-input v-model="docForm.reviewResult" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="材料完整"><el-switch v-model="docForm.materialCompleteFlag" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="已关闭"><el-switch v-model="docForm.closedFlag" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="docDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="docSaving" @click="handleDocSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Opinion Dialog -->
    <el-dialog v-model="opinionDialogVisible" :title="editingOpinionId ? '编辑意见' : '添加意见'" width="480px">
      <el-form :model="opinionForm" label-width="90px">
        <el-form-item label="专家ID"><el-input-number v-model="opinionForm.expertUserId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="专家组"><el-input v-model="opinionForm.expertGroupName" /></el-form-item>
        <el-form-item label="关联文档"><el-input-number v-model="opinionForm.docFileId" style="width:100%" /></el-form-item>
        <el-form-item label="问题等级">
          <el-select v-model="opinionForm.problemLevel" style="width:100%">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="重要" value="MAJOR" />
            <el-option label="一般" value="MINOR" />
            <el-option label="建议" value="SUGGESTION" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="opinionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="opinionSaving" @click="handleOpinionSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, MagicStick } from '@element-plus/icons-vue'
import { getReviewMeeting, updateReviewMeeting, updateMeetingStatus, type ReviewMeetingItem } from '@/api/review-meeting'
import { meetingTypeLabel } from '@/utils/labels'
import {
  getReviewMeetingDocuments, createReviewMeetingDocument,
  updateReviewMeetingDocument, deleteReviewMeetingDocument,
  type ReviewMeetingDocumentItem
} from '@/api/review-meeting-document'
import {
  getReviewExpertOpinions, createReviewExpertOpinion,
  updateReviewExpertOpinion, deleteReviewExpertOpinion,
  type ReviewExpertOpinionItem
} from '@/api/review-expert-opinion'
import { opinionSummary } from '@/api/ai'

const route = useRoute()
const meetingId = Number(route.params.id)

const loading = ref(false)
const meeting = ref<ReviewMeetingItem | null>(null)
const activeTab = ref('docs')

// Meeting status
function statusTag(s?: string) {
  const map: Record<string, string> = { DRAFT: 'info', SCHEDULED: 'primary', IN_PROGRESS: 'warning', COMPLETED: 'success', CANCELLED: '' }
  return s ? map[s] || 'info' : 'info'
}

async function fetchMeeting() {
  loading.value = true
  try {
    const res = await getReviewMeeting(meetingId)
    meeting.value = res.data.data
  } finally { loading.value = false }
}

async function handleStatus(status: string) {
  try {
    await updateMeetingStatus(meetingId, status)
    if (meeting.value) meeting.value.status = status
    ElMessage.success('状态更新成功')
  } catch { /* handled */ }
}

// Edit meeting
const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive<ReviewMeetingItem>({} as ReviewMeetingItem)

function showEditDialog() {
  Object.assign(editForm, meeting.value || {})
  editVisible.value = true
}
async function handleEditSave() {
  saving.value = true
  try {
    await updateReviewMeeting(meetingId, { ...editForm })
    ElMessage.success('更新成功')
    editVisible.value = false
    fetchMeeting()
  } finally { saving.value = false }
}

// Documents
const documents = ref<ReviewMeetingDocumentItem[]>([])
const docsLoading = ref(false)
const docDialogVisible = ref(false)
const editingDocId = ref<number | null>(null)
const docSaving = ref(false)
const docForm = reactive<ReviewMeetingDocumentItem>({ meetingId, docFileId: 0, docVersionId: 0, reviewResult: '', materialCompleteFlag: false, closedFlag: false })

async function fetchDocuments() {
  docsLoading.value = true
  try {
    const res = await getReviewMeetingDocuments(meetingId)
    documents.value = res.data.data
  } finally { docsLoading.value = false }
}
function showDocDialog(row?: ReviewMeetingDocumentItem) {
  if (row) {
    editingDocId.value = row.id!
    Object.assign(docForm, row)
  } else {
    editingDocId.value = null
    Object.assign(docForm, { meetingId, docFileId: 0, docVersionId: 0, reviewResult: '', materialCompleteFlag: false, closedFlag: false })
  }
  docDialogVisible.value = true
}
async function handleDocSave() {
  docSaving.value = true
  try {
    if (editingDocId.value) {
      await updateReviewMeetingDocument(editingDocId.value, { ...docForm })
      ElMessage.success('更新成功')
    } else {
      await createReviewMeetingDocument({ ...docForm })
      ElMessage.success('添加成功')
    }
    docDialogVisible.value = false
    fetchDocuments()
  } finally { docSaving.value = false }
}
async function handleDeleteDoc(row: ReviewMeetingDocumentItem) {
  await ElMessageBox.confirm('确定移除此文档吗？', '确认', { type: 'warning' })
  try { await deleteReviewMeetingDocument(row.id!); ElMessage.success('已移除'); fetchDocuments() } catch { /* */ }
}

// Opinions
const opinions = ref<ReviewExpertOpinionItem[]>([])
const opinionsLoading = ref(false)
const opinionDialogVisible = ref(false)
const editingOpinionId = ref<number | null>(null)
const opinionSaving = ref(false)
const opinionForm = reactive<ReviewExpertOpinionItem>({ meetingId, expertUserId: 0, expertGroupName: '', docFileId: 0, fileObjectId: 0, problemLevel: 'MINOR', uploadedAt: '' })

function levelType(l: string) {
  return { CRITICAL: 'danger', MAJOR: 'warning', MINOR: 'info', SUGGESTION: 'success' }[l] || 'info'
}
async function fetchOpinions() {
  opinionsLoading.value = true
  try {
    const res = await getReviewExpertOpinions(meetingId)
    opinions.value = res.data.data
  } finally { opinionsLoading.value = false }
}
function showOpinionDialog(row?: ReviewExpertOpinionItem) {
  if (row) {
    editingOpinionId.value = row.id!
    Object.assign(opinionForm, row)
  } else {
    editingOpinionId.value = null
    Object.assign(opinionForm, { meetingId, expertUserId: 0, expertGroupName: '', docFileId: 0, fileObjectId: 0, problemLevel: 'MINOR', uploadedAt: '' })
  }
  opinionDialogVisible.value = true
}
async function handleOpinionSave() {
  opinionSaving.value = true
  try {
    if (editingOpinionId.value) {
      await updateReviewExpertOpinion(editingOpinionId.value, { ...opinionForm })
      ElMessage.success('更新成功')
    } else {
      await createReviewExpertOpinion({ ...opinionForm })
      ElMessage.success('添加成功')
    }
    opinionDialogVisible.value = false
    fetchOpinions()
  } finally { opinionSaving.value = false }
}
async function handleDeleteOpinion(row: ReviewExpertOpinionItem) {
  await ElMessageBox.confirm('确定删除此意见吗？', '确认', { type: 'warning' })
  try { await deleteReviewExpertOpinion(row.id!); ElMessage.success('已删除'); fetchOpinions() } catch { /* */ }
}

// AI Summary
const aiLoading = ref(false)
const aiSummary = ref('')

async function handleAiSummary() {
  aiLoading.value = true
  try {
    const res = await opinionSummary(meetingId)
    aiSummary.value = res.data.data?.summary || res.data.data?.content || JSON.stringify(res.data.data)
    activeTab.value = 'summary'
    ElMessage.success('AI 汇总完成')
  } catch {
    ElMessage.error('AI 汇总失败')
  } finally {
    aiLoading.value = false
  }
}

onMounted(() => {
  fetchMeeting()
  fetchDocuments()
  fetchOpinions()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); max-width: 1400px; }
.back-bar { margin-bottom: 16px; }
.info-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; }
.card-header h2 { margin: 0 0 8px; font-size: 20px; }
.meta { display: flex; gap: 8px; align-items: center; color: #909399; font-size: 13px; }
.actions { display: flex; gap: 8px; }
.tabs-card { min-height: 400px; }
.summary-content { padding: 16px; background: #f5f7fa; border-radius: 8px; }
.summary-text { white-space: pre-wrap; line-height: 1.8; font-size: 14px; }
</style>
