import React, { createContext, useContext, useReducer, useRef, useCallback } from 'react';
import { Client, StompSubscription } from '@stomp/stompjs';
import { createStompClient } from '../api/websocket';

export type WebSocketStatus = 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED';

interface WebSocketState {
  status: WebSocketStatus;
}

interface WebSocketActions {
  connect: () => void;
  disconnect: () => void;
  subscribe: (listId: string, callback: (message: unknown) => void) => void;
  unsubscribe: (listId: string) => void;
}

interface WebSocketContextType extends WebSocketState, WebSocketActions {}

type WebSocketAction =
  | { type: 'CONNECTING' }
  | { type: 'CONNECTED' }
  | { type: 'DISCONNECTED' };

function webSocketReducer(state: WebSocketState, action: WebSocketAction): WebSocketState {
  switch (action.type) {
    case 'CONNECTING':
      return { ...state, status: 'CONNECTING' };
    case 'CONNECTED':
      return { ...state, status: 'CONNECTED' };
    case 'DISCONNECTED':
      return { ...state, status: 'DISCONNECTED' };
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

  const connect = useCallback(() => {
    if (clientRef.current?.connected) {
      return;
    }

    const token = localStorage.getItem('authToken');
    if (!token) {
      console.warn('[WebSocket] Tentativa de connect sem token de autenticação');
      return;
    }

    dispatch({ type: 'CONNECTING' });

    const client = createStompClient(token);

    client.onConnect = () => {
      dispatch({ type: 'CONNECTED' });
      // Processar subscriptions pendentes enfileiradas antes da conexão ser estabelecida
      pendingSubscriptionsRef.current.forEach(({ listId, callback }) => {
        doSubscribe(client, listId, callback);
      });
      pendingSubscriptionsRef.current = [];
    };

    client.onDisconnect = () => {
      dispatch({ type: 'DISCONNECTED' });
    };

    client.onStompError = (frame) => {
      console.error('[WebSocket] STOMP error:', frame);
      dispatch({ type: 'DISCONNECTED' });
    };

    clientRef.current = client;
    client.activate();
  }, [doSubscribe]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      subscriptionsRef.current.forEach((sub) => sub.unsubscribe());
      subscriptionsRef.current.clear();
      pendingSubscriptionsRef.current = [];
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

  return (
    <WebSocketContext.Provider
      value={{
        status: state.status,
        connect,
        disconnect,
        subscribe,
        unsubscribe,
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
