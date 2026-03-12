import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { NotificationBell } from './NotificationBell'
import { NotificationCtx } from '../contexts/NotificationContext'
import type { AppNotification } from '../contexts/NotificationContext'

let mockIsMobile = false

vi.mock('../hooks/useIsMobile', () => ({
  useIsMobile: () => mockIsMobile,
}))

function makeCtx(notifications: AppNotification[] = [], markAllRead = vi.fn(), clearAll = vi.fn()) {
  return {
    notifications,
    unreadCount: notifications.filter((n) => !n.read).length,
    markAllRead,
    clearAll,
  }
}

function renderBell(ctx = makeCtx()) {
  return render(
    <NotificationCtx.Provider value={ctx}>
      <NotificationBell />
    </NotificationCtx.Provider>
  )
}

describe('NotificationBell', () => {
  beforeEach(() => {
    mockIsMobile = false
  })

  it('deve mostrar badge com unreadCount quando > 0', () => {
    const notifications: AppNotification[] = [
      { id: '1', type: 'ITEM_ADDED', listId: 'l1', message: 'Teste', timestamp: '', read: false },
    ]
    renderBell(makeCtx(notifications))

    expect(screen.getByText('1')).toBeTruthy()
  })

  it('não deve mostrar badge quando unreadCount é 0', () => {
    const notifications: AppNotification[] = [
      { id: '1', type: 'ITEM_ADDED', listId: 'l1', message: 'Teste', timestamp: '', read: true },
    ]
    renderBell(makeCtx(notifications))

    expect(screen.queryByText('1')).toBeNull()
  })

  it('deve chamar markAllRead quando dropdown abre', () => {
    const markAllRead = vi.fn()
    renderBell(makeCtx([], markAllRead))

    fireEvent.click(screen.getByLabelText('Notificações'))

    expect(markAllRead).toHaveBeenCalledTimes(1)
  })

  it('deve exibir "Nenhuma notificação" quando lista está vazia', () => {
    renderBell(makeCtx([]))
    fireEvent.click(screen.getByLabelText('Notificações'))

    expect(screen.getByText('Nenhuma notificação')).toBeTruthy()
  })

  it('deve exibir mensagens das notificações no dropdown', () => {
    const notifications: AppNotification[] = [
      {
        id: '1',
        type: 'ITEM_ADDED',
        listId: 'l1',
        message: 'joao adicionou "Leite"',
        timestamp: '',
        read: false,
      },
    ]
    renderBell(makeCtx(notifications))
    fireEvent.click(screen.getByLabelText('Notificações'))

    expect(screen.getByText('joao adicionou "Leite"')).toBeTruthy()
  })

  it('deve fechar o dropdown ao pressionar Escape', () => {
    renderBell(makeCtx([]))
    fireEvent.click(screen.getByLabelText('Notificações'))
    expect(screen.getByText('Nenhuma notificação')).toBeTruthy()

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByText('Nenhuma notificação')).toBeNull()
  })

  it('deve fechar o dropdown ao clicar fora do componente', () => {
    renderBell(makeCtx([]))
    fireEvent.click(screen.getByLabelText('Notificações'))
    expect(screen.getByText('Nenhuma notificação')).toBeTruthy()

    fireEvent.mouseDown(document.body)
    expect(screen.queryByText('Nenhuma notificação')).toBeNull()
  })

  it('usa sheet no mobile e permite limpar notificações', () => {
    mockIsMobile = true
    const clearAll = vi.fn()
    const formattedTime = new Date('2026-03-11T10:00:00Z').toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    })
    const notifications: AppNotification[] = [
      {
        id: '1',
        type: 'ITEM_ADDED',
        listId: 'l1',
        message: 'joao adicionou "Leite"',
        timestamp: '2026-03-11T10:00:00Z',
        read: false,
      },
    ]

    renderBell(makeCtx(notifications, vi.fn(), clearAll))
    fireEvent.click(screen.getByLabelText('Notificações'))

    expect(screen.getByRole('dialog', { name: 'Notificações' })).toBeInTheDocument()
    expect(screen.getByText('Limpar tudo')).toBeInTheDocument()
    expect(screen.getByText(formattedTime)).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Limpar tudo' }))
    expect(clearAll).toHaveBeenCalledTimes(1)
  })

  it('permite limpar notificações no dropdown desktop e exibe timestamp formatado', () => {
    const clearAll = vi.fn()
    const formattedTime = new Date('2026-03-11T10:00:00Z').toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
    })
    const notifications: AppNotification[] = [
      {
        id: '1',
        type: 'ITEM_ADDED',
        listId: 'l1',
        message: 'joao adicionou "Leite"',
        timestamp: '2026-03-11T10:00:00Z',
        read: false,
      },
    ]

    renderBell(makeCtx(notifications, vi.fn(), clearAll))
    fireEvent.click(screen.getByLabelText('Notificações'))

    expect(screen.getByText(formattedTime)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Limpar tudo' }))

    expect(clearAll).toHaveBeenCalledTimes(1)
  })

  it('fecha o sheet mobile pelo botão de fechar', () => {
    mockIsMobile = true

    renderBell(makeCtx([]))
    fireEvent.click(screen.getByLabelText('Notificações'))
    expect(screen.getByRole('dialog', { name: 'Notificações' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }))
    expect(screen.queryByRole('dialog', { name: 'Notificações' })).not.toBeInTheDocument()
  })
})
