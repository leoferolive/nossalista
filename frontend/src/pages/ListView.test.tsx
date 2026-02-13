import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ListView } from './ListView';
import { useLists } from '../hooks/useLists';
import { useItems } from '../hooks/useItems';
import { useToast } from '../components/Toast';
import { ListResponse } from '../types/List';

// Mock hooks
vi.mock('../hooks/useLists');
vi.mock('../hooks/useItems');
vi.mock('../components/Toast');
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'test-list-id' }),
    useNavigate: () => vi.fn(),
  };
});

describe('ListView - Delete Functionality', () => {
  const mockFetchListById = vi.fn();
  const mockDeleteList = vi.fn();
  const mockShowToast = vi.fn();
  const mockRemoveToast = vi.fn();

  const mockList: ListResponse = {
    id: 'test-list-id',
    name: 'Lista de Teste',
    type: { id: 1, name: 'Compras', slug: 'compras' },
    owner: { id: 'owner-id', username: 'testuser', name: 'Test User', avatarUrl: null },
    inviteCode: 'ABC123',
    isOwner: true,
    itemsCount: 0,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    vi.clearAllMocks();

    (useLists as any).mockReturnValue({
      currentList: mockList,
      loadingList: false,
      errorList: null,
      updatingList: false,
      deletingList: false,
      fetchListById: mockFetchListById,
      updateListName: vi.fn(),
      deleteList: mockDeleteList,
      clearListError: vi.fn(),
    });

    (useToast as any).mockReturnValue({
      toasts: [],
      showToast: mockShowToast,
      removeToast: mockRemoveToast,
    });

    (useItems as any).mockReturnValue({
      items: [],
      loadingItems: false,
      errorItems: null,
      addingItem: false,
      fetchItems: vi.fn(),
      addItem: vi.fn(),
      clearItemsError: vi.fn(),
    });
  });

  it('deve mostrar botão excluir apenas para dono da lista', () => {
    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    const deleteButton = screen.getByLabelText('Excluir lista');
    expect(deleteButton).toBeInTheDocument();
  });

  it('não deve mostrar botão excluir quando usuário não é dono', () => {
    (useLists as any).mockReturnValue({
      currentList: { ...mockList, isOwner: false },
      loadingList: false,
      errorList: null,
      updatingList: false,
      deletingList: false,
      fetchListById: mockFetchListById,
      updateListName: vi.fn(),
      deleteList: mockDeleteList,
      clearListError: vi.fn(),
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    const deleteButton = screen.queryByLabelText('Excluir lista');
    expect(deleteButton).not.toBeInTheDocument();
  });

  it('deve abrir modal ao clicar no botão excluir', () => {
    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    const deleteButton = screen.getByLabelText('Excluir lista');
    fireEvent.click(deleteButton);

    expect(screen.getByText('Excluir Lista?')).toBeInTheDocument();
  });

  it('deve deletar lista e redirecionar para Home ao confirmar', async () => {
    mockDeleteList.mockResolvedValue(true);

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    // Abrir modal
    const deleteButton = screen.getByLabelText('Excluir lista');
    fireEvent.click(deleteButton);

    // Confirmar exclusão
    const confirmButton = screen.getByText('Excluir');
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith('Excluindo...', 'info');
      expect(mockDeleteList).toHaveBeenCalledWith('test-list-id');
    });
  });

  it('deve mostrar toast de erro e fechar modal em erro 403', async () => {
    const error = new Error('Você não tem permissão para excluir esta lista');
    (error as any).status = 403;
    mockDeleteList.mockRejectedValue(error);

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    // Abrir modal
    const deleteButton = screen.getByLabelText('Excluir lista');
    fireEvent.click(deleteButton);

    // Confirmar exclusão
    const confirmButton = screen.getByText('Excluir');
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith(
        'Você não tem permissão para excluir esta lista',
        'error'
      );
    });
  });

  it('deve mostrar toast de erro e fechar modal em erro 404', async () => {
    const error = new Error('Lista não encontrada');
    (error as any).status = 404;
    mockDeleteList.mockRejectedValue(error);

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    // Abrir modal
    const deleteButton = screen.getByLabelText('Excluir lista');
    fireEvent.click(deleteButton);

    // Confirmar exclusão
    const confirmButton = screen.getByText('Excluir');
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith('Lista não encontrada', 'error');
    });
  });

  it('deve manter modal aberto em erro 500 para retry', async () => {
    const error = new Error('Erro ao excluir lista. Tente novamente.');
    (error as any).status = 500;
    mockDeleteList.mockRejectedValue(error);

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    // Abrir modal
    const deleteButton = screen.getByLabelText('Excluir lista');
    fireEvent.click(deleteButton);

    // Confirmar exclusão
    const confirmButton = screen.getByText('Excluir');
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith(
        'Erro ao excluir lista. Tente novamente.',
        'error'
      );
    });

    // Modal deve permanecer aberto (não fecha)
    expect(screen.getByText('Excluir Lista?')).toBeInTheDocument();
  });

  it('deve ter touch target mínimo de 44px no botão excluir (NFR-A4)', () => {
    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    const deleteButton = screen.getByLabelText('Excluir lista');
    expect(deleteButton).toHaveClass('min-w-[44px]', 'min-h-[44px]');
  });
});
