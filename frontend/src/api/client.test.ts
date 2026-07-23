import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../auth/session', () => ({
  clearLegacyAuthStorage: vi.fn(),
}))

type RequestHandlers = {
  fulfilled?: (config: {
    method?: string
    url?: string
    headers?: Record<string, unknown>
  }) => unknown
  rejected?: (error: unknown) => Promise<unknown>
}

type ResponseHandlers = {
  fulfilled?: (response: unknown) => unknown
  rejected?: (error: unknown) => Promise<unknown>
}

async function loadClientHandlers() {
  const { default: client } = await import('./client')

  const requestHandlers = (
    client.interceptors.request as typeof client.interceptors.request & {
      handlers?: RequestHandlers[]
    }
  ).handlers?.[0]

  const responseHandlers = (
    client.interceptors.response as typeof client.interceptors.response & {
      handlers?: ResponseHandlers[]
    }
  ).handlers?.[0]

  return { client, requestHandlers, responseHandlers }
}

describe('client request interceptor', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('nao envia Authorization para a sessao baseada em cookie', async () => {
    const { requestHandlers } = await loadClientHandlers()

    const config = { method: 'get', headers: {} as Record<string, unknown> }
    const result = (await requestHandlers?.fulfilled?.(config)) as { headers: Record<string, unknown> }

    expect(result.headers.Authorization).toBeUndefined()
  })

  it('propaga o erro do interceptor de request', async () => {
    const { requestHandlers } = await loadClientHandlers()

    const error = new Error('falha de configuracao')
    await expect(requestHandlers?.rejected?.(error)).rejects.toBe(error)
  })

  it('obtém o CSRF antes da primeira mutação quando o cookie ainda não existe', async () => {
    const { client, requestHandlers } = await loadClientHandlers()
    const csrfRequest = vi.spyOn(client, 'get').mockResolvedValueOnce({ data: null })

    await requestHandlers?.fulfilled?.({ method: 'post', url: '/api/lists', headers: {} })

    expect(csrfRequest).toHaveBeenCalledWith('/api/auth/csrf', {
      preserveSessionOnUnauthorized: true,
    })
  })

  it('reutiliza o cookie CSRF já emitido sem buscar outro token', async () => {
    document.cookie = 'XSRF-TOKEN=existing-token'
    const { client, requestHandlers } = await loadClientHandlers()
    const csrfRequest = vi.spyOn(client, 'get')

    await requestHandlers?.fulfilled?.({ method: 'patch', url: '/api/lists/1', headers: {} })

    expect(csrfRequest).not.toHaveBeenCalled()
    document.cookie = 'XSRF-TOKEN=; Max-Age=0'
  })
})

describe('client response interceptor', () => {
  const originalLocation = window.location

  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  afterEach(() => {
    Object.defineProperty(window, 'location', { value: originalLocation, writable: true })
  })

  it('repassa a resposta em caso de sucesso', async () => {
    const { responseHandlers } = await loadClientHandlers()

    const response = { status: 200, data: { ok: true } }
    expect(responseHandlers?.fulfilled?.(response)).toBe(response)
  })

  it('preserves the session for requests that handle 401 locally', async () => {
    const { clearLegacyAuthStorage } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    const error = {
      response: { status: 401 },
      config: { preserveSessionOnUnauthorized: true },
    }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearLegacyAuthStorage).not.toHaveBeenCalled()
  })

  it('limpa a sessao e redireciona para / em 401 nao tratado localmente', async () => {
    const { clearLegacyAuthStorage } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    Object.defineProperty(window, 'location', {
      value: { ...originalLocation, pathname: '/lists/abc', href: '' },
      writable: true,
    })

    const error = { response: { status: 401 }, config: {} }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearLegacyAuthStorage).toHaveBeenCalled()
    expect(window.location.href).toBe('/')
  })

  it('nao redireciona novamente quando ja esta na landing', async () => {
    const { clearLegacyAuthStorage } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    Object.defineProperty(window, 'location', {
      value: { ...originalLocation, pathname: '/', href: 'unchanged' },
      writable: true,
    })

    const error = { response: { status: 401 }, config: {} }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearLegacyAuthStorage).toHaveBeenCalled()
    expect(window.location.href).toBe('unchanged')
  })

  it('propaga erros que nao sao 401 sem limpar a sessao', async () => {
    const { clearLegacyAuthStorage } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    const error = { response: { status: 500 }, config: {} }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearLegacyAuthStorage).not.toHaveBeenCalled()
  })
})
