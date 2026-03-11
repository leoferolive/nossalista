import { render, act, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { NotificationProvider, useNotificationContext } from './NotificationContext'
import { WebSocketContext } from './WebSocketContext'

const makeWsContext = (subscribeTopic = vi.fn(), unsubscribeTopic = vi.fn()) => ({
  status: 'CONNECTED' as const,
  connect: vi.fn(),
  disconnect: vi.fn(),
  subscribe: vi.fn(),
  unsubscribe: vi.fn(),
  subscribeTopic,
  unsubscribeTopic,
  send: vi.fn(),
})

function TestConsumer() {
  const { notifications, unreadCount, markAllRead, clearAll } = useNotificationContext()
  return (
    <div>
      <span data-testid="count">{notifications.length}</span>
      <span data-testid="unread">{unreadCount}</span>
      <button onClick={markAllRead}>markAllRead</button>
      <button onClick={clearAll}>clearAll</button>
      <ul>
        {notifications.map((n) => (
          <li key={n.id} data-testid="notification">
            {n.message}
          </li>
        ))}
      </ul>
    </div>
  )
}

function renderWithProviders(subscribeTopic = vi.fn()) {
  const wsCtx = makeWsContext(subscribeTopic)
  return {
    wsCtx,
    ...render(
      <WebSocketContext.Provider value={wsCtx}>
        <NotificationProvider userId="user-123">
          <TestConsumer />
        </NotificationProvider>
      </WebSocketContext.Provider>
    ),
  }
}

describe('NotificationContext', () => {
  it('deve adicionar notificação quando mensagem ITEM_ADDED é recebida', async () => {
    let capturedCallback: ((msg: unknown) => void) | null = null
    const subscribeTopic = vi.fn((_, cb) => {
      capturedCallback = cb
    })

    renderWithProviders(subscribeTopic)

    expect(capturedCallback).not.toBeNull()

    await act(async () => {
      capturedCallback!({
        eventId: 'ev-1',
        listId: 'list-1',
        channel: 'notifications',
        type: 'ITEM_ADDED',
        timestamp: new Date().toISOString(),
        actor: { id: 'actor-1', username: 'joao' },
        payload: {
          id: 'item-1',
          name: 'Leite',
          checked: false,
          position: 0,
          createdBy: { id: 'actor-1', username: 'joao', name: 'João', avatarUrl: null },
          quantity: null,
          dueDate: null,
          url: null,
          createdAt: '',
          updatedAt: '',
        },
      })
    })

    expect(screen.getByTestId('count').textContent).toBe('1')
    expect(screen.getByTestId('unread').textContent).toBe('1')
    expect(screen.getByTestId('notification').textContent).toContain('joao')
    expect(screen.getByTestId('notification').textContent).toContain('Leite')
  })

  it('deve respeitar limite FIFO de 50 notificações', async () => {
    let capturedCallback: ((msg: unknown) => void) | null = null
    const subscribeTopic = vi.fn((_, cb) => {
      capturedCallback = cb
    })

    renderWithProviders(subscribeTopic)

    await act(async () => {
      for (let i = 0; i < 55; i++) {
        capturedCallback!({
          eventId: `ev-${i}`,
          listId: 'list-1',
          channel: 'notifications',
          type: 'ITEM_ADDED',
          timestamp: new Date().toISOString(),
          actor: { id: 'actor-1', username: `user${i}` },
          payload: {
            id: `item-${i}`,
            name: `Item ${i}`,
            checked: false,
            position: i,
            createdBy: { id: 'actor-1', username: `user${i}`, name: `User ${i}`, avatarUrl: null },
            quantity: null,
            dueDate: null,
            url: null,
            createdAt: '',
            updatedAt: '',
          },
        })
      }
    })

    expect(parseInt(screen.getByTestId('count').textContent!)).toBe(50)
  })

  it('deve marcar todas as notificações como lidas com markAllRead', async () => {
    let capturedCallback: ((msg: unknown) => void) | null = null
    const subscribeTopic = vi.fn((_, cb) => {
      capturedCallback = cb
    })

    renderWithProviders(subscribeTopic)

    await act(async () => {
      capturedCallback!({
        eventId: 'ev-1',
        listId: 'list-1',
        channel: 'notifications',
        type: 'ITEM_ADDED',
        timestamp: new Date().toISOString(),
        actor: { id: 'actor-1', username: 'joao' },
        payload: {
          id: 'item-1',
          name: 'Leite',
          checked: false,
          position: 0,
          createdBy: { id: 'actor-1', username: 'joao', name: 'João', avatarUrl: null },
          quantity: null,
          dueDate: null,
          url: null,
          createdAt: '',
          updatedAt: '',
        },
      })
    })

    expect(screen.getByTestId('unread').textContent).toBe('1')

    await act(async () => {
      screen.getByText('markAllRead').click()
    })

    expect(screen.getByTestId('unread').textContent).toBe('0')
    expect(screen.getByTestId('count').textContent).toBe('1')
  })

  it('deve limpar todas as notificações com clearAll', async () => {
    let capturedCallback: ((msg: unknown) => void) | null = null
    const subscribeTopic = vi.fn((_, cb) => {
      capturedCallback = cb
    })

    renderWithProviders(subscribeTopic)

    await act(async () => {
      capturedCallback!({
        eventId: 'ev-1',
        listId: 'list-1',
        channel: 'notifications',
        type: 'ITEM_ADDED',
        timestamp: new Date().toISOString(),
        actor: { id: 'actor-1', username: 'joao' },
        payload: {
          id: 'item-1',
          name: 'Leite',
          checked: false,
          position: 0,
          createdBy: { id: 'actor-1', username: 'joao', name: 'João', avatarUrl: null },
          quantity: null,
          dueDate: null,
          url: null,
          createdAt: '',
          updatedAt: '',
        },
      })
    })

    await act(async () => {
      screen.getByText('clearAll').click()
    })

    expect(screen.getByTestId('count').textContent).toBe('0')
    expect(screen.getByTestId('unread').textContent).toBe('0')
  })

  it('deve cancelar subscrição ao desmontar o componente', () => {
    const unsubscribeTopic = vi.fn()
    const wsCtx = makeWsContext(vi.fn(), unsubscribeTopic)
    const { unmount } = render(
      <WebSocketContext.Provider value={wsCtx}>
        <NotificationProvider userId="user-unmount">
          <div />
        </NotificationProvider>
      </WebSocketContext.Provider>
    )
    unmount()
    expect(unsubscribeTopic).toHaveBeenCalled()
  })

  it('deve ignorar mensagens de outros canais', async () => {
    let capturedCallback: ((msg: unknown) => void) | null = null
    const subscribeTopic = vi.fn((_, cb) => {
      capturedCallback = cb
    })

    renderWithProviders(subscribeTopic)

    await act(async () => {
      capturedCallback!({
        eventId: 'ev-1',
        listId: 'list-1',
        channel: 'items',
        type: 'ITEM_ADDED',
        timestamp: new Date().toISOString(),
        actor: { id: 'actor-1', username: 'joao' },
        payload: { id: 'item-1', name: 'Leite', checked: false },
      })
    })

    expect(screen.getByTestId('count').textContent).toBe('0')
  })

  it.each([
    ['ITEM_UPDATED', { id: 'i', name: 'Arroz', checked: false }, 'editou "Arroz"'],
    ['ITEM_REMOVED', { id: 'i', name: 'Sal', checked: false }, 'removeu "Sal"'],
    ['ITEM_CHECKED', { id: 'i', name: 'Óleo', checked: true }, 'marcou "Óleo" como concluído'],
    ['ITEM_CHECKED', { id: 'i', name: 'Óleo', checked: false }, 'desmarcou "Óleo"'],
    ['LIST_NAME_UPDATED', { oldName: 'A', newName: 'B' }, 'renomeou a lista para "B"'],
    ['MEMBER_JOINED', { userId: 'u', username: 'maria' }, 'entrou na lista'],
    ['MEMBER_LEFT', { userId: 'u', username: 'pedro' }, 'saiu da lista'],
    ['MEMBER_REMOVED', { userId: 'u', username: 'ana' }, 'foi removido da lista'],
    ['UNKNOWN_TYPE', {}, 'Nova atividade na lista'],
  ])('deve formatar mensagem para tipo %s', async (type, payload, expectedText) => {
    let capturedCallback: ((msg: unknown) => void) | null = null
    const subscribeTopic = vi.fn((_, cb) => {
      capturedCallback = cb
    })

    renderWithProviders(subscribeTopic)

    await act(async () => {
      capturedCallback!({
        eventId: `ev-${type}`,
        listId: 'list-1',
        channel: 'notifications',
        type,
        timestamp: new Date().toISOString(),
        actor: { id: 'actor-1', username: 'joao' },
        payload,
      })
    })

    expect(screen.getByTestId('notification').textContent).toContain(expectedText)
  })
})
