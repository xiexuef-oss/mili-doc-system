<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索标题/内容/标签" style="width: 280px" clearable @change="fetchData" />
        <el-select v-model="filterCategory" placeholder="分类" style="width: 160px" clearable @change="fetchData">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加知识条目</el-button>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="标签" width="180">
        <template #default="{ row }">
          <el-tag v-for="t in parseTags(row.tags)" :key="t" size="small" style="margin: 1px">{{ t }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="附件" width="120">
        <template #default="{ row }">
          <span v-if="row.fileName" style="color: #409eff; cursor: pointer; font-size: 13px" @click="handleDownload(row)">
            {{ row.fileName }}
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '归档' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">查看</el-button>
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="success" @click="showUpload(row)">
            <el-icon><Upload /></el-icon>
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

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑知识条目' : '添加知识条目'"
      width="700px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-input v-model="form.category" placeholder="如：标准规范" list="cat-list" />
              <datalist id="cat-list">
                <option v-for="c in categories" :key="c" :value="c" />
              </datalist>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持纯文本和Markdown格式" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="ARCHIVED">归档</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.fileName" label="附件">
          <el-tag type="success" closable @close="form.fileObjectId=''; form.fileName=''; form.fileSize=0; form.fileType=''">
            {{ form.fileName }} ({{ formatFileSize(form.fileSize || 0) }})
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="知识条目详情" width="700px">
      <template v-if="detail">
        <h2 style="margin: 0 0 16px">{{ detail.title }}</h2>
        <div style="margin-bottom: 12px">
          <el-tag v-if="detail.category" size="small" type="warning">{{ detail.category }}</el-tag>
          <el-tag v-for="t in parseTags(detail.tags)" :key="t" size="small" style="margin-left: 4px">{{ t }}</el-tag>
        </div>
        <div style="white-space: pre-wrap; line-height: 1.8; margin-bottom: 16px; max-height: 400px; overflow-y: auto">
          {{ detail.content }}
        </div>
        <div v-if="detail.fileName" style="color: #909399; font-size: 13px">
          附件：<el-button link type="primary" @click="handleDownload(detail)">{{ detail.fileName }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Upload File Dialog -->
    <el-dialog v-model="uploadVisible" title="上传附件" width="460px">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!pendingFile" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  getKnowledgeBaseList, createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase,
  uploadKnowledgeBaseFile, getKnowledgeBaseDownloadUrl, getKnowledgeBaseCategories,
  type KnowledgeBaseItem
} from '@/api/knowledge-base'

const loading = ref(false)
const saving = ref(false)
const list = ref<KnowledgeBaseItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const filterCategory = ref('')
const categories = ref<string[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): KnowledgeBaseItem => ({
  title: '', content: '', category: '', tags: '',
  fileObjectId: '', fileName: '', fileSize: 0, fileType: '', status: 'ACTIVE'
})

const form = reactive<KnowledgeBaseItem>(emptyForm())
const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getKnowledgeBaseList({
      pageNo: pageNo.value, pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      category: filterCategory.value || undefined
    })
    const data = res.data.data
    list.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

async function loadCategories() {
  try {
    const res = await getKnowledgeBaseCategories()
    categories.value = res.data.data || []
  } catch { /* ignore */ }
}

function showCreateDialog() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function showEditDialog(row: KnowledgeBaseItem) {
  editingId.value = row.id!
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateKnowledgeBase(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createKnowledgeBase({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

async function handleDelete(row: KnowledgeBaseItem) {
  await ElMessageBox.confirm(`确定要删除「${row.title}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteKnowledgeBase(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled */ }
}

// Detail
const detailVisible = ref(false)
const detail = ref<KnowledgeBaseItem | null>(null)

function showDetail(row: KnowledgeBaseItem) {
  detail.value = row
  detailVisible.value = true
}

// File upload
const uploadVisible = ref(false)
const uploading = ref(false)
const pendingFile = ref<File | null>(null)
const currentUploadId = ref(0)

function showUpload(row: KnowledgeBaseItem) {
  currentUploadId.value = row.id!
  pendingFile.value = null
  uploadVisible.value = true
}
function handleFileChange(file: any) { pendingFile.value = file.raw }
function handleFileRemove() { pendingFile.value = null }

async function handleUpload() {
  if (!pendingFile.value) return
  uploading.value = true
  try {
    await uploadKnowledgeBaseFile(currentUploadId.value, pendingFile.value)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchData()
  } finally { uploading.value = false }
}

async function handleDownload(row: KnowledgeBaseItem) {
  try {
    const res = await getKnowledgeBaseDownloadUrl(row.id!)
    const url = res.data.data
    const blobResp = await fetch(url)
    const blob = await blobResp.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = row.fileName || row.title || 'knowledge.docx'
    a.click()
    URL.revokeObjectURL(a.href)
  } catch { /* ignore */ }
}

function parseTags(tags: string): string[] {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i]
}

onMounted(() => { fetchData(); loadCategories() })
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.text-muted { color: #c0c4cc; }
</style>
