import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('./client', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

import client from './client'
import { authApi } from './authApi'

const mockedClient = vi.mocked(client)

function axiosErrorWithStatus(status: number, detail?: string) {
  return {
    isAxiosError: true,
    response: { status, data: detail ? { detail } : undefined },
  }
}

describe('authApi.exchangeOAuthCode (Q2.3)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

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
    mockedClient.post.mockResolvedValueOnce({ data: payload })

    const result = await authApi.exchangeOAuthCode('the-code')

    expect(mockedClient.post).toHaveBeenCalledWith('/api/auth/oauth/exchange', { code: 'the-code' })
    expect(result.token).toBe('jwt-token')
    expect(result.email).toBe('leo@gmail.com')
  })

  it('lança erro amigável quando o code é inválido/expirado (400)', async () => {
    mockedClient.post.mockRejectedValueOnce(axiosErrorWithStatus(400, 'Código inválido'))

    await expect(authApi.exchangeOAuthCode('bad')).rejects.toThrow('Código inválido')
  })
})

describe('authApi.verifyEmail (Q2.7)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('chama o endpoint de verificação com o token', async () => {
    mockedClient.get.mockResolvedValueOnce({ data: null })

    await authApi.verifyEmail('verify-token')

    expect(mockedClient.get).toHaveBeenCalledWith('/api/auth/verify-email', {
      params: { token: 'verify-token' },
    })
  })

  it('lança erro amigável para token inválido (400)', async () => {
    mockedClient.get.mockRejectedValueOnce(axiosErrorWithStatus(400, 'Token expirado'))

    await expect(authApi.verifyEmail('bad-token')).rejects.toThrow('Token expirado')
  })
})

describe('authApi.resendVerification (Q2.7)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('faz POST com o e-mail e resolve sem lançar', async () => {
    mockedClient.post.mockResolvedValueOnce({ data: null })

    await expect(authApi.resendVerification('leo@gmail.com')).resolves.toBeUndefined()
    expect(mockedClient.post).toHaveBeenCalledWith('/api/auth/resend-verification', {
      email: 'leo@gmail.com',
    })
  })
})
