import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { InviteModal } from './InviteModal'
import { useToast } from '../contexts/ToastContext'

vi.mock('../contexts/ToastContext')

describe('InviteModal - convite por username', () => {
  const mockShowToast = vi.fn()
  const mockOnClose = vi.fn()
  const mockOnGenerateLink = vi.fn()
  const mockOnSearchUsers = vi.fn()
  const mockOnInviteByUsername = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    ;(useToast as any).mockReturnValue({
      showToast: mockShowToast,
      toasts: [],
      removeToast: vi.fn(),
    })

    mockOnSearchUsers.mockResolvedValue([
      { username: 'leo', name: 'Leo Oliveira', avatarUrl: null },
    ])
    mockOnInviteByUsername.mockResolvedValue({
      invitedUsername: 'leo',
      message: 'leo adicionado!',
    })
  })

  it('autocomplete renderiza resultados corretamente', async () => {
    const user = userEvent.setup()

    render(
      <InviteModal
        isOpen={true}
        listName="Lista Teste"
        onClose={mockOnClose}
        onGenerateLink={mockOnGenerateLink}
        onSearchUsers={mockOnSearchUsers}
        onInviteByUsername={mockOnInviteByUsername}
      />
    )

    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    await waitFor(() => {
      expect(mockOnSearchUsers).toHaveBeenCalledWith('leo')
      expect(screen.getByText('leo')).toBeInTheDocument()
      expect(screen.getByText('Leo Oliveira')).toBeInTheDocument()
    })
  })

  it('clique em convidar envia request correto', async () => {
    const user = userEvent.setup()

    render(
      <InviteModal
        isOpen={true}
        listName="Lista Teste"
        onClose={mockOnClose}
        onGenerateLink={mockOnGenerateLink}
        onSearchUsers={mockOnSearchUsers}
        onInviteByUsername={mockOnInviteByUsername}
      />
    )

    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    await user.click(await screen.findByRole('button', { name: /leo/i }))
    await user.click(screen.getByRole('button', { name: 'Convidar por username' }))

    await waitFor(() => {
      expect(mockOnInviteByUsername).toHaveBeenCalledWith('leo')
      expect(mockShowToast).toHaveBeenCalledWith('leo convidado!', 'success')
      expect(mockOnClose).toHaveBeenCalled()
    })
  })

  it('erros 404/409 exibem feedback adequado', async () => {
    const user = userEvent.setup()
    mockOnInviteByUsername.mockRejectedValueOnce(new Error('Usuário já é membro'))

    render(
      <InviteModal
        isOpen={true}
        listName="Lista Teste"
        onClose={mockOnClose}
        onGenerateLink={mockOnGenerateLink}
        onSearchUsers={mockOnSearchUsers}
        onInviteByUsername={mockOnInviteByUsername}
      />
    )

    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    await user.click(await screen.findByRole('button', { name: /leo/i }))
    await user.click(screen.getByRole('button', { name: 'Convidar por username' }))

    await waitFor(() => {
      expect(screen.getByText('Usuário já é membro')).toBeInTheDocument()
      expect(mockShowToast).toHaveBeenCalledWith('Usuário já é membro', 'error')
    })
  })
})
