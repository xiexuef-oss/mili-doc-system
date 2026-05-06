<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索角色编码/名称" style="width: 240px" clearable @change="fetchData" />
      </div>
      <el-button type="primary" @click="showCreateDialog">创建角色</el-button>
    </div>

    <el-table :data="roles" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleCode" label="角色编码" width="140" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="orderNum" label="排序" width="70" />
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
          <el-button link type="warning" @click="showPermDialog(row)">分配权限</el-button>
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

    <!-- 创建/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑角色' : '创建角色'"
      width="520px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色编码" prop="roleCode">
              <el-input v-model="form.roleCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
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

    <!-- 分配权限对话框 -->
    <el-dialog
      v-model="permDialogVisible"
      title="分配权限"
      width="520px"
    >
      <el-tree
        ref="permTreeRef"
        :data="permTreeData"
        :props="{ label: 'permissionName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="checkedPermIds"
      />
      <el-empty v-if="permTreeData.length === 0" description="暂无权限" />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPerms" @click="handleSavePerms">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoles, createRole, updateRole, deleteRole, type RoleItem } from '@/api/role'
import { getPermissionList, getRolePermissions, setRolePermissions } from '@/api/permission'

const loading = ref(false)
const saving = ref(false)
const roles = ref<RoleItem[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): RoleItem => ({
  roleCode: '',
  roleName: '',
  description: '',
  orderNum: 0,
  status: 'ACTIVE'
})

const form = reactive<RoleItem>(emptyForm())
const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getRoles({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    const data = res.data.data
    roles.value = data.records
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

function showEditDialog(row: RoleItem) {
  editingId.value = row.id!
  Object.assign(form, row)
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
      await updateRole(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createRole({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: RoleItem) {
  await ElMessageBox.confirm(`确定要删除角色「${row.roleName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteRole(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled or error */ }
}

// 权限分配
const permDialogVisible = ref(false)
const savingPerms = ref(false)
const permTreeRef = ref()
const permTreeData = ref<any[]>([])
const checkedPermIds = ref<number[]>([])
const currentPermRoleId = ref<number>(0)

function buildPermTree(list: any[]): any[] {
  const map = new Map<number, any>()
  const tree: any[] = []
  for (const item of list) {
    map.set(item.id, { ...item, children: [] })
  }
  for (const item of list) {
    const node = map.get(item.id)
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId).children.push(node)
    } else {
      tree.push(node)
    }
  }
  return tree
}

async function showPermDialog(row: RoleItem) {
  currentPermRoleId.value = row.id!
  checkedPermIds.value = []
  try {
    const [permsRes, rolePermsRes] = await Promise.all([
      getPermissionList(),
      getRolePermissions(row.id!)
    ])
    permTreeData.value = buildPermTree(permsRes.data.data)
    checkedPermIds.value = rolePermsRes.data.data
  } catch { /* ignore */ }
  permDialogVisible.value = true
}

async function handleSavePerms() {
  savingPerms.value = true
  try {
    const keys = permTreeRef.value?.getCheckedKeys() || []
    const halfKeys = permTreeRef.value?.getHalfCheckedKeys() || []
    await setRolePermissions(currentPermRoleId.value, [...keys, ...halfKeys])
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } finally {
    savingPerms.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
