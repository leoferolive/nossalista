import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { WebSocketProvider, getBackoffDelay, useWebSocketContext } from './WebSocketContext';
import { createStompClient } from '../api/websocket';

vi.mock('../api/websocket', () => ({
  createStompClient: vi.fn(),
}));

interface MockClient {
  connected: boolean;
  activate: ReturnType<typeof vi.fn>;
  deactivate: ReturnType<typeof vi.fn>;
  subscribe: ReturnType<typeof vi.fn>;
  publish: ReturnType<typeof vi.fn>;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onStompError?: (frame: unknown) => void;
}

const createdClients: MockClient[] = [];
let latestContext: ReturnType<typeof useWebSocketContext> | null = null;

function createMockClient(): MockClient {
  const client: MockClient = {
    connected: false,
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn(() => ({ unsubscribe: vi.fn() })),
    publish: vi.fn(),
  };
  createdClients.push(client);
  return client;
}

function TestConsumer() {
  const context = useWebSocketContext();
  latestContext = context;

  return <div>{context.status}</div>;
}

describe('WebSocketContext', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    localStorage.setItem('authToken', 'token-teste');
    createdClients.length = 0;
    latestContext = null;
    vi.mocked(createStompClient).mockImplementation(() => createMockClient() as never);
  });

  afterEach(() => {
    vi.useRealTimers();
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('desconexao nao voluntaria muda para RECONNECTING e mostra notificacao uma vez', () => {
    const onReconnecting = vi.fn();

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    );

    act(() => {
      latestContext?.connect({ onReconnecting });
    });

    act(() => {
      createdClients[0].onDisconnect?.();
    });

    expect(screen.getByText('RECONNECTING')).toBeInTheDocument();
    expect(onReconnecting).toHaveBeenCalledTimes(1);

    act(() => {
      vi.advanceTimersByTime(0);
    });

    act(() => {
      createdClients[1].onDisconnect?.();
    });

    expect(onReconnecting).toHaveBeenCalledTimes(1);

    act(() => {
      vi.advanceTimersByTime(1999);
    });
    expect(createdClients).toHaveLength(2);

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(createdClients).toHaveLength(3);
  });

  it('reconexao bem-sucedida volta para CONNECTED, notifica e reseta contadores', () => {
    const onReconnecting = vi.fn();
    const onReconnected = vi.fn();

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    );

    act(() => {
      latestContext?.connect({ onReconnecting, onReconnected });
    });

    act(() => {
      createdClients[0].onDisconnect?.();
      vi.advanceTimersByTime(0);
      createdClients[1].onConnect?.();
    });

    expect(screen.getByText('CONNECTED')).toBeInTheDocument();
    expect(onReconnecting).toHaveBeenCalledTimes(1);
    expect(onReconnected).toHaveBeenCalledTimes(1);

    act(() => {
      createdClients[1].onDisconnect?.();
    });

    act(() => {
      vi.advanceTimersByTime(0);
    });

    expect(createdClients).toHaveLength(3);
  });

  it('disconnect voluntario nao deve reconectar automaticamente', () => {
    const onReconnecting = vi.fn();

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    );

    act(() => {
      latestContext?.connect({ onReconnecting });
    });

    act(() => {
      latestContext?.disconnect();
      createdClients[0].onDisconnect?.();
      vi.runOnlyPendingTimers();
    });

    expect(screen.getByText('DISCONNECTED')).toBeInTheDocument();
    expect(onReconnecting).not.toHaveBeenCalled();
    expect(createdClients).toHaveLength(1);
  });

  it('getBackoffDelay retorna 0, 2000, 5000, 10000, 10000', () => {
    expect(getBackoffDelay(0)).toBe(0);
    expect(getBackoffDelay(1)).toBe(2000);
    expect(getBackoffDelay(2)).toBe(5000);
    expect(getBackoffDelay(3)).toBe(10000);
    expect(getBackoffDelay(10)).toBe(10000);
  });

  it('onStompError nao voluntario muda para RECONNECTING e mostra notificacao uma vez', () => {
    const onReconnecting = vi.fn();

    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    );

    act(() => {
      latestContext?.connect({ onReconnecting });
    });

    act(() => {
      createdClients[0].onStompError?.({});
    });

    expect(screen.getByText('RECONNECTING')).toBeInTheDocument();
    expect(onReconnecting).toHaveBeenCalledTimes(1);

    // Segunda falha (após timer disparar) não deve repetir o toast
    act(() => {
      vi.advanceTimersByTime(0);
    });

    act(() => {
      createdClients[1].onStompError?.({});
    });

    expect(onReconnecting).toHaveBeenCalledTimes(1);
    expect(createdClients).toHaveLength(2);
  });

  it('onStompError e onDisconnect na mesma falha nao devem double-incrementar o backoff', () => {
    render(
      <WebSocketProvider>
        <TestConsumer />
      </WebSocketProvider>
    );

    act(() => {
      latestContext?.connect({});
    });

    // Simula onStompError seguido de onDisconnect (ambos disparam na mesma falha)
    act(() => {
      createdClients[0].onStompError?.({});
      createdClients[0].onDisconnect?.();
    });

    // reconnectAttemptRef deve ter sido incrementado apenas uma vez → delay 0ms
    act(() => {
      vi.advanceTimersByTime(0);
    });

    // Apenas um novo cliente deve ter sido criado (não dois)
    expect(createdClients).toHaveLength(2);
  });
});
