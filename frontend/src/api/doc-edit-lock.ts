import api from './index'

export interface DocEditLockItem {
  id?: number
  docFileId: number
  lockedVersionId: number
  lockedBy: number
  lockType: string
  lockStatus: string
  expireAt: string
}

export function getDocEditLocks(docFileId?: number) {
  return api.get('/doc-edit-locks', { params: { docFileId } })
}

export function createDocEditLock(data: DocEditLockItem) {
  return api.post('/doc-edit-locks', data)
}

export function updateDocEditLock(id: number, data: DocEditLockItem) {
  return api.put(`/doc-edit-locks/${id}`, data)
}

export function deleteDocEditLock(id: number) {
  return api.delete(`/doc-edit-locks/${id}`)
}
