# Story 5.2: Formato de Mensagem e Broadcast de Itens

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a desenvolvedor,
I want implementar o formato Event-Type Envelope e o broadcast automático de ações de itens via WebSocket,
So that todos os clientes conectados a uma lista recebam atualizações de itens em tempo real.

## Acceptance Criteria

**AC1 — Formato Event-Type Envelope:**
**Given** o sistema de broadcast WebSocket
**Then** toda mensagem STOMP publicada em `/topic/list/{listId}` segue o formato JSON:
```json
{
  "type": "ITEM_ADDED",
  "payload": { /* ListItemResponseDTO */ },
  "userId": "uuid-do-usuário-que-agiu",
  "username": "username-do-usuário",
  "timestamp": "2026-03-01T22:00:00Z"
}
```
**And** os valores válidos de `type` para esta story são: `ITEM_ADDED`, `ITEM_UPDATED`, `ITEM_REMOVED`, `ITEM_CHECKED`

**AC2 — Broadcast automático após addItem:**
**Given** `ListItemService.addItem()` executado com sucesso
**Then** broadcast `ITEM_ADDED` publicado em `/topic/list/{listId}`
**And** payload contém o `ListItemResponseDTO` do item criado
**And** `userId` e `username` são do usuário que criou o item

**AC3 — Broadcast automático após updateItem:**
**Given** `ListItemService.updateItem()` executado com sucesso
**Then** broadcast `ITEM_UPDATED` publicado em `/topic/list/{listId}`
**And** payload contém o `ListItemResponseDTO` do item atualizado

**AC4 — Broadcast automático após deleteItem:**
**Given** `ListItemService.deleteItem()` executado com sucesso
**Then** broadcast `ITEM_REMOVED` publicado em `/topic/list/{listId}`
**And** payload contém o `ListItemResponseDTO` do item removido (capturado antes da deleção)

**AC5 — Broadcast automático após toggleItemCheck:**
**Given** `ListItemService.toggleItemCheck()` executado com sucesso
**Then** broadcast `ITEM_CHECKED` publicado em `/topic/list/{listId}`
**And** payload contém o `ListItemResponseDTO` com o novo valor de `checked`

**AC6 — Frontend: WebSocketMessage interface definida:**
**Given** frontend configurado
**Then** interface `WebSocketMessage<T>` definida com campos: `type`, `payload: T`, `userId`, `username`, `timestamp`
**And** tipo discriminado `ListWebSocketMessage` com payload tipado como `ListItem`

**AC7 — Frontend: ListView integrado ao WebSocket:**
**Given** usuário na página `/lists/:id`
**Then** `connect()` chamado ao montar o componente
**And** `subscribe(listId, handler)` chamado quando status for `CONNECTED`
**And** `unsubscribe(listId)` e `disconnect()` chamados ao desmontar

**AC8 — Frontend: ITEM_ADDED tratado:**
**Given** mensagem `ITEM_ADDED` recebida
**Then** item adicionado ao estado `items` sem recarregar da API
**And** animação pulse (300ms) aplicada ao novo item
**And** Toast exibido: `"{username} adicionou {itemName}"`
**And** item do próprio usuário NÃO exibe Toast (sem eco)

**AC9 — Frontend: ITEM_UPDATED tratado:**
**Given** mensagem `ITEM_UPDATED` recebida
**Then** item existente (match por `id`) atualizado no estado `items`
**And** Toast exibido: `"{username} editou {itemName}"`
**And** item do próprio usuário NÃO exibe Toast (sem eco)

**AC10 — Frontend: ITEM_REMOVED tratado:**
**Given** mensagem `ITEM_REMOVED` recebida
**Then** item removido do estado `items`
**And** Toast exibido: `"{username} removeu {itemName}"`
**And** item do próprio usuário NÃO exibe Toast (sem eco)

**AC11 — Frontend: ITEM_CHECKED tratado:**
**Given** mensagem `ITEM_CHECKED` recebida
**Then** campo `checked` do item atualizado no estado `items`
**And** Toast exibido: `"{username} marcou {itemName}"` ou `"{username} desmarcou {itemName}"`
**And** item do próprio usuário NÃO exibe Toast (sem eco)

