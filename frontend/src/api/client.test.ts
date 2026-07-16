import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../auth/session', () => ({
  clearStoredSession: vi.fn(),
  getStoredAuthToken: vi.fn(() => 'token-de-teste'),
}))

type RequestHandlers = {
  fulfilled?: (config: { headers?: Record<string, unknown> }) => unknown
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

  it('adiciona o header Authorization quando ha token armazenado', async () => {
    const { requestHandlers } = await loadClientHandlers()

    const config = { headers: {} as Record<string, unknown> }
    const result = requestHandlers?.fulfilled?.(config) as { headers: Record<string, unknown> }

    expect(result.headers.Authorization).toBe('Bearer token-de-teste')
  })

  it('propaga o erro do interceptor de request', async () => {
    const { requestHandlers } = await loadClientHandlers()

    const error = new Error('falha de configuracao')
    await expect(requestHandlers?.rejected?.(error)).rejects.toBe(error)
  })
})

describe('client request interceptor sem token armazenado', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.doMock('../auth/session', () => ({
      clearStoredSession: vi.fn(),
      getStoredAuthToken: vi.fn(() => null),
    }))
  })

  it('nao adiciona o header Authorization quando nao ha token', async () => {
    const { requestHandlers } = await loadClientHandlers()

    const config = { headers: {} as Record<string, unknown> }
    const result = requestHandlers?.fulfilled?.(config) as { headers: Record<string, unknown> }

    expect(result.headers.Authorization).toBeUndefined()
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
    const { clearStoredSession } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    const error = {
      response: { status: 401 },
      config: { preserveSessionOnUnauthorized: true },
    }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearStoredSession).not.toHaveBeenCalled()
  })

  it('limpa a sessao e redireciona para / em 401 nao tratado localmente', async () => {
    const { clearStoredSession } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    Object.defineProperty(window, 'location', {
      value: { ...originalLocation, pathname: '/lists/abc', href: '' },
      writable: true,
    })

    const error = { response: { status: 401 }, config: {} }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearStoredSession).toHaveBeenCalled()
    expect(window.location.href).toBe('/')
  })

  it('nao redireciona novamente quando ja esta na landing', async () => {
    const { clearStoredSession } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    Object.defineProperty(window, 'location', {
      value: { ...originalLocation, pathname: '/', href: 'unchanged' },
      writable: true,
    })

    const error = { response: { status: 401 }, config: {} }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearStoredSession).toHaveBeenCalled()
    expect(window.location.href).toBe('unchanged')
  })

  it('propaga erros que nao sao 401 sem limpar a sessao', async () => {
    const { clearStoredSession } = await import('../auth/session')
    const { responseHandlers } = await loadClientHandlers()

    const error = { response: { status: 500 }, config: {} }

    await expect(responseHandlers?.rejected?.(error)).rejects.toBe(error)
    expect(clearStoredSession).not.toHaveBeenCalled()
  })
})
