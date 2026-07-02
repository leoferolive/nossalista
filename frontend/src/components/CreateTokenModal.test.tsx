import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { CreateTokenModal } from './CreateTokenModal'

describe('CreateTokenModal', () => {
  const mockOnClose = vi.fn()
  const mockOnCreate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('não renderiza nada quando isOpen é false', () => {
    const { container } = render(
      <CreateTokenModal isOpen={false} onClose={mockOnClose} onCreate={mockOnCreate} />
    )

    expect(container.firstChild).toBeNull()
  })

  it('renderiza o formulário com valores padrão', () => {
    render(<CreateTokenModal isOpen onClose={mockOnClose} onCreate={mockOnCreate} />)

    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
    expect(screen.getByLabelText('Escopo de acesso')).toHaveValue('READ')
    expect(screen.getByLabelText('Expiração')).toHaveValue('30')
  })

  it('mostra erro quando o nome está vazio ao submeter', async () => {
    render(<CreateTokenModal isOpen onClose={mockOnClose} onCreate={mockOnCreate} />)

    fireEvent.click(screen.getByText('Criar token'))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Dê um nome ao token')
    })
    expect(mockOnCreate).not.toHaveBeenCalled()
  })

  it('chama onCreate com os dados do formulário', async () => {
    mockOnCreate.mockResolvedValue(undefined)
    render(<CreateTokenModal isOpen onClose={mockOnClose} onCreate={mockOnCreate} />)

    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Claude Desktop' } })
    fireEvent.change(screen.getByLabelText('Escopo de acesso'), { target: { value: 'READ_WRITE' } })
    fireEvent.change(screen.getByLabelText('Expiração'), { target: { value: '90' } })
    fireEvent.click(screen.getByText('Criar token'))

    await waitFor(() => {
      expect(mockOnCreate).toHaveBeenCalledWith({
        name: 'Claude Desktop',
        scope: 'READ_WRITE',
        expiresInDays: 90,
      })
    })
  })

  it('envia expiresInDays undefined quando "Sem expiração" é selecionado', async () => {
    mockOnCreate.mockResolvedValue(undefined)
    render(<CreateTokenModal isOpen onClose={mockOnClose} onCreate={mockOnCreate} />)

    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Sem prazo' } })
    fireEvent.change(screen.getByLabelText('Expiração'), { target: { value: 'never' } })
    fireEvent.click(screen.getByText('Criar token'))

    await waitFor(() => {
      expect(mockOnCreate).toHaveBeenCalledWith({
        name: 'Sem prazo',
        scope: 'READ',
        expiresInDays: undefined,
      })
    })
  })

  it('mostra mensagem de erro quando onCreate falha', async () => {
    mockOnCreate.mockRejectedValue(new Error('Limite de tokens atingido'))
    render(<CreateTokenModal isOpen onClose={mockOnClose} onCreate={mockOnCreate} />)

    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Token' } })
    fireEvent.click(screen.getByText('Criar token'))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Limite de tokens atingido')
    })
  })

  it('chama onClose ao clicar em Cancelar', () => {
    render(<CreateTokenModal isOpen onClose={mockOnClose} onCreate={mockOnCreate} />)

    fireEvent.click(screen.getByText('Cancelar'))

    expect(mockOnClose).toHaveBeenCalledTimes(1)
  })

  it('desabilita os campos e o botão durante isCreating', () => {
    render(<CreateTokenModal isOpen onClose={mockOnClose} onCreate={mockOnCreate} isCreating />)

    expect(screen.getByLabelText('Nome')).toBeDisabled()
    expect(screen.getByText('Criando…')).toBeDisabled()
  })
})
