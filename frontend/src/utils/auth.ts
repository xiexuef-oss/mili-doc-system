const TOKEN_KEY = 'military-doc-token'
const USER_KEY = 'military-doc-user'

export interface UserInfo {
  userId: number
  username: string
  realName: string
  token: string
  roles?: string[]
}

export function isAdmin(): boolean {
  const user = getUser()
  return user?.roles?.includes('ADMIN') ?? false
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getUser(): UserInfo | null {
  const user = localStorage.getItem(USER_KEY)
  return user ? JSON.parse(user) : null
}

export function setUser(user: UserInfo): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function isLoggedIn(): boolean {
  return !!getToken()
}
