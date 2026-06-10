import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import { AuthCallback } from './AuthCallback'
import { authApi } from '../api/authApi'
import { persistAuthToken } from '../auth/session'

const mockNavigate = vi.fn()
const mockLogin = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ login: mockLogin }),
}))

vi.mock('../api/authApi', () => ({
  authApi: {
    exchangeOAuthCode: vi.fn(),
  },
}))

vi.mock('../api/listsApi', () => ({
  listsApi: {
    joinList: vi.fn(),
  },
}))

vi.mock('../auth/session', () => ({
  persistAuthToken: vi.fn(),
  clearStoredSession: vi.fn(),
}))

describe('AuthCallback page (Q2.3 one-time code)', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    mockLogin.mockReset()
    vi.mocked(authApi.exchangeOAuthCode).mockReset()
    vi.mocked(persistAuthToken).mockReset()
    sessionStorage.clear()
  })

  const renderCallback = (initialEntry: string) =>
    render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/auth/callback" element={<AuthCallback />} />
        </Routes>
      </MemoryRouter>
    )

  it('troca o code pelo JWT, persiste o token e redireciona para /home', async () => {
    vi.mocked(authApi.exchangeOAuthCode).mockResolvedValueOnce({
      id: 'u1',
      username: 'leo',
      email: 'leo@gmail.com',
      name: 'Leo',
      avatarUrl: null,
      onboardingCompletedAt: null,
      authProvider: 'GOOGLE',
      createdAt: '2026-01-01T00:00:00Z',
      token: 'jwt-token',
      expiresAt: '2026-01-08T00:00:00Z',
    })

    renderCallback('/auth/callback?code=the-code')

    await waitFor(() => expect(authApi.exchangeOAuthCode).toHaveBeenCalledWith('the-code'))
    expect(persistAuthToken).toHaveBeenCalledWith('jwt-token')
    expect(mockLogin).toHaveBeenCalledWith('jwt-token', expect.objectContaining({ id: 'u1' }))
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/home', { replace: true }))
  })

  it('mostra erro quando não há code na URL e não chama o exchange', async () => {
    renderCallback('/auth/callback')

    await waitFor(() => expect(screen.getByText('Falha no Login')).toBeInTheDocument())
    expect(authApi.exchangeOAuthCode).not.toHaveBeenCalled()
  })

  it('mostra erro quando o code é inválido/expirado', async () => {
    vi.mocked(authApi.exchangeOAuthCode).mockRejectedValueOnce(new Error('Link de login expirado'))

    renderCallback('/auth/callback?code=bad')

    await waitFor(() => expect(screen.getByText('Falha no Login')).toBeInTheDocument())
    expect(screen.getByText('Link de login expirado')).toBeInTheDocument()
  })
})
