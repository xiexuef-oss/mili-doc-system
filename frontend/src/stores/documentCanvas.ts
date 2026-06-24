import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as aiDocApi from '@/api/ai-document'
import { extractOutline, getEditorText, type OutlineItem } from '@/utils/editorPatch'

export interface AiDocument {
  id: number; projectId?: number; userId: number; title: string
  description?: string; documentType?: string; sourcePrompt?: string; status: string
}

export const useDocumentCanvasStore = defineStore('documentCanvas', () => {
  const document = ref<AiDocument | null>(null)
  const contentJson = ref<any>(null)
  const outline = ref<OutlineItem[]>([])
  const recentDocuments = ref<AiDocument[]>([])
  const activeHeadingId = ref<string | null>(null)
  const loading = ref(false)
  const generating = ref(false)
  const saveStatus = ref<'saved'|'saving'|'unsaved'|'error'>('saved')

  let saveTimer: ReturnType<typeof setTimeout> | null = null

  async function loadDocument(id: number) {
    loading.value = true
    try {
      const res = await aiDocApi.getDocument(id)
      document.value = res.data?.data?.document || null
      const cj = res.data?.data?.contentJson
      contentJson.value = (cj && typeof cj === 'string') ? JSON.parse(cj) : (cj || { type: 'doc', content: [] })
      outline.value = extractOutline(contentJson.value)
    } finally { loading.value = false }
  }

  async function createDocument(projectId: number, prompt: string, docType: string) {
    const res = await aiDocApi.createDocument({ projectId, prompt, documentType: docType })
    document.value = res.data?.data?.document || null
    contentJson.value = { type: 'doc', content: [] }
    outline.value = []
    return document.value
  }

  async function loadRecentDocuments(projectId?: number) {
    try { const res = await aiDocApi.listDocuments(projectId); recentDocuments.value = res.data?.data || [] }
    catch { /* ignore */ }
  }

  function reset() {
    document.value = null; contentJson.value = null; outline.value = []
    activeHeadingId.value = null; generating.value = false
  }

  function handleContentUpdate(json: any) {
    contentJson.value = json
    outline.value = extractOutline(json)
    saveStatus.value = 'unsaved'
    if (saveTimer) clearTimeout(saveTimer)
    saveTimer = setTimeout(() => saveToServer(), 2000)
  }

  async function saveToServer() {
    if (!document.value?.id || !contentJson.value) return
    saveStatus.value = 'saving'
    try {
      await aiDocApi.updateDocumentContent(document.value.id, contentJson.value)
      saveStatus.value = 'saved'
    } catch { saveStatus.value = 'error' }
  }

  function applyCanvasPatches(patches: any[]) {
    if (!patches || !Array.isArray(patches)) return
    for (const p of patches) {
      if (!p?.type) continue
      const payload = p.payload || {}
      switch (p.type) {
        case 'set_document_title': if (document.value) document.value.title = payload.title; break
        case 'set_document_status': if (document.value) document.value.status = payload.status; break
        case 'update_section_content': case 'append_section_content': {
          if (!contentJson.value) contentJson.value = { type: 'doc', content: [] }
          const blocks = (payload.content || payload.delta || '').split('\n\n').filter(Boolean)
          for (const block of blocks) {
            contentJson.value.content.push({ type: 'paragraph', content: [{ type: 'text', text: block }] })
          }
          outline.value = extractOutline(contentJson.value)
          break
        }
      }
    }
  }

  const editorText = computed(() => getEditorText(contentJson.value))

  return {
    document, contentJson, outline, recentDocuments, activeHeadingId,
    loading, generating, saveStatus, editorText,
    loadDocument, createDocument, loadRecentDocuments, reset,
    handleContentUpdate, saveToServer, applyCanvasPatches,
  }
})
