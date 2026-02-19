# Story 4.3: Aceitar Convite via Link (Read-Only Mode)

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a pessoa sem conta (ou não autenticada),
I want visualizar uma lista via link de convite sem criar conta,
So that possa decidir se quero participar antes de me comprometer.

## Acceptance Criteria

**Given** endpoint GET /api/lists/join/{inviteCode} disponível (SEM autenticação requerida)
**When** request sem JWT, invite_code válido e não expirou
**Then** response 200 OK com: id, name, type, owner (public info), items (read-only), inviteCode, expiresAt, mode = "READ_ONLY"

**Given** endpoint GET /api/lists/join/{inviteCode}
**When** invite_code não existe no banco
**Then** response 404 Not Found com RFC 7807 e mensagem "Convite não encontrado"

**Given** endpoint GET /api/lists/join/{inviteCode}
**When** invite_code existe mas invite_expires_at < agora
**Then** response 410 Gone com RFC 7807 e mensagem "Este link de convite expirou. Peça um novo link ao dono da lista."

**Given** JoinList page no frontend (rota pública `/join/:inviteCode`)
**When** carregada via link válido
**Then** header: "Modo Leitura - Entre para Editar", lista visível com nome e tipo
**And** itens exibidos SEM interação (checkbox disabled, sem campo adição, sem botões de ação)
**And** aviso: "Você está visualizando em modo leitura. Entre para colaborar!"
**And** botões "Entrar com Google" e "Entrar com Email" proeminentes no rodapé

**Given** JoinList page com link que expira em menos de 5 minutos
**When** carregada
**Then** aviso adicional: "Este link expira em breve! Entre agora ou peça um novo link."

**Given** JoinList page com link inválido (404)
**When** carregada
**Then** tela de erro: "Convite não encontrado. Este link pode ter sido desativado ou não existe."

**Given** JoinList page com link expirado (410)
**When** carregada
**Then** tela de erro: "Este link de convite expirou. Peça um novo link ao dono da lista."

## Tasks / Subtasks

### Backend (Java/Spring Boot)

- [x] Criar exception InviteExpiredException (AC: 3)
  - [x] Classe em `backend/src/main/java/br/com/leoferolive/nossalista/list/exception/InviteExpiredException.java`
  - [x] Extends RuntimeException, construtor com mensagem
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/list/exception/InviteExpiredException.java`

- [x] Criar DTO JoinListResponse (AC: 1)
  - [x] Record class com: id (UUID), name, type_slug, type_name, owner_username, owner_name, owner_avatar_url, items (List<JoinListItemResponse>), invite_code, expires_at, mode ("READ_ONLY")
  - [x] Record interno JoinListItemResponse: id, name, checked, quantity, due_date, url, position
  - [x] snake_case em todos os campos (padrão do projeto)
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/list/dto/JoinListResponse.java`

- [x] Adicionar handler 410 Gone ao GlobalExceptionHandler (AC: 3)
  - [x] Handler para InviteExpiredException retornando HTTP 410 com RFC 7807
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java`

- [x] Adicionar método findByInviteCode ao ListRepository (AC: 1, 2, 3)
  - [x] `Optional<List> findByInviteCode(String inviteCode)` via Spring Data JPA
  - [x] Adicionado findByInviteCodeWithDetails com JOIN FETCH
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/list/repository/ListRepository.java`

- [x] Implementar ListJoinService.getListByInviteCode() (AC: 1, 2, 3)
  - [x] Buscar lista por invite_code (404 se não encontrada)
  - [x] Validar expiração: se expires_at < agora → throw InviteExpiredException (410)
  - [x] Carregar itens ordenados por position ASC
  - [x] Retornar JoinListResponse com mode = "READ_ONLY"
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/member/service/ListJoinService.java`

- [x] Criar ListJoinController (público, sem @SecurityRequirement) (AC: 1, 2, 3)
  - [x] @RestController sem @SecurityRequirement no nível de classe
  - [x] GET /api/lists/join/{inviteCode} → retorna JoinListResponse
  - [x] SpringDoc annotations: @Operation, @ApiResponses (200, 404, 410)
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/ListJoinController.java`