**AC12 — Latência SLA (NFR-P1):**
**Given** 3+ clientes conectados ao mesmo `/topic/list/{listId}`
**Then** todos recebem o broadcast em < 500ms após a ação REST

## Tasks / Subtasks

### Backend

- [x] Criar `WebSocketMessage.java` em `websocket/` (AC: 1)
  - [x] Record ou classe com campos: `type` (String), `payload` (Object), `userId` (UUID), `username` (String), `timestamp` (Instant)
  - [x] Usar `@Builder` + `@Data` (Lombok) ou record Java
  - [x] Serializa `Instant` como ISO 8601 UTC via `@JsonSerialize` ou configuração global Jackson
- [x] Modificar `ListItemService.java` para injetar `SimpMessagingTemplate` (AC: 2-5)
  - [x] Adicionar `SimpMessagingTemplate` via constructor injection
  - [x] Criar método privado `broadcastItemEvent(String type, ListItemResponseDTO dto, User actor, UUID listId)`
  - [x] Em `addItem()`: chamar `broadcastItemEvent("ITEM_ADDED", result, creator, listId)` após salvar
  - [x] Em `updateItem()`: chamar `broadcastItemEvent("ITEM_UPDATED", result, user, listId)` após salvar
  - [x] Em `toggleItemCheck()`: chamar `broadcastItemEvent("ITEM_CHECKED", result, user, listId)` após salvar
  - [x] Em `deleteItem()`: mapear item para DTO ANTES de deletar, depois chamar `broadcastItemEvent("ITEM_REMOVED", itemDTO, user, listId)`

### Testes Backend

- [x] Criar `WebSocketMessageTest.java` em `websocket/` nos testes (AC: 1)
  - [x] Verificar serialização JSON: campos presentes, timestamp em ISO 8601
- [x] Modificar `ListItemServiceTest.java` (AC: 2-5)
  - [x] Injetar mock de `SimpMessagingTemplate`
  - [x] Verificar `convertAndSend("/topic/list/{listId}", message)` chamado em `addItem()`
  - [x] Verificar type = `ITEM_ADDED`, payload != null, userId e username corretos
  - [x] Verificar `convertAndSend()` chamado em `updateItem()` com type `ITEM_UPDATED`
  - [x] Verificar `convertAndSend()` chamado em `toggleItemCheck()` com type `ITEM_CHECKED`
  - [x] Verificar `convertAndSend()` chamado em `deleteItem()` com type `ITEM_REMOVED`

### Frontend

- [x] Criar `frontend/src/types/WebSocketMessage.ts` (AC: 6)
  - [x] Interface genérica `WebSocketMessage<T>` com campos: `type`, `payload: T`, `userId`, `username`, `timestamp`
  - [x] Tipo discriminado `ListWebSocketMessage` = `WebSocketMessage<ListItem>` com union de types
  - [x] Constante/enum `ListWebSocketEventType` com os 4 valores
- [x] Modificar `ListView.tsx` para integrar WebSocket (AC: 7-11)
  - [x] Importar `useWebSocket` e `useAuth`
  - [x] No mount: chamar `connect()` se `isAuthenticated`
  - [x] Usar `useEffect` com dependência em `status`: quando `status === 'CONNECTED'`, chamar `subscribe(listId, handleWebSocketMessage)`
  - [x] No unmount: chamar `unsubscribe(listId)` e `disconnect()`
  - [x] Implementar `handleWebSocketMessage(message: unknown)`: parse para `ListWebSocketMessage` e dispatch por tipo
  - [x] Handler `ITEM_ADDED`: `setItems(prev => [...prev, payload])`, adicionar classe `ws-item-added` no item novo (pulse 300ms)
  - [x] Handler `ITEM_UPDATED`: `setItems(prev => prev.map(i => i.id === payload.id ? payload : i))`
  - [x] Handler `ITEM_REMOVED`: `setItems(prev => prev.filter(i => i.id !== payload.id))`
  - [x] Handler `ITEM_CHECKED`: `setItems(prev => prev.map(i => i.id === payload.id ? {...i, checked: payload.checked} : i))`
  - [x] Lógica de eco: `if (message.userId !== currentUser.id)` antes de exibir Toast
  - [x] Toasts para cada tipo de evento (ver AC8-11)
