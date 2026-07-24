import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { AuthCallback } from './AuthCallback'
import { authApi } from '../api/authApi'

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

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
  },
  preserveSessionOnUnauthorizedConfig: { preserveSessionOnUnauthorized: true },
}))

vi.mock('../api/listsApi', () => ({
  listsApi: {
    joinList: vi.fn(),
  },
}))

describe('AuthCallback page (Q2.3 one-time code)', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    mockLogin.mockReset()
    vi.mocked(authApi.exchangeOAuthCode).mockReset()
    sessionStorage.clear()
  })

  const profileResponse = {
    id: 'u1',
    username: 'leo',
    email: 'leo@gmail.com',
    name: 'Leo',
    avatarUrl: null,
    onboardingCompletedAt: null,
    authProvider: 'GOOGLE',
    createdAt: '2026-01-01T00:00:00Z',
  } as const

  const renderCallback = (initialEntry: string) =>
    render(
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/auth/callback" element={<AuthCallback />} />
        </Routes>
      </MemoryRouter>
    )

  it('troca o code por sessão HttpOnly e redireciona para /home', async () => {
    vi.mocked(authApi.exchangeOAuthCode).mockResolvedValueOnce(profileResponse)

    renderCallback('/auth/callback?code=the-code')

    await waitFor(() => expect(authApi.exchangeOAuthCode).toHaveBeenCalledWith('the-code'))
    expect(mockLogin).toHaveBeenCalledWith(expect.objectContaining({ id: 'u1', username: 'leo' }))
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/home', { replace: true }))
  })

  it('mostra erro quando não há code na URL e não chama o exchange', async () => {
    renderCallback('/auth/callback')

    await waitFor(() => expect(screen.getByText('Falha no Login')).toBeInTheDocument())
    expect(authApi.exchangeOAuthCode).not.toHaveBeenCalled()
  })

  it('mostra erro quando o code é inválido ou expirado', async () => {
    vi.mocked(authApi.exchangeOAuthCode).mockRejectedValueOnce(new Error('Link de login expirado'))

    renderCallback('/auth/callback?code=bad')

    await waitFor(() => expect(screen.getByText('Falha no Login')).toBeInTheDocument())
    expect(screen.getByText('Link de login expirado')).toBeInTheDocument()
  })

  it('volta para a página inicial a partir do estado de erro', async () => {
    vi.mocked(authApi.exchangeOAuthCode).mockRejectedValueOnce(new Error('Link de login expirado'))
    renderCallback('/auth/callback?code=bad')

    await waitFor(() => expect(screen.getByText('Falha no Login')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /voltar para inicio/i }))

    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
  })

  it('não troca o mesmo code duas vezes: restaura a sessão pelo cookie', async () => {
    const { default: client } = await import('../api/client')
    vi.mocked(authApi.exchangeOAuthCode).mockResolvedValue(profileResponse)
    vi.mocked(client.get).mockResolvedValue({ data: profileResponse } as never)

    const { unmount } = renderCallback('/auth/callback?code=same-code')
    await waitFor(() => expect(authApi.exchangeOAuthCode).toHaveBeenCalledTimes(1))
    unmount()

    renderCallback('/auth/callback?code=same-code')

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/home', { replace: true }))
    expect(authApi.exchangeOAuthCode).toHaveBeenCalledTimes(1)
    expect(client.get).toHaveBeenCalledWith('/api/users/me', {
      preserveSessionOnUnauthorized: true,
    })
  })
})
