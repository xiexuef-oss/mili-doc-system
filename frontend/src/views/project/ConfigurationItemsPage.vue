<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回阶段工作台</el-button>
        <h3 style="display:inline;margin-left:16px">技术状态项管理</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加技术状态项</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="ciCode" label="CI编号" width="120" />
      <el-table-column prop="ciName" label="名称" min-width="160" />
      <el-table-column prop="ciType" label="类型" width="120">
        <template #default="{ row }"><el-tag size="small">{{ ciTypeLabel(row.ciType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="currentVersion" label="当前版本" width="100" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="isKeyItem" label="关键件" width="80">
        <template #default="{ row }">{{ row.isKeyItem ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑技术状态项' : '添加技术状态项'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="CI编号"><el-input v-model="form.ciCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="名称"><el-input v-model="form.ciName" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="类型">
          <el-select v-model="form.ciType" style="width:100%">
            <el-option label="产品" value="PRODUCT" /><el-option label="软件" value="SOFTWARE" />
            <el-option label="文档" value="DOCUMENT" /><el-option label="接口" value="INTERFACE" />
            <el-option label="试验件" value="TEST_ITEM" /><el-option label="工艺" value="PROCESS" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="当前版本"><el-input v-model="form.currentVersion" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态">
            <el-select v-model="form.status" style="width:100%">
              <el-option label="活动" value="ACTIVE" /><el-option label="非活动" value="INACTIVE" /><el-option label="作废" value="OBSOLETE" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="关键件"><el-switch v-model="form.isKeyItem" /></el-form-item>
        <el-form-item label="受控"><el-switch v-model="form.isControlled" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getConfigurationItems, createConfigurationItem, updateConfigurationItem, type ConfigurationItemVO } from '@/api/configuration-management'

const route = useRoute()
const projectId = Number(route.params.projectId)
const stageId = Number(route.params.stageId)

const loading = ref(false); const saving = ref(false)
const items = ref<ConfigurationItemVO[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): ConfigurationItemVO => ({
  projectId, stageId, ciCode: '', ciName: '', ciType: 'PRODUCT',
  status: 'ACTIVE', isControlled: true, isKeyItem: false
})
const form = reactive<ConfigurationItemVO>(empty())

const ciTypeLabel = (t: string) => ({ PRODUCT: '产品', SOFTWARE: '软件', DOCUMENT: '文档', INTERFACE: '接口', TEST_ITEM: '试验件', PROCESS: '工艺' }[t] || t)

async function loadItems() {
  loading.value = true
  try { const res = await getConfigurationItems(projectId, stageId); items.value = res.data.data || [] } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: ConfigurationItemVO) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateConfigurationItem(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createConfigurationItem(projectId, { ...form }); ElMessage.success('创建成功') }
    dialogVisible.value = false; loadItems()
  } catch { /* handled */ } finally { saving.value = false }
}
async function handleDelete(row: ConfigurationItemVO) {
  await ElMessageBox.confirm('确定删除此技术状态项吗？', '确认', { type: 'warning' })
  try { await updateConfigurationItem(row.id!, { ...row, status: 'OBSOLETE' }); ElMessage.success('已作废'); loadItems() } catch { /* cancelled */ }
}
onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
