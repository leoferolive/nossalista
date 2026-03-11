import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider, useTheme } from './ThemeContext'

function ThemeProbe() {
  const { theme, toggleTheme, setTheme } = useTheme()
  return (
    <div>
      <span data-testid="theme">{theme}</span>
      <button onClick={toggleTheme}>toggle</button>
      <button onClick={() => setTheme('dark')}>set-dark</button>
      <button onClick={() => setTheme('light')}>set-light</button>
    </div>
  )
}

describe('ThemeContext', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockReturnValue({
        matches: false,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      })
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('inicia com tema light quando nao ha preferencia salva', () => {
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    expect(screen.getByTestId('theme').textContent).toBe('light')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })

  it('inicia com tema dark quando localStorage tem nl-theme=dark', () => {
    localStorage.setItem('nl-theme', 'dark')
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    expect(screen.getByTestId('theme').textContent).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('inicia com tema dark quando prefers-color-scheme e dark', () => {
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockReturnValue({
        matches: true,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      })
    )
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    expect(screen.getByTestId('theme').textContent).toBe('dark')
  })

  it('toggleTheme alterna entre light e dark', async () => {
    const user = userEvent.setup()
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    expect(screen.getByTestId('theme').textContent).toBe('light')
    await user.click(screen.getByText('toggle'))
    expect(screen.getByTestId('theme').textContent).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    await user.click(screen.getByText('toggle'))
    expect(screen.getByTestId('theme').textContent).toBe('light')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })

  it('setTheme persiste no localStorage', async () => {
    const user = userEvent.setup()
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    await user.click(screen.getByText('set-dark'))
    expect(localStorage.getItem('nl-theme')).toBe('dark')
    await user.click(screen.getByText('set-light'))
    expect(localStorage.getItem('nl-theme')).toBe('light')
  })

  it('aplica data-theme=dark no documentElement ao mudar para dark', async () => {
    const user = userEvent.setup()
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    await user.click(screen.getByText('set-dark'))
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('aplica data-theme=light ao mudar para light', async () => {
    localStorage.setItem('nl-theme', 'dark')
    const user = userEvent.setup()
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    await user.click(screen.getByText('set-light'))
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })

  it('sincroniza com mudanca no prefers-color-scheme quando nao ha preferencia salva', () => {
    let changeHandler: ((e: MediaQueryListEvent) => void) | undefined
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockReturnValue({
        matches: false,
        addEventListener: (_: string, fn: (e: MediaQueryListEvent) => void) => {
          changeHandler = fn
        },
        removeEventListener: vi.fn(),
      })
    )
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    act(() => {
      changeHandler?.({ matches: true } as MediaQueryListEvent)
    })
    expect(screen.getByTestId('theme').textContent).toBe('dark')
  })

  it('nao sincroniza com prefers-color-scheme quando ha preferencia salva', () => {
    localStorage.setItem('nl-theme', 'light')
    let changeHandler: ((e: MediaQueryListEvent) => void) | undefined
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockReturnValue({
        matches: false,
        addEventListener: (_: string, fn: (e: MediaQueryListEvent) => void) => {
          changeHandler = fn
        },
        removeEventListener: vi.fn(),
      })
    )
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>
    )
    act(() => {
      changeHandler?.({ matches: true } as MediaQueryListEvent)
    })
    expect(screen.getByTestId('theme').textContent).toBe('light')
  })

  it('useTheme lanca erro fora do ThemeProvider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => render(<ThemeProbe />)).toThrow('useTheme must be used within ThemeProvider')
    spy.mockRestore()
  })
})
