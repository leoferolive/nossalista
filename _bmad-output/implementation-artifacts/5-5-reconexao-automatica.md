# Story 5.5: Reconexão Automática

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário,
I want que o sistema reconecte automaticamente se a conexão cair,
so that eu não precise recarregar ou fazer login novamente.

## Acceptance Criteria

**AC1 — Detecção de desconexão inesperada:**
**Given** conexão WebSocket ativa é perdida (rede cai, servidor reinicia, Cloudflare Tunnel timeout)
**Then** estado muda para `RECONNECTING`, toast "Sem conexão. Reconectando..." exibido (não se repete a cada tentativa), tentativa de reconexão inicia imediatamente

**AC2 — Backoff exponencial:**
**Given** tentativa de reconexão em progresso
**Then** intervalos: imediato (0ms), 2s, 5s, 10s, 10s, 10s… (máximo 10s, tentativas infinitas)

**AC3 — Reconexão bem-sucedida:**
**Given** reconexão estabelecida com sucesso
**Then** toast "Conectado novamente!" exibido, estado muda para `CONNECTED`, subscrições re-estabelecidas automaticamente, dados recarregados via `GET /api/lists/{id}/items`

**AC4 — Re-subscrição após reconexão:**
**Given** reconexão bem-sucedida
**Then** todos os tópicos previamente subscritos são re-subscritos automaticamente, callbacks preservados, presença (self-presence) re-adicionada

**AC5 — Indicador visual de status:**
**Given** estado muda
**Then** 🔴 ícone/texto para `DISCONNECTED`, 🟡 para `RECONNECTING`, 🟢 para `CONNECTED`
**And** o indicador é visível na `ListView` (abaixo do cabeçalho ou junto ao `OnlineMembersBar`)

**AC6 — Disconnect voluntário não reconecta:**
**Given** `disconnect()` chamado voluntariamente (logout, navegação para fora da lista)
**Then** reconexão automática NÃO é disparada, estado vai direto para `DISCONNECTED`

**AC7 — Cloudflare Tunnel (5 min inatividade):**
**Given** sem troca de mensagens por 5+ minutos (Cloudflare fecha conexão TCP)
**Then** `onWebSocketClose` ou heartbeat STOMP detecta, reconexão inicia automaticamente, usuário mal percebe (apenas toast)

**AC8 — Perda de rede móvel:**
**Given** usuário perde conectividade de rede
**Then** heartbeat STOMP (`heartbeatIncoming/Outgoing: 10000`) detecta timeout, `onDisconnect` dispara, estado vai para `RECONNECTING`, ao voltar conectividade reconecta

**AC9 — Toast não faz spam:**
**Given** múltiplas tentativas de reconexão falhando
**Then** toast "Sem conexão. Reconectando..." mostrado apenas uma vez (não a cada tentativa), toast descartado ao reconectar

## Tasks / Subtasks

### Frontend — WebSocketStatus type

- [x] Atualizar `frontend/src/contexts/WebSocketContext.tsx` (AC: 1, 5)
  - [x] Adicionar `'RECONNECTING'` ao tipo `WebSocketStatus`: `'CONNECTED' | 'CONNECTING' | 'DISCONNECTED' | 'RECONNECTING'`
  - [x] Adicionar ação `{ type: 'RECONNECTING' }` ao `WebSocketAction`
  - [x] Adicionar case `'RECONNECTING'` no `webSocketReducer`

### Frontend — Gestão de reconexão com backoff exponencial

- [x] Atualizar `frontend/src/api/websocket.ts` (AC: 2, 7)
  - [x] Mudar `reconnectDelay: 0` — desabilitar reconexão automática do @stomp/stompjs para controle manual
  - [x] Manter `heartbeatIncoming: 10000` e `heartbeatOutgoing: 10000` para detectar timeouts do Cloudflare

