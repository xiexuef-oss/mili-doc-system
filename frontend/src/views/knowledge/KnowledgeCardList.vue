<template>
  <div class="page">
    <div class="page-header">
      <h3>知识卡片管理</h3>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="搜索知识卡片..." style="width:260px" clearable @change="search" @clear="loadCards">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterType" placeholder="卡片类型" clearable style="width:160px" @change="loadCards">
          <el-option label="GJB条款" value="GJB_CLAUSE" />
          <el-option label="编写指南" value="WRITING_GUIDE" />
          <el-option label="常见错误" value="COMMON_MISTAKE" />
          <el-option label="模板说明" value="TEMPLATE_NOTE" />
        </el-select>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>新建卡片
        </el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.id" :span="8">
        <el-card shadow="hover" class="knowledge-card-item">
          <template #header>
            <div class="card-header">
              <el-tag size="small" :type="cardTypeTag(card.cardType)">{{ card.cardType || 'GENERAL' }}</el-tag>
              <el-tag v-if="card.status" size="small" :type="card.status === 'ACTIVE' ? 'success' : 'info'">{{ card.status }}</el-tag>
              <div class="card-actions-header">
                <el-button link size="small" type="primary" @click="editCard(card)">编辑</el-button>
                <el-button link size="small" type="danger" @click="handleDelete(card)">删除</el-button>
              </div>
            </div>
          </template>
          <h4 class="card-title">{{ card.title }}</h4>
          <div v-if="card.plainLanguage" class="card-plain">
            <el-icon><ChatDotSquare /></el-icon>
            {{ card.plainLanguage }}
          </div>
          <div v-if="card.gjbReference" class="card-ref">
            <el-tag size="small" type="danger">GJB</el-tag>
            {{ card.gjbReference }}
          </div>
          <div v-if="card.tags" class="card-tags">
            <el-tag v-for="tag in cardTagList(card.tags)" :key="tag" size="small" type="info" style="margin:2px">
              {{ tag }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="cards.length === 0 && !loading" description="暂无知识卡片" :image-size="80" />

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showCreateDialog" :title="editingCard ? '编辑知识卡片' : '新建知识卡片'" width="560px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="如: 为什么要写战术技术指标符合性？" />
        </el-form-item>
        <el-form-item label="卡片类型">
          <el-select v-model="form.cardType" style="width:100%">
            <el-option label="GJB条款" value="GJB_CLAUSE" />
            <el-option label="编写指南" value="WRITING_GUIDE" />
            <el-option label="常见错误" value="COMMON_MISTAKE" />
            <el-option label="模板说明" value="TEMPLATE_NOTE" />
            <el-option label="通用" value="GENERAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="白话解释">
          <el-input v-model="form.plainLanguage" type="textarea" :rows="3" placeholder="用大白话解释这个条款是什么意思" />
        </el-form-item>
        <el-form-item label="GJB参考">
          <el-input v-model="form.gjbReference" placeholder="如: GJB/Z 170.4-2013 第5章" />
        </el-form-item>
        <el-form-item label="关联">
          <el-select v-model="form.targetTable" placeholder="关联数据表" clearable style="width:100%">
            <el-option label="模板" value="doc_template_v2" />
            <el-option label="章节" value="doc_chapter" />
            <el-option label="文档" value="doc_ledger" />
            <el-option label="标准元素" value="doc_template_element" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联ID">
          <el-input-number v-model="form.targetId" style="width:100%" :min="0" placeholder="可选" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="tagsInput" placeholder="用逗号分隔多个标签" />
          <span class="hint">如: 研制总结, 战术技术指标, GJB170</span>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, ChatDotSquare } from '@element-plus/icons-vue'
import {
  getKnowledgeCards, searchKnowledgeCards,
  createKnowledgeCard, updateKnowledgeCard, deleteKnowledgeCard,
  type KnowledgeCard
} from '@/api/knowledge-card'

const cards = ref<KnowledgeCard[]>([])
const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const showCreateDialog = ref(false)
const editingCard = ref<KnowledgeCard | null>(null)
const tagsInput = ref('')

const form = reactive({
  title: '', cardType: 'GJB_CLAUSE', plainLanguage: '', gjbReference: '',
  targetTable: '', targetId: null as number | null, status: 'ACTIVE', tags: ''
})

function cardTypeTag(type?: string) {
  const map: Record<string, string> = {
    GJB_CLAUSE: 'success', WRITING_GUIDE: 'warning', COMMON_MISTAKE: 'danger', TEMPLATE_NOTE: 'info'
  }
  return map[type || ''] || 'info'
}

function cardTagList(tags: string) {
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function resetForm() {
  Object.assign(form, { title: '', cardType: 'GJB_CLAUSE', plainLanguage: '', gjbReference: '', targetTable: '', targetId: null, status: 'ACTIVE', tags: '' })
  tagsInput.value = ''
  editingCard.value = null
}

async function loadCards() {
  loading.value = true
  try {
    if (filterType.value) {
      const res = await getKnowledgeCards(filterType.value)
      cards.value = res.data.data || []
    } else {
      const res = await getKnowledgeCards()
      cards.value = res.data.data || []
    }
  } catch { /* ignore */ }
  loading.value = false
}

async function search() {
  if (!searchKeyword.value) { loadCards(); return }
  loading.value = true
  try {
    const res = await searchKnowledgeCards(searchKeyword.value)
    cards.value = res.data.data || []
  } catch { /* ignore */ }
  loading.value = false
}

function editCard(card: KnowledgeCard) {
  editingCard.value = card
  Object.assign(form, {
    title: card.title, cardType: card.cardType || 'GJB_CLAUSE',
    plainLanguage: card.plainLanguage || '', gjbReference: card.gjbReference || '',
    targetTable: card.targetTable || '', targetId: card.targetId || null,
    status: card.status || 'ACTIVE', tags: card.tags || ''
  })
  tagsInput.value = card.tags || ''
  showCreateDialog.value = true
}

async function handleSave() {
  if (!form.title) { ElMessage.warning('请输入标题'); return }
  saving.value = true
  try {
    form.tags = tagsInput.value
    if (editingCard.value?.id) {
      await updateKnowledgeCard(editingCard.value.id, form as any)
      ElMessage.success('更新成功')
    } else {
      await createKnowledgeCard(form as any)
      ElMessage.success('创建成功')
    }
    showCreateDialog.value = false
    loadCards()
  } catch {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

async function handleDelete(card: KnowledgeCard) {
  try {
    await ElMessageBox.confirm(`删除知识卡片 "${card.title}"？`, '确认', { type: 'warning' })
    await deleteKnowledgeCard(card.id!)
    ElMessage.success('已删除')
    loadCards()
  } catch { /* cancelled */ }
}

onMounted(loadCards)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 16px; }
.header-actions { display: flex; gap: 8px; }

.knowledge-card-item { margin-bottom: 16px; cursor: pointer; }
.knowledge-card-item:hover { border-color: var(--el-color-primary); }
.card-header { display: flex; align-items: center; gap: 6px; }
.card-actions-header { margin-left: auto; display: flex; gap: 4px; }
.card-title { margin: 0 0 8px; font-size: 14px; }
.card-plain {
  display: flex; align-items: flex-start; gap: 4px;
  font-size: 13px; color: #67c23a; margin-bottom: 6px;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.card-ref { font-size: 12px; color: #606266; margin-bottom: 6px; display: flex; align-items: center; gap: 6px; }
.card-tags { display: flex; flex-wrap: wrap; gap: 2px; }
.hint { font-size: 11px; color: #909399; margin-top: 4px; }
</style>
