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
  return api.get(`/review-expert-opinion-files/meeting/${meetingId}`)
}

export function createReviewExpertOpinion(data: ReviewExpertOpinionItem) {
  return api.post('/review-expert-opinion-files', data)
}

export function updateReviewExpertOpinion(id: number, data: ReviewExpertOpinionItem) {
  return api.put(`/review-expert-opinion-files/${id}`, data)
}

export function deleteReviewExpertOpinion(id: number) {
  return api.delete(`/review-expert-opinion-files/${id}`)
}
