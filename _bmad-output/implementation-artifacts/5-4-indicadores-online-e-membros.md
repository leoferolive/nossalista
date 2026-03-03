# Story 5.4: Indicadores Online e Membros

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário,
I want ver quem está online na lista em tempo real,
so that eu saiba com quem estou colaborando.

## Acceptance Criteria

**AC1 — Registro de sessão no subscribe:**
**Given** WebSocketSession subscribe em `/topic/list/{listId}`
**Then** sessão registrada no listId no `PresenceService`, mapa `Map<UUID, Set<String>>` sessões ativas por lista mantido (listId → Set de sessionIds)

**AC2 — Broadcast MEMBER_ONLINE:**
**Given** usuário faz subscribe em `/topic/list/{listId}`
**Then** evento `MEMBER_ONLINE` enviado para `/topic/list/{listId}`, payload: `{ userId, username, name, avatarUrl }`

**AC3 — Broadcast MEMBER_OFFLINE:**
**Given** cliente desconecta (DISCONNECT) ou faz UNSUBSCRIBE de `/topic/list/{listId}`
**Then** evento `MEMBER_OFFLINE` enviado para `/topic/list/{listId}`, payload: `{ userId, username }`

**AC4 — Exibição de MEMBER_ONLINE no frontend:**
**Given** mensagem `MEMBER_ONLINE` recebida
**Then** avatar aparece na seção "Online agora", bolinha verde (●) visível, nome exibido, contador "X online" atualizado

**AC5 — Remoção de MEMBER_OFFLINE no frontend:**
**Given** mensagem `MEMBER_OFFLINE` recebida
**Then** membro removido da seção "Online agora", bolinha some, contador decrementado

**AC6 — Layout da seção Online na ListView:**
**Given** ListView com pelo menos um membro online
**Then** seção "Online agora: X" visível acima da lista de itens, avatares horizontalmente com overlap, bolinhas verdes

**AC7 — Estado sem outros online:**
**Given** ListView sem outros membros online (só o próprio usuário)
**Then** seção exibe "Apenas você online agora" ou contador mostra "1 online"

**AC8 — Heartbeat mechanism:**
**Given** cliente conectado
**Then** cliente envia mensagem `HEARTBEAT` para `/app/list/{listId}/heartbeat` a cada 30s
**And** servidor registra timestamp do último heartbeat por sessão
**And** sessão encerrada (e MEMBER_OFFLINE emitido) após 60s sem heartbeat (2 heartbeats perdidos)

## Tasks / Subtasks

### Backend — PresenceService

- [x] Criar `backend/src/main/java/.../websocket/PresenceService.java` (AC: 1, 2, 3, 8)
  - [x] Campo `Map<UUID, Map<String, PresenceEntry>>` onde chave=listId, valor=Map<sessionId, PresenceEntry>
  - [x] `PresenceEntry` record: `{ User user, Instant lastHeartbeat }`
  - [x] Método `registerSession(UUID listId, String sessionId, User user)` — adiciona entrada
  - [x] Método `removeSession(UUID listId, String sessionId)` — remove entrada, retorna User se encontrado
  - [x] Método `removeSessionAllLists(String sessionId)` — remove de todas as listas (para DISCONNECT)
  - [x] Método `getOnlineUsers(UUID listId)` — retorna coleção de User para a lista
  - [x] Método `updateHeartbeat(UUID listId, String sessionId)` — atualiza lastHeartbeat
  - [x] Método `evictExpiredSessions(Duration timeout)` — remove sessões expiradas (chamado por scheduler)
  - [x] Usar `ConcurrentHashMap` para thread safety

### Backend — PresenceEventListener

