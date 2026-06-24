<template>
  <div class="doc-toolbar">
    <span class="dt-title">📄 {{ docName }}</span>
    <div class="dt-center">
      <span class="dt-stats">{{ chapters.length }}章 · {{ filledCount }}完成</span>
      <el-button v-if="chapters.length>0 && filledCount<chapters.length" size="small" type="success" @click="$emit('writeAll')">AI写全部</el-button>
    </div>
    <div class="dt-actions">
      <el-button size="small" :loading="refreshing" @click="$emit('refresh')">刷新</el-button>
      <el-button size="small" type="primary" @click="$emit('export')">导出DOCX</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  docName: string
  chapters: any[]
  refreshing?: boolean
}>()

defineEmits<{
  (e: 'writeAll'): void
  (e: 'refresh'): void
  (e: 'export'): void
}>()

const filledCount = computed(() => props.chapters.filter(c => c.content && c.content.length > 50).length)
</script>

<style scoped>
.doc-toolbar { display:flex; justify-content:space-between; align-items:center; padding:10px 14px; border-bottom:1px solid var(--el-border-color-light); flex-shrink:0; gap:8px; }
.dt-title { font-weight:600; font-size:14px; white-space:nowrap; }
.dt-center { display:flex; align-items:center; gap:8px; }
.dt-stats { font-size:11px; color:var(--el-text-color-secondary); white-space:nowrap; }
.dt-actions { display:flex; gap:6px; }
</style>
