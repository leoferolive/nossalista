import React, { useEffect, useState } from 'react'

export type ToastType = 'success' | 'error' | 'info'

interface ToastProps {
  message: string
  type: ToastType
  duration?: number
  onClose: () => void
  index?: number
}

const icons: Record<ToastType, React.ReactNode> = {
  success: (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
      <path
        fillRule="evenodd"
        d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z"
        clipRule="evenodd"
      />
    </svg>
  ),
  error: (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
      <path
        fillRule="evenodd"
        d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z"
        clipRule="evenodd"
      />
    </svg>
  ),
  info: (
    <svg className="h-5 w-5 shrink-0" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
      <path
        fillRule="evenodd"
        d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z"
        clipRule="evenodd"
      />
    </svg>
  ),
}

const toastStyles: Record<ToastType, string> = {
  success: 'text-nl-primary',
  error: 'text-nl-danger',
  info: 'text-nl-primary',
}

/**
 * Componente de notificacao toast
 * Usa nl-card-soft + shadow-earthen, SVG icons, stacking e z-[70]
 */
export const Toast: React.FC<ToastProps> = ({
  message,
  type,
  duration = 3000,
  onClose,
  index = 0,
}) => {
  const [exiting, setExiting] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => {
      setExiting(true)
    }, duration - 200)

    return () => clearTimeout(timer)
  }, [duration])

  useEffect(() => {
    if (!exiting) return

    const exitTimer = setTimeout(() => {
      onClose()
    }, 200)

    return () => clearTimeout(exitTimer)
  }, [exiting, onClose])

  return (
    <div
      className={`
        fixed right-4 z-[70]
        nl-card-soft shadow-earthen
        px-4 py-3
        flex items-center gap-3
        max-w-md
        ${exiting ? 'animate-fade-out' : 'animate-slideIn'}
      `}
      style={{ top: `calc(1rem + ${index} * 4.5rem)` }}
      role="status"
      aria-live="polite"
    >
      <span className={toastStyles[type]}>{icons[type]}</span>
      <span className="text-sm font-medium text-nl-text">{message}</span>
      <button
        onClick={() => setExiting(true)}
        className="ml-auto rounded-lg p-1 text-nl-muted transition-colors hover:text-nl-text"
        aria-label="Fechar notificação"
      >
        <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
        </svg>
      </button>
    </div>
  )
}

/**
 * Hook para gerenciar toasts
 */
interface ToastState {
  message: string
  type: ToastType
  id: number
}

export const useToast = () => {
  const [toasts, setToasts] = React.useState<ToastState[]>([])

  const showToast = React.useCallback((message: string, type: ToastType = 'info') => {
    const id = Date.now()
    setToasts((prev) => [...prev, { message, type, id }])
  }, [])

  const removeToast = React.useCallback((id: number) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id))
  }, [])

  return {
    toasts,
    showToast,
    removeToast,
  }
}
