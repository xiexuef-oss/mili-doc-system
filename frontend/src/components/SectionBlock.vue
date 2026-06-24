<template>
  <div :id="'section-'+section.id" class="section-block" :class="{active:active}">
    <div class="sb-heading" :style="{paddingLeft:(section.chapterLevel||1)*8+'px'}">
      <span class="sb-num">{{ section.chapterNumber }}</span>
      <span class="sb-title">{{ section.chapterTitle }}</span>
      <span class="sb-status">
        <el-tag v-if="hasContent" size="small" type="success" effect="plain">✓</el-tag>
        <el-tag v-else size="small" type="info" effect="plain">待写</el-tag>
      </span>
    </div>
    <div class="sb-body">
      <div v-if="editing" class="sb-edit">
        <el-input v-model="editText" type="textarea" :rows="6" @keyup="onEditKeyup" />
        <div class="sb-edit-actions">
          <el-button size="small" type="primary" @click="save">保存</el-button>
          <el-button size="small" @click="cancel">取消</el-button>
          <div style="flex:1"/>
          <el-button size="small" @click="$emit('aiEdit','rewrite')">🔄 重写</el-button>
          <el-button size="small" @click="$emit('aiEdit','expand')">📝 扩写</el-button>
          <el-button size="small" @click="$emit('aiEdit','shorten')">📏 缩短</el-button>
          <el-button size="small" @click="$emit('aiEdit','polish')">✨ 优化</el-button>
        </div>
      </div>
      <div v-else class="sb-content" @dblclick="startEdit">
        <div v-if="hasContent" v-html="renderedContent"/>
        <span v-else class="sb-placeholder">双击编辑，或点击上方按钮 AI 生成</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  section: any
  active: boolean
}>()

const emit = defineEmits<{
  (e: 'aiEdit', action: string): void
  (e: 'save', content: string): void
}>()

const editing = ref(false)
const editText = ref('')
const saveTimer = ref<ReturnType<typeof setTimeout>|null>(null)

const hasContent = computed(() => props.section.content && props.section.content.length > 50)
const renderedContent = computed(() => {
  const text = props.section.content || ''
  return text.replace(/\n\n/g,'</p><p>').replace(/\n/g,'<br>').replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>').replace(/^/,'<p>').replace(/$/,'</p>')
})

watch(() => props.section.content, (val) => { if (!editing.value) editText.value = val || '' })

function startEdit() { editing.value = true; editText.value = props.section.content || '' }
function cancel() { editing.value = false }
function save() { emit('save', editText.value); editing.value = false }
function onEditKeyup() {
  if (saveTimer.value) clearTimeout(saveTimer.value)
  saveTimer.value = setTimeout(() => emit('save', editText.value), 2000)
}
</script>

<style scoped>
.section-block { scroll-margin-top:20px; margin-bottom:4px; border-radius:6px; border:2px solid transparent; transition:border-color .2s; }
.section-block.active { border-color:var(--el-color-primary-light-5); background:var(--el-color-primary-light-9); }
.sb-heading { display:flex; align-items:center; gap:6px; padding:8px 12px; cursor:pointer; border-radius:6px 6px 0 0; }
.sb-heading:hover { background:var(--el-fill-color-light); }
.sb-num { color:var(--el-color-primary); font-weight:600; font-size:13px; white-space:nowrap; }
.sb-title { font-weight:600; font-size:14px; color:#303133; }
.sb-status { margin-left:auto; flex-shrink:0; }
.sb-body { padding:0 12px 8px; }
.sb-content { font-size:13px; line-height:1.9; color:#303133; cursor:text; min-height:24px; padding:4px 0; }
.sb-content:hover { background:var(--el-fill-color-lighter); border-radius:4px; }
.sb-placeholder { color:var(--el-text-color-placeholder); font-style:italic; font-size:12px; }
.sb-edit-actions { display:flex; gap:6px; margin-top:8px; flex-wrap:wrap; }
.sb-edit :deep(textarea) { font-size:13px; line-height:1.8; }
</style>
