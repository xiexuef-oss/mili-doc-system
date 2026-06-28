import api from './index'

export interface DocLedgerItem {
  id?: number
  projectId: number
  stageId?: number
  catalogId?: number
  docCode?: string
  docName: string
  docCategory?: string
  docType?: string
  stageCode?: string
  requiredFlag?: boolean
  meetingUsage?: string
  usageSource?: string
  usageAdjustReason?: string
  changeReason?: string
  responsibleUserId?: number
  securityLevel?: string
  lifecycleStatus?: string
  fileObjectId?: string
  contentSize?: number
  docContent?: string
  createdBy?: number
  createdAt?: string
  updatedBy?: number
  updatedAt?: string
}

export interface DocLedgerLogItem {
  id: number
  docLedgerId: number
  fromStatus?: string
  toStatus: string
  operatorId?: number
  operatedAt: string
  remark?: string
}

export function getDocLedgers(params: {
  projectId: number
  stageId?: number
  lifecycleStatus?: string
  pageNo?: number
  pageSize?: number
}) {
  return api.get('/doc-ledgers', { params })
}

export function getDocLedger(id: number) {
  return api.get(`/doc-ledgers/${id}`)
}

export function createDocLedger(data: DocLedgerItem) {
  return api.post('/doc-ledgers', data)
}

export function updateDocLedger(id: number, data: Partial<DocLedgerItem>) {
  return api.put(`/doc-ledgers/${id}`, data)
}

export function transitionStatus(id: number, targetStatus: string, remark?: string) {
  return api.put(`/doc-ledgers/${id}/status`, { targetStatus, remark })
}

export function getKanbanData(projectId: number, stageId?: number) {
  return api.get('/doc-ledgers/kanban', { params: { projectId, stageId } })
}

export function getDocLedgerLogs(id: number) {
  return api.get(`/doc-ledgers/${id}/logs`)
}

export function checkDocuments(projectId: number, fromStageId: number) {
  return api.post('/stage-transitions/check-documents', { projectId, fromStageId })
}

export function deleteDocLedger(id: number) {
  return api.delete(`/doc-ledgers/${id}`)
}

export function syncFromCatalog(projectId: number, stageId: number) {
  return api.post('/doc-ledgers/sync-from-catalog', null, { params: { projectId, stageId } })
}

export function syncFromChecklist(projectId: number, stageId: number) {
  return api.post(`/projects/${projectId}/stages/${stageId}/checklist/sync-to-ledger`)
}
