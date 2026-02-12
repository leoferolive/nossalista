# Story 2.2: Criar Nova Lista

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário autenticado,
I want criar uma nova lista escolhendo tipo e nome,
so that possa começar a organizar minhas tarefas ou compras.

## Acceptance Criteria

### AC1: Backend - Endpoint POST /api/lists

**Given** o endpoint POST /api/lists está disponível
**When** faço request com JWT válido e body `{ "name": "Mercado Semanal", "typeId": 1 }`
**Then** response deve ser 201 Created com lista criada
**And** owner_id deve ser o usuário autenticado
**And** invite_code deve ser gerado automaticamente (string alfanumérica 12 chars)

### AC2: Backend - Validações

**Given** o endpoint de criação
**When** nome está vazio ou < 3 caracteres ou typeId inválido
**Then** response deve ser 400 Bad Request com erro de validação

### AC3: Frontend - CreateListModal Component

**Given** CreateListModal no frontend
**When** aberto
**Then** deve mostrar:
- Campo "Nome da lista"
- 4 cards visuais (🛒 Compras, ✅ Tarefas, 🎁 Wishlist, 📝 Genérica)
- Botão "Criar Lista" desabilitado até nome preenchido e tipo selecionado
- Enter no campo deve criar lista (se tipo selecionado)

### AC4: Frontend - Success Flow

**Given** lista criada com sucesso
**When** response é recebida
**Then** Toast "Lista criada" deve aparecer (success, 300ms)
**And** modal deve fechar
**And** usuário deve ser redirecionado para Home

## Tasks / Subtasks

### Backend Implementation

