import React, { useEffect, useState, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { UserProfile } from '../components/UserProfile'
import { useToast } from '../contexts/ToastContext'
import { usersApi } from '../api/usersApi'
import { ApiError } from '../types/ApiError'
import { useAuth } from '../contexts/AuthContext'
import { AppHeader } from '../components/AppHeader'

// Note: usersApi is still imported for getProfile/updateProfile.
// The logout API call is now handled by AuthContext.logout().

/**
 * Página de Perfil do Usuário
 * FR4: Usuário pode acessar seu próprio perfil
 * FR5: Usuário pode atualizar informações do próprio perfil
 */
export const Profile: React.FC = () => {
  const navigate = useNavigate()
  const { showToast } = useToast()
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
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const deleteDialogRef = useRef<HTMLDialogElement>(null)

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

  const handleLogout = useCallback(() => {
    logout()
    showToast('Até logo!', 'info')
    navigate('/', { replace: true })
  }, [logout, navigate, showToast])

  const handleOpenDeleteConfirm = useCallback(() => {
    setShowDeleteConfirm(true)
    // Use requestAnimationFrame to ensure dialog is in DOM before calling showModal
    requestAnimationFrame(() => {
      deleteDialogRef.current?.showModal()
    })
  }, [])

  const handleCloseDeleteConfirm = useCallback(() => {
    deleteDialogRef.current?.close()
    setShowDeleteConfirm(false)
  }, [])

  const handleDeleteAccount = useCallback(async () => {
    setDeleting(true)
    try {
      await usersApi.deleteAccount()
      handleCloseDeleteConfirm()
      logout()
      showToast('Conta excluída com sucesso.', 'info')
      navigate('/', { replace: true })
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao excluir conta. Tente novamente.'
      showToast(message, 'error')
    } finally {
      setDeleting(false)
    }
  }, [logout, navigate, showToast, handleCloseDeleteConfirm])

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

        {/* Excluir Conta */}
        <div className="mt-6 border-t border-nl-border pt-6">
          <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-nl-danger">
            Zona de Perigo
          </h3>
          <p className="mb-4 text-sm text-nl-muted">
            Ao excluir sua conta, todas as suas listas, itens e dados serão removidos
            permanentemente.
          </p>
          <button
            onClick={handleOpenDeleteConfirm}
            disabled={deleting}
            className="w-full rounded-xl border-2 border-nl-danger bg-transparent px-6 py-2 font-medium text-nl-danger transition-colors hover:bg-nl-danger hover:text-white focus-visible:ring-2 focus-visible:ring-nl-danger/40"
            aria-label="Excluir conta permanentemente"
          >
            Excluir Minha Conta
          </button>
        </div>

        {/* Modal de confirmação de exclusão */}
        {showDeleteConfirm && (
          <dialog
            ref={deleteDialogRef}
            className="nl-card fixed inset-0 m-auto max-w-md rounded-2xl p-6 backdrop:bg-black/50"
            onClose={handleCloseDeleteConfirm}
          >
            <h2 className="mb-2 text-lg font-bold text-nl-danger">Excluir conta</h2>
            <p className="mb-6 text-sm text-nl-muted">
              Tem certeza? Esta ação é irreversível. Todas as suas listas, itens e dados serão
              permanentemente removidos.
            </p>
            <div className="flex gap-3">
              <button
                onClick={handleCloseDeleteConfirm}
                disabled={deleting}
                className="nl-btn flex-1"
              >
                Cancelar
              </button>
              <button
                onClick={handleDeleteAccount}
                disabled={deleting}
                className="nl-btn-danger flex-1"
                aria-label="Confirmar exclusão da conta"
              >
                {deleting ? 'Excluindo…' : 'Sim, excluir'}
              </button>
            </div>
          </dialog>
        )}
      </div>
    </div>
  )
}
