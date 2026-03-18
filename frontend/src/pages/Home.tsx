import React, { useState, useEffect, useMemo } from 'react'
import { useLocation } from 'react-router-dom'
import { CreateListModal } from '../components/CreateListModal'
import { Toast, useToast } from '../components/Toast'
import { useLists } from '../hooks/useLists'
import { ListCard } from '../components/ListCard'
import { AppHeader } from '../components/AppHeader'
import { useAuth } from '../contexts/AuthContext'
import { useOnboarding } from '../contexts/OnboardingContext'

const HOME_TOAST_SESSION_KEY = 'homeToast'

/**
 * Página Home - Exemplo de integração com CreateListModal
 * AC4: Toast, modal fecha, redirecionamento (ou refetch)
 */
export const Home: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('')
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
      sessionStorage.removeItem(HOME_TOAST_SESSION_KEY)
      return
    }

    const storedToast = sessionStorage.getItem(HOME_TOAST_SESSION_KEY)
    if (!storedToast) {
      return
    }

    try {
      const parsedToast = JSON.parse(storedToast) as {
        toastMessage?: string
        toastType?: 'success' | 'error' | 'info'
      }

      if (parsedToast.toastMessage) {
        showToast(parsedToast.toastMessage, parsedToast.toastType ?? 'info')
      }
    } finally {
      sessionStorage.removeItem(HOME_TOAST_SESSION_KEY)
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

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearchTerm(searchTerm.trim())
    }, 250)

    return () => window.clearTimeout(timeoutId)
  }, [searchTerm])

  const filteredLists = useMemo(() => {
    if (!debouncedSearchTerm) {
      return lists
    }

    const query = debouncedSearchTerm.toLocaleLowerCase('pt-BR')
    return lists.filter((list) => {
      const name = list.name.toLocaleLowerCase('pt-BR')
      const typeName = list.type.name.toLocaleLowerCase('pt-BR')
      return name.includes(query) || typeName.includes(query)
    })
  }, [debouncedSearchTerm, lists])

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
          primaryAction={
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
            <div className="nl-spinner" role="status" aria-label="Carregando listas" />
          </div>
        )}

        {/* Error State */}
        {error && !loading && (
          <div className="nl-alert col-span-full py-12 text-center">
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
              <div className="nl-preview-card col-span-full px-6 py-16 text-center">
                <p className="mb-2 font-sans text-xs font-semibold uppercase tracking-[0.2em] text-nl-accent">
                  Comece Agora
                </p>
                <h2 className="font-display text-3xl font-semibold text-nl-text">
                  Sua primeira lista começa aqui.
                </h2>
                <p className="mb-5 mt-3 font-sans text-lg text-nl-muted">
                  Você ainda não tem listas. Crie a primeira e compartilhe em segundos.
                </p>
                <button onClick={() => setIsModalOpen(true)} className="nl-btn-primary">
                  <span aria-hidden="true">+</span>
                  Criar Primeira Lista
                </button>
              </div>
            ) : (
              <>
                <div className="col-span-full">
                  <label htmlFor="home-list-search" className="sr-only">
                    Buscar listas
                  </label>
                  <div className="nl-card-soft flex items-center gap-3 px-4 py-3">
                    <svg className="h-5 w-5 text-nl-muted" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                      <path fillRule="evenodd" d="M9 3.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11zM2 9a7 7 0 1112.452 4.391l3.328 3.329a.75.75 0 11-1.06 1.06l-3.329-3.328A7 7 0 012 9z" clipRule="evenodd" />
                    </svg>
                    <input
                      id="home-list-search"
                      type="search"
                      value={searchTerm}
                      onChange={(event) => setSearchTerm(event.target.value)}
                      className="w-full border-0 bg-transparent text-sm text-nl-text placeholder:text-nl-muted focus:outline-none"
                      placeholder="Buscar por nome ou tipo da lista"
                    />
                  </div>
                </div>

                {filteredLists.length > 0 ? (
                  filteredLists.map((list) => <ListCard key={list.id} list={list} />)
                ) : (
                  <div className="nl-card col-span-full px-6 py-12 text-center">
                    <p className="font-sans text-base font-semibold text-nl-text">
                      Nenhuma lista encontrada
                    </p>
                    <p className="mt-2 text-sm text-nl-muted">
                      Tente outro nome ou tipo para continuar.
                    </p>
                  </div>
                )}
              </>
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
        {toasts.map((toast, i) => (
          <Toast
            key={toast.id}
            message={toast.message}
            type={toast.type}
            duration={3000}
            onClose={() => removeToast(toast.id)}
            index={i}
          />
        ))}
      </div>
    </div>
  )
}
