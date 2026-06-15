<template>
  <div class="approval-flow">
    <h4 style="margin-bottom: 16px">签审流程</h4>
    <el-steps :active="activeIndex" finish-status="success" process-status="process" align-center>
      <el-step
        v-for="(step, idx) in steps"
        :key="idx"
        :title="stepLabel(step.approvalStep)"
        :description="stepDescription(step, idx)"
        :status="stepStatus(step)"
      />
    </el-steps>

    <!-- 当前审批操作 -->
    <div v-if="currentStep && !readonly" class="approval-action">
      <el-divider />
      <div class="action-row">
        <span>当前步骤：<strong>{{ currentStep.approvalStep }}</strong> — {{ positionLabel(currentStep.approverPosition) }}</span>
        <div class="action-buttons">
          <el-input
            v-model="opinion"
            placeholder="审批意见（可选）"
            style="width: 280px; margin-right: 12px"
            size="small"
          />
          <el-button type="success" :loading="submitting" @click="submit('APPROVED')">
            <el-icon><Check /></el-icon>批准
          </el-button>
          <el-button type="danger" :loading="submitting" @click="submit('REJECTED')">
            <el-icon><Close /></el-icon>驳回
          </el-button>
          <el-button :loading="submitting" @click="submit('RETURN')">
            退回修改
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Check, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface ApprovalStep {
  id: number
  docLedgerId: number
  approvalStep: string
  approverId?: number
  approverPosition: string
  approvalResult?: string
  approvalOpinion?: string
  approvedAt?: string
  sortOrder: number
}

const props = defineProps<{
  steps: ApprovalStep[]
  readonly?: boolean
}>()

const emit = defineEmits<{
  submit: [recordId: number, result: string, opinion: string]
}>()

const opinion = ref('')
const submitting = ref(false)

const activeIndex = computed(() => {
  const idx = props.steps.findIndex(s => !s.approvalResult)
  return idx >= 0 ? idx : props.steps.length
})

const currentStep = computed(() => {
  return props.steps.find(s => !s.approvalResult) || null
})

function stepStatus(step: ApprovalStep) {
  if (!step.approvalResult) return 'process'
  if (step.approvalResult === 'APPROVED') return 'success'
  if (step.approvalResult === 'REJECTED') return 'error'
  return 'warning'
}

function stepLabel(step: string) {
  const map: Record<string, string> = {
    AUTHOR: '编制',
    REVIEW: '审核',
    COUNTERSIGN: '会签',
    QUALITY_APPROVE: '质量批准',
    APPROVE: '批准',
    CUSTOMER_COUNTERSIGN: '顾客会签'
  }
  return map[step] || step
}

function stepDescription(step: ApprovalStep, _idx: number) {
  let desc = positionLabel(step.approverPosition)
  if (step.approvalResult) {
    desc += ` — ${resultLabel(step.approvalResult)}`
    if (step.approvedAt) desc += ` (${step.approvedAt.substring(0, 10)})`
  }
  return desc
}

function positionLabel(code: string) {
  const map: Record<string, string> = {
    // 技术线
    CHIEF_DESIGNER: '总设计师',
    DEPUTY_CHIEF_DESIGNER: '副总设计师',
    CHIEF_DESIGNER_ENGINEER: '主任设计师',
    LEAD_DESIGNER_ENGINEER: '主管设计师',
    DESIGNER_ENGINEER: '设计师',
    SYSTEM_ENGINEER: '系统工程师',
    SOFTWARE_ENGINEER: '软件工程师',
    LEAD_SOFTWARE_ENGINEER: '软件主管师',
    // 行政线
    CHIEF_COMMANDER: '总指挥',
    DEPUTY_CHIEF_COMMANDER: '副总指挥',
    PROJECT_OFFICE_DIRECTOR: '项目办主任',
    PLAN_SUPERVISOR: '计划主管',
    PROJECT_ASSISTANT: '项目助理',
    // 质量线
    CHIEF_QUALITY_ENGINEER: '总质量师',
    QUALITY_SUPERVISOR: '质量主管',
    QUALITY_ENGINEER: '质量师',
    STANDARDIZATION_ENGINEER: '标准化师',
    // 工艺线
    CHIEF_PROCESS_ENGINEER: '总工艺师',
    PROCESS_ENGINEER: '工艺师',
    // 军方
    MILITARY_REP: '军代表'
  }
  return map[code] || code
}

function resultLabel(result: string) {
  switch (result) {
    case 'APPROVED': return '通过'
    case 'REJECTED': return '驳回'
    case 'RETURN': return '退回修改'
    default: return result
  }
}

async function submit(result: string) {
  if (!currentStep.value) return
  submitting.value = true
  try {
    emit('submit', currentStep.value.id, result, opinion.value)
    opinion.value = ''
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.approval-flow { padding: 16px; background: #fff; border-radius: 4px; }
.approval-action { margin-top: 8px; }
.action-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.action-buttons { display: flex; align-items: center; gap: 8px; }
</style>
