import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { MagicLogin } from './MagicLogin'

const navigateMock = vi.fn()
const loginMock = vi.fn()
const magicLoginMock = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ login: loginMock }) }))
vi.mock('../api/authApi', () => ({ authApi: { magicLogin: (t: string) => magicLoginMock(t) } }))

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
    localStorage.clear()
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

  it('mostra erro quando o token é inválido/expirado', async () => {
    magicLoginMock.mockRejectedValue(new Error('Token inválido'))
    renderAt('/magic-login?token=bad')
    await waitFor(() => expect(screen.getByText(/não foi possível/i)).toBeInTheDocument())
  })
})
