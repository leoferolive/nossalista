# Story 3.4: Marcar/Desmarcar Item como Concluído

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participante de uma lista,
I want marcar itens como concluídos,
So that possa controlar o que já fiz/comprei.

## Acceptance Criteria

**Given** o endpoint PATCH /api/lists/{listId}/items/{itemId}/check está disponível
**When** faço request com JWT válido e sou participante da lista
**Then** response deve ser 200 OK com item atualizado e checked invertido (toggle)

**Given** o endpoint PATCH /api/lists/{listId}/items/{itemId}/check
**When** item não existe
**Then** response deve ser 404 Not Found conforme RFC 7807

**Given** o endpoint PATCH /api/lists/{listId}/items/{itemId}/check
**When** usuário não é participante da lista
**Then** response deve ser 403 Forbidden conforme RFC 7807

**Given** ListItem component
**When** usuário toca no checkbox
**Then** request PATCH é enviado, animação "pop" executa (300ms), checkbox muda estado, Toast "Sincronizado" aparece
**And** optimistic UI: estado visual muda imediatamente antes da resposta

**Given** checkbox animação "pop"
**When** executada
**Then** keyframes: scale(0.8) → scale(1.2) → scale(1.0), 300ms, cubic-bezier(0.175, 0.885, 0.32, 1.275)

**Given** ListItem
**When** usuário toca no texto/nome (não no checkbox)
**Then** modal de edição abre (Story 3.5), checkbox NÃO alterado

**Given** erro de rede ao marcar item
**When** request falha
**Then** checkbox reverte para estado anterior, Toast "Erro ao sincronizar" aparece

## Tasks / Subtasks

- [x] Task 1: Criar método toggleItemCheck no ListItemService (AC: Toggle checked)
  - [x] 1.1: Adicionar método `toggleItemCheck(UUID listId, UUID itemId, User user)` no ListItemService
  - [x] 1.2: Validar que lista existe (404 se não)
  - [x] 1.3: Validar que usuário é participante (403 se não)
  - [x] 1.4: Validar que item existe e pertence à lista (404 se não)
  - [x] 1.5: Inverter estado checked (toggle: true → false, false → true)
  - [x] 1.6: Salvar item atualizado
  - [x] 1.7: Retornar ListItemResponseDTO atualizado

- [x] Task 2: Criar endpoint PATCH no ListItemController (AC: Endpoint REST PATCH)
  - [x] 2.1: Adicionar método PATCH `@PatchMapping("/{itemId}/check")` no ListItemController
  - [x] 2.2: Path: `/api/lists/{listId}/items/{itemId}/check`
  - [x] 2.3: Extrair user do JWT via @AuthenticationPrincipal
  - [x] 2.4: Chamar ListItemService.toggleItemCheck()
  - [x] 2.5: Retornar ResponseEntity.ok(responseDTO)
  - [x] 2.6: Documentação OpenAPI/Swagger completa

- [x] Task 3: Adicionar API call no frontend (AC: Comunicação com backend)
  - [x] 3.1: Adicionar `toggleItemCheck(listId, itemId)` em `api/itemsApi.ts`
  - [x] 3.2: Retornar Promise<ListItem> com item atualizado
  - [x] 3.3: Tratar erros 404, 403, 401 com mensagens apropriadas

- [x] Task 4: Atualizar hook useItems (AC: Gerenciamento de estado)
  - [x] 4.1: Adicionar `toggleItem(itemId)` no hook useItems
  - [x] 4.2: Implementar optimistic update: atualizar estado local imediatamente
  - [x] 4.3: Reverter estado em caso de erro
  - [x] 4.4: Retornar função toggleItem para componentes

- [x] Task 5: Integrar checkbox com toggle na ListView (AC: UI interativa)
  - [x] 5.1: Passar função toggleItem para prop onToggle do ListItem
  - [x] 5.2: Implementar handler onToggle no ListView
  - [x] 5.3: Mostrar Toast "Sincronizado" após sucesso
  - [x] 5.4: Mostrar Toast "Erro ao sincronizar" em caso de falha
  - [x] 5.5: Garantir que clique no texto abra modal (não toggle)

