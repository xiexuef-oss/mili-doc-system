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

  function applyCanvasPatches(patches: any[]): { storeApplied: number; editorPatches: any[] } {
    const editorPatches: any[] = []
    let storeApplied = 0

    if (!patches || !Array.isArray(patches)) {
      console.warn('[Store] applyCanvasPatches received invalid patches:', patches)
      return { storeApplied: 0, editorPatches: [] }
    }

    for (const p of patches) {
      if (!p?.type) continue

      // 统一patch格式：支持 { type, payload } 和 { type, blockId, newContent } 两种格式
      const payload = p.payload || p

      try {
        switch (p.type) {
          case 'set_document_title': {
            if (document.value) document.value.title = payload.title || payload
            storeApplied++
            break
          }
          case 'set_document_status': {
            if (document.value) document.value.status = payload.status || payload
            storeApplied++
            break
          }
          case 'set_outline': {
            const sections = payload.sections || payload
            if (Array.isArray(sections)) {
              const blocks = sections.map((sec: any) => ({
                type: 'heading',
                attrs: { level: sec.level || 1, id: sec.id ? `sec-${sec.id}` : `h-${Date.now()}-${Math.random().toString(36).slice(2, 6)}` },
                content: [{ type: 'text', text: sec.title || '' }]
              }))
              contentJson.value = { type: 'doc', content: blocks }
              outline.value = extractOutline(contentJson.value)
            }
            storeApplied++
            break
          }
          case 'add_section': {
            const sec = payload.section || payload
            if (!contentJson.value) contentJson.value = { type: 'doc', content: [] }
            const headingId = sec.id ? `sec-${sec.id}` : `h-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
            contentJson.value.content.push({
              type: 'heading',
              attrs: { level: sec.level || 1, id: headingId },
              content: [{ type: 'text', text: sec.title || '' }]
            })
            if (sec.content) {
              for (const para of sec.content.split('\n\n').filter(Boolean)) {
                contentJson.value.content.push({
                  type: 'paragraph',
                  content: [{ type: 'text', text: para.trim() }]
                })
              }
            }
            outline.value = extractOutline(contentJson.value)
            storeApplied++
            break
          }
          case 'delete_section': {
            const sid = payload.sectionId || payload.id
            if (sid && contentJson.value?.content) {
              const headingId = typeof sid === 'string' && sid.startsWith('sec-') ? sid : `sec-${sid}`
              const idx = contentJson.value.content.findIndex((node: any) =>
                node.type === 'heading' && node.attrs?.id === headingId
              )
              if (idx >= 0) {
                // 删除该heading及其后续同层级或更低层级的节点，直到遇到同级或更高级heading
                const level = contentJson.value.content[idx].attrs?.level || 1
                let endIdx = idx + 1
                while (endIdx < contentJson.value.content.length) {
                  const node = contentJson.value.content[endIdx]
                  if (node.type === 'heading' && (node.attrs?.level || 1) <= level) break
                  endIdx++
                }
                contentJson.value.content.splice(idx, endIdx - idx)
                outline.value = extractOutline(contentJson.value)
              }
            }
            storeApplied++
            break
          }
          case 'rename_section': {
            const sid = payload.sectionId || payload.id
            const title = payload.title
            if (sid && title && contentJson.value?.content) {
              const headingId = typeof sid === 'string' && sid.startsWith('sec-') ? sid : `sec-${sid}`
              const node = contentJson.value.content.find((n: any) =>
                n.type === 'heading' && n.attrs?.id === headingId
              )
              if (node) {
                node.content = [{ type: 'text', text: title }]
                outline.value = extractOutline(contentJson.value)
              }
            }
            storeApplied++
            break
          }
          case 'update_section_content':
          case 'append_section_content': {
            const sid = payload.sectionId
            const content = payload.content || payload.delta || ''
            if (!contentJson.value) contentJson.value = { type: 'doc', content: [] }
            if (content && typeof content === 'string') {
              if (sid) {
                // 尝试定位到指定section并在其后追加内容
                const headingId = `sec-${sid}`
                const idx = contentJson.value.content.findIndex((node: any) =>
                  node.type === 'heading' && node.attrs?.id === headingId
                )
                if (idx >= 0) {
                  const level = contentJson.value.content[idx].attrs?.level || 1
                  let insertIdx = idx + 1
                  while (insertIdx < contentJson.value.content.length) {
                    const node = contentJson.value.content[insertIdx]
                    if (node.type === 'heading' && (node.attrs?.level || 1) <= level) break
                    insertIdx++
                  }
                  const newParagraphs = content.split('\n\n').filter(Boolean).map((text: string) => ({
                    type: 'paragraph',
                    content: [{ type: 'text', text: text.trim() }]
                  }))
                  contentJson.value.content.splice(insertIdx, 0, ...newParagraphs)
                } else {
                  // 未找到对应heading，追加到末尾
                  for (const para of content.split('\n\n').filter(Boolean)) {
                    contentJson.value.content.push({
                      type: 'paragraph',
                      content: [{ type: 'text', text: para.trim() }]
                    })
                  }
                }
              } else {
                // 无sectionId，直接追加到末尾
                for (const para of content.split('\n\n').filter(Boolean)) {
                  contentJson.value.content.push({
                    type: 'paragraph',
                    content: [{ type: 'text', text: para.trim() }]
                  })
                }
              }
            }
            outline.value = extractOutline(contentJson.value)
            storeApplied++
            break
          }
          case 'set_blocks': {
            const blocks = payload.blocks || payload
            if (Array.isArray(blocks)) {
              contentJson.value = { type: 'doc', content: blocks }
              outline.value = extractOutline(contentJson.value)
            }
            storeApplied++
            break
          }
          // Editor-level patches: 转发给编辑器组件处理
          case 'replace_block':
          case 'replace_selection':
          case 'insert_after':
          case 'delete_block':
          case 'insert_at_cursor': {
            editorPatches.push({ type: p.type, ...payload })
            break
          }
          default: {
            console.warn('[Store] Unknown patch type:', p.type)
            // 未知类型也尝试给editor处理
            editorPatches.push({ type: p.type, ...payload })
          }
        }
      } catch (e) {
        console.error('[Store] Failed to apply patch:', p.type, e)
      }
    }

    return { storeApplied, editorPatches }
  }

  const editorText = computed(() => getEditorText(contentJson.value))

  return {
    document, contentJson, outline, recentDocuments, activeHeadingId,
    loading, generating, saveStatus, editorText,
    loadDocument, createDocument, loadRecentDocuments, reset,
    handleContentUpdate, saveToServer, applyCanvasPatches,
  }
})
