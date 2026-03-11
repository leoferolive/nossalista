declare module 'virtual:pwa-register' {
  interface RegisterSWOptions {
    immediate?: boolean
  }

  export function registerSW(options?: RegisterSWOptions): () => Promise<void>
}

declare module 'workbox-precaching' {
  export function cleanupOutdatedCaches(): void
  export function precacheAndRoute(entries: unknown): void
}

interface ServiceWorkerGlobalScope {
  __WB_MANIFEST: unknown
}
