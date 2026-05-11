<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回阶段工作台</el-button>
        <h3 style="display:inline;margin-left:16px">技术状态记实</h3>
      </div>
    </div>

    <el-timeline v-if="items.length > 0" v-loading="loading">
      <el-timeline-item
        v-for="e in paginatedItems"
        :key="e.id"
        :timestamp="e.eventTime"
        placement="top"
        :type="eventColor(e.eventType)"
      >
        <strong>{{ e.eventName }}</strong>
        <p v-if="e.eventDescription" style="margin:4px 0 0;color:#909399;font-size:13px">{{ e.eventDescription }}</p>
        <span style="font-size:12px;color:#c0c4cc">
          {{ e.eventType }} | {{ e.relatedObjectType }}#{{ e.relatedObjectId }}
        </span>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else-if="!loading" description="暂无技术状态记实" />

    <el-pagination
      v-if="items.length > pageSize"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="items.length"
      layout="prev, pager, next"
      style="margin-top:20px;justify-content:center"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getStatusAccounting, type ConfigurationStatusAccountingVO } from '@/api/configuration-management'

const route = useRoute()
const projectId = Number(route.params.projectId)
const stageId = Number(route.params.stageId)

const loading = ref(false)
const items = ref<ConfigurationStatusAccountingVO[]>([])
const currentPage = ref(1)
const pageSize = 20

const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return items.value.slice(start, start + pageSize)
})

const eventColor = (type: string) => {
  if (type?.includes('COMPLETE') || type?.includes('EFFECTIVE') || type?.includes('APPROVE')) return 'success'
  if (type?.includes('CREATE') || type?.includes('SUBMIT') || type?.includes('START')) return 'primary'
  if (type?.includes('CHANGE') || type?.includes('GATE')) return 'warning'
  if (type?.includes('TERMINATE') || type?.includes('SUSPEND')) return 'danger'
  return 'info'
}

async function fetch() {
  loading.value = true
  try { const res = await getStatusAccounting(projectId, stageId); items.value = res.data.data || [] } finally { loading.value = false }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
