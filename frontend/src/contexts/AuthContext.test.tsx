import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act, renderHook } from '@testing-library/react'
import { AuthProvider, useAuth } from './AuthContext'

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
  },
}))

vi.mock('../api/usersApi', () => ({
  usersApi: {
    logout: vi.fn(() => Promise.resolve()),
  },
}))

import client from '../api/client'
import { usersApi } from '../api/usersApi'

function AuthProbe() {
  const { isAuthenticated, isBootstrapping, user } = useAuth()

  return (
    <div>
      <span data-testid="bootstrapping">{String(isBootstrapping)}</span>
      <span data-testid="authenticated">{String(isAuthenticated)}</span>
      <span data-testid="username">{user?.username ?? 'none'}</span>
    </div>
  )
}

describe('AuthContext bootstrap', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(usersApi.logout).mockResolvedValue(undefined)
    localStorage.clear()
  })

  it('restaura a sessao quando existe token valido', async () => {
    localStorage.setItem('authToken', 'token-valido')
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: 'stale-user',
        username: 'stale',
        email: 'stale@test.com',
        displayName: 'Stale User',
      })
    )

    vi.mocked(client.get).mockResolvedValue({
      data: {
        id: 'user-1',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo',
        avatarUrl: null,
      },
    } as never)

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('bootstrapping')).toHaveTextContent('false')
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true')
      expect(screen.getByTestId('username')).toHaveTextContent('leo')
    })

    expect(localStorage.getItem('authToken')).toBe('token-valido')
    expect(JSON.parse(localStorage.getItem('user') || '{}')).toMatchObject({
      username: 'leo',
      displayName: 'Leo',
    })
  })

  it('usa avatarUrl e onboardingCompletedAt quando presentes na resposta', async () => {
    localStorage.setItem('authToken', 'token-valido')

    vi.mocked(client.get).mockResolvedValue({
      data: {
        id: 'user-2',
        username: 'mia',
        email: 'mia@test.com',
        name: 'Mia',
        avatarUrl: 'https://cdn/avatar.png',
        onboardingCompletedAt: '2026-01-01T00:00:00.000Z',
      },
    } as never)

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('authenticated')).toHaveTextContent('true')
      expect(screen.getByTestId('username')).toHaveTextContent('mia')
    })

    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    expect(stored).toMatchObject({
      avatarUrl: 'https://cdn/avatar.png',
      onboardingCompletedAt: '2026-01-01T00:00:00.000Z',
      displayName: 'Mia',
    })
  })

  it('limpa a sessao quando o bootstrap falha com token invalido', async () => {
    localStorage.setItem('authToken', 'token-invalido')
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: 'user-1',
        username: 'leo',
        email: 'leo@test.com',
        displayName: 'Leo',
      })
    )

    vi.mocked(client.get).mockRejectedValue(new Error('401'))

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('bootstrapping')).toHaveTextContent('false')
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
      expect(screen.getByTestId('username')).toHaveTextContent('none')
    })

    expect(localStorage.getItem('authToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('sem token mas com usuario salvo: limpa a sessao salva', async () => {
    // Sem authToken, mas com user no storage -> branch if(!storedToken)+if(savedUser)
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: 'user-stale',
        username: 'stale',
        email: 'stale@test.com',
        displayName: 'Stale',
      })
    )

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('bootstrapping')).toHaveTextContent('false')
    })

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
    expect(screen.getByTestId('username')).toHaveTextContent('none')
    // sessao salva foi limpa
    expect(localStorage.getItem('user')).toBeNull()
    // client.get nunca foi chamado (sem token)
    expect(client.get).not.toHaveBeenCalled()
  })

  it('sem token e sem usuario salvo: apenas finaliza bootstrap', async () => {
    // Sem authToken e sem user -> branch if(!storedToken)+if(!savedUser)
    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('bootstrapping')).toHaveTextContent('false')
    })

    expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
    expect(screen.getByTestId('username')).toHaveTextContent('none')
    expect(client.get).not.toHaveBeenCalled()
  })
})

function useAuthHarness() {
  return useAuth()
}

