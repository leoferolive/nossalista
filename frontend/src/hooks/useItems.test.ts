import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useItems } from './useItems';
import { itemsApi } from '../api/itemsApi';

// Mock da API
vi.mock('../api/itemsApi', () => ({
  itemsApi: {
    getItemsByListId: vi.fn(),
    addItem: vi.fn(),
    toggleItemCheck: vi.fn(),
  },
}));

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
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('deve fazer optimistic update imediatamente', async () => {
    // Mock fetchItems para carregar items inicialmente
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem]);
    
    // Mock toggleItemCheck para retornar sucesso
    vi.mocked(itemsApi.toggleItemCheck).mockResolvedValue({
      ...mockItem,
      checked: true,
    });

    const { result } = renderHook(() => useItems());

    // Carregar items
    await act(async () => {
      await result.current.fetchItems('list-1');
    });

    // Verificar estado inicial
    expect(result.current.items[0].checked).toBe(false);

    // Chamar toggle
    act(() => {
      result.current.toggleItem('list-1', 'item-1');
    });

    // Verificar optimistic update (deve estar true imediatamente)
    expect(result.current.items[0].checked).toBe(true);
  });

  it('deve reverter estado em caso de erro', async () => {
    // Mock fetchItems para carregar items inicialmente
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem]);
    
    // Mock toggleItemCheck para falhar
    vi.mocked(itemsApi.toggleItemCheck).mockRejectedValue(new Error('Erro de rede'));

    const { result } = renderHook(() => useItems());

    // Carregar items
    await act(async () => {
      await result.current.fetchItems('list-1');
    });

    // Estado inicial
    expect(result.current.items[0].checked).toBe(false);

    // Chamar toggle e esperar erro
    await act(async () => {
      try {
        await result.current.toggleItem('list-1', 'item-1');
      } catch (e) {
        // Esperado
      }
    });

    // Verificar que voltou ao estado original
    expect(result.current.items[0].checked).toBe(false);
    expect(result.current.errorItems).toBe('Erro de rede');
  });

  it('deve manter estado atualizado após sucesso', async () => {
    vi.mocked(itemsApi.getItemsByListId).mockResolvedValue([mockItem]);
    vi.mocked(itemsApi.toggleItemCheck).mockResolvedValue({
      ...mockItem,
      checked: true,
    });

    const { result } = renderHook(() => useItems());

    await act(async () => {
      await result.current.fetchItems('list-1');
    });

    await act(async () => {
      await result.current.toggleItem('list-1', 'item-1');
    });

    // Estado deve permanecer true após sucesso
    expect(result.current.items[0].checked).toBe(true);
    expect(result.current.errorItems).toBeNull();
  });
});
