import api from './index'

export interface DocFileItem {
  id?: number
  catalogId?: number
  projectId: number
  stageId?: number
  docName: string
  docType: string
  securityLevel: string
  status: string
}

export function getDocFiles(params: {
  pageNo?: number
  pageSize?: number
  projectId?: number
  catalogId?: number
  status?: string
}) {
  return api.get('/doc-files', { params })
}

export function getDocFile(id: number) {
  return api.get(`/doc-files/${id}`)
}

export function createDocFile(data: DocFileItem) {
  return api.post('/doc-files', data)
}

export function updateDocFile(id: number, data: DocFileItem) {
  return api.put(`/doc-files/${id}`, data)
}

export function deleteDocFile(id: number) {
  return api.delete(`/doc-files/${id}`)
}
