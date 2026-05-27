<template>
  <div class="checklist-section">
    <div class="checklist-header">
      <span class="checklist-title">文档清单</span>
      <el-tag size="small" type="info">{{ items.length }} 项</el-tag>
      <span v-if="stats" class="checklist-stats">
        已完成 {{ stats.completed }}/{{ stats.total }}
        <el-progress :percentage="stats.completionRate" :stroke-width="6" style="width:80px;display:inline-flex;vertical-align:middle" />
      </span>
      <el-button size="small" text @click="toggleAll">
        {{ allCollapsed ? '全部展开' : '全部折叠' }}
      </el-button>
      <el-button size="small" type="primary" :loading="generating" @click="handleGenerate">
        从模板库生成
      </el-button>
      <el-button size="small" type="success" :loading="syncing" @click="handleSyncToLedger">
        同步到台账
      </el-button>
    </div>

    <div v-if="items.length === 0" class="checklist-empty">暂无文档清单，点击"从模板库生成"自动创建</div>

    <div v-for="group in groupedItems" :key="group.category" class="category-group">
      <div class="category-header" @click="toggleCategory(group.category)">
        <el-icon class="category-arrow" :class="{ expanded: isExpanded(group.category) }">
          <ArrowRight />
        </el-icon>
        <span class="category-name">{{ group.category }}</span>
        <el-tag size="small" type="success" effect="plain">{{ group.completed }}/{{ group.total }}</el-tag>
        <el-progress :percentage="group.total > 0 ? Math.round(100 * group.completed / group.total) : 0"
          :stroke-width="4" style="width:60px" />
      </div>

      <div v-if="isExpanded(group.category)" class="category-items">
        <div v-for="item in group.items" :key="item.id" class="checklist-item">
          <span class="item-name" :title="item.docName">{{ item.docName }}</span>
          <el-select v-model="item.docStatus" size="small" style="width:100px" @change="(v:string) => handleStatusChange(item, v)">
            <el-option label="未开始" value="NOT_STARTED" />
            <el-option label="起草中" value="DRAFT" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="已批准" value="APPROVED" />
          </el-select>
          <el-input v-model="item.responsiblePerson" size="small" placeholder="负责人" style="width:90px"
            @blur="handleUpdateItem(item)" />
          <el-tag v-if="item.isCustom" size="small" type="warning">自定义</el-tag>
          <el-button size="small" text type="primary" @click="showRenameDialog(item)">改名</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(item)">删除</el-button>
        </div>
      </div>
    </div>

    <div class="checklist-add">
      <el-button size="small" type="success" @click="addVisible = true">
        <el-icon><Plus /></el-icon>添加自定义文档
      </el-button>
    </div>

    <!-- Add custom item dialog -->
    <el-dialog v-model="addVisible" title="添加自定义文档" width="420px">
      <el-form label-width="80px">
        <el-form-item label="文档名称">
          <el-input v-model="addForm.docName" placeholder="输入文档名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="addForm.category" filterable allow-create placeholder="选择或输入分类" style="width:100%">
            <el-option v-for="cat in categoryList" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddItem">确定</el-button>
      </template>
    </el-dialog>

    <!-- Rename dialog -->
    <el-dialog v-model="renameVisible" title="修改文档名称" width="420px">
      <el-form label-width="80px">
        <el-form-item label="文档名称">
          <el-input v-model="renameForm.docName" placeholder="输入新名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowRight } from '@element-plus/icons-vue'
import {
  getChecklist, getChecklistStats, generateChecklist,
  addCustomItem, updateChecklistItem, deleteChecklistItem,
  syncChecklistToLedger,
  type ProjectDocChecklistItem
} from '@/api/checklist'

const props = defineProps<{
  projectId: number
  stageId: number
  stageCode: string
}>()

const items = ref<ProjectDocChecklistItem[]>([])
const stats = ref<any>(null)
const generating = ref(false)
const syncing = ref(false)

// Track expanded state directly via reactive object — most reliable Vue 3 pattern
const expandedState = reactive<Record<string, boolean>>({})

function isExpanded(cat: string) {
  return expandedState[cat] !== false
}

function ensureExpanded(cat: string) {
  if (!(cat in expandedState)) {
    expandedState[cat] = true
  }
}

const allCollapsed = computed(() => {
  if (groupedItems.value.length === 0) return false
  return groupedItems.value.every(g => expandedState[g.category] === false)
})

const addVisible = ref(false)
const addForm = reactive({ docName: '', category: '' })

const renameVisible = ref(false)
const renameForm = reactive({ itemId: 0, docName: '' })

const groupedItems = computed(() => {
  const map = new Map<string, ProjectDocChecklistItem[]>()
  for (const item of items.value) {
    const cat = item.category || '其他'
    if (!map.has(cat)) map.set(cat, [])
    map.get(cat)!.push(item)
  }
  return Array.from(map.entries()).map(([category, catItems]) => ({
    category,
    total: catItems.length,
    completed: catItems.filter(i => i.docStatus === 'APPROVED').length,
    items: catItems
  }))
})

