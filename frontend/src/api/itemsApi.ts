import client from './client';
import { ListItem, CreateItemRequest, UpdateItemRequest } from '../types/Item';
import { ProblemDetail } from '../types/ProblemDetail';
import { AxiosError } from 'axios';

/**
 * API para gerenciamento de itens de lista
 */
export const itemsApi = {
  /**
   * Busca todos os itens de uma lista
   * @param listId - UUID da lista
   * @returns Promise com array de itens ordenados por position
   * @throws Error com mensagem específica baseada no status HTTP
   */
  async getItemsByListId(listId: string): Promise<ListItem[]> {
    try {
      const response = await client.get<ListItem[]>(`/api/lists/${listId}/items`);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 404) {
          throw new Error('Lista não encontrada');
        } else if (status === 403) {
          throw new Error('Você não tem permissão para ver os itens desta lista');
        } else if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.');
        } else {
          throw new Error(
            problemDetail?.detail || 'Erro ao carregar itens. Tente novamente.'
          );
        }
      }

      throw new Error('Erro de conexão. Verifique sua internet.');
    }
  },

  /**
   * Adiciona um novo item à lista
   * @param listId - UUID da lista
   * @param request - Dados do item (name, quantity, dueDate, url, notes)
   * @returns Promise com o item criado
   * @throws Error com mensagem específica baseada no status HTTP
   */
  async addItem(listId: string, request: CreateItemRequest): Promise<ListItem> {
    try {
      const response = await client.post<ListItem>(`/api/lists/${listId}/items`, request);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 400) {
          throw new Error(problemDetail?.detail || 'Dados inválidos. Verifique os campos.');
        } else if (status === 403) {
          throw new Error('Você não tem permissão para adicionar itens nesta lista');
        } else if (status === 404) {
          throw new Error('Lista não encontrada');
        } else if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.');
        } else {
          throw new Error(
            problemDetail?.detail || 'Erro ao adicionar item. Tente novamente.'
          );
        }
      }

      throw new Error('Erro de conexão. Verifique sua internet.');
    }
  },

  /**
   * Marca/desmarca um item como concluído (toggle)
   * @param listId - UUID da lista
   * @param itemId - UUID do item
   * @returns Promise com o item atualizado
   * @throws Error com mensagem específica baseada no status HTTP
   */
  async toggleItemCheck(listId: string, itemId: string): Promise<ListItem> {
    try {
      const response = await client.patch<ListItem>(`/api/lists/${listId}/items/${itemId}/check`);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 404) {
          throw new Error('Item não encontrado');
        } else if (status === 403) {
          throw new Error('Você não tem permissão para modificar itens desta lista');
        } else if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.');
        } else {
          throw new Error(
            problemDetail?.detail || 'Erro ao atualizar item. Tente novamente.'
          );
        }
      }

      throw new Error('Erro de conexão. Verifique sua internet.');
    }
  },

  /**
   * Atualiza um item existente
   * @param listId - UUID da lista
   * @param itemId - UUID do item
   * @param request - Dados para atualizar (name, quantity, dueDate, url)
   * @returns Promise com o item atualizado
   * @throws Error com mensagem específica baseada no status HTTP
   */
  async updateItem(listId: string, itemId: string, request: UpdateItemRequest): Promise<ListItem> {
    try {
      const response = await client.patch<ListItem>(`/api/lists/${listId}/items/${itemId}`, request);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 400) {
          throw new Error(problemDetail?.detail || 'Campos inválidos para este tipo de lista');
        } else if (status === 403) {
          throw new Error('Você não tem permissão para editar itens desta lista');
        } else if (status === 404) {
          throw new Error('Item não encontrado');
        } else if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.');
        } else {
          throw new Error(
            problemDetail?.detail || 'Erro ao atualizar item. Tente novamente.'
          );
        }
      }

      throw new Error('Erro de conexão. Verifique sua internet.');
    }
  },
};
