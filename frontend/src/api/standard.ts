import api from './index'

export interface StandardItem {
  id?: number
  standardCode: string
  standardName: string
  standardType: string
  category: string
  version: string
  publishDate: string
  effectiveDate: string
  description: string
  fileObjectId: string
  fileName: string
  fileSize: number
  fileType: string
  status: string
}

export interface StandardClauseItem {
  id?: number
  standardId: number
  clauseNumber: string
  clauseTitle: string
  clauseContent: string
  parentId: number
  orderNum: number
  keywords: string
}

export function getStandardList(params?: any) {
  return api.get('/standards', { params })
}

export function getStandard(id: number) {
  return api.get(`/standards/${id}`)
}

export function createStandard(data: StandardItem) {
  return api.post('/standards', data)
}

export function updateStandard(id: number, data: StandardItem) {
  return api.put(`/standards/${id}`, data)
}

export function deleteStandard(id: number) {
  return api.delete(`/standards/${id}`)
}

export function parseStandardFile(file: File) {
  const form = new FormData()
  form.append('file', file)
  return api.post('/standards/parse', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadStandardFile(id: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return api.post(`/standards/${id}/upload`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function batchUploadStandardFiles(files: File[]) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  return api.post('/standards/batch-upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getStandardDownloadUrl(id: number) {
  return api.get(`/standards/${id}/download-url`)
}

export function getStandardTypes() {
  return api.get('/standards/types')
}

export function getStandardCategories() {
  return api.get('/standards/categories')
}

// Clauses
export function getStandardClauses(standardId: number) {
  return api.get(`/standards/${standardId}/clauses`)
}

export function createStandardClause(standardId: number, data: StandardClauseItem) {
  return api.post(`/standards/${standardId}/clauses`, data)
}

export function updateStandardClause(standardId: number, clauseId: number, data: StandardClauseItem) {
  return api.put(`/standards/${standardId}/clauses/${clauseId}`, data)
}

export function deleteStandardClause(standardId: number, clauseId: number) {
  return api.delete(`/standards/${standardId}/clauses/${clauseId}`)
}

export function searchStandardClauses(standardId: number, keyword: string) {
  return api.get(`/standards/${standardId}/clauses/search`, { params: { keyword } })
}
