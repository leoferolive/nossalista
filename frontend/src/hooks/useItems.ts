import { useState, useCallback } from 'react';
import { itemsApi } from '../api/itemsApi';
import { ListItem, CreateItemRequest } from '../types/Item';

interface UseItemsReturn {
  // States
  items: ListItem[];
  loadingItems: boolean;
  errorItems: string | null;
  addingItem: boolean;

  // Actions
  fetchItems: (listId: string) => Promise<void>;
  addItem: (listId: string, request: CreateItemRequest) => Promise<ListItem>;
  clearItemsError: () => void;
}

/**
 * Hook para gerenciar estado de itens de uma lista
 */
export const useItems = (): UseItemsReturn => {
  const [items, setItems] = useState<ListItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [errorItems, setErrorItems] = useState<string | null>(null);
  const [addingItem, setAddingItem] = useState(false);

  /**
   * Busca todos os itens de uma lista
   */
  const fetchItems = useCallback(async (listId: string) => {
    setLoadingItems(true);
    setErrorItems(null);

    try {
      const data = await itemsApi.getItemsByListId(listId);
      setItems(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar itens';
      setErrorItems(message);
    } finally {
      setLoadingItems(false);
    }
  }, []);

  /**
   * Adiciona um novo item à lista
   * Mantém a lista ordenada por position após adicionar
   */
  const addItem = useCallback(
    async (listId: string, request: CreateItemRequest): Promise<ListItem> => {
      setAddingItem(true);
      setErrorItems(null);

      try {
        const newItem = await itemsApi.addItem(listId, request);
        // Adiciona e re-ordena por position para manter consistência
        setItems((prev) => [...prev, newItem].sort((a, b) => a.position - b.position));
        return newItem;
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Erro ao adicionar item';
        setErrorItems(message);
        throw new Error(message);
      } finally {
        setAddingItem(false);
      }
    },
    []
  );

  /**
   * Limpa o erro de itens
   */
  const clearItemsError = useCallback(() => {
    setErrorItems(null);
  }, []);

  return {
    items,
    loadingItems,
    errorItems,
    addingItem,
    fetchItems,
    addItem,
    clearItemsError,
  };
};
