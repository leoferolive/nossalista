import client from './client';
import { AxiosError } from 'axios';
import { CreateListRequest, ListResponse, InviteLinkResponse, JoinListResponse } from '../types/List';
import { ProblemDetail } from '../types/ProblemDetail';
import { ApiError } from '../types/ApiError';

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

  /**
   * Exclui uma lista existente
   * @param id - UUID da lista
   * @returns Promise void (204 No Content)
   * @throws ApiError com status code e mensagem específica
   */
  async deleteList(id: string): Promise<void> {
    try {
      await client.delete(`/api/lists/${id}`);
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 403) {
          throw new ApiError('Você não tem permissão para excluir esta lista', 403);
        } else if (status === 404) {
          throw new ApiError('Lista não encontrada', 404);
        } else if (status === 401) {
          throw new ApiError('Sessão expirada. Faça login novamente.', 401);
        } else {
          throw new ApiError(
            problemDetail?.detail || 'Erro ao excluir lista. Tente novamente.',
            status
          );
        }
      }

      throw new ApiError('Erro de conexão. Verifique sua internet.');
    }
  },

  /**
   * Gera ou retorna um link de convite válido para a lista
   * Apenas o dono da lista pode gerar links de convite
   * @param id - UUID da lista
   * @returns Promise com o link de convite gerado
   * @throws ApiError com status code e mensagem específica
   */
  async generateInviteLink(id: string): Promise<InviteLinkResponse> {
    try {
      const response = await client.post<InviteLinkResponse>(`/api/lists/${id}/invite-link`);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 403) {
          throw new ApiError('Apenas o dono pode gerar link de convite', 403);
        } else if (status === 404) {
          throw new ApiError('Lista não encontrada', 404);
        } else if (status === 401) {
          throw new ApiError('Sessão expirada. Faça login novamente.', 401);
        } else {
          throw new ApiError(
            problemDetail?.detail || 'Erro ao gerar link de convite. Tente novamente.',
            status
          );
        }
      }

      throw new ApiError('Erro de conexão. Verifique sua internet.');
    }
  },

  /**
   * Busca uma lista pelo código de convite (endpoint público, modo read-only)
   * Não requer autenticação - usa o client compartilhado que omite o header
   * Authorization quando não há token armazenado
   * @param inviteCode - Código de convite
   * @returns Promise com dados da lista em modo read-only
   * @throws ApiError com status code para detecção de erro tipada
   */
  async getListByInviteCode(inviteCode: string): Promise<JoinListResponse> {
    try {
      const response = await client.get<JoinListResponse>(`/api/lists/join/${inviteCode}`);
      return response.data;
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>;

      if (axiosError.response) {
        const status = axiosError.response.status;
        const problemDetail = axiosError.response.data;

        if (status === 404) {
          throw new ApiError('Convite não encontrado. Este link pode ter sido desativado ou não existe.', 404);
        } else if (status === 410) {
          throw new ApiError('Este link de convite expirou. Peça um novo link ao dono da lista.', 410);
        } else {
          throw new ApiError(
            problemDetail?.detail || 'Erro ao carregar lista. Tente novamente.',
            status
          );
        }
      }

      throw new ApiError('Erro de conexão. Verifique sua internet.');
    }
  },
};
