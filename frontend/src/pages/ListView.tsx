import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useLists } from '../hooks/useLists';
import { useItems } from '../hooks/useItems';
import { useActivities } from '../hooks/useActivities';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../contexts/AuthContext';
import { LIST_TYPES } from '../types/List';
import { EditListNameModal } from '../components/EditListNameModal';
import { DeleteListModal } from '../components/DeleteListModal';
import { DeleteConfirmModal } from '../components/DeleteConfirmModal';
import { ListItemComponent } from '../components/ListItem';
import { EditItemModal } from '../components/EditItemModal';
import { InviteModal } from '../components/InviteModal';
import { MembersModal } from '../components/MembersModal';
import { ActivityTimeline } from '../components/ActivityTimeline';
import { listsApi } from '../api/listsApi';
import { useToast, Toast } from '../components/Toast';
import { ApiError } from '../types/ApiError';
import { ListItem } from '../types/Item';
import { ListMemberResponse } from '../types/List';
import { ListWebSocketMessage } from '../types/WebSocketMessage';

/**
 * Página de visualização de detalhes de uma lista
 * AC3: Mostra header, info da lista, seção de itens vazia
 * AC4: Estados de loading e erro
 */
export const ListView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user: currentUser, isAuthenticated } = useAuth();
  const { status: wsStatus, connect, disconnect, subscribe, unsubscribe } = useWebSocket();
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
  } = useLists();
  const { toasts, showToast, removeToast } = useToast();

  // Exibir toast passado via navigation state (ex: boas-vindas após join via convite)
  useEffect(() => {
    const state = location.state as { toastMessage?: string; toastType?: 'success' | 'error' | 'info' } | null;
    if (state?.toastMessage) {
      showToast(state.toastMessage, state.toastType ?? 'info');
      window.history.replaceState({}, '');
    }
  }, [location.state, showToast]);

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
  } = useItems();

  // Estado da timeline de atividades (declarado antes do hook para uso como enabled)
  const [isActivityTimelineOpen, setIsActivityTimelineOpen] = useState(false);

  // Hook para gerenciar atividades — carregamento lazy: só busca quando o painel estiver aberto
  const {
    activities,
    loading: loadingActivities,
    hasMore: hasMoreActivities,
    loadMore: loadMoreActivities,
  } = useActivities(id!, isActivityTimelineOpen);

  // Estado do modal de edição de nome da lista
  const [isEditListModalOpen, setIsEditListModalOpen] = useState(false);

  // Estado do modal de edição de item
  const [isEditItemModalOpen, setIsEditItemModalOpen] = useState(false);

  // Estado do modal de exclusão de lista
  const [isDeleteListModalOpen, setIsDeleteListModalOpen] = useState(false);

  // Estado do modal de confirmação de exclusão de item
  const [isDeleteItemModalOpen, setIsDeleteItemModalOpen] = useState(false);
  const [deletingItem, setDeletingItem] = useState<ListItem | null>(null);

  // Estado do modal de convite
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [recentInvitedUsers, setRecentInvitedUsers] = useState<string[]>([]);
  const [isMembersModalOpen, setIsMembersModalOpen] = useState(false);
  const [members, setMembers] = useState<ListMemberResponse[]>([]);
  const [memberCount, setMemberCount] = useState<number | null>(null);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [membersError, setMembersError] = useState<string | null>(null);
  const [isLeaveConfirmOpen, setIsLeaveConfirmOpen] = useState(false);
  const [leavingList, setLeavingList] = useState(false);
  const [removeConfirmMemberId, setRemoveConfirmMemberId] = useState<string | null>(null);
  const [removingMemberId, setRemovingMemberId] = useState<string | null>(null);

  // Estado de itens adicionados via WebSocket (para animação pulse)
  const [wsAddedItemIds, setWsAddedItemIds] = useState<Set<string>>(new Set());
  const [wsCheckedItemIds, setWsCheckedItemIds] = useState<Set<string>>(new Set());

  // Estado do formulário de adicionar item
  const [newItemName, setNewItemName] = useState('');

  // Handler de mensagens WebSocket — processa eventos de itens em tempo real
  const handleWebSocketMessage = useCallback((raw: unknown) => {
    const message = raw as ListWebSocketMessage;
    const isOwnAction = message.userId === currentUser?.id;

    switch (message.type) {
      case 'ITEM_ADDED':
        if (!isOwnAction) {
          setItems((prev) => {
            if (prev.some((i) => i.id === message.payload.id)) return prev;
            return [...prev, message.payload];
          });
          setWsAddedItemIds((prev) => new Set([...prev, message.payload.id]));
          setTimeout(() => {
            setWsAddedItemIds((prev) => {
              const next = new Set(prev);
              next.delete(message.payload.id);
              return next;
            });
          }, 300);
          showToast(`${message.username} adicionou ${message.payload.name}`, 'info');
        }
        break;
      case 'ITEM_UPDATED':
        if (!isOwnAction) {
          setItems((prev) =>
            prev.map((i) => (i.id === message.payload.id ? message.payload : i))
          );
          showToast(`${message.username} editou ${message.payload.name}`, 'info');
        }
        break;
      case 'ITEM_REMOVED':
        if (!isOwnAction) {
          setItems((prev) => prev.filter((i) => i.id !== message.payload.id));
          showToast(`${message.username} removeu ${message.payload.name}`, 'info');
        }
        break;
      case 'ITEM_CHECKED':
        setWsCheckedItemIds((prev) => new Set([...prev, message.payload.id]));
        setTimeout(() => {
          setWsCheckedItemIds((prev) => {
            const next = new Set(prev);
            next.delete(message.payload.id);
            return next;
          });
        }, 300);

        if (!isOwnAction) {
          setItems((prev) =>
            prev.map((i) =>
              i.id === message.payload.id ? { ...i, checked: message.payload.checked } : i
            )
          );
          const action = message.payload.checked ? 'marcou' : 'desmarcou';
          showToast(`${message.username} ${action} ${message.payload.name}`, 'info');
        }
        break;
    }
  }, [currentUser?.id, setItems, showToast]);

  // Conexão WebSocket: conectar ao montar (apenas se autenticado), desconectar ao desmontar
  useEffect(() => {
    if (isAuthenticated) {
      connect();
    }
    return () => disconnect();
  }, [connect, disconnect, isAuthenticated]);

  // Subscrição WebSocket: subscrever quando CONNECTED, desinscrever ao desmontar/trocar lista
  useEffect(() => {
    if (wsStatus === 'CONNECTED' && id) {
      subscribe(id, handleWebSocketMessage);
      return () => unsubscribe(id);
    }
    return undefined;
  }, [wsStatus, id, subscribe, unsubscribe, handleWebSocketMessage]);

  // Carregar dados da lista e itens ao montar o componente
  useEffect(() => {
    if (id) {
      fetchListById(id);
      fetchItems(id);
      listsApi.getListMembers(id)
        .then((data) => setMemberCount(data.length))
        .catch(() => setMemberCount(null));
    }
  }, [id, fetchListById, fetchItems]);

  // Handler para abrir modal de edição de nome da lista
  const handleOpenEditModal = () => {
    setIsEditListModalOpen(true);
  };

  // Handler para fechar modal de edição de nome da lista
  const handleCloseEditModal = () => {
    setIsEditListModalOpen(false);
  };

  // Handler para salvar novo nome
  const handleSaveListName = async (newName: string) => {
    if (!id) return;

    // AC3: Toast "Atualizando..." ao clicar Salvar
    showToast('Atualizando...', 'info');

    try {
      await updateListName(id, newName);
      showToast('Lista atualizada', 'success');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar lista';
      showToast(message, 'error');
      throw err; // Re-throw para o modal saber que falhou
    }
  };

  // Handler para abrir modal de exclusão de lista
  const handleOpenDeleteListModal = () => {
    setIsDeleteListModalOpen(true);
  };

  // Handler para fechar modal de exclusão de lista
  const handleCloseDeleteListModal = () => {
    setIsDeleteListModalOpen(false);
  };

  // Handler para confirmar exclusão
  const handleConfirmDelete = async () => {
    if (!id) return;

    // AC3: Toast "Excluindo..." ao clicar Excluir
    showToast('Excluindo...', 'info');

    try {
      await deleteList(id);
      showToast('Lista excluída', 'success');
      // AC3: Redireciona para Home após sucesso
      navigate('/');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao excluir lista';
      showToast(message, 'error');

      // AC4: Fecha modal e redireciona em erro 403/404
      // FIX: Usa status code em vez de string matching (mais robusto)
      if (err instanceof ApiError && (err.status === 403 || err.status === 404)) {
        setIsDeleteListModalOpen(false);
        navigate('/');
      }
      throw err;
    }
  };

  // Handler para toggle de item (marcar/desmarcar)
  const handleToggleItem = async (itemId: string) => {
    if (!id || togglingItemId === itemId) return;

    try {
      await toggleItem(id, itemId);
      showToast('Sincronizado', 'success');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao sincronizar';
      showToast(message, 'error');
    }
  };

  // Handler para editar item
  const [editingItem, setEditingItem] = useState<ListItem | null>(null);

  const handleEditItem = (item: ListItem) => {
    setEditingItem(item);
    setIsEditItemModalOpen(true);
  };

  // Handler para iniciar exclusão de item (abre modal de confirmação)
  const handleDeleteItem = (item: ListItem) => {
    setDeletingItem(item);
    setIsDeleteItemModalOpen(true);
  };

  // Handler para confirmar exclusão de item
  const handleConfirmDeleteItem = async () => {
    if (!id || !deletingItem) return;

    try {
      await deleteItem(id, deletingItem.id);
      showToast('Item removido', 'success');
      setIsDeleteItemModalOpen(false);
      setDeletingItem(null);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao remover item';
      showToast(message, 'error');
    }
  };

  // Handler para cancelar exclusão de item
  const handleCancelDeleteItem = () => {
    setIsDeleteItemModalOpen(false);
    setDeletingItem(null);
  };

  // Handler para abrir modal de convite
  const handleOpenInviteModal = () => {
    setIsInviteModalOpen(true);
  };

  // Handler para fechar modal de convite
  const handleCloseInviteModal = () => {
    setIsInviteModalOpen(false);
  };

  // Handler para gerar link de convite
  const handleGenerateInviteLink = useCallback(async () => {
    if (!id) throw new Error('ID da lista não encontrado');
    return await listsApi.generateInviteLink(id);
  }, [id]);

  const handleSearchUsers = useCallback(async (query: string) => {
    return await listsApi.searchUsers(query);
  }, []);

  const handleInviteByUsername = useCallback(async (username: string) => {
    if (!id) throw new Error('ID da lista não encontrado');
    return await listsApi.inviteByUsername(id, username);
  }, [id]);

  const handleInviteSuccess = useCallback(async (invitedUsername: string) => {
    setRecentInvitedUsers((prev) => {
      if (prev.includes(invitedUsername)) {
        return prev;
      }
      return [...prev, invitedUsername].slice(-3);
    });

    if (!id) {
      return;
    }
    await fetchListById(id);
  }, [fetchListById, id]);

  const handleSaveEditItem = async (itemId: string, request: { name: string; quantity?: number; dueDate?: string; url?: string }) => {
    if (!id) return;

    try {
      // AC7: Toast "Sincronizando..." antes de enviar request
      showToast('Sincronizando...', 'info');

      // USAR O HOOK - não fazer update direto!
      await updateItem(id, itemId, request);

      // Apenas após sucesso real, mostrar toast de sucesso
      showToast('Sincronizado', 'success');
      setIsEditItemModalOpen(false);
      setEditingItem(null);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao atualizar item';
      showToast(message, 'error');
      throw err;
    }
  };

  const handleOpenMembersModal = async () => {
    if (!id) {
      return;
    }

    setIsMembersModalOpen(true);
    setLoadingMembers(true);
    setMembersError(null);

    try {
      const data = await listsApi.getListMembers(id);
      setMembers(data);
      setMemberCount(data.length);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar membros';
      setMembersError(message);
      showToast(message, 'error');
    } finally {
      setLoadingMembers(false);
    }
  };

  const handleCloseMembersModal = () => {
    if (leavingList || removingMemberId) {
      return;
    }
    setIsMembersModalOpen(false);
    setMembersError(null);
    setIsLeaveConfirmOpen(false);
    setRemoveConfirmMemberId(null);
  };

  const handleRemoveMember = (userId: string) => {
    setRemoveConfirmMemberId(userId);
  };

  const handleConfirmRemove = async () => {
    if (!id || !removeConfirmMemberId || removingMemberId) return;

    const userId = removeConfirmMemberId;
    const username = members.find((m) => m.user.id === userId)?.user.username ?? userId;
    setRemovingMemberId(userId);

    try {
      await listsApi.deleteListMember(id, userId);
      setMembers((prev) => prev.filter((m) => m.user.id !== userId));
      setMemberCount((prev) => (prev !== null ? prev - 1 : null));
      setRemoveConfirmMemberId(null);
      showToast(`${username} removido da lista`, 'success');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao remover participante';
      showToast(message, 'error');
    } finally {
      setRemovingMemberId(null);
    }
  };

  const handleCancelRemove = () => {
    setRemoveConfirmMemberId(null);
  };

  const handleConfirmLeaveList = async () => {
    if (!id || leavingList) {
      return;
    }

    setLeavingList(true);

    try {
      await listsApi.leaveList(id);
      setIsLeaveConfirmOpen(false);
      setIsMembersModalOpen(false);
      navigate('/', {
        state: {
          toastMessage: 'Você saiu',
          toastType: 'success',
          refreshLists: true,
        },
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao sair da lista';
      showToast(message, 'error');
    } finally {
      setLeavingList(false);
    }
  };

  // Handler para adicionar novo item
  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!id || !newItemName.trim()) return;

    showToast('Adicionando item...', 'info');

    try {
      await addItem(id, { name: newItemName.trim() });
      setNewItemName(''); // Limpa o campo
      showToast('Item adicionado', 'success');
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao adicionar item';
      showToast(message, 'error');
    }
  };

  // Determinar emoji do tipo de lista
  const typeEmoji = currentList
    ? LIST_TYPES.find((t) => t.id === currentList.type.id)?.emoji || '📝'
    : '📝';

  // Loading state
  if (loadingList) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-4xl mx-auto p-4">
          {/* Skeleton Header */}
          <div className="flex items-center gap-4 mb-6">
            <div className="w-10 h-10 bg-gray-200 rounded-lg animate-pulse" />
            <div className="h-8 w-64 bg-gray-200 rounded animate-pulse" />
          </div>

          {/* Skeleton Info Card */}
          <div className="bg-white rounded-lg p-4 mb-6 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 bg-gray-200 rounded-full animate-pulse" />
              <div className="flex-1">
                <div className="h-5 w-32 bg-gray-200 rounded animate-pulse mb-2" />
                <div className="h-4 w-48 bg-gray-200 rounded animate-pulse" />
              </div>
            </div>
          </div>

          {/* Skeleton Items Section */}
          <div className="bg-white rounded-lg p-4 shadow-sm">
            <div className="h-6 w-32 bg-gray-200 rounded animate-pulse mb-4" />
            <div className="space-y-3">
              <div className="h-16 w-full bg-gray-200 rounded animate-pulse" />
              <div className="h-16 w-full bg-gray-200 rounded animate-pulse" />
              <div className="h-16 w-full bg-gray-200 rounded animate-pulse" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Error state - 404 ou 403 (não encontrada ou sem permissão)
  const isNotFound = errorList?.includes('não encontrada');
  const isForbidden = errorList?.includes('permissão');

  if (errorList && (isNotFound || isForbidden)) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-4xl mx-auto p-4">
          <div className="bg-red-50 border border-red-200 rounded-lg p-8 text-center mt-12">
            <p className="text-red-800 font-semibold mb-4 text-lg">
              {errorList}
            </p>
            <button
              onClick={() => navigate('/')}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
            >
              Voltar para Home
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Error state - outros erros (500, network, etc)
  if (errorList) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-4xl mx-auto p-4">
          <div className="bg-red-50 border border-red-200 rounded-lg p-8 text-center mt-12">
            <p className="text-red-800 font-semibold mb-4 text-lg">
              {errorList}
            </p>
            <button
              onClick={() => {
                clearListError();
                if (id) fetchListById(id);
              }}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
            >
              Tentar Novamente
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Se não tem dados ainda, não renderiza nada
  if (!currentList) {
    return null;
  }

  const memberCountLabel = memberCount === null ? '--' : String(memberCount);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto p-4">
        {/* Header: Nome da lista + botão voltar + botão editar */}
        <div className="flex items-center gap-4 mb-6">
          <button
            onClick={() => navigate(-1)}
            className="p-3 min-w-[44px] min-h-[44px] hover:bg-gray-200 rounded-lg transition-colors flex items-center justify-center"
            aria-label="Voltar"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6 text-gray-700"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10 19l-7-7m0 0l7-7m-7 7h18"
              />
            </svg>
          </button>
          <h1 className="text-2xl font-bold text-gray-900 flex-1">{currentList.name}</h1>
          <button
            onClick={handleOpenMembersModal}
            className="inline-flex items-center gap-2 px-4 py-2 min-h-[44px] rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors"
            aria-label="Abrir membros"
          >
            <span aria-hidden="true">👥</span>
            <span className="font-medium text-sm">Membros</span>
            <span className="text-sm text-gray-600">{memberCountLabel}</span>
          </button>
          {/* Menu de ações - apenas para dono da lista */}
          <button
            onClick={() => setIsActivityTimelineOpen(true)}
            className="p-3 min-w-[44px] min-h-[44px] hover:bg-gray-100 rounded-lg transition-colors flex items-center justify-center relative"
            aria-label="Ver atividades da lista"
            title="Histórico de atividades"
          >
            <span className="text-xl">📜</span>
          </button>
          {currentList.isOwner && (
            <div className="flex items-center gap-1">
              {/* Botão convidar */}
              <button
                onClick={handleOpenInviteModal}
                className="p-3 min-w-[44px] min-h-[44px] hover:bg-green-100 rounded-lg transition-colors flex items-center justify-center"
                aria-label="Convidar para lista"
                title="Convidar para lista"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5 text-green-600"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"
                  />
                </svg>
              </button>
              {/* Botão editar */}
              <button
                onClick={handleOpenEditModal}
                className="p-3 min-w-[44px] min-h-[44px] hover:bg-gray-200 rounded-lg transition-colors flex items-center justify-center"
                aria-label="Editar nome da lista"
                title="Editar nome da lista"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5 text-gray-600"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"
                  />
                </svg>
              </button>
              {/* Botão excluir */}
              <button
                onClick={handleOpenDeleteListModal}
                className="p-3 min-w-[44px] min-h-[44px] hover:bg-red-100 rounded-lg transition-colors flex items-center justify-center"
                aria-label="Excluir lista"
                title="Excluir lista"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5 text-red-600"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                  />
                </svg>
              </button>
            </div>
          )}
        </div>

        {/* Info da lista: Tipo (emoji + nome), Dono (avatar + username) */}
        <div className="bg-white rounded-lg p-4 mb-6 shadow-sm">
          <div className="flex items-center gap-3 mb-3">
            <span className="text-4xl" aria-hidden="true">
              {typeEmoji}
            </span>
            <div>
              <p className="font-semibold text-gray-900">
                {currentList.type.name}
              </p>
              <p className="text-sm text-gray-500">
                Criada por{' '}
                <span className="font-medium">{currentList.owner.username}</span>
              </p>
            </div>
          </div>

          {/* Badge de propriedade */}
          <div className="flex items-center gap-2 mt-2">
            {currentList.isOwner ? (
              <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded font-medium">
                Você é o dono
              </span>
            ) : (
              <span className="text-xs bg-gray-100 text-gray-800 px-2 py-1 rounded font-medium">
                Lista compartilhada
              </span>
            )}
          </div>

          {recentInvitedUsers.length > 0 && (
            <p className="text-xs text-green-700 mt-2">
              Convidados nesta sessao: {recentInvitedUsers.map((username) => `@${username}`).join(', ')}
            </p>
          )}
        </div>

        {/* Seção "Itens": Título + lista de itens */}
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">
            Itens ({items.length})
          </h2>

          {/* Loading state para itens */}
          {loadingItems && (
            <div className="space-y-3">
              <div className="h-16 w-full bg-gray-200 rounded animate-pulse" />
              <div className="h-16 w-full bg-gray-200 rounded animate-pulse" />
              <div className="h-16 w-full bg-gray-200 rounded animate-pulse" />
            </div>
          )}

          {/* Error state para itens */}
          {errorItems && !loadingItems && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center">
              <p className="text-red-800 mb-2">{errorItems}</p>
              <button
                onClick={() => {
                  clearItemsError();
                  if (id) fetchItems(id);
                }}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors text-sm"
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
                />
              ))}
            </div>
          )}

          {/* Estado vazio */}
          {!loadingItems && !errorItems && items.length === 0 && (
            <div className="text-center py-12 text-gray-500">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-16 w-16 mx-auto mb-4 text-gray-300"
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
              <p className="mb-2 text-lg">
                Esta lista ainda não tem itens.
              </p>
              <p className="text-sm">Adicione o primeiro!</p>
            </div>
          )}

          {/* Formulário para adicionar item */}
          <form onSubmit={handleAddItem} className="mt-4">
            <div className="flex gap-2">
              <input
                type="text"
                value={newItemName}
                onChange={(e) => setNewItemName(e.target.value)}
                placeholder="Adicionar novo item..."
                disabled={addingItem}
                className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                maxLength={200}
              />
              <button
                type="submit"
                disabled={addingItem || !newItemName.trim()}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center gap-2 min-w-[44px] min-h-[44px]"
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
                  {addingItem ? 'Adicionando...' : 'Adicionar'}
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
            setIsEditItemModalOpen(false);
            setEditingItem(null);
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
      <ActivityTimeline
        isOpen={isActivityTimelineOpen}
        onClose={() => setIsActivityTimelineOpen(false)}
        activities={activities}
        loading={loadingActivities}
        hasMore={hasMoreActivities}
        onLoadMore={loadMoreActivities}
      />

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
  );
};
