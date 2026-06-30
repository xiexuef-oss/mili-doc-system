import { sanitizeHtml } from './sanitize'

/** Shared markdown-to-HTML renderer. Replaces 3 identical/near-identical copies
 *  previously in DocKanbanBoard, ProjectAiAssistant, and ChatPage. */
export function renderMarkdown(text: string | null | undefined): string {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  html = html.replace(/^#### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
  html = html.replace(/\n\n+/g, '</p><p>')
  html = '<p>' + html + '</p>'
  html = html.replace(/\n/g, '<br>')
  html = html.replace(/<p><\/p>/g, '')
  return sanitizeHtml(html)
}

/** Lightweight markdown for chat messages (bold + line breaks only). */
export function renderChatMarkdown(text: string | null | undefined): string {
  if (!text) return ''
  let html = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\n/g, '<br>')
  return sanitizeHtml(html)
}
