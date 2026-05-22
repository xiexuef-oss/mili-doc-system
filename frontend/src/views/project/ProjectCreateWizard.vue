<template>
  <div class="page">
    <div class="page-header">
      <el-button link @click="$router.back()"><el-icon><ArrowLeft /></el-icon>返回项目列表</el-button>
      <h3>创建新项目</h3>
    </div>

    <el-steps :active="step" align-center style="margin:24px 0">
      <el-step title="基本信息" />
      <el-step title="输入文件" />
      <el-step title="主数据" />
      <el-step title="确认创建" />
    </el-steps>

    <div class="step-content">
      <!-- Step 1: Basic Info -->
      <el-form v-if="step === 0" ref="form1Ref" :model="form" :rules="rules1" label-width="100px" class="wizard-form">
        <el-form-item label="项目编号" prop="projectCode">
          <el-input v-model="form.projectCode" placeholder="如 PRJ-2026-001" />
        </el-form-item>
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="输入项目名称" />
        </el-form-item>
        <el-form-item label="项目类型" prop="projectType">
          <el-select v-model="form.projectType" style="width:100%" placeholder="选择项目类型">
            <el-option v-for="t in projectTypes" :key="t.dictCode" :label="t.dictName" :value="t.dictCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="密级" prop="securityLevel">
          <el-select v-model="form.securityLevel" style="width:100%">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="秘密" value="SECRET" />
            <el-option label="机密" value="CONFIDENTIAL" />
            <el-option label="绝密" value="TOP_SECRET" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用标准">
          <el-input v-model="form.applicableStandards" type="textarea" :rows="2" placeholder="如 GJB 6387-2008, GJB 0.2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目简要描述" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>

      <!-- Step 2: Input Files -->
      <div v-else-if="step === 1">
        <el-empty v-if="inputFiles.length === 0" description="暂无输入文件，可创建项目后再添加">
          <el-button type="primary" @click="step = 2">跳过此步</el-button>
        </el-empty>
        <div v-else class="input-files">
          <div v-for="(f, i) in inputFiles" :key="i" class="file-row">
            <el-input v-model="f.fileName" placeholder="文件名" style="width:250px" />
            <el-input v-model="f.fileUrl" placeholder="文件URL/路径" style="width:300px" />
            <el-button link type="danger" @click="inputFiles.splice(i,1)">删除</el-button>
          </div>
          <el-button type="primary" link @click="inputFiles.push({fileName:'',fileUrl:''})">
            <el-icon><Plus /></el-icon>添加文件
          </el-button>
        </div>
      </div>

      <!-- Step 3: Master Data -->
      <div v-else-if="step === 2">
        <el-alert title="项目主数据可在创建后填写，此步骤可选" type="info" :closable="false" show-icon style="margin-bottom:16px" />
        <el-form label-width="120px" size="small">
          <template v-for="section in masterDataSections" :key="section.key">
            <MasterDataFormSection
              :section="section"
              :model="masterData"
              :prefix="section.key"
            />
          </template>
        </el-form>
      </div>

      <!-- Step 4: Confirm -->
      <div v-else-if="step === 3">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目编号">{{ form.projectCode }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ form.projectName }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ typeLabel(form.projectType) }}</el-descriptions-item>
          <el-descriptions-item label="密级">{{ securityLabel(form.securityLevel) }}</el-descriptions-item>
          <el-descriptions-item label="适用标准">{{ form.applicableStandards || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ form.startDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="输入文件" :span="2">{{ inputFiles.length }} 个文件</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ form.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <div class="step-actions">
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="step === 3" type="success" :loading="creating" @click="handleCreate">创建项目</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { createProject } from '@/api/project'
import { saveMasterData } from '@/api/project-master-data'
import { getDictItems, type DictItem } from '@/api/dict'
import MasterDataFormSection from '@/components/MasterDataFormSection.vue'

const router = useRouter()
const step = ref(0)
const creating = ref(false)
const projectTypes = ref<DictItem[]>([])

const form = reactive({
  projectCode: '', projectName: '', projectType: 'MODEL',
  securityLevel: 'INTERNAL', applicableStandards: '', description: '', startDate: ''
})

const inputFiles = ref<Array<{fileName: string; fileUrl: string}>>([])

const masterData: Record<string, any> = reactive({
  equipmentInfo: {}, tacticalIndicators: {}, productTree: {},
  teamMembers: {}, milestones: {}
})

const masterDataSections = [
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
      { name: 'range', label: '作用距离', type: 'text', tip: '单位: km' },
      { name: 'accuracy', label: '精度', type: 'text' },
      { name: 'weight', label: '重量', type: 'text', tip: '单位: kg' },
      { name: 'power', label: '功耗', type: 'text', tip: '单位: W' },
      { name: 'mtbf', label: 'MTBF', type: 'text', tip: '平均故障间隔时间' },
      { name: 'conclusion', label: '符合性结论', type: 'select', options: ['满足', '基本满足', '需验证', '不满足'] }
    ]
  },
  {
    key: 'productTree', label: '产品分解结构',
    fields: [
      { name: 'level', label: '产品层级', type: 'select', options: ['系统', '分系统', '设备', '组件', '部件', '零件'] },
      { name: 'productName', label: '产品名称', type: 'text' },
      { name: 'productCode', label: '产品代号', type: 'text' },
      { name: 'parentProduct', label: '父级产品', type: 'text' },
      { name: 'quantity', label: '数量', type: 'text' },
      { name: 'remark', label: '备注', type: 'textarea' }
    ]
  },
  {
    key: 'teamMembers', label: '项目团队',
    fields: [
      { name: 'role', label: '角色', type: 'select', options: ['总设计师', '副总设计师', '主任设计师', '主管设计师', '设计师', '质量师', '标准化师', '工艺师', '可靠性工程师', '项目管理员'] },
      { name: 'name', label: '姓名', type: 'text' },
      { name: 'department', label: '所属部门', type: 'text' },
      { name: 'phone', label: '联系电话', type: 'text' },
      { name: 'email', label: '邮箱', type: 'text' }
    ]
  },
  {
    key: 'milestones', label: '里程碑',
    fields: [
      { name: 'stageCode', label: '阶段', type: 'select', options: ['L', 'F', 'C', 'S', 'D', 'P', 'N'] },
      { name: 'name', label: '名称', type: 'text' },
      { name: 'plannedDate', label: '计划日期', type: 'date' },
      { name: 'actualDate', label: '实际日期', type: 'date' },
      { name: 'keyDeliverables', label: '关键交付物', type: 'textarea' },
      { name: 'status', label: '状态', type: 'select', options: ['未开始', '进行中', '已完成', '已延期'] }
    ]
  }
]

