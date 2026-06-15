import api from './index'

export interface ProjectMemberItem {
  id?: number
  projectId: number
  userId: number
  roleInProject: string
  memberLine?: string      // TECHNICAL/ADMINISTRATIVE/QUALITY/CRAFT
  memberPosition?: string  // CHIEF_DESIGNER/CHIEF_COMMANDER/...
  supervisorId?: number    // 上级 member_id
  sortOrder?: number
  duties: string
  status: string
  // 联表查询时可能返回的扩展字段
  userName?: string
  userOrgName?: string
  userTitle?: string
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
