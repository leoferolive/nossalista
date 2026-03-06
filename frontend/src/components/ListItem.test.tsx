import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ListItemComponent } from './ListItem'
import { ListItem } from '../types/Item'

describe('ListItemComponent', () => {
  const mockItem: ListItem = {
    id: 'test-item-id',
    name: 'Test Item',
    checked: false,
    quantity: 2,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: {
      id: 'user-id',
      username: 'testuser',
      name: 'Test User',
      avatarUrl: null,
    },
    createdAt: '2026-02-13T10:00:00Z',
    updatedAt: '2026-02-13T10:00:00Z',
  }

  it('deve chamar onToggle ao clicar no checkbox', () => {
    const onToggle = vi.fn()
    const onEdit = vi.fn()

    render(<ListItemComponent item={mockItem} onToggle={onToggle} onEdit={onEdit} />)

    // Clicar no checkbox (botão com role checkbox)
    const checkbox = screen.getByRole('checkbox')
    fireEvent.click(checkbox)

    // Verificar se onToggle foi chamado com o id correto
    expect(onToggle).toHaveBeenCalledWith('test-item-id')
    expect(onToggle).toHaveBeenCalledTimes(1)
  })

  it('deve chamar onEdit (não onToggle) ao clicar no texto/nome do item', () => {
    const onToggle = vi.fn()
    const onEdit = vi.fn()

    render(<ListItemComponent item={mockItem} onToggle={onToggle} onEdit={onEdit} />)

    // Clicar no nome do item (container do texto)
    const itemName = screen.getByText('Test Item')
    fireEvent.click(itemName)

    // Verificar que onEdit foi chamado e onToggle não foi
    expect(onEdit).toHaveBeenCalledWith(mockItem)
    expect(onEdit).toHaveBeenCalledTimes(1)
    expect(onToggle).not.toHaveBeenCalled()
  })

  it('deve aplicar estilo de riscado quando item está checked', () => {
    const onToggle = vi.fn()
    const onEdit = vi.fn()

    const checkedItem = { ...mockItem, checked: true }

    render(<ListItemComponent item={checkedItem} onToggle={onToggle} onEdit={onEdit} />)

    // Verificar se o checkbox está marcado
    const checkbox = screen.getByRole('checkbox')
    expect(checkbox).toHaveAttribute('aria-checked', 'true')

    // Verificar se o texto tem classe de riscado
    const itemName = screen.getByText('Test Item')
    expect(itemName).toHaveClass('line-through')
  })

  it('deve aplicar classe ws-item-checked quando isWsCheckedHighlight=true', () => {
    render(
      <ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} isWsCheckedHighlight />
    )

    const itemContainer = screen.getByTestId(`list-item-${mockItem.id}`)
    expect(itemContainer).toHaveClass('ws-item-checked')
  })

  it('deve aplicar classe animate-pop no checkbox quando isWsChecked=true', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} isWsChecked />)

    const checkbox = screen.getByRole('checkbox')
    expect(checkbox).toHaveClass('animate-pop')
  })

  it('não deve aplicar classes de websocket quando isWsChecked=false', () => {
    render(<ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />)

    const itemContainer = screen.getByTestId(`list-item-${mockItem.id}`)
    const checkbox = screen.getByRole('checkbox')
    expect(itemContainer).not.toHaveClass('ws-item-checked')
    expect(checkbox).not.toHaveClass('animate-pop')
  })
})
