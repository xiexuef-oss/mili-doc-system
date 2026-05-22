<template>
  <el-popover
    :visible="visible"
    placement="right"
    :width="360"
    trigger="click"
    @show="loadCard"
    @hide="visible = false"
  >
    <template #reference>
      <el-button
        link
        type="warning"
        size="small"
        @click="visible = !visible"
      >
        <el-icon><QuestionFilled /></el-icon>
        {{ label || 'GJB参考' }}
      </el-button>
    </template>

    <div v-if="card" class="card-popover">
      <h4>{{ card.title }}</h4>
      <div v-if="card.plainLanguage" class="card-plain">
        <el-icon><ChatDotSquare /></el-icon>
        <span>{{ card.plainLanguage }}</span>
      </div>
      <div v-if="card.gjbReference" class="card-ref">
        <el-tag size="small" type="danger">GJB参考</el-tag>
        <span>{{ card.gjbReference }}</span>
      </div>
      <div v-if="card.tags" class="card-tags">
        <el-tag v-for="tag in tagList" :key="tag" size="small" type="info" style="margin-right:4px">{{ tag }}</el-tag>
      </div>
    </div>
    <div v-else-if="loading" style="text-align:center;padding:20px;color:#999">加载中...</div>
    <div v-else style="text-align:center;padding:20px;color:#999">暂无相关知识卡片</div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { QuestionFilled, ChatDotSquare } from '@element-plus/icons-vue'
import { searchKnowledgeCards, type KnowledgeCard } from '@/api/knowledge-card'

const props = defineProps<{
  keyword?: string
  label?: string
  cardType?: string
  targetTable?: string
  targetId?: number
}>()

const visible = ref(false)
const loading = ref(false)
const card = ref<KnowledgeCard | null>(null)

const tagList = computed(() => {
  if (!card.value?.tags) return []
  return card.value.tags.split(',').map(t => t.trim()).filter(Boolean)
})

async function loadCard() {
  if (card.value) return
  loading.value = true
  try {
    let res
    if (props.keyword) {
      res = await searchKnowledgeCards(props.keyword)
    } else if (props.cardType && props.targetTable) {
      const { getKnowledgeCards } = await import('@/api/knowledge-card')
      res = await getKnowledgeCards(props.cardType, props.targetTable, props.targetId)
    }
    const data = res?.data?.data
    card.value = Array.isArray(data) ? data[0] : data
  } catch { /* ignore */ }
  loading.value = false
}
</script>

<style scoped>
.card-popover h4 { margin: 0 0 8px; font-size: 15px; }
.card-plain {
  display: flex; align-items: flex-start; gap: 6px;
  padding: 8px; background: #f0f9eb; border-radius: 4px;
  margin-bottom: 8px; font-size: 13px; line-height: 1.6;
}
.card-ref {
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 12px;
}
.card-tags { margin-top: 4px; }
</style>
