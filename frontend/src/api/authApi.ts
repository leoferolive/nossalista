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

export interface OAuthExchangeResponse {
  id: string
  username: string
  email: string
  name: string | null
  avatarUrl: string | null
  onboardingCompletedAt: string | null
  authProvider: string
  createdAt: string
  token: string
  expiresAt: string
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
   * Q2.3: troca o one-time code do OAuth2 pelo JWT (o token não vem mais na URL).
   * @param code - One-time code recebido no callback do OAuth2
   * @returns Promise com os dados do usuário e o JWT
   * @throws ApiError com status code (ex.: 400 code inválido ou expirado)
   */
  async exchangeOAuthCode(code: string): Promise<OAuthExchangeResponse> {
    try {
      const response = await client.post<OAuthExchangeResponse>('/api/auth/oauth/exchange', {
        code,
      })
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
   * Q2.7: confirma a verificação de e-mail usando o token recebido por e-mail.
   * @param token - Token de verificação recebido por email
   * @throws ApiError com status code (ex.: 400 token inválido ou expirado)
   */
  async verifyEmail(token: string): Promise<void> {
    try {
      await client.get('/api/auth/verify-email', { params: { token } })
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Q2.7: reenvia o e-mail de verificação.
   * @param email - Email da conta
   * @throws ApiError com status code em caso de falha
   */
  async resendVerification(email: string): Promise<void> {
    try {
      await client.post('/api/auth/resend-verification', { email })
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
