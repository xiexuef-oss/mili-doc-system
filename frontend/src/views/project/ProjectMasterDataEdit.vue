<template>
  <div class="page">
    <div class="page-header">
      <h3>项目主数据</h3>
      <div class="header-actions">
        <el-tag v-if="version > 0" size="small" type="info">版本 {{ version }}</el-tag>
        <el-button type="primary" :loading="saving" @click="handleSave">
          <el-icon><Check /></el-icon>保存主数据
        </el-button>
      </div>
    </div>

    <el-alert
      title="项目主数据将在文档生成时自动填充到各章节，减少重复填写工作"
      type="success" :closable="false" show-icon style="margin-bottom:16px"
    />

    <el-form ref="formRef" :model="form" label-width="140px">
      <el-tabs v-model="activeSection" type="border-card">
        <el-tab-pane
          v-for="section in sections"
          :key="section.key"
          :label="section.label"
          :name="section.key"
        >
          <template v-if="section.key === 'tacticalIndicators' || section.key === 'productTree' || section.key === 'teamMembers' || section.key === 'milestones'">
            <div style="margin-bottom:12px">
              <el-button size="small" type="primary" @click="addRow(section.key)">
                <el-icon><Plus /></el-icon>添加行
              </el-button>
              <span class="hint">多行数据，可添加多条记录</span>
            </div>
            <el-table :data="form[section.key]" border size="small">
              <el-table-column
                v-for="field in section.fields"
                :key="field.name"
                :label="field.label"
                :width="field.type === 'textarea' ? 200 : undefined"
              >
                <template #default="{ row }">
                  <el-input
                    v-if="field.type === 'text'"
                    v-model="row[field.name]"
                    size="small"
                  />
                  <el-input
                    v-else-if="field.type === 'textarea'"
                    v-model="row[field.name]"
                    type="textarea"
                    :rows="2"
                    size="small"
                  />
                  <el-select
                    v-else-if="field.type === 'select'"
                    v-model="row[field.name]"
                    size="small"
                    style="width:100%"
                  >
                    <el-option
                      v-for="opt in (field.options || [])"
                      :key="typeof opt === 'string' ? opt : (opt as any).value"
                      :label="typeof opt === 'string' ? opt : (opt as any).label"
                      :value="typeof opt === 'string' ? opt : (opt as any).value"
                    />
                  </el-select>
                  <el-date-picker
                    v-else-if="field.type === 'date'"
                    v-model="row[field.name]"
                    type="date"
                    size="small"
                    style="width:100%"
                    value-format="YYYY-MM-DD"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60" fixed="right">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="form[section.key].splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>

          <template v-else>
            <MasterDataFormSection
              :section="section"
              :model="singleSectionData(section.key)"
              :prefix="section.key"
            />
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Plus } from '@element-plus/icons-vue'
import { getMasterData, saveMasterData } from '@/api/project-master-data'
import MasterDataFormSection from '@/components/MasterDataFormSection.vue'

const route = useRoute()
const projectId = Number(route.params.projectId)

const saving = ref(false)
const version = ref(0)
const activeSection = ref('equipmentInfo')

const form: Record<string, any> = reactive({
  equipmentInfo: {},
  tacticalIndicators: [],
  productTree: [],
  teamMembers: [],
  milestones: []
})

const sections = [
  {
    key: 'equipmentInfo', label: '装备基本信息',
    fields: [
      { name: 'equipmentName', label: '装备名称', type: 'text', required: true },
      { name: 'equipmentType', label: '装备类型', type: 'select', options: ['雷达', '通信', '电子对抗', '指挥控制', '导弹', '无人机', '卫星', '其他'] },
      { name: 'model', label: '型号', type: 'text' },
      { name: 'manufacturer', label: '研制单位', type: 'text' },
      { name: 'overview', label: '概述', type: 'textarea' }
    ]
  },
  {
    key: 'tacticalIndicators', label: '战术技术指标',
    fields: [
      { name: 'indicatorName', label: '指标名称', type: 'text' },
      { name: 'required', label: '要求值', type: 'text' },
      { name: 'actual', label: '实测值', type: 'text' },
      { name: 'unit', label: '单位', type: 'text' },
      { name: 'conclusion', label: '符合性', type: 'select', options: ['满足', '基本满足', '不满足'] },
      { name: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  {
    key: 'productTree', label: '产品分解结构',
    fields: [
      { name: 'level', label: '层级', type: 'select', options: ['系统', '分系统', '设备', '组件'] },
      { name: 'productName', label: '产品名称', type: 'text' },
      { name: 'productCode', label: '代号', type: 'text' },
      { name: 'parentProduct', label: '父级', type: 'text' },
      { name: 'quantity', label: '数量', type: 'text' },
      { name: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  {
    key: 'teamMembers', label: '项目团队',
    fields: [
      { name: 'role', label: '角色', type: 'select', options: ['总设计师', '副总设计师', '主任设计师', '主管设计师', '设计师', '质量师', '标准化师'] },
      { name: 'name', label: '姓名', type: 'text' },
      { name: 'department', label: '部门', type: 'text' },
      { name: 'phone', label: '电话', type: 'text' },
      { name: 'email', label: '邮箱', type: 'text' }
    ]
  },
  {
    key: 'milestones', label: '里程碑',
    fields: [
      { name: 'stageCode', label: '阶段代码', type: 'select', options: ['L', 'F', 'C', 'S', 'D', 'P', 'N'] },
      { name: 'name', label: '名称', type: 'text' },
      { name: 'plannedDate', label: '计划日期', type: 'date' },
      { name: 'actualDate', label: '实际日期', type: 'date' },
      { name: 'keyDeliverables', label: '关键交付物', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: ['未开始', '进行中', '已完成'] }
    ]
  }
]

function singleSectionData(key: string) {
  return computed(() => form[key] || {})
}

function addRow(key: string) {
  const section = sections.find(s => s.key === key)
  if (!section) return
  const row: Record<string, any> = {}
  for (const f of section.fields) row[f.name] = ''
  form[key].push(row)
}

async function loadMasterData() {
  try {
    const res = await getMasterData(projectId)
    const data = res.data.data
    if (data) {
      version.value = data.versionNo || 0
      form.equipmentInfo = data.equipmentInfo || {}
      form.tacticalIndicators = data.tacticalIndicators || []
      form.productTree = data.productTree || []
      form.teamMembers = data.teamMembers || []
      form.milestones = data.milestones || []
    }
  } catch { /* ignore */ }
}

async function handleSave() {
  saving.value = true
  try {
    const data: Record<string, any> = {}
    for (const section of sections) {
      data[section.key] = form[section.key]
    }
    const res = await saveMasterData(projectId, data, 1)
    version.value = res.data?.data?.versionNo || (version.value + 1)
    ElMessage.success('主数据已保存')
  } catch {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

onMounted(loadMasterData)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 16px; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 12px; color: #909399; margin-left: 8px; }
</style>
