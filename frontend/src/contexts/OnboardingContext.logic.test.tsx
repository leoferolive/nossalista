import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { OnboardingProvider, useOnboarding } from './OnboardingContext'
import { ToastProvider } from './ToastContext'

const mockMarkOnboardingCompleted = vi.fn()
const mockCompleteOnboarding = vi.fn()

const authState = {
  isAuthenticated: true,
  user: {
    id: 'user-1',
    username: 'leo',
    email: 'leo@test.com',
    displayName: 'Leo',
    avatarUrl: null,
    onboardingCompletedAt: null as string | null,
  },
  markOnboardingCompleted: mockMarkOnboardingCompleted,
}

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => authState,
}))

vi.mock('../api/usersApi', () => ({
  usersApi: {
    completeOnboarding: (...args: unknown[]) => mockCompleteOnboarding(...args),
  },
}))

vi.mock('../components/OnboardingTourOverlay', () => ({
  OnboardingTourOverlay: ({
    active,
    step,
    canAdvance,
    currentStepIndex,
    totalSteps,
    onNext,
    onSkip,
  }: {
    active: boolean
    step: { id: string } | null
    canAdvance: boolean
    currentStepIndex: number
    totalSteps: number
    onNext: () => void
    onSkip: () => void
  }) => {
    if (!active) {
      return null
    }

    return (
      <div>
        <p data-testid="overlay-step">{step?.id ?? 'none'}</p>
        <p data-testid="overlay-index">
          {currentStepIndex}/{totalSteps}
        </p>
        <p data-testid="overlay-can-advance">{String(canAdvance)}</p>
        <button type="button" onClick={() => void onNext()}>
          mock-next
        </button>
        <button type="button" onClick={() => void onSkip()}>
          mock-skip
        </button>
      </div>
    )
  },
}))

function ContextProbe() {
  const {
    isRunning,
    activeStepId,
    requestOpenCreateListModal,
    acknowledgeCreateListModalRequest,
    onListCreated,
    startReplay,
  } = useOnboarding()
  const location = useLocation()
  const navigate = useNavigate()

  return (
    <div>
      <span data-testid="running">{String(isRunning)}</span>
      <span data-testid="active-step">{activeStepId ?? 'none'}</span>
      <span data-testid="request-open">{String(requestOpenCreateListModal)}</span>
      <span data-testid="path">{location.pathname}</span>

      <button type="button" onClick={() => onListCreated('list-123')}>
        mark-created
      </button>
      <button type="button" onClick={acknowledgeCreateListModalRequest}>
        ack-modal
      </button>
      <button type="button" onClick={startReplay}>
        start-replay
      </button>
      <button type="button" onClick={() => navigate('/home')}>
        go-home
      </button>
      <button type="button" onClick={() => navigate('/lists/other')}>
        go-list
      </button>
    </div>
  )
}

function renderWithRouter(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <ToastProvider>
        <OnboardingProvider>
          <ContextProbe />
          <Routes>
            <Route path="/home" element={<div>Home</div>} />
            <Route path="/lists/:id" element={<div>List</div>} />
          </Routes>
        </OnboardingProvider>
      </ToastProvider>
    </MemoryRouter>
  )
}

