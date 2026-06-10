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

  describe('forgotPassword', () => {
    it('propaga ApiError com status do servidor em caso de falha', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(500, 'Falha interna.'))

      await expect(authApi.forgotPassword('a@b.com')).rejects.toMatchObject({
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
