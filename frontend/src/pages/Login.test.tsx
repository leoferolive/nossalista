import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import Login from './Login'
import { ThemeProvider } from '../contexts/ThemeContext'

const mockLogin = vi.fn()

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    login: mockLogin,
  }),
}))

vi.mock('../api/client', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('Login page', () => {
  beforeEach(() => {
    mockLogin.mockReset()
    sessionStorage.clear()
  })

  const renderLogin = (initialEntry: string) =>
    render(
      <ThemeProvider>
        <MemoryRouter initialEntries={[initialEntry]}>
          <Routes>
            <Route path="/login" element={<Login />} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    )

  it('exibe links de cadastro e recuperacao preservando redirect', () => {
    renderLogin('/login?redirect=%2Fjoin%2Fabc123')

    expect(
      screen
        .getAllByRole('link', { name: 'Criar conta' })
        .every((link) => link.getAttribute('href') === '/register?redirect=%2Fjoin%2Fabc123')
    ).toBe(true)
    expect(screen.getByRole('link', { name: 'Esqueci minha senha' })).toHaveAttribute(
      'href',
      '/forgot-password?redirect=%2Fjoin%2Fabc123'
    )
  })

  it('mostra mensagem de sucesso quando usuario vem do cadastro', () => {
    renderLogin('/login?registered=1&email=leo%40test.com')

    expect(screen.getByText(/Conta criada com sucesso/i)).toBeInTheDocument()
    expect(screen.getByDisplayValue('leo@test.com')).toBeInTheDocument()
  })

  it('envia credenciais, atualiza o contexto e segue para a home', async () => {
    const { default: client } = await import('../api/client')
    vi.mocked(client.post).mockResolvedValueOnce({
      data: {
        id: 'u1',
        username: 'leo',
        email: 'leo@test.com',
        name: 'Leo',
        avatarUrl: null,
        onboardingCompletedAt: null,
      },
    })
    renderLogin('/login')

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'leo@test.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: '123456' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/api/auth/login', {
        email: 'leo@test.com',
        password: '123456',
      })
    })
    expect(mockLogin).toHaveBeenCalledWith(expect.objectContaining({ id: 'u1', username: 'leo' }))
  })

  it('inicia o OAuth do Google na rota segura do backend', () => {
    const originalLocation = window.location
    Object.defineProperty(window, 'location', {
      value: { origin: 'http://localhost', href: '' },
      writable: true,
    })
    renderLogin('/login')

    fireEvent.click(screen.getByRole('button', { name: /continuar com google/i }))

    expect(window.location.href).toBe('http://localhost/api/auth/google')
    Object.defineProperty(window, 'location', { value: originalLocation, writable: true })
  })
})
