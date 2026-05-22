<template>
  <div class="process-pipeline">
    <div class="pipeline-track">
      <div
        v-for="(stage, idx) in stages"
        :key="stage.code"
        class="pipeline-node"
        :class="{
          'is-current': stage.code === currentStage,
          'is-completed': stage.completed,
          'is-active': stage.active
        }"
      >
        <div class="node-dot">
          <el-icon v-if="stage.completed"><Check /></el-icon>
          <span v-else>{{ idx + 1 }}</span>
        </div>
        <div class="node-label">{{ stage.name || stage.code }}</div>
        <div v-if="stage.docCount !== undefined" class="node-meta">
          {{ stage.releasedDocs || 0 }}/{{ stage.docCount }} 文档
        </div>
        <div v-if="stage.completionRate !== undefined" class="node-rate">
          {{ stage.completionRate }}%
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue'

defineProps<{
  stages: Array<{
    code: string
    name?: string
    completed?: boolean
    active?: boolean
    docCount?: number
    releasedDocs?: number
    completionRate?: number
  }>
  currentStage?: string
}>()
</script>

<style scoped>
.process-pipeline { padding: 12px 0; overflow-x: auto; }
.pipeline-track { display: flex; align-items: flex-start; gap: 0; min-width: 600px; }
.pipeline-node {
  flex: 1; text-align: center; position: relative; padding: 0 8px;
}
.pipeline-node::after {
  content: ''; position: absolute; top: 14px; left: 60%; width: 80%;
  height: 3px; background: #dcdfe6; z-index: 0;
}
.pipeline-node:last-child::after { display: none; }
.pipeline-node.is-completed::after { background: #67c23a; }
.pipeline-node.is-active::after { background: #409eff; }

.node-dot {
  width: 32px; height: 32px; border-radius: 50%; background: #dcdfe6;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; color: #fff; position: relative; z-index: 1;
}
.pipeline-node.is-current .node-dot { background: #409eff; }
.pipeline-node.is-completed .node-dot { background: #67c23a; }
.pipeline-node.is-active .node-dot { background: #e6a23c; }

.node-label { margin-top: 6px; font-size: 13px; font-weight: 500; color: #303133; }
.node-meta { font-size: 11px; color: #909399; margin-top: 2px; }
.node-rate { font-size: 12px; font-weight: 600; color: #409eff; margin-top: 2px; }
</style>
