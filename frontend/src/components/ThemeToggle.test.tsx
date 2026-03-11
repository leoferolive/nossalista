import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ThemeToggle } from './ThemeToggle'

const mockSetTheme = vi.fn()
const mockUseTheme = vi.fn()

vi.mock('../contexts/ThemeContext', () => ({
  useTheme: () => mockUseTheme(),
}))

describe('ThemeToggle', () => {
  beforeEach(() => {
    mockSetTheme.mockReset()
    mockUseTheme.mockReturnValue({
      theme: 'light',
      setTheme: mockSetTheme,
    })
  })

  it('marca o tema ativo com data-active', () => {
    render(<ThemeToggle />)

    expect(screen.getByRole('button', { name: /claro/i })).toHaveAttribute('data-active', 'true')
    expect(screen.getByRole('button', { name: /escuro/i })).toHaveAttribute('data-active', 'false')
  })

  it('troca o tema ao clicar nos botoes', () => {
    render(<ThemeToggle />)

    fireEvent.click(screen.getByRole('button', { name: /escuro/i }))
    fireEvent.click(screen.getByRole('button', { name: /claro/i }))

    expect(mockSetTheme).toHaveBeenNthCalledWith(1, 'dark')
    expect(mockSetTheme).toHaveBeenNthCalledWith(2, 'light')
  })
})
