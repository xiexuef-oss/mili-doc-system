<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回模板列表</el-button>
        <h3 style="display:inline;margin-left:16px">章节结构编辑器</h3>
        <el-tag v-if="template" size="small" type="success" style="margin-left:12px">{{ template.templateName }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showAddChapter = true">
          <el-icon><Plus /></el-icon>添加章节
        </el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header><span>章节结构树</span></template>
          <ChapterTreeViewer
            :tree-data="chapterTree"
            @node-click="selectChapter"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span v-if="selectedChapter">编辑: {{ selectedChapter.chapterTitle }}</span>
            <span v-else>选择一个章节进行编辑</span>
          </template>
          <template v-if="selectedChapter">
            <el-form label-width="100px" size="small">
              <el-form-item label="章节编号">
                <el-input v-model="selectedChapter.chapterNumber" />
              </el-form-item>
              <el-form-item label="章节标题">
                <el-input v-model="selectedChapter.chapterTitle" />
              </el-form-item>
              <el-form-item label="层级">
                <el-input-number v-model="selectedChapter.chapterLevel" :min="1" :max="6" />
              </el-form-item>
              <el-form-item label="排序">
                <el-input-number v-model="selectedChapter.orderNum" :min="0" />
              </el-form-item>
              <el-form-item label="必写项">
                <el-switch v-model="selectedChapter.isRequired" />
              </el-form-item>
              <el-form-item label="编写提示">
                <el-input v-model="selectedChapter.writingTips" type="textarea" :rows="3" placeholder="编写提示文字..." />
              </el-form-item>
              <el-form-item label="内容结构(JSON)">
                <el-input v-model="contentSchemaStr" type="textarea" :rows="5" placeholder='{"fields":[...]}' />
                <span class="hint">JSON Schema 定义该章节需要填写的字段/表格</span>
              </el-form-item>
            </el-form>
            <div style="margin-top:12px;display:flex;gap:8px">
              <el-button type="primary" size="small" :loading="saving" @click="handleSaveChapter">保存章节</el-button>
              <el-button type="danger" size="small" @click="handleDeleteChapter">删除章节</el-button>
            </div>
          </template>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header><span>标准元素</span></template>
          <div v-if="selectedChapter" style="margin-bottom:16px">
            <div v-if="chapterElements.length === 0" style="color:#999;font-size:13px;text-align:center;padding:12px">暂无关联元素</div>
            <el-tag
              v-for="el in chapterElements" :key="el.id"
              size="small" closable style="margin:2px"
              @close="handleDetachElement(el.id!)"
            >{{ el.elementCode }} {{ el.elementName }}</el-tag>
          </div>
          <el-divider />
          <el-select v-model="selectedElementId" filterable placeholder="搜索元素..." style="width:100%" size="small">
            <el-option v-for="el in allElements" :key="el.id" :label="`${el.elementCode} ${el.elementName}`" :value="el.id" />
          </el-select>
          <el-button size="small" type="primary" style="margin-top:8px;width:100%" @click="handleAttachElement">
            关联到此章节
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- Add Chapter Dialog -->
    <el-dialog v-model="showAddChapter" title="添加章节" width="420px">
      <el-form :model="newChapter" label-width="100px">
        <el-form-item label="章节编号">
          <el-input v-model="newChapter.chapterNumber" placeholder="如 2.1" />
        </el-form-item>
        <el-form-item label="章节标题" required>
          <el-input v-model="newChapter.chapterTitle" placeholder="如 技术方案" />
        </el-form-item>
        <el-form-item label="层级">
          <el-input-number v-model="newChapter.chapterLevel" :min="1" :max="6" />
        </el-form-item>
        <el-form-item label="父章节">
          <el-select v-model="newChapter.parentId" clearable style="width:100%" placeholder="顶级章节则留空">
            <el-option v-for="ch in flatChapters" :key="ch.id" :label="`${ch.chapterNumber || ''} ${ch.chapterTitle}`" :value="ch.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="必写项">
          <el-switch v-model="newChapter.isRequired" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddChapter = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAddChapter">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import {
  getTemplate, getChapters, createChapter, updateChapter, deleteChapter,
  getChapterElements, attachElement, detachElement,
  getElements, type DocTemplateV2, type DocTemplateChapter
} from '@/api/template-v2'
import ChapterTreeViewer from '@/components/ChapterTreeViewer.vue'

const route = useRoute()
const templateId = Number(route.params.templateId)

const template = ref<DocTemplateV2 | null>(null)
const chapters = ref<DocTemplateChapter[]>([])
const allElements = ref<any[]>([])
const chapterElements = ref<any[]>([])
const selectedChapter = ref<DocTemplateChapter | null>(null)
const selectedElementId = ref<number | null>(null)
const showAddChapter = ref(false)
const saving = ref(false)
const adding = ref(false)

