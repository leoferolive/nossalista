import React from 'react';
import { WebSocketStatus } from '../contexts/WebSocketContext';

interface ConnectionStatusIndicatorProps {
  status: WebSocketStatus;
}

export const ConnectionStatusIndicator: React.FC<ConnectionStatusIndicatorProps> = ({ status }) => {
  if (status === 'CONNECTED') {
    return null;
  }

  const styles: Record<Exclude<WebSocketStatus, 'CONNECTED'>, string> = {
    CONNECTING: 'text-teal-700 bg-teal-50 border-teal-200',
    RECONNECTING: 'text-amber-700 bg-amber-50 border-amber-200',
    DISCONNECTED: 'text-red-700 bg-red-50 border-red-200',
  };

  const dots: Record<Exclude<WebSocketStatus, 'CONNECTED'>, string> = {
    CONNECTING: 'bg-teal-400 animate-pulse',
    RECONNECTING: 'bg-amber-400 animate-pulse',
    DISCONNECTED: 'bg-red-500',
  };

  const labels: Record<Exclude<WebSocketStatus, 'CONNECTED'>, string> = {
    CONNECTING: 'Conectando…',
    RECONNECTING: 'Reconectando…',
    DISCONNECTED: 'Offline',
  };

  return (
    <div
      className={`mb-3 inline-flex items-center gap-2 rounded-xl border px-3 py-1.5 text-sm font-medium shadow-sm ${styles[status]}`}
      role="status"
      aria-live="polite"
    >
      <span className={`h-2 w-2 rounded-full ${dots[status]}`} aria-hidden="true" />
      <span>{labels[status]}</span>
    </div>
  );
};
