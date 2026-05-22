<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
        <h3 style="display:inline;margin-left:16px">完整性检查</h3>
      </div>
      <div class="header-actions">
        <el-select v-model="docLedgerId" placeholder="选择文档" style="width:280px" filterable @change="handleDocChange">
          <el-option v-for="d in docList" :key="d.id" :label="`${d.docCode || ''} ${d.docName}`" :value="d.id" />
        </el-select>
        <el-button type="primary" :loading="checking" @click="runCheck" :disabled="!docLedgerId">
          <el-icon><CaretRight /></el-icon>运行检查
        </el-button>
      </div>
    </div>

    <!-- Latest Check Summary -->
    <el-row :gutter="16" v-if="latestResult">
      <el-col :span="8">
        <el-card shadow="never">
          <div class="score-display">
            <el-progress type="circle" :percentage="latestResult.score || 0" :color="scoreColor(latestResult.score || 0)" :width="120" />
            <div class="score-label">完整性评分</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header><span>检查摘要</span></template>
          <CompletenessProgressBar
            :passed="latestResult.passedItems || 0"
            :warnings="latestResult.warningItems || 0"
            :errors="latestResult.errorItems || 0"
            :total="latestResult.totalItems || 1"
          />
          <el-descriptions :column="3" border size="small" style="margin-top:12px">
            <el-descriptions-item label="检查时间">{{ latestResult.checkedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="总检查项">{{ latestResult.totalItems }}</el-descriptions-item>
            <el-descriptions-item label="通过率">{{ latestResult.score }}%</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <!-- Detail Items -->
    <el-card shadow="never" style="margin-top:16px" v-if="checkItems.length > 0">
      <template #header>
        <span>检查明细</span>
        <el-tag size="small" style="margin-left:8px" type="danger">{{ errorCount }} 错误</el-tag>
        <el-tag size="small" style="margin-left:4px" type="warning">{{ warningCount }} 警告</el-tag>
        <el-tag size="small" style="margin-left:4px" type="success">{{ passCount }} 通过</el-tag>
      </template>
      <el-table :data="checkItems" border stripe>
        <el-table-column label="严重度" width="90">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)" size="small">{{ severityLabel(row.severity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="章节" width="160">
          <template #default="{ row }">{{ row.chapterNumber }} {{ row.chapterTitle }}</template>
        </el-table-column>
        <el-table-column prop="description" label="检查项描述" min-width="250" />
        <el-table-column label="标准参考" width="160">
          <template #default="{ row }">
            <KnowledgeCardPopover
              v-if="row.standardRef"
              :keyword="row.standardRef"
              :label="row.standardRef"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.missingFields && row.missingFields > 0" link type="warning" size="small">
              查看 {{ row.missingFields }} 缺失项
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- History -->
    <el-card shadow="never" style="margin-top:16px" v-if="history.length > 0">
      <template #header><span>历史记录</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="h in history"
          :key="h.id"
          :timestamp="h.checkedAt"
          placement="top"
          :color="scoreColor(h.score || 0)"
        >
          <span>
            得分: {{ h.score }}% |
            通过 {{ h.passedItems }}/{{ h.totalItems }} |
            警告 {{ h.warningItems }} | 错误 {{ h.errorItems }}
          </span>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-empty v-if="!latestResult && !checking" description="选择文档并运行完整性检查" :image-size="80" style="margin-top:40px" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CaretRight } from '@element-plus/icons-vue'
import { checkDocument, getCheckHistory, type CompletenessCheckResult, type CheckItem } from '@/api/completeness'
import { getKanbanData, type DocLedgerItem } from '@/api/doc-ledger'
import CompletenessProgressBar from '@/components/CompletenessProgressBar.vue'
import KnowledgeCardPopover from '@/components/KnowledgeCardPopover.vue'

const route = useRoute()
const projectId = Number(route.params.projectId)

const docList = ref<DocLedgerItem[]>([])
const docLedgerId = ref<number | null>(null)
const checking = ref(false)
const latestResult = ref<CompletenessCheckResult | null>(null)
const checkItems = ref<CheckItem[]>([])
const history = ref<CompletenessCheckResult[]>([])

const errorCount = computed(() => checkItems.value.filter(i => i.severity === 'ERROR').length)
const warningCount = computed(() => checkItems.value.filter(i => i.severity === 'WARNING').length)
const passCount = computed(() => checkItems.value.filter(i => i.severity === 'PASS').length)

function scoreColor(s: number) {
  return s >= 90 ? '#67c23a' : s >= 70 ? '#e6a23c' : '#f56c6c'
}

function severityType(s: string) {
  const map: Record<string, string> = { ERROR: 'danger', WARNING: 'warning', PASS: 'success' }
  return map[s] || 'info'
}

function severityLabel(s: string) {
  const map: Record<string, string> = { ERROR: '错误', WARNING: '警告', PASS: '通过' }
  return map[s] || s
}

async function loadDocList() {
  try {
    const res = await getKanbanData(projectId)
    const allDocs = Object.values(res.data.data || {}).flat() as DocLedgerItem[]
    docList.value = allDocs
  } catch { /* ignore */ }
}

async function runCheck() {
  if (!docLedgerId.value) { ElMessage.warning('请选择文档'); return }
  checking.value = true
  try {
    const res = await checkDocument(projectId, docLedgerId.value, 1)
    const result = res.data.data
    latestResult.value = result

    // Parse detail items
    if (result.detailJson) {
      try {
        checkItems.value = JSON.parse(result.detailJson)
      } catch { checkItems.value = [] }
    }

    ElMessage.success(`检查完成，得分: ${result.score}%`)
    loadHistory()
  } catch {
    ElMessage.error('检查失败')
  }
  checking.value = false
}

async function loadHistory() {
  if (!docLedgerId.value) return
  try {
    const res = await getCheckHistory(docLedgerId.value)
    history.value = res.data.data || []
  } catch { /* ignore */ }
}

function handleDocChange() {
  latestResult.value = null
  checkItems.value = []
  history.value = []
}

onMounted(loadDocList)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-actions { display: flex; gap: 8px; }
.score-display { text-align: center; padding: 16px; }
.score-label { margin-top: 8px; font-size: 14px; color: #606266; }
</style>
