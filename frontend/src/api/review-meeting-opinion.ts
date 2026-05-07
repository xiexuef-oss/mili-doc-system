import api from './index'

export interface ReviewMeetingOpinionItem {
  id?: number
  meetingId: number
  docFileId: number
  opinionType: string
  fileObjectId: number
  status: string
  uploadedBy: number
  uploadedAt: string
}

export function getReviewMeetingOpinions(meetingId: number) {
  return api.get(`/review-meeting-opinions/meeting/${meetingId}`)
}

export function createReviewMeetingOpinion(data: ReviewMeetingOpinionItem) {
  return api.post('/review-meeting-opinions', data)
}

export function updateReviewMeetingOpinion(id: number, data: ReviewMeetingOpinionItem) {
  return api.put(`/review-meeting-opinions/${id}`, data)
}

export function deleteReviewMeetingOpinion(id: number) {
  return api.delete(`/review-meeting-opinions/${id}`)
}
