import client, { preserveSessionOnUnauthorizedConfig } from './client'
import { AxiosError } from 'axios'
import { ProblemDetail } from '../types/ProblemDetail'
import { ApiError } from '../types/ApiError'

export interface UserProfileResponse {
  username: string
  email: string
  name: string | null
  avatarUrl: string | null
  authProvider: string
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
      const axiosError = error as AxiosError<ProblemDetail>

      if (axiosError.response) {
        const status = axiosError.response.status
        const problemDetail = axiosError.response.data

        if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.')
        }

        throw new Error(problemDetail?.detail || 'Erro ao carregar perfil. Tente novamente.')
      }

      throw new Error('Erro de conexão. Verifique sua internet.')
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
      const axiosError = error as AxiosError<ProblemDetail>

      if (axiosError.response) {
        const status = axiosError.response.status
        const problemDetail = axiosError.response.data

        if (status === 400) {
          throw new Error(
            problemDetail?.detail || 'Dados inválidos. Verifique o nome e tente novamente.'
          )
        } else if (status === 401) {
          throw new Error('Sessão expirada. Faça login novamente.')
        }

        throw new ApiError(
          problemDetail?.detail || 'Erro ao atualizar perfil. Tente novamente.',
          status
        )
      }

      throw new ApiError('Erro de conexão. Verifique sua internet.', 500)
    }
  },

  /**
   * Faz logout do usuário
   * Apenas remove o token do localStorage, o redirecionamento é tratado pelo componente
   * @returns Promise void
   */
  async logout(): Promise<void> {
    // Em uma implementação completa, isso pode chamar um endpoint de logout no backend
    // Por enquanto, a função principal é limpar o token e redirecionar
    return Promise.resolve()
  },
}
