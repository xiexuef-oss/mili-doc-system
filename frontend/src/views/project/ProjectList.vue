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
          <el-col v-if="editingId" :span="12">
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
          <el-select v-model="selectedStandards" multiple filterable placeholder="选择适用标准" style="width: 100%" clearable>
            <el-option v-for="s in standards" :key="s.id" :label="`${s.standardCode} - ${s.standardName}`" :value="s.standardCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>

        <!-- Initial stage selection (only on create) -->
        <el-form-item v-if="!editingId" label="初始阶段" prop="initialStageCode">
          <p style="color:#909399;font-size:12px;margin-bottom:8px">选择项目从哪个阶段开始，该阶段及其后续所有阶段将自动纳入项目管理</p>
          <el-radio-group v-model="form.initialStageCode">
            <div v-for="def in stageDefs" :key="def.code" class="stage-def-item">
              <el-radio :value="def.code">
                <span class="def-name">{{ def.name }}</span>
                <span class="def-desc">— {{ def.description }}</span>
              </el-radio>
            </div>
          </el-radio-group>
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
import { getStageDefinitions, initializeStages, type StageDefinitionItem } from '@/api/project-stage'
import { getStandardList, type StandardItem } from '@/api/standard'

const loading = ref(false)
const projectTypes = ref<DictItem[]>([])
const stageDefs = ref<StageDefinitionItem[]>([])
const saving = ref(false)
const projects = ref<ProjectItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const statusFilter = ref('')

const standards = ref<StandardItem[]>([])
const selectedStandards = ref<string[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = () => ({
  projectCode: '',
  projectName: '',
  projectType: '',
  securityLevel: 'INTERNAL',
  status: 'IN_PROGRESS',
  ownerUserId: '',
  applicableStandards: '',
  startDate: '',
  endDate: '',
  description: '',
  initialStageCode: ''
})

const form = reactive(emptyForm())
const formRules = {
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  initialStageCode: [{ required: true, message: '请选择初始阶段', trigger: 'change' }]
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
  form.initialStageCode = stageDefs.value[0]?.code || '' // default: first stage
  selectedStandards.value = []
  dialogVisible.value = true
}

function showEditDialog(row: ProjectItem) {
  editingId.value = row.id!
  Object.assign(form, row)
  selectedStandards.value = row.applicableStandards ? row.applicableStandards.split(',').filter(Boolean) : []
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSave() {
  await formRef.value?.validate()
  form.applicableStandards = selectedStandards.value.join(',')
  saving.value = true
  try {
    if (editingId.value) {
      await updateProject(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      const { initialStageCode, ...projectData } = form
      const res = await createProject(projectData as ProjectItem)
      const newProjectId = res.data.data.id
      if (newProjectId && initialStageCode) {
        await initializeStages(newProjectId, initialStageCode)
      }
      ElMessage.success('创建成功，阶段已初始化')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function fetchStageDefs() {
  try {
    const res = await getStageDefinitions()
    stageDefs.value = res.data.data || []
  } catch { /* ignore */ }
}

async function fetchStandards() {
  try {
    const res = await getStandardList({ pageSize: 200 })
    standards.value = res.data.data?.records || []
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchProjectTypes()
  fetchStageDefs()
  fetchStandards()
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

.stage-def-item { padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.stage-def-item:last-child { border-bottom: none; }
.def-name { font-weight: 600; color: #303133; }
.def-desc { color: #909399; font-size: 12px; }
</style>
