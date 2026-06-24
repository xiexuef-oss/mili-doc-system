<template>
  <div class="chapter-tree-viewer">
    <el-input
      v-model="filterText"
      placeholder="搜索章节..."
      size="small"
      clearable
      style="margin-bottom: 8px"
    />
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="treeProps"
      node-key="id"
      :filter-node-method="filterNode"
      :default-expanded-keys="defaultExpanded"
      :expand-on-click-node="true"
      :draggable="draggable"
      :allow-drag="allowDrag"
      :allow-drop="allowDrop"
      highlight-current
      @node-click="handleNodeClick"
      @node-drag-end="handleDragEnd"
    >
      <template #default="{ data }">
        <span class="tree-node" :class="fillClass(data)">
          <el-tag v-if="data.chapterNumber" size="small" type="info" class="node-num">{{ data.chapterNumber }}</el-tag>
          <span class="node-title">{{ data.chapterTitle || data.title }}</span>
          <el-tag v-if="data.fillStatus" size="small" :type="fillTagType(data.fillStatus)" class="node-status">
            {{ fillLabel(data.fillStatus) }}
          </el-tag>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ElTree } from 'element-plus'

const props = defineProps<{
  treeData: any[]
  defaultExpanded?: number[]
  draggable?: boolean
}>()

const emit = defineEmits<{
  (e: 'node-click', node: any): void
  (e: 'reorder', orderedIds: number[]): void
}>()

const treeRef = ref<InstanceType<typeof ElTree>>()
const filterText = ref('')

const treeProps = { children: 'children', label: 'chapterTitle' }

function filterNode(value: string, data: any) {
  if (!value) return true
  return (data.chapterTitle || data.title || '').includes(value)
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

function handleNodeClick(data: any) {
  emit('node-click', data)
}

function allowDrag(_node: any) {
  return props.draggable === true
}

function allowDrop(_draggingNode: any, _dropNode: any, type: string) {
  return type !== 'inner'
}

function handleDragEnd(_draggingNode: any, _dropNode: any, _dropType: string) {
  if (!props.draggable) return
  const orderedIds = collectOrderedIds(treeRef.value?.store.data || [])
  emit('reorder', orderedIds)
}

function collectOrderedIds(nodes: any[]): number[] {
  const ids: number[] = []
  for (const node of nodes) {
    ids.push(node.data?.id ?? node.id)
    if (node.childNodes && node.childNodes.length > 0) {
      ids.push(...collectOrderedIds(node.childNodes))
    }
  }
  return ids
}

function fillClass(data: any) {
  return {
    'node-filled': data.fillStatus === 'FILLED',
    'node-partial': data.fillStatus === 'PARTIAL',
    'node-empty': data.fillStatus === 'EMPTY' || !data.fillStatus
  }
}

function fillTagType(status: string) {
  const map: Record<string, string> = { FILLED: 'success', PARTIAL: 'warning', EMPTY: 'info' }
  return map[status] || 'info'
}

function fillLabel(status: string) {
  const map: Record<string, string> = { FILLED: '已完成', PARTIAL: '部分', EMPTY: '待填写' }
  return map[status] || status
}
</script>

<style scoped>
.chapter-tree-viewer { min-width: 240px; }
.tree-node { display: flex; align-items: center; gap: 6px; flex: 1; font-size: 13px; }
.node-num { flex-shrink: 0; }
.node-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.node-status { flex-shrink: 0; margin-left: auto; }

.node-filled .node-title { color: #67c23a; }
.node-partial .node-title { color: #e6a23c; }
.node-empty .node-title { color: #909399; }
</style>
