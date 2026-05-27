<template>
  <div class="page">
    <div class="back-bar">
      <el-button link @click="$router.push('/standards')">
        <el-icon><ArrowLeft /></el-icon>返回标准库
      </el-button>
    </div>

    <el-card v-loading="loading" class="standard-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>{{ standard?.standardCode }} {{ standard?.standardName }}</h2>
            <div class="meta">
              <el-tag size="small">{{ standard?.standardType }}</el-tag>
              <el-tag size="small" type="info">{{ standard?.category }}</el-tag>
              <span v-if="standard?.version">版本: {{ standard?.version }}</span>
              <span v-if="standard?.publishDate">发布: {{ standard?.publishDate }}</span>
              <span v-if="standard?.effectiveDate">生效: {{ standard?.effectiveDate }}</span>
            </div>
          </div>
          <div>
            <el-button v-if="standard?.fileObjectId" type="primary" @click="handleDownload">下载标准文件</el-button>
            <el-tag v-else type="warning" size="small">未上传文件</el-tag>
          </div>
        </div>
      </template>

      <div class="description" v-if="standard?.description">
        <h4>标准简介</h4>
        <p>{{ standard?.description }}</p>
      </div>

      <div class="clauses-section">
        <div class="section-header">
          <h4>标准条款结构</h4>
          <div style="display: flex; gap: 10px; align-items: center">
            <el-input v-model="keyword" placeholder="搜索条款内容..." style="width: 260px" clearable @change="fetchClauses" />
            <el-button v-if="standard?.fileObjectId" type="success" size="small" :loading="extracting" @click="handleExtractClauses">
              自动提取条款
            </el-button>
            <el-button type="primary" size="small" @click="showClauseDialog()">
              <el-icon><Plus /></el-icon>添加条款
            </el-button>
          </div>
        </div>
        <el-table :data="clauses" v-loading="clausesLoading" max-height="500" style="margin-top: 12px">
          <el-table-column prop="clauseNumber" label="章节号" width="100" />
          <el-table-column prop="clauseTitle" label="标题" width="220" />
          <el-table-column prop="clauseContent" label="内容" min-width="250" show-overflow-tooltip />
          <el-table-column prop="keywords" label="关键字" width="140">
            <template #default="{ row }">
              <el-tag v-for="kw in (row.keywords || '').split(',')" :key="kw" size="small" class="kw-tag">
                {{ kw.trim() }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link size="small" type="primary" @click="showClauseDialog(row)">编辑</el-button>
              <el-button link size="small" type="danger" @click="handleDeleteClause(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Clause Edit Dialog -->
    <el-dialog v-model="clauseDialogVisible" :title="editingClauseId ? '编辑条款' : '添加条款'" width="600px">
      <el-form :model="clauseForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="章节号"><el-input v-model="clauseForm.clauseNumber" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号"><el-input-number v-model="clauseForm.orderNum" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="父条款ID"><el-input-number v-model="clauseForm.parentId" :min="0" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标题"><el-input v-model="clauseForm.clauseTitle" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="clauseForm.clauseContent" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="关键字"><el-input v-model="clauseForm.keywords" placeholder="逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clauseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="clauseSaving" @click="handleClauseSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken } from '@/utils/auth'
import {
  getStandard, getStandardDownloadUrl, getStandardClauses, searchStandardClauses,
  extractStandardClauses, createStandardClause, updateStandardClause, deleteStandardClause,
  type StandardItem, type StandardClauseItem
} from '@/api/standard'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const standard = ref<StandardItem | null>(null)
const clauses = ref<StandardClauseItem[]>([])
const clausesLoading = ref(false)
const keyword = ref('')
const extracting = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await getStandard(id)
    standard.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function fetchClauses() {
  if (!standard.value) return
  clausesLoading.value = true
  try {
    let res
    if (keyword.value) {
      res = await searchStandardClauses(standard.value.id!, keyword.value)
    } else {
      res = await getStandardClauses(standard.value.id!)
    }
    clauses.value = res.data.data
  } finally {
    clausesLoading.value = false
  }
}

async function handleDownload() {
  try {
    const res = await getStandardDownloadUrl(id)
    const url = res.data.data
    const token = getToken()
    const blobResp = await fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    if (!blobResp.ok) throw new Error('Download failed')
    const blob = await blobResp.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = standard.value?.fileName || standard.value?.standardName || 'standard.docx'
    a.click()
    URL.revokeObjectURL(a.href)
  } catch { /* ignore */ }
}

async function handleExtractClauses() {
  extracting.value = true
  try {
    const res = await extractStandardClauses(id)
    const result = res.data.data
    const count = result?.clauseCount || 0
    if (result?.warning) {
      ElMessage.warning(result.warning)
    } else if (count > 0) {
      ElMessage.success(`条款提取完成，共生成 ${count} 个条款`)
    } else {
      ElMessage.info('未识别到条款结构')
    }
    fetchClauses()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '条款提取失败')
  } finally {
    extracting.value = false
  }
}

// Clause editing
const clauseDialogVisible = ref(false)
const editingClauseId = ref<number | null>(null)
const clauseSaving = ref(false)
const clauseForm = reactive<StandardClauseItem>({
  standardId: id, clauseNumber: '', clauseTitle: '', clauseContent: '', parentId: 0, orderNum: 0, keywords: ''
})

function showClauseDialog(row?: StandardClauseItem) {
  if (row) {
    editingClauseId.value = row.id!
    Object.assign(clauseForm, row)
  } else {
    editingClauseId.value = null
    Object.assign(clauseForm, {
      standardId: id, clauseNumber: '', clauseTitle: '', clauseContent: '', parentId: 0, orderNum: 0, keywords: ''
    })
  }
  clauseDialogVisible.value = true
}

async function handleClauseSave() {
  clauseSaving.value = true
  try {
    if (editingClauseId.value) {
      await updateStandardClause(id, editingClauseId.value, { ...clauseForm })
      ElMessage.success('条款更新成功')
    } else {
      await createStandardClause(id, { ...clauseForm })
      ElMessage.success('条款创建成功')
    }
    clauseDialogVisible.value = false
    fetchClauses()
  } finally {
    clauseSaving.value = false
  }
}

async function handleDeleteClause(row: StandardClauseItem) {
  await ElMessageBox.confirm(`确定删除条款「${row.clauseNumber} ${row.clauseTitle}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteStandardClause(id, row.id!)
    ElMessage.success('删除成功')
    fetchClauses()
  } catch { /* cancelled */ }
}

onMounted(() => {
  fetchData()
  fetchClauses()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.back-bar { margin-bottom: 16px; }
.standard-card { max-width: 1100px; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; }
.card-header h2 { margin: 0 0 8px; font-size: 20px; }
.meta { display: flex; gap: 12px; align-items: center; color: #909399; font-size: 13px; }
.description { margin: 8px 0 20px; padding-bottom: 16px; border-bottom: 1px solid #ebeef5; }
.description h4 { margin: 0 0 8px; font-size: 15px; }
.description p { color: #606266; line-height: 1.7; margin: 0; }
.clauses-section { margin-top: 8px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.section-header h4 { margin: 0; font-size: 15px; }
.kw-tag { margin: 2px; }
</style>
