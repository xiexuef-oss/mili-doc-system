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
          <el-input v-model="keyword" placeholder="搜索条款内容..." style="width: 260px" clearable @change="fetchClauses" />
        </div>
        <el-table :data="clauses" v-loading="clausesLoading" max-height="500" style="margin-top: 12px">
          <el-table-column prop="clauseNumber" label="章节号" width="100" />
          <el-table-column prop="clauseTitle" label="标题" width="220" />
          <el-table-column prop="clauseContent" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="keywords" label="关键字" width="140">
            <template #default="{ row }">
              <el-tag v-for="kw in (row.keywords || '').split(',')" :key="kw" size="small" class="kw-tag">
                {{ kw.trim() }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getStandard, getStandardDownloadUrl, getStandardClauses, searchStandardClauses, type StandardItem, type StandardClauseItem } from '@/api/standard'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const standard = ref<StandardItem | null>(null)
const clauses = ref<StandardClauseItem[]>([])
const clausesLoading = ref(false)
const keyword = ref('')

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
    window.open(res.data.data, '_blank')
  } catch { /* ignore */ }
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
