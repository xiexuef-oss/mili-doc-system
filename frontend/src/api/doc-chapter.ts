import api from './index'

export interface DocChapter {
  id?: number
  docLedgerId: number
  templateChapterId?: number
  parentId?: number
  chapterNumber?: string
  chapterTitle: string
  chapterLevel?: number
  orderNum?: number
  content?: string
  contentJson?: string
  fillStatus?: string
  fillPercentage?: number
  status?: string
  versionNo?: number
}

export function initFromTemplate(docLedgerId: number, templateId: number, operatorId: number) {
  return api.post('/doc-chapters/init', null, { params: { docLedgerId, templateId, operatorId } })
}

export function getChaptersByLedger(docLedgerId: number) {
  return api.get(`/doc-chapters/ledger/${docLedgerId}`)
}

export function getChapterTree(docLedgerId: number) {
  return api.get(`/doc-chapters/ledger/${docLedgerId}/tree`)
}

export function getCompletionSummary(docLedgerId: number) {
  return api.get(`/doc-chapters/ledger/${docLedgerId}/summary`)
}

export function getChapter(id: number) {
  return api.get(`/doc-chapters/${id}`)
}

export function updateChapterContent(id: number, content: string, contentJson?: string, operatorId?: number) {
  return api.put(`/doc-chapters/${id}/content`, { content, contentJson, operatorId: operatorId?.toString() })
}

export function updateFillStatus(id: number, fillStatus: string, fillPercentage?: number) {
  return api.put(`/doc-chapters/${id}/fill-status`, { fillStatus, fillPercentage })
}
