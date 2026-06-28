<template>
  <div class="canvas-editor">
    <div v-if="editor" class="ce-toolbar">
      <button
        v-for="item in toolbarItems"
        :key="item.name"
        class="ce-tb-btn"
        :class="{ active: item.isActive?.() }"
        :title="item.title"
        @click="item.action()"
      >
        {{ item.icon }}
      </button>
    </div>
    <editor-content :editor="editor" class="ce-content" />
  </div>
</template>

<script setup lang="ts">
import { watch, computed } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import { Table } from '@tiptap/extension-table'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import { TableRow } from '@tiptap/extension-table-row'
import { Underline } from '@tiptap/extension-underline'
import { Heading } from '@tiptap/extension-heading'
import { mergeAttributes } from '@tiptap/core'

const props = defineProps<{ contentJson: any }>()
const emit = defineEmits<{
  (e: 'update:contentJson', json: any): void
  (e: 'selectionChange', ctx: any): void
  (e: 'activeHeadingChange', id: string | null): void
}>()

// Custom heading extension that renders data-heading-id for outline tracking
const CustomHeading = Heading.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      id: {
        default: null,
        parseHTML: element => element.getAttribute('data-heading-id'),
        renderHTML: attributes => {
          if (!attributes.id) return {}
          return { 'data-heading-id': attributes.id }
        },
      },
    }
  },
})

const editor = useEditor({
  content: props.contentJson || { type: 'doc', content: [] },
  extensions: [
    StarterKit.configure({ heading: false, underline: false }),
    CustomHeading.configure({ levels: [1, 2, 3, 4] }),
    Placeholder.configure({ placeholder: '开始输入，或使用左侧AI助手生成内容...' }),
    Table.configure({ resizable: true }),
    TableRow,
    TableHeader,
    TableCell,
    Underline,
  ],
  onUpdate: ({ editor: ed }) => {
    emit('update:contentJson', ed.getJSON())
  },
  onSelectionUpdate: ({ editor: ed }) => {
    const { from, to, empty } = ed.state.selection
    if (empty) { emit('selectionChange', null); return }
    const doc = ed.state.doc
    const ctx = {
      from, to,
      selectedText: doc.textBetween(from, to, '\n'),
      beforeText: doc.textBetween(Math.max(0, from - 500), from, '\n'),
      afterText: doc.textBetween(to, Math.min(doc.content.size, to + 500), '\n'),
      blockId: '',
      blockType: '',
      headingPath: [] as string[]
    }
    doc.nodesBetween(from, to, (node: any) => {
      if (node.attrs?.id) { ctx.blockId = node.attrs.id; ctx.blockType = node.type.name }
      if (node.type.name === 'heading') ctx.headingPath.push(node.textContent)
    })
    emit('selectionChange', ctx)
    // Track active heading
    let activeId: string | null = null
    ed.state.doc.nodesBetween(0, from, (node: any) => {
      if (node.type.name === 'heading' && node.attrs?.id) activeId = node.attrs.id
      return true
    })
    emit('activeHeadingChange', activeId)
  },
})

