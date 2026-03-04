import { Client, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Cria e configura um cliente STOMP com SockJS para conexão WebSocket.
 * Usa a API v7 do @stomp/stompjs com new Client({...}).
 *
 * @param token JWT token para autenticação WebSocket
 * @returns cliente STOMP configurado (ainda não conectado)
 */
export function createStompClient(token: string): Client {
  const config: StompConfig = {
    webSocketFactory: () => new SockJS(`${API_URL}/ws`),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 0,
    debug: (str: string) => {
      if (import.meta.env.DEV) {
        console.log('[STOMP]', str);
      }
    },
  };

  return new Client(config);
}
