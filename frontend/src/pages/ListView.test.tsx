import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ListView } from './ListView';
import { useLists } from '../hooks/useLists';
import { useItems } from '../hooks/useItems';
import { useToast } from '../components/Toast';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../contexts/AuthContext';
import { ListResponse } from '../types/List';
import { ListItem } from '../types/Item';
import { listsApi } from '../api/listsApi';

const mockNavigate = vi.fn();

// Mock hooks
vi.mock('../hooks/useLists');
vi.mock('../hooks/useItems');
vi.mock('../components/Toast');
vi.mock('../hooks/useWebSocket');
vi.mock('../contexts/AuthContext');
vi.mock('../api/listsApi', () => ({
  listsApi: {
    getListMembers: vi.fn(),
    leaveList: vi.fn(),
    generateInviteLink: vi.fn(),
    searchUsers: vi.fn(),
    inviteByUsername: vi.fn(),
    deleteListMember: vi.fn(),
  },
}));
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'test-list-id' }),
    useNavigate: () => mockNavigate,
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
    mockNavigate.mockReset();

    (listsApi.getListMembers as any).mockResolvedValue([
      {
        user: { id: 'owner-id', username: 'testuser', name: 'Test User', avatar_url: null },
        role: 'OWNER',
        joined_at: new Date().toISOString(),
      },
    ]);
    (listsApi.leaveList as any).mockResolvedValue(undefined);

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
      setItems: vi.fn(),
      loadingItems: false,
      errorItems: null,
      addingItem: false,
      fetchItems: vi.fn(),
      addItem: vi.fn(),
      toggleItem: vi.fn(),
      updateItem: vi.fn(),
      deleteItem: vi.fn(),
      clearItemsError: vi.fn(),
    });

    (useWebSocket as any).mockReturnValue({
      status: 'DISCONNECTED',
      connect: vi.fn(),
      disconnect: vi.fn(),
      subscribe: vi.fn(),
      unsubscribe: vi.fn(),
    });

    (useAuth as any).mockReturnValue({
      user: { id: 'owner-id', username: 'testuser', email: 'test@test.com', displayName: 'Test User' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
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

  it('deve abrir modal de membros e mostrar aviso para owner', async () => {
    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    fireEvent.click(screen.getByLabelText('Abrir membros'));

    expect(await screen.findByRole('heading', { name: 'Membros' })).toBeInTheDocument();
    expect(screen.getAllByText('Você é o dono').length).toBeGreaterThan(0);
  });

  it('deve mostrar botão sair da lista para membro', async () => {
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
    (listsApi.getListMembers as any).mockResolvedValue([
      {
        user: { id: 'owner-id', username: 'owner', name: 'Owner', avatar_url: null },
        role: 'OWNER',
        joined_at: new Date().toISOString(),
      },
      {
        user: { id: 'member-id', username: 'member', name: 'Member', avatar_url: null },
        role: 'MEMBER',
        joined_at: new Date().toISOString(),
      },
    ]);

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    fireEvent.click(screen.getByLabelText('Abrir membros'));

    expect(await screen.findByText('Sair da Lista')).toBeInTheDocument();
  });

  it('deve confirmar saída, chamar API e redirecionar para Home', async () => {
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
    (listsApi.getListMembers as any).mockResolvedValue([
      {
        user: { id: 'owner-id', username: 'owner', name: 'Owner', avatar_url: null },
        role: 'OWNER',
        joined_at: new Date().toISOString(),
      },
      {
        user: { id: 'member-id', username: 'member', name: 'Member', avatar_url: null },
        role: 'MEMBER',
        joined_at: new Date().toISOString(),
      },
    ]);
    (listsApi.leaveList as any).mockResolvedValue(undefined);

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    fireEvent.click(screen.getByLabelText('Abrir membros'));
    fireEvent.click(await screen.findByText('Sair da Lista'));

    expect(screen.getByText('Sair da lista? Você perderá acesso.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Sair' }));

    await waitFor(() => {
      expect(listsApi.leaveList).toHaveBeenCalledWith('test-list-id');
      expect(mockNavigate).toHaveBeenCalledWith('/', {
        state: {
          toastMessage: 'Você saiu',
          toastType: 'success',
          refreshLists: true,
        },
      });
    });
  });
});