- [x] Atualizar SecurityConfig para permitir endpoint público (AC: 1)
  - [x] Adicionar `/api/lists/join/**` à lista de endpoints públicos (permitAll)
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java`

### Testing (Backend)

- [x] Testes unitários - ListJoinServiceTest (AC: 1, 2, 3)
  - [x] shouldReturnJoinListResponseWhenInviteCodeValid
  - [x] shouldThrowListNotFoundWhenInviteCodeDoesNotExist
  - [x] shouldThrowInviteExpiredExceptionWhenLinkIsExpired
  - [x] shouldReturnItemsOrderedByPosition
  - [x] shouldReturnReadOnlyMode
  - [x] File: `backend/src/test/java/br/com/leoferolive/nossalista/member/service/ListJoinServiceTest.java`

- [x] Testes de integração - ListJoinControllerIntegrationTest (AC: 1, 2, 3)
  - [x] shouldReturn200WithListDataWhenInviteCodeValid
  - [x] shouldReturn404WhenInviteCodeNotFound
  - [x] shouldReturn410WhenInviteCodeExpired
  - [x] shouldReturn200WithoutAuthentication (endpoint público)
  - [x] shouldReturnItemsInReadOnlyMode
  - [x] File: `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/ListJoinControllerIntegrationTest.java`

### Frontend (React/TypeScript)

- [x] Adicionar tipo JoinListResponse ao types/List.ts (AC: 1)
  - [x] Interface com: id, name, type_slug, type_name, owner_username, owner_name, owner_avatar_url, items, invite_code, expires_at, mode
  - [x] Interface JoinListItem: id, name, checked, quantity, due_date, url, position
  - [x] File: `frontend/src/types/List.ts`

- [x] Adicionar função getListByInviteCode ao listsApi.ts (AC: 1)
  - [x] GET /api/lists/join/{inviteCode} SEM header de autenticação (uso de axios base sem auth interceptor)
  - [x] Retorna JoinListResponse
  - [x] File: `frontend/src/api/listsApi.ts`

- [x] Criar JoinListPage.tsx (AC: 4, 5, 6, 7)
  - [x] Estado: loading, error (null | 'not_found' | 'expired'), listData (JoinListResponse | null)
  - [x] useEffect para carregar lista ao montar
  - [x] Estado de loading com skeleton
  - [x] Estado de erro (404 e 410 com mensagens distintas)
  - [x] View de read-only: header, lista, itens disabled, aviso, CTAs
  - [x] Aviso de expiração próxima (< 5 min)
  - [x] File: `frontend/src/pages/JoinListPage.tsx`

- [x] Adicionar rota pública `/join/:inviteCode` ao main.tsx (AC: 4)
  - [x] Rota SEM ProtectedRoute (pública)
  - [x] Importar JoinListPage
  - [x] File: `frontend/src/main.tsx`

## Dev Notes

### 🎯 CONTEXTO ESSENCIAL

**Propósito da Story:**
Esta story implementa a "primeira experiência" do NossaLista para usuários sem conta. Quando Pedro recebe um link de convite da Mariana via WhatsApp, ele consegue:
1. **Visualizar a lista imediatamente** — sem cadastro, sem login
2. **Entender o que é o NossaLista** — vê a interface, entende o produto
3. **Tomar uma decisão informada** — "quero participar?" antes de criar conta

**Jornada do Usuário:**
```
Pedro recebe link: https://nossalista.leoferolive.com.br/join/ABC123XYZ789
  → Clica no link (Story 4.2 gerou o link)
  → Abre JoinListPage em modo leitura
  → Vê a lista "Mercado Semanal" da Mariana com 5 itens
  → Lê: "Você está visualizando em modo leitura. Entre para colaborar!"
  → Clica "Entrar com Google" (redirecionamento para OAuth2)
  → Autentica com Google
  → Story 4.4: POST /api/lists/join/{code} → Pedro vira membro
  → Redireciona para ListView com acesso completo
```

**Valor de Negócio:**
- Reduz fricção de adoção: zero barreiras para ver o conteúdo
- Converte curiosidade em participação (conversão meta: >30%)
- Diferencial competitivo: Google Keep exige login mesmo para ver

### 🏗️ ARQUITETURA E PADRÕES

**Decisão arquitetural crítica — Controller separado para endpoints públicos:**
O ListController existente tem `@SecurityRequirement(name = "JWT")` no nível de classe, o que documenta que TODOS os seus endpoints requerem JWT. O endpoint de join é público, então DEVE estar em um controller separado para:
1. Não confundir a documentação SpringDoc
2. Separar claramente autenticado vs público
3. Seguir o padrão do projeto (ListController = autenticado, ListJoinController = público)

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── list/
│   ├── controller/
│   │   └── ListController.java (EXISTENTE - não modificar para este endpoint)
│   ├── dto/
│   │   └── JoinListResponse.java (NOVO - record class com snake_case)
│   ├── exception/
│   │   └── InviteExpiredException.java (NOVA)
│   └── repository/
│       └── ListRepository.java (MODIFICAR - adicionar findByInviteCode)
├── member/
│   ├── controller/
│   │   └── ListJoinController.java (NOVO - público, sem @SecurityRequirement)
│   └── service/
│       └── ListJoinService.java (NOVO)
└── config/
    ├── SecurityConfig.java (MODIFICAR - adicionar /api/lists/join/**)
    └── GlobalExceptionHandler.java (MODIFICAR - handler 410 Gone)
```

