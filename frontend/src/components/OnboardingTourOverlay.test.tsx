import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OnboardingTourOverlay } from './OnboardingTourOverlay'

const baseStep = {
  id: 'home-create-list' as const,
  selector: '[data-tour="target"]',
  title: 'Titulo do passo',
  description: 'Descricao do passo',
}

function setViewport(width: number, height = 900) {
  Object.defineProperty(window, 'innerWidth', {
    writable: true,
    configurable: true,
    value: width,
  })
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: height,
  })
}

function appendTarget() {
  const target = document.createElement('button')
  target.setAttribute('data-tour', 'target')
  target.getBoundingClientRect = vi.fn(() => ({
    width: 200,
    height: 60,
    top: 100,
    left: 50,
    right: 250,
    bottom: 160,
    x: 50,
    y: 100,
    toJSON: () => ({}),
  }))
  target.scrollIntoView = vi.fn()
  document.body.appendChild(target)
  return target
}

describe('OnboardingTourOverlay', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
    setViewport(1280)
  })

  it('não renderiza quando estiver inativo', () => {
    const { container } = render(
      <OnboardingTourOverlay
        active={false}
        step={baseStep}
        currentStepIndex={0}
        totalSteps={6}
        canAdvance
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    expect(container).toBeEmptyDOMElement()
  })

  it('renderiza spotlight quando encontra o alvo e ajusta foco/scroll', async () => {
    const target = appendTarget()
    const { container } = render(
      <OnboardingTourOverlay
        active
        step={baseStep}
        currentStepIndex={0}
        totalSteps={6}
        canAdvance
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    await waitFor(() => {
      const spotlight = container.querySelector('[aria-hidden="true"][style*="width"]')
      expect(spotlight).toBeInTheDocument()
      expect(spotlight).toHaveStyle({
        top: '90px',
        left: '40px',
      })
    })

    expect(target.scrollIntoView).toHaveBeenCalled()
    expect(screen.getByRole('dialog')).toHaveFocus()
  })

  it('renderiza fallback escuro quando alvo não existe', async () => {
    const { container } = render(
      <OnboardingTourOverlay
        active
        step={baseStep}
        currentStepIndex={0}
        totalSteps={6}
        canAdvance
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    await waitFor(() => {
      const hiddenLayers = Array.from(container.querySelectorAll('[aria-hidden="true"]'))
      const fallbackLayer = hiddenLayers.find((layer) =>
        layer.className.includes('absolute inset-0 bg-[rgba(8,6,4,0.78)]')
      )
      expect(fallbackLayer).toBeInTheDocument()
    })
  })

  it('recalcula spotlight quando alvo aparece depois da renderização inicial', async () => {
    const { container } = render(
      <OnboardingTourOverlay
        active
        step={baseStep}
        currentStepIndex={0}
        totalSteps={6}
        canAdvance
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    await waitFor(() => {
      const hiddenLayers = Array.from(container.querySelectorAll('[aria-hidden="true"]'))
      const fallbackLayer = hiddenLayers.find((layer) =>
        layer.className.includes('absolute inset-0 bg-[rgba(8,6,4,0.78)]')
      )
      expect(fallbackLayer).toBeInTheDocument()
    })

    appendTarget()

    await waitFor(() => {
      const spotlight = container.querySelector('[aria-hidden="true"][style*="width"]')
      expect(spotlight).toBeInTheDocument()
      expect(spotlight).toHaveStyle({
        top: '90px',
        left: '40px',
      })
    })
  })

  it('dispara atalhos de teclado Esc/Enter/N conforme regra de avanço', async () => {
    appendTarget()
    const onNext = vi.fn()
    const onSkip = vi.fn()

    render(
      <OnboardingTourOverlay
        active
        step={baseStep}
        currentStepIndex={0}
        totalSteps={6}
        canAdvance
        onNext={onNext}
        onSkip={onSkip}
      />
    )

    fireEvent.keyDown(document, { key: 'Escape' })
    fireEvent.keyDown(document, { key: 'Enter' })
    fireEvent.keyDown(document, { key: 'N' })

    await waitFor(() => {
      expect(onSkip).toHaveBeenCalledTimes(1)
      expect(onNext).toHaveBeenCalledTimes(2)
    })
  })

  it('não avança com Enter quando canAdvance for false', async () => {
    appendTarget()
    const onNext = vi.fn()

    render(
      <OnboardingTourOverlay
        active
        step={baseStep}
        currentStepIndex={1}
        totalSteps={6}
        canAdvance={false}
        onNext={onNext}
        onSkip={vi.fn()}
      />
    )

    fireEvent.keyDown(document, { key: 'Enter' })
    await waitFor(() => {
      expect(onNext).not.toHaveBeenCalled()
    })
  })

  it('em etapa create-list esconde CTAs e mostra dica de continuidade', async () => {
    appendTarget()

    render(
      <OnboardingTourOverlay
        active
        step={{ ...baseStep, id: 'create-list-modal' }}
        currentStepIndex={1}
        totalSteps={6}
        canAdvance={false}
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    expect(
      await screen.findByText('Crie a lista para liberar o próximo passo.')
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Pular' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Próximo' })).not.toBeInTheDocument()
  })

  it('mantém o card no topo em desktop no passo create-list sem spotlight', async () => {
    render(
      <OnboardingTourOverlay
        active
        step={{ ...baseStep, id: 'create-list-modal' }}
        currentStepIndex={1}
        totalSteps={6}
        canAdvance={false}
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveStyle({
      top: '16px',
      left: '16px',
      width: '360px',
    })
  })

  it('exibe texto Concluir no último passo', async () => {
    appendTarget()

    render(
      <OnboardingTourOverlay
        active
        step={baseStep}
        currentStepIndex={5}
        totalSteps={6}
        canAdvance
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    expect(await screen.findByRole('button', { name: 'Concluir' })).toBeInTheDocument()
  })

  it('usa layout de mobile quando viewport é pequena', async () => {
    setViewport(600)
    appendTarget()

    render(
      <OnboardingTourOverlay
        active
        step={baseStep}
        currentStepIndex={0}
        totalSteps={6}
        canAdvance
        onNext={vi.fn()}
        onSkip={vi.fn()}
      />
    )

    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveStyle({
      left: '16px',
      right: '16px',
      bottom: '16px',
    })
  })
})
