import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { usersApi } from '../api/usersApi'
import { useAuth } from './AuthContext'
import { Toast, useToast } from '../components/Toast'
import { OnboardingTourOverlay } from '../components/OnboardingTourOverlay'

export type OnboardingStepId =
  | 'home-create-list'
  | 'create-list-modal'
  | 'list-ready'
  | 'list-invite'
  | 'list-add-edit'
  | 'list-realtime'

interface OnboardingStep {
  id: OnboardingStepId
  route: 'home' | 'list'
  selector: string
  title: string
  description: string
}

const ONBOARDING_STEPS: OnboardingStep[] = [
  {
    id: 'home-create-list',
    route: 'home',
    selector: '[data-tour="home-create-list"]',
    title: 'Comece pela primeira lista',
    description:
      'Use este botão para abrir o criador. Em menos de um minuto você começa a colaborar de verdade.',
  },
  {
    id: 'create-list-modal',
    route: 'home',
    selector: '[data-tour="create-list-modal"]',
    title: 'Defina nome e tipo',
    description:
      'Escolha um nome claro, selecione o tipo e confirme em “Criar Lista”. Assim que criar, levamos você para a lista automaticamente.',
  },
  {
    id: 'list-ready',
    route: 'list',
    selector: '[data-tour="list-header"]',
    title: 'Lista criada e pronta para uso',
    description:
      'Este é o hub da colaboração. Aqui você organiza itens, acompanha presença online e controla convites.',
  },
  {
    id: 'list-invite',
    route: 'list',
    selector: '[data-tour="list-invite"]',
    title: 'Compartilhe com os amigos',
    description:
      'Clique em “Convidar” para chamar por username ou gerar um link único. Ideal para trazer o grupo rapidamente.',
  },
  {
    id: 'list-add-edit',
    route: 'list',
    selector: '[data-tour="list-add-item"]',
    title: 'Adicione e edite itens',
    description:
      'Crie um item por aqui. Para editar depois, toque no item ou use o menu de opções com clique longo no celular.',
  },
  {
    id: 'list-realtime',
    route: 'list',
    selector: '[data-tour="list-realtime"]',
    title: 'Acompanhe tudo em tempo real',
    description:
      'Conexão ativa e mudanças aparecendo na hora. Quando quiser revisar o histórico, use o botão “Histórico” no topo.',
  },
]

interface TourState {
  active: boolean
  shouldPersistCompletion: boolean
  stepIndex: number
  createdListId: string | null
}

interface OnboardingContextValue {
  isRunning: boolean
  activeStepId: OnboardingStepId | null
  requestOpenCreateListModal: boolean
  acknowledgeCreateListModalRequest: () => void
  onListCreated: (listId: string) => void
  startReplay: () => void
}

const OnboardingContext = createContext<OnboardingContextValue>({
  isRunning: false,
  activeStepId: null,
  requestOpenCreateListModal: false,
  acknowledgeCreateListModalRequest: () => undefined,
  onListCreated: () => undefined,
  startReplay: () => undefined,
})

function isHomeRoute(pathname: string) {
  return pathname === '/home'
}

function isListRoute(pathname: string) {
  return pathname.startsWith('/lists/')
}

