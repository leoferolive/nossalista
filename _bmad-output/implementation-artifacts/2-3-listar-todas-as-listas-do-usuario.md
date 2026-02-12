# Story 2.3: Listar Todas as Listas do Usuário

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário autenticado,
I want ver todas as minhas listas na tela inicial,
So that possa acessar rapidamente o que preciso.

## Acceptance Criteria

### AC1: Backend - Endpoint GET /api/lists

**Given** o endpoint GET /api/lists está disponível
**When** faço request com JWT válido
**Then** response deve ser 200 OK com array de listas
**And** cada lista deve ter: id, name, type, owner_id, isOwner (boolean)
**And** deve vir ordenadas por updated_at DESC

### AC2: Frontend - Home Screen

**Given** Home screen no frontend
**When** carregada
**Then** deve mostrar:
- Header "Minhas Listas"
- Botão "+ Nova Lista"
- Grid de ListCards (responsive: 1/2/3 colunas)
**And** ListCard deve ter:
- Emoji do tipo
- Nome
- Contagem de itens
- Indicador "minha"/"compartilhada"
**And** touch target ≥ 44px (NFR-A4)

### AC3: Frontend - Estado Vazio

**Given** usuário sem listas
**When** Home screen é carregada
**Then** deve mostrar estado vazio com mensagem e botão "+ Criar Primeira Lista"

## Tasks / Subtasks

### Backend Implementation