- [x] Criar `backend/src/main/java/.../websocket/PresenceEventListener.java` (AC: 1, 2, 3)
  - [x] Anotado com `@Component`
  - [x] Injetar `PresenceService`, `SimpMessagingTemplate`, `WebSocketSubscriptionInterceptor` (para extrair User do SUBSCRIBE)
  - [x] Método `handleSubscribe(@EventListener SessionSubscribeEvent)` — extrair listId do destination, registrar sessão, broadcast MEMBER_ONLINE
  - [x] Método `handleUnsubscribe(@EventListener SessionUnsubscribeEvent)` — extrair listId, remover sessão, broadcast MEMBER_OFFLINE
  - [x] Método `handleDisconnect(@EventListener SessionDisconnectEvent)` — remover de todas as listas, broadcast MEMBER_OFFLINE para cada lista afetada
  - [x] Filtrar apenas destinations `/topic/list/{listId}` (ignorar outros eventos de subscribe)

### Backend — MemberOnlinePayload DTO

- [x] Criar `backend/src/main/java/.../websocket/dto/MemberOnlinePayload.java` (AC: 2)
  - [x] Record com campos: `userId`, `username`, `name`, `avatarUrl` (tudo String/nullable)

### Backend — MemberOfflinePayload DTO

- [x] Criar `backend/src/main/java/.../websocket/dto/MemberOfflinePayload.java` (AC: 3)
  - [x] Record com campos: `userId` (String), `username` (String)

### Backend — HeartbeatController

- [x] Criar `backend/src/main/java/.../websocket/HeartbeatController.java` (AC: 8)
  - [x] Anotado com `@Controller`
  - [x] `@MessageMapping("/list/{listId}/heartbeat")` — endpoint STOMP
  - [x] Extrair sessionId via `SimpMessageHeaderAccessor.getSessionId()`
  - [x] Chamar `presenceService.updateHeartbeat(listId, sessionId)`

### Backend — HeartbeatScheduler

- [x] Criar `backend/src/main/java/.../websocket/HeartbeatScheduler.java` (AC: 8)
  - [x] Anotado com `@Component`
  - [x] `@Scheduled(fixedDelay = 30_000)` — executar a cada 30s
  - [x] Chamar `presenceService.evictExpiredSessions(Duration.ofSeconds(60))`
  - [x] Para cada sessão expirada: broadcast MEMBER_OFFLINE na lista correspondente
  - [x] Ativar `@EnableScheduling` em `NossaListaApplication` ou `WebSocketConfig`

### Testes Backend

- [x] Criar `PresenceServiceTest.java` (AC: 1, 3, 8)
  - [x] Teste: `registerSession` adiciona entrada corretamente
  - [x] Teste: `removeSession` retorna user e limpa entrada
  - [x] Teste: `removeSessionAllLists` limpa sessão de todas as listas
  - [x] Teste: `evictExpiredSessions` remove apenas sessões expiradas, preserva as válidas
  - [x] Teste: `getOnlineUsers` retorna apenas usuários com sessão ativa

- [x] Criar `PresenceEventListenerTest.java` (AC: 2, 3)
  - [x] Teste: SUBSCRIBE em `/topic/list/{id}` → `registerSession` chamado, MEMBER_ONLINE enviado
  - [x] Teste: DISCONNECT → `removeSessionAllLists` chamado, MEMBER_OFFLINE enviado
  - [x] Teste: SUBSCRIBE em outro destino → sem efeito no PresenceService

### Frontend — Tipos WebSocket

- [x] Atualizar `frontend/src/types/WebSocketMessage.ts` (AC: 4, 5)
  - [x] Adicionar `'MEMBER_ONLINE' | 'MEMBER_OFFLINE'` em `ListWebSocketEventType`
  - [x] Criar interface `MemberOnlinePayload { userId: string; username: string; name: string; avatarUrl: string | null }`
  - [x] Criar interface `MemberOfflinePayload { userId: string; username: string }`

### Frontend — Tipos Online Member

- [x] Criar `frontend/src/types/OnlineMember.ts` (AC: 4, 5, 6)
  - [x] Interface `OnlineMember { userId: string; username: string; name: string; avatarUrl: string | null }`

### Frontend — ListView: estado e handlers online

