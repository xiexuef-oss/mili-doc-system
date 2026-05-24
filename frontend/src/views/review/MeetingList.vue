<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-select v-if="!workspaceProjectId" v-model="projectId" placeholder="选择项目" style="width: 200px" clearable @change="fetchData">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="会议状态" style="width: 140px" clearable @change="fetchData">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已安排" value="SCHEDULED" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建会议</el-button>
    </div>

    <el-table :data="meetings" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="meetingCode" label="会议编号" width="140" />
      <el-table-column prop="meetingName" label="会议名称" min-width="200" />
      <el-table-column prop="meetingType" label="会议类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ meetingTypeLabel(row.meetingType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="meetingDate" label="会议日期" width="120" />
      <el-table-column prop="meetingLocation" label="地点" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/review-meetings/${row.id}`)">详情</el-button>
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="handleStatus(row, 'SCHEDULED')">排期</el-button>
          <el-button v-if="row.status === 'SCHEDULED'" link type="success" @click="handleStatus(row, 'IN_PROGRESS')">开始</el-button>
          <el-button v-if="row.status === 'IN_PROGRESS'" link type="success" @click="handleStatus(row, 'COMPLETED')">完成</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑会议' : '创建会议'"
      width="640px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item v-if="!workspaceProjectId" label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会议编号" prop="meetingCode">
              <el-input v-model="form.meetingCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议类型" prop="meetingType">
              <el-select v-model="form.meetingType" style="width: 100%">
                <el-option label="设计评审" value="DESIGN_REVIEW" />
                <el-option label="技术评审" value="TECH_REVIEW" />
                <el-option label="质量评审" value="QUALITY_REVIEW" />
                <el-option label="阶段评审" value="STAGE_REVIEW" />
                <el-option label="最终评审" value="FINAL_REVIEW" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="会议名称" prop="meetingName">
          <el-input v-model="form.meetingName" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="会议日期" prop="meetingDate">
              <el-date-picker v-model="form.meetingDate" type="date" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="会议地点">
              <el-input v-model="form.meetingLocation" placeholder="如：XX会议室" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="主持人">
          <el-input v-model="form.hostUserId" placeholder="主持人用户ID" />
        </el-form-item>
        <el-form-item label="参会人员">
          <el-input v-model="form.attendeeUsers" placeholder="参会人员列表（逗号分隔的用户ID）" />
        </el-form-item>
        <el-form-item label="专家组">
          <el-input v-model="form.expertGroup" placeholder="专家组名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewMeetings, createReviewMeeting, updateReviewMeeting, updateMeetingStatus, deleteReviewMeeting, type ReviewMeetingItem } from '@/api/review-meeting'
import { getProjects, type ProjectItem } from '@/api/project'

const route = useRoute()
const router = useRouter()
const workspaceProjectId = computed(() => {
  const pid = route.params.projectId
  return pid ? Number(pid) : undefined
})

const loading = ref(false)
const saving = ref(false)
const meetings = ref<ReviewMeetingItem[]>([])
const projects = ref<ProjectItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const projectId = ref<number | undefined>(workspaceProjectId.value)
const statusFilter = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): ReviewMeetingItem => ({
  projectId: 0,
  meetingCode: '',
  meetingName: '',
  meetingType: '',
  meetingDate: '',
  meetingLocation: '',
  status: 'DRAFT'
})

const form = reactive<ReviewMeetingItem>(emptyForm())
const formRules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  meetingName: [{ required: true, message: '请输入会议名称', trigger: 'blur' }]
}

function meetingTypeLabel(type: string) {
  const map: Record<string, string> = {
    DESIGN_REVIEW: '设计评审', TECH_REVIEW: '技术评审', QUALITY_REVIEW: '质量评审',
    STAGE_REVIEW: '阶段评审', FINAL_REVIEW: '最终评审'
  }
  return map[type] || type
}

function statusTag(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', SCHEDULED: 'primary', IN_PROGRESS: 'warning', COMPLETED: 'success', CANCELLED: ''
  }
  return map[status] || 'info'
}

async function fetchProjects() {
  try {
    const res = await getProjects({ pageNo: 1, pageSize: 100 })
    projects.value = res.data.data.records
  } catch { /* handled */ }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getReviewMeetings({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      projectId: projectId.value,
      status: statusFilter.value || undefined
    })
    const data = res.data.data
    meetings.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  editingId.value = null
  Object.assign(form, { ...emptyForm(), projectId: workspaceProjectId.value || 0 })
  dialogVisible.value = true
}

function showEditDialog(row: ReviewMeetingItem) {
  editingId.value = row.id!
  Object.assign(form, row)
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateReviewMeeting(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createReviewMeeting({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleStatus(row: ReviewMeetingItem, status: string) {
  try {
    await updateMeetingStatus(row.id!, status)
    ElMessage.success('状态更新成功')
    fetchData()
  } catch { /* handled */ }
}

async function handleDelete(row: ReviewMeetingItem) {
  await ElMessageBox.confirm(`确定要删除会议「${row.meetingName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteReviewMeeting(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled or error */ }
}

onMounted(() => {
  if (!workspaceProjectId.value) fetchProjects()
  fetchData()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
