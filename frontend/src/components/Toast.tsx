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
    success: 'bg-green-500',
    error: 'bg-red-500',
    info: 'bg-blue-500',
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
        ${bgColor} text-white
        px-6 py-3 rounded-lg shadow-lg
        flex items-center gap-3
        animate-slideIn
        max-w-md
      `}
      role="alert"
      aria-live="polite"
    >
      <span className="text-xl font-bold" aria-hidden="true">
        {icon}
      </span>
      <span className="text-sm font-medium">{message}</span>
      <button
        onClick={onClose}
        className="ml-2 text-white hover:text-gray-200 focus:outline-none"
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
