import api from './index'

export interface DocChangeImpactItem {
  id?: number
  changeEventId: number
  impactedDocFileId: number
  impactReason: string
  suggestAction: string
  responsibleUserId: number
  status: string
  closedAt: string
}

export function getDocChangeImpacts(changeEventId?: number) {
  return api.get('/doc-change-impacts', { params: { changeEventId } })
}

export function createDocChangeImpact(data: DocChangeImpactItem) {
  return api.post('/doc-change-impacts', data)
}

export function updateDocChangeImpact(id: number, data: DocChangeImpactItem) {
  return api.put(`/doc-change-impacts/${id}`, data)
}

export function deleteDocChangeImpact(id: number) {
  return api.delete(`/doc-change-impacts/${id}`)
}
