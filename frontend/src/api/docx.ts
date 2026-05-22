import api from './index'

export function generateDocx(docLedgerId: number, includeCover = true, showHighlights = true) {
  return api.post(`/docx/generate/${docLedgerId}?includeCover=${includeCover}&showHighlights=${showHighlights}`, null, { responseType: 'blob' })
}

export function generateAndUpload(docLedgerId: number, includeCover = true, showHighlights = true) {
  return api.post(`/docx/generate/${docLedgerId}/upload?includeCover=${includeCover}&showHighlights=${showHighlights}`)
}

export function parseDocx(fileObjectId: string) {
  return api.post(`/docx/parse/${fileObjectId}`)
}

export function parseAndUpdateChapters(fileObjectId: string, docLedgerId: number, operatorId: number) {
  return api.post(`/docx/parse/${fileObjectId}/update-chapters?docLedgerId=${docLedgerId}&operatorId=${operatorId}`)
}
