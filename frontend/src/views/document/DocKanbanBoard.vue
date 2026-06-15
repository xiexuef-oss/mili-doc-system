<template>
  <div class="kanban-page">
    <!-- Header -->
    <div class="kanban-header">
      <div class="header-left">
        <el-select v-model="selectedStageId" placeholder="筛选阶段" clearable style="width:200px" @change="loadKanban">
          <el-option v-for="s in stages" :key="s.id" :label="s.stageName" :value="s.id" />
        </el-select>
        <el-select v-model="selectedCategory" placeholder="筛选类别" clearable style="width:180px">
          <el-option v-for="cat in availableCategories" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <el-input v-model="searchText" placeholder="搜索文档..." clearable size="small" style="width:200px" />
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="kanban">看板</el-radio-button>
          <el-radio-button value="tree">树形</el-radio-button>
        </el-radio-group>
      </div>
      <div class="header-right">
        <el-button type="success" :loading="syncing" @click="handleSyncFromCatalog" :disabled="!selectedStageId">
          <el-icon><RefreshRight /></el-icon>从目录同步
        </el-button>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>新建文档条目
        </el-button>
      </div>
    </div>

    <!-- Tree view -->
    <div v-if="viewMode === 'tree'" class="tree-view">
      <div class="tree-toolbar">
        <el-input v-model="treeFilter" placeholder="搜索文档..." clearable size="small" style="width:300px" />
        <el-button size="small" @click="expandAll">全部展开</el-button>
        <el-button size="small" @click="collapseAll">全部折叠</el-button>
      </div>
      <div class="tree-card">
        <el-tree
          ref="treeRef"
          :key="treeRenderKey"
          :data="treeData"
          :props="treeProps"
          :default-expanded-keys="expandedKeys"
          node-key="key"
          :filter-node-method="filterTreeNode"
          :default-expand-all="false"
          highlight-current
          @node-click="handleTreeNodeClick"
        >
          <template #default="{ data }">
            <div class="tree-node-content">
              <!-- Category node -->
              <template v-if="data.type === 'category'">
                <el-icon style="margin-right:6px;color:var(--el-color-success)"><FolderOpened /></el-icon>
                <span class="tree-cat-label">{{ data.label }}</span>
                <el-tag size="small" type="success" style="margin-left:8px">{{ data.children?.length || 0 }} 份</el-tag>
              </template>
              <!-- Document node -->
              <template v-else>
                <span class="tree-doc-code">{{ data.docCode || '—' }}</span>
                <span class="tree-doc-name">{{ data.docName }}</span>
                <el-tag v-if="data.docType" size="small" type="info" style="margin-left:4px">{{ docTypeLabel(data.docType) }}</el-tag>
                <el-tag size="small" :type="statusTagType(data.lifecycleStatus)" style="margin-left:4px">{{ statusLabel(data.lifecycleStatus) }}</el-tag>
                <el-tag v-if="data.securityLevel" size="small" :type="data.securityLevel === 'TOP_SECRET' || data.securityLevel === 'SECRET' ? 'danger' : 'warning'" style="margin-left:4px">
                  {{ securityLabel(data.securityLevel) }}
                </el-tag>
                <span class="tree-actions" @click.stop>
                  <el-button v-if="data.lifecycleStatus === 'PLANNED'" size="small" type="primary" link @click="generateDraft(data)">AI生成</el-button>
                  <el-button v-if="data.lifecycleStatus === 'DRAFTING'" size="small" type="primary" link @click="goAssembly(data)">章节编辑</el-button>
                  <el-button v-if="data.lifecycleStatus === 'DRAFTING'" size="small" type="success" link @click="exportDocx(data)">导出</el-button>
                  <el-button v-if="data.lifecycleStatus === 'DRAFTING'" size="small" type="warning" link @click="aiProofread(data)">校对</el-button>
                  <el-button v-if="data.lifecycleStatus === 'CHECKING'" size="small" type="warning" link @click="aiPreReview(data)">预评审</el-button>
                  <el-button v-if="data.lifecycleStatus === 'RELEASED'" size="small" type="success" link @click="exportDocx(data)">导出</el-button>
                  <el-select
                    v-if="canTransitionFrom(data.lifecycleStatus)"
                    :model-value="''"
                    placeholder="转移"
                    size="small"
                    style="width:80px;margin-left:4px"
                    @change="(val: string) => doTransition(data.id!, val)"
                  >
                    <el-option v-for="t in allowedTransitions(data.lifecycleStatus)" :key="t" :label="statusLabel(t)" :value="t" />
                  </el-select>
                </span>
              </template>
            </div>
          </template>
        </el-tree>
      </div>
    </div>

    <!-- Kanban columns -->
    <div v-show="viewMode !== 'tree'" class="kanban-board">
      <div
        v-for="col in enrichedColumns"
        :key="col.key"
        class="kanban-column"
        :class="{ 'drag-over': dragOverColumn === col.key }"
        @dragover.prevent="dragOverColumn = col.key"
        @dragleave="dragOverColumn = dragOverColumn === col.key ? null : dragOverColumn"
        @drop="handleDrop($event, col.key)"
      >
        <div class="column-header">
          <span class="column-title">{{ col.label }}</span>
          <el-tag size="small" :type="col.tagType">{{ getColumnCount(col.key) }}</el-tag>
        </div>
        <div class="column-body">
          <template v-for="grp in col.groups" :key="grp.category || '__flat__'">
            <div v-if="grp.category" class="col-cat-header" @click.stop="toggleCat(col.key + '-' + grp.category)" style="cursor:pointer">
              <el-icon style="margin-right:4px;font-size:12px"><component :is="collapsedCats[col.key + '-' + grp.category] ? 'ArrowRight' : 'ArrowDown'" /></el-icon>
              <span>{{ grp.category }}</span>
              <el-tag size="small">{{ grp.items.length }}</el-tag>
            </div>
            <div
              v-show="!collapsedCats[col.key + '-' + grp.category]"
              v-for="item in grp.items"
              :key="item.id"
              class="kanban-card"
              :draggable="canTransitionFrom(col.key)"
              @dragstart="handleDragStart($event, item)"
              @click="showDetail(item)"
            >
              <div class="card-code">
                <span v-if="item.catalogId" class="catalog-badge" title="来自文档目录">📋</span>
                {{ item.docCode || '—' }}
              </div>
              <div class="card-name">{{ item.docName }}</div>
              <div class="card-tags">
                <el-tag v-if="item.docCategory" size="small" type="success">{{ item.docCategory }}</el-tag>
                <el-tag size="small" type="info">{{ docTypeLabel(item.docType) }}</el-tag>
                <el-tag v-if="item.stageCode" size="small" type="warning">{{ item.stageCode }}</el-tag>
                <el-tag v-if="item.securityLevel" size="small" :type="item.securityLevel === 'TOP_SECRET' || item.securityLevel === 'SECRET' ? 'danger' : 'warning'">
                  {{ securityLabel(item.securityLevel) }}
                </el-tag>
              </div>
              <div v-if="completenessMap.get(item.id!)" class="card-completeness" @click.stop="goAssembly(item)">
                <CompletenessProgressBar
                  :passed="completenessMap.get(item.id!)!.passed"
                  :warnings="completenessMap.get(item.id!)!.warnings"
                  :errors="completenessMap.get(item.id!)!.errors"
                  :total="completenessMap.get(item.id!)!.total"
                />
              </div>
              <div v-if="col.key === 'PLANNED'" class="card-actions">
                <el-button size="small" type="primary" link @click.stop="generateDraft(item)">AI 生成初稿</el-button>
                <el-button size="small" type="danger" link @click.stop="handleDelete(item)">删除</el-button>
              </div>
              <div v-if="col.key === 'RELEASED'" class="card-actions">
                <el-button size="small" type="success" link @click.stop="exportDocx(item)">导出.docx</el-button>
              </div>
              <div v-if="col.key === 'DRAFTING'" class="card-actions">
                <el-button size="small" type="primary" link @click.stop="goAssembly(item)">章节编辑</el-button>
                <el-button size="small" type="success" link @click.stop="exportDocx(item)">导出.docx</el-button>
                <el-button size="small" type="warning" link @click.stop="aiProofread(item)">AI 校对</el-button>
                <el-button size="small" type="danger" link @click.stop="handleDelete(item)">删除</el-button>
              </div>
              <div v-if="col.key === 'CHECKING'" class="card-actions">
                <el-button size="small" type="warning" link @click.stop="aiPreReview(item)">AI 预评审</el-button>
              </div>
            </div>
          </template>
          <div v-if="getColumnCount(col.key) === 0" class="column-empty">拖拽卡片至此列</div>
        </div>
      </div>
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建文档台账条目" width="480px" @closed="resetForm">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="文档名称" required>
          <el-input v-model="createForm.docName" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="文档编号">
          <el-input v-model="createForm.docCode" placeholder="如 GJB-XXX-001" />
        </el-form-item>
        <el-form-item label="文档类别">
          <el-select v-model="createForm.docCategory" style="width:100%" @change="onCreateCategoryChange" placeholder="选择类别">
            <el-option v-for="c in docCategories" :key="c.dictCode" :label="c.dictName" :value="c.dictCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="createForm.docType" style="width:100%" :disabled="!createForm.docCategory" placeholder="选择具体类型">
            <el-option v-for="t in filteredDocTypes" :key="t.dictCode" :label="t.dictName" :value="t.dictCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="密级">
          <el-select v-model="createForm.securityLevel" style="width:100%">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="秘密" value="SECRET" />
            <el-option label="机密" value="CONFIDENTIAL" />
            <el-option label="绝密" value="TOP_SECRET" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必需">
          <el-switch v-model="createForm.requiredFlag" />
        </el-form-item>
        <el-form-item label="所属阶段">
          <el-select v-model="createForm.stageId" style="width:100%" clearable placeholder="选择阶段">
            <el-option v-for="s in stages" :key="s.id" :label="s.stageName" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="showDrawer" title="文档详情" size="480px">
      <template v-if="selectedItem">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="ID">{{ selectedItem.id }}</el-descriptions-item>
          <el-descriptions-item label="文档编号">{{ selectedItem.docCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文档名称">{{ selectedItem.docName }}</el-descriptions-item>
          <el-descriptions-item label="文档类别">
            <el-tag v-if="selectedItem.docCategory" size="small" type="success">{{ categoryLabel(selectedItem.docCategory) }}</el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="文档类型">{{ docTypeLabel(selectedItem.docType) }}</el-descriptions-item>
          <el-descriptions-item label="阶段">{{ selectedItem.stageCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="密级">{{ securityLabel(selectedItem.securityLevel || '') }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusTagType(selectedItem.lifecycleStatus!)">{{ statusLabel(selectedItem.lifecycleStatus!) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否必需">
            <el-tag :type="selectedItem.requiredFlag ? 'danger' : 'info'" size="small">{{ selectedItem.requiredFlag ? '必需' : '非必需' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源目录ID">
            <span v-if="selectedItem.catalogId" style="color:var(--el-color-success)">目录 #{{ selectedItem.catalogId }}</span>
            <span v-else style="color:var(--el-text-color-placeholder)">手动创建</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ selectedItem.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Status transition -->
        <div style="margin-top:16px">
          <h4 style="margin-bottom:8px">状态转移</h4>
          <el-select v-model="targetStatus" placeholder="选择目标状态" style="width:100%">
            <el-option
              v-for="s in allowedTransitions(selectedItem.lifecycleStatus!)"
              :key="s"
              :label="statusLabel(s)"
              :value="s"
            />
          </el-select>
          <el-button
            type="primary"
            :disabled="!targetStatus"
            :loading="transitioning"
            style="margin-top:8px;width:100%"
            @click="handleTransition"
          >转移</el-button>
        </div>

        <!-- Logs timeline -->
        <div style="margin-top:24px">
          <h4 style="margin-bottom:8px">操作日志</h4>
          <el-timeline v-if="logs.length > 0">
            <el-timeline-item
              v-for="log in logs"
              :key="log.id"
              :timestamp="log.operatedAt"
              placement="top"
            >
              <span v-if="log.fromStatus">{{ statusLabel(log.fromStatus) }} → </span>
              {{ statusLabel(log.toStatus) }}
              <div v-if="log.remark" style="color:#999;font-size:12px">{{ log.remark }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作日志" :image-size="40" />
        </div>
      </template>
    </el-drawer>

    <!-- AI Draft Generation Dialog -->
    <el-dialog v-model="showDraftDialog" title="AI 生成文档初稿" width="700px" :close-on-click-modal="false" @closed="stopDraftGeneration">
      <div v-if="draftItem" style="margin-bottom:12px">
        <el-tag size="small" type="info">{{ draftItem.docCode }}</el-tag>
        <span style="margin-left:8px;font-weight:500">{{ draftItem.docName }}</span>
      </div>
      <el-alert v-if="!healthOk" title="AI 服务未连接，无法生成初稿" type="error" :closable="false" show-icon style="margin-bottom:12px" />
      <el-form label-width="100px">
        <el-form-item label="参考目录">
          <div style="display:flex;align-items:center;gap:8px;width:100%">
            <el-select v-model="draftCatalogId" placeholder="可选，选择目录条目提供更精确的文档规格" filterable clearable style="flex:1" :disabled="draftStreaming" :loading="catalogLoading">
              <el-option v-for="c in catalogEntries" :key="c.id" :label="`${c.docCode} - ${c.docName}`" :value="c.id!" />
            </el-select>
            <el-icon v-if="catalogLoading" class="is-loading"><Loading /></el-icon>
          </div>
          <div style="font-size:12px;color:var(--el-text-color-secondary);margin-top:4px">可选：选择目录条目可提供更精确的文档规格。不选时使用当前文档元数据，AI 仍会参考模板、输入文件和军标来生成。</div>
        </el-form-item>
      </el-form>
      <el-alert v-if="!catalogLoading && catalogEntries.length === 0" :title="`该项目暂无目录条目，将使用当前文档元数据生成（仍会参考输入文件和军标）`" type="info" :closable="false" show-icon style="margin-top:8px" />
      <div v-if="draftContent || draftStreaming" class="draft-output">
        <div class="draft-output-header">
          <span>生成结果</span>
          <span v-if="draftStreaming" style="color:var(--el-color-warning)">生成中，已接收 {{ draftCharCount }} 字...</span>
        </div>
        <div class="draft-output-body">
          <div class="markdown-body" v-html="renderMarkdown(draftContent)" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showDraftDialog = false" :disabled="draftStreaming">关闭</el-button>
        <el-button v-if="!draftStreaming" type="primary" @click="startDraftGeneration" :disabled="!healthOk">开始生成</el-button>
        <el-button v-else type="danger" plain @click="stopDraftGeneration">停止生成</el-button>
        <el-button v-if="draftContent && !draftStreaming" type="success" :loading="draftSaving" @click="handleSaveDraft">保存为文档</el-button>
      </template>
    </el-dialog>

    <!-- AI Proofread Result Dialog -->
    <el-dialog v-model="showProofreadDialog" title="AI 校对结果" width="560px">
      <div v-if="proofreadItem" style="margin-bottom:12px">
        <el-tag size="small" type="info">{{ proofreadItem.docCode }}</el-tag>
        <span style="margin-left:8px;font-weight:500">{{ proofreadItem.docName }}</span>
      </div>
      <div v-loading="proofreading">
        <div v-if="proofreadResult">
          <div v-if="proofreadResult.summary" style="margin-bottom:12px">
            <el-alert :title="proofreadResult.summary" type="info" :closable="false" show-icon />
          </div>
          <el-table :data="proofreadResult.issues || []" border size="small" max-height="350">
            <el-table-column label="严重度" width="80">
              <template #default="{ row }">
                <el-tag :type="row.severity === 'ERROR' ? 'danger' : 'warning'" size="small">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="位置" width="150" />
            <el-table-column prop="description" label="问题描述" min-width="200" />
            <el-table-column prop="suggestion" label="建议" min-width="200" />
          </el-table>
          <el-empty v-if="!proofreadResult.issues?.length" description="未发现问题" :image-size="40" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showProofreadDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Docx Export Dialog -->
    <DocxExportDialog
      v-if="showExportItem"
      v-model="showExportDialog"
      :doc-ledger-id="showExportItem.id!"
      :doc-name="showExportItem.docName"
      :chapter-count="exportChapterCount"
      :fill-rate="exportFillRate"
      @done="loadKanban"
      @closed="showExportItem = null; showExportDialog = false"
    />

    <!-- AI Pre-Review Result Dialog -->
    <el-dialog v-model="showPreReviewDialog" title="AI 预评审结果" width="560px">
      <div v-if="preReviewItem" style="margin-bottom:12px">
        <el-tag size="small" type="info">{{ preReviewItem.docCode }}</el-tag>
        <span style="margin-left:8px;font-weight:500">{{ preReviewItem.docName }}</span>
      </div>
      <div v-loading="preReviewing">
        <div v-if="preReviewResult">
          <div style="display:flex;gap:16px;margin-bottom:12px">
            <el-statistic title="合规度" :value="preReviewResult.complianceScore || 0" suffix="/100" />
            <el-statistic title="完备度" :value="preReviewResult.completenessScore || 0" suffix="/100" />
          </div>
          <el-alert v-if="preReviewResult.summary" :title="preReviewResult.summary" type="warning" :closable="false" show-icon style="margin-bottom:12px" />
          <div v-if="preReviewResult.issues?.length">
            <h4 style="margin:8px 0">评审意见</h4>
            <div v-for="(issue, i) in preReviewResult.issues" :key="i" style="margin-bottom:8px;padding:8px;background:var(--el-fill-color-lighter);border-radius:4px">
              <el-tag size="small" :type="issue.status === 'NON_COMPLIANT' ? 'danger' : 'warning'" style="margin-right:8px">{{ issue.status }}</el-tag>
              <span v-if="issue.clauseRef" style="color:var(--el-text-color-secondary);margin-right:8px">[{{ issue.clauseRef }}]</span>
              {{ issue.description }}
            </div>
          </div>
          <div v-if="preReviewResult.recommendations?.length">
            <h4 style="margin:8px 0">改进建议</h4>
            <ul style="padding-left:20px">
              <li v-for="(rec, i) in preReviewResult.recommendations" :key="i" style="margin-bottom:4px">{{ rec }}</li>
            </ul>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPreReviewDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useProjectStore } from '@/stores/project'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, FolderOpened, Loading, ArrowRight, ArrowDown } from '@element-plus/icons-vue'
import {
  getKanbanData, createDocLedger, transitionStatus, getDocLedgerLogs, syncFromChecklist, deleteDocLedger,
  type DocLedgerItem, type DocLedgerLogItem
} from '@/api/doc-ledger'
import { getCompletionSummaryBatch, getCompletionSummary } from '@/api/doc-chapter'
import CompletenessProgressBar from '@/components/CompletenessProgressBar.vue'
import DocxExportDialog from '@/components/DocxExportDialog.vue'
import { getProjectStages, type ProjectStageItem } from '@/api/project-stage'
import { getDocCatalogsByProject, type DocCatalogItem } from '@/api/doc-catalog'
import {
  proofread, preReview, streamDraft, saveDraft
} from '@/api/ai'
import { getDictItems, type DictItem } from '@/api/dict'

const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.projectId)
const pStore = useProjectStore()

// Auto-refresh when chat agent generates documents
watch(() => pStore.kanbanRefresh, () => { loadKanban() })

const completenessMap = ref<Map<number, { passed: number; warnings: number; errors: number; total: number }>>(new Map())

const columns = [
  { key: 'PLANNED',  label: '策划',     tagType: 'info' },
  { key: 'DRAFTING', label: '起草',     tagType: 'info' },
  { key: 'CHECKING', label: '校对',     tagType: 'warning' },
  { key: 'REVIEWING',label: '评审',     tagType: 'warning' },
  { key: 'APPROVING',label: '批准',     tagType: 'warning' },
  { key: 'RELEASED', label: '已发布',   tagType: 'success' },
  { key: 'ARCHIVED', label: '已归档',   tagType: 'info' }
]

const TRANSITIONS: Record<string, string[]> = {
  PLANNED:  ['DRAFTING'],
  DRAFTING: ['CHECKING', 'PLANNED'],
  CHECKING: ['REVIEWING', 'DRAFTING'],
  REVIEWING:['APPROVING', 'DRAFTING'],
  APPROVING:['RELEASED', 'DRAFTING'],
  RELEASED: ['ARCHIVED', 'DRAFTING']
}

const kanbanData = ref<Record<string, DocLedgerItem[]>>({})
const stages = ref<ProjectStageItem[]>([])
const selectedStageId = ref<number | null>(null)
const selectedCategory = ref('')
const searchText = ref('')
const viewMode = ref<'kanban' | 'tree'>('kanban')
const collapsedCats = ref<Record<string, boolean>>({})
const showCreateDialog = ref(false)
const creating = ref(false)
const showDrawer = ref(false)
const selectedItem = ref<DocLedgerItem | null>(null)
const targetStatus = ref('')
const transitioning = ref(false)
const logs = ref<DocLedgerLogItem[]>([])
const dragOverColumn = ref<string | null>(null)
const syncing = ref(false)
let draggedItem: DocLedgerItem | null = null

const createForm = reactive({
  docName: '', docCode: '', docCategory: '', docType: '',
  securityLevel: 'INTERNAL', requiredFlag: true, stageId: null as number | null
})
const docCategories = ref<DictItem[]>([])
const docTypes = ref<DictItem[]>([])
const filteredDocTypes = ref<DictItem[]>([])

function onCreateCategoryChange(categoryCode: string) {
  createForm.docType = ''
  filteredDocTypes.value = categoryCode
    ? docTypes.value.filter(t => t.parentCode === categoryCode)
    : []
}

function getColumnItems(key: string) {
  let items = kanbanData.value[key] || []
  if (selectedCategory.value) {
    items = items.filter(i => i.docCategory === selectedCategory.value)
  }
  if (searchText.value) {
    const s = searchText.value.toLowerCase()
    items = items.filter(i => 
      (i.docName || '').toLowerCase().includes(s) ||
      (i.docCode || '').toLowerCase().includes(s) ||
      (i.docType || '').toLowerCase().includes(s)
    )
  }
  return items
}
function getColumnCount(key: string) { return getColumnItems(key).length }

const enrichedColumns = computed(() => {
  return columns.map(col => {
    const items = getColumnItems(col.key)
    if (viewMode.value === 'list') {
      return { ...col, groups: [{ category: '', items }] }
    }
    // kanban mode: group by category
    const map = new Map<string, DocLedgerItem[]>()
    for (const item of items) {
      const cat = item.docCategory || '其他'
      if (!map.has(cat)) map.set(cat, [])
      map.get(cat)!.push(item)
    }
    const groups = Array.from(map.entries()).map(([category, catItems]) => ({ category, items: catItems }))
    return { ...col, groups }
  })
})

const availableCategories = computed(() => {
  const cats = new Set<string>()
  for (const col of columns) {
    for (const item of (kanbanData.value[col.key] || [])) {
      if (item.docCategory) cats.add(item.docCategory)
    }
  }
  return Array.from(cats).sort()
})

// ---- Tree view ----
const treeFilter = ref('')
const treeRef = ref<any>(null)
const expandedKeys = ref<string[]>([])
const treeRenderKey = ref(0)

interface TreeNode {
  key: string
  label: string
  type: 'category' | 'document'
  children?: TreeNode[]
  // document leaf props
  docCode?: string
  docName?: string
  docType?: string
  securityLevel?: string
  lifecycleStatus?: string
  id?: number
  catalogId?: number
}

const treeProps = { children: 'children', label: 'label' }

const treeData = computed<TreeNode[]>(() => {
  const allItems: DocLedgerItem[] = []
  // In tree mode, ignore column grouping — collect all items across all statuses
  for (const col of columns) {
    for (const item of (kanbanData.value[col.key] || [])) {
      if (selectedCategory.value && item.docCategory !== selectedCategory.value) continue
      allItems.push(item)
    }
  }

  // Group: category → documents (2-level tree)
  const catMap = new Map<string, DocLedgerItem[]>()
  for (const item of allItems) {
    const cat = item.docCategory || '其他'
    if (!catMap.has(cat)) catMap.set(cat, [])
    catMap.get(cat)!.push(item)
  }

  const nodes: TreeNode[] = []
  const sortedCats = Array.from(catMap.keys()).sort()
  for (const cat of sortedCats) {
    const docItems = catMap.get(cat)!
    const catLabel = categoryNameMap.value[cat] || cat
    const docNodes: TreeNode[] = docItems.map(doc => ({
      key: `doc-${doc.id}`,
      label: `${doc.docCode || '—'} ${doc.docName}`,
      type: 'document' as const,
      docCode: doc.docCode,
      docName: doc.docName,
      docType: doc.docType,
      securityLevel: doc.securityLevel,
      lifecycleStatus: doc.lifecycleStatus,
      id: doc.id,
      catalogId: doc.catalogId
    }))
    nodes.push({
      key: `cat-${cat}`,
      label: catLabel,
      type: 'category',
      children: docNodes
    })
  }
  return nodes
})

function filterTreeNode(value: string, data: TreeNode): boolean {
  if (!value) return true
  const lower = value.toLowerCase()
  if (data.label.toLowerCase().includes(lower)) return true
  if (data.docCode?.toLowerCase().includes(lower)) return true
  if (data.docName?.toLowerCase().includes(lower)) return true
  return false
}

function expandAll() {
  const keys = treeData.value.map(n => n.key)
  expandedKeys.value = keys
  treeRenderKey.value++
}

function toggleCat(catKey: string) {
  collapsedCats.value = { ...collapsedCats.value, [catKey]: !collapsedCats.value[catKey] }
}

function collapseAll() {
  expandedKeys.value = []
  treeRenderKey.value++
}

function handleTreeNodeClick(data: TreeNode) {
  if (data.type === 'document') {
    const item: DocLedgerItem = {
      id: data.id,
      projectId,
      docCode: data.docCode,
      docName: data.docName || '',
      docType: data.docType,
      docCategory: data.label,
      securityLevel: data.securityLevel,
      lifecycleStatus: data.lifecycleStatus,
      catalogId: data.catalogId
    }
    showDetail(item)
  }
}

function securityLabel(s: string) {
  const map: Record<string, string> = {
    PUBLIC:'公开',INTERNAL:'内部',SECRET:'秘密',CONFIDENTIAL:'机密',TOP_SECRET:'绝密'
  }
  return map[s] || s
}
const typeNameMap = ref<Record<string, string>>({})
const categoryNameMap = ref<Record<string, string>>({})
function categoryLabel(c?: string) {
  if (!c) return '-'
  return categoryNameMap.value[c] || c
}
function docTypeLabel(t?: string) {
  if (!t) return '-'
  return typeNameMap.value[t] || t
}
function statusLabel(s: string) {
  const map: Record<string, string> = {
    PLANNED:'策划',DRAFTING:'起草',CHECKING:'校对',REVIEWING:'评审',
    APPROVING:'批准',RELEASED:'已发布',ARCHIVED:'已归档'
  }
  return map[s] || s
}
function statusTagType(s: string) {
  const map: Record<string, string> = {
    PLANNED:'info',DRAFTING:'info',CHECKING:'warning',REVIEWING:'warning',
    APPROVING:'warning',RELEASED:'success',ARCHIVED:'info'
  }
  return map[s] || 'info'
}
function allowedTransitions(current: string) { return TRANSITIONS[current] || [] }
function canTransitionFrom(key: string) { return !!(TRANSITIONS[key] && TRANSITIONS[key].length > 0) }

function goAssembly(item: DocLedgerItem) {
  router.push({ name: 'DocAssembly', params: { projectId, docLedgerId: item.id } })
}

function exportDocx(item: DocLedgerItem) {
  showExportItem.value = item

  const cached = completenessMap.value.get(item.id!)
  if (cached) {
    exportChapterCount.value = cached.total
    exportFillRate.value = cached.total > 0 ? Math.round(cached.passed / cached.total * 100) : 0
  } else {
    fetchExportStats(item.id!)
  }

  showExportDialog.value = true
}

async function fetchExportStats(ledgerId: number) {
  try {
    const res = await getCompletionSummary(ledgerId)
    const s = res.data.data
    if (s) {
      exportChapterCount.value = s.total || 0
      exportFillRate.value = s.total > 0 ? Math.round((s.filled || 0) / s.total * 100) : 0
    }
  } catch { /* ignore */ }
}

const showExportDialog = ref(false)
const showExportItem = ref<DocLedgerItem | null>(null)
const exportChapterCount = ref(0)
const exportFillRate = ref(0)

async function handleDelete(item: DocLedgerItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档"${item.docName}"吗？此操作将同时删除关联的章节内容和状态日志，不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteDocLedger(item.id!)
    ElMessage.success('已删除')
    await loadKanban()
  } catch {
    // user cancelled or error
  }
}

async function loadCompletenessForAll() {
  const allItems = Object.values(kanbanData.value).flat().filter(i => i.id)
  if (allItems.length === 0) return
  try {
    const ids = allItems.map(i => i.id!)
    const res = await getCompletionSummaryBatch(ids)
    const batch: Record<number, any> = res.data.data || {}
    // Build new map and replace in one operation to minimize reactive updates
    const newMap = new Map<number, { passed: number; warnings: number; errors: number; total: number }>()
    for (const [key, s] of Object.entries(batch)) {
      const id = Number(key)
      if (s && (s.total > 0 || s.filled > 0 || s.partial > 0 || s.empty > 0)) {
        newMap.set(id, {
          passed: s.filled || 0,
          warnings: s.partial || 0,
          errors: s.empty || 0,
          total: s.total || 0
        })
      }
    }
    completenessMap.value = newMap
  } catch { /* ignore */ }
}

async function loadKanban() {
  try {
    const res = await getKanbanData(projectId, selectedStageId.value || undefined)
    kanbanData.value = res.data.data
  } catch { /* ignore */ }
}
async function loadStages() {
  try {
    const res = await getProjectStages(projectId)
    stages.value = res.data.data || []
  } catch { /* ignore */ }
}

function handleDragStart(_ev: DragEvent, item: DocLedgerItem) { draggedItem = item }
function handleDrop(_ev: DragEvent, targetColumn: string) {
  dragOverColumn.value = null
  if (!draggedItem || draggedItem.lifecycleStatus === targetColumn) return
  const allowed = allowedTransitions(draggedItem.lifecycleStatus!)
  if (!allowed.includes(targetColumn)) {
    ElMessage.warning(`不允许从 ${statusLabel(draggedItem.lifecycleStatus!)} 直接转移到 ${statusLabel(targetColumn)}`)
    return
  }
  doTransition(draggedItem.id!, targetColumn)
}
async function doTransition(id: number, target: string) {
  try {
    await transitionStatus(id, target)
    ElMessage.success('状态转移成功')
    loadKanban()
  } catch {
    ElMessage.error('状态转移失败')
  }
}

async function handleCreate() {
  if (!createForm.docName) { ElMessage.warning('请输入文档名称'); return }
  creating.value = true
  try {
    await createDocLedger({ ...createForm, projectId } as any)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    loadKanban()
  } catch {
    ElMessage.error('创建失败')
  }
  creating.value = false
}
function resetForm() {
  Object.assign(createForm, { docName:'',docCode:'',docCategory:'',docType:'',securityLevel:'INTERNAL',requiredFlag:true,stageId:null })
}

async function handleSyncFromCatalog() {
  if (!selectedStageId.value) { ElMessage.warning('请先选择阶段'); return }
  syncing.value = true
  try {
    const res = await syncFromChecklist(projectId, selectedStageId.value)
    const count = res.data?.data?.syncedCount || 0
    ElMessage.success(`已从阶段文档清单同步 ${count} 个台账条目`)
    loadKanban()
  } catch {
    ElMessage.error('同步失败')
  }
  syncing.value = false
}

async function showDetail(item: DocLedgerItem) {
  selectedItem.value = item
  targetStatus.value = ''
  showDrawer.value = true
  try {
    const res = await getDocLedgerLogs(item.id!)
    logs.value = res.data.data || []
  } catch { logs.value = [] }
}
async function handleTransition() {
  if (!selectedItem.value || !targetStatus.value) return
  transitioning.value = true
  try {
    await transitionStatus(selectedItem.value.id!, targetStatus.value)
    ElMessage.success('状态转移成功')
    selectedItem.value.lifecycleStatus = targetStatus.value
    targetStatus.value = ''
    loadKanban()
    // Refresh logs
    const res = await getDocLedgerLogs(selectedItem.value.id!)
    logs.value = res.data.data || []
  } catch {
    ElMessage.error('状态转移失败')
  }
  transitioning.value = false
}

// ---- AI: Draft Generation ----
const showDraftDialog = ref(false)
const draftItem = ref<DocLedgerItem | null>(null)
const draftCatalogId = ref<number | null>(null)
const draftStreaming = ref(false)
const draftContent = ref('')
const draftCharCount = ref(0)
const draftSaving = ref(false)
const catalogEntries = ref<DocCatalogItem[]>([])
let draftAbortController: AbortController | null = null

const catalogLoading = ref(false)

async function generateDraft(item: DocLedgerItem) {
  draftItem.value = item
  draftContent.value = ''
  draftCharCount.value = 0
  draftCatalogId.value = item.catalogId || null
  catalogEntries.value = []
  catalogLoading.value = true
  showDraftDialog.value = true
  try {
    const res = await getDocCatalogsByProject(projectId)
    catalogEntries.value = res.data.data || []
    // Auto-select if document has no catalogId and there's exactly one entry
    if (!draftCatalogId.value && catalogEntries.value.length === 1) {
      draftCatalogId.value = catalogEntries.value[0].id!
    }
  } catch { catalogEntries.value = [] }
  finally { catalogLoading.value = false }
}

function startDraftGeneration() {
  if (!healthOk.value) {
    ElMessage.error('本地大模型未连接，无法生成初稿')
    return
  }

  draftStreaming.value = true
  draftContent.value = ''
  draftCharCount.value = 0

  draftAbortController = streamDraft(
    projectId,
    draftCatalogId.value,
    draftItem.value?.id ?? null,
    (chunk: string) => {
      draftContent.value += chunk
      draftCharCount.value = draftContent.value.length
    },
    (fullText: string) => {
      draftContent.value = fullText
      draftCharCount.value = fullText.length
      draftStreaming.value = false
      ElMessage.success(`初稿生成完成，共 ${draftCharCount.value} 字`)
    },
    (err: Error) => {
      draftStreaming.value = false
      ElMessage.error(`生成失败: ${err.message}`)
    }
  )
}

function stopDraftGeneration() {
  if (draftAbortController) {
    draftAbortController.abort()
    draftAbortController = null
  }
  draftStreaming.value = false
}

async function handleSaveDraft() {
  if (!draftContent.value || !draftItem.value) return
  draftSaving.value = true
  try {
    await saveDraft({
      projectId,
      docLedgerId: draftItem.value.id ?? undefined,
      catalogId: draftCatalogId.value ?? undefined,
      stageId: selectedStageId.value ?? undefined,
      docName: draftItem.value.docName || 'AI 生成文档',
      docType: draftItem.value.docType || 'MANAGEMENT_DOC',
      securityLevel: draftItem.value.securityLevel || '内部',
      content: draftContent.value
    })
    ElMessage.success('文档初稿已保存')
    showDraftDialog.value = false
    try { await loadKanban() } catch { /* refresh may fail silently */ }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    draftSaving.value = false
  }
}

// ---- AI: Proofread ----
const showProofreadDialog = ref(false)
const proofreading = ref(false)
const proofreadResult = ref<any>(null)
const proofreadItem = ref<DocLedgerItem | null>(null)

async function aiProofread(item: DocLedgerItem) {
  proofreadItem.value = item
  proofreadResult.value = null
  showProofreadDialog.value = true
  proofreading.value = true
  try {
    const res = await proofread(item.id!)
    proofreadResult.value = res.data.data
  } catch {
    ElMessage.error('AI 校对失败')
    showProofreadDialog.value = false
  } finally {
    proofreading.value = false
  }
}

// ---- AI: Pre-Review ----
const showPreReviewDialog = ref(false)
const preReviewing = ref(false)
const preReviewResult = ref<any>(null)
const preReviewItem = ref<DocLedgerItem | null>(null)

async function aiPreReview(item: DocLedgerItem) {
  preReviewItem.value = item
  preReviewResult.value = null
  showPreReviewDialog.value = true
  preReviewing.value = true
  try {
    const res = await preReview(item.id!)
    preReviewResult.value = res.data.data
  } catch {
    ElMessage.error('AI 预评审失败')
    showPreReviewDialog.value = false
  } finally {
    preReviewing.value = false
  }
}

// ---- AI Health Check ----
const healthOk = ref(false)
async function checkHealth() {
  try {
    const res = await (await import('@/api/ai')).checkAiHealth()
    healthOk.value = res.data.data?.connected === true
  } catch { healthOk.value = false }
}

// Lightweight Markdown to HTML (same as ProjectAiAssistant)
function renderMarkdown(text: string): string {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  html = html.replace(/^#### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
  html = html.replace(/\n\n+/g, '</p><p>')
  html = '<p>' + html + '</p>'
  html = html.replace(/\n/g, '<br>')
  html = html.replace(/<p><\/p>/g, '')
  return html
}

async function loadDocDicts() {
  try {
    const [catRes, typeRes] = await Promise.all([
      getDictItems('DOC_CATEGORY'),
      getDictItems('DOC_TYPE')
    ])
    docCategories.value = catRes.data.data || []
    docTypes.value = typeRes.data.data || []
    for (const c of docCategories.value) {
      categoryNameMap.value[c.dictCode] = c.dictName
    }
    for (const t of docTypes.value) {
      typeNameMap.value[t.dictCode] = t.dictName
    }
  } catch { /* ignore */ }
}

const completenessLoaded = ref(false)

onMounted(() => {
  loadKanban(); loadStages(); checkHealth(); loadDocDicts()
  // Lazy-load completeness data after initial render to avoid blocking
  setTimeout(() => {
    if (!completenessLoaded.value) {
      completenessLoaded.value = true
      loadCompletenessForAll()
    }
  }, 2000)
})

watch(searchText, (val) => {
  treeFilter.value = val
})
watch(treeFilter, (val) => {
  treeRef.value?.filter(val)
})
</script>

<style scoped>
.kanban-page { padding: 16px 0; }
.kanban-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }

.kanban-board { display:flex; gap:12px; overflow-x:auto; padding-bottom:8px; min-height:60vh; }
.kanban-column {
  flex:1; min-width:180px; max-width:260px;
  background:var(--el-fill-color-lighter); border-radius:8px;
  display:flex; flex-direction:column; transition: background .2s;
}
.kanban-column.drag-over { background:var(--el-color-primary-light-9); }
.column-header { padding:12px 12px 8px; display:flex; justify-content:space-between; align-items:center; }
.column-title { font-weight:600; font-size:14px; }
.column-body { padding:0 8px 8px; flex:1; overflow-y:auto; }
.column-empty { text-align:center; color:var(--el-text-color-placeholder); font-size:12px; padding:24px 0; }
.kanban-card {
  background:#fff; border-radius:6px; padding:10px; margin-bottom:8px;
  cursor:grab; box-shadow:0 1px 3px rgba(0,0,0,.08);
  transition: box-shadow .2s, transform .15s;
}
.kanban-card:hover { box-shadow:0 2px 8px rgba(0,0,0,.15); transform:translateY(-1px); }
.kanban-card:active { cursor:grabbing; }
.card-code { font-size:11px; color:var(--el-text-color-placeholder); margin-bottom:4px; }
.card-name { font-size:13px; font-weight:500; margin-bottom:6px; line-height:1.4; }
.card-tags { display:flex; gap:4px; flex-wrap:wrap; }
.card-actions { margin-top:8px; padding-top:6px; border-top:1px solid var(--el-border-color-lighter); }
.header-left, .header-right { display:flex; align-items:center; gap:8px; }
.col-cat-header { display:flex; justify-content:space-between; align-items:center; padding:4px 6px; margin:2px 0 4px; background:rgba(0,0,0,.04); border-radius:4px; font-size:12px; font-weight:500; color:var(--el-text-color-secondary); }

.draft-output { margin-top:16px; border:1px solid var(--el-border-color); border-radius:6px; overflow:hidden; }
.draft-output-header { display:flex; justify-content:space-between; align-items:center; padding:8px 16px; background:var(--el-fill-color-light); border-bottom:1px solid var(--el-border-color); font-size:13px; }
.draft-output-body { padding:16px; max-height:300px; overflow-y:auto; background:#fff; font-size:14px; line-height:1.8; }
.markdown-body :deep(h1) { font-size:20px; margin:16px 0 8px; border-bottom:1px solid var(--el-border-color); padding-bottom:4px; }
.markdown-body :deep(h2) { font-size:18px; margin:14px 0 6px; }
.markdown-body :deep(h3) { font-size:16px; margin:12px 0 4px; }
.markdown-body :deep(h4) { font-size:15px; margin:10px 0 4px; }
.markdown-body :deep(p) { margin:4px 0; }
.markdown-body :deep(ul) { margin:4px 0; padding-left:24px; }
.markdown-body :deep(li) { margin:2px 0; }
.markdown-body :deep(strong) { font-weight:600; }

/* Tree view */
.tree-view { padding:4px 0; }
.tree-toolbar { display:flex; align-items:center; gap:8px; margin-bottom:12px; }
.tree-card {
  background:var(--el-fill-color-lighter); border-radius:8px; padding:8px 12px;
  max-height:calc(100vh - 240px); overflow-y:auto;
}
.tree-card :deep(.el-tree-node__content) { height:auto; padding:2px 0; }
.tree-card :deep(.el-tree-node__expand-icon) { color:var(--el-text-color-secondary); }
.tree-node-content { display:flex; align-items:center; gap:4px; flex-wrap:wrap; width:100%; }
.tree-cat-label { font-weight:600; font-size:14px; color:var(--el-text-color-primary); }
.tree-doc-code { font-size:11px; color:var(--el-text-color-placeholder); min-width:80px; }
.tree-doc-name { font-size:13px; font-weight:500; flex:1; }
.tree-actions { display:flex; align-items:center; gap:2px; margin-left:auto; }
.tree-actions :deep(.el-select) { width:80px; }
</style>