// Guard to prevent watch re-applying changes that were just made by applyPatch
const toolbarItems = computed(() => {
  const ed = editor.value
  if (!ed) return []
  return [
    { name: 'undo', icon: '↩️', title: '撤销', action: () => ed.chain().focus().undo().run(), isActive: () => false },
    { name: 'redo', icon: '↪️', title: '重做', action: () => ed.chain().focus().redo().run(), isActive: () => false },
    { name: 'separator1', icon: '|', title: '', action: () => {}, isActive: () => false },
    { name: 'bold', icon: 'B', title: '加粗', action: () => ed.chain().focus().toggleBold().run(), isActive: () => ed.isActive('bold') },
    { name: 'italic', icon: 'I', title: '斜体', action: () => ed.chain().focus().toggleItalic().run(), isActive: () => ed.isActive('italic') },
    { name: 'underline', icon: 'U', title: '下划线', action: () => ed.chain().focus().toggleUnderline().run(), isActive: () => ed.isActive('underline') },
    { name: 'separator2', icon: '|', title: '', action: () => {}, isActive: () => false },
    { name: 'h1', icon: 'H1', title: '一级标题', action: () => ed.chain().focus().toggleHeading({ level: 1 }).run(), isActive: () => ed.isActive('heading', { level: 1 }) },
    { name: 'h2', icon: 'H2', title: '二级标题', action: () => ed.chain().focus().toggleHeading({ level: 2 }).run(), isActive: () => ed.isActive('heading', { level: 2 }) },
    { name: 'h3', icon: 'H3', title: '三级标题', action: () => ed.chain().focus().toggleHeading({ level: 3 }).run(), isActive: () => ed.isActive('heading', { level: 3 }) },
    { name: 'separator3', icon: '|', title: '', action: () => {}, isActive: () => false },
    { name: 'bulletList', icon: '•', title: '无序列表', action: () => ed.chain().focus().toggleBulletList().run(), isActive: () => ed.isActive('bulletList') },
    { name: 'orderedList', icon: '1.', title: '有序列表', action: () => ed.chain().focus().toggleOrderedList().run(), isActive: () => ed.isActive('orderedList') },
    { name: 'blockquote', icon: '"', title: '引用', action: () => ed.chain().focus().toggleBlockquote().run(), isActive: () => ed.isActive('blockquote') },
    { name: 'codeBlock', icon: '</>', title: '代码块', action: () => ed.chain().focus().toggleCodeBlock().run(), isActive: () => ed.isActive('codeBlock') },
    { name: 'separator4', icon: '|', title: '', action: () => {}, isActive: () => false },
    { name: 'table', icon: '▦', title: '插入表格', action: () => ed.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(), isActive: () => ed.isActive('table') },
  ]
})

let skipNextWatch = false

// Sync external contentJson changes into the editor
watch(() => props.contentJson, (newJson) => {
  if (skipNextWatch) {
    skipNextWatch = false
    return
  }
  const ed = editor.value
  if (!ed || !newJson) return
  const currentJson = ed.getJSON()
  if (JSON.stringify(currentJson) !== JSON.stringify(newJson)) {
    ed.commands.setContent(newJson)
  }
}, { deep: true })

function scrollToHeading(id: string) {
  const ed = editor.value
  if (!ed) return
  ed.state.doc.descendants((node: any, pos: number) => {
    if (node.attrs?.id === id) {
      // Select text inside the heading (skip node boundaries which are not inline)
      const textStart = pos + 1  // skip opening tag
      const textEnd = pos + (node.nodeSize || 1) - 1  // skip closing tag
      ed.chain()
        .setTextSelection(textStart <= textEnd ? { from: textStart, to: textEnd } : pos)
        .scrollIntoView()
        .run()
      // Flash highlight effect on the DOM element
      const el = ed.view.dom.querySelector(`[data-heading-id="${id}"]`)
      if (el) {
        el.classList.remove('heading-flash')
        void (el as HTMLElement).offsetWidth
        el.classList.add('heading-flash')
      }
      return false
    }
    return true
  })
}

function applyPatch(patch: any): boolean {
  const ed = editor.value
  if (!ed) return false
  console.log('[CanvasEditor] applyPatch:', patch.type, patch)

  const { state, view } = ed
  const { doc } = state

  let applied = false

  switch (patch.type) {
    case 'replace_block': {
      const targetId = patch.blockId || patch.id
      const newContent = patch.content || patch.newContent
      if (!targetId) break
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === targetId) {
          applied = true
          let newNode
          if (typeof newContent === 'string') {
            newNode = state.schema.nodeFromJSON({
              type: node.type.name,
              attrs: { ...node.attrs },
              content: [{ type: 'text', text: newContent }]
            })
          } else {
            newNode = state.schema.nodeFromJSON(newContent)
          }
          const tr = state.tr.replaceWith(pos, pos + node.nodeSize, newNode)
          view.dispatch(tr)
          return false
        }
        return true
      })
      break
    }

    case 'replace_selection': {
      const targetId = patch.blockId || patch.id
      const newContent = patch.newContent || patch.content
      if (!targetId) break
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === targetId) {
          applied = true
          let contentNode
          if (typeof newContent === 'string') {
            contentNode = state.schema.text(newContent)
          } else {
            contentNode = state.schema.nodeFromJSON(newContent)
          }
          const tr = state.tr.replaceWith(pos + 1, pos + node.nodeSize - 1, contentNode)
          view.dispatch(tr)
          return false
        }
        return true
      })
      break
    }

    case 'insert_after': {
      const targetId = patch.blockId || patch.id
      const content = patch.content
      if (!targetId || !content) break
      const nodes = Array.isArray(content)
        ? content.map((c: any) => state.schema.nodeFromJSON(c))
        : [state.schema.nodeFromJSON(content)]
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === targetId) {
          applied = true
          const tr = state.tr.insert(pos + node.nodeSize, nodes)
          view.dispatch(tr)
          return false
        }
        return true
      })
      break
    }

    case 'delete_block': {
      const targetId = patch.blockId || patch.id
      if (!targetId) break
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === targetId) {
          applied = true
          const tr = state.tr.delete(pos, pos + node.nodeSize)
          view.dispatch(tr)
          return false
        }
        return true
      })
      break
    }

    case 'insert_at_cursor': {
      const insertContent = patch.content || patch.newContent
      if (!insertContent) break
      const { from } = state.selection
      let nodes
      if (typeof insertContent === 'string') {
        nodes = [state.schema.nodeFromJSON({ type: 'paragraph', content: [{ type: 'text', text: insertContent }] })]
      } else if (Array.isArray(insertContent)) {
        nodes = insertContent.map((c: any) => state.schema.nodeFromJSON(c))
      } else {
        nodes = [state.schema.nodeFromJSON(insertContent)]
      }
      const tr = state.tr.insert(from, nodes)
      view.dispatch(tr)
      applied = true
      break
    }

    default:
      console.warn('[CanvasEditor] Unknown patch type:', patch.type)
  }

  if (applied) {
    // Prevent watch from re-applying the same change next tick
    skipNextWatch = true
  }
  return applied
}

