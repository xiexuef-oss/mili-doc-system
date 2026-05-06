<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索标准编号/名称" style="width: 240px" clearable @change="fetchData" />
        <el-select v-model="filterType" placeholder="标准类型" style="width: 150px" clearable @change="fetchData">
          <el-option v-for="t in standardTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="filterCategory" placeholder="分类" style="width: 150px" clearable @change="fetchData">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加标准</el-button>
    </div>

    <el-table :data="standards" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="standardCode" label="标准编号" width="160" />
      <el-table-column prop="standardName" label="标准名称" min-width="280" />
      <el-table-column prop="standardType" label="类型" width="110" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column label="文件" width="180">
        <template #default="{ row }">
          <span v-if="row.fileName" style="color: #409eff; cursor: pointer" @click="handleDownload(row)">
            {{ row.fileName }}
          </span>
          <span v-else class="text-muted">未上传</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">条款</el-button>
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="success" @click="showUpload(row)">
            <el-icon><Upload /></el-icon>上传文件
          </el-button>
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
      :title="editingId ? '编辑标准' : '添加标准'"
      width="600px"
      @close="resetForm"
    >
      <!-- File auto-parse (only in create mode) -->
      <div v-if="!editingId" class="parse-section">
        <el-upload
          ref="createUploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          :on-change="handleParseFile"
          :disabled="parsing"
          accept=".pdf,.doc,.docx"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            <template v-if="parsing">
              <el-icon class="is-loading"><Loading /></el-icon> 正在解析: {{ parseFileName }}
            </template>
            <template v-else>
              拖拽标准文件到此处自动识别信息<br/><small>支持 .pdf / .doc / .docx，解析后可手动修正</small>
            </template>
          </div>
        </el-upload>
      </div>

      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" style="margin-top: 16px">
        <el-form-item label="标准编号" prop="standardCode">
          <el-input v-model="form.standardCode" />
        </el-form-item>
        <el-form-item label="标准名称" prop="standardName">
          <el-input v-model="form.standardName" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标准类型">
              <el-input v-model="form.standardType" placeholder="如：GJB" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-input v-model="form.category" placeholder="如：管理类" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="版本">
              <el-input v-model="form.version" placeholder="如：2023" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="ACTIVE">有效</el-radio>
                <el-radio value="INACTIVE">作废</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发布日期">
              <el-date-picker v-model="form.publishDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期">
              <el-date-picker v-model="form.effectiveDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <!-- Show file info when parsed -->
        <el-form-item v-if="form.fileName" label="已上传文件">
          <el-tag type="success">{{ form.fileName }} ({{ formatFileSize(form.fileSize || 0) }})</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 上传文件对话框 -->
    <el-dialog v-model="uploadVisible" title="上传标准文件" width="460px">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        accept=".pdf,.doc,.docx"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .pdf/.doc/.docx</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!pendingFile" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 条款浏览抽屉 -->
    <el-drawer
      v-model="clauseDrawerVisible"
      title="标准条款"
      size="600px"
    >
      <template v-if="detailStandard">
        <div class="standard-info">
          <h4>{{ detailStandard.standardCode }} {{ detailStandard.standardName }}</h4>
          <p class="text-muted">{{ detailStandard.description }}</p>
        </div>
        <div class="clause-header">
          <el-input v-model="clauseKeyword" placeholder="搜索条款" style="width: 240px" clearable @change="fetchClauses" />
          <el-button type="primary" size="small" @click="showClauseCreateDialog">添加条款</el-button>
        </div>
        <el-table :data="clauses" v-loading="clausesLoading" style="margin-top: 12px" max-height="460">
          <el-table-column prop="clauseNumber" label="章节号" width="100" />
          <el-table-column prop="clauseTitle" label="标题" width="200" />
          <el-table-column prop="clauseContent" label="内容" min-width="240" show-overflow-tooltip />
          <el-table-column prop="keywords" label="关键字" width="120" />
          <el-table-column label="操作" width="120">
            <template #default="{ row: clause }">
              <el-button link type="primary" size="small" @click="showClauseEditDialog(clause)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleClauseDelete(clause)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 条款编辑对话框 -->
        <el-dialog
          v-model="clauseDialogVisible"
          :title="editingClauseId ? '编辑条款' : '添加条款'"
          width="520px"
        >
          <el-form ref="clauseFormRef" :model="clauseForm" label-width="80px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="章节号">
                  <el-input v-model="clauseForm.clauseNumber" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="排序">
                  <el-input-number v-model="clauseForm.orderNum" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="标题">
              <el-input v-model="clauseForm.clauseTitle" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="clauseForm.clauseContent" type="textarea" :rows="5" />
            </el-form-item>
            <el-form-item label="关键字">
              <el-input v-model="clauseForm.keywords" placeholder="逗号分隔" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="clauseDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="savingClause" @click="handleSaveClause">保存</el-button>
          </template>
        </el-dialog>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  getStandardList, createStandard, updateStandard, deleteStandard,
  uploadStandardFile, parseStandardFile, getStandardDownloadUrl, getStandardTypes, getStandardCategories,
  getStandardClauses, createStandardClause, updateStandardClause, deleteStandardClause, searchStandardClauses,
  type StandardItem, type StandardClauseItem
} from '@/api/standard'

