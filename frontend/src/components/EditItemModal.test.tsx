import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { EditItemModal } from './EditItemModal'
import { ListItem } from '../types/Item'

// Mock do hook useToast
const mockShowToast = vi.fn()
vi.mock('../contexts/ToastContext', () => ({
  useToast: () => ({
    showToast: mockShowToast,
    toasts: [],
    removeToast: vi.fn(),
  }),
}))

describe('EditItemModal', () => {
  const mockItem: ListItem = {
    id: 'item-1',
    name: 'Item Original',
    checked: false,
    quantity: 2,
    dueDate: '2026-02-25T10:00:00Z',
    url: 'https://example.com',
    position: 1,
    createdAt: '2026-02-20T10:00:00Z',
    updatedAt: '2026-02-20T10:00:00Z',
    createdBy: {
      id: 'user-1',
      username: 'testuser',
      name: 'Test User',
      avatarUrl: null,
    },
  }

  const mockOnClose = vi.fn()
  const mockOnSave = vi.fn().mockResolvedValue(undefined)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  const renderModal = (props = {}) => {
    return render(
      <EditItemModal
        item={mockItem}
        listType="compras"
        isOpen={true}
        onClose={mockOnClose}
        onSave={mockOnSave}
        {...props}
      />
    )
  }

  it('não deve renderizar quando isOpen é false', () => {
    renderModal({ isOpen: false })
    expect(screen.queryByText('Editar Item')).not.toBeInTheDocument()
  })

  it('deve renderizar campos básicos corretamente', () => {
    renderModal()
    expect(screen.getByText('Editar Item')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toHaveValue('Item Original')
  })

  it('deve renderizar campo de quantidade para listas de compras', () => {
    renderModal({ listType: 'compras' })
    expect(screen.getByLabelText('Quantidade')).toBeInTheDocument()
    expect(screen.getByLabelText('Quantidade')).toHaveValue(2)
  })

  it('deve renderizar campo de data para listas de tarefas', () => {
    renderModal({ listType: 'tarefas' })
    expect(screen.getByLabelText('Data de Prazo')).toBeInTheDocument()

    // Calcular o valor esperado baseado no fuso horário local (mesma lógica do componente)
    const date = new Date(mockItem.dueDate!)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const expectedValue = `${year}-${month}-${day}T${hours}:${minutes}`

    expect(screen.getByLabelText('Data de Prazo')).toHaveValue(expectedValue)
  })

  it('deve renderizar campo de URL para listas de wishlist', () => {
    renderModal({ listType: 'wishlist' })
    expect(screen.getByLabelText('URL/Link')).toBeInTheDocument()
    expect(screen.getByLabelText('URL/Link')).toHaveValue('https://example.com')
  })

  it('deve mostrar erro se o nome estiver vazio ao salvar', async () => {
    renderModal()
    const nameInput = screen.getByLabelText('Nome')
    fireEvent.change(nameInput, { target: { value: '  ' } })

    const saveButton = screen.getByText('Salvar')
    fireEvent.click(saveButton)

    expect(mockShowToast).toHaveBeenCalledWith('Nome do item é obrigatório', 'error')
    expect(mockOnSave).not.toHaveBeenCalled()
  })

  it('deve chamar onSave com dados atualizados para compras', async () => {
    renderModal({ listType: 'compras' })

    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Item Atualizado' } })
    fireEvent.change(screen.getByLabelText('Quantidade'), { target: { value: '5' } })

    fireEvent.click(screen.getByText('Salvar'))

    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalledWith('item-1', {
        name: 'Item Atualizado',
        quantity: 5,
      })
    })
    expect(mockOnClose).toHaveBeenCalled()
  })

  it('deve chamar onSave com dados atualizados para tarefas', async () => {
    renderModal({ listType: 'tarefas' })

    fireEvent.change(screen.getByLabelText('Data de Prazo'), {
      target: { value: '2026-12-31T23:59' },
    })

    fireEvent.click(screen.getByText('Salvar'))

    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalledWith(
        'item-1',
        expect.objectContaining({
          name: 'Item Original',
          dueDate: new Date('2026-12-31T23:59').toISOString(),
        })
      )
    })
  })

  it('deve fechar o modal ao clicar em Cancelar', () => {
    renderModal()
    fireEvent.click(screen.getByText('Cancelar'))
    expect(mockOnClose).toHaveBeenCalled()
  })

  it('deve desabilitar botões enquanto está salvando', async () => {
    // Mock onSave para demorar
    let resolveSave: () => void
    mockOnSave.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          resolveSave = resolve
        })
    )

    renderModal()
    fireEvent.click(screen.getByText('Salvar'))

    await waitFor(() => {
      expect(screen.getByText(/Salvando/i)).toBeInTheDocument()
      expect(screen.getByText(/Salvando/i)).toBeDisabled()
      expect(screen.getByText('Cancelar')).toBeDisabled()
    })

    // Cleanup: resolve the pending save
    await act(async () => {
      resolveSave!()
    })
  })

  it('deve chamar onSave ao clicar em Salvar', async () => {
    renderModal({ listType: 'compras' })

    fireEvent.click(screen.getByText('Salvar'))

    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalledTimes(1)
      expect(mockOnSave).toHaveBeenCalledWith('item-1', {
        name: 'Item Original',
        quantity: 2,
      })
    })
  })
})
