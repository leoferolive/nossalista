import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosResponse } from 'axios'
import { AxiosError, AxiosHeaders } from 'axios'
import { oauthConnectionsApi } from './oauthConnectionsApi'
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

describe('oauthConnectionsApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('list retorna as conexoes OAuth do usuario', async () => {
    const connections = [
      {
        clientId: 'claude-ai',
        clientName: 'Claude.ai',
        scope: 'READ' as const,
        createdAt: '2026-01-01T00:00:00Z',
        lastUsedAt: null,
      },
    ]
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: connections } as AxiosResponse)

    await expect(oauthConnectionsApi.list()).resolves.toEqual(connections)
    expect(client.get).toHaveBeenCalledWith('/api/oauth/connections')
  })

  it('list propaga ApiError quando nao autenticado', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(401))

    await expect(oauthConnectionsApi.list()).rejects.toMatchObject({ status: 401 })
  })

  it('revoke desconecta um assistente', async () => {
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await oauthConnectionsApi.revoke('claude-ai')

    expect(client.post).toHaveBeenCalledWith('/api/oauth/connections/claude-ai/revoke')
  })

  it('revoke propaga ApiError quando a conexao nao existe', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(404))

    await expect(oauthConnectionsApi.revoke('desconhecido')).rejects.toMatchObject({ status: 404 })
  })
})