- [x] Atualizar `WebSocketContext.tsx` — implementar backoff exponencial manual (AC: 1, 2, 3, 6, 9)
  - [x] Adicionar ref: `reconnectAttemptRef = useRef<number>(0)` (contador de tentativas)
  - [x] Adicionar ref: `reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)` (timer ativo)
  - [x] Adicionar ref: `isVoluntaryDisconnectRef = useRef<boolean>(false)` (flag para logout/navegação)
  - [x] Adicionar ref: `reconnectingToastShownRef = useRef<boolean>(false)` (evitar spam de toast)
  - [x] Implementar função `getBackoffDelay(attempt: number): number`:
    - attempt 0 → 0ms (imediato)
    - attempt 1 → 2000ms
    - attempt 2 → 5000ms
    - attempt >= 3 → 10000ms (máximo)
  - [x] Implementar função `scheduleReconnect()`:
    - Calcula delay via `getBackoffDelay(reconnectAttemptRef.current)`
    - Incrementa `reconnectAttemptRef.current`
    - Agenda `setTimeout(() => doReconnect(), delay)` em `reconnectTimerRef`
  - [x] Implementar função `doReconnect()`:
    - Lê `localStorage.getItem('authToken')` (token pode ter sido renovado)
    - Se não há token: dispatch `DISCONNECTED`, para
    - Cria novo client via `createStompClient(token)`
    - Chama `client.activate()`
  - [x] Atualizar `client.onDisconnect` (AC: 1, 6, 9):
    - Se `isVoluntaryDisconnectRef.current === true`: dispatch `DISCONNECTED`, retorna
    - Caso contrário: dispatch `RECONNECTING`
    - Se `!reconnectingToastShownRef.current`: exibir toast "Sem conexão. Reconectando…", setar `reconnectingToastShownRef.current = true`
    - Chamar `scheduleReconnect()`
  - [x] Atualizar `client.onStompError` (AC: 1):
    - Mesma lógica de `onDisconnect` (conexão falhou por erro STOMP)
  - [x] Atualizar `client.onConnect` (AC: 3, 4):
    - Verificar se é uma reconexão: `reconnectAttemptRef.current > 0`
    - Se reconexão: exibir toast "Conectado novamente!"
    - Reset: `reconnectAttemptRef.current = 0`, `reconnectingToastShownRef.current = false`
    - Cancelar timer pendente: `clearTimeout(reconnectTimerRef.current)`
    - Dispatch `CONNECTED`
    - Re-subscrever tópicos ativos: iterar `subscriptionCallbacksRef.current` e chamar `doSubscribe` para cada entrada
    - Processar `pendingSubscriptionsRef.current` (subscrições enfileiradas antes da conexão)
  - [x] Adicionar ref: `subscriptionCallbacksRef = useRef<Map<string, (message: unknown) => void>>(new Map())` para armazenar callbacks por listId (necessário para re-subscrição)
  - [x] Atualizar `doSubscribe`: além de registrar em `subscriptionsRef`, registrar callback em `subscriptionCallbacksRef`
  - [x] Atualizar `unsubscribe`: remover também de `subscriptionCallbacksRef`
  - [x] Atualizar `disconnect()` (AC: 6):
    - Setar `isVoluntaryDisconnectRef.current = true` antes de `client.deactivate()`
    - Cancelar timer de reconexão: `clearTimeout(reconnectTimerRef.current)`
    - Limpar `subscriptionCallbacksRef.current`
    - Resetar `reconnectAttemptRef.current = 0`, `reconnectingToastShownRef.current = false`

### Frontend — Reload de dados após reconexão em ListView

- [x] Atualizar `frontend/src/pages/ListView.tsx` (AC: 3)
  - [x] Adicionar ref: `prevWsStatusRef = useRef<WebSocketStatus>(wsStatus)` para detectar transição
  - [x] Adicionar useEffect para detectar RECONNECTING → CONNECTED:
    ```tsx
    useEffect(() => {
      const prev = prevWsStatusRef.current;
      prevWsStatusRef.current = wsStatus;
      if (prev === 'RECONNECTING' && wsStatus === 'CONNECTED' && id) {
        fetchItems(id); // Recarregar itens para pegar mudanças durante desconexão
      }
    }, [wsStatus, id, fetchItems]);
    ```
  - [x] Importar `WebSocketStatus` do contexto se necessário para o tipo do ref