**Frontend Structure:**
```
frontend/src/
├── pages/
│   └── JoinListPage.tsx (NOVO - tela read-only)
├── api/
│   └── listsApi.ts (MODIFICAR - adicionar getListByInviteCode)
├── types/
│   └── List.ts (MODIFICAR - adicionar JoinListResponse e JoinListItem)
└── main.tsx (MODIFICAR - adicionar rota pública /join/:inviteCode)
```

**API Contract (RESTful Pattern):**
```http
GET /api/lists/join/{inviteCode}
Authorization: (opcional - endpoint público)

Response 200 OK:
{
  "id": "uuid-lista",
  "name": "Mercado Semanal",
  "type_slug": "compras",
  "type_name": "Compras",
  "owner_username": "mariana",
  "owner_name": "Mariana Silva",
  "owner_avatar_url": "https://...",
  "items": [
    {
      "id": "uuid-item",
      "name": "Arroz",
      "checked": false,
      "quantity": 2,
      "due_date": null,
      "url": null,
      "position": 0
    }
  ],
  "invite_code": "ABC123XYZ789",
  "expires_at": "2026-02-20T15:30:00",
  "mode": "READ_ONLY"
}

Response 404 Not Found (RFC 7807):
{
  "type": "https://api.nossalista.com/docs/errors/invite-not-found",
  "title": "Convite não encontrado",
  "status": 404,
  "detail": "Convite não encontrado. Este link pode ter sido desativado ou não existe.",
  "instance": "/api/lists/join/{inviteCode}"
}

Response 410 Gone (RFC 7807):
{
  "type": "https://api.nossalista.com/docs/errors/invite-expired",
  "title": "Link de convite expirado",
  "status": 410,
  "detail": "Este link de convite expirou. Peça um novo link ao dono da lista.",
  "instance": "/api/lists/join/{inviteCode}"
}
```

**⚠️ ATENÇÃO HTTP 410 Gone:**
HTTP 410 é diferente de 404:
- **404 Not Found**: Recurso não existe ou não foi encontrado (pode existir em outro lugar)
- **410 Gone**: Recurso existiu mas foi permanentemente removido/expirado

Para invite links expirados, 410 é semanticamente correto (link existiu, mas não é mais válido).
Spring Boot suporta nativamente via `HttpStatus.GONE`.

### 🔐 SEGURANÇA E VALIDAÇÃO

**SecurityConfig — Adição crítica:**
```java
// SecurityConfig.java — ADICIONAR esta linha ANTES do ".requestMatchers("/api/**").authenticated()"
.requestMatchers("/api/lists/join/**").permitAll()

// Ordem importa! A regra mais específica deve vir ANTES da regra genérica
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**", "/api/health", "/actuator/health").permitAll()
    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
    .requestMatchers("/api/lists/join/**").permitAll()  // ← ADICIONAR AQUI
    .requestMatchers("/api/**").authenticated()
    .anyRequest().denyAll()
)
```

**Dados expostos no endpoint público:**
- ✅ Expostos: id, name, type, owner (username/name/avatar), items (sem created_by email)
- ❌ NÃO expor: owner email, owner auth_provider, lista completa de membros, tokens, IDs internos de outros members
- ❌ NÃO expor: `created_by` como objeto completo (apenas nome visível nos itens, se necessário)

**Validação de expiração:**
```java
// ListJoinService.getListByInviteCode()
public JoinListResponse getListByInviteCode(String inviteCode) {
    List list = listRepository.findByInviteCode(inviteCode)
        .orElseThrow(() -> new ListNotFoundException("Convite não encontrado"));

    if (list.getInviteExpiresAt() == null ||
        list.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
        throw new InviteExpiredException(
            "Este link de convite expirou. Peça um novo link ao dono da lista."
        );
    }

    // Carregar itens para o response
    // ... mapear para JoinListResponse
}
```

**Proteção contra enumeração (importante):**
O invite_code tem 12 caracteres alfanuméricos (A-Z, 0-9) = 36^12 ≈ 4.7 × 10^18 combinações. Força bruta não é viável. A validação simples é suficiente.

