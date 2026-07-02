import React, { useCallback, useEffect, useRef } from 'react'

interface RevokeTokenModalProps {
  isOpen: boolean
  tokenName: string
  onClose: () => void
  onConfirm: () => Promise<void>
  isRevoking?: boolean
}

function useRevokeModalFocus(isOpen: boolean, isBusy: boolean, onClose: () => void) {
  const cancelButtonRef = useRef<HTMLButtonElement>(null)
  const confirmButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (isOpen && cancelButtonRef.current) {
      cancelButtonRef.current.focus()
    }
  }, [isOpen])

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (isBusy) return

      if (e.key === 'Escape') {
        onClose()
      }

      if (e.key === 'Tab') {
        e.preventDefault()
        if (document.activeElement === cancelButtonRef.current) {
          confirmButtonRef.current?.focus()
        } else {
          cancelButtonRef.current?.focus()
        }
      }
    },
    [isBusy, onClose]
  )

  return { cancelButtonRef, confirmButtonRef, handleKeyDown }
}

function RevokeWarningIcon() {
  return (
    <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-nl-danger/15">
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
  )
}

interface RevokeModalActionsProps {
  cancelButtonRef: React.RefObject<HTMLButtonElement | null>
  confirmButtonRef: React.RefObject<HTMLButtonElement | null>
  onClose: () => void
  onConfirm: () => void
  isRevoking: boolean
}

function RevokeModalActions({
  cancelButtonRef,
  confirmButtonRef,
  onClose,
  onConfirm,
  isRevoking,
}: RevokeModalActionsProps) {
  return (
    <div className="flex gap-3 justify-end">
      <button
        ref={cancelButtonRef}
        onClick={onClose}
        disabled={isRevoking}
        className="min-h-[44px] min-w-[44px] rounded-xl bg-nl-surface-strong px-4 py-2 font-medium text-nl-muted transition-colors hover:bg-nl-surface/50 focus-visible:ring-2 focus-visible:ring-nl-accent/30 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Cancelar
      </button>
      <button
        ref={confirmButtonRef}
        onClick={onConfirm}
        disabled={isRevoking}
        className="flex min-h-[44px] min-w-[44px] items-center justify-center gap-2 rounded-xl bg-nl-danger px-4 py-2 font-medium text-white transition-colors hover:bg-nl-danger/80 focus-visible:ring-2 focus-visible:ring-nl-danger/40 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {isRevoking ? (
          <>
            <span className="nl-spinner-sm" />
            Revogando…
          </>
        ) : (
          'Revogar'
        )}
      </button>
    </div>
  )
}

/**
 * Modal de confirmação para revogação de um Personal Access Token.
 * Segue o mesmo padrão de {@code DeleteListModal} (focus trap, auto-focus,
 * ESC para fechar) para consistência de acessibilidade entre confirmações
 * destrutivas do app.
 */
export const RevokeTokenModal: React.FC<RevokeTokenModalProps> = ({
  isOpen,
  tokenName,
  onClose,
  onConfirm,
  isRevoking = false,
}) => {
  const { cancelButtonRef, confirmButtonRef, handleKeyDown } = useRevokeModalFocus(
    isOpen,
    isRevoking,
    onClose
  )

  const handleConfirm = async () => {
    try {
      await onConfirm()
    } catch {
      // Erros são tratados pelo componente pai (toast); modal permanece aberto para retry.
    }
  }

  if (!isOpen) {
    return null
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-nl-bg/80"
      onClick={isRevoking ? undefined : onClose}
      onKeyDown={handleKeyDown}
      role="dialog"
      aria-modal="true"
      aria-labelledby="revoke-token-modal-title"
    >
      <div className="nl-card mx-4 w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-3 mb-4">
          <RevokeWarningIcon />
          <h2 id="revoke-token-modal-title" className="font-display text-xl font-bold text-nl-text">
            Revogar token?
          </h2>
        </div>

        <div className="mb-6">
          <p className="mb-2 text-nl-muted">
            Tem certeza que deseja revogar{' '}
            <span className="font-semibold text-nl-text">&ldquo;{tokenName}&rdquo;</span>?
          </p>
          <p className="text-nl-danger text-sm font-medium">
            Qualquer cliente MCP/API que use este token para de funcionar imediatamente. Esta ação
            não pode ser desfeita.
          </p>
        </div>

        <RevokeModalActions
          cancelButtonRef={cancelButtonRef}
          confirmButtonRef={confirmButtonRef}
          onClose={onClose}
          onConfirm={handleConfirm}
          isRevoking={isRevoking}
        />
      </div>
    </div>
  )
}
