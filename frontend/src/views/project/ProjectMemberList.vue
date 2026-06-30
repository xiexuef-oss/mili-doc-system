<template>
  <div class="page">
    <div class="page-header">
      <div>
        <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回项目详情</el-button>
        <h3 style="display:inline;margin-left:16px">项目成员管理（两师系统）</h3>
      </div>
      <el-button type="primary" @click="showCreateDialog">添加成员</el-button>
    </div>

    <!-- 组织架构树 -->
    <MemberOrgTree :members="items" />

    <!-- 成员表格 -->
    <el-table :data="items" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="用户" width="120">
        <template #default="{ row }">{{ row.userName || `用户${row.userId}` }}</template>
      </el-table-column>
      <el-table-column label="指挥线" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.memberLine" :type="lineTagType(row.memberLine)" size="small">
            {{ lineLabel(row.memberLine) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="岗位" width="140">
        <template #default="{ row }">
          {{ positionLabel(row.memberPosition || row.roleInProject) }}
        </template>
      </el-table-column>
      <el-table-column label="上级" width="100">
        <template #default="{ row }">
          {{ supervisorName(row.supervisorId) }}
        </template>
      </el-table-column>
      <el-table-column prop="duties" label="职责" min-width="160" />
      <el-table-column label="单位/职称" width="150">
        <template #default="{ row }">
          <span v-if="row.userOrgName">{{ row.userOrgName }}</span>
          <span v-if="row.userTitle" style="color:#909399;margin-left:4px">{{ row.userTitle }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.status === 'ACTIVE' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="showEditDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑成员' : '添加成员'" width="520px">
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="选择用户" required>
          <el-select
            v-model="form.userId"
            filterable
            remote
            :remote-method="searchUserOptions"
            :loading="userSearchLoading"
            placeholder="输入姓名或用户名搜索"
            style="width:100%"
            @change="onUserSelected"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`${u.realName} (${u.username})`"
              :value="u.id!"
            >
              <div class="user-option">
                <span class="user-name">{{ u.realName }}</span>
                <span class="user-info">
                  <el-tag size="small" effect="plain">{{ u.orgName || '未分配部门' }}</el-tag>
                  <span v-if="u.title" style="color:#909399;font-size:12px;margin-left:4px">{{ u.title }}</span>
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="指挥线">
          <el-select v-model="form.memberLine" placeholder="选择指挥线" style="width:100%" clearable>
            <el-option v-for="l in lineOptions" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.memberPosition" placeholder="选择岗位" style="width:100%" clearable filterable>
            <el-option-group v-for="group in positionGroups" :key="group.line" :label="group.label">
              <el-option v-for="p in group.positions" :key="p.value" :label="p.label" :value="p.value" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="上级">
          <el-select v-model="form.supervisorId" placeholder="选择上级（可选）" style="width:100%" clearable>
            <el-option v-for="m in activeMembers" :key="m.id" :label="`${m.userName || '用户'+m.userId} (${positionLabel(m.memberPosition || m.roleInProject)})`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="职责">
          <el-input v-model="form.duties" type="textarea" :rows="2" placeholder="主要职责描述" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getProjectMembers, createProjectMember, updateProjectMember, deleteProjectMember, type ProjectMemberItem } from '@/api/project-member'
import { searchUsers, type UserItem } from '@/api/user'
import MemberOrgTree from '@/components/MemberOrgTree.vue'

const route = useRoute()
const projectId = Number(route.params.projectId)

const loading = ref(false); const saving = ref(false)
const items = ref<ProjectMemberItem[]>([])
const dialogVisible = ref(false); const editingId = ref<number | null>(null)

const empty = (): ProjectMemberItem => ({
  projectId, userId: 0, roleInProject: '', memberLine: '', memberPosition: '',
  supervisorId: undefined, sortOrder: 0, duties: '', status: 'ACTIVE'
})
const form = reactive<ProjectMemberItem>(empty())

const lineOptions = [
  { value: 'TECHNICAL', label: '技术指挥线' },
  { value: 'ADMINISTRATIVE', label: '行政指挥线' },
  { value: 'QUALITY', label: '质量线' },
  { value: 'CRAFT', label: '工艺线' }
]

const positionGroups = [
  {
    line: 'TECHNICAL', label: '技术指挥线',
    positions: [
      { value: 'CHIEF_DESIGNER', label: '总设计师' },
      { value: 'DEPUTY_CHIEF_DESIGNER', label: '副总设计师' },
      { value: 'CHIEF_DESIGNER_ENGINEER', label: '主任设计师' },
      { value: 'LEAD_DESIGNER_ENGINEER', label: '主管设计师' },
      { value: 'DESIGNER_ENGINEER', label: '设计师' }
    ]
  },
  {
    line: 'ADMINISTRATIVE', label: '行政指挥线',
    positions: [
      { value: 'CHIEF_COMMANDER', label: '总指挥' },
      { value: 'DEPUTY_CHIEF_COMMANDER', label: '副总指挥' },
      { value: 'PROJECT_OFFICE_DIRECTOR', label: '项目办主任' },
      { value: 'PLAN_SUPERVISOR', label: '计划主管' },
      { value: 'PROJECT_ASSISTANT', label: '项目助理' }
    ]
  },
  {
    line: 'QUALITY', label: '质量线',
    positions: [
      { value: 'CHIEF_QUALITY_ENGINEER', label: '总质量师' },
      { value: 'QUALITY_SUPERVISOR', label: '质量主管' },
      { value: 'QUALITY_ENGINEER', label: '质量师' }
    ]
  },
  {
    line: 'CRAFT', label: '工艺线',
    positions: [
      { value: 'CHIEF_PROCESS_ENGINEER', label: '总工艺师' },
      { value: 'PROCESS_ENGINEER', label: '工艺师' }
    ]
  }
]

const activeMembers = computed(() => items.value.filter(m => m.status === 'ACTIVE' && m.id !== editingId.value))

// 用户搜索
const userOptions = ref<UserItem[]>([])
const userSearchLoading = ref(false)

async function searchUserOptions(query: string) {
  userSearchLoading.value = true
  try {
    const res = await searchUsers(query)
    const data = res.data.data
    userOptions.value = data?.records || data || []
  } finally { userSearchLoading.value = false }
}

function onUserSelected(userId: number) {
  const user = userOptions.value.find(u => u.id === userId)
  if (user) {
    // 自动带入用户的部门和职称信息到成员记录中
    form.userName = user.realName
    ;(form as any).userOrgName = user.orgName
    ;(form as any).userTitle = user.title
  }
}

async function loadItems() {
  loading.value = true
  try { const res = await getProjectMembers(projectId); items.value = res.data.data || [] } finally { loading.value = false }
}

function showCreateDialog() { editingId.value = null; Object.assign(form, empty()); dialogVisible.value = true; searchUserOptions('') }
function showEditDialog(row: ProjectMemberItem) { editingId.value = row.id!; Object.assign(form, row); dialogVisible.value = true; searchUserOptions('') }

async function handleSave() {
  saving.value = true
  try {
    if (editingId.value) { await updateProjectMember(editingId.value, { ...form }); ElMessage.success('更新成功') }
    else { await createProjectMember({ ...form }); ElMessage.success('添加成功') }
    dialogVisible.value = false; loadItems()
  } finally { saving.value = false }
}

async function handleDelete(row: ProjectMemberItem) {
  await ElMessageBox.confirm('确定移除此成员吗？', '确认', { type: 'warning' })
  try { await deleteProjectMember(row.id!); ElMessage.success('已移除'); loadItems() } catch { /* cancelled */ }
}

function lineLabel(line: string) {
  const map: Record<string, string> = { TECHNICAL: '技术线', ADMINISTRATIVE: '行政线', QUALITY: '质量线', CRAFT: '工艺线' }
  return map[line] || line
}

function lineTagType(line: string) {
  switch (line) { case 'TECHNICAL': return 'primary'; case 'ADMINISTRATIVE': return 'success'; case 'QUALITY': return 'warning'; case 'CRAFT': return 'info'; default: return '' }
}

function positionLabel(code: string) {
  const map: Record<string, string> = {
    CHIEF_DESIGNER: '总设计师', DEPUTY_CHIEF_DESIGNER: '副总设计师',
    CHIEF_DESIGNER_ENGINEER: '主任设计师', LEAD_DESIGNER_ENGINEER: '主管设计师',
    DESIGNER_ENGINEER: '设计师', CHIEF_COMMANDER: '总指挥',
    DEPUTY_CHIEF_COMMANDER: '副总指挥', PROJECT_OFFICE_DIRECTOR: '项目办主任',
    PLAN_SUPERVISOR: '计划主管', PROJECT_ASSISTANT: '项目助理',
    CHIEF_QUALITY_ENGINEER: '总质量师', QUALITY_SUPERVISOR: '质量主管',
    QUALITY_ENGINEER: '质量师', CHIEF_PROCESS_ENGINEER: '总工艺师',
    PROCESS_ENGINEER: '工艺师'
  }
  return map[code] || code
}

function supervisorName(supervisorId?: number) {
  if (!supervisorId) return '-'
  const sup = items.value.find(m => m.id === supervisorId)
  return sup ? (sup.userName || `用户${sup.userId}`) : '-'
}

onMounted(loadItems)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.user-option { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.user-name { font-weight: 500; }
.user-info { display: flex; align-items: center; gap: 4px; }
</style>