- [x] Task 6: Testes backend (AC: Cobertura de testes)
  - [x] 6.1: Teste de unidade: toggleItemCheck inverte checked com sucesso
  - [x] 6.2: Teste: toggle de true para false
  - [x] 6.3: Teste: toggle de false para true
  - [x] 6.4: Teste: erro 404 quando item não existe
  - [x] 6.5: Teste: erro 403 quando usuário não é participante
  - [x] 6.6: Teste: erro 404 quando item não pertence à lista
  - [x] 6.7: Teste de integração: PATCH endpoint completo

- [x] Task 7: Testes frontend (AC: Componentes testados)
  - [x] 7.1: Teste ListItem chama onToggle ao clicar no checkbox
  - [x] 7.2: Teste ListItem chama onEdit (não onToggle) ao clicar no texto
  - [x] 7.3: Teste useItems toggleItem faz optimistic update
  - [x] 7.4: Teste useItems reverte em caso de erro
  - [x] 7.5: Teste Toast aparece após sincronização

## Dev Notes

### 🎯 Contexto da Story

Esta é a **QUARTA STORY** do Epic 3 (Gestão de Itens), implementando a funcionalidade de marcar/desmarcar itens como concluídos.

**Epic 3 (PROGRESSO):**
- Story 3.1 (done): Modelagem de dados (ListItem entity, repository, DTOs)
- Story 3.2 (done): Adicionar item (POST /api/lists/{id}/items)
- Story 3.3 (done): Listar itens (GET /api/lists/{id}/items, componente ListItem)
- **Story 3.4 (atual):** Marcar/desmarcar item (PATCH /api/lists/{id}/items/{itemId}/check)

**Objetivo Principal:** Implementar **toggle do campo checked** com:
1. Endpoint REST PATCH para toggle no backend
2. Validações de permissão (participante da lista)
3. UI com checkbox clicável e animação "pop"
4. Optimistic UI para resposta imediata
5. Feedback via Toast (sucesso/erro)

**FRs Cobertos (Epics.md):**
- FR18: Participante da lista pode marcar/desmarcar item como concluído

### 🏗️ Decisões Arquiteturais Relevantes

**Decision #002: Data Model - Campos Dinâmicos por Tipo (Architecture.md):**
> "Colunas nullable em list_items para campos dinâmicos (quantity, due_date, url)"

**Campo checked:**
- Já existe na entidade ListItem (boolean, default false)
- Usado para marcar item como concluído
- Deve ser togglável (true ↔ false)

**Permissionamento (Story 3.2/3.3 Dev Notes):**
- Qualquer participante pode marcar/desmarcar itens (dono ou membro)
- Verificação via `isParticipant(list, user)` já implementado
- Retornar 403 Forbidden se não autorizado

**Pattern de Toggle:**
- Operação idempotente (mesmo resultado se chamada múltiplas vezes)
- Inverte estado atual: `item.setChecked(!item.isChecked())`
- Retorna item atualizado no response

### 📦 Stack Técnico Específico

**Backend:**
- Spring Boot 4.0.2 + Java 25
- Spring Data JPA
- ListItemRepository já possui `findById` e save
- RFC 7807 ProblemDetail para erros

**Frontend:**
- React 19 + TypeScript
- Axios para API calls
- Animação "pop" já existe em index.css
- Toast system já implementado (useToast)
- Optimistic UI pattern

**Entidades JPA Já Existentes:**
- ListItem.java - Entidade completa com campo checked
- ListItemRepository.java - Repository padrão
- DTOs criados: ListItemResponseDTO
- ListItemMapper.java - Mapper para converter entity → DTO

**Service Já Existente:**
- ListItemService.java - Já tem método `isParticipant()` e validações

### 🔐 Segurança - Considerações

**Regras de Acesso:**
- Usuário deve ser participante da lista (dono OU membro)
- Item deve pertencer à lista especificada
- Retornar 403 Forbidden se não autorizado
- Retornar 404 Not Found se item ou lista não existir

**Validações Necessárias:**
1. Lista existe
2. Usuário é participante
3. Item existe
4. Item pertence à lista especificada