**Error Handling (RFC 7807):**
- 404 Not Found: Invite code não existe (`ListNotFoundException`)
- 410 Gone: Link expirado (`InviteExpiredException`) — NOVO handler no GlobalExceptionHandler
- Erros genéricos (5xx): Já tratados pelo handler existente

**GlobalExceptionHandler — Adicionar handler 410:**
```java
@ExceptionHandler(InviteExpiredException.class)
ResponseEntity<ProblemDetail> inviteExpired(InviteExpiredException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.GONE,
        ex.getMessage()
    );
    problem.setType(URI.create("https://api.nossalista.com/docs/errors/invite-expired"));
    problem.setTitle("Link de convite expirado");
    problem.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.GONE).body(problem);
}
```

### 📊 SCHEMA E MIGRATIONS

**✅ NENHUMA MIGRATION NOVA NECESSÁRIA**

Schema já está completo (Stories 2.1 e 4.1):
```sql
-- Tabela lists (columns relevant to this story)
-- invite_code: VARCHAR(20) UNIQUE — já existe (Story 2.1)
-- invite_expires_at: TIMESTAMP — já existe (Story 4.1 via V5 migration)

-- Tabela list_items (columns relevant to this story)
-- id, name, checked, quantity, due_date, url, position, created_by — já existem (Story 3.1)
```

**Repository — Adicionar método:**
```java
// ListRepository.java
Optional<List> findByInviteCode(String inviteCode);
// Spring Data JPA gera automaticamente: SELECT * FROM lists WHERE invite_code = ?
```

### 🧪 TESTING REQUIREMENTS

**Unit Tests (ListJoinServiceTest.java) — 5 test cases:**

1. **shouldReturnJoinListResponseWhenInviteCodeValid**
   ```java
   // Arrange: Lista com invite_code válido, não expirado, com 3 itens
   // Act: getListByInviteCode("ABC123")
   // Assert: response != null, mode == "READ_ONLY", items.size() == 3
   ```

2. **shouldThrowListNotFoundWhenInviteCodeDoesNotExist**
   ```java
   // Arrange: listRepository.findByInviteCode("INVALID") retorna Optional.empty()
   // Act: getListByInviteCode("INVALID")
   // Assert: throws ListNotFoundException
   ```

3. **shouldThrowInviteExpiredExceptionWhenLinkIsExpired**
   ```java
   // Arrange: Lista com invite_expires_at = LocalDateTime.now().minusHours(1)
   // Act: getListByInviteCode("EXPIRED")
   // Assert: throws InviteExpiredException
   ```

4. **shouldReturnItemsOrderedByPosition**
   ```java
   // Arrange: Lista com 3 itens em posições 2, 0, 1 (desordenados)
   // Act: getListByInviteCode("ABC")
   // Assert: items[0].position == 0, items[1].position == 1, items[2].position == 2
   ```

5. **shouldReturnReadOnlyMode**
   ```java
   // Arrange: Lista válida
   // Act: getListByInviteCode("ABC")
   // Assert: response.mode() == "READ_ONLY"
   ```

**Integration Tests (ListJoinControllerIntegrationTest.java) — 5 test cases:**

1. **shouldReturn200WithListDataWhenInviteCodeValid**
   - SEM token JWT (endpoint público)
   - GET /api/lists/join/{validCode}
   - Expect: 200 OK, JSON com id, name, items, mode = "READ_ONLY"

2. **shouldReturn404WhenInviteCodeNotFound**
   - GET /api/lists/join/INVALIDCODE
   - Expect: 404 Not Found, RFC 7807 ProblemDetail

3. **shouldReturn410WhenInviteCodeExpired**
   - GET /api/lists/join/{expiredCode}
   - Expect: 410 Gone, RFC 7807 ProblemDetail

4. **shouldReturn200WithoutAuthentication**
   - GET /api/lists/join/{validCode} sem Authorization header
   - Expect: 200 OK (confirma que endpoint é verdadeiramente público)

5. **shouldReturnItemsInReadOnlyMode**
   - GET /api/lists/join/{validCode} com lista contendo 3 itens
   - Expect: response.items.size() == 3, mode == "READ_ONLY"

**Coverage Target:** 80% (unit) + 20% (integration) = padrão do projeto
**Linha de base:** 313 testes passando (242 backend + 71 frontend)

### 🎨 UX/UI SPECIFICATIONS

**JoinListPage Layout:**