const rules1 = {
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectType: [{ required: true, message: '请选择项目类型', trigger: 'change' }],
  securityLevel: [{ required: true, message: '请选择密级', trigger: 'change' }]
}

function typeLabel(t: string) {
  const map: Record<string, string> = { MODEL: '型号项目', PRE_RESEARCH: '预研项目', TECH_IMPROVE: '技改项目' }
  return map[t] || t
}

function securityLabel(s: string) {
  const map: Record<string, string> = { PUBLIC: '公开', INTERNAL: '内部', SECRET: '秘密', CONFIDENTIAL: '机密', TOP_SECRET: '绝密' }
  return map[s] || s
}

async function nextStep() {
  if (step.value === 0) {
    if (!form.projectCode || !form.projectName || !form.projectType) {
      ElMessage.warning('请填写必填项'); return
    }
  }
  step.value++
}

async function handleCreate() {
  creating.value = true
  try {
    const res = await createProject(form as any)
    const project = res.data.data
    const projectId = project.id

    // Save master data if any filled
    const hasAnyData = Object.values(masterData).some((v: any) => Object.keys(v || {}).length > 0)
    if (hasAnyData) {
      await saveMasterData(projectId, { ...masterData }, 1)
    }

    ElMessage.success('项目创建成功')
    router.push({ name: 'ProjectWorkspace', params: { projectId } })
  } catch {
    ElMessage.error('创建失败')
  }
  creating.value = false
}

async function loadProjectTypes() {
  try {
    const res = await getDictItems('PROJECT_TYPE')
    projectTypes.value = res.data.data || []
  } catch { /* ignore */ }
}

onMounted(loadProjectTypes)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
.page-header h3 { margin: 0; font-size: 16px; }
.step-content { max-width: 700px; margin: 0 auto; }
.wizard-form { margin-top: 16px; }
.step-actions { text-align: center; margin-top: 32px; }
.input-files { display: flex; flex-direction: column; gap: 8px; }
.file-row { display: flex; align-items: center; gap: 12px; }
</style>
