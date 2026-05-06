import api from './index'

export interface StageTransitionItem {
  id?: number
  projectId: number
  fromStageId: number
  toStageId: number
  checkStatus: string
  blockerItems: string
  checkResult: string
  checkedBy: number
  checkedAt: string
}

export function getStageTransitions(projectId: number) {
  return api.get(`/stage-transitions/project/${projectId}`)
}

export function createStageTransition(data: StageTransitionItem) {
  return api.post('/stage-transitions', data)
}

export function updateStageTransition(id: number, data: StageTransitionItem) {
  return api.put(`/stage-transitions/${id}`, data)
}

export function deleteStageTransition(id: number) {
  return api.delete(`/stage-transitions/${id}`)
}
