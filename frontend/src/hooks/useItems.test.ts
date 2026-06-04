import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useItems } from './useItems'
import { itemsApi } from '../api/itemsApi'

// Mock da API
vi.mock('../api/itemsApi', () => ({
  itemsApi: {
    getItemsByListId: vi.fn(),
    addItem: vi.fn(),
    toggleItemCheck: vi.fn(),
    updateItem: vi.fn(),
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
  })

  it('deve remover item do estado após sucesso da API', async () => {
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
      await result.current.deleteItem('list-1', 'item-1')
    })

    // Item deve ter sido removido
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
      await result.current.deleteItem('list-1', 'item-1')
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

  it('deve usar mensagem padrão quando o erro não é uma instância de Error', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    // Rejeita com algo que não é Error (cobre o ramo falso do ternário instanceof)
    vi.mocked(itemsApi.deleteItem).mockRejectedValue('falha string')

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      try {
        await result.current.deleteItem('list-1', 'item-1')
      } catch {
        // esperado
      }
    })

    expect(result.current.errorItems).toBe('Erro ao remover item')
    // Item permanece pois a API falhou
    expect(result.current.items).toHaveLength(1)
  })

  it('deve marcar deletingItemId durante a operação e limpar ao final', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.deleteItem).mockResolvedValue(undefined)

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      await result.current.deleteItem('list-1', 'item-1')
    })

    // finally sempre limpa deletingItemId
    expect(result.current.deletingItemId).toBeNull()
  })
})

describe('useItems - fetchItems', () => {
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

  it('deve carregar itens com sucesso e limpar loading/erro', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])

    const { result } = renderHook(() => useItems())

    // Estado inicial
    expect(result.current.items).toHaveLength(0)
    expect(result.current.loadingItems).toBe(false)

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0].id).toBe('item-1')
    expect(result.current.loadingItems).toBe(false)
    expect(result.current.errorItems).toBeNull()
    expect(itemsApi.getItemsByListId).toHaveBeenCalledWith('list-1')
  })

  it('deve carregar lista vazia sem erro', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([])

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    expect(result.current.items).toHaveLength(0)
    expect(result.current.errorItems).toBeNull()
  })

  it('deve definir erro com a mensagem da Error quando a API falha', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockRejectedValue(new Error('Falha ao buscar'))

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    // catch com instanceof Error verdadeiro
    expect(result.current.errorItems).toBe('Falha ao buscar')
    expect(result.current.loadingItems).toBe(false)
    expect(result.current.items).toHaveLength(0)
  })

  it('deve usar mensagem padrão quando o erro não é uma Error', async () => {
    // Rejeita com objeto que não é Error (ramo falso do ternário)
    vi.mocked(itemsApi.getItemsByListId).mockRejectedValue({ code: 500 })

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    expect(result.current.errorItems).toBe('Erro ao carregar itens')
  })
})

describe('useItems - addItem', () => {
  const mockItem = {
    id: 'item-1',
    name: 'Existing Item',
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

  const newItem = {
    ...mockItem,
    id: 'item-2',
    name: 'New Item',
    position: 1,
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('deve adicionar item ao final da lista existente', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.addItem).mockResolvedValue(newItem)

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    let returned
    await act(async () => {
      returned = await result.current.addItem('list-1', { name: 'New Item' })
    })

    expect(result.current.items).toHaveLength(2)
    expect(result.current.items[1].id).toBe('item-2')
    expect(returned).toEqual(newItem)
    expect(result.current.addingItem).toBe(false)
    expect(result.current.errorItems).toBeNull()
    expect(itemsApi.addItem).toHaveBeenCalledWith('list-1', { name: 'New Item' })
  })

  it('deve adicionar item a uma lista inicialmente vazia', async () => {
    vi.mocked(itemsApi.addItem).mockResolvedValue(newItem)

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.addItem('list-1', { name: 'New Item' })
    })

    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0].id).toBe('item-2')
  })

  it('deve lançar erro e definir errorItems quando a API falha (Error)', async () => {
    vi.mocked(itemsApi.addItem).mockRejectedValue(new Error('Falha ao criar'))

    const { result } = renderHook(() => useItems())

    let errorCaught = false
    await act(async () => {
      try {
        await result.current.addItem('list-1', { name: 'New Item' })
      } catch (e) {
        errorCaught = true
        expect((e as Error).message).toBe('Falha ao criar')
      }
    })

    expect(errorCaught).toBe(true)
    expect(result.current.errorItems).toBe('Falha ao criar')
    expect(result.current.items).toHaveLength(0)
    expect(result.current.addingItem).toBe(false)
  })

  it('deve usar mensagem padrão quando o erro de addItem não é uma Error', async () => {
    vi.mocked(itemsApi.addItem).mockRejectedValue(null)

    const { result } = renderHook(() => useItems())

    let errorCaught = false
    await act(async () => {
      try {
        await result.current.addItem('list-1', { name: 'New Item' })
      } catch (e) {
        errorCaught = true
        expect((e as Error).message).toBe('Erro ao adicionar item')
      }
    })

    expect(errorCaught).toBe(true)
    expect(result.current.errorItems).toBe('Erro ao adicionar item')
  })
})

