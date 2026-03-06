import React, { useState, useCallback } from 'react'
import { itemsApi } from '../api/itemsApi'
import { ListItem, CreateItemRequest, UpdateItemRequest } from '../types/Item'

interface UseItemsReturn {
  // States
  items: ListItem[]
  loadingItems: boolean
  errorItems: string | null
  addingItem: boolean
  togglingItemId: string | null
  updatingItemId: string | null
  deletingItemId: string | null

  // Actions
  setItems: React.Dispatch<React.SetStateAction<ListItem[]>>
  fetchItems: (listId: string) => Promise<void>
  addItem: (listId: string, request: CreateItemRequest) => Promise<ListItem>
  toggleItem: (listId: string, itemId: string) => Promise<ListItem>
  updateItem: (listId: string, itemId: string, request: UpdateItemRequest) => Promise<ListItem>
  deleteItem: (listId: string, itemId: string) => Promise<void>
  clearItemsError: () => void
}

/**
 * Hook para gerenciar estado de itens de uma lista
 */
export const useItems = (): UseItemsReturn => {
  const [items, setItems] = useState<ListItem[]>([])
  const [loadingItems, setLoadingItems] = useState(false)
  const [errorItems, setErrorItems] = useState<string | null>(null)
  const [addingItem, setAddingItem] = useState(false)
  const [togglingItemId, setTogglingItemId] = useState<string | null>(null)
  const [updatingItemId, setUpdatingItemId] = useState<string | null>(null)
  const [deletingItemId, setDeletingItemId] = useState<string | null>(null)

  /**
   * Busca todos os itens de uma lista
   */
  const fetchItems = useCallback(async (listId: string) => {
    setLoadingItems(true)
    setErrorItems(null)

    try {
      const data = await itemsApi.getItemsByListId(listId)
      setItems(data)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro ao carregar itens'
      setErrorItems(message)
    } finally {
      setLoadingItems(false)
    }
  }, [])

  /**
   * Adiciona um novo item à lista
   * Mantém a lista ordenada por position após adicionar
   */
  const addItem = useCallback(
    async (listId: string, request: CreateItemRequest): Promise<ListItem> => {
      setAddingItem(true)
      setErrorItems(null)

      try {
        const newItem = await itemsApi.addItem(listId, request)
        // Adiciona sem reordenar - o backend já retorna itens ordenados por position
        setItems((prev) => [...prev, newItem])
        return newItem
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Erro ao adicionar item'
        setErrorItems(message)
        throw new Error(message)
      } finally {
        setAddingItem(false)
      }
    },
    []
  )

  /**
   * Marca/desmarca um item como concluído (toggle)
   * Implementa optimistic update: atualiza UI imediatamente e reverte em caso de erro
   */
  const toggleItem = useCallback(
    async (listId: string, itemId: string): Promise<ListItem> => {
      setTogglingItemId(itemId)
      setErrorItems(null)

      // Encontrar item atual e seu estado
      const item = items.find((i) => i.id === itemId)
      if (!item) {
        setTogglingItemId(null)
        throw new Error('Item não encontrado')
      }

      const originalChecked = item.checked
      const newChecked = !originalChecked

      // Optimistic update: atualizar estado local imediatamente
      setItems((prev) => prev.map((i) => (i.id === itemId ? { ...i, checked: newChecked } : i)))

      try {
        // Fazer request para backend
        const updated = await itemsApi.toggleItemCheck(listId, itemId)
        return updated
      } catch (err) {
        // Reverter em caso de erro
        setItems((prev) =>
          prev.map((i) => (i.id === itemId ? { ...i, checked: originalChecked } : i))
        )
        const message = err instanceof Error ? err.message : 'Erro ao atualizar item'
        setErrorItems(message)
        throw new Error(message)
      } finally {
        setTogglingItemId(null)
      }
    },
    [items]
  )

  /**
   * Atualiza um item existente
   * Implementa optimistic update: atualiza UI imediatamente e reverte em caso de erro
   */
  const updateItem = useCallback(
    async (listId: string, itemId: string, request: UpdateItemRequest): Promise<ListItem> => {
      setUpdatingItemId(itemId)
      setErrorItems(null)

      // Encontrar item atual e seu estado
      const item = items.find((i) => i.id === itemId)
      if (!item) {
        setUpdatingItemId(null)
        throw new Error('Item não encontrado')
      }

      // Backup do estado original para rollback
      const originalItem = { ...item }

      // Optimistic update: merge inteligente - apenas atualiza campos fornecidos
      setItems((prev) =>
        prev.map((i) =>
          i.id === itemId
            ? {
                ...i,
                ...(request.name !== undefined && { name: request.name }),
                ...(request.quantity !== undefined && { quantity: request.quantity }),
                ...(request.dueDate !== undefined && { dueDate: request.dueDate }),
                ...(request.url !== undefined && { url: request.url }),
              }
            : i
        )
      )

      try {
        const updated = await itemsApi.updateItem(listId, itemId, request)
        return updated
      } catch (err) {
        // Reverter em caso de erro
        setItems((prev) => prev.map((i) => (i.id === itemId ? originalItem : i)))
        const message = err instanceof Error ? err.message : 'Erro ao atualizar item'
        setErrorItems(message)
        throw new Error(message)
      } finally {
        setUpdatingItemId(null)
      }
    },
    [items]
  )

  /**
   * Remove um item da lista
   * AC Story 3.6: Fade-out animation (200ms) before removing
   * Aguarda animação CSS completar antes de remover do estado
   */
  const deleteItem = useCallback(
    async (listId: string, itemId: string): Promise<void> => {
      setDeletingItemId(itemId)
      setErrorItems(null)

      // Encontrar item atual
      const item = items.find((i) => i.id === itemId)
      if (!item) {
        setDeletingItemId(null)
        throw new Error('Item não encontrado')
      }

      try {
        // Fazer request para backend
        await itemsApi.deleteItem(listId, itemId)

        // Aguardar 200ms para animação fade-out completar
        await new Promise((resolve) => setTimeout(resolve, 200))

        // Remover do estado local após animação
        setItems((prev) => prev.filter((i) => i.id !== itemId))
      } catch (err) {
        // Em caso de erro, não remove do estado
        const message = err instanceof Error ? err.message : 'Erro ao remover item'
        setErrorItems(message)
        throw new Error(message)
      } finally {
        setDeletingItemId(null)
      }
    },
    [items]
  )

  /**
   * Limpa o erro de itens
   */
  const clearItemsError = useCallback(() => {
    setErrorItems(null)
  }, [])

  return {
    items,
    setItems,
    loadingItems,
    errorItems,
    addingItem,
    togglingItemId,
    updatingItemId,
    deletingItemId,
    fetchItems,
    addItem,
    toggleItem,
    updateItem,
    deleteItem,
    clearItemsError,
  }
}
