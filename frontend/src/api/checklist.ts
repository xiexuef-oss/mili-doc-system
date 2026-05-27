import api from './index'

export interface StageChecklistTemplate {
  id: number
  docCode: string
  docName: string
  category: string
  categoryCode: string
  applicableStages: string
  primaryStage: string
  responsibility: string
  requiredFlag: boolean
  gjbReference: string
  description: string
  keywords: string
  orderNum: number
}

export interface ProjectDocChecklistItem {
  id?: number
  projectId: number
  stageId: number
  stageCode: string
  templateId?: number
  docName: string
  category: string
  categoryCode: string
  docStatus: string
  responsiblePerson?: string
  plannedDate?: string
  completedDate?: string
  fileId?: number
  sortOrder: number
  isCustom: boolean
  notes?: string
  createdAt?: string
  updatedAt?: string
}

export function getTemplates(stageCode: string) {
  return api.get('/stage-checklist/templates', { params: { stageCode } })
}

export function getTemplatesByCategory(stageCode: string) {
  return api.get('/stage-checklist/templates/by-category', { params: { stageCode } })
}

export function generateChecklist(projectId: number, stageId: number) {
  return api.post(`/projects/${projectId}/stages/${stageId}/checklist/generate`)
}

export function getChecklist(projectId: number, stageId: number) {
  return api.get(`/projects/${projectId}/stages/${stageId}/checklist`)
}

export function getChecklistStats(projectId: number, stageId: number) {
  return api.get(`/projects/${projectId}/stages/${stageId}/checklist/stats`)
}

export function addCustomItem(projectId: number, stageId: number, docName: string, category?: string) {
  return api.post(`/projects/${projectId}/stages/${stageId}/checklist/items`, { docName, category })
}

export function updateChecklistItem(projectId: number, stageId: number, itemId: number, data: {
  docName?: string
  docStatus?: string
  responsiblePerson?: string
  notes?: string
}) {
  return api.put(`/projects/${projectId}/stages/${stageId}/checklist/items/${itemId}`, data)
}

export function deleteChecklistItem(projectId: number, stageId: number, itemId: number) {
  return api.delete(`/projects/${projectId}/stages/${stageId}/checklist/items/${itemId}`)
}

export function syncChecklistToLedger(projectId: number, stageId: number) {
  return api.post(`/projects/${projectId}/stages/${stageId}/checklist/sync-to-ledger`)
}

export function getStageDefinitionsWithChecklist() {
  return api.get('/stages/definitions/with-checklist')
}
