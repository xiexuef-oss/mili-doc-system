<template>
  <div class="page">
    <div class="back-bar">
      <el-button link @click="$router.push('/templates')">
        <el-icon><ArrowLeft /></el-icon>返回模版库
      </el-button>
    </div>

    <el-card v-loading="loading" class="editor-card">
      <template #header>
        <h3>从模版创建文档</h3>
      </template>

      <el-steps :active="activeStep" align-center style="margin-bottom: 32px">
        <el-step title="选择模版" />
        <el-step title="填写变量" />
        <el-step title="确认生成" />
      </el-steps>

      <!-- Step 1: 选择模版 -->
      <div v-show="activeStep === 0" class="step-content">
        <el-form label-width="100px">
          <el-form-item label="选择项目">
            <el-select v-model="selectedProjectId" placeholder="选择目标项目" style="width: 400px" filterable>
              <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="选择模版">
            <el-radio-group v-model="selectedTemplateId" class="template-radios">
              <div v-for="t in templates" :key="t.id" class="template-option">
                <el-radio :value="t.id" border>
                  <div class="template-info">
                    <strong>{{ t.templateName }}</strong>
                    <span class="template-type">{{ t.templateType }}</span>
                    <span class="template-desc">{{ t.description }}</span>
                  </div>
                </el-radio>
              </div>
            </el-radio-group>
            <el-empty v-if="templates.length === 0" description="暂无可用的模版" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2: 填写变量 -->
      <div v-show="activeStep === 1" class="step-content">
        <div v-if="variableFields.length === 0" class="no-vars">
          <el-empty description="该模版没有定义变量，可直接生成" />
        </div>
        <el-form v-else ref="varFormRef" label-width="140px" class="var-form">
          <el-form-item
            v-for="(desc, key) in variableFields"
            :key="key"
            :label="key"
          >
            <el-input v-model="variableValues[key]" :placeholder="'请输入' + desc" style="width: 400px" />
            <span class="var-desc">{{ desc }}</span>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 3: 确认生成 -->
      <div v-show="activeStep === 2" class="step-content">
        <el-descriptions title="生成确认" :column="2" border>
          <el-descriptions-item label="目标项目">{{ selectedProject?.projectName }}</el-descriptions-item>
          <el-descriptions-item label="使用模版">{{ selectedTemplate?.templateName }}</el-descriptions-item>
          <el-descriptions-item label="模版类型">{{ selectedTemplate?.templateType }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ selectedProject?.projectType }}</el-descriptions-item>
          <el-descriptions-item v-if="Object.keys(variableValues).length > 0" label="变量值" :span="2">
            <el-tag v-for="(v, k) in variableValues" :key="k" size="small" style="margin: 2px">
              {{ k }} = {{ v }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="step-actions">
        <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
        <el-button v-if="activeStep < 2" type="primary" @click="handleNext">下一步</el-button>
        <el-button v-if="activeStep === 2" type="primary" :loading="generating" @click="handleGenerate">
          <el-icon><DocumentAdd /></el-icon>生成文档
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, DocumentAdd } from '@element-plus/icons-vue'
import { getTemplateList, generateFromTemplate, type TemplateItem } from '@/api/template'
import { getProjects, type ProjectItem } from '@/api/project'

const router = useRouter()

const loading = ref(false)
const generating = ref(false)
const activeStep = ref(0)
const templates = ref<TemplateItem[]>([])
const projects = ref<ProjectItem[]>([])
const selectedTemplateId = ref<number | null>(null)
const selectedProjectId = ref<number | null>(null)
const variableValues = ref<Record<string, string>>({})

const selectedTemplate = computed(() => templates.value.find(t => t.id === selectedTemplateId.value) || null)
const selectedProject = computed(() => projects.value.find(p => p.id === selectedProjectId.value) || null)

const variableFields = computed(() => {
  if (!selectedTemplate.value?.variables) return {}
  try {
    return JSON.parse(selectedTemplate.value.variables)
  } catch {
    return {}
  }
})

function handleNext() {
  if (activeStep.value === 0) {
    if (!selectedProjectId.value) { ElMessage.warning('请选择目标项目'); return }
    if (!selectedTemplateId.value) { ElMessage.warning('请选择模版'); return }
  }
  activeStep.value++
}

async function handleGenerate() {
  generating.value = true
  try {
    await generateFromTemplate(
      selectedTemplateId.value!,
      selectedProjectId.value!,
      variableValues.value
    )
    ElMessage.success('文档已生成，跳转到项目文档页面')
    router.push(`/projects/${selectedProjectId.value}/documents`)
  } catch {
    ElMessage.error('生成失败')
  } finally {
    generating.value = false
  }
}

async function fetchData() {
  loading.value = true
  try {
    const [templateRes, projectRes] = await Promise.all([
      getTemplateList({ pageSize: 100, status: 'ACTIVE' }).catch(() => ({ data: { data: { records: [] } } })),
      getProjects({ pageSize: 100 }).catch(() => ({ data: { data: { records: [] } } }))
    ])
    templates.value = templateRes.data.data.records
    projects.value = projectRes.data.data.records
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 4px; min-height: calc(100vh - 140px); }
.back-bar { margin-bottom: 16px; }
.editor-card { max-width: 900px; }
.editor-card h3 { margin: 0; font-size: 17px; }
.step-content { min-height: 260px; padding: 8px 0; }
.step-actions { margin-top: 24px; display: flex; justify-content: center; gap: 12px; }
.template-radios { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.template-option { width: 100%; }
.template-info { display: flex; align-items: center; gap: 16px; }
.template-type { color: #409eff; font-size: 12px; background: #ecf5ff; padding: 2px 8px; border-radius: 3px; }
.template-desc { color: #909399; font-size: 13px; }
.no-vars { text-align: center; padding: 40px 0; }
.var-form { max-width: 600px; }
.var-desc { color: #909399; font-size: 12px; margin-left: 8px; }
</style>
