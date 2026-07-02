import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { RevokeTokenModal } from './RevokeTokenModal'

describe('RevokeTokenModal', () => {
  const mockOnClose = vi.fn()
  const mockOnConfirm = vi.fn()
  const defaultProps = {
    isOpen: true,
    tokenName: 'Claude Desktop',
    onClose: mockOnClose,
    onConfirm: mockOnConfirm,
    isRevoking: false,
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('não renderiza nada quando isOpen é false', () => {
    const { container } = render(<RevokeTokenModal {...defaultProps} isOpen={false} />)

    expect(container.firstChild).toBeNull()
  })

  it('renderiza o nome do token na mensagem de confirmação', () => {
    render(<RevokeTokenModal {...defaultProps} />)

    expect(screen.getByText(/Claude Desktop/)).toBeInTheDocument()
  })

  it('chama onClose ao clicar em Cancelar sem chamar onConfirm', () => {
    render(<RevokeTokenModal {...defaultProps} />)

    fireEvent.click(screen.getByText('Cancelar'))

    expect(mockOnClose).toHaveBeenCalledTimes(1)
    expect(mockOnConfirm).not.toHaveBeenCalled()
  })

  it('chama onConfirm ao clicar em Revogar', async () => {
    mockOnConfirm.mockResolvedValue(undefined)
    render(<RevokeTokenModal {...defaultProps} />)

    fireEvent.click(screen.getByText('Revogar'))

    await waitFor(() => {
      expect(mockOnConfirm).toHaveBeenCalledTimes(1)
    })
  })

  it('desabilita botões durante isRevoking', () => {
    render(<RevokeTokenModal {...defaultProps} isRevoking />)

    expect(screen.getByText('Cancelar')).toBeDisabled()
    expect(screen.getByText(/Revogando/i)).toBeDisabled()
  })

  it('fecha ao pressionar ESC (se não isRevoking)', () => {
    render(<RevokeTokenModal {...defaultProps} />)

    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })

    expect(mockOnClose).toHaveBeenCalledTimes(1)
  })

  it('tem ARIA labels apropriados', () => {
    render(<RevokeTokenModal {...defaultProps} />)

    const modal = screen.getByRole('dialog')
    expect(modal).toHaveAttribute('aria-modal', 'true')
    expect(modal).toHaveAttribute('aria-labelledby', 'revoke-token-modal-title')
  })

  it('implementa focus trap com TAB entre Cancelar e Revogar', () => {
    render(<RevokeTokenModal {...defaultProps} />)

    const modal = screen.getByRole('dialog')
    const cancelButton = screen.getByText('Cancelar')
    const confirmButton = screen.getByText('Revogar')

    expect(cancelButton).toHaveFocus()

    fireEvent.keyDown(modal, { key: 'Tab' })
    expect(confirmButton).toHaveFocus()

    fireEvent.keyDown(modal, { key: 'Tab' })
    expect(cancelButton).toHaveFocus()
  })

  it('não fecha nem navega com TAB/ESC durante isRevoking', () => {
    render(<RevokeTokenModal {...defaultProps} isRevoking />)

    const modal = screen.getByRole('dialog')
    fireEvent.keyDown(modal, { key: 'Escape' })
    fireEvent.keyDown(modal, { key: 'Tab' })

    expect(mockOnClose).not.toHaveBeenCalled()
  })
})
