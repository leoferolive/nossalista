import client from './client'
import { handleApiError } from './handleApiError'

export interface RegisterPayload {
  email: string
  username: string
  password: string
  name?: string
}

export interface RegisterResponse {
  id: string
  username: string
  email: string
  name: string | null
  avatarUrl: string | null
  authProvider: string
  createdAt: string
}

export const authApi = {
  /**
   * Cria uma nova conta de usuário (email/senha)
   * @param payload - Dados de cadastro
   * @returns Promise com os dados do usuário criado
   * @throws ApiError com status code (ex.: 409 email/username em uso, 400 dados invalidos)
   */
  async register(payload: RegisterPayload): Promise<RegisterResponse> {
    try {
      const response = await client.post<RegisterResponse>('/api/auth/register', payload)
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Solicita o envio de um link de redefinição de senha
   * @param email - Email da conta
   * @throws ApiError com status code em caso de falha
   */
  async forgotPassword(email: string): Promise<void> {
    try {
      await client.post('/api/auth/forgot-password', { email })
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Redefine a senha a partir de um token de redefinição
   * @param token - Token recebido por email
   * @param newPassword - Nova senha
   * @throws ApiError com status code (ex.: 400 token invalido ou expirado)
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    try {
      await client.post('/api/auth/reset-password', { token, newPassword })
    } catch (error) {
      handleApiError(error)
    }
  },
}
