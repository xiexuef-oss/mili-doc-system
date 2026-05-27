<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
        <h3 style="display:inline;margin-left:16px">章节编辑器</h3>
      </div>
      <div class="header-actions">
        <el-select v-model="fillStatus" size="small" style="width:120px" @change="handleStatusChange">
          <el-option label="已完成" value="FILLED" />
          <el-option label="部分完成" value="PARTIAL" />
          <el-option label="待填写" value="EMPTY" />
        </el-select>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </div>

    <div class="editor-header" v-if="chapter">
      <el-tag size="small">{{ chapter.chapterNumber }}</el-tag>
      <strong style="margin-left:8px;font-size:16px">{{ chapter.chapterTitle }}</strong>
      <KnowledgeCardPopover
        v-if="chapter.chapterTitle"
        :keyword="chapter.chapterTitle"
        label="GJB写作指南"
        style="margin-left:12px"
      />
      <div style="margin-left:auto;display:flex;gap:8px">
        <el-button v-if="projectId" size="small" type="success" :loading="autoFilling" @click="handleAutoFill">
          自动填充
        </el-button>
        <el-button v-if="projectId" size="small" type="warning" :loading="aiGenerating" @click="handleAiGenerate">
          AI生成
        </el-button>
      </div>
    </div>

    <!-- Three-library writing guide -->
    <ChapterWritingGuide :context="writingContext" />

    <el-alert
      v-if="chapter && chapter.writingTips"
      :title="chapter.writingTips"
      type="info"
      :closable="true"
      show-icon
      style="margin-bottom:16px"
    />

    <div class="editor-body">
      <div class="editor-toolbar">
        <el-button-group size="small">
          <el-button @click="insertMarkdown('## ')">H2</el-button>
          <el-button @click="insertMarkdown('### ')">H3</el-button>
          <el-button @click="insertMarkdown('**文本**')"><strong>B</strong></el-button>
          <el-button @click="insertMarkdown('*文本*')"><em>I</em></el-button>
          <el-button @click="insertMarkdown('- ')">列表</el-button>
        </el-button-group>
        <span class="toolbar-right">
          已输入 {{ charCount }} 字
        </span>
      </div>

      <el-input
        v-model="content"
        type="textarea"
        :rows="24"
        placeholder="在此编写章节内容...&#10;&#10;支持 Markdown 语法：&#10;## 标题&#10;**加粗** *斜体*&#10;- 列表项"
        class="editor-textarea"
      />
    </div>

    <!-- Content schema fields panel -->
    <div v-if="schemaFields.length > 0" class="schema-panel">
      <el-divider content-position="left">结构化数据字段</el-divider>
      <el-form label-width="120px" size="small">
        <el-form-item
          v-for="field in schemaFields"
          :key="field.name"
          :label="field.label"
          :required="field.required"
        >
          <el-input v-if="field.type === 'text'" v-model="contentData[field.name]" />
          <el-input v-else-if="field.type === 'textarea'" v-model="contentData[field.name]" type="textarea" :rows="3" />
          <el-select v-else-if="field.type === 'select'" v-model="contentData[field.name]" style="width:100%">
            <el-option
              v-for="opt in (field.options || [])"
              :key="typeof opt === 'string' ? opt : (opt as any).value"
              :label="typeof opt === 'string' ? opt : (opt as any).label"
              :value="typeof opt === 'string' ? opt : (opt as any).value"
            />
          </el-select>
          <el-input-number v-else-if="field.type === 'number'" v-model="contentData[field.name]" style="width:100%" />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getChapter, updateChapterContent, updateFillStatus,
  getChapterWritingContext, generateChapter, autoFillChapter, type ChapterWritingContext } from '@/api/doc-chapter'
import KnowledgeCardPopover from '@/components/KnowledgeCardPopover.vue'
import ChapterWritingGuide from '@/components/ChapterWritingGuide.vue'

const route = useRoute()
const chapterId = Number(route.params.chapterId)
const projectId = Number(route.query.projectId || 0)

const chapter = ref<any>(null)
const content = ref('')
const fillStatus = ref('EMPTY')
const saving = ref(false)
const contentData: Record<string, any> = reactive({})

// Three-library fusion
const writingContext = ref<ChapterWritingContext | null>(null)
const aiGenerating = ref(false)
const autoFilling = ref(false)

const charCount = computed(() => content.value.length)

const schemaFields = computed(() => {
  if (!chapter.value?.contentSchema) return []
  try {
    const schema = typeof chapter.value.contentSchema === 'string'
      ? JSON.parse(chapter.value.contentSchema)
      : chapter.value.contentSchema
    return schema.fields || []
  } catch { return [] }
})

function insertMarkdown(template: string) {
  content.value += '\n' + template
}

async function loadChapter() {
  try {
    const res = await getChapter(chapterId)
    chapter.value = res.data.data
    if (chapter.value) {
      content.value = chapter.value.content || ''
      fillStatus.value = chapter.value.fillStatus || 'EMPTY'
      if (chapter.value.contentJson) {
        try {
          const parsed = JSON.parse(chapter.value.contentJson)
          Object.assign(contentData, parsed)
        } catch { /* ignore */ }
      }
    }
    // Load writing context if projectId is provided
    if (projectId) {
      try {
        const ctxRes = await getChapterWritingContext(chapterId, projectId)
        writingContext.value = ctxRes.data.data
      } catch { /* ignore */ }
    }
  } catch { /* ignore */ }
}

async function handleSave() {
  saving.value = true
  try {
    const contentJson = Object.keys(contentData).length > 0 ? JSON.stringify(contentData) : undefined
    await updateChapterContent(chapterId, content.value, contentJson, 1)
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
  saving.value = false
}

async function handleStatusChange() {
  try {
    const fillPercent = fillStatus.value === 'FILLED' ? 100 : fillStatus.value === 'PARTIAL' ? 50 : 0
    await updateFillStatus(chapterId, fillStatus.value, fillPercent)
    ElMessage.success('状态已更新')
  } catch { /* ignore */ }
}

async function handleAiGenerate() {
  if (!projectId) return
  aiGenerating.value = true
  try {
    const res = await generateChapter(chapterId, projectId)
    const generated = res.data.data
    if (generated) {
      content.value = generated
      ElMessage.success('AI内容生成完成')
    }
  } catch { ElMessage.error('AI生成失败') }
  aiGenerating.value = false
}

async function handleAutoFill() {
  if (!projectId) return
  autoFilling.value = true
  try {
    const res = await autoFillChapter(chapterId, projectId)
    const updated = res.data.data
    if (updated?.content) {
      content.value = updated.content
      ElMessage.success('占位符自动填充完成')
    } else {
      ElMessage.info('未发现可填充的占位符')
    }
    // Refresh writing context
    const ctxRes = await getChapterWritingContext(chapterId, projectId)
    writingContext.value = ctxRes.data.data
  } catch { ElMessage.error('自动填充失败') }
  autoFilling.value = false
}

onMounted(loadChapter)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-actions { display: flex; gap: 8px; }
.editor-header { margin-bottom: 16px; padding: 12px; background: var(--el-fill-color-lighter); border-radius: 4px; display: flex; align-items: center; }
.editor-body { margin-bottom: 16px; }
.editor-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; padding: 6px; background: #f5f7fa; border-radius: 4px; }
.toolbar-right { font-size: 12px; color: #909399; }
.editor-textarea :deep(textarea) { font-family: 'Courier New', monospace; font-size: 14px; line-height: 1.8; }
.schema-panel { margin-top: 16px; padding: 16px; background: var(--el-fill-color-lighter); border-radius: 6px; }
</style>
