<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索项目名称" style="width: 240px" clearable @change="fetchData" />
        <el-select v-model="statusFilter" placeholder="项目状态" style="width: 140px" clearable @change="fetchData">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已归档" value="ARCHIVED" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建项目</el-button>
    </div>

    <el-table :data="projects" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="projectCode" label="项目编号" width="140" />
      <el-table-column prop="projectName" label="项目名称" min-width="180" />
      <el-table-column prop="projectType" label="类型" width="100">
        <template #default="{ row }">
          {{ typeLabel(row.projectType) }}
        </template>
      </el-table-column>
      <el-table-column prop="securityLevel" label="密级" width="80">
        <template #default="{ row }">
          <el-tag :type="row.securityLevel === 'TOP_SECRET' ? 'danger' : 'warning'" size="small">
            {{ securityLabel(row.securityLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/projects/${row.id}`)">查看</el-button>
          <el-button link type="warning" @click="showEditDialog(row)">编辑</el-button>
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

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑项目' : '创建项目'"
      width="640px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目编号" prop="projectCode">
              <el-input v-model="form.projectCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类型" prop="projectType">
              <el-select v-model="form.projectType" style="width: 100%">
                <el-option v-for="t in projectTypes" :key="t.dictCode" :label="t.dictName" :value="t.dictCode" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密级" prop="securityLevel">
              <el-select v-model="form.securityLevel" style="width: 100%">
                <el-option label="公开" value="PUBLIC" />
                <el-option label="内部" value="INTERNAL" />
                <el-option label="秘密" value="SECRET" />
                <el-option label="机密" value="CONFIDENTIAL" />
                <el-option label="绝密" value="TOP_SECRET" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="进行中" value="IN_PROGRESS" />
                <el-option label="已完成" value="COMPLETED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="适用标准">
          <el-input v-model="form.applicableStandards" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProjects, createProject, updateProject, type ProjectItem } from '@/api/project'
import { getDictItems, type DictItem } from '@/api/dict'

const loading = ref(false)
const projectTypes = ref<DictItem[]>([])
const saving = ref(false)
const projects = ref<ProjectItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const statusFilter = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): ProjectItem => ({
  projectCode: '',
  projectName: '',
  projectType: '',
  securityLevel: 'INTERNAL',
  status: 'DRAFT',
  ownerUserId: '',
  applicableStandards: '',
  startDate: '',
  endDate: '',
  description: ''
})

const form = reactive<ProjectItem>(emptyForm())
const formRules = {
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

function statusTag(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', IN_PROGRESS: 'primary', COMPLETED: 'success', ARCHIVED: 'warning'
  }
  return map[status] || 'info'
}

function typeLabel(type: string) {
  const found = projectTypes.value.find(t => t.dictCode === type)
  return found?.dictName || type
}

async function fetchProjectTypes() {
  try {
    const res = await getDictItems('PROJECT_TYPE')
    projectTypes.value = res.data.data
  } catch { /* handled */ }
}

function securityLabel(level: string) {
  const map: Record<string, string> = {
    PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密', TOP_SECRET: '绝密'
  }
  return map[level] || level
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿', IN_PROGRESS: '进行中', COMPLETED: '已完成', ARCHIVED: '已归档'
  }
  return map[status] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getProjects({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined
    })
    const data = res.data.data
    projects.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function showEditDialog(row: ProjectItem) {
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
      await updateProject(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createProject({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchProjectTypes()
  fetchData()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
