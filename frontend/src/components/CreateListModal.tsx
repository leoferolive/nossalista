import React, { useState, useEffect, useRef } from 'react'
import { TypeCard } from './TypeCard'
import { LIST_TYPES } from '../types/List'
import { CreateListRequest } from '../types/List'

interface CreateListModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (listId: string) => void
  onSubmit: (request: CreateListRequest) => Promise<{ id: string }>
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
  const [name, setName] = useState('')
  const [selectedTypeId, setSelectedTypeId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Auto-focus no input quando modal abre
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus()
    }
  }, [isOpen])

  // Reset form quando modal fecha
  useEffect(() => {
    if (!isOpen) {
      setName('')
      setSelectedTypeId(null)
      setError(null)
    }
  }, [isOpen])

  const isNameValid = name.trim().length >= 3
  const isTypeSelected = selectedTypeId !== null
  const isFormValid = isNameValid && isTypeSelected

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) {
      e.preventDefault()
    }

    if (!isFormValid || loading) {
      return
    }

    setLoading(true)
    setError(null)

    try {
      const result = await onSubmit({
        name: name.trim(),
        typeId: selectedTypeId!,
      })

      // Chama onSuccess com ID real da lista criada
      onSuccess(result.id)
    } catch (err) {
      const axiosError = err as {
        response?: { data?: { detail?: string; errors?: Record<string, string> } }
      }
      const fieldErrors = axiosError.response?.data?.errors
      const detail = axiosError.response?.data?.detail

      let errorMessage = 'Erro ao criar lista'
      if (fieldErrors) {
        // Exibe primeiro erro de campo específico (RFC 7807)
        const firstError = Object.values(fieldErrors)[0]
        errorMessage = firstError || detail || 'Erro ao criar lista'
      } else if (detail) {
        errorMessage = detail
      } else if (err instanceof Error) {
        errorMessage = err.message
      }
      setError(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    // Enter no campo de nome submete o form (se válido)
    if (e.key === 'Enter' && isFormValid) {
      e.preventDefault()
      handleSubmit()
    }
    // Escape fecha o modal
    if (e.key === 'Escape') {
      onClose()
    }
  }

  if (!isOpen) {
    return null
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-nl-bg/80 p-4"
      onClick={(e) => {
        // Fecha ao clicar no overlay (fora do modal)
        if (e.target === e.currentTarget) {
          onClose()
        }
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div
        className="nl-card animate-scale-in max-h-[90vh] w-full max-w-2xl overflow-y-auto p-6"
        style={{ overscrollBehavior: 'contain' }}
      >
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h2 id="modal-title" className="font-display text-2xl font-bold text-nl-text">
            Criar Nova Lista
          </h2>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-nl-muted transition-colors hover:text-nl-muted focus-visible:ring-2 focus-visible:ring-nl-accent/30"
            aria-label="Fechar modal"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
            <label htmlFor="list-name" className="mb-2 block text-sm font-medium text-nl-muted">
              Nome da lista
            </label>
            <input
              ref={inputRef}
              id="list-name"
              name="listName"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ex: Mercado semanal…"
              className="w-full rounded-xl border border-nl-border bg-nl-surface-strong px-4 py-2.5 text-nl-text placeholder:text-nl-muted/70 caret-nl-accent focus:border-nl-accent focus-visible:ring-2 focus-visible:ring-nl-accent/30"
              autoComplete="off"
              aria-describedby="name-error"
              aria-invalid={name.length > 0 && !isNameValid}
            />
            {name.length > 0 && !isNameValid && (
              <p id="name-error" className="mt-1 text-sm text-nl-danger" role="alert">
                O nome deve ter no mínimo 3 caracteres
              </p>
            )}
          </div>

          {/* Tipo da lista */}
          <div className="mb-6">
            <label className="mb-3 block text-sm font-medium text-nl-muted">Tipo da lista</label>
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
              className="mb-4 p-3 bg-nl-danger/10 border border-nl-danger/30 rounded-lg"
              role="alert"
              aria-live="polite"
            >
              <p className="text-sm text-nl-danger">{error}</p>
            </div>
          )}

          {/* Botões */}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="rounded-xl bg-nl-surface-strong px-4 py-2 text-nl-muted transition-colors hover:bg-nl-surface/50 focus-visible:ring-2 focus-visible:ring-nl-accent/30 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={!isFormValid || loading}
              className={`
                rounded-xl px-6 py-2 font-medium
                focus-visible:ring-2 focus-visible:ring-nl-accent/30
                ${
                  isFormValid && !loading
                    ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white hover:from-orange-600 hover:to-amber-600'
                    : 'bg-gray-300 text-nl-muted cursor-not-allowed'
                }
              `}
            >
              {loading ? 'Criando…' : 'Criar Lista'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
