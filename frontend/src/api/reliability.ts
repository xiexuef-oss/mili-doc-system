import request from './index'

// ===== 可靠性文档生成 =====

/** 生成可靠性大纲 */
export function generateReliabilityOutline(projectId: number, stageId?: number) {
  return request.post('/reliability/outline/generate', null, {
    params: { projectId, stageId }
  })
}

/** SSE 串流生成可靠性大纲 */
export function generateReliabilityOutlineStream(projectId: number, stageId?: number): EventSource {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return new EventSource(
    `${base}/reliability/outline/generate-stream?projectId=${projectId}&stageId=${stageId || ''}`
  )
}

/** 生成降额设计报告 */
export function generateDeratingReport(projectId: number, stageId?: number) {
  return request.post('/reliability/derating/generate', null, {
    params: { projectId, stageId }
  })
}

/** 生成可靠性预计报告 */
export function generatePredictionReport(
  projectId: number, stageId: number | undefined,
  bomItems?: any[], environment?: string
) {
  return request.post('/reliability/prediction/generate', bomItems || [], {
    params: { projectId, stageId, environment: environment || 'G_FIX' }
  })
}

/** 预览预计结果（不存库） */
export function previewPrediction(bomItems: any[], environment?: string) {
  return request.post('/reliability/prediction/preview', bomItems, {
    params: { environment: environment || 'G_FIX' }
  })
}

/** 生成可靠性分配报告 */
export function generateAllocationReport(
  projectId: number, stageId: number | undefined, params: any
) {
  return request.post('/reliability/allocation/generate', params, {
    params: { projectId, stageId }
  })
}

/** 预览分配结果 */
export function previewAllocation(params: any) {
  return request.post('/reliability/allocation/preview', params)
}

// ===== 可靠性指标管理 =====

/** 获取项目可靠性指标 */
export function getRelRequirements(projectId: number) {
  return request.get('/reliability/requirement', { params: { projectId } })
}

/** 保存可靠性指标 */
export function saveRelRequirement(data: any) {
  return request.post('/reliability/requirement', data)
}
