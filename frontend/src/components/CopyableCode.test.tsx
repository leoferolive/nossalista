import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { CopyableCode } from './CopyableCode'

describe('CopyableCode', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
  })

  it('exibe o código informado', () => {
    render(<CopyableCode code="npx exemplo --flag" />)

    expect(screen.getByText('npx exemplo --flag')).toBeInTheDocument()
  })

  it('exibe o rótulo quando informado', () => {
    render(<CopyableCode code="npx exemplo" label="Comando" />)

    expect(screen.getByText('Comando')).toBeInTheDocument()
  })

  it('não exibe rótulo quando não informado', () => {
    render(<CopyableCode code="npx exemplo" />)

    expect(screen.queryByText('Comando')).not.toBeInTheDocument()
  })

  it('copia o código para a área de transferência ao clicar em copiar', async () => {
    render(<CopyableCode code="npx exemplo --flag" />)

    fireEvent.click(screen.getByLabelText('Copiar comando'))

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('npx exemplo --flag')
    })
    expect(screen.getByText('Copiado!')).toBeInTheDocument()
  })

  it('volta a exibir "Copiar" após o tempo de feedback', async () => {
    render(<CopyableCode code="npx exemplo" />)

    fireEvent.click(screen.getByLabelText('Copiar comando'))

    await waitFor(() => {
      expect(screen.getByText('Copiado!')).toBeInTheDocument()
    })

    await waitFor(
      () => {
        expect(screen.getByText('Copiar')).toBeInTheDocument()
      },
      { timeout: 3000 }
    )
  })

  it('usa o fallback de textarea quando navigator.clipboard não está disponível', async () => {
    Object.assign(navigator, { clipboard: undefined })
    document.execCommand = vi.fn().mockReturnValue(true)

    render(<CopyableCode code="npx exemplo" />)
    fireEvent.click(screen.getByLabelText('Copiar comando'))

    await waitFor(() => {
      expect(document.execCommand).toHaveBeenCalledWith('copy')
    })
    expect(screen.getByText('Copiado!')).toBeInTheDocument()
  })

  it('não quebra quando a cópia falha', async () => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockRejectedValue(new Error('denied')) },
    })

    render(<CopyableCode code="npx exemplo" />)
    fireEvent.click(screen.getByLabelText('Copiar comando'))

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalled()
    })
    expect(screen.queryByText('Copiado!')).not.toBeInTheDocument()
    expect(screen.getByText('Copiar')).toBeInTheDocument()
  })
})
