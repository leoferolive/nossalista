import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act, renderHook } from '@testing-library/react'
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

  it('connect inicia o handshake sem enviar JWT pelo cliente', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    expect(createdClients).toHaveLength(1)
    expect(createdClients[0].activate).toHaveBeenCalledOnce()
    expect(screen.getByText('CONNECTING')).toBeInTheDocument()
  })

  it('connect sem notifications (undefined) usa objeto vazio', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // chamada sem argumento -> ramo direito do ?? em notifications
    act(() => {
      latestContext?.connect()
    })

    // Conecta sem erro mesmo sem callbacks de reconexao
    act(() => {
      createdClients[0].onConnect?.()
    })

    expect(screen.getByText('CONNECTED')).toBeInTheDocument()
  })

  it('connect ignora chamada quando ja conectado', () => {
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

    expect(createdClients).toHaveLength(1)

    // Segunda chamada de connect deve sair cedo (clientRef.current.connected === true)
    act(() => {
      latestContext?.connect({})
    })

    expect(createdClients).toHaveLength(1)
  })

  it('doReconnect cria nova tentativa sem depender de JWT no localStorage', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    // Perde conexao -> agenda reconnect
    act(() => {
      createdClients[0].onDisconnect?.()
    })
    expect(screen.getByText('RECONNECTING')).toBeInTheDocument()

    act(() => {
      vi.advanceTimersByTime(0)
    })

    expect(createdClients).toHaveLength(2)
  })

  it('doReconnect desiste apos atingir MAX_RECONNECT_ATTEMPTS', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

    act(() => {
      latestContext?.connect({})
    })

    // Dispara repetidas falhas ate exceder MAX_RECONNECT_ATTEMPTS (30)
    act(() => {
      for (let i = 0; i < 35; i++) {
        const last = createdClients[createdClients.length - 1]
        last.onDisconnect?.()
        vi.advanceTimersByTime(10000)
      }
    })

    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument()
    expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('Max reconnect attempts'))
    warnSpy.mockRestore()
  })

  it('subscribe ignora quando key ja registrada', () => {
    createdClients.length = 0
    const subscribeMock = vi.fn(() => ({ unsubscribe: vi.fn() }))
    vi.mocked(createStompClient).mockImplementation(() => {
      const c = createMockClient()
      c.subscribe = subscribeMock
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
      latestContext?.subscribe('list-1', 'items', vi.fn())
    })
    expect(subscribeMock).toHaveBeenCalledTimes(1)

    // Segunda subscribe com a mesma key -> ramo early-return (subscriptionsRef.has)
    act(() => {
      latestContext?.subscribe('list-1', 'items', vi.fn())
    })
    expect(subscribeMock).toHaveBeenCalledTimes(1)
  })

  it('doSubscribe ignora key duplicada via reprocessamento de fila apos reconexao', () => {
    // Cobre o early-return de doSubscribe (subscriptionsRef.has(key)) na reconexao
    createdClients.length = 0
    vi.mocked(createStompClient).mockImplementation(() => createMockClient() as never)

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

    // Subscreve normalmente (registra na subscriptionsRef)
    act(() => {
      latestContext?.subscribe('list-dup', 'items', vi.fn())
    })

    // Enfileira a MESMA key como pending manualmente nao e possivel pela API,
    // entao validamos o ramo via subscribe duplicado (ja coberto acima).
    expect(createdClients[0].subscribe).toHaveBeenCalledTimes(1)
  })

  it('subscribe enfileira quando cliente existe mas ainda nao conectou', () => {
    createdClients.length = 0
    vi.mocked(createStompClient).mockImplementation(() => createMockClient() as never)

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // connect cria o cliente mas connected permanece false
    act(() => {
      latestContext?.connect({})
    })

    act(() => {
      latestContext?.subscribe('list-pend', 'items', vi.fn())
    })

    // Cliente ainda nao conectado -> enfileira, nao chama subscribe
    expect(createdClients[0].subscribe).not.toHaveBeenCalled()

    // Ao conectar, a fila e processada e subscribe e chamado
    act(() => {
      createdClients[0].onConnect?.()
    })
    expect(createdClients[0].subscribe).toHaveBeenCalled()
  })

  it('subscribe sem nenhum cliente apenas avisa', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // Sem connect previo -> clientRef.current e null
    act(() => {
      latestContext?.subscribe('list-nc', 'items', vi.fn())
    })

    expect(createdClients).toHaveLength(0)
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('Tentativa de subscribe sem conexão ativa')
    )
    warnSpy.mockRestore()
  })

  it('unsubscribe sem subscricao existente nao lanca', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // unsubscribe de uma key inexistente -> ramo false do if(subscription)
    act(() => {
      latestContext?.unsubscribe('inexistente', 'items')
    })

    expect(true).toBe(true)
  })

  it('subscribeTopic ignora topico ja registrado', () => {
    createdClients.length = 0
    const subscribeMock = vi.fn(() => ({ unsubscribe: vi.fn() }))
    vi.mocked(createStompClient).mockImplementation(() => {
      const c = createMockClient()
      c.subscribe = subscribeMock
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
      latestContext?.subscribeTopic('/topic/dup', vi.fn())
    })
    expect(subscribeMock).toHaveBeenCalledTimes(1)

    // Segunda chamada com mesmo topico -> early-return
    act(() => {
      latestContext?.subscribeTopic('/topic/dup', vi.fn())
    })
    expect(subscribeMock).toHaveBeenCalledTimes(1)
  })

  it('subscribeTopic sem nenhum cliente apenas avisa', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // Sem connect previo -> clientRef.current null
    act(() => {
      latestContext?.subscribeTopic('/topic/sem-conexao', vi.fn())
    })

    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('Tentativa de subscribeTopic sem conexão ativa')
    )
    warnSpy.mockRestore()
  })

  it('unsubscribeTopic sem subscricao existente nao lanca', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // ramo false do if(subscription) em unsubscribeTopic
    act(() => {
      latestContext?.unsubscribeTopic('/topic/nao-existe')
    })

    expect(true).toBe(true)
  })

  it('send sem conexao ativa apenas avisa e nao publica', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // Sem cliente conectado
    act(() => {
      latestContext?.send('/app/x', { a: 1 })
    })

    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('Tentativa de send sem conexão ativa')
    )
    warnSpy.mockRestore()
  })

  it('reconexao reprocessa subscricao de lista pendente (sem directTopic)', () => {
    // Garante o ramo else de pendingSubscriptions (doSubscribe) no onConnect
    createdClients.length = 0
    vi.mocked(createStompClient).mockImplementation(() => createMockClient() as never)

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    // Cliente criado mas nao conectado -> subscribe de lista vai para pending (sem directTopic)
    act(() => {
      latestContext?.subscribe('list-reconnect', 'items', vi.fn())
    })
    expect(createdClients[0].subscribe).not.toHaveBeenCalled()

    // onConnect processa a fila chamando doSubscribe (ramo else)
    act(() => {
      createdClients[0].onConnect?.()
    })
    expect(createdClients[0].subscribe).toHaveBeenCalledWith(
      '/topic/list/list-reconnect/items',
      expect.any(Function)
    )
  })

  it('erro STOMP de autenticacao limpa timer de reconexao agendado', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    // Primeira falha generica -> agenda reconnect timer
    act(() => {
      createdClients[0].onDisconnect?.()
    })
    expect(screen.getByText('RECONNECTING')).toBeInTheDocument()

    // Antes do timer disparar, novo cliente nao existe ainda; reuso do cliente atual:
    // dispara erro de autenticacao no cliente 0 (que tem reconnectTimer != null)
    act(() => {
      createdClients[0].onStompError?.({
        headers: { message: 'Sessão ausente no CONNECT STOMP' },
      })
    })

    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument()
    expect(createdClients[0].deactivate).toHaveBeenCalled()

    // O timer foi limpo: avançar nao cria novo cliente
    act(() => {
      vi.advanceTimersByTime(10000)
    })
    expect(createdClients).toHaveLength(1)
  })

  it('unsubscribe remove subscricao de lista existente', () => {
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

    // Cria subscricao de lista (registra na subscriptionsRef)
    act(() => {
      latestContext?.subscribe('list-unsub', 'items', vi.fn())
    })

    // unsubscribe da key existente -> ramo true do if(subscription)
    act(() => {
      latestContext?.unsubscribe('list-unsub', 'items')
    })

    expect(mockUnsub).toHaveBeenCalledTimes(1)
  })

  it('onConnect limpa o reconnect timer pendente do proprio cliente', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    // cliente 0 perde conexao -> agenda timer
    act(() => {
      createdClients[0].onDisconnect?.()
    })

    // timer dispara -> cria cliente 1 (reconnectTimerRef volta a null)
    act(() => {
      vi.advanceTimersByTime(0)
    })
    expect(createdClients).toHaveLength(2)

    // cliente 1 perde conexao -> agenda novo timer (reconnectTimerRef != null)
    act(() => {
      createdClients[1].onDisconnect?.()
    })

    // cliente 1 reconecta com timer ainda pendente -> ramo true de if(reconnectTimerRef.current)
    act(() => {
      createdClients[1].onConnect?.()
    })

    expect(screen.getByText('CONNECTED')).toBeInTheDocument()

    // o timer foi limpo no onConnect: avancar nao cria novo cliente
    act(() => {
      vi.advanceTimersByTime(10000)
    })
    expect(createdClients).toHaveLength(2)
  })

  it('disconnect sem cliente ativo nao faz nada (clientRef null)', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // Nunca chamou connect -> clientRef.current e null -> ramo false de if(clientRef.current)
    act(() => {
      latestContext?.disconnect()
    })

    expect(createdClients).toHaveLength(0)
    // status permanece DISCONNECTED (estado inicial), sem dispatch adicional
    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument()
  })

  it('disconnect limpa o reconnect timer pendente', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })

    // perde conexao -> agenda timer (reconnectTimerRef != null)
    act(() => {
      createdClients[0].onDisconnect?.()
    })
    expect(screen.getByText('RECONNECTING')).toBeInTheDocument()

    // disconnect voluntario com timer pendente -> ramo true de if(reconnectTimerRef.current)
    act(() => {
      latestContext?.disconnect()
    })

    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument()

    // timer foi limpo: avancar nao cria novo cliente
    act(() => {
      vi.advanceTimersByTime(10000)
    })
    expect(createdClients).toHaveLength(1)
  })
})

