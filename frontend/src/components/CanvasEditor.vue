<template>
  <div class="canvas-editor">
    <editor-content :editor="editor" class="ce-content" />
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'

const props = defineProps<{ contentJson: any }>()
const emit = defineEmits<{
  (e: 'update:contentJson', json: any): void
  (e: 'selectionChange', ctx: any): void
  (e: 'activeHeadingChange', id: string | null): void
}>()

const editor = useEditor({
  content: props.contentJson || { type: 'doc', content: [] },
  extensions: [
    StarterKit.configure({ heading: { levels: [1, 2, 3, 4] } }),
    Placeholder.configure({ placeholder: '开始输入，或使用左侧AI助手生成内容...' }),
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

// Sync external contentJson changes into the editor
watch(() => props.contentJson, (newJson) => {
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
      ed.chain().setTextSelection(pos).scrollIntoView().run()
      return false
    }
    return true
  })
}

function applyPatch(patch: any) {
  const ed = editor.value
  if (!ed) return
  const { state, view } = ed
  const { doc } = state
  const tr = state.tr
  switch (patch.type) {
    case 'replace_block': {
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === patch.blockId) {
          const newNode = state.schema.nodeFromJSON(patch.content || { type: 'paragraph', content: [{ type: 'text', text: patch.newContent || '' }] })
          view.dispatch(tr.replaceWith(pos, pos + node.nodeSize, newNode))
          return false
        }
        return true
      })
      break
    }
    case 'insert_after': {
      const nodes = Array.isArray(patch.content) ? patch.content.map((c: any) => state.schema.nodeFromJSON(c)) : []
      doc.descendants((node: any, pos: number) => {
        if (node.attrs?.id === patch.blockId) {
          view.dispatch(tr.insert(pos + node.nodeSize, nodes))
          return false
        }
        return true
      })
      break
    }
  }
}

function getContentJson() { return editor.value?.getJSON() || props.contentJson }

defineExpose({ scrollToHeading, applyPatch, getContentJson, editor })
</script>

<style scoped>
.canvas-editor { flex: 1; overflow-y: auto; min-height: 0; }
.ce-content { padding: 40px 60px; max-width: 800px; margin: 0 auto; min-height: 100%; }
.ce-content :deep(.ProseMirror) { outline: none; min-height: 600px; font-size: 15px; line-height: 1.8; color: #303133; }
.ce-content :deep(.ProseMirror p.is-editor-empty:first-child::before) { content: attr(data-placeholder); color: #c0c4cc; float: left; pointer-events: none; height: 0; }
.ce-content :deep(h1) { font-size: 24px; font-weight: 700; margin: 24px 0 12px; border-bottom: 2px solid #409eff; padding-bottom: 6px; }
.ce-content :deep(h2) { font-size: 20px; font-weight: 600; margin: 20px 0 10px; }
.ce-content :deep(h3) { font-size: 17px; font-weight: 600; margin: 16px 0 8px; }
.ce-content :deep(p) { margin: 8px 0; text-indent: 2em; }
.ce-content :deep(blockquote) { border-left: 3px solid #409eff; padding: 8px 16px; margin: 12px 0; background: #ecf5ff; }
.ce-content :deep(ul), .ce-content :deep(ol) { padding-left: 2em; }
.ce-content :deep(code) { background: #f0f2f5; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
.ce-content :deep(pre) { background: #282c34; color: #abb2bf; padding: 16px; border-radius: 6px; overflow-x: auto; }
.ce-content :deep(pre code) { background: none; padding: 0; }
</style>
