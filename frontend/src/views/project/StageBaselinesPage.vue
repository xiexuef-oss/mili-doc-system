<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回阶段工作台</el-button>
        <h3 style="display:inline;margin-left:16px">技术状态基线管理</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建基线</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="baselineCode" label="基线编号" width="140" />
      <el-table-column prop="baselineName" label="基线名称" min-width="180" />
      <el-table-column prop="baselineType" label="类型" width="120">
        <template #default="{ row }"><el-tag size="small">{{ baselineTypeLabel(row.baselineType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="baselineVersion" label="版本" width="80" />
      <el-table-column prop="baselineStatus" label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.baselineStatus)" size="small">{{ row.baselineStatus }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="approveTime" label="批准时间" width="160" />
      <el-table-column prop="effectiveTime" label="生效时间" width="160" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.baselineStatus === 'DRAFT' || row.baselineStatus === 'REVIEWING'" link type="primary" size="small" @click="handleApprove(row)">批准</el-button>
          <el-button v-if="row.baselineStatus === 'APPROVED'" link type="success" size="small" @click="handleEffective(row)">生效</el-button>
          <el-button link size="small" @click="viewItems(row)">基线项</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="创建基线" width="400px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="基线类型">
          <el-select v-model="form.baselineType" style="width:100%">
            <el-option label="功能基线 (FBL)" value="FUNCTIONAL_BASELINE" />
            <el-option label="分配基线 (ABL)" value="ALLOCATED_BASELINE" />
            <el-option label="产品基线 (PBL)" value="PRODUCT_BASELINE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemsVisible" title="基线项列表" width="600px">
      <el-table :data="baselineItems" size="small">
        <el-table-column prop="itemCode" label="编号" width="120" />
        <el-table-column prop="itemName" label="名称" min-width="200" />
        <el-table-column prop="itemType" label="类型" width="100" />
        <el-table-column prop="itemVersion" label="版本" width="80" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createBaseline, getBaselines, getBaselineItems, approveBaseline, setBaselineEffective, type ConfigurationBaselineVO, type ConfigurationBaselineItemVO } from '@/api/configuration-management'
import { baselineTypeLabel } from '@/utils/labels'

const route = useRoute()
const projectId = Number(route.params.projectId)
const stageId = Number(route.params.stageId)

const loading = ref(false); const saving = ref(false)
const items = ref<ConfigurationBaselineVO[]>([])
const dialogVisible = ref(false); const itemsVisible = ref(false)
const baselineItems = ref<ConfigurationBaselineItemVO[]>([])

const form = reactive({ baselineType: 'FUNCTIONAL_BASELINE' })

const statusType = (s: string) => ({ DRAFT: 'info', REVIEWING: 'warning', APPROVED: 'primary', EFFECTIVE: 'success', SUPERSEDED: '', ARCHIVED: 'info' }[s] || 'info')

async function loadItems() {
  loading.value = true
  try { const res = await getBaselines(projectId, stageId); items.value = res.data.data || [] } finally { loading.value = false }
}
function showCreateDialog() { form.baselineType = 'FUNCTIONAL_BASELINE'; dialogVisible.value = true }
async function handleCreate() {
  saving.value = true
  try { await createBaseline(projectId, stageId, form.baselineType); ElMessage.success('基线创建成功'); dialogVisible.value = false; loadItems() }
  catch { /* handled */ } finally { saving.value = false }
}
async function handleApprove(row: ConfigurationBaselineVO) {
  try { await approveBaseline(row.id!); ElMessage.success('基线已批准'); loadItems() } catch { /* handled */ }
}
async function handleEffective(row: ConfigurationBaselineVO) {
  try { await setBaselineEffective(row.id!); ElMessage.success('基线已生效'); loadItems() } catch { /* handled */ }
}
async function viewItems(row: ConfigurationBaselineVO) {
  try { const res = await getBaselineItems(row.id!); baselineItems.value = res.data.data || []; itemsVisible.value = true } catch { /* handled */ }
}
async function handleDelete(row: ConfigurationBaselineVO) {
  await ElMessageBox.confirm('确定删除此基线吗？', '确认', { type: 'warning' })
  try { await setBaselineEffective(row.id!); ElMessage.success('已归档'); loadItems() } catch { /* cancelled */ }
}
onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
