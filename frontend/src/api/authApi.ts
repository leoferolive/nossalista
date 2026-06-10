import { AxiosError } from 'axios'
import client from './client'
import { ProblemDetail } from '../types/ProblemDetail'

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

function extractProblemMessage(error: unknown, fallback: string) {
  const axiosError = error as AxiosError<ProblemDetail>
  return axiosError.response?.data?.detail || fallback
}

export const authApi = {
  async register(payload: RegisterPayload): Promise<RegisterResponse> {
    try {
      const response = await client.post<RegisterResponse>('/api/auth/register', payload)
      return response.data
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>

      if (axiosError.response?.status === 409) {
        throw new Error(extractProblemMessage(error, 'Email ou username ja estao em uso.'))
      }

      if (axiosError.response?.status === 400) {
        throw new Error(
          extractProblemMessage(error, 'Confira os dados informados e tente novamente.')
        )
      }

      throw new Error(extractProblemMessage(error, 'Nao foi possivel criar sua conta agora.'))
    }
  },

  /**
   * Q2.3: troca o one-time code do OAuth2 pelo JWT (o token não vem mais na URL).
   */
  async exchangeOAuthCode(code: string): Promise<OAuthExchangeResponse> {
    try {
      const response = await client.post<OAuthExchangeResponse>('/api/auth/oauth/exchange', {
        code,
      })
      return response.data
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>

      if (axiosError.response?.status === 400) {
        throw new Error(
          extractProblemMessage(error, 'Link de login expirado ou inválido. Tente novamente.')
        )
      }

      throw new Error(extractProblemMessage(error, 'Não foi possível concluir o login com Google.'))
    }
  },

  async forgotPassword(email: string): Promise<void> {
    await client.post('/api/auth/forgot-password', { email })
  },

  /**
   * Q2.7: confirma a verificação de e-mail usando o token recebido por e-mail.
   */
  async verifyEmail(token: string): Promise<void> {
    try {
      await client.get('/api/auth/verify-email', { params: { token } })
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>

      if (axiosError.response?.status === 400) {
        throw new Error(
          extractProblemMessage(
            error,
            'Token de verificação inválido ou expirado. Solicite um novo link.'
          )
        )
      }

      throw new Error(extractProblemMessage(error, 'Não foi possível verificar seu e-mail agora.'))
    }
  },

  /**
   * Q2.7: reenvia o e-mail de verificação. Sempre resolve (não revela existência).
   */
  async resendVerification(email: string): Promise<void> {
    await client.post('/api/auth/resend-verification', { email })
  },

  async resetPassword(token: string, newPassword: string): Promise<void> {
    try {
      await client.post('/api/auth/reset-password', { token, newPassword })
    } catch (error) {
      const axiosError = error as AxiosError<ProblemDetail>

      if (axiosError.response?.status === 400) {
        throw new Error(
          extractProblemMessage(error, 'Token invalido ou expirado. Solicite um novo link.')
        )
      }

      throw new Error(extractProblemMessage(error, 'Nao foi possivel redefinir sua senha agora.'))
    }
  },
}
