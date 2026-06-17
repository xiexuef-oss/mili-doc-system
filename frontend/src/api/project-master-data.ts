import api from './index'

export interface ProjectMasterData {
  id?: number
  projectId: number
  equipmentInfo?: string
  tacticalIndicators?: string
  productTree?: string
  teamMembers?: string
  milestones?: string
  extendedFields?: string
  versionNo?: number
  status?: string
  createdBy?: number
  createdAt?: string
  updatedBy?: number
  updatedAt?: string
}

export function getMasterData(projectId: number) {
  return api.get(`/projects/${projectId}/master-data`)
}

export function getFlattenedMasterData(projectId: number) {
  return api.get(`/projects/${projectId}/master-data/flattened`)
}

export function saveMasterData(projectId: number, data: Record<string, any>, operatorId: number) {
  return api.post(`/projects/${projectId}/master-data?operatorId=${operatorId}`, data)
}

export function getMasterDataSchema() {
  return api.get('/projects/1/master-data/schema')
}

export function getDefaultMasterData() {
  return api.get('/projects/1/master-data/default')
}

export function getNavigatorData(projectId: number) {
  return api.get(`/projects/${projectId}/navigator`)
}

export function extractMasterData(projectId: number) {
  return api.post(`/projects/${projectId}/master-data/extract`)
}
