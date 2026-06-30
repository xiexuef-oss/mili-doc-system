<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回阶段工作台</el-button>
        <h3 style="display:inline;margin-left:16px">技术状态更改控制</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">创建更改申请</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="changeCode" label="更改编号" width="130" />
      <el-table-column prop="changeTitle" label="标题" min-width="180" />
      <el-table-column prop="changeType" label="类型" width="120">
        <template #default="{ row }"><el-tag size="small">{{ changeTypeLabel(row.changeType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="changeLevel" label="级别" width="80">
        <template #default="{ row }"><el-tag :type="levelType(row.changeLevel)" size="small">{{ changeLevelLabel(row.changeLevel) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="160" />
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button v-if="row.status === 'SUBMITTED'" link type="primary" size="small" @click="processChange(row, 'analyze')">分析</el-button>
          <el-button v-if="row.status === 'ANALYZING'" link type="primary" size="small" @click="processChange(row, 'review')">审查</el-button>
          <el-button v-if="row.status === 'REVIEWING'" link type="success" size="small" @click="processChange(row, 'approve')">批准</el-button>
          <el-button v-if="row.status === 'REVIEWING'" link type="danger" size="small" @click="processChange(row, 'reject')">驳回</el-button>
          <el-button v-if="row.status === 'APPROVED'" link type="primary" size="small" @click="processChange(row, 'implement')">实施</el-button>
          <el-button v-if="row.status === 'IMPLEMENTED'" link type="success" size="small" @click="processChange(row, 'close')">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="创建更改申请" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题"><el-input v-model="form.changeTitle" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="更改类型">
            <el-select v-model="form.changeType" style="width:100%">
              <el-option label="设计更改" value="DESIGN_CHANGE" /><el-option label="工艺更改" value="PROCESS_CHANGE" />
              <el-option label="文档更改" value="DOC_CHANGE" /><el-option label="接口更改" value="INTERFACE_CHANGE" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="更改级别">
            <el-select v-model="form.changeLevel" style="width:100%">
              <el-option label="重大" value="CRITICAL" /><el-option label="较大" value="MAJOR" /><el-option label="一般" value="MINOR" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="更改原因"><el-input v-model="form.changeReason" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="更改内容"><el-input v-model="form.changeContent" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="影响分析"><el-input v-model="form.impactAnalysis" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createChangeRequest, getChangeRequests, processChangeRequest, type ConfigurationChangeRequestVO } from '@/api/configuration-management'

const route = useRoute()
const projectId = Number(route.params.projectId)
const stageId = Number(route.params.stageId)

const loading = ref(false); const saving = ref(false)
const items = ref<ConfigurationChangeRequestVO[]>([])
const dialogVisible = ref(false)

const empty = (): ConfigurationChangeRequestVO => ({
  projectId, stageId, changeTitle: '', changeType: 'DESIGN_CHANGE', changeLevel: 'MAJOR'
})
const form = reactive<ConfigurationChangeRequestVO>(empty())

const changeTypeLabel = (t: string) => ({ DESIGN_CHANGE: '设计更改', PROCESS_CHANGE: '工艺更改', DOC_CHANGE: '文档更改', INTERFACE_CHANGE: '接口更改' }[t] || t)
const changeLevelLabel = (l: string) => ({ CRITICAL: '重大', MAJOR: '较大', MINOR: '一般' }[l] || l)
const levelType = (l: string) => ({ CRITICAL: 'danger', MAJOR: 'warning', MINOR: 'info' }[l] || 'info')
const statusType = (s: string) => ({ SUBMITTED: 'info', ANALYZING: 'warning', REVIEWING: 'warning', APPROVED: 'primary', REJECTED: 'danger', IMPLEMENTED: 'success', CLOSED: 'info' }[s] || 'info')

async function loadItems() {
  loading.value = true
  try { const res = await getChangeRequests(projectId, stageId); items.value = res.data.data || [] } finally { loading.value = false }
}
function showCreateDialog() { Object.assign(form, empty()); dialogVisible.value = true }
async function handleCreate() {
  saving.value = true
  try { await createChangeRequest(projectId, { ...form }); ElMessage.success('更改申请已提交'); dialogVisible.value = false; loadItems() }
  catch { /* handled */ } finally { saving.value = false }
}
async function processChange(row: ConfigurationChangeRequestVO, action: string) {
  try { await processChangeRequest(row.id!, action); ElMessage.success('操作成功'); loadItems() } catch { /* handled */ }
}
onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
