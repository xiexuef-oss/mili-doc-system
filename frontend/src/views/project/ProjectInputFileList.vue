<template>
  <div class="input-files-page">
    <div class="page-header">
      <span class="text-muted">上传项目输入文件（任务书、合同、质量程序、标准要求等），作为文档目录生成依据</span>
      <el-button type="primary" @click="showUploadDialog">上传文件</el-button>
    </div>

    <el-table :data="files" v-loading="loading">
      <el-table-column prop="fileName" label="文件名" min-width="280" />
      <el-table-column prop="inputType" label="文件类型" width="140">
        <template #default="{ row }">
          <el-tag size="small">{{ inputTypeLabel(row.inputType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="uploadedAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && files.length === 0" description="暂无输入文件，请上传" />

    <el-dialog v-model="dialogVisible" title="上传输入文件" width="480px">
      <el-form label-width="80px">
        <el-form-item label="文件类型" required>
          <el-select v-model="uploadType" style="width: 100%">
            <el-option label="任务书" value="TASK_BOOK" />
            <el-option label="合同/技术协议" value="CONTRACT" />
            <el-option label="质量程序文件" value="QUALITY_PROCEDURE" />
            <el-option label="标准要求" value="STANDARD_REQ" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadDesc" placeholder="文件说明（可选）" />
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.zip"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处或<em>点击上传</em></div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!pendingFile || !uploadType" @click="handleUpload">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import api from '@/api/index'

const route = useRoute()
const projectId = computed(() => Number(route.params.projectId))

const loading = ref(false)
const uploading = ref(false)
const files = ref<any[]>([])

const dialogVisible = ref(false)
const uploadType = ref('')
const uploadDesc = ref('')
const pendingFile = ref<File | null>(null)

function inputTypeLabel(type: string) {
  const map: Record<string, string> = {
    TASK_BOOK: '任务书', CONTRACT: '合同/技术协议',
    QUALITY_PROCEDURE: '质量程序文件', STANDARD_REQ: '标准要求', OTHER: '其他'
  }
  return map[type] || type
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i]
}

async function fetchFiles() {
  loading.value = true
  try {
    const res = await api.get(`/projects/${projectId.value}/input-files`)
    files.value = res.data.data
  } finally {
    loading.value = false
  }
}

function showUploadDialog() {
  uploadType.value = ''
  uploadDesc.value = ''
  pendingFile.value = null
  dialogVisible.value = true
}

function handleFileChange(file: any) {
  pendingFile.value = file.raw
}

function handleFileRemove() {
  pendingFile.value = null
}

async function handleUpload() {
  if (!pendingFile.value || !uploadType.value) return
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', pendingFile.value)
    form.append('inputType', uploadType.value)
    if (uploadDesc.value) form.append('description', uploadDesc.value)
    await api.post(`/projects/${projectId.value}/input-files`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    dialogVisible.value = false
    fetchFiles()
  } finally {
    uploading.value = false
  }
}

async function handleDownload(row: any) {
  try {
    const res = await api.get(`/projects/${projectId.value}/input-files/${row.id}/download-url`)
    const url = res.data.data
    const blobResp = await fetch(url)
    const blob = await blobResp.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = row.fileName || 'file'
    a.click()
    URL.revokeObjectURL(a.href)
  } catch { /* ignore */ }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定要删除「${row.fileName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await api.delete(`/projects/${projectId.value}/input-files/${row.id}`)
    ElMessage.success('删除成功')
    fetchFiles()
  } catch { /* ignore */ }
}

onMounted(fetchFiles)
</script>

<style scoped>
.input-files-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.text-muted { color: #909399; font-size: 14px; }
</style>
