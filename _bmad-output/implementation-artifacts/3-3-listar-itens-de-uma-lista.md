# Story 3.3: Listar Itens de uma Lista

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participante de uma lista,
I want ver todos os itens da lista,
So that possa ver o que precisa ser feito/comprado.

## Acceptance Criteria

**Given** o endpoint GET /api/lists/{id}/items está disponível
**When** faço request com JWT válido e sou participante da lista
**Then** response deve ser 200 OK com array de itens ordenados por position ASC
**And** cada item tem: id, name, checked, quantity, due_date, url, position, created_by, created_at, updated_at

**Given** o endpoint GET /api/lists/{id}/items
**When** lista não existe
**Then** response deve ser 404 Not Found conforme RFC 7807

**Given** o endpoint GET /api/lists/{id}/items
**When** usuário não é participante da lista (não é dono nem membro)
**Then** response deve ser 403 Forbidden conforme RFC 7807

**Given** ListView no frontend
**When** itens carregados
**Then** cada item renderizado como ListItem com: checkbox customizado, nome, campos extras (quantity/due_date/url), criador (avatar + username)

**Given** ListItem com checked = true
**When** renderizado
**Then** checkbox marcado, texto com line-through e opacidade 50%

**Given** ListItem com checked = false
**When** renderizado
**Then** checkbox desmarcado, texto normal sem line-through

**Given** ListItem do tipo Compras
**When** renderizado
**Then** mostra campo quantity se não for null

**Given** ListItem do tipo Tarefas
**When** renderizado
**Then** mostra campo due_date formatado se não for null

**Given** ListItem do tipo Wishlist
**When** renderizado
**Then** mostra campo url como link clicável se não for null

**Given** lista sem itens
**When** ListView carregada
**Then** mostra estado vazio: "Esta lista ainda não tem itens. Adicione o primeiro!"

**Given** lista com itens
**When** ListView carregada
**Then** itens são exibidos em ordem de position (do menor para o maior)

## Tasks / Subtasks

- [x] Task 1: Criar método getItemsByListId no ListItemService (AC: Buscar itens da lista)
  - [x] 1.1: Adicionar método `getItemsByListId(UUID listId, User user)` no ListItemService
  - [x] 1.2: Validar que lista existe (404 se não)
  - [x] 1.3: Validar que usuário é participante (403 se não)
  - [x] 1.4: Buscar itens via `listItemRepository.findByListIdOrderByPositionAsc(listId)`
  - [x] 1.5: Mapear para List<ListItemResponseDTO> usando listItemMapper
  - [x] 1.6: Retornar lista ordenada

- [x] Task 2: Criar endpoint GET no ListItemController (AC: Endpoint REST GET /api/lists/{id}/items)
  - [x] 2.1: Adicionar método GET `@GetMapping` no ListItemController
  - [x] 2.2: Path: `/api/lists/{listId}/items`
  - [x] 2.3: Extrair user do JWT via @AuthenticationPrincipal
  - [x] 2.4: Chamar ListItemService.getItemsByListId()
  - [x] 2.5: Retornar ResponseEntity.ok(responseDTOs)

- [x] Task 3: Implementar componente ListItem no frontend (AC: Renderização de cada item)
  - [x] 3.1: Criar componente ListItem.tsx em components/list/
  - [x] 3.2: Props: item (ListItemType), onToggle, onEdit
  - [x] 3.3: Checkbox customizado com animação "pop" (300ms)
  - [x] 3.4: Mostrar nome do item com line-through quando checked=true
  - [x] 3.5: Mostrar campos extras: quantity, dueDate, url conforme tipo
  - [x] 3.6: Mostrar criador (avatar + username)
  - [x] 3.7: Touch target ≥ 44px (NFR-A4)

