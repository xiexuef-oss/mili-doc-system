<template>
  <div class="page">
    <div class="page-header">
      <div class="filters">
        <el-select v-model="dictTypeFilter" placeholder="字典类型" style="width: 180px" clearable @change="fetchData">
          <el-option v-for="t in dictTypes" :key="t" :label="dictTypeLabel(t)" :value="t" />
        </el-select>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加字典项</el-button>
    </div>

    <el-table :data="dicts" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="dictType" label="字典类型" width="140">
        <template #default="{ row }">
          <el-tag size="small">{{ dictTypeLabel(row.dictType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dictCode" label="字典编码" width="160" />
      <el-table-column prop="dictName" label="字典名称" width="160" />
      <el-table-column prop="orderNum" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑字典项' : '添加字典项'"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="字典类型" prop="dictType">
          <el-select v-model="form.dictType" style="width: 100%" :disabled="!!editingId" allow-create filterable>
            <el-option v-for="t in dictTypes" :key="t" :label="dictTypeLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="字典编码" prop="dictCode">
          <el-input v-model="form.dictCode" />
        </el-form-item>
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="form.dictName" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="ACTIVE">启用</el-radio>
                <el-radio value="DISABLED">停用</el-radio>
              </el-radio-group>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictTypes, getDicts, createDict, updateDict, deleteDict, type DictItem } from '@/api/dict'

const loading = ref(false)
const saving = ref(false)
const dicts = ref<DictItem[]>([])
const dictTypes = ref<string[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(50)
const dictTypeFilter = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const emptyForm = (): DictItem => ({
  dictType: '',
  dictCode: '',
  dictName: '',
  orderNum: 0,
  status: 'ACTIVE'
})

const form = reactive<DictItem>(emptyForm())
const formRules = {
  dictType: [{ required: true, message: '请选择或输入字典类型', trigger: 'change' }],
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }]
}

function dictTypeLabel(type: string) {
  const map: Record<string, string> = {
    PROJECT_TYPE: '项目类型'
  }
  return map[type] || type
}

async function fetchDictTypes() {
  try {
    const res = await getDictTypes()
    dictTypes.value = res.data.data
  } catch { /* handled */ }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getDicts({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      dictType: dictTypeFilter.value || undefined
    })
    const data = res.data.data
    dicts.value = data.records
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

function showEditDialog(row: DictItem) {
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
      await updateDict(editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createDict({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchDictTypes()
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: DictItem) {
  await ElMessageBox.confirm(`确定要删除「${row.dictName}」吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteDict(row.id!)
    ElMessage.success('删除成功')
    fetchDictTypes()
    fetchData()
  } catch { /* cancelled or error */ }
}

onMounted(() => {
  fetchDictTypes()
  fetchData()
})
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.filters { display: flex; gap: 12px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