```
┌─────────────────────────────────────┐
│  [Logo] NossaLista   [Entrar]       │ ← Header simples (sem voltar)
│                                     │
│  🔒 Modo Leitura                    │ ← Badge amarelo
│                                     │
│  📋 Mercado Semanal                 │ ← Nome da lista
│  🛒 Compras • por @mariana         │ ← Tipo e owner
│                                     │
│  ⚠️ Você está visualizando em      │ ← Aviso azul (se < 5min → laranja)
│   modo leitura. Entre para colaborar│
│                                     │
│  Itens (5)                          │
│  ┌─────────────────────────────┐   │
│  │ ○ Arroz              ×2     │   │ ← Checkbox disabled, sem botões
│  │ ○ Feijão             ×1     │   │
│  │ ✓ Leite              ×3     │   │ ← Marcado = riscado + opacidade
│  │ ○ Pão                       │   │
│  │ ○ Ovos               ×12    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ╔═════════════════════════════╗   │ ← Rodapé fixo
│  ║  [G] Entrar com Google      ║   │
│  ║  [✉] Entrar com Email       ║   │
│  ╚═════════════════════════════╝   │
└─────────────────────────────────────┘
```

**Estados da página:**

1. **Loading:** Skeleton (seguir padrão do ListView)
   ```tsx
   <div className="animate-pulse">
     <div className="h-8 bg-gray-200 rounded mb-4 w-2/3" />
     <div className="space-y-3">
       {[1,2,3].map(i => <div key={i} className="h-12 bg-gray-100 rounded" />)}
     </div>
   </div>
   ```

2. **Erro 404:** Tela centralizada
   ```tsx
   <div className="text-center p-8">
     <div className="text-6xl mb-4">🔗</div>
     <h2>Convite não encontrado</h2>
     <p>Este link pode ter sido desativado ou não existe.</p>
   </div>
   ```

3. **Erro 410 (Expirado):** Tela centralizada
   ```tsx
   <div className="text-center p-8">
     <div className="text-6xl mb-4">⏰</div>
     <h2>Link de convite expirado</h2>
     <p>Peça um novo link ao dono da lista.</p>
   </div>
   ```

4. **Read-Only View:** Layout principal com 3 zonas:
   - **Zona A:** Header com nome/tipo/owner
   - **Zona B:** Aviso de modo leitura (e aviso de expiração próxima se < 5min)
   - **Zona C:** Lista de itens (disabled)
   - **Zona D:** Rodapé fixo com CTAs de login

**Items em modo read-only:**
```tsx
// Checkbox disabled — NÃO clicável
<input
  type="checkbox"
  checked={item.checked}
  disabled  // ← CRÍTICO: sem interação
  className="opacity-60 cursor-not-allowed"
/>

// Sem campo de adição (ausente)
// Sem botão de opções (ausente)
// Sem botão de edição (ausente)
```

**Aviso de expiração próxima (< 5 minutos):**
```tsx
const minutesRemaining = Math.floor(
  (new Date(listData.expires_at).getTime() - Date.now()) / 60000
);
const isExpiringSoon = minutesRemaining < 5;

{isExpiringSoon && (
  <div className="bg-orange-50 border border-orange-200 rounded-xl p-4 text-orange-800">
    ⚠️ Este link expira em breve! Entre agora ou peça um novo link.
  </div>
)}
```

**CTAs de autenticação:**
```tsx
// Botão Google (primário)
<a href="/api/auth/google" className="btn-primary w-full flex items-center gap-2">
  <GoogleIcon />
  Entrar com Google
</a>

// Botão Email (secundário)
<a href="/login" className="btn-secondary w-full flex items-center gap-2">
  <EmailIcon />
  Entrar com Email
</a>
```

**Acessibilidade (WCAG 2.1 AA):**
- Touch targets: min-h-[48px] em botões de CTA
- Checkboxes disabled devem ter `aria-disabled="true"` e não receber foco via Tab
- Aviso de modo leitura: `role="alert"` para screen readers
- Contraste: Badges/alerts com razão ≥ 4.5:1

**Responsividade:**
- Mobile (< 640px): Layout single column, rodapé fixo bottom-0
- Desktop (> 640px): Layout max-w-lg centralizado, rodapé ao final do conteúdo

### 🔗 DEPENDENCIES & INTEGRATION

