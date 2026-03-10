import { ListItem } from './Item'
import websocketEnvelopeContract from '../../../contracts/websocket-envelope-v2.json'

const WEBSOCKET_SCHEMA_VERSION = websocketEnvelopeContract.schemaVersion as number
const SUPPORTED_CHANNELS = websocketEnvelopeContract.channels as Array<
  WebSocketMessage<unknown>['channel']
>
const ACTOR_REQUIRED_EVENT_TYPES = new Set<string>(
  websocketEnvelopeContract.actorRequiredEventTypes as string[]
)
const REVISION_REQUIRED_CHANNELS = new Set<string>(
  websocketEnvelopeContract.revisionRequiredChannels as string[]
)

export interface WebSocketActor {
  id: string
  username: string
}

/**
 * Interface genérica para mensagens WebSocket (Event-Type Envelope)
 * Contrato v2 do envelope WebSocket
 */
export interface WebSocketMessage<T> {
  schemaVersion: number
  eventId: string
  listId: string
  channel: 'items' | 'presence' | 'system' | 'notifications'
  revision?: number
  type: string
  payload: T
  actor?: WebSocketActor
  timestamp: string
}

/**
 * Tipos de eventos WebSocket para itens de lista
 */
export type ListWebSocketEventType =
  | 'ITEM_ADDED'
  | 'ITEM_UPDATED'
  | 'ITEM_REMOVED'
  | 'ITEM_CHECKED'
  | 'LIST_LAYOUT_UPDATED'
  | 'PRESENCE_SNAPSHOT'
  | 'MEMBER_ONLINE'
  | 'MEMBER_OFFLINE'

export interface MemberOnlinePayload {
  userId: string
  username: string
  name: string
  avatarUrl: string | null
}

export interface MemberOfflinePayload {
  userId: string
  username: string
}

export interface PresenceSnapshotPayload {
  members: MemberOnlinePayload[]
}

export interface ListLayoutUpdatedPayload {
  positions: Array<{
    itemId: string
    position: number
  }>
}

export interface ListNameUpdatedPayload {
  listId: string
  oldName: string
  newName: string
}

export interface MemberJoinedPayload {
  userId: string
  username: string
  name: string
  avatarUrl: string | null
  listId: string
  listName: string
}

export interface MemberLeftPayload {
  userId: string
  username: string
  listId: string
  listName: string
  reason: 'LEFT' | 'REMOVED'
}

export type NotificationEventType =
  | 'ITEM_ADDED'
  | 'ITEM_UPDATED'
  | 'ITEM_REMOVED'
  | 'ITEM_CHECKED'
  | 'LIST_NAME_UPDATED'
  | 'MEMBER_JOINED'
  | 'MEMBER_LEFT'
  | 'MEMBER_REMOVED'

export type NotificationWebSocketMessage =
  | (WebSocketMessage<ListItem> & {
      channel: 'notifications'
      type: 'ITEM_ADDED' | 'ITEM_UPDATED' | 'ITEM_REMOVED' | 'ITEM_CHECKED'
    })
  | (WebSocketMessage<ListNameUpdatedPayload> & { channel: 'notifications'; type: 'LIST_NAME_UPDATED' })
  | (WebSocketMessage<MemberJoinedPayload> & { channel: 'notifications'; type: 'MEMBER_JOINED' })
  | (WebSocketMessage<MemberLeftPayload> & {
      channel: 'notifications'
      type: 'MEMBER_LEFT' | 'MEMBER_REMOVED'
    })

/**
 * Union discriminada por tipo de evento — permite narrowing correto do payload no switch/case
 */
export type ListWebSocketMessage =
  | (WebSocketMessage<ListItem> & {
      type: 'ITEM_ADDED' | 'ITEM_UPDATED' | 'ITEM_REMOVED' | 'ITEM_CHECKED'
    })
  | (WebSocketMessage<ListLayoutUpdatedPayload> & { type: 'LIST_LAYOUT_UPDATED' })
  | (WebSocketMessage<PresenceSnapshotPayload> & { type: 'PRESENCE_SNAPSHOT' })
  | (WebSocketMessage<MemberOnlinePayload> & { type: 'MEMBER_ONLINE' })
  | (WebSocketMessage<MemberOfflinePayload> & { type: 'MEMBER_OFFLINE' })

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isString(value: unknown): value is string {
  return typeof value === 'string'
}

