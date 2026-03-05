import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act, cleanup, within } from '@testing-library/react';
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

afterEach(() => {
  vi.useRealTimers();
  cleanup();
});

// Mock hooks
vi.mock('../hooks/useLists');
vi.mock('../hooks/useItems');
vi.mock('../components/Toast');
vi.mock('../hooks/useWebSocket');
vi.mock('../contexts/AuthContext');
vi.mock('../api/listsApi', () => ({
  listsApi: {
    getListMembers: vi.fn(),
    getListState: vi.fn(),
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

    (listsApi.getListMembers as any).mockImplementation(() => new Promise(() => {}));
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
      send: vi.fn(),
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
    const confirmButton = within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir' });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith('Excluindo…', 'info');
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
    const confirmButton = within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir' });
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
    const confirmButton = within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir' });
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
    const confirmButton = within(screen.getByRole('dialog')).getByRole('button', { name: 'Excluir' });
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
    expect(deleteButton).toHaveClass('min-h-[48px]');
  });

  it('deve abrir modal de membros e mostrar aviso para owner', async () => {
    (listsApi.getListMembers as any).mockResolvedValue([
      {
        user: { id: 'owner-id', username: 'testuser', name: 'Test User', avatar_url: null },
        role: 'OWNER',
        joined_at: new Date().toISOString(),
      },
    ]);

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

  it('deve atualizar contador de membros após convite por username com sucesso', async () => {
    (listsApi.getListMembers as any)
      .mockResolvedValueOnce([
        {
          user: { id: 'owner-id', username: 'testuser', name: 'Test User', avatar_url: null },
          role: 'OWNER',
          joined_at: new Date().toISOString(),
        },
      ])
      .mockResolvedValueOnce([
        {
          user: { id: 'owner-id', username: 'testuser', name: 'Test User', avatar_url: null },
          role: 'OWNER',
          joined_at: new Date().toISOString(),
        },
        {
          user: { id: 'invited-id', username: 'leo', name: 'Leo Oliveira', avatar_url: null },
          role: 'MEMBER',
          joined_at: new Date().toISOString(),
        },
      ]);

    (listsApi.searchUsers as any).mockResolvedValue([
      { username: 'leo', name: 'Leo Oliveira', avatarUrl: null },
    ]);
    (listsApi.inviteByUsername as any).mockResolvedValue({
      invited_username: 'leo',
      message: 'leo adicionado!',
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText('Abrir membros')).toHaveTextContent('1');
    });

    fireEvent.click(screen.getByLabelText('Convidar para lista'));
    fireEvent.change(screen.getByPlaceholderText(/Buscar usuário/i), {
      target: { value: 'leo' },
    });

    expect(await screen.findByRole('button', { name: /leo/i })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /leo/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Convidar' }));

    await waitFor(() => {
      expect(listsApi.inviteByUsername).toHaveBeenCalledWith('test-list-id', 'leo');
      expect(screen.getByLabelText('Abrir membros')).toHaveTextContent('2');
    });
  });
});

describe('ListView - WebSocket Integration', () => {
  const mockShowToast = vi.fn();
  const mockConnect = vi.fn();
  const mockDisconnect = vi.fn();
  const mockSubscribe = vi.fn();
  const mockUnsubscribe = vi.fn();
  const mockSend = vi.fn();
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

  const createWsMessage = ({
    type,
    payload,
    actorId,
    actorUsername,
    channel,
  }: {
    type: string;
    payload: unknown;
    actorId?: string;
    actorUsername?: string;
    channel?: 'items' | 'presence';
  }) => ({
    schemaVersion: 2,
    eventId: `${type}-${Date.now()}`,
    listId: 'test-list-id',
    channel: channel ?? (type === 'PRESENCE_SNAPSHOT' || type.startsWith('MEMBER_') ? 'presence' : 'items'),
    revision: channel === 'presence' || type === 'PRESENCE_SNAPSHOT' || type.startsWith('MEMBER_')
      ? undefined
      : Date.now(),
    type,
    payload,
    ...(actorId && actorUsername ? { actor: { id: actorId, username: actorUsername } } : {}),
    timestamp: new Date().toISOString(),
  });

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

    (listsApi.getListMembers as any).mockImplementation(() => new Promise(() => {}));

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
      send: mockSend,
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

    expect(mockSubscribe).toHaveBeenCalledWith('test-list-id', 'items', expect.any(Function));
    expect(mockSubscribe).toHaveBeenCalledWith('test-list-id', 'presence', expect.any(Function));
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
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    expect(capturedHandler).not.toBeNull();

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_ADDED',
        payload: newItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
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
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_ADDED',
        payload: newItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
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
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_ADDED',
        payload: ownItem,
        actorId: currentUserId,
        actorUsername: 'testuser',
      }));
    });

    expect(mockShowToast).not.toHaveBeenCalledWith(
      expect.stringContaining('adicionou'),
      'info'
    );
    expect(mockSetItems).not.toHaveBeenCalled();
  });

  it('deve remover item do estado quando recebe ITEM_REMOVED de outro usuário', () => {
    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_REMOVED',
        payload: existingItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria removeu Item Existente', 'info');
  });

  it('deve atualizar item no estado quando recebe ITEM_UPDATED de outro usuário', () => {
    const updatedItem: ListItem = { ...existingItem, name: 'Item Atualizado' };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_UPDATED',
        payload: updatedItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria editou Item Atualizado', 'info');
  });

  it('NÃO deve exibir Toast quando ITEM_UPDATED é do próprio usuário', () => {
    const updatedItem: ListItem = { ...existingItem, name: 'Item Atualizado Próprio' };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_UPDATED',
        payload: updatedItem,
        actorId: currentUserId,
        actorUsername: 'testuser',
      }));
    });

    expect(mockSetItems).not.toHaveBeenCalled();
    expect(mockShowToast).not.toHaveBeenCalledWith(
      expect.stringContaining('editou'), 'info'
    );
  });

  it('deve exibir Toast "marcou" quando recebe ITEM_CHECKED com checked=true de outro usuário', () => {
    const checkedItem: ListItem = { ...existingItem, checked: true };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_CHECKED',
        payload: checkedItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria marcou Item Existente', 'info');
  });

  it('deve aplicar highlight amarelo apenas para ITEM_CHECKED de outro usuário', () => {
    const checkedItem: ListItem = { ...existingItem, checked: true };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_CHECKED',
        payload: checkedItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
    });

    const itemContainer = screen.getByTestId('list-item-item-1');
    const checkbox = screen.getByRole('checkbox');

    expect(itemContainer).toHaveClass('ws-item-checked');
    expect(checkbox).toHaveClass('animate-pop');
  });

  it('deve exibir Toast "desmarcou" quando recebe ITEM_CHECKED com checked=false de outro usuário', () => {
    const uncheckedItem: ListItem = { ...existingItem, checked: false };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_CHECKED',
        payload: uncheckedItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
    });

    expect(mockSetItems).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith('maria desmarcou Item Existente', 'info');
  });

  it('não deve exibir Toast de marcou/desmarcou para ITEM_CHECKED do próprio usuário', () => {
    const ownCheckedItem: ListItem = { ...existingItem, checked: true };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_CHECKED',
        payload: ownCheckedItem,
        actorId: currentUserId,
        actorUsername: 'testuser',
      }));
    });

    expect(mockSetItems).not.toHaveBeenCalled();
    expect(mockShowToast).not.toHaveBeenCalledWith(expect.stringContaining('marcou'), 'info');
    expect(mockShowToast).not.toHaveBeenCalledWith(expect.stringContaining('desmarcou'), 'info');
  });

  it('não deve aplicar highlight amarelo para ITEM_CHECKED do próprio usuário', () => {
    const ownCheckedItem: ListItem = { ...existingItem, checked: true };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_CHECKED',
        payload: ownCheckedItem,
        actorId: currentUserId,
        actorUsername: 'testuser',
      }));
    });

    const itemContainer = screen.getByTestId('list-item-item-1');
    const checkbox = screen.getByRole('checkbox');

    expect(itemContainer).not.toHaveClass('ws-item-checked');
    expect(checkbox).toHaveClass('animate-pop');
  });

  it('deve limpar animações de ITEM_CHECKED em até 300ms (NFR-P1 local)', async () => {
    vi.useFakeTimers();
    const checkedItem: ListItem = { ...existingItem, checked: true };

    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    const start = performance.now();

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'ITEM_CHECKED',
        payload: checkedItem,
        actorId: otherUserId,
        actorUsername: 'maria',
      }));
    });

    const itemContainer = screen.getByTestId('list-item-item-1');
    const checkbox = screen.getByRole('checkbox');

    expect(itemContainer).toHaveClass('ws-item-checked');
    expect(checkbox).toHaveClass('animate-pop');

    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(itemContainer).not.toHaveClass('ws-item-checked');
    expect(checkbox).not.toHaveClass('animate-pop');

    const elapsed = performance.now() - start;
    expect(elapsed).toBeLessThan(500);

    vi.useRealTimers();
  });

  it('deve renderizar Online agora quando recebe MEMBER_ONLINE', async () => {
    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'PRESENCE_SNAPSHOT',
        payload: {
          members: [
            {
              userId: currentUserId,
              username: 'testuser',
              name: 'Test User',
              avatarUrl: null,
            },
          ],
        },
        channel: 'presence',
      }));
      capturedHandler!(createWsMessage({
        type: 'MEMBER_ONLINE',
        payload: {
          userId: otherUserId,
          username: 'maria',
          name: 'Maria',
          avatarUrl: null,
        },
        actorId: otherUserId,
        actorUsername: 'maria',
        channel: 'presence',
      }));
    });

    expect(await screen.findByText('Online agora: 2')).toBeInTheDocument();
  });

  it('deve remover membro online quando recebe MEMBER_OFFLINE', async () => {
    let capturedHandler: ((msg: unknown) => void) | null = null;
    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'PRESENCE_SNAPSHOT',
        payload: {
          members: [
            {
              userId: currentUserId,
              username: 'testuser',
              name: 'Test User',
              avatarUrl: null,
            },
          ],
        },
        channel: 'presence',
      }));
      capturedHandler!(createWsMessage({
        type: 'MEMBER_ONLINE',
        payload: {
          userId: otherUserId,
          username: 'maria',
          name: 'Maria',
          avatarUrl: null,
        },
        actorId: otherUserId,
        actorUsername: 'maria',
        channel: 'presence',
      }));
    });

    expect(await screen.findByText('Online agora: 2')).toBeInTheDocument();

    act(() => {
      capturedHandler!(createWsMessage({
        type: 'MEMBER_OFFLINE',
        payload: {
          userId: otherUserId,
          username: 'maria',
        },
        actorId: otherUserId,
        actorUsername: 'maria',
        channel: 'presence',
      }));
    });

    expect(await screen.findByText('Apenas você online agora')).toBeInTheDocument();
  });

  it('deve enviar heartbeat a cada 30s quando conectado', () => {
    vi.useFakeTimers();

    render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      vi.advanceTimersByTime(30_000);
    });

    expect(mockSend).toHaveBeenCalledWith('/app/list/test-list-id/heartbeat', {});

    vi.useRealTimers();
  });

  it('deve parar heartbeat ao desmontar o componente', () => {
    vi.useFakeTimers();

    const { unmount } = render(<BrowserRouter><ListView /></BrowserRouter>);

    act(() => {
      vi.advanceTimersByTime(30_000);
    });
    expect(mockSend).toHaveBeenCalledWith('/app/list/test-list-id/heartbeat', {});
    mockSend.mockClear();

    unmount();

    act(() => {
      vi.advanceTimersByTime(30_000);
    });
    expect(mockSend).not.toHaveBeenCalledWith('/app/list/test-list-id/heartbeat', {});

    vi.useRealTimers();
  });
});

