import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LoginModal } from './LoginModal'

const mockLogin = vi.fn()
const mockNavigate = vi.fn()

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ login: mockLogin }),
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

vi.mock('../api/client', () => ({
  default: { post: vi.fn() },
}))

function renderModal(onClose = vi.fn()) {
  return render(
    <MemoryRouter>
      <LoginModal onClose={onClose} />
    </MemoryRouter>
  )
}

describe('LoginModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza campos de email e senha e botão de entrar', () => {
    renderModal()

    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Senha')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('renderiza botão de continuar com Google', () => {
    renderModal()

    expect(screen.getByRole('button', { name: /Continuar com Google/i })).toBeInTheDocument()
  })

  it('renderiza link de criar conta', () => {
    renderModal()

    expect(screen.getByRole('link', { name: 'Criar conta' })).toBeInTheDocument()
  })

  it('chama onClose ao clicar no botão fechar', () => {
    const onClose = vi.fn()
    renderModal(onClose)

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }))

    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('chama onClose ao pressionar Escape', () => {
    const onClose = vi.fn()
    renderModal(onClose)

    fireEvent.keyDown(document, { key: 'Escape' })

    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('exibe erro ao falhar no login', async () => {
    const client = await import('../api/client')
    vi.mocked(client.default.post).mockRejectedValueOnce(new Error('unauthorized'))

    renderModal()

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@test.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'wrongpass' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Email ou senha inválidos')).toBeInTheDocument()
  })
})
