import api from './index'

export interface DocEditSessionItem {
  id?: number
  docFileId: number
  baseVersionId: number
  draftVersionId: number
  editorUserId: number
  sessionStatus: string
  openedAt: string
  submittedAt: string
}

export function getDocEditSessions(docFileId?: number) {
  return api.get(`/doc-edit-sessions/doc-file/${docFileId}`)
}

export function createDocEditSession(data: DocEditSessionItem) {
  return api.post('/doc-edit-sessions/open', data)
}

export function submitDocEditSession(id: number, data: DocEditSessionItem) {
  return api.put(`/doc-edit-sessions/${id}/submit`, data)
}

export function closeDocEditSession(id: number) {
  return api.put(`/doc-edit-sessions/${id}/close`)
}
