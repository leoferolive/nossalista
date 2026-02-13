import { useState, useCallback } from 'react';
import { itemsApi } from '../api/itemsApi';
import { ListItem, CreateItemRequest } from '../types/Item';

interface UseItemsReturn {
  // States
  items: ListItem[];
  loadingItems: boolean;
  errorItems: string | null;
  addingItem: boolean;
  togglingItemId: string | null;

  // Actions
  fetchItems: (listId: string) => Promise<void>;
  addItem: (listId: string, request: CreateItemRequest) => Promise<ListItem>;
  toggleItem: (listId: string, itemId: string) => Promise<ListItem>;
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
  const [togglingItemId, setTogglingItemId] = useState<string | null>(null);

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
   * Marca/desmarca um item como concluído (toggle)
   * Implementa optimistic update: atualiza UI imediatamente e reverte em caso de erro
   */
  const toggleItem = useCallback(
    async (listId: string, itemId: string): Promise<ListItem> => {
      setTogglingItemId(itemId);
      setErrorItems(null);

      // Encontrar item atual e seu estado
      const item = items.find((i) => i.id === itemId);
      if (!item) {
        setTogglingItemId(null);
        throw new Error('Item não encontrado');
      }

      const originalChecked = item.checked;
      const newChecked = !originalChecked;

      // Optimistic update: atualizar estado local imediatamente
      setItems((prev) =>
        prev.map((i) => (i.id === itemId ? { ...i, checked: newChecked } : i))
      );

      try {
        // Fazer request para backend
        const updated = await itemsApi.toggleItemCheck(listId, itemId);
        return updated;
      } catch (err) {
        // Reverter em caso de erro
        setItems((prev) =>
          prev.map((i) =>
            i.id === itemId ? { ...i, checked: originalChecked } : i
          )
        );
        const message =
          err instanceof Error ? err.message : 'Erro ao atualizar item';
        setErrorItems(message);
        throw new Error(message);
      } finally {
        setTogglingItemId(null);
      }
    },
    [items]
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
    togglingItemId,
    fetchItems,
    addItem,
    toggleItem,
    clearItemsError,
  };
};
