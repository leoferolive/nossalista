import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosResponse } from 'axios'
import { AxiosError, AxiosHeaders } from 'axios'
import { oauthConsentApi } from './oauthConsentApi'
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

describe('oauthConsentApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('get retorna os dados do pedido de autorizacao pendente', async () => {
    const pending = {
      requestId: 'req-1',
      clientId: 'claude-ai',
      clientName: 'Claude.ai',
      scope: 'READ' as const,
      redirectUriHost: 'claude.ai',
    }
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: pending } as AxiosResponse)

    await expect(oauthConsentApi.get('req-1')).resolves.toEqual(pending)
    expect(client.get).toHaveBeenCalledWith('/api/oauth/consent/req-1')
  })

  it('get propaga ApiError quando o pedido nao existe', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(404))

    await expect(oauthConsentApi.get('req-1')).rejects.toMatchObject({ status: 404 })
  })

  it('approve aprova o consentimento e retorna a URL de retorno', async () => {
    const decision = { redirectUrl: 'https://claude.ai/callback?code=abc' }
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: decision } as AxiosResponse)

    await expect(oauthConsentApi.approve('req-1')).resolves.toEqual(decision)
    expect(client.post).toHaveBeenCalledWith('/api/oauth/consent/req-1/approve')
  })

  it('approve propaga ApiError quando o pedido expirou', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(410))

    await expect(oauthConsentApi.approve('req-1')).rejects.toMatchObject({ status: 410 })
  })

  it('deny nega o consentimento e retorna a URL de retorno com erro', async () => {
    const decision = { redirectUrl: 'https://claude.ai/callback?error=access_denied' }
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: decision } as AxiosResponse)

    await expect(oauthConsentApi.deny('req-1')).resolves.toEqual(decision)
    expect(client.post).toHaveBeenCalledWith('/api/oauth/consent/req-1/deny')
  })

  it('deny propaga ApiError quando o pedido nao existe', async () => {
    vi.spyOn(client, 'post').mockRejectedValueOnce(buildAxiosError(404))

    await expect(oauthConsentApi.deny('req-1')).rejects.toMatchObject({ status: 404 })
  })
})
