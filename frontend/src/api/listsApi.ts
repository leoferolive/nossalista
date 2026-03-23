import client, { preserveSessionOnUnauthorizedConfig } from './client'
import {
  CreateListRequest,
  ListResponse,
  ListStateResponse,
  InviteLinkResponse,
  JoinListResponse,
  ListJoinedResponse,
  UserSearchResult,
  InviteByUsernameResponse,
  ListMemberResponse,
} from '../types/List'
import { ActivityResponse } from '../types/ActivityLog'
import { handleApiError } from './handleApiError'

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
    const response = await client.post<ListResponse>(
      '/api/lists',
      request,
      preserveSessionOnUnauthorizedConfig
    )
    return response.data
  },

  /**
   * Busca todas as listas do usuário autenticado
   * @returns Promise com array de listas
   */
  async getAllLists(): Promise<ListResponse[]> {
    const response = await client.get<ListResponse[]>(
      '/api/lists',
      preserveSessionOnUnauthorizedConfig
    )
    return response.data
  },

  /**
   * Busca uma lista específica por ID
   * @param id - UUID da lista
   * @returns Promise com a lista encontrada
   * @throws ApiError com status code e mensagem extraida do ProblemDetail
   */
  async getListById(id: string): Promise<ListResponse> {
    try {
      const response = await client.get<ListResponse>(
        `/api/lists/${id}`,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  async getListState(id: string): Promise<ListStateResponse> {
    try {
      const response = await client.get<ListStateResponse>(
        `/api/lists/${id}/state`,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Atualiza o nome de uma lista existente
   * @param id - UUID da lista
   * @param name - Novo nome da lista
   * @returns Promise com a lista atualizada
   * @throws ApiError com status code e mensagem extraida do ProblemDetail
   */
  async updateListName(id: string, name: string): Promise<ListResponse> {
    try {
      const response = await client.patch<ListResponse>(
        `/api/lists/${id}`,
        { name },
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
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
      await client.delete(`/api/lists/${id}`, preserveSessionOnUnauthorizedConfig)
    } catch (error) {
      handleApiError(error)
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
      const response = await client.post<InviteLinkResponse>(
        `/api/lists/${id}/invite-link`,
        undefined,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
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
      const response = await client.get<JoinListResponse>(`/api/lists/join/${inviteCode}`)
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Entra em uma lista via código de convite (endpoint autenticado)
   * Requer autenticação JWT - criar membro na lista
   * @param inviteCode - Código de convite
   * @returns Promise com dados da lista e mensagem de boas-vindas
   * @throws ApiError com status code para detecção de erro tipada
   */
  async joinList(inviteCode: string): Promise<ListJoinedResponse> {
    try {
      const response = await client.post<ListJoinedResponse>(
        `/api/lists/join/${inviteCode}`,
        undefined,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  async searchUsers(query: string): Promise<UserSearchResult[]> {
    try {
      const response = await client.get<UserSearchResult[]>('/api/users/search', {
        ...preserveSessionOnUnauthorizedConfig,
        params: { q: query },
      })
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  async inviteByUsername(listId: string, username: string): Promise<InviteByUsernameResponse> {
    try {
      const response = await client.post<InviteByUsernameResponse>(
        `/api/lists/${listId}/invite`,
        { username },
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  async getListMembers(listId: string): Promise<ListMemberResponse[]> {
    try {
      const response = await client.get<ListMemberResponse[]>(
        `/api/lists/${listId}/members`,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  async deleteListMember(listId: string, userId: string): Promise<void> {
    try {
      await client.delete(
        `/api/lists/${listId}/members/${userId}`,
        preserveSessionOnUnauthorizedConfig
      )
    } catch (error) {
      handleApiError(error)
    }
  },

  async leaveList(listId: string): Promise<void> {
    try {
      await client.post(
        `/api/lists/${listId}/leave`,
        undefined,
        preserveSessionOnUnauthorizedConfig
      )
    } catch (error) {
      handleApiError(error)
    }
  },

  async getActivities(listId: string, page = 0, size = 50): Promise<ActivityResponse> {
    try {
      const response = await client.get<ActivityResponse>(`/api/lists/${listId}/activity`, {
        ...preserveSessionOnUnauthorizedConfig,
        params: { page, size },
      })
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },
}
