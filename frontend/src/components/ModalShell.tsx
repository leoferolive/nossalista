import { useCallback, useEffect, useRef } from 'react'

interface ModalShellProps {
  title: string
  eyebrow?: string
  description?: string
  onClose: () => void
  children: React.ReactNode
  size?: 'md' | 'lg'
}

export function ModalShell({
  title,
  eyebrow,
  description,
  onClose,
  children,
  size = 'md',
}: ModalShellProps) {
  const hasRequestedCloseRef = useRef(false)

  const requestClose = useCallback(() => {
    if (hasRequestedCloseRef.current) {
      return
    }

    hasRequestedCloseRef.current = true
    onClose()
  }, [onClose])

  useEffect(() => {
    hasRequestedCloseRef.current = false
  }, [title])

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        requestClose()
      }
    }

    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [requestClose])

  return (
    <div
      className="nl-modal-backdrop animate-fade-in"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          requestClose()
        }
      }}
      role="dialog"
      aria-modal="true"
      aria-label={title}
    >
      <div
        className={[
          'nl-card',
          'nl-modal-panel',
          'animate-scale-in',
          size === 'lg' ? 'nl-modal-panel-lg' : '',
        ]
          .filter(Boolean)
          .join(' ')}
        style={{ overscrollBehavior: 'contain' }}
      >
        <div className="nl-modal-header">
          <div>
            {eyebrow && <p className="nl-kicker">{eyebrow}</p>}
            <h2 className="mt-2 font-display text-3xl font-semibold text-nl-text">{title}</h2>
            {description && (
              <p className="mt-3 max-w-2xl text-sm leading-6 text-nl-muted">{description}</p>
            )}
          </div>

          <button
            type="button"
            onClick={requestClose}
            className="nl-modal-close"
            aria-label="Fechar"
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
                strokeWidth={1.8}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {children}
      </div>
    </div>
  )
}
