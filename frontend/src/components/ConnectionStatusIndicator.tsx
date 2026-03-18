import React from 'react'
import { WebSocketStatus } from '../contexts/WebSocketContext'

interface ConnectionStatusIndicatorProps {
  status: WebSocketStatus
  dataTour?: string
  className?: string
}

export const ConnectionStatusIndicator: React.FC<ConnectionStatusIndicatorProps> = ({
  status,
  dataTour,
  className,
}) => {
  const styles: Record<WebSocketStatus, string> = {
    CONNECTED: 'text-nl-primary bg-nl-primary/10 border-nl-primary/30',
    CONNECTING: 'text-nl-primary bg-nl-primary/10 border-nl-primary/30',
    RECONNECTING: 'text-nl-accent bg-nl-accent/10 border-nl-accent/30',
    DISCONNECTED: 'text-nl-danger bg-nl-danger/10 border-nl-danger/30',
  }

  const dots: Record<WebSocketStatus, string> = {
    CONNECTED: 'bg-nl-primary',
    CONNECTING: 'bg-nl-primary animate-pulse',
    RECONNECTING: 'bg-nl-accent animate-pulse',
    DISCONNECTED: 'bg-nl-danger',
  }

  const labels: Record<WebSocketStatus, string> = {
    CONNECTED: 'Online',
    CONNECTING: 'Conectando…',
    RECONNECTING: 'Reconectando…',
    DISCONNECTED: 'Offline',
  }

  return (
    <div
      className={`inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-sm font-medium shadow-earthen ${styles[status]} ${className ?? ''}`}
      data-tour={dataTour}
      role="status"
      aria-live="polite"
    >
      <span className={`h-2 w-2 rounded-full ${dots[status]}`} aria-hidden="true" />
      <span>{labels[status]}</span>
    </div>
  )
}