describe('ListView - Reconnection UX', () => {
  const mockConnect = vi.fn();
  const mockDisconnect = vi.fn();
  const mockSubscribe = vi.fn();
  const mockUnsubscribe = vi.fn();
  const mockSend = vi.fn();
  const mockFetchItems = vi.fn();

  const mockList: ListResponse = {
    id: 'test-list-id',
    name: 'Lista Reconexao',
    type: { id: 1, name: 'Compras', slug: 'compras' },
    owner: { id: 'owner-id', username: 'testuser', name: 'Test User', avatarUrl: null },
    inviteCode: 'RC123',
    isOwner: true,
    itemsCount: 0,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    (listsApi.getListMembers as any).mockImplementation(() => new Promise(() => {}));
    (listsApi.getListState as any).mockResolvedValue({
      listId: 'test-list-id',
      revision: 100,
      updatedAt: new Date().toISOString(),
      itemsCount: 0,
    });

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

    (useItems as any).mockReturnValue({
      items: [],
      setItems: vi.fn(),
      loadingItems: false,
      errorItems: null,
      addingItem: false,
      togglingItemId: null,
      deletingItemId: null,
      fetchItems: mockFetchItems,
      addItem: vi.fn(),
      toggleItem: vi.fn(),
      updateItem: vi.fn(),
      deleteItem: vi.fn(),
      clearItemsError: vi.fn(),
    });

    (useToast as any).mockReturnValue({
      toasts: [],
      showToast: vi.fn(),
      removeToast: vi.fn(),
    });

    (useAuth as any).mockReturnValue({
      user: { id: 'owner-id', username: 'testuser', email: 'test@test.com', displayName: 'Test User' },
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
    });
  });

  it('deve recarregar itens na transicao RECONNECTING -> CONNECTED', async () => {
    let wsStatus: 'RECONNECTING' | 'CONNECTED' = 'RECONNECTING';

    (useWebSocket as any).mockImplementation(() => ({
      status: wsStatus,
      connect: mockConnect,
      disconnect: mockDisconnect,
      subscribe: mockSubscribe,
      unsubscribe: mockUnsubscribe,
      send: mockSend,
    }));

    const { rerender } = render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    mockFetchItems.mockClear();
    (listsApi.getListState as any).mockClear();
    wsStatus = 'CONNECTED';

    rerender(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );
    await Promise.resolve();

    expect(listsApi.getListState).toHaveBeenCalledWith('test-list-id');
    expect(mockFetchItems).toHaveBeenCalledWith('test-list-id');
  });

  it('nao deve recarregar itens na reconexao quando revision do backend nao avancou', async () => {
    let wsStatus: 'RECONNECTING' | 'CONNECTED' = 'CONNECTED';
    let capturedHandler: ((msg: unknown) => void) | null = null;

    (listsApi.getListState as any).mockResolvedValue({
      listId: 'test-list-id',
      revision: 100,
      updatedAt: new Date().toISOString(),
      itemsCount: 1,
    });

    mockSubscribe.mockImplementation((_listId: string, _channel: string, handler: (msg: unknown) => void) => {
      capturedHandler = handler;
    });

    (useWebSocket as any).mockImplementation(() => ({
      status: wsStatus,
      connect: mockConnect,
      disconnect: mockDisconnect,
      subscribe: mockSubscribe,
      unsubscribe: mockUnsubscribe,
      send: mockSend,
    }));

    const { rerender } = render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    expect(capturedHandler).not.toBeNull();

    act(() => {
      capturedHandler!({
        schemaVersion: 2,
        eventId: 'event-1',
        listId: 'test-list-id',
        channel: 'items',
        revision: 100,
        type: 'ITEM_ADDED',
        actor: { id: 'other-user-id', username: 'maria' },
        payload: {
          id: 'item-1',
          name: 'Item 1',
          checked: false,
          quantity: null,
          dueDate: null,
          url: null,
          position: 0,
          createdBy: {
            id: 'other-user-id',
            username: 'maria',
            name: 'Maria',
            avatarUrl: null,
          },
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
        timestamp: new Date().toISOString(),
      });
    });

    mockFetchItems.mockClear();
    (listsApi.getListState as any).mockClear();

    wsStatus = 'RECONNECTING';
    rerender(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );
    await Promise.resolve();

    wsStatus = 'CONNECTED';
    rerender(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );
    await Promise.resolve();

    expect(listsApi.getListState).toHaveBeenCalledWith('test-list-id');
    expect(mockFetchItems).not.toHaveBeenCalled();
  });

  it('deve renderizar ConnectionStatusIndicator com status correto', () => {
    (useWebSocket as any).mockReturnValue({
      status: 'RECONNECTING',
      connect: mockConnect,
      disconnect: mockDisconnect,
      subscribe: mockSubscribe,
      unsubscribe: mockUnsubscribe,
      send: mockSend,
    });

    render(
      <BrowserRouter>
        <ListView />
      </BrowserRouter>
    );

    expect(screen.getByText('Reconectando…')).toBeInTheDocument();
  });
});
