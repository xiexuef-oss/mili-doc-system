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
  return api.get(`/doc-locks/doc-file/${docFileId}`)
}

export function createDocEditLock(data: DocEditLockItem) {
  return api.post('/doc-locks/lock', data)
}

export function updateDocEditLock(id: number, data: DocEditLockItem) {
  return api.put(`/doc-locks/${id}/unlock`, data)
}

export function deleteDocEditLock(id: number) {
  return api.delete(`/doc-locks/${id}`)
}