### 🎨 Estrutura de Código Backend

**Arquivos a Modificar:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── controller/
│   │   └── ListItemController.java       # [MODIFICAR] Adicionar PATCH endpoint
│   └── service/
│       └── ListItemService.java          # [MODIFICAR] Adicionar toggleItemCheck

test/java/br/com/leoferolive/nossalista/listitem/
├── service/
│   └── ListItemServiceTest.java          # [MODIFICAR] +testes toggle
└── controller/
    └── ListItemControllerTest.java       # [MODIFICAR] +testes endpoint
```

**Convenções de Código:**
- Constructor injection (sem @Autowired)
- @Transactional para operações de escrita
- RFC 7807 ProblemDetail para erros
- ResponseEntity para controle de status HTTP

### 🎨 Estrutura de Código Frontend

**Arquivos a Modificar:**

```
frontend/src/
├── api/
│   └── itemsApi.ts                       # [MODIFICAR] +toggleItemCheck
├── hooks/
│   └── useItems.ts                       # [MODIFICAR] +toggleItem (optimistic)
├── components/
│   └── ListItem.tsx                      # [MODIFICAR] Integrar onToggle
└── pages/
    └── ListView.tsx                      # [MODIFICAR] Handler + Toast
```

**Padrões UI/UX:**
- Checkbox customizado com animação "pop" (300ms) - JÁ EXISTE
- Optimistic UI: estado muda imediatamente
- Toast feedback: "Sincronizado" (success) ou "Erro ao sincronizar" (error)
- Touch targets ≥ 44px (NFR-A4)
- Separação clara: checkbox (toggle) vs texto (editar)

### 📋 Especificação Detalhada

**1. ListItemService.java - Novo Método**

```java
/**
 * Toggle do estado checked de um item
 * Valida permissões (usuário deve ser participante)
 *
 * @param listId ID da lista
 * @param itemId ID do item
 * @param user   Usuário solicitante
 * @return DTO com item atualizado
 * @throws ListNotFoundException  se a lista não existir
 * @throws ItemNotFoundException  se o item não existir
 * @throws ForbiddenException     se o usuário não for participante
 */
@Transactional
public ListItemResponseDTO toggleItemCheck(UUID listId, UUID itemId, User user) {
    // 1. Verificar se lista existe
    List list = listRepository.findById(listId)
            .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

    // 2. Verificar se usuário é participante
    if (!isParticipant(list, user)) {
        throw new ForbiddenException("Você não tem permissão para modificar itens desta lista");
    }

    // 3. Buscar item
    ListItem item = listItemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item não encontrado"));

    // 4. Verificar se item pertence à lista
    if (!item.getList().getId().equals(listId)) {
        throw new ItemNotFoundException("Item não encontrado nesta lista");
    }

    // 5. Toggle checked
    item.setChecked(!item.isChecked());

    // 6. Salvar (updated_at atualizado automaticamente via @PreUpdate)
    ListItem saved = listItemRepository.save(item);

    // 7. Log
    log.info("Item toggled: itemId={}, checked={}, listId={}, user={}",
            itemId, saved.isChecked(), listId, user.getId());

    // 8. Retornar DTO
    return listItemMapper.toListItemResponseDTO(saved);
}
```

**2. ListItemController.java - Novo Endpoint**

```java
/**
 * PATCH /api/lists/{listId}/items/{itemId}/check
 * Toggle do estado checked de um item
 */
