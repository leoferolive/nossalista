import client from './client'
import { handleApiError } from './handleApiError'

export type TokenScope = 'READ' | 'READ_WRITE'

export interface PersonalAccessToken {
  id: string
  name: string
  prefix: string
  scope: TokenScope
  expiresAt: string | null
  lastUsedAt: string | null
  createdAt: string
}

export interface PersonalAccessTokenCreated extends PersonalAccessToken {
  token: string
}

export interface CreateTokenRequest {
  name: string
  scope: TokenScope
  expiresInDays?: number
}

/**
 * API para gestão de Personal Access Tokens (PAT) — autenticação de
 * clientes MCP/API externos.
 */
export const tokensApi = {
  /**
   * Lista os tokens do usuário autenticado (metadados, sem o valor em claro).
   */
  async list(): Promise<PersonalAccessToken[]> {
    try {
      const response = await client.get<PersonalAccessToken[]>('/api/users/me/tokens')
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Cria um novo token. O valor em claro só é retornado nesta chamada.
   */
  async create(data: CreateTokenRequest): Promise<PersonalAccessTokenCreated> {
    try {
      const response = await client.post<PersonalAccessTokenCreated>('/api/users/me/tokens', data)
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Revoga um token. Idempotente no backend.
   */
  async revoke(id: string): Promise<void> {
    try {
      await client.delete(`/api/users/me/tokens/${id}`)
    } catch (error) {
      handleApiError(error)
    }
  },
}
