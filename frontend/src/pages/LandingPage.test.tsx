import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { LandingPage } from './LandingPage'
import { ThemeProvider } from '../contexts/ThemeContext'

const mockNavigate = vi.fn()

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: false, isBootstrapping: false }),
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../components/LoginModal', () => ({
  LoginModal: ({
    onClose,
    initialEmail,
    showRegisteredMessage,
  }: {
    onClose: () => void
    initialEmail?: string
    showRegisteredMessage?: boolean
  }) => (
    <div
      data-testid="login-modal"
      data-email={initialEmail ?? ''}
      data-registered={showRegisteredMessage ? '1' : '0'}
    >
      <button onClick={onClose}>Fechar modal</button>
    </div>
  ),
}))

vi.mock('../components/RegisterModal', () => ({
  RegisterModal: ({ onClose }: { onClose: () => void }) => (
    <div data-testid="register-modal">
      <button onClick={onClose}>Fechar cadastro</button>
    </div>
  ),
}))

function LocationProbe() {
  const location = useLocation()
  return <p data-testid="location-search">{location.search}</p>
}

function renderLanding(initialEntries: string[] = ['/']) {
  return render(
    <ThemeProvider>
      <MemoryRouter initialEntries={initialEntries}>
        <LandingPage />
        <LocationProbe />
      </MemoryRouter>
    </ThemeProvider>
  )
}

describe('LandingPage', () => {
  it('renderiza headline e CTA principal', () => {
    renderLanding()

    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Criar conta gratis/i })).toBeInTheDocument()
  })

  it('abre o modal de cadastro ao clicar em Criar conta gratis', () => {
    renderLanding()

    fireEvent.click(screen.getByRole('button', { name: /Criar conta gratis/i }))

    expect(screen.getByTestId('register-modal')).toBeInTheDocument()
  })

  it('abre o modal de login ao clicar em Ja tenho conta', () => {
    renderLanding()

    fireEvent.click(screen.getByRole('button', { name: /Ja tenho conta/i }))

    expect(screen.getByTestId('login-modal')).toBeInTheDocument()
  })

  it('fecha o modal ao chamar onClose', () => {
    renderLanding()

    fireEvent.click(screen.getByRole('button', { name: /Criar conta gratis/i }))
    expect(screen.getByTestId('register-modal')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Fechar cadastro' }))
    expect(screen.queryByTestId('register-modal')).not.toBeInTheDocument()
    expect(screen.getByTestId('location-search')).toHaveTextContent('')
  })

  it('renderiza elementos de valor na landing', () => {
    renderLanding()

    expect(screen.getByText('Sync instantaneo')).toBeInTheDocument()
    expect(screen.getByText('Convite por link ou username')).toBeInTheDocument()
    expect(screen.getByText('Compras, tarefas e desejos')).toBeInTheDocument()
  })

  it('abre login via query string e repassa estado de cadastro concluido', () => {
    renderLanding(['/?auth=login&registered=1&email=leo%40test.com'])

    const loginModal = screen.getByTestId('login-modal')
    expect(loginModal).toBeInTheDocument()
    expect(loginModal).toHaveAttribute('data-registered', '1')
    expect(loginModal).toHaveAttribute('data-email', 'leo@test.com')
  })
})