**Prerequisites (já implementadas):**
- ✅ `invite_code` column em `lists` table (Story 2.1)
- ✅ `invite_expires_at` column em `lists` table (Story 4.1 via V5 migration)
- ✅ `list_items` table com position, checked, quantity, due_date, url (Story 3.1)
- ✅ `generateInviteCode()` em ListService gera código de 12 chars (Story 4.1)
- ✅ Link format: `{frontendBaseUrl}/join/{code}` (Story 4.2)
- ✅ GlobalExceptionHandler com RFC 7807 para 404 (config existente)
- ✅ BrowserRouter com Routes configurado no main.tsx
- ✅ AuthContext com isAuthenticated disponível

**Esta Story Habilita:**
- **Story 4.4: Entrar na Lista (Autenticado)**
  - Após Google OAuth2 ou login por email, redirect de volta para `/join/{code}`
  - POST /api/lists/join/{inviteCode} → cria ListMember com role MEMBER
  - Redireciona para ListView com acesso completo

**Contexto de Rota:**
```tsx
// main.tsx — ADICIONAR antes do catch-all:
<Route path="/join/:inviteCode" element={<JoinListPage />} />
// ⚠️ NÃO usar ProtectedRoute aqui — endpoint deve ser acessível sem login
```

**Frontend API — Usar axios SEM auth interceptor:**
```typescript
// listsApi.ts — CRÍTICO: não usar o axios instance com auth header
// Criar request público ou usar axios diretamente:
export async function getListByInviteCode(inviteCode: string): Promise<JoinListResponse> {
  const response = await axios.get(`/api/lists/join/${inviteCode}`);
  return response.data;
}

// ALTERNATIVA: Se o interceptor de auth é global no client.ts,
// verificar se ele já lida graciosamente com ausência de token
// (não adicionar header se token não existe)
```

**⚠️ Verificar client.ts:** Confirmar que o axios interceptor de autenticação não quebra quando não há token — ele deve simplesmente não adicionar o header `Authorization` em vez de lançar erro.

### 📚 LEARNINGS FROM PREVIOUS STORIES

**Story 4.2 (Gerar Link de Convite) — Direto ao ponto:**
- ✅ `list.getInviteCode()` e `list.getInviteExpiresAt()` já existem na entity List.java
- ✅ snake_case em todos os campos de DTOs (não camelCase!)
- ✅ SpringDoc @Operation e @ApiResponses em todos os endpoints
- ✅ `@Value("${frontend.url}")` para URL base do frontend
- ✅ Todos os testes de integração precisam de `frontend.url` em `application-test.yml`

**Story 4.1 (Modelagem de Membros):**
- ✅ ListMember entity tem enum `ListMemberRole { OWNER, MEMBER }`
- ✅ OWNER é criado automaticamente ao criar a lista
- ✅ Estrutura de package: `member/` para entidades relacionadas a membros

**Epic 3 (Gestão de Itens) — Padrões:**
- ✅ Pattern de DTO record classes com JavaDoc
- ✅ Exception handling → GlobalExceptionHandler → RFC 7807
- ✅ Integration tests usam @SpringBootTest + TestRestTemplate ou MockMvc
- ✅ Arrange-Act-Assert com @DisplayName descritivo

**Git Intelligence (últimos 5 commits):**
```
a6e0a1d feat(list): implement invite link generation (story 4.2)
bb21a78 docs(member): add code review documentation and fix comments (story 4.1)
d79dac0 feat(member): add invite expiration and owner auto-creation (story 4.1)
3ab5867 fix(frontend): fix unhandled promise rejection in useItems tests
be70edd feat(listitem): implement delete item with code review fixes (story 3-6)
```
→ Backend e frontend evoluindo juntos em cada story
→ Stories de membro estão no package `member/`
→ Commit message format: `feat({scope}): {action} (story {N.M})`

**Testing Pattern (baseado em stories anteriores):**
```java
@DisplayName("ListJoinService - getListByInviteCode")
class ListJoinServiceTest {
    @Mock
    private ListRepository listRepository;

    @InjectMocks
    private ListJoinService listJoinService;

    @BeforeEach
    void setUp() {
        // Setup mocks com dados de teste
    }

    @Test
    @DisplayName("Deve retornar JoinListResponse quando invite code é válido")
    void shouldReturnJoinListResponseWhenInviteCodeValid() {
        // Arrange
        // Act
        // Assert
    }
}
```

### ⚠️ CRITICAL IMPLEMENTATION NOTES

**1. Ordem de verificação na SecurityConfig — CRÍTICO:**
```java
// ✅ CORRETO — mais específico primeiro
.requestMatchers("/api/lists/join/**").permitAll()
.requestMatchers("/api/**").authenticated()

// ❌ ERRADO — regra genérica bloqueia a específica
.requestMatchers("/api/**").authenticated()
.requestMatchers("/api/lists/join/**").permitAll()  // Nunca alcançada!
```

