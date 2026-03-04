# Story 5.6: Performance e Otimizações

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a desenvolvedor,
I want garantir performance e otimizações do WebSocket,
so that latência seja < 500ms (NFR-P1) e o sistema escale adequadamente.

## Acceptance Criteria

**AC1 — Latência mesma rede:**
**Given** latência medida entre cliente A e cliente B na mesma rede
**Then** média < 100ms, p95 < 300ms, p99 < 500ms (NFR-P1)

**AC2 — Latência 4G:**
**Given** latência medida em conexão 4G
**Then** média < 300ms, p95 < 500ms, p99 < 1s

**AC3 — Throughput com 10 usuários:**
**Given** teste de 10 usuários enviando 10 itens em 10s
**Then** todas as mensagens entregues, nenhuma perdida, ordem preservada, latência média < 500ms

**AC4 — Throttling de edições rápidas:**
**Given** usuário edita item rapidamente (5x em 1s)
**Then** debounce de 500ms aplicado no frontend (apenas último valor enviado ao backend), sem flood de chamadas REST/WebSocket

**AC5 — Payload minimizado (backend):**
**Given** `SimpMessagingTemplate.convertAndSend` broadcast via Jackson
**Then** payload JSON contém apenas campos necessários, timestamp ISO 8601, sem campos nulos desnecessários

**AC6 — Frontend React otimizado:**
**Given** frontend handler de mensagens WebSocket
**Then** JSON.parse nativo, `useCallback`/`useMemo` aplicados para evitar re-renders desnecessários, render seletivo por item

**AC7 — Conexão única reutilizada:**
**Given** lista subscrita via `WebSocketContext`
**Then** única conexão STOMP reusada para múltiplos subscribes (já implementado — verificar e confirmar)

**AC8 — Métricas no Actuator:**
**Given** GET `/actuator/health`
**Then** status WebSocket incluído (conexões ativas via `PresenceService`)

**AC9 — Limpeza de sessões:**
**Given** sessões WebSocket monitoradas
**Then** sessões inativas não crescem indefinidamente, heartbeat STOMP (10s) limpa conexões órfãs

**AC10 — Debug false em produção:**
**Given** STOMP frontend em produção
**Then** `debug: false` (sem logs no console), heartbeat in/out 10000ms — já implementado, verificar

## Tasks / Subtasks

### Frontend — Throttling de edições rápidas

- [x] Implementar debounce de 500ms em `EditItemModal` ou no handler de submit (AC: 4)
  - [x] Verificar onde edição de item é disparada (provavelmente `frontend/src/components/EditItemModal.tsx` ou similar)
  - [x] Adicionar `useRef` para `debounceTimerRef` com `setTimeout` de 500ms
  - [x] Garantir que ao fechar o modal o timer pendente é cancelado (`clearTimeout`)
  - [x] Alternativa simples: usar `useDebouncedCallback` do `use-debounce` se já instalado, senão implementar manual com `useRef + setTimeout`
  - [x] **VERIFICAR PRIMEIRO:** `npm ls use-debounce` — se não existir, implementar manual

### Frontend — Verificação e otimização React (useMemo/useCallback)

- [x] Auditar `frontend/src/pages/ListView.tsx` para re-renders desnecessários (AC: 6)
  - [x] Verificar se array `items` causa re-render desnecessário — considerar `useMemo` se necessário
  - [x] Verificar se `handleWebSocketMessage` já tem `useCallback` estável (já deve ter)
  - [x] Verificar se `fetchItems` já tem `useCallback` estável (já deve ter)
  - [x] Verificar se componentes filhos (`ListItem`) têm `React.memo` para evitar re-render quando outros itens mudam

- [x] Verificar e confirmar AC10 (debug false em produção) (AC: 10)
  - [x] Em `frontend/src/api/websocket.ts`: `debug` já é condicional (`import.meta.env.DEV`) — confirmar e documentar

- [x] Verificar e confirmar AC7 (conexão única) (AC: 7)
  - [x] `WebSocketContext.tsx` já usa uma conexão STOMP global com múltiplos subscribes via `subscriptionsRef` — confirmar e documentar

