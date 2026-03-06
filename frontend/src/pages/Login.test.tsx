import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import Login from './Login'

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

  it('exibe links de cadastro e recuperacao preservando redirect', () => {
    render(
      <MemoryRouter initialEntries={['/login?redirect=%2Fjoin%2Fabc123']}>
        <Routes>
          <Route path="/login" element={<Login />} />
        </Routes>
      </MemoryRouter>
    )

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
    render(
      <MemoryRouter initialEntries={['/login?registered=1&email=leo%40test.com']}>
        <Routes>
          <Route path="/login" element={<Login />} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText(/Conta criada com sucesso/i)).toBeInTheDocument()
    expect(screen.getByDisplayValue('leo@test.com')).toBeInTheDocument()
  })
})