describe('useItems - toggleItem (branches adicionais)', () => {
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

  it('deve lançar "Item não encontrado" quando o item não existe', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await expect(async () => {
      await act(async () => {
        await result.current.toggleItem('list-1', 'item-999')
      })
    }).rejects.toThrow('Item não encontrado')

    // toggleItemCheck nem deve ser chamado
    expect(itemsApi.toggleItemCheck).not.toHaveBeenCalled()
    expect(result.current.togglingItemId).toBeNull()
  })

  it('deve alternar de checked=true para false (originalChecked verdadeiro)', async () => {
    const checkedItem = { ...mockItem, checked: true }
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([checkedItem])
    vi.mocked(itemsApi.toggleItemCheck).mockResolvedValue({ ...checkedItem, checked: false })

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    expect(result.current.items[0].checked).toBe(true)

    await act(async () => {
      await result.current.toggleItem('list-1', 'item-1')
    })

    expect(result.current.items[0].checked).toBe(false)
  })

  it('deve usar mensagem padrão quando o erro do toggle não é uma Error', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.toggleItemCheck).mockRejectedValue(undefined)

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      try {
        await result.current.toggleItem('list-1', 'item-1')
      } catch {
        // esperado
      }
    })

    expect(result.current.errorItems).toBe('Erro ao atualizar item')
    // Reverte ao estado original
    expect(result.current.items[0].checked).toBe(false)
  })

  it('deve preservar os outros itens da lista ao alternar um único item (ramo não-correspondente)', async () => {
    const otherItem = { ...mockItem, id: 'item-2', name: 'Other', checked: false }
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem, otherItem])
    vi.mocked(itemsApi.toggleItemCheck).mockResolvedValue({ ...mockItem, checked: true })

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      await result.current.toggleItem('list-1', 'item-1')
    })

    // item-1 alternado, item-2 (ramo `: i`) intacto
    expect(result.current.items[0].checked).toBe(true)
    expect(result.current.items[1].id).toBe('item-2')
    expect(result.current.items[1].checked).toBe(false)
  })

  it('deve preservar os outros itens ao reverter o toggle em caso de erro (ramo não-correspondente)', async () => {
    const otherItem = { ...mockItem, id: 'item-2', name: 'Other', checked: true }
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem, otherItem])
    vi.mocked(itemsApi.toggleItemCheck).mockRejectedValue(new Error('Erro de rede'))

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      try {
        await result.current.toggleItem('list-1', 'item-1')
      } catch {
        // esperado
      }
    })

    // item-1 revertido ao original, item-2 intacto em ambos os maps
    expect(result.current.items[0].checked).toBe(false)
    expect(result.current.items[1].id).toBe('item-2')
    expect(result.current.items[1].checked).toBe(true)
  })
})

