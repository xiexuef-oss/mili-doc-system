import api from './index'

export function generateAllDocs(projectId: number, stageId: number) {
  return api.post(`/chat/generate-all/${projectId}/${stageId}`)
}

export function getQualityReport(projectId: number, stageId?: number) {
  return api.get(`/chat/quality-report/${projectId}`, { params: { stageId } })
}

export async function sendMessage(projectId: number, message: string, sessionId?: string) {
  const res = await api.post('/chat/message', { projectId, message, sessionId })
  return res.data.data as { taskId: string; status: string }
}

export async function pollTask(taskId: string) {
  const res = await api.get(`/chat/task/${taskId}`)
  return res.data.data as { status: string; progress: string; result?: any; error?: string }
}
