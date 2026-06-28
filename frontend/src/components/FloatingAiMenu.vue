<template>
  <transition name="fade">
    <div v-if="visible" class="floating-ai-menu" :style="positionStyle">
      <div class="fam-title">AI 快捷操作</div>
      <div class="fam-items">
        <button class="fam-item" @click="emitAction('continue')">✍️ 继续写</button>
        <button class="fam-item" @click="emitAction('insert_paragraph')">📄 插入一段</button>
        <button class="fam-item" @click="emitAction('insert_heading')">📑 插入标题</button>
        <button class="fam-item" @click="emitAction('insert_list')">📋 插入列表</button>
        <button class="fam-item" @click="emitAction('insert_table')">▦ 插入表格</button>
        <button class="fam-item" @click="emitAction('insert_summary')">📊 插入小结</button>
        <button class="fam-item" @click="emitAction('insert_action_items')">✅ 插入行动项</button>
      </div>
      <div class="fam-divider"></div>
      <div class="fam-custom">
        <el-input v-model="customText" size="small" placeholder="自定义生成..." @keyup.enter="emitCustom"/>
        <el-button size="small" type="primary" @click="emitCustom">生成</el-button>
      </div>
      <button class="fam-close" @click="visible = false">✕</button>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'

const props = defineProps<{
  editor: any
}>()

const emit = defineEmits<{
  (e: 'action', type: string, context?: string): void
}>()

const visible = ref(false)
const customText = ref('')
const menuPos = ref({ x: 0, y: 0 })

const positionStyle = computed(() => ({
  left: `${menuPos.value.x}px`,
  top: `${menuPos.value.y}px`
}))

function emitAction(type: string) {
  visible.value = false
  emit('action', type)
}

function emitCustom() {
  if (!customText.value.trim()) return
  visible.value = false
  emit('action', 'custom', customText.value.trim())
  customText.value = ''
}

function showAt(x: number, y: number) {
  // Clamp position to prevent overflow scrolling
  const vw = window.innerWidth
  const vh = window.innerHeight
  const menuW = 200, menuH = 300
  menuPos.value = {
    x: Math.min(x, vw - menuW - 10),
    y: Math.min(y, vh - menuH - 10)
  }
  visible.value = true
}

function hide() {
  visible.value = false
}

// Watch editor selection to auto-show/hide
watch(() => props.editor, (ed) => {
  if (!ed) return
  const checkSelection = () => {
    const { empty } = ed.state.selection
    if (empty && !visible.value) {
      // Calculate position near cursor
      try {
        const { from } = ed.state.selection
        const coords = ed.view.coordsAtPos(from)
        const container = ed.view.dom.closest('.cp-content')?.getBoundingClientRect()
        if (container) {
          showAt(coords.left - container.left + 20, coords.bottom - container.top + 8)
        }
      } catch { /* ignore */ }
    }
  }
  ed.on('selectionUpdate', checkSelection)
}, { immediate: true })

defineExpose({ showAt, hide })
</script>

<style scoped>
.floating-ai-menu {
  position: absolute;
  z-index: 200;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0,0,0,.15);
  padding: 10px 12px;
  min-width: 180px;
}
.fam-title { font-size: 12px; font-weight: 600; color: #909399; margin-bottom: 6px; padding: 0 4px; }
.fam-items { display: flex; flex-wrap: wrap; gap: 6px; }
.fam-item {
  padding: 5px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #606266;
  transition: all .15s;
  white-space: nowrap;
}
.fam-item:hover { background: #ecf5ff; border-color: #b3d8ff; color: #409eff; }
.fam-divider { height: 1px; background: #e4e7ed; margin: 8px 0; }
.fam-custom { display: flex; gap: 6px; align-items: center; }
.fam-close {
  position: absolute; top: 4px; right: 6px;
  width: 18px; height: 18px; border: none; background: transparent;
  cursor: pointer; font-size: 11px; color: #c0c4cc;
}
.fam-close:hover { color: #f56c6c; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s, transform .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
