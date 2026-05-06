import api from './index'

export interface ReviewMeetingDocumentItem {
  id?: number
  meetingId: number
  docFileId: number
  docVersionId: number
  reviewResult: string
  materialCompleteFlag: boolean
  closedFlag: boolean
}

export function getReviewMeetingDocuments(meetingId: number) {
  return api.get(`/review-meeting-documents/meeting/${meetingId}`)
}

export function createReviewMeetingDocument(data: ReviewMeetingDocumentItem) {
  return api.post('/review-meeting-documents', data)
}

export function updateReviewMeetingDocument(id: number, data: ReviewMeetingDocumentItem) {
  return api.put(`/review-meeting-documents/${id}`, data)
}

export function deleteReviewMeetingDocument(id: number) {
  return api.delete(`/review-meeting-documents/${id}`)
}