const categoryList = computed(() => {
  const cats = new Set<string>()
  for (const item of items.value) {
    if (item.category) cats.add(item.category)
  }
  return Array.from(cats).sort()
})

function toggleCategory(cat: string) {
  ensureExpanded(cat)
  expandedState[cat] = !expandedState[cat]
}

function toggleAll() {
  if (allCollapsed.value) {
    // expand all
    for (const g of groupedItems.value) {
      expandedState[g.category] = true
    }
  } else {
    // collapse all
    for (const g of groupedItems.value) {
      expandedState[g.category] = false
    }
  }
}

async function fetchItems() {
  try {
    const res = await getChecklist(props.projectId, props.stageId)
    items.value = res.data.data || []
  } catch { items.value = [] }
}

async function fetchStats() {
  try {
    const res = await getChecklistStats(props.projectId, props.stageId)
    stats.value = res.data.data
  } catch { stats.value = null }
}

async function handleGenerate() {
  generating.value = true
  try {
    const res = await generateChecklist(props.projectId, props.stageId)
    items.value = res.data.data || []
    // expand all after generation
    for (const key of Object.keys(expandedState)) {
      delete expandedState[key]
    }
    await fetchStats()
    ElMessage.success(`已从模板库生成 ${items.value.length} 个文档`)
  } catch {
    ElMessage.error('生成失败')
  } finally { generating.value = false }
}

async function handleSyncToLedger() {
  syncing.value = true
  try {
    const res = await syncChecklistToLedger(props.projectId, props.stageId)
    const count = res.data.data?.syncedCount || 0
    ElMessage.success(res.data.data?.message || `已同步 ${count} 条到台账`)
  } catch {
    ElMessage.error('同步失败')
  } finally { syncing.value = false }
}

async function handleAddItem() {
  if (!addForm.docName.trim()) {
    ElMessage.warning('请输入文档名称')
    return
  }
  try {
    await addCustomItem(props.projectId, props.stageId, addForm.docName.trim(), addForm.category.trim() || undefined)
    addVisible.value = false
    addForm.docName = ''
    addForm.category = ''
    await fetchItems()
    await fetchStats()
    ElMessage.success('已添加')
  } catch { /* handled */ }
}

async function handleStatusChange(item: ProjectDocChecklistItem, newStatus: string) {
  try {
    await updateChecklistItem(props.projectId, props.stageId, item.id!, { docStatus: newStatus })
    await fetchStats()
  } catch {
    await fetchItems()
  }
}

async function handleUpdateItem(item: ProjectDocChecklistItem) {
  try {
    await updateChecklistItem(props.projectId, props.stageId, item.id!, {
      responsiblePerson: item.responsiblePerson
    })
  } catch { /* ignore */ }
}

function showRenameDialog(item: ProjectDocChecklistItem) {
  renameForm.itemId = item.id!
  renameForm.docName = item.docName
  renameVisible.value = true
}

async function handleRename() {
  if (!renameForm.docName.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  try {
    await updateChecklistItem(props.projectId, props.stageId, renameForm.itemId, { docName: renameForm.docName.trim() })
    renameVisible.value = false
    await fetchItems()
    ElMessage.success('已更新')
  } catch { /* handled */ }
}

async function handleDelete(item: ProjectDocChecklistItem) {
  try {
    await ElMessageBox.confirm(`确定删除"${item.docName}"吗？`, '确认删除', { type: 'warning' })
  } catch { return }
  try {
    await deleteChecklistItem(props.projectId, props.stageId, item.id!)
    await fetchItems()
    await fetchStats()
    ElMessage.success('已删除')
  } catch { /* handled */ }
}

watch(() => props.stageId, () => {
  if (props.stageId) {
    // Reset expanded state - all expanded by default
    for (const key of Object.keys(expandedState)) {
      delete expandedState[key]
    }
    fetchItems(); fetchStats()
  }
})

onMounted(() => {
  if (props.stageId) { fetchItems(); fetchStats() }
})
</script>

<style scoped>
.checklist-section {
  margin-top: 10px;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}

.checklist-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.checklist-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.checklist-stats {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.checklist-empty {
  color: #c0c4cc;
  font-size: 12px;
  text-align: center;
  padding: 12px 0;
}

/* Category groups */
.category-group {
  margin-bottom: 2px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}

.category-header:hover {
  background: #f0f2f5;
}

.category-arrow {
  font-size: 12px;
  color: #909399;
  transition: transform 0.2s;
}

.category-arrow.expanded {
  transform: rotate(90deg);
}

.category-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.category-items {
  padding: 0 10px 6px 22px;
}

/* Items */
.checklist-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  font-size: 12px;
}

.item-name {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checklist-add {
  margin-top: 10px;
}
</style>