**2. ListJoinController sem @SecurityRequirement — CRÍTICO:**
```java
@RestController
@RequestMapping("/api/lists")
@Tag(name = "Convites", description = "Endpoints públicos de convite")
// ⚠️ NÃO adicionar @SecurityRequirement(name = "JWT") aqui
public class ListJoinController {

    @GetMapping("/join/{inviteCode}")
    @Operation(summary = "Visualizar lista via convite (read-only, sem autenticação)")
    // ...
}
```

**3. Carregar itens para o endpoint de join:**
A lista carregada via `listRepository.findByInviteCode()` pode não ter os itens inicializados (lazy loading JPA). O `ListJoinService` deve garantir que os itens sejam carregados:
```java
// Opção A: EAGER loading no relacionamento (pode impactar performance em outros endpoints)
// Opção B: Carregar explicitamente no serviço (RECOMENDADO)
List list = listRepository.findByInviteCode(inviteCode).orElseThrow(...);
// ... validação de expiração ...
// Inicializar itens via Hibernate.initialize() ou query separada
java.util.List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(list.getId());
```

**4. HTTP 410 Gone — suporte no Spring Boot 4:**
```java
// InviteExpiredException.java
public class InviteExpiredException extends RuntimeException {
    public InviteExpiredException(String message) {
        super(message);
    }
}

// GlobalExceptionHandler.java
@ExceptionHandler(InviteExpiredException.class)
ResponseEntity<ProblemDetail> inviteExpired(InviteExpiredException ex, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    problem.setType(URI.create("https://api.nossalista.com/docs/errors/invite-expired"));
    problem.setTitle("Link de convite expirado");
    problem.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.GONE).body(problem);
}
```

**5. Frontend — verificar client.ts antes de implementar getListByInviteCode:**
Verificar se o interceptor de auth no `client.ts` vai funcionar corretamente para requisições sem token. Se o interceptor tenta sempre ler o token e lança erro quando não existe, é necessário usar `axios` puro para esta requisição específica.

**6. Expiração "em breve" — calcular no frontend:**
```typescript
const now = new Date();
const expiresAt = new Date(listData.expires_at);
const minutesRemaining = Math.floor((expiresAt.getTime() - now.getTime()) / 60000);
const isExpiringSoon = minutesRemaining >= 0 && minutesRemaining < 5;
```

**7. Redirect pós-autenticação para Story 4.4:**
O link `/join/{inviteCode}` deve ser preservado após autenticação para que Story 4.4 possa executar o POST de join automaticamente. Salvar o `inviteCode` em sessionStorage antes do redirect para OAuth2/login.

### Project Structure Notes

**Alinhamento com estrutura do projeto:**
- Novos arquivos de backend em `member/` package (consistente com Story 4.1)
- `ListJoinController.java` em `member/controller/` (endpoints de join são sobre membros)
- `ListJoinService.java` em `member/service/`
- DTOs permanecem em `list/dto/` (seguindo padrão existente de DTOs de listas)
- Exception em `list/exception/` (InviteExpiredException é sobre invite do contexto de lista)
- Nova page em `frontend/src/pages/` (JoinListPage.tsx)

**Variances detectadas:**
- `JoinListResponse` poderia estar em `member/dto/` mas como contém dados de lista, faz mais sentido em `list/dto/` (decisão de localização baseada em contexto de dados, não de controller)

### References