@PatchMapping("/{itemId}/check")
@Operation(
    summary = "Marcar/desmarcar item como concluído",
    description = "Inverte o estado checked do item (toggle). " +
                  "Usuário deve ser dono ou membro da lista."
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Item atualizado com sucesso",
        content = @Content(schema = @Schema(implementation = ListItemResponseDTO.class))
    ),
    @ApiResponse(
        responseCode = "401",
        description = "Não autenticado (JWT ausente ou inválido)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
        responseCode = "403",
        description = "Usuário não tem permissão para modificar itens desta lista",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Lista ou item não encontrado",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
})
public ResponseEntity<ListItemResponseDTO> toggleItemCheck(
        @PathVariable UUID listId,
        @PathVariable UUID itemId,
        @AuthenticationPrincipal User user) {

    ListItemResponseDTO updated = listItemService.toggleItemCheck(listId, itemId, user);
    return ResponseEntity.ok(updated);
}
```

**3. Frontend - API Call**

```typescript
// api/itemsApi.ts
async toggleItemCheck(listId: string, itemId: string): Promise<ListItem> {
  try {
    const response = await client.patch<ListItem>(
      `/api/lists/${listId}/items/${itemId}/check`
    );
    return response.data;
  } catch (error) {
    // Tratar erros 404, 403, 401
    const axiosError = error as AxiosError<ProblemDetail>;
    // ... mensagens específicas
    throw new Error(message);
  }
}
```

**4. Frontend - Hook (Optimistic Update)**

```typescript
// hooks/useItems.ts
const toggleItem = useCallback(
  async (itemId: string): Promise<ListItem> => {
    // Encontrar item atual
    const item = items.find(i => i.id === itemId);
    if (!item) throw new Error('Item não encontrado');

    const originalChecked = item.checked;
    const newChecked = !originalChecked;

    // Optimistic update: atualizar estado local imediatamente
    setItems(prev =>
      prev.map(i =>
        i.id === itemId ? { ...i, checked: newChecked } : i
      )
    );

    try {
      // Fazer request para backend
      const updated = await itemsApi.toggleItemCheck(listId, itemId);
      return updated;
    } catch (err) {
      // Reverter em caso de erro
      setItems(prev =>
        prev.map(i =>
          i.id === itemId ? { ...i, checked: originalChecked } : i
        )
      );
      throw err;
    }
  },
  [items, listId]
);
```

**5. Frontend - ListView Integration**

```typescript
// pages/ListView.tsx
const handleToggleItem = async (itemId: string) => {
  try {
    await toggleItem(itemId);
    showToast('Sincronizado', 'success');
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Erro ao sincronizar';
    showToast(message, 'error');
  }
};

// Renderização do ListItem
<ListItemComponent
  item={item}
  onToggle={handleToggleItem}
  onEdit={handleEditItem}
