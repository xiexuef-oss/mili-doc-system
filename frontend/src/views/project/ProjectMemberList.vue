<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回项目详情</el-button>
        <h3 style="display:inline;margin-left:16px">项目成员管理</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加成员</el-button>
    </div>

    <el-table :data="items" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="roleInProject" label="项目角色" width="130" />
      <el-table-column prop="duties" label="职责" min-width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑成员' : '添加成员'" width="480px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="用户ID">
          <el-input-number v-model="form.userId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="项目角色">
          <el-input v-model="form.roleInProject" placeholder="如：项目经理" />
        </el-form-item>
        <el-form-item label="职责">
          <el-input v-model="form.duties" type="textarea" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { getProjectMembers, createProjectMember, updateProjectMember, deleteProjectMember, type ProjectMemberItem } from '@/api/project-member'

const route = useRoute()
const projectId = Number(route.params.projectId)

const loading = ref(false); const saving = ref(false)
const items = ref<ProjectMemberItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): ProjectMemberItem => ({ projectId, userId: 0, roleInProject: '', duties: '', status: 'ACTIVE' })
const form = reactive<ProjectMemberItem>(empty())

async function fetch() {
  loading.value = true
  try { const res = await getProjectMembers(projectId); items.value = res.data.data } finally { loading.value = false }
}
function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true }
function showEditDialog(row: ProjectMemberItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateProjectMember(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createProjectMember({ ...form }); ElMessage.success('添加成功') }
    dialogVisible.value = false; fetch()
  } finally { saving.value = false }
}
async function handleDelete(row: ProjectMemberItem) {
  await ElMessageBox.confirm('确定移除此成员吗？', '确认', { type: 'warning' })
  try { await deleteProjectMember(row.id!); ElMessage.success('已移除'); fetch() } catch { /* cancelled */ }
}
onMounted(fetch)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
</style>
