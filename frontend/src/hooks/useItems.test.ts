import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useItems } from './useItems'
import { itemsApi } from '../api/itemsApi'

// Mock da API
vi.mock('../api/itemsApi', () => ({
  itemsApi: {
    getItemsByListId: vi.fn(),
    addItem: vi.fn(),
    toggleItemCheck: vi.fn(),
    deleteItem: vi.fn(),
  },
}))

describe('useItems - toggleItem', () => {
  const mockItem = {
    id: 'item-1',
    name: 'Test Item',
    checked: false,
    quantity: null,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: {
      id: 'user-1',
      username: 'testuser',
      name: 'Test User',
      avatarUrl: null,
    },
    createdAt: '2026-02-13T10:00:00Z',
    updatedAt: '2026-02-13T10:00:00Z',
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('deve fazer optimistic update imediatamente', async () => {
    // Mock fetchItems para carregar items inicialmente
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])

    // Mock toggleItemCheck para retornar sucesso
    vi.mocked(itemsApi.toggleItemCheck).mockResolvedValue({
      ...mockItem,
      checked: true,
    })

    const { result } = renderHook(() => useItems())

    // Carregar items
    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    // Verificar estado inicial
    expect(result.current.items[0].checked).toBe(false)

    // Chamar toggle e aguardar conclusão
    await act(async () => {
      await result.current.toggleItem('list-1', 'item-1')
    })

    // Verificar optimistic update (deve estar true imediatamente)
    expect(result.current.items[0].checked).toBe(true)
  })

  it('deve reverter estado em caso de erro', async () => {
    // Mock fetchItems para carregar items inicialmente
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])

    // Mock toggleItemCheck para falhar
    vi.mocked(itemsApi.toggleItemCheck).mockRejectedValue(new Error('Erro de rede'))

    const { result } = renderHook(() => useItems())

    // Carregar items
    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    // Estado inicial
    expect(result.current.items[0].checked).toBe(false)

    // Chamar toggle e esperar erro
    await act(async () => {
      try {
        await result.current.toggleItem('list-1', 'item-1')
      } catch (e) {
        // Esperado
      }
    })

    // Verificar que voltou ao estado original
    expect(result.current.items[0].checked).toBe(false)
    expect(result.current.errorItems).toBe('Erro de rede')
  })

  it('deve manter estado atualizado após sucesso', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.toggleItemCheck).mockResolvedValue({
      ...mockItem,
      checked: true,
    })

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      await result.current.toggleItem('list-1', 'item-1')
    })

    // Estado deve permanecer true após sucesso
    expect(result.current.items[0].checked).toBe(true)
    expect(result.current.errorItems).toBeNull()
  })
})

describe('useItems - deleteItem', () => {
  const mockItem = {
    id: 'item-1',
    name: 'Test Item',
    checked: false,
    quantity: null,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: {
      id: 'user-1',
      username: 'testuser',
      name: 'Test User',
      avatarUrl: null,
    },
    createdAt: '2026-02-13T10:00:00Z',
    updatedAt: '2026-02-13T10:00:00Z',
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('deve aguardar 200ms (animação fade-out) antes de remover do estado', async () => {
    // Mock fetchItems para carregar items inicialmente
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])

    // Mock deleteItem para retornar sucesso
    vi.mocked(itemsApi.deleteItem).mockResolvedValue(undefined)

    const { result } = renderHook(() => useItems())

    // Carregar items
    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    // Verificar estado inicial
    expect(result.current.items).toHaveLength(1)

    // Chamar deleteItem e aguardar completion
    await act(async () => {
      const promise = result.current.deleteItem('list-1', 'item-1')
      // Avançar timers para permitir o setTimeout completar
      await vi.runAllTimersAsync()
      await promise
    })

    // Item deve ter sido removido após animação
    expect(result.current.items).toHaveLength(0)
    expect(result.current.deletingItemId).toBeNull()
  })

  it('deve chamar API deleteItem com listId e itemId corretos', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.deleteItem).mockResolvedValue(undefined)

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      const promise = result.current.deleteItem('list-1', 'item-1')
      await vi.runAllTimersAsync()
      await promise
    })

    // Verificar que API foi chamada com parâmetros corretos
    expect(itemsApi.deleteItem).toHaveBeenCalledWith('list-1', 'item-1')
    expect(itemsApi.deleteItem).toHaveBeenCalledTimes(1)
  })

  it('NÃO deve remover do estado se API falhar', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.deleteItem).mockRejectedValue(new Error('Erro de rede'))

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    // Tentar deletar (vai falhar)
    let errorCaught = false
    await act(async () => {
      try {
        await result.current.deleteItem('list-1', 'item-1')
        await vi.runAllTimersAsync()
      } catch (e) {
        errorCaught = true
      }
    })

    // Verificar que o erro foi capturado
    expect(errorCaught).toBe(true)

    // Item ainda deve estar no estado
    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0].id).toBe('item-1')
    expect(result.current.errorItems).toBe('Erro de rede')
    expect(result.current.deletingItemId).toBeNull()
  })

  it('deve lançar erro se item não encontrado', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    // Tentar deletar item inexistente
    await expect(async () => {
      await act(async () => {
        await result.current.deleteItem('list-1', 'item-999')
      })
    }).rejects.toThrow('Item não encontrado')
  })
})
