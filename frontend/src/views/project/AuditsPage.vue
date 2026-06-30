<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回阶段工作台</el-button>
        <h3 style="display:inline;margin-left:16px">技术状态审核</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建审核</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="auditCode" label="审核编号" width="140" />
      <el-table-column prop="auditName" label="名称" min-width="180" />
      <el-table-column prop="auditType" label="类型" width="90">
        <template #default="{ row }"><el-tag size="small">{{ auditTypeLabel(row.auditType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.auditStatus)" size="small">{{ row.auditStatus }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="auditResult" label="结果" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.auditResult" :type="resultType(row.auditResult)" size="small">{{ row.auditResult }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditOpinion" label="审核意见" min-width="200" show-overflow-tooltip />
      <el-table-column prop="auditTime" label="审核时间" width="160" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-if="row.auditStatus === 'PLANNED' || row.auditStatus === 'IN_PROGRESS'" link type="primary" size="small" @click="showCompleteDialog(row)">完成</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="创建审核" width="400px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="审核类型">
          <el-select v-model="form.auditType" style="width:100%">
            <el-option label="功能配置审核 (FCA)" value="FCA" />
            <el-option label="物理配置审核 (PCA)" value="PCA" />
            <el-option label="阶段审核" value="STAGE_AUDIT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeVisible" title="完成审核" width="400px">
      <el-form :model="completeForm" label-width="90px">
        <el-form-item label="审核结果">
          <el-select v-model="completeForm.auditResult" style="width:100%">
            <el-option label="通过" value="PASSED" />
            <el-option label="不通过" value="FAILED" />
            <el-option label="有条件通过" value="CONDITIONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核意见"><el-input v-model="completeForm.auditOpinion" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleComplete">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createAudit, getAudits, completeAudit, type ConfigurationAuditVO } from '@/api/configuration-management'

const route = useRoute()
const projectId = Number(route.params.projectId)
const stageId = Number(route.params.stageId)

const loading = ref(false); const saving = ref(false)
const items = ref<ConfigurationAuditVO[]>([])
const dialogVisible = ref(false); const completeVisible = ref(false)
const auditingId = ref<number | null>(null)

const form = reactive({ auditType: 'FCA' })
const completeForm = reactive({ auditResult: 'PASSED', auditOpinion: '' })

const auditTypeLabel = (t: string) => ({ FCA: '功能审核', PCA: '物理审核', STAGE_AUDIT: '阶段审核' }[t] || t)
const statusType = (s: string) => ({ PLANNED: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success', CLOSED: 'info' }[s] || 'info')
const resultType = (r: string) => ({ PASSED: 'success', FAILED: 'danger', CONDITIONAL: 'warning' }[r] || 'info')

async function loadItems() {
  loading.value = true
  try { const res = await getAudits(projectId, stageId); items.value = res.data.data || [] } finally { loading.value = false }
}
function showCreateDialog() { form.auditType = 'FCA'; dialogVisible.value = true }
async function handleCreate() {
  saving.value = true
  try { await createAudit(projectId, stageId, form.auditType); ElMessage.success('审核创建成功'); dialogVisible.value = false; loadItems() }
  catch { /* handled */ } finally { saving.value = false }
}
function showCompleteDialog(row: ConfigurationAuditVO) {
  auditingId.value = row.id!
  completeForm.auditResult = 'PASSED'
  completeForm.auditOpinion = ''
  completeVisible.value = true
}
async function handleComplete() {
  saving.value = true
  try {
    await completeAudit(auditingId.value!, completeForm.auditResult, completeForm.auditOpinion)
    ElMessage.success('审核已提交'); completeVisible.value = false; loadItems()
  } catch { /* handled */ } finally { saving.value = false }
}
async function handleDelete(row: ConfigurationAuditVO) {
  await ElMessageBox.confirm('确定删除此审核吗？', '确认', { type: 'warning' })
  try { await completeAudit(row.id!, 'FAILED', '已删除'); ElMessage.success('已处理'); loadItems() } catch { /* cancelled */ }
}
onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
