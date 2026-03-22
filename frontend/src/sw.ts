/// <reference lib="webworker" />
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching'

declare const self: ServiceWorkerGlobalScope

cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

self.addEventListener('push', (event) => {
  let data: { title?: string; body?: string; icon?: string; tag?: string; url?: string } = {}
  try {
    data = event.data?.json() ?? {}
  } catch {
    data = { title: 'NossaLista', body: 'Nova notificação' }
  }
  event.waitUntil(
    self.registration.showNotification(data.title ?? 'NossaLista', {
      body: data.body,
      icon: data.icon || '/favicon.ico',
      tag: data.tag,
      data: { url: data.url || '/' },
    })
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil(self.clients.openWindow(event.notification.data?.url || '/'))
})
