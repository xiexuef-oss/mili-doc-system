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
      <el-button type="success" @click="showBatchUpload">批量上传</el-button>
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
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">条款</el-button>
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
        <!-- Show extracted clause preview -->
        <el-form-item v-if="parsedClauses.length > 0" label="提取条款">
          <div class="clause-preview">
            <el-tag type="warning" size="small" style="margin-bottom: 8px">
              共识别 {{ parsedClauses.length }} 个条款章节，保存后将自动生成
            </el-tag>
            <div class="clause-tree">
              <div
                v-for="c in parsedClauses"
                :key="c.clauseNumber"
                :class="['clause-node', c.parentClauseNumber ? 'child-node' : 'parent-node']"
                :title="c.clauseTitle.length > 30 ? c.clauseTitle : undefined"
              >
                <span class="clause-num">{{ c.clauseNumber }}</span>
                <span class="clause-title">{{ c.clauseTitle }}</span>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!editingId && !form.fileObjectId" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量上传对话框 -->
    <el-dialog v-model="batchUploadVisible" title="批量上传标准文件" width="520px">
      <el-upload
        ref="batchUploadRef"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleBatchFileChange"
        :on-remove="handleBatchFileRemove"
        accept=".pdf,.doc,.docx"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或<em>点击选择多个文件</em></div>
        <template #tip>
          <div class="el-upload__tip">支持同时选择多个 .pdf/.doc/.docx 文件，系统将自动解析标准元数据</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="batchUploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchUploading" :disabled="batchFiles.length === 0" @click="handleBatchUpload">
          上传 {{ batchFiles.length > 0 ? `(${batchFiles.length} 个文件)` : '' }}
        </el-button>
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
          <div class="clause-actions">
            <el-button v-if="detailStandard?.fileObjectId" type="success" size="small" :loading="extractingClauses" @click="handleExtractClauses">
              自动提取条款
            </el-button>
            <el-button type="primary" size="small" @click="showClauseCreateDialog">添加条款</el-button>
          </div>
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
          <el-form :model="clauseForm" label-width="80px">
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
import { Loading, UploadFilled } from '@element-plus/icons-vue'
import { getToken } from '@/utils/auth'
import {
  getStandardList, createStandard, updateStandard, deleteStandard,
  batchUploadStandardFiles, parseStandardFile, getStandardDownloadUrl, getStandardTypes, getStandardCategories,
  getStandardClauses, createStandardClause, updateStandardClause, deleteStandardClause, searchStandardClauses,
  extractStandardClauses,
  type StandardItem, type StandardClauseItem, type StandardClauseExtract, type ParseResult
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
const parsedClauses = ref<StandardClauseExtract[]>([])
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
  parsedClauses.value = []
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
    const data = res.data.data as ParseResult
    const { standard, extractedClauses: clauses, ocrUsed, ocrStats } = data
    Object.assign(form, standard)
    parsedClauses.value = clauses || []
    if (ocrUsed && ocrStats) {
      ElMessage.info(`已使用 OCR 识别扫描件 (${ocrStats})`)
    }
    if (parsedClauses.value.length > 0) {
      ElMessage.success(`解析成功: 识别到 ${parsedClauses.value.length} 个条款章节，请核对信息`)
    } else if (!ocrUsed) {
      ElMessage.warning('文件解析完成，未识别到条款结构。如为扫描件，请启用 OCR 功能')
    } else {
      ElMessage.warning('OCR 识别完成但未提取到条款结构，请手动添加')
    }
  } catch (e: any) {
    parsedClauses.value = []
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
    await batchUploadStandardFiles(batchFiles.value)
    ElMessage.success(`批量上传成功，共创建 ${batchFiles.value.length} 条标准记录`)
    batchUploadVisible.value = false
    fetchData()
  } finally {
    batchUploading.value = false
  }
}

async function handleDownload(row: StandardItem) {
  try {
    const res = await getStandardDownloadUrl(row.id!)
    const url = res.data.data
    const token = getToken()
    const blobResp = await fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    if (!blobResp.ok) throw new Error('Download failed')
    const blob = await blobResp.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = row.fileName || row.standardName || 'standard.docx'
    a.click()
    URL.revokeObjectURL(a.href)
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
const extractingClauses = ref(false)


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

async function handleExtractClauses() {
  if (!detailStandard.value) return
  extractingClauses.value = true
  try {
    const res = await extractStandardClauses(detailStandard.value.id!)
    const result = res.data.data
    const count = result?.clauseCount || 0
    if (result?.warning) {
      ElMessage.warning(result.warning)
    } else {
      ElMessage.success(`条款提取完成，共生成 ${count} 个条款`)
    }
    fetchClauses()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '条款提取失败')
  } finally {
    extractingClauses.value = false
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
.clause-actions { display: flex; gap: 8px; }
.parse-section { margin-bottom: 8px; }
.parse-section .el-upload { width: 100%; }
.parse-section .el-upload-dragger { width: 100%; }
.clause-preview { width: 100%; }
.clause-tree { max-height: 200px; overflow-y: auto; border: 1px solid #ebeef5; border-radius: 4px; padding: 8px 12px; }
.clause-node { display: flex; gap: 8px; padding: 3px 0; font-size: 13px; line-height: 1.5; }
.child-node { padding-left: 24px; }
.clause-num { color: #409eff; font-weight: 500; min-width: 40px; flex-shrink: 0; }
.clause-title { color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
