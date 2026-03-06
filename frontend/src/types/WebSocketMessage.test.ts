import { describe, expect, it } from 'vitest'
import { parseListWebSocketMessage } from './WebSocketMessage'

describe('parseListWebSocketMessage', () => {
  it('aceita envelope v2 valido de item', () => {
    const parsed = parseListWebSocketMessage({
      schemaVersion: 2,
      eventId: 'event-1',
      listId: 'list-1',
      channel: 'items',
      revision: 1741040400000,
      type: 'ITEM_ADDED',
      actor: {
        id: 'user-1',
        username: 'leo',
      },
      payload: {
        id: 'item-1',
        name: 'Arroz',
        checked: false,
        quantity: null,
        dueDate: null,
        url: null,
        position: 0,
        createdBy: {
          id: 'user-1',
          username: 'leo',
          name: 'Leo',
          avatarUrl: null,
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    })

    expect(parsed).not.toBeNull()
    expect(parsed?.type).toBe('ITEM_ADDED')
  })

  it('rejeita mensagem legada sem schemaVersion/channel/listId', () => {
    const parsed = parseListWebSocketMessage({
      type: 'ITEM_ADDED',
      payload: {},
      userId: 'user-1',
      username: 'leo',
      timestamp: new Date().toISOString(),
    })

    expect(parsed).toBeNull()
  })

  it('rejeita mensagem de item sem actor', () => {
    const parsed = parseListWebSocketMessage({
      schemaVersion: 2,
      eventId: 'event-2',
      listId: 'list-1',
      channel: 'items',
      revision: 1741040400001,
      type: 'ITEM_REMOVED',
      payload: {
        id: 'item-1',
        name: 'Arroz',
        checked: false,
        quantity: null,
        dueDate: null,
        url: null,
        position: 0,
        createdBy: {
          id: 'user-1',
          username: 'leo',
          name: 'Leo',
          avatarUrl: null,
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    })

    expect(parsed).toBeNull()
  })

  it('rejeita mensagem de itens sem revision', () => {
    const parsed = parseListWebSocketMessage({
      schemaVersion: 2,
      eventId: 'event-3',
      listId: 'list-1',
      channel: 'items',
      type: 'ITEM_UPDATED',
      actor: {
        id: 'user-1',
        username: 'leo',
      },
      payload: {
        id: 'item-1',
        name: 'Arroz',
        checked: false,
        quantity: null,
        dueDate: null,
        url: null,
        position: 0,
        createdBy: {
          id: 'user-1',
          username: 'leo',
          name: 'Leo',
          avatarUrl: null,
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      timestamp: new Date().toISOString(),
    })

    expect(parsed).toBeNull()
  })
})
