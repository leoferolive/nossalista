import React, { createContext, useContext, useState, useCallback, useEffect } from 'react'
import client from '../api/client'
import {
  clearStoredSession,
  getStoredAuthToken,
  getStoredUser,
  persistAuthSession,
  StoredUser,
} from '../auth/session'

interface CurrentUserResponse {
  id: string
  username: string
  email: string
  name: string | null
  avatarUrl?: string | null
}

type User = StoredUser

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isBootstrapping: boolean
  login: (token: string, user: User) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => getStoredUser())
  const [isBootstrapping, setIsBootstrapping] = useState(true)

  useEffect(() => {
    const bootstrapSession = async () => {
      const token = getStoredAuthToken()
      const savedUser = getStoredUser()

      if (!token) {
        if (savedUser) {
          clearStoredSession()
          setUser(null)
        }
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
        }
        persistAuthSession(token, normalizedUser)
        setUser(normalizedUser)
      } catch {
        clearStoredSession()
        setUser(null)
      } finally {
        setIsBootstrapping(false)
      }
    }

    void bootstrapSession()
  }, [])

  const isAuthenticated = !!user && !!getStoredAuthToken()

  const login = useCallback((token: string, userData: User) => {
    persistAuthSession(token, userData)
    setUser(userData)
    setIsBootstrapping(false)
  }, [])

  const logout = useCallback(() => {
    clearStoredSession()
    setUser(null)
    setIsBootstrapping(false)
  }, [])

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isBootstrapping, login, logout }}>
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
