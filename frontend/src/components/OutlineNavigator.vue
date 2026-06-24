<template>
  <div class="outline-nav">
    <div class="on-header">大纲导航</div>
    <div class="on-list">
      <div v-for="item in outline" :key="item.id"
        class="on-item"
        :class="{ active: activeId === item.id }"
        :style="{ paddingLeft: (item.level * 12 + 4) + 'px' }"
        @click="$emit('navigate', item.id)">
        <span class="on-text">{{ item.text }}</span>
      </div>
      <div v-if="outline.length === 0" class="on-empty">暂无大纲，开始撰写后自动提取</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OutlineItem } from '@/utils/editorPatch'

const props = defineProps<{
  items: OutlineItem[]
  activeId: string | null
}>()

defineEmits<{ (e: 'navigate', id: string): void }>()

const outline = computed(() => props.items || [])
</script>

<style scoped>
.outline-nav { width: 160px; min-width: 120px; overflow-y: auto; height: 100%; border-right: 1px solid var(--el-border-color-light); padding: 8px 0; background: var(--el-fill-color-lighter); flex-shrink: 0; }
.on-header { font-size: 12px; font-weight: 600; color: var(--el-text-color-secondary); padding: 4px 10px 8px; }
.on-list { display: flex; flex-direction: column; }
.on-item { padding: 4px 8px; cursor: pointer; font-size: 12px; border-radius: 3px; margin: 0 4px; transition: background .15s; line-height: 1.4; }
.on-item:hover { background: var(--el-fill-color); }
.on-item.active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-weight: 600; }
.on-text { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.on-empty { padding: 12px 8px; font-size: 11px; color: #c0c4cc; text-align: center; }
</style>