- [x] Atualizar `frontend/src/pages/ListView.tsx` — estado e WebSocket (AC: 4, 5, 7, 8)
  - [x] Adicionar estado: `const [onlineMembers, setOnlineMembers] = useState<Map<string, OnlineMember>>(new Map())`
  - [x] No `handleWebSocketMessage`, adicionar cases:
    - `MEMBER_ONLINE`: inserir membro em `onlineMembers` (Map<userId, OnlineMember>)
    - `MEMBER_OFFLINE`: remover membro de `onlineMembers` por userId
  - [x] Ao montar (após subscrição): incluir o próprio usuário no `onlineMembers` (self-presence)
  - [x] Ao desmontar: limpar `onlineMembers`
  - [x] Adicionar heartbeat: `useEffect` que chama `sendHeartbeat()` via WebSocketContext a cada 30s enquanto conectado

### Frontend — WebSocketContext: send

- [x] Atualizar `frontend/src/contexts/WebSocketContext.tsx` (AC: 8)
  - [x] Adicionar método `send(destination: string, body: unknown): void` — chama `client.publish({ destination, body: JSON.stringify(body) })`
  - [x] Expor `send` na interface `WebSocketActions` e no `WebSocketContextType`
  - [x] Retornar no Provider value

### Frontend — OnlineMembersBar component

- [x] Criar `frontend/src/components/OnlineMembersBar.tsx` (AC: 6, 7)
  - [x] Props: `members: OnlineMember[]`, `currentUserId: string`
  - [x] Se `members.length === 1` (só o próprio): exibir "Apenas você online agora"
  - [x] Se `members.length > 1`: exibir `"Online agora: X"` + avatares em overlap horizontal
  - [x] Cada avatar: `<img>` com `avatarUrl` ou iniciais do username (fallback)
  - [x] Bolinha verde `w-2.5 h-2.5 bg-green-400 rounded-full` posicionada absolute bottom-0 right-0
  - [x] Overlap: `-ml-2` em avatares após o primeiro, `z-index` crescente
  - [x] Máximo de 5 avatares visíveis + `"+N"` para o excesso

### Testes Frontend

- [x] Criar `frontend/src/components/OnlineMembersBar.test.tsx` (AC: 6, 7)
  - [x] Teste: 1 membro (próprio) → exibe "Apenas você online agora"
  - [x] Teste: 2 membros → exibe "Online agora: 2", dois avatares
  - [x] Teste: 6 membros → exibe 5 avatares + "+1"
  - [x] Teste: avatar com `avatarUrl` null → exibe iniciais

- [x] Atualizar `frontend/src/pages/ListView.test.tsx` (AC: 4, 5)
  - [x] Simular `MEMBER_ONLINE`: verificar que `OnlineMembersBar` aparece com membro correto
  - [x] Simular `MEMBER_OFFLINE`: verificar remoção do membro
  - [x] Heartbeat: verificar que `send` é chamado após 30s de conexão

## Dev Notes

### Contexto da Story

Esta story é **full-stack**. O Epic 5 até aqui focou em sincronização de itens (5.1-5.3). A 5.4 adiciona **presença em tempo real** — exibir quem está online na lista enquanto você colabora.

**O que já existe:**
- `WebSocketSubscriptionInterceptor`: intercepta SUBSCRIBE e verifica autorização. Esse mesmo interceptor será reutilizado para extrair o User no `PresenceEventListener`
- `SimpMessagingTemplate`: já injetado no `ListItemService`, mesmo padrão para broadcast
- `WebSocketConfig`: já configurado com `SimpleMessageBroker`, `@EnableWebSocketMessageBroker`
- Frontend: `WebSocketContext` com connect/disconnect/subscribe/unsubscribe
- Frontend: `handleWebSocketMessage` com `useCallback` em `ListView.tsx` (linhas 120-186)

