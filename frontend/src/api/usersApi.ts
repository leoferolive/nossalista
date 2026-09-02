import client, { preserveSessionOnUnauthorizedConfig } from './client'
import { handleApiError } from './handleApiError'

export interface UserProfileResponse {
  username: string
  email: string
  name: string | null
  avatarUrl: string | null
  authProvider: string
  onboardingCompletedAt: string | null
}

export interface UpdateProfileRequest {
  name: string
  avatarUrl?: string
}

/**
 * API para gerenciamento de usuários
 */
export const usersApi = {
  /**
   * Obtém o perfil do usuário autenticado
   * @returns Promise com dados do perfil
   */
  async getProfile(): Promise<UserProfileResponse> {
    try {
      const response = await client.get<UserProfileResponse>(
        '/api/users/me',
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Atualiza informações do perfil do usuário autenticado
   * @param data - Dados para atualizar (name, avatarUrl)
   * @returns Promise com dados atualizados
   */
  async updateProfile(data: UpdateProfileRequest): Promise<UserProfileResponse> {
    try {
      const response = await client.patch<UserProfileResponse>(
        '/api/users/me',
        data,
        preserveSessionOnUnauthorizedConfig
      )
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Encerra a sessão no servidor, expirando o cookie HttpOnly.
   */
  async logout(): Promise<void> {
    await client.post('/api/auth/logout')
  },

  async completeOnboarding(): Promise<void> {
    try {
      await client.post(
        '/api/users/me/onboarding/complete',
        undefined,
        preserveSessionOnUnauthorizedConfig
      )
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Exclui permanentemente a conta do usuário autenticado (LGPD).
   * Remove todas as listas, itens, membros e dados associados.
   * @returns Promise void
   */
  async deleteAccount(): Promise<void> {
    try {
      await client.delete('/api/users/me')
    } catch (error) {
      handleApiError(error)
    }
  },
}
