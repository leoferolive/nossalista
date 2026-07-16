import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { JoinListPage } from './JoinListPage'
import { ThemeProvider } from '../contexts/ThemeContext'
import { listsApi } from '../api/listsApi'
import { ApiError } from '../types/ApiError'

const mockNavigate = vi.fn()
let mockIsAuthenticated = false

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: mockIsAuthenticated }),
}))

vi.mock('../api/listsApi', () => ({
  listsApi: {
    getListByInviteCode: vi.fn(),
    joinList: vi.fn(),
  },
}))

const baseJoinList = {
  id: 'list-1',
  name: 'Mercado da semana',
  typeSlug: 'compras',
  typeName: 'Compras',
  ownerUsername: 'leo',
  ownerName: 'Leo',
  ownerAvatarUrl: null,
  items: [
    { id: 'item-1', name: 'Arroz', checked: false, quantity: 2, dueDate: null, url: null, position: 0 },
    { id: 'item-2', name: 'Feijao', checked: true, quantity: null, dueDate: null, url: null, position: 1 },
  ],
  inviteCode: 'abc123',
  expiresAt: '2099-01-01T00:00:00.000Z',
  mode: 'READ_ONLY' as const,
}

function renderJoinListPage(inviteCode = 'abc123') {
  return render(
    <ThemeProvider>
      <MemoryRouter initialEntries={[`/join/${inviteCode}`]}>
        <Routes>
          <Route path="/join/:inviteCode" element={<JoinListPage />} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>
  )
}

describe('JoinListPage', () => {
  beforeEach(() => {
    mockNavigate.mockReset()
    mockIsAuthenticated = false
    vi.mocked(listsApi.getListByInviteCode).mockReset()
    vi.mocked(listsApi.joinList).mockReset()
    sessionStorage.clear()
    Object.defineProperty(window, 'location', {
      value: { ...window.location, href: '' },
      writable: true,
    })
  })

  it('mostra a lista em modo leitura quando o convite e valido', async () => {
    vi.mocked(listsApi.getListByInviteCode).mockResolvedValueOnce(baseJoinList)

    renderJoinListPage()

    expect(await screen.findByText('Mercado da semana')).toBeInTheDocument()
    expect(screen.getByText('Arroz')).toBeInTheDocument()
    expect(screen.getByText('Feijao')).toBeInTheDocument()
    expect(screen.getByText(/Compras • por @leo/)).toBeInTheDocument()
    expect(screen.getByText('Itens (2)')).toBeInTheDocument()
  })

  it('mostra estado vazio quando a lista nao tem itens', async () => {
    vi.mocked(listsApi.getListByInviteCode).mockResolvedValueOnce({ ...baseJoinList, items: [] })

    renderJoinListPage()

    expect(await screen.findByText('Esta lista ainda não tem itens.')).toBeInTheDocument()
  })

  it('mostra aviso quando o link expira em breve', async () => {
    const soon = new Date(Date.now() + 2 * 60 * 1000).toISOString()
    vi.mocked(listsApi.getListByInviteCode).mockResolvedValueOnce({
      ...baseJoinList,
      expiresAt: soon,
    })

    renderJoinListPage()

    expect(await screen.findByText(/Este link expira em breve!/)).toBeInTheDocument()
  })

  it('mostra tela de convite nao encontrado em erro 404', async () => {
    vi.mocked(listsApi.getListByInviteCode).mockRejectedValueOnce(new ApiError('Nao encontrado', 404))

    renderJoinListPage()

    expect(await screen.findByText('Convite não encontrado')).toBeInTheDocument()
  })

  it('mostra tela de link expirado em erro 410', async () => {
    vi.mocked(listsApi.getListByInviteCode).mockRejectedValueOnce(new ApiError('Expirado', 410))

    renderJoinListPage()

    expect(await screen.findByText('Link de convite expirado')).toBeInTheDocument()
  })

  it('mostra erro generico com a mensagem do servidor para outras falhas', async () => {
    vi.mocked(listsApi.getListByInviteCode).mockRejectedValueOnce(new Error('Falha de rede'))

    renderJoinListPage()

    expect(await screen.findByText('Erro ao carregar lista')).toBeInTheDocument()
    expect(screen.getByText('Falha de rede')).toBeInTheDocument()
  })

  it('salva o inviteCode e redireciona para login ao clicar em Entrar com Email', async () => {
    vi.mocked(listsApi.getListByInviteCode).mockResolvedValueOnce(baseJoinList)

    renderJoinListPage()

    await screen.findByText('Mercado da semana')

    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(sessionStorage.getItem('pendingInviteCode')).toBe('abc123')
    expect(mockNavigate).toHaveBeenCalledWith('/?auth=login')
  })

  it('salva o inviteCode e redireciona para o Google OAuth ao clicar em Entrar com Google', async () => {
    vi.mocked(listsApi.getListByInviteCode).mockResolvedValueOnce(baseJoinList)

    renderJoinListPage()

    await screen.findByText('Mercado da semana')

    fireEvent.click(screen.getByRole('button', { name: 'Entrar com Google' }))

    expect(sessionStorage.getItem('pendingInviteCode')).toBe('abc123')
    expect(window.location.href).toBe('/api/auth/google')
  })

  it('entra automaticamente na lista quando o usuario ja esta autenticado', async () => {
    mockIsAuthenticated = true
    vi.mocked(listsApi.getListByInviteCode).mockResolvedValueOnce(baseJoinList)
    vi.mocked(listsApi.joinList).mockResolvedValueOnce({
      id: 'list-1',
      name: 'Mercado da semana',
      typeSlug: 'compras',
      typeName: 'Compras',
      role: 'MEMBER',
      message: 'Bem-vindo à lista!',
      created: false,
      list: {
        id: 'list-1',
        name: 'Mercado da semana',
        type: { id: 1, name: 'Compras', slug: 'compras' },
        owner: { id: 'owner-1', username: 'leo', name: 'Leo', avatarUrl: null },
        inviteCode: 'abc123',
        isOwner: false,
        itemsCount: 2,
        createdAt: '2026-01-01T00:00:00.000Z',
        updatedAt: '2026-01-01T00:00:00.000Z',
      },
    })

    renderJoinListPage()

    await waitFor(() => expect(listsApi.joinList).toHaveBeenCalledWith('abc123'))
    await waitFor(() =>
      expect(mockNavigate).toHaveBeenCalledWith('/lists/list-1', {
        replace: true,
        state: { toastMessage: 'Bem-vindo à lista!', toastType: 'success' },
      })
    )
  })

  it('mostra erro quando o auto-join falha com convite expirado', async () => {
    mockIsAuthenticated = true
    vi.mocked(listsApi.getListByInviteCode).mockResolvedValueOnce(baseJoinList)
    vi.mocked(listsApi.joinList).mockRejectedValueOnce(new ApiError('Expirado', 410))

    renderJoinListPage()

    expect(await screen.findByText('Link de convite expirado')).toBeInTheDocument()
  })
})
