import React, { createContext, useContext, useReducer, useRef, useCallback } from 'react'
import { Client, StompSubscription } from '@stomp/stompjs'
import { createStompClient, getListTopic, WebSocketChannel } from '../api/websocket'
import { getStoredAuthToken } from '../auth/session'

export type WebSocketStatus = 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED' | 'RECONNECTING'

interface ReconnectNotifications {
  onReconnecting?: () => void
  onReconnected?: () => void
}

interface WebSocketState {
  status: WebSocketStatus
}

interface WebSocketActions {
  connect: (notifications?: ReconnectNotifications) => void
  disconnect: () => void
  subscribe: (
    listId: string,
    channel: WebSocketChannel,
    callback: (message: unknown) => void
  ) => void
  unsubscribe: (listId: string, channel: WebSocketChannel) => void
  subscribeTopic: (topic: string, callback: (message: unknown) => void) => void
  unsubscribeTopic: (topic: string) => void
  send: (destination: string, body: unknown) => void
}

interface WebSocketContextType extends WebSocketState, WebSocketActions {}

type WebSocketAction =
  | { type: 'CONNECTING' }
  | { type: 'CONNECTED' }
  | { type: 'DISCONNECTED' }
  | { type: 'RECONNECTING' }

const MAX_RECONNECT_ATTEMPTS = 30

export function getBackoffDelay(attempt: number): number {
  if (attempt === 0) return 0
  if (attempt === 1) return 2000
  if (attempt === 2) return 5000
  return 10000
}

function webSocketReducer(state: WebSocketState, action: WebSocketAction): WebSocketState {
  switch (action.type) {
    case 'CONNECTING':
      return { ...state, status: 'CONNECTING' }
    case 'CONNECTED':
      return { ...state, status: 'CONNECTED' }
    case 'DISCONNECTED':
      return { ...state, status: 'DISCONNECTED' }
    case 'RECONNECTING':
      return { ...state, status: 'RECONNECTING' }
    default:
      return state
  }
}

const initialState: WebSocketState = {
  status: 'DISCONNECTED',
}

export const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined)

interface PendingSubscription {
  key: string
  listId: string
  channel: WebSocketChannel
  callback: (message: unknown) => void
  directTopic?: string
}

function getSubscriptionKey(listId: string, channel: WebSocketChannel): string {
  return `${channel}:${listId}`
}

function isAuthenticationStompError(frame: {
  headers?: Record<string, string | undefined>
}): boolean {
  const message = frame.headers?.message?.toLowerCase() ?? ''
  return (
    message.includes('nao autenticado') ||
    message.includes('não autenticado') ||
    message.includes('acesso negado') ||
    message.includes('token jwt')
  )
}