### Backend — HealthIndicator para WebSocket

- [x] Criar `WebSocketHealthIndicator` em `backend/.../websocket/` (AC: 8)
  - [x] Implementar `HealthIndicator` do Spring Boot Actuator
  - [x] Injetar `PresenceService` para obter contagem de conexões ativas
  - [x] Retornar `Health.up().withDetail("activeSessions", count)` ou `Health.down()` se `PresenceService` falhar
  - [x] Exemplo:
    ```java
    @Component
    public class WebSocketHealthIndicator implements HealthIndicator {
        private final PresenceService presenceService;

        @Override
        public Health health() {
            try {
                int totalSessions = presenceService.getTotalActiveSessions();
                return Health.up()
                    .withDetail("activeSessions", totalSessions)
                    .build();
            } catch (Exception ex) {
                return Health.down(ex).build();
            }
        }
    }
    ```
  - [x] Adicionar método `getTotalActiveSessions()` em `PresenceService` se não existir
  - [x] **Verificar `application.properties`**: garantir que `/actuator/health` está exposto (`management.endpoints.web.exposure.include=health,info`)

### Backend — Verificação de payload minimizado

- [x] Verificar `WebSocketMessage.java` — configurar Jackson para excluir nulos (AC: 5)
  - [x] Adicionar `@JsonInclude(JsonInclude.Include.NON_NULL)` na classe `WebSocketMessage`
  - [x] Confirmar que `timestamp` usa `Instant` (ISO 8601 via Jackson padrão)
  - [x] Verificar se `payload` (objeto genérico) também tem `@JsonInclude(NON_NULL)` no DTO de item

### Backend — Thread pool do broker WebSocket (opcional, se necessário)

- [x] Avaliar se `configureClientOutboundChannel` precisa de thread pool explícito (AC: 3, 9)
  - [x] Por padrão Spring usa SimpleBroker com 1 thread — suficiente para MVP (10-50 usuários)
  - [x] Se testes mostrarem bottleneck: `registration.taskExecutor().corePoolSize(4).maxPoolSize(10)`
  - [x] **APENAS se necessário** — não over-engineer para MVP

### Testes Frontend

- [x] Criar/atualizar teste para debounce em `EditItemModal.test.tsx` (AC: 4)
  - [x] Teste: editar campo 5x rapidamente → apenas 1 chamada API enviada após 500ms
  - [x] Usar `vi.useFakeTimers()` para controlar tempo nos testes Vitest

### Testes Backend

- [x] Criar `WebSocketHealthIndicatorTest.java` (AC: 8)
  - [x] Teste: `PresenceService` com 3 sessões → `Health.up()` com `activeSessions: 3`
  - [x] Teste: `PresenceService` lança exceção → `Health.down()`

### Verificação final

- [x] Executar todos os testes existentes: `npm test -- --run` (frontend) e `./mvnw test` (backend)
- [ ] Verificar latência manualmente: abrir 2 abas, adicionar item, observar sincronização (AC1, AC2, AC3 — pendente validação manual)

### Review Follow-ups (AI)

- [ ] [AI-Review][HIGH] AC1/AC2/AC3 (latência e throughput) — Validação manual pendente (decisão do product owner: adiada). Quando pronto: abrir 2+ abas do app, adicionar itens, medir latência de sincronização. Para AC3: abrir 10 abas simultâneas. Arquitetura validada por design (SimpleBroker + STOMP heartbeat 10s, conforme Dev Notes).
- [x] [AI-Review][HIGH] Duplicate toast corrigido — `EditItemModal.tsx:93` chamava `showToast('Item atualizado')` após `onSave()`, enquanto `handleSaveEditItem` em `ListView.tsx` já chamava `showToast('Sincronizado')`. Duplo toast visível ao usuário. **Fix aplicado**: removido toast de EditItemModal; removido double-close de `handleSaveEditItem`.

## Dev Notes

### Estado Atual do WebSocket (Stories 5.1-5.5 Completadas)

**O que já existe e NÃO precisa ser alterado:**

