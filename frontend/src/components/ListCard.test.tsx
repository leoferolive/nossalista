import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ListCard } from './ListCard';
import { BrowserRouter } from 'react-router-dom';
import type { ListResponse } from '../types/List';

// Mock do useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('ListCard', () => {
  const mockList: ListResponse = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    name: 'Minha Lista de Teste',
    type: {
      id: 1,
      name: 'Compras',
      slug: 'compras',
    },
    owner: {
      id: '123e4567-e89b-12d3-a456-426614174001',
      username: 'testuser',
      name: 'Test User',
      avatarUrl: null,
    },
    inviteCode: 'ABC123XYZ',
    isOwner: true,
    itemsCount: 5,
    createdAt: '2026-02-12T10:00:00Z',
    updatedAt: '2026-02-12T10:00:00Z',
  };

  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('deve renderizar emoji correto baseado no tipo da lista', () => {
    render(
      <BrowserRouter>
        <ListCard list={mockList} />
      </BrowserRouter>
    );

    expect(screen.getByText('🛒')).toBeInTheDocument();
  });

  it('deve renderizar nome da lista', () => {
    render(
      <BrowserRouter>
        <ListCard list={mockList} />
      </BrowserRouter>
    );

    expect(screen.getByText('Minha Lista de Teste')).toBeInTheDocument();
  });

  it('deve renderizar badge "Minha" quando isOwner é true', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, isOwner: true }} />
      </BrowserRouter>
    );

    expect(screen.getByText('Minha')).toBeInTheDocument();
    expect(screen.queryByText('Compartilhada')).not.toBeInTheDocument();
  });

  it('deve renderizar badge "Compartilhada" quando isOwner é false', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, isOwner: false }} />
      </BrowserRouter>
    );

    expect(screen.getByText('Compartilhada')).toBeInTheDocument();
    expect(screen.queryByText('Minha')).not.toBeInTheDocument();
  });

  it('deve renderizar contagem de itens correta', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, itemsCount: 5 }} />
      </BrowserRouter>
    );

    expect(screen.getByText('5 itens')).toBeInTheDocument();
  });

  it('deve renderizar "1 item" quando itemsCount é 1 (singular)', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, itemsCount: 1 }} />
      </BrowserRouter>
    );

    expect(screen.getByText('1 item')).toBeInTheDocument();
  });

  it('deve navegar para /lists/{id} quando clicado', () => {
    render(
      <BrowserRouter>
        <ListCard list={mockList} />
      </BrowserRouter>
    );

    const card = screen.getByRole('button', { name: /abrir lista/i });
    fireEvent.click(card);

    expect(mockNavigate).toHaveBeenCalledWith(`/lists/${mockList.id}`);
  });

  it('deve ter atributos de acessibilidade corretos', () => {
    render(
      <BrowserRouter>
        <ListCard list={mockList} />
      </BrowserRouter>
    );

    const card = screen.getByRole('button', { name: /abrir lista minha lista de teste/i });
    expect(card).toHaveAttribute('tabIndex', '0');
    expect(card).toHaveClass('min-h-[160px]'); // NFR-A4: Touch target
  });

  it('deve renderizar emoji correto para tipo Tarefas', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, type: { id: 2, name: 'Tarefas', slug: 'tarefas' } }} />
      </BrowserRouter>
    );

    expect(screen.getByText('✅')).toBeInTheDocument();
  });

  it('deve renderizar emoji correto para tipo Wishlist', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, type: { id: 3, name: 'Wishlist', slug: 'wishlist' } }} />
      </BrowserRouter>
    );

    expect(screen.getByText('🎁')).toBeInTheDocument();
  });

  it('deve renderizar emoji correto para tipo Genérica', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, type: { id: 4, name: 'Genérica', slug: 'generica' } }} />
      </BrowserRouter>
    );

    expect(screen.getByText('📝')).toBeInTheDocument();
  });

  it('deve renderizar emoji padrão para tipo desconhecido', () => {
    render(
      <BrowserRouter>
        <ListCard list={{ ...mockList, type: { id: 999, name: 'Desconhecido', slug: 'unknown' } }} />
      </BrowserRouter>
    );

    expect(screen.getByText('📝')).toBeInTheDocument();
  });
});