const newChapter = reactive({
  chapterNumber: '', chapterTitle: '', chapterLevel: 1,
  parentId: null as number | null, isRequired: true, orderNum: 0
})

const contentSchemaStr = ref('')

const chapterTree = computed(() => buildTree(chapters.value))
const flatChapters = computed(() => chapters.value)

function buildTree(list: DocTemplateChapter[]): any[] {
  const map = new Map<number, any>()
  const roots: any[] = []
  for (const ch of list) {
    map.set(ch.id!, { ...ch, children: [] })
  }
  for (const ch of list) {
    const node = map.get(ch.id!)
    if (ch.parentId && map.has(ch.parentId)) {
      map.get(ch.parentId)!.children.push(node)
    } else {
      roots.push(node)
    }
  }
  return roots
}

function selectChapter(ch: DocTemplateChapter) {
  selectedChapter.value = ch
  contentSchemaStr.value = ch.contentSchema || ''
  loadChapterElements(ch.id!)
}

async function loadTemplate() {
  try {
    const res = await getTemplate(templateId)
    template.value = res.data.data
  } catch { /* ignore */ }
}

async function loadChapters() {
  try {
    const res = await getChapters(templateId)
    chapters.value = flattenTree(res.data.data || [])
  } catch { /* ignore */ }
}

function flattenTree(nodes: any[], result: any[] = []): any[] {
  for (const n of nodes) {
    result.push({ id: n.id, chapterNumber: n.chapterNumber, chapterTitle: n.chapterTitle, chapterLevel: n.chapterLevel, orderNum: n.orderNum, parentId: n.parentId, isRequired: n.isRequired, writingTips: n.writingTips, contentSchema: n.contentSchema })
    if (n.children) flattenTree(n.children, result)
  }
  return result
}

async function loadElements() {
  try {
    const res = await getElements()
    allElements.value = res.data.data || []
  } catch { /* ignore */ }
}

async function loadChapterElements(chapterId: number) {
  try {
    const res = await getChapterElements(chapterId)
    chapterElements.value = res.data.data || []
  } catch { chapterElements.value = [] }
}

async function handleSaveChapter() {
  if (!selectedChapter.value?.id) return
  saving.value = true
  try {
    await updateChapter(selectedChapter.value.id, {
      chapterNumber: selectedChapter.value.chapterNumber,
      chapterTitle: selectedChapter.value.chapterTitle,
      chapterLevel: selectedChapter.value.chapterLevel,
      orderNum: selectedChapter.value.orderNum,
      isRequired: selectedChapter.value.isRequired,
      writingTips: selectedChapter.value.writingTips,
      contentSchema: contentSchemaStr.value
    })
    ElMessage.success('保存成功')
    loadChapters()
  } catch { ElMessage.error('保存失败') }
  saving.value = false
}

async function handleDeleteChapter() {
  if (!selectedChapter.value?.id) return
  try {
    await ElMessageBox.confirm('删除将同时删除子章节，确认？', '警告', { type: 'warning' })
    await deleteChapter(selectedChapter.value.id)
    ElMessage.success('已删除')
    selectedChapter.value = null
    loadChapters()
  } catch { /* cancelled */ }
}

async function handleAddChapter() {
  if (!newChapter.chapterTitle) { ElMessage.warning('请输入章节标题'); return }
  adding.value = true
  try {
    await createChapter(templateId, {
      ...newChapter,
      writingTips: '', contentSchema: ''
    } as any)
    ElMessage.success('添加成功')
    showAddChapter.value = false
    Object.assign(newChapter, { chapterNumber: '', chapterTitle: '', chapterLevel: 1, parentId: null, isRequired: true })
    loadChapters()
  } catch { ElMessage.error('添加失败') }
  adding.value = false
}

async function handleAttachElement() {
  if (!selectedElementId.value || !selectedChapter.value?.id) return
  try {
    await attachElement(selectedChapter.value.id, selectedElementId.value)
    ElMessage.success('已关联元素')
    loadChapterElements(selectedChapter.value.id)
  } catch { ElMessage.error('关联失败') }
}

async function handleDetachElement(elementId: number) {
  if (!selectedChapter.value?.id) return
  try {
    await detachElement(selectedChapter.value.id, elementId)
    ElMessage.success('已取消关联')
    loadChapterElements(selectedChapter.value.id)
  } catch { ElMessage.error('操作失败') }
}

onMounted(() => { loadTemplate(); loadChapters(); loadElements() })
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-actions { display: flex; gap: 8px; }
.hint { font-size: 11px; color: #909399; margin-top: 4px; }
</style>
