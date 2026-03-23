import React, { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { UserProfile } from '../components/UserProfile'
import { Toast, useToast } from '../components/Toast'
import { usersApi } from '../api/usersApi'
import { ApiError } from '../types/ApiError'
import { useAuth } from '../contexts/AuthContext'
import { AppHeader } from '../components/AppHeader'

/**
 * Página de Perfil do Usuário
 * FR4: Usuário pode acessar seu próprio perfil
 * FR5: Usuário pode atualizar informações do próprio perfil
 */
export const Profile: React.FC = () => {
  const navigate = useNavigate()
  const { toasts, showToast, removeToast } = useToast()
  const { logout } = useAuth()

  const [userData, setUserData] = useState({
    username: '',
    email: '',
    name: null as string | null,
    avatarUrl: null as string | null,
    authProvider: '' as string,
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [updating, setUpdating] = useState(false)

  // Carregar dados do perfil ao montar
  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true)
        setError(null)
        const profile = await usersApi.getProfile()
        setUserData({
          username: profile.username,
          email: profile.email,
          name: profile.name,
          avatarUrl: profile.avatarUrl,
          authProvider: profile.authProvider,
        })
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Erro ao carregar perfil'
        setError(message)
        showToast(message, 'error')
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [showToast])

  const handleUpdateProfile = useCallback(
    async (data: { name: string; avatarUrl: string }) => {
      setUpdating(true)
      try {
        await usersApi.updateProfile(data)
        // Atualizar estado local
        setUserData((prev) => ({
          ...prev,
          name: data.name,
          avatarUrl: data.avatarUrl || prev.avatarUrl,
        }))
        showToast('Perfil atualizado com sucesso!', 'success')
        setIsEditing(false)
      } catch (err) {
        const apiError = err as ApiError
        const message = apiError?.message || 'Erro ao atualizar perfil. Tente novamente.'
        showToast(message, 'error')
        // Permanecer em modo de edição em caso de erro
      } finally {
        setUpdating(false)
      }
    },
    [showToast]
  )

  const handleLogout = useCallback(async () => {
    try {
      await usersApi.logout()
      logout()
      showToast('Até logo!', 'info')
      navigate('/', { replace: true })
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao fazer logout'
      showToast(message, 'error')
    }
  }, [logout, navigate, showToast])

  // Loading state
  if (loading) {
    return (
      <div className="nl-page flex items-center justify-center p-8">
        <div className="nl-spinner" role="status" aria-label="Carregando perfil" />
      </div>
    )
  }

  // Error state
  if (error) {
    return (
      <div className="nl-page flex items-center justify-center p-8">
        <div className="nl-card max-w-md p-8 text-center">
          <p className="mb-2 text-xl font-bold text-nl-danger">Erro ao Carregar Perfil</p>
          <p className="mb-4 text-nl-danger">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="rounded-xl bg-nl-danger px-6 py-2 font-medium text-white transition-colors hover:bg-nl-danger/80 focus-visible:ring-2 focus-visible:ring-nl-danger/40"
          >
            Tentar Novamente
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="nl-page">
      <div className="nl-container max-w-2xl">
        <AppHeader
          eyebrow="NossaLista"
          title="Meu Perfil"
          subtitle="Atualize seu nome, avatar e preferencia de acesso sem perder o ritmo das listas."
          onBack={() => navigate(-1)}
        />

        {/* UserProfile component */}
        <UserProfile
          username={userData.username}
          email={userData.email}
          name={userData.name}
          avatarUrl={userData.avatarUrl}
          authProvider={userData.authProvider}
          isEditing={isEditing}
          onEdit={() => setIsEditing(true)}
          onCancelEdit={() => setIsEditing(false)}
          onSave={handleUpdateProfile}
        />

        {/* Botão Logout */}
        <div className="mt-8 border-t border-nl-border pt-6">
          <button
            onClick={handleLogout}
            disabled={updating}
            className="nl-btn-danger w-full"
            aria-label="Sair da conta"
          >
            {updating ? 'Saindo…' : 'Sair da Conta'}
          </button>
        </div>

        {/* Toasts */}
        {toasts.map((toast, i) => (
          <Toast
            key={toast.id}
            message={toast.message}
            type={toast.type}
            onClose={() => removeToast(toast.id)}
            index={i}
          />
        ))}
      </div>
    </div>
  )
}
