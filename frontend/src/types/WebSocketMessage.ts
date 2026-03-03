import { ListItem } from './Item';

/**
 * Interface genérica para mensagens WebSocket (Event-Type Envelope)
 * AC6: campos type, payload, userId, username, timestamp
 */
export interface WebSocketMessage<T> {
  type: string;
  payload: T;
  userId: string;
  username: string;
  timestamp: string;
}

/**
 * Tipos de eventos WebSocket para itens de lista
 */
export type ListWebSocketEventType =
  | 'ITEM_ADDED'
  | 'ITEM_UPDATED'
  | 'ITEM_REMOVED'
  | 'ITEM_CHECKED'
  | 'MEMBER_ONLINE'
  | 'MEMBER_OFFLINE';

export interface MemberOnlinePayload {
  userId: string;
  username: string;
  name: string;
  avatarUrl: string | null;
}

export interface MemberOfflinePayload {
  userId: string;
  username: string;
}

/**
 * Union discriminada por tipo de evento — permite narrowing correto do payload no switch/case
 */
export type ListWebSocketMessage =
  | (WebSocketMessage<ListItem> & { type: 'ITEM_ADDED' | 'ITEM_UPDATED' | 'ITEM_REMOVED' | 'ITEM_CHECKED' })
  | (WebSocketMessage<MemberOnlinePayload> & { type: 'MEMBER_ONLINE' })
  | (WebSocketMessage<MemberOfflinePayload> & { type: 'MEMBER_OFFLINE' });
