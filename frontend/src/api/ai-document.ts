import api from './index'
import { getToken } from '@/utils/auth'

export function createDocument(params: { projectId?: number; prompt: string; documentType?: string }) {
  return api.post('/ai-documents', params)
}
export function getDocument(id: number) { return api.get(`/ai-documents/${id}`) }
export function listDocuments(projectId?: number) {
  return api.get('/ai-documents', { params: projectId ? { projectId } : {} })
}
export function updateSection(docId: number, sectionId: number, data: { content?: string; title?: string }) {
  return api.patch(`/ai-documents/${docId}/sections/${sectionId}`, data)
}
export function aiEditSection(docId: number, sectionId: number, instruction: string, mode: string) {
  return api.post(`/ai-documents/${docId}/sections/${sectionId}/ai-edit`, { instruction, mode })
}
export function chatWithDocument(docId: number, message: string) {
  return api.post(`/ai-documents/${docId}/chat`, { message })
}
export function addSection(docId: number, title: string, level: number, afterSectionId?: number, parentId?: number) {
  return api.post(`/ai-documents/${docId}/sections`, { title, level, afterSectionId, parentId })
}
export function deleteSection(docId: number, sectionId: number) {
  return api.delete(`/ai-documents/${docId}/sections/${sectionId}`)
}
export function moveSection(docId: number, sectionId: number, sortOrder: number, parentId?: number) {
  return api.patch(`/ai-documents/${docId}/sections/${sectionId}/move`, { sortOrder, parentId })
}
export function listVersions(docId: number) { return api.get(`/ai-documents/${docId}/versions`) }
export function restoreVersion(docId: number, versionId: number) {
  return api.post(`/ai-documents/${docId}/versions/${versionId}/restore`)
}
export function listOperations(docId: number) { return api.get(`/ai-documents/${docId}/operations`) }

export function updateDocumentContent(docId: number, contentJson: any) {
  return api.put(`/ai-documents/${docId}/content`, { contentJson: JSON.stringify(contentJson) })
}
export function updateDocumentTitle(docId: number, title: string) {
  return api.put(`/ai-documents/${docId}`, { title })
}
export function exportDocument(docId: number, format: string) {
  return api.get(`/ai-documents/${docId}/export`, { params: { format } })
}
export function getProjectDocChecklist(projectId: number) {
  return api.get(`/ai-documents/project/${projectId}/checklist`)
}
export function startDocumentFromCatalog(projectId: number, catalogId: number, docName?: string, docType?: string, ledgerId?: number) {
  return api.post('/ai-documents/from-catalog', { projectId, catalogId, docName, docType, ledgerId })
}
export function loadOutlineFromTemplate(docId: number, templateId: number) {
  return api.post(`/ai-documents/${docId}/load-template`, { templateId })
}
export function generateTemplateFromKnowledge(docId: number) {
  return api.post(`/ai-documents/${docId}/generate-template`)
}
export function confirmTemplate(docId: number, outline: any[]) {
  return api.post(`/ai-documents/${docId}/confirm-template`, { outline })
}
export function generateContent(docId: number) {
  return api.post(`/ai-documents/${docId}/generate-content`, {}, { responseType: 'stream' })
}
/** Start async content generation (non-SSE, reliable) */
export function generateContentAsync(docId: number) {
  return api.post(`/ai-documents/${docId}/generate-content-async`)
}
/** Poll generation progress */
export function getGenerationStatus(docId: number) {
  return api.get(`/ai-documents/${docId}/generation-status`)
}
export function linkToDocLedger(docId: number, catalogId: number) {
  return api.post(`/ai-documents/${docId}/link-ledger`, { catalogId })
}
export function saveToDocLedger(docId: number, ledgerId: number) {
  return api.post(`/ai-documents/${docId}/save-ledger`, { ledgerId })
}
export function aiEditSelection(docId: number, params: {
  blockId: string; selectedText: string; beforeText: string; afterText: string
  headingPath: string[]; mode: string; instruction?: string
}) {
  return api.post(`/ai-documents/${docId}/ai/edit-selection`, params)
}
export function aiInsertAtCursor(docId: number, params: { context: string; instruction: string }) {
  return api.post(`/ai-documents/${docId}/ai/insert-at-cursor`, params)
}

// SSE streaming for document generation
export function streamGenerateDocument(
  docId: number,
  onPatch: (p: any) => void,
  onMessage: (m: string) => void,
  onDone: () => void
): AbortController {
  const controller = new AbortController()
  const token = getToken()
  console.log('[SSE] Starting stream for doc', docId)
  fetch(`/api/v1/ai-documents/${docId}/generate`, {
    method: 'POST',
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Accept': 'text/event-stream'
    },
    signal: controller.signal
  }).then(async res => {
    console.log('[SSE] Response:', res.status, res.statusText)
    if (!res.ok) { console.error('[SSE] Bad response'); onDone(); return }
    const reader = res.body?.getReader()
    if (!reader) { console.error('[SSE] No reader'); onDone(); return }
    const decoder = new TextDecoder()
    let buf = '', eventType = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) { console.log('[SSE] Stream done'); break }
      const chunk = decoder.decode(value, { stream: true })
      buf += chunk
      const lines = buf.split('\n')
      buf = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          let raw = line.substring(5)
          const data = raw.startsWith(' ') ? raw.substring(1) : raw
          if (eventType === 'patch') {
            try {
              const patch = JSON.parse(data)
              console.log('[SSE] patch:', patch.type)
              onPatch(patch)
            } catch (e) { console.warn('[SSE] parse error:', e) }
          } else if (eventType === 'message') {
            console.log('[SSE] message:', data.substring(0, 60))
            onMessage(data)
          } else if (eventType === 'done') {
            console.log('[SSE] DONE event')
            onDone()
            return
          }
        }
      }
    }
    onDone()
  }).catch((e) => { console.error('[SSE] fetch error:', e); onDone() })
  return controller
}
