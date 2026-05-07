import api from './index'

export interface DocEffectiveBaselineItem {
  id?: number
  projectId: number
  stageId: number
  docFileId: number
  effectiveVersionId: number
  finalVersionId: number
  confirmedBy: number
  confirmedAt: string
  baselineStatus: string
}

export function getDocEffectiveBaselines(projectId?: number) {
  return api.get('/doc-baselines', { params: { projectId } })
}

export function createDocEffectiveBaseline(data: DocEffectiveBaselineItem) {
  return api.post('/doc-baselines', data)
}

export function updateDocEffectiveBaseline(id: number, data: DocEffectiveBaselineItem) {
  return api.put(`/doc-baselines/${id}`, data)
}

export function deleteDocEffectiveBaseline(id: number) {
  return api.delete(`/doc-baselines/${id}`)
}
