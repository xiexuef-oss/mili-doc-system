<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索模版名称/编码" style="width: 240px" clearable @change="fetchData" />
        <el-select v-model="filterType" placeholder="模版类型" style="width: 160px" clearable @change="fetchData">
          <el-option v-for="t in templateTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="filterProjectType" placeholder="适用项目类型" style="width: 160px" clearable @change="fetchData">
          <el-option v-for="t in projectTypes" :key="t" :label="t" :value="t" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建模版</el-button>
      <el-button type="success" @click="showBatchUpload">批量上传</el-button>
    </div>

    <el-table :data="templates" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="templateName" label="模版名称" min-width="180" />
      <el-table-column prop="templateCode" label="编码" width="140" />
      <el-table-column prop="templateType" label="类型" width="120" />
      <el-table-column prop="applicableProjectType" label="适用项目类型" width="140" />
      <el-table-column label="文件" width="180">
        <template #default="{ row }">
          <span v-if="row.fileName" style="color: #409eff; cursor: pointer" @click="handleDownload(row)">
            {{ row.fileName }}
          </span>
          <span v-else class="text-muted">未上传</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="success" @click="showUpload(row)">
            <el-icon><Upload /></el-icon>上传文件
          </el-button>
          <el-button link type="warning" @click="showVariablesDialog(row)">变量配置</el-button>
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

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑模版' : '创建模版'"
      width="560px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="模版名称" prop="templateName">
          <el-input v-model="form.templateName" />
        </el-form-item>
        <el-form-item label="模版编码" prop="templateCode">
          <el-input v-model="form.templateCode" :disabled="!!editingId" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模版类型" prop="templateType">
              <el-input v-model="form.templateType" placeholder="如：技术方案" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用项目类型">
              <el-input v-model="form.applicableProjectType" placeholder="如：型号项目" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 上传文件对话框 -->
    <el-dialog v-model="uploadVisible" title="上传模版文件" width="460px">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        accept=".doc,.docx,.pdf,.xls,.xlsx,.ppt,.pptx"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .doc/.docx/.pdf/.xls/.xlsx/.ppt/.pptx</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!pendingFile" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 批量上传对话框 -->
    <el-dialog v-model="batchUploadVisible" title="批量上传模版文件" width="520px">
      <el-upload
        ref="batchUploadRef"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleBatchFileChange"
        :on-remove="handleBatchFileRemove"
        accept=".doc,.docx,.pdf,.xls,.xlsx,.ppt,.pptx"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或<em>点击选择多个文件</em></div>
        <template #tip>
          <div class="el-upload__tip">支持同时选择多个模版文件，文件名将作为模版名称</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="batchUploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchUploading" :disabled="batchFiles.length === 0" @click="handleBatchUpload">
          上传 {{ batchFiles.length > 0 ? `(${batchFiles.length} 个文件)` : '' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 变量配置对话框 -->
    <el-dialog v-model="variablesVisible" title="模版变量配置" width="660px">
      <div v-if="currentTemplate" class="variables-section">
        <div class="section-tip">定义模版中的占位变量，格式为 JSON。例如：<code>{"projectName": "项目名称", "projectCode": "项目编号", "date": "编制日期"}</code></div>
        <el-input
          v-model="variablesJson"
          type="textarea"
          :rows="10"
          placeholder='{"key": "变量说明", ...}'
        />
      </div>
      <template #footer>
        <el-button @click="variablesVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingVars" @click="handleSaveVariables">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  getTemplateList, createTemplate, updateTemplate, deleteTemplate,
  uploadTemplateFile, batchUploadTemplateFiles, getTemplateDownloadUrl,
  type TemplateItem
} from '@/api/template'
import { getDictItems } from '@/api/dict'

const loading = ref(false)
const saving = ref(false)
const templates = ref<TemplateItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const filterType = ref('')
const filterProjectType = ref('')
const templateTypes = ref<string[]>([])
const projectTypes = ref<string[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): TemplateItem => ({
  templateName: '',
  templateCode: '',
  templateType: '',
  applicableProjectType: '',
  description: '',
  fileObjectId: '',
  fileName: '',
  fileSize: 0,
  fileType: '',
  variables: '{}',
  status: 'ACTIVE'
})

const form = reactive<TemplateItem>(emptyForm())
const formRules = {
  templateName: [{ required: true, message: '请输入模版名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模版编码', trigger: 'blur' }],
  templateType: [{ required: true, message: '请输入模版类型', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getTemplateList({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      templateType: filterType.value || undefined,
      applicableProjectType: filterProjectType.value || undefined
    })
    const data = res.data.data
    templates.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadTypes() {
  try {
    const [typeRes, projectRes] = await Promise.all([
      getDictItems('template_type').catch(() => ({ data: { data: [] } })),
      getDictItems('project_type').catch(() => ({ data: { data: [] } }))
    ])
    templateTypes.value = typeRes.data.data.map((d: any) => d.dictName || d.dictCode)
    projectTypes.value = projectRes.data.data.map((d: any) => d.dictName || d.dictCode)
  } catch { /* ignore */ }
}

function showCreateDialog() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function showEditDialog(row: TemplateItem) {
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
      await updateTemplate(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createTemplate({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: TemplateItem) {
  await ElMessageBox.confirm(`确定要删除模版「${row.templateName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteTemplate(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled */ }
}

// 文件上传
const uploadVisible = ref(false)
const uploading = ref(false)
const pendingFile = ref<File | null>(null)
const currentUploadId = ref<number>(0)

function showUpload(row: TemplateItem) {
  currentUploadId.value = row.id!
  pendingFile.value = null
  uploadVisible.value = true
}

function handleFileChange(file: any) {
  pendingFile.value = file.raw
}

function handleFileRemove() {
  pendingFile.value = null
}

async function handleUpload() {
  if (!pendingFile.value) return
  uploading.value = true
  try {
    await uploadTemplateFile(currentUploadId.value, pendingFile.value)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchData()
  } finally {
    uploading.value = false
  }
}

// 批量上传
const batchUploadVisible = ref(false)
const batchUploading = ref(false)
const batchFiles = ref<File[]>([])

function showBatchUpload() {
  batchFiles.value = []
  batchUploadVisible.value = true
}

function handleBatchFileChange(file: any) {
  batchFiles.value.push(file.raw)
}

function handleBatchFileRemove(file: any) {
  const idx = batchFiles.value.indexOf(file.raw)
  if (idx >= 0) batchFiles.value.splice(idx, 1)
}

async function handleBatchUpload() {
  if (batchFiles.value.length === 0) return
  batchUploading.value = true
  try {
    await batchUploadTemplateFiles(batchFiles.value)
    ElMessage.success(`批量上传成功，共创建 ${batchFiles.value.length} 条模版记录`)
    batchUploadVisible.value = false
    fetchData()
  } finally {
    batchUploading.value = false
  }
}

async function handleDownload(row: TemplateItem) {
  try {
    const res = await getTemplateDownloadUrl(row.id!)
    window.open(res.data.data, '_blank')
  } catch { /* ignore */ }
}

// 变量配置
const variablesVisible = ref(false)
const savingVars = ref(false)
const currentTemplate = ref<TemplateItem | null>(null)
const variablesJson = ref('')

function showVariablesDialog(row: TemplateItem) {
  currentTemplate.value = row
  variablesJson.value = row.variables || '{}'
  variablesVisible.value = true
}

async function handleSaveVariables() {
  savingVars.value = true
  try {
    await updateTemplate(currentTemplate.value!.id!, {
      ...currentTemplate.value!,
      variables: variablesJson.value
    })
    ElMessage.success('变量配置已保存')
    variablesVisible.value = false
    fetchData()
  } finally {
    savingVars.value = false
  }
}

onMounted(() => {
  fetchData()
  loadTypes()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.text-muted { color: #c0c4cc; }
.variables-section { padding: 8px 0; }
.section-tip { font-size: 13px; color: #909399; margin-bottom: 12px; line-height: 1.6; }
.section-tip code { background: #f5f7fa; padding: 2px 6px; border-radius: 3px; font-size: 12px; }
</style>
