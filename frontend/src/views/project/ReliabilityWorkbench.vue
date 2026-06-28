<template>
  <div class="reliability-workbench">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">可靠性设计工作台</span>
      
  <!-- Prerequisites check -->
  <PrerequisitesCheck
    v-model="prereqVisible"
    :project-id="projectId"
    :doc-type="prereqDocType"
    @proceed="onPrereqProceed"
  />
</template>
    </el-page-header>

    <el-alert type="info" :closable="false" style="margin-top:16px">
      <template #title>
        使用说明 | 本页是可靠性设计的<strong>专用工作台</strong>，生成的文档统一归入「文档台账」管理
      </template>
      <ul style="margin:4px 0;padding-left:20px;font-size:13px;line-height:1.8">
        <li><strong>第1步：</strong>设置可靠性指标（MTBF 必填，其余选填）</li>
        <li><strong>第2步：</strong>选择文档类型，点击生成，AI 按 GJB 标准撰写初稿</li>
        <li><strong>第3步：</strong>生成后自动存入 <el-link type="primary" @click="$router.push({name:'ProjectDocLedger',params:{projectId}})">📋 文档台账</el-link>，可在台账中编辑、校对、导出</li>
        <li><strong>预计/分配：</strong>需先输入计算参数，再生成报告</li>
      </ul>
    </el-alert>

    <el-card class="section-card" shadow="hover">
      <template #header>
        <span>📊 可靠性指标要求</span>
        <el-button type="primary" size="small" style="float:right" @click="openReqDialog">编辑指标</el-button>
      </template>
      <el-descriptions :column="3" border v-if="requirement">
        <el-descriptions-item label="MTBF">{{ requirement.mtbfHours || '—' }} h</el-descriptions-item>
        <el-descriptions-item label="MTBCF">{{ requirement.mtbcfHours || '—' }} h</el-descriptions-item>
        <el-descriptions-item label="验证方法">{{ {TEST:'鉴定试验',ANALYSIS:'分析评价',EVALUATION:'使用评估'}[requirement.verificationMethod] || requirement.verificationMethod || '—' }}</el-descriptions-item>
        <el-descriptions-item label="可靠度 R(t)">
          <span v-if="requirement.reliabilityAtTime">{{ requirement.reliabilityAtTime }} @ {{ requirement.reliabilityTimeHours }}h</span>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="使用寿命">{{ requirement.serviceLifeYears || '—' }} 年</el-descriptions-item>
        <el-descriptions-item label="指标来源">{{ {CONTRACT:'合同/研制总要求',SPEC:'研制规范',ALLOCATION:'分配结果'}[requirement.requirementSource] || requirement.requirementSource || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="尚未设置可靠性指标，请点击编辑指标按钮设置" />
    </el-card>

    <el-card class="section-card" shadow="hover">
      <template #header>📝 可靠性文档生成</template>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never" class="doc-card">
            <div class="doc-title">📋 可靠性大纲 <el-tag size="small" type="info">纯文本</el-tag></div>
            <div class="doc-desc">依据 GJB 450B，覆盖 100/300/400 系列工作项目</div>
            <el-button :type="isGenerated('outline') ? 'default' : 'primary'" :loading="generating==='outline'" @click="checkThenGenerate('reliability_outline', () => generateDoc('outline'))">{{ genBtnLabel('outline') }}</el-button>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="doc-card">
            <div class="doc-title">🔻 降额设计报告 <el-tag size="small" type="info">纯文本</el-tag></div>
            <div class="doc-desc">依据 GJB/Z 35，逐类器件降额审核</div>
            <el-button :type="isGenerated('derating') ? 'default' : 'primary'" :loading="generating==='derating'" @click="checkThenGenerate('derating', () => generateDoc('derating'))">{{ genBtnLabel('derating') }}</el-button>
          </el-card>
        </el-col>
        <el-col :span="12" style="margin-top:12px">
          <el-card shadow="never" class="doc-card">
            <div class="doc-title">🔢 可靠性预计报告 <el-tag size="small" type="warning">需BOM</el-tag></div>
            <div class="doc-desc">GJB/Z 299D 应力分析法，导入 BOM 自动计算 MTBF</div>
            <el-button type="success" @click="showPredictionDialog=true">导入BOM预计</el-button>
          </el-card>
        </el-col>
        <el-col :span="12" style="margin-top:12px">
          <el-card shadow="never" class="doc-card">
            <div class="doc-title">📐 可靠性分配报告 <el-tag size="small" type="warning">需指标</el-tag></div>
            <div class="doc-desc">等分配/评分/AGREE 三种方法，自动验证</div>
            <el-button type="success" @click="showAllocationDialog=true">执行分配</el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 已生成文档列表 -->
    <el-card class="section-card" v-if="Object.keys(generatedDocs).length > 0" shadow="hover">
      <template #header>📄 已生成的可靠性文档</template>
      <el-table :data="Object.entries(generatedDocs).map(([k,v]) => ({type:k,...v}))" size="small">
        <el-table-column label="文档名称" prop="docName" />
        <el-table-column label="章节数" width="80">
          <template #default="s">{{ s.row.chapters || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="s">
            <el-tag size="small" :type="s.row.docLedgerId ? 'success' : 'warning'">
              {{ s.row.docLedgerId ? '已入库' : '未入库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="s">
            <el-button size="small" @click="$router.push({name:'ProjectDocLedger',params:{projectId}})">台账查看</el-button>
            <el-button size="small" type="primary" @click="generateDoc(s.row.type)">重新生成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 生成进度 -->
    <el-card class="section-card" v-if="generating" shadow="hover">
      <template #header>
        <span>⏳ 正在生成...</span>
        <span style="float:right;color:#909399;font-size:13px">已用 {{ elapsed }} 秒</span>
      </template>
      <el-progress :percentage="progressPercent" :stroke-width="8" :text-inside="true"
        :status="progressPercent === 100 ? 'success' : ''" />
      <p style="text-align:center;margin-top:8px;color:#606266">{{ progressText }}</p>
    </el-card>

    <el-card class="section-card" v-if="generatedContent" shadow="hover">
      <template #header>
        <span>✅ 生成结果</span>
        <span v-if="lastDocInfo?.docLedgerId" style="margin-left:12px;color:#67c23a;font-size:13px">
          已存入文档台账 #{{ lastDocInfo.docLedgerId }} | {{ lastDocInfo.chapters || 0 }} 章
          <el-link type="primary" underline="never" @click="$router.push({name:'ProjectDocLedger',params:{projectId}})" style="margin-left:8px;font-size:13px">
            去台账查看 →
          </el-link>
        </span>
        <el-button size="small" style="float:right" @click="copyContent">复制</el-button>
      </template>
      <div class="markdown-preview" v-html="renderedContent"></div>
    </el-card>

    <!-- 指标编辑对话框 -->
    <el-dialog v-model="showReqDialog" title="编辑可靠性指标" width="500px">
      <el-form :model="reqForm" label-width="100px">
        <el-divider content-position="left">基本可靠性指标（核心）</el-divider>
        <el-form-item label="MTBF (h)" required>
          <el-input-number v-model="reqForm.mtbfHours" :min="0" :step="100" />
        </el-form-item>
        <el-divider content-position="left">扩展指标（选填）</el-divider>
        <el-form-item label="MTBCF (h)">
          <el-input-number v-model="reqForm.mtbcfHours" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="可靠度 R(t)">
          <el-input-number v-model="reqForm.reliabilityAtTime" :min="0" :max="1" :step="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="对应时间(h)">
          <el-input-number v-model="reqForm.reliabilityTimeHours" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="使用寿命(年)">
          <el-input-number v-model="reqForm.serviceLifeYears" :min="0" :step="1" />
        </el-form-item>
        <el-divider content-position="left">验证信息</el-divider>
        <el-form-item label="验证方法">
          <el-select v-model="reqForm.verificationMethod">
            <el-option label="鉴定试验" value="TEST" />
            <el-option label="分析评价" value="ANALYSIS" />
            <el-option label="使用评估" value="EVALUATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障判据">
          <el-input v-model="reqForm.failureCriteria" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReqDialog=false">取消</el-button>
        <el-button type="primary" @click="saveRequirement">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预计对话框 -->
    <el-dialog v-model="showPredictionDialog" title="可靠性预计" width="700px">
      <el-form inline>
        <el-form-item label="环境类别">
          <el-select v-model="predictionEnv" style="width:180px">
            <el-option label="地面固定 G_FIX" value="G_FIX" />
            <el-option label="机载有人战斗机 A_IF" value="A_IF" />
            <el-option label="舰船固定 N_FIX" value="N_FIX" />
            <el-option label="航天飞行 S_F" value="S_F" />
            <el-option label="导弹飞行 M_F" value="M_F" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="runPredictionPreview">预计计算</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="bomItems" border size="small" max-height="300">
        <el-table-column prop="partName" label="器件" width="80" />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column prop="subtype" label="子类型" width="120" />
        <el-table-column prop="quantity" label="数量" width="60" />
        <el-table-column prop="qualityLevel" label="质量等级" width="80" />
        <el-table-column prop="temperature" label="温度℃" width="70" />
        <el-table-column prop="stressRatio" label="应力比" width="70" />
      </el-table>
      <div v-if="predictionResult" class="prediction-result">
        <el-statistic title="总失效率" :value="predictionResult.totalFailureRate" :precision="6" suffix="×10⁻⁶/h" />
        <el-statistic title="MTBF" :value="Math.round(predictionResult.mtbf)" suffix="h" />
      </div>
      <template #footer>
        <el-button @click="showPredictionDialog=false">取消</el-button>
        <el-button type="primary" @click="checkThenGenerate('reliability_prediction', generatePrediction)" :loading="generating==='prediction'">生成预计报告</el-button>
      </template>
    </el-dialog>

    <!-- 分配对话框 -->
    <el-dialog v-model="showAllocationDialog" title="可靠性分配" width="500px">
      <el-form label-width="100px">
        <el-form-item label="分配方法">
          <el-select v-model="allocMethod">
            <el-option label="等分配法" value="EQUAL" />
            <el-option label="评分分配法" value="SCORING" />
            <el-option label="AGREE分配法" value="AGREE" />
          </el-select>
        </el-form-item>
        <el-form-item label="系统MTBF(h)">
          <el-input-number v-model="allocSysMtbf" :min="1" :step="1000" />
        </el-form-item>
        <el-form-item label="分配单元数" v-if="allocMethod==='EQUAL'">
          <el-input-number v-model="allocUnits" :min="2" :max="20" />
        </el-form-item>
      </el-form>
      <div v-if="allocationResult">
        <el-table :data="allocationResult.items" border size="small">
          <el-table-column prop="unitName" label="单元" />
          <el-table-column label="分配MTBF(h)">
            <template #default="scope">{{ Math.round(scope.row.allocatedMtbf) }}</template>
          </el-table-column>
          <el-table-column label="比例">
            <template #default="scope">{{ (scope.row.allocationRatio*100).toFixed(1) }}%</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="showAllocationDialog=false">取消</el-button>
        <el-button @click="runAllocationPreview">预览</el-button>
        <el-button type="primary" @click="checkThenGenerate('reliability_allocation', generateAllocation)" :loading="generating==='allocation'">生成分配报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRelRequirements, saveRelRequirement, generateReliabilityOutline, generateDeratingReport, generatePredictionReport, previewPrediction, generateAllocationReport, previewAllocation } from '@/api/reliability'
import PrerequisitesCheck from '@/components/PrerequisitesCheck.vue'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const projectId = computed(() => Number(route.params.projectId) || 0)

/** Unwrap axios response: { data: { code:'SUCCESS', data: actual } } → actual */
function unwrap(res: any) {
  if (!res) return null
  const d = res.data
  if (d && typeof d === 'object' && 'data' in d) return d.data
  return d || res
}

const requirement = ref<any>(null)
const showReqDialog = ref(false)
const reqForm = ref<any>({ mtbfHours: 5000, verificationMethod: 'ANALYSIS' })
const generating = ref<string|null>(null)
const currentStageId = ref<number|undefined>(undefined)
const lastDocInfo = ref<any>(null)
const genStorageKey = computed(() => `rel-docs-${projectId.value}`)
const generatedDocs = ref<Record<string, any>>(
  JSON.parse(localStorage.getItem(`rel-docs-${projectId.value}`) || '{}')
)

/** Save generated docs state to localStorage */
function saveGenState() {
  localStorage.setItem(genStorageKey.value, JSON.stringify(generatedDocs.value))
}

/** Check if a doc type has been generated */
function isGenerated(type: string) { return !!generatedDocs.value[type] }

/** Get button label based on state */
function genBtnLabel(type: string) {
  const names: Record<string, string> = { outline: '可靠性大纲', derating: '降额设计报告', prediction: '可靠性预计报告', allocation: '可靠性分配报告' }
  return isGenerated(type) ? '重新生成' : '生成' + names[type]
}
const generatedContent = ref('')
const progressPercent = ref(0)
const progressText = ref('')
const elapsed = ref(0)
let progressTimer: ReturnType<typeof setInterval> | null = null
const renderedContent = computed(() => sanitizeHtml(generatedContent.value
  .replace(/^### (.+)$/gm, '<h4>$1</h4>')
  .replace(/^## (.+)$/gm, '<h3>$1</h3>')
  .replace(/^# (.+)$/gm, '<h2>$1</h2>')
  .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  .replace(/\n/g, '<br>')))

const showPredictionDialog = ref(false)
const predictionEnv = ref('G_FIX')
const predictionResult = ref<any>(null)
const bomItems = ref([
  { partName:'R1',category:'电阻器',subtype:'片式膜电阻器',quantity:10,qualityLevel:'A2',temperature:45,stressRatio:0.5 },
  { partName:'C1',category:'电容器',subtype:'2类瓷介电容器',quantity:5,qualityLevel:'A2',temperature:50,stressRatio:0.6 },
  { partName:'U1',category:'微电路',subtype:'MOS数字电路',quantity:2,qualityLevel:'B1',temperature:55,stressRatio:0.7 },
  { partName:'D1',category:'半导体分立器件',subtype:'普通二极管',quantity:4,qualityLevel:'A2',temperature:48,stressRatio:0.5 },
])

const showAllocationDialog = ref(false)
const allocMethod = ref('EQUAL')
const allocSysMtbf = ref(5000)
const allocUnits = ref(3)
const allocationResult = ref<any>(null)

onMounted(async () => {
  if (projectId.value) {
    try {
      const res: any = await getRelRequirements(projectId.value)
      let list = res?.data?.data
      if (!Array.isArray(list)) list = Array.isArray(res?.data) ? res.data : []

      if (Array.isArray(list) && list.length > 0) {
        // Sort by id descending to get the latest
        list.sort((a: any, b: any) => (b.id || 0) - (a.id || 0))
        requirement.value = list[0]
      }
    } catch(e){ }
  }
})

async function saveRequirement() {
  try {
    reqForm.value.projectId = projectId.value
    const res: any = await saveRelRequirement(reqForm.value)
    requirement.value = unwrap(res)
    showReqDialog.value = false
    ElMessage.success('指标已保存')
  } catch(e: any) { ElMessage.error('保存失败: '+(e?.message||e)) }
}

function openReqDialog() {
  if (requirement.value) {
    reqForm.value = { ...requirement.value }
  } else {
    reqForm.value = { mtbfHours: 5000, verificationMethod: 'ANALYSIS' }
  }
  showReqDialog.value = true
}

// Progress helpers now inline in generateDoc/generatePrediction/generateAllocation


// Prerequisites check
const prereqVisible = ref(false)
const prereqDocType = ref('')
const prereqCallback = ref(null as (() => void) | null)

function checkThenGenerate(docType: string, callback: () => void) {
  prereqDocType.value = docType
  prereqCallback.value = callback
  prereqVisible.value = true
}

function onPrereqProceed() {
  if (prereqCallback.value) prereqCallback.value()
}

async function generateDoc(type: string) {
  // Show progress
  generating.value = type
  progressPercent.value = 5
  progressText.value = '正在调用 AI 生成...'
  elapsed.value = 0
  const timer = setInterval(() => {
    elapsed.value++
    if (progressPercent.value < 92) progressPercent.value += Math.random() * 8
    const msgs = ['组装上下文...', '加载指标...', '调用 AI...', '格式化...']
    progressText.value = msgs[elapsed.value % msgs.length]
  }, 1000)

  try {
    let res: any
    if (type === 'outline') res = await generateReliabilityOutline(projectId.value)
    else res = await generateDeratingReport(projectId.value)
    const genData = unwrap(res)
    generatedContent.value = typeof genData === 'string' ? genData : (genData?.content || '')
    lastDocInfo.value = genData
    generatedDocs.value[type] = { docLedgerId: genData?.docLedgerId, docName: genData?.docName, chapters: genData?.chapters, updatedAt: new Date() }
    saveGenState()
    progressPercent.value = 100
    progressText.value = '完成'
    ElMessage.success(`生成完成：${genData?.chapters || 0} 章，已存入文档台账 #${genData?.docLedgerId}`)
  } catch(e: any) {
    ElMessage.error('生成失败: '+(e?.message||e))
  } finally {
    clearInterval(timer)
    setTimeout(() => { generating.value = null }, 600)
  }
}

async function runPredictionPreview() {
  try {
    const res: any = await previewPrediction(bomItems.value, predictionEnv.value)
    predictionResult.value = unwrap(res)
  } catch(e: any) { ElMessage.error('预计失败: '+(e?.message||e)) }
}

async function generatePrediction() {
  generating.value = 'prediction'
  progressPercent.value = 10
  elapsed.value = 0
  const timer = setInterval(() => { elapsed.value++; if (progressPercent.value < 90) progressPercent.value += 5 }, 1000)
  try {
    const res: any = await generatePredictionReport(projectId.value, currentStageId.value, bomItems.value, predictionEnv.value)
    const genData = unwrap(res); generatedContent.value = typeof genData === 'string' ? genData : (genData?.content || '')
    showPredictionDialog.value = false
    generatedDocs.value['prediction'] = { docLedgerId: genData?.docLedgerId, docName: '可靠性预计报告', chapters: genData?.chapters, updatedAt: new Date() }
    saveGenState()
    progressPercent.value = 100
    ElMessage.success('预计报告生成完成')
  } catch(e: any) { ElMessage.error('生成失败: '+(e?.message||e)) }
  finally { clearInterval(timer); setTimeout(() => { generating.value = null }, 600) }
}

async function runAllocationPreview() {
  try {
    const res: any = await previewAllocation({ method: allocMethod.value, systemMtbf: allocSysMtbf.value, units: allocUnits.value })
    allocationResult.value = unwrap(res)
  } catch(e: any) { ElMessage.error('分配失败: '+(e?.message||e)) }
}

async function generateAllocation() {
  generating.value = 'allocation'
  progressPercent.value = 10
  elapsed.value = 0
  const timer = setInterval(() => { elapsed.value++; if (progressPercent.value < 90) progressPercent.value += 5 }, 1000)
  try {
    const res: any = await generateAllocationReport(projectId.value, currentStageId.value, { method: allocMethod.value, systemMtbf: allocSysMtbf.value, units: allocUnits.value })
    const genData = unwrap(res); generatedContent.value = typeof genData === 'string' ? genData : (genData?.content || '')
    showAllocationDialog.value = false
    generatedDocs.value['allocation'] = { docLedgerId: genData?.docLedgerId, docName: '可靠性分配报告', chapters: genData?.chapters, updatedAt: new Date() }
    saveGenState()
    progressPercent.value = 100
    ElMessage.success('分配报告生成完成')
  } catch(e: any) { ElMessage.error('生成失败: '+(e?.message||e)) }
  finally { clearInterval(timer); setTimeout(() => { generating.value = null }, 600) }
}

function copyContent() {
  navigator.clipboard.writeText(generatedContent.value)
  ElMessage.success('已复制')
}
</script>

<style scoped>
.reliability-workbench { padding: 16px; max-width: 1200px; margin: 0 auto; }
.page-title { font-size: 18px; font-weight: 600; }
.section-card { margin-top: 16px; }
.doc-card { text-align: center; padding: 8px; }
.doc-title { font-size: 16px; font-weight: 600; margin-bottom: 8px; }
.doc-desc { color: #909399; font-size: 13px; margin-bottom: 12px; }
.markdown-preview { background: #f5f7fa; padding: 16px; border-radius: 4px; max-height: 600px; overflow-y: auto; line-height: 1.8; }
.prediction-result { display: flex; gap: 32px; justify-content: center; margin-top: 12px; }
</style>
