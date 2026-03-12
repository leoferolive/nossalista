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
    CONNECTED: 'text-emerald-700 bg-emerald-50 border-emerald-200',
    CONNECTING: 'text-nl-primary bg-nl-primary/10 border-nl-primary/30',
    RECONNECTING: 'text-amber-700 bg-amber-50 border-amber-200',
    DISCONNECTED: 'text-nl-danger bg-nl-danger/10 border-nl-danger/30',
  }

  const dots: Record<WebSocketStatus, string> = {
    CONNECTED: 'bg-emerald-500',
    CONNECTING: 'bg-nl-primary animate-pulse',
    RECONNECTING: 'bg-amber-400 animate-pulse',
    DISCONNECTED: 'bg-nl-danger/100',
  }

  const labels: Record<WebSocketStatus, string> = {
    CONNECTED: 'Online',
    CONNECTING: 'Conectando…',
    RECONNECTING: 'Reconectando…',
    DISCONNECTED: 'Offline',
  }

  return (
    <div
      className={`inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-sm font-medium shadow-sm ${styles[status]} ${className ?? ''}`}
      data-tour={dataTour}
      role="status"
      aria-live="polite"
    >
      <span className={`h-2 w-2 rounded-full ${dots[status]}`} aria-hidden="true" />
      <span>{labels[status]}</span>
    </div>
  )
}
