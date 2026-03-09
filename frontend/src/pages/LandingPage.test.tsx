import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LandingPage } from './LandingPage'

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

function renderLanding() {
  return render(
    <MemoryRouter>
      <LandingPage />
    </MemoryRouter>
  )
}

describe('LandingPage', () => {
  it('renderiza headline e CTA principal', () => {
    renderLanding()

    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Começar agora/i })).toBeInTheDocument()
  })

  it('abre o modal de login ao clicar em Começar agora', () => {
    renderLanding()

    fireEvent.click(screen.getByRole('button', { name: /Começar agora/i }))

    expect(screen.getByTestId('login-modal')).toBeInTheDocument()
  })

  it('abre o modal de login ao clicar em Já tenho conta', () => {
    renderLanding()

    fireEvent.click(screen.getByRole('button', { name: /Já tenho conta/i }))

    expect(screen.getByTestId('login-modal')).toBeInTheDocument()
  })

  it('fecha o modal ao chamar onClose', () => {
    renderLanding()

    fireEvent.click(screen.getByRole('button', { name: /Começar agora/i }))
    expect(screen.getByTestId('login-modal')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Fechar modal' }))
    expect(screen.queryByTestId('login-modal')).not.toBeInTheDocument()
  })

  it('renderiza trust signals', () => {
    renderLanding()

    expect(screen.getByText('Sync instantâneo')).toBeInTheDocument()
    expect(screen.getByText('Compartilhe por link')).toBeInTheDocument()
    expect(screen.getByText('Compras, tarefas e mais')).toBeInTheDocument()
  })
})