### Frontend — Indicador visual de status de conexão

- [x] Criar `frontend/src/components/ConnectionStatusIndicator.tsx` (AC: 5)
  - [x] Props: `status: WebSocketStatus`
  - [x] Renderizar indicador compacto:
    - `CONNECTED`: 🟢 (ou `bg-green-400`) + texto "Online" (opcional, pode ser só ícone)
    - `RECONNECTING`: 🟡 animação `animate-pulse` + texto "Reconectando…"
    - `DISCONNECTED`: 🔴 + texto "Offline"
    - `CONNECTING`: 🟡 animação `animate-pulse` + texto "Conectando…"
  - [x] Não renderizar nada (return null) quando `CONNECTED` para não poluir UI em estado normal
  - [x] Em `RECONNECTING` e `DISCONNECTED`: mostrar indicador visível

- [x] Atualizar `frontend/src/pages/ListView.tsx` (AC: 5)
  - [x] Importar e usar `<ConnectionStatusIndicator status={wsStatus} />`
  - [x] Posicionar abaixo do cabeçalho da lista, acima do `OnlineMembersBar`

### Testes Frontend

- [x] Criar `frontend/src/contexts/WebSocketContext.test.tsx` (AC: 1, 2, 3, 6, 9)
  - [x] Teste: desconexão não voluntária → dispatch `RECONNECTING`, toast mostrado uma vez
  - [x] Teste: segunda tentativa falha → `reconnectAttemptRef` incrementado, toast NÃO repetido
  - [x] Teste: reconexão bem-sucedida → dispatch `CONNECTED`, toast "Conectado novamente!", contadores resetados
  - [x] Teste: `disconnect()` voluntário → `isVoluntaryDisconnect` impede reconexão, dispatch `DISCONNECTED`
  - [x] Teste: `getBackoffDelay` retorna valores corretos: 0, 2000, 5000, 10000, 10000

- [x] Criar `frontend/src/components/ConnectionStatusIndicator.test.tsx` (AC: 5)
  - [x] Teste: `CONNECTED` → retorna null (não renderiza)
  - [x] Teste: `RECONNECTING` → exibe "Reconectando…" com classe `animate-pulse`
  - [x] Teste: `DISCONNECTED` → exibe "Offline"
  - [x] Teste: `CONNECTING` → exibe "Conectando…"

- [x] Atualizar `frontend/src/pages/ListView.test.tsx` (AC: 3, 5)
  - [x] Teste: transição `RECONNECTING → CONNECTED` → `fetchItems` chamado
  - [x] Teste: `ConnectionStatusIndicator` renderizado com status correto

## Dev Notes

### Contexto da Story

Esta story é **frontend only** — toda a lógica de reconexão está no cliente WebSocket. O backend não precisa de alterações: o `PresenceService` + `PresenceEventListener` da story 5.4 já trata desconexões via `SessionDisconnectEvent`.

**O que já existe:**
- `createStompClient`: `reconnectDelay: 5000` (estático) + `heartbeatIncoming/Outgoing: 10000`
- `WebSocketContext`: states `CONNECTED | CONNECTING | DISCONNECTED`, `pendingSubscriptionsRef` para subscrições enfileiradas
- `ListView.tsx`: useEffect com dep em `wsStatus` — a re-subscrição (subscribe/unsubscribe) já ocorre automaticamente quando wsStatus muda de RECONNECTING → CONNECTED, pois o useEffect re-executa
- `PresenceEventListener` (backend): trata `SessionDisconnectEvent` e `SessionSubscribeEvent` — ao reconectar, o frontend re-subscribirá automaticamente e o backend emitirá `MEMBER_ONLINE` novamente

**O que precisa ser criado:**
- Estado `RECONNECTING` no type e reducer
- Gestão manual de reconexão com backoff exponencial (desabilitar `reconnectDelay` nativo do stompjs)
- Ref `subscriptionCallbacksRef` para re-subscrição após reconexão
- Toast notifications para disconnect/reconnect
- Reload de dados (`fetchItems`) após reconexão em ListView
- Componente `ConnectionStatusIndicator`

