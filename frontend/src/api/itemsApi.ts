import client, { preserveSessionOnUnauthorizedConfig } from './client'
import { ListItem, CreateItemRequest, UpdateItemRequest } from '../types/Item'
import { handleApiError } from './handleApiError'

/**
 * API para gerenciamento de itens de lista
 */
export const itemsApi = {
  /**
   * Busca todos os itens de uma lista
   * @param listId - UUID da lista
   * @returns Promise com array de itens ordenados por position
   * @throws ApiError com status code e mensagem extraida do ProblemDetail
   */
  async getItemsByListId(listId: string): Promise<ListItem[]> {
    try {
      const response = await client.get<ListItem[]>(
        `/api/lists/${listId}/items`,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Adiciona um novo item à lista
   * @param listId - UUID da lista
   * @param request - Dados do item (name, quantity, dueDate, url, notes)
   * @returns Promise com o item criado
   * @throws ApiError com status code e mensagem extraida do ProblemDetail
   */
  async addItem(listId: string, request: CreateItemRequest): Promise<ListItem> {
    try {
      const response = await client.post<ListItem>(
        `/api/lists/${listId}/items`,
        request,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Marca/desmarca um item como concluído (toggle)
   * @param listId - UUID da lista
   * @param itemId - UUID do item
   * @returns Promise com o item atualizado
   * @throws ApiError com status code e mensagem extraida do ProblemDetail
   */
  async toggleItemCheck(listId: string, itemId: string): Promise<ListItem> {
    try {
      const response = await client.patch<ListItem>(
        `/api/lists/${listId}/items/${itemId}/check`,
        undefined,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Atualiza um item existente
   * @param listId - UUID da lista
   * @param itemId - UUID do item
   * @param request - Dados para atualizar (name, quantity, dueDate, url)
   * @returns Promise com o item atualizado
   * @throws ApiError com status code e mensagem extraida do ProblemDetail
   */
  async updateItem(listId: string, itemId: string, request: UpdateItemRequest): Promise<ListItem> {
    try {
      const response = await client.patch<ListItem>(
        `/api/lists/${listId}/items/${itemId}`,
        request,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Remove um item da lista
   * @param listId - UUID da lista
   * @param itemId - UUID do item
   * @returns Promise<void> (204 No Content)
   * @throws ApiError com status code e mensagem extraida do ProblemDetail
   */
  async deleteItem(listId: string, itemId: string): Promise<void> {
    try {
      await client.delete(
        `/api/lists/${listId}/items/${itemId}`,
        preserveSessionOnUnauthorizedConfig
      )
    } catch (error) {
      handleApiError(error)
    }
  },
}
