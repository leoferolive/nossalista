# Story 4.4: entrar-na-lista-autenticado

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário autenticado,
I want entrar em uma lista via link de convite,
So that possa colaborar nela.

## Acceptance Criteria

**Given** endpoint POST /api/lists/join/{inviteCode} disponível
**When** request JWT válido, invite_code válido, não sou membro
**Then** 201 Created, registro list_members criado (role = 'MEMBER'), Toast "Bem-vindo à lista {nome}!"

**Given** endpoint join
**When** já sou membro
**Then** 200 OK com "Você já é membro", lista completa

**Given** endpoint join
**When** sou dono
**Then** 200 OK com "Você é o dono", lista completa

**Given** usuário JoinList (read-only)
**When** clica "Entrar com Google"
**Then** OAuth2 inicia, após auth volta para JoinList, POST join chamado automaticamente, se sucesso redireciona para ListView (full access)

**Given** POST join com sucesso
**Then** transição: modo leitura → completo, botão "Entrar" some, campo adição aparece, checkboxes habilitados, Toast "Bem-vindo!"

## Tasks / Subtasks

### Backend (Java/Spring Boot)

- [x] Criar DTO JoinListRequest (AC: 1, 2, 3)
  - [x] Record class vazio (endpoint POST sem body)
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/member/dto/JoinListRequest.java`

- [x] Criar DTO ListJoinedResponse (AC: 1, 2, 3)
  - [x] Record class com: id, name, type_slug, type_name, role, message, list (ListResponse completo)
  - [x] snake_case em todos os campos (padrão do projeto)
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/member/dto/ListJoinedResponse.java`

- [x] Implementar ListJoinService.joinList(userId, inviteCode) (AC: 1, 2, 3)
  - [x] Buscar lista por invite_code (404 se não encontrada)
  - [x] Validar expiração: se expires_at < agora → throw InviteExpiredException (410)
  - [x] Verificar se usuário já é membro (200 OK com mensagem "Você já é membro")
  - [x] Verificar se usuário é dono (200 OK com mensagem "Você é o dono")
  - [x] Se não é membro: criar ListMember com role = MEMBER
  - [x] Retornar ListJoinedResponse com dados completos da lista
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/member/service/ListJoinService.java`

- [x] Adicionar endpoint POST /api/lists/join/{inviteCode} ao ListJoinController (AC: 1, 2, 3)
  - [x] Método com @AuthenticationPrincipal para obter userId
  - [x] Endpoint REQUER autenticação (JWT válido)
  - [x] SpringDoc annotations: @Operation, @ApiResponses (201, 200, 404, 410, 401)
  - [x] File: `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/ListJoinController.java`

### Testing (Backend)

- [x] Testes unitários - ListJoinServiceTest (AC: 1, 2, 3)
  - [x] shouldCreateMemberWhenUserJoinsNewList
  - [x] shouldReturnOkWhenUserAlreadyMember
  - [x] shouldReturnOkWhenUserIsOwner
  - [x] shouldThrowListNotFoundWhenInviteCodeDoesNotExist
  - [x] shouldThrowInviteExpiredExceptionWhenLinkExpired
  - [x] File: `backend/src/test/java/br/com/leoferolive/nossalista/member/service/ListJoinServiceTest.java`

- [x] Testes de integração - ListJoinControllerIntegrationTest (AC: 1, 2, 3)
  - [x] shouldReturn201WhenUserJoinsAsNewMember
  - [x] shouldReturn200WhenUserAlreadyMember
  - [x] shouldReturn200WhenUserIsOwner
  - [x] shouldReturn404WhenInviteCodeNotFound
  - [x] shouldReturn410WhenInviteCodeExpired
  - [x] shouldReturn401WithoutAuthentication (endpoint requer JWT)
  - [x] File: `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/ListJoinControllerIntegrationTest.java`

### Frontend (React/TypeScript)

- [x] Adicionar tipo ListJoinedResponse ao types/List.ts (AC: 1, 2, 3)
  - [x] Interface com: id, name, type_slug, type_name, role, message, list
  - [x] File: `frontend/src/types/List.ts`

- [x] Adicionar função joinList ao listsApi.ts (AC: 1, 2, 3)
  - [x] POST /api/lists/join/{inviteCode} COM header de autenticação (JWT)
  - [x] Retorna ListJoinedResponse
  - [x] File: `frontend/src/api/listsApi.ts`

- [x] Modificar JoinListPage.tsx para autenticação (AC: 4, 5)
  - [x] Salvar inviteCode em sessionStorage ANTES de iniciar OAuth2
  - [x] Botão "Entrar com Google" redireciona para `/api/auth/google`
  - [x] Botão "Entrar com Email" redireciona para `/login` com parâmetro redirect
  - [x] Usuários autenticados entram automaticamente na lista
  - [x] File: `frontend/src/pages/JoinListPage.tsx`

- [x] Modificar AuthCallback.tsx para processar pending invite (AC: 4, 5)
  - [x] Após login bem-sucedido, verificar se há pendingInviteCode no sessionStorage
  - [x] Se sim: chamar listsApi.joinList(inviteCode) automaticamente
  - [x] Se join sucesso (201 ou 200): redirecionar para /lists/{listId}
  - [x] Se join falha (410 expirado): mostrar erro e limpar sessionStorage
  - [x] File: `frontend/src/pages/AuthCallback.tsx`

- [x] Modificar Login.tsx para suportar redirect parameter (AC: 4)
  - [x] Aceitar query param `redirect` (ex: /login?redirect=/join/ABC123)
  - [x] Salvar redirect path no sessionStorage antes de iniciar autenticação
  - [x] File: `frontend/src/pages/Login.tsx`

## Dev Notes

### 🎯 CONTEXTO ESSENCIAL

**Propósito da Story:**
Esta story completa o fluxo de convite via link implementando a **conversão de visitante para membro**. Quando Pedro visualiza a lista da Mariana em modo read-only (Story 4.3) e decide participar:
1. **Autentica com Google/Email** — cria conta ou faz login
2. **Junta-se automaticamente à lista** — POST /join cria ListMember
3. **Acessa a lista em modo completo** — pode adicionar, editar, marcar itens

**Jornada do Usuário:**
```
Pedro em JoinListPage (read-only) vendo "Mercado Semanal" da Mariana
  → Clica "Entrar com Google"
  → inviteCode salvo em sessionStorage ("ABC123XYZ789")
  → Redireciona para Google OAuth2
  → Autentica com Google (cria conta ou login)
  → Google callback → /auth/callback?token={jwt}
  → AuthCallback:
     - Detecta pendingInviteCode em sessionStorage
     - POST /api/lists/join/ABC123XYZ789 (com JWT)
     - Backend cria ListMember (role=MEMBER)
     - Response: { id, name, message: "Bem-vindo à lista Mercado Semanal!" }
     - Limpa sessionStorage.pendingInviteCode
     - Redireciona para /lists/{id} (ListView completo)
     - Toast: "Bem-vindo à lista Mercado Semanal!"
  → Pedro agora pode adicionar/editar itens normalmente
