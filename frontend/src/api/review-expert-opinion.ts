import api from './index'

export interface ReviewExpertOpinionItem {
  id?: number
  meetingId: number
  expertUserId: number
  expertGroupName: string
  docFileId: number
  fileObjectId: number
  problemLevel: string
  uploadedAt: string
}

export function getReviewExpertOpinions(meetingId: number) {
  return api.get(`/review-expert-opinions/meeting/${meetingId}`)
}

export function createReviewExpertOpinion(data: ReviewExpertOpinionItem) {
  return api.post('/review-expert-opinions', data)
}

export function updateReviewExpertOpinion(id: number, data: ReviewExpertOpinionItem) {
  return api.put(`/review-expert-opinions/${id}`, data)
}

export function deleteReviewExpertOpinion(id: number) {
  return api.delete(`/review-expert-opinions/${id}`)
}
