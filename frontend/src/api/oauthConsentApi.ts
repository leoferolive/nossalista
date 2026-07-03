import client from './client'
import { handleApiError } from './handleApiError'

export type OAuthScope = 'READ' | 'READ_WRITE'

export interface PendingAuthorization {
  requestId: string
  clientId: string
  clientName: string
  scope: OAuthScope
  redirectUriHost: string
}

export interface ConsentDecision {
  redirectUrl: string
}

/**
 * API por trás da tela de consentimento OAuth (`/oauth/consent`) usada por
 * clientes MCP externos (claude.ai, Claude Code) — ver docs/mcp.md.
 */
export const oauthConsentApi = {
  /**
   * Carrega os dados de um pedido de autorização pendente.
   * @throws ApiError 404 se o pedido não existir, já tiver sido decidido ou expirado
   */
  async get(requestId: string): Promise<PendingAuthorization> {
    try {
      const response = await client.get<PendingAuthorization>(`/api/oauth/consent/${requestId}`)
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Aprova o consentimento — o backend emite o authorization code e devolve a
   * URL de retorno ao cliente OAuth.
   */
  async approve(requestId: string): Promise<ConsentDecision> {
    try {
      const response = await client.post<ConsentDecision>(`/api/oauth/consent/${requestId}/approve`)
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Nega o consentimento — devolve a URL de retorno com `error=access_denied`.
   */
  async deny(requestId: string): Promise<ConsentDecision> {
    try {
      const response = await client.post<ConsentDecision>(`/api/oauth/consent/${requestId}/deny`)
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },
}