const loading = ref(false)
const saving = ref(false)
const standards = ref<StandardItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const filterType = ref('')
const filterCategory = ref('')
const standardTypes = ref<string[]>([])
const categories = ref<string[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const parsing = ref(false)
const parseFileName = ref('')
const formRef = ref()

const emptyForm = (): StandardItem => ({
  standardCode: '',
  standardName: '',
  standardType: '',
  category: '',
  version: '',
  publishDate: '',
  effectiveDate: '',
  description: '',
  fileObjectId: '',
  fileName: '',
  fileSize: 0,
  fileType: '',
  status: 'ACTIVE'
})

const form = reactive<StandardItem>(emptyForm())
const formRules = {
  standardCode: [{ required: true, message: '请输入标准编号', trigger: 'blur' }],
  standardName: [{ required: true, message: '请输入标准名称', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getStandardList({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      standardType: filterType.value || undefined,
      category: filterCategory.value || undefined
    })
    const data = res.data.data
    standards.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadFilters() {
  try {
    const [typesRes, catsRes] = await Promise.all([
      getStandardTypes().catch(() => ({ data: { data: [] } })),
      getStandardCategories().catch(() => ({ data: { data: [] } }))
    ])
    standardTypes.value = typesRes.data.data || []
    categories.value = catsRes.data.data || []
  } catch { /* ignore */ }
}

function showCreateDialog() {
  editingId.value = null
  Object.assign(form, emptyForm())
  parseFileName.value = ''
  parsing.value = false
  dialogVisible.value = true
}

function showEditDialog(row: StandardItem) {
  editingId.value = row.id!
  Object.assign(form, { ...row })
  parseFileName.value = ''
  parsing.value = false
  dialogVisible.value = true
}

async function handleParseFile(file: any) {
  parsing.value = true
  parseFileName.value = file.name
  try {
    const res = await parseStandardFile(file.raw)
    const { standard } = res.data.data
    Object.assign(form, standard)
    ElMessage.success('文件解析成功，请核对自动识别的信息')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '文件解析失败，请手动填写')
  } finally {
    parsing.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateStandard(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createStandard({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: StandardItem) {
  await ElMessageBox.confirm(`确定要删除标准「${row.standardName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteStandard(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled */ }
}

// 文件上传
const uploadVisible = ref(false)
const uploading = ref(false)
const pendingFile = ref<File | null>(null)
const currentUploadId = ref<number>(0)

function showUpload(row: StandardItem) {
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
    await uploadStandardFile(currentUploadId.value, pendingFile.value)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchData()
  } finally {
    uploading.value = false
  }
}

async function handleDownload(row: StandardItem) {
  try {
    const res = await getStandardDownloadUrl(row.id!)
    window.open('/api/v1' + res.data.data, '_blank')
  } catch { /* ignore */ }
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i]
}

// 条款管理
const clauseDrawerVisible = ref(false)
const detailStandard = ref<StandardItem | null>(null)
const clauses = ref<StandardClauseItem[]>([])
const clausesLoading = ref(false)
const clauseKeyword = ref('')
const clauseDialogVisible = ref(false)
const editingClauseId = ref<number | null>(null)
const savingClause = ref(false)
const clauseFormRef = ref()

const emptyClauseForm = (): StandardClauseItem => ({
  standardId: 0,
  clauseNumber: '',
  clauseTitle: '',
  clauseContent: '',
  parentId: 0,
  orderNum: 0,
  keywords: ''
})

const clauseForm = reactive<StandardClauseItem>(emptyClauseForm())

function showDetail(row: StandardItem) {
  detailStandard.value = row
  clauseKeyword.value = ''
  clauseDrawerVisible.value = true
  fetchClauses()
}

async function fetchClauses() {
  if (!detailStandard.value) return
  clausesLoading.value = true
  try {
    let res
    if (clauseKeyword.value) {
      res = await searchStandardClauses(detailStandard.value.id!, clauseKeyword.value)
    } else {
      res = await getStandardClauses(detailStandard.value.id!)
    }
    clauses.value = res.data.data
  } finally {
    clausesLoading.value = false
  }
}

function showClauseCreateDialog() {
  editingClauseId.value = null
  Object.assign(clauseForm, emptyClauseForm())
  clauseDialogVisible.value = true
}

function showClauseEditDialog(clause: StandardClauseItem) {
  editingClauseId.value = clause.id!
  Object.assign(clauseForm, { ...clause })
  clauseDialogVisible.value = true
}

async function handleSaveClause() {
  savingClause.value = true
  try {
    const sid = detailStandard.value!.id!
    if (editingClauseId.value) {
      await updateStandardClause(sid, editingClauseId.value, { ...clauseForm })
      ElMessage.success('更新成功')
    } else {
      await createStandardClause(sid, { ...clauseForm })
      ElMessage.success('创建成功')
    }
    clauseDialogVisible.value = false
    fetchClauses()
  } finally {
    savingClause.value = false
  }
}

async function handleClauseDelete(clause: StandardClauseItem) {
  await ElMessageBox.confirm('确定要删除该条款吗？', '确认删除', { type: 'warning' })
  try {
    await deleteStandardClause(detailStandard.value!.id!, clause.id!)
    ElMessage.success('删除成功')
    fetchClauses()
  } catch { /* cancelled */ }
}

onMounted(() => {
  fetchData()
  loadFilters()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.text-muted { color: #c0c4cc; }
.standard-info { padding: 8px 0 16px; border-bottom: 1px solid #ebeef5; margin-bottom: 16px; }
.standard-info h4 { margin: 0 0 8px; font-size: 16px; }
.clause-header { display: flex; justify-content: space-between; align-items: center; }
.parse-section { margin-bottom: 8px; }
.parse-section .el-upload { width: 100%; }
.parse-section .el-upload-dragger { width: 100%; }
</style>