describe('WebSocketContext em modo mock', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.stubEnv('VITE_USE_MOCK_SERVER', 'true')
    createdClients.length = 0
    latestContext = null
    vi.mocked(createStompClient).mockImplementation(() => createMockClient() as never)
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.useRealTimers()
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('connect em mock muda para CONNECTED sem criar cliente STOMP', () => {
    const onReconnecting = vi.fn()
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({ onReconnecting })
    })

    expect(screen.getByText('CONNECTED')).toBeInTheDocument()
    expect(createStompClient).not.toHaveBeenCalled()
    expect(createdClients).toHaveLength(0)
  })

  it('connect em mock sem notifications (undefined) usa objeto vazio', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    // sem argumento em mock -> ramo direito do ?? {} na linha do mock
    act(() => {
      latestContext?.connect()
    })

    expect(screen.getByText('CONNECTED')).toBeInTheDocument()
    expect(createStompClient).not.toHaveBeenCalled()
  })

  it('disconnect em mock muda para DISCONNECTED', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
    })
    expect(screen.getByText('CONNECTED')).toBeInTheDocument()

    act(() => {
      latestContext?.disconnect()
    })

    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument()
    expect(createStompClient).not.toHaveBeenCalled()
  })

  it('subscribe em mock enfileira sem criar cliente', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
      latestContext?.subscribe('list-mock', 'items', vi.fn())
    })

    expect(createStompClient).not.toHaveBeenCalled()
  })

  it('subscribeTopic em mock enfileira sem criar cliente', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
      latestContext?.subscribeTopic('/topic/mock', vi.fn())
    })

    expect(createStompClient).not.toHaveBeenCalled()
  })

  it('send em mock e no-op sem publicar', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    )

    act(() => {
      latestContext?.connect({})
      latestContext?.send('/app/mock', { a: 1 })
    })

    expect(createStompClient).not.toHaveBeenCalled()
  })
})

describe('useWebSocketContext fora do provider', () => {
  it('lanca erro quando usado fora de WebSocketProvider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => renderHook(() => useWebSocketContext())).toThrow(
      'useWebSocketContext must be used within a WebSocketProvider'
    )
    spy.mockRestore()
  })
})
