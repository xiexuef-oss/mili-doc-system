import api from './index'

export interface RoleItem {
  id?: number
  roleCode: string
  roleName: string
  description: string
  orderNum: number
  status: string
}

export function getRoles(params: {
  pageNo?: number
  pageSize?: number
  keyword?: string
}) {
  return api.get('/roles', { params })
}

export function getRole(id: number) {
  return api.get(`/roles/${id}`)
}

export function createRole(data: RoleItem) {
  return api.post('/roles', data)
}

export function updateRole(id: number, data: RoleItem) {
  return api.put(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return api.delete(`/roles/${id}`)
}
