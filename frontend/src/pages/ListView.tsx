import React, { useEffect, useState, useCallback, useRef } from 'react'
import { Link, useParams, useNavigate, useLocation } from 'react-router-dom'
import { useLists } from '../hooks/useLists'
import { useItems } from '../hooks/useItems'
import { useActivities } from '../hooks/useActivities'
import { useWebSocket } from '../hooks/useWebSocket'
import { useAuth } from '../contexts/AuthContext'
import { LIST_TYPES } from '../types/List'
import { EditListNameModal } from '../components/EditListNameModal'
import { DeleteListModal } from '../components/DeleteListModal'
import { DeleteConfirmModal } from '../components/DeleteConfirmModal'
import { ListItemComponent } from '../components/ListItem'
import { EditItemModal } from '../components/EditItemModal'
import { InviteModal } from '../components/InviteModal'
import { MembersModal } from '../components/MembersModal'
import { ActivityTimeline } from '../components/ActivityTimeline'
import { OnlineMembersBar } from '../components/OnlineMembersBar'
import { ConnectionStatusIndicator } from '../components/ConnectionStatusIndicator'
import { AppHeader } from '../components/AppHeader'
import { listsApi } from '../api/listsApi'
import { useToast, Toast } from '../components/Toast'
import { ApiError } from '../types/ApiError'
import { ListItem } from '../types/Item'
import { ListMemberResponse } from '../types/List'
import { parseListWebSocketMessage } from '../types/WebSocketMessage'
import { OnlineMember } from '../types/OnlineMember'
import { useListRealtimeSync } from '../hooks/useListRealtimeSync'

const ACTIVITY_TIMELINE_ENABLED = true

function sortItemsByPosition(items: ListItem[]): ListItem[] {
  return [...items].sort((a, b) => a.position - b.position)
}

function sortOnlineMembers(members: OnlineMember[], currentUserId?: string): OnlineMember[] {
  return [...members].sort((a, b) => {
    if (a.userId === currentUserId) return -1
    if (b.userId === currentUserId) return 1

    return (a.name || a.username).localeCompare(b.name || b.username, 'pt-BR', {
      sensitivity: 'base',
    })
  })
}

/**
 * Página de visualização de detalhes de uma lista
 * AC3: Mostra header, info da lista, seção de itens vazia
 * AC4: Estados de loading e erro
 */
