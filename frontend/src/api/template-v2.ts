import api from './index'

export interface DocTemplateCategory {
  id?: number
  categoryCode: string
  categoryName: string
  parentId?: number
  gjbReference?: string
  description?: string
  orderNum?: number
  status?: string
}

export interface DocTemplateV2 {
  id?: number
  categoryId: number
  templateCode: string
  templateName: string
  templateType?: string
  applicableStageCodes?: string
  applicableProjectType?: string
  gjbStandardRef?: string
  documentClass?: string
  variablesSchema?: string
  status?: string
  versionNo?: number
}

export interface DocTemplateChapter {
  id?: number
  templateId?: number
  parentId?: number
  chapterNumber?: string
  chapterTitle: string
  chapterLevel?: number
  orderNum?: number
  isRequired?: boolean
  applicabilityRule?: string
  contentSchema?: string
  standardClauseRef?: string
  description?: string
  writingTips?: string
  sampleContent?: string
  isReusableElement?: boolean
  // V5: auto-parsed fields
  headingStyle?: string
  numberingFormat?: string
  hasTable?: boolean
  tableJson?: string
  variablePlaceholders?: string
  fontEmphasis?: string
}

export interface DocxParseResult {
  templateId: number
  title: string
  securityLevel: string
  docCodeFormat: string
  hasCover: boolean
  chaptersCreated: number
  variablesDetected: number
  tablesDetected: number
  totalChars: number
  variableList: Array<{ placeholder: string; type: string }>
}

export interface DocTemplateElement {
  id?: number
  elementCode: string
  elementName: string
  elementType?: string
  elementCategory?: string
  contentJson?: string
  standardRefs?: string
  keywords?: string
  status?: string
}

// Categories
export function getCategories() { return api.get('/template-structure/categories') }
export function getCategoryTree() { return api.get('/template-structure/categories/tree') }

// Templates V2
export function getTemplates(categoryId?: number) {
  return api.get('/template-structure/templates', { params: categoryId ? { categoryId } : {} })
}
export function getTemplate(id: number) { return api.get(`/template-structure/templates/${id}`) }
export function createTemplate(data: DocTemplateV2) { return api.post('/template-structure/templates', data) }
export function updateTemplate(id: number, data: DocTemplateV2) { return api.put(`/template-structure/templates/${id}`, data) }
export function deleteTemplate(id: number) { return api.delete(`/template-structure/templates/${id}`) }

// Chapters
export function getChapters(templateId: number) { return api.get(`/template-structure/templates/${templateId}/chapters`) }
export function getRequiredChapters(templateId: number) { return api.get(`/template-structure/templates/${templateId}/chapters/required`) }
export function createChapter(templateId: number, data: DocTemplateChapter) { return api.post(`/template-structure/templates/${templateId}/chapters`, data) }
export function updateChapter(id: number, data: DocTemplateChapter) { return api.put(`/template-structure/chapters/${id}`, data) }
export function deleteChapter(id: number) { return api.delete(`/template-structure/chapters/${id}`) }
export function reorderChapters(templateId: number, chapterIds: number[]) { return api.put(`/template-structure/templates/${templateId}/chapters/reorder`, chapterIds) }

// Elements
export function getElements(category?: string) { return api.get('/template-structure/elements', { params: category ? { category } : {} }) }
export function createElement(data: DocTemplateElement) { return api.post('/template-structure/elements', data) }
export function attachElement(chapterId: number, elementId: number, required?: boolean, orderNum?: number) {
  return api.post(`/template-structure/chapters/${chapterId}/elements/${elementId}`, null, { params: { required, orderNum } })
}
export function detachElement(chapterId: number, elementId: number) { return api.delete(`/template-structure/chapters/${chapterId}/elements/${elementId}`) }
export function getChapterElements(chapterId: number) { return api.get(`/template-structure/chapters/${chapterId}/elements`) }

// DOCX upload and auto-parse
export function uploadAndParseDocx(templateId: number, file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return api.post(`/template-structure/templates/${templateId}/upload-docx`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
