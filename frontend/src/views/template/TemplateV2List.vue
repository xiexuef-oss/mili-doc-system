<template>
  <div class="page">
    <div class="page-header">
      <h3>增强模板管理</h3>
      <div class="header-actions">
        <el-select v-model="filterCategory" placeholder="筛选分类" clearable style="width:200px" @change="loadTemplates">
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>新建模板
        </el-button>
      </div>
    </div>

    <el-table :data="templates" v-loading="loading" border stripe>
      <el-table-column prop="templateCode" label="模板编号" width="140" />
      <el-table-column prop="templateName" label="模板名称" min-width="180" />
      <el-table-column label="分类" width="140">
        <template #default="{ row }">
          <el-tag size="small">{{ categoryName(row.categoryId) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="versionNo" label="版本" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="editChapters(row)">章节结构</el-button>
          <el-button link type="success" size="small" @click="editTemplate(row)">编辑</el-button>
          <el-button link type="warning" size="small" @click="manageElements(row)">标准元素</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Template Dialog -->
    <el-dialog v-model="showCreateDialog" :title="editingTemplate ? '编辑模板' : '新建模板'" width="560px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板编号" required>
          <el-input v-model="form.templateCode" placeholder="如 DEV_SUMMARY_V2" />
        </el-form-item>
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="如 研制总结" />
        </el-form-item>
        <el-form-item label="所属分类" required>
          <el-select v-model="form.categoryId" style="width:100%" placeholder="选择模板分类">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用范围">
          <el-input v-model="form.applicableProjectType" type="textarea" :rows="2" placeholder="描述模板适用范围" />
        </el-form-item>
        <el-form-item label="GJB参考标准">
          <el-input v-model="form.gjbStandardRef" placeholder="如 GJB/Z 170.4-2013" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTemplates, getCategories, createTemplate, updateTemplate, deleteTemplate,
  type DocTemplateV2, type DocTemplateCategory
} from '@/api/template-v2'

const router = useRouter()
const categories = ref<DocTemplateCategory[]>([])
const templates = ref<DocTemplateV2[]>([])
const loading = ref(false)
const saving = ref(false)
const filterCategory = ref<number | null>(null)
const showCreateDialog = ref(false)
const editingTemplate = ref<DocTemplateV2 | null>(null)

const form = reactive({
  templateCode: '', templateName: '', categoryId: 0,
  applicableProjectType: '', gjbStandardRef: '', status: 'ACTIVE'
})

function categoryName(id: number) {
  return categories.value.find(c => c.id === id)?.categoryName || '-'
}

function resetForm() {
  Object.assign(form, { templateCode: '', templateName: '', categoryId: 0, applicableProjectType: '', gjbStandardRef: '', status: 'ACTIVE' })
  editingTemplate.value = null
}

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res.data.data || []
  } catch { /* ignore */ }
}

async function loadTemplates() {
  loading.value = true
  try {
    const res = await getTemplates(filterCategory.value || undefined)
    templates.value = res.data.data || []
  } finally { loading.value = false }
}

function editTemplate(row: DocTemplateV2) {
  editingTemplate.value = row
  Object.assign(form, {
    templateCode: row.templateCode, templateName: row.templateName,
    categoryId: row.categoryId, applicableProjectType: row.applicableProjectType || '',
    gjbStandardRef: row.gjbStandardRef || '', status: row.status || 'ACTIVE'
  })
  showCreateDialog.value = true
}

async function handleSave() {
  if (!form.templateCode || !form.templateName || !form.categoryId) {
    ElMessage.warning('请填写必填项'); return
  }
  saving.value = true
  try {
    const data = form as unknown as DocTemplateV2
    if (editingTemplate.value?.id) {
      await updateTemplate(editingTemplate.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(data)
      ElMessage.success('创建成功')
    }
    showCreateDialog.value = false
    loadTemplates()
  } catch { ElMessage.error('保存失败') }
  saving.value = false
}

async function handleDelete(row: DocTemplateV2) {
  try {
    await ElMessageBox.confirm(`确定删除模板 "${row.templateName}" 吗？`, '确认删除', { type: 'warning' })
    await deleteTemplate(row.id!)
    ElMessage.success('已删除')
    loadTemplates()
  } catch { /* cancelled */ }
}

function editChapters(row: DocTemplateV2) {
  router.push({ name: 'TemplateChapterEditor', params: { templateId: row.id } })
}

function manageElements(row: DocTemplateV2) {
  // Elements managed within chapter editor for now
  router.push({ name: 'TemplateChapterEditor', params: { templateId: row.id } })
}

onMounted(() => { loadCategories(); loadTemplates() })
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 16px; }
.header-actions { display: flex; gap: 8px; }
</style>
