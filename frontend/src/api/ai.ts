import api from './index'
import { getToken } from '@/utils/auth'

export interface CatalogItem {
  docCode: string
  docName: string
  docType: string
  requiredFlag: boolean
}

export interface DocCatalog {
  id?: number
  projectId: number
  stageId?: number
  docCode: string
  docName: string
  docType: string
  requiredFlag: boolean
  status: string
}

export interface DocFileSummary {
  id: number
  docName: string
  docType: string
  status: string
}

export function generateCatalog(params: {
  projectId: number
  stageId?: number
  overwrite?: boolean
}) {
  return api.post('/ai/catalog/generate', params)
}

export function checkAiHealth() {
  return api.get('/ai/health')
}

export function getLlmProvider() {
  return api.get('/ai/provider')
}

export function switchLlmProvider(provider: string) {
  return api.put('/ai/provider', { provider })
}

export function saveDraft(params: {
  projectId: number
  catalogId?: number
  docLedgerId?: number
  stageId?: number
  docName: string
  docType: string
  securityLevel?: string
  content: string
}) {
  return api.post('/ai/draft/save', params)
}

export function streamDraft(
  projectId: number,
  catalogId: number | null,
  docLedgerId: number | null,
  onChunk: (text: string) => void,
  onDone: (fullText: string) => void,
  onError: (err: Error) => void
): AbortController {
  const controller = new AbortController()
  const token = getToken()
  let url = `/api/v1/ai/draft/stream?projectId=${projectId}`
  if (catalogId) url += `&catalogId=${catalogId}`
  if (docLedgerId) url += `&docLedgerId=${docLedgerId}`

  let fullText = ''

  fetch(url, {
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Accept': 'text/event-stream'
    },
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is not readable')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          // SSE format: "data: <payload>". Strip the "data:" (5 chars) plus exactly one space delimiter.
          const raw = line.substring(5)
          const data = raw.startsWith(' ') ? raw.substring(1) : raw
          if (eventType === 'chunk') {
            fullText += data
            onChunk(data)
          } else if (eventType === 'done') {
            onDone(fullText)
            return
          }
          eventType = ''
        }
      }
    }
    onDone(fullText)
  }).catch(err => {
    if (err.name !== 'AbortError') {
      onError(err)
    }
  })

  return controller
}

// ---- Phase 2: AI 校对、预评审、合规检查、意见汇总、转阶段评估 ----

export function proofread(docLedgerId: number) {
  return api.post(`/ai/proofread/${docLedgerId}`)
}

export function preReview(docLedgerId: number) {
  return api.post(`/ai/pre-review/${docLedgerId}`)
}

export function complianceCheck(projectId: number, baselineId: number) {
  return api.post('/ai/compliance/check', { projectId, baselineId })
}

export function opinionSummary(meetingId: number) {
  return api.post(`/ai/opinion-summary/${meetingId}`)
}

export function stageReadiness(projectId: number, stageId: number) {
  return api.post('/ai/stage-readiness', { projectId, stageId })
}

export function archiveAdvice(docLedgerId: number) {
  return api.post(`/ai/archive-advice/${docLedgerId}`)
}

export function changeImpactAnalysis(projectId: number, changeDescription: string, baselineId?: number) {
  return api.post('/ai/change-impact', { projectId, changeDescription, baselineId })
}

// ---- Phase 3: Training Data ----

export interface TrainingExampleItem {
  id?: number
  projectId: number
  docFileId?: number
  catalogId?: number
  prompt?: string
  completion?: string
  quality: string
  createdAt?: string
}

export function collectTraining(params: {
  docFileId: number
  projectId: number
  catalogId?: number
}) {
  return api.post('/ai/training/collect', params)
}

export function getTrainingExamples(params: {
  quality?: string
  page?: number
  size?: number
}) {
  return api.get('/ai/training/examples', { params })
}

export function approveTrainingExample(id: number) {
  return api.put(`/ai/training/examples/${id}/approve`)
}

export function rejectTrainingExample(id: number) {
  return api.put(`/ai/training/examples/${id}/reject`)
}

export function exportTraining(quality?: string) {
  return api.get('/ai/training/export', { params: { quality: quality || 'APPROVED' } })
}

// ---- Embedding / Vector Index Management ----

export function indexAllClauses() {
  return api.post('/ai/embedding/index-clauses')
}

export function indexAllKnowledge() {
  return api.post('/ai/embedding/index-knowledge')
}

export function getEmbeddingStats() {
  return api.get('/ai/embedding/stats')
}

export function getIndexTasks() {
  return api.get('/ai/embedding/tasks')
}

// ---- General AI Chat ----

export function streamChat(
  message: string,
  history: { role: string; content: string }[],
  onChunk: (text: string) => void,
  onDone: (fullText: string) => void,
  onError: (err: Error) => void
): AbortController {
  const controller = new AbortController()
  const token = getToken()
  const url = '/api/v1/ai/chat/stream'

  let fullText = ''

  fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify({ message, history }),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is not readable')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          // SSE format: "data: <payload>". Strip the "data:" (5 chars) plus exactly one space delimiter.
          const raw = line.substring(5)
          const data = raw.startsWith(' ') ? raw.substring(1) : raw
          if (eventType === 'chunk') {
            fullText += data
            onChunk(data)
          } else if (eventType === 'done') {
            onDone(fullText)
            return
          } else if (eventType === 'error') {
            onError(new Error(data))
            return
          }
          eventType = ''
        }
      }
    }
    onDone(fullText)
  }).catch(err => {
    if (err.name !== 'AbortError') {
      onError(err)
    }
  })

  return controller
}