describe('ListView - WebSocket Integration', () => {
  const mockShowToast = vi.fn();
  const mockConnect = vi.fn();
  const mockDisconnect = vi.fn();
  const mockSubscribe = vi.fn();
  const mockUnsubscribe = vi.fn();
  const mockSetItems = vi.fn();

  const mockList: ListResponse = {
    id: 'test-list-id',
    name: 'Lista WebSocket',
    type: { id: 1, name: 'Compras', slug: 'compras' },
    owner: { id: 'owner-id', username: 'testuser', name: 'Test User', avatarUrl: null },
    inviteCode: 'WS123',
    isOwner: true,
    itemsCount: 0,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const currentUserId = 'owner-id';
  const otherUserId = 'other-user-id';

  const existingItem: ListItem = {
    id: 'item-1',
    name: 'Item Existente',
    checked: false,
    quantity: null,
    dueDate: null,
    url: null,
    position: 0,
    createdBy: { id: currentUserId, username: 'testuser', name: 'Test User', avatarUrl: null },
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    vi.clearAllMocks();

    (listsApi.getListMembers as any).mockResolvedValue([]);

    (useLists as any).mockReturnValue({
      currentList: mockList,
      loadingList: false,
      errorList: null,
      updatingList: false,
      deletingList: false,
      fetchListById: vi.fn(),
      updateListName: vi.fn(),
      deleteList: vi.fn(),
      clearListError: vi.fn(),
    });

    (useToast as any).mockReturnValue({
      toasts: [],
      showToast: mockShowToast,
      removeToast: vi.fn(),
    });

    (useItems as any).mockReturnValue({
      items: [existingItem],
      setItems: mockSetItems,
      loadingItems: false,
      errorItems: null,
      addingItem: false,
      fetchItems: vi.fn(),
      addItem: vi.fn(),
      toggleItem: vi.fn(),
      updateItem: vi.fn(),
      deleteItem: vi.fn(),
      clearItemsError: vi.fn(),
    });

    (useAuth as any).mockReturnValue({
      user: { id: currentUserId, username: 'testuser', email: 'test@test.com', displayName: 'Test User' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    });

    (useWebSocket as any).mockReturnValue({
      status: 'CONNECTED',
      connect: mockConnect,
      disconnect: mockDisconnect,
      subscribe: mockSubscribe,
      unsubscribe: mockUnsubscribe,
    });
  });

  it('deve chamar connect() ao montar o componente', () => {
    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    expect(mockConnect).toHaveBeenCalled();
  });

  it('deve chamar subscribe(listId, handler) quando status é CONNECTED', () => {
    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    expect(mockSubscribe).toHaveBeenCalledWith('test-list-id', expect.any(Function));
  });

  it('deve chamar disconnect() ao desmontar o componente', () => {
    const { unmount } = render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    unmount();

    expect(mockDisconnect).toHaveBeenCalled();
  });

  it('deve adicionar item ao estado quando recebe ITEM_ADDED de outro usuário', () => {
    const newItem: ListItem = {
      id: 'item-new',
      name: 'Novo Item WS',
      checked: false,
      quantity: null,
      dueDate: null,
      url: null,
      position: 1,
      createdBy: { id: otherUserId, username: 'maria', name: 'Maria', avatarUrl: null },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    expect(capturedHandler).not.toBeNull();

    act(() => {
      capturedHandler!({
        type: 'ITEM_ADDED',
        payload: newItem,
        userId: otherUserId,
        username: 'maria',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockSetItems).toHaveBeenCalled();
  });

  it('deve exibir Toast quando recebe ITEM_ADDED de outro usuário', () => {
    const newItem: ListItem = {
      id: 'item-ws',
      name: 'Item da Maria',
      checked: false,
      quantity: null,
      dueDate: null,
      url: null,
      position: 1,
      createdBy: { id: otherUserId, username: 'maria', name: 'Maria', avatarUrl: null },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    act(() => {
      capturedHandler!({
        type: 'ITEM_ADDED',
        payload: newItem,
        userId: otherUserId,
        username: 'maria',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockShowToast).toHaveBeenCalledWith('maria adicionou Item da Maria', 'info');
  });

  it('NÃO deve exibir Toast quando ITEM_ADDED é do próprio usuário', () => {
    const ownItem: ListItem = {
      id: 'item-own',
      name: 'Meu Item',
      checked: false,
      quantity: null,
      dueDate: null,
      url: null,
      position: 1,
      createdBy: { id: currentUserId, username: 'testuser', name: 'Test User', avatarUrl: null },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    act(() => {
      capturedHandler!({
        type: 'ITEM_ADDED',
        payload: ownItem,
        userId: currentUserId,
        username: 'testuser',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockShowToast).not.toHaveBeenCalledWith(
      expect.stringContaining('adicionou'),
      'info'
    );
    expect(mockSetItems).not.toHaveBeenCalled();
  });

  it('deve remover item do estado quando recebe ITEM_REMOVED de outro usuário', () => {
    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    act(() => {
      capturedHandler!({
        type: 'ITEM_REMOVED',
        payload: existingItem,
        userId: otherUserId,
        username: 'maria',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria removeu Item Existente', 'info');
  });

  it('deve atualizar item no estado quando recebe ITEM_UPDATED de outro usuário', () => {
    const updatedItem: ListItem = { ...existingItem, name: 'Item Atualizado' };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!({
        type: 'ITEM_UPDATED',
        payload: updatedItem,
        userId: otherUserId,
        username: 'maria',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria editou Item Atualizado', 'info');
  });

  it('NÃO deve exibir Toast quando ITEM_UPDATED é do próprio usuário', () => {
    const updatedItem: ListItem = { ...existingItem, name: 'Item Atualizado Próprio' };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!({
        type: 'ITEM_UPDATED',
        payload: updatedItem,
        userId: currentUserId,
        username: 'testuser',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockSetItems).not.toHaveBeenCalled();
    expect(mockShowToast).not.toHaveBeenCalledWith(
      expect.stringContaining('editou'), 'info'
    );
  });

  it('deve exibir Toast "marcou" quando recebe ITEM_CHECKED com checked=true de outro usuário', () => {
    const checkedItem: ListItem = { ...existingItem, checked: true };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!({
        type: 'ITEM_CHECKED',
        payload: checkedItem,
        userId: otherUserId,
        username: 'maria',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria marcou Item Existente', 'info');
  });

  it('deve exibir Toast "desmarcou" quando recebe ITEM_CHECKED com checked=false de outro usuário', () => {
    const uncheckedItem: ListItem = { ...existingItem, checked: false };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!({
        type: 'ITEM_CHECKED',
        payload: uncheckedItem,
        userId: otherUserId,
        username: 'maria',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria desmarcou Item Existente', 'info');
  });

  it('não deve exibir Toast de marcou/desmarcou para ITEM_CHECKED do próprio usuário', () => {
    const ownCheckedItem: ListItem = { ...existingItem, checked: true };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!({
        type: 'ITEM_CHECKED',
        payload: ownCheckedItem,
        userId: currentUserId,
        username: 'testuser',
        timestamp: new Date().toISOString(),
      });
    });

    expect(mockSetItems).not.toHaveBeenCalled();
    expect(mockShowToast).not.toHaveBeenCalledWith(expect.stringContaining('marcou'), 'info');
    expect(mockShowToast).not.toHaveBeenCalledWith(expect.stringContaining('desmarcou'), 'info');
  });
});