- [x] Task 4: Criar hook useItems para gerenciar estado (AC: Estado dos itens no frontend)
  - [x] 4.1: Criar hook useItems.ts em hooks/
  - [x] 4.2: Estado: items, loadingItems, errorItems
  - [x] 4.3: Função fetchItems(listId) para buscar itens da API
  - [x] 4.4: Integrar com useLists ou criar context separado

- [x] Task 5: Integrar ListItem na ListView (AC: UI completa de itens)
  - [x] 5.1: Atualizar ListView para buscar itens ao carregar
  - [x] 5.2: Renderizar lista de ListItem components
  - [x] 5.3: Mostrar estado vazio quando não houver itens
  - [x] 5.4: Scroll container para lista de itens
  - [x] 5.5: Manter campo de adição sempre visível no bottom

- [x] Task 6: Criar tipos TypeScript para itens (AC: Type safety)
  - [x] 6.1: Criar arquivo types/Item.ts
  - [x] 6.2: Definir interface ListItem com todos os campos
  - [x] 6.3: Definir tipo CreatorInfo para createdBy

- [x] Task 7: Testes backend (AC: Cobertura de testes)
  - [x] 7.1: Teste de unidade: getItemsByListId com sucesso
  - [x] 7.2: Teste: Retorna lista vazia quando não há itens
  - [x] 7.3: Teste: Erro 404 quando lista não existe
  - [x] 7.4: Teste: Erro 403 quando usuário não é participante
  - [x] 7.5: Teste: Ordem correta por position
  - [x] 7.6: Teste de integração: GET /api/lists/{id}/items completo

- [x] Task 8: Testes frontend (AC: Componentes testados)
  - [x] 8.1: Teste ListItem renderiza corretamente
  - [x] 8.2: Teste ListItem mostra line-through quando checked
  - [x] 8.3: Teste ListItem mostra campos extras conforme tipo
  - [x] 8.4: Teste ListView busca itens ao carregar
  - [x] 8.5: Teste estado vazio renderizado quando sem itens

## Dev Notes

### 🎯 Contexto da Story

Esta é a **TERCEIRA STORY** do Epic 3 (Gestão de Itens), implementando a funcionalidade de listar/visualizar itens de uma lista.

**Epic 2 (COMPLETO - 6 stories done):** Estabeleceu infraestrutura de listas
**Story 3.1 (COMPLETO - done):** Criou fundação de dados (ListItem entity, repository, DTOs)
**Story 3.2 (COMPLETO - done):** Implementou adição de itens (POST /api/lists/{id}/items)

**Objetivo Principal:** Implementar **listagem de itens** com:
1. Endpoint REST GET /api/lists/{id}/items
2. Validações de permissão (participante da lista)
3. Ordenação por position ASC
4. UI de visualização com checkbox customizado
5. Campos dinâmicos renderizados conforme tipo

**FRs Cobertos (Epics.md):**
- FR16: Participante da lista pode ver itens (parcial - visualização)
- FR19: Campo quantity (Compras) - visualização
- FR20: Campo due_date (Tarefas) - visualização
- FR21: Campo URL (Wishlist) - visualização
- FR22: Sistema registra quem criou cada item - visualização

### 🏗️ Decisões Arquiteturais Relevantes

**Decision #002: Data Model - Campos Dinâmicos por Tipo (Architecture.md):**
> "Colunas nullable em list_items para campos dinâmicos (quantity, due_date, url)"

**Campos por Tipo de Lista:**

| Tipo | Campos Visíveis | UI Indicadores |
|------|----------------|----------------|
| Compras | name, quantity, checked | Quantidade: "2x" ou badge |
| Tarefas | name, dueDate, checked | Data: ícone calendário + data |
| Wishlist | name, url, checked | Link: ícone link + "Ver produto" |
| Genérica | name, checked | Apenas nome e checkbox |

**Ordenação - Position ASC:**

```java
// JPA Repository já tem o método
List<ListItem> findByListIdOrderByPositionAsc(UUID listId);
```

