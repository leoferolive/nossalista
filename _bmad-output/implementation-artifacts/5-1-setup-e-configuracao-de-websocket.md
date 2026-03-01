# Story 5.1: Setup e Configuração de WebSocket

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a desenvolvedor,
I want configurar WebSocket com STOMP sobre SockJS,
So that o sistema possa suportar sincronização em tempo real.

## Acceptance Criteria

**AC1 — Dependências WebSocket no Backend:**
**Given** dependência WebSocket no pom.xml
**Then** `spring-boot-starter-websocket` e `spring-messaging` incluídos (já incluídos via starter websocket)

**AC2 — WebSocketConfig configurado:**
**Given** `WebSocketConfig` configurado
**Then** anotado com `@EnableWebSocketMessageBroker`
**And** `configureMessageBroker`: simple broker em `/topic`, application destination prefix `/app`
**And** STOMP endpoint `/ws` registrado com SockJS habilitado
**And** CORS configurado para mesmas origens do `SecurityConfig` (localhost:5173 e produção)

**AC3 — WebSocketAuthInterceptor:**
**Given** `WebSocketAuthInterceptor` como `HandshakeInterceptor`
**When** cliente inicia conexão WebSocket
**Then** extrai JWT do query param `token` ou do header `Authorization`
**And** valida token via `JwtService.validateToken()`
**And** autentica usuário no `SecurityContext` do WebSocket
**And** rejeita conexão com HTTP 401 se token ausente ou inválido

**AC4 — ChannelInterceptor de autorização de subscribe:**
**Given** `WebSocketSubscriptionInterceptor` como `ChannelInterceptor`
**When** cliente tenta subscribe em `/topic/list/{listId}`
**Then** verifica se usuário autenticado é dono ou membro da lista
**And** lança `MessageDeliveryException` (ou similar) se não autorizado
**And** desconecta cliente não autorizado

**AC5 — Frontend: dependências instaladas:**
**Given** frontend configurado
**Then** `@stomp/stompjs` e `sockjs-client` instalados no `package.json`

**AC6 — Frontend: WebSocketContext criado:**
**Given** `WebSocketContext.tsx` em `frontend/src/contexts/`
**Then** gerencia estado CONNECTED/CONNECTING/DISCONNECTED
**And** cria conexão STOMP via SockJS no endpoint `/ws`
**And** envia `Authorization: Bearer {token}` nos headers STOMP
**And** expõe métodos: `connect(listId)`, `disconnect()`, `subscribe(listId, callback)`, `unsubscribe(listId)`

**AC7 — Frontend: useWebSocket hook:**
**Given** `useWebSocket.ts` em `frontend/src/hooks/`
**Then** hook que consome `WebSocketContext`
**And** expõe estado de conexão e métodos de subscribe/unsubscribe

## Tasks / Subtasks

### Backend

- [x] Adicionar dependência `spring-boot-starter-websocket` ao `pom.xml` (AC: 1)
  - [x] Confirmar que `spring-messaging` é transitivo do starter websocket
- [x] Criar `WebSocketConfig.java` em `config/` (AC: 2)
  - [x] Anotar com `@Configuration` e `@EnableWebSocketMessageBroker`
  - [x] Implementar `configureMessageBroker`: simple broker `/topic`, prefix `/app`
  - [x] Implementar `registerStompEndpoints`: endpoint `/ws` com SockJS e CORS permitindo origens do `SecurityConfig`
  - [x] Registrar `WebSocketAuthInterceptor` como `HandshakeInterceptor`
  - [x] Registrar `WebSocketSubscriptionInterceptor` como `ChannelInterceptor` no inbound channel
- [x] Criar `WebSocketAuthInterceptor.java` em `websocket/` (AC: 3)
  - [x] Implementar `HandshakeInterceptor`
  - [x] Extrair token JWT do query param `token` ou header `Authorization: Bearer {token}`
  - [x] Validar via `JwtService.validateToken()` e carregar usuário via `UserService`
  - [x] Autenticar no `SecurityContext` (atributos do WebSocket)
  - [x] Retornar `false` (HTTP 401) se token inválido ou ausente
- [x] Criar `WebSocketSubscriptionInterceptor.java` em `websocket/` (AC: 4)
  - [x] Implementar `ChannelInterceptor` com `preSend`
  - [x] Verificar `StompCommand.SUBSCRIBE` e destino `/topic/list/{listId}`
  - [x] Extrair `listId` do destination
  - [x] Verificar via `ListMemberRepository` ou `MemberService` se usuário é dono ou membro
  - [x] Lançar exceção se não autorizado (desconectar cliente)
