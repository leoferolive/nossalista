import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Connections } from './Connections'
import { ThemeProvider } from '../contexts/ThemeContext'
import { ToastProvider } from '../contexts/ToastContext'
import { tokensApi } from '../api/tokensApi'
import type { PersonalAccessToken, PersonalAccessTokenCreated } from '../api/tokensApi'

vi.mock('../api/tokensApi', () => ({
  tokensApi: {
    list: vi.fn(),
    create: vi.fn(),
    revoke: vi.fn(),
  },
}))

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { displayName: 'Leo', username: 'leo' },
    logout: vi.fn(),
  }),
}))

vi.mock('../contexts/OnboardingContext', () => ({
  useOnboarding: () => ({
    startReplay: vi.fn(),
  }),
}))

function renderConnections() {
  return render(
    <ThemeProvider>
      <ToastProvider>
        <MemoryRouter initialEntries={['/connections']}>
          <Connections />
        </MemoryRouter>
      </ToastProvider>
    </ThemeProvider>
  )
}

const existingToken: PersonalAccessToken = {
  id: 'token-1',
  name: 'Claude Desktop',
  prefix: 'nlmcp_abc123',
  scope: 'READ',
  expiresAt: null,
  lastUsedAt: null,
  createdAt: '2026-01-01T00:00:00.000Z',
}

describe('Connections', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    ;(tokensApi.list as any).mockResolvedValue([existingToken])
  })

  afterEach(() => {
    cleanup()
  })

  it('carrega e exibe os tokens do usuário', async () => {
    renderConnections()

    await waitFor(() => {
      expect(screen.getByText('Claude Desktop')).toBeInTheDocument()
    })
    expect(screen.getByText('Somente leitura')).toBeInTheDocument()
  })

  it('exibe estado vazio quando não há tokens', async () => {
    ;(tokensApi.list as any).mockResolvedValue([])
    renderConnections()

    await waitFor(() => {
      expect(screen.getByText(/Nenhum token criado ainda/)).toBeInTheDocument()
    })
  })

  it('exibe erro quando falha ao carregar tokens', async () => {
    ;(tokensApi.list as any).mockRejectedValue(new Error('Falha de rede'))
    renderConnections()

    await waitFor(() => {
      expect(screen.getByText('Erro ao carregar tokens')).toBeInTheDocument()
    })
  })

  it('cria um novo token e exibe o modal de token criado', async () => {
    const created: PersonalAccessTokenCreated = {
      id: 'token-2',
      name: 'Novo Token',
      token: 'nlmcp_deadbeef',
      prefix: 'nlmcp_deadbe',
      scope: 'READ',
      expiresAt: null,
      lastUsedAt: null,
      createdAt: '2026-01-02T00:00:00.000Z',
    }
    ;(tokensApi.create as any).mockResolvedValue(created)

    renderConnections()

    await waitFor(() => expect(screen.getByText('Claude Desktop')).toBeInTheDocument())

    fireEvent.click(screen.getByText('Criar token'))
    const dialog = screen.getByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText('Nome'), { target: { value: 'Novo Token' } })
    fireEvent.click(within(dialog).getByText('Criar token'))

    await waitFor(() => {
      expect(tokensApi.create).toHaveBeenCalledWith({
        name: 'Novo Token',
        scope: 'READ',
        expiresInDays: 30,
      })
    })
    await waitFor(() => {
      expect(screen.getByTestId('token-value')).toHaveTextContent('nlmcp_deadbeef')
    })
  })

  it('revoga um token após confirmação', async () => {
    ;(tokensApi.revoke as any).mockResolvedValue(undefined)
    renderConnections()

    await waitFor(() => expect(screen.getByText('Claude Desktop')).toBeInTheDocument())

    fireEvent.click(screen.getByLabelText('Revogar token Claude Desktop'))
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByText('Revogar'))

    await waitFor(() => {
      expect(tokensApi.revoke).toHaveBeenCalledWith('token-1')
    })
    await waitFor(() => {
      expect(screen.queryByText('Claude Desktop')).not.toBeInTheDocument()
    })
  })
})
