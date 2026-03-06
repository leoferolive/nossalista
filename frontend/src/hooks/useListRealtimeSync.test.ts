import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useListRealtimeSync } from './useListRealtimeSync'
import { ListStateResponse } from '../types/List'

describe('useListRealtimeSync', () => {
  it('aceita revision nova e rejeita stale', () => {
    const fetchItems = vi.fn()
    const getListState = vi.fn<(_: string) => Promise<ListStateResponse>>()

    const { result } = renderHook(() =>
      useListRealtimeSync({
        listId: 'list-1',
        wsStatus: 'CONNECTED',
        fetchItems,
        getListState,
      })
    )

    expect(result.current.handleIncomingItemsRevision(100)).toBe('accepted')
    expect(result.current.handleIncomingItemsRevision(90)).toBe('stale')
    expect(result.current.handleIncomingItemsRevision(undefined)).toBe('missing')
  })

  it('faz resync na reconexao quando revision do backend avanca', async () => {
    const fetchItems = vi.fn().mockResolvedValue(undefined)
    const getListState = vi.fn<(_: string) => Promise<ListStateResponse>>().mockResolvedValue({
      listId: 'list-1',
      revision: 200,
      updatedAt: new Date().toISOString(),
      itemsCount: 2,
    })

    const { result, rerender } = renderHook(
      ({ wsStatus }) =>
        useListRealtimeSync({
          listId: 'list-1',
          wsStatus,
          fetchItems,
          getListState,
        }),
      {
        initialProps: { wsStatus: 'RECONNECTING' as 'RECONNECTING' | 'CONNECTED' },
      }
    )

    act(() => {
      result.current.handleIncomingItemsRevision(100)
    })

    rerender({ wsStatus: 'CONNECTED' })

    await waitFor(() => {
      expect(getListState).toHaveBeenCalledWith('list-1')
      expect(fetchItems).toHaveBeenCalledWith('list-1')
    })
  })
})
