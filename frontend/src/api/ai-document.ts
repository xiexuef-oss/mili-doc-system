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
  fetch(`/api/v1/ai-documents/${docId}/generate`, {
    method: 'POST',
    headers: {
      'Authorization': token ? `Bearer ${token}` : '',
      'Accept': 'text/event-stream'
    },
    signal: controller.signal
  }).then(async res => {
    if (!res.ok) { onDone(); return }
    const reader = res.body?.getReader()
    if (!reader) { onDone(); return }
    const decoder = new TextDecoder()
    let buf = '', eventType = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      const lines = buf.split('\n')
      buf = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.substring(6).trim()
        } else if (line.startsWith('data:')) {
          let raw = line.substring(5)
          const data = raw.startsWith(' ') ? raw.substring(1) : raw
          if (eventType === 'patch') {
            try { onPatch(JSON.parse(data)) } catch (e) { console.warn('SSE patch parse error:', e) }
          } else if (eventType === 'message') {
            onMessage(data)
          } else if (eventType === 'done') {
            onDone()
            return
          }
        }
      }
    }
    onDone()
  }).catch(() => { onDone() })
  return controller
}
