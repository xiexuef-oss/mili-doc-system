import api from './index'

export interface ProjectItem {
  id?: number
  projectCode: string
  projectName: string
  projectType: string
  securityLevel: string
  status: string
  ownerUserId: string
  applicableStandards: string
  startDate: string
  endDate: string
  description: string
}

export function getProjects(params: { pageNo?: number; pageSize?: number; keyword?: string; status?: string }) {
  return api.get('/projects', { params })
}

export function getProject(id: number) {
  return api.get(`/projects/${id}`)
}

export function createProject(data: ProjectItem) {
  return api.post('/projects', data)
}

export function updateProject(id: number, data: ProjectItem) {
  return api.put(`/projects/${id}`, data)
}
