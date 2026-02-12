import client from './client';
import { CreateListRequest, ListResponse } from '../types/List';

/**
 * API para gerenciamento de listas
 */
export const listsApi = {
  /**
   * Cria uma nova lista
   * @param request - Dados da lista (name, typeId)
   * @returns Promise com a lista criada
   */
  async createList(request: CreateListRequest): Promise<ListResponse> {
    const response = await client.post<ListResponse>('/api/lists', request);
    return response.data;
  },

  /**
   * Busca todas as listas do usuário autenticado
   * @returns Promise com array de listas
   */
  async getAllLists(): Promise<ListResponse[]> {
    const response = await client.get<ListResponse[]>('/api/lists');
    return response.data;
  },

  /**
   * Busca uma lista específica por ID
   * @param id - UUID da lista
   * @returns Promise com a lista encontrada
   */
  async getListById(id: string): Promise<ListResponse> {
    const response = await client.get<ListResponse>(`/api/lists/${id}`);
    return response.data;
  },
};
