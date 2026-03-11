import { describe, it, expect, vi, beforeEach } from 'vitest'
import userEvent from '@testing-library/user-event'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AppHeader } from './AppHeader'

const mockNavigate = vi.fn()
const mockLogout = vi.fn()
const mockStartReplay = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../contexts/ThemeContext', () => ({
  useTheme: () => ({ theme: 'light', toggleTheme: vi.fn(), setTheme: vi.fn() }),
}))

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'user-1',
      username: 'leo',
      email: 'leo@test.com',
      displayName: 'Leo Oliveira',
      avatarUrl: null,
    },
    logout: mockLogout,
  }),
}))

vi.mock('../contexts/OnboardingContext', () => ({
  useOnboarding: () => ({
    startReplay: mockStartReplay,
  }),
}))

describe('AppHeader', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    mockLogout.mockReset()
    mockStartReplay.mockReset()
  })

  it('abre o menu da conta e faz logout', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" subtitle="Organize seu dia." />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))

    expect(screen.getByRole('menu')).toBeInTheDocument()
    expect(screen.getByRole('menuitem', { name: 'Meu perfil' })).toBeInTheDocument()

    await user.click(screen.getByRole('menuitem', { name: 'Sair' }))

    expect(mockLogout).toHaveBeenCalledTimes(1)
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
  })

  it('abre o menu e dispara replay do tutorial', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" subtitle="Organize seu dia." />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    await user.click(screen.getByRole('menuitem', { name: 'Ver tutorial' }))

    expect(mockStartReplay).toHaveBeenCalledTimes(1)
  })
})
