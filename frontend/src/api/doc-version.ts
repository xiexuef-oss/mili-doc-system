import api from './index'

export interface DocVersionItem {
  id?: number
  docFileId: number
  versionNo: string
  sourceType: string
  baseVersionId: number
  fileObjectId: number
  versionStatus: string
  optimisticVersion: number
  submitUserId: number
  submitTime: string
  changeSummary: string
}

export function getDocVersions(docFileId: number) {
  return api.get(`/doc-versions/doc-file/${docFileId}`)
}

export function getDocVersion(id: number) {
  return api.get(`/doc-versions/${id}`)
}

export function createDocVersion(data: DocVersionItem) {
  return api.post('/doc-versions', data)
}

export function updateDocVersion(id: number, data: DocVersionItem) {
  return api.put(`/doc-versions/${id}`, data)
}
