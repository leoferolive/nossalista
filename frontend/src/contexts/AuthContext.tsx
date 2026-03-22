import React, { createContext, useContext, useState, useCallback, useEffect } from 'react'
import client from '../api/client'
import {
  clearStoredSession,
  getStoredAuthToken,
  getStoredUser,
  persistAuthSession,
  StoredUser,
} from '../auth/session'
import { usersApi } from '../api/usersApi'

interface CurrentUserResponse {
  id: string
  username: string
  email: string
  name: string | null
  avatarUrl?: string | null
  onboardingCompletedAt?: string | null
}

type User = StoredUser

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isBootstrapping: boolean
  login: (token: string, user: User) => void
  markOnboardingCompleted: (completedAt?: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => getStoredUser())
  const [token, setToken] = useState<string | null>(() => getStoredAuthToken())
  const [isBootstrapping, setIsBootstrapping] = useState(true)

  useEffect(() => {
    const bootstrapSession = async () => {
      const storedToken = getStoredAuthToken()
      const savedUser = getStoredUser()

      if (!storedToken) {
        if (savedUser) {
          clearStoredSession()
          setUser(null)
        }
        setToken(null)
        setIsBootstrapping(false)
        return
      }

      try {
        const { data } = await client.get<CurrentUserResponse>('/api/users/me')
        const normalizedUser: User = {
          id: data.id,
          username: data.username,
          email: data.email,
          displayName: data.name,
          avatarUrl: data.avatarUrl ?? null,
          onboardingCompletedAt: data.onboardingCompletedAt ?? null,
        }
        persistAuthSession(storedToken, normalizedUser)
        setToken(storedToken)
        setUser(normalizedUser)
      } catch {
        clearStoredSession()
        setToken(null)
        setUser(null)
      } finally {
        setIsBootstrapping(false)
      }
    }

    void bootstrapSession()
  }, [])

  const isAuthenticated = !!user && !!token

  const login = useCallback((newToken: string, userData: User) => {
    persistAuthSession(newToken, userData)
    setToken(newToken)
    setUser(userData)
    setIsBootstrapping(false)
  }, [])

  const markOnboardingCompleted = useCallback((completedAt?: string) => {
    setUser((prev) => {
      if (!prev) {
        return prev
      }

      const nextUser: User = {
        ...prev,
        onboardingCompletedAt: completedAt ?? new Date().toISOString(),
      }

      const token = getStoredAuthToken()
      if (token) {
        persistAuthSession(token, nextUser)
      }

      return nextUser
    })
  }, [])

  const logout = useCallback(() => {
    // Fire-and-forget: call backend logout but don't block on errors
    usersApi.logout().catch(() => {})
    clearStoredSession()
    setToken(null)
    setUser(null)
    setIsBootstrapping(false)
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated, isBootstrapping, login, markOnboardingCompleted, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