```

**Valor de Negócio:**
- Completa o ciclo de conversão: ver → decidir → participar (< 60s)
- Zero fricção: join é automático após autenticação
- Experiência seamless: usuário nem percebe o backend criando o member
- Taxa de conversão esperada: >50% dos que veem a lista (Story 4.3) completam o join

### 🏗️ ARQUITETURA E PADRÕES

**Decisão arquitetural crítica — Endpoint autenticado vs público:**
- GET /api/lists/join/{code} é **público** (Story 4.3 - visualização read-only)
- POST /api/lists/join/{code} é **autenticado** (Story 4.4 - criar membership)

**Rationale:**
- Visualizar (GET) não modifica estado → pode ser público para facilitar compartilhamento
- Juntar-se (POST) cria registro → DEVE ser autenticado para garantir identidade do usuário

**SecurityConfig:**
```java
// NÃO modificar SecurityConfig - POST /api/lists/join/** já requer autenticação
.requestMatchers("/api/lists/join/**").permitAll()  // Apenas GET é público
.requestMatchers("/api/**").authenticated()  // POST /api/** requer JWT
```

Spring Security diferencia automaticamente:
- GET /api/lists/join/{code} → permitAll() → público
- POST /api/lists/join/{code} → não casa com permitAll (é POST, não GET) → cai em authenticated()

**Backend Structure:**
```
backend/src/main/java/br/com/leoferolive/nossalista/
├── member/
│   ├── controller/
│   │   └── ListJoinController.java (MODIFICAR - adicionar endpoint POST)
│   ├── dto/
│   │   ├── JoinListRequest.java (NOVO - record vazio, POST sem body)
│   │   └── ListJoinedResponse.java (NOVO - response do POST join)
│   └── service/
│       └── ListJoinService.java (MODIFICAR - adicionar método joinList)
└── list/
    └── repository/
        └── ListRepository.java (usar existente - findByInviteCode já existe)
```

**Frontend Structure:**
```
frontend/src/
├── pages/
│   ├── JoinListPage.tsx (MODIFICAR - salvar inviteCode antes OAuth2)
│   ├── AuthCallback.tsx (MODIFICAR - processar pending invite após login)
│   └── Login.tsx (MODIFICAR - suportar redirect parameter)
├── api/
│   └── listsApi.ts (MODIFICAR - adicionar joinList function)
└── types/
    └── List.ts (MODIFICAR - adicionar ListJoinedResponse)
```

**API Contract (RESTful Pattern):**
```http
POST /api/lists/join/{inviteCode}
Authorization: Bearer {jwt}  // ← OBRIGATÓRIO

Response 201 Created (novo membro):
{
  "id": "uuid-lista",
  "name": "Mercado Semanal",
  "type_slug": "compras",
  "type_name": "Compras",
  "role": "MEMBER",
  "message": "Bem-vindo à lista Mercado Semanal!",
  "list": {
    // ListResponse completo (mesmo formato do GET /api/lists/{id})
    "id": "uuid",
    "name": "Mercado Semanal",
    "type_slug": "compras",
    ...
  }
}

Response 200 OK (já é membro):
{
  "id": "uuid-lista",
  "name": "Mercado Semanal",
  "type_slug": "compras",
  "type_name": "Compras",
  "role": "MEMBER",
  "message": "Você já é membro desta lista",
  "list": { ... }
}

