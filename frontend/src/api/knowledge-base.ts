import api from './index'

export interface KnowledgeBaseItem {
  id?: number
  title: string
  content: string
  category: string
  tags: string
  fileObjectId: string
  fileName: string
  fileSize: number
  fileType: string
  status: string
  createdAt?: string
}

export function getKnowledgeBaseList(params?: any) {
  return api.get('/knowledge', { params })
}

export function getKnowledgeBase(id: number) {
  return api.get(`/knowledge/${id}`)
}

export function createKnowledgeBase(data: KnowledgeBaseItem) {
  return api.post('/knowledge', data)
}

export function updateKnowledgeBase(id: number, data: KnowledgeBaseItem) {
  return api.put(`/knowledge/${id}`, data)
}

export function deleteKnowledgeBase(id: number) {
  return api.delete(`/knowledge/${id}`)
}

export function uploadKnowledgeBaseFile(id: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return api.post(`/knowledge/${id}/upload`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getKnowledgeBaseDownloadUrl(id: number) {
  return api.get(`/knowledge/${id}/download-url`)
}

export function getKnowledgeBaseCategories() {
  return api.get('/knowledge/categories')
}

export function getKnowledgeBaseTags() {
  return api.get('/knowledge/tags')
}
