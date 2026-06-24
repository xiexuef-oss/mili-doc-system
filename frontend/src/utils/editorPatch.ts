// EditorPatch: fine-grained editor operations returned by AI
export type EditorPatch =
  | { type: 'replace_selection'; blockId: string; newContent: any }      // Replace selected content
  | { type: 'insert_at_cursor'; blockId: string; content: any }           // Insert at cursor position
  | { type: 'insert_after'; blockId: string; content: any[] }             // Insert blocks after a given block
  | { type: 'replace_block'; blockId: string; content: any }              // Replace entire block
  | { type: 'delete_block'; blockId: string }                             // Delete a block
  | { type: 'set_blocks'; blocks: any[] }                                 // Set entire document blocks (initial load)

// Selection context sent to AI for context-aware editing
export interface EditorSelectionContext {
  from: number          // absolute position start
  to: number            // absolute position end
  selectedText: string  // selected text content
  beforeText: string    // text before selection (up to 500 chars)
  afterText: string     // text after selection (up to 500 chars)
  blockId: string       // ID of the block containing selection
  blockType: string     // 'heading' | 'paragraph' | 'listItem' | etc.
  headingPath: string[] // breadcrumb: ['第一章', '1.1 背景']
}

// Extracted outline item from editor JSON
export interface OutlineItem {
  id: string
  text: string
  level: number  // 1-6
  pos: number    // absolute position in document
}

// Apply a patch to the TipTap editor
export function applyEditorPatch(editor: any, patch: EditorPatch) {
  if (!editor) return
  const { state, view } = editor
  const { doc, tr } = state

  switch (patch.type) {
    case 'replace_selection': {
      // Find the block by id and update its content
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === patch.blockId) {
          const contentNode = typeof patch.newContent === 'string'
            ? state.schema.text(patch.newContent)
            : state.schema.nodeFromJSON(patch.newContent)
          view.dispatch(tr.replaceWith(pos + 1, pos + node.nodeSize - 1, contentNode))
          return false
        }
        return true
      })
      break
    }
    case 'replace_block': {
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === patch.blockId) {
          const newNode = state.schema.nodeFromJSON(patch.content)
          view.dispatch(tr.replaceWith(pos, pos + node.nodeSize, newNode))
          return false
        }
        return true
      })
      break
    }
    case 'insert_after': {
      const blocks = (patch.content || []).map((c: any) => state.schema.nodeFromJSON(c))
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === patch.blockId) {
          view.dispatch(tr.insert(pos + node.nodeSize, blocks))
          return false
        }
        return true
      })
      break
    }
    case 'delete_block': {
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === patch.blockId) {
          view.dispatch(tr.delete(pos, pos + node.nodeSize))
          return false
        }
        return true
      })
      break
    }
    case 'set_blocks': {
      const nodes = (patch.blocks || []).map((c: any) => state.schema.nodeFromJSON(c))
      view.dispatch(tr.replaceWith(0, doc.content.size, nodes))
      break
    }
  }
}

// Extract headings from TipTap JSON for outline navigation
export function extractOutline(docJson: any): OutlineItem[] {
  if (!docJson?.content) return []
  const items: OutlineItem[] = []
  let pos = 0

  function walk(nodes: any[]) {
    for (const node of nodes) {
      if (node.type === 'heading') {
        const text = node.content?.map((c: any) => c.text || '').join('') || ''
        const id = node.attrs?.id || `h-${items.length}`
        items.push({ id, text, level: node.attrs?.level || 1, pos })
      }
      pos += (node.content?.length || 0) + 2 // rough position estimation
      if (node.content) walk(node.content)
    }
  }
  walk(docJson.content)
  return items
}

// Get TipTap JSON as plain text
export function getEditorText(docJson: any): string {
  if (!docJson?.content) return ''
  const parts: string[] = []
  function walk(nodes: any[]) {
    for (const node of nodes) {
      if (node.type === 'text') parts.push(node.text || '')
      if (node.content) walk(node.content)
    }
  }
  walk(docJson.content)
  return parts.join('\n')
}
