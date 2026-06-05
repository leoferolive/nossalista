import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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

  // ---------------------------------------------------------------------------
  // Render condicional / props opcionais
  // ---------------------------------------------------------------------------

  const baseProps = () => ({
    isOpen: true,
    listName: 'Lista Teste',
    onClose: mockOnClose,
    onGenerateLink: mockOnGenerateLink,
    onSearchUsers: mockOnSearchUsers,
    onInviteByUsername: mockOnInviteByUsername,
  })

  it('não renderiza nada quando isOpen=false', () => {
    const { container } = render(<InviteModal {...baseProps()} isOpen={false} />)
    expect(container).toBeEmptyDOMElement()
    expect(screen.queryByText('Convidar Pessoas')).not.toBeInTheDocument()
  })

  it('exibe a dica de "Digite ao menos 2 caracteres" quando o campo está vazio', () => {
    render(<InviteModal {...baseProps()} />)
    expect(screen.getByText(/Digite ao menos 2 caracteres para buscar/i)).toBeInTheDocument()
  })

  it('não dispara busca com menos de 2 caracteres digitados', async () => {
    const user = userEvent.setup({ delay: null })
    render(<InviteModal {...baseProps()} />)

    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'l')

    // Espera além do debounce (350ms) para garantir que não houve chamada
    await new Promise((r) => setTimeout(r, 450))
    expect(mockOnSearchUsers).not.toHaveBeenCalled()
    // A dica de 2 caracteres some quando há texto, então o helper padrão fica
    expect(screen.queryByText(/Digite ao menos 2 caracteres para buscar/i)).not.toBeInTheDocument()
  })

  it('exibe estado "Buscando..." enquanto a busca está em andamento', async () => {
    const user = userEvent.setup({ delay: null })
    let resolveSearch: (v: any) => void = () => {}
    mockOnSearchUsers.mockImplementationOnce(
      () => new Promise((resolve) => (resolveSearch = resolve))
    )

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    expect(await screen.findByText('Buscando...')).toBeInTheDocument()

    resolveSearch([{ username: 'leo', name: 'Leo Oliveira', avatarUrl: null }])
    await waitFor(() => expect(screen.queryByText('Buscando...')).not.toBeInTheDocument())
  })

  it('exibe mensagem de erro quando a busca de usuários falha', async () => {
    const user = userEvent.setup({ delay: null })
    mockOnSearchUsers.mockRejectedValueOnce(new Error('Falha ao buscar'))

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    expect(await screen.findByRole('alert')).toHaveTextContent('Falha ao buscar')
  })

  it('usa mensagem padrão quando o erro de busca não é uma Error', async () => {
    const user = userEvent.setup({ delay: null })
    mockOnSearchUsers.mockRejectedValueOnce('boom')

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    expect(await screen.findByText('Erro ao buscar usuarios')).toBeInTheDocument()
  })

  it('renderiza avatar do usuário quando avatarUrl está presente', async () => {
    const user = userEvent.setup({ delay: null })
    mockOnSearchUsers.mockResolvedValueOnce([
      { username: 'leo', name: 'Leo Oliveira', avatarUrl: 'https://example.com/avatar.png' },
    ])

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    const avatar = await screen.findByAltText('leo')
    expect(avatar).toHaveAttribute('src', 'https://example.com/avatar.png')
  })

  it('renderiza iniciais quando avatarUrl é null', async () => {
    const user = userEvent.setup({ delay: null })

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')

    // Sem avatar: deve exibir as iniciais (LE) no fallback
    expect(await screen.findByText('LE')).toBeInTheDocument()
  })

  it('não chama onInviteByUsername quando nenhum usuário foi selecionado', async () => {
    const user = userEvent.setup({ delay: null })
    render(<InviteModal {...baseProps()} />)

    const inviteBtn = screen.getByRole('button', { name: 'Convidar por username' })
    // Botão fica desabilitado sem usuário selecionado
    expect(inviteBtn).toBeDisabled()
    await user.click(inviteBtn)
    expect(mockOnInviteByUsername).not.toHaveBeenCalled()
  })

  it('usa mensagem padrão quando o erro do convite não é uma Error', async () => {
    const user = userEvent.setup({ delay: null })
    mockOnInviteByUsername.mockRejectedValueOnce({ status: 500 })

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')
    await user.click(await screen.findByRole('button', { name: /leo/i }))
    await user.click(screen.getByRole('button', { name: 'Convidar por username' }))

    await waitFor(() => {
      expect(screen.getByText('Erro ao convidar usuario')).toBeInTheDocument()
      expect(mockShowToast).toHaveBeenCalledWith('Erro ao convidar usuario', 'error')
    })
  })

  it('chama onInviteSuccess quando o convite é bem-sucedido', async () => {
    const user = userEvent.setup({ delay: null })
    const mockOnInviteSuccess = vi.fn()

    render(<InviteModal {...baseProps()} onInviteSuccess={mockOnInviteSuccess} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')
    await user.click(await screen.findByRole('button', { name: /leo/i }))
    await user.click(screen.getByRole('button', { name: 'Convidar por username' }))

    await waitFor(() => expect(mockOnInviteSuccess).toHaveBeenCalledWith('leo'))
  })

  it('mostra "Convidando..." enquanto o convite está em andamento', async () => {
    const user = userEvent.setup({ delay: null })
    let resolveInvite: (v: any) => void = () => {}
    mockOnInviteByUsername.mockImplementationOnce(
      () => new Promise((resolve) => (resolveInvite = resolve))
    )

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')
    await user.click(await screen.findByRole('button', { name: /leo/i }))
    await user.click(screen.getByRole('button', { name: 'Convidar por username' }))

    expect(await screen.findByRole('button', { name: 'Convidando...' })).toBeInTheDocument()
    resolveInvite({ invitedUsername: 'leo', message: 'ok' })
  })

  it('não dispara nova busca quando a query é igual ao usuário selecionado', async () => {
    const user = userEvent.setup({ delay: null })
    render(<InviteModal {...baseProps()} />)

    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')
    await user.click(await screen.findByRole('button', { name: /leo/i }))

    // Após selecionar, a query passa a ser igual ao username selecionado
    mockOnSearchUsers.mockClear()
    await new Promise((r) => setTimeout(r, 450))
    expect(mockOnSearchUsers).not.toHaveBeenCalled()
  })

  // ---------------------------------------------------------------------------
  // Convite por link (geração / cópia)
  // ---------------------------------------------------------------------------

  it('gera link e exibe o link ativo', async () => {
    const user = userEvent.setup({ delay: null })
    mockOnGenerateLink.mockResolvedValueOnce({
      inviteCode: 'abc',
      inviteLink: 'https://nossalista.app/join/abc',
      expiresAt: '2026-06-10T00:00:00Z',
    })

    render(<InviteModal {...baseProps()} />)
    await user.click(screen.getByRole('button', { name: 'Gerar Link' }))

    await waitFor(() =>
      expect(screen.getByText('https://nossalista.app/join/abc')).toBeInTheDocument()
    )
    expect(screen.getByText('Link ativo')).toBeInTheDocument()
    // Botão muda para "Copiar Link" após gerar
    expect(screen.getByRole('button', { name: 'Copiar Link' })).toBeInTheDocument()
  })

  it('exibe erro quando a geração de link falha (Error)', async () => {
    const user = userEvent.setup({ delay: null })
    mockOnGenerateLink.mockRejectedValueOnce(new Error('Servidor indisponível'))

    render(<InviteModal {...baseProps()} />)
    await user.click(screen.getByRole('button', { name: 'Gerar Link' }))

    expect(await screen.findByText('Servidor indisponível')).toBeInTheDocument()
  })

  it('usa mensagem padrão quando o erro de geração de link não é Error', async () => {
    const user = userEvent.setup({ delay: null })
    mockOnGenerateLink.mockRejectedValueOnce('falhou')

    render(<InviteModal {...baseProps()} />)
    await user.click(screen.getByRole('button', { name: 'Gerar Link' }))

    expect(await screen.findByText('Erro ao gerar link')).toBeInTheDocument()
  })

  it('mostra "Gerando..." enquanto gera o link', async () => {
    const user = userEvent.setup({ delay: null })
    let resolveGen: (v: any) => void = () => {}
    mockOnGenerateLink.mockImplementationOnce(
      () => new Promise((resolve) => (resolveGen = resolve))
    )

    render(<InviteModal {...baseProps()} />)
    await user.click(screen.getByRole('button', { name: 'Gerar Link' }))

    expect(await screen.findByRole('button', { name: 'Gerando...' })).toBeInTheDocument()
    resolveGen({ inviteCode: 'x', inviteLink: 'https://x', expiresAt: 'z' })
  })

  it('copia o link via navigator.clipboard e mostra estado "Copiado!"', async () => {
    const user = userEvent.setup({ delay: null })
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
      writable: true,
    })
    mockOnGenerateLink.mockResolvedValueOnce({
      inviteCode: 'abc',
      inviteLink: 'https://nossalista.app/join/abc',
      expiresAt: '2026-06-10T00:00:00Z',
    })

    render(<InviteModal {...baseProps()} />)
    await user.click(screen.getByRole('button', { name: 'Gerar Link' }))
    await user.click(await screen.findByRole('button', { name: 'Copiar Link' }))

    await waitFor(() => {
      expect(writeText).toHaveBeenCalledWith('https://nossalista.app/join/abc')
      expect(mockShowToast).toHaveBeenCalledWith('Link copiado!', 'success')
    })
    expect(screen.getByRole('button', { name: 'Copiado!' })).toBeDisabled()
  })

  it('copia o link via fallback execCommand quando clipboard não está disponível', async () => {
    const user = userEvent.setup({ delay: null })
    Object.defineProperty(navigator, 'clipboard', {
      value: undefined,
      configurable: true,
      writable: true,
    })
    const execCommand = vi.fn().mockReturnValue(true)
    // document.execCommand não existe no jsdom por padrão
    ;(document as any).execCommand = execCommand

    mockOnGenerateLink.mockResolvedValueOnce({
      inviteCode: 'abc',
      inviteLink: 'https://nossalista.app/join/abc',
      expiresAt: '2026-06-10T00:00:00Z',
    })

    render(<InviteModal {...baseProps()} />)
    await user.click(screen.getByRole('button', { name: 'Gerar Link' }))
    await user.click(await screen.findByRole('button', { name: 'Copiar Link' }))

    await waitFor(() => {
      expect(execCommand).toHaveBeenCalledWith('copy')
      expect(mockShowToast).toHaveBeenCalledWith('Link copiado!', 'success')
    })
  })

  it('exibe erro quando a cópia do link falha', async () => {
    const user = userEvent.setup({ delay: null })
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
      configurable: true,
      writable: true,
    })
    mockOnGenerateLink.mockResolvedValueOnce({
      inviteCode: 'abc',
      inviteLink: 'https://nossalista.app/join/abc',
      expiresAt: '2026-06-10T00:00:00Z',
    })

    render(<InviteModal {...baseProps()} />)
    await user.click(screen.getByRole('button', { name: 'Gerar Link' }))
    await user.click(await screen.findByRole('button', { name: 'Copiar Link' }))

    await waitFor(() => {
      expect(screen.getByText('Nao foi possivel copiar o link')).toBeInTheDocument()
      expect(mockShowToast).toHaveBeenCalledWith('Nao foi possivel copiar o link', 'error')
    })
  })

  it('limpa o estado interno ao fechar via botão de fechar do modal', async () => {
    const user = userEvent.setup({ delay: null })
    render(<InviteModal {...baseProps()} />)

    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')
    await user.click(await screen.findByRole('button', { name: /leo/i }))

    // Fecha pelo botão de fechar do ModalShell
    await user.click(screen.getByRole('button', { name: /fechar/i }))
    expect(mockOnClose).toHaveBeenCalled()
  })

  it('descarta resultado de busca obsoleto (sucesso) quando o modal fecha durante o request', async () => {
    const user = userEvent.setup({ delay: null })
    let resolveSearch: (v: any) => void = () => {}
    mockOnSearchUsers.mockImplementationOnce(
      () => new Promise((resolve) => (resolveSearch = resolve))
    )

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')
    await screen.findByText('Buscando...')

    // Fecha o modal: handleClose incrementa searchRequestRef, invalidando o request em voo.
    await user.click(screen.getByRole('button', { name: /fechar/i }))

    // Resolve a busca obsoleta: o guard requestId !== searchRequestRef.current ignora o resultado.
    resolveSearch([{ username: 'leo', name: 'Leo Oliveira', avatarUrl: null }])
    await new Promise((r) => setTimeout(r, 0))

    // Nenhum resultado deve ter sido aplicado.
    expect(screen.queryByText('Leo Oliveira')).not.toBeInTheDocument()
  })

  it('descarta erro de busca obsoleto quando o modal fecha durante o request', async () => {
    const user = userEvent.setup({ delay: null })
    let rejectSearch: (e: any) => void = () => {}
    mockOnSearchUsers.mockImplementationOnce(
      () => new Promise((_resolve, reject) => (rejectSearch = reject))
    )

    render(<InviteModal {...baseProps()} />)
    await user.type(screen.getByPlaceholderText(/Buscar usuario/i), 'leo')
    await screen.findByText('Buscando...')

    // Fecha durante o request, invalidando o requestId.
    await user.click(screen.getByRole('button', { name: /fechar/i }))

    rejectSearch(new Error('Falha tardia'))
    await new Promise((r) => setTimeout(r, 0))

    // Erro obsoleto não deve ser exibido.
    expect(screen.queryByText('Falha tardia')).not.toBeInTheDocument()
  })

  it('reseta o estado "Copiado!" para "Copiar Link" após o timeout de 2s', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
      writable: true,
    })
    mockOnGenerateLink.mockResolvedValueOnce({
      inviteCode: 'abc',
      inviteLink: 'https://nossalista.app/join/abc',
      expiresAt: '2026-06-10T00:00:00Z',
    })

    render(<InviteModal {...baseProps()} />)
    fireEvent.click(screen.getByRole('button', { name: 'Gerar Link' }))

    const copyBtn = await screen.findByRole('button', { name: 'Copiar Link' })
    fireEvent.click(copyBtn)

    // Estado "Copiado!" ativo após a cópia
    expect(await screen.findByRole('button', { name: 'Copiado!' })).toBeInTheDocument()

    // O timer real de 2s reseta `copied`, voltando para "Copiar Link"
    expect(
      await screen.findByRole('button', { name: 'Copiar Link' }, { timeout: 3000 })
    ).toBeInTheDocument()
  })
})
