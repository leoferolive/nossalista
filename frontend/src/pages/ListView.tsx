import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useLists } from '../hooks/useLists';
import { LIST_TYPES } from '../types/List';
import { EditListNameModal } from '../components/EditListNameModal';
import { useToast, Toast } from '../components/Toast';

/**
 * Página de visualização de detalhes de uma lista
 * AC3: Mostra header, info da lista, seção de itens vazia
 * AC4: Estados de loading e erro
 */
export const ListView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const {
    currentList,
    loadingList,
    errorList,
    updatingList,
    fetchListById,
    updateListName,
    clearListError,
  } = useLists();
  const { toasts, showToast, removeToast } = useToast();

  // Estado do modal de edição
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  // Carregar dados da lista ao montar o componente
  useEffect(() => {
    if (id) {
      fetchListById(id);
    }
  }, [id, fetchListById]);

  // Handler para abrir modal de edição
  const handleOpenEditModal = () => {
    setIsEditModalOpen(true);
  };

  // Handler para fechar modal de edição
  const handleCloseEditModal = () => {
    setIsEditModalOpen(false);
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
            <div className="h-4 w-full bg-gray-200 rounded animate-pulse mb-2" />
            <div className="h-4 w-3/4 bg-gray-200 rounded animate-pulse" />
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
          <h1 className="text-2xl font-bold text-gray-900 flex-1">
            {currentList.name}
          </h1>
          {/* Botão editar - apenas para dono da lista */}
          {currentList.isOwner && (
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
        </div>

        {/* Seção "Itens": Título + área vazia */}
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">
            Itens ({currentList.itemsCount})
          </h2>

          {/* Estado vazio */}
          {currentList.itemsCount === 0 && (
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

          {/* Botão "Adicionar Item" - disabled (será habilitado no Epic 3) */}
          <button
            disabled
            className="w-full mt-4 py-3 px-4 bg-gray-200 text-gray-500 rounded-lg cursor-not-allowed font-medium flex items-center justify-center gap-2"
            title="Disponível no Epic 3"
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
            Adicionar Item
          </button>
          <p className="text-center text-xs text-gray-400 mt-2">
            (Funcionalidade disponível no Epic 3)
          </p>
        </div>
      </div>

      {/* Modal de edição de nome */}
      <EditListNameModal
        isOpen={isEditModalOpen}
        listName={currentList.name}
        onClose={handleCloseEditModal}
        onSave={handleSaveListName}
        isSaving={updatingList}
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