Response 200 OK (é o dono):
{
  "id": "uuid-lista",
  "name": "Mercado Semanal",
  "type_slug": "compras",
  "type_name": "Compras",
  "role": "OWNER",
  "message": "Você é o dono desta lista",
  "list": { ... }
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

Response 401 Unauthorized (RFC 7807):
{
  "type": "https://api.nossalista.com/docs/errors/unauthorized",
  "title": "Não autenticado",
  "status": 401,
  "detail": "Token JWT ausente ou inválido",
  "instance": "/api/lists/join/{inviteCode}"
}
```

### 🔐 SEGURANÇA E VALIDAÇÃO

**Authentication Pattern:**
```java
// ListJoinController.java
@PostMapping("/join/{inviteCode}")
public ResponseEntity<ListJoinedResponse> joinList(
    @PathVariable String inviteCode,
    @AuthenticationPrincipal UserDetails userDetails  // ← Spring Security injeta usuário autenticado
) {
    UUID userId = UUID.fromString(userDetails.getUsername());  // username = UUID no nosso sistema
    ListJoinedResponse response = listJoinService.joinList(userId, inviteCode);

    // Retornar 201 se novo membro, 200 se já era membro/dono
    HttpStatus status = "Bem-vindo".equals(response.message()) ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(response);
}
```

**Validações Backend:**
1. **JWT válido** — Spring Security valida automaticamente (401 se inválido)
2. **Invite code existe** — ListRepository.findByInviteCode() → 404 se não encontrado
3. **Link não expirado** — invite_expires_at >= now → 410 Gone se expirado
4. **Verificar membership existente** — ListMemberRepository.findByListAndUser()
   - Se existe: retornar 200 OK com mensagem amigável
   - Se não existe: criar novo ListMember

**Idempotência:**
O endpoint POST /join é **idempotente** — chamar múltiplas vezes com o mesmo usuário/invite retorna 200 OK sem criar duplicatas. Importante para:
- Retry automático de rede (frontend pode tentar novamente sem medo)
- Usuário clicando múltiplas vezes no botão "Entrar"
- Race conditions em reconexões OAuth2

**Constraint de DB:**
```sql
-- Tabela list_members tem constraint único:
ALTER TABLE list_members
ADD CONSTRAINT uk_list_members UNIQUE (list_id, user_id);

-- Garante: um usuário só pode ser membro UMA vez de cada lista
-- Se tentar inserir duplicata → constraint violation → catch no service → retornar 200 OK
```

### 📊 SCHEMA E MIGRATIONS

**✅ NENHUMA MIGRATION NOVA NECESSÁRIA**

Schema já está completo (Story 4.1):
```sql
-- Tabela list_members (já existe)
CREATE TABLE list_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  list_id UUID NOT NULL REFERENCES lists(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL,  -- 'OWNER' ou 'MEMBER'
  joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_list_members UNIQUE (list_id, user_id)
);

-- Indexes
CREATE INDEX idx_list_members_list ON list_members(list_id);
CREATE INDEX idx_list_members_user ON list_members(user_id);
```

**Service Implementation:**
```java
// ListJoinService.java
@Transactional
public ListJoinedResponse joinList(UUID userId, String inviteCode) {
    // 1. Buscar lista e validar expiração (reusa lógica do GET join)
    List list = listRepository.findByInviteCode(inviteCode)
        .orElseThrow(() -> new ListNotFoundException("Convite não encontrado"));

    if (list.getInviteExpiresAt() == null ||
        list.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
        throw new InviteExpiredException("Link expirado");
    }

    // 2. Carregar usuário
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

    // 3. Verificar se já é membro (idempotência)
    Optional<ListMember> existingMember = listMemberRepository.findByListAndUser(list, user);

    if (existingMember.isPresent()) {
        ListMember member = existingMember.get();
        String message = member.getRole() == ListMemberRole.OWNER
            ? "Você é o dono desta lista"
            : "Você já é membro desta lista";

        return buildResponse(list, member.getRole(), message);
    }

    // 4. Criar novo membro
    ListMember newMember = new ListMember();
    newMember.setList(list);
    newMember.setUser(user);
    newMember.setRole(ListMemberRole.MEMBER);
    newMember.setJoinedAt(LocalDateTime.now());
    listMemberRepository.save(newMember);

    return buildResponse(list, ListMemberRole.MEMBER,
        "Bem-vindo à lista " + list.getName() + "!");
}
```

### 🧪 TESTING REQUIREMENTS

**Unit Tests (ListJoinServiceTest.java) — 5 test cases:**

1. **shouldCreateMemberWhenUserJoinsNewList**
   ```java
   // Arrange: Lista válida, usuário não é membro
   // Act: joinList(userId, inviteCode)
   // Assert: ListMember criado, role=MEMBER, message="Bem-vindo à lista X!"
   ```

2. **shouldReturnOkWhenUserAlreadyMember**
   ```java
   // Arrange: Usuário já é MEMBER da lista
   // Act: joinList(userId, inviteCode)
   // Assert: Nenhum novo registro, message="Você já é membro"
   ```

3. **shouldReturnOkWhenUserIsOwner**
   ```java
   // Arrange: Usuário é OWNER da lista
   // Act: joinList(userId, inviteCode)
   // Assert: Nenhuma modificação, message="Você é o dono"
   ```

4. **shouldThrowListNotFoundWhenInviteCodeDoesNotExist**
   ```java
   // Arrange: invite_code não existe
   // Act: joinList(userId, "INVALID")
   // Assert: throws ListNotFoundException
   ```

5. **shouldThrowInviteExpiredExceptionWhenLinkExpired**
   ```java
   // Arrange: Lista com invite_expires_at < now
   // Act: joinList(userId, expiredCode)
   // Assert: throws InviteExpiredException
   ```

**Integration Tests (ListJoinControllerIntegrationTest.java) — 6 test cases:**

1. **shouldReturn201WhenUserJoinsAsNewMember**
   - COM token JWT (usuário não é membro)
   - POST /api/lists/join/{validCode}
   - Expect: 201 Created, JSON com role=MEMBER, message="Bem-vindo"

2. **shouldReturn200WhenUserAlreadyMember**
   - COM token JWT (usuário já é membro)
   - POST /api/lists/join/{validCode}
   - Expect: 200 OK, message="Você já é membro"

3. **shouldReturn200WhenUserIsOwner**
   - COM token JWT (usuário é dono)
   - POST /api/lists/join/{validCode}
   - Expect: 200 OK, message="Você é o dono"

4. **shouldReturn404WhenInviteCodeNotFound**
   - POST /api/lists/join/INVALIDCODE
   - Expect: 404 Not Found, RFC 7807

5. **shouldReturn410WhenInviteCodeExpired**
   - POST /api/lists/join/{expiredCode}
   - Expect: 410 Gone, RFC 7807

6. **shouldReturn401WithoutAuthentication**
   - POST /api/lists/join/{validCode} SEM Authorization header
   - Expect: 401 Unauthorized, RFC 7807

**Coverage Target:** 80% (unit) + 20% (integration) = padrão do projeto
**Linha de base:** 260 backend tests + 71 frontend tests (Story 4.3)

### 🎨 UX/UI SPECIFICATIONS - FRONTEND FLOW

**JoinListPage.tsx — Modificações:**

**Botão "Entrar com Google" (antes do OAuth2):**
```tsx
const handleGoogleLogin = () => {
  // CRÍTICO: Salvar inviteCode ANTES de redirecionar
  sessionStorage.setItem('pendingInviteCode', inviteCode);

  // Redirecionar para OAuth2 flow
  window.location.href = '/api/auth/google';
};

// No JSX:
<button
  onClick={handleGoogleLogin}
  className="w-full bg-blue-500 hover:bg-blue-600 text-white font-bold py-3 px-4 rounded-lg flex items-center justify-center gap-2"
>
  <GoogleIcon />
  Entrar com Google
</button>
```

**Botão "Entrar com Email":**
```tsx
const handleEmailLogin = () => {
  // Salvar inviteCode para usar depois do login
  sessionStorage.setItem('pendingInviteCode', inviteCode);

  // Redirecionar para página de login com redirect parameter
  navigate(`/login?redirect=${encodeURIComponent(`/join/${inviteCode}`)}`);
};
```

**AuthCallback.tsx — Modificações (após login bem-sucedido):**

```tsx
const finishAuth = async () => {
  try {
    // 1. Salvar token e carregar perfil (já existe)
    localStorage.setItem('authToken', token);
    const { data } = await client.get<CurrentUserResponse>('/api/users/me');
    login(token, {
      id: data.id,
      username: data.username,
      email: data.email,
      displayName: data.name,
      avatarUrl: data.avatarUrl,
    });

    // 2. NOVO: Verificar pending invite
    const pendingInviteCode = sessionStorage.getItem('pendingInviteCode');

    if (pendingInviteCode) {
      try {
        // Chamar POST /api/lists/join/{inviteCode}
        const joinResponse = await listsApi.joinList(pendingInviteCode);

        // Limpar sessionStorage
        sessionStorage.removeItem('pendingInviteCode');

        // Mostrar toast de boas-vindas
        toast.success(joinResponse.message);  // "Bem-vindo à lista X!"

        // Redirecionar para a lista
        navigate(`/lists/${joinResponse.id}`, { replace: true });
        return;  // ← IMPORTANTE: não executar navigate('/') abaixo
      } catch (error) {
        // Se join falhou (link expirado, etc), limpar e mostrar erro
        sessionStorage.removeItem('pendingInviteCode');

        if (error instanceof ApiError && error.status === 410) {
          toast.error('Link de convite expirou. Peça um novo link.');
        } else {
          toast.error('Erro ao entrar na lista. Tente novamente.');
        }

        // Redirecionar para Home em caso de erro
        navigate('/', { replace: true });
        return;
      }
    }

    // 3. Se não há pending invite, redirecionar para Home normalmente
    navigate('/', { replace: true });

  } catch {
    localStorage.removeItem('authToken');
    sessionStorage.removeItem('pendingInviteCode');  // Limpar também
    setError('Não foi possível concluir o login com Google.');
  }
};
```

**Login.tsx — Modificações (suportar redirect):**

```tsx
export function Login() {
  const [searchParams] = useSearchParams();
  const redirectPath = searchParams.get('redirect');

  // Salvar redirect no sessionStorage se presente
  useEffect(() => {
    if (redirectPath) {
      sessionStorage.setItem('postLoginRedirect', redirectPath);
    }
  }, [redirectPath]);

  // ... resto do componente ...

  // Após login bem-sucedido com email/senha:
  const postLoginRedirect = sessionStorage.getItem('postLoginRedirect');
  if (postLoginRedirect) {
    sessionStorage.removeItem('postLoginRedirect');
    navigate(postLoginRedirect, { replace: true });
  } else {
    navigate('/', { replace: true });
  }
}
```

**Transição Visual (AC 5):**

Quando POST /join retorna sucesso, AuthCallback redireciona para ListView. O ListView carrega com:
- Checkboxes habilitados (não disabled)
- Campo "Adicionar item" visível
- Botões de ação (editar, remover) visíveis
- Badge "Modo Leitura" ausente
- Toast de sucesso: "Bem-vindo à lista Mercado Semanal!" (verde, 3s)

**Acessibilidade:**
- Botões "Entrar" devem ter min-h-[48px] (touch target)
- Toast de sucesso deve ter `role="status"` para screen readers
- Transição para ListView deve preservar foco (não perder contexto)

### 🔗 DEPENDENCIES & INTEGRATION

**Prerequisites (já implementadas):**
- ✅ GET /api/lists/join/{code} endpoint público (Story 4.3)
- ✅ JoinListPage.tsx em modo read-only (Story 4.3)
- ✅ OAuth2SuccessHandler redireciona para /auth/callback?token={jwt} (Story 1.4)
- ✅ AuthCallback.tsx processa token e faz login (Story 1.4)
- ✅ ListMember entity com role OWNER/MEMBER (Story 4.1)
- ✅ ListMemberRepository com constraint único (list_id, user_id) (Story 4.1)
- ✅ InviteExpiredException já existe (Story 4.3)
- ✅ GlobalExceptionHandler trata 410 Gone (Story 4.3)

**Esta Story Habilita:**
- **Story 4.5: Convidar por Username**
  - Com endpoint POST /join funcionando, membros adicionados por username também ficam registrados
  - Mesma tabela list_members, mesma lógica de role
- **Story 4.6: Ver Membros e Sair da Lista**
  - Lista de membros mostrará todos os users que entraram via link ou username
  - Botão "Sair" remove registro de list_members criado aqui
- **Epic 5: Real-time Synchronization**
  - Membros que entraram na lista via join receberão updates WebSocket
  - Broadcast precisa saber quem está conectado → usa list_members

**Fluxo Completo Integrado (Stories 4.2 → 4.3 → 4.4):**

```
1. Mariana cria lista "Mercado Semanal" (Epic 2)
   → ListMember criado automaticamente (role=OWNER)

2. Mariana clica "Convidar" (Story 4.2)
   → POST /api/lists/{id}/invite-link
   → invite_code gerado: "ABC123XYZ789"
   → invite_expires_at: +24h
   → Link: https://nossalista.../join/ABC123XYZ789
   → Mariana copia e cola no WhatsApp

3. Pedro recebe link e clica (Story 4.3)
   → GET /api/lists/join/ABC123XYZ789 (SEM auth)
   → Response: lista em modo READ_ONLY
   → JoinListPage renderiza: itens disabled, botões "Entrar"

4. Pedro clica "Entrar com Google" (Story 4.4)
   → sessionStorage.setItem('pendingInviteCode', 'ABC123XYZ789')
   → window.location.href = '/api/auth/google'
   → OAuth2 flow com Google
   → Google callback: /api/auth/google/callback
   → OAuth2SuccessHandler: gera JWT, redireciona /auth/callback?token={jwt}

5. AuthCallback detecta pendingInviteCode (Story 4.4)
   → POST /api/lists/join/ABC123XYZ789 (COM JWT)
   → Backend:
      - Valida token (401 se inválido)
      - Busca lista (404 se não existe)
      - Verifica expiração (410 se expirou)
      - Cria ListMember (Pedro, role=MEMBER)
   → Response 201: { id, name, message: "Bem-vindo à lista Mercado Semanal!" }
   → sessionStorage.removeItem('pendingInviteCode')
   → Toast: "Bem-vindo à lista Mercado Semanal!"
   → navigate('/lists/{id}')

6. Pedro agora em ListView (modo completo)
   → GET /api/lists/{id} retorna lista completa
   → GET /api/lists/{id}/items retorna itens
   → Pedro pode adicionar, editar, marcar itens
   → Mariana vê atualizações em tempo real (Epic 5 - futuro)
```

### 📚 LEARNINGS FROM PREVIOUS STORIES

**Story 4.3 (Aceitar Convite via Link - Read-Only) — Padrões Críticos:**
- ✅ ListJoinController em `member/controller/` (endpoints de join são sobre membros)
- ✅ ListJoinService em `member/service/` (lógica de negócio de convite)
- ✅ DTOs em `list/dto/` ou `member/dto/` conforme contexto de dados
- ✅ InviteExpiredException retorna 410 Gone via GlobalExceptionHandler
- ✅ Validação de expiração: `invite_expires_at.isBefore(LocalDateTime.now())`
- ✅ SpringDoc `@Operation` e `@ApiResponses` em todos os endpoints
- ✅ Integration tests aplicam `.apply(springSecurity())` ao MockMvc para testar real security filter chain
- ✅ Frontend usa `ApiError` tipado com status code para detecção de erros

**Story 4.2 (Gerar Link de Convite) — Detalhes Importantes:**
- ✅ `@Value("${frontend.url}")` para URL base do frontend
- ✅ `frontend.url` configurado em `application.yml` e `application-test.yml`
- ✅ Reutilizar invite_code se ainda válido (não expirou)
- ✅ Gerar novo código se expirou
- ✅ Formato de link: `{frontendUrl}/join/{code}`

**Story 4.1 (Modelagem de Membros) — Fundação:**
- ✅ ListMember entity: `id, list, user, role, joinedAt`
- ✅ ListMemberRole enum: `OWNER, MEMBER`
- ✅ OWNER criado automaticamente ao criar lista (ListService.createList)
- ✅ Constraint único: `uk_list_members (list_id, user_id)`
- ✅ Cascade DELETE: remover lista remove todos os members

**Story 1.4 (Google OAuth2) — Autenticação:**
- ✅ OAuth2SuccessHandler processa callback do Google
- ✅ Extrai email, name, picture dos atributos OAuth2
- ✅ Cria usuário se não existe, atualiza se existe (apenas GOOGLE provider)
- ✅ Gera JWT token via JwtService
- ✅ Redireciona para `{frontendUrl}/auth/callback?token={jwt}`
- ✅ AuthCallback salva token, carrega perfil (/api/users/me), faz login

**Epic 3 (Gestão de Itens) — Padrões de Teste:**
- ✅ Arrange-Act-Assert pattern com @DisplayName descritivo
- ✅ MockMvc para integration tests com security filter chain
- ✅ TestRestTemplate para testes que precisam de contexto completo
- ✅ @Transactional em services de escrita (create, update, delete)

**Git Intelligence (últimos 5 commits):**
```
dc0de83 feat(member): implement invite link read-only view with code review fixes (story 4.3)
a6e0a1d feat(list): implement invite link generation (story 4.2)
bb21a78 docs(member): add code review documentation and fix comments (story 4.1)
d79dac0 feat(member): add invite expiration and owner auto-creation (story 4.1)
3ab5867 fix(frontend): fix unhandled promise rejection in useItems tests
```
→ Commit message pattern: `feat({scope}): {description} (story {N.M})`
→ Scopes: `member` para features de convite/membros, `list` para features de listas
→ Code reviews são feitos após implementação inicial (commits com "code review fixes")

### ⚠️ CRITICAL IMPLEMENTATION NOTES

**1. SessionStorage vs LocalStorage — CRÍTICO:**
```typescript
// ✅ CORRETO — usar sessionStorage para pendingInviteCode
sessionStorage.setItem('pendingInviteCode', inviteCode);
// Razão: sessionStorage limpa ao fechar aba, evita convites "fantasma"
// localStorage persiste entre sessões → usuário pode entrar em lista errada dias depois

// ✅ CORRETO — usar localStorage para authToken
localStorage.setItem('authToken', token);
// Razão: token deve persistir entre sessões para "lembrar login"
```

**2. Ordem de execução no AuthCallback — CRÍTICO:**
```typescript
// ✅ CORRETO — ordem de operações após OAuth2:
// 1. Salvar token no localStorage (para interceptor funcionar)
// 2. Carregar perfil do usuário (/api/users/me)
// 3. Chamar login() do AuthContext (atualiza estado React)
// 4. Verificar pendingInviteCode (DEPOIS do login para ter auth header)
// 5. Se tem invite: POST /join → redireciona /lists/{id}
// 6. Se não tem: redireciona /

// ❌ ERRADO — chamar POST /join ANTES de salvar token:
// Interceptor não terá Authorization header → 401 Unauthorized
```

**3. Idempotência do POST /join — Pattern Recomendado:**
```java
// ListJoinService.java
@Transactional
public ListJoinedResponse joinList(UUID userId, String inviteCode) {
    // ... validações de lista e expiração ...

    // Verificar membership ANTES de tentar criar
    Optional<ListMember> existing = listMemberRepository.findByListAndUser(list, user);

    if (existing.isPresent()) {
        // Retornar 200 OK (idempotente)
        return buildResponse(list, existing.get().getRole(), getMessage(existing.get()));
    }

    // ✅ GARANTIA EXTRA: usar try-catch para constraint violation
    try {
        ListMember newMember = new ListMember();
        // ... set fields ...
        listMemberRepository.save(newMember);
        return buildResponse(list, MEMBER, "Bem-vindo à lista!");
    } catch (DataIntegrityViolationException e) {
        // Race condition: outro request criou member no meio tempo
        // Recarregar e retornar OK
        ListMember member = listMemberRepository.findByListAndUser(list, user)
            .orElseThrow(); // Impossível não existir aqui
        return buildResponse(list, member.getRole(), getMessage(member));
    }
}
```

**4. Frontend — Limpar sessionStorage SEMPRE:**
```typescript
// ✅ SEMPRE limpar pendingInviteCode, mesmo em caso de erro:

try {
  const response = await listsApi.joinList(pendingInviteCode);
  sessionStorage.removeItem('pendingInviteCode');  // ← Limpar ANTES de navegar
  toast.success(response.message);
  navigate(`/lists/${response.id}`);
} catch (error) {
  sessionStorage.removeItem('pendingInviteCode');  // ← Limpar TAMBÉM no erro
  // ... tratamento de erro ...
}

// Razão: se não limpar no erro, próximo login tentará fazer join novamente
```

**5. Diferença HTTP Status 201 vs 200 — Semântica REST:**
```java
// ListJoinController.java
@PostMapping("/join/{inviteCode}")
public ResponseEntity<ListJoinedResponse> joinList(...) {
    ListJoinedResponse response = listJoinService.joinList(...);

    // ✅ 201 Created: novo recurso (ListMember) foi criado
    // ✅ 200 OK: recurso já existia (idempotente)
    boolean isNewMember = response.message().startsWith("Bem-vindo");
    HttpStatus status = isNewMember ? HttpStatus.CREATED : HttpStatus.OK;

    return ResponseEntity.status(status).body(response);
}
```

**6. Toast Messages — UX Consistency:**
```typescript
// IMPORTANTE: Mensagens devem corresponder às do backend

// Backend: "Bem-vindo à lista Mercado Semanal!"
// Frontend toast.success: usar response.message diretamente

toast.success(response.message);  // ✅ Consistência garantida

// NÃO hardcodar mensagens no frontend:
toast.success("Você entrou na lista!");  // ❌ Desalinhado com backend
```

**7. Redirect Flow — Preservar Invite Code:**
```typescript
// JoinListPage.tsx — Botão "Entrar com Email"
const handleEmailLogin = () => {
  // Salvar invite ANTES de sair da página
  sessionStorage.setItem('pendingInviteCode', inviteCode);

  // Navegar para login COM redirect parameter
  navigate(`/login?redirect=${encodeURIComponent(`/join/${inviteCode}`)}`);
};

// Login.tsx — Após login bem-sucedido:
const redirect = sessionStorage.getItem('postLoginRedirect');
if (redirect) {
  sessionStorage.removeItem('postLoginRedirect');
  navigate(redirect);  // Volta para /join/{code}
  // JoinListPage detecta auth e chama POST /join automaticamente
}
```

**8. Testing — Autenticação em Integration Tests:**
```java
@Test
void shouldReturn201WhenUserJoinsAsNewMember() {
    // Setup: criar lista com invite_code, criar usuário
    String inviteCode = "ABC123";
    User user = createTestUser();

    // CRÍTICO: Gerar JWT token para o usuário
    String token = jwtService.generateToken(user);

    // Request COM Authorization header
    mockMvc.perform(post("/api/lists/join/" + inviteCode)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("MEMBER"))
        .andExpect(jsonPath("$.message").value(containsString("Bem-vindo")));
}

@Test
void shouldReturn401WithoutAuthentication() {
    // Request SEM Authorization header
    mockMvc.perform(post("/api/lists/join/ABC123"))
        .andExpect(status().isUnauthorized())  // 401
        .andExpect(jsonPath("$.status").value(401));
}
```

### Project Structure Notes

**Alinhamento com estrutura do projeto:**
- Backend changes em `member/` package (ListJoinController, ListJoinService)
- DTOs em `member/dto/` (ListJoinedResponse é sobre membership)
- Reutilização de repositories existentes (ListRepository, ListMemberRepository, UserRepository)
- Frontend changes em `pages/` (JoinListPage, AuthCallback, Login)
- API client changes em `api/listsApi.ts`
- Types changes em `types/List.ts`

**Novos arquivos:**
- `backend/src/main/java/.../member/dto/ListJoinedResponse.java` (novo DTO)
- `backend/src/test/java/.../member/service/ListJoinServiceTest.java` (testes unitários)
- `backend/src/test/java/.../member/controller/ListJoinControllerIntegrationTest.java` (testes integração)

**Arquivos modificados:**
- `backend/src/main/java/.../member/controller/ListJoinController.java` (adicionar endpoint POST)
- `backend/src/main/java/.../member/service/ListJoinService.java` (adicionar método joinList)
- `frontend/src/pages/JoinListPage.tsx` (salvar inviteCode antes OAuth2)
- `frontend/src/pages/AuthCallback.tsx` (processar pending invite)
- `frontend/src/pages/Login.tsx` (suportar redirect parameter)
- `frontend/src/api/listsApi.ts` (adicionar joinList function)
- `frontend/src/types/List.ts` (adicionar ListJoinedResponse interface)

**Arquivos NÃO modificados:**
- `SecurityConfig.java` — POST /api/lists/join/** já cai em .authenticated() automaticamente
- `GlobalExceptionHandler.java` — handler 410 Gone já existe (Story 4.3)
- `ListRepository.java` — findByInviteCode já existe (Story 4.3)
- `ListMemberRepository.java` — entity e repository já completos (Story 4.1)

**Variances detectadas:**
- ✅ ListJoinedResponse poderia estar em `list/dto/` mas como é resposta de join (criar membership), `member/dto/` é mais adequado

### References

- [Story 4.4 Acceptance Criteria: _bmad-output/planning-artifacts/epics.md#Story 4.4, linhas 1003-1029]
- [Story 4.3 - Read-Only View: _bmad-output/implementation-artifacts/4-3-aceitar-convite-via-link-read-only-mode.md]
- [Story 4.2 - Generate Invite Link: _bmad-output/implementation-artifacts/4-2-gerar-link-de-convite.md]
- [Story 4.1 - Members Data Model: _bmad-output/implementation-artifacts/4-1-modelagem-de-dados-de-membros-e-convites.md]
- [Story 1.4 - Google OAuth2: _bmad-output/implementation-artifacts/1-4-integracao-google-oauth2.md]
- [SecurityConfig: backend/src/main/java/.../config/SecurityConfig.java]
- [OAuth2SuccessHandler: backend/src/main/java/.../auth/OAuth2SuccessHandler.java]
- [AuthCallback: frontend/src/pages/AuthCallback.tsx]
- [Architecture Decision - Authentication: _bmad-output/planning-artifacts/architecture.md#Authentication]
- [PRD FR24-FR30 - Sharing & Collaboration: _bmad-output/planning-artifacts/prd.md#Sharing]

## Change Log

- 2026-02-19: Code review executado — 4 issues HIGH e 5 issues MEDIUM corrigidos:
  - [HIGH] Fix: condição `!listData` em JoinListPage.tsx impedia auto-join de usuários autenticados — corrigido para `listData && !joining`
  - [HIGH] Fix: toast de boas-vindas ausente em AuthCallback e JoinListPage — agora passado via navigation state para ListView/Home
  - [HIGH] Fix: `shouldCreateMemberWhenUserJoinsNewList` não verificava save() — adicionado `verify(listMemberRepository).save(any(ListMember.class))`
  - [HIGH] Fix: HTTP status determinado por `startsWith("Bem-vindo")` — substituído por campo `boolean created` em `ListJoinedResponse`
  - [MEDIUM] Fix: comentário de classe e @Tag do controller diziam "endpoints públicos" — corrigidos
  - [MEDIUM] Fix: `@RequestBody(required = false) JoinListRequest request` não usado — removido do controller
  - [MEDIUM] Fix: `IllegalStateException` para usuário não encontrado gerava 500 — substituído por `NotAuthenticatedException` (401)
  - [MEDIUM] Fix: erros não-410 em AuthCallback navegavam para home sem feedback — agora passam toastMessage via state
  - [MEDIUM] Fix: `redirectPath.replace('/join/', '')` frágil em Login.tsx — substituído por `slice` com validação `startsWith('/join/')`
  - Backend: 271 testes passando; Frontend: 71 testes passando
- 2026-02-19: Story 4.4 completada - Entrar na Lista (Autenticado)
  - Backend:
    - Criados DTOs JoinListRequest e ListJoinedResponse
    - Implementado ListJoinService.joinList() com validações e idempotência
    - Adicionado endpoint POST /api/lists/join/{inviteCode} ao controller
    - Ajustado SecurityConfig para permitir apenas GET público em /join/**
    - Criados 5 testes unitários e 6 testes de integração
    - Todos os 271 testes do backend passando
  - Frontend:
    - Adicionado tipo ListJoinedResponse em types/List.ts
    - Adicionada função joinList em listsApi.ts
    - Modificado JoinListPage para salvar inviteCode e entrar automaticamente se autenticado
    - Modificado AuthCallback para processar pending invite após OAuth2
    - Modificado Login para suportar redirect parameter
    - Todos os 71 testes do frontend passando
  - Status atualizado para "review"

### Project Structure Notes

[Placeholder]

### References

[Placeholder]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List

**Backend - Novos arquivos:**
- `backend/src/main/java/br/com/leoferolive/nossalista/member/dto/JoinListRequest.java` (novo)
- `backend/src/main/java/br/com/leoferolive/nossalista/member/dto/ListJoinedResponse.java` (novo)

**Backend - Arquivos modificados:**
- `backend/src/main/java/br/com/leoferolive/nossalista/member/service/ListJoinService.java` (adicionado método joinList)
- `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/ListJoinController.java` (adicionado endpoint POST)
- `backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java` (ajustado para GET permitAll em /join/**)
- `backend/src/test/java/br/com/leoferolive/nossalista/member/service/ListJoinServiceTest.java` (adicionados 5 testes)
- `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/ListJoinControllerIntegrationTest.java` (adicionados 6 testes)

**Frontend - Arquivos modificados:**
- `frontend/src/types/List.ts` (adicionado ListJoinedResponse)
- `frontend/src/api/listsApi.ts` (adicionada função joinList)
- `frontend/src/pages/JoinListPage.tsx` (modificado para salvar inviteCode e entrar automaticamente)
- `frontend/src/pages/AuthCallback.tsx` (modificado para processar pending invite)
- `frontend/src/pages/Login.tsx` (modificado para suportar redirect parameter)
