import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ConnectionStatusIndicator } from './ConnectionStatusIndicator'

describe('ConnectionStatusIndicator', () => {
  it('CONNECTED deve mostrar Online', () => {
    render(<ConnectionStatusIndicator status="CONNECTED" />)
    expect(screen.getByText('Online')).toBeInTheDocument()
  })

  it('RECONNECTING deve mostrar Reconectando… com animate-pulse', () => {
    render(<ConnectionStatusIndicator status="RECONNECTING" />)

    expect(screen.getByText('Reconectando…')).toBeInTheDocument()
    expect(screen.getByText('Reconectando…').previousSibling).toHaveClass('animate-pulse')
  })

  it('DISCONNECTED deve mostrar Offline', () => {
    render(<ConnectionStatusIndicator status="DISCONNECTED" />)

    expect(screen.getByText('Offline')).toBeInTheDocument()
  })

  it('CONNECTING deve mostrar Conectando…', () => {
    render(<ConnectionStatusIndicator status="CONNECTING" />)

    expect(screen.getByText('Conectando…')).toBeInTheDocument()
  })

  it('aplica data-tour quando informado', () => {
    const { container } = render(<ConnectionStatusIndicator status="CONNECTED" dataTour="realtime" />)
    expect(container.querySelector('[data-tour="realtime"]')).toBeInTheDocument()
  })
})
