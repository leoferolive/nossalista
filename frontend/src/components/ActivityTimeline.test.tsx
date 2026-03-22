import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ActivityTimeline } from './ActivityTimeline'
import { ActivityLog } from '../types/ActivityLog'

// Mock do hook useToast
const mockShowToast = vi.fn()
vi.mock('../contexts/ToastContext', () => ({
  useToast: () => ({
    showToast: mockShowToast,
    toasts: [],
    removeToast: vi.fn(),
  }),
}))

describe('ActivityTimeline', () => {
  const mockActivities: ActivityLog[] = [
    {
      id: 'act-1',
      listId: 'list-123',
      userId: 'user-1',
      userName: 'Ana Silva',
      action: 'ITEM_ADDED',
      targetType: 'ITEM',
      targetId: 'item-1',
      targetName: 'Café',
      details: null,
      createdAt: new Date().toISOString(),
    },
    {
      id: 'act-2',
      listId: 'list-123',
      userId: 'user-2',
      userName: 'Bruno Costa',
      action: 'ITEM_CHECKED',
      targetType: 'ITEM',
      targetId: 'item-2',
      targetName: 'Leite',
      details: null,
      createdAt: new Date(Date.now() - 3600000).toISOString(), // 1h atrás
    },
    {
      id: 'act-3',
      listId: 'list-123',
      userId: 'user-1',
      userName: 'Ana Silva',
      action: 'MEMBER_JOINED',
      targetType: 'MEMBER',
      targetId: 'user-1',
      targetName: 'Usuário',
      details: { method: 'LINK' },
      createdAt: new Date(Date.now() - 86400000).toISOString(), // Ontem
    },
  ]

  const mockOnClose = vi.fn()
  const mockOnLoadMore = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  const renderTimeline = (props = {}) => {
    return render(
      <ActivityTimeline
        activities={mockActivities}
        isOpen={true}
        onClose={mockOnClose}
        onLoadMore={mockOnLoadMore}
        hasMore={true}
        loading={false}
        {...props}
      />
    )
  }

  it('não deve renderizar quando isOpen é false', () => {
    renderTimeline({ isOpen: false })
    expect(screen.queryByText('Atividades')).not.toBeInTheDocument()
  })

  it('deve renderizar a lista de atividades corretamente', () => {
    renderTimeline()
    expect(screen.getByText('Atividades')).toBeInTheDocument()
    expect(screen.getAllByText('Ana Silva').length).toBeGreaterThan(0)
    expect(screen.getByText('Bruno Costa')).toBeInTheDocument()
    expect(screen.getByText(/adicionou "Café"/i)).toBeInTheDocument()
    expect(screen.getByText(/marcou "Leite" como concluído/i)).toBeInTheDocument()
    expect(screen.getByText(/entrou via link de convite/i)).toBeInTheDocument()
  })

  it('deve mostrar mensagem de lista vazia', () => {
    renderTimeline({ activities: [] })
    expect(screen.getByText('Nenhuma atividade ainda')).toBeInTheDocument()
  })

  it('deve formatar o tempo relativo', () => {
    renderTimeline()
    expect(screen.getByText('agora mesmo')).toBeInTheDocument()
    expect(screen.getByText('há 1h')).toBeInTheDocument()
    expect(screen.getByText('ontem')).toBeInTheDocument()
  })

  it('deve alternar a exibição de detalhes quando disponível', () => {
    renderTimeline()
    const expandButton = screen.getByText('Ver detalhes')
    fireEvent.click(expandButton)

    expect(screen.getByText(/"method": "LINK"/i)).toBeInTheDocument()
    expect(screen.getByText('Ocultar detalhes')).toBeInTheDocument()

    fireEvent.click(screen.getByText('Ocultar detalhes'))
    expect(screen.queryByText(/"method": "LINK"/i)).not.toBeInTheDocument()
  })

  it('deve chamar onLoadMore ao clicar no botão de carregar mais', () => {
    renderTimeline({ hasMore: true })
    const loadMoreButton = screen.getByText('Carregar mais atividades')
    fireEvent.click(loadMoreButton)
    expect(mockOnLoadMore).toHaveBeenCalled()
  })

  it('deve mostrar o indicador de carregamento e ocultar botão quando loading é true', () => {
    renderTimeline({ loading: true })
    expect(screen.queryByText('Carregar mais atividades')).not.toBeInTheDocument()
    // Verifica presença do spinner pelo classe nl-spinner-sm
    expect(document.querySelector('.nl-spinner-sm')).toBeInTheDocument()
  })

  it('deve chamar onClose ao clicar no botão fechar', () => {
    renderTimeline()
    const closeButton = screen.getByLabelText('Fechar timeline')
    fireEvent.click(closeButton)
    expect(mockOnClose).toHaveBeenCalled()
  })
})
