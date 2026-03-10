import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { OnboardingProvider, useOnboarding } from './OnboardingContext'

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

function LocationProbe() {
  const location = useLocation()
  return <span data-testid="location-path">{location.pathname}</span>
}

function ReplayProbe() {
  const { startReplay } = useOnboarding()
  return (
    <button type="button" onClick={startReplay}>
      replay
    </button>
  )
}

describe('OnboardingContext', () => {
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

  it('inicia tutorial automaticamente na Home quando onboarding ainda não foi concluído', async () => {
    render(
      <MemoryRouter initialEntries={['/home']}>
        <OnboardingProvider>
          <Routes>
            <Route path="/home" element={<LocationProbe />} />
          </Routes>
        </OnboardingProvider>
      </MemoryRouter>
    )

    expect(await screen.findByText('Comece pela primeira lista')).toBeInTheDocument()
  })

  it('não inicia tutorial automaticamente quando onboarding já foi concluído', async () => {
    authState.user.onboardingCompletedAt = '2026-03-10T10:00:00.000Z'

    render(
      <MemoryRouter initialEntries={['/home']}>
        <OnboardingProvider>
          <Routes>
            <Route path="/home" element={<LocationProbe />} />
          </Routes>
        </OnboardingProvider>
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.queryByText('Comece pela primeira lista')).not.toBeInTheDocument()
    })
  })

  it('permite replay manual e redireciona para /home', async () => {
    authState.user.onboardingCompletedAt = '2026-03-10T10:00:00.000Z'
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/lists/abc']}>
        <OnboardingProvider>
          <ReplayProbe />
          <Routes>
            <Route path="/home" element={<LocationProbe />} />
            <Route path="/lists/:id" element={<LocationProbe />} />
          </Routes>
        </OnboardingProvider>
      </MemoryRouter>
    )

    expect(screen.getByTestId('location-path')).toHaveTextContent('/lists/abc')

    await user.click(screen.getByRole('button', { name: 'replay' }))

    await waitFor(() => {
      expect(screen.getByTestId('location-path')).toHaveTextContent('/home')
      expect(screen.getByText('Comece pela primeira lista')).toBeInTheDocument()
    })
  })

  it('ao pular, persiste conclusão do onboarding', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/home']}>
        <OnboardingProvider>
          <Routes>
            <Route path="/home" element={<LocationProbe />} />
          </Routes>
        </OnboardingProvider>
      </MemoryRouter>
    )

    await user.click(await screen.findByRole('button', { name: 'Pular' }))

    await waitFor(() => {
      expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1)
      expect(mockMarkOnboardingCompleted).toHaveBeenCalledTimes(1)
    })
  })
})
