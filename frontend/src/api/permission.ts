import api from './index'

export interface PermissionItem {
  id?: number
  permissionCode: string
  permissionName: string
  resourceType: string
  path: string
  parentId: number
  orderNum: number
}

export function getPermissionList() {
  return api.get('/permissions')
}

export function getPermissionTree() {
  return api.get('/permissions/tree')
}

export function getPermission(id: number) {
  return api.get(`/permissions/${id}`)
}

export function createPermission(data: PermissionItem) {
  return api.post('/permissions', data)
}

export function updatePermission(id: number, data: PermissionItem) {
  return api.put(`/permissions/${id}`, data)
}

export function deletePermission(id: number) {
  return api.delete(`/permissions/${id}`)
}

export function getRolePermissions(roleId: number) {
  return api.get(`/roles/${roleId}/permissions`)
}

export function setRolePermissions(roleId: number, permIds: number[]) {
  return api.put(`/roles/${roleId}/permissions`, permIds)
}
