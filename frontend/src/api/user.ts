import api from './index'

export interface UserItem {
  id?: number
  username: string
  password?: string
  realName: string
  email: string
  phone: string
  status: string
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
