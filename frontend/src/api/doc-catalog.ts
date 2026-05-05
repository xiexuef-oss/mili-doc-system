import api from './index'

export interface DocCatalogItem {
  id?: number
  projectId: number
  stageId?: number
  docCode: string
  docName: string
  docType: string
  requiredFlag: boolean
  meetingUsage?: string
  usageSource?: string
  usageAdjustReason?: string
  responsibleUserId?: number
  status: string
}

export function getDocCatalogs(params: {
  pageNo?: number
  pageSize?: number
  projectId?: number
  stageId?: number
}) {
  return api.get('/doc-catalogs', { params })
}

export function getDocCatalogsByProject(projectId: number) {
  return api.get(`/doc-catalogs/project/${projectId}`)
}

export function getDocCatalog(id: number) {
  return api.get(`/doc-catalogs/${id}`)
}

export function createDocCatalog(data: DocCatalogItem) {
  return api.post('/doc-catalogs', data)
}

export function updateDocCatalog(id: number, data: DocCatalogItem) {
  return api.put(`/doc-catalogs/${id}`, data)
}

export function deleteDocCatalog(id: number) {
  return api.delete(`/doc-catalogs/${id}`)
}
