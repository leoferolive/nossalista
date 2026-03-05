import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { DeleteConfirmModal } from './DeleteConfirmModal'

describe('DeleteConfirmModal', () => {
  it('NÃO deve renderizar quando isOpen é false', () => {
    render(
      <DeleteConfirmModal isOpen={false} itemName="Arroz" onConfirm={vi.fn()} onCancel={vi.fn()} />
    )

    expect(screen.queryByTestId('delete-confirm-modal')).not.toBeInTheDocument()
  })

  it('deve renderizar quando isOpen é true', () => {
    render(
      <DeleteConfirmModal isOpen={true} itemName="Arroz" onConfirm={vi.fn()} onCancel={vi.fn()} />
    )

    expect(screen.getByTestId('delete-confirm-modal')).toBeInTheDocument()
  })

  it('deve renderizar mensagem com nome do item conforme AC4', () => {
    render(
      <DeleteConfirmModal
        isOpen={true}
        itemName="Arroz Integral"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    )

    // Verifica título
    expect(screen.getByText('Remover item?')).toBeInTheDocument()

    // Verifica mensagem com nome do item
    expect(screen.getByText(/Tem certeza que deseja remover/)).toBeInTheDocument()
    expect(screen.getByText(/'Arroz Integral'/)).toBeInTheDocument()
  })

  it('deve chamar onConfirm quando clicar em Confirmar', () => {
    const onConfirm = vi.fn()
    render(
      <DeleteConfirmModal
        isOpen={true}
        itemName="Feijão"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    )

    const confirmButton = screen.getByTestId('delete-confirm-button')
    fireEvent.click(confirmButton)

    expect(onConfirm).toHaveBeenCalledTimes(1)
  })

  it('deve chamar onCancel quando clicar em Cancelar', () => {
    const onCancel = vi.fn()
    render(
      <DeleteConfirmModal
        isOpen={true}
        itemName="Macarrão"
        onConfirm={vi.fn()}
        onCancel={onCancel}
      />
    )

    const cancelButton = screen.getByTestId('delete-cancel-button')
    fireEvent.click(cancelButton)

    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  it('deve chamar onCancel quando clicar no backdrop', () => {
    const onCancel = vi.fn()
    render(
      <DeleteConfirmModal isOpen={true} itemName="Café" onConfirm={vi.fn()} onCancel={onCancel} />
    )

    const backdrop = screen.getByTestId('delete-confirm-modal')
    fireEvent.click(backdrop)

    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  it('NÃO deve chamar onCancel quando clicar dentro do modal', () => {
    const onCancel = vi.fn()
    render(
      <DeleteConfirmModal isOpen={true} itemName="Açúcar" onConfirm={vi.fn()} onCancel={onCancel} />
    )

    // Clicar no conteúdo do modal (não no backdrop)
    const modalContent = screen.getByText('Remover item?')
    fireEvent.click(modalContent)

    // NÃO deve ter chamado onCancel
    expect(onCancel).not.toHaveBeenCalled()
  })

  it('deve fechar ao pressionar Escape', () => {
    const onCancel = vi.fn()
    render(
      <DeleteConfirmModal isOpen={true} itemName="Sal" onConfirm={vi.fn()} onCancel={onCancel} />
    )

    // Simular tecla Escape
    fireEvent.keyDown(document, { key: 'Escape' })

    expect(onCancel).toHaveBeenCalledTimes(1)
  })
})
