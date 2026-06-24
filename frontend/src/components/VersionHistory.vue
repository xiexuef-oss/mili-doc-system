<template>
  <el-dialog v-model="visible" title="版本历史" width="600px" destroy-on-close>
    <div v-loading="loading">
      <div v-if="versions.length === 0" style="text-align:center;padding:20px;color:#909399">
        暂无版本记录
      </div>
      <el-timeline v-else>
        <el-timeline-item
          v-for="v in versions"
          :key="v.id"
          :timestamp="formatTime(v.createdAt)"
          placement="top"
        >
          <div class="vh-item">
            <div class="vh-header">
              <span class="vh-title">{{ v.title || '快照' }}</span>
              <el-tag size="small" type="info">{{ v.reason || '自动保存' }}</el-tag>
            </div>
            <div class="vh-user" v-if="v.userId">用户ID: {{ v.userId }}</div>
            <el-button size="small" type="primary" link @click="handleRestore(v.id)">
              恢复此版本
            </el-button>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as aiDocApi from '@/api/ai-document'

const props = defineProps<{ modelValue: boolean; documentId: number }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'restored'): void
}>()

const visible = ref(props.modelValue)
const loading = ref(false)
const versions = ref<any[]>([])

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val && props.documentId) {
    loading.value = true
    try {
      const res = await aiDocApi.listVersions(props.documentId)
      versions.value = res.data?.data || []
    } catch { versions.value = [] }
    finally { loading.value = false }
  }
})

watch(visible, (v) => emit('update:modelValue', v))

async function handleRestore(versionId: number) {
  try {
    await ElMessageBox.confirm('恢复到此版本将覆盖当前内容，确定继续？', '确认恢复', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
    loading.value = true
    await aiDocApi.restoreVersion(props.documentId, versionId)
    ElMessage.success('版本已恢复')
    visible.value = false
    emit('restored')
  } catch { /* cancelled */ }
  finally { loading.value = false }
}

function formatTime(t: string) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}
</script>

<style scoped>
.vh-item { padding: 4px 0; }
.vh-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.vh-title { font-weight: 600; font-size: 14px; }
.vh-user { font-size: 12px; color: #909399; margin-bottom: 4px; }
</style>
