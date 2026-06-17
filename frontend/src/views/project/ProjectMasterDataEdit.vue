<template>
  <div class="page">
    <div class="page-header">
      <h3>项目主数据</h3>
      <div class="header-actions">
        <el-tag v-if="version > 0" size="small" type="info">版本 {{ version }}</el-tag>
        <el-button type="success" :loading="extracting" @click="handleExtract" :disabled="extracting">
          <el-icon><MagicStick /></el-icon>{{ extracting ? 'AI 提取中...' : 'AI 智能提取' }}
        </el-button>
        <el-button v-if="isEditing" type="primary" :loading="saving" @click="handleSave">
          <el-icon><Check /></el-icon>保存
        </el-button>
        <el-button v-if="!isEditing && hasData" type="default" @click="isEditing = true">
          <el-icon><Edit /></el-icon>编辑
        </el-button>
        <el-button v-if="isEditing" type="default" @click="cancelEdit">
          取消
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="extracting"
      title="正在从项目输入文件中 AI 提取产品信息、战术指标、产品结构、团队和里程碑..."
      type="info" :closable="false" show-icon
      style="margin-bottom:16px"
    >
      <template #default>
        <el-progress :percentage="extractProgress" :stroke-width="6" style="margin-top:8px" />
        <div class="progress-text">{{ extractStatus }}</div>
      </template>
    </el-alert>

    <el-alert
      v-if="extractedFrom && !extracting"
      :title="'数据来源: 从 ' + extractedFrom + ' 个输入文件中 AI 提取'"
      type="success" :closable="false" show-icon
      style="margin-bottom:16px"
    />

    <el-empty
      v-if="!hasData && !extracting"
      description="暂无主数据，可从输入文件 AI 提取或手动填写"
      :image-size="120"
      style="margin:40px 0"
    >
      <el-button type="success" @click="handleExtract">AI 智能提取</el-button>
      <el-button type="default" @click="isEditing = true" style="margin-left:8px">手动填写</el-button>
    </el-empty>

    <template v-if="hasData || isEditing">
      <el-alert
        v-if="!isEditing && hasData"
        title="查看确认模式 - 点击「编辑」可修改，确认无误后保存即可"
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
            <template v-if="section.key !== 'equipmentInfo'">
              <div v-if="isEditing" style="margin-bottom:12px">
                <el-button size="small" type="primary" @click="addRow(section.key)">
                  <el-icon><Plus /></el-icon>添加行
                </el-button>
              </div>

              <template v-if="!isEditing">
                <div v-if="!form[section.key] || form[section.key].length === 0" class="empty-section">
                  暂无数据
                </div>
                <div v-else class="view-cards">
                  <div v-for="(row, idx) in form[section.key]" :key="idx" class="data-card">
                    <div v-for="field in section.fields" :key="field.name" class="card-field">
                      <span class="field-label">{{ field.label }}</span>
                      <span class="field-value">{{ formatValue(row[field.name], field.type) || '-' }}</span>
                    </div>
                  </div>
                </div>
              </template>

              <el-table v-else :data="form[section.key]" border size="small">
                <el-table-column
                  v-for="field in section.fields"
                  :key="field.name"
                  :label="field.label"
                  :width="field.type === 'textarea' ? 200 : undefined"
                >
                  <template #default="{ row }">
                    <el-input v-if="field.type === 'text'" v-model="row[field.name]" size="small" />
                    <el-input v-else-if="field.type === 'textarea'" v-model="row[field.name]" type="textarea" :rows="2" size="small" />
                    <el-select v-else-if="field.type === 'select'" v-model="row[field.name]" size="small" style="width:100%">
                      <el-option v-for="opt in (field.options || [])" :key="typeof opt === 'string' ? opt : opt.value" :label="typeof opt === 'string' ? opt : opt.label" :value="typeof opt === 'string' ? opt : opt.value" />
                    </el-select>
                    <el-date-picker v-else-if="field.type === 'date'" v-model="row[field.name]" type="date" size="small" style="width:100%" value-format="YYYY-MM-DD" />
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
              <template v-if="!isEditing">
                <div class="view-cards">
                  <div class="data-card">
                    <div v-for="field in section.fields" :key="field.name" class="card-field">
                      <span class="field-label">{{ field.label }}</span>
                      <span class="field-value">{{ formatValue(form.equipmentInfo[field.name], field.type) || '-' }}</span>
                    </div>
                  </div>
                </div>
              </template>
              <template v-else>
                <MasterDataFormSection :section="section" :model="singleSectionData(section.key)" :prefix="section.key" />
              </template>
            </template>
          </el-tab-pane>
        </el-tabs>
      </el-form>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Plus, MagicStick, Edit } from '@element-plus/icons-vue'
