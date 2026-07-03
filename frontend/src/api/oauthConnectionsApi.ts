import client from './client'
import { handleApiError } from './handleApiError'
import type { OAuthScope } from './oauthConsentApi'

export interface OAuthConnection {
  clientId: string
  clientName: string
  scope: OAuthScope
  createdAt: string
  lastUsedAt: string | null
}

/**
 * API de gestão de assistentes conectados via OAuth (claude.ai, Claude Code) —
 * exibida na tela "Conexões" ao lado dos Personal Access Tokens.
 */
export const oauthConnectionsApi = {
  async list(): Promise<OAuthConnection[]> {
    try {
      const response = await client.get<OAuthConnection[]>('/api/oauth/connections')
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Desconecta um assistente — revoga toda a família de refresh tokens desse cliente.
   */
  async revoke(clientId: string): Promise<void> {
    try {
      await client.post(`/api/oauth/connections/${clientId}/revoke`)
    } catch (error) {
      handleApiError(error)
    }
  },
}