export function WebSocketProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(webSocketReducer, initialState)
  const clientRef = useRef<Client | null>(null)
  const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map())
  const pendingSubscriptionsRef = useRef<Map<string, PendingSubscription>>(new Map())
  const reconnectAttemptRef = useRef<number>(0)
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const isVoluntaryDisconnectRef = useRef<boolean>(false)
  const reconnectingToastShownRef = useRef<boolean>(false)
  const reconnectNotificationsRef = useRef<ReconnectNotifications>({})
  const isMockMode = import.meta.env.VITE_USE_MOCK_SERVER === 'true'

  const doSubscribe = useCallback(
    (
      client: Client,
      listId: string,
      channel: WebSocketChannel,
      callback: (message: unknown) => void
    ) => {
      const key = getSubscriptionKey(listId, channel)
      if (subscriptionsRef.current.has(key)) {
        return
      }

      const destination = getListTopic(listId, channel)
      const subscription = client.subscribe(destination, (stompMessage) => {
        try {
          const parsed = JSON.parse(stompMessage.body)
          callback(parsed)
        } catch {
          callback(stompMessage.body)
        }
      })
      subscriptionsRef.current.set(key, subscription)
    },
    []
  )

  const doReconnect = useCallback(() => {
    if (isVoluntaryDisconnectRef.current) {
      dispatch({ type: 'DISCONNECTED' })
      return
    }

    if (reconnectAttemptRef.current >= MAX_RECONNECT_ATTEMPTS) {
      console.warn(
        `[WebSocket] Max reconnect attempts (${MAX_RECONNECT_ATTEMPTS}) reached. Giving up.`
      )
      dispatch({ type: 'DISCONNECTED' })
      return
    }

    const token = getStoredAuthToken()
    if (!token) {
      dispatch({ type: 'DISCONNECTED' })
      return
    }

    const client = createStompClient(token)

    client.onConnect = () => {
      const isReconnect = reconnectAttemptRef.current > 0

      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current)
        reconnectTimerRef.current = null
      }

      reconnectAttemptRef.current = 0
      reconnectingToastShownRef.current = false
      dispatch({ type: 'CONNECTED' })

      if (isReconnect) {
        reconnectNotificationsRef.current.onReconnected?.()
      }

      // Processar subscrições enfileiradas antes da conexão ser estabelecida.
      // Re-subscrição após reconexão é tratada pelo useEffect em ListView (via mudança de wsStatus).
      pendingSubscriptionsRef.current.forEach(({ key, listId, channel, callback, directTopic }) => {
        if (directTopic) {
          const subscription = client.subscribe(directTopic, (stompMessage) => {
            try {
              const parsed = JSON.parse(stompMessage.body)
              callback(parsed)
            } catch {
              callback(stompMessage.body)
            }
          })
          subscriptionsRef.current.set(key, subscription)
        } else {
          doSubscribe(client, listId, channel, callback)
        }
      })
      pendingSubscriptionsRef.current.clear()
    }

    // Handler compartilhado para onDisconnect e onStompError — evita double-increment
    // do reconnectAttemptRef quando ambos disparam na mesma falha de conexão.
    const handleConnectionLost = () => {
      if (isVoluntaryDisconnectRef.current) {
        isVoluntaryDisconnectRef.current = false
        dispatch({ type: 'DISCONNECTED' })
        return
      }

      // The broker-side subscriptions are gone after a disconnect, so the
      // local registry must be cleared to allow a clean re-subscribe.
      subscriptionsRef.current.clear()

      // Já há um timer agendado (ex: onStompError disparou antes de onDisconnect).
      // Evita double-increment do backoff counter e double-scheduling.
      if (reconnectTimerRef.current !== null) {
        return
      }

      dispatch({ type: 'RECONNECTING' })
      if (!reconnectingToastShownRef.current) {
        reconnectNotificationsRef.current.onReconnecting?.()
        reconnectingToastShownRef.current = true
      }

      const delay = getBackoffDelay(reconnectAttemptRef.current)
      reconnectAttemptRef.current += 1

      reconnectTimerRef.current = setTimeout(() => {
        reconnectTimerRef.current = null
        doReconnect()
      }, delay)
    }

    client.onDisconnect = handleConnectionLost

    client.onStompError = (frame) => {
      console.error('[WebSocket] STOMP error:', frame)

      if (isAuthenticationStompError(frame)) {
        if (reconnectTimerRef.current) {
          clearTimeout(reconnectTimerRef.current)
          reconnectTimerRef.current = null
        }

        reconnectAttemptRef.current = 0
        reconnectingToastShownRef.current = false
        pendingSubscriptionsRef.current.clear()
        clientRef.current = null
        isVoluntaryDisconnectRef.current = true
        client.deactivate()
        dispatch({ type: 'DISCONNECTED' })
        return
      }

      handleConnectionLost()
    }

    clientRef.current = client
    client.activate()
  }, [doSubscribe])

  const connect = useCallback(
    (notifications?: ReconnectNotifications) => {
      if (isMockMode) {
        reconnectNotificationsRef.current = notifications ?? {}
        dispatch({ type: 'CONNECTED' })
        return
      }

      if (clientRef.current?.connected) {
        return
      }

      const token = getStoredAuthToken()
      if (!token) {
        console.warn('[WebSocket] Tentativa de connect sem token de autenticação')
        return
      }

      reconnectNotificationsRef.current = notifications ?? {}
      isVoluntaryDisconnectRef.current = false

      dispatch({ type: 'CONNECTING' })
      doReconnect()
    },
    [doReconnect, isMockMode]
  )

  const disconnect = useCallback(() => {
    if (isMockMode) {
      dispatch({ type: 'DISCONNECTED' })
      return
    }

    if (clientRef.current) {
      isVoluntaryDisconnectRef.current = true

      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current)
        reconnectTimerRef.current = null
      }

      subscriptionsRef.current.forEach((sub) => sub.unsubscribe())
      subscriptionsRef.current.clear()
      pendingSubscriptionsRef.current.clear()

      reconnectAttemptRef.current = 0
      reconnectingToastShownRef.current = false

      clientRef.current.deactivate()
      clientRef.current = null
      dispatch({ type: 'DISCONNECTED' })
    }
  }, [isMockMode])

  const subscribe = useCallback(
    (listId: string, channel: WebSocketChannel, callback: (message: unknown) => void) => {
      if (isMockMode) {
        pendingSubscriptionsRef.current.set(getSubscriptionKey(listId, channel), {
          key: getSubscriptionKey(listId, channel),
          listId,
          channel,
          callback,
        })
        return
      }

      const client = clientRef.current
      const key = getSubscriptionKey(listId, channel)

      if (subscriptionsRef.current.has(key) || pendingSubscriptionsRef.current.has(key)) {
        return
      }

      if (!client?.connected) {
        if (client) {
          // Conexão em andamento — enfileirar para quando conectar
          pendingSubscriptionsRef.current.set(key, { key, listId, channel, callback })
          return
        }
        console.warn('[WebSocket] Tentativa de subscribe sem conexão ativa')
        return
      }
      doSubscribe(client, listId, channel, callback)
    },
    [doSubscribe, isMockMode]
  )

  const unsubscribe = useCallback((listId: string, channel: WebSocketChannel) => {
    const key = getSubscriptionKey(listId, channel)
    const subscription = subscriptionsRef.current.get(key)
    if (subscription) {
      subscription.unsubscribe()
      subscriptionsRef.current.delete(key)
    }
    // Remover também de pendentes caso a conexão ainda não tenha sido estabelecida
    pendingSubscriptionsRef.current.delete(key)
  }, [])

  const subscribeTopic = useCallback(
    (topic: string, callback: (message: unknown) => void) => {
      if (isMockMode) {
        pendingSubscriptionsRef.current.set(topic, {
          key: topic,
          listId: topic,
          channel: 'notifications',
          callback,
          directTopic: topic,
        })
        return
      }

      const client = clientRef.current

      if (subscriptionsRef.current.has(topic) || pendingSubscriptionsRef.current.has(topic)) {
        return
      }

      if (!client?.connected) {
        if (client) {
          pendingSubscriptionsRef.current.set(topic, {
            key: topic,
            listId: topic,
            channel: 'notifications',
            callback,
            directTopic: topic,
          })
          return
        }
        console.warn('[WebSocket] Tentativa de subscribeTopic sem conexão ativa')
        return
      }

      const subscription = client.subscribe(topic, (stompMessage) => {
        try {
          const parsed = JSON.parse(stompMessage.body)
          callback(parsed)
        } catch {
          callback(stompMessage.body)
        }
      })
      subscriptionsRef.current.set(topic, subscription)
    },
    [isMockMode]
  )

  const unsubscribeTopic = useCallback((topic: string) => {
    const subscription = subscriptionsRef.current.get(topic)
    if (subscription) {
      subscription.unsubscribe()
      subscriptionsRef.current.delete(topic)
    }
    pendingSubscriptionsRef.current.delete(topic)
  }, [])

  const send = useCallback(
    (destination: string, body: unknown) => {
      if (isMockMode) {
        return
      }

      const client = clientRef.current
      if (!client?.connected) {
        console.warn('[WebSocket] Tentativa de send sem conexão ativa')
        return
      }

      client.publish({
        destination,
        body: JSON.stringify(body),
      })
    },
    [isMockMode]
  )

  return (
    <WebSocketContext.Provider
      value={{
        status: state.status,
        connect,
        disconnect,
        subscribe,
        unsubscribe,
        subscribeTopic,
        unsubscribeTopic,
        send,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  )
}

export function useWebSocketContext(): WebSocketContextType {
  const context = useContext(WebSocketContext)
  if (context === undefined) {
    throw new Error('useWebSocketContext must be used within a WebSocketProvider')
  }
  return context
}
