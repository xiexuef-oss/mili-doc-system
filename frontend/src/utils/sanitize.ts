import DOMPurify from 'dompurify'

/** Sanitizes AI/document-generated HTML before it's passed to v-html, to prevent stored XSS. */
export function sanitizeHtml(html: string | null | undefined): string {
  if (!html) return ''
  return DOMPurify.sanitize(html)
}