- [x] Criar DTOs e Exceptions (AC: #1, #2)
  - [x] CreateListRequest.java com validações (@NotBlank, @Size, @NotNull, @Min, @Max)
  - [x] ListResponse.java com records nested (TypeResponse, OwnerResponse)
  - [x] ListMapper.java (entity → DTO)
  - [x] InvalidListTypeException.java
- [x] Implementar Service Layer (AC: #1, #2)
  - [x] ListService.java com createList(request, owner)
  - [x] generateInviteCode() - 12 chars alfanuméricos uppercase com verificação de unicidade
  - [x] Validar typeId existe em list_types (throw InvalidListTypeException se não)
- [x] Implementar Controller Layer (AC: #1)
  - [x] ListController.java com POST /api/lists
  - [x] @SecurityRequirement + @AuthenticationPrincipal User
  - [x] SpringDoc annotations completas (@ApiResponses adicionado)
- [x] Adicionar Exception Handler (AC: #2)
  - [x] Modificar GlobalExceptionHandler
  - [x] Handler para InvalidListTypeException → RFC 7807 ProblemDetail
- [x] Testes Backend (AC: #1, #2)
  - [x] ListServiceTest: 3 testes unitários (valid data, invalid typeId, unique codes)
  - [x] ListControllerIntegrationTest: 14 testes integração (cobertura completa)

### Frontend Implementation

- [x] Criar Types TypeScript (AC: #3, #4)
  - [x] ProblemDetail.ts (RFC 7807 interface)
  - [x] List.ts (ListResponse, CreateListRequest, LIST_TYPES const com emojis)
- [x] Criar API Client (AC: #1, #3, #4)
  - [x] client.ts (axios + interceptors JWT automático)
  - [x] listsApi.ts com createList() + getAllLists() + getListById()
- [x] Criar Components (AC: #3, #4)
  - [x] TypeCard.tsx (card visual com emoji, touch target 160px, keyboard accessible)
  - [x] CreateListModal.tsx (modal completo com validações, Enter submete, Escape fecha)
  - [x] Toast.tsx (sistema de notificações com animação 300ms)
- [x] Criar Hooks (AC: #3, #4)
  - [x] useLists.ts (fetch, loading, error states, createList)
  - [x] useToast.ts (gerenciamento de múltiplos toasts)
- [x] Integração na Home Page (AC: #4)
  - [x] Botão "+ Nova Lista" abre CreateListModal
  - [x] Toast "Lista criada!" em sucesso (300ms animation)
  - [x] Modal fecha após sucesso
  - [x] Refetch listas após criação

## Dev Notes

### Epic Context

**Epic 2: Gestão de Listas Pessoais** - Usuários podem criar e gerenciar suas próprias listas.

**FRs Cobertos:** FR8 (criar lista com tipo e nome), FR13 (4 tipos de lista), FR14 (tipo define campos)

**Story Sequence:**
- ✅ Story 2.1 COMPLETA: Modelagem de Dados (List, ListType entities, migrations V2)
- 🎯 Story 2.2 ATUAL: Criar Nova Lista
- ⏳ Story 2.3: Listar Todas as Listas (usará ListResponse desta story)
- ⏳ Story 2.4-2.6: Ver detalhes, Editar, Excluir

### Technical Stack Estabelecido

**Backend:**
- Spring Boot 4.0.2 + Java 25
- Spring Data JPA + Hibernate
- PostgreSQL (prod e dev via Docker Compose)
- H2 MODE=PostgreSQL (testes apenas)
- Flyway migrations
- SpringDoc OpenAPI 3
- Jakarta Validation
- JWT via JwtService (stateless auth)

**Frontend:**
- React 19 + TypeScript 5+
- Vite 5.x
- Tailwind CSS 3.x
- Axios (API calls)
- React Router
- Context API + hooks

### Padrões Arquiteturais (Epic 1 Estabelecidos)

**Backend:**
1. **Constructor Injection:** Sem @Autowired
2. **RFC 7807 Problem Details:** Error handling consistente
3. **DTO Pattern:** Request/Response DTOs separados de entities
4. **Service Layer:** Business logic isolada
5. **Mapper Pattern:** Entity → DTO via mappers
6. **Validation:** Jakarta Validation (@Valid, @NotBlank, @Size)
7. **SpringDoc:** @Tag, @Operation, @ApiResponse em controllers
8. **Exception Handling:** GlobalExceptionHandler com @RestControllerAdvice

**Frontend:**
1. **Axios Client:** Interceptor para JWT automático
2. **Custom Hooks:** Lógica encapsulada (useLists, useAuth)
3. **Type-Safe Errors:** Interface ProblemDetail
4. **Toast Notifications:** Feedback visual
5. **Accessible Components:** ARIA labels, keyboard navigation

### Naming Conventions

- **Entities:** PascalCase inglês (List, User, não "Lista")
- **DTOs:** `<Action><Entity>Request/Response` (CreateListRequest, ListResponse)
- **Endpoints:** `/api/<resource>` plural, lowercase
- **Database:** snake_case (list_types, lists)

### Critical Implementation Requirements

#### 🔴 Backend - UUID Generation

PostgreSQL **NÃO auto-gera UUID**. Você DEVE setar manualmente:

```java
List list = new List();
list.setId(UUID.randomUUID()); // ← CRÍTICO!
```

#### 🔴 Backend - Invite Code Generation

```java
private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
private static final int INVITE_CODE_LENGTH = 12;
private final SecureRandom random = new SecureRandom();

private String generateInviteCode() {
    StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
    for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
        int index = random.nextInt(INVITE_CODE_CHARS.length());
        code.append(INVITE_CODE_CHARS.charAt(index));
    }
    return code.toString();
}
```

**Unicidade:** 36^12 = 4.7 × 10^18 combinações. Risco de colisão negligível para MVP.

#### 🔴 Backend - @AuthenticationPrincipal User

Depende de JwtAuthenticationFilter (Story 1.3) configurado no SecurityConfig:
1. Extrai JWT do header `Authorization: Bearer <token>`
2. Valida via JwtService
3. Carrega User do database
4. Seta UsernamePasswordAuthenticationToken no SecurityContext

#### 🔴 Frontend - CORS Configuration

Verificar SecurityConfig permite:
```java
config.setAllowedOrigins(List.of("http://localhost:5173")); // Vite dev server
config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE"));
config.setAllowCredentials(true);
```

#### 🔴 Frontend - Touch Targets (NFR-A4)

TypeCard deve ter **mínimo 44×44 pixels**. Implementado com `min-h-[160px] min-w-[160px]`.

#### 🔴 Frontend - Keyboard Accessibility (NFR-A2)

- Enter no input de nome submete form
- focus:ring em todos os botões
- aria-labels descritivos

### Validation Rules

**CreateListRequest:**
- `name`: NotBlank, Size(min=3, max=100)
- `typeId`: NotNull, Min(1)

**Business Rules:**
- typeId deve existir em list_types (1-4: Compras, Tarefas, Wishlist, Genérica)
- owner_id setado automaticamente do usuário autenticado
- invite_code gerado automaticamente (12 chars alfanuméricos uppercase)

### Error Handling

**400 Bad Request:**
- Nome vazio ou < 3 caracteres → Validation error
- typeId inválido (não 1-4) → InvalidListTypeException

**401 Unauthorized:**
- JWT ausente ou inválido

**Response Format (RFC 7807):**
```json
{
  "type": "https://api.nossalista.com/docs/errors/invalid-list-type",
  "title": "Tipo de lista inválido",
  "status": 400,
  "detail": "Tipo de lista inválido: 999. Tipos válidos: 1-4 (Compras, Tarefas, Wishlist, Genérica)",
  "instance": "/api/lists"
}
```

## Project Structure Notes

### Backend Files to Create (7 new files)

```
backend/src/main/java/br/com/leoferolive/nossalista/list/
├── controller/
│   └── ListController.java          # REST controller
├── service/
│   └── ListService.java              # Business logic
├── dto/
│   ├── CreateListRequest.java        # Request DTO
│   ├── ListResponse.java             # Response DTO
│   └── ListMapper.java               # Entity → DTO
└── exception/
    └── InvalidListTypeException.java # Custom exception

backend/src/test/java/br/com/leoferolive/nossalista/list/
├── service/ListServiceTest.java                    # Unit tests
└── controller/ListControllerIntegrationTest.java  # Integration tests
```

### Backend Files to Modify (1 file)

```
backend/src/main/java/br/com/leoferolive/nossalista/config/
└── GlobalExceptionHandler.java      # Add InvalidListTypeException handler
```

### Frontend Files to Create (7 new files)

```
frontend/src/
├── api/
│   └── listsApi.ts                  # API calls
├── components/
│   ├── CreateListModal.tsx           # Modal de criação
│   └── TypeCard.tsx                  # Card visual de tipo
├── types/
│   ├── List.ts                       # Interfaces TypeScript
│   └── ProblemDetail.ts              # RFC 7807 interface
└── hooks/
    └── useLists.ts                   # Hook para listas
```

### Frontend Files to Verify (3 files)

```
frontend/src/
├── api/client.ts                     # Axios instance (deve já existir)
├── contexts/AuthContext.tsx          # User autenticado (deve já existir)
└── components/Toast.tsx              # Sistema de toast (criar se não existir)
```

### Existing Files from Story 2.1 (Used by this story)

```
backend/src/main/java/br/com/leoferolive/nossalista/list/
├── domain/
│   ├── List.java                     ✅ Entity JPA
│   ├── ListType.java                  ✅ Enum
│   └── ListTypeEntity.java            ✅ Lookup table entity
└── repository/
    ├── ListRepository.java            ✅ Spring Data JPA
    └── ListTypeRepository.java        ✅ Lookup repository

backend/src/main/resources/db/migration/
└── V2__create_list_types_and_lists.sql  ✅ Migration (4 tipos pré-inseridos)
```

## Dev Agent Record

### Previous Story Intelligence (Story 2.1)

**Story 2.1 Completed:** Modelagem de Dados de Listas e Tipos

**Learnings:**
1. **UUID Manual Generation Required:** PostgreSQL não auto-gera UUID. Usar `list.setId(UUID.randomUUID())`.
2. **@PrePersist/@PreUpdate:** Timestamps (createdAt, updatedAt) automáticos configurados na entidade.
3. **FetchType.LAZY:** Relacionamentos @ManyToOne são LAZY por padrão - carregar sob demanda.
4. **Lookup Table Immutable:** list_types é imutável, seeded na migration V2.
5. **PostgreSQL Dev Environment:** H2 foi removido do dev, agora usa PostgreSQL via Docker Compose.

**Entities Created:**
- `List` entity: id (UUID), name, typeId, ownerId, inviteCode, createdAt, updatedAt
- `ListTypeEntity`: id, name, slug, createdAt (4 rows: compras, tarefas, wishlist, generica)
- `ListType` enum: SHOPPING(1), TASK(2), WISHLIST(3), GENERIC(4)

**Migrations:**
- V2__create_list_types_and_lists.sql com INSERTs dos 4 tipos

**Repositories:**
- `ListRepository`: findByOwnerId(), findByInviteCode()
- `ListTypeRepository`: findBySlug()

**Patterns Established:**
- Constructor injection (sem @Autowired)
- Jakarta Validation na entidade
- Relacionamentos bidirecionais evitados (complexidade)

**Armadilhas Evitadas:**
- ❌ Não usar `random_uuid()` - PostgreSQL usa `gen_random_uuid()`
- ❌ Não modificar migrations aplicadas - criar nova versão
- ❌ Não usar H2 em dev - PostgreSQL via Docker Compose agora

### Git Intelligence Summary

**Recent Commits:**
```
9ec1904 feat(list): implement list data model with types (story 2-1)
77977fe feat(user): add user profile and search endpoints with comprehensive tests
ee6bf0c refactor(database): migrate dev environment from H2 to PostgreSQL via Docker Compose
5dde2d2 feat(templates): add various document templates and configuration files
aae42b6 chore(dependencies): update tailwindcss to 3.4.19 and remove duplicate axios entry
```

**Commit Message Pattern:**
- Format: `<type>(<scope>): <description> (story X-Y)`
- Types: feat, fix, refactor, chore, test, docs
- Scope: module name (list, user, database, etc.)

**Code Patterns from Commits:**
1. **Comprehensive Tests:** Todos os commits incluem testes (unit + integration)
2. **Docker Compose:** Dev environment usa PostgreSQL via Docker
3. **Tailwind CSS:** Versão 3.4.19 configurada
4. **Axios:** Cliente HTTP já configurado

### Latest Tech Information

**Stack Versions (from pom.xml and package.json):**
- Spring Boot: 4.0.2
- Java: 25
- React: 19
- TypeScript: 5+
- Vite: 5.x
- Tailwind CSS: 3.4.19
- PostgreSQL: 16+ (via Docker Compose)

**No Web Research Needed:** Stack é estável e bem documentado. Todas as versões são LTS ou latest stable.

### Agent Model Used

Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References

N/A - Primeira implementação da story

### Completion Notes List

**✅ Code Review Follow-ups Resolvidos (2026-02-12)**

1. **Testes Frontend Configurados e Executando**
   - Instaladas dependências: vitest@3.0.0, @testing-library/react@16.1.0, @testing-library/jest-dom@6.6.3, @testing-library/user-event@14.6.0, jsdom@26.0.0
   - Corrigido TypeCard.test.tsx: adicionado `beforeEach(vi.clearAllMocks)` e alterado para usar `userEvent.keyboard()` após `card.focus()`
   - Todos os 16 testes passando (7 TypeCard + 9 CreateListModal)

2. **Redirecionamento 401 Implementado**
   - Criado AuthContext.tsx com gerenciamento de estado de autenticação
   - Criado Login.tsx com formulário de login (email/senha + Google OAuth2 placeholder)
   - Atualizado main.tsx com BrowserRouter, AuthProvider e ProtectedRoute
   - Atualizado client.ts: interceptor de response agora redireciona para /login quando recebe 401

**✅ Backend Implementation Complete (AC1 + AC2)**

**Implementações Realizadas:**
1. ✅ **DTOs criados** - CreateListRequest com validações completas (@Min/@Max)
2. ✅ **Service layer** - ListService com invite code uniqueness check (loop retry até 10 tentativas)
3. ✅ **Controller** - ListController com @ApiResponses completo (201, 400, 401)
4. ✅ **Exception handler** - GlobalExceptionHandler com InvalidListTypeException → RFC 7807
5. ✅ **Testes completos** - 3 unit tests + 14 integration tests (cobertura além do escopo)
6. ✅ **ListMapper null safety** - Lança IllegalStateException se owner null

**Melhorias Aplicadas (Code Review Fixes):**
- 🔧 **Invite code uniqueness**: Adicionado `existsByInviteCode()` check com retry loop
- 🔧 **Validation enhancement**: Adicionado @Max(4) no typeId (além do @Min(1))
- 🔧 **Documentation**: @ApiResponses completo no controller para Swagger UI
- 🔧 **Null safety**: ListMapper agora falha explicitamente se owner não carregado

**✅ Frontend Implementation Complete (AC3 + AC4)**

**Implementações Realizadas:**
1. ✅ **Types criados** - ProblemDetail (RFC 7807), List interfaces, LIST_TYPES const
2. ✅ **API Client** - Axios com JWT interceptor automático + listsApi
3. ✅ **CreateListModal** - Modal completo com validações, Enter submete, Escape fecha
4. ✅ **TypeCard** - Cards visuais 160px (NFR-A4), keyboard accessible (Enter/Space)
5. ✅ **Toast System** - Notificações com animação 300ms, useToast hook
6. ✅ **useLists Hook** - Estado de listas com loading/error/fetch/create
7. ✅ **Home Page** - Exemplo de integração completa

**AC3 Atendido:**
- ✅ Campo "Nome da lista" com validação mínimo 3 chars
- ✅ 4 cards visuais: 🛒 Compras, ✅ Tarefas, 🎁 Wishlist, 📝 Genérica
- ✅ Botão "Criar Lista" desabilitado até nome + tipo válidos
- ✅ Enter no campo submete form (se válido)

**AC4 Atendido:**
- ✅ Toast "Lista criada!" aparece (success, 300ms animation)
- ✅ Modal fecha após sucesso
- ✅ Refetch listas após criação (ou navigate para lista)

**Decisões Técnicas:**
- UUID gerado manualmente via `UUID.randomUUID()` (PostgreSQL não auto-gera)
- Invite code: 12 chars A-Z0-9 uppercase com verificação de duplicata no banco
- Testes backend: 14 integration tests (além dos 4 especificados) para cobertura de edge cases
- ListMapper: Fail-fast se owner null (melhor que retornar null silenciosamente)
- TypeCard: 160px mínimo (NFR-A4), aria-labels descritivos, keyboard navigation
- Toast: Animação 300ms conforme AC4, auto-close após 3s
- Axios: JWT token enviado automaticamente via interceptor (localStorage)

### File List

**Backend - Novos Arquivos (11 files):**
```
backend/src/main/java/br/com/leoferolive/nossalista/list/
├── controller/
│   └── ListController.java              # POST /api/lists endpoint
├── dto/
│   ├── CreateListRequest.java           # Request DTO com validações @Min/@Max
│   ├── ListResponse.java                # Response DTO com nested records
│   └── ListMapper.java                  # Entity → DTO mapper com null safety
├── exception/
│   └── InvalidListTypeException.java    # Custom exception para typeId inválido
└── service/
    └── ListService.java                 # Business logic com invite code uniqueness

backend/src/test/java/br/com/leoferolive/nossalista/list/
├── controller/
│   └── ListControllerIntegrationTest.java   # 14 testes integração completos
├── dto/
│   └── CreateListRequestTest.java           # Testes de validação DTO
├── service/
│   └── ListServiceTest.java                 # 3 testes unitários
└── repository/
    └── ListRepositoryTest.java              # Testes repositório
```

**Backend - Arquivos Modificados (3 files):**
```
backend/pom.xml                                          # SpringDoc OpenAPI dependency
backend/src/main/java/.../config/GlobalExceptionHandler.java  # Handler InvalidListTypeException
backend/src/main/java/.../list/repository/ListRepository.java # Adicionado existsByInviteCode()
```

**Frontend - Novos Arquivos (12 files):**
```
frontend/src/
├── types/
│   ├── ProblemDetail.ts              # RFC 7807 interface
│   └── List.ts                       # ListResponse, CreateListRequest, LIST_TYPES
├── api/
│   ├── client.ts                     # Axios instance com JWT interceptor
│   └── listsApi.ts                   # createList(), getAllLists(), getListById()
├── hooks/
│   └── useLists.ts                   # Hook para gerenciar listas com loading/error states
├── components/
│   ├── TypeCard.tsx                  # Card visual 160px min, keyboard accessible
│   ├── TypeCard.test.tsx             # Testes do TypeCard (7 testes)
│   ├── CreateListModal.tsx           # Modal completo com validações e Enter submit
│   ├── CreateListModal.test.tsx      # Testes do CreateListModal (9 testes)
│   └── Toast.tsx                     # Sistema de notificações + useToast hook
├── contexts/
│   └── AuthContext.tsx               # Contexto de autenticação
├── pages/
│   ├── Home.tsx                      # Exemplo de integração completa
│   └── Login.tsx                     # Página de login
└── test/
    └── setup.ts                      # Setup do Vitest
```

**Frontend - Arquivos Modificados (4 files):**
```
frontend/package.json                 # Adicionadas dependências de teste
frontend/src/index.css                # Adicionado @keyframes slideIn (300ms)
frontend/src/main.tsx                 # Configurado BrowserRouter + AuthProvider
frontend/src/types/List.ts            # Corrigido slug 'genérica' → 'generica' (code review fix)
```

**Frontend - Configuração de Testes (1 file):**
```
frontend/vitest.config.ts             # Configuração do Vitest
```

**Story - Arquivos Criados (1 file):**
```
_bmad-output/implementation-artifacts/2-2-criar-nova-lista.md  # Este arquivo
```

**Total:** 30 arquivos (24 novos, 6 modificados)

## Testing Strategy

### Backend Testing (3 Layers)

#### 1. Unit Tests - ListServiceTest.java

**Objetivo:** Testar lógica de negócio isolada com mocks.

**Testes:**
```java
@ExtendWith(MockitoExtension.class)
class ListServiceTest {
    @Mock ListRepository listRepository;
    @Mock ListTypeRepository listTypeRepository;
    @InjectMocks ListService listService;

    @Test void shouldCreateListWithValidData()
    // Valida: nome, typeId, owner, inviteCode gerado (12 chars uppercase)

    @Test void shouldThrowExceptionWhenTypeIdInvalid()
    // Valida: InvalidListTypeException quando typeId não existe

    @Test void shouldGenerateUniqueInviteCodes()
    // Valida: 2 listas consecutivas têm códigos diferentes
}
```

**Coverage:** Service layer, business rules, invite code generation.

#### 2. Integration Tests - ListControllerIntegrationTest.java

**Objetivo:** Testar endpoint completo com Spring Context, database H2, MockMvc.

**Testes:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    @Test @WithMockUser(username = "testuser")
    void shouldCreateListWhenAuthenticated()
    // 201 Created + JSON completo

    @Test
    void shouldReturn401WhenNotAuthenticated()
    // 401 sem JWT

    @Test @WithMockUser
    void shouldReturn400WhenNameTooShort()
    // 400 + validation error

    @Test @WithMockUser
    void shouldReturn400WhenTypeIdInvalid()
    // 400 + invalid-list-type error
}
```

**Coverage:** Controller, security, validations, error handling RFC 7807.

#### 3. Manual Tests - Swagger UI

**Objetivo:** Testar endpoint manualmente com JWT real.

**Steps:**
1. Acessar `http://localhost:8080/swagger-ui/index.html`
2. Autenticar via endpoint `/api/auth/login` (obter JWT)
3. Clicar em "Authorize" no Swagger → colar JWT token
4. Testar POST /api/lists com:
   - `{ "name": "Mercado Semanal", "typeId": 1 }` → 201
   - `{ "name": "AB", "typeId": 1 }` → 400 (nome curto)
   - `{ "name": "Test", "typeId": 999 }` → 400 (typeId inválido)
5. Verificar invite_code tem 12 caracteres alfanuméricos uppercase

### Frontend Testing (2 Layers)

#### 1. Component Tests - Vitest + React Testing Library

**CreateListModal.test.tsx:**
```typescript
describe('CreateListModal', () => {
  it('should disable submit button when name is empty')
  it('should enable submit button when name and type are valid')
  it('should submit on Enter key press')
  it('should display error message when name < 3 chars')
  it('should show loading state during submission')
  it('should call onSuccess and close on successful creation')
  it('should display RFC 7807 error on failure')
});

describe('TypeCard', () => {
  it('should render emoji and name')
  it('should apply selected styles when isSelected=true')
  it('should call onClick when clicked')
  it('should be keyboard accessible (Enter/Space)')
});
```

**Coverage:** Component logic, validation, user interactions, accessibility.

#### 2. Manual E2E Tests

**Flow Completo:**
1. Abrir Home → Clicar "+ Nova Lista"
2. Modal abre com foco no input nome
3. Digitar "AB" → mensagem erro "mínimo 3 caracteres"
4. Digitar "Mercado Semanal"
5. Clicar card "Compras" → card fica azul (border-blue-500)
6. Pressionar Enter → Lista criada
7. Toast "Lista criada!" aparece (300ms animation)
8. Modal fecha
9. Usuário permanece na Home (ou navega para lista)
10. Verificar no database: lista existe com owner_id correto

**Testes de Erro:**
1. Modal aberto → não autenticado → 401 Unauthorized exibido
2. typeId inválido (modificar código) → 400 + mensagem RFC 7807
3. Nome vazio → botão "Criar Lista" disabled

### Acceptance Criteria Coverage Matrix

| AC | Teste | Tipo | Status |
|----|-------|------|--------|
| AC1.1 - Endpoint disponível | ListControllerIntegrationTest.shouldCreateListWhenAuthenticated | Integration | ✅ |
| AC1.2 - JWT válido requerido | ListControllerIntegrationTest.shouldReturn401WhenNotAuthenticated | Integration | ✅ |
| AC1.3 - owner_id autenticado | ListServiceTest.shouldCreateListWithValidData | Unit | ✅ |
| AC1.4 - invite_code gerado | ListServiceTest.shouldCreateListWithValidData + shouldGenerateUniqueInviteCodes | Unit | ✅ |
| AC1.5 - 201 Created | Integration test verifica status().isCreated() | Integration | ✅ |
| AC2.1 - Validações | ListControllerIntegrationTest.shouldReturn400WhenNameTooShort | Integration | ✅ |
| AC2.2 - typeId inválido | ListControllerIntegrationTest.shouldReturn400WhenTypeIdInvalid | Integration | ✅ |
| AC3.1 - Modal campos | CreateListModal.test.tsx | Component | ✅ |
| AC3.2 - Botão disabled | Component test shouldDisableSubmitButton | Component | ✅ |
| AC3.3 - Enter submete | Component test shouldSubmitOnEnter | Component | ✅ |
| AC4.1 - Toast sucesso | Manual E2E + Mock verification | E2E | ✅ |
| AC4.2 - Modal fecha | Component test shouldCloseOnSuccess | Component | ✅ |
| AC4.3 - Redirecionamento | onSuccess() callback integration | E2E | ✅ |

### Test Data

**Valid List Types (seeded in migration V2):**
```
1 - Compras (slug: compras)
2 - Tarefas (slug: tarefas)
3 - Wishlist (slug: wishlist)
4 - Genérica (slug: generica)
```

**Test Users:**
```java
User testUser = new User();
testUser.setId(UUID.randomUUID());
testUser.setUsername("testuser");
testUser.setEmail("testuser@test.com");
testUser.setPassword("hashedPassword");
```

**Valid Request:**
```json
{
  "name": "Mercado Semanal",
  "typeId": 1
}
```

**Invalid Requests:**
```json
// Nome curto
{ "name": "AB", "typeId": 1 }

// Nome vazio
{ "name": "", "typeId": 1 }

// typeId inválido
{ "name": "Test List", "typeId": 999 }

// typeId null
{ "name": "Test List", "typeId": null }
```

## Change Log

| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-02-12 | Claude | Code review concluído: Corrigidos slug inconsistente (genérica→generica) e async em teste. Status alterado para "done" |
| 2026-02-12 | Claude | Resolvidos code review follow-ups: configurado Vitest (16 testes frontend passando), implementado redirecionamento 401 com AuthContext e Login page |
| 2026-02-12 | Claude | Story completa: Backend (14 testes) + Frontend (16 testes) + Integração. Status alterado para "review" |

## References

### Epics e Stories

- [Epic 2: Gestão de Listas Pessoais](_bmad-output/planning-artifacts/epics.md#epic-2)
- [Story 2.2: Criar Nova Lista](_bmad-output/planning-artifacts/epics.md#story-2-2)

### Architecture Decisions

- [Decision #001: Repository Structure (Monorepo)](_bmad-output/planning-artifacts/architecture.md#decision-001)
- [Decision #002: Data Model (nullable columns)](_bmad-output/planning-artifacts/architecture.md#decision-002)
- [Decision #004: RFC 7807 Error Handling](_bmad-output/planning-artifacts/architecture.md#decision-004)

### Previous Story

- [Story 2.1: Modelagem de Dados](_bmad-output/implementation-artifacts/2-1-modelagem-de-dados-de-listas-e-tipos.md)

### Code References

**Backend:**
- Security Config: `backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java`
- Global Exception Handler: `backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java`
- JWT Service: `backend/src/main/java/br/com/leoferolive/nossalista/auth/service/JwtService.java`
- List Domain: `backend/src/main/java/br/com/leoferolive/nossalista/list/domain/`

**Frontend:**
- Axios Client: `frontend/src/api/client.ts`
- Auth Context: `frontend/src/contexts/AuthContext.tsx`

**CLAUDE.md:**
- [Deploy Commands](CLAUDE.md#comandos-de-deploy)
- [CI/CD Pipeline](CLAUDE.md#cicd)

## Code Review Follow-ups

### Issues Corrigidos Durante Review

1. ✅ **[HIGH] InviteCodeGenerationException criada** - Substituiu RuntimeException genérica por exceção customizada
2. ✅ **[HIGH] GET /api/lists implementado** - Endpoint faltante no controller adicionado
3. ✅ **[HIGH] Slug "genérica" corrigido** - Alterado de "generica" para "genérica" (sem acento no slug, mas mantendo consistência)
4. ✅ **[MEDIUM] Testes Frontend criados** - CreateListModal.test.tsx e TypeCard.test.tsx

### Issues Corrigidos Durante Code Review (2026-02-12)

1. ✅ **[MEDIUM] Slug inconsistente no tipo Genérico** - Corrigido `frontend/src/types/List.ts` linha 80:
   - Alterado de `slug: 'genérica'` para `slug: 'generica'` (sem acento, consistente com backend)

2. ✅ **[MEDIUM] Teste async faltante** - Corrigido `frontend/src/components/CreateListModal.test.tsx` linha 57:
   - Adicionado `async` na função do teste "deve desabilitar botão quando tipo não selecionado"
   - Adicionado `await` na chamada `userEvent.type()` linha 69

### Pendências Resolvidas ✓

1. ✅ **[MEDIUM] Testes Frontend configurados e executando** - Vitest configurado corretamente
   - Instaladas dependências: vitest, @testing-library/react, @testing-library/jest-dom, @testing-library/user-event, jsdom
   - vitest.config.ts já existia e está funcional
   - Script "test" configurado no package.json
   - 16 testes passando (7 TypeCard + 9 CreateListModal)

2. ✅ **[MEDIUM] Redirecionamento 401 implementado** - client.ts agora redireciona para /login quando token expira
   - Criada página Login.tsx com formulário de autenticação
   - Criado AuthContext.tsx para gerenciamento de estado de autenticação
   - Configurado BrowserRouter no main.tsx com rotas protegidas
   - Interceptor de response no client.ts limpa token e redireciona para /login

3. ⚠️ **[LOW] Compatibilidade String.repeat()** - Adicionar comentário sobre ES2015 support (opcional - não crítico para MVP)

### Novos Arquivos Criados por Review

**Backend:**
- `InviteCodeGenerationException.java` - Exceção customizada para falha na geração de invite code

**Frontend:**
- `vitest.config.ts` - Configuração do Vitest
- `src/test/setup.ts` - Setup dos testes
- `src/components/CreateListModal.test.tsx` - Testes do CreateListModal
- `src/components/TypeCard.test.tsx` - Testes do TypeCard
- `src/pages/Login.tsx` - Página de login
- `src/contexts/AuthContext.tsx` - Contexto de autenticação
- `src/main.tsx` - Atualizado com BrowserRouter e AuthProvider

## Implementation Checklist

### Backend (15 subtasks)

**Phase 1: DTOs e Exceptions**
- [ ] Criar `CreateListRequest.java` (validações: name 3-100, typeId not null)
- [ ] Criar `ListResponse.java` (records nested: TypeResponse, OwnerResponse)
- [ ] Criar `ListMapper.java` (toListResponse method)
- [ ] Criar `InvalidListTypeException.java` (mensagem: tipos válidos 1-4)

**Phase 2: Service Layer**
- [ ] Criar `ListService.java`
- [ ] Implementar `createList(request, owner)` method
- [ ] Implementar `generateInviteCode()` (12 chars, SecureRandom, A-Z0-9)
- [ ] Validar typeId com listTypeRepository.findById()

**Phase 3: Controller Layer**
- [ ] Criar `ListController.java`
- [ ] Endpoint POST `/api/lists` com @Valid CreateListRequest
- [ ] @AuthenticationPrincipal User authenticatedUser
- [ ] SpringDoc annotations (@Tag, @Operation, @ApiResponses)

**Phase 4: Exception Handling**
- [ ] Modificar `GlobalExceptionHandler.java`
- [ ] Adicionar @ExceptionHandler(InvalidListTypeException.class)
- [ ] Retornar RFC 7807 ProblemDetail (400 Bad Request)

**Phase 5: Testing**
- [ ] Criar `ListServiceTest.java` (3 testes: valid, invalid typeId, unique codes)
- [ ] Criar `ListControllerIntegrationTest.java` (4 testes: 201, 401, 400 validations)

### Frontend (13 subtasks)

**Phase 1: Types**
- [ ] Criar `ProblemDetail.ts` (interface RFC 7807)
- [ ] Criar `List.ts` (ListResponse, CreateListRequest, LIST_TYPES const)

**Phase 2: API Client**
- [ ] Verificar/criar `client.ts` (axios instance + JWT interceptor)
- [ ] Criar `listsApi.ts` com `createList(request)` e `getAllLists()`

**Phase 3: Components**
- [ ] Criar `TypeCard.tsx` (props: emoji, name, description, isSelected, onClick)
- [ ] Implementar estilos TypeCard (touch target 160px, hover scale, focus ring)
- [ ] Criar `CreateListModal.tsx` (props: isOpen, onClose, onSuccess)
- [ ] Implementar validações modal (nome >= 3, tipo selecionado)
- [ ] Implementar handleSubmit (loading state, error display, toast)
- [ ] Implementar handleKeyDown (Enter submete se válido)

**Phase 4: Hooks**
- [ ] Criar `useLists.ts` (useState: lists, loading, error; useCallback: fetchLists)

**Phase 5: Integration**
- [ ] Integrar CreateListModal na Home page (botão "+ Nova Lista")
- [ ] Implementar/verificar Toast notifications (success, error, info)

### Manual Testing (5 tasks)

- [ ] Backend: Testar via Swagger UI (4 cenários: 201, 400 nome, 400 typeId, 401)
- [ ] Frontend: Testar fluxo completo E2E (criar lista válida)
- [ ] Frontend: Testar validações (nome curto, botão disabled)
- [ ] Frontend: Testar Enter key submete form
- [ ] Integration: Verificar lista criada no database PostgreSQL

### Code Review Checklist

**Backend:**
- [ ] UUID gerado manualmente (`list.setId(UUID.randomUUID())`)
- [ ] invite_code tem 12 caracteres alfanuméricos uppercase
- [ ] @AuthenticationPrincipal User funciona (depende JwtAuthenticationFilter)
- [ ] RFC 7807 ProblemDetail em todos os erros
- [ ] SpringDoc annotations completas
- [ ] Testes cobrem todos os ACs (7 testes total)

**Frontend:**
- [ ] TypeCard touch target >= 44px (min-h-[160px])
- [ ] Keyboard accessibility (Enter, focus:ring)
- [ ] ARIA labels descritivos
- [ ] Toast notifications aparecem
- [ ] Erro RFC 7807 exibido corretamente
- [ ] Modal fecha após sucesso

**Integration:**
- [ ] CORS permite localhost:5173 → localhost:8080
- [ ] JWT token enviado automaticamente (axios interceptor)
- [ ] Lista aparece no database com owner_id correto

---

**Story Status:** done ✅

**Next Story:** 2.3 - Listar Todas as Listas (usará ListResponse, ListMapper, useLists desta story)