- [x] Criar pacote `websocket/` na estrutura de pacotes principal (AC: 2, 3, 4)
- [x] Atualizar `SecurityConfig` para permitir endpoint `/ws/**` sem JWT HTTP filter (AC: 2)
  - [x] Adicionar `.requestMatchers("/ws/**").permitAll()` antes de `.anyRequest().denyAll()`
  - [x] Nota: a auth WebSocket é feita pelo `WebSocketAuthInterceptor`, não pelo filtro HTTP

### Frontend

- [x] Instalar dependências npm (AC: 5)
  - [x] `npm install @stomp/stompjs sockjs-client`
  - [x] `npm install -D @types/sockjs-client`
- [x] Criar `websocket.ts` em `frontend/src/api/` (AC: 6)
  - [x] Configurar cliente STOMP com SockJS no endpoint `/ws`
  - [x] Exportar factory function `createStompClient(token: string)`
  - [x] Configurar heartbeat: 10000ms in/out
  - [x] Configurar `debug` = false em produção
- [x] Criar `WebSocketContext.tsx` em `frontend/src/contexts/` (AC: 6)
  - [x] Interface `WebSocketState`: `status: 'CONNECTED' | 'CONNECTING' | 'DISCONNECTED'`
  - [x] Interface `WebSocketActions`: `connect`, `disconnect`, `subscribe`, `unsubscribe`
  - [x] Provider gerencia instância STOMP única
  - [x] `connect(listId)`: cria/reutiliza cliente STOMP, autentica com JWT, subscribe ao tópico
  - [x] `disconnect()`: desconecta graciosamente
  - [x] `subscribe(listId, callback)`: subscribe a `/topic/list/{listId}`, recebe `WebSocketMessage`
  - [x] `unsubscribe(listId)`: remove subscription ao sair da lista
  - [x] Headers de conexão: `{ Authorization: 'Bearer ${token}' }`
- [x] Criar `useWebSocket.ts` em `frontend/src/hooks/` (AC: 7)
  - [x] Hook que consome `WebSocketContext` via `useContext`
  - [x] Throw se usado fora de `WebSocketProvider`
  - [x] Expõe `{ status, connect, disconnect, subscribe, unsubscribe }`

### Testes Backend

- [x] Criar `WebSocketConfigTest.java` em `config/` nos testes (AC: 2)
  - [x] Verificar que `@EnableWebSocketMessageBroker` está presente
  - [x] Testar que configuração não lança exceções ao inicializar
- [x] Criar `WebSocketAuthInterceptorTest.java` em `websocket/` nos testes (AC: 3)
  - [x] Token válido no query param → autenticação no SecurityContext
  - [x] Token válido no header → autenticação no SecurityContext
  - [x] Token inválido → retorna false (reject handshake)
  - [x] Sem token → retorna false (reject handshake)

## Dev Notes

### Contexto da Story

Esta story abre o Epic 5 (Sincronização em Tempo Real) — o "momento Aha!" do NossaLista. Nenhuma funcionalidade de WebSocket existe no projeto ainda. Esta story é puramente de **infraestrutura WebSocket**: instalar dependências, configurar o servidor STOMP, criar o interceptor de autenticação e montar o contexto frontend. Nenhum broadcast de item ou ação de usuário é implementado aqui (isso é Epic 5.2+).

**Dependências diretas:**
- Epic 4 completo: `MemberService`, `ListMemberRepository` e `ListService` existem e podem ser usados no `WebSocketSubscriptionInterceptor`.
- `JwtService.validateToken()` e `JwtService.getUserIdFromToken()` existem em `auth/service/JwtService.java`.
- `UserService.findById()` existe em `user/service/UserService.java`.

### Developer Context Section

- **NÃO criar `ListWebSocketController` agora**: ele só será necessário nas stories 5.2+ quando broadcasts de ações existirem. Esta story apenas configura a infraestrutura.
- **WebSocketAuthInterceptor via HandshakeInterceptor**: JWT deve ser validado no handshake HTTP inicial, antes de estabelecer a conexão WebSocket. Isso é diferente de um `ChannelInterceptor` — o handshake é HTTP, então o token pode vir como query param (`/ws?token=...`) ou header `Authorization`.
- **SecurityConfig deve ser atualizado**: O `JwtAuthenticationFilter` (HTTP filter) vai tentar processar `/ws/**` e rejeitar a conexão. Adicionar `/ws/**` ao `permitAll()` para que o auth WebSocket seja tratado pelo `WebSocketAuthInterceptor`.
- **Reutilizar padrões do `AuthContext`**: O `WebSocketContext` frontend deve usar o token do `AuthContext` (via `useAuth()`) para autenticar a conexão STOMP.
- **Única conexão STOMP**: O cliente STOMP deve ser único por sessão de usuário, compartilhado entre múltiplas listas via `WebSocketContext`. Múltiplos subscribes na mesma conexão.
- **SockJS necessário para Cloudflare Tunnel**: O túnel Cloudflare pode não suportar WebSocket nativo em todas as situações. SockJS provê fallback via long-polling, garantindo compatibilidade.
- **`@types/sockjs-client` requerido**: `sockjs-client` é uma biblioteca JavaScript pura. Os tipos TypeScript estão em pacote separado `@types/sockjs-client`.

