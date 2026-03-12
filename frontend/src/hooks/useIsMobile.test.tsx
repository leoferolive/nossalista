import { act, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useIsMobile } from './useIsMobile'

function TestComponent() {
  const isMobile = useIsMobile()
  return <span>{isMobile ? 'mobile' : 'desktop'}</span>
}

describe('useIsMobile', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('retorna false quando matchMedia não está disponível', () => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: undefined,
    })

    render(<TestComponent />)

    expect(screen.getByText('desktop')).toBeInTheDocument()
  })

  it('usa o valor inicial do media query e reage a mudanças', () => {
    let changeListener: ((event: MediaQueryListEvent) => void) | undefined
    const removeEventListener = vi.fn()

    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: vi.fn().mockImplementation(() => ({
        matches: true,
        addEventListener: vi.fn(
          (_event: string, listener: (event: MediaQueryListEvent) => void) => {
            changeListener = listener
          }
        ),
        removeEventListener,
      })),
    })

    const { unmount } = render(<TestComponent />)
    expect(screen.getByText('mobile')).toBeInTheDocument()

    act(() => {
      changeListener?.({ matches: false } as MediaQueryListEvent)
    })

    expect(screen.getByText('desktop')).toBeInTheDocument()

    unmount()
    expect(removeEventListener).toHaveBeenCalled()
  })
})
