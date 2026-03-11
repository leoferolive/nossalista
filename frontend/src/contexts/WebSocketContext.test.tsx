import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import { WebSocketProvider, getBackoffDelay, useWebSocketContext } from './WebSocketContext'
import { createStompClient } from '../api/websocket'

vi.mock('../api/websocket', () => ({
  createStompClient: vi.fn(),
  getListTopic: vi.fn((listId: string, channel: string) => `/topic/list/${listId}/${channel}`),
  getUserNotificationsTopic: vi.fn((userId: string) => `/topic/user/${userId}/notifications`),
}))

interface MockClient {
  connected: boolean
  activate: ReturnType<typeof vi.fn>
  deactivate: ReturnType<typeof vi.fn>
  subscribe: ReturnType<typeof vi.fn>
  publish: ReturnType<typeof vi.fn>
  onConnect?: () => void
  onDisconnect?: () => void
  onStompError?: (frame: unknown) => void
}

const createdClients: MockClient[] = []
let latestContext: ReturnType<typeof useWebSocketContext> | null = null

function createMockClient(): MockClient {
  const client: MockClient = {
    connected: false,
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn(() => ({ unsubscribe: vi.fn() })),
    publish: vi.fn(),
  }
  createdClients.push(client)
  return client
}

function TestConsumer() {
  const context = useWebSocketContext()
  latestContext = context

  return <div>{context.status}</div>
}

