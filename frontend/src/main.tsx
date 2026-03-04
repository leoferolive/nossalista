import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext.tsx'
import { WebSocketProvider } from './contexts/WebSocketContext.tsx'
import Login from './pages/Login.tsx'
import { AuthCallback } from './pages/AuthCallback.tsx'
import { Home } from './pages/Home.tsx'
import { ListView } from './pages/ListView.tsx'
import { JoinListPage } from './pages/JoinListPage.tsx'
import { Profile } from './pages/Profile.tsx'
import { ProtectedRoute } from './components/ProtectedRoute.tsx'
import './index.css'

// sockjs-client still references the Node-style global object in browser builds.
const browserGlobal = globalThis as typeof globalThis & {
  global?: typeof globalThis
}

if (typeof browserGlobal.global === 'undefined') {
  browserGlobal.global = globalThis
}

/**
 * Configuração de rotas da aplicação
 */
function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/auth/callback" element={<AuthCallback />} />
      <Route
        path="/"
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
    <BrowserRouter>
      <AuthProvider>
        <WebSocketProvider>
          <AppRoutes />
        </WebSocketProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
)
