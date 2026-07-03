import React, { useCallback, useEffect, useRef } from 'react'

interface DisconnectOAuthModalProps {
  isOpen: boolean
  clientName: string
  onClose: () => void
  onConfirm: () => Promise<void>
  isDisconnecting?: boolean
}

function useDisconnectModalFocus(isOpen: boolean, isBusy: boolean, onClose: () => void) {
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

interface DisconnectModalActionsProps {
  cancelButtonRef: React.RefObject<HTMLButtonElement | null>
  confirmButtonRef: React.RefObject<HTMLButtonElement | null>
  onClose: () => void
  onConfirm: () => void
  isDisconnecting: boolean
}

function DisconnectModalActions({
  cancelButtonRef,
  confirmButtonRef,
  onClose,
  onConfirm,
  isDisconnecting,
}: DisconnectModalActionsProps) {
  return (
    <div className="flex justify-end gap-3">
      <button
        ref={cancelButtonRef}
        onClick={onClose}
        disabled={isDisconnecting}
        className="min-h-[44px] min-w-[44px] rounded-xl bg-nl-surface-strong px-4 py-2 font-medium text-nl-muted transition-colors hover:bg-nl-surface/50 focus-visible:ring-2 focus-visible:ring-nl-accent/30 disabled:cursor-not-allowed disabled:opacity-50"
      >
        Cancelar
      </button>
      <button
        ref={confirmButtonRef}
        onClick={onConfirm}
        disabled={isDisconnecting}
        className="flex min-h-[44px] min-w-[44px] items-center justify-center gap-2 rounded-xl bg-nl-danger px-4 py-2 font-medium text-white transition-colors hover:bg-nl-danger/80 focus-visible:ring-2 focus-visible:ring-nl-danger/40 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {isDisconnecting ? (
          <>
            <span className="nl-spinner-sm" />
            Desconectando…
          </>
        ) : (
          'Desconectar'
        )}
      </button>
    </div>
  )
}

/**
 * Modal de confirmação para desconectar um assistente OAuth (claude.ai,
 * Claude Code) — mesmo padrão de acessibilidade de {@code RevokeTokenModal}
 * (focus trap, auto-focus, ESC para fechar), com cópia própria porque a ação
 * revoga uma família de refresh tokens, não um Personal Access Token.
 */
export const DisconnectOAuthModal: React.FC<DisconnectOAuthModalProps> = ({
  isOpen,
  clientName,
  onClose,
  onConfirm,
  isDisconnecting = false,
}) => {
  const { cancelButtonRef, confirmButtonRef, handleKeyDown } = useDisconnectModalFocus(
    isOpen,
    isDisconnecting,
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
      onClick={isDisconnecting ? undefined : onClose}
      onKeyDown={handleKeyDown}
      role="dialog"
      aria-modal="true"
      aria-labelledby="disconnect-oauth-modal-title"
    >
      <div className="nl-card mx-4 w-full max-w-md p-6" onClick={(e) => e.stopPropagation()}>
        <h2
          id="disconnect-oauth-modal-title"
          className="mb-4 font-display text-xl font-bold text-nl-text"
        >
          Desconectar assistente?
        </h2>

        <div className="mb-6">
          <p className="mb-2 text-nl-muted">
            Tem certeza que deseja desconectar{' '}
            <span className="font-semibold text-nl-text">&ldquo;{clientName}&rdquo;</span>?
          </p>
          <p className="text-nl-danger text-sm font-medium">
            O assistente perde acesso imediatamente e precisará se conectar novamente para voltar a
            usar suas listas.
          </p>
        </div>

        <DisconnectModalActions
          cancelButtonRef={cancelButtonRef}
          confirmButtonRef={confirmButtonRef}
          onClose={onClose}
          onConfirm={handleConfirm}
          isDisconnecting={isDisconnecting}
        />
      </div>
    </div>
  )
}
