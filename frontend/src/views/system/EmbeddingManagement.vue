<template>
  <div class="embedding-management">
    <h2>向量索引管理</h2>
    <p class="subtitle">管理标准条款和知识库的语义向量嵌入索引，用于 AI 语义检索（RAG）</p>

    <!-- Stats -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.indexedClauses ?? '--' }} / {{ stats.totalClauses ?? '--' }}</div>
            <div class="stat-label">标准条款已索引</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.indexedKnowledge ?? '--' }} / {{ stats.totalKnowledge ?? '--' }}</div>
            <div class="stat-label">知识库文章已索引</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.embeddingModel || '--' }}</div>
            <div class="stat-label">嵌入模型</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-tag :type="stats.semanticRagEnabled ? 'success' : 'info'" size="large">
              {{ stats.semanticRagEnabled ? '已启用' : '未启用' }}
            </el-tag>
            <div class="stat-label">语义 RAG</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Actions -->
    <el-card class="actions-card">
      <template #header>
        <span>索引操作</span>
      </template>
      <el-space>
        <el-button type="primary" :loading="indexingClauses" @click="handleIndexClauses">
          索引全部标准条款
        </el-button>
        <el-button type="success" :loading="indexingKnowledge" @click="handleIndexKnowledge">
          索引全部知识库
        </el-button>
        <el-button @click="refreshStats" :loading="refreshing">刷新状态</el-button>
      </el-space>
      <p class="hint">
        索引可能需要几十秒到几分钟，取决于数据量。等待过程中可以刷新查看进度。
        语义 RAG 当前为 <strong>{{ stats.semanticRagEnabled ? '启用' : '禁用' }}</strong> 状态，
        可通过 <code>application.yml</code> 中 <code>embedding.semantic-rag-enabled</code> 控制。
      </p>
    </el-card>

    <!-- Recent tasks -->
    <el-card class="tasks-card">
      <template #header>
        <span>索引任务记录</span>
      </template>
      <el-table :data="tasks" v-loading="loadingTasks" style="width: 100%" size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="taskType" label="任务类型" width="160">
          <template #default="{ row }">
            <el-tag size="small">{{ taskTypeLabel(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetTable" label="目标表" width="140" />
        <el-table-column label="进度" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="row.totalCount > 0 ? Math.round(row.completedCount / row.totalCount * 100) : 0"
              :status="row.status === 'COMPLETED' ? 'success' : row.status === 'FAILED' ? 'exception' : undefined"
            />
            <span class="progress-text">{{ row.completedCount }}/{{ row.totalCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  indexAllClauses, indexAllKnowledge,
  getEmbeddingStats, getIndexTasks
} from '@/api/ai'

const stats = ref<any>({})
const tasks = ref<any[]>([])
const indexingClauses = ref(false)
const indexingKnowledge = ref(false)
const refreshing = ref(false)
const loadingTasks = ref(false)

async function refreshStats() {
  refreshing.value = true
  try {
    const [statsRes, tasksRes] = await Promise.all([
      getEmbeddingStats(),
      getIndexTasks()
    ])
    stats.value = statsRes.data?.data || {}
    tasks.value = tasksRes.data?.data || []
  } catch {
    // handled by interceptor
  } finally {
    refreshing.value = false
  }
}

async function handleIndexClauses() {
  indexingClauses.value = true
  try {
    const res = await indexAllClauses()
    ElMessage.success(`索引任务已启动 (ID: ${res.data?.data?.taskId})`)
    setTimeout(() => refreshStats(), 2000)
  } catch {
    // handled by interceptor
  } finally {
    indexingClauses.value = false
  }
}

async function handleIndexKnowledge() {
  indexingKnowledge.value = true
  try {
    const res = await indexAllKnowledge()
    ElMessage.success(`索引任务已启动 (ID: ${res.data?.data?.taskId})`)
    setTimeout(() => refreshStats(), 2000)
  } catch {
    // handled by interceptor
  } finally {
    indexingKnowledge.value = false
  }
}

function taskTypeLabel(type: string) {
  const map: Record<string, string> = {
    INDEX_CLAUSES: '标准条款索引',
    INDEX_KNOWLEDGE: '知识库索引',
    FULL_INDEX: '全量索引',
    REINDEX_CLAUSES: '重建条款索引',
    REINDEX_KNOWLEDGE: '重建知识库索引'
  }
  return map[type] || type
}

function statusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'info',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

onMounted(() => {
  refreshStats()
})
</script>

<style scoped>
.embedding-management {
  padding: 20px;
}
.subtitle {
  color: #909399;
  margin-bottom: 20px;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  padding: 10px 0;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  font-family: 'Courier New', monospace;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}
.actions-card {
  margin-bottom: 20px;
}
.hint {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}
.hint code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}
.tasks-card {
  margin-bottom: 20px;
}
.progress-text {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
