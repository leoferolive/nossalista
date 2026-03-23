import { useEffect, useRef, useState } from 'react'
import { useNotificationContext } from '../contexts/NotificationContext'
import { useIsMobile } from '../hooks/useIsMobile'
import { ResponsiveSheet } from './ResponsiveSheet'

export function NotificationBell() {
  const { notifications, unreadCount, markAllRead, clearAll } = useNotificationContext()
  const isMobile = useIsMobile()
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  const toggleDropdown = () => {
    if (!isOpen) {
      markAllRead()
    }
    setIsOpen((prev) => !prev)
  }

  useEffect(() => {
    if (!isOpen || isMobile) return

    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setIsOpen(false)
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [isMobile, isOpen])

  const content =
    notifications.length === 0 ? (
      <div className="rounded-2xl border border-dashed border-nl-border bg-nl-surface-muted/50 p-4 text-center">
        <p className="font-sans text-sm text-nl-muted">Nenhuma notificação</p>
        <p className="mt-1 font-sans text-xs text-nl-muted/70">
          Você será notificado quando membros alterarem suas listas.
        </p>
      </div>
    ) : (
      <ul className="space-y-2">
        {notifications.map((notification) => (
          <li
            key={notification.id}
            className="rounded-2xl border border-nl-border bg-nl-surface-strong px-4 py-3"
          >
            <p className="font-sans text-sm text-nl-text">{notification.message}</p>
            {notification.timestamp ? (
              <p className="mt-1 font-sans text-xs text-nl-muted">
                {new Date(notification.timestamp).toLocaleTimeString('pt-BR', {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </p>
            ) : null}
          </li>
        ))}
      </ul>
    )

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={toggleDropdown}
        aria-label="Notificações"
        className="relative inline-flex min-h-[44px] min-w-[44px] items-center justify-center rounded-2xl border border-nl-border bg-nl-surface-strong text-nl-muted transition-colors hover:border-nl-border-strong hover:text-nl-text focus-visible:ring-2 focus-visible:ring-nl-accent/40"
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
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>

        {unreadCount > 0 ? (
          <span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-nl-danger font-sans text-[10px] font-bold text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        ) : null}
      </button>

      {isMobile ? (
        <ResponsiveSheet
          isOpen={isOpen}
          onClose={() => setIsOpen(false)}
          title="Notificações"
          description="Atualizações recentes da sua atividade compartilhada."
          footer={
            notifications.length > 0 ? (
              <button type="button" onClick={clearAll} className="nl-btn-secondary w-full">
                Limpar tudo
              </button>
            ) : null
          }
        >
          {content}
        </ResponsiveSheet>
      ) : null}

      {!isMobile && isOpen ? (
        <div className="absolute right-0 top-full z-50 mt-2 w-80 overflow-hidden rounded-2xl border border-nl-border bg-nl-surface shadow-earthen-strong">
          <div className="flex items-center justify-between border-b border-nl-border px-4 py-3">
            <span className="font-sans text-sm font-semibold text-nl-text">Notificações</span>
            {notifications.length > 0 ? (
              <button
                type="button"
                onClick={clearAll}
                className="font-sans text-xs text-nl-muted transition-colors hover:text-nl-text"
              >
                Limpar tudo
              </button>
            ) : null}
          </div>

          <div className="p-3">
            {notifications.length === 0 ? (
              <div className="p-4 text-center">
                <p className="font-sans text-sm text-nl-muted">Nenhuma notificação</p>
                <p className="mt-1 font-sans text-xs text-nl-muted/70">
                  Você será notificado quando membros alterarem suas listas.
                </p>
              </div>
            ) : (
              <ul className="max-h-80 space-y-2 overflow-y-auto">
                {notifications.map((notification) => (
                  <li
                    key={notification.id}
                    className="rounded-2xl border border-nl-border bg-nl-surface-strong px-4 py-3"
                  >
                    <p className="font-sans text-sm text-nl-text">{notification.message}</p>
                    {notification.timestamp ? (
                      <p className="mt-1 font-sans text-xs text-nl-muted">
                        {new Date(notification.timestamp).toLocaleTimeString('pt-BR', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </p>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}
