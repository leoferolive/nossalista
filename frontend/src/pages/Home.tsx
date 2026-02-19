import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { CreateListModal } from '../components/CreateListModal';
import { Toast, useToast } from '../components/Toast';
import { useLists } from '../hooks/useLists';
import { ListCard } from '../components/ListCard';

/**
 * Página Home - Exemplo de integração com CreateListModal
 * AC4: Toast, modal fecha, redirecionamento (ou refetch)
 */
export const Home: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { createList, lists, fetchLists, loading, error, clearError } = useLists();
  const { toasts, showToast, removeToast } = useToast();
  const location = useLocation();

  // Carregar listas ao montar o componente
  useEffect(() => {
    fetchLists();
  }, [fetchLists]);

  // Exibir toast passado via navigation state (ex: erro após falha de join via convite)
  useEffect(() => {
    const state = location.state as { toastMessage?: string; toastType?: 'success' | 'error' | 'info' } | null;
    if (state?.toastMessage) {
      showToast(state.toastMessage, state.toastType ?? 'info');
      window.history.replaceState({}, '');
    }
  }, [location.state, showToast]);

  const handleCreateList = async (request: {
    name: string;
    typeId: number;
  }) => {
    const newList = await createList(request);
    return { id: newList.id };
  };

  const handleSuccess = async (listId: string) => {
    void listId;
    // AC4: Modal fecha primeiro (evita race condition com refetch)
    setIsModalOpen(false);

    // AC4: Toast "Lista criada" (success, 300ms animation)
    showToast('Lista criada!', 'success');

    // AC4: Refetch listas após modal fechar
    try {
      await fetchLists();
    } catch {
      showToast('Lista criada, mas houve erro ao atualizar a lista.', 'error');
    }

    // Opcional: navegar para lista criada
    // navigate(`/lists/${listId}`);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Minhas Listas</h1>
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
          >
            + Nova Lista
          </button>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        )}

        {/* Error State */}
        {error && !loading && (
          <div className="col-span-full text-center py-12">
            <p className="text-red-500 text-lg mb-4">{error}</p>
            <button
              onClick={() => {
                clearError();
                fetchLists();
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Tentar Novamente
            </button>
          </div>
        )}

        {/* Lista de cards */}
        {!loading && !error && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {lists.length === 0 ? (
              <div className="col-span-full text-center py-12">
                <p className="text-gray-500 text-lg mb-4">
                  Você ainda não tem listas. Crie sua primeira lista!
                </p>
                <button
                  onClick={() => setIsModalOpen(true)}
                  className="px-6 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
                >
                  + Criar Primeira Lista
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
  );
};