function getContentJson() { return editor.value?.getJSON() || props.contentJson }

defineExpose({ scrollToHeading, applyPatch, getContentJson, editor })
</script>

<style scoped>
.canvas-editor { flex: 1; overflow-y: auto; min-height: 0; display: flex; flex-direction: column; }
.ce-toolbar { display: flex; align-items: center; gap: 2px; padding: 6px 12px; border-bottom: 1px solid var(--el-border-color-light); background: #fff; flex-shrink: 0; overflow-x: auto; }
.ce-tb-btn { min-width: 28px; height: 28px; padding: 0 6px; border: 1px solid transparent; border-radius: 4px; background: transparent; cursor: pointer; font-size: 13px; font-weight: 600; color: #606266; transition: all .15s; display: flex; align-items: center; justify-content: center; }
.ce-tb-btn:hover { background: #f2f6fc; border-color: #dcdfe6; }
.ce-tb-btn.active { background: #ecf5ff; color: #409eff; border-color: #b3d8ff; }
.ce-tb-btn[disabled] { opacity: .4; cursor: not-allowed; }
.ce-content { padding: 40px 60px; margin: 0; min-height: 100%; width: 100%; }
.ce-content :deep(.ProseMirror) { outline: none; min-height: 600px; font-size: 15px; line-height: 1.8; color: #303133; }
.ce-content :deep(.ProseMirror p.is-editor-empty:first-child::before) { content: attr(data-placeholder); color: #c0c4cc; float: left; pointer-events: none; height: 0; }
.ce-content :deep(h1) { font-size: 24px; font-weight: 700; margin: 24px 0 12px; border-bottom: 2px solid var(--el-color-primary); padding-bottom: 6px; }
.ce-content :deep(h2) { font-size: 20px; font-weight: 600; margin: 20px 0 10px; }
.ce-content :deep(h3) { font-size: 17px; font-weight: 600; margin: 16px 0 8px; }
.ce-content :deep(p) { margin: 8px 0; text-indent: 2em; }
.ce-content :deep(blockquote) { border-left: 3px solid #409eff; padding: 8px 16px; margin: 12px 0; background: #ecf5ff; }
.ce-content :deep(ul), .ce-content :deep(ol) { padding-left: 2em; }
.ce-content :deep(code) { background: #f0f2f5; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
.ce-content :deep(pre) { background: #282c34; color: #abb2bf; padding: 16px; border-radius: 6px; overflow-x: auto; }
.ce-content :deep(pre code) { background: none; padding: 0; }
/* Heading flash highlight when selected from outline */
.ce-content :deep(.heading-flash) {
  animation: heading-highlight 1.5s ease-out;
}
@keyframes heading-highlight {
  0% { background-color: rgba(64, 158, 255, 0.25); border-radius: 4px; box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15); }
  100% { background-color: transparent; box-shadow: none; }
}
</style>
