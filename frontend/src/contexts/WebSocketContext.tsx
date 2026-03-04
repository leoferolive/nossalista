import React, { createContext, useContext, useReducer, useRef, useCallback } from 'react';
import { Client, StompSubscription } from '@stomp/stompjs';
import { createStompClient } from '../api/websocket';

export type WebSocketStatus = 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED' | 'RECONNECTING';

interface ReconnectNotifications {
  onReconnecting?: () => void;
  onReconnected?: () => void;
}

interface WebSocketState {
  status: WebSocketStatus;
}

interface WebSocketActions {
  connect: (notifications?: ReconnectNotifications) => void;
  disconnect: () => void;
  subscribe: (listId: string, callback: (message: unknown) => void) => void;
  unsubscribe: (listId: string) => void;
  send: (destination: string, body: unknown) => void;
}

interface WebSocketContextType extends WebSocketState, WebSocketActions {}

type WebSocketAction =
  | { type: 'CONNECTING' }
  | { type: 'CONNECTED' }
  | { type: 'DISCONNECTED' }
  | { type: 'RECONNECTING' };

export function getBackoffDelay(attempt: number): number {
  if (attempt === 0) return 0;
  if (attempt === 1) return 2000;
  if (attempt === 2) return 5000;
  return 10000;
}

function webSocketReducer(state: WebSocketState, action: WebSocketAction): WebSocketState {
  switch (action.type) {
    case 'CONNECTING':
      return { ...state, status: 'CONNECTING' };
    case 'CONNECTED':
      return { ...state, status: 'CONNECTED' };
    case 'DISCONNECTED':
      return { ...state, status: 'DISCONNECTED' };
    case 'RECONNECTING':
      return { ...state, status: 'RECONNECTING' };
    default:
      return state;
  }
}

const initialState: WebSocketState = {
  status: 'DISCONNECTED',
};

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

interface PendingSubscription {
  listId: string;
  callback: (message: unknown) => void;
}

