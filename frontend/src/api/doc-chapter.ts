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

export function getCompletionSummaryBatch(docLedgerIds: number[]) {
  return api.post('/doc-chapters/ledger/summary/batch', { docLedgerIds })
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

// ========== Three-library fusion ==========

export interface ChapterWritingContext {
  docChapterId: number
  templateChapterId: number
  chapterNumber: string
  chapterTitle: string
  chapterLevel: number
  templateDescription: string
  writingTips: string
  sampleContent: string
  contentSchema: any
  applicableClauses: StandardClauseRef[]
  relevantCards: KnowledgeCardRef[]
  relevantFields: MasterDataFieldRef[]
}

export interface StandardClauseRef {
  clauseId: number
  standardCode: string
  standardName: string
  clauseNumber: string
  clauseTitle: string
  clauseContent: string
  linkType: string
}

export interface KnowledgeCardRef {
  cardId: number
  title: string
  plainLanguage: string
  gjbReference: string
  tags: string
}

export interface MasterDataFieldRef {
  masterDataPath: string
  fieldLabel: string
  required: boolean
  currentValue: any
  valueStatus: string
}

export function getChapterWritingContext(chapterId: number, projectId: number) {
  return api.get(`/doc-chapters/${chapterId}/writing-context`, { params: { projectId } })
}

export function generateChapter(chapterId: number, projectId: number) {
  return api.post(`/doc-chapters/${chapterId}/generate`, null, { params: { projectId } })
}

export function autoFillChapter(chapterId: number, projectId: number) {
  return api.post(`/doc-chapters/${chapterId}/auto-fill`, null, { params: { projectId } })
}

export function autoFillAll(docLedgerId: number, projectId: number) {
  return api.post('/doc-chapters/auto-fill-all', null, { params: { docLedgerId, projectId } })
}

export function extractMasterData(projectId: number) {
  return api.post('/doc-chapters/extract-master-data', null, { params: { projectId } })
}
