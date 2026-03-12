import { describe, it, expect, vi, beforeEach } from 'vitest'
import userEvent from '@testing-library/user-event'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AppHeader } from './AppHeader'

const mockNavigate = vi.fn()
const mockLogout = vi.fn()
const mockStartReplay = vi.fn()
let mockIsMobile = false

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

let mockAvatarUrl: string | null = null

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'user-1',
      username: 'leo',
      email: 'leo@test.com',
      displayName: 'Leo Oliveira',
      get avatarUrl() {
        return mockAvatarUrl
      },
      onboardingCompletedAt: null,
    },
    logout: mockLogout,
  }),
}))

vi.mock('../contexts/OnboardingContext', () => ({
  useOnboarding: () => ({
    startReplay: mockStartReplay,
  }),
}))

const mockEnablePush = vi.fn()
const mockDisablePush = vi.fn()
let mockPermissionState: string = 'default'
let mockPushEnabled = false
let mockAvailability: 'available' | 'unsupported' | 'server-not-configured' | 'checking' =
  'available'

vi.mock('../hooks/usePushNotifications', () => ({
  usePushNotifications: () => ({
    get permissionState() {
      return mockPermissionState
    },
    get pushEnabled() {
      return mockPushEnabled
    },
    get availability() {
      return mockAvailability
    },
    enablePush: mockEnablePush,
    disablePush: mockDisablePush,
  }),
}))

vi.mock('../hooks/useIsMobile', () => ({
  useIsMobile: () => mockIsMobile,
}))

describe('AppHeader', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    mockLogout.mockReset()
    mockStartReplay.mockReset()
    mockEnablePush.mockReset()
    mockDisablePush.mockReset()
    mockPermissionState = 'default'
    mockPushEnabled = false
    mockAvailability = 'available'
    mockAvatarUrl = null
    mockIsMobile = false
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

  it('exibe botão de ativar push e chama enablePush ao clicar', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    const pushBtn = screen.getByRole('menuitem', { name: /Ativar notificações push/i })
    expect(pushBtn).toBeInTheDocument()

    await user.click(pushBtn)

    expect(mockEnablePush).toHaveBeenCalledTimes(1)
  })

  it('exibe botão de desativar push quando já está ativo', async () => {
    mockPermissionState = 'granted'
    mockPushEnabled = true
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    const disableBtn = screen.getByRole('menuitem', { name: /Desativar notificações push/i })
    expect(disableBtn).toBeInTheDocument()
    await user.click(disableBtn)
    expect(mockDisablePush).toHaveBeenCalledTimes(1)
  })

  it('oculta botão de push quando API não suportada', async () => {
    mockPermissionState = 'unsupported'
    mockAvailability = 'unsupported'
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    expect(screen.queryByRole('menuitem', { name: /Ativar notificações push/i })).toBeNull()
  })

  it('mostra status de indisponível e oculta ações quando backend não tem VAPID', async () => {
    mockAvailability = 'server-not-configured'
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    expect(screen.getByText(/Push: indisponível no ambiente/i)).toBeInTheDocument()
    expect(screen.queryByRole('menuitem', { name: /Ativar notificações push/i })).toBeNull()
    expect(screen.queryByRole('menuitem', { name: /Desativar notificações push/i })).toBeNull()
  })

  it('exibe avatar quando usuário tem avatarUrl', () => {
    mockAvatarUrl = 'https://example.com/avatar.jpg'

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    expect(screen.getByRole('img', { name: 'Leo Oliveira' })).toBeInTheDocument()
  })

  it('fecha menu ao clicar fora', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    fireEvent.mouseDown(document.body)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('renderiza botão de voltar quando onBack é fornecido', async () => {
    const mockOnBack = vi.fn()
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Lista" onBack={mockOnBack} backLabel="Voltar" />
      </MemoryRouter>
    )

    const backBtn = screen.getByRole('button', { name: 'Voltar' })
    expect(backBtn).toBeInTheDocument()
    await user.click(backBtn)
    expect(mockOnBack).toHaveBeenCalledTimes(1)
  })

  it('fecha menu ao pressionar Escape', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('usa sheet no mobile e renderiza ações extras da conta', async () => {
    mockIsMobile = true
    const user = userEvent.setup()
    const mobileAction = vi.fn()

    render(
      <MemoryRouter>
        <AppHeader
          title="Minhas Listas"
          subtitle="Organize seu dia."
          accountActions={
            <button type="button" onClick={mobileAction} className="nl-action-row">
              Ação extra
            </button>
          }
        />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    expect(screen.getByRole('dialog', { name: 'Conta' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Home' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Meu perfil' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Ação extra' }))
    expect(mobileAction).toHaveBeenCalledTimes(1)
  })

  it('expande a ação principal e mantém ações secundárias navegáveis no mobile', async () => {
    mockIsMobile = true
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader
          title="Minhas Listas"
          primaryAction={
            <button type="button" className="cta-principal">
              Nova lista
            </button>
          }
          secondaryActions={[
            <button key="history" type="button">
              Histórico
            </button>,
            <button key="members" type="button">
              Membros
            </button>,
          ]}
        />
      </MemoryRouter>
    )

    expect(screen.getByRole('button', { name: 'Nova lista' })).toHaveClass('w-full')
    expect(screen.getByRole('button', { name: 'Histórico' }).parentElement).toHaveClass(
      'overflow-x-auto',
      'pb-1'
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    await user.click(screen.getByRole('button', { name: 'Home' }))

    expect(mockNavigate).toHaveBeenCalledWith('/home')
  })

  it('navega para o perfil pelo sheet mobile', async () => {
    mockIsMobile = true
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AppHeader title="Minhas Listas" />
      </MemoryRouter>
    )

    await user.click(screen.getByRole('button', { name: 'Abrir menu da conta' }))
    await user.click(screen.getByRole('button', { name: 'Meu perfil' }))

    expect(mockNavigate).toHaveBeenCalledWith('/profile')
  })
})
