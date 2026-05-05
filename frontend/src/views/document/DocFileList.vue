<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" clearable @change="fetchData">
          <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="文档状态" style="width: 140px" clearable @change="fetchData">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="评审中" value="IN_REVIEW" />
          <el-option label="已批准" value="APPROVED" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已作废" value="OBSOLETE" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建文档</el-button>
    </div>

    <el-table :data="docFiles" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="docName" label="文档名称" min-width="200" />
      <el-table-column prop="docType" label="文档类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ docTypeLabel(row.docType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="securityLevel" label="密级" width="100">
        <template #default="{ row }">
          <el-tag :type="row.securityLevel === 'TOP_SECRET' ? 'danger' : 'warning'" size="small">
            {{ row.securityLevel }}
          </el-tag>
        </template>
      </el-table-column>
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
      :title="editingId ? '编辑文档' : '创建文档'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档名称" prop="docName">
          <el-input v-model="form.docName" />
        </el-form-item>
        <el-row :gutter="20">
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
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="评审中" value="IN_REVIEW" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已作废" value="OBSOLETE" />
          </el-select>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDocFiles, createDocFile, updateDocFile, deleteDocFile, type DocFileItem } from '@/api/doc-file'
import { getProjects, type ProjectItem } from '@/api/project'

const loading = ref(false)
const saving = ref(false)
const docFiles = ref<DocFileItem[]>([])
const projects = ref<ProjectItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const projectId = ref<number | undefined>()
const statusFilter = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): DocFileItem => ({
  projectId: 0,
  docName: '',
  docType: '',
  securityLevel: 'INTERNAL',
  status: 'DRAFT'
})

const form = reactive<DocFileItem>(emptyForm())
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
    DRAFT: 'info', IN_REVIEW: 'primary', APPROVED: 'success', PUBLISHED: '', OBSOLETE: 'warning'
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
    const res = await getDocFiles({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      projectId: projectId.value,
      status: statusFilter.value || undefined
    })
    const data = res.data.data
    docFiles.value = data.records
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

function showEditDialog(row: DocFileItem) {
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
      await updateDocFile(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createDocFile({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: DocFileItem) {
  await ElMessageBox.confirm(`确定要删除文档「${row.docName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteDocFile(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled or error */ }
}

onMounted(() => {
  fetchProjects()
  fetchData()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
