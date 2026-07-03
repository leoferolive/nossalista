import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ConnectAssistant } from './ConnectAssistant'
import { ThemeProvider } from '../contexts/ThemeContext'

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

function renderPage() {
  return render(
    <ThemeProvider>
      <MemoryRouter initialEntries={['/connections/help']}>
        <Routes>
          <Route path="/connections/help" element={<ConnectAssistant />} />
          <Route path="/connections" element={<div>Tela de Conexões</div>} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>
  )
}

describe('ConnectAssistant page', () => {
  beforeEach(() => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
  })

  it('mostra o título e as instruções para os três clientes MCP suportados', () => {
    renderPage()

    expect(screen.getByRole('heading', { name: /conectar seu assistente/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Claude Code' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Claude Desktop' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Cursor' })).toBeInTheDocument()
  })

  it('exibe o comando do Claude Code com a URL de produção do servidor MCP', () => {
    renderPage()

    expect(screen.getByText(/claude mcp add/)).toBeInTheDocument()
    expect(screen.getAllByText(/nossalista\.leoferolive\.com\.br\/mcp/).length).toBeGreaterThan(0)
  })

  it('copia o comando ao clicar em "Copiar" e mostra confirmação temporária', async () => {
    renderPage()

    const copyButtons = screen.getAllByRole('button', { name: /copiar comando/i })
    fireEvent.click(copyButtons[0])

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        expect.stringContaining('claude mcp add')
      )
    })
    expect(screen.getAllByText('Copiado!').length).toBeGreaterThan(0)
  })

  it('explica a diferença entre escopo somente-leitura e leitura-e-escrita', () => {
    renderPage()

    expect(screen.getByText(/somente leitura/i)).toBeInTheDocument()
    expect(screen.getByText(/leitura e escrita/i)).toBeInTheDocument()
  })

  it('linka de volta para a tela de Conexões para criar um token', () => {
    renderPage()

    const links = screen.getAllByRole('link', { name: /conex(õ|o)es/i })
    expect(links.length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('link', { name: /ir para conexões/i }))
    expect(screen.getByText('Tela de Conexões')).toBeInTheDocument()
  })
})