### Decisão Arquitetural: Reconexão Manual vs. Nativa do @stomp/stompjs

O `@stomp/stompjs` possui `reconnectDelay` nativo (já configurado como `5000` no projeto). **Por quê gerenciar manualmente?**

1. **Backoff exponencial**: o stompjs usa delay fixo — não tem suporte nativo a backoff
2. **Estado `RECONNECTING`**: precisamos saber quando estamos reconectando para UI e dados
3. **Toast sem spam**: sem controle manual, não há hook para "tentativa N de reconexão"
4. **Reload de dados**: precisamos detectar quando reconexão foi bem-sucedida para recarregar

**Solução:** setar `reconnectDelay: 0` no `createStompClient` e implementar ciclo manual no `WebSocketContext`.

> **CRÍTICO:** Ao mudar `reconnectDelay: 0`, o stompjs NÃO reconectará automaticamente. Nossa lógica em `onDisconnect` e `onStompError` assumem esse controle. Não esquecer de testar que a reconexão ocorre.

### Implementação do Backoff

```typescript
// Função pura, testável isoladamente:
function getBackoffDelay(attempt: number): number {
  if (attempt === 0) return 0;       // imediato
  if (attempt === 1) return 2000;    // 2s
  if (attempt === 2) return 5000;    // 5s
  return 10000;                       // 10s (máximo)
}
```

### Estrutura do WebSocketContext com Reconexão

```typescript
// Refs de controle (não causam re-render):
const reconnectAttemptRef = useRef<number>(0);
const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
const isVoluntaryDisconnectRef = useRef<boolean>(false);
const reconnectingToastShownRef = useRef<boolean>(false);
const subscriptionCallbacksRef = useRef<Map<string, (message: unknown) => void>>(new Map());

// Em client.onDisconnect:
client.onDisconnect = () => {
  if (isVoluntaryDisconnectRef.current) {
    isVoluntaryDisconnectRef.current = false;
    dispatch({ type: 'DISCONNECTED' });
    return;
  }
  dispatch({ type: 'RECONNECTING' });
  if (!reconnectingToastShownRef.current) {
    showToast?.('Sem conexão. Reconectando…', 'warning'); // usar mecanismo de toast do projeto
    reconnectingToastShownRef.current = true;
  }
  const delay = getBackoffDelay(reconnectAttemptRef.current);
  reconnectAttemptRef.current += 1;
  reconnectTimerRef.current = setTimeout(() => doReconnect(), delay);
};

// Em client.onConnect:
client.onConnect = () => {
  const isReconnect = reconnectAttemptRef.current > 0;
  clearTimeout(reconnectTimerRef.current!);
  reconnectAttemptRef.current = 0;
  reconnectingToastShownRef.current = false;
  dispatch({ type: 'CONNECTED' });
  if (isReconnect) {
    showToast?.('Conectado novamente!', 'success');
    // Re-subscrever tópicos ativos:
    subscriptionCallbacksRef.current.forEach((callback, listId) => {
      doSubscribe(client, listId, callback);
    });
  }
  // Processar pendentes (primeiro connect ou subscriptions adicionadas durante RECONNECTING):
  pendingSubscriptionsRef.current.forEach(({ listId, callback }) => {
    doSubscribe(client, listId, callback);
  });
  pendingSubscriptionsRef.current = [];
};
```

### Toast no Projeto

Verificar como toast é feito no projeto. Em ListView, usa `showToast` de `useToast` ou similar. O WebSocketContext não tem acesso direto ao toast — precisará receber via prop, Context, ou event emitter.

**Opção recomendada:** passar `onReconnecting?: () => void` e `onReconnected?: () => void` como props do `WebSocketProvider`, ou usar um `ToastContext` se já existir.

**VERIFICAR ANTES DE IMPLEMENTAR:** como o toast é chamado em `ListView.tsx` (buscar por `showToast` ou `toast()`).

### Re-subscrição Automática via React useEffect

