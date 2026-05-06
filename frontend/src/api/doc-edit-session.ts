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
  return api.get('/doc-edit-sessions', { params: { docFileId } })
}

export function createDocEditSession(data: DocEditSessionItem) {
  return api.post('/doc-edit-sessions', data)
}

export function updateDocEditSession(id: number, data: DocEditSessionItem) {
  return api.put(`/doc-edit-sessions/${id}`, data)
}
