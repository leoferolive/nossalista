import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { authApi } from './authApi'
import client from './client'
import { ApiError } from '../types/ApiError'
import type { ProblemDetail } from '../types/ProblemDetail'

function buildAxiosError(status: number, detail: string, type?: string): AxiosError<ProblemDetail> {
  const data: ProblemDetail = {
    type: type ?? 'about:blank',
    title: 'Error',
    status,
    detail,
  }

  const response = {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data,
  } as AxiosResponse<ProblemDetail>

  return new AxiosError<ProblemDetail>(
    `Request failed with status code ${status}`,
    String(status),
    response.config,
    undefined,
    response
  )
}

describe('authApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('register', () => {
    it('lanca ApiError com status 409 quando email/username ja existe', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(
        buildAxiosError(409, 'Email ou username ja estao em uso.')
      )

      await expect(
        authApi.register({ email: 'a@b.com', username: 'leo', password: '123456' })
      ).rejects.toMatchObject({
        name: 'ApiError',
        status: 409,
        message: 'Email ou username ja estao em uso.',
      })
    })

    it('lanca ApiError com status 400 quando os dados sao invalidos', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(
        buildAxiosError(400, 'Confira os dados informados.')
      )

      const error = await authApi
        .register({ email: 'a@b.com', username: 'leo', password: '123' })
        .catch((e) => e)

      expect(error).toBeInstanceOf(ApiError)
      expect(error).toBeInstanceOf(Error)
      expect(error.status).toBe(400)
      expect(error.message).toBe('Confira os dados informados.')
    })
  })

  describe('exchangeOAuthCode (Q2.3)', () => {
    it('troca o code pelo JWT e retorna a resposta', async () => {
      const payload = {
        id: 'u1',
        username: 'leo',
        email: 'leo@gmail.com',
        name: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
        authProvider: 'GOOGLE',
        createdAt: '2026-01-01T00:00:00Z',
        token: 'jwt-token',
        expiresAt: '2026-01-08T00:00:00Z',
      }
      const postSpy = vi.spyOn(client, 'post').mockResolvedValueOnce({ data: payload })

      const result = await authApi.exchangeOAuthCode('the-code')

      expect(postSpy).toHaveBeenCalledWith('/api/auth/oauth/exchange', { code: 'the-code' })
      expect(result.token).toBe('jwt-token')
      expect(result.email).toBe('leo@gmail.com')
    })

    it('lanca ApiError com status 400 quando o code e invalido/expirado', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(400, 'Código inválido'))

      await expect(authApi.exchangeOAuthCode('bad')).rejects.toMatchObject({
        name: 'ApiError',
        status: 400,
        message: 'Código inválido',
      })
    })
  })

  describe('forgotPassword', () => {
    it('propaga ApiError com status do servidor em caso de falha', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(500, 'Falha interna.'))

      await expect(authApi.forgotPassword('a@b.com')).rejects.toMatchObject({
        name: 'ApiError',
        status: 500,
      })
    })
  })

  describe('verifyEmail (Q2.7)', () => {
    it('chama o endpoint de verificação com o token', async () => {
      const getSpy = vi.spyOn(client, 'get').mockResolvedValueOnce({ data: null })

      await authApi.verifyEmail('verify-token')

      expect(getSpy).toHaveBeenCalledWith('/api/auth/verify-email', {
        params: { token: 'verify-token' },
      })
    })

    it('lanca ApiError com status 400 para token invalido', async () => {
      vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(400, 'Token expirado'))

      await expect(authApi.verifyEmail('bad-token')).rejects.toMatchObject({
        name: 'ApiError',
        status: 400,
        message: 'Token expirado',
      })
    })
  })

  describe('resendVerification (Q2.7)', () => {
    it('faz POST com o e-mail e resolve sem lançar', async () => {
      const postSpy = vi.spyOn(client, 'post').mockResolvedValueOnce({ data: null })

      await expect(authApi.resendVerification('leo@gmail.com')).resolves.toBeUndefined()
      expect(postSpy).toHaveBeenCalledWith('/api/auth/resend-verification', {
        email: 'leo@gmail.com',
      })
    })

    it('propaga ApiError quando o servidor falha', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(500, 'Falha interna.'))

      await expect(authApi.resendVerification('leo@gmail.com')).rejects.toMatchObject({
        name: 'ApiError',
        status: 500,
      })
    })
  })

  describe('resetPassword', () => {
    it('lanca ApiError com status 400 quando o token e invalido ou expirado', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(
        buildAxiosError(400, 'Token invalido ou expirado.')
      )

      await expect(authApi.resetPassword('bad-token', '123456')).rejects.toMatchObject({
        name: 'ApiError',
        status: 400,
        message: 'Token invalido ou expirado.',
      })
    })
  })
})
