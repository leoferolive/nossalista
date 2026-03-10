import React, { createContext, useContext, useReducer, useEffect, useCallback } from 'react'
import { useWebSocketContext } from './WebSocketContext'
import { getUserNotificationsTopic } from '../api/websocket'
import type {
  WebSocketActor,
  ListNameUpdatedPayload,
  MemberJoinedPayload,
  MemberLeftPayload,
} from '../types/WebSocketMessage'
import type { ListItem } from '../types/Item'

const MAX_NOTIFICATIONS = 50

export interface AppNotification {
  id: string
  type: string
  listId: string
  message: string
  actor?: WebSocketActor
  timestamp: string
  read: boolean
}

interface NotificationState {
  notifications: AppNotification[]
}

type NotificationAction =
  | { type: 'ADD'; notification: AppNotification }
  | { type: 'MARK_ALL_READ' }
  | { type: 'CLEAR_ALL' }

function notificationReducer(
  state: NotificationState,
  action: NotificationAction
): NotificationState {
  switch (action.type) {
    case 'ADD': {
      const updated = [action.notification, ...state.notifications]
      return { notifications: updated.slice(0, MAX_NOTIFICATIONS) }
    }
    case 'MARK_ALL_READ':
      return {
        notifications: state.notifications.map((n) => ({ ...n, read: true })),
      }
    case 'CLEAR_ALL':
      return { notifications: [] }
    default:
      return state
  }
}

interface NotificationContextType {
  notifications: AppNotification[]
  unreadCount: number
  markAllRead: () => void
  clearAll: () => void
}

const NotificationCtx = createContext<NotificationContextType | undefined>(undefined)

function formatMessage(type: string, payload: unknown, actor: WebSocketActor): string {
  const actorName = actor?.username ?? 'Alguém'

  switch (type) {
    case 'ITEM_ADDED': {
      const p = payload as ListItem
      return `${actorName} adicionou "${p.name}"`
    }
    case 'ITEM_UPDATED': {
      const p = payload as ListItem
      return `${actorName} editou "${p.name}"`
    }
    case 'ITEM_REMOVED': {
      const p = payload as ListItem
      return `${actorName} removeu "${p.name}"`
    }
    case 'ITEM_CHECKED': {
      const p = payload as ListItem
      return p.checked
        ? `${actorName} marcou "${p.name}" como concluído`
        : `${actorName} desmarcou "${p.name}"`
    }
    case 'LIST_NAME_UPDATED': {
      const p = payload as ListNameUpdatedPayload
      return `${actorName} renomeou a lista para "${p.newName}"`
    }
    case 'MEMBER_JOINED': {
      const p = payload as MemberJoinedPayload
      return `${p.username} entrou na lista`
    }
    case 'MEMBER_LEFT': {
      const p = payload as MemberLeftPayload
      return `${p.username} saiu da lista`
    }
    case 'MEMBER_REMOVED': {
      const p = payload as MemberLeftPayload
      return `${p.username} foi removido da lista`
    }
    default:
      return 'Nova atividade na lista'
  }
}

interface NotificationProviderProps {
  children: React.ReactNode
  userId: string
}

export function NotificationProvider({ children, userId }: NotificationProviderProps) {
  const [state, dispatch] = useReducer(notificationReducer, { notifications: [] })
  const { subscribeTopic, unsubscribeTopic } = useWebSocketContext()

  const handleMessage = useCallback(
    (raw: unknown) => {
      if (typeof raw !== 'object' || raw === null) return
      const msg = raw as Record<string, unknown>

      if (msg.channel !== 'notifications') return

      const type = msg.type as string
      const payload = msg.payload
      const actor = msg.actor as WebSocketActor | undefined
      const listId = msg.listId as string
      const timestamp = msg.timestamp as string
      const eventId = msg.eventId as string

      const message = formatMessage(type, payload, actor ?? { id: '', username: 'Alguém' })

      dispatch({
        type: 'ADD',
        notification: {
          id: eventId ?? crypto.randomUUID(),
          type,
          listId,
          message,
          actor,
          timestamp,
          read: false,
        },
      })
    },
    []
  )

  useEffect(() => {
    if (!userId) return
    const topic = getUserNotificationsTopic(userId)
    subscribeTopic(topic, handleMessage)
    return () => {
      unsubscribeTopic(topic)
    }
  }, [userId, subscribeTopic, unsubscribeTopic, handleMessage])

  const markAllRead = useCallback(() => dispatch({ type: 'MARK_ALL_READ' }), [])
  const clearAll = useCallback(() => dispatch({ type: 'CLEAR_ALL' }), [])

  const unreadCount = state.notifications.filter((n) => !n.read).length

  return (
    <NotificationCtx.Provider value={{ notifications: state.notifications, unreadCount, markAllRead, clearAll }}>
      {children}
    </NotificationCtx.Provider>
  )
}

export function useNotificationContext(): NotificationContextType {
  const ctx = useContext(NotificationCtx)
  if (ctx === undefined) {
    throw new Error('useNotificationContext must be used within a NotificationProvider')
  }
  return ctx
}
