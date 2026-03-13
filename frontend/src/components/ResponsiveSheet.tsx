import { useCallback, useEffect, useMemo, useRef, type ReactNode, type TouchEvent } from 'react'

interface ResponsiveSheetProps {
  isOpen: boolean
  onClose: () => void
  title: string
  description?: string
  children: ReactNode
  footer?: ReactNode
}

export function ResponsiveSheet({
  isOpen,
  onClose,
  title,
  description,
  children,
  footer,
}: ResponsiveSheetProps) {
  const panelRef = useRef<HTMLDivElement>(null)
  const touchStartYRef = useRef<number | null>(null)

  const requestClose = useCallback(() => {
    onClose()
  }, [onClose])

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [isOpen])

  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        requestClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [isOpen, requestClose])

  useEffect(() => {
    if (!isOpen) {
      return
    }

    panelRef.current?.focus()
  }, [isOpen])

  const touchHandlers = useMemo(
    () => ({
      onTouchStart: (event: TouchEvent<HTMLDivElement>) => {
        touchStartYRef.current = event.touches[0]?.clientY ?? null
      },
      onTouchEnd: (event: TouchEvent<HTMLDivElement>) => {
        const startY = touchStartYRef.current
        const endY = event.changedTouches[0]?.clientY ?? null
        touchStartYRef.current = null

        if (startY === null || endY === null) {
          return
        }

        if (endY - startY > 60) {
          requestClose()
        }
      },
    }),
    [requestClose]
  )

  if (!isOpen) {
    return null
  }

  return (
    <div
      className="nl-sheet-backdrop animate-fade-in"
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
        ref={panelRef}
        tabIndex={-1}
        className="nl-sheet-panel animate-sheet-in"
        style={{ overscrollBehavior: 'contain' }}
        {...touchHandlers}
      >
        <div className="nl-sheet-handle" aria-hidden="true" />

        <div className="nl-sheet-header">
          <div className="min-w-0">
            <p className="nl-kicker">Menu</p>
            <h2 className="mt-1.5 font-display text-xl font-semibold text-nl-text">{title}</h2>
            {description ? (
              <p className="mt-1.5 text-xs leading-5 text-nl-muted">{description}</p>
            ) : null}
          </div>

          <button
            type="button"
            onClick={requestClose}
            className="nl-modal-close shrink-0"
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

        <div className="nl-sheet-body">{children}</div>

        {footer && <div className="nl-sheet-footer">{footer}</div>}
      </div>
    </div>
  )
}
