import api from './index'

export interface StageDefinitionItem {
  code: string
  name: string
  order: number
  defaultBaselineType: string | null
  description: string
}

export interface ProjectStageItem {
  id?: number
  projectId: number
  stageCode: string
  stageName: string
  stageOrder: number
  status: string
  startDate: string
  endDate: string
  stageGoal: string
  entryCriteria: string
  exitCriteria: string
  stageManagerId?: number
  technicalManagerId?: number
  qualityManagerId?: number
  isCurrent?: boolean
  allowParallel?: boolean
}

// Get predefined stage definitions (7 fixed stages)
export function getStageDefinitions() {
  return api.get('/stages/definitions')
}

// Initialize project stages from a single initial stage (auto-includes all subsequent stages)
export function initializeStages(projectId: number, initialStageCode: string) {
  return api.post(`/projects/${projectId}/stages/initialize`, { initialStageCode })
}

export function getProjectStages(projectId: number) {
  return api.get(`/projects/${projectId}/stages`)
}

export function updateProjectStage(projectId: number, stageId: number, data: ProjectStageItem) {
  return api.put(`/projects/${projectId}/stages/${stageId}`, data)
}

// Request stage transition (gate check → complete current → start next)
export function requestTransition(projectId: number, stageId: number) {
  return api.post(`/projects/${projectId}/stages/${stageId}/request-transition`)
}

export function suspendStage(projectId: number, stageId: number) {
  return api.post(`/projects/${projectId}/stages/${stageId}/suspend`)
}

export function terminateStage(projectId: number, stageId: number) {
  return api.post(`/projects/${projectId}/stages/${stageId}/terminate`)
}

export function getStageWorkbench(projectId: number, stageId: number) {
  return api.get(`/projects/${projectId}/stages/${stageId}/workbench`)
}

export function gateCheck(projectId: number, stageId: number) {
  return api.post(`/projects/${projectId}/stages/${stageId}/gate-check`)
}