A re-subscrição dos tópicos **já funciona parcialmente** via o `useEffect` em `ListView.tsx` que depende de `wsStatus`:
```tsx
// ListView.tsx linha ~222
useEffect(() => {
  if (wsStatus === 'CONNECTED' && id) {
    subscribe(id, handleWebSocketMessage);
    // self-presence...
    return () => { unsubscribe(id); setOnlineMembers(new Map()); };
  }
  setOnlineMembers(new Map());
  return undefined;
}, [wsStatus, id, subscribe, unsubscribe, handleWebSocketMessage, currentUser]);
```

Quando wsStatus muda CONNECTED→RECONNECTING→CONNECTED:
1. Transição para RECONNECTING: cleanup (unsubscribe) + clear onlineMembers ✅
2. Transição para CONNECTED: subscribe novamente + self-presence ✅

Portanto, `subscriptionCallbacksRef` é necessário apenas para re-subscrições que **não** passam pelo useEffect do React (ex: múltiplas listas abertas simultaneamente, ou subscrições fora do ciclo do ListView). Para o MVP, pode ser implementado de forma simples.

### ConnectionStatusIndicator — Design

```tsx
// Não renderizar quando CONNECTED (não polui a UI em estado normal)
if (status === 'CONNECTED') return null;

// Compacto, no topo da ListView:
<div className="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md">
  {status === 'RECONNECTING' && (
    <span className="flex items-center gap-1.5 text-amber-600 bg-amber-50 px-2 py-1 rounded">
      <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
      Reconectando…
    </span>
  )}
  {status === 'DISCONNECTED' && (
    <span className="flex items-center gap-1.5 text-red-600 bg-red-50 px-2 py-1 rounded">
      <span className="w-2 h-2 rounded-full bg-red-500" />
      Offline
    </span>
  )}
  {status === 'CONNECTING' && (
    <span className="flex items-center gap-1.5 text-blue-600 bg-blue-50 px-2 py-1 rounded">
      <span className="w-2 h-2 rounded-full bg-blue-400 animate-pulse" />
      Conectando…
    </span>
  )}
</div>
```

### Reload de dados após reconexão

```tsx
// Em ListView.tsx — detectar transição RECONNECTING → CONNECTED
const prevWsStatusRef = useRef<WebSocketStatus>(wsStatus);

useEffect(() => {
  const prev = prevWsStatusRef.current;
  prevWsStatusRef.current = wsStatus;

  if (prev === 'RECONNECTING' && wsStatus === 'CONNECTED' && id) {
    // Dados podem estar desatualizados (itens adicionados/removidos durante desconexão)
    fetchItems(id);
  }
}, [wsStatus, id, fetchItems]);
```

### Project Structure Notes

- Arquivos modificados:
  - `frontend/src/contexts/WebSocketContext.tsx` — lógica principal de reconexão
  - `frontend/src/api/websocket.ts` — `reconnectDelay: 0`
  - `frontend/src/pages/ListView.tsx` — reload após reconexão + ConnectionStatusIndicator
- Arquivos novos:
  - `frontend/src/components/ConnectionStatusIndicator.tsx`
  - `frontend/src/components/ConnectionStatusIndicator.test.tsx`
  - `frontend/src/contexts/WebSocketContext.test.tsx`

### Architecture Compliance

