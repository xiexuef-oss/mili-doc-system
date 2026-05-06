import api from './index'

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
}

export function getProjectStages(projectId: number) {
  return api.get(`/stages/project/${projectId}`)
}

export function createProjectStage(data: ProjectStageItem) {
  return api.post('/stages', data)
}

export function updateProjectStage(id: number, data: ProjectStageItem) {
  return api.put(`/stages/${id}`, data)
}
