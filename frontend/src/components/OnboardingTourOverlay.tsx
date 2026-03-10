import { useEffect, useMemo, useRef, useState } from 'react'
import type { OnboardingStepId } from '../contexts/OnboardingContext'

interface StepConfig {
  id: OnboardingStepId
  selector: string
  title: string
  description: string
}

interface OnboardingTourOverlayProps {
  active: boolean
  step: StepConfig | null
  currentStepIndex: number
  totalSteps: number
  canAdvance: boolean
  onNext: () => void
  onSkip: () => void
}

interface SpotlightRect {
  top: number
  left: number
  width: number
  height: number
}

function useIsMobileBreakpoint() {
  const [isMobile, setIsMobile] = useState(() => window.innerWidth < 768)

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 768)
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return isMobile
}

function buildSpotlightRect(step: StepConfig | null) {
  if (!step) {
    return null
  }

  const target = document.querySelector<HTMLElement>(step.selector)
  if (!target) {
    return null
  }

  const rect = target.getBoundingClientRect()
  const padding = 10

  return {
    top: Math.max(8, rect.top - padding),
    left: Math.max(8, rect.left - padding),
    width: Math.min(window.innerWidth - 16, rect.width + padding * 2),
    height: Math.min(window.innerHeight - 16, rect.height + padding * 2),
  }
}

export function OnboardingTourOverlay({
  active,
  step,
  currentStepIndex,
  totalSteps,
  canAdvance,
  onNext,
  onSkip,
}: OnboardingTourOverlayProps) {
  const panelRef = useRef<HTMLDivElement>(null)
  const isMobile = useIsMobileBreakpoint()
  const [spotlightRect, setSpotlightRect] = useState<SpotlightRect | null>(null)

  useEffect(() => {
    if (!active || !step) {
      setSpotlightRect(null)
      return
    }

    let animationFrameId: number | null = null
    const recompute = () => {
      const rect = buildSpotlightRect(step)
      setSpotlightRect(rect)

      if (rect) {
        const target = document.querySelector<HTMLElement>(step.selector)
        target?.scrollIntoView({ block: 'center', behavior: 'smooth' })
      }
    }

    const scheduleRecompute = () => {
      if (animationFrameId != null) {
        window.cancelAnimationFrame(animationFrameId)
      }

      animationFrameId = window.requestAnimationFrame(() => {
        animationFrameId = null
        recompute()
      })
    }

    scheduleRecompute()
    window.addEventListener('resize', scheduleRecompute)
    window.addEventListener('scroll', scheduleRecompute, true)

    const observer = new MutationObserver(() => {
      scheduleRecompute()
    })
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'style', 'data-tour', 'aria-hidden'],
    })

    return () => {
      if (animationFrameId != null) {
        window.cancelAnimationFrame(animationFrameId)
      }
      observer.disconnect()
      window.removeEventListener('resize', scheduleRecompute)
      window.removeEventListener('scroll', scheduleRecompute, true)
    }
  }, [active, step])

  useEffect(() => {
    if (!active || !panelRef.current) {
      return
    }

    panelRef.current.focus()
  }, [active, currentStepIndex])

  useEffect(() => {
    if (!active) {
      return
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        void onSkip()
      }

      if ((event.key === 'Enter' || event.key === 'N') && canAdvance) {
        event.preventDefault()
        void onNext()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [active, canAdvance, onNext, onSkip])

  const isLastStep = currentStepIndex === totalSteps - 1
  const isCreateStep = step?.id === 'create-list-modal'

  const panelStyle = useMemo(() => {
    if (!step) {
      return {
        left: 16,
        right: 16,
        bottom: 16,
      }
    }

    if (step.id === 'create-list-modal' && !isMobile) {
      return {
        width: 360,
        top: 16,
        left: 16,
      }
    }

    if (isMobile || !spotlightRect) {
      return {
        left: 16,
        right: 16,
        bottom: 16,
      }
    }

    const panelWidth = 360
    const viewportPadding = 16
    const targetRight = spotlightRect.left + spotlightRect.width
    const canPlaceRight = targetRight + panelWidth + viewportPadding <= window.innerWidth
    const canPlaceLeft = spotlightRect.left - panelWidth - viewportPadding >= 0

    let left = spotlightRect.left
    if (canPlaceRight) {
      left = targetRight + 14
    } else if (canPlaceLeft) {
      left = spotlightRect.left - panelWidth - 14
    } else {
      left = Math.max(
        viewportPadding,
        Math.min(window.innerWidth - panelWidth - viewportPadding, left)
      )
    }

    const top = Math.max(
      viewportPadding,
      Math.min(window.innerHeight - 230 - viewportPadding, spotlightRect.top)
    )

    return {
      width: panelWidth,
      top,
      left,
    }
  }, [isMobile, spotlightRect, step])

  if (!active || !step) {
    return null
  }

  return (
    <div className="pointer-events-none fixed inset-0 z-[80]" aria-live="polite">
      {spotlightRect ? (
        <div
          className="absolute rounded-[22px] border border-amber-300/70 shadow-[0_0_0_9999px_rgba(8,6,4,0.76),0_0_0_3px_rgba(255,239,206,0.2)]"
          style={{
            top: spotlightRect.top,
            left: spotlightRect.left,
            width: spotlightRect.width,
            height: spotlightRect.height,
          }}
          aria-hidden="true"
        />
      ) : (
        <div className="absolute inset-0 bg-[rgba(8,6,4,0.78)]" aria-hidden="true" />
      )}

      <div
        ref={panelRef}
        tabIndex={-1}
        className={`fixed rounded-3xl border border-amber-200/40 bg-gradient-to-br from-[#2f2016] via-[#281b13] to-[#1e1510] p-5 text-[#f7ecde] shadow-[0_30px_80px_rgba(0,0,0,0.55)] backdrop-blur-md ${isCreateStep ? 'pointer-events-none' : 'pointer-events-auto'}`}
        style={panelStyle}
        role="dialog"
        aria-modal="true"
        aria-label={`Tutorial - passo ${currentStepIndex + 1} de ${totalSteps}`}
      >
        <p className="font-sans text-[10px] font-semibold uppercase tracking-[0.28em] text-amber-200/80">
          Tutorial rapido • passo {currentStepIndex + 1} de {totalSteps}
        </p>

        <h3 className="mt-2 font-display text-2xl leading-tight text-[#fff6ea]">{step.title}</h3>

        <p className="mt-2 font-sans text-sm leading-6 text-amber-50/90">{step.description}</p>

        {!isCreateStep && (
          <div className="mt-5 flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={() => void onSkip()}
              className="rounded-2xl border border-amber-200/35 px-4 py-2.5 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-amber-100 transition-colors hover:bg-amber-50/10 focus-visible:ring-2 focus-visible:ring-amber-200/60"
            >
              Pular
            </button>

            <button
              type="button"
              disabled={!canAdvance}
              onClick={() => void onNext()}
              className="rounded-2xl bg-gradient-to-r from-[#f3a45c] to-[#df7248] px-5 py-2.5 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-[#2f1a10] shadow-[0_12px_30px_rgba(207,117,71,0.45)] transition-transform hover:-translate-y-0.5 focus-visible:ring-2 focus-visible:ring-amber-100/70 disabled:cursor-not-allowed disabled:opacity-55"
            >
              {isLastStep ? 'Concluir' : 'Próximo'}
            </button>
          </div>
        )}

        {!canAdvance && currentStepIndex === 1 && (
          <p className="mt-3 font-sans text-xs text-amber-100/80">
            Crie a lista para liberar o próximo passo.
          </p>
        )}
      </div>
    </div>
  )
}