**O que precisa ser criado:**
- Backend: `PresenceService` + `PresenceEventListener` + `HeartbeatController` + `HeartbeatScheduler`
- Backend: DTOs `MemberOnlinePayload` e `MemberOfflinePayload`
- Frontend: novos tipos, `OnlineMembersBar`, método `send` no WebSocketContext, estado `onlineMembers` na ListView

### Decisão de Design: ApplicationEvents vs SessionAttributes

Spring WebSocket emite `ApplicationEvent`s nativos para eventos de sessão:
- `SessionSubscribeEvent` — disparado após SUBSCRIBE bem-sucedido
- `SessionUnsubscribeEvent` — disparado após UNSUBSCRIBE
- `SessionDisconnectEvent` — disparado após DISCONNECT (inclui todos os subscriptions da sessão)

**Usar `@EventListener` no `PresenceEventListener`** é a abordagem mais limpa — desacoplada do interceptor e sem modificações no `WebSocketSubscriptionInterceptor`.

### Extraindo User nos Events

```java
// Em SessionSubscribeEvent:
StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
String sessionId = sha.getSessionId();
String destination = sha.getDestination(); // "/topic/list/{listId}"
Principal principal = sha.getUser();       // UsernamePasswordAuthenticationToken
User user = extractUser(sha);              // mesmo padrão do WebSocketSubscriptionInterceptor
```

**CRÍTICO:** O `SessionSubscribeEvent` é emitido **após** o `WebSocketSubscriptionInterceptor` já ter validado a autorização, portanto o usuário está garantidamente autorizado ao chegar no listener.

### Padrão de Broadcast MEMBER_ONLINE

```java
// No PresenceEventListener.handleSubscribe():
UUID listId = UUID.fromString(destination.substring("/topic/list/".length()));
presenceService.registerSession(listId, sessionId, user);

MemberOnlinePayload memberPayload = new MemberOnlinePayload(
    user.getId().toString(),
    user.getUsername(),
    user.getName(),
    user.getAvatarUrl()
);

WebSocketMessage message = WebSocketMessage.builder()
    .type("MEMBER_ONLINE")
    .payload(memberPayload)
    .userId(user.getId())
    .username(user.getUsername())
    .timestamp(Instant.now())
    .build();

simpMessagingTemplate.convertAndSend("/topic/list/" + listId, message);
```

### PresenceService — ConcurrentHashMap structure

```java
// Map<listId, Map<sessionId, PresenceEntry>>
private final Map<UUID, Map<String, PresenceEntry>> sessionsByList = new ConcurrentHashMap<>();

record PresenceEntry(User user, Instant lastHeartbeat) {}

public void registerSession(UUID listId, String sessionId, User user) {
    sessionsByList
        .computeIfAbsent(listId, k -> new ConcurrentHashMap<>())
        .put(sessionId, new PresenceEntry(user, Instant.now()));
}

public Optional<User> removeSession(UUID listId, String sessionId) {
    Map<String, PresenceEntry> sessions = sessionsByList.get(listId);
    if (sessions == null) return Optional.empty();
    PresenceEntry entry = sessions.remove(sessionId);
    if (sessions.isEmpty()) sessionsByList.remove(listId);
    return Optional.ofNullable(entry).map(PresenceEntry::user);
}
```

### Heartbeat: @Scheduled

Para ativar o scheduler, adicionar `@EnableScheduling` na classe `NossaListaApplication`:

```java
@SpringBootApplication
@EnableScheduling  // Adicionar esta anotação
public class NossaListaApplication { ... }
```

O `HeartbeatScheduler` chama `presenceService.evictExpiredSessions(Duration.ofSeconds(60))` que retorna uma lista de `(listId, User)` expirados para que o scheduler possa emitir `MEMBER_OFFLINE`.

### Frontend: método `send` no WebSocketContext

```typescript
// Na interface WebSocketActions:
send: (destination: string, body: unknown) => void;

// Implementação no Provider:
const send = useCallback((destination: string, body: unknown) => {
  const client = clientRef.current;
  if (!client?.connected) {
    console.warn('[WebSocket] Tentativa de send sem conexão ativa');
    return;
  }
  client.publish({ destination, body: JSON.stringify(body) });
}, []);
```

