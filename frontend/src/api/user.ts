import api from './index'

export interface UserItem {
  id?: number
  username: string
  password?: string
  realName: string
  email: string
  phone: string
  status: string
  orgName?: string   // 部门
  title?: string     // 职称/军衔
}

/** 用户搜索(不分页)，用于成员选择器 */
export function searchUsers(keyword?: string) {
  return api.get('/users', { params: { pageSize: 500, keyword: keyword || undefined } })
}

export function getUsers(params: {
  pageNo?: number
  pageSize?: number
  keyword?: string
}) {
  return api.get('/users', { params })
}

export function getUser(id: number) {
  return api.get(`/users/${id}`)
}

export function createUser(data: UserItem) {
  return api.post('/users', data)
}

export function updateUser(id: number, data: UserItem) {
  return api.put(`/users/${id}`, data)
}

export function deleteUser(id: number) {
  return api.delete(`/users/${id}`)
}

export function getUserRoles(userId: number) {
  return api.get(`/users/${userId}/roles`)
}

export function setUserRoles(userId: number, roleIds: number[]) {
  return api.put(`/users/${userId}/roles`, roleIds)
}
