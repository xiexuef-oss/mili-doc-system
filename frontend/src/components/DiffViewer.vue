<template>
  <el-dialog v-model="visible" title="文档进化分析" width="700px" destroy-on-close>
    <div v-loading="loading">
      <template v-if="report">
        <div class="dv-summary">
          <div class="dv-stat">
            <span class="dv-stat-label">初稿字数</span>
            <span class="dv-stat-val">{{ report.initialChars?.toLocaleString() }}</span>
          </div>
          <div class="dv-stat">
            <span class="dv-stat-label">终稿字数</span>
            <span class="dv-stat-val">{{ report.finalChars?.toLocaleString() }}</span>
          </div>
          <div class="dv-stat" :class="report.sizeChangePercent > 0 ? 'up' : 'down'">
            <span class="dv-stat-label">内容变化</span>
            <span class="dv-stat-val">{{ report.sizeChangePercent > 0 ? '+' : '' }}{{ report.sizeChangePercent }}%</span>
          </div>
        </div>

        <div class="dv-changes">
          <div class="dv-section-title">差异分析</div>
          <div v-if="report.changes?.length">
            <div v-for="c in report.changes" :key="c.description" class="dv-change">
              <el-tag size="small" :type="c.category === '结构' ? '' : c.category === '数据' ? 'warning' : 'info'">
                {{ c.category }}
              </el-tag>
              <span>{{ c.description }}</span>
            </div>
          </div>

          <div class="dv-detail">
            <div v-if="report.chapterChange !== 0">
              章节: {{ report.initialChapterCount }} → {{ report.finalChapterCount }}
              ({{ report.chapterChange > 0 ? '+' : '' }}{{ report.chapterChange }})
            </div>
            <div v-if="report.warningsResolved > 0">
              待核实标记消除: {{ report.warningsResolved }} 处
            </div>
          </div>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import request from '@/api/index'

const props = defineProps<{ modelValue: boolean; ledgerId: number; finalContent?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const visible = ref(props.modelValue)
const loading = ref(false)
const report = ref<any>(null)

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val && props.ledgerId) {
    loading.value = true
    try {
      const res = await request.post(`/reliability/documents/${props.ledgerId}/diff-analysis`, {
        finalContent: props.finalContent || ''
      })
      report.value = res.data?.data
    } catch { /* ignore */ }
    finally { loading.value = false }
  }
})

watch(visible, (v) => emit('update:modelValue', v))
</script>

<style scoped>
.dv-summary { display: flex; gap: 16px; margin-bottom: 16px; }
.dv-stat { flex: 1; text-align: center; padding: 12px; background: #f5f7fa; border-radius: 6px; }
.dv-stat-label { display: block; font-size: 12px; color: #909399; }
.dv-stat-val { display: block; font-size: 20px; font-weight: 600; margin-top: 4px; }
.dv-stat.up .dv-stat-val { color: #67c23a; }
.dv-stat.down .dv-stat-val { color: #f56c6c; }
.dv-section-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.dv-change { display: flex; align-items: center; gap: 8px; padding: 6px 0; }
.dv-detail { margin-top: 8px; font-size: 13px; color: #606266; }
.dv-detail div { padding: 3px 0; }
</style>