```typescript
// websocket.ts — já otimizado:
reconnectDelay: 0,         // ✅ AC11: sem reconexão nativa (backoff manual)
heartbeatIncoming: 10000,  // ✅ AC11: heartbeat 10s (previne timeout Cloudflare)
heartbeatOutgoing: 10000,  // ✅ AC11: heartbeat 10s
debug: (str) => {          // ✅ AC10: debug apenas em DEV
  if (import.meta.env.DEV) console.log('[STOMP]', str);
}
```

```typescript
// WebSocketContext.tsx — já otimizado:
const doSubscribe = useCallback(...)     // ✅ estável
const doReconnect = useCallback(...)     // ✅ estável
const connect = useCallback(...)         // ✅ estável
const disconnect = useCallback(...)      // ✅ estável
const subscribe = useCallback(...)       // ✅ estável
const unsubscribe = useCallback(...)     // ✅ estável
const send = useCallback(...)            // ✅ estável
// subscriptionsRef = única conexão com múltiplos subscribes ✅ AC7
```

**O que precisa ser verificado/implementado:**
1. `WebSocketMessage.java` → `@JsonInclude(NON_NULL)` para payload minimizado
2. `PresenceService` → método `getTotalActiveSessions()` para HealthIndicator
3. `WebSocketHealthIndicator` → novo componente actuator
4. `EditItemModal` → debounce 500ms para edições rápidas
5. `ListView` → `React.memo` em `ListItem` se necessário

### Scope do MVP para esta Story

**Esta story é primariamente de verificação/otimização, não feature nova.** O sistema WebSocket já atinge os requisitos de performance do MVP pela arquitetura adotada:
- SimpleBroker Spring é suficiente para < 100 usuários simultâneos
- STOMP heartbeat 10s previne timeout do Cloudflare Tunnel
- Conexão única por sessão de usuário (não por lista subscrita)
- JSON.parse nativo já usado no `doSubscribe`

**Foco principal:** throttling de edição (AC4), HealthIndicator (AC8), `@JsonInclude(NON_NULL)` (AC5).

### Throttling — Debounce de Edição

Localizar onde edição de item é disparada:

```bash
# Buscar onde updateItem é chamado no frontend
grep -rn "updateItem\|editItem\|onEdit" frontend/src/
```

Implementação manual de debounce (sem dependência nova):

```typescript
// Em EditItemModal.tsx ou similar:
const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

const handleSubmit = (data: ItemFormData) => {
  if (debounceRef.current) clearTimeout(debounceRef.current);
  debounceRef.current = setTimeout(() => {
    onUpdate(data);  // chamada REST real
  }, 500);
};

// Cleanup ao fechar modal:
useEffect(() => {
  return () => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
  };
}, []);
```

**Nota:** Para `EditItemModal`, debounce faz mais sentido em **campos de texto com auto-save** ou **inline editing**. Se a edição apenas ocorre ao clicar "Salvar" (botão), o debounce no botão pode ser feito com um simples flag `isSubmitting` para evitar double-submit. **Verificar o comportamento atual antes de implementar.**

### Backend — WebSocketMessage com @JsonInclude

```java
// WebSocketMessage.java — adicionar:
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {
    private String type;
    private Object payload;
    private String userId;
    private String username;
    private Instant timestamp;
    // ...
}
```

Isso garante que campos `null` (ex: `details` em eventos de presença) não apareçam no JSON.

### Backend — PresenceService e getTotalActiveSessions

```java
// Em PresenceService.java — adicionar:
public int getTotalActiveSessions() {
    return onlineMembers.values().stream()
        .mapToInt(Map::size)
        .sum();
}
// onlineMembers é ConcurrentHashMap<String, ConcurrentHashMap<String, UserInfo>>
// chave externa: listId, chave interna: sessionId
```

### Verificação do application.properties

```properties
# Garantir que actuator health está exposto:
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### Padrão de Testes do Projeto

**Frontend (Vitest + React Testing Library):**
```typescript
// Simular tempo em testes de debounce:
import { vi } from 'vitest';