export function OnboardingProvider({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, isAuthenticated, markOnboardingCompleted } = useAuth()
  const { toasts, showToast, removeToast } = useToast()
  const autoStartAttemptedRef = useRef(false)

  const [tourState, setTourState] = useState<TourState>({
    active: false,
    shouldPersistCompletion: false,
    stepIndex: 0,
    createdListId: null,
  })
  const [requestOpenCreateListModal, setRequestOpenCreateListModal] = useState(false)
  const [suppressAutoStartInSession, setSuppressAutoStartInSession] = useState(false)

  const currentStep = tourState.active ? ONBOARDING_STEPS[tourState.stepIndex] : null

  const startTour = useCallback(() => {
    const shouldPersistCompletion = user?.onboardingCompletedAt == null

    setTourState({
      active: true,
      shouldPersistCompletion,
      stepIndex: 0,
      createdListId: null,
    })
    setRequestOpenCreateListModal(false)

    if (!isHomeRoute(location.pathname)) {
      navigate('/home')
    }
  }, [location.pathname, navigate, user?.onboardingCompletedAt])

  const finishTour = useCallback(
    async (reason: 'completed' | 'skipped') => {
      const shouldPersist = tourState.shouldPersistCompletion

      setTourState((prev) => ({
        ...prev,
        active: false,
        createdListId: null,
      }))
      setRequestOpenCreateListModal(false)

      if (!shouldPersist) {
        return
      }

      try {
        await usersApi.completeOnboarding()
        markOnboardingCompleted()
      } catch {
        setSuppressAutoStartInSession(true)
        showToast('Tutorial encerrado. Não foi possível salvar essa preferência agora.', 'info')
      }

      if (reason === 'completed') {
        showToast('Tutorial concluído. Sua primeira colaboração já começou.', 'success')
      }
    },
    [markOnboardingCompleted, showToast, tourState.shouldPersistCompletion]
  )

  const handleNext = useCallback(async () => {
    if (!tourState.active) {
      return
    }

    if (tourState.stepIndex === 0) {
      setTourState((prev) => ({ ...prev, stepIndex: 1 }))
      setRequestOpenCreateListModal(true)
      return
    }

    if (tourState.stepIndex === 1) {
      if (!tourState.createdListId) {
        showToast('Crie a lista para continuar o tutorial.', 'info')
      }
      return
    }

    if (tourState.stepIndex >= ONBOARDING_STEPS.length - 1) {
      await finishTour('completed')
      return
    }

    setTourState((prev) => ({ ...prev, stepIndex: prev.stepIndex + 1 }))
  }, [finishTour, showToast, tourState.active, tourState.createdListId, tourState.stepIndex])

  const handleSkip = useCallback(async () => {
    if (!tourState.active) {
      return
    }

    await finishTour('skipped')
  }, [finishTour, tourState.active])

  const startReplay = useCallback(() => {
    startTour()
  }, [startTour])

  const acknowledgeCreateListModalRequest = useCallback(() => {
    setRequestOpenCreateListModal(false)
  }, [])

  const onListCreated = useCallback(
    (listId: string) => {
      setTourState((prev) => {
        if (!prev.active) {
          return prev
        }

        return {
          ...prev,
          createdListId: listId,
          stepIndex: Math.max(prev.stepIndex, 2),
        }
      })
      navigate(`/lists/${listId}`)
    },
    [navigate]
  )

  const canAdvance = useMemo(() => {
    if (!tourState.active) {
      return false
    }

    if (tourState.stepIndex === 1) {
      return false
    }

    return true
  }, [tourState.active, tourState.stepIndex])

  useEffect(() => {
    if (!isAuthenticated || !user) {
      autoStartAttemptedRef.current = false
      setSuppressAutoStartInSession(false)
      setTourState((prev) => ({ ...prev, active: false }))
      return
    }

    if (user.onboardingCompletedAt) {
      return
    }

    if (!isHomeRoute(location.pathname)) {
      return
    }

    if (tourState.active || suppressAutoStartInSession || autoStartAttemptedRef.current) {
      return
    }

    autoStartAttemptedRef.current = true
    startTour()
  }, [
    isAuthenticated,
    location.pathname,
    startTour,
    suppressAutoStartInSession,
    tourState.active,
    user,
  ])

  useEffect(() => {
    if (!tourState.active || !currentStep) {
      return
    }

    if (currentStep.route === 'home' && !isHomeRoute(location.pathname)) {
      navigate('/home')
      return
    }

    if (currentStep.route === 'list') {
      if (tourState.createdListId && location.pathname !== `/lists/${tourState.createdListId}`) {
        navigate(`/lists/${tourState.createdListId}`)
      } else if (!isListRoute(location.pathname) && tourState.createdListId) {
        navigate(`/lists/${tourState.createdListId}`)
      }
    }
  }, [currentStep, location.pathname, navigate, tourState.active, tourState.createdListId])

  const value = useMemo<OnboardingContextValue>(
    () => ({
      isRunning: tourState.active,
      activeStepId: currentStep?.id ?? null,
      requestOpenCreateListModal,
      acknowledgeCreateListModalRequest,
      onListCreated,
      startReplay,
    }),
    [
      acknowledgeCreateListModalRequest,
      currentStep?.id,
      onListCreated,
      requestOpenCreateListModal,
      startReplay,
      tourState.active,
    ]
  )

  return (
    <OnboardingContext.Provider value={value}>
      {children}
      <OnboardingTourOverlay
        active={tourState.active}
        step={currentStep}
        currentStepIndex={tourState.stepIndex}
        totalSteps={ONBOARDING_STEPS.length}
        canAdvance={canAdvance}
        onNext={handleNext}
        onSkip={handleSkip}
      />
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          message={toast.message}
          type={toast.type}
          duration={3200}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </OnboardingContext.Provider>
  )
}

export function useOnboarding() {
  return useContext(OnboardingContext)
}