- [x] Adicionar CSS animations em `frontend/src/index.css` ou arquivo de estilos (AC: 8)
  - [x] `.ws-item-added { animation: pulse 300ms ease-out; }`
  - [x] `@keyframes pulse { 0% { opacity: 0.6; transform: scale(0.98); } 100% { opacity: 1; transform: scale(1); } }`

### Testes Frontend

- [x] Criar testes para WebSocket message handling em `ListView.test.tsx` (AC: 7-11)
  - [x] Mock `useWebSocket` hook
  - [x] Verificar que `connect()` é chamado ao montar
  - [x] Verificar que `subscribe(listId, handler)` é chamado quando status = CONNECTED
  - [x] Simular mensagem `ITEM_ADDED` e verificar item adicionado ao estado
  - [x] Simular mensagem `ITEM_REMOVED` e verificar item removido
  - [x] Simular mensagem do próprio usuário: verificar que Toast NÃO é exibido

## Dev Notes

### Contexto da Story

Esta story implementa o "momento Aha!" do NossaLista — a sincronização em tempo real de itens entre múltiplos usuários. A infraestrutura WebSocket já existe (Story 5.1), agora o trabalho é:

1. **Backend:** Injetar `SimpMessagingTemplate` no `ListItemService` e fazer broadcast após cada operação CRUD
2. **Frontend:** Integrar o `WebSocketContext` ao `ListView` para receber e aplicar atualizações de outros usuários

**Fluxo de dados completo (dupla via):**
```
Usuário A → REST POST /api/lists/{id}/items → ListItemService.addItem()
  → broadcast ITEM_ADDED → /topic/list/{id}
  → Usuário B (conectado via STOMP) recebe mensagem
  → ListView.handleWebSocketMessage() → setItems([...items, newItem])
  → React re-render com novo item + pulse animation + Toast
```

**Importante:** Não é WebSocket-para-WebSocket. O cliente só FAZ subscrições (subscribe), nunca envia mensagens STOMP. Toda ação parte via REST e o broadcast é disparado pelo `ListItemService` no backend.

### Developer Context Section

#### Padrão de Broadcast no ListItemService

O `ListItemService` já tem TODOs marcados (linha ~103):
```java
// TODO: WebSocket broadcast (Story 5.2)
// broadcastItemAdded(saved, creator);
```

A implementação correta usa `SimpMessagingTemplate`:
```java
private final SimpMessagingTemplate simpMessagingTemplate;

private void broadcastItemEvent(String type, ListItemResponseDTO dto, User actor, UUID listId) {
    WebSocketMessage message = WebSocketMessage.builder()
        .type(type)
        .payload(dto)
        .userId(actor.getId())
        .username(actor.getUsername())
        .timestamp(Instant.now())
        .build();
    simpMessagingTemplate.convertAndSend("/topic/list/" + listId, message);
}
```

#### deleteItem() requer captura antes da deleção

O método `deleteItem()` retorna `void` e deleta o item. Para broadcast `ITEM_REMOVED`, capture o DTO ANTES de deletar:
```java
public void deleteItem(UUID listId, UUID itemId, User user) {
    ListItem item = listItemRepository.findById(itemId)...;
    // Verificar permissões...

    ListItemResponseDTO itemDTO = listItemMapper.toListItemResponseDTO(item); // ANTES
    listItemRepository.delete(item);
    reorderPositions(listId);

    broadcastItemEvent("ITEM_REMOVED", itemDTO, user, listId); // DEPOIS da deleção
}
```

#### WebSocketMessage DTO — Serialização de Instant

O campo `timestamp` é `Instant`. Configure Jackson para serializar como ISO 8601 string. Verifique se `application.properties` já tem:
```properties
spring.jackson.serialization.write-dates-as-timestamps=false
```
Se não, adicione ou use `@JsonSerialize(using = InstantSerializer.class)` no campo.

#### Frontend — Lógica de Eco (Anti-echo)

Quando o usuário faz uma ação (ex: adiciona item via REST), a API retorna o item criado e o estado local já é atualizado. O broadcast via WebSocket chegará também, incluindo para o próprio usuário. Para evitar duplicação/eco:

