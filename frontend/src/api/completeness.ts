import api from './index'

export interface CompletenessCheckResult {
  id?: number
  projectId: number
  docLedgerId?: number
  stageId?: number
  checkType?: string
  totalItems: number
  passedItems: number
  warningItems: number
  errorItems: number
  score: number
  detailJson?: string
  checkedBy?: number
  checkedAt?: string
}

export interface CheckItem {
  severity: 'ERROR' | 'WARNING' | 'PASS'
  chapterTitle?: string
  chapterNumber?: string
  description: string
  standardRef?: string
  tip?: string
  missingFields?: number
}

export function checkDocument(projectId: number, docLedgerId: number, operatorId: number) {
  return api.post(`/completeness/check?projectId=${projectId}&docLedgerId=${docLedgerId}&operatorId=${operatorId}`)
}

export function getCheckHistory(docLedgerId: number) {
  return api.get(`/completeness/history/${docLedgerId}`)
}

export function getProjectSummary(projectId: number) {
  return api.get(`/completeness/project/${projectId}/summary`)
}
