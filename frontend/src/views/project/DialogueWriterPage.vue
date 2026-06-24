<template>
  <div class="dw-page">
    <div class="dw-page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h3>对话式写作 — {{ docName }}</h3>
    </div>
    <DialogueWriter v-if="ready" :project-id="projectId" :doc-type="docType" :doc-name="docName" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import DialogueWriter from '@/components/DialogueWriter.vue'

const route = useRoute()
const router = useRouter()
const projectId = ref(Number(route.query.projectId) || 0)
const docType = ref((route.query.docType as string) || 'any_draft')
const docName = ref((route.query.docName as string) || '文档')
const ready = ref(false)

function goBack() { router.back() }
onMounted(() => { ready.value = true })
</script>

<style scoped>
.dw-page { height: 100vh; display: flex; flex-direction: column; }
.dw-page-header { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-bottom: 1px solid #ebeef5; }
.dw-page-header h3 { margin: 0; font-size: 16px; }
</style>
