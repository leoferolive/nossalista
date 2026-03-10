import client from './client'

export const pushApi = {
  getVapidPublicKey: (): Promise<string | null> =>
    client
      .get<string>('/api/push/vapid-public-key')
      .then((r) => r.data)
      .catch(() => null),

  subscribe: (subscription: PushSubscriptionJSON): Promise<void> =>
    client.post('/api/push/subscribe', subscription).then(() => undefined),

  unsubscribe: (endpoint: string): Promise<void> =>
    client
      .delete('/api/push/unsubscribe', { data: { endpoint } })
      .then(() => undefined),
}
