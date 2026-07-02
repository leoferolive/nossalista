import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { tokensApi } from './tokensApi'
import client from './client'
import type { ProblemDetail } from '../types/ProblemDetail'

function buildAxiosError(status: number, detail: string): AxiosError<ProblemDetail> {
  const data: ProblemDetail = {
    type: 'about:blank',
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

describe('tokensApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('list', () => {
    it('retorna a lista de tokens do usuário', async () => {
      const tokens = [
        {
          id: '1',
          name: 'Claude Desktop',
          prefix: 'nlmcp_abc123',
          scope: 'READ' as const,
          expiresAt: null,
          lastUsedAt: null,
          createdAt: '2026-01-01T00:00:00.000Z',
        },
      ]
      vi.spyOn(client, 'get').mockResolvedValueOnce({ data: tokens } as AxiosResponse)

      await expect(tokensApi.list()).resolves.toEqual(tokens)
      expect(client.get).toHaveBeenCalledWith('/api/users/me/tokens')
    })

    it('lança ApiError quando não autenticado', async () => {
      vi.spyOn(client, 'get').mockRejectedValueOnce(buildAxiosError(401, 'Não autenticado'))

      await expect(tokensApi.list()).rejects.toMatchObject({ status: 401 })
    })
  })

  describe('create', () => {
    it('cria um token e retorna o valor em claro', async () => {
      const created = {
        id: '1',
        name: 'Claude Desktop',
        token: 'nlmcp_deadbeef',
        prefix: 'nlmcp_deadbe',
        scope: 'READ' as const,
        expiresAt: null,
        createdAt: '2026-01-01T00:00:00.000Z',
        lastUsedAt: null,
      }
      vi.spyOn(client, 'post').mockResolvedValueOnce({ data: created } as AxiosResponse)

      const result = await tokensApi.create({ name: 'Claude Desktop', scope: 'READ' })

      expect(result).toEqual(created)
      expect(client.post).toHaveBeenCalledWith('/api/users/me/tokens', {
        name: 'Claude Desktop',
        scope: 'READ',
      })
    })

    it('lança ApiError com 409 quando limite de tokens é atingido', async () => {
      vi.spyOn(client, 'post').mockRejectedValueOnce(
        buildAxiosError(409, 'Limite de 10 tokens ativos atingido.')
      )

      await expect(tokensApi.create({ name: 'Token', scope: 'READ_WRITE' })).rejects.toMatchObject({
        status: 409,
      })
    })
  })

  describe('revoke', () => {
    it('revoga o token informado', async () => {
      vi.spyOn(client, 'delete').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

      await tokensApi.revoke('token-id')

      expect(client.delete).toHaveBeenCalledWith('/api/users/me/tokens/token-id')
    })

    it('lança ApiError com 404 quando o token não existe', async () => {
      vi.spyOn(client, 'delete').mockRejectedValueOnce(buildAxiosError(404, 'Token não encontrado'))

      await expect(tokensApi.revoke('unknown-id')).rejects.toMatchObject({ status: 404 })
    })
  })
})
