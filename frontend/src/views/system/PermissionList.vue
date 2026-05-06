<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索权限编码/名称" style="width: 240px" clearable />
      </div>
      <el-button type="primary" @click="showCreateDialog(null)">创建权限</el-button>
    </div>

    <el-table
      :data="treeData"
      v-loading="loading"
      row-key="id"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      default-expand-all
      style="width: 100%"
    >
      <el-table-column prop="permissionCode" label="权限编码" width="220" />
      <el-table-column prop="permissionName" label="权限名称" width="160" />
      <el-table-column prop="resourceType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.resourceType === 'MENU' ? 'primary' : 'info'" size="small">
            {{ row.resourceType === 'MENU' ? '菜单' : '按钮' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" width="180" show-overflow-tooltip />
      <el-table-column prop="orderNum" label="排序" width="70" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showCreateDialog(row)">添加子级</el-button>
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑权限' : '创建权限'"
      width="520px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="权限编码" prop="permissionCode">
              <el-input v-model="form.permissionCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="resourceType">
              <el-select v-model="form.resourceType" style="width: 100%">
                <el-option label="菜单" value="MENU" />
                <el-option label="按钮" value="BTN" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="权限名称" prop="permissionName">
          <el-input v-model="form.permissionName" />
        </el-form-item>
        <el-form-item label="父级权限">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'permissionName', value: 'id', children: 'children' }"
            placeholder="无(顶级权限)"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="路径">
              <el-input v-model="form.path" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPermissionList, createPermission, updatePermission, deletePermission,
  type PermissionItem
} from '@/api/permission'

const loading = ref(false)
const saving = ref(false)
const flatList = ref<PermissionItem[]>([])
const keyword = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): PermissionItem => ({
  permissionCode: '',
  permissionName: '',
  resourceType: 'MENU',
  path: '',
  parentId: 0,
  orderNum: 0
})

const form = reactive<PermissionItem>(emptyForm())
const formRules = {
  permissionCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function buildTree(list: PermissionItem[]): any[] {
  const map = new Map<number, any>()
  const tree: any[] = []
  for (const item of list) {
    map.set(item.id!, { ...item, children: [] })
  }
  for (const item of list) {
    const node = map.get(item.id!)!
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId)!.children.push(node)
    } else {
      tree.push(node)
    }
  }
  return tree
}

const treeData = computed(() => {
  let list = flatList.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(
      p => p.permissionCode.toLowerCase().includes(kw) ||
           p.permissionName.toLowerCase().includes(kw)
    )
  }
  return buildTree(list)
})

const parentOptions = computed(() => {
  return [
    { id: 0, permissionName: '无(顶级权限)', children: buildTree(flatList.value) }
  ]
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getPermissionList()
    flatList.value = res.data.data
  } finally {
    loading.value = false
  }
}

function showCreateDialog(parent: PermissionItem | null) {
  editingId.value = null
  Object.assign(form, {
    ...emptyForm(),
    parentId: parent ? parent.id! : 0
  })
  dialogVisible.value = true
}

function showEditDialog(row: PermissionItem) {
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
      await updatePermission(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createPermission({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: PermissionItem) {
  await ElMessageBox.confirm(
    `确定要删除权限「${row.permissionName}」吗？子级权限将保留。`,
    '确认删除',
    { type: 'warning' }
  )
  try {
    await deletePermission(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* cancelled or error */ }
}

onMounted(fetchData)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
</style>