### Frontend: Heartbeat em ListView.tsx

```typescript
// Novo useEffect após a subscrição:
useEffect(() => {
  if (wsStatus !== 'CONNECTED' || !id) return;

  const heartbeatInterval = setInterval(() => {
    send(`/app/list/${id}/heartbeat`, {});
  }, 30_000);

  return () => clearInterval(heartbeatInterval);
}, [wsStatus, id, send]);
```

### Frontend: Estado onlineMembers em ListView.tsx

```typescript
const [onlineMembers, setOnlineMembers] = useState<Map<string, OnlineMember>>(new Map());

// No handleWebSocketMessage, adicionar cases:
case 'MEMBER_ONLINE': {
  const p = message.payload as MemberOnlinePayload;
  setOnlineMembers((prev) => new Map(prev).set(p.userId, {
    userId: p.userId,
    username: p.username,
    name: p.name,
    avatarUrl: p.avatarUrl,
  }));
  break;
}
case 'MEMBER_OFFLINE': {
  const p = message.payload as MemberOfflinePayload;
  setOnlineMembers((prev) => {
    const next = new Map(prev);
    next.delete(p.userId);
    return next;
  });
  break;
}
```

**Self-presence**: ao subscrever, adicionar o próprio usuário ao `onlineMembers`:
```typescript
// No useEffect de subscrição:
useEffect(() => {
  if (wsStatus === 'CONNECTED' && id && currentUser) {
    subscribe(id, handleWebSocketMessage);
    // Adicionar self-presence
    setOnlineMembers((prev) => new Map(prev).set(currentUser.id, {
      userId: currentUser.id,
      username: currentUser.username,
      name: currentUser.displayName ?? currentUser.username,
      avatarUrl: currentUser.avatarUrl ?? null,
    }));
    return () => {
      unsubscribe(id);
      setOnlineMembers(new Map()); // Limpar ao sair
    };
  }
  return undefined;
}, [wsStatus, id, subscribe, unsubscribe, handleWebSocketMessage, currentUser]);
```

### Frontend: OnlineMembersBar — posicionamento na ListView

Inserir `<OnlineMembersBar>` logo abaixo do header da lista e acima da lista de itens:

```tsx
{/* Online Members Bar - AC6, AC7 */}
{onlineMembers.size > 0 && (
  <OnlineMembersBar
    members={Array.from(onlineMembers.values())}
    currentUserId={currentUser?.id ?? ''}
  />
)}
```

### Project Structure Notes

- Novos arquivos backend em `backend/src/main/java/br/com/leoferolive/nossalista/websocket/`
- DTOs em subpacote `websocket/dto/`
- Frontend: `OnlineMembersBar.tsx` em `frontend/src/components/`
- Tipo `OnlineMember` em `frontend/src/types/OnlineMember.ts`

### Conflitos e cuidados

- **`@EnableScheduling` em NossaListaApplication** — não duplicar se já existe
- **`handleWebSocketMessage` `useCallback` deps**: adicionar `send` às dependências se `send` não for estável. Setters de `useState` são estáveis; `send` deve usar `useCallback` no WebSocketContext para garantir estabilidade
- **Tipagem do payload**: `ListWebSocketMessage` atual é `WebSocketMessage<ListItem>`. Os novos eventos têm payloads diferentes. Considerar union type ou `WebSocketMessage<unknown>` com type guards para os novos tipos
- **STOMP `send` vs `publish`**: no @stomp/stompjs v7, usar `client.publish()` (não `client.send()` — deprecated)

### Architecture Compliance

