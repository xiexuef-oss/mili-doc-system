<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-select v-if="!workspaceProjectId" v-model="projectId" placeholder="选择项目" style="width: 200px" clearable @change="fetchData">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建目录条目</el-button>
    </div>

    <el-table :data="catalogs" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="docCode" label="文档编号" width="140" />
      <el-table-column prop="docName" label="文档名称" min-width="200" />
      <el-table-column prop="docType" label="文档类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ docTypeLabel(row.docType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="requiredFlag" label="必选" width="70">
        <template #default="{ row }">
          <el-tag :type="row.requiredFlag ? 'danger' : 'info'" size="small">
            {{ row.requiredFlag ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="meetingUsage" label="会议用途" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
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
      :title="editingId ? '编辑目录条目' : '创建目录条目'"
      width="640px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item v-if="!workspaceProjectId" label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="文档编号" prop="docCode">
              <el-input v-model="form.docCode" placeholder="如 WBS-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文档类型" prop="docType">
              <el-select v-model="form.docType" style="width: 100%">
                <el-option label="技术文档" value="TECH_DOC" />
                <el-option label="管理文档" value="MGMT_DOC" />
                <el-option label="质量文档" value="QUALITY_DOC" />
                <el-option label="设计文档" value="DESIGN_DOC" />
                <el-option label="测试文档" value="TEST_DOC" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="文档名称" prop="docName">
          <el-input v-model="form.docName" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="必选" prop="requiredFlag">
              <el-switch v-model="form.requiredFlag" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已下发" value="ISSUED" />
                <el-option label="已变更" value="CHANGED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="会议用途">
          <el-input v-model="form.meetingUsage" placeholder="如：评审会、设计审查" />
        </el-form-item>
        <el-form-item label="使用来源">
          <el-input v-model="form.usageSource" placeholder="如：GJB-XXX 标准" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="form.usageAdjustReason" type="textarea" :rows="2" />
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDocCatalogs, createDocCatalog, updateDocCatalog, deleteDocCatalog, type DocCatalogItem } from '@/api/doc-catalog'
import { getProjects, type ProjectItem } from '@/api/project'

const route = useRoute()
const workspaceProjectId = computed(() => {
  const pid = route.params.projectId
  return pid ? Number(pid) : undefined
})

const loading = ref(false)
const saving = ref(false)
const catalogs = ref<DocCatalogItem[]>([])
const projects = ref<ProjectItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const projectId = ref<number | undefined>(workspaceProjectId.value)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): DocCatalogItem => ({
  projectId: 0,
  docCode: '',
  docName: '',
  docType: '',
  requiredFlag: true,
  status: 'DRAFT'
})

const form = reactive<DocCatalogItem>(emptyForm())
const formRules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  docName: [{ required: true, message: '请输入文档名称', trigger: 'blur' }]
}

function docTypeLabel(type: string) {
  const map: Record<string, string> = {
    TECH_DOC: '技术文档', MGMT_DOC: '管理文档', QUALITY_DOC: '质量文档',
    DESIGN_DOC: '设计文档', TEST_DOC: '测试文档'
  }
  return map[type] || type
}

function statusTag(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info', ISSUED: 'success', CHANGED: 'warning'
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
    const res = await getDocCatalogs({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      projectId: projectId.value
    })
    const data = res.data.data
    catalogs.value = data.records
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

function showEditDialog(row: DocCatalogItem) {
  editingId.value = row.id!
  Object.assign(form, { ...row })
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
      await updateDocCatalog(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createDocCatalog({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: DocCatalogItem) {
  await ElMessageBox.confirm(`确定要删除目录条目「${row.docName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteDocCatalog(row.id!)
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
