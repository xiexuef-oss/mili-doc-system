<template>
  <el-tooltip :content="tooltipText" placement="bottom">
    <el-tag
      :type="tagType"
      size="small"
      effect="plain"
      class="locality-badge"
    >
      <el-icon v-if="isLocal" class="badge-icon"><Monitor /></el-icon>
      <el-icon v-else class="badge-icon"><Cloudy /></el-icon>
      {{ label }}
    </el-tag>
  </el-tooltip>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Monitor, Cloudy } from '@element-plus/icons-vue'
import { getAiLocality, type LocalityInfo } from '@/api/ai'

const locality = ref<LocalityInfo | null>(null)
const loading = ref(false)

const isLocal = computed(() => locality.value?.isLocal ?? false)
const label = computed(() => {
  if (!locality.value) return '未知'
  return isLocal.value ? '本地模型' : '云端模型'
})
const tagType = computed(() => {
  if (!locality.value) return 'info'
  return isLocal.value ? 'success' : 'warning'
})
const tooltipText = computed(() => {
  if (!locality.value) return '无法获取模型状态'
  const info = locality.value
  return `提供商: ${info.provider}\n模型: ${info.model}\n地址: ${info.baseUrl}\n脱敏: ${info.desensitizationEnabled ? '已启用' : '未启用'}`
})

async function fetchLocality() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getAiLocality()
    locality.value = res.data
  } catch {
    // 静默失败，显示"未知"
  } finally {
    loading.value = false
  }
}

onMounted(fetchLocality)
</script>

<style scoped>
.locality-badge {
  cursor: pointer;
  font-size: 12px;
}
.badge-icon {
  margin-right: 2px;
  vertical-align: middle;
}
</style>
