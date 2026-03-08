import React, { useCallback, useEffect, useRef } from 'react'

interface DeleteListModalProps {
  isOpen: boolean
  listName: string
  onClose: () => void
  onConfirm: () => Promise<void>
  isDeleting?: boolean
}

/**
 * Modal de confirmação para exclusão de lista
 * AC3: Modal destrutivo com aviso, confirmação e design defensivo
 * AC4: Estados de loading, tratamento de erros
 * NFR-A4: Focus trap e auto-focus para acessibilidade
 */
export const DeleteListModal: React.FC<DeleteListModalProps> = ({
  isOpen,
  listName,
  onClose,
  onConfirm,
  isDeleting = false,
}) => {
  // Refs para botões (focus trap e auto-focus)
  const cancelButtonRef = useRef<HTMLButtonElement>(null)
  const confirmButtonRef = useRef<HTMLButtonElement>(null)

  // Auto-focus no botão Cancelar quando modal abre (safe default)
  useEffect(() => {
    if (isOpen && cancelButtonRef.current) {
      cancelButtonRef.current.focus()
    }
  }, [isOpen])

  // Handler para tecla ESC (não fecha durante deleção)
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Escape' && !isDeleting) {
        onClose()
      }

      // Focus trap: TAB navega entre Cancelar e Excluir
      if (e.key === 'Tab' && !isDeleting) {
        e.preventDefault()
        if (document.activeElement === cancelButtonRef.current) {
          confirmButtonRef.current?.focus()
        } else {
          cancelButtonRef.current?.focus()
        }
      }
    },
    [onClose, isDeleting]
  )

  // Handler para confirmar exclusão
  const handleConfirm = async () => {
    try {
      await onConfirm()
      // onClose é chamado pelo componente pai após redirecionamento
    } catch {
      // Erros são tratados pelo componente pai (ListView)
      // Modal permanece aberto para retry em erros 500
      // Modal fecha em erros 403/404 (redirecionamento)
    }
  }

  // Se não está aberto, não renderiza nada
  if (!isOpen) {
    return null
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50"
      onClick={isDeleting ? undefined : onClose}
      onKeyDown={handleKeyDown}
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-modal-title"
    >
      <div className="nl-card mx-4 w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
        {/* Header com ícone de alerta */}
        <div className="flex items-center gap-3 mb-4">
          <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-nl-danger/10">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6 text-nl-danger"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          </div>
          <h2 id="delete-modal-title" className="font-display text-xl font-bold text-nl-text">
            Excluir Lista?
          </h2>
        </div>

        {/* Mensagem de confirmação */}
        <div className="mb-6">
          <p className="mb-2 text-nl-text">
            Tem certeza que deseja excluir{' '}
            <span className="font-semibold text-nl-text">&ldquo;{listName}&rdquo;</span>?
          </p>
          <p className="text-nl-danger text-sm font-medium">
            Esta ação não pode ser desfeita. Todos os itens e membros serão removidos
            permanentemente.
          </p>
        </div>

        {/* Buttons */}
        <div className="flex gap-3 justify-end">
          <button
            ref={cancelButtonRef}
            onClick={onClose}
            disabled={isDeleting}
            className="min-h-[44px] min-w-[44px] rounded-xl bg-nl-surface-strong px-4 py-2 font-medium text-nl-text transition-colors hover:bg-nl-surface-strong focus-visible:ring-2 focus-visible:ring-nl-focus/40 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            ref={confirmButtonRef}
            onClick={handleConfirm}
            disabled={isDeleting}
            className="flex min-h-[44px] min-w-[44px] items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-2 font-medium text-white transition-colors hover:bg-red-700 focus-visible:ring-2 focus-visible:ring-nl-danger/40 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isDeleting ? (
              <>
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
                Excluindo…
              </>
            ) : (
              'Excluir'
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