export const ListView: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const { user: currentUser } = useAuth()
  const { status: wsStatus, subscribe, unsubscribe, send } = useWebSocket()
  const {
    currentList,
    loadingList,
    errorList,
    updatingList,
    deletingList,
    fetchListById,
    updateListName,
    deleteList,
    clearListError,
  } = useLists()
  const { toasts, showToast, removeToast } = useToast()

  // Exibir toast passado via navigation state (ex: boas-vindas após join via convite)
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

  // Hook para gerenciar itens
  const {
    items,
    setItems,
    loadingItems,
    errorItems,
    addingItem,
    togglingItemId,
    deletingItemId,
    fetchItems,
    addItem,
    toggleItem,
    updateItem,
    deleteItem,
    clearItemsError,
  } = useItems()

  // Estado da timeline de atividades (declarado antes do hook para uso como enabled)
  const [isActivityTimelineOpen, setIsActivityTimelineOpen] = useState(false)

  // Hook para gerenciar atividades — carregamento lazy: só busca quando o painel estiver aberto
  const {
    activities,
    loading: loadingActivities,
    hasMore: hasMoreActivities,
    loadMore: loadMoreActivities,
  } = useActivities(id!, ACTIVITY_TIMELINE_ENABLED && isActivityTimelineOpen)

  // Estado do modal de edição de nome da lista
  const [isEditListModalOpen, setIsEditListModalOpen] = useState(false)

  // Estado do modal de edição de item
  const [isEditItemModalOpen, setIsEditItemModalOpen] = useState(false)

  // Estado do modal de exclusão de lista
  const [isDeleteListModalOpen, setIsDeleteListModalOpen] = useState(false)

  // Estado do modal de confirmação de exclusão de item
  const [isDeleteItemModalOpen, setIsDeleteItemModalOpen] = useState(false)
  const [deletingItem, setDeletingItem] = useState<ListItem | null>(null)

  // Estado do modal de convite
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false)
  const [recentInvitedUsers, setRecentInvitedUsers] = useState<string[]>([])
  const [isMembersModalOpen, setIsMembersModalOpen] = useState(false)
  const [members, setMembers] = useState<ListMemberResponse[]>([])
  const [memberCount, setMemberCount] = useState<number | null>(null)
  const [loadingMembers, setLoadingMembers] = useState(false)
  const [membersError, setMembersError] = useState<string | null>(null)
  const [isLeaveConfirmOpen, setIsLeaveConfirmOpen] = useState(false)
  const [leavingList, setLeavingList] = useState(false)
  const [removeConfirmMemberId, setRemoveConfirmMemberId] = useState<string | null>(null)
  const [removingMemberId, setRemovingMemberId] = useState<string | null>(null)

  // Estado de itens adicionados via WebSocket (para animação pulse)
  const [wsAddedItemIds, setWsAddedItemIds] = useState<Set<string>>(new Set())
  const [wsCheckedItemIds, setWsCheckedItemIds] = useState<Set<string>>(new Set())
  const [wsCheckedHighlightItemIds, setWsCheckedHighlightItemIds] = useState<Set<string>>(new Set())
  const [onlineMembers, setOnlineMembers] = useState<Map<string, OnlineMember>>(new Map())
  const [hasPresenceSnapshot, setHasPresenceSnapshot] = useState(false)
  const membersRefreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const wsAnimationTimersRef = useRef<ReturnType<typeof setTimeout>[]>([])

  const { handleIncomingItemsRevision, resetTrackedRevision } = useListRealtimeSync({
    listId: id,
    wsStatus,
    fetchItems,
    getListState: listsApi.getListState,
  })

  const refreshMembersCountFromServer = useCallback(async () => {
    if (!id) {
      return
    }

    try {
      const updatedMembers = await listsApi.getListMembers(id)
      setMembers(updatedMembers)
      setMemberCount(updatedMembers.length)
    } catch (err) {
      console.error('[Realtime members refresh]', err)
    }
  }, [id])

  const scheduleMembersCountRefresh = useCallback(() => {
    if (membersRefreshTimerRef.current) {
      clearTimeout(membersRefreshTimerRef.current)
    }

    membersRefreshTimerRef.current = setTimeout(() => {
      void refreshMembersCountFromServer()
    }, 400)
  }, [refreshMembersCountFromServer])

  // Estado do formulário de adicionar item
  const [newItemName, setNewItemName] = useState('')

  // Handler de mensagens WebSocket — processa eventos de itens em tempo real
  const handleWebSocketMessage = useCallback(
    (raw: unknown) => {
      const message = parseListWebSocketMessage(raw)
      if (!message) {
        console.warn('[WebSocket] Ignorando mensagem invalida', raw)
        return
      }

      const actorId = message.actor?.id
      const actorUsername = message.actor?.username ?? 'Alguem'
      const isOwnAction = actorId === currentUser?.id

      if (message.channel === 'items') {
        const revisionStatus = handleIncomingItemsRevision(message.revision)
        if (revisionStatus === 'missing') {
          if (id) {
            console.warn('[WebSocket] Evento de itens sem revision, forçando resync', message)
            fetchItems(id)
          }
          return
        }
        if (revisionStatus === 'stale') {
          console.warn('[WebSocket] Evento de itens stale ignorado', message)
          return
        }
      }

      switch (message.type) {
        case 'ITEM_ADDED':
          if (!isOwnAction) {
            setItems((prev) => {
              if (prev.some((i) => i.id === message.payload.id)) return prev
              return sortItemsByPosition([...prev, message.payload])
            })
            setWsAddedItemIds((prev) => new Set([...prev, message.payload.id]))
            wsAnimationTimersRef.current.push(
              setTimeout(() => {
                setWsAddedItemIds((prev) => {
                  const next = new Set(prev)
                  next.delete(message.payload.id)
                  return next
                })
              }, 300)
            )
            showToast(`${actorUsername} adicionou ${message.payload.name}`, 'info')
          }
          break
        case 'ITEM_UPDATED':
          if (!isOwnAction) {
            setItems((prev) =>
              sortItemsByPosition(
                prev.map((i) => (i.id === message.payload.id ? message.payload : i))
              )
            )
            showToast(`${actorUsername} editou ${message.payload.name}`, 'info')
          }
          break
        case 'ITEM_REMOVED':
          if (!isOwnAction) {
            setItems((prev) => prev.filter((i) => i.id !== message.payload.id))
            showToast(`${actorUsername} removeu ${message.payload.name}`, 'info')
          }
          break
        case 'ITEM_CHECKED':
          setWsCheckedItemIds((prev) => new Set([...prev, message.payload.id]))
          wsAnimationTimersRef.current.push(
            setTimeout(() => {
              setWsCheckedItemIds((prev) => {
                const next = new Set(prev)
                next.delete(message.payload.id)
                return next
              })
            }, 300)
          )

          if (!isOwnAction) {
            setWsCheckedHighlightItemIds((prev) => new Set([...prev, message.payload.id]))
            wsAnimationTimersRef.current.push(
              setTimeout(() => {
                setWsCheckedHighlightItemIds((prev) => {
                  const next = new Set(prev)
                  next.delete(message.payload.id)
                  return next
                })
              }, 300)
            )

            setItems((prev) =>
              prev.map((i) =>
                i.id === message.payload.id ? { ...i, checked: message.payload.checked } : i
              )
            )
            const action = message.payload.checked ? 'marcou' : 'desmarcou'
            showToast(`${actorUsername} ${action} ${message.payload.name}`, 'info')
          }
          break
        case 'LIST_LAYOUT_UPDATED': {
          const positionsMap = new Map(
            message.payload.positions.map((entry) => [entry.itemId, entry.position])
          )
          setItems((prev) =>
            sortItemsByPosition(
              prev.map((item) => {
                const nextPosition = positionsMap.get(item.id)
                return nextPosition === undefined ? item : { ...item, position: nextPosition }
              })
            )
          )
          break
        }
        case 'PRESENCE_SNAPSHOT': {
          setHasPresenceSnapshot(true)
          setOnlineMembers(
            new Map(
              message.payload.members.map((member) => [
                member.userId,
                {
                  userId: member.userId,
                  username: member.username,
                  name: member.name,
                  avatarUrl: member.avatarUrl,
                },
              ])
            )
          )
          scheduleMembersCountRefresh()
          break
        }
        case 'MEMBER_ONLINE': {
          const payload = message.payload
          setOnlineMembers((prev) => {
            const next = new Map(prev)
            next.set(payload.userId, {
              userId: payload.userId,
              username: payload.username,
              name: payload.name,
              avatarUrl: payload.avatarUrl,
            })
            return next
          })
          scheduleMembersCountRefresh()
          break
        }
        case 'MEMBER_OFFLINE': {
          const payload = message.payload
          setOnlineMembers((prev) => {
            const next = new Map(prev)
            next.delete(payload.userId)
            return next
          })
          break
        }
      }
    },
    [
      currentUser?.id,
      fetchItems,
      handleIncomingItemsRevision,
      id,
      scheduleMembersCountRefresh,
      setItems,
      showToast,
    ]
  )

  useEffect(() => {
    return () => {
      if (membersRefreshTimerRef.current) {
        clearTimeout(membersRefreshTimerRef.current)
      }
      wsAnimationTimersRef.current.forEach(clearTimeout)
      wsAnimationTimersRef.current = []
    }
  }, [])

  // Subscrição WebSocket: subscrever quando CONNECTED, desinscrever ao desmontar/trocar lista
  useEffect(() => {
    resetTrackedRevision()
  }, [id, resetTrackedRevision])

  useEffect(() => {
    if (wsStatus === 'CONNECTED' && id) {
      subscribe(id, 'items', handleWebSocketMessage)
      subscribe(id, 'presence', handleWebSocketMessage)

      return () => {
        unsubscribe(id, 'items')
        unsubscribe(id, 'presence')
        setHasPresenceSnapshot(false)
        setOnlineMembers(new Map())
      }
    }

    setHasPresenceSnapshot(false)
    setOnlineMembers(new Map())
    return undefined
  }, [wsStatus, id, subscribe, unsubscribe, handleWebSocketMessage])

  useEffect(() => {
    if (wsStatus !== 'CONNECTED' || !id) {
      return undefined
    }

    const heartbeatInterval = setInterval(() => {
      send(`/app/list/${id}/heartbeat`, {})
    }, 30_000)

    return () => clearInterval(heartbeatInterval)
  }, [wsStatus, id, send])

  // Carregar dados da lista e itens ao montar o componente
  useEffect(() => {
    if (id) {
      fetchListById(id)
      fetchItems(id)
      listsApi
        .getListMembers(id)
        .then((data) => setMemberCount(data.length))
        .catch(() => setMemberCount(null))
    }
  }, [id, fetchListById, fetchItems])

  // Handler para abrir modal de edição de nome da lista
  const handleOpenEditModal = () => {
    setIsEditListModalOpen(true)
  }

  // Handler para fechar modal de edição de nome da lista
  const handleCloseEditModal = () => {
    setIsEditListModalOpen(false)
  }

  // Handler para salvar novo nome
  const handleSaveListName = async (newName: string) => {
    if (!id) return

    // AC3: Toast "Atualizando…" ao clicar Salvar
    showToast('Atualizando…', 'info')

    try {
      await updateListName(id, newName)
      showToast('Lista atualizada', 'success')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar lista'
      showToast(message, 'error')
      throw err // Re-throw para o modal saber que falhou
    }
  }

  // Handler para abrir modal de exclusão de lista
  const handleOpenDeleteListModal = () => {
    setIsDeleteListModalOpen(true)
  }

  // Handler para fechar modal de exclusão de lista
  const handleCloseDeleteListModal = () => {
    setIsDeleteListModalOpen(false)
  }

  // Handler para confirmar exclusão
  const handleConfirmDelete = async () => {
    if (!id) return

    // AC3: Toast "Excluindo…" ao clicar Excluir
    showToast('Excluindo…', 'info')

    try {
      await deleteList(id)
      showToast('Lista excluída', 'success')
      // AC3: Redireciona para Home após sucesso
      navigate('/')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao excluir lista'
      showToast(message, 'error')

      // AC4: Fecha modal e redireciona em erro 403/404
      // FIX: Usa status code em vez de string matching (mais robusto)
      if (err instanceof ApiError && (err.status === 403 || err.status === 404)) {
        setIsDeleteListModalOpen(false)
        navigate('/')
      }
      throw err
    }
  }

  // Handler para toggle de item (marcar/desmarcar)
  const handleToggleItem = async (itemId: string) => {
    if (!id || togglingItemId === itemId) return

    try {
      await toggleItem(id, itemId)
      showToast('Sincronizado', 'success')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao sincronizar'
      showToast(message, 'error')
    }
  }

  // Handler para editar item
  const [editingItem, setEditingItem] = useState<ListItem | null>(null)

  const handleEditItem = (item: ListItem) => {
    setEditingItem(item)
    setIsEditItemModalOpen(true)
  }

  // Handler para iniciar exclusão de item (abre modal de confirmação)
  const handleDeleteItem = (item: ListItem) => {
    setDeletingItem(item)
    setIsDeleteItemModalOpen(true)
  }

  // Handler para confirmar exclusão de item
  const handleConfirmDeleteItem = async () => {
    if (!id || !deletingItem) return

    try {
      await deleteItem(id, deletingItem.id)
      showToast('Item removido', 'success')
      setIsDeleteItemModalOpen(false)
      setDeletingItem(null)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao remover item'
      showToast(message, 'error')
    }
  }

  // Handler para cancelar exclusão de item
  const handleCancelDeleteItem = () => {
    setIsDeleteItemModalOpen(false)
    setDeletingItem(null)
  }

  // Handler para abrir modal de convite
  const handleOpenInviteModal = () => {
    setIsInviteModalOpen(true)
  }

  // Handler para fechar modal de convite
  const handleCloseInviteModal = () => {
    setIsInviteModalOpen(false)
  }

  // Handler para gerar link de convite
  const handleGenerateInviteLink = useCallback(async () => {
    if (!id) throw new Error('ID da lista não encontrado')
    return await listsApi.generateInviteLink(id)
  }, [id])

  const handleSearchUsers = useCallback(async (query: string) => {
    return await listsApi.searchUsers(query)
  }, [])

  const handleInviteByUsername = useCallback(
    async (username: string) => {
      if (!id) throw new Error('ID da lista não encontrado')
      return await listsApi.inviteByUsername(id, username)
    },
    [id]
  )

  const handleInviteSuccess = useCallback(
    async (invitedUsername: string) => {
      setRecentInvitedUsers((prev) => {
        if (prev.includes(invitedUsername)) {
          return prev
        }
        return [...prev, invitedUsername].slice(-3)
      })

      if (!id) {
        return
      }

      try {
        const updatedMembers = await listsApi.getListMembers(id)
        setMembers(updatedMembers)
        setMemberCount(updatedMembers.length)
      } catch (err) {
        console.error('[Invite refresh members]', err)
      }

      await fetchListById(id)
    },
    [fetchListById, id]
  )

  const handleSaveEditItem = async (
    itemId: string,
    request: { name: string; quantity?: number; dueDate?: string; url?: string }
  ) => {
    if (!id) return

    try {
      // AC7: Toast "Sincronizando…" antes de enviar request
      showToast('Sincronizando…', 'info')

      // USAR O HOOK - não fazer update direto!
      await updateItem(id, itemId, request)

      // Apenas após sucesso real, mostrar toast de sucesso
      showToast('Sincronizado', 'success')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar item'
      showToast(message, 'error')
      throw err
    }
  }

  const handleOpenMembersModal = async () => {
    if (!id) {
      return
    }

    setIsMembersModalOpen(true)
    setLoadingMembers(true)
    setMembersError(null)

    try {
      const data = await listsApi.getListMembers(id)
      setMembers(data)
      setMemberCount(data.length)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar membros'
      setMembersError(message)
      showToast(message, 'error')
    } finally {
      setLoadingMembers(false)
    }
  }

  const handleCloseMembersModal = () => {
    if (leavingList || removingMemberId) {
      return
    }
    setIsMembersModalOpen(false)
    setMembersError(null)
    setIsLeaveConfirmOpen(false)
    setRemoveConfirmMemberId(null)
  }

  const handleRemoveMember = (userId: string) => {
    setRemoveConfirmMemberId(userId)
  }

  const handleConfirmRemove = async () => {
    if (!id || !removeConfirmMemberId || removingMemberId) return

    const userId = removeConfirmMemberId
    const username = members.find((m) => m.user.id === userId)?.user.username ?? userId
    setRemovingMemberId(userId)

    try {
      await listsApi.deleteListMember(id, userId)
      setMembers((prev) => prev.filter((m) => m.user.id !== userId))
      setMemberCount((prev) => (prev !== null ? prev - 1 : null))
      setRemoveConfirmMemberId(null)
      showToast(`${username} removido da lista`, 'success')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao remover participante'
      showToast(message, 'error')
    } finally {
      setRemovingMemberId(null)
    }
  }

  const handleCancelRemove = () => {
    setRemoveConfirmMemberId(null)
  }

  const handleConfirmLeaveList = async () => {
    if (!id || leavingList) {
      return
    }

    setLeavingList(true)

    try {
      await listsApi.leaveList(id)
      setIsLeaveConfirmOpen(false)
      setIsMembersModalOpen(false)
      navigate('/', {
        state: {
          toastMessage: 'Você saiu',
          toastType: 'success',
          refreshLists: true,
        },
      })
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao sair da lista'
      showToast(message, 'error')
    } finally {
      setLeavingList(false)
    }
  }

  // Handler para adicionar novo item
  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!id || !newItemName.trim()) return

    showToast('Adicionando Item…', 'info')

    try {
      await addItem(id, { name: newItemName.trim() })
      setNewItemName('') // Limpa o campo
      showToast('Item adicionado', 'success')
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao adicionar item'
      showToast(message, 'error')
    }
  }

  // Determinar emoji do tipo de lista
  const typeEmoji = currentList
    ? LIST_TYPES.find((t) => t.id === currentList.type.id)?.emoji || '📝'
    : '📝'

  // Loading state
  if (loadingList) {
    return (
      <div className="nl-page">
        <div className="nl-container max-w-4xl">
          {/* Skeleton Header */}
          <div className="flex items-center gap-4 mb-6">
            <div className="h-10 w-10 animate-pulse rounded-xl bg-nl-surface-strong" />
            <div className="h-8 w-64 animate-pulse rounded-xl bg-nl-surface-strong" />
          </div>

          {/* Skeleton Info Card */}
          <div className="nl-card mb-6 p-4">
            <div className="flex items-center gap-3">
              <div className="h-12 w-12 animate-pulse rounded-full bg-nl-surface-strong" />
              <div className="flex-1">
                <div className="mb-2 h-5 w-32 animate-pulse rounded bg-nl-surface-strong" />
                <div className="h-4 w-48 animate-pulse rounded bg-nl-surface-strong" />
              </div>
            </div>
          </div>

          {/* Skeleton Items Section */}
          <div className="nl-card p-4">
            <div className="mb-4 h-6 w-32 animate-pulse rounded bg-nl-surface-strong" />
            <div className="space-y-3">
              <div className="h-16 w-full animate-pulse rounded bg-nl-surface-strong" />
              <div className="h-16 w-full animate-pulse rounded bg-nl-surface-strong" />
              <div className="h-16 w-full animate-pulse rounded bg-nl-surface-strong" />
            </div>
          </div>
        </div>
      </div>
    )
  }

  // Error state - 404 ou 403 (não encontrada ou sem permissão)
  const isNotFound = errorList?.includes('não encontrada')
  const isForbidden = errorList?.includes('permissão')

  if (errorList && (isNotFound || isForbidden)) {
    return (
      <div className="nl-page">
        <div className="nl-container max-w-4xl">
          <div className="nl-card mt-12 border border-nl-danger/30 bg-nl-danger/10 p-8 text-center">
            <p className="text-nl-danger font-semibold mb-4 text-lg">{errorList}</p>
            <Link
              to="/"
              className="inline-flex rounded-xl bg-nl-danger px-6 py-2 font-medium text-white transition-colors hover:bg-nl-danger/80 focus-visible:ring-2 focus-visible:ring-nl-danger/40"
            >
              Voltar para Home
            </Link>
          </div>
        </div>
      </div>
    )
  }

  // Error state - outros erros (500, network, etc)
  if (errorList) {
    return (
      <div className="nl-page">
        <div className="nl-container max-w-4xl">
          <div className="nl-card mt-12 border border-nl-danger/30 bg-nl-danger/10 p-8 text-center">
            <p className="text-nl-danger font-semibold mb-4 text-lg">{errorList}</p>
            <button
              onClick={() => {
                clearListError()
                if (id) fetchListById(id)
              }}
              className="rounded-xl bg-nl-danger px-6 py-2 font-medium text-white transition-colors hover:bg-nl-danger/80 focus-visible:ring-2 focus-visible:ring-nl-danger/40"
            >
              Tentar Novamente
            </button>
          </div>
        </div>
      </div>
    )
  }

  // Se não tem dados ainda, não renderiza nada
  if (!currentList) {
    return null
  }

  const memberCountLabel = memberCount === null ? '--' : String(memberCount)
  const sortedOnlineMembers = sortOnlineMembers(Array.from(onlineMembers.values()), currentUser?.id)

  return (
    <div className="nl-page">
      <div className="nl-container max-w-4xl">
        <div data-tour="list-header">
          <AppHeader
            eyebrow="Lista Viva"
            title={currentList.name}
            subtitle="Compartilhe progresso, acompanhe presenca em tempo real e mantenha tudo sincronizado."
            onBack={() => navigate(-1)}
            actions={
              <>
                <button
                  onClick={handleOpenMembersModal}
                  className="nl-btn-secondary"
                  aria-label="Abrir membros"
                >
                  <span aria-hidden="true">👥</span>
                  <span>Membros</span>
                  <span className="font-tabular rounded-full bg-nl-surface-strong px-2 py-0.5 text-xs text-nl-accent">
                    {memberCountLabel}
                  </span>
                </button>
                {ACTIVITY_TIMELINE_ENABLED && (
                  <button
                    onClick={() => setIsActivityTimelineOpen(true)}
                    className="nl-btn-secondary"
                    aria-label="Ver atividades da lista"
                    title="Histórico de atividades"
                  >
                    <span aria-hidden="true">📜</span>
                    Historico
                  </button>
                )}
                {currentList.isOwner && (
                  <>
                    <button
                      onClick={handleOpenInviteModal}
                      data-tour="list-invite"
                      className="nl-btn-primary"
                      aria-label="Convidar para lista"
                      title="Convidar para lista"
                    >
                      <span aria-hidden="true">+</span>
                      Convidar
                    </button>
                    <button
                      onClick={handleOpenEditModal}
                      className="nl-btn-secondary"
                      aria-label="Editar nome da lista"
                      title="Editar nome da lista"
                    >
                      Editar nome
                    </button>
                    <button
                      onClick={handleOpenDeleteListModal}
                      className="nl-btn-danger"
                      aria-label="Excluir lista"
                      title="Excluir lista"
                    >
                      Excluir
                    </button>
                  </>
                )}
              </>
            }
          />
        </div>

        {/* Info da lista: Tipo (emoji + nome), Dono (avatar + username) */}
        <div className="nl-preview-card mb-6">
          <div className="mb-3 flex items-center gap-3">
            <span className="text-4xl" aria-hidden="true">
              {typeEmoji}
            </span>
            <div>
              <p className="font-semibold text-nl-text">{currentList.type.name}</p>
              <p className="text-sm text-nl-muted">
                Criada por <span className="font-medium">{currentList.owner.username}</span>
              </p>
            </div>
          </div>

          {/* Badge de propriedade */}
          <div className="mt-2 flex items-center gap-2">
            {currentList.isOwner ? (
              <span className="nl-pill">Voce e o dono</span>
            ) : (
              <span className="nl-pill">Lista compartilhada</span>
            )}
          </div>

          {recentInvitedUsers.length > 0 && (
            <p className="mt-2 text-xs text-nl-primary">
              Convidados nesta sessao:{' '}
              {recentInvitedUsers.map((username) => `@${username}`).join(', ')}
            </p>
          )}
        </div>

        <ConnectionStatusIndicator status={wsStatus} dataTour="list-realtime" />

        {hasPresenceSnapshot && onlineMembers.size > 0 && (
          <OnlineMembersBar members={sortedOnlineMembers} currentUserId={currentUser?.id ?? ''} />
        )}

        {/* Seção "Itens": Título + lista de itens */}
        <div className="nl-card p-5 shadow-sm">
          <h2 className="mb-4 font-display text-2xl font-semibold text-nl-text">
            Itens ({items.length})
          </h2>

          {/* Loading state para itens */}
          {loadingItems && (
            <div className="space-y-3">
              <div className="h-16 w-full rounded bg-nl-surface-strong animate-pulse" />
              <div className="h-16 w-full rounded bg-nl-surface-strong animate-pulse" />
              <div className="h-16 w-full rounded bg-nl-surface-strong animate-pulse" />
            </div>
          )}

          {/* Error state para itens */}
          {errorItems && !loadingItems && (
            <div className="rounded-2xl border border-nl-danger/30 bg-nl-danger/10 p-4 text-center">
              <p className="text-nl-danger mb-2">{errorItems}</p>
              <button
                onClick={() => {
                  clearItemsError()
                  if (id) fetchItems(id)
                }}
                className="nl-btn-danger"
              >
                Tentar Novamente
              </button>
            </div>
          )}

          {/* Lista de itens */}
          {!loadingItems && !errorItems && items.length > 0 && (
            <div className="space-y-2">
              {items.map((item) => (
                <ListItemComponent
                  key={item.id}
                  item={item}
                  onToggle={handleToggleItem}
                  onEdit={handleEditItem}
                  onDelete={handleDeleteItem}
                  isDeleting={deletingItemId === item.id}
                  isWsAdded={wsAddedItemIds.has(item.id)}
                  isWsChecked={wsCheckedItemIds.has(item.id)}
                  isWsCheckedHighlight={wsCheckedHighlightItemIds.has(item.id)}
                />
              ))}
            </div>
          )}

          {/* Estado vazio */}
          {!loadingItems && !errorItems && items.length === 0 && (
            <div className="py-12 text-center text-nl-muted">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-16 w-16 mx-auto mb-4 text-nl-muted/50"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                />
              </svg>
              <p className="mb-2 text-lg">Esta lista ainda não tem itens.</p>
              <p className="text-sm">Adicione o primeiro!</p>
            </div>
          )}

          {/* Formulário para adicionar item */}
          <form onSubmit={handleAddItem} className="mt-4" data-tour="list-add-item">
            <div className="flex gap-2">
              <input
                type="text"
                value={newItemName}
                onChange={(e) => setNewItemName(e.target.value)}
                placeholder="Adicionar novo item…"
                disabled={addingItem}
                className="flex-1 rounded-2xl border border-nl-border bg-nl-surface-strong px-4 py-3 text-nl-text placeholder:text-nl-muted/70 caret-nl-accent focus:border-nl-accent focus-visible:ring-2 focus-visible:ring-nl-accent/30 disabled:cursor-not-allowed disabled:bg-nl-surface"
                maxLength={200}
                name="newItem"
                autoComplete="off"
              />
              <button
                type="submit"
                disabled={addingItem || !newItemName.trim()}
                className="flex min-h-[44px] min-w-[44px] items-center gap-2 rounded-2xl bg-gradient-to-r from-orange-500 to-amber-500 px-6 py-3 font-medium text-white transition-colors hover:from-orange-600 hover:to-amber-600 disabled:cursor-not-allowed disabled:bg-slate-300"
                title="Adicionar item"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 4v16m8-8H4"
                  />
                </svg>
                <span className="hidden sm:inline">
                  {addingItem ? 'Adicionando…' : 'Adicionar'}
                </span>
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Modal de edição de nome */}
      <EditListNameModal
        isOpen={isEditListModalOpen}
        listName={currentList.name}
        onClose={handleCloseEditModal}
        onSave={handleSaveListName}
        isSaving={updatingList}
      />

      {/* Modal de confirmação de exclusão de lista */}
      <DeleteListModal
        isOpen={isDeleteListModalOpen}
        listName={currentList.name}
        onClose={handleCloseDeleteListModal}
        onConfirm={handleConfirmDelete}
        isDeleting={deletingList}
      />

      {/* Modal de edição de item */}
      {editingItem && (
        <EditItemModal
          item={editingItem}
          listType={String(currentList.type.id)}
          isOpen={isEditItemModalOpen}
          onClose={() => {
            setIsEditItemModalOpen(false)
            setEditingItem(null)
          }}
          onSave={handleSaveEditItem}
        />
      )}

      {/* Modal de confirmação de exclusão de item */}
      {deletingItem && (
        <DeleteConfirmModal
          isOpen={isDeleteItemModalOpen}
          itemName={deletingItem.name}
          onConfirm={handleConfirmDeleteItem}
          onCancel={handleCancelDeleteItem}
        />
      )}

      {/* Modal de convite */}
      {currentList && (
        <InviteModal
          isOpen={isInviteModalOpen}
          listName={currentList.name}
          onClose={handleCloseInviteModal}
          onGenerateLink={handleGenerateInviteLink}
          onSearchUsers={handleSearchUsers}
          onInviteByUsername={handleInviteByUsername}
          onInviteSuccess={handleInviteSuccess}
        />
      )}

      {/* Modal de membros */}
      {currentList && (
        <MembersModal
          isOpen={isMembersModalOpen}
          members={members}
          loading={loadingMembers}
          error={membersError}
          isOwner={currentList.isOwner}
          onClose={handleCloseMembersModal}
          onLeaveList={() => setIsLeaveConfirmOpen(true)}
          leaving={leavingList}
          isLeaveConfirmOpen={isLeaveConfirmOpen}
          onConfirmLeave={handleConfirmLeaveList}
          onCancelLeave={() => setIsLeaveConfirmOpen(false)}
          onRemoveMember={handleRemoveMember}
          removingMemberId={removingMemberId}
          removeConfirmMemberId={removeConfirmMemberId}
          onConfirmRemove={handleConfirmRemove}
          onCancelRemove={handleCancelRemove}
        />
      )}

      {/* Timeline de atividades */}
      {ACTIVITY_TIMELINE_ENABLED && (
        <ActivityTimeline
          isOpen={isActivityTimelineOpen}
          onClose={() => setIsActivityTimelineOpen(false)}
          activities={activities}
          loading={loadingActivities}
          hasMore={hasMoreActivities}
          onLoadMore={loadMoreActivities}
        />
      )}

      {/* Toasts */}
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          message={toast.message}
          type={toast.type}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </div>
  )
}
