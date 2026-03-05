import React, { useState, useEffect, useCallback } from 'react';

interface EditListNameModalProps {
  isOpen: boolean;
  listName: string;
  onClose: () => void;
  onSave: (newName: string) => Promise<void>;
  isSaving?: boolean;
}

const MIN_NAME_LENGTH = 3;
const MAX_NAME_LENGTH = 100;

/**
 * Modal para edição do nome da lista
 * AC3: Modal com campo de texto, validação, contador de caracteres
 * AC4: Estados de validação, botões habilitados/desabilitados
 */
export const EditListNameModal: React.FC<EditListNameModalProps> = ({
  isOpen,
  listName,
  onClose,
  onSave,
  isSaving = false,
}) => {
  const [editedName, setEditedName] = useState(listName);
  const [validationError, setValidationError] = useState<string | null>(null);

  // Reset state quando o modal abre
  useEffect(() => {
    if (isOpen) {
      setEditedName(listName);
      setValidationError(null);
    }
  }, [isOpen, listName]);

  // Focus e selecionar texto quando abre
  useEffect(() => {
    if (isOpen) {
      const input = document.getElementById('list-name-input') as HTMLInputElement;
      if (input) {
        input.focus();
        input.select();
      }
    }
  }, [isOpen]);

  // Handler para tecla ESC
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    },
    [onClose]
  );

  // Validar nome
  const validateName = (name: string): string | null => {
    const trimmed = name.trim();
    if (trimmed.length === 0) {
      return 'Nome é obrigatório';
    }
    if (trimmed.length < MIN_NAME_LENGTH) {
      return `Nome deve ter pelo menos ${MIN_NAME_LENGTH} caracteres`;
    }
    if (trimmed.length > MAX_NAME_LENGTH) {
      return `Nome deve ter no máximo ${MAX_NAME_LENGTH} caracteres`;
    }
    return null;
  };

  // Handler para mudança no input
  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    // Limitar a 100 caracteres no input
    if (value.length <= MAX_NAME_LENGTH) {
      setEditedName(value);
      setValidationError(validateName(value));
    }
  };

  // Handler para salvar
  const handleSave = async () => {
    const trimmedName = editedName.trim();
    const error = validateName(trimmedName);

    if (error) {
      setValidationError(error);
      return;
    }

    // Não salvar se o nome não mudou
    if (trimmedName === listName.trim()) {
      onClose();
      return;
    }

    try {
      await onSave(trimmedName);
      onClose();
    } catch (err) {
      // AC4: Modal deve fechar em erro 403 (permissão negada)
      const errorMessage = err instanceof Error ? err.message : '';
      if (errorMessage.includes('permissão')) {
        onClose();
      }
      // Outros erros: modal fica aberto para retry
    }
  };

  // Handler para tecla Enter no input
  const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSave();
    }
  };

  // Se não está aberto, não renderiza nada
  if (!isOpen) {
    return null;
  }

  const trimmedName = editedName.trim();
  const isNameValid = !validateName(trimmedName);
  const isNameChanged = trimmedName !== listName.trim();
  const canSave = isNameValid && isNameChanged && !isSaving;
  const charCount = editedName.length;
  const isMaxLength = charCount >= MAX_NAME_LENGTH;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50"
      onClick={onClose}
      onKeyDown={handleKeyDown}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div
        className="nl-card mx-4 w-full max-w-md p-6"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <h2
          id="modal-title"
          className="mb-4 font-display text-xl font-bold text-slate-900"
        >
          Editar Nome da Lista
        </h2>

        {/* Input field */}
        <div className="mb-4">
          <label
            htmlFor="list-name-input"
            className="mb-2 block text-sm font-medium text-slate-700"
          >
            Nome da lista
          </label>
          <input
            id="list-name-input"
            type="text"
            name="listName"
            value={editedName}
            onChange={handleNameChange}
            onKeyDown={handleInputKeyDown}
            placeholder="Nome da lista…"
            disabled={isSaving}
            className={`w-full rounded-xl border px-4 py-2.5 transition-colors focus-visible:ring-2 focus-visible:ring-orange-300 ${
              validationError
                ? 'border-red-500'
                : 'border-orange-200 focus:border-orange-400'
            } ${isSaving ? 'cursor-not-allowed bg-slate-100' : ''}`}
            aria-invalid={!!validationError}
            aria-describedby={
              validationError ? 'name-error' : 'name-counter'
            }
            autoComplete="off"
          />

          {/* Contador de caracteres */}
          <div className="flex justify-between mt-1">
            {validationError ? (
              <span id="name-error" className="text-sm text-red-600" role="alert">
                {validationError}
              </span>
            ) : (
              <span></span>
            )}
            <span
              id="name-counter"
              className={`text-sm ${
                isMaxLength ? 'font-medium text-red-600' : 'text-slate-500'
              }`}
            >
              {charCount}/{MAX_NAME_LENGTH}
            </span>
          </div>
        </div>

        {/* Buttons */}
        <div className="flex gap-3 justify-end">
          <button
            onClick={onClose}
            disabled={isSaving}
            className="min-h-[44px] min-w-[44px] rounded-xl bg-slate-100 px-4 py-2 font-medium text-slate-700 transition-colors hover:bg-slate-200 focus-visible:ring-2 focus-visible:ring-orange-300 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            disabled={!canSave}
            className={`min-h-[44px] min-w-[44px] rounded-xl px-4 py-2 font-medium transition-colors ${
              canSave
                ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white hover:from-orange-600 hover:to-amber-600 focus-visible:ring-2 focus-visible:ring-orange-300'
                : 'cursor-not-allowed bg-orange-200 text-white'
            }`}
          >
            {isSaving ? (
              <span className="flex items-center gap-2">
                <svg
                  className="animate-spin h-4 w-4"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                Salvando…
              </span>
            ) : (
              'Salvar'
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