- [Story 4.3 Acceptance Criteria: _bmad-output/planning-artifacts/epics.md#Story 4.3]
- [FR25 - Accept invite via link: _bmad-output/planning-artifacts/epics.md#FR25]
- [NFR-S6 - 24h Expiration: epics.md#NFR-S6]
- [SecurityConfig: backend/src/main/java/.../config/SecurityConfig.java]
- [GlobalExceptionHandler: backend/src/main/java/.../config/GlobalExceptionHandler.java]
- [ListController pattern: backend/src/main/java/.../list/controller/ListController.java]
- [main.tsx routing: frontend/src/main.tsx]
- [Story 4.2 - Previous Story: _bmad-output/implementation-artifacts/4-2-gerar-link-de-convite.md]

## Change Log

- 2026-02-18: Story 4.3 implementada - Aceitar Convite via Link (Read-Only Mode)
  - Backend: InviteExpiredException, JoinListResponse, ListJoinService, ListJoinController
  - Frontend: JoinListPage com modo leitura, avisos de expiração, CTAs de login
  - Tests: 18 novos testes backend (9 unitários + 9 integração), todos passando
- 2026-02-18: Code review (AI) - 7 issues corrigidos (1 alta, 6 médias)
  - [HIGH] Integration test: adicionado `.apply(springSecurity())` ao MockMvcBuilders para testar real security filter chain
  - [MEDIUM] ListJoinService: adicionado `@Transactional(readOnly = true)` para consistência entre queries
  - [MEDIUM] ListJoinController: adicionado `@Validated` + `@Size` + `@Pattern` no path variable `inviteCode`
  - [MEDIUM] listsApi.ts: substituído `axios.get` com URL manual por `client.get`; erros tipados com `ApiError` (status code)
  - [MEDIUM] JoinListPage.tsx: detecção de erro migrada de substring matching para `ApiError.status`
  - [MEDIUM] JoinListPage.tsx: adicionado `role="alert"` no aviso de modo leitura (acessibilidade)
  - [MEDIUM] Story: subtasks de InviteExpiredException corrigidas de `[ ]` para `[x]`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

**Backend Implementation:**
1. Criada exception `InviteExpiredException` para tratamento de links expirados (HTTP 410)
2. Criado DTO `JoinListResponse` com snake_case conforme padrão do projeto
3. Adicionado handler 410 Gone ao `GlobalExceptionHandler` com RFC 7807
4. Criado método `findByInviteCodeWithDetails` no `ListRepository` com JOIN FETCH
5. Implementado `ListJoinService` com validação de expiração e ordenação de itens
6. Criado `ListJoinController` endpoint público sem @SecurityRequirement
7. Atualizado `SecurityConfig` para permitir `/api/lists/join/**` como público

**Backend Tests:**
- 9 testes unitários em `ListJoinServiceTest` (todos passando)
- 9 testes de integração em `ListJoinControllerIntegrationTest` (todos passando)
- Total: 260 testes backend passando (1 skipped)

**Frontend Implementation:**
1. Adicionados tipos `JoinListResponse` e `JoinListItem` em `types/List.ts`
2. Adicionada função `getListByInviteCode` em `listsApi.ts` usando axios puro (endpoint público)
3. Criado componente `JoinListPage.tsx` com:
   - Estados de loading, erro (404/410) e visualização
   - Badge "Modo Leitura" amarelo
   - Lista de itens com checkboxes disabled
   - Aviso de expiração próxima (< 5 min)
   - CTAs para login com Google e Email
   - Rodapé fixo com botões de ação
4. Adicionada rota pública `/join/:inviteCode` em `main.tsx`

**Frontend Tests:**
- 71 testes existentes continuam passando
- Build do frontend realizado com sucesso

### Senior Developer Review (AI)

**Data:** 2026-02-18
**Resultado:** Aprovado após correções
**Issues encontrados:** 1 Alta, 6 Médias, 4 Baixas
**Issues corrigidos automaticamente:** 7 (1 Alta + 6 Médias)
**Issues como action items (baixas):** nenhum — registrados acima para acompanhamento

**Resumo da revisão:**
- Implementação está correta e completa. Todos os ACs implementados.
- Ponto crítico corrigido: integration test para endpoint público não aplicava Spring Security filter chain, comprometendo a validação real do AC de autorização.
- 6 issues médios corrigidos: transação, validação de input, tipagem de erros frontend, URL consistency, acessibilidade.
- 260 backend tests + 71 frontend tests passando após correções.

### File List

**Backend:**
- `backend/src/main/java/br/com/leoferolive/nossalista/list/exception/InviteExpiredException.java` (novo)
- `backend/src/main/java/br/com/leoferolive/nossalista/list/dto/JoinListResponse.java` (novo)
- `backend/src/main/java/br/com/leoferolive/nossalista/member/service/ListJoinService.java` (novo)
- `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/ListJoinController.java` (novo)
- `backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java` (modificado)
- `backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java` (modificado)
- `backend/src/main/java/br/com/leoferolive/nossalista/list/repository/ListRepository.java` (modificado)

**Backend Tests:**
- `backend/src/test/java/br/com/leoferolive/nossalista/member/service/ListJoinServiceTest.java` (novo)
- `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/ListJoinControllerIntegrationTest.java` (novo)

**Frontend:**
- `frontend/src/types/List.ts` (modificado)
- `frontend/src/api/listsApi.ts` (modificado)
- `frontend/src/pages/JoinListPage.tsx` (novo)
- `frontend/src/main.tsx` (modificado)
