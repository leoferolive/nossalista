import { describe, it, expect, vi, beforeEach } from 'vitest'
import { act, render, renderHook, screen, waitFor } from '@testing-library/react'
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

const sessionUser = {
  id: 'u1',
  username: 'leo',
  email: 'leo@test.com',
  displayName: 'Leo',
  avatarUrl: null,
  onboardingCompletedAt: null,
}

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

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(usersApi.logout).mockResolvedValue(undefined)
    localStorage.clear()
  })

  it('restaura a sessão pelo cookie consultando /api/users/me e remove dados legados', async () => {
    localStorage.setItem('authToken', 'jwt-legado')
    localStorage.setItem('user', JSON.stringify({ username: 'stale' }))
    vi.mocked(client.get).mockResolvedValue({
      data: {
        id: 'u1',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
      },
    } as never)

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => expect(screen.getByTestId('authenticated')).toHaveTextContent('true'))
    expect(screen.getByTestId('username')).toHaveTextContent('leo')
    expect(client.get).toHaveBeenCalledWith('/api/users/me')
    expect(localStorage.getItem('authToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('permanece desautenticado quando não há uma sessão cookie válida', async () => {
    vi.mocked(client.get).mockRejectedValue(new Error('401'))

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => expect(screen.getByTestId('bootstrapping')).toHaveTextContent('false'))
    expect(screen.getByTestId('authenticated')).toHaveTextContent('false')
  })

  it('login mantém apenas o perfil na memória e atualiza onboarding', async () => {
    vi.mocked(client.get).mockRejectedValue(new Error('401'))
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))
    act(() => result.current.login(sessionUser))
    act(() => result.current.markOnboardingCompleted('2026-02-02T10:00:00.000Z'))

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.onboardingCompletedAt).toBe('2026-02-02T10:00:00.000Z')
    expect(localStorage.getItem('authToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('logout chama o endpoint que expira o cookie e limpa o estado local', async () => {
    vi.mocked(client.get).mockRejectedValue(new Error('401'))
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })

    await waitFor(() => expect(result.current.isBootstrapping).toBe(false))
    act(() => result.current.login(sessionUser))
    act(() => result.current.logout())

    expect(usersApi.logout).toHaveBeenCalledOnce()
    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })
})

describe('useAuth', () => {
  it('falha fora do provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => renderHook(() => useAuth())).toThrow('useAuth must be used within an AuthProvider')
    spy.mockRestore()
  })
})
