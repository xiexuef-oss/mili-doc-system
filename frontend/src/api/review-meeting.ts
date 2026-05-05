import api from './index'

export interface ReviewMeetingItem {
  id?: number
  projectId: number
  stageId?: number
  meetingCode: string
  meetingName: string
  meetingType: string
  meetingDate: string
  meetingLocation: string
  hostUserId?: number
  attendeeUsers?: string
  expertGroup?: string
  status: string
}

export function getReviewMeetings(params: {
  pageNo?: number
  pageSize?: number
  projectId?: number
  stageId?: number
  status?: string
}) {
  return api.get('/review-meetings', { params })
}

export function getReviewMeeting(id: number) {
  return api.get(`/review-meetings/${id}`)
}

export function createReviewMeeting(data: ReviewMeetingItem) {
  return api.post('/review-meetings', data)
}

export function updateReviewMeeting(id: number, data: ReviewMeetingItem) {
  return api.put(`/review-meetings/${id}`, data)
}

export function updateMeetingStatus(id: number, status: string) {
  return api.put(`/review-meetings/${id}/status`, null, { params: { status } })
}

export function deleteReviewMeeting(id: number) {
  return api.delete(`/review-meetings/${id}`)
}
