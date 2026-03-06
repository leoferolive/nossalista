import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import userEvent from '@testing-library/user-event'
import { TypeCard } from './TypeCard'

describe('TypeCard', () => {
  const mockProps = {
    emoji: '🛒',
    name: 'Compras',
    description: 'Lista de compras do mercado',
    isSelected: false,
    onClick: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('deve renderizar emoji, nome e descrição', () => {
    render(<TypeCard {...mockProps} />)

    expect(screen.getByText('🛒')).toBeInTheDocument()
    expect(screen.getByText('Compras')).toBeInTheDocument()
    expect(screen.getByText('Lista de compras do mercado')).toBeInTheDocument()
  })

  it('deve aplicar estilos selecionados quando isSelected=true', () => {
    const { container } = render(<TypeCard {...mockProps} isSelected={true} />)

    const card = container.firstChild as HTMLElement
    expect(card.className).toContain('border-teal-500')
    expect(card.className).toContain('bg-teal-50')
  })

  it('deve chamar onClick quando clicado', async () => {
    render(<TypeCard {...mockProps} />)

    const card = screen.getByRole('button')
    await userEvent.click(card)

    expect(mockProps.onClick).toHaveBeenCalledTimes(1)
  })

  it('deve ser acessível por teclado (Enter)', async () => {
    render(<TypeCard {...mockProps} />)

    const card = screen.getByRole('button')
    card.focus()
    await userEvent.keyboard('{Enter}')

    expect(mockProps.onClick).toHaveBeenCalledTimes(1)
  })

  it('deve ser acessível por teclado (Space)', async () => {
    render(<TypeCard {...mockProps} />)

    const card = screen.getByRole('button')
    card.focus()
    await userEvent.keyboard(' ')

    expect(mockProps.onClick).toHaveBeenCalledTimes(1)
  })

  it('deve ter aria-label descritivo', () => {
    render(<TypeCard {...mockProps} />)

    const card = screen.getByRole('button')
    expect(card).toHaveAttribute(
      'aria-label',
      'Selecionar tipo Compras: Lista de compras do mercado'
    )
  })

  it('deve ter min-h-[160px] min-w-[160px] para NFR-A4', () => {
    const { container } = render(<TypeCard {...mockProps} />)

    const card = container.firstChild as HTMLElement
    expect(card.className).toContain('min-h-[160px]')
    expect(card.className).toContain('min-w-[160px]')
  })
})