### Technical Requirements

- **Backend STOMP Config:**
  ```
  Simple Broker: /topic
  App Destination Prefix: /app
  STOMP Endpoint: /ws
  SockJS: habilitado com allowedOrigins
  ```
- **JWT no WebSocket handshake (dois métodos suportados):**
  1. Query param: `wss://api.nossalista.leoferolive.com.br/ws?token=<JWT>`
  2. Header STOMP: `{ Authorization: 'Bearer <JWT>' }` (headers STOMP, não HTTP)
- **Autorização de subscribe:**
  - `/topic/list/{listId}` → verificar via `ListMemberRepository.findByListIdAndUserId()` OU `MemberService`
  - Usuário deve ser OWNER ou MEMBER (qualquer role em list_members)
- **SecurityConfig - endpoint público:**
  ```java
  .requestMatchers("/ws/**").permitAll()  // Auth feita pelo WebSocketAuthInterceptor
  ```

### Architecture Compliance

- **Backend estrutura feature-based:**
  - `config/WebSocketConfig.java` — configuração STOMP (pacote `config/`, igual a `SecurityConfig`)
  - `websocket/WebSocketAuthInterceptor.java` — pacote novo `websocket/`
  - `websocket/WebSocketSubscriptionInterceptor.java` — pacote novo `websocket/`
- **Frontend estrutura por tipo:**
  - `contexts/WebSocketContext.tsx` — contexto global (igual a `AuthContext.tsx`)
  - `hooks/useWebSocket.ts` — hook consumidor (igual a `useAuth.ts`, `useLists.ts`)
  - `api/websocket.ts` — cliente STOMP (igual a `client.ts`, `listsApi.ts`)