describe('WebSocketContext', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    localStorage.setItem('authToken', 'token-teste')
    createdClients.length = 0
    latestContext = null
    vi.mocked(createStompClient).mockImplementation(() => createMockClient() as never)
  })

  afterEach(() => {
    vi.useRealTimers()
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('desconexao nao voluntaria muda para RECONNECTING e mostra notificacao uma vez', () => {
    const onReconnecting = vi.fn()

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({ onReconnecting })
    })

    act(() => {
      createdClients[0].onDisconnect?.()
    })

    expect(screen.getByText('RECONNECTING')).toBeInTheDocument()
    expect(onReconnecting).toHaveBeenCalledTimes(1)

    act(() => {
      vi.advanceTimersByTime(0)
    })

    act(() => {
      createdClients[1].onDisconnect?.()
    })

    expect(onReconnecting).toHaveBeenCalledTimes(1)

    act(() => {
      vi.advanceTimersByTime(1999)
    })
    expect(createdClients).toHaveLength(2)

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(createdClients).toHaveLength(3)
  })

  it('reconexao bem-sucedida volta para CONNECTED, notifica e reseta contadores', () => {
    const onReconnecting = vi.fn()
    const onReconnected = vi.fn()

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({ onReconnecting, onReconnected })
    })

    act(() => {
      createdClients[0].onDisconnect?.()
      vi.advanceTimersByTime(0)
      createdClients[1].onConnect?.()
    })

    expect(screen.getByText('CONNECTED')).toBeInTheDocument()
    expect(onReconnecting).toHaveBeenCalledTimes(1)
    expect(onReconnected).toHaveBeenCalledTimes(1)

    act(() => {
      createdClients[1].onDisconnect?.()
    })

    act(() => {
      vi.advanceTimersByTime(0)
    })

    expect(createdClients).toHaveLength(3)
  })

  it('disconnect voluntario nao deve reconectar automaticamente', () => {
    const onReconnecting = vi.fn()

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({ onReconnecting })
    })

    act(() => {
      latestContext?.disconnect()
      createdClients[0].onDisconnect?.()
      vi.runOnlyPendingTimers()
    })

    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument()
    expect(onReconnecting).not.toHaveBeenCalled()
    expect(createdClients).toHaveLength(1)
  })

  it('getBackoffDelay retorna 0, 2000, 5000, 10000, 10000', () => {
    expect(getBackoffDelay(0)).toBe(0)
    expect(getBackoffDelay(1)).toBe(2000)
    expect(getBackoffDelay(2)).toBe(5000)
    expect(getBackoffDelay(3)).toBe(10000)
    expect(getBackoffDelay(10)).toBe(10000)
  })

  it('onStompError nao voluntario muda para RECONNECTING e mostra notificacao uma vez', () => {
    const onReconnecting = vi.fn()

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({ onReconnecting })
    })

    act(() => {
      createdClients[0].onStompError?.({})
    })

    expect(screen.getByText('RECONNECTING')).toBeInTheDocument()
    expect(onReconnecting).toHaveBeenCalledTimes(1)

    // Segunda falha (após timer disparar) não deve repetir o toast
    act(() => {
      vi.advanceTimersByTime(0)
    })

    act(() => {
      createdClients[1].onStompError?.({})
    })

    expect(onReconnecting).toHaveBeenCalledTimes(1)
    expect(createdClients).toHaveLength(2)
  })

  it('onStompError e onDisconnect na mesma falha nao devem double-incrementar o backoff', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    // Simula onStompError seguido de onDisconnect (ambos disparam na mesma falha)
    act(() => {
      createdClients[0].onStompError?.({})
      createdClients[0].onDisconnect?.()
    })

    // reconnectAttemptRef deve ter sido incrementado apenas uma vez → delay 0ms
    act(() => {
      vi.advanceTimersByTime(0)
    })

    // Apenas um novo cliente deve ter sido criado (não dois)
    expect(createdClients).toHaveLength(2)
  })

  it('erro STOMP de autenticacao nao deve entrar em loop de reconexao', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    act(() => {
      createdClients[0].onStompError?.({
        headers: {
          message: 'Usuário não autenticado para subscribe em: /topic/list/abc',
        },
      })
      vi.runOnlyPendingTimers()
    })

    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument()
    expect(createdClients[0].deactivate).toHaveBeenCalledTimes(1)
    expect(createdClients).toHaveLength(1)
  })

  it('subscribe com client conectado assina e dispara callback ao receber mensagem', () => {
    const callback = vi.fn()
    let capturedHandler: ((msg: { body: string }) => void) | null = null
    createdClients.length = 0
    vi.mocked(createStompClient).mockImplementation(() => {
      const c = createMockClient()
      c.subscribe = vi.fn((_, handler) => {
        capturedHandler = handler
        return { unsubscribe: vi.fn() }
      })
      return c as never
    })

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
      createdClients[0].connected = true
      createdClients[0].onConnect?.()
    })

    act(() => {
      latestContext?.subscribe('list-abc', 'items', callback)
    })

    expect(createdClients[0].subscribe).toHaveBeenCalled()

    // Cobre o inner callback (JSON válido)
    act(() => {
      capturedHandler?.({ body: JSON.stringify({ type: 'ITEM_ADDED' }) })
    })
    expect(callback).toHaveBeenCalledWith({ type: 'ITEM_ADDED' })

    // Cobre o bloco catch (JSON inválido)
    act(() => {
      capturedHandler?.({ body: 'raw-text' })
    })
    expect(callback).toHaveBeenCalledWith('raw-text')
  })

  it('disconnect cancela subscrições ativas antes de desativar', () => {
    const mockUnsub = vi.fn()
    createdClients.length = 0
    vi.mocked(createStompClient).mockImplementation(() => {
      const c = createMockClient()
      c.subscribe = vi.fn(() => ({ unsubscribe: mockUnsub }))
      return c as never
    })

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
      createdClients[0].connected = true
      createdClients[0].onConnect?.()
    })

    // Criar uma subscrição ativa
    act(() => {
      latestContext?.subscribe('list-xyz', 'items', vi.fn())
    })

    // Disconnect deve chamar unsubscribe em todas as subscrições
    act(() => {
      latestContext?.disconnect()
    })

    expect(mockUnsub).toHaveBeenCalled()
    expect(createdClients[0].deactivate).toHaveBeenCalled()
  })

  it('send publica mensagem quando conectado', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
      createdClients[0].connected = true
      createdClients[0].onConnect?.()
    })

    act(() => {
      latestContext?.send('/app/list/abc/items', { name: 'Leite' })
    })

    expect(createdClients[0].publish).toHaveBeenCalledWith({
      destination: '/app/list/abc/items',
      body: JSON.stringify({ name: 'Leite' }),
    })
  })

  it('subscribeTopic enfileira quando nao conectado e assina apos onConnect, disparando callback', () => {
    const callback = vi.fn()
    let capturedDirectHandler: ((msg: { body: string }) => void) | null = null
    createdClients.length = 0
    vi.mocked(createStompClient).mockImplementation(() => {
      const c = createMockClient()
      c.subscribe = vi.fn((_, handler) => {
        capturedDirectHandler = handler
        return { unsubscribe: vi.fn() }
      })
      return c as never
    })

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    // Antes de onConnect: enfileira subscrição direta
    act(() => {
      latestContext?.subscribeTopic('/topic/user/u1/notifications', callback)
    })

    expect(createdClients[0].subscribe).not.toHaveBeenCalled()

    // Após onConnect: processa fila e registra subscrição direta
    act(() => {
      createdClients[0].onConnect?.()
    })

    expect(createdClients[0].subscribe).toHaveBeenCalledWith(
      '/topic/user/u1/notifications',
      expect.any(Function)
    )

    // Simula chegada de mensagem (cobre inner callback do directTopic)
    act(() => {
      capturedDirectHandler?.({ body: JSON.stringify({ channel: 'notifications' }) })
    })
    expect(callback).toHaveBeenCalledWith({ channel: 'notifications' })

    // Simula mensagem inválida (cobre o bloco catch)
    act(() => {
      capturedDirectHandler?.({ body: 'invalid-json{' })
    })
    expect(callback).toHaveBeenCalledWith('invalid-json{')
  })

  it('unsubscribeTopic remove subscrição existente', () => {
    const callback = vi.fn()
    const mockUnsub = vi.fn()
    let capturedStompHandler: ((msg: { body: string }) => void) | null = null
    createdClients.length = 0
    vi.mocked(createStompClient).mockImplementation(() => {
      const c = createMockClient()
      c.subscribe = vi.fn((_, handler) => {
        capturedStompHandler = handler
        return { unsubscribe: mockUnsub }
      })
      return c as never
    })

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
      createdClients[0].connected = true
      createdClients[0].onConnect?.()
    })

    act(() => {
      latestContext?.subscribeTopic('/topic/user/u2/notifications', callback)
    })

    // Simula recebimento de mensagem (cobre o inner callback)
    act(() => {
      capturedStompHandler?.({ body: JSON.stringify({ type: 'ITEM_ADDED' }) })
    })
    expect(callback).toHaveBeenCalledWith({ type: 'ITEM_ADDED' })

    // Simula mensagem inválida (cobre o bloco catch)
    act(() => {
      capturedStompHandler?.({ body: 'not-json' })
    })
    expect(callback).toHaveBeenCalledWith('not-json')

    act(() => {
      latestContext?.unsubscribeTopic('/topic/user/u2/notifications')
    })

    expect(mockUnsub).toHaveBeenCalled()
  })
})
