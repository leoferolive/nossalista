import { describe, expect, it, vi } from 'vitest'
import type { StompConfig } from '@stomp/stompjs'

const clientCtor = vi.fn()

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function MockClient(config: StompConfig) {
    clientCtor(config)
    return { config }
  }),
}))

vi.mock('sockjs-client', () => ({
  default: vi.fn().mockImplementation(function MockSockJS() {
    return { mocked: 'sockjs' }
  }),
}))

describe('websocket api', () => {
  it('getListTopic monta o topico STOMP do canal de uma lista', async () => {
    const { getListTopic } = await import('./websocket')

    expect(getListTopic('list-1', 'items')).toBe('/topic/list/list-1/items')
    expect(getListTopic('list-2', 'presence')).toBe('/topic/list/list-2/presence')
  })

  it('getUserNotificationsTopic monta o topico STOMP de notificacoes do usuario', async () => {
    const { getUserNotificationsTopic } = await import('./websocket')

    expect(getUserNotificationsTopic('user-1')).toBe('/topic/user/user-1/notifications')
  })

  it('createStompClient configura o cliente STOMP com o token de autenticacao', async () => {
    const { createStompClient } = await import('./websocket')

    createStompClient('jwt-token')

    expect(clientCtor).toHaveBeenCalledTimes(1)
    const config = clientCtor.mock.calls[0][0] as StompConfig
    expect(config.connectHeaders).toEqual({ Authorization: 'Bearer jwt-token' })
    expect(config.heartbeatIncoming).toBe(10000)
    expect(config.heartbeatOutgoing).toBe(10000)
    expect(config.reconnectDelay).toBe(0)
    expect(typeof config.webSocketFactory).toBe('function')
    expect(config.webSocketFactory?.()).toEqual({ mocked: 'sockjs' })
  })

  it('debug do STOMP nao lanca ao ser chamado', async () => {
    const { createStompClient } = await import('./websocket')

    createStompClient('jwt-token')

    const config = clientCtor.mock.calls[clientCtor.mock.calls.length - 1][0] as StompConfig
    expect(() => config.debug?.('mensagem de debug')).not.toThrow()
  })
})
