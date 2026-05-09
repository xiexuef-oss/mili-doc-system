import api from './index'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  roles: string[]
}

export function login(data: LoginRequest) {
  return api.post<{ data: LoginResponse }>('/auth/login', data)
}
