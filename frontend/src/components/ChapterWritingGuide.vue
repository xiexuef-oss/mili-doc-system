<template>
  <div class="writing-guide" v-if="context">
    <!-- Template Guidance -->
    <el-collapse v-model="activeNames">
      <el-collapse-item name="guide" v-if="hasGuide">
        <template #title>
          <el-icon><Document /></el-icon>
          <span class="collapse-title">编写指南（模板要求）</span>
        </template>
        <div v-if="context.templateDescription" class="block">
          <h4>章节说明</h4>
          <p>{{ context.templateDescription }}</p>
        </div>
        <div v-if="context.writingTips" class="block">
          <h4>编写提示</h4>
          <p>{{ context.writingTips }}</p>
        </div>
        <div v-if="context.sampleContent" class="block">
          <h4>参考示例</h4>
          <div class="sample-content">{{ context.sampleContent }}</div>
        </div>
      </el-collapse-item>

      <!-- Standard Clauses -->
      <el-collapse-item name="clauses" v-if="context.applicableClauses && context.applicableClauses.length > 0">
        <template #title>
          <el-icon><Collection /></el-icon>
          <span class="collapse-title">适用标准条款</span>
          <el-tag size="small" type="danger">{{ context.applicableClauses.length }}</el-tag>
        </template>
        <div v-for="clause in context.applicableClauses" :key="clause.clauseId" class="clause-item">
          <div class="clause-header">
            <el-tag size="small" type="danger">{{ clause.standardCode }}</el-tag>
            <strong>{{ clause.clauseNumber }} {{ clause.clauseTitle }}</strong>
            <el-tag size="small" type="info">{{ linkTypeLabel(clause.linkType) }}</el-tag>
          </div>
          <p v-if="clause.clauseContent" class="clause-content">{{ truncate(clause.clauseContent, 300) }}</p>
        </div>
      </el-collapse-item>

      <!-- Knowledge Cards -->
      <el-collapse-item name="cards" v-if="context.relevantCards && context.relevantCards.length > 0">
        <template #title>
          <el-icon><Reading /></el-icon>
          <span class="collapse-title">编写知识与技巧</span>
          <el-tag size="small" type="success">{{ context.relevantCards.length }}</el-tag>
        </template>
        <div v-for="card in context.relevantCards" :key="card.cardId" class="card-item">
          <h4>{{ card.title }}</h4>
          <p v-if="card.plainLanguage" class="plain-text">{{ card.plainLanguage }}</p>
          <el-tag v-if="card.gjbReference" size="small" type="warning">{{ card.gjbReference }}</el-tag>
        </div>
      </el-collapse-item>

      <!-- Master Data Fields -->
      <el-collapse-item name="fields" v-if="context.relevantFields && context.relevantFields.length > 0">
        <template #title>
          <el-icon><DataLine /></el-icon>
          <span class="collapse-title">关联主数据字段</span>
          <el-tag size="small" type="warning">{{ filledCount }}/{{ context.relevantFields.length }}</el-tag>
        </template>
        <div class="field-list">
          <div v-for="field in context.relevantFields" :key="field.masterDataPath" class="field-item">
            <span v-if="field.valueStatus === 'FILLED'" class="check-icon">✓</span>
            <span v-else class="empty-icon">✗</span>
            <span class="field-label">{{ field.fieldLabel || field.masterDataPath }}</span>
            <el-tag size="small" :type="field.valueStatus === 'FILLED' ? 'success' : 'info'">
              {{ field.valueStatus === 'FILLED' ? field.currentValue : '待填写' }}
            </el-tag>
            <el-tag v-if="field.required" size="small" type="danger">必填</el-tag>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <el-empty v-if="!hasGuide && !hasClauses && !hasCards && !hasFields" description="暂无三库关联信息" :image-size="40" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Document, Collection, Reading, DataLine } from '@element-plus/icons-vue'
import type { ChapterWritingContext } from '@/api/doc-chapter'

const props = defineProps<{
  context: ChapterWritingContext | null
}>()

const activeNames = ref(['guide', 'clauses', 'cards', 'fields'])

const hasGuide = computed(() => props.context?.templateDescription || props.context?.writingTips || props.context?.sampleContent)
const hasClauses = computed(() => (props.context?.applicableClauses?.length || 0) > 0)
const hasCards = computed(() => (props.context?.relevantCards?.length || 0) > 0)
const hasFields = computed(() => (props.context?.relevantFields?.length || 0) > 0)

const filledCount = computed(() =>
  (props.context?.relevantFields || []).filter(f => f.valueStatus === 'FILLED').length
)

function linkTypeLabel(type: string) {
  const map: Record<string, string> = { REFERENCES: '参考引用', REQUIRES: '必须满足', INFORMS: '提供信息' }
  return map[type] || type
}

function truncate(text: string, max: number) {
  return text.length > max ? text.substring(0, max) + '...' : text
}
</script>

<style scoped>
.writing-guide { margin-bottom: 16px; }
.collapse-title { margin-left: 6px; font-weight: 500; }
.el-collapse-item .el-tag { margin-left: 8px; }

.block { margin-bottom: 12px; }
.block h4 { margin: 0 0 4px; font-size: 13px; color: #409eff; }
.block p { margin: 0; font-size: 13px; color: #606266; line-height: 1.7; }
.sample-content {
  white-space: pre-wrap; font-size: 13px; color: #67c23a;
  background: #f0f9eb; padding: 8px 12px; border-radius: 4px;
}

.clause-item { margin-bottom: 10px; padding-bottom: 10px; border-bottom: 1px dashed #ebeef5; }
.clause-item:last-child { border-bottom: none; }
.clause-header { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
.clause-content { margin: 4px 0 0; font-size: 12px; color: #909399; line-height: 1.6; }

.card-item { margin-bottom: 8px; }
.card-item h4 { margin: 0 0 4px; font-size: 13px; }
.plain-text { font-size: 12px; color: #67c23a; margin: 4px 0; line-height: 1.6; }

.field-list { display: flex; flex-direction: column; gap: 6px; }
.field-item { display: flex; gap: 8px; align-items: center; font-size: 13px; }
.check-icon { color: #67c23a; font-weight: bold; }
.empty-icon { color: #f56c6c; font-weight: bold; }
.field-label { flex: 1; color: #606266; font-size: 12px; }
</style>
