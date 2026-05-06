import api from './index'

export interface ProjectMemberItem {
  id?: number
  projectId: number
  userId: number
  roleInProject: string
  duties: string
  status: string
}

export function getProjectMembers(projectId: number) {
  return api.get(`/project-members/project/${projectId}`)
}

export function createProjectMember(data: ProjectMemberItem) {
  return api.post('/project-members', data)
}

export function updateProjectMember(id: number, data: ProjectMemberItem) {
  return api.put(`/project-members/${id}`, data)
}

export function deleteProjectMember(id: number) {
  return api.delete(`/project-members/${id}`)
}
