import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosResponse } from 'axios'
import { AxiosError, AxiosHeaders } from 'axios'
import { usersApi } from './usersApi'
import client from './client'

function buildAxiosError(status: number): AxiosError {
  const response = {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { detail: 'Falhou' },
  } as AxiosResponse

  return new AxiosError(`Request failed with status code ${status}`, String(status), response.config, undefined, response)
}

describe('usersApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('getProfile retorna o perfil do usuario autenticado', async () => {
    const profile = {
      username: 'leo',
      email: 'leo@example.com',
      name: 'Leo',
      avatarUrl: null,
      authProvider: 'GOOGLE',
      onboardingCompletedAt: null,
    }
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: profile } as AxiosResponse)

    await expect(usersApi.getProfile()).resolves.toEqual(profile)
    expect(client.get).toHaveBeenCalledWith('/api/users/me', expect.any(Object))
  })

  it('getProfile propaga ApiError quando nao autenticado', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(401))

    await expect(usersApi.getProfile()).rejects.toMatchObject({ status: 401 })
  })

  it('updateProfile atualiza o perfil e retorna os dados atualizados', async () => {
    const updated = {
      username: 'leo',
      email: 'leo@example.com',
      name: 'Leonardo',
      avatarUrl: 'https://avatar.example.com/leo.png',
      authProvider: 'GOOGLE',
      onboardingCompletedAt: null,
    }
    vi.spyOn(client, 'patch').mockResolvedValueOnce({ data: updated } as AxiosResponse)

    await expect(
      usersApi.updateProfile({ name: 'Leonardo', avatarUrl: 'https://avatar.example.com/leo.png' })
    ).resolves.toEqual(updated)
    expect(client.patch).toHaveBeenCalledWith(
      '/api/users/me',
      { name: 'Leonardo', avatarUrl: 'https://avatar.example.com/leo.png' },
      expect.any(Object)
    )
  })

  it('updateProfile propaga ApiError quando os dados sao invalidos', async () => {
    vi.spyOn(client, 'patch').mockRejectedValueOnce(buildAxiosError(400))

    await expect(usersApi.updateProfile({ name: '' })).rejects.toMatchObject({ status: 400 })
  })

  it('logout resolve sem chamar a API', async () => {
    const getSpy = vi.spyOn(client, 'get')

    await expect(usersApi.logout()).resolves.toBeUndefined()
    expect(getSpy).not.toHaveBeenCalled()
  })

  it('completeOnboarding marca o onboarding como concluido', async () => {
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await usersApi.completeOnboarding()

    expect(client.post).toHaveBeenCalledWith(
      '/api/users/me/onboarding/complete',
      undefined,
      expect.any(Object)
    )
  })

  it('completeOnboarding propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(500))

    await expect(usersApi.completeOnboarding()).rejects.toMatchObject({ status: 500 })
  })

  it('deleteAccount exclui a conta do usuario', async () => {
    vi.spyOn(client, 'delete').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await usersApi.deleteAccount()

    expect(client.delete).toHaveBeenCalledWith('/api/users/me')
  })

  it('deleteAccount propaga ApiError quando a request falha', async () => {
    vi.spyOn(client, 'delete').mockRejectedValueOnce(buildAxiosError(500))

    await expect(usersApi.deleteAccount()).rejects.toMatchObject({ status: 500 })
  })
})
