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

  it('deve formatar o tempo relativo e agrupar por data', () => {
    renderTimeline()
    // Tempo relativo para entradas recentes
    expect(screen.getByText('agora mesmo')).toBeInTheDocument()
    expect(screen.getByText('há 1h')).toBeInTheDocument()
    // Agrupamento por data: "Hoje" e "Ontem" como headers de grupo
    expect(screen.getByText('Hoje')).toBeInTheDocument()
    expect(screen.getByText('Ontem')).toBeInTheDocument()
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

  // ---------------------------------------------------------------------------
  // getActivityDescription: cobertura de todos os tipos de ação
  // ---------------------------------------------------------------------------

  const baseActivity = (overrides: Partial<ActivityLog>): ActivityLog => ({
    id: 'a-x',
    listId: 'list-1',
    userId: 'user-x',
    userName: 'Carlos',
    action: 'ITEM_ADDED',
    targetType: 'ITEM',
    targetId: 'item-x',
    targetName: 'Pão',
    details: null,
    createdAt: new Date().toISOString(),
    ...overrides,
  })

  it('descreve ITEM_UPDATED', () => {
    renderTimeline({
      activities: [baseActivity({ id: 'u1', action: 'ITEM_UPDATED', targetName: 'Arroz' })],
    })
    expect(screen.getByText(/editou "Arroz"/i)).toBeInTheDocument()
  })

  it('descreve ITEM_REMOVED', () => {
    renderTimeline({
      activities: [baseActivity({ id: 'r1', action: 'ITEM_REMOVED', targetName: 'Açúcar' })],
    })
    expect(screen.getByText(/removeu "Açúcar"/i)).toBeInTheDocument()
  })

  it('descreve ITEM_UNCHECKED', () => {
    renderTimeline({
      activities: [baseActivity({ id: 'un1', action: 'ITEM_UNCHECKED', targetName: 'Sal' })],
    })
    expect(screen.getByText(/desmarcou "Sal"/i)).toBeInTheDocument()
  })

  it('descreve MEMBER_JOINED sem método LINK como "foi convidado"', () => {
    renderTimeline({
      activities: [
        baseActivity({
          id: 'mj1',
          action: 'MEMBER_JOINED',
          targetType: 'MEMBER',
          details: { method: 'USERNAME' },
        }),
      ],
    })
    expect(screen.getByText(/foi convidado/i)).toBeInTheDocument()
  })

  it('descreve MEMBER_JOINED sem details como "foi convidado"', () => {
    renderTimeline({
      activities: [
        baseActivity({
          id: 'mj2',
          action: 'MEMBER_JOINED',
          targetType: 'MEMBER',
          details: null,
        }),
      ],
    })
    expect(screen.getByText(/foi convidado/i)).toBeInTheDocument()
  })

  it('descreve MEMBER_LEFT', () => {
    renderTimeline({
      activities: [
        baseActivity({ id: 'ml1', action: 'MEMBER_LEFT', targetType: 'MEMBER', details: null }),
      ],
    })
    expect(screen.getByText(/saiu da lista/i)).toBeInTheDocument()
  })

  it('descreve MEMBER_REMOVED com removedBy', () => {
    renderTimeline({
      activities: [
        baseActivity({
          id: 'mr1',
          action: 'MEMBER_REMOVED',
          targetType: 'MEMBER',
          details: { removedBy: 'Admin' },
        }),
      ],
    })
    expect(screen.getByText(/foi removido por Admin/i)).toBeInTheDocument()
  })

  it('descreve MEMBER_REMOVED sem removedBy', () => {
    renderTimeline({
      activities: [
        baseActivity({
          id: 'mr2',
          action: 'MEMBER_REMOVED',
          targetType: 'MEMBER',
          details: null,
        }),
      ],
    })
    const desc = screen.getByText(/foi removido/i)
    expect(desc).toBeInTheDocument()
    expect(desc.textContent).not.toMatch(/por/i)
  })

  it('usa descrição padrão para ação desconhecida', () => {
    renderTimeline({
      // Força uma ação fora do enum para o ramo default
      activities: [baseActivity({ id: 'unk', action: 'SOMETHING_ELSE' as any })],
    })
    expect(screen.getByText(/realizou uma ação/i)).toBeInTheDocument()
  })

  it('usa "um item" quando targetName está ausente', () => {
    renderTimeline({
      activities: [baseActivity({ id: 'noname', action: 'ITEM_ADDED', targetName: null })],
    })
    expect(screen.getByText(/adicionou "um item"/i)).toBeInTheDocument()
  })

  it('usa "um item" no fallback de ITEM_CHECKED/UNCHECKED/UPDATED/REMOVED sem targetName', () => {
    renderTimeline({
      activities: [
        baseActivity({ id: 'c-nn', action: 'ITEM_CHECKED', targetName: null, targetId: 't1' }),
        baseActivity({ id: 'u-nn', action: 'ITEM_UNCHECKED', targetName: null, targetId: 't2' }),
        baseActivity({ id: 'e-nn', action: 'ITEM_UPDATED', targetName: null, targetId: 't3' }),
        baseActivity({ id: 'r-nn', action: 'ITEM_REMOVED', targetName: null, targetId: 't4' }),
      ],
    })
    expect(screen.getByText(/marcou "um item" como concluído/i)).toBeInTheDocument()
    expect(screen.getByText(/desmarcou "um item"/i)).toBeInTheDocument()
    expect(screen.getByText(/editou "um item"/i)).toBeInTheDocument()
    expect(screen.getByText(/removeu "um item"/i)).toBeInTheDocument()
  })

  it('usa "Usuário" quando userName está ausente e inicial do userId no avatar', () => {
    renderTimeline({
      activities: [
        baseActivity({ id: 'nouser', userName: null, userId: 'zeta-123', action: 'ITEM_ADDED' }),
      ],
    })
    expect(screen.getByText('Usuário')).toBeInTheDocument()
    // O avatar usa a inicial do userId (Z) quando userName é null
    expect(screen.getByText('Z')).toBeInTheDocument()
  })

  // ---------------------------------------------------------------------------
  // formatRelativeTime: ramos de segundos/minutos/horas/data
  // ---------------------------------------------------------------------------

  it('mostra segundos quando entre 10s e 60s', () => {
    renderTimeline({
      activities: [
        baseActivity({ id: 's1', createdAt: new Date(Date.now() - 30_000).toISOString() }),
      ],
    })
    expect(screen.getByText(/há \d+s/)).toBeInTheDocument()
  })

  it('mostra minutos quando entre 1min e 60min', () => {
    renderTimeline({
      activities: [
        baseActivity({ id: 'm1', createdAt: new Date(Date.now() - 5 * 60_000).toISOString() }),
      ],
    })
    expect(screen.getByText(/há 5 min/)).toBeInTheDocument()
  })

  it('mostra data formatada quando passou mais de 24h e o dia não é ontem', () => {
    const old = new Date(Date.now() - 5 * 86_400_000) // 5 dias atrás
    renderTimeline({
      activities: [baseActivity({ id: 'd1', createdAt: old.toISOString() })],
    })
    // Para >24h não-ontem, o componente exibe o horário (toLocaleTimeString HH:MM)
    expect(screen.getByText(/^\d{2}:\d{2}$/)).toBeInTheDocument()
    // E o header do grupo deve ser uma data DD/MM/AAAA (não "Hoje"/"Ontem")
    expect(screen.getByText(/^\d{2}\/\d{2}\/\d{4}$/)).toBeInTheDocument()
  })

  // ---------------------------------------------------------------------------
  // Colapso de toggles repetidos (>= 3 ITEM_CHECKED/UNCHECKED no mesmo target)
  // ---------------------------------------------------------------------------

  it('colapsa 3+ toggles do mesmo item em uma única entrada', () => {
    const now = Date.now()
    const toggles: ActivityLog[] = [0, 1, 2, 3].map((i) =>
      baseActivity({
        id: `tg-${i}`,
        action: i % 2 === 0 ? 'ITEM_CHECKED' : 'ITEM_UNCHECKED',
        targetId: 'same-item',
        targetName: 'Banana',
        createdAt: new Date(now - i * 1000).toISOString(),
      })
    )
    renderTimeline({ activities: toggles })

    expect(screen.getByText(/foi/i)).toBeInTheDocument()
    expect(screen.getByText(/marcado\/desmarcado/i)).toBeInTheDocument()
    expect(screen.getByText(/4 vezes/i)).toBeInTheDocument()
    // Entrada colapsada presente
    expect(screen.getByTestId('activity-collapsed-tg-0')).toBeInTheDocument()
  })

  it('usa "item" quando a entrada colapsada não tem targetName', () => {
    const now = Date.now()
    const toggles: ActivityLog[] = [0, 1, 2].map((i) =>
      baseActivity({
        id: `tn-${i}`,
        action: 'ITEM_CHECKED',
        targetId: 'item-z',
        targetName: null,
        createdAt: new Date(now - i * 1000).toISOString(),
      })
    )
    renderTimeline({ activities: toggles })
    expect(screen.getByText(/foi/)).toBeInTheDocument()
    expect(screen.getByText(/3 vezes/i)).toBeInTheDocument()
    // O nome cai para "item"
    expect(screen.getByText(/"item"/)).toBeInTheDocument()
  })

  it('não colapsa quando há menos de 3 toggles consecutivos', () => {
    const now = Date.now()
    const toggles: ActivityLog[] = [0, 1].map((i) =>
      baseActivity({
        id: `nc-${i}`,
        action: 'ITEM_CHECKED',
        targetId: 'item-y',
        targetName: 'Maçã',
        createdAt: new Date(now - i * 1000).toISOString(),
      })
    )
    renderTimeline({ activities: toggles })
    // Sem colapso: nenhuma entrada do tipo collapsed
    expect(screen.queryByText(/vezes/i)).not.toBeInTheDocument()
    expect(screen.getAllByText(/marcou "Maçã" como concluído/i).length).toBe(2)
  })

  // ---------------------------------------------------------------------------
  // hasMore / onLoadMore: ramos do botão e do handler
  // ---------------------------------------------------------------------------

  it('não exibe o botão "Carregar mais" quando hasMore é false', () => {
    renderTimeline({ hasMore: false })
    expect(screen.queryByText('Carregar mais atividades')).not.toBeInTheDocument()
  })

  it('não exibe o botão "Carregar mais" quando onLoadMore não é fornecido', () => {
    render(
      <ActivityTimeline
        activities={mockActivities}
        isOpen={true}
        onClose={mockOnClose}
        hasMore={true}
        loading={false}
      />
    )
    expect(screen.queryByText('Carregar mais atividades')).not.toBeInTheDocument()
  })

  it('não chama onLoadMore quando loading=true (handler guard)', () => {
    // hasMore + loading: botão fica oculto, mas garantimos que o estado loading prevalece
    renderTimeline({ loading: true, hasMore: true })
    expect(screen.queryByText('Carregar mais atividades')).not.toBeInTheDocument()
    expect(mockOnLoadMore).not.toHaveBeenCalled()
  })

  it('não renderiza o botão "Ver detalhes" quando a atividade não tem details', () => {
    renderTimeline({
      activities: [baseActivity({ id: 'nd', details: null })],
    })
    expect(screen.queryByText('Ver detalhes')).not.toBeInTheDocument()
  })
})