describe('OnboardingContext (lógica de fluxo)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authState.isAuthenticated = true
    authState.user = {
      id: 'user-1',
      username: 'leo',
      email: 'leo@test.com',
      displayName: 'Leo',
      avatarUrl: null,
      onboardingCompletedAt: null,
    }
    mockCompleteOnboarding.mockResolvedValue(undefined)
  })

  it('na etapa de criação sem lista criada, mantém etapa e mostra aviso', async () => {
    const user = userEvent.setup()
    renderWithRouter('/home')

    expect(await screen.findByTestId('overlay-step')).toHaveTextContent('home-create-list')
    await user.click(screen.getByRole('button', { name: 'mock-next' }))

    expect(screen.getByTestId('overlay-step')).toHaveTextContent('create-list-modal')
    expect(screen.getByTestId('request-open')).toHaveTextContent('true')

    await user.click(screen.getByRole('button', { name: 'ack-modal' }))
    expect(screen.getByTestId('request-open')).toHaveTextContent('false')

    await user.click(screen.getByRole('button', { name: 'mock-next' }))

    expect(await screen.findByText('Crie a lista para continuar o tutorial.')).toBeInTheDocument()
    expect(screen.getByTestId('overlay-step')).toHaveTextContent('create-list-modal')
  })

  it('navega para lista criada e conclui tutorial persistindo no backend', async () => {
    const user = userEvent.setup()
    renderWithRouter('/home')

    await user.click(await screen.findByRole('button', { name: 'mock-next' }))
    await user.click(screen.getByRole('button', { name: 'mark-created' }))

    await waitFor(() => {
      expect(screen.getByTestId('path')).toHaveTextContent('/lists/list-123')
      expect(screen.getByTestId('overlay-step')).toHaveTextContent('list-ready')
    })

    await user.click(screen.getByRole('button', { name: 'mock-next' }))
    await user.click(screen.getByRole('button', { name: 'mock-next' }))
    await user.click(screen.getByRole('button', { name: 'mock-next' }))
    await user.click(screen.getByRole('button', { name: 'mock-next' }))

    await waitFor(() => {
      expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1)
      expect(mockMarkOnboardingCompleted).toHaveBeenCalledTimes(1)
      expect(
        screen.getByText('Tutorial concluído. Sua primeira colaboração já começou.')
      ).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'mock-next' })).not.toBeInTheDocument()
    })
  })

  it('quando persistência falha no skip, mostra aviso e não reinicia automaticamente na sessão', async () => {
    const user = userEvent.setup()
    mockCompleteOnboarding.mockRejectedValueOnce(new Error('falha'))
    renderWithRouter('/home')

    await user.click(await screen.findByRole('button', { name: 'mock-skip' }))

    await waitFor(() => {
      expect(
        screen.getByText('Tutorial encerrado. Não foi possível salvar essa preferência agora.')
      ).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'mock-next' })).not.toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Fechar notificação' }))
    await waitFor(() => {
      expect(
        screen.queryByText('Tutorial encerrado. Não foi possível salvar essa preferência agora.')
      ).not.toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'go-list' }))
    await user.click(screen.getByRole('button', { name: 'go-home' }))

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'mock-next' })).not.toBeInTheDocument()
    })
  })

  it('em replay manual de usuário já onboarded, não persiste conclusão novamente', async () => {
    const user = userEvent.setup()
    authState.user.onboardingCompletedAt = '2026-03-10T10:00:00.000Z'
    renderWithRouter('/lists/existente')

    await user.click(screen.getByRole('button', { name: 'start-replay' }))
    await waitFor(() => {
      expect(screen.getByTestId('path')).toHaveTextContent('/home')
      expect(screen.getByRole('button', { name: 'mock-skip' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'mock-skip' }))

    await waitFor(() => {
      expect(mockCompleteOnboarding).not.toHaveBeenCalled()
      expect(mockMarkOnboardingCompleted).not.toHaveBeenCalled()
    })
  })

  it('retorna funções noop seguras ao usar hook fora do provider', async () => {
    const user = userEvent.setup()

    function OutsideProviderProbe() {
      const onboarding = useOnboarding()
      return (
        <button
          type="button"
          onClick={() => {
            onboarding.acknowledgeCreateListModalRequest()
            onboarding.onListCreated('noop-list')
            onboarding.startReplay()
          }}
        >
          call-default-noops
        </button>
      )
    }

    render(<OutsideProviderProbe />)
    await user.click(screen.getByRole('button', { name: 'call-default-noops' }))

    expect(screen.getByRole('button', { name: 'call-default-noops' })).toBeInTheDocument()
  })
})