- **Sem Redux** (Decisão #006) — `onlineMembers` como `useState<Map>` local ✅
- **`useCallback` para handler**: adicionar `send` às deps ou garantir estabilidade ✅
- **ConcurrentHashMap** no backend para thread-safety com múltiplos WebSocket threads ✅
- **ApplicationEvents** no Spring — desacoplado, não modifica código existente ✅

### Library Framework Requirements

- **Spring `@EventListener`**: nativo do Spring Context, sem dependência nova
- **`@EnableScheduling` + `@Scheduled`**: nativo do Spring, sem dependência nova
- **@stomp/stompjs v7**: `client.publish()` para enviar mensagens do cliente ao servidor
- **React 19**: `useState<Map>` com `new Map(prev)` para imutabilidade ✅

### File Structure Requirements

**Backend — arquivos novos:**
- `backend/src/main/java/.../websocket/PresenceService.java`
- `backend/src/main/java/.../websocket/PresenceEventListener.java`
- `backend/src/main/java/.../websocket/HeartbeatController.java`
- `backend/src/main/java/.../websocket/HeartbeatScheduler.java`
- `backend/src/main/java/.../websocket/dto/MemberOnlinePayload.java`
- `backend/src/main/java/.../websocket/dto/MemberOfflinePayload.java`

**Backend — arquivos modificados:**
- `backend/src/main/java/.../NossaListaApplication.java` — adicionar `@EnableScheduling`

**Frontend — arquivos novos:**
- `frontend/src/types/OnlineMember.ts`
- `frontend/src/components/OnlineMembersBar.tsx`
- `frontend/src/components/OnlineMembersBar.test.tsx`

**Frontend — arquivos modificados:**
- `frontend/src/types/WebSocketMessage.ts` — novos tipos de evento e payloads
- `frontend/src/contexts/WebSocketContext.tsx` — método `send`
- `frontend/src/pages/ListView.tsx` — estado online, casos no handler, heartbeat, componente
- `frontend/src/pages/ListView.test.tsx` — novos testes para presença

### Testing Requirements

**Backend:**
```java
// PresenceServiceTest.java
@Test
void registerSession_addsEntryForList() {
    presenceService.registerSession(listId, "session-1", user);
    assertThat(presenceService.getOnlineUsers(listId)).contains(user);
}

@Test
void removeSession_returnsUserAndClearsEntry() {
    presenceService.registerSession(listId, "session-1", user);
    Optional<User> removed = presenceService.removeSession(listId, "session-1");
    assertThat(removed).contains(user);
    assertThat(presenceService.getOnlineUsers(listId)).isEmpty();
}

@Test
void evictExpiredSessions_removesOnlyExpired() {
    presenceService.registerSession(listId, "expired-session", user);
    // Simular sessão com lastHeartbeat > 60s atrás via reflection ou clock injection
    java.util.List<ExpiredEntry> evicted = presenceService.evictExpiredSessions(Duration.ofSeconds(60));
    assertThat(evicted).hasSize(1);
    assertThat(presenceService.getOnlineUsers(listId)).isEmpty();
}
```

**Frontend:**
```typescript
// OnlineMembersBar.test.tsx
it('exibe "Apenas você online agora" quando só o próprio usuário está online', () => {
  render(<OnlineMembersBar members={[selfMember]} currentUserId={selfMember.userId} />);
  expect(screen.getByText(/Apenas você online agora/)).toBeInTheDocument();
});

it('exibe "Online agora: 2" com dois avatares', () => {
  render(<OnlineMembersBar members={[selfMember, otherMember]} currentUserId={selfMember.userId} />);
  expect(screen.getByText(/Online agora: 2/)).toBeInTheDocument();
  expect(screen.getAllByRole('img')).toHaveLength(2);
});
```

**Executar antes de marcar done:**
- Backend: `./mvnw test` (327 testes existentes + novos devem passar, 0 falhas)
- Frontend: `npm test -- --run` (127 testes existentes + novos devem passar, 0 falhas)

### Previous Story Intelligence (Story 5.3)

**Padrão de `wsCheckedItemIds` / `wsAddedItemIds`** — reutilizar para `onlineMembers`:
- State com `Map` (não `Set`) para armazenar dados ricos por membro
- `new Map(prev)` para imutabilidade, assim como `new Set(prev)` nas stories anteriores

**`handleWebSocketMessage` com `useCallback`** (linha 120, ListView.tsx):
- Dependências atuais: `[currentUser?.id, setItems, showToast]`
- Após esta story: adicionar `send` (se não for estável) e `setOnlineMembers`
- **Nota:** setters de `useState` são estáveis — não precisam estar nas deps do useCallback

**Padrão de `isOwnAction`** (linha 122, ListView.tsx):
- `MEMBER_ONLINE` e `MEMBER_OFFLINE` são eventos de outros usuários — não aplicar filtro de `isOwnAction` (o próprio usuário já está no `onlineMembers` via self-presence)

**Tests estrutura** — seguir padrão de `ListView.test.tsx` para mocking de WebSocket callback:
```typescript
const mockSubscribe = vi.fn((listId, callback) => {
  callback({ type: 'MEMBER_ONLINE', payload: memberPayload, userId: 'other-id', ... });
});
```

### Git Intelligence Summary

**Padrão de commits:**
- `feat(websocket): <descrição> (story 5.4)` para backend
- `feat(frontend): <descrição> (story 5.4)` para frontend
- `test(websocket): <descrição> (story 5.4)` para testes

**Últimos commits relevantes:**
- `af46f04 fix(frontend): separar pop e highlight do ITEM_CHECKED (story 5.3)`
- `9a9c3f1 feat(frontend): sincroniza feedback visual de ITEM_CHECKED (story 5.3)`
- `36990dc feat(global): define globalThis for compatibility and clean up ListView state management`

### Latest Tech Information

**Spring `SessionSubscribeEvent` / `SessionDisconnectEvent`:**
- Disponíveis no `spring-messaging` (já no classpath via `spring-boot-starter-websocket`)
- O evento contém a mensagem STOMP completa — usar `StompHeaderAccessor.wrap(event.getMessage())` para extrair headers
- `SessionDisconnectEvent` tem `getSessionId()` mas **não** tem a lista de subscriptions — por isso o `PresenceService` precisa do mapa `Map<listId, Map<sessionId, ...>>` invertido para lookup por sessionId

**@stomp/stompjs v7 — `client.publish()`:**
```typescript
client.publish({
  destination: '/app/list/{listId}/heartbeat',
  body: JSON.stringify({}),
  headers: {},  // opcional
});
```

**React Map state — imutabilidade:**
- `new Map(prev)` — cria shallow copy do Map (nova referência → trigger de re-render)
- `new Map(prev).set(key, value)` — fluent update
- `new Map(prev)` + `next.delete(key)` — remoção segura

### References

- Epic 5, Story 5.4: `_bmad-output/planning-artifacts/epics.md`
- Story 5.3 (anterior): `_bmad-output/implementation-artifacts/5-3-sincronizacao-de-checkbox-item-check.md`
- `WebSocketSubscriptionInterceptor.java`: `backend/src/main/java/.../websocket/WebSocketSubscriptionInterceptor.java`
- `WebSocketConfig.java`: `backend/src/main/java/.../config/WebSocketConfig.java`
- `WebSocketMessage.java` (backend): `backend/src/main/java/.../websocket/WebSocketMessage.java`
- `WebSocketContext.tsx`: `frontend/src/contexts/WebSocketContext.tsx`
- `WebSocketMessage.ts` (frontend): `frontend/src/types/WebSocketMessage.ts`
- `ListView.tsx`: `frontend/src/pages/ListView.tsx` (handler em linha 120)
- `Item.ts` (tipos): `frontend/src/types/Item.ts`
- Arquitetura: `_bmad-output/planning-artifacts/architecture.md`

## Project Context Reference

- `project-context.md` não encontrado no workspace. Contexto derivado de epics.md, architecture.md e stories anteriores do Epic 5.

## Story Completion Status

- Status: `review`
- Completion note: Presença em tempo real implementada com backend (event listeners, heartbeat, scheduler), frontend (barra de membros online, heartbeat client, novos tipos) e cobertura de testes.

## Dev Agent Record

### Agent Model Used

gpt-5.3-codex

### Debug Log References

- Backend (red): `./mvnw -Dtest=PresenceServiceTest,PresenceEventListenerTest test` (falhou por classes ausentes)
- Backend (green): `./mvnw -Dtest=PresenceServiceTest,PresenceEventListenerTest test` (passou)
- Backend (regressão): `./mvnw test` (passou; sem falhas nos relatórios surefire)
- Frontend (red): `npm test -- --run src/components/OnlineMembersBar.test.tsx src/pages/ListView.test.tsx` (falhou antes da implementação)
- Frontend (green): `npm test -- --run src/components/OnlineMembersBar.test.tsx src/pages/ListView.test.tsx` (passou)
- Frontend (regressão): `npm test -- --run` (137 testes passando)
- Frontend (qualidade): `npm run lint` falhou por dependência ausente no workspace (`typescript-eslint`)

### Completion Notes List

- Implementado `PresenceService` com mapa thread-safe por lista/sessão, controle de heartbeat, expiração e remoção por disconnect/unsubscribe.
- Implementado `PresenceEventListener` para eventos `SUBSCRIBE`, `UNSUBSCRIBE` e `DISCONNECT` com broadcast `MEMBER_ONLINE` / `MEMBER_OFFLINE`.
- Implementado endpoint STOMP `@MessageMapping("/list/{listId}/heartbeat")` e `HeartbeatScheduler` com limpeza automática de sessões expiradas.
- Ativado agendamento com `@EnableScheduling` na aplicação.
- Adicionados DTOs backend `MemberOnlinePayload` e `MemberOfflinePayload`.
- Frontend atualizado com novos tipos de mensagem WebSocket, estado `onlineMembers` na `ListView`, self-presence, remoção on unmount e heartbeat a cada 30s.
- Adicionado método `send` no `WebSocketContext` e propagado no hook `useWebSocket`.
- Criado componente `OnlineMembersBar` com contador, fallback por iniciais, overlap de avatares, limite de 5 avatares e indicador `+N`.
- Testes adicionados/atualizados para backend e frontend cobrindo cenários de presença online/offline e heartbeat.

### File List

- backend/src/main/java/br/com/leoferolive/nossalista/NossaListaApplication.java
- backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketSubscriptionInterceptor.java
- backend/src/main/java/br/com/leoferolive/nossalista/websocket/PresenceService.java
- backend/src/main/java/br/com/leoferolive/nossalista/websocket/PresenceEventListener.java
- backend/src/main/java/br/com/leoferolive/nossalista/websocket/HeartbeatController.java
- backend/src/main/java/br/com/leoferolive/nossalista/websocket/HeartbeatScheduler.java
- backend/src/main/java/br/com/leoferolive/nossalista/websocket/dto/MemberOnlinePayload.java
- backend/src/main/java/br/com/leoferolive/nossalista/websocket/dto/MemberOfflinePayload.java
- backend/src/test/java/br/com/leoferolive/nossalista/websocket/PresenceServiceTest.java
- backend/src/test/java/br/com/leoferolive/nossalista/websocket/PresenceEventListenerTest.java
- frontend/src/types/WebSocketMessage.ts
- frontend/src/types/OnlineMember.ts
- frontend/src/contexts/WebSocketContext.tsx
- frontend/src/hooks/useWebSocket.ts
- frontend/src/components/OnlineMembersBar.tsx
- frontend/src/components/OnlineMembersBar.test.tsx
- frontend/src/pages/ListView.tsx
- frontend/src/pages/ListView.test.tsx
- _bmad-output/implementation-artifacts/5-4-indicadores-online-e-membros.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-03-03: Implementação completa da story 5.4 (presença online + heartbeat + scheduler + UI de membros online + testes backend/frontend) e atualização de status para `review`.
