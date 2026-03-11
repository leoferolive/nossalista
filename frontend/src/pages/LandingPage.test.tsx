import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
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
  LoginModal: ({ onClose }: { onClose: () => void }) => (
    <div data-testid="login-modal">
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

function renderLanding() {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <LandingPage />
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
  })

  it('renderiza elementos de valor na landing', () => {
    renderLanding()

    expect(screen.getByText('Sync instantaneo')).toBeInTheDocument()
    expect(screen.getByText('Compartilhe por username ou link')).toBeInTheDocument()
    expect(screen.getByText('Compras, tarefas e wishlist')).toBeInTheDocument()
  })
})