function isNullableString(value: unknown): value is string | null {
  return value === null || typeof value === 'string'
}

function isValidRevision(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value > 0
}

function isWebSocketActor(value: unknown): value is WebSocketActor {
  return isObject(value) && isString(value.id) && isString(value.username)
}

function hasListItemShape(value: unknown): value is ListItem {
  return (
    isObject(value) &&
    isString(value.id) &&
    isString(value.name) &&
    typeof value.checked === 'boolean' &&
    typeof value.position === 'number' &&
    isObject(value.createdBy) &&
    isString(value.createdBy.id) &&
    isString(value.createdBy.username) &&
    isString(value.createdBy.name) &&
    isNullableString(value.createdBy.avatarUrl) &&
    (typeof value.quantity === 'number' || value.quantity === null) &&
    isNullableString(value.dueDate) &&
    isNullableString(value.url) &&
    isString(value.createdAt) &&
    isString(value.updatedAt)
  )
}

function isMemberOnlinePayload(value: unknown): value is MemberOnlinePayload {
  return (
    isObject(value) &&
    isString(value.userId) &&
    isString(value.username) &&
    isString(value.name) &&
    isNullableString(value.avatarUrl)
  )
}

function isMemberOfflinePayload(value: unknown): value is MemberOfflinePayload {
  return isObject(value) && isString(value.userId) && isString(value.username)
}

function isPresenceSnapshotPayload(value: unknown): value is PresenceSnapshotPayload {
  return (
    isObject(value) && Array.isArray(value.members) && value.members.every(isMemberOnlinePayload)
  )
}

function isListLayoutUpdatedPayload(value: unknown): value is ListLayoutUpdatedPayload {
  return (
    isObject(value) &&
    Array.isArray(value.positions) &&
    value.positions.every(
      (entry) => isObject(entry) && isString(entry.itemId) && typeof entry.position === 'number'
    )
  )
}

export function parseListWebSocketMessage(raw: unknown): ListWebSocketMessage | null {
  if (!isObject(raw)) {
    return null
  }

  if (
    raw.schemaVersion !== WEBSOCKET_SCHEMA_VERSION ||
    !isString(raw.eventId) ||
    !isString(raw.type) ||
    !isString(raw.listId) ||
    !isString(raw.channel) ||
    !isString(raw.timestamp)
  ) {
    return null
  }

  if (!SUPPORTED_CHANNELS.includes(raw.channel as WebSocketMessage<unknown>['channel'])) {
    return null
  }

  if ('actor' in raw && raw.actor !== undefined && !isWebSocketActor(raw.actor)) {
    return null
  }

  if ('revision' in raw && raw.revision !== undefined && !isValidRevision(raw.revision)) {
    return null
  }

  if (REVISION_REQUIRED_CHANNELS.has(raw.channel) && !isValidRevision(raw.revision)) {
    return null
  }

  if (ACTOR_REQUIRED_EVENT_TYPES.has(raw.type) && !isWebSocketActor(raw.actor)) {
    return null
  }

  switch (raw.type) {
    case 'ITEM_ADDED':
    case 'ITEM_UPDATED':
    case 'ITEM_REMOVED':
    case 'ITEM_CHECKED':
      return hasListItemShape(raw.payload) ? (raw as unknown as ListWebSocketMessage) : null
    case 'LIST_LAYOUT_UPDATED':
      return isListLayoutUpdatedPayload(raw.payload)
        ? (raw as unknown as ListWebSocketMessage)
        : null
    case 'PRESENCE_SNAPSHOT':
      return isPresenceSnapshotPayload(raw.payload)
        ? (raw as unknown as ListWebSocketMessage)
        : null
    case 'MEMBER_ONLINE':
      return isMemberOnlinePayload(raw.payload) ? (raw as unknown as ListWebSocketMessage) : null
    case 'MEMBER_OFFLINE':
      return isMemberOfflinePayload(raw.payload) ? (raw as unknown as ListWebSocketMessage) : null
    default:
      return null
  }
}