beforeEach(() => { vi.useFakeTimers(); });
afterEach(() => { vi.useRealTimers(); });

it('debounce aguarda 500ms', async () => {
  // ... render component
  fireEvent.change(input, { target: { value: 'novo nome' } });
  fireEvent.change(input, { target: { value: 'novo nome 2' } });

  // Antes de 500ms: nenhuma chamada
  expect(mockUpdateItem).not.toHaveBeenCalled();

  // Avançar 500ms:
  vi.advanceTimersByTime(500);

  expect(mockUpdateItem).toHaveBeenCalledTimes(1);
  expect(mockUpdateItem).toHaveBeenCalledWith(expect.objectContaining({ name: 'novo nome 2' }));
});
```

**Backend (JUnit 5 + Mockito):**
```java
@ExtendWith(MockitoExtension.class)
class WebSocketHealthIndicatorTest {
    @Mock PresenceService presenceService;
    @InjectMocks WebSocketHealthIndicator healthIndicator;

    @Test
    void deveRetornarUpComSessõesAtivas() {
        when(presenceService.getTotalActiveSessions()).thenReturn(3);
        Health health = healthIndicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals(3, health.getDetails().get("activeSessions"));
    }
}
```

### Estrutura de Arquivos

**Arquivos a verificar (sem modificação esperada):**
- `frontend/src/api/websocket.ts` — debug condicional já implementado
- `frontend/src/contexts/WebSocketContext.tsx` — useCallback já implementado
- `frontend/src/hooks/useWebSocket.ts` — sem mudanças esperadas

**Arquivos a modificar:**
- `backend/.../websocket/WebSocketMessage.java` — adicionar `@JsonInclude(NON_NULL)`
- `backend/.../websocket/PresenceService.java` — adicionar `getTotalActiveSessions()`
- `frontend/src/components/EditItemModal.tsx` (ou similar) — debounce 500ms

**Arquivos novos:**
- `backend/.../websocket/WebSocketHealthIndicator.java`
- `backend/.../websocket/WebSocketHealthIndicatorTest.java`
- `frontend/src/components/EditItemModal.test.tsx` (se não existir) ou atualizar

### Project Structure Notes

- Backend: `backend/src/main/java/br/com/leoferolive/nossalista/websocket/`
- Backend config: `backend/src/main/java/br/com/leoferolive/nossalista/config/`
- Frontend components: `frontend/src/components/`
- Frontend context: `frontend/src/contexts/`
- Frontend API: `frontend/src/api/`

**Padrão de naming do projeto:**
- Java: `PascalCase` para classes, camelCase para métodos
- TypeScript: `camelCase` para funções/variáveis, `PascalCase` para components/interfaces
- Commits: `feat(websocket):`, `feat(frontend):`, `fix(backend):`, `test(frontend):`

### Architecture Compliance

- **Sem Redux** (Decisão #006) — estado via `useReducer`/`useRef` ✅
- **React 19 + TypeScript**: tipagem estrita ✅
- **@stomp/stompjs v7**: `Client` API com `activate()`/`deactivate()` ✅
- **SimpleBroker Spring**: suficiente para MVP (< 100 usuários) ✅
- **@JsonInclude**: Jackson padrão do Spring Boot ✅

### Library Framework Requirements

- **Nenhuma biblioteca nova necessária** — debounce implementado manual com `useRef + setTimeout`
- **Spring Boot Actuator**: já incluído no projeto (health checks no k8s)
- **Jackson**: já configurado via Spring Boot — apenas adicionar `@JsonInclude`
- **Vitest fake timers**: nativo do Vitest, sem instalação adicional

### References

- Story 5.5 (anterior): `_bmad-output/implementation-artifacts/5-5-reconexao-automatica.md`
- Story 5.4: `_bmad-output/implementation-artifacts/5-4-indicadores-online-e-membros.md`
- Epic 5, Story 5.6: `_bmad-output/planning-artifacts/epics.md` linha 1308
- Architecture WebSocket #005: `_bmad-output/planning-artifacts/architecture.md` linha 903
- `PresenceService.java`: `backend/.../websocket/PresenceService.java`
- `WebSocketConfig.java`: `backend/.../config/WebSocketConfig.java`
- `WebSocketContext.tsx`: `frontend/src/contexts/WebSocketContext.tsx`
- `websocket.ts`: `frontend/src/api/websocket.ts`
- Spring Boot Actuator HealthIndicator: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

## Dev Agent Record

### Agent Model Used

openai/gpt-5.3-codex

### Debug Log References

- `npm ls use-debounce` (frontend): dependência não instalada; debounce implementado manualmente.
- `npm test -- EditItemModal.test.tsx --run` (frontend): suíte do modal validada, incluindo debounce de 500ms.
- `./mvnw -Dtest=PresenceServiceTest,WebSocketHealthIndicatorTest,WebSocketMessageTest test` (backend): testes direcionados passando.
- `npm test -- --run` (frontend): 151 testes passando.
- `./mvnw test` (backend): 341 testes executados, 0 falhas, 1 skipped.
- `npm run lint` (frontend): falha por configuração pré-existente (`Cannot find package 'typescript-eslint'`).

### Implementation Plan

- Aplicar debounce manual de 500ms no fluxo de salvar edição de item, com cleanup ao fechar/desmontar o modal.
- Reduzir re-render em lista de itens com memoização do componente de item.
- Adicionar health check de WebSocket no backend com total de sessões ativas via `PresenceService`.
- Minimizar payload de mensagens STOMP com exclusão de campos nulos no envelope WebSocket.
- Cobrir mudanças com testes unitários frontend/backend e rodar suítes completas para regressão.

### Completion Notes List

- ✅ Debounce de 500ms implementado em `EditItemModal`, com cancelamento de timer pendente no close/unmount.
- ✅ `ListItemComponent` passou a usar `React.memo`, reduzindo re-render de itens não alterados.
- ✅ AC10 confirmado: `debug` STOMP segue condicionado a `import.meta.env.DEV` em `frontend/src/api/websocket.ts`.
- ✅ AC7 confirmado: conexão única STOMP já era reutilizada via `WebSocketContext` com múltiplos subscribes.
- ✅ `WebSocketHealthIndicator` criado e integrado ao Actuator, expondo `activeSessions`.
- ✅ `PresenceService#getTotalActiveSessions()` adicionado para agregar sessões ativas entre listas.
- ✅ `WebSocketMessage` atualizado com `@JsonInclude(Include.NON_NULL)` mantendo `Instant` para timestamp ISO 8601.
- ✅ Testes adicionados/atualizados para debounce, health indicator e serialização sem nulos.
- ℹ️ Avaliação de thread pool do broker mantida sem mudança (sem evidência de gargalo para escopo MVP).
- ⚠️ Lint frontend não executou por dependência ausente na configuração atual (`typescript-eslint`), issue pré-existente.

### File List

- `frontend/src/components/EditItemModal.tsx`
- `frontend/src/components/EditItemModal.test.tsx`
- `frontend/src/components/ListItem.tsx`
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/PresenceService.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketMessage.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketHealthIndicator.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/PresenceServiceTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/WebSocketMessageTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/WebSocketHealthIndicatorTest.java`
- `_bmad-output/implementation-artifacts/5-6-performance-e-otimizacoes.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Change Log

- 2026-03-03: Implementadas otimizações de performance do fluxo WebSocket (debounce frontend, memoização de item, health indicator backend e payload sem campos nulos), com cobertura de testes e validação de regressão.
- 2026-03-03: Code review — corrigido toast duplicado ao salvar item (HIGH-4): removido `showToast('Item atualizado')` do EditItemModal e double-close de handleSaveEditItem; adicionado action item para validação de latência (AC1/2/3). Testes atualizados: 11 EditItemModal + 32 ListView passando.
- 2026-03-03: Dev session — suite completa: 151 testes frontend + 341 backend, 0 falhas. Actuator health confirmado (health,info exposto). Validação manual de latência (AC1/2/3) adiada por decisão do product owner — action item registrado para execução futura.
