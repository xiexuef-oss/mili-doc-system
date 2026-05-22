<template>
  <el-dialog v-model="visible" title="导出 Word 文档" width="480px" @closed="reset">
    <el-form label-width="110px">
      <el-form-item label="包含封面页">
        <el-switch v-model="options.includeCover" />
        <span class="hint">封面包含项目名称、密级、单位等信息</span>
      </el-form-item>
      <el-form-item label="显示填写标记">
        <el-switch v-model="options.showHighlights" />
        <span class="hint">预填内容黄色高亮，缺失项红色标记</span>
      </el-form-item>
      <el-form-item label="文档信息">
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="文档名称">{{ docName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="章节数">{{ chapterCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="完成度">{{ fillRate }}%</el-descriptions-item>
        </el-descriptions>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="generating" @click="handleGenerateDownload">
        <el-icon><Download /></el-icon>生成并下载
      </el-button>
      <el-button type="success" :loading="generating" @click="handleGenerateUpload">
        <el-icon><Upload /></el-icon>生成并上传
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload } from '@element-plus/icons-vue'
import { generateDocx, generateAndUpload } from '@/api/docx'

const props = defineProps<{
  modelValue: boolean
  docLedgerId: number
  docName?: string
  chapterCount?: number
  fillRate?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'done'): void
}>()

const visible = ref(props.modelValue)
const generating = ref(false)
const options = reactive({ includeCover: true, showHighlights: true })

function reset() {
  options.includeCover = true
  options.showHighlights = true
}

async function handleGenerateDownload() {
  generating.value = true
  try {
    const res = await generateDocx(props.docLedgerId, options.includeCover, options.showHighlights)
    const blob = res.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url

    const disposition = res.headers?.['content-disposition']
    let filename = `document_${props.docLedgerId}.docx`
    if (disposition) {
      const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;\n]*)/i)
      if (match) {
        try {
          filename = decodeURIComponent(match[1])
        } catch {
          filename = match[1]
        }
      }
    }

    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('文档已生成并开始下载')
    emit('done')
    visible.value = false
  } catch {
    ElMessage.error('生成失败')
  }
  generating.value = false
}

async function handleGenerateUpload() {
  generating.value = true
  try {
    await generateAndUpload(props.docLedgerId, options.includeCover, options.showHighlights)
    ElMessage.success('文档已生成并上传至文件存储')
    emit('done')
    visible.value = false
  } catch {
    ElMessage.error('生成失败')
  }
  generating.value = false
}
</script>

<style scoped>
.hint { font-size: 11px; color: #909399; margin-left: 8px; }
</style>
