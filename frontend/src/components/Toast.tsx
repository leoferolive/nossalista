import React, { useEffect } from 'react';

export type ToastType = 'success' | 'error' | 'info';

interface ToastProps {
  message: string;
  type: ToastType;
  duration?: number;
  onClose: () => void;
}

/**
 * Componente de notificação toast
 * Aparece no topo da tela com animação de 300ms
 */
export const Toast: React.FC<ToastProps> = ({
  message,
  type,
  duration = 3000,
  onClose,
}) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const bgColor = {
    success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
    error: 'border-red-200 bg-red-50 text-red-900',
    info: 'border-teal-200 bg-teal-50 text-teal-900',
  }[type];

  const icon = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
  }[type];

  return (
    <div
      className={`
        fixed top-4 right-4 z-50
        ${bgColor}
        px-5 py-3 rounded-2xl shadow-tropical border
        flex items-center gap-3
        animate-slideIn
        max-w-md
      `}
      role="status"
      aria-live="polite"
    >
      <span className="text-lg font-bold" aria-hidden="true">
        {icon}
      </span>
      <span className="text-sm font-medium">{message}</span>
      <button
        onClick={onClose}
        className="ml-2 rounded-lg p-1 text-current transition-colors hover:bg-black/5 focus-visible:ring-2 focus-visible:ring-orange-400"
        aria-label="Fechar notificação"
      >
        ✕
      </button>
    </div>
  );
};

/**
 * Hook para gerenciar toasts
 */
interface ToastState {
  message: string;
  type: ToastType;
  id: number;
}

export const useToast = () => {
  const [toasts, setToasts] = React.useState<ToastState[]>([]);

  const showToast = React.useCallback(
    (message: string, type: ToastType = 'info') => {
      const id = Date.now();
      setToasts((prev) => [...prev, { message, type, id }]);
    },
    []
  );

  const removeToast = React.useCallback((id: number) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  return {
    toasts,
    showToast,
    removeToast,
  };
};
