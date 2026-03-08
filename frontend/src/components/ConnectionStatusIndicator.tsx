import React from 'react'
import { WebSocketStatus } from '../contexts/WebSocketContext'

interface ConnectionStatusIndicatorProps {
  status: WebSocketStatus
}

export const ConnectionStatusIndicator: React.FC<ConnectionStatusIndicatorProps> = ({ status }) => {
  if (status === 'CONNECTED') {
    return null
  }

  const styles: Record<Exclude<WebSocketStatus, 'CONNECTED'>, string> = {
    CONNECTING: 'text-nl-primary bg-nl-primary/15 border-nl-border/20',
    RECONNECTING: 'text-nl-accent bg-nl-bg-soft border-nl-border/20',
    DISCONNECTED: 'text-nl-danger bg-nl-danger/10 border-nl-danger/30',
  }

  const dots: Record<Exclude<WebSocketStatus, 'CONNECTED'>, string> = {
    CONNECTING: 'bg-teal-400 animate-pulse',
    RECONNECTING: 'bg-amber-400 animate-pulse',
    DISCONNECTED: 'bg-red-500',
  }

  const labels: Record<Exclude<WebSocketStatus, 'CONNECTED'>, string> = {
    CONNECTING: 'Conectando…',
    RECONNECTING: 'Reconectando…',
    DISCONNECTED: 'Offline',
  }

  return (
    <div
      className={`mb-3 inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-sm font-medium shadow-sm ${styles[status]}`}
      role="status"
      aria-live="polite"
    >
      <span className={`h-2 w-2 rounded-full ${dots[status]}`} aria-hidden="true" />
      <span>{labels[status]}</span>
    </div>
  )
}
