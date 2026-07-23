import { Client, StompConfig } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export type WebSocketChannel = 'items' | 'presence' | 'notifications'

export function getListTopic(listId: string, channel: WebSocketChannel): string {
  return `/topic/list/${listId}/${channel}`
}

export function getUserNotificationsTopic(userId: string): string {
  return `/topic/user/${userId}/notifications`
}

/**
 * Cria um cliente STOMP autenticado pela sessão HttpOnly enviada no handshake
 * SockJS. Nenhum JWT é exposto ao JavaScript ou aos headers STOMP.
 */
export function createStompClient(): Client {
  const config: StompConfig = {
    webSocketFactory: () => new SockJS('/ws'),
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    reconnectDelay: 0,
    debug: (str: string) => {
      if (import.meta.env.DEV) {
        console.log('[STOMP]', str)
      }
    },
  }

  return new Client(config)
}