- [ ] Implementar Repository Method (AC: #1)
  - [ ] Adicionar findByOwnerIdOrMemberId() no ListRepository
  - [ ] Query personalizada com JOIN em list_members
  - [ ] Ordenação por updated_at DESC
- [ ] Atualizar ListResponse DTO (AC: #1)
  - [ ] Adicionar campo isOwner (boolean calculado)
  - [ ] Adicionar campo itemsCount (opcional para listagem)
- [ ] Implementar Service Layer (AC: #1)
  - [ ] ListService.getAllListsForUser(userId)
  - [ ] Calcular isOwner comparando owner_id com userId
  - [ ] Mapear entities para ListResponse
- [ ] Implementar Controller Endpoint (AC: #1)
  - [ ] GET /api/lists em ListController
  - [ ] @AuthenticationPrincipal User
  - [ ] SpringDoc annotations completas
- [ ] Testes Backend (AC: #1)
  - [ ] ListServiceTest: listar listas onde usuário é owner
  - [ ] ListServiceTest: listar listas onde usuário é member
  - [ ] ListControllerIntegrationTest: 200 OK com array
  - [ ] ListControllerIntegrationTest: 401 sem autenticação
  - [ ] ListControllerIntegrationTest: array vazio se sem listas
  - [ ] ListControllerIntegrationTest: verificar ordenação DESC

### Frontend Implementation

- [ ] Criar ListCard Component (AC: #2)
  - [ ] Props: list (id, name, type, isOwner, itemsCount)
  - [ ] Renderizar emoji do tipo (usar LIST_TYPES de List.ts)
  - [ ] Renderizar nome da lista
  - [ ] Badge "Minha" se isOwner, "Compartilhada" caso contrário
  - [ ] Touch target mínimo 160px (NFR-A4)
  - [ ] Hover e focus states
  - [ ] onClick navega para /lists/{id}
- [ ] Atualizar useLists Hook (AC: #1, #2)
  - [ ] Adicionar getAllLists() - já existe da Story 2.2
  - [ ] Verificar se fetchLists() usa endpoint correto
  - [ ] Estado: lists, loading, error
- [ ] Atualizar Home Page (AC: #2, #3)
  - [ ] Header "Minhas Listas" + botão "+ Nova Lista"
  - [ ] Grid responsivo com ListCards
  - [ ] Estado vazio: mensagem + botão "+ Criar Primeira Lista"
  - [ ] Loading state enquanto carrega
  - [ ] Error state se falhar
  - [ ] useEffect para carregar listas ao montar
- [ ] Implementar Grid Responsivo (AC: #2)
  - [ ] Mobile (< 640px): 1 coluna
  - [ ] Tablet (640-1024px): 2 colunas
  - [ ] Desktop (> 1024px): 3 colunas
  - [ ] Gap adequado entre cards

### Manual Testing

- [ ] Backend: Testar via Swagger UI (cenários: owner, member, misto, vazio)
- [ ] Frontend: Testar Home com 0, 1, 3, 10 listas
- [ ] Frontend: Testar estados de loading e erro
- [ ] Frontend: Testar grid responsivo (mobile, tablet, desktop)
- [ ] Integration: Clicar em ListCard navega para ListView

## Dev Notes

### Epic Context

**Epic 2: Gestão de Listas Pessoais** - Usuários podem criar e gerenciar suas próprias listas.

**FRs Cobertos:**
- FR9: Usuário pode visualizar todas as listas que possui ou participa

**Story Sequence:**
- ✅ Story 2.1 COMPLETA: Modelagem de Dados (List, ListType entities, migrations V2)
- ✅ Story 2.2 EM REVIEW: Criar Nova Lista (POST /api/lists, CreateListModal)
- 🎯 Story 2.3 ATUAL: Listar Todas as Listas
- ⏳ Story 2.4: Ver detalhes de uma lista
- ⏳ Story 2.5: Editar nome da lista
- ⏳ Story 2.6: Excluir lista

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
- Tailwind CSS 3.4.19
- Axios (API calls com JWT interceptor automático)
- React Router
- Context API + hooks (useLists, useAuth)

### Padrões Arquiteturais Estabelecidos (Epic 1-2)

**Backend:**
1. **Constructor Injection:** Sem @Autowired
2. **RFC 7807 Problem Details:** Error handling consistente
3. **DTO Pattern:** Request/Response DTOs separados de entities
4. **Service Layer:** Business logic isolada
5. **Mapper Pattern:** Entity → DTO via mappers (ListMapper.toListResponse)
6. **SpringDoc:** @Tag, @Operation, @ApiResponse em controllers
7. **Repository Query Methods:** Spring Data JPA query derivation ou @Query

**Frontend:**
1. **Axios Client:** JWT token automático via interceptor (client.ts)
2. **Custom Hooks:** useLists, useAuth para encapsular lógica
3. **Type-Safe APIs:** Interface ProblemDetail, List types
4. **Toast Notifications:** Feedback visual consistente
5. **Accessible Components:** ARIA labels, keyboard navigation
6. **Responsive Grid:** Tailwind responsive classes (grid-cols-1 md:grid-cols-2 lg:grid-cols-3)

### Critical Implementation Requirements

#### 🔴 Backend - Query de Listas (Owner + Member)

O usuário precisa ver:
1. **Listas que ele criou** (owner_id = userId)
2. **Listas que ele participa** (existe em list_members com role MEMBER)

**Solução 1: Query Personalizada @Query (RECOMENDADO):**

```java
@Repository
public interface ListRepository extends JpaRepository<List, UUID> {

    @Query("SELECT DISTINCT l FROM List l " +
           "LEFT JOIN ListMember lm ON lm.list.id = l.id " +
           "WHERE l.owner.id = :userId OR lm.user.id = :userId " +
           "ORDER BY l.updatedAt DESC")
    java.util.List<List> findByOwnerIdOrMemberId(@Param("userId") UUID userId);
}
```

**Solução 2: Método Derivado (Menos Eficiente):**
```java
java.util.List<List> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
java.util.List<List> findByMembersUserIdOrderByUpdatedAtDesc(UUID userId);
// Service combina as duas listas e remove duplicatas
```

**ATENÇÃO:** Solução 1 é preferível - uma query, sem duplicatas, ordenação garantida.

#### 🔴 Backend - Campo isOwner no Response

O frontend precisa saber se o usuário é dono ou apenas membro (para mostrar badge correto).

**Opção A: Calcular no Mapper (RECOMENDADO):**

```java
public class ListMapper {
    public static ListResponse toListResponse(List list, UUID currentUserId) {
        boolean isOwner = list.getOwner().getId().equals(currentUserId);

        return new ListResponse(
            list.getId(),
            list.getName(),
            new TypeResponse(list.getType().getId(), list.getType().getName()),
            new OwnerResponse(
                list.getOwner().getId(),
                list.getOwner().getUsername(),
                list.getOwner().getName(),
                list.getOwner().getAvatarUrl()
            ),
            isOwner, // ← NOVO CAMPO
            0 // itemsCount - placeholder (pode ser calculado depois ou via JOIN COUNT)
        );
    }
}
```

**ListResponse atualizado:**
```java
public record ListResponse(
    UUID id,
    String name,
    TypeResponse type,
    OwnerResponse owner,
    boolean isOwner, // ← NOVO
    int itemsCount   // ← NOVO (opcional, pode começar com 0)
) {
    public record TypeResponse(int id, String name) {}
    public record OwnerResponse(UUID id, String username, String name, String avatarUrl) {}
}
```

#### 🔴 Frontend - Grid Responsivo com Tailwind

**Pattern estabelecido:**

```tsx
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  {lists.map(list => (
    <ListCard key={list.id} list={list} />
  ))}
</div>
```

- `grid-cols-1`: Mobile (< 768px) - 1 coluna
- `md:grid-cols-2`: Tablet (≥ 768px) - 2 colunas
- `lg:grid-cols-3`: Desktop (≥ 1024px) - 3 colunas
- `gap-4`: Espaçamento consistente (1rem)

#### 🔴 Frontend - ListCard Touch Target (NFR-A4)

**Mínimo: 44×44 pixels**

Implementar com `min-h-[160px]` para garantir área clicável adequada mesmo em mobile.

```tsx
export function ListCard({ list }: { list: ListResponse }) {
  const navigate = useNavigate();
  const typeEmoji = LIST_TYPES.find(t => t.id === list.type.id)?.emoji || '📝';

  return (
    <div
      onClick={() => navigate(`/lists/${list.id}`)}
      className="
        min-h-[160px]
        border border-gray-200 rounded-lg p-4
        hover:shadow-lg hover:border-blue-400
        transition-all cursor-pointer
        focus:outline-none focus:ring-2 focus:ring-blue-500
      "
      tabIndex={0}
      role="button"
      aria-label={`Abrir lista ${list.name}`}
    >
      {/* Conteúdo do card */}
    </div>
  );
}
```

#### 🔴 Frontend - Estado Vazio (Empty State)

**Pattern UX:**

```tsx
{lists.length === 0 && !loading && (
  <div className="text-center py-12">
    <p className="text-gray-500 mb-4 text-lg">
      Você ainda não tem listas. Crie sua primeira lista!
    </p>
    <button
      onClick={() => setShowCreateModal(true)}
      className="btn btn-primary"
    >
      + Criar Primeira Lista
    </button>
  </div>
)}
```

#### 🔴 Frontend - Badge "Minha" vs "Compartilhada"

```tsx
{list.isOwner ? (
  <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
    Minha
  </span>
) : (
  <span className="text-xs bg-gray-100 text-gray-800 px-2 py-1 rounded">
    Compartilhada
  </span>
)}
```

### Validation Rules

**Endpoint GET /api/lists:**
- JWT obrigatório (401 se ausente/inválido)
- Sem query parameters necessários
- Response sempre array (vazio [] se sem listas)

**Business Rules:**
- Usuário vê APENAS listas onde é owner OU member
- Ordenação: updated_at DESC (listas recentemente modificadas primeiro)
- isOwner = true se owner_id == currentUserId

### Error Handling

**200 OK:**
- Array vazio `[]` se usuário não tem listas

**401 Unauthorized:**
- JWT ausente ou inválido

**500 Internal Server Error (evitar):**
- Se query falhar, retornar array vazio com log (graceful degradation)

### Integration Points

**Story 2.2 (Criar Nova Lista):**
- ✅ ListResponse DTO já existe (precisa adicionar isOwner, itemsCount)
- ✅ ListMapper já existe (precisa adicionar currentUserId parameter)
- ✅ useLists hook já existe (API call getAllLists provavelmente já implementada)
- ✅ CreateListModal já existe (botão "+ Nova Lista" já funciona)

**Story 2.4 (Ver Detalhes):**
- ListCard onClick deve navegar para `/lists/{id}` (preparar para Story 2.4)

## Project Structure Notes

### Backend Files to Modify (4 files)

```
backend/src/main/java/br/com/leoferolive/nossalista/list/
├── repository/
│   └── ListRepository.java              # Adicionar findByOwnerIdOrMemberId()
├── dto/
│   ├── ListResponse.java                # Adicionar isOwner, itemsCount
│   └── ListMapper.java                  # Adicionar currentUserId param, calcular isOwner
└── controller/
    └── ListController.java              # Adicionar GET /api/lists
```

### Backend Files to Create (1 file)

```
backend/src/test/java/br/com/leoferolive/nossalista/list/
└── controller/
    └── ListControllerGetAllTest.java    # Testes específicos para GET /api/lists
```

**OU adicionar aos testes existentes:**
```
backend/src/test/java/br/com/leoferolive/nossalista/list/
└── controller/
    └── ListControllerIntegrationTest.java  # Adicionar testes GET /api/lists
```

### Frontend Files to Create (1 file)

```
frontend/src/components/
└── ListCard.tsx                         # Card visual para lista
```

### Frontend Files to Modify (1 file)

```
frontend/src/pages/
└── Home.tsx                             # Grid de listas + estado vazio
```

### Frontend Files to Verify (Já existem da Story 2.2)

```
frontend/src/
├── hooks/
│   └── useLists.ts                      # Verificar se getAllLists() já existe
├── api/
│   └── listsApi.ts                      # Verificar se getAllLists() já existe
└── types/
    └── List.ts                          # Atualizar ListResponse com isOwner, itemsCount
```

### Existing Files from Previous Stories

**Story 2.1:**
- List entity, ListType, ListRepository básico

**Story 2.2:**
- ListResponse DTO, ListMapper, ListService, ListController (POST /api/lists)
- useLists hook, listsApi, CreateListModal, Toast

## Dev Agent Record

### Previous Story Intelligence (Story 2.2)

**Story 2.2 Status:** EM REVIEW (backend + frontend completos, pronto para code review)

**Key Learnings:**

1. **ListResponse DTO Structure:**
   - Nested records: TypeResponse, OwnerResponse
   - Pattern estabelecido: dados mínimos para listagem, detalhes vêm em endpoints específicos

2. **ListMapper Pattern:**
   - Static method `toListResponse(List entity)`
   - Null safety: IllegalStateException se owner null
   - Precisa ser atualizado para incluir currentUserId e calcular isOwner

3. **useLists Hook:**
   - Estado: lists, loading, error
   - Funções: fetchLists, createList, refetch
   - Axios client com JWT interceptor automático

4. **CreateListModal:**
   - TypeCard components (160px touch target, keyboard accessible)
   - Toast notifications (300ms animation)
   - Enter submete form, Escape fecha modal

5. **Home Page (Story 2.2):**
   - Botão "+ Nova Lista" abre CreateListModal
   - Após criar lista, refetch é chamado
   - **IMPORTANTE:** Grid de listas provavelmente ainda NÃO implementado (Story 2.2 foca em CRIAR, não LISTAR)

**Potenciais Conflitos/Ajustes:**
- Story 2.2 pode ter implementado `getAllLists()` no listsApi.ts como preparação
- Home.tsx pode ter exemplo básico de listagem, precisa ser expandido para grid completo
- ListResponse precisa ser atualizado (isOwner, itemsCount) - **mudança breaking se Story 2.2 ainda não commitada**

**Arquivos Criados na Story 2.2 (reusar):**
- `ListResponse.java`, `ListMapper.java` (modificar)
- `ListController.java` (adicionar GET endpoint)
- `useLists.ts` (verificar/atualizar)
- `listsApi.ts` (verificar/atualizar)
- `Home.tsx` (expandir grid)

### Git Intelligence Summary

**Recent Commits:**
```
9ec1904 feat(list): implement list data model with types (story 2-1)
77977fe feat(user): add user profile and search endpoints with comprehensive tests
ee6bf0c refactor(database): migrate dev environment from H2 to PostgreSQL via Docker Compose
```

**Commit Message Pattern:**
- Format: `<type>(<scope>): <description> (story X-Y)`
- Types: feat, fix, refactor, chore, test, docs
- Scope: module name (list, user, database, etc.)

**Code Patterns from Recent Work:**

1. **Comprehensive Tests:** Todos os commits incluem testes (unit + integration)
2. **PostgreSQL Dev:** Dev environment usa PostgreSQL via Docker (não H2)
3. **Repository Pattern:** findByOwnerId(), findByInviteCode() - queries personalizadas são comuns
4. **DTO Nested Records:** Pattern de records nested (TypeResponse, OwnerResponse)

**Files Modified Recently (relacionados):**
- `ListRepository.java` - Story 2.2 adicionou `existsByInviteCode()`
- `GlobalExceptionHandler.java` - Story 2.2 adicionou handler para InvalidListTypeException

**Next Commit Message (sugestão):**
```
feat(list): implement list retrieval endpoint (story 2-3)

- Add GET /api/lists endpoint to list all user lists
- Update ListResponse with isOwner and itemsCount fields
- Implement findByOwnerIdOrMemberId query in ListRepository
- Add ListCard component with responsive grid
- Add empty state to Home page
- Tests: 6 integration tests for GET /api/lists
```

### Latest Tech Information

**Stack Versions (confirmado nos commits recentes):**
- Spring Boot: 4.0.2
- Java: 25
- PostgreSQL: 16+ (Docker Compose)
- React: 19
- TypeScript: 5+
- Vite: 5.x
- Tailwind CSS: 3.4.19

**Dependencies Confirmadas:**
- SpringDoc OpenAPI (pom.xml atualizado na Story 2.2)
- Axios (frontend/package.json)
- JWT (jjwt library no backend)

**Nenhuma Mudança de Versão Necessária:** Stack estável, sem atualizações críticas.

### Architecture Compliance

**Decision #001: Monorepo Structure**
- ✅ Backend: `backend/src/main/java/br/com/leoferolive/nossalista/list/`
- ✅ Frontend: `frontend/src/components/`, `frontend/src/pages/`

**Decision #002: Data Model**
- ✅ Entities JPA: List, ListType, User
- ✅ Migrations: Flyway V2 (list_types, lists)
- ⚠️ **NOTA:** Story 4.1 adicionará `list_members` table - query precisa considerar futuro JOIN

**Decision #003: API Design**
- ✅ SpringDoc OpenAPI 3 para documentação
- ✅ Endpoints RESTful: GET /api/lists
- ✅ Response sempre JSON

**Decision #004: RFC 7807 Error Handling**
- ✅ ProblemDetail para erros 4xx/5xx
- ✅ GlobalExceptionHandler já configurado

**Decision #006: Frontend State Management**
- ✅ Context API: AuthContext (JWT)
- ✅ Custom hooks: useLists, useAuth
- ⚠️ Sem Redux - lógica no hook (fetchLists, loading, error)

**NFR-A4: Touch Targets ≥ 44px**
- ✅ TypeCard: 160px (Story 2.2)
- 🎯 ListCard: mínimo 160px altura (garantir área clicável)

**NFR-P2: Time to Interactive < 3s (4G)**
- ⚠️ Grid responsivo: evitar renderizar TODOS os cards se lista > 100 (considerar virtualization se necessário)
- ✅ Code splitting: Home page pode ser lazy loaded

## References

### Epics e Stories

- [Epic 2: Gestão de Listas Pessoais](_bmad-output/planning-artifacts/epics.md#epic-2)
- [Story 2.3: Listar Todas as Listas](_bmad-output/planning-artifacts/epics.md#story-2-3)

### Architecture Decisions

- [Decision #001: Repository Structure (Monorepo)](_bmad-output/planning-artifacts/architecture.md#decision-001)
- [Decision #002: Data Model](_bmad-output/planning-artifacts/architecture.md#decision-002)
- [Decision #003: API Design](_bmad-output/planning-artifacts/architecture.md#decision-003)
- [Decision #004: RFC 7807 Error Handling](_bmad-output/planning-artifacts/architecture.md#decision-004)
- [Decision #006: Frontend State Management](_bmad-output/planning-artifacts/architecture.md#decision-006)

### Previous Stories

- [Story 2.1: Modelagem de Dados](_bmad-output/implementation-artifacts/2-1-modelagem-de-dados-de-listas-e-tipos.md)
- [Story 2.2: Criar Nova Lista](_bmad-output/implementation-artifacts/2-2-criar-nova-lista.md)

### Code References

**Backend:**
- List Entity: `backend/src/main/java/br/com/leoferolive/nossalista/list/domain/List.java`
- ListRepository: `backend/src/main/java/br/com/leoferolive/nossalista/list/repository/ListRepository.java`
- ListService: `backend/src/main/java/br/com/leoferolive/nossalista/list/service/ListService.java`
- ListController: `backend/src/main/java/br/com/leoferolive/nossalista/list/controller/ListController.java`
- ListMapper: `backend/src/main/java/br/com/leoferolive/nossalista/list/dto/ListMapper.java`

**Frontend:**
- Home Page: `frontend/src/pages/Home.tsx`
- useLists Hook: `frontend/src/hooks/useLists.ts`
- listsApi: `frontend/src/api/listsApi.ts`
- List Types: `frontend/src/types/List.ts`

**CLAUDE.md:**
- [Deploy Commands](CLAUDE.md#comandos-de-deploy)
- [Stack Técnico](CLAUDE.md#stack-técnico-planejada)

## Implementation Checklist

### Backend (10 subtasks)

**Phase 1: Repository Layer**
- [ ] Adicionar @Query para findByOwnerIdOrMemberId() em ListRepository
- [ ] Verificar ordenação por updated_at DESC
- [ ] Testar query manualmente (H2 console ou logs SQL)

**Phase 2: DTO Updates**
- [ ] Atualizar ListResponse record: adicionar boolean isOwner, int itemsCount
- [ ] Atualizar ListMapper.toListResponse: adicionar UUID currentUserId parameter
- [ ] Calcular isOwner: owner.id.equals(currentUserId)
- [ ] Placeholder itemsCount = 0 (calcular depois se necessário)

**Phase 3: Service Layer**
- [ ] Adicionar ListService.getAllListsForUser(UUID userId)
- [ ] Chamar repository.findByOwnerIdOrMemberId(userId)
- [ ] Mapear cada List para ListResponse usando ListMapper.toListResponse(list, userId)

**Phase 4: Controller Layer**
- [ ] Adicionar GET /api/lists endpoint em ListController
- [ ] @AuthenticationPrincipal User authenticatedUser
- [ ] Chamar listService.getAllListsForUser(authenticatedUser.getId())
- [ ] SpringDoc annotations: @Operation, @ApiResponse 200/401

**Phase 5: Testing**
- [ ] Testes integração: 200 OK com array de listas
- [ ] Testes integração: 401 sem autenticação
- [ ] Testes integração: array vazio [] se usuário sem listas
- [ ] Testes integração: verificar ordenação por updated_at DESC
- [ ] Testes integração: verificar isOwner correto (owner vs member)

### Frontend (8 subtasks)

**Phase 1: Types**
- [ ] Atualizar List.ts: adicionar isOwner, itemsCount em ListResponse interface

**Phase 2: ListCard Component**
- [ ] Criar ListCard.tsx component
- [ ] Props: list (ListResponse)
- [ ] Renderizar emoji (usar LIST_TYPES.find(t => t.id === list.type.id)?.emoji)
- [ ] Renderizar nome da lista
- [ ] Badge "Minha" (isOwner) ou "Compartilhada" (!isOwner)
- [ ] Touch target: min-h-[160px]
- [ ] onClick navega para /lists/{id} via useNavigate
- [ ] Keyboard accessible: tabIndex={0}, role="button", aria-label

**Phase 3: Home Page Updates**
- [ ] Atualizar Home.tsx: adicionar grid responsivo (grid-cols-1 md:grid-cols-2 lg:grid-cols-3)
- [ ] Renderizar ListCard para cada lista
- [ ] Estado vazio: mensagem + botão "+ Criar Primeira Lista"
- [ ] Loading state: skeleton ou spinner
- [ ] Error state: mensagem de erro + botão "Tentar Novamente"
- [ ] useEffect: carregar listas ao montar (fetchLists())

**Phase 4: Hooks/API Verification**
- [ ] Verificar useLists.ts: método fetchLists() ou getAllLists() existe
- [ ] Verificar listsApi.ts: getAllLists() existe e usa GET /api/lists
- [ ] Se não existir, criar getAllLists() no listsApi.ts
- [ ] Se não existir, adicionar fetchLists() no useLists hook

### Manual Testing (5 tasks)

- [ ] Backend: Swagger UI - GET /api/lists retorna array vazio
- [ ] Backend: Swagger UI - GET /api/lists com 1 lista (owner) retorna isOwner=true
- [ ] Backend: Swagger UI - GET /api/lists com lista compartilhada retorna isOwner=false
- [ ] Frontend: Home com 0 listas mostra estado vazio
- [ ] Frontend: Home com 3+ listas mostra grid responsivo correto
- [ ] Frontend: Clicar ListCard navega para /lists/{id}
- [ ] Frontend: Badge "Minha" vs "Compartilhada" correto

### Code Review Checklist

**Backend:**
- [ ] Query findByOwnerIdOrMemberId usa LEFT JOIN (evitar duplicatas)
- [ ] Ordenação updated_at DESC funciona
- [ ] isOwner calculado corretamente
- [ ] itemsCount placeholder (0) ou query com COUNT?
- [ ] SpringDoc annotations completas
- [ ] Testes cobrem: array vazio, owner, member, ordenação

**Frontend:**
- [ ] ListCard touch target ≥ 44px (min-h-[160px])
- [ ] Grid responsivo: 1/2/3 colunas corretas
- [ ] Estado vazio aparece corretamente
- [ ] Loading e error states implementados
- [ ] Badge "Minha"/"Compartilhada" visível e correto
- [ ] Navegação para /lists/{id} funciona
- [ ] Keyboard accessible (Enter abre lista)

**Integration:**
- [ ] JWT token enviado automaticamente (axios interceptor)
- [ ] Array vazio [] não quebra UI
- [ ] Lista aparece após criar nova (Story 2.2 integration)

---

**Story Status:** ready-for-dev ✅

**Next Story:** 2.4 - Ver Detalhes de uma Lista (usará GET /api/lists/{id})
