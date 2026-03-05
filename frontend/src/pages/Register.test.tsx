import { describe, it, expect, vi, beforeEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';
import { Register } from './Register';
import { authApi } from '../api/authApi';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../api/authApi', () => ({
  authApi: {
    register: vi.fn(),
  },
}));

describe('Register page', () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    vi.mocked(authApi.register).mockReset();
  });

  it('envia cadastro e redireciona para login preservando redirect', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.register).mockResolvedValue({
      id: 'user-1',
      username: 'leo',
      email: 'leo@test.com',
      name: 'Leo',
      avatarUrl: null,
      authProvider: 'EMAIL',
      createdAt: '2026-03-04T12:00:00',
    });

    render(
      <MemoryRouter initialEntries={['/register?redirect=%2Fjoin%2Fcode-1']}>
        <Routes>
          <Route path="/register" element={<Register />} />
        </Routes>
      </MemoryRouter>
    );

    await user.type(screen.getByLabelText('Nome'), 'Leo Oliveira');
    await user.type(screen.getByLabelText('Username'), 'Leo_User');
    await user.type(screen.getByLabelText('Email'), 'Leo@Test.com');
    await user.type(screen.getByLabelText('Senha'), '123456');
    await user.type(screen.getByLabelText('Confirmar senha'), '123456');
    await user.click(screen.getByRole('button', { name: /Criar Conta/i }));

    await waitFor(() => {
      expect(authApi.register).toHaveBeenCalledWith({
        name: 'Leo Oliveira',
        username: 'leo_user',
        email: 'leo@test.com',
        password: '123456',
      });
    });

    expect(mockNavigate).toHaveBeenCalledWith(
      '/login?registered=1&email=leo%40test.com&redirect=%2Fjoin%2Fcode-1',
      { replace: true }
    );
  });

  it('bloqueia envio quando as senhas nao conferem', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/register']}>
        <Routes>
          <Route path="/register" element={<Register />} />
        </Routes>
      </MemoryRouter>
    );

    await user.type(screen.getByLabelText('Username'), 'leo');
    await user.type(screen.getByLabelText('Email'), 'leo@test.com');
    await user.type(screen.getByLabelText('Senha'), '123456');
    await user.type(screen.getByLabelText('Confirmar senha'), 'abcdef');
    await user.click(screen.getByRole('button', { name: /Criar Conta/i }));

    expect(await screen.findByText('As senhas nao conferem.')).toBeInTheDocument();
    expect(authApi.register).not.toHaveBeenCalled();
  });
});
