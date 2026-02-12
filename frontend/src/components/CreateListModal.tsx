import React, { useState, useEffect, useRef } from 'react';
import { TypeCard } from './TypeCard';
import { LIST_TYPES } from '../types/List';
import { CreateListRequest } from '../types/List';

interface CreateListModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (listId: string) => void;
  onSubmit: (request: CreateListRequest) => Promise<{ id: string }>;
}

/**
 * Modal para criar nova lista
 * AC3: Campo nome, 4 cards visuais, botão desabilitado, Enter submete
 * AC4: Toast success, modal fecha, redireciona
 */
export const CreateListModal: React.FC<CreateListModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  onSubmit,
}) => {
  const [name, setName] = useState('');
  const [selectedTypeId, setSelectedTypeId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Auto-focus no input quando modal abre
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  // Reset form quando modal fecha
  useEffect(() => {
    if (!isOpen) {
      setName('');
      setSelectedTypeId(null);
      setError(null);
    }
  }, [isOpen]);

  const isNameValid = name.trim().length >= 3;
  const isTypeSelected = selectedTypeId !== null;
  const isFormValid = isNameValid && isTypeSelected;

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) {
      e.preventDefault();
    }

    if (!isFormValid || loading) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await onSubmit({
        name: name.trim(),
        typeId: selectedTypeId!,
      });

      // Chama onSuccess com ID real da lista criada
      onSuccess(result.id);
    } catch (err) {
      const axiosError = err as { response?: { data?: { detail?: string; errors?: Record<string, string> } } };
      const fieldErrors = axiosError.response?.data?.errors;
      const detail = axiosError.response?.data?.detail;

      let errorMessage = 'Erro ao criar lista';
      if (fieldErrors) {
        // Exibe primeiro erro de campo específico (RFC 7807)
        const firstError = Object.values(fieldErrors)[0];
        errorMessage = firstError || detail || 'Erro ao criar lista';
      } else if (detail) {
        errorMessage = detail;
      } else if (err instanceof Error) {
        errorMessage = err.message;
      }
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    // Enter no campo de nome submete o form (se válido)
    if (e.key === 'Enter' && isFormValid) {
      e.preventDefault();
      handleSubmit();
    }
    // Escape fecha o modal
    if (e.key === 'Escape') {
      onClose();
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4"
      onClick={(e) => {
        // Fecha ao clicar no overlay (fora do modal)
        if (e.target === e.currentTarget) {
          onClose();
        }
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h2
            id="modal-title"
            className="text-2xl font-bold text-gray-900"
          >
            Criar Nova Lista
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
            aria-label="Fechar modal"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          {/* Nome da lista */}
          <div className="mb-6">
            <label
              htmlFor="list-name"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Nome da lista
            </label>
            <input
              ref={inputRef}
              id="list-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ex: Mercado Semanal"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              aria-describedby="name-error"
              aria-invalid={name.length > 0 && !isNameValid}
            />
            {name.length > 0 && !isNameValid && (
              <p
                id="name-error"
                className="mt-1 text-sm text-red-600"
                role="alert"
              >
                O nome deve ter no mínimo 3 caracteres
              </p>
            )}
          </div>

          {/* Tipo da lista */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-3">
              Tipo da lista
            </label>
            <div
              className="grid grid-cols-2 gap-4 sm:grid-cols-4"
              role="radiogroup"
              aria-label="Selecione o tipo de lista"
            >
              {LIST_TYPES.map((type) => (
                <TypeCard
                  key={type.id}
                  emoji={type.emoji}
                  name={type.name}
                  description={type.description}
                  isSelected={selectedTypeId === type.id}
                  onClick={() => setSelectedTypeId(type.id)}
                />
              ))}
            </div>
          </div>

          {/* Erro */}
          {error && (
            <div
              className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg"
              role="alert"
            >
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          {/* Botões */}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={!isFormValid || loading}
              className={`
                px-6 py-2 rounded-lg font-medium
                focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
                ${
                  isFormValid && !loading
                    ? 'bg-blue-600 text-white hover:bg-blue-700'
                    : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                }
              `}
            >
              {loading ? 'Criando...' : 'Criar Lista'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
