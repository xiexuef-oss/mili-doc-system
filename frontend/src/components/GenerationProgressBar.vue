<template>
  <div class="generation-progress">
    <el-steps :active="activePhaseIndex" align-center finish-status="success" process-status="process">
      <el-step
        v-for="(step, index) in phases"
        :key="index"
        :title="step.label"
        :description="activePhaseIndex === index ? step.description : ''"
      />
    </el-steps>
    <div class="progress-stats" v-if="showStats">
      <span class="stat-item">
        <strong>{{ current }}/{{ total }}</strong> 文档
      </span>
      <span class="stat-separator">|</span>
      <span class="stat-item">
        已完成 <el-tag size="small" type="success">{{ doneCount }}</el-tag>
      </span>
      <span class="stat-item" v-if="errorCount > 0">
        失败 <el-tag size="small" type="danger">{{ errorCount }}</el-tag>
      </span>
      <span class="stat-separator" v-if="etaText">|</span>
      <span class="stat-item eta" v-if="etaText">
        ⏱ 预计剩余 {{ etaText }}
      </span>
      <span class="stat-item current-doc" v-if="currentDocName">
        📝 {{ currentDocName }}
      </span>
    </div>
    <el-progress
      v-if="total > 0"
      :percentage="percentage"
      :status="progressStatus"
      :stroke-width="12"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const phases = [
  { label: '分析阶段', description: '加载检查清单...' },
  { label: '构建上下文', description: '组装项目信息...' },
  { label: '拓扑排序', description: '计算依赖关系...' },
  { label: '生成文档', description: 'AI 撰写中...' },
  { label: '后处理', description: '保存结果...' }
]

const phaseMap: Record<string, number> = {
  'ANALYZING': 0,
  'BUILDING_CONTEXT': 1,
  'TOPOLOGICAL_SORTING': 2,
  'GENERATING': 3,
  'POST_PROCESSING': 4
}

const props = withDefaults(defineProps<{
  currentPhase?: string
  current?: number
  total?: number
  doneCount?: number
  errorCount?: number
  currentDocName?: string
  etaSeconds?: number
  showStats?: boolean
}>(), {
  showStats: true
})

const activePhaseIndex = computed(() => {
  if (!props.currentPhase) return -1
  return phaseMap[props.currentPhase] ?? -1
})

const percentage = computed(() => {
  if (!props.total || props.total === 0) return 0
  return Math.round(((props.doneCount ?? props.current ?? 0) / props.total) * 100)
})

const progressStatus = computed(() => {
  if (props.errorCount && props.errorCount > 0) return 'exception'
  if (percentage.value >= 100) return 'success'
  return ''
})

const etaText = computed(() => {
  if (!props.etaSeconds || props.etaSeconds <= 0) return ''
  const mins = Math.floor(props.etaSeconds / 60)
  const secs = props.etaSeconds % 60
  if (mins > 0) return `${mins}分${secs}秒`
  return `${secs}秒`
})
</script>

<style scoped>
.generation-progress {
  padding: 8px 0;
}
.progress-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
}
.stat-separator { color: #dcdfe6; }
.eta { color: #409eff; }
.current-doc {
  color: #e6a23c;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
