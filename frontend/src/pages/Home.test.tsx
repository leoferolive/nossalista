import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Home } from './Home'
import { ThemeProvider } from '../contexts/ThemeContext'
import { ToastProvider } from '../contexts/ToastContext'

const mockFetchLists = vi.fn()
const mockCreateList = vi.fn()
const mockClearError = vi.fn()
const mockAcknowledgeCreateListModalRequest = vi.fn()
const mockOnListCreated = vi.fn()

const baseLists = [
  {
    id: 'list-1',
    name: 'Mercado da semana',
    type: { id: 1, name: 'Compras', slug: 'compras' },
    owner: { id: 'u1', username: 'leo', name: 'Leo', avatarUrl: null },
    inviteCode: 'INVITE1',
    isOwner: true,
    itemsCount: 3,
    createdAt: '2026-03-01T00:00:00.000Z',
    updatedAt: '2026-03-01T00:00:00.000Z',
  },
  {
    id: 'list-2',
    name: 'Projeto app',
    type: { id: 2, name: 'Tarefas', slug: 'tarefas' },
    owner: { id: 'u2', username: 'ana', name: 'Ana', avatarUrl: null },
    inviteCode: 'INVITE2',
    isOwner: false,
    itemsCount: 6,
    createdAt: '2026-03-01T00:00:00.000Z',
    updatedAt: '2026-03-01T00:00:00.000Z',
  },
]

let listsFixture = baseLists

vi.mock('../hooks/useLists', () => ({
  useLists: () => ({
    createList: mockCreateList,
    lists: listsFixture,
    fetchLists: mockFetchLists,
    loading: false,
    error: null,
    clearError: mockClearError,
  }),
}))

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { displayName: 'Leo', username: 'leo' },
  }),
}))

vi.mock('../contexts/OnboardingContext', () => ({
  useOnboarding: () => ({
    isRunning: false,
    requestOpenCreateListModal: false,
    acknowledgeCreateListModalRequest: mockAcknowledgeCreateListModalRequest,
    onListCreated: mockOnListCreated,
  }),
}))

describe('Home search', () => {
  beforeEach(() => {
    listsFixture = baseLists
    mockFetchLists.mockReset()
    mockCreateList.mockReset()
    mockClearError.mockReset()
    mockAcknowledgeCreateListModalRequest.mockReset()
    mockOnListCreated.mockReset()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const renderHome = () =>
    render(
      <ThemeProvider>
        <ToastProvider>
          <MemoryRouter>
            <Home />
          </MemoryRouter>
        </ToastProvider>
      </ThemeProvider>
    )

  it('filtra listas por nome', () => {
    renderHome()

    expect(screen.getByText('Mercado da semana')).toBeInTheDocument()
    expect(screen.getByText('Projeto app')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Buscar listas'), {
      target: { value: 'mercado' },
    })

    act(() => {
      vi.advanceTimersByTime(260)
    })

    expect(screen.getByText('Mercado da semana')).toBeInTheDocument()
    expect(screen.queryByText('Projeto app')).not.toBeInTheDocument()
  })

  it('filtra listas por tipo', () => {
    renderHome()

    fireEvent.change(screen.getByLabelText('Buscar listas'), {
      target: { value: 'tarefas' },
    })

    act(() => {
      vi.advanceTimersByTime(260)
    })

    expect(screen.getByText('Projeto app')).toBeInTheDocument()
    expect(screen.queryByText('Mercado da semana')).not.toBeInTheDocument()
  })

  it('mostra estado vazio da busca e restaura ao limpar filtro', () => {
    renderHome()

    fireEvent.change(screen.getByLabelText('Buscar listas'), {
      target: { value: 'inexistente' },
    })

    act(() => {
      vi.advanceTimersByTime(260)
    })

    expect(screen.getByText('Nenhuma lista encontrada')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Buscar listas'), {
      target: { value: '' },
    })

    act(() => {
      vi.advanceTimersByTime(260)
    })

    expect(screen.getByText('Mercado da semana')).toBeInTheDocument()
    expect(screen.getByText('Projeto app')).toBeInTheDocument()
  })
})
