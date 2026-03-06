const AUTH_TOKEN_KEY = 'authToken'
const USER_KEY = 'user'

export interface StoredUser {
  id: string
  username: string
  email: string
  displayName: string | null
  avatarUrl?: string | null
}

export function getStoredAuthToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export function getStoredUser(): StoredUser | null {
  const rawUser = localStorage.getItem(USER_KEY)

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as StoredUser
  } catch {
    clearStoredSession()
    return null
  }
}

export function persistAuthSession(token: string, user: StoredUser): void {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function persistAuthToken(token: string): void {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
}

export function clearStoredSession(): void {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
