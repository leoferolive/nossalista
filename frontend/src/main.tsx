import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { registerSW } from 'virtual:pwa-register'
import { AuthProvider, useAuth } from './contexts/AuthContext.tsx'
import { OnboardingProvider } from './contexts/OnboardingContext.tsx'
import { WebSocketProvider } from './contexts/WebSocketContext.tsx'
import { NotificationProvider } from './contexts/NotificationContext.tsx'
import { ThemeProvider } from './contexts/ThemeContext.tsx'
import { ToastProvider } from './contexts/ToastContext.tsx'
import { WebSocketConnectionManager } from './components/WebSocketConnectionManager.tsx'
import { AuthCallback } from './pages/AuthCallback.tsx'
import { Home } from './pages/Home.tsx'
import { LandingPage } from './pages/LandingPage.tsx'
import { ListView } from './pages/ListView.tsx'
import { JoinListPage } from './pages/JoinListPage.tsx'
import { Profile } from './pages/Profile.tsx'
import { LegacyLoginRedirect } from './pages/LegacyLoginRedirect.tsx'
import { Register } from './pages/Register.tsx'
import { ForgotPassword } from './pages/ForgotPassword.tsx'
import { ResetPassword } from './pages/ResetPassword.tsx'
import { ProtectedRoute } from './components/ProtectedRoute.tsx'
import './index.css'

// sockjs-client still references the Node-style global object in browser builds.
const browserGlobal = globalThis as typeof globalThis & {
  global?: typeof globalThis
}

if (typeof browserGlobal.global === 'undefined') {
  browserGlobal.global = globalThis
}

try {
  registerSW({ immediate: true })
} catch {
  // Service worker registration failed — app continues without SW
}

function NotificationWrapper({ children }: { children: React.ReactNode }) {
  const { user } = useAuth()
  if (!user) return <>{children}</>
  return <NotificationProvider userId={user.id}>{children}</NotificationProvider>
}

/**
 * Configuração de rotas da aplicação
 */
function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LegacyLoginRedirect />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/auth/callback" element={<AuthCallback />} />
      <Route
        path="/home"
        element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lists/:id"
        element={
          <ProtectedRoute>
            <ListView />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        }
      />
      {/* Rota pública para join via convite - NÃO usar ProtectedRoute */}
      <Route path="/join/:inviteCode" element={<JoinListPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider>
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <OnboardingProvider>
              <WebSocketProvider>
                <WebSocketConnectionManager />
                <NotificationWrapper>
                  <AppRoutes />
                </NotificationWrapper>
              </WebSocketProvider>
            </OnboardingProvider>
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>
)
