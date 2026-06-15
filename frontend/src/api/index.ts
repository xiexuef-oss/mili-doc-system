import axios from 'axios'
import { getToken, removeToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 300000  // 5min for AI generation
})

api.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code && data.code !== 'SUCCESS') {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return response
  },
  error => {
    // Skip error toast for cancelled requests
    if (error.code === 'ERR_CANCELED' || error.name === 'CanceledError') {
      return Promise.reject(error)
    }
    // Auth errors: redirect to login
    if (error.response?.status === 401 || error.response?.status === 403) {
      removeToken()
      window.location.href = '/login'
      return Promise.reject(error)
    }
    // Server errors: generic message
    if (error.response?.status === 500) {
      ElMessage.error('服务器内部错误，请稍后重试')
      return Promise.reject(error)
    }
    // Network errors: specific message
    if (error.code === 'ERR_NETWORK' || !error.response) {
      ElMessage.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default api
