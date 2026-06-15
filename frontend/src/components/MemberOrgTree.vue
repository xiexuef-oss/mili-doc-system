<template>
  <div class="org-tree">
    <div class="org-tree-header">
      <h4>项目组织架构（两师系统）</h4>
    </div>
    <el-tree
      :data="treeData"
      :props="{ children: 'children', label: 'label' }"
      node-key="id"
      default-expand-all
      :expand-on-click-node="false"
    >
      <template #default="{ data }">
        <span class="tree-node">
          <el-tag
            :type="lineTagType(data.line)"
            size="small"
            effect="plain"
            style="margin-right: 8px"
          >
            {{ lineLabel(data.line) }}
          </el-tag>
          <span class="node-name">{{ data.label }}</span>
          <el-tag v-if="data.position" size="small" type="info" effect="plain" style="margin-left: 6px">
            {{ data.position }}
          </el-tag>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProjectMemberItem } from '@/api/project-member'

const props = defineProps<{
  members: ProjectMemberItem[]
}>()

interface TreeNode {
  id: string
  label: string
  line: string
  position: string
  children: TreeNode[]
}

const LINE_MAP: Record<string, string> = {
  TECHNICAL: '技术线',
  ADMINISTRATIVE: '行政线',
  QUALITY: '质量线',
  CRAFT: '工艺线'
}

const treeData = computed<TreeNode[]>(() => {
  const lines: Record<string, TreeNode> = {
    TECHNICAL: { id: 'line-tech', label: '技术指挥线', line: 'TECHNICAL', position: '', children: [] },
    ADMINISTRATIVE: { id: 'line-admin', label: '行政指挥线', line: 'ADMINISTRATIVE', position: '', children: [] },
    QUALITY: { id: 'line-qual', label: '质量线', line: 'QUALITY', position: '', children: [] },
    CRAFT: { id: 'line-craft', label: '工艺线', line: 'CRAFT', position: '', children: [] }
  }

  for (const m of props.members) {
    if (!m.memberLine || m.status !== 'ACTIVE') continue
    const line = lines[m.memberLine]
    if (!line) continue
    line.children.push({
      id: `member-${m.id}`,
      label: m.userName || `用户${m.userId}`,
      line: m.memberLine,
      position: positionLabel(m.memberPosition || m.roleInProject),
      children: []
    })
  }

  // 只返回有成员的线
  return Object.values(lines).filter(l => l.children.length > 0)
})

function lineLabel(line: string) {
  return LINE_MAP[line] || line
}

function lineTagType(line: string) {
  switch (line) {
    case 'TECHNICAL': return 'primary'
    case 'ADMINISTRATIVE': return 'success'
    case 'QUALITY': return 'warning'
    case 'CRAFT': return 'info'
    default: return ''
  }
}

function positionLabel(code: string) {
  const map: Record<string, string> = {
    CHIEF_DESIGNER: '总设计师',
    DEPUTY_CHIEF_DESIGNER: '副总设计师',
    CHIEF_DESIGNER_ENGINEER: '主任设计师',
    LEAD_DESIGNER_ENGINEER: '主管设计师',
    DESIGNER_ENGINEER: '设计师',
    CHIEF_COMMANDER: '总指挥',
    DEPUTY_CHIEF_COMMANDER: '副总指挥',
    PROJECT_OFFICE_DIRECTOR: '项目办主任',
    PLAN_SUPERVISOR: '计划主管',
    PROJECT_ASSISTANT: '项目助理',
    CHIEF_QUALITY_ENGINEER: '总质量师',
    QUALITY_SUPERVISOR: '质量主管',
    QUALITY_ENGINEER: '质量师',
    CHIEF_PROCESS_ENGINEER: '总工艺师',
    PROCESS_ENGINEER: '工艺师'
  }
  return map[code] || code
}
</script>

<style scoped>
.org-tree { background: #fafafa; padding: 16px; border-radius: 4px; margin-bottom: 16px; }
.org-tree-header { margin-bottom: 8px; }
.org-tree-header h4 { margin: 0; color: #303133; }
.tree-node { display: flex; align-items: center; font-size: 14px; }
.node-name { font-weight: 500; }
</style>
