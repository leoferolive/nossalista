import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { MagicLogin } from './MagicLogin'
import { persistAuthToken, getStoredAuthToken } from '../auth/session'

const navigateMock = vi.fn()
const loginMock = vi.fn()
const magicLoginMock = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ login: loginMock }) }))
vi.mock('../api/authApi', () => ({ authApi: { magicLogin: (t: string) => magicLoginMock(t) } }))
vi.mock('../auth/session', () => ({
  persistAuthToken: vi.fn(),
  getStoredAuthToken: vi.fn(() => null),
}))

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/magic-login" element={<MagicLogin />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('MagicLogin', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    loginMock.mockReset()
    magicLoginMock.mockReset()
    vi.mocked(persistAuthToken).mockReset()
    vi.mocked(getStoredAuthToken).mockReset().mockReturnValue(null)
    localStorage.clear()
    sessionStorage.clear()
  })

  it('consome o token, loga e redireciona para /home', async () => {
    magicLoginMock.mockResolvedValue({
      id: '1',
      username: 'ana',
      email: 'ana@test.com',
      name: 'Ana',
      avatarUrl: null,
      onboardingCompletedAt: null,
      authProvider: 'EMAIL',
      createdAt: '2026-01-01',
      token: 'jwt-123',
      expiresAt: '2026-01-08',
    })

    renderAt('/magic-login?token=abc')

    await waitFor(() => expect(magicLoginMock).toHaveBeenCalledWith('abc'))
    await waitFor(() => expect(loginMock).toHaveBeenCalled())
    expect(navigateMock).toHaveBeenCalledWith('/home', { replace: true })
  })

  it('mostra erro quando o token está ausente', async () => {
    renderAt('/magic-login')
    await waitFor(() => expect(screen.getByText(/link.*inválido|token.*não/i)).toBeInTheDocument())
    expect(magicLoginMock).not.toHaveBeenCalled()
  })

  it('surfaca a mensagem real do backend quando o token é inválido/expirado', async () => {
    magicLoginMock.mockRejectedValue(new Error('Token expirado'))
    renderAt('/magic-login?token=bad')
    await waitFor(() => expect(screen.getByText(/token expirado/i)).toBeInTheDocument())
  })

  it('no reload após sucesso: não reconsome o token e vai para /home', async () => {
    // Guard do sessionStorage já marcado (consumo anterior) + sessão já autenticada.
    sessionStorage.setItem('magic_login:abc', '1')
    vi.mocked(getStoredAuthToken).mockReturnValue('jwt-ja-salvo')

    renderAt('/magic-login?token=abc')

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/home', { replace: true }))
    expect(magicLoginMock).not.toHaveBeenCalled()
    expect(screen.queryByText('Falha no Login')).not.toBeInTheDocument()
  })

  it('token já consumido mas navegador já autenticado: vai para /home sem erro', async () => {
    // O magicLogin falha (token single-use já usado), mas já há sessão no browser.
    magicLoginMock.mockRejectedValue(new Error('Token inválido ou já utilizado'))
    vi.mocked(getStoredAuthToken).mockReturnValue('jwt-ja-salvo')

    renderAt('/magic-login?token=dup')

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/home', { replace: true }))
    expect(screen.queryByText('Falha no Login')).not.toBeInTheDocument()
  })
})
