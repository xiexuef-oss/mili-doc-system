import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getProject, type ProjectItem } from '@/api/project'
import { getProjectStages, type ProjectStageItem } from '@/api/project-stage'

/**
 * Global project state store.
 * Eliminates duplicate API calls across 10+ pages in ProjectWorkspace.
 */
export const useProjectStore = defineStore('project', () => {
  const projectId = ref<number>(0)
  const project = ref<ProjectItem | null>(null)
  const stages = ref<ProjectStageItem[]>([])
  const loading = ref(false)
  const kanbanRefresh = ref(0) // increment to trigger kanban reload

  function triggerKanbanRefresh() { kanbanRefresh.value++ }
  const projectName = computed(() => project.value?.projectName || '')
  const projectType = computed(() => project.value?.projectType || '')
  const securityLevel = computed(() => project.value?.securityLevel || '内部')
  const projectStatus = computed(() => project.value?.status || '')

  async function loadProject(id: number) {
    if (projectId.value === id && project.value) return project.value
    projectId.value = id
    loading.value = true
    try {
      const res = await getProject(id)
      project.value = res.data.data as ProjectItem
      return project.value
    } finally {
      loading.value = false
    }
  }

  async function loadStages() {
    if (!projectId.value) return []
    try {
      const res = await getProjectStages(projectId.value)
      stages.value = res.data.data || []
      return stages.value
    } catch {
      return []
    }
  }

  function $reset() {
    projectId.value = 0
    project.value = null
    stages.value = []
    loading.value = false
  }

  return {
    projectId, project, stages, loading, kanbanRefresh,
    projectName, projectType, securityLevel, projectStatus,
    loadProject, loadStages, triggerKanbanRefresh, $reset
  }
})