import { getMasterData, saveMasterData, extractMasterData } from '@/api/project-master-data'
import MasterDataFormSection from '@/components/MasterDataFormSection.vue'

const route = useRoute()
const projectId = Number(route.params.projectId)

const saving = ref(false)
const extracting = ref(false)
const extractProgress = ref(0)
const extractStatus = ref('')
const extractedFrom = ref(0)
const version = ref(0)
const activeSection = ref('equipmentInfo')
const isEditing = ref(false)

const form: Record<string, any> = reactive({
  equipmentInfo: {},
  tacticalIndicators: [] as any[],
  productTree: [] as any[],
  milestones: [] as any[]
})

const hasData = computed(() => {
  const ei = form.equipmentInfo
  const hasEi = ei && Object.keys(ei).some((k: string) => ei[k])
  const hasList = (form.tacticalIndicators?.length || 0) > 0
    || (form.productTree?.length || 0) > 0
    || (form.milestones?.length || 0) > 0
  return !!(hasEi || hasList)
})

const sections = [
  {
    key: 'equipmentInfo', label: '产品基本信息',
    fields: [
      { name: 'equipmentName', label: '产品名称', type: 'text', required: true },
      { name: 'equipmentType', label: '产品类型', type: 'select', options: ['雷达', '通信', '电子对抗', '指挥控制', '导弹', '无人机', '卫星', '其他'] },
      { name: 'model', label: '产品型号', type: 'text' },
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

function formatValue(val: any, type: string): string {
  if (val === null || val === undefined || val === '') return ''
  if (type === 'date' && val) {
    const d = new Date(val)
    if (!isNaN(d.getTime())) return d.toLocaleDateString('zh-CN')
    return val
  }
  return String(val)
}

function parseIfNeeded(v: any, fallback: any): any {
  if (v === null || v === undefined) return fallback
  if (typeof v === 'string') {
    try { return JSON.parse(v) } catch { return fallback }
  }
  return v
}

function loadFormFromData(data: any) {
  form.equipmentInfo = parseIfNeeded(data.equipmentInfo, {})
  form.tacticalIndicators = parseIfNeeded(data.tacticalIndicators, [])
  form.productTree = parseIfNeeded(data.productTree, [])
  form.milestones = parseIfNeeded(data.milestones, [])
}

async function loadMasterData() {
  try {
    const res = await getMasterData(projectId)
    const data = res.data.data
    if (data) {
      version.value = data.versionNo || 0
      loadFormFromData(data)
    }
  } catch { /* ignore */ }
}

async function handleExtract() {
  extracting.value = true
  extractProgress.value = 10
  extractStatus.value = '正在读取项目输入文件...'

  const timer = setInterval(() => {
    if (extractProgress.value < 90) {
      extractProgress.value += Math.random() * 12
    }
    const msgs = ['正在读取项目输入文件...', '正在分析文档内容...', 'AI 正在提取产品信息...', 'AI 正在提取战术指标...', '整理结构化数据...']
    extractStatus.value = msgs[Math.floor(Math.random() * msgs.length)]
  }, 1500)

  try {
    const res = await extractMasterData(projectId)
    clearInterval(timer)
    extractProgress.value = 100
    extractStatus.value = '提取完成!'

    const result = res.data
    const data = result?.data ?? result
    if (data && typeof data === 'object') {
      loadFormFromData(data)
      version.value = (data.versionNo || 0) + 1
      extractedFrom.value = data._fileCount || 1
      await saveMasterData(projectId, {
        equipmentInfo: form.equipmentInfo,
        tacticalIndicators: form.tacticalIndicators,
        productTree: form.productTree,
        milestones: form.milestones
      }, 1)
      ElMessage.success('AI 提取完成，数据已自动保存。请查看确认各模块内容。')
      isEditing.value = false
    } else {
      ElMessage.warning('提取完成但返回数据为空')
    }
  } catch (err: any) {
    clearInterval(timer)
    const msg = err?.response?.data?.message || err?.message || '提取失败'
    ElMessage.error(msg)
  } finally {
    setTimeout(() => {
      extracting.value = false
      extractProgress.value = 0
      extractStatus.value = ''
    }, 600)
  }
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
    isEditing.value = false
  } catch {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

function cancelEdit() {
  loadMasterData()
  isEditing.value = false
}

onMounted(loadMasterData)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 16px; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.progress-text { font-size: 12px; color: #606266; margin-top: 4px; }
.view-cards { display: flex; flex-wrap: wrap; gap: 12px; }
.data-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px 16px;
  background: #fafafa;
  min-width: 280px;
}
.card-field { display: flex; padding: 4px 0; font-size: 13px; }
.field-label { color: #909399; width: 80px; flex-shrink: 0; }
.field-value { color: #303133; word-break: break-all; }
.empty-section { color: #c0c4cc; font-size: 13px; padding: 16px 0; text-align: center; }
</style>
