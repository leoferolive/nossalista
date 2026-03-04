import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';

const mockUseAuth = vi.fn();

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

describe('ProtectedRoute', () => {
  beforeEach(() => {
    mockUseAuth.mockReset();
  });

  it('mostra loading enquanto a sessao esta em bootstrap', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isBootstrapping: true,
    });

    render(
      <MemoryRouter initialEntries={['/lists/123']}>
        <Routes>
          <Route
            path="/lists/:id"
            element={(
              <ProtectedRoute>
                <div>Conteudo protegido</div>
              </ProtectedRoute>
            )}
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Validando sessão...')).toBeInTheDocument();
  });

  it('redireciona para login preservando o redirect quando nao autenticado', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
      isBootstrapping: false,
    });

    render(
      <MemoryRouter initialEntries={['/lists/123?tab=items']}>
        <Routes>
          <Route
            path="/lists/:id"
            element={(
              <ProtectedRoute>
                <div>Conteudo protegido</div>
              </ProtectedRoute>
            )}
          />
          <Route path="/login" element={<div>Tela de login</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Tela de login')).toBeInTheDocument();
  });

  it('renderiza o conteudo quando autenticado', () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
      isBootstrapping: false,
    });

    render(
      <MemoryRouter initialEntries={['/lists/123']}>
        <Routes>
          <Route
            path="/lists/:id"
            element={(
              <ProtectedRoute>
                <div>Conteudo protegido</div>
              </ProtectedRoute>
            )}
          />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Conteudo protegido')).toBeInTheDocument();
  });
});
