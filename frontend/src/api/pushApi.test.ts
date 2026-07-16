import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AxiosResponse } from 'axios'
import { pushApi } from './pushApi'
import client from './client'

describe('pushApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('getVapidPublicKey retorna a chave publica VAPID', async () => {
    vi.spyOn(client, 'get').mockResolvedValueOnce({ data: 'chave-publica' } as AxiosResponse)

    await expect(pushApi.getVapidPublicKey()).resolves.toBe('chave-publica')
    expect(client.get).toHaveBeenCalledWith('/api/push/vapid-public-key')
  })

  it('getVapidPublicKey retorna null quando a request falha', async () => {
    vi.spyOn(client, 'get').mockRejectedValueOnce(new Error('offline'))

    await expect(pushApi.getVapidPublicKey()).resolves.toBeNull()
  })

  it('subscribe registra a subscription de push', async () => {
    const subscription = { endpoint: 'https://push.example.com/1' } as PushSubscriptionJSON
    vi.spyOn(client, 'post').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await expect(pushApi.subscribe(subscription)).resolves.toBeUndefined()
    expect(client.post).toHaveBeenCalledWith('/api/push/subscribe', subscription)
  })

  it('unsubscribe remove a subscription de push pelo endpoint', async () => {
    vi.spyOn(client, 'delete').mockResolvedValueOnce({ data: undefined } as AxiosResponse)

    await expect(pushApi.unsubscribe('https://push.example.com/1')).resolves.toBeUndefined()
    expect(client.delete).toHaveBeenCalledWith('/api/push/unsubscribe', {
      data: { endpoint: 'https://push.example.com/1' },
    })
  })
})
