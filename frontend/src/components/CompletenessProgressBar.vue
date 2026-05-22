<template>
  <div class="completeness-bar">
    <div class="bar-stats">
      <span class="stat passed">通过 {{ passed }}</span>
      <span class="stat warning">警告 {{ warnings }}</span>
      <span class="stat error">错误 {{ errors }}</span>
      <span class="stat score">得分 {{ score }}%</span>
    </div>
    <div class="bar-track">
      <div class="bar-segment passed" :style="{ width: passedPct + '%' }" />
      <div class="bar-segment warning" :style="{ width: warningPct + '%' }" />
      <div class="bar-segment error" :style="{ width: errorPct + '%' }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  passed: number
  warnings: number
  errors: number
  total: number
}>()

const score = computed(() => {
  if (props.total === 0) return 0
  return Math.round((props.passed / props.total) * 100)
})

const passedPct = computed(() => props.total ? (props.passed / props.total) * 100 : 0)
const warningPct = computed(() => props.total ? (props.warnings / props.total) * 100 : 0)
const errorPct = computed(() => props.total ? (props.errors / props.total) * 100 : 0)
</script>

<style scoped>
.completeness-bar { width: 100%; }
.bar-stats { display: flex; gap: 12px; font-size: 12px; margin-bottom: 4px; }
.bar-stats .stat.passed { color: #67c23a; }
.bar-stats .stat.warning { color: #e6a23c; }
.bar-stats .stat.error { color: #f56c6c; }
.bar-stats .stat.score { color: #409eff; font-weight: 600; margin-left: auto; }
.bar-track { display: flex; height: 8px; border-radius: 4px; overflow: hidden; background: #ebeef5; }
.bar-segment { height: 100%; transition: width 0.3s; }
.bar-segment.passed { background: #67c23a; }
.bar-segment.warning { background: #e6a23c; }
.bar-segment.error { background: #f56c6c; }
</style>
