import api from './index'

export interface KnowledgeCard {
  id?: number
  cardType?: string
  targetTable?: string
  targetId?: number
  title: string
  plainLanguage?: string
  gjbReference?: string
  tags?: string
  status?: string
}

export function getKnowledgeCards(cardType?: string, targetTable?: string, targetId?: number) {
  const params: Record<string, any> = {}
  if (cardType) params.cardType = cardType
  if (targetTable) { params.targetTable = targetTable; if (targetId) params.targetId = targetId }
  return api.get('/knowledge-cards', { params })
}

export function searchKnowledgeCards(keyword: string) {
  return api.get('/knowledge-cards/search', { params: { keyword } })
}

export function getCardsByTag(tag: string) {
  return api.get(`/knowledge-cards/tag/${tag}`)
}

export function createKnowledgeCard(data: KnowledgeCard) {
  return api.post('/knowledge-cards', data)
}

export function updateKnowledgeCard(id: number, data: KnowledgeCard) {
  return api.put(`/knowledge-cards/${id}`, data)
}

export function deleteKnowledgeCard(id: number) {
  return api.delete(`/knowledge-cards/${id}`)
}