describe('useItems - updateItem', () => {
  const mockItem = {
    id: 'item-1',
    name: 'Original Name',
    checked: false,
    quantity: 5,
    dueDate: '2026-03-01T10:00:00Z',
    url: 'https://old.example.com',
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

  it('deve atualizar todos os campos fornecidos (todos os ramos defined)', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    const updated = {
      ...mockItem,
      name: 'New Name',
      quantity: 10,
      dueDate: '2026-04-01T10:00:00Z',
      url: 'https://new.example.com',
    }
    vi.mocked(itemsApi.updateItem).mockResolvedValue(updated)

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    let returned
    await act(async () => {
      returned = await result.current.updateItem('list-1', 'item-1', {
        name: 'New Name',
        quantity: 10,
        dueDate: '2026-04-01T10:00:00Z',
        url: 'https://new.example.com',
      })
    })

    // Optimistic update aplicou todos os campos
    expect(result.current.items[0].name).toBe('New Name')
    expect(result.current.items[0].quantity).toBe(10)
    expect(result.current.items[0].dueDate).toBe('2026-04-01T10:00:00Z')
    expect(result.current.items[0].url).toBe('https://new.example.com')
    expect(returned).toEqual(updated)
    expect(result.current.updatingItemId).toBeNull()
    expect(result.current.errorItems).toBeNull()
  })

  it('deve atualizar apenas o name e preservar os demais campos (ramos undefined)', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.updateItem).mockResolvedValue({ ...mockItem, name: 'Renamed' })

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      // quantity, dueDate e url ausentes -> ramos falsos do !== undefined
      await result.current.updateItem('list-1', 'item-1', { name: 'Renamed' })
    })

    expect(result.current.items[0].name).toBe('Renamed')
    // Campos não fornecidos permanecem inalterados
    expect(result.current.items[0].quantity).toBe(5)
    expect(result.current.items[0].dueDate).toBe('2026-03-01T10:00:00Z')
    expect(result.current.items[0].url).toBe('https://old.example.com')
  })

  it('deve aplicar valores null explícitos (defined com valor falsy)', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.updateItem).mockResolvedValue({
      ...mockItem,
      quantity: null,
      dueDate: null,
      url: null,
    })

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      // valores null são !== undefined -> ramos verdadeiros, aplicam null
      await result.current.updateItem('list-1', 'item-1', {
        name: 'Original Name',
        quantity: null,
        dueDate: null,
        url: null,
      })
    })

    expect(result.current.items[0].quantity).toBeNull()
    expect(result.current.items[0].dueDate).toBeNull()
    expect(result.current.items[0].url).toBeNull()
  })

  it('deve lançar "Item não encontrado" quando o item não existe', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await expect(async () => {
      await act(async () => {
        await result.current.updateItem('list-1', 'item-999', { name: 'X' })
      })
    }).rejects.toThrow('Item não encontrado')

    expect(itemsApi.updateItem).not.toHaveBeenCalled()
    expect(result.current.updatingItemId).toBeNull()
  })

  it('deve reverter ao item original quando a API falha (Error)', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.updateItem).mockRejectedValue(new Error('Falha ao atualizar'))

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      try {
        await result.current.updateItem('list-1', 'item-1', { name: 'New Name', quantity: 99 })
      } catch {
        // esperado
      }
    })

    // Reverteu ao item original
    expect(result.current.items[0].name).toBe('Original Name')
    expect(result.current.items[0].quantity).toBe(5)
    expect(result.current.errorItems).toBe('Falha ao atualizar')
    expect(result.current.updatingItemId).toBeNull()
  })

  it('deve usar mensagem padrão quando o erro de updateItem não é uma Error', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem])
    vi.mocked(itemsApi.updateItem).mockRejectedValue('string de erro')

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    let errorCaught = false
    await act(async () => {
      try {
        await result.current.updateItem('list-1', 'item-1', { name: 'New Name' })
      } catch (e) {
        errorCaught = true
        expect((e as Error).message).toBe('Erro ao atualizar item')
      }
    })

    expect(errorCaught).toBe(true)
    expect(result.current.errorItems).toBe('Erro ao atualizar item')
    // Reverteu ao original
    expect(result.current.items[0].name).toBe('Original Name')
  })

  it('deve preservar os outros itens ao atualizar um único item (ramo não-correspondente)', async () => {
    const otherItem = { ...mockItem, id: 'item-2', name: 'Other Item' }
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem, otherItem])
    vi.mocked(itemsApi.updateItem).mockResolvedValue({ ...mockItem, name: 'Renamed' })

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      await result.current.updateItem('list-1', 'item-1', { name: 'Renamed' })
    })

    // item-1 atualizado, item-2 (ramo `: i`) intacto
    expect(result.current.items[0].name).toBe('Renamed')
    expect(result.current.items[1].id).toBe('item-2')
    expect(result.current.items[1].name).toBe('Other Item')
  })

  it('deve preservar os outros itens ao reverter a atualização em erro (ramo não-correspondente)', async () => {
    const otherItem = { ...mockItem, id: 'item-2', name: 'Other Item' }
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem, otherItem])
    vi.mocked(itemsApi.updateItem).mockRejectedValue(new Error('Falha'))

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    await act(async () => {
      try {
        await result.current.updateItem('list-1', 'item-1', { name: 'Renamed' })
      } catch {
        // esperado
      }
    })

    // item-1 revertido, item-2 intacto no map de reversão
    expect(result.current.items[0].name).toBe('Original Name')
    expect(result.current.items[1].id).toBe('item-2')
    expect(result.current.items[1].name).toBe('Other Item')
  })
})

describe('useItems - setItems e clearItemsError', () => {
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

  it('clearItemsError deve limpar a mensagem de erro', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockRejectedValue(new Error('Algum erro'))

    const { result } = renderHook(() => useItems())

    await act(async () => {
      await result.current.fetchItems('list-1')
    })

    expect(result.current.errorItems).toBe('Algum erro')

    act(() => {
      result.current.clearItemsError()
    })

    expect(result.current.errorItems).toBeNull()
  })

  it('setItems deve aceitar um valor direto (ramo não-função)', () => {
    const { result } = renderHook(() => useItems())

    act(() => {
      // action é um array, não uma função -> ramo "não-função" do typeof
      result.current.setItems([mockItem])
    })

    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0].id).toBe('item-1')
  })

  it('setItems deve aceitar uma função updater (ramo função)', () => {
    const { result } = renderHook(() => useItems())

    act(() => {
      result.current.setItems([mockItem])
    })

    act(() => {
      // action é uma função -> ramo "função" do typeof
      result.current.setItems((prev) => prev.filter((i) => i.id !== 'item-1'))
    })

    expect(result.current.items).toHaveLength(0)
  })
})
