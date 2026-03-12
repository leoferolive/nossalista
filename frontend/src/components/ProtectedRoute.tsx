import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isBootstrapping } = useAuth()
  const location = useLocation()

  if (isBootstrapping) {
    return (
      <div className="nl-page flex items-center justify-center">
        <div className="nl-card px-6 py-4 text-nl-muted">Validando Sessão…</div>
      </div>
    )
  }

  if (!isAuthenticated) {
    const redirectPath = `${location.pathname}${location.search}${location.hash}`
    const searchParams = new URLSearchParams({ auth: 'login', redirect: redirectPath })
    return <Navigate to={`/?${searchParams.toString()}`} replace />
  }

  return <>{children}</>
}