- **Padrões obrigatórios:**
  - Sem Redux, sem Zustand — React Context (Decisão #006)
  - `useReducer` para estado complexo de conexão dentro do Context
  - CORS WebSocket deve usar as mesmas origens do `SecurityConfig.corsConfigurationSource()`

### Library Framework Requirements

**Backend — NOVA DEPENDÊNCIA:**
```xml
<!-- pom.xml - adicionar -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```
> `spring-messaging` é dependência transitiva do starter websocket — não adicionar separadamente.

**Frontend — NOVAS DEPENDÊNCIAS:**
```bash
npm install @stomp/stompjs sockjs-client
npm install -D @types/sockjs-client
```
Versões estáveis (março 2026):
- `@stomp/stompjs`: ^7.0.0 (API com `Client`, `StompConfig`, `StompHeaders`)
- `sockjs-client`: ^1.6.1
- `@types/sockjs-client`: ^1.5.4

**API `@stomp/stompjs` v7 (Client API, não StompJS antigo):**
```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('/ws'),
  connectHeaders: { Authorization: `Bearer ${token}` },
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  reconnectDelay: 5000,
  debug: (str) => { if (import.meta.env.DEV) console.log(str); }
});
```
> ⚠️ Usar `Client` de `@stomp/stompjs` v7, NÃO `Stomp.over()` da API antiga. A v7 usa `new Client({})`.

### File Structure Requirements

**Backend — arquivos a criar:**
- `backend/src/main/java/br/com/leoferolive/nossalista/config/WebSocketConfig.java` — NOVO
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketAuthInterceptor.java` — NOVO (pacote novo)
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketSubscriptionInterceptor.java` — NOVO
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/WebSocketAuthInterceptorTest.java` — NOVO
- `backend/src/test/java/br/com/leoferolive/nossalista/config/WebSocketConfigTest.java` — NOVO

**Backend — arquivos a modificar:**
- `backend/pom.xml` — adicionar `spring-boot-starter-websocket`
- `backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java` — adicionar `/ws/**` ao permitAll()

**Frontend — arquivos a criar:**
- `frontend/src/api/websocket.ts` — NOVO (cliente STOMP factory)
- `frontend/src/contexts/WebSocketContext.tsx` — NOVO
- `frontend/src/hooks/useWebSocket.ts` — NOVO

**Frontend — arquivos a modificar:**
- `frontend/package.json` — novas dependências (via npm install)
- `frontend/src/main.tsx` — envolver com `<WebSocketProvider>` (após `AuthProvider`)

### Testing Requirements

- **Backend — unit tests (`WebSocketAuthInterceptorTest`):**
  - Usar `@ExtendWith(MockitoExtension.class)` + `@Mock JwtService`, `@Mock UserService`
  - Simular `ServerHttpRequest` com query params ou headers
  - Verificar que `beforeHandshake()` retorna `true` com token válido e `false` com inválido
  - **NÃO** usar MockMvc (não é endpoint REST)

- **Backend — test de configuração (`WebSocketConfigTest`):**
  - `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` opcional
  - Verificar que bean `WebSocketConfig` carrega sem erros no contexto Spring
  - Verificar presença de `@EnableWebSocketMessageBroker`

- **Frontend — sem testes obrigatórios nesta story:**
  - WebSocketContext é infraestrutura. Testes de integração E2E seriam necessários para verificar a conexão real.
  - Opcional: unit test básico de `useWebSocket` verificando throw fora do Provider.

- **Executar suites completas antes de marcar done:**
  - Backend: `./mvnw test` (todos os 303 testes existentes devem continuar passando)
  - Frontend: `npm test -- --run` (todos os 112 testes existentes devem continuar passando)

### Previous Story Intelligence (Story 4.7)

- **Padrão de injeção de dependências**: construtor explícito sem `@Autowired` (padrão Spring Boot 3+/4). Manter este padrão em `WebSocketAuthInterceptor` e `WebSocketSubscriptionInterceptor`.
- **`MemberService`** (ou `ListMemberRepository`) já existe para verificar ownership/membership. Injetar no `WebSocketSubscriptionInterceptor` para verificar acesso à lista.
- **Convenção de packages**: estrutura feature-based. Criar pacote `websocket/` no mesmo nível de `config/`, `auth/`, `member/`, etc.
- **Suite de testes**: 303 backend + 112 frontend (0 falhas). Garantir que adição de WebSocket não quebra nada existente — especialmente `SecurityConfigTest` e testes de integração de auth.
- **Bug detectado em 4.7**: testes de integração que usam `.with(user(...))` do Spring Security Test vs `SecurityContextHolder` direto. Para testes WebSocket, o `StompSession` será testado diferente (unit tests do interceptor, não integration tests).

### Git Intelligence Summary

- **Padrão de commit**: `feat(websocket): <descrição> (story 5.1)` — manter rastreabilidade por story.
- **Epic 5 é o ponto de inflexão do projeto**: esta story instala as primeiras dependências WebSocket. Commit separado para `pom.xml` e `package.json` pode ajudar no rastreamento.
- **Commits recentes mostram ciclo**: `feat` → code review fixes. Implementar seguindo os padrões revisados de código das stories anteriores.

### Latest Tech Information

**Spring Boot 4.x WebSocket:**
- `spring-boot-starter-websocket` incluído como dependência managed — sem necessidade de especificar versão no pom.xml
- `WebSocketMessageBrokerConfigurer` é a interface a implementar em `WebSocketConfig`
- `HandshakeInterceptor` interface: `beforeHandshake(ServerHttpRequest, ServerHttpResponse, WebSocketHandler, Map)` e `afterHandshake(...)`
- Para `ChannelInterceptor` de autorização, usar `configureClientInboundChannel(ChannelRegistration)` para registrar
- Spring Security + WebSocket: adicionar `WebSocketMessageBrokerSecurityConfig` NÃO é necessário para MVP — o `HandshakeInterceptor` é suficiente para autenticação no handshake HTTP

**@stomp/stompjs v7 vs v6 (CRÍTICO):**
- v7 usa `new Client({...})` ao invés de `Stomp.over(socket)` (API v6/legacy)
- `client.activate()` para conectar, `client.deactivate()` para desconectar
- `client.subscribe('/topic/list/{id}', callback)` retorna `StompSubscription` com `unsubscribe()`
- `client.onConnect` callback quando conexão estabelecida
- `client.onDisconnect` callback quando desconectado
- `client.onStompError` callback para erros STOMP

**SockJS com @stomp/stompjs v7:**
```typescript
webSocketFactory: () => new SockJS(`${import.meta.env.VITE_API_URL}/ws`)
```
> A URL do SockJS deve ser a URL HTTP(S) da API, não wss://. SockJS gerencia o upgrade.

**Cloudflare Tunnel + WebSocket:**
- SockJS com fallback long-polling é crítico para sobreviver ao timeout de 5min do Cloudflare Tunnel em inatividade (Story 5.5 resolve reconnect)
- Configurar `heartbeatIncoming` e `heartbeatOutgoing` em 10000ms (10s) para manter a conexão viva através do tunnel

### References

- `_bmad-output/planning-artifacts/epics.md` — Epic 5, Story 5.1 (linha ~1141)
- `_bmad-output/planning-artifacts/architecture.md` — Decisão #005 (WebSocket Message Format, linha ~903), Decisão #006 (State Management, linha ~1050), seção WebSocket Endpoints (linha ~3351)
- `backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java` — padrões CORS e security chain
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/service/JwtService.java` — validação JWT
- `backend/src/main/java/br/com/leoferolive/nossalista/config/JwtAuthenticationFilter.java` — padrão de extração do JWT do header

## Project Context Reference

- `project-context.md` não encontrado no workspace. Contexto derivado de epics.md, architecture.md e stories anteriores do Epic 4.

## Story Completion Status

- Status: `ready-for-dev`
- Completion note: `Ultimate context engine analysis completed - comprehensive developer guide created`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

Corrigidos 3 `UnnecessaryStubbing` nos testes `WebSocketAuthInterceptorTest`: removidos stubs desnecessários para `request.getHeaders()` em casos onde o token é extraído do query param (early return antes de acessar headers).

### Completion Notes List

- Infraestrutura WebSocket completa: STOMP + SockJS configurado no backend e frontend
- `WebSocketConfig`: simple broker `/topic`, app prefix `/app`, endpoint `/ws` com SockJS e CORS
- `WebSocketAuthInterceptor`: autentica via JWT em query param ou header Authorization no handshake HTTP
- `WebSocketSubscriptionInterceptor`: verifica membership via `ListMemberRepository.existsByListIdAndUserId()` antes de aceitar subscribe em `/topic/list/{listId}`
- `SecurityConfig` atualizado: `/ws/**` em permitAll() (auth WebSocket feita pelo interceptor)
- Frontend: `createStompClient()` factory, `WebSocketContext` com useReducer, `useWebSocket` hook
- `main.tsx` atualizado com `<WebSocketProvider>` envolvendo `<AppRoutes>`
- Testes: 317 backend (0 falhas) + 112 frontend (0 falhas)
- 14 novos testes: WebSocketAuthInterceptorTest (6) + WebSocketConfigTest (2) + WebSocketSubscriptionInterceptorTest (6)
- Code review: `connect()` refatorado para API correta (sem params, token do localStorage); CORS do WebSocket reusa CorsConfigurationSource; SecurityContextHolder.clearContext() no afterHandshake; subscribe() enfileira pendências durante CONNECTING

### File List

**Backend — novos arquivos:**
- `backend/src/main/java/br/com/leoferolive/nossalista/config/WebSocketConfig.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketAuthInterceptor.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketSubscriptionInterceptor.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/config/WebSocketConfigTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/WebSocketAuthInterceptorTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/websocket/WebSocketSubscriptionInterceptorTest.java`

**Backend — arquivos modificados:**
- `backend/pom.xml`
- `backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java`

**Frontend — novos arquivos:**
- `frontend/src/api/websocket.ts`
- `frontend/src/contexts/WebSocketContext.tsx`
- `frontend/src/hooks/useWebSocket.ts`

**Frontend — arquivos modificados:**
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/src/main.tsx`

**BMAD — arquivos modificados:**
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Change Log

- 2026-03-01: Code review adversarial (story 5.1) — corrigidos 2 HIGH e 5 MEDIUM: (1) `connect()` no WebSocketContext agora lê token do localStorage internamente, sem params; (2) criado `WebSocketSubscriptionInterceptorTest` com 6 testes; (3) WebSocketConfig reutiliza `CorsConfigurationSource` do SecurityConfig; (4) `WebSocketAuthInterceptor.afterHandshake` chama `SecurityContextHolder.clearContext()`; (5) subscribe() enfileira subscriptions pendentes durante CONNECTING; (6) File List corrigida (pom.xml movido para modificados, sprint-status.yaml adicionado).
- 2026-03-01: Implementação da Story 5.1 — Setup e Configuração de WebSocket. Infraestrutura STOMP/SockJS completa no backend (WebSocketConfig, WebSocketAuthInterceptor, WebSocketSubscriptionInterceptor) e frontend (websocket.ts, WebSocketContext.tsx, useWebSocket.ts). 8 novos testes adicionados. 311 backend + 112 frontend passando.