/>
```

### 🧪 Testes e Validação

**Testes Unitários (ListItemServiceTest):**

```java
@Test
void shouldToggleCheckedFromFalseToTrue() {
    // Given
    UUID listId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    User user = createTestUser();
    List list = createTestList(user);
    ListItem item = createItem(list, "Item", false); // checked = false

    when(listRepository.findById(listId)).thenReturn(Optional.of(list));
    when(listItemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(listItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    // When
    ListItemResponseDTO result = service.toggleItemCheck(listId, itemId, user);

    // Then
    assertTrue(result.checked());
}

@Test
void shouldToggleCheckedFromTrueToFalse() {
    // Similar, mas com checked = true inicialmente
}

@Test
void shouldThrow404WhenItemNotFound() {
    // Given
    when(listItemRepository.findById(any())).thenReturn(Optional.empty());

    // When/Then
    assertThrows(ItemNotFoundException.class, () ->
        service.toggleItemCheck(listId, itemId, user));
}

@Test
void shouldThrow403WhenNotParticipant() {
    // Similar aos testes anteriores de permissão
}

@Test
void shouldThrow404WhenItemDoesNotBelongToList() {
    // Item existe mas pertence a outra lista
}
```

**Testes de Integração (ListItemControllerTest):**

```java
@Test
void shouldToggleItemCheckEndpoint() throws Exception {
    mockMvc.perform(patch("/api/lists/{listId}/items/{itemId}/check", listId, itemId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checked").value(true));
}
```

### 🚨 Armadilhas Comuns a Evitar

1. **Não validar se item pertence à lista** - Sempre verificar `item.getList().getId().equals(listId)`
2. **Esquecer @Transactional** - Operação de escrita precisa de transação
3. **Não fazer optimistic update** - UI deve responder imediatamente
4. **Não reverter em caso de erro** - Estado deve voltar ao original se falhar
5. **Toast genérico** - Mensagens específicas: "Sincronizado" vs "Erro ao sincronizar"
6. **Clique no texto dispara toggle** - Separar cliques: checkbox (toggle) vs texto (editar)
7. **Não atualizar updated_at** - Usar @PreUpdate na entidade (já configurado)

### 🔗 Relacionamento com Outras Stories

**Depende de:**
- ✅ Story 3.1: Modelagem de dados (ListItem entity com campo checked)
- ✅ Story 3.2: Adicionar item (padrões de validação e permissão)
- ✅ Story 3.3: Listar itens (componente ListItem, hook useItems)

**Próximas Stories Usarão:**
- Story 3.5: Editar item - mesma estrutura, clique no texto abre modal
- Story 5.3: WebSocket sincronização - ITEM_CHECKED broadcast em tempo real

**Esta Story Habilita:**
- ✅ Funcionalidade completa de marcar/desmarcar itens
- ✅ UX fluida com optimistic update
- ✅ Base para sincronização real-time (Epic 5)

### 📊 Checklist de Implementação

Antes de marcar esta story como completa:

- [ ] Método toggleItemCheck no ListItemService
- [ ] Validação de permissão (participante da lista)
- [ ] Validação de item pertencente à lista
- [ ] Endpoint PATCH /api/lists/{id}/items/{itemId}/check funcionando
- [ ] RFC 7807 error responses configurados
- [ ] API call toggleItemCheck no frontend
- [ ] Optimistic UI implementado no useItems
- [ ] Toast "Sincronizado" após sucesso
- [ ] Toast "Erro ao sincronizar" em caso de falha
- [ ] Checkbox vs texto separados (toggle vs editar)
- [ ] Animação "pop" funcionando no checkbox
- [ ] Testes unitários passando
- [ ] Testes de integração passando
- [ ] Testes frontend passando

### Project Structure Notes

**Alinhamento com Estrutura de Projeto Unificada:**

Esta story segue o padrão já estabelecido:
- `listitem/` módulo (DDD-lite)
- Controller → Service → Repository → Entity
- DTOs para request/response
- Componentes React com TypeScript
- Hooks customizados para estado

**Padrões de Código Estabelecidos (Stories Anteriores):**

- **Constructor injection:** Sem @Autowired
- **@Transactional:** Operações de escrita
- **RFC 7807:** Erros padronizados
- **Optimistic UI:** Atualização local imediata + revert em erro
- **FetchType.LAZY:** Evitar N+1
- **Testes:** @DataJpaTest, @WebMvcTest, @SpringBootTest
- **Frontend:** Hooks customizados, componentes funcionais, Tailwind CSS

### References

**Epics e Stories:**
- [Fonte: _bmad-output/planning-artifacts/epics.md#Story-3.4]
  - Epic 3: Gestão de Itens
  - Story 3.4: Marcar/Desmarcar Item como Concluído
  - FR18: Marcar/desmarcar item como concluído

**Story Anterior:**
- [Fonte: _bmad-output/implementation-artifacts/3-3-listar-itens-de-uma-lista.md]
  - Story 3.3: Listar Itens (componente ListItem, hook useItems)
  - Padrões de checkbox com animação
  - Estrutura de ListView

**Decisões Arquiteturais:**
- [Fonte: _bmad-output/planning-artifacts/architecture.md]
  - **Decision #002:** Campos nullable para dinâmica
  - **Decision #004:** RFC 7807 Problem Details
  - **Decision #006:** React Context + hooks

**Git Intelligence (Recent Commits):**
```
fc64fd1 feat(listitem): implement list items view with code review fixes (story 3-3)
ac6109e feat(listitem): implement add item to list with code review fixes (story 3-2)
81d675b feat(listitem): implement list items data model with code review fixes (story 3-1)
```

**Padrões de Commit:**
- Formato: `feat(scope): description`
- Referência à story: `(story 3-4)`

## Dev Agent Record

### Agent Model Used

Story criada por: Claude Opus 4.6

### Debug Log References

### Completion Notes List

**Implementado:**
- Método toggleItemCheck no ListItemService com validações completas
- Endpoint PATCH /api/lists/{listId}/items/{itemId}/check no ListItemController
- API call toggleItemCheck no itemsApi.ts com tratamento de erros
- Função toggleItem no hook useItems com optimistic update e rollback
- Integração na ListView com Toast feedback ("Sincronizado"/"Erro ao sincronizar")
- Testes desta story: 13 backend (6 unit + 7 integration) + 6 frontend (3 ListItem + 3 useItems)
- Total do projeto: 42 testes backend + 54 testes frontend = 96 testes passando
- Exceção ItemNotFoundException e handler RFC 7807

**Padrões Seguidos:**
- Constructor injection, @Transactional, RFC 7807 ProblemDetail
- Optimistic UI: atualização imediata + revert em erro
- Separação clara: checkbox (toggle) vs texto (editar)
- Animação "pop" 300ms com cubic-bezier já existente

**Code Review Fixes (2026-02-13):**
- ✅ Corrigido status da story (ready-for-dev → review) para consistência com sprint-status.yaml
- ✅ Corrigida File List: useItems.test.ts e ListItem.test.tsx marcados como [MODIFICADO]
- ✅ Adicionado sprint-status.yaml à File List (estava modificado mas não documentado)
- ✅ Clarificada contagem de testes (19 novos desta story, 96 totais no projeto)

### File List

**Arquivos Modificados:**

```
_bmad-output/implementation-artifacts/
└── sprint-status.yaml                    # [MODIFICADO] Status 3-4 → review

backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── controller/
│   │   └── ListItemController.java       # [MODIFICADO] PATCH endpoint
│   ├── service/
│   │   └── ListItemService.java          # [MODIFICADO] toggleItemCheck
│   └── exception/
│       └── ItemNotFoundException.java    # [CRIADO] Nova exceção
├── config/
│   └── GlobalExceptionHandler.java       # [MODIFICADO] Handler ItemNotFound
test/java/br/com/leoferolive/nossalista/listitem/
├── service/
│   └── ListItemServiceTest.java          # [MODIFICADO] +6 testes toggle
└── controller/
    └── ListItemControllerTest.java       # [MODIFICADO] +7 testes PATCH

frontend/src/
├── api/
│   └── itemsApi.ts                       # [MODIFICADO] +toggleItemCheck
├── hooks/
│   └── useItems.ts                       # [MODIFICADO] +toggleItem
│   └── useItems.test.ts                  # [MODIFICADO] +testes toggle
├── components/
│   └── ListItem.tsx                      # [VERIFICADO] Já integrado
│   └── ListItem.test.tsx                 # [MODIFICADO] +testes toggle
└── pages/
    └── ListView.tsx                      # [MODIFICADO] Handler + Toast
```

**Arquivos Existentes (para referência):**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── domain/ListItem.java              # Tem campo checked
│   ├── repository/ListItemRepository.java
│   └── dto/ListItemResponseDTO.java
└── common/exception/
    ├── ListNotFoundException.java
    └── ForbiddenException.java

frontend/src/
├── types/Item.ts                         # Tipos já definidos
├── api/itemsApi.ts                       # API base
├── hooks/useItems.ts                     # Hook base
├── components/ListItem.tsx               # Componente base
└── index.css                             # Animação pop já existe
```

---

### Change Log

| Data | Alteração |
|------|-----------|
| 2026-02-13 | Story criada - Documentação completa para implementação |
| 2026-02-13 | Story implementada - Backend (Service, Controller, Exceção, Handler), Frontend (API, Hook, View), Testes (13 backend + 6 frontend) |
| 2026-02-13 | Code review aplicado - 4 issues corrigidos (status inconsistente, File List, sprint-status, contagem testes) |
| 2026-02-13 | Story marcada como done - Todos ACs implementados, testes passando (42 backend + 54 frontend) |

---

**Story Status:** done ✅

**Próximos Passos:**
Story 3.4 está completa! Próxima story: 3.5 - Editar Item
1. Implementar backend: ListItemService.toggleItemCheck() + Controller PATCH endpoint
2. Implementar frontend: API call, hook toggleItem (optimistic), integração ListView
3. Escrever testes
4. Executar dev-story para desenvolvimento