describe('AuthContext login/logout/markOnboardingCompleted', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(usersApi.logout).mockResolvedValue(undefined)
    localStorage.clear()
  })

  it('login persiste a sessao e marca autenticado', async () => {
    const { result } = renderHook(() => useAuthHarness(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))

    act(() => {
      result.current.login('jwt-token', {
        id: 'u1',
        username: 'leo',
        email: 'leo@test.com',
        displayName: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
      })
    })

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.username).toBe('leo')
    expect(localStorage.getItem('authToken')).toBe('jwt-token')
    expect(result.current.isBootstrapping).toBe(false)
  })

  it('markOnboardingCompleted com argumento explicito e token presente persiste', async () => {
    localStorage.setItem('authToken', 'jwt-token')
    const { result } = renderHook(() => useAuthHarness(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))

    act(() => {
      result.current.login('jwt-token', {
        id: 'u1',
        username: 'leo',
        email: 'leo@test.com',
        displayName: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
      })
    })

    act(() => {
      // completedAt explicito -> ramo esquerdo do ?? ; token presente -> ramo true do if(token)
      result.current.markOnboardingCompleted('2026-02-02T10:00:00.000Z')
    })

    expect(result.current.user?.onboardingCompletedAt).toBe('2026-02-02T10:00:00.000Z')
    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    expect(stored.onboardingCompletedAt).toBe('2026-02-02T10:00:00.000Z')
  })

  it('markOnboardingCompleted sem argumento usa data atual; sem token nao persiste', async () => {
    const { result } = renderHook(() => useAuthHarness(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))

    act(() => {
      result.current.login('jwt-token', {
        id: 'u1',
        username: 'leo',
        email: 'leo@test.com',
        displayName: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
      })
    })

    // Remover token para exercitar o ramo false de if(token) dentro do setUser
    localStorage.removeItem('authToken')
    const userBefore = localStorage.getItem('user')

    act(() => {
      // sem argumento -> ramo direito do ?? (new Date().toISOString())
      result.current.markOnboardingCompleted()
    })

    // onboardingCompletedAt foi definido no estado em memoria
    expect(result.current.user?.onboardingCompletedAt).toEqual(expect.any(String))
    expect(result.current.user?.onboardingCompletedAt).not.toBeNull()
    // mas o storage do user nao foi reescrito (token ausente)
    expect(localStorage.getItem('user')).toBe(userBefore)
  })

  it('markOnboardingCompleted sem usuario (prev null) retorna sem alterar', async () => {
    // Bootstrap sem token => user permanece null
    const { result } = renderHook(() => useAuthHarness(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))
    expect(result.current.user).toBeNull()

    act(() => {
      // prev === null -> retorna prev sem persistir
      result.current.markOnboardingCompleted('2026-03-03T00:00:00.000Z')
    })

    expect(result.current.user).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('logout limpa a sessao e chama usersApi.logout (resolvido)', async () => {
    const { result } = renderHook(() => useAuthHarness(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))

    act(() => {
      result.current.login('jwt-token', {
        id: 'u1',
        username: 'leo',
        email: 'leo@test.com',
        displayName: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
      })
    })

    expect(result.current.isAuthenticated).toBe(true)

    act(() => {
      result.current.logout()
    })

    expect(usersApi.logout).toHaveBeenCalledTimes(1)
    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
    expect(localStorage.getItem('authToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('logout engole erro de usersApi.logout (ramo .catch)', async () => {
    vi.mocked(usersApi.logout).mockRejectedValue(new Error('network'))

    const { result } = renderHook(() => useAuthHarness(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))

    act(() => {
      result.current.login('jwt-token', {
        id: 'u1',
        username: 'leo',
        email: 'leo@test.com',
        displayName: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
      })
    })

    // Nao deve lancar mesmo com logout rejeitando
    act(() => {
      result.current.logout()
    })

    // Aguarda a microtask do .catch ser processada
    await act(async () => {
      await Promise.resolve()
    })

    expect(usersApi.logout).toHaveBeenCalledTimes(1)
    expect(result.current.isAuthenticated).toBe(false)
    expect(localStorage.getItem('authToken')).toBeNull()
  })
})

describe('useAuth fora do provider', () => {
  it('lanca erro quando usado fora de AuthProvider', () => {
    // Silencia o console.error do React para o erro esperado
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => renderHook(() => useAuth())).toThrow('useAuth must be used within an AuthProvider')
    spy.mockRestore()
  })
})
