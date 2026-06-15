<template>
  <div class="audit-log-page">
    <h2>AI API 审计日志</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalCalls ?? '-' }}</div>
            <div class="stat-label">总调用次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" :class="stats.successRate > 0.95 ? 'text-success' : 'text-warning'">
              {{ stats.successRate ? (stats.successRate * 100).toFixed(1) + '%' : '-' }}
            </div>
            <div class="stat-label">成功率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.avgLatencyMs ? stats.avgLatencyMs + 'ms' : '-' }}</div>
            <div class="stat-label">平均延迟</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ localCallsPercent }}%</div>
            <div class="stat-label">本地调用占比</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="任务类型">
          <el-select v-model="filters.taskType" clearable placeholder="全部" style="width: 180px">
            <el-option label="目录生成" value="catalog" />
            <el-option label="初稿生成" value="draft" />
            <el-option label="校对" value="proofread" />
            <el-option label="预评审" value="pre-review" />
            <el-option label="合规检查" value="compliance" />
            <el-option label="意见汇总" value="opinion-summary" />
            <el-option label="阶段评估" value="stage-readiness" />
            <el-option label="归档建议" value="archive-advice" />
            <el-option label="变更分析" value="change-impact" />
            <el-option label="AI对话" value="CHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志表格 -->
    <el-card>
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="taskType" label="任务类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ taskTypeLabel(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="提供商" width="90" />
        <el-table-column prop="model" label="模型" width="140" />
        <el-table-column label="位置" width="80">
          <template #default="{ row }">
            <el-tag :type="row.locality === 'OLLAMA_LOCAL' ? 'success' : 'warning'" size="small">
              {{ row.locality === 'OLLAMA_LOCAL' ? '本地' : '云端' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Token数" width="90">
          <template #default="{ row }">
            {{ row.inputTokens ? (row.inputTokens + (row.outputTokens || 0)) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="延迟" width="80">
          <template #default="{ row }">
            {{ row.latencyMs ? row.latencyMs + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
        <el-table-column label="脱敏字段" width="150">
          <template #default="{ row }">
            <template v-if="row.scrubbedFields && row.scrubbedFields !== '[]'">
              <el-tag v-for="f in parseScrubbedFields(row.scrubbedFields)" :key="f" size="small" type="info" style="margin: 1px">
                {{ f }}
              </el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchLogs"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getAiAuditLogs, getAiAuditStats, type AuditLogParams } from '@/api/ai'

interface AuditLog {
  id: number
  projectId: number | null
  taskType: string
  provider: string
  model: string
  inputTokens: number | null
  outputTokens: number | null
  latencyMs: number | null
  success: boolean
  errorCode: string | null
  errorMessage: string | null
  scrubbedFields: string | null
  locality: string
  createdAt: string
}

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)

const filters = reactive({
  taskType: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const stats = reactive({
  totalCalls: 0,
  successRate: 0,
  avgLatencyMs: 0,
  byLocality: {} as Record<string, number>
})

const localCallsPercent = computed(() => {
  const local = stats.byLocality?.['OLLAMA_LOCAL'] ?? 0
  const total = stats.totalCalls || 1
  return ((local / total) * 100).toFixed(0)
})

function taskTypeLabel(type: string) {
  const map: Record<string, string> = {
    'catalog': '目录生成',
    'draft': '初稿生成',
    'proofread': '校对',
    'pre-review': '预评审',
    'compliance': '合规检查',
    'opinion-summary': '意见汇总',
    'stage-readiness': '阶段评估',
    'archive-advice': '归档建议',
    'change-impact': '变更分析',
    'CHAT': 'AI对话'
  }
  return map[type] || type
}

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

function parseScrubbedFields(fields: string): string[] {
  try {
    // 尝试解析 JSON 数组格式 ["装备型号","战术指标"]
    const parsed = JSON.parse(fields)
    if (Array.isArray(parsed)) return parsed
    // 兼容 [装备型号, 战术指标] 格式
    return fields.replace(/[\[\]"]/g, '').split(',').map(s => s.trim()).filter(Boolean)
  } catch {
    return []
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const params: AuditLogParams = {
      taskType: filters.taskType || undefined,
      page: pagination.page,
      size: pagination.size
    }
    if (dateRange.value) {
      params.from = dateRange.value[0]
      params.to = dateRange.value[1]
    }
    const res = await getAiAuditLogs(params)
    logs.value = res.data.data?.records || []
    pagination.total = res.data.data?.total || 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await getAiAuditStats()
    if (res.data?.data) {
      stats.totalCalls = res.data.data.totalCalls ?? 0
      stats.successRate = res.data.data.successRate ?? 0
      stats.avgLatencyMs = res.data.data.avgLatencyMs ?? 0
      stats.byLocality = res.data.data.byLocality ?? {}
    }
  } catch { /* ignore */ }
}

function resetFilters() {
  filters.taskType = ''
  dateRange.value = null
  pagination.page = 1
  fetchLogs()
  fetchStats()
}

onMounted(() => {
  fetchLogs()
  fetchStats()
})
</script>

<style scoped>
.audit-log-page { max-width: 1400px; }
.audit-log-page h2 { margin-bottom: 16px; }
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.filter-card { margin-bottom: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.text-muted { color: #c0c4cc; }
</style>
