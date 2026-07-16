import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { Profile } from './Profile'
import { ThemeProvider } from '../contexts/ThemeContext'
import { ToastProvider } from '../contexts/ToastContext'
import { usersApi } from '../api/usersApi'
import { ApiError } from '../types/ApiError'

const mockLogout = vi.fn()
const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ logout: mockLogout }),
}))

vi.mock('../api/usersApi', () => ({
  usersApi: {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    deleteAccount: vi.fn(),
  },
}))

const baseProfile = {
  username: 'leo',
  email: 'leo@example.com',
  name: 'Leo',
  avatarUrl: null,
  authProvider: 'GOOGLE',
  onboardingCompletedAt: null,
}

function renderProfile() {
  return render(
    <ThemeProvider>
      <ToastProvider>
        <MemoryRouter>
          <Profile />
        </MemoryRouter>
      </ToastProvider>
    </ThemeProvider>
  )
}

describe('Profile page', () => {
  beforeEach(() => {
    mockLogout.mockReset()
    mockNavigate.mockReset()
    vi.mocked(usersApi.getProfile).mockReset()
    vi.mocked(usersApi.updateProfile).mockReset()
    vi.mocked(usersApi.deleteAccount).mockReset()

    // jsdom nao implementa <dialog>.showModal()/close() — simula a abertura/fechamento
    // nativa para que o conteudo do modal fique acessivel nas queries de role.
    HTMLDialogElement.prototype.showModal = vi.fn(function (this: HTMLDialogElement) {
      this.setAttribute('open', '')
    })
    HTMLDialogElement.prototype.close = vi.fn(function (this: HTMLDialogElement) {
      this.removeAttribute('open')
    })
  })

  it('mostra o estado de carregamento e depois os dados do perfil', async () => {
    vi.mocked(usersApi.getProfile).mockResolvedValueOnce(baseProfile)

    renderProfile()

    expect(screen.getByRole('status', { name: /carregando perfil/i })).toBeInTheDocument()

    await waitFor(() => expect(screen.getByText('Seu Perfil')).toBeInTheDocument())
    expect(screen.getByText('leo@example.com')).toBeInTheDocument()
  })

  it('mostra o estado de erro quando o carregamento falha e permite tentar novamente', async () => {
    vi.mocked(usersApi.getProfile).mockRejectedValueOnce(new Error('Falha ao buscar perfil'))
    const reload = vi.fn()
    Object.defineProperty(window, 'location', {
      value: { ...window.location, reload },
      writable: true,
    })

    renderProfile()

    await waitFor(() => expect(screen.getByText('Erro ao Carregar Perfil')).toBeInTheDocument())
    expect(screen.getAllByText('Falha ao buscar perfil').length).toBeGreaterThanOrEqual(1)

    fireEvent.click(screen.getByRole('button', { name: 'Tentar Novamente' }))
    expect(reload).toHaveBeenCalled()
  })

  it('faz logout e navega para a landing', async () => {
    vi.mocked(usersApi.getProfile).mockResolvedValueOnce(baseProfile)

    renderProfile()

    await waitFor(() => expect(screen.getByText('Seu Perfil')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: 'Sair da conta' }))

    expect(mockLogout).toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
  })

  it('abre e fecha o dialogo de confirmacao de exclusao de conta', async () => {
    vi.mocked(usersApi.getProfile).mockResolvedValueOnce(baseProfile)

    renderProfile()

    await waitFor(() => expect(screen.getByText('Seu Perfil')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: 'Excluir conta permanentemente' }))
    expect(await screen.findByRole('heading', { name: 'Excluir conta' })).toBeInTheDocument()

    fireEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))
  })

  it('exclui a conta com sucesso e redireciona apos confirmar', async () => {
    vi.mocked(usersApi.getProfile).mockResolvedValueOnce(baseProfile)
    vi.mocked(usersApi.deleteAccount).mockResolvedValueOnce(undefined)

    renderProfile()

    await waitFor(() => expect(screen.getByText('Seu Perfil')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: 'Excluir conta permanentemente' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Confirmar exclusão da conta' }))

    await waitFor(() => expect(usersApi.deleteAccount).toHaveBeenCalled())
    expect(mockLogout).toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
  })

  it('mostra toast de erro quando a exclusao da conta falha', async () => {
    vi.mocked(usersApi.getProfile).mockResolvedValueOnce(baseProfile)
    vi.mocked(usersApi.deleteAccount).mockRejectedValueOnce(new ApiError('Erro ao excluir', 500))

    renderProfile()

    await waitFor(() => expect(screen.getByText('Seu Perfil')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: 'Excluir conta permanentemente' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Confirmar exclusão da conta' }))

    await waitFor(() => expect(usersApi.deleteAccount).toHaveBeenCalled())
    expect(mockNavigate).not.toHaveBeenCalledWith('/', { replace: true })
  })
})
