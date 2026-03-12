import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
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

vi.mock('../api/listsApi', () => ({
  listsApi: { joinList: vi.fn() },
}))

function renderModal(onClose = vi.fn(), initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <LoginModal onClose={onClose} />
    </MemoryRouter>
  )
}

describe('LoginModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
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

  it('usa callback de troca para cadastro quando onSwitchToRegister for informado', () => {
    const onSwitchToRegister = vi.fn()

    render(
      <MemoryRouter>
        <LoginModal onClose={vi.fn()} onSwitchToRegister={onSwitchToRegister} />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByRole('button', { name: 'Abrir cadastro' }))

    expect(onSwitchToRegister).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('link', { name: 'Criar conta' })).not.toBeInTheDocument()
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

  it('prefill de email e mensagem de cadastro concluido', () => {
    render(
      <MemoryRouter>
        <LoginModal onClose={vi.fn()} initialEmail="leo@test.com" showRegisteredMessage />
      </MemoryRouter>
    )

    expect(screen.getByDisplayValue('leo@test.com')).toBeInTheDocument()
    expect(screen.getByText(/Conta criada com sucesso/i)).toBeInTheDocument()
  })

  it('entra na lista automaticamente quando existe invite pendente', async () => {
    const onClose = vi.fn()
    const client = await import('../api/client')
    const { listsApi } = await import('../api/listsApi')
    vi.mocked(client.default.post).mockResolvedValueOnce({
      data: {
        id: 'user-1',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
        token: 'token',
      },
    })
    vi.mocked(listsApi.joinList).mockResolvedValueOnce({
      id: 'list-joined',
      name: 'Mercado da Semana',
      type_slug: 'compras',
      type_name: 'Compras',
      role: 'MEMBER',
      message: 'Entrou!',
      created: true,
      list: {
        id: 'list-joined',
        name: 'Mercado da Semana',
        type: { id: 1, name: 'Compras', slug: 'compras' },
        owner: { id: 'user-2', username: 'ana', name: 'Ana', avatarUrl: null },
        inviteCode: 'MOCKSHOP',
        isOwner: false,
        itemsCount: 0,
        createdAt: '2026-03-11T00:00:00.000Z',
        updatedAt: '2026-03-11T00:00:00.000Z',
      },
    })
    sessionStorage.setItem('pendingInviteCode', 'INVITE-123')

    renderModal(onClose)

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'leo@test.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: '123456' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => {
      expect(listsApi.joinList).toHaveBeenCalledWith('INVITE-123')
    })

    expect(onClose).not.toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/lists/list-joined', {
      state: {
        toastMessage: 'Entrou!',
        toastType: 'success',
      },
    })
    expect(sessionStorage.getItem('pendingInviteCode')).toBeNull()
  })

  it('preserva redirect de rota protegida e navega para ele apos login', async () => {
    const client = await import('../api/client')
    vi.mocked(client.default.post).mockResolvedValueOnce({
      data: {
        id: 'user-1',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
        token: 'token',
      },
    })

    renderModal(vi.fn(), '/?auth=login&redirect=%2Fhome')

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'leo@test.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: '123456' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/home')
    })

    expect(sessionStorage.getItem('postLoginRedirect')).toBeNull()
  })

  it('propaga redirect para links de cadastro e esqueci a senha', () => {
    renderModal(vi.fn(), '/?auth=login&redirect=%2Flists%2Fabc')

    expect(screen.getByRole('link', { name: 'Criar conta' })).toHaveAttribute(
      'href',
      '/register?redirect=%2Flists%2Fabc'
    )
    expect(screen.getByRole('link', { name: 'Esqueci minha senha' })).toHaveAttribute(
      'href',
      '/forgot-password?redirect=%2Flists%2Fabc'
    )
  })
})