export function WebSocketProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(webSocketReducer, initialState);
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map());
  const pendingSubscriptionsRef = useRef<PendingSubscription[]>([]);
  const reconnectAttemptRef = useRef<number>(0);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isVoluntaryDisconnectRef = useRef<boolean>(false);
  const reconnectingToastShownRef = useRef<boolean>(false);
  const reconnectNotificationsRef = useRef<ReconnectNotifications>({});

  const doSubscribe = useCallback((client: Client, listId: string, callback: (message: unknown) => void) => {
    const destination = `/topic/list/${listId}`;
    const subscription = client.subscribe(destination, (stompMessage) => {
      try {
        const parsed = JSON.parse(stompMessage.body);
        callback(parsed);
      } catch {
        callback(stompMessage.body);
      }
    });
    subscriptionsRef.current.set(listId, subscription);
  }, []);

  const doReconnect = useCallback(() => {
    if (isVoluntaryDisconnectRef.current) {
      dispatch({ type: 'DISCONNECTED' });
      return;
    }

    const token = localStorage.getItem('authToken');
    if (!token) {
      dispatch({ type: 'DISCONNECTED' });
      return;
    }

    const client = createStompClient(token);

    client.onConnect = () => {
      const isReconnect = reconnectAttemptRef.current > 0;

      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }

      reconnectAttemptRef.current = 0;
      reconnectingToastShownRef.current = false;
      dispatch({ type: 'CONNECTED' });

      if (isReconnect) {
        reconnectNotificationsRef.current.onReconnected?.();
      }

      // Processar subscrições enfileiradas antes da conexão ser estabelecida.
      // Re-subscrição após reconexão é tratada pelo useEffect em ListView (via mudança de wsStatus).
      pendingSubscriptionsRef.current.forEach(({ listId, callback }) => {
        doSubscribe(client, listId, callback);
      });
      pendingSubscriptionsRef.current = [];
    };

    // Handler compartilhado para onDisconnect e onStompError — evita double-increment
    // do reconnectAttemptRef quando ambos disparam na mesma falha de conexão.
    const handleConnectionLost = () => {
      if (isVoluntaryDisconnectRef.current) {
        isVoluntaryDisconnectRef.current = false;
        dispatch({ type: 'DISCONNECTED' });
        return;
      }

      // Já há um timer agendado (ex: onStompError disparou antes de onDisconnect).
      // Evita double-increment do backoff counter e double-scheduling.
      if (reconnectTimerRef.current !== null) {
        return;
      }

      dispatch({ type: 'RECONNECTING' });
      if (!reconnectingToastShownRef.current) {
        reconnectNotificationsRef.current.onReconnecting?.();
        reconnectingToastShownRef.current = true;
      }

      const delay = getBackoffDelay(reconnectAttemptRef.current);
      reconnectAttemptRef.current += 1;

      reconnectTimerRef.current = setTimeout(() => {
        reconnectTimerRef.current = null;
        doReconnect();
      }, delay);
    };

    client.onDisconnect = handleConnectionLost;

    client.onStompError = (frame) => {
      console.error('[WebSocket] STOMP error:', frame);
      handleConnectionLost();
    };

    clientRef.current = client;
    client.activate();
  }, [doSubscribe]);

  const connect = useCallback((notifications?: ReconnectNotifications) => {
    if (clientRef.current?.connected) {
      return;
    }

    const token = localStorage.getItem('authToken');
    if (!token) {
      console.warn('[WebSocket] Tentativa de connect sem token de autenticação');
      return;
    }

    reconnectNotificationsRef.current = notifications ?? {};
    isVoluntaryDisconnectRef.current = false;

    dispatch({ type: 'CONNECTING' });
    doReconnect();
  }, [doReconnect]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      isVoluntaryDisconnectRef.current = true;

      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }

      subscriptionsRef.current.forEach((sub) => sub.unsubscribe());
      subscriptionsRef.current.clear();
      pendingSubscriptionsRef.current = [];

      reconnectAttemptRef.current = 0;
      reconnectingToastShownRef.current = false;

      clientRef.current.deactivate();
      clientRef.current = null;
      dispatch({ type: 'DISCONNECTED' });
    }
  }, []);

  const subscribe = useCallback((listId: string, callback: (message: unknown) => void) => {
    const client = clientRef.current;

    if (!client?.connected) {
      if (client) {
        // Conexão em andamento — enfileirar para quando conectar
        pendingSubscriptionsRef.current.push({ listId, callback });
        return;
      }
      console.warn('[WebSocket] Tentativa de subscribe sem conexão ativa');
      return;
    }
    doSubscribe(client, listId, callback);
  }, [doSubscribe]);

  const unsubscribe = useCallback((listId: string) => {
    const subscription = subscriptionsRef.current.get(listId);
    if (subscription) {
      subscription.unsubscribe();
      subscriptionsRef.current.delete(listId);
    }
    // Remover também de pendentes caso a conexão ainda não tenha sido estabelecida
    pendingSubscriptionsRef.current = pendingSubscriptionsRef.current.filter(
      (p) => p.listId !== listId,
    );
  }, []);

  const send = useCallback((destination: string, body: unknown) => {
    const client = clientRef.current;
    if (!client?.connected) {
      console.warn('[WebSocket] Tentativa de send sem conexão ativa');
      return;
    }

    client.publish({
      destination,
      body: JSON.stringify(body),
    });
  }, []);

  return (
    <WebSocketContext.Provider
      value={{
        status: state.status,
        connect,
        disconnect,
        subscribe,
        unsubscribe,
        send,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocketContext(): WebSocketContextType {
  const context = useContext(WebSocketContext);
  if (context === undefined) {
    throw new Error('useWebSocketContext must be used within a WebSocketProvider');
  }
  return context;
}
