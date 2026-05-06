<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索用户名/姓名/邮箱" style="width: 280px" clearable @change="fetchData" />
      </div>
      <el-button type="primary" @click="showCreateDialog">创建用户</el-button>
    </div>

    <el-table :data="users" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="showRoleDialog(row)">分配角色</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </div>

    <!-- 创建/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑用户' : '创建用户'"
      width="520px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="密码" :prop="editingId ? null : 'password'">
          <el-input v-model="form.password" type="password" show-password :placeholder="editingId ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="分配角色"
      width="480px"
    >
      <el-checkbox-group v-model="checkedRoleIds">
        <div v-for="role in allRoles" :key="role.id" class="role-item">
          <el-checkbox :value="role.id" :label="role.id">
            <span class="role-label">{{ role.roleName }}</span>
            <span class="role-code">{{ role.roleCode }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <el-empty v-if="allRoles.length === 0" description="暂无角色" />
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRoles" @click="handleSaveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, createUser, updateUser, deleteUser, getUserRoles, setUserRoles, type UserItem } from '@/api/user'
import { getRoles, type RoleItem } from '@/api/role'

const loading = ref(false)
const saving = ref(false)
const users = ref<UserItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): UserItem => ({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  status: 'ACTIVE'
})

const form = reactive<UserItem>(emptyForm())
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getUsers({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    const data = res.data.data
    users.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function showEditDialog(row: UserItem) {
  editingId.value = row.id!
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      const payload = { ...form }
      if (!payload.password) delete payload.password
      await updateUser(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createUser({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteUser(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled or error */ }
}

// 角色分配
const roleDialogVisible = ref(false)
const savingRoles = ref(false)
const checkedRoleIds = ref<number[]>([])
const allRoles = ref<RoleItem[]>([])
const currentRoleUserId = ref<number>(0)

async function showRoleDialog(row: UserItem) {
  currentRoleUserId.value = row.id!
  checkedRoleIds.value = []
  try {
    const [rolesRes, userRolesRes] = await Promise.all([
      getRoles({ pageSize: 100 }),
      getUserRoles(row.id!)
    ])
    allRoles.value = rolesRes.data.data.records
    checkedRoleIds.value = userRolesRes.data.data
  } catch { /* ignore */ }
  roleDialogVisible.value = true
}

async function handleSaveRoles() {
  savingRoles.value = true
  try {
    await setUserRoles(currentRoleUserId.value, checkedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } finally {
    savingRoles.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
.role-item { padding: 6px 0; border-bottom: 1px solid #f0f0f0; }
.role-item:last-child { border-bottom: none; }
.role-label { font-weight: 500; }
.role-code { color: #909399; font-size: 12px; margin-left: 8px; }
</style>
