import { useEffect, useRef } from 'react'

interface DeleteConfirmModalProps {
  isOpen: boolean
  itemName: string
  onConfirm: () => void
  onCancel: () => void
}

/**
 * Modal de confirmação para remoção de item
 * Exibe o nome do item e pede confirmação antes de deletar
 */
export function DeleteConfirmModal({
  isOpen,
  itemName,
  onConfirm,
  onCancel,
}: DeleteConfirmModalProps) {
  const cancelButtonRef = useRef<HTMLButtonElement>(null)

  // Focar no botão de cancelar quando abrir (safer default)
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        cancelButtonRef.current?.focus()
      }, 100)
    }
  }, [isOpen])

  // Fechar ao pressionar Escape
  useEffect(() => {
    if (!isOpen) return

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onCancel()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [isOpen, onCancel])

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-nl-bg/80 animate-fade-in"
      onClick={onCancel}
      data-testid="delete-confirm-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-item-modal-title"
    >
      <div
        className="nl-card mx-4 w-full max-w-md animate-scale-in p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          id="delete-item-modal-title"
          className="mb-2 font-display text-xl font-semibold text-nl-text"
        >
          Remover item?
        </h2>
        <p className="mb-6 text-nl-muted">
          Tem certeza que deseja remover{' '}
          <strong className="text-nl-text">&apos;{itemName}&apos;</strong>?
        </p>

        <div className="flex gap-2 justify-end">
          <button
            ref={cancelButtonRef}
            onClick={onCancel}
            className="min-h-[44px] rounded-xl border border-nl-border px-4 py-2 text-nl-muted transition-colors hover:bg-nl-surface-strong focus-visible:ring-2 focus-visible:ring-nl-accent/30"
            data-testid="delete-cancel-button"
          >
            Cancelar
          </button>
          <button
            onClick={onConfirm}
            className="min-h-[44px] rounded-xl bg-nl-danger px-4 py-2 text-white transition-colors hover:bg-nl-danger/80 focus-visible:ring-2 focus-visible:ring-nl-danger/40"
            data-testid="delete-confirm-button"
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  )
}
