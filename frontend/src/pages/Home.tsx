import React, { useState, useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { CreateListModal } from '../components/CreateListModal'
import { Toast, useToast } from '../components/Toast'
import { useLists } from '../hooks/useLists'
import { ListCard } from '../components/ListCard'
import { AppHeader } from '../components/AppHeader'
import { useAuth } from '../contexts/AuthContext'
import { useOnboarding } from '../contexts/OnboardingContext'

/**
 * Página Home - Exemplo de integração com CreateListModal
 * AC4: Toast, modal fecha, redirecionamento (ou refetch)
 */
export const Home: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const { createList, lists, fetchLists, loading, error, clearError } = useLists()
  const { toasts, showToast, removeToast } = useToast()
  const location = useLocation()
  const { user } = useAuth()
  const {
    isRunning: isOnboardingRunning,
    requestOpenCreateListModal,
    acknowledgeCreateListModalRequest,
    onListCreated,
  } = useOnboarding()

  // Carregar listas ao montar o componente
  useEffect(() => {
    fetchLists()
  }, [fetchLists])

  // Exibir toast passado via navigation state (ex: erro após falha de join via convite)
  useEffect(() => {
    const state = location.state as {
      toastMessage?: string
      toastType?: 'success' | 'error' | 'info'
    } | null
    if (state?.toastMessage) {
      showToast(state.toastMessage, state.toastType ?? 'info')
      window.history.replaceState({}, '')
    }
  }, [location.state, showToast])

  useEffect(() => {
    const state = location.state as { refreshLists?: boolean } | null
    if (state?.refreshLists) {
      fetchLists()
    }
  }, [location.state, fetchLists])

  useEffect(() => {
    if (!requestOpenCreateListModal) {
      return
    }

    setIsModalOpen(true)
    acknowledgeCreateListModalRequest()
  }, [acknowledgeCreateListModalRequest, requestOpenCreateListModal])

  const handleCreateList = async (request: { name: string; typeId: number }) => {
    const newList = await createList(request)
    return { id: newList.id }
  }

  const handleSuccess = async (listId: string) => {
    if (isOnboardingRunning) {
      onListCreated(listId)
    }

    // AC4: Modal fecha primeiro (evita race condition com refetch)
    setIsModalOpen(false)

    // AC4: Toast "Lista criada" (success, 300ms animation)
    showToast('Lista criada!', 'success')

    // AC4: Refetch listas após modal fechar
    if (!isOnboardingRunning) {
      try {
        await fetchLists()
      } catch {
        showToast('Lista criada, mas houve erro ao atualizar a lista.', 'error')
      }
    }

    // Opcional: navegar para lista criada
    // navigate(`/lists/${listId}`);
  }

  return (
    <div className="nl-page">
      <div className="nl-container">
        <AppHeader
          eyebrow="NossaLista"
          title="Minhas Listas"
          subtitle={`Tudo pronto para ${user?.displayName || user?.username}. Crie, compartilhe e encontre suas listas sem ruído.`}
          actions={
            <button
              onClick={() => setIsModalOpen(true)}
              data-tour="home-create-list"
              className="nl-btn-primary"
            >
              <span aria-hidden="true">+</span>
              Nova Lista
            </button>
          }
        />

        {/* Loading State */}
        {loading && (
          <div className="nl-card flex items-center justify-center py-12">
            <div
              className="h-12 w-12 animate-spin rounded-full border-b-2 border-nl-accent"
              aria-label="Carregando listas"
            />
          </div>
        )}

        {/* Error State */}
        {error && !loading && (
          <div className="nl-card col-span-full py-12 text-center">
            <p className="mb-4 font-sans text-lg text-nl-danger">{error}</p>
            <button
              onClick={() => {
                clearError()
                fetchLists()
              }}
              className="nl-btn-danger"
            >
              Tentar Novamente
            </button>
          </div>
        )}

        {/* Lista de cards */}
        {!loading && !error && (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {lists.length === 0 ? (
              <div className="nl-card col-span-full px-6 py-16 text-center">
                <p className="mb-2 font-sans text-xs font-semibold uppercase tracking-[0.2em] text-nl-accent">
                  Comece Agora
                </p>
                <h2 className="font-display text-3xl font-semibold text-nl-text">
                  Sua primeira lista comeca aqui.
                </h2>
                <p className="mb-5 mt-3 font-sans text-lg text-nl-muted">
                  Voce ainda nao tem listas. Crie a primeira e compartilhe em segundos.
                </p>
                <button onClick={() => setIsModalOpen(true)} className="nl-btn-primary">
                  <span aria-hidden="true">+</span>
                  Criar Primeira Lista
                </button>
              </div>
            ) : (
              lists.map((list) => <ListCard key={list.id} list={list} />)
            )}
          </div>
        )}

        {/* Modal de criar lista */}
        <CreateListModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSuccess={handleSuccess}
          onSubmit={handleCreateList}
        />

        {/* Toasts */}
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            message={toast.message}
            type={toast.type}
            duration={3000}
            onClose={() => removeToast(toast.id)}
          />
        ))}
      </div>
    </div>
  )
}
