import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import { AuthCallback } from './AuthCallback'
import { authApi } from '../api/authApi'
import { persistAuthToken, clearStoredSession, getStoredAuthToken } from '../auth/session'

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
  getStoredAuthToken: vi.fn(() => null),
}))

describe('AuthCallback page (Q2.3 one-time code)', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    mockLogin.mockReset()
    vi.mocked(authApi.exchangeOAuthCode).mockReset()
    vi.mocked(persistAuthToken).mockReset()
    vi.mocked(clearStoredSession).mockReset()
    vi.mocked(getStoredAuthToken).mockReset().mockReturnValue(null)
    sessionStorage.clear()
  })

  const tokenResponse = {
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
  } as const

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

  it('NÃO limpa a sessão e vai pra /home quando o exchange falha mas já há token (troca duplicada do mesmo code)', async () => {
    // Cenário real: callbacks OAuth duplicados emitem o mesmo code; a 1ª troca
    // autenticou (token presente), a 2ª troca do mesmo code retorna 400.
    vi.mocked(authApi.exchangeOAuthCode).mockRejectedValueOnce(
      new Error('Código de login inválido ou expirado')
    )
    vi.mocked(getStoredAuthToken).mockReturnValue('jwt-ja-salvo')

    renderCallback('/auth/callback?code=dup-code')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/home', { replace: true }))
    expect(clearStoredSession).not.toHaveBeenCalled()
    expect(screen.queryByText('Falha no Login')).not.toBeInTheDocument()
  })

  it('NÃO troca o mesmo code duas vezes (guard de idempotência por code)', async () => {
    vi.mocked(authApi.exchangeOAuthCode).mockResolvedValue({ ...tokenResponse })

    const { unmount } = renderCallback('/auth/callback?code=same-code')
    await waitFor(() => expect(authApi.exchangeOAuthCode).toHaveBeenCalledTimes(1))
    unmount()

    // 2ª montagem com o MESMO code (ex.: reload do service worker): já há token e
    // o guard impede nova troca — só segue pra /home.
    vi.mocked(getStoredAuthToken).mockReturnValue('jwt-token')
    renderCallback('/auth/callback?code=same-code')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/home', { replace: true }))
    expect(authApi.exchangeOAuthCode).toHaveBeenCalledTimes(1)
  })
})