**Permissionamento (Story 3.2 Dev Notes):**
- Qualquer participante pode ver itens (dono ou membro)
- Verificação via `isParticipant(list, user)` (já implementado no Story 3.2)

### 📦 Stack Técnico Específico

**Backend:**
- Spring Boot 4.0.2 + Java 25
- Spring Data JPA (já configurado)
- ListItemRepository já possui método de busca ordenada

**Frontend:**
- React 19 + TypeScript
- Axios para API calls
- Tailwind CSS para UI
- Animação "pop" no checkbox: keyframes CSS

**Entidades JPA Já Existentes (Story 3.1):**
- ListItem.java - Entidade completa
- ListItemRepository.java - Repository com `findByListIdOrderByPositionAsc`
- DTOs criados: ListItemResponseDTO (já tem todos os campos necessários)
- ListItemMapper.java - Mapper para converter entity → DTO

**Service Já Existente (Story 3.2):**
- ListItemService.java - Já tem método `isParticipant()` e validações

### 🔐 Segurança - Considerações

**Regras de Acesso:**
- Usuário deve ser participante da lista (dono OU membro)
- Mesma verificação do Story 3.2 via `isParticipant()`
- Retornar 403 Forbidden se não autorizado

**Performance:**
- Usar `findByListIdOrderByPositionAsc` (índice já existe: idx_list_items_position)
- Evitar N+1: o método do repository retorna todas as entidades de uma vez
- Considerar paginação futura (não necessária para MVP)

### 🎨 Estrutura de Código Backend

**Arquivos a Modificar:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── controller/
│   │   └── ListItemController.java       # [MODIFICAR] Adicionar GET endpoint
│   └── service/
│       └── ListItemService.java          # [MODIFICAR] Adicionar getItemsByListId

test/java/br/com/leoferolive/nossalista/listitem/
├── service/
│   └── ListItemServiceTest.java          # [MODIFICAR] Adicionar testes
└── controller/
    └── ListItemControllerTest.java       # [MODIFICAR] Adicionar testes
```

**Convenções de Código:**
- Constructor injection (sem @Autowired)
- @Transactional(readOnly = true) para operações de leitura
- RFC 7807 ProblemDetail para erros
- ResponseEntity para controle de status HTTP

### 🎨 Estrutura de Código Frontend

**Novos Arquivos a Criar:**

```
frontend/src/
├── types/
│   └── Item.ts                           # NOVO - Tipos TypeScript
├── api/
│   └── items.ts                          # NOVO - API calls GET
├── hooks/
│   └── useItems.ts                       # NOVO - Hook para gerenciar itens
└── components/list/
    └── ListItem.tsx                      # NOVO - Componente de item
```

**Arquivos a Modificar:**
- `frontend/src/pages/ListView.tsx`: Integrar busca de itens e renderização

**Padrões UI/UX:**
- Checkbox customizado com animação "pop" (300ms, cubic-bezier)
- Line-through e opacidade 50% para itens checked
- Touch targets ≥ 44px (NFR-A4)
- Estado vazio com ícone e mensagem amigável

### 📋 Especificação Detalhada

**1. ListItemService.java - Novo Método**

```java
/**
 * Busca todos os itens de uma lista
 * Valida permissões (usuário deve ser participante)
 *
 * @param listId ID da lista
 * @param user   Usuário solicitante
 * @return Lista de DTOs com todos os itens ordenados por position
 * @throws ListNotFoundException se a lista não existir
 * @throws ForbiddenException    se o usuário não for participante
 */
@Transactional(readOnly = true)
public List<ListItemResponseDTO> getItemsByListId(UUID listId, User user) {
    // 1. Verificar se lista existe
    List list = listRepository.findById(listId)
            .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

    // 2. Verificar se usuário é participante
    if (!isParticipant(list, user)) {
        throw new ForbiddenException("Você não tem permissão para ver os itens desta lista");
    }

    // 3. Buscar itens ordenados por position ASC
    List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(listId);

    // 4. Mapear para DTOs
    return items.stream()
            .map(listItemMapper::toListItemResponseDTO)
            .collect(Collectors.toList());
}
```

**2. ListItemController.java - Novo Endpoint**

```java
/**
 * GET /api/lists/{listId}/items
 * Retorna todos os itens de uma lista ordenados por position
 */
