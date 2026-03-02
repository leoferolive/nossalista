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
  | 'ITEM_CHECKED';

/**
 * Tipo discriminado para mensagens de itens de lista
 * AC6: payload tipado como ListItem
 */
export type ListWebSocketMessage = WebSocketMessage<ListItem> & {
  type: ListWebSocketEventType;
};
