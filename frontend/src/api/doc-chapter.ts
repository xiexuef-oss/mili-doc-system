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

// ========== AI Chapter Structure Generation ==========

export interface AiStructureRequest {
  projectId?: number
  docLedgerId?: number
  templateId?: number
  docType?: string
  additionalPrompt?: string
  optimize?: boolean
}

export interface AiChapterNode {
  chapterNumber: string
  chapterTitle: string
  chapterLevel: number
  orderNum: number
  isRequired?: boolean
  writingTips?: string
  contentSchema?: any
  children: AiChapterNode[]
}

export interface AiStructureResponse {
  chapters: AiChapterNode[]
  totalChapters: number
  summary: string
}

/** AI preview chapter structure (no persistence) */
export function previewChapterStructure(req: AiStructureRequest) {
  return api.post('/ai/chapter-structure/preview', req)
}

/** Apply AI-generated chapter structure to a doc ledger */
export function applyChapterStructure(docLedgerId: number, chapters: AiChapterNode[]) {
  return api.post('/ai/chapter-structure/apply', { docLedgerId, chapters })
}

/** AI optimize existing chapter structure */
export function optimizeChapterStructure(req: AiStructureRequest) {
  return api.post('/ai/chapter-structure/optimize', req)
}

// ========== Chapter Structure Validation ==========

export interface StructureIssue {
  level: string       // ERROR or WARNING
  chapterRef: string  // e.g. "3.2"
  description: string
}

export interface ChapterStructureValidation {
  valid: boolean
  totalChapters: number
  issuesFound: number
  issues: StructureIssue[]
  summary: string
}

/** Validate chapter structure for a document */
export function validateChapterStructure(docLedgerId: number) {
  return api.get(`/doc-chapters/ledger/${docLedgerId}/validate`)
}

/** AI edit a chapter (rewrite/expand/shorten/polish) */
export function aiEditChapter(chapterId: number, action: string, instruction?: string) {
  return api.post(`/doc-chapters/${chapterId}/ai-edit`, { action, instruction })
}

// ========== DDXML Block Operations ==========

export interface ContentBlock {
  id: number; type: string; content: string
}

/** Generate chapter content as DDXML blocks */
export function generateBlocks(chapterId: number, projectId: number) {
  return api.post(`/ddxml/chapters/${chapterId}/generate`, null, { params: { projectId } })
}

/** Execute a block-level edit command */
export function blockCommand(chapterId: number, command: string, params?: Record<string,any>) {
  return api.post(`/ddxml/chapters/${chapterId}/command`, { command, ...params })
}

/** Get blocks for a chapter */
export function getBlocks(chapterId: number) {
  return api.get(`/ddxml/chapters/${chapterId}/blocks`)
}

/** Auto-fix chapter structure numbering and hierarchy */
export function fixChapterStructure(docLedgerId: number) {
  return api.post(`/doc-chapters/ledger/${docLedgerId}/fix`)
}
