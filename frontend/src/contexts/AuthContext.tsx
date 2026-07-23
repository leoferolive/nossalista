import React, { createContext, useCallback, useContext, useEffect, useState } from 'react'
import client from '../api/client'
import { clearLegacyAuthStorage } from '../auth/session'
import { usersApi } from '../api/usersApi'

interface CurrentUserResponse {
  id: string
  username: string
  email: string
  name: string | null
  avatarUrl?: string | null
  onboardingCompletedAt?: string | null
}

export interface AuthUser {
  id: string
  username: string
  email: string
  displayName: string | null
  avatarUrl?: string | null
  onboardingCompletedAt: string | null
}

interface AuthContextType {
  user: AuthUser | null
  isAuthenticated: boolean
  isBootstrapping: boolean
  login: (user: AuthUser) => void
  markOnboardingCompleted: (completedAt?: string) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

function toAuthUser(data: CurrentUserResponse): AuthUser {
  return {
    id: data.id,
    username: data.username,
    email: data.email,
    displayName: data.name,
    avatarUrl: data.avatarUrl ?? null,
    onboardingCompletedAt: data.onboardingCompletedAt ?? null,
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isBootstrapping, setIsBootstrapping] = useState(true)

  useEffect(() => {
    const bootstrapSession = async () => {
      clearLegacyAuthStorage()

      try {
        const { data } = await client.get<CurrentUserResponse>('/api/users/me')
        setUser(toAuthUser(data))
      } catch {
        setUser(null)
      } finally {
        setIsBootstrapping(false)
      }
    }

    void bootstrapSession()
  }, [])

  const login = useCallback((userData: AuthUser) => {
    setUser(userData)
    setIsBootstrapping(false)
  }, [])

  const markOnboardingCompleted = useCallback((completedAt?: string) => {
    setUser((previous) => {
      if (!previous) {
        return previous
      }

      return {
        ...previous,
        onboardingCompletedAt: completedAt ?? new Date().toISOString(),
      }
    })
  }, [])

  const logout = useCallback(() => {
    usersApi.logout().catch(() => {})
    clearLegacyAuthStorage()
    setUser(null)
    setIsBootstrapping(false)
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isBootstrapping,
        login,
        markOnboardingCompleted,
        logout,
      }}
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
