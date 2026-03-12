import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { ResponsiveActionMenu } from './ResponsiveActionMenu'

let mockIsMobile = false

vi.mock('../hooks/useIsMobile', () => ({
  useIsMobile: () => mockIsMobile,
}))

describe('ResponsiveActionMenu', () => {
  beforeEach(() => {
    mockIsMobile = false
  })

  it('abre menu desktop, executa ação e fecha', () => {
    const onArchive = vi.fn()

    render(
      <ResponsiveActionMenu
        triggerLabel="Mais ações da lista"
        title="Mais ações"
        items={[
          { label: 'Arquivar', onSelect: onArchive },
          { label: 'Excluir', onSelect: vi.fn(), tone: 'danger' },
        ]}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: 'Mais ações da lista' }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    const archiveButton = screen.getByRole('menuitem', { name: 'Arquivar' })
    fireEvent.click(archiveButton)

    expect(onArchive).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('fecha o menu desktop ao clicar fora ou pressionar Escape', () => {
    render(
      <ResponsiveActionMenu
        triggerLabel="Mais ações da lista"
        title="Mais ações"
        items={[{ label: 'Arquivar', onSelect: vi.fn() }]}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: 'Mais ações da lista' }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    fireEvent.mouseDown(document.body)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Mais ações da lista' }))
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('usa bottom sheet no mobile e fecha ao selecionar uma ação', () => {
    mockIsMobile = true
    const onArchive = vi.fn()

    render(
      <ResponsiveActionMenu
        triggerLabel="Mais ações da lista"
        title="Mais ações da lista"
        items={[{ label: 'Arquivar', onSelect: onArchive }]}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: 'Mais ações da lista' }))
    expect(screen.getByRole('dialog', { name: 'Mais ações da lista' })).toBeInTheDocument()
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Arquivar' }))

    expect(onArchive).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('dialog', { name: 'Mais ações da lista' })).not.toBeInTheDocument()
  })

  it('fecha o sheet mobile pelo botão de fechar', () => {
    mockIsMobile = true

    render(
      <ResponsiveActionMenu
        triggerLabel="Mais ações da lista"
        title="Mais ações da lista"
        items={[{ label: 'Arquivar', onSelect: vi.fn() }]}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: 'Mais ações da lista' }))
    expect(screen.getByRole('dialog', { name: 'Mais ações da lista' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }))
    expect(screen.queryByRole('dialog', { name: 'Mais ações da lista' })).not.toBeInTheDocument()
  })
})
