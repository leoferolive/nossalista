import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'

const mockUseAuth = vi.fn()

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}))

function LocationProbe() {
  const location = useLocation()
  return <div>{`${location.pathname}${location.search}`}</div>
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    mockUseAuth.mockReset()
  })

  it('mostra loading enquanto a sessao esta em bootstrap', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isBootstrapping: true,
    })

    render(
      <MemoryRouter initialEntries={['/lists/123']}>
        <Routes>
          <Route
            path="/lists/:id"
            element={
              <ProtectedRoute>
                <div>Conteudo protegido</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Validando Sessão…')).toBeInTheDocument()
  })

  it('redireciona para a landing com login aberto e redirect preservado quando nao autenticado', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isBootstrapping: false,
    })

    render(
      <MemoryRouter initialEntries={['/lists/123?tab=items']}>
        <Routes>
          <Route
            path="/lists/:id"
            element={
              <ProtectedRoute>
                <div>Conteudo protegido</div>
              </ProtectedRoute>
            }
          />
          <Route
            path="/"
            element={
              <div>
                Landing:
                <LocationProbe />
              </div>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(
      screen.getByText('/?auth=login&redirect=%2Flists%2F123%3Ftab%3Ditems')
    ).toBeInTheDocument()
  })

  it('renderiza o conteudo quando autenticado', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      isBootstrapping: false,
    })

    render(
      <MemoryRouter initialEntries={['/lists/123']}>
        <Routes>
          <Route
            path="/lists/:id"
            element={
              <ProtectedRoute>
                <div>Conteudo protegido</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText('Conteudo protegido')).toBeInTheDocument()
  })
})
