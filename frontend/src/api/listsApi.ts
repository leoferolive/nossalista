import client from './client';
import { CreateListRequest, ListResponse } from '../types/List';
import { ProblemDetail } from '../types/ProblemDetail';
import { AxiosError } from 'axios';

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
   * @throws Error com mensagem específica baseada no status HTTP
   */
  async getListById(id: string): Promise<ListResponse> {
    try {
      const response = await client.get<ListResponse>(`/api/lists/${id}`);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 404) {
          throw new Error('Lista não encontrada');
        } else if (status === 403) {
          throw new Error('Você não tem permissão para acessar esta lista');
        } else if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.');
        } else {
          throw new Error(
            problemDetail?.detail || 'Erro ao carregar lista. Tente novamente.'
          );
        }
      }

      throw new Error('Erro de conexão. Verifique sua internet.');
    }
  },

  /**
   * Atualiza o nome de uma lista existente
   * @param id - UUID da lista
   * @param name - Novo nome da lista
   * @returns Promise com a lista atualizada
   * @throws Error com mensagem específica baseada no status HTTP
   */
  async updateListName(id: string, name: string): Promise<ListResponse> {
    try {
      const response = await client.patch<ListResponse>(`/api/lists/${id}`, { name });
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 400) {
          throw new Error(problemDetail?.detail || 'Nome inválido. Use entre 3 e 100 caracteres.');
        } else if (status === 403) {
          throw new Error('Você não tem permissão para editar esta lista');
        } else if (status === 404) {
          throw new Error('Lista não encontrada');
        } else if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.');
        } else {
          throw new Error(
            problemDetail?.detail || 'Erro ao atualizar lista. Tente novamente.'
          );
        }
      }

      throw new Error('Erro de conexão. Verifique sua internet.');
    }
  },
};
