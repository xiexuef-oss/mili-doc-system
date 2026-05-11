import api from './index'

// ---- 技术状态项 ----

export interface ConfigurationItemVO {
  id?: number
  projectId: number
  stageId?: number
  ciCode: string
  ciName: string
  ciType: string
  parentCiId?: number
  responsibleUserId?: number
  currentVersion?: string
  status: string
  isControlled: boolean
  isKeyItem: boolean
  description?: string
}

export function getConfigurationItems(projectId: number, stageId?: number) {
  return api.get(`/projects/${projectId}/configuration-items`, { params: stageId ? { stageId } : {} })
}

export function createConfigurationItem(projectId: number, data: ConfigurationItemVO) {
  return api.post(`/projects/${projectId}/configuration-items`, data)
}

export function updateConfigurationItem(id: number, data: ConfigurationItemVO) {
  return api.put(`/configuration-items/${id}`, data)
}

// ---- 基线管理 ----

export interface ConfigurationBaselineVO {
  id?: number
  projectId: number
  stageId: number
  baselineCode?: string
  baselineName: string
  baselineType: string
  baselineVersion?: string
  baselineStatus?: string
  approveUserId?: number
  approveTime?: string
  effectiveTime?: string
  description?: string
  createdAt?: string
}

export interface ConfigurationBaselineItemVO {
  id?: number
  baselineId: number
  itemType: string
  itemId: number
  itemVersionId?: number
  itemCode?: string
  itemName?: string
  itemVersion?: string
}

export function createBaseline(projectId: number, stageId: number, baselineType: string) {
  return api.post(`/projects/${projectId}/stages/${stageId}/baselines`, null, { params: { baselineType } })
}

export function getBaselines(projectId: number, stageId: number) {
  return api.get(`/projects/${projectId}/stages/${stageId}/baselines`)
}

export function getBaseline(id: number) {
  return api.get(`/baselines/${id}`)
}

export function getBaselineItems(baselineId: number) {
  return api.get(`/baselines/${baselineId}/items`)
}

export function approveBaseline(id: number) {
  return api.put(`/baselines/${id}/approve`)
}

export function setBaselineEffective(id: number) {
  return api.put(`/baselines/${id}/effective`)
}

// ---- 技术状态更改控制 ----

export interface ConfigurationChangeRequestVO {
  id?: number
  projectId: number
  stageId?: number
  changeCode?: string
  changeTitle: string
  changeType?: string
  changeLevel?: string
  changeReason?: string
  changeContent?: string
  impactAnalysis?: string
  applicantId?: number
  responsibleUserId?: number
  status?: string
  ccbMeetingId?: number
  approveResult?: string
  approveOpinion?: string
  approveTime?: string
  createdAt?: string
}

export function createChangeRequest(projectId: number, data: ConfigurationChangeRequestVO) {
  return api.post(`/projects/${projectId}/change-requests`, data)
}

export function getChangeRequests(projectId: number, stageId?: number) {
  return api.get(`/projects/${projectId}/change-requests`, { params: stageId ? { stageId } : {} })
}

export function processChangeRequest(id: number, action: string) {
  return api.put(`/change-requests/${id}/process`, null, { params: { action } })
}

// ---- 技术状态记实 ----

export interface ConfigurationStatusAccountingVO {
  id?: number
  projectId: number
  stageId?: number
  ciId?: number
  docLedgerId?: number
  docVersionId?: number
  eventType: string
  eventName?: string
  eventDescription?: string
  relatedObjectType?: string
  relatedObjectId?: number
  eventTime?: string
  operatorId?: number
}

export function getStatusAccounting(projectId: number, stageId?: number) {
  return api.get(`/projects/${projectId}/status-accounting`, { params: stageId ? { stageId } : {} })
}

// ---- 技术状态审核 ----

export interface ConfigurationAuditVO {
  id?: number
  projectId: number
  stageId?: number
  baselineId?: number
  auditCode?: string
  auditName: string
  auditType: string
  auditStatus?: string
  meetingId?: number
  auditResult?: string
  auditOpinion?: string
  auditTime?: string
  createdAt?: string
}

export function createAudit(projectId: number, stageId: number, auditType: string) {
  return api.post(`/projects/${projectId}/stages/${stageId}/audits`, null, { params: { auditType } })
}

export function getAudits(projectId: number, stageId?: number) {
  return api.get(`/projects/${projectId}/audits`, { params: stageId ? { stageId } : {} })
}

export function completeAudit(id: number, auditResult: string, auditOpinion?: string) {
  return api.put(`/audits/${id}/complete`, null, { params: { auditResult, auditOpinion } })
}
