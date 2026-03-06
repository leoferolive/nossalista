import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { AuthProvider, useAuth } from './AuthContext'

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
  },
}))

import client from '../api/client'

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
})
