import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useItems } from './useItems';
import { itemsApi } from '../api/itemsApi';
import { ListItem } from '../types/Item';

// Mock da API
vi.mock('../api/itemsApi');

describe('useItems', () => {
  const mockFetchItems = vi.fn();
  const mockAddItem = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    (itemsApi.getItemsByListId as any) = mockFetchItems;
    (itemsApi.addItem as any) = mockAddItem;
  });

  it('deve inicializar com estado vazio', () => {
    const { result } = renderHook(() => useItems());

    expect(result.current.items).toEqual([]);
    expect(result.current.loadingItems).toBe(false);
    expect(result.current.errorItems).toBeNull();
  });

  it('deve buscar itens com sucesso', async () => {
    const mockItems: ListItem[] = [
      {
        id: 'item-1',
        name: 'Item 1',
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
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    ];
    mockFetchItems.mockResolvedValue(mockItems);

    const { result } = renderHook(() => useItems());

    await act(async () => {
      await result.current.fetchItems('list-123');
    });

    expect(result.current.items).toEqual(mockItems);
    expect(result.current.loadingItems).toBe(false);
    expect(result.current.errorItems).toBeNull();
    expect(mockFetchItems).toHaveBeenCalledWith('list-123');
  });

  it('deve mostrar loading enquanto busca itens', async () => {
    mockFetchItems.mockImplementation(() => new Promise(() => {})); // Never resolves

    const { result } = renderHook(() => useItems());

    act(() => {
      result.current.fetchItems('list-123');
    });

    expect(result.current.loadingItems).toBe(true);
  });

  it('deve tratar erro ao buscar itens', async () => {
    mockFetchItems.mockRejectedValue(new Error('Erro ao carregar itens'));

    const { result } = renderHook(() => useItems());

    await act(async () => {
      await result.current.fetchItems('list-123');
    });

    expect(result.current.items).toEqual([]);
    expect(result.current.loadingItems).toBe(false);
    expect(result.current.errorItems).toBe('Erro ao carregar itens');
  });

  it('deve limpar erro ao chamar clearItemsError', async () => {
    mockFetchItems.mockRejectedValue(new Error('Erro ao carregar itens'));

    const { result } = renderHook(() => useItems());

    await act(async () => {
      await result.current.fetchItems('list-123');
    });

    expect(result.current.errorItems).toBe('Erro ao carregar itens');

    act(() => {
      result.current.clearItemsError();
    });

    expect(result.current.errorItems).toBeNull();
  });

  it('deve adicionar item com sucesso', async () => {
    const newItem: ListItem = {
      id: 'item-2',
      name: 'Novo Item',
      checked: false,
      quantity: 2,
      dueDate: null,
      url: null,
      position: 1,
      createdBy: {
        id: 'user-1',
        username: 'testuser',
        name: 'Test User',
        avatarUrl: null,
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    mockAddItem.mockResolvedValue(newItem);

    const { result } = renderHook(() => useItems());

    const createdItem = await act(async () => {
      return await result.current.addItem('list-123', {
        name: 'Novo Item',
        quantity: 2,
      });
    });

    expect(createdItem).toEqual(newItem);
    expect(result.current.items).toContainEqual(newItem);
    expect(result.current.addingItem).toBe(false);
    expect(mockAddItem).toHaveBeenCalledWith('list-123', {
      name: 'Novo Item',
      quantity: 2,
    });
  });

  it('deve tratar erro ao adicionar item', async () => {
    mockAddItem.mockRejectedValue(new Error('Erro ao adicionar item'));

    const { result } = renderHook(() => useItems());

    // O hook captura o erro e não relança, apenas atualiza o estado
    await act(async () => {
      try {
        await result.current.addItem('list-123', { name: 'Item' });
      } catch (e) {
        // Erro é relançado pelo hook
      }
    });

    expect(result.current.errorItems).toBe('Erro ao adicionar item');
    expect(result.current.addingItem).toBe(false);
  });

  it('deve mostrar addingItem enquanto adiciona', async () => {
    mockAddItem.mockImplementation(() => new Promise(() => {})); // Never resolves

    const { result } = renderHook(() => useItems());

    act(() => {
      result.current.addItem('list-123', { name: 'Item' });
    });

    expect(result.current.addingItem).toBe(true);
  });

  it('deve retornar lista vazia quando não há itens', async () => {
    mockFetchItems.mockResolvedValue([]);

    const { result } = renderHook(() => useItems());

    await act(async () => {
      await result.current.fetchItems('list-123');
    });

    expect(result.current.items).toEqual([]);
    expect(result.current.loadingItems).toBe(false);
    expect(result.current.errorItems).toBeNull();
  });
});
