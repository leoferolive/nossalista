import { useWebSocketContext, WebSocketStatus } from '../contexts/WebSocketContext';

interface UseWebSocketReturn {
  status: WebSocketStatus;
  connect: () => void;
  disconnect: () => void;
  subscribe: (listId: string, callback: (message: unknown) => void) => void;
  unsubscribe: (listId: string) => void;
  send: (destination: string, body: unknown) => void;
}

/**
 * Hook para consumir o WebSocketContext.
 * Lança erro se usado fora de um WebSocketProvider.
 *
 * @returns estado de conexão e métodos de subscribe/unsubscribe
 */
export function useWebSocket(): UseWebSocketReturn {
  const { status, connect, disconnect, subscribe, unsubscribe, send } = useWebSocketContext();

  return { status, connect, disconnect, subscribe, unsubscribe, send };
}