```typescript
const handleWebSocketMessage = useCallback((raw: unknown) => {
    const message = raw as ListWebSocketMessage;
    const isOwnAction = message.userId === currentUser?.id;

    switch (message.type) {
        case 'ITEM_ADDED':
            if (!isOwnAction) {  // Não duplicar: usuário já tem o item via REST response
                setItems(prev => [...prev, message.payload]);
                showToast(`${message.username} adicionou ${message.payload.name}`);
            }
            break;
        // ... outros casos
    }
}, [currentUser?.id]);
```

#### Frontend — useEffect para subscribe com status CONNECTED

```typescript
useEffect(() => {
    if (status === 'CONNECTED' && listId) {
        subscribe(listId, handleWebSocketMessage);
        return () => unsubscribe(listId);
    }
}, [status, listId, subscribe, unsubscribe, handleWebSocketMessage]);

useEffect(() => {
    connect();
    return () => disconnect();
}, [connect, disconnect]);
```

**CRÍTICO:** Não chamar `subscribe()` dentro do `connect()` callback — use dois `useEffect` separados conforme acima. O estado `status` garante que `subscribe()` só é chamado quando `CONNECTED`.

#### Frontend — WebSocketContext subscribe já retorna unknown

O `subscribe(listId, callback: (message: unknown) => void)` em `WebSocketContext.tsx` já parseia o JSON. O callback recebe o objeto JavaScript tipado como `unknown`. Fazer cast para `ListWebSocketMessage` no handler.

#### Frontend — NÃO usar WebSocketContext.connect() com parâmetros

Após o code review da Story 5.1, `connect()` não tem parâmetros — lê o token do `localStorage.getItem('authToken')` internamente. **NÃO** passar token nem listId:
```typescript
// CORRETO:
connect()

// ERRADO:
connect(listId, token)
```

### Technical Requirements

**Backend:**
- `SimpMessagingTemplate` é injetável via constructor, Spring gerencia o bean automaticamente
- Destination format: `/topic/list/{listId}` (string, não UUID object)
- `WebSocketMessage.type` é String (não enum) para serialização JSON limpa
- `WebSocketMessage.payload` pode ser `Object` — Jackson serializa o `ListItemResponseDTO` corretamente
- `Instant.now()` para timestamp — UTC por padrão

**Frontend:**
- `ListWebSocketMessage` deve ser type discriminado para type-safety no switch
- Animação pulse via CSS class dinâmica — adicionar classe e remover após 300ms via `setTimeout`
- Toast para eventos de outros usuários — usar o `useToast()` hook já existente no ListView
- `currentUser` disponível via `useAuth().user`

### Architecture Compliance

**Backend:**
- `WebSocketMessage.java` no pacote `websocket/` (mesma convenção de `WebSocketAuthInterceptor`)
- Constructor injection sem `@Autowired` (padrão Spring Boot 4)
- `ListItemService` já no pacote `listitem/service/` — apenas adicionar `SimpMessagingTemplate`
- NÃO criar `ListWebSocketController` (não necessário: broadcast via SimpMessagingTemplate é suficiente para este fluxo)

