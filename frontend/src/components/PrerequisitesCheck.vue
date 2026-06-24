<template>
  <el-dialog
    v-model="visible"
    title="前置条件检查"
    width="520px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-loading="loading">
      <template v-if="result">
        <!-- Status header -->
        <div :class="['check-header', result.allHardMet ? 'pass' : 'fail']">
          <el-icon :size="20">
            <CircleCheckFilled v-if="result.allHardMet" />
            <WarningFilled v-else />
          </el-icon>
          <span>{{ result.allHardMet ? '前置条件已满足' : '前置条件未满足' }}</span>
        </div>

        <div class="check-doc-name">{{ result.docName }}</div>

        <!-- Hard dependencies -->
        <div v-if="result.hardDeps?.length" class="dep-section">
          <div class="dep-label">硬依赖（必须满足）</div>
          <div v-for="dep in result.hardDeps" :key="dep.docType" class="dep-item">
            <el-icon :size="16">
              <CircleCheckFilled v-if="dep.satisfied" style="color: #67c23a" />
              <CircleCloseFilled v-else style="color: #f56c6c" />
            </el-icon>
            <div class="dep-info">
              <div class="dep-name">
                {{ dep.docName }}
                <el-tag v-if="!dep.satisfied" size="small" type="danger">缺失</el-tag>
              </div>
              <div class="dep-detail">{{ dep.detail }}</div>
            </div>
          </div>
        </div>

        <!-- Soft dependencies -->
        <div v-if="result.softDeps?.length" class="dep-section">
          <div class="dep-label">软依赖（建议满足，影响质量）</div>
          <div v-for="dep in result.softDeps" :key="dep.docType" class="dep-item">
            <el-icon :size="16">
              <CircleCheckFilled v-if="dep.satisfied" style="color: #67c23a" />
              <WarningFilled v-else style="color: #e6a23c" />
            </el-icon>
            <div class="dep-info">
              <div class="dep-name">
                {{ dep.docName || dep.description }}
                <el-tag v-if="!dep.satisfied" size="small" type="warning">缺失</el-tag>
              </div>
              <div class="dep-detail" v-if="dep.detail">{{ dep.detail }}</div>
            </div>
          </div>
        </div>

        <!-- Suggestion -->
        <div v-if="result.suggestion" class="suggestion">
          <el-icon><InfoFilled /></el-icon>
          {{ result.suggestion }}
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :disabled="!result?.allHardMet"
        @click="handleProceed"
      >
        {{ result?.allHardMet ? '开始生成' : '先完成前置条件' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { CircleCheckFilled, CircleCloseFilled, WarningFilled, InfoFilled } from '@element-plus/icons-vue'
import { checkPrerequisites } from '@/api/reliability'
import { checkDraftPrerequisites } from '@/api/ai'

const props = defineProps<{
  modelValue: boolean
  projectId: number
  docType: string
  /** 'reliability' | 'general' — 决定调用哪个API */
  api?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'proceed'): void
}>()

const visible = ref(props.modelValue)
const loading = ref(false)
const result = ref<any>(null)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.projectId && props.docType) {
    loadCheck()
  }
})

watch(visible, (val) => emit('update:modelValue', val))

async function loadCheck() {
  loading.value = true
  try {
    const apiFn = props.api === 'general' ? checkDraftPrerequisites : checkPrerequisites
    const res = await apiFn(props.projectId, props.docType)
    result.value = res.data?.data ?? res.data
  } catch (e) {
    console.error('Prerequisites check failed', e)
  } finally {
    loading.value = false
  }
}

function handleProceed() {
  visible.value = false
  emit('proceed')
}
</script>

<style scoped>
.check-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
.check-header.pass {
  background: #f0f9eb;
  color: #67c23a;
}
.check-header.fail {
  background: #fef0f0;
  color: #f56c6c;
}
.check-doc-name {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
  padding-left: 4px;
}
.dep-section {
  margin-bottom: 14px;
}
.dep-label {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 8px;
  text-transform: uppercase;
}
.dep-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 4px;
  border-bottom: 1px solid #f2f3f5;
}
.dep-item:last-child {
  border-bottom: none;
}
.dep-info {
  flex: 1;
}
.dep-name {
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}
.dep-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.suggestion {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 13px;
  color: #409eff;
  margin-top: 12px;
}
</style>
