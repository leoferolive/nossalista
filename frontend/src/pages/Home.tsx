import React, { useState, useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { CreateListModal } from '../components/CreateListModal'
import { Toast, useToast } from '../components/Toast'
import { useLists } from '../hooks/useLists'
import { ListCard } from '../components/ListCard'
import { AppHeader } from '../components/AppHeader'
import { useAuth } from '../contexts/AuthContext'

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

  const handleCreateList = async (request: { name: string; typeId: number }) => {
    const newList = await createList(request)
    return { id: newList.id }
  }

  const handleSuccess = async (listId: string) => {
    void listId
    // AC4: Modal fecha primeiro (evita race condition com refetch)
    setIsModalOpen(false)

    // AC4: Toast "Lista criada" (success, 300ms animation)
    showToast('Lista criada!', 'success')

    // AC4: Refetch listas após modal fechar
    try {
      await fetchLists()
    } catch {
      showToast('Lista criada, mas houve erro ao atualizar a lista.', 'error')
    }

    // Opcional: navegar para lista criada
    // navigate(`/lists/${listId}`);
  }

  return (
    <div className="nl-page">
      <div className="nl-container">
        <AppHeader
          eyebrow="Painel Tropical"
          title="Minhas Listas"
          subtitle={`Tudo pronto para ${user?.displayName || user?.username}. Crie, compartilhe e retome suas listas com ritmo rapido.`}
          actions={
            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex min-h-[48px] items-center gap-2 rounded-2xl bg-gradient-to-r from-nl-accent to-nl-accent-strong px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-orange-900/20 transition-transform hover:-translate-y-0.5 hover:from-orange-600 hover:to-amber-600 focus-visible:ring-2 focus-visible:ring-nl-focus/40"
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
            <p className="mb-4 text-lg text-nl-danger">{error}</p>
            <button
              onClick={() => {
                clearError()
                fetchLists()
              }}
              className="rounded-xl bg-red-600 px-4 py-2 font-medium text-white transition-colors hover:bg-red-700 focus-visible:ring-2 focus-visible:ring-nl-danger/40"
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
                <p className="mb-2 text-xs font-semibold uppercase tracking-[0.2em] text-nl-accent">
                  Comece Agora
                </p>
                <p className="mb-5 text-lg text-nl-text">
                  Voce ainda nao tem listas. Crie a primeira e compartilhe em segundos.
                </p>
                <button
                  onClick={() => setIsModalOpen(true)}
                  className="inline-flex min-h-[48px] items-center gap-2 rounded-2xl bg-gradient-to-r from-nl-primary to-nl-primary-strong px-5 py-3 text-sm font-semibold text-white transition-colors hover:from-nl-primary-strong hover:to-nl-primary-strong focus-visible:ring-2 focus-visible:ring-nl-focus/40"
                >
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