**Frontend:**
- `WebSocketMessage.ts` em `frontend/src/types/` (mesmo padrão de `Item.ts`, `List.ts`)
- Toda lógica WebSocket no `ListView.tsx` via `useWebSocket()` — não criar hooks adicionais
- Sem Redux/Zustand (Decisão #006)
- `useCallback` para `handleWebSocketMessage` (performance, evitar re-subscribe desnecessário)

### Library Framework Requirements

**Backend — SimpMessagingTemplate:**
- Já disponível via `spring-boot-starter-websocket` (adicionado na Story 5.1)
- Import: `org.springframework.messaging.simp.SimpMessagingTemplate`
- Método: `convertAndSend(String destination, Object payload)`
- Jackson serializa automaticamente o objeto para JSON

**Frontend — sem novas dependências:**
- Toda a infraestrutura já está instalada (@stomp/stompjs v7, sockjs-client)
- WebSocketContext já expõe `connect()`, `disconnect()`, `subscribe()`, `unsubscribe()`

### File Structure Requirements

**Backend — novos arquivos:**
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketMessage.java` — DTO de mensagem broadcast

**Backend — arquivos modificados:**
- `backend/src/main/java/br/com/leoferolive/nossalista/listitem/service/ListItemService.java` — injetar SimpMessagingTemplate + 4 chamadas de broadcast
- `backend/src/test/java/br/com/leoferolive/nossalista/listitem/service/ListItemServiceTest.java` — adicionar mocks e verificações de broadcast

**Backend — novos testes:**
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/WebSocketMessageTest.java`

**Frontend — novos arquivos:**
- `frontend/src/types/WebSocketMessage.ts`

**Frontend — arquivos modificados:**
- `frontend/src/pages/ListView.tsx` — integração WebSocket (connect, subscribe, handlers, toasts)
- `frontend/src/index.css` — animação `.ws-item-added` pulse keyframe

### Testing Requirements

**Backend — adicionar testes em `ListItemServiceTest`:**
```java
@Mock
private SimpMessagingTemplate simpMessagingTemplate;

@Test
void addItem_shouldBroadcastItemAdded() {
    // Arrange: mock repositories, mapper, etc.
    // Act: listItemService.addItem(listId, request, user)
    // Assert:
    verify(simpMessagingTemplate).convertAndSend(
        eq("/topic/list/" + listId),
        argThat(msg -> {
            WebSocketMessage m = (WebSocketMessage) msg;
            return "ITEM_ADDED".equals(m.getType())
                && m.getPayload() != null
                && user.getId().equals(m.getUserId())
                && user.getUsername().equals(m.getUsername());
        })
    );
}
```

**Frontend — novos testes em `ListView.test.tsx`:**
- Mock `useWebSocket` retornando `{ status: 'CONNECTED', connect, subscribe, unsubscribe, disconnect }`
- Verificar `connect()` chamado ao montar
- Simular chamada ao `subscribe` callback com mensagem `ITEM_ADDED`:
  ```typescript
  const mockSubscribe = vi.fn((listId, callback) => {
      callback({ type: 'ITEM_ADDED', payload: newItem, userId: 'other-user', username: 'maria', timestamp: '' });
  });
  ```
- Verificar que item aparece na lista após ITEM_ADDED de outro usuário
- Verificar que Toast aparece com texto correto
- Verificar que item do próprio usuário NÃO gera Toast

**Executar suites antes de marcar done:**
- Backend: `./mvnw test` (317 testes + novos devem passar, 0 falhas)
- Frontend: `npm test -- --run` (112 testes + novos devem passar, 0 falhas)

### Previous Story Intelligence (Story 5.1)

- **WebSocket infrastructure completa:** `WebSocketConfig`, `WebSocketAuthInterceptor`, `WebSocketSubscriptionInterceptor` já existem e funcionam
- **`connect()` sem parâmetros:** refatorado no code review de 5.1 — lê token de `localStorage.getItem('authToken')` internamente
- **Pending subscriptions queue:** `subscribe()` durante `CONNECTING` já é enfileirado — mas é mais seguro usar o pattern de `useEffect` com dependência no `status`
- **`SecurityContextHolder.clearContext()`** já é chamado no `afterHandshake` — sem problemas de thread-safety
- **CORS:** WebSocketConfig já reutiliza `CorsConfigurationSource` — sem hardcoding
- **Padrão de injeção:** constructor sem `@Autowired` — seguir em `WebSocketMessage` se for classe com `@Component` (mas provavelmente será apenas um DTO sem anotação Spring)
- **317 backend + 112 frontend** testes passando antes desta story — manter verde

### Git Intelligence Summary

- **Padrão de commit:** `feat(websocket): <descrição> (story 5.2)` — rastreabilidade por story
- **Commits recentes relevantes:**
  - `c185585 feat(websocket): implement WebSocket/STOMP infrastructure` — story 5.1 completa
  - `4fc1f6f feat(member): implement remove member endpoint` — padrão de injeção de dependência

### Latest Tech Information

**Spring SimpMessagingTemplate (Spring Boot 4.x / Spring Framework 6.x):**
- Bean disponível automaticamente quando `@EnableWebSocketMessageBroker` está ativo
- `convertAndSend(String destination, Object payload)` — serializa payload via Jackson registrado no `MessageConverter`
- Thread-safe — pode ser chamado de qualquer thread (inclusive thread de request HTTP)
- Não bloqueia — entrega é assíncrona via simple broker

**Jackson + Instant (Spring Boot 4.x):**
- `JavaTimeModule` registrado automaticamente pelo Spring Boot autoconfigure
- Para serializar como string ISO 8601, checar `spring.jackson.serialization.write-dates-as-timestamps=false` em `application.properties`
- Alternativa anotação: `@JsonFormat(shape = JsonFormat.Shape.STRING)` no campo `timestamp`

**React 19 + useEffect + WebSocket:**
- No React 19 com Strict Mode, `useEffect` dispara duas vezes em dev — o `connect()` pode ser chamado duas vezes. O guard `if (clientRef.current?.connected)` no `WebSocketContext.connect()` já protege contra isso.
- `useCallback` com dependências estáveis evita re-renders e re-subscribe desnecessários

### References

- Architecture Decision #005 (Event-Type Envelope): `_bmad-output/planning-artifacts/architecture.md` seção Decision #005
- Architecture Decision #006 (State Management): `_bmad-output/planning-artifacts/architecture.md` seção Decision #006
- Epic 5, Story 5.2: `_bmad-output/planning-artifacts/epics.md`
- Story 5.1 (infraestrutura WebSocket): `_bmad-output/implementation-artifacts/5-1-setup-e-configuracao-de-websocket.md`
- `WebSocketConfig.java`: `backend/src/main/java/br/com/leoferolive/nossalista/config/WebSocketConfig.java`
- `WebSocketContext.tsx`: `frontend/src/contexts/WebSocketContext.tsx`
- `ListItemService.java`: `backend/src/main/java/br/com/leoferolive/nossalista/listitem/service/ListItemService.java`
- `ListItemResponseDTO.java`: `backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/ListItemResponseDTO.java`
- `ListItemMapper.java`: `backend/src/main/java/br/com/leoferolive/nossalista/listitem/mapper/ListItemMapper.java`
- `ListView.tsx`: `frontend/src/pages/ListView.tsx`
- `ListItem.tsx` (component): `frontend/src/components/ListItem.tsx`
- `Item.ts` (types): `frontend/src/types/Item.ts`

## Project Context Reference

- `project-context.md` não encontrado no workspace. Contexto derivado de epics.md, architecture.md e stories anteriores do Epic 5.

## Story Completion Status

- Status: `review`
- Completion note: `Implementação completa — 325 testes backend e 119 testes frontend passando`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Implementado `WebSocketMessage.java` como POJO com builder pattern manual (sem Lombok — não disponível no projeto)
- `SimpMessagingTemplate` injetado via constructor em `ListItemService` — broadcast após addItem, updateItem, toggleItemCheck, deleteItem
- `deleteItem()` captura DTO antes de deletar (para broadcast ITEM_REMOVED com dados corretos)
- Configuração Jackson adicionada em `application.yml`: `write-dates-as-timestamps: false` para serialização ISO 8601
- Frontend: `useItems` expõe `setItems` para permitir updates diretos do WebSocket
- Frontend: dois `useEffect` separados (connect/disconnect vs subscribe/unsubscribe) conforme padrão da Dev Notes
- Lógica anti-echo: `userId !== currentUser?.id` evita processar eventos do próprio usuário
- Corrigido bug pré-existente: `setRemoveConfirmMemberUsername(null)` removida de `ListView.tsx` (variável inexistente)
- Backend: 325 testes (era 317, +8 novos). Frontend: 119 testes (era 112, +7 novos)

### File List

**Backend — novos arquivos:**
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketMessage.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/WebSocketMessageTest.java`

**Backend — arquivos modificados:**
- `backend/src/main/java/br/com/leoferolive/nossalista/listitem/service/ListItemService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/br/com/leoferolive/nossalista/listitem/service/ListItemServiceTest.java`

**Frontend — novos arquivos:**
- `frontend/src/types/WebSocketMessage.ts`

**Frontend — arquivos modificados:**
- `frontend/src/pages/ListView.tsx`
- `frontend/src/pages/ListView.test.tsx`
- `frontend/src/hooks/useItems.ts`
- `frontend/src/index.css`
