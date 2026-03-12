import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ResponsiveSheet } from './ResponsiveSheet'

describe('ResponsiveSheet', () => {
  it('não renderiza quando fechado', () => {
    const onClose = vi.fn()

    const { container } = render(
      <ResponsiveSheet isOpen={false} onClose={onClose} title="Conta">
        <p>Conteúdo</p>
      </ResponsiveSheet>
    )

    expect(container).toBeEmptyDOMElement()
  })

  it('trava o scroll da página e restaura ao fechar', () => {
    const onClose = vi.fn()
    const { rerender } = render(
      <ResponsiveSheet isOpen onClose={onClose} title="Conta">
        <p>Conteúdo</p>
      </ResponsiveSheet>
    )

    expect(document.body.style.overflow).toBe('hidden')

    rerender(
      <ResponsiveSheet isOpen={false} onClose={onClose} title="Conta">
        <p>Conteúdo</p>
      </ResponsiveSheet>
    )

    expect(document.body.style.overflow).toBe('')
  })

  it('foca o painel e fecha com Escape ou clique no backdrop', () => {
    const onClose = vi.fn()

    render(
      <ResponsiveSheet isOpen onClose={onClose} title="Conta" description="Atalhos">
        <p>Conteúdo</p>
      </ResponsiveSheet>
    )

    const dialog = screen.getByRole('dialog', { name: 'Conta' })
    const panel = dialog.firstElementChild as HTMLElement
    expect(panel).toBe(document.activeElement)
    expect(screen.getByText('Atalhos')).toBeInTheDocument()

    fireEvent.keyDown(document, { key: 'Escape' })
    fireEvent.click(dialog)

    expect(onClose).toHaveBeenCalledTimes(2)
  })

  it('não fecha ao clicar no painel e fecha no botão fechar', () => {
    const onClose = vi.fn()

    render(
      <ResponsiveSheet
        isOpen
        onClose={onClose}
        title="Conta"
        footer={<button type="button">Salvar</button>}
      >
        <p>Conteúdo</p>
      </ResponsiveSheet>
    )

    const dialog = screen.getByRole('dialog', { name: 'Conta' })
    const panel = dialog.firstElementChild as HTMLElement

    fireEvent.click(panel)
    expect(onClose).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }))
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeInTheDocument()
  })

  it('fecha com gesto de arrastar para baixo acima do limite', () => {
    const onClose = vi.fn()

    render(
      <ResponsiveSheet isOpen onClose={onClose} title="Conta">
        <p>Conteúdo</p>
      </ResponsiveSheet>
    )

    const panel = screen.getByRole('dialog', { name: 'Conta' }).firstElementChild as HTMLElement

    fireEvent.touchStart(panel, { touches: [{ clientY: 120 }] })
    fireEvent.touchEnd(panel, { changedTouches: [{ clientY: 195 }] })
    fireEvent.touchStart(panel, { touches: [{ clientY: 120 }] })
    fireEvent.touchEnd(panel, { changedTouches: [{ clientY: 150 }] })

    expect(onClose).toHaveBeenCalledTimes(1)
  })
})
