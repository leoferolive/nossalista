import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { TokenCreatedModal } from './TokenCreatedModal'
import type { PersonalAccessTokenCreated } from '../api/tokensApi'

const token: PersonalAccessTokenCreated = {
  id: '1',
  name: 'Claude Desktop',
  token: 'nlmcp_deadbeefdeadbeef',
  prefix: 'nlmcp_deadbe',
  scope: 'READ',
  expiresAt: null,
  lastUsedAt: null,
  createdAt: '2026-01-01T00:00:00.000Z',
}

describe('TokenCreatedModal', () => {
  const mockOnClose = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
  })

  it('exibe o valor em claro do token', () => {
    render(<TokenCreatedModal token={token} onClose={mockOnClose} />)

    expect(screen.getByTestId('token-value')).toHaveTextContent(token.token)
  })

  it('copia o token para a área de transferência ao clicar em copiar', async () => {
    render(<TokenCreatedModal token={token} onClose={mockOnClose} />)

    fireEvent.click(screen.getByText('Copiar token'))

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(token.token)
    })
    expect(screen.getByText('Copiado!')).toBeInTheDocument()
  })

  it('chama onClose ao clicar em fechar', () => {
    render(<TokenCreatedModal token={token} onClose={mockOnClose} />)

    fireEvent.click(screen.getByText('Já copiei, fechar'))

    expect(mockOnClose).toHaveBeenCalledTimes(1)
  })

  it('avisa que o token não será mostrado novamente', () => {
    render(<TokenCreatedModal token={token} onClose={mockOnClose} />)

    expect(screen.getByRole('alert')).toHaveTextContent(/único momento/i)
  })

  it('usa o fallback de textarea quando navigator.clipboard não está disponível', async () => {
    Object.assign(navigator, { clipboard: undefined })
    document.execCommand = vi.fn().mockReturnValue(true)

    render(<TokenCreatedModal token={token} onClose={mockOnClose} />)
    fireEvent.click(screen.getByText('Copiar token'))

    await waitFor(() => {
      expect(document.execCommand).toHaveBeenCalledWith('copy')
    })
    expect(screen.getByText('Copiado!')).toBeInTheDocument()
  })

  it('não quebra quando a cópia falha', async () => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
    })

    render(<TokenCreatedModal token={token} onClose={mockOnClose} />)
    fireEvent.click(screen.getByText('Copiar token'))

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalled()
    })
    expect(screen.queryByText('Copiado!')).not.toBeInTheDocument()
  })
})