@GetMapping
public ResponseEntity<List<ListItemResponseDTO>> getItems(
        @PathVariable UUID listId,
        @AuthenticationPrincipal UserDetails userDetails) {

    User user = ((ApplicationUserDetails) userDetails).getUser();
    List<ListItemResponseDTO> items = listItemService.getItemsByListId(listId, user);

    return ResponseEntity.ok(items);
}
```

**3. types/Item.ts (Frontend)**

```typescript
export interface CreatorInfo {
  id: string;
  username: string;
  name: string;
  avatarUrl: string;
}

export interface ListItem {
  id: string;
  name: string;
  checked: boolean;
  quantity: number | null;
  dueDate: string | null;  // ISO 8601
  url: string | null;
  position: number;
  createdBy: CreatorInfo;
  createdAt: string;  // ISO 8601
  updatedAt: string;  // ISO 8601
}
```

**4. ListItem.tsx (Frontend)**

```typescript
import React from 'react';
import { ListItem as ListItemType } from '../../types/Item';

interface ListItemProps {
  item: ListItemType;
  onToggle: (id: string) => void;
  onEdit: (item: ListItemType) => void;
}

export const ListItemComponent: React.FC<ListItemProps> = ({
  item,
  onToggle,
  onEdit,
}) => {
  const handleCheckboxClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onToggle(item.id);
  };

  const handleItemClick = () => {
    onEdit(item);
  };

  return (
    <div
      className={`flex items-center gap-3 p-3 rounded-lg border transition-all cursor-pointer hover:bg-gray-50 ${
        item.checked ? 'opacity-50' : ''
      }`}
      onClick={handleItemClick}
      style={{ minHeight: '44px' }} // NFR-A4
    >
      {/* Checkbox customizado */}
      <button
        onClick={handleCheckboxClick}
        className={`w-6 h-6 rounded border-2 flex items-center justify-center transition-all duration-300 ${
          item.checked
            ? 'bg-blue-500 border-blue-500 scale-100'
            : 'border-gray-300 hover:border-blue-400'
        }`}
        style={{
          animation: item.checked ? 'pop 300ms cubic-bezier(0.175, 0.885, 0.32, 1.275)' : 'none',
        }}
      >
        {item.checked && (
          <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              clipRule="evenodd"
            />
          </svg>
        )}
      </button>

      {/* Conteúdo do item */}
      <div className="flex-1 min-w-0">
        <p className={`font-medium truncate ${item.checked ? 'line-through text-gray-500' : 'text-gray-900'}`}>
          {item.name}
        </p>

        {/* Campos extras */}
        <div className="flex items-center gap-2 text-sm text-gray-500 mt-1">
          {item.quantity && (
            <span className="bg-blue-100 text-blue-800 px-2 py-0.5 rounded text-xs font-medium">
              {item.quantity}x
            </span>
          )}
          {item.dueDate && (
            <span className="flex items-center gap-1">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              {new Date(item.dueDate).toLocaleDateString('pt-BR')}
            </span>
          )}
          {item.url && (
            <a
              href={item.url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 hover:underline flex items-center gap-1"
              onClick={(e) => e.stopPropagation()}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
              Ver produto
            </a>
          )}
        </div>
      </div>

      {/* Criador */}
      <div className="flex items-center gap-2 text-sm text-gray-500">
        <img
          src={item.createdBy.avatarUrl || '/default-avatar.png'}
          alt={item.createdBy.username}
          className="w-6 h-6 rounded-full"
        />
        <span className="hidden sm:inline">{item.createdBy.username}</span>
      </div>
    </div>
  );
};
```

**5. Animação CSS (pop)**

```css
@keyframes pop {
  0% {
    transform: scale(0.8);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}
```

### 🧪 Testes e Validação

**Testes Unitários (ListItemServiceTest):**

```java
@Test
void shouldReturnItemsOrderedByPosition() {
    // Given
    UUID listId = UUID.randomUUID();
    User user = createTestUser();
    List list = createTestList(user);

    ListItem item1 = createItem(list, "Item 1", 0);
    ListItem item2 = createItem(list, "Item 2", 1);
    ListItem item3 = createItem(list, "Item 3", 2);

    when(listRepository.findById(listId)).thenReturn(Optional.of(list));
    when(listItemRepository.findByListIdOrderByPositionAsc(listId))
        .thenReturn(List.of(item1, item2, item3));

    // When
    List<ListItemResponseDTO> result = service.getItemsByListId(listId, user);

    // Then
    assertEquals(3, result.size());
    assertEquals("Item 1", result.get(0).name());
    assertEquals("Item 2", result.get(1).name());
    assertEquals("Item 3", result.get(2).name());
}

@Test
void shouldReturnEmptyListWhenNoItems() {
    // Given
    UUID listId = UUID.randomUUID();
    User user = createTestUser();
    List list = createTestList(user);

    when(listRepository.findById(listId)).thenReturn(Optional.of(list));
    when(listItemRepository.findByListIdOrderByPositionAsc(listId))
        .thenReturn(List.of());

    // When
    List<ListItemResponseDTO> result = service.getItemsByListId(listId, user);

    // Then
    assertTrue(result.isEmpty());
}

@Test
void shouldThrow404WhenListNotFound() {
    // Given
    UUID listId = UUID.randomUUID();
    when(listRepository.findById(listId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(ListNotFoundException.class, () ->
        service.getItemsByListId(listId, createTestUser()));
}

@Test
void shouldThrow403WhenNotParticipant() {
    // Given - usuário não é participante
    UUID listId = UUID.randomUUID();
    User owner = createTestUser();
    User nonParticipant = createAnotherUser();
    List list = createTestList(owner);

    when(listRepository.findById(listId)).thenReturn(Optional.of(list));

    // When/Then
    assertThrows(ForbiddenException.class, () ->
        service.getItemsByListId(listId, nonParticipant));
}
```

**Testes de Integração (ListItemControllerTest):**

```java
@Test
void shouldGetItemsEndpoint() throws Exception {
    mockMvc.perform(get("/api/lists/{listId}/items", listId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Item 1"))
        .andExpect(jsonPath("$[0].checked").value(false));
}
```

### 🚨 Armadilhas Comuns a Evitar

1. **Não verificar permissão** - Sempre validar se usuário é participante da lista
2. **Ordenação incorreta** - Garantir que está usando ASC (do menor position para o maior)
3. **N+1 problem** - Verificar que o repository está carregando dados eficientemente
4. **Checkbox sem animação** - A animação "pop" é requisito de UX (300ms)
5. **Não mostrar criador** - O createdBy deve ser exibido em cada item
6. **Campos extras sempre visíveis** - Mostrar quantity/due_date/url apenas se não forem null
7. **Estado vazio ausente** - Sempre mostrar mensagem quando lista não tem itens
8. **Line-through incompleto** - Texto deve ter line-through E opacidade reduzida quando checked

### 🔗 Relacionamento com Outras Stories

**Depende de:**
- ✅ Story 2.1-2.6: Gestão de listas (infraestrutura completa)
- ✅ Story 3.1: Modelagem de dados (ListItem entity, repository, DTOs)
- ✅ Story 3.2: Adicionar item (endpoint POST já existe, pode testar integração)

**Próximas Stories Usarão:**
- Story 3.4: Marcar item (PATCH) - usará mesma entidade, campo checked
- Story 3.5: Editar item - mesma entidade, atualização parcial
- Story 3.6: Remover item - mesma entidade, DELETE
- Story 5.2: WebSocket broadcast - ITEM_ADDED atualizará lista em tempo real

**Esta Story Habilita:**
- ✅ Visualização completa de itens
- ✅ Integração frontend-backend para itens
- ✅ Base para as operações de update/delete
- ✅ UX completa de visualização de lista

### 📊 Checklist de Implementação

Antes de marcar esta story como completa:

- [ ] Método getItemsByListId no ListItemService
- [ ] Validação de permissão (participante da lista)
- [ ] Ordenação por position ASC funcionando
- [ ] Endpoint GET /api/lists/{id}/items funcionando
- [ ] RFC 7807 error responses configurados
- [ ] ListItem componente criado (frontend)
- [ ] Checkbox customizado com animação "pop"
- [ ] Campos extras renderizados conforme tipo
- [ ] Criador exibido em cada item
- [ ] Estado vazio implementado
- [ ] Integração com API no frontend
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

**Padrões de Código Estabelecidos (Stories Anteriores):**

- **Constructor injection:** Sem @Autowired
- **@Transactional(readOnly = true):** Operações de leitura
- **RFC 7807:** Erros padronizados
- **FetchType.LAZY:** Evitar N+1
- **Validações:** Jakarta Validation para writes
- **Testes:** @DataJpaTest, @WebMvcTest, @SpringBootTest
- **Frontend:** Hooks customizados, componentes funcionais, Tailwind CSS

### References

**Epics e Stories:**
- [Fonte: _bmad-output/planning-artifacts/epics.md#Story-3.3]
  - Epic 3: Gestão de Itens
  - Story 3.3: Listar Itens de uma Lista
  - FR16, FR19-22: Requisitos funcionais

**Story Anterior:**
- [Fonte: _bmad-output/implementation-artifacts/3-2-adicionar-item-a-lista.md]
  - Story 3.2: Adicionar Item (endpoints e lógica já estabelecidos)
  - Padrões de validação e permissão

**Decisões Arquiteturais:**
- [Fonte: _bmad-output/planning-artifacts/architecture.md]
  - **Decision #002:** Campos nullable para dinâmica
  - **Decision #004:** RFC 7807 Problem Details
  - **Decision #006:** React Context + hooks

**Git Intelligence (Recent Commits):**
```
ac6109e feat(listitem): implement add item to list with code review fixes (story 3-2)
81d675b feat(listitem): implement list items data model with code review fixes (story 3-1)
7349cd1 feat(list): implement delete list with code review fixes (story 2-6)
```

**Padrões de Commit:**
- Formato: `feat(scope): description`
- Referência à story: `(story 3-3)`

## Dev Agent Record

### Agent Model Used

Story criada por: Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References

### Completion Notes List

**Implementado - Todas as Tasks Completas:**

**Backend (Tasks 1-2):**
- ✅ Método `getItemsByListId(UUID listId, User user)` no ListItemService
- ✅ Endpoint GET `/api/lists/{listId}/items` no ListItemController
- ✅ 4 testes unitários + 6 testes de integração (10 total backend)

**Frontend (Tasks 3-6):**
- ✅ Tipos TypeScript: `types/Item.ts` com ListItem, CreatorInfo, CreateItemRequest
- ✅ API: `api/itemsApi.ts` com getItemsByListId e addItem
- ✅ Hook: `hooks/useItems.ts` com fetchItems, addItem, estados loading/error
- ✅ Componente: `components/ListItem.tsx` com checkbox animado, campos extras, criador
- ✅ Integração: `pages/ListView.tsx` atualizada para buscar e renderizar itens
- ✅ Animação CSS: "pop" no checkbox (300ms cubic-bezier)

**Testes (Tasks 7-8):**
- ✅ Backend: 183 testes passando (sem regressões)
- ✅ Frontend: 73 testes passando (16 ListItem + 9 useItems + 48 existentes)
- ✅ Total: 256 testes passando

**Reutilizado:**
- `isParticipant()` do Story 3.2 para validação de permissão
- Padrões de código existentes (DTOs, Mapper, hooks)

**Decisões Técnicas:**
- Variáveis não utilizadas nos handlers prefixadas com `_` (convenção TypeScript)
- Datas formatadas com `toLocaleDateString('pt-BR')` para exibição
- Touch targets ≥ 44px conforme NFR-A4
- Estados de loading, erro e vazio implementados na UI

**Code Review Fixes (2026-02-13):**
- ✅ Removido campo `notes` de CreateItemRequest (não suportado pelo backend)
- ✅ Corrigida ordenação no useItems.addItem (agora ordena por position após adicionar)
- ✅ Habilitado formulário de adicionar item na ListView (funcionalidade estava pronta mas disabled)
- ✅ Adicionado sprint-status.yaml à File List (estava modificado mas não documentado)

### File List

**Arquivos Modificados/Criados:**

```
_bmad-output/implementation-artifacts/
└── sprint-status.yaml                    # [MODIFICADO] Status da story atualizado

backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── controller/
│   │   └── ListItemController.java       # [MODIFICADO] Adicionado endpoint GET
│   └── service/
│       └── ListItemService.java          # [MODIFICADO] Adicionado getItemsByListId()

test/java/br/com/leoferolive/nossalista/listitem/
├── service/
│   └── ListItemServiceTest.java          # [MODIFICADO] +4 testes unitários
└── controller/
    └── ListItemControllerTest.java       # [MODIFICADO] +6 testes de integração

frontend/src/
├── types/
│   └── Item.ts                           # [NOVO] Tipos TypeScript
├── api/
│   └── itemsApi.ts                       # [NOVO] API calls
├── hooks/
│   ├── useItems.ts                       # [NOVO] Hook
│   └── useItems.test.ts                  # [NOVO] 9 testes
├── components/
│   ├── ListItem.tsx                      # [NOVO] Componente
│   └── ListItem.test.tsx                 # [NOVO] 16 testes
├── pages/
│   ├── ListView.tsx                      # [MODIFICADO] Integração itens + formulário adicionar
│   └── ListView.test.tsx                 # [MODIFICADO] +mock useItems
└── index.css                             # [MODIFICADO] Animação pop
```

frontend/src/
├── types/
│   └── Item.ts                           # [NOVO] Tipos
├── api/
│   └── items.ts                          # [NOVO] API calls
├── hooks/
│   └── useItems.ts                       # [NOVO] Hook
├── components/list/
│   └── ListItem.tsx                      # [NOVO] Componente
└── pages/
    └── ListView.tsx                      # [MODIFICAR] Integração
```

**Arquivos Existentes (para referência):**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── domain/ListItem.java
│   ├── repository/ListItemRepository.java  # Tem findByListIdOrderByPositionAsc
│   └── dto/ListItemResponseDTO.java        # Já completo
└── list/
    └── controller/ListController.java      # Padrão de controller
```

---

### Change Log

| Data | Alteração |
|------|-----------|
| 2026-02-13 | Story criada - Documentação completa para implementação |
| 2026-02-13 | Tasks 1-2 completas: Backend GET /api/lists/{id}/items implementado com testes (13 unitários + 6 integração) |
| 2026-02-13 | Tasks 3-8 completas: Frontend implementado com ListItem, useItems, integração na ListView, 73 testes frontend passando |
| 2026-02-13 | Story completa - 183 testes backend + 73 testes frontend = 256 testes totais passando |
| 2026-02-13 | Code review aplicado - 4 issues corrigidos (API contract, ordenação, botão adicionar item, File List) |

---

**Story Status:** done ✅

**Próximos Passos:**
Story 3.3 está completa! Próxima story: 3.4 - Marcar/Desmarcar Item como Concluído
