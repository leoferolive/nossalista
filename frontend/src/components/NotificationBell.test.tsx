import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { NotificationBell } from './NotificationBell'
import { NotificationCtx } from '../contexts/NotificationContext'
import type { AppNotification } from '../contexts/NotificationContext'

function makeCtx(
  notifications: AppNotification[] = [],
  markAllRead = vi.fn(),
  clearAll = vi.fn()
) {
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
      { id: '1', type: 'ITEM_ADDED', listId: 'l1', message: 'joao adicionou "Leite"', timestamp: '', read: false },
    ]
    renderBell(makeCtx(notifications))
    fireEvent.click(screen.getByLabelText('Notificações'))

    expect(screen.getByText('joao adicionou "Leite"')).toBeTruthy()
  })
})
