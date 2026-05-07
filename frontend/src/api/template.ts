import api from './index'

export interface TemplateItem {
  id?: number
  templateName: string
  templateCode: string
  templateType: string
  applicableProjectType: string
  description: string
  fileObjectId: string
  fileName: string
  fileSize: number
  fileType: string
  variables: string
  status: string
}

export function getTemplateList(params?: any) {
  return api.get('/templates', { params })
}

export function getTemplate(id: number) {
  return api.get(`/templates/${id}`)
}

export function createTemplate(data: TemplateItem) {
  return api.post('/templates', data)
}

export function updateTemplate(id: number, data: TemplateItem) {
  return api.put(`/templates/${id}`, data)
}

export function deleteTemplate(id: number) {
  return api.delete(`/templates/${id}`)
}

export function uploadTemplateFile(id: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return api.post(`/templates/${id}/upload`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function batchUploadTemplateFiles(files: File[]) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  return api.post('/templates/batch-upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function generateFromTemplate(templateId: number, projectId: number, variables: Record<string, string>) {
  return api.post(`/templates/${templateId}/generate`, { projectId, variables })
}

export function getTemplateDownloadUrl(id: number) {
  return api.get(`/templates/${id}/download-url`)
}

export function getTemplateTypes() {
  return api.get('/templates/types')
}
