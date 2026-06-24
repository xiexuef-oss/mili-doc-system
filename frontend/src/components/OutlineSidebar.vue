<template>
  <div class="outline-sidebar">
    <div class="os-header">章节导航</div>
    <div class="os-list">
      <div v-for="ch in chapters" :key="ch.id"
        class="os-item" :class="{active:activeId===ch.id}"
        :style="{paddingLeft:(ch.chapterLevel||1)*8+'px'}"
        @click="$emit('select',ch)">
        <span class="os-num">{{ ch.chapterNumber }}</span>
        <span class="os-name">{{ ch.chapterTitle }}</span>
        <el-tag v-if="ch.content && ch.content.length>50" size="small" type="success" effect="plain" style="transform:scale(0.65)">✓</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{ chapters: any[] }>()
defineEmits<{ (e: 'select', ch: any): void }>()

const activeId = ref<number|null>(null)
let observer: IntersectionObserver | null = null

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        const id = entry.target.id.replace('section-','')
        activeId.value = Number(id)
      }
    }
  }, { rootMargin: '-20% 0px -70% 0px' })
  // Observe all section elements
  for (const ch of props.chapters) {
    const el = document.getElementById('section-'+ch.id)
    if (el) observer.observe(el)
  }
})

onUnmounted(() => observer?.disconnect())
</script>

<style scoped>
.outline-sidebar { width:170px; overflow-y:auto; border-right:1px solid var(--el-border-color-light); padding:8px 0; background:var(--el-fill-color-lighter); flex-shrink:0; }
.os-header { font-size:12px; font-weight:600; color:var(--el-text-color-secondary); padding:4px 10px 8px; }
.os-list { display:flex; flex-direction:column; }
.os-item { display:flex; align-items:center; gap:3px; padding:3px 8px; cursor:pointer; font-size:12px; border-radius:3px; margin:0 4px; transition:background .15s; }
.os-item:hover { background:var(--el-fill-color); }
.os-item.active { background:var(--el-color-primary-light-9); color:var(--el-color-primary); }
.os-num { color:var(--el-text-color-secondary); font-weight:500; flex-shrink:0; font-size:11px; }
.os-name { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
</style>