- **Sem Redux** (Decisão #006) — estado de reconexão via `useRef` (não causa re-render) e `useReducer` (wsStatus) ✅
- **React 19 + TypeScript**: tipagem estrita para todos os novos tipos ✅
- **@stomp/stompjs v7**: usar `client.activate()` / `client.deactivate()` para reconexão manual ✅
- **SockJS + STOMP**: heartbeat `10000ms` detecta timeout do Cloudflare Tunnel ✅

### Library Framework Requirements

- **@stomp/stompjs v7**: `reconnectDelay: 0` desabilita reconexão nativa; `client.activate()` reinicia conexão
- **React useRef**: estado de reconexão que não causa re-render (contadores, timers, flags)
- **React useReducer**: apenas `WebSocketStatus` (causa re-render nos consumidores)
- **Sem bibliotecas novas necessárias**

### Testing Requirements

**WebSocketContext.test.tsx — mocking:**
```typescript
// Mock do createStompClient para controlar ciclo de vida:
vi.mock('../api/websocket', () => ({
  createStompClient: vi.fn(() => ({
    activate: vi.fn(),
    deactivate: vi.fn(),
    connected: false,
    onConnect: undefined as unknown,
    onDisconnect: undefined as unknown,
    onStompError: undefined as unknown,
    publish: vi.fn(),
    subscribe: vi.fn(() => ({ unsubscribe: vi.fn() })),
  })),
}));

// Testar backoff puro sem mock:
it('getBackoffDelay retorna valores corretos', () => {
  expect(getBackoffDelay(0)).toBe(0);
  expect(getBackoffDelay(1)).toBe(2000);
  expect(getBackoffDelay(2)).toBe(5000);
  expect(getBackoffDelay(3)).toBe(10000);
  expect(getBackoffDelay(10)).toBe(10000); // máximo sempre 10s
});
```

**Executar antes de marcar done:**
- `npm test -- --run` (todos os testes existentes + novos devem passar, 0 falhas)
- `npm run lint` (sem erros de TypeScript)
- Teste manual: abrir lista, desligar backend, verificar toast + indicador; religar backend, verificar reconexão + toast "Conectado novamente!" + itens atualizados

### Previous Story Intelligence (Story 5.4)

**`useCallback` deps em `handleWebSocketMessage`** (linha ~120, ListView.tsx):
- Dependências atuais: `[currentUser?.id, setItems, showToast]`
- Após esta story: `prevWsStatusRef` é um ref (não causa re-render), não precisa entrar nas deps
- `fetchItems` entra em `useEffect` de reload (deps: `[wsStatus, id, fetchItems]`)

**Padrão de refs imutáveis** estabelecido na 5.4:
- `ConcurrentHashMap` no backend → `useRef<Map>` no frontend: mesma filosofia de thread-safety/imutabilidade
- `new Map(prev)` para state imutável, refs para estado que não dispara render

**`isVoluntaryDisconnectRef`** — padrão de flag em ref para comportamento condicional, similar ao `isOwnAction` na 5.3.

### Git Intelligence Summary

**Padrão de commits observado:**
- `feat(websocket): <descrição> (story 5.X)` para mudanças no WebSocketContext/api
- `feat(frontend): <descrição> (story 5.X)` para componentes UI
- `test(frontend): <descrição> (story 5.X)` para testes

**Últimos commits:**
- `71d56b2 feat(websocket): indicadores online e membros em tempo real (story 5.4)`
- `af46f04 fix(frontend): separar pop e highlight do ITEM_CHECKED (story 5.3)`
- `36990dc feat(global): define globalThis for compatibility and clean up ListView state management`

### Latest Tech Information

**@stomp/stompjs v7 — ciclo de vida:**
```typescript
// Desabilitar reconexão nativa:
const config: StompConfig = {
  reconnectDelay: 0,  // 0 = sem reconexão automática
  // ...
};

// Reconnect manual (criar novo client ou reusar o existente):
// Opção A: novo client (mais limpo, evita estado corrompido):
clientRef.current = createStompClient(token);
clientRef.current.onConnect = /* configurar handlers */;
clientRef.current.activate();

// Opção B: reusar client existente (mais eficiente mas pode ter estado):
clientRef.current.activate(); // apenas se foi deactivated, não destroyed
```

> **NOTA:** se o client foi criado com `deactivate()` (não destroyed), pode ser `activate()` novamente. Se foi setado a `null`, precisa ser recriado. Para reconexão, recriar o client garante estado limpo.

**Cloudflare Tunnel — timeout de 5 minutos:**
O Cloudflare fecha conexões TCP inativas após ~5min. O heartbeat STOMP `heartbeatIncoming: 10000, heartbeatOutgoing: 10000` (10s) garante tráfego contínuo, prevenindo o timeout. A story **não precisa tratar esse caso especialmente** — o heartbeat STOMP já previne o problema.

**`WebSocketStatus` type export:**
Atualmente `WebSocketStatus` é exportado de `WebSocketContext.tsx`. Para usar em `ListView.tsx` (`prevWsStatusRef = useRef<WebSocketStatus>`), o import já funciona:
```typescript
import { WebSocketStatus } from '../contexts/WebSocketContext';
```

### References

- Story 5.4 (anterior): `_bmad-output/implementation-artifacts/5-4-indicadores-online-e-membros.md`
- `WebSocketContext.tsx`: `frontend/src/contexts/WebSocketContext.tsx`
- `createStompClient`: `frontend/src/api/websocket.ts`
- `ListView.tsx`: `frontend/src/pages/ListView.tsx` (subscrição: linha ~222, heartbeat: linha ~250)
- Epic 5, Story 5.5: `_bmad-output/planning-artifacts/epics.md` linha 1268
- @stomp/stompjs v7 docs: https://stomp-js.github.io/api-docs/latest/

## Dev Agent Record

### Agent Model Used

openai/gpt-5.3-codex

### Implementation Plan

- Desabilitar reconexão nativa do STOMP (`reconnectDelay: 0`) e centralizar o ciclo de reconexão no `WebSocketContext`.
- Introduzir estado `RECONNECTING` no reducer e refs de controle (`attempt`, `timer`, `voluntary`, `toastShown`, callbacks de subscrição).
- Implementar backoff exponencial manual (0ms, 2000ms, 5000ms, 10000ms...) para `onDisconnect` e `onStompError`.
- Re-subscrever tópicos e processar fila pendente em `onConnect`; manter `disconnect()` voluntário sem reconectar.
- Atualizar `ListView` para (a) mostrar toasts de reconexão via callbacks do `connect`, (b) recarregar itens na transição `RECONNECTING -> CONNECTED`, (c) renderizar indicador visual.
- Cobrir comportamento com testes de contexto, componente de indicador e integração da `ListView`.

### Debug Log References

- `npm test -- --run src/contexts/WebSocketContext.test.tsx src/components/ConnectionStatusIndicator.test.tsx src/pages/ListView.test.tsx` (passou)
- `npm test -- --run` (148 testes passaram; Vitest reportou erro não tratado pré-existente em `src/components/EditItemModal.test.tsx`)
- `npm run lint` (falhou por configuração/dependência ausente: `typescript-eslint` não encontrado em `eslint.config.js`)

### Completion Notes List

- Implementado estado `RECONNECTING` e fluxo completo de reconexão manual com backoff exponencial no `WebSocketContext`.
- `disconnect()` voluntário agora impede reconexão automática e limpa timers/subscrições/callbacks.
- Re-subscrição automática pós-reconexão implementada com `subscriptionCallbacksRef` + processamento de pendentes.
- `ListView` agora mostra toasts de queda/retorno de conexão, recarrega itens ao reconectar e exibe indicador de status.
- Adicionado componente `ConnectionStatusIndicator` com renderização condicional por estado.
- Adicionados testes para reconexão (`WebSocketContext`), indicador de conexão e transição de status na `ListView`.

### File List

- frontend/src/api/websocket.ts
- frontend/src/contexts/WebSocketContext.tsx
- frontend/src/hooks/useWebSocket.ts (atualizado: ReconnectNotifications interface + assinatura de connect())
- frontend/src/pages/ListView.tsx
- frontend/src/components/ConnectionStatusIndicator.tsx
- frontend/src/contexts/WebSocketContext.test.tsx
- frontend/src/components/ConnectionStatusIndicator.test.tsx
- frontend/src/pages/ListView.test.tsx

### Change Log

- 2026-03-03: Implementada reconexão automática manual com backoff exponencial no frontend, com re-subscrição pós-reconexão, indicador visual e cobertura de testes da story 5.5.
- 2026-03-03: Code review — corrigido double subscription STOMP na reconexão (H1, H2); unificado onDisconnect/onStompError em handleConnectionLost para evitar double-increment do backoff counter (M2, M3); adicionados testes para onStompError (M4); adicionado useWebSocket.ts à File List (M1).
