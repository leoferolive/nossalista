import React, { useState } from 'react';
import { CreateListModal } from '../components/CreateListModal';
import { Toast, useToast } from '../components/Toast';
import { useLists } from '../hooks/useLists';

/**
 * Página Home - Exemplo de integração com CreateListModal
 * AC4: Toast, modal fecha, redirecionamento (ou refetch)
 */
export const Home: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { createList, lists, fetchLists } = useLists();
  const { toasts, showToast, removeToast } = useToast();

  const handleCreateList = async (request: {
    name: string;
    typeId: number;
  }) => {
    await createList(request);
  };

  const handleSuccess = () => {
    // AC4: Toast "Lista criada" (success, 300ms animation)
    showToast('Lista criada!', 'success');

    // AC4: Modal fecha
    setIsModalOpen(false);

    // AC4: Refetch listas (ou navegue para lista criada)
    fetchLists();
    // Alternativa: navigate(`/lists/${listId}`) se usar React Router
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

        {/* Lista de cards (placeholder) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {lists.length === 0 ? (
            <div className="col-span-full text-center py-12">
              <p className="text-gray-500 text-lg mb-4">
                Você ainda não tem listas
              </p>
              <button
                onClick={() => setIsModalOpen(true)}
                className="text-blue-600 hover:text-blue-700 font-medium"
              >
                + Criar Primeira Lista
              </button>
            </div>
          ) : (
            lists.map((list) => (
              <div
                key={list.id}
                className="bg-white p-6 rounded-lg shadow hover:shadow-md transition-shadow"
              >
                <h3 className="text-xl font-semibold mb-2">{list.name}</h3>
                <p className="text-gray-600 text-sm">
                  Tipo: {list.type.name}
                </p>
              </div>
            ))
          )}
        </div>

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
