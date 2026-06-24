<template>
  <div class="quality-panel" v-if="score">
    <div class="qp-header">
      <span class="qp-title">文档质量</span>
      <el-tag :type="gradeType" size="large">{{ score.grade }}</el-tag>
    </div>

    <!-- Main score -->
    <div class="qp-score-ring">
      <div class="qp-ring">
        <svg viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="42" fill="none" stroke="#ebeef5" stroke-width="8"/>
          <circle cx="50" cy="50" r="42" fill="none"
            :stroke="scoreColor" stroke-width="8" stroke-linecap="round"
            :stroke-dasharray="circumference"
            :stroke-dashoffset="dashOffset"
            transform="rotate(-90 50 50)"/>
        </svg>
        <div class="qp-ring-text">
          <span class="qp-score-num">{{ score.totalScore }}</span>
          <span class="qp-score-label">分</span>
        </div>
      </div>
    </div>

    <!-- Dimension breakdown -->
    <div class="qp-dims">
      <div class="qp-dim" v-for="d in dimensions" :key="d.key">
        <span class="qp-dim-label">{{ d.label }}</span>
        <el-progress :percentage="d.value" :color="dimColor(d.value)" :stroke-width="6"/>
      </div>
    </div>

    <!-- AI_META chapter scores -->
    <div v-if="score.aiMetaScores && Object.keys(score.aiMetaScores).length" class="qp-meta">
      <div class="qp-section-title">各章完成度</div>
      <div v-for="(s, ch) in score.aiMetaScores" :key="ch" class="qp-chapter">
        <span class="qp-chapter-name">{{ ch }}</span>
        <span class="qp-chapter-score" :style="{color: dimColor(s)}">{{ s }}%</span>
      </div>
    </div>

    <!-- Review issues summary -->
    <div v-if="review" class="qp-review">
      <div class="qp-section-title">审查结果</div>
      <div v-if="review.forbiddenWordIssues?.length" class="qp-issue error">
        禁用词: {{ review.forbiddenWordIssues.length }}处
      </div>
      <div v-if="review.toVerifyItems?.length" class="qp-issue warn">
        待核实: {{ review.toVerifyItems.length }}项
      </div>
      <div v-if="review.missingItems?.length" class="qp-issue info">
        缺项: {{ review.missingItems.length }}项
      </div>
      <div v-if="review.totalIssues === 0" class="qp-issue clean">全部通过</div>
    </div>

    <div v-loading="loading" class="qp-loading-area"/>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import request from '@/api/index'

const props = defineProps<{ ledgerId: number }>()

interface QualityScore {
  totalScore: number; grade: string; chapterCompleteness: number
  contentCoverage: number; dataAccuracy: number; formatCompliance: number
  readability: number; aiMetaScores: Record<string, number>
}
interface ReviewReport {
  forbiddenWordIssues: any[]; toVerifyItems: any[]; missingItems: any[]
  totalIssues: number
}

const score = ref<QualityScore | null>(null)
const review = ref<ReviewReport | null>(null)
const loading = ref(false)

const circumference = 2 * Math.PI * 42
const dashOffset = computed(() => circumference * (1 - (score.value?.totalScore || 0) / 100))

const gradeType = computed(() => {
  const s = score.value?.totalScore || 0
  if (s >= 80) return 'success'
  if (s >= 60) return 'warning'
  return 'danger'
})
const scoreColor = computed(() => {
  const s = score.value?.totalScore || 0
  if (s >= 80) return '#67c23a'
  if (s >= 60) return '#e6a23c'
  return '#f56c6c'
})

const dimensions = computed(() => [
  { key: 'chapterCompleteness', label: '章节完整性', value: score.value?.chapterCompleteness || 0 },
  { key: 'contentCoverage', label: '内容覆盖度', value: score.value?.contentCoverage || 0 },
  { key: 'dataAccuracy', label: '数据准确性', value: score.value?.dataAccuracy || 0 },
  { key: 'formatCompliance', label: '格式规范性', value: score.value?.formatCompliance || 0 },
  { key: 'readability', label: '可读性', value: score.value?.readability || 0 },
])

function dimColor(v: number) {
  if (v >= 80) return '#67c23a'
  if (v >= 60) return '#e6a23c'
  return '#f56c6c'
}

async function load() {
  if (!props.ledgerId) return
  loading.value = true
  try {
    const [qRes, rRes] = await Promise.all([
      request.get(`/reliability/documents/${props.ledgerId}/quality-score`),
      request.get(`/reliability/documents/${props.ledgerId}/auto-review`),
    ])
    score.value = qRes.data?.data
    review.value = rRes.data?.data
  } catch { /* ignore */ }
  finally { loading.value = false }
}

watch(() => props.ledgerId, load)
onMounted(load)
</script>

<style scoped>
.quality-panel { padding: 16px; background: #fff; border-radius: 8px; border: 1px solid #ebeef5; }
.qp-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.qp-title { font-size: 15px; font-weight: 600; }
.qp-score-ring { display: flex; justify-content: center; margin: 8px 0 16px; }
.qp-ring { position: relative; width: 100px; height: 100px; }
.qp-ring-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); text-align: center; }
.qp-score-num { font-size: 28px; font-weight: 700; }
.qp-score-label { font-size: 12px; color: #909399; display: block; }
.qp-dims { margin-bottom: 12px; }
.qp-dim { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.qp-dim-label { width: 80px; font-size: 12px; color: #606266; text-align: right; flex-shrink: 0; }
.qp-dim :deep(.el-progress) { flex: 1; }
.qp-meta { margin: 12px 0; }
.qp-section-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.qp-chapter { display: flex; justify-content: space-between; padding: 4px 0; font-size: 12px; border-bottom: 1px solid #f5f5f5; }
.qp-chapter-name { color: #606266; }
.qp-review { margin-top: 12px; }
.qp-issue { padding: 4px 8px; border-radius: 4px; font-size: 12px; margin-bottom: 4px; }
.qp-issue.error { background: #fef0f0; color: #f56c6c; }
.qp-issue.warn { background: #fdf6ec; color: #e6a23c; }
.qp-issue.info { background: #f4f4f5; color: #909399; }
.qp-issue.clean { background: #f0f9eb; color: #67c23a; }
</style>
