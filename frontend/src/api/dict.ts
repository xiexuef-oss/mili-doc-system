import api from './index'

export interface DictItem {
  id?: number
  dictType: string
  dictCode: string
  dictName: string
  orderNum: number
  status: string
}

export function getDictTypes() {
  return api.get('/dicts/types')
}

export function getDictItems(dictType: string) {
  return api.get(`/dicts/items/${dictType}`)
}

export function getDicts(params: {
  pageNo?: number
  pageSize?: number
  dictType?: string
}) {
  return api.get('/dicts', { params })
}

export function createDict(data: DictItem) {
  return api.post('/dicts', data)
}

export function updateDict(id: number, data: DictItem) {
  return api.put(`/dicts/${id}`, data)
}

export function deleteDict(id: number) {
  return api.delete(`/dicts/${id}`)
}
