# Story 3.6: Remover Item

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participante de uma lista,
I want remover itens da lista,
So that possa deletar o que não preciso mais.

## Acceptance Criteria

1. **Given** o endpoint DELETE /api/lists/{listId}/items/{itemId} está disponível
   **When** faço request com JWT válido, sou participante da lista
   **Then** response deve ser 204 No Content, item removido do database

2. **Given** lista com item removido
   **When** positions verificadas
   **Then** positions dos itens restantes reordenadas automaticamente (0, 1, 2...)

3. **Given** ListItem na ListView
   **When** faço long-press (1 segundo)
   **Then** menu opções aparece com "Editar", "Remover"

4. **Given** menu opções aberto
   **When** toco "Remover"
   **Then** modal confirmação: "Remover item? Tem certeza que deseja remover '{nome}'?"

5. **Given** modal de confirmação aberto
   **When** confirmo a remoção
   **Then** DELETE request enviado, fade-out (200ms), item desaparece, Toast "Item removido", outros itens reordenam

6. **Given** o endpoint DELETE /api/lists/{listId}/items/{itemId}
   **When** item não existe ou lista não existe
   **Then** response deve ser 404 Not Found conforme RFC 7807

7. **Given** o endpoint DELETE /api/lists/{listId}/items/{itemId}
   **When** usuário não é participante da lista
   **Then** response deve ser 403 Forbidden conforme RFC 7807

8. **Given** modal de confirmação aberto
   **When** cancelo a remoção
   **Then** modal fecha sem alterações, Toast não aparece

## Tasks / Subtasks

- [x] Task 1: Criar método deleteItem no ListItemService (AC: Delete com validação)
  - [x] 1.1: Adicionar método `deleteItem(UUID listId, UUID itemId, User user)` no ListItemService
  - [x] 1.2: Validar que lista existe (404 se não)
  - [x] 1.3: Validar que usuário é participante (403 se não)
  - [x] 1.4: Validar que item existe e pertence à lista (404 se não)
  - [x] 1.5: Deletar item do database (hard delete)
  - [x] 1.6: Reordenar positions dos itens restantes (0, 1, 2...)
  - [x] 1.7: Retornar void (HTTP 204)

- [x] Task 2: Criar endpoint DELETE no ListItemController (AC: Endpoint REST DELETE)
  - [x] 2.1: Adicionar método DELETE `@DeleteMapping("/{itemId}")` no ListItemController
  - [x] 2.2: Path: `/api/lists/{listId}/items/{itemId}`
  - [x] 2.3: Extrair user do JWT via @AuthenticationPrincipal
  - [x] 2.4: Chamar ListItemService.deleteItem()
  - [x] 2.5: Retornar ResponseEntity.noContent()
  - [x] 2.6: Documentação OpenAPI/Swagger completa

- [x] Task 3: Adicionar API call no frontend (AC: Comunicação com backend)
  - [x] 3.1: Adicionar `deleteItem(listId, itemId)` em `api/itemsApi.ts`
  - [x] 3.2: Retornar Promise<void> (204 No Content)
  - [x] 3.3: Tratar erros 404, 403, 401 com mensagens apropriadas

- [x] Task 4: Criar componente de menu e confirmação (AC: Long-press + modal)
  - [x] 4.1: Implementar long-press (1 segundo) no ListItem
  - [x] 4.2: Criar menu dropdown com opções "Editar" e "Remover"
  - [x] 4.3: Criar componente DeleteConfirmModal
  - [x] 4.4: Modal com título "Remover item?" e mensagem "Tem certeza que deseja remover '{nome}'?"
  - [x] 4.5: Botões "Cancelar" e "Confirmar" no modal

- [x] Task 5: Atualizar hook useItems (AC: Gerenciamento de estado)
  - [x] 5.1: Adicionar `deleteItem(itemId)` no hook useItems
  - [x] 5.2: Implementar optimistic update: remover do estado local imediatamente
  - [x] 5.3: Reverter estado em caso de erro
  - [x] 5.4: Retornar função deleteItem para componentes

- [x] Task 6: Integrar delete na ListView (AC: UI interativa)
  - [x] 6.1: Handler onDelete no ListItem chama função deleteItem do hook
  - [x] 6.2: Animação fade-out de 200ms antes de remover
  - [x] 6.3: Mostrar Toast "Item removido" após sucesso
  - [x] 6.4: Mostrar Toast "Erro ao remover item" em caso de falha
  - [x] 6.5: Reordenar visualmente itens restantes

- [x] Task 7: Testes backend (AC: Cobertura de testes)
  - [x] 7.1: Teste de unidade: deleteItem remove item com sucesso
  - [x] 7.2: Teste: deleteItem reordena positions corretamente
  - [x] 7.3: Teste: erro 404 quando item não existe
  - [x] 7.4: Teste: erro 404 quando lista não existe
  - [x] 7.5: Teste: erro 403 quando usuário não é participante
  - [x] 7.6: Teste: erro 404 quando item não pertence à lista
  - [x] 7.7: Teste de integração: DELETE endpoint completo

- [x] Task 8: Testes frontend (AC: Componentes testados)
  - [x] 8.1: Teste long-press ativa menu após 1 segundo
  - [x] 8.2: Teste DeleteConfirmModal renderiza mensagem com nome do item
  - [x] 8.3: Teste useItems deleteItem faz optimistic update
  - [x] 8.4: Teste useItems reverte em caso de erro
  - [x] 8.5: Teste animação fade-out antes de remover
  - [x] 8.6: Teste Toast aparece após remoção

## Dev Notes

### 🎯 Contexto da Story

Esta é a **SEXTA E ÚLTIMA STORY** do Epic 3 (Gestão de Itens), implementando a funcionalidade de remover itens da lista.

**Epic 3 (COMPLETO APÓS ESTA STORY):**
- Story 3.1 (done): Modelagem de dados (ListItem entity com campos dinâmicos)
- Story 3.2 (done): Adicionar item (POST /api/lists/{id}/items, validações por tipo)
- Story 3.3 (done): Listar itens (GET /api/lists/{id}/items, componente ListItem)
- Story 3.4 (done): Marcar/desmarcar item (PATCH /api/lists/{id}/items/{itemId}/check)
- Story 3.5 (done): Editar item (PATCH /api/lists/{id}/items/{itemId}, EditItemModal)
- **Story 3.6 (atual):** Remover item (DELETE /api/lists/{id}/items/{itemId}, DeleteConfirmModal)

**Objetivo Principal:** Implementar **remoção completa de itens** com:
1. Endpoint REST DELETE no backend
2. Validações de permissão (participante da lista)
3. Hard delete (remoção física do database)
4. Reordenação automática de positions
5. Long-press interaction (1 segundo)
6. Modal de confirmação obrigatória
7. Animação fade-out (200ms)
8. Feedback via Toast (sucesso/erro)

**FRs Cobertos (Epics.md):**
- FR17: Participante da lista pode remover itens

### 🏗️ Decisões Arquiteturais Relevantes

**Decision #004: RFC 7807 Problem Details (Architecture.md):**
> "Adotar RFC 7807 Problem Details para todas as respostas de erro"

**Erros Esperados:**
- **204 No Content:** Item deletado com sucesso (sem body)
- **401 Unauthorized:** JWT ausente ou inválido
- **403 Forbidden:** Usuário não é participante da lista
- **404 Not Found:** Item ou lista não existe

**Hard Delete vs Soft Delete:**
- Arquitetura define **hard delete** (remoção física do database)
- Activity log registra a ação ANTES do delete para auditoria
- Não há soft delete (deleted_at, is_deleted) no modelo de dados

**Reordenação de Positions:**
- Após deletar item, positions dos itens restantes devem ser recalculadas
- Exemplo: [0, 1, 2, 3] → delete 1 → [0, 1, 2] (item 2 vira 1, item 3 vira 2)

**Permissionamento (Story 3.2/3.3/3.4/3.5 Dev Notes):**
- Qualquer participante pode remover itens (dono ou membro)
- Verificação via `isParticipant(list, user)` já implementado
- Retornar 403 Forbidden se não autorizado

### 📦 Stack Técnico Específico

**Backend:**
- Spring Boot 4.0.2 + Java 25
- Spring Data JPA
- RFC 7807 ProblemDetail para erros
- @Transactional para operação de delete

**Frontend:**
- React 19 + TypeScript
- Axios para API calls
- Tailwind CSS para estilização
- Toast system já implementado (useToast)
- Optimistic UI pattern
- Long-press interaction (react-use ou custom hook)

**Entidades JPA Já Existentes:**
- ListItem.java - Entidade completa com position field
- ListItemRepository.java - Repository padrão + query customizada para reordenação
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
5. Delete físico do item
6. Reordenar positions restantes

**CRÍTICO - Activity Log:**
- ActivityService.log() deve ser chamado ANTES do delete
- Garante que a ação fica no histórico mesmo após exclusão
- Log type: ITEM_DELETED com payload do item deletado

### 🎨 Estrutura de Código Backend

**Arquivos a Modificar:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── controller/
│   │   └── ListItemController.java       # [MODIFICAR] Adicionar DELETE endpoint
│   ├── service/
│   │   └── ListItemService.java          # [MODIFICAR] Adicionar deleteItem + reorderPositions
│   └── repository/
│       └── ListItemRepository.java       # [MODIFICAR] Adicionar query para reordenação

test/java/br/com/leoferolive/nossalista/listitem/
├── service/
│   └── ListItemServiceTest.java          # [MODIFICAR] +testes delete
└── controller/
    └── ListItemControllerTest.java       # [MODIFICAR] +testes endpoint DELETE
```

**Convenções de Código:**
- Constructor injection (sem @Autowired)
- @Transactional para operações de escrita
- RFC 7807 ProblemDetail para erros
- ResponseEntity para controle de status HTTP
- HTTP 204 No Content para DELETE bem-sucedido (sem body)

### 🎨 Estrutura de Código Frontend

**Arquivos a Modificar:**

```
frontend/src/
├── api/
│   └── itemsApi.ts                       # [MODIFICAR] +deleteItem
├── hooks/
│   ├── useItems.ts                        # [MODIFICAR] +deleteItem (optimistic)
│   └── useLongPress.ts                    # [CRIAR] Hook customizado para long-press
├── components/
│   ├── ListItem.tsx                       # [MODIFICAR] Integrar long-press e menu
│   ├── ItemOptionsMenu.tsx                # [CRIAR] Menu dropdown com Editar/Remover
│   └── DeleteConfirmModal.tsx             # [CRIAR] Modal de confirmação
└── pages/
    └── ListView.tsx                       # [MODIFICAR] Handler + modal state
```

**Padrões UI/UX:**
- Long-press 1 segundo (não swipe, não clique rápido)
- Menu dropdown com glassmorphism (padrão já existente)
- Modal de confirmação intrusiva (backdrop blur)
- Animação fade-out 200ms antes de remover
- Optimistic UI: estado muda imediatamente
- Toast feedback: "Item removido" (success) ou "Erro ao remover item" (error)
- Touch targets ≥ 44px (NFR-A4)

### 📋 Especificação Detalhada

**1. ListItemService.java - Novo Método deleteItem**

```java
/**
 * Remove um item da lista
 * Valida permissões, deleta item e reordena positions
 *
 * @param listId  ID da lista
 * @param itemId  ID do item
 * @param user    Usuário solicitante
 * @throws ListNotFoundException  se a lista não existir
 * @throws ItemNotFoundException  se o item não existir
 * @throws ForbiddenException     se o usuário não for participante
 */
@Transactional
public void deleteItem(UUID listId, UUID itemId, User user) {

    // 1. Verificar se lista existe
    List list = listRepository.findById(listId)
            .orElseThrow(() -> new ListNotFoundException("Lista não encontrada"));

    // 2. Verificar se usuário é participante
    if (!isParticipant(list, user)) {
        throw new ForbiddenException("Você não tem permissão para remover itens desta lista");
    }

    // 3. Buscar item
    ListItem item = listItemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item não encontrado"));

    // 4. Verificar se item pertence à lista
    if (!item.getList().getId().equals(listId)) {
        throw new ItemNotFoundException("Item não encontrado nesta lista");
    }

    // 5. Registrar activity log ANTES de deletar (para auditoria)
    // Nota: Será implementado em Epic 6, mas já preparar para isso
    // activityService.log(list, user, "ITEM_DELETED", item);

    // 6. Deletar item
    Integer deletedPosition = item.getPosition();
    listItemRepository.delete(item);

    // 7. Reordenar positions dos itens restantes
    reorderPositions(listId, deletedPosition);

    // 8. Log
    log.info("Item deleted: itemId={}, listId={}, user={}", itemId, listId, user.getId());
}

/**
 * Reordena positions após deletar item
 * Items com position > deletedPosition têm position decrementada em 1
 */
private void reorderPositions(UUID listId, Integer deletedPosition) {
    // Buscar todos os itens com position > deletedPosition
    List<ListItem> itemsToReorder = listItemRepository
            .findByListIdAndPositionGreaterThanOrderByPositionAsc(listId, deletedPosition);

    // Decrementar position de cada item
    for (ListItem item : itemsToReorder) {
        item.setPosition(item.getPosition() - 1);
    }

    // Salvar todos (batch update)
    listItemRepository.saveAll(itemsToReorder);
}
```

**2. ListItemRepository.java - Query Customizada**

```java
package br.com.leoferolive.nossalista.listitem.repository;

import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ListItemRepository extends JpaRepository<ListItem, UUID> {

    // Query existente
    List<ListItem> findByListIdOrderByPositionAsc(UUID listId);

    // Nova query para reordenação
    List<ListItem> findByListIdAndPositionGreaterThanOrderByPositionAsc(UUID listId, Integer position);
}
```

**3. ListItemController.java - Novo Endpoint DELETE**

```java
/**
 * DELETE /api/lists/{listId}/items/{itemId}
 * Remove um item da lista
 */
@DeleteMapping("/{itemId}")
@Operation(
    summary = "Remover item",
    description = "Remove um item da lista e reordena positions dos itens restantes. " +
                  "Usuário deve ser dono ou membro da lista. " +
                  "Item é deletado permanentemente do database."
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "204",
        description = "Item removido com sucesso (sem conteúdo)"
    ),
    @ApiResponse(
        responseCode = "401",
        description = "Não autenticado (JWT ausente ou inválido)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
        responseCode = "403",
        description = "Usuário não tem permissão para remover itens desta lista",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Lista ou item não encontrado",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
})
public ResponseEntity<Void> deleteItem(
        @PathVariable UUID listId,
        @PathVariable UUID itemId,
        @AuthenticationPrincipal User user) {

    listItemService.deleteItem(listId, itemId, user);
    return ResponseEntity.noContent().build();
}
```

**4. Frontend - API Call**

```typescript
// api/itemsApi.ts
async deleteItem(listId: string, itemId: string): Promise<void> {
  try {
    await client.delete(`/api/lists/${listId}/items/${itemId}`);
  } catch (error) {
    const axiosError = error as AxiosError<ProblemDetail>;
    let message = 'Erro ao remover item';

    if (axiosError.response) {
      const status = axiosError.response.status;
      const problem = axiosError.response.data;

      switch (status) {
        case 403:
          message = 'Você não tem permissão para remover itens desta lista';
          break;
        case 404:
          message = 'Item não encontrado';
          break;
        default:
          message = problem.detail || message;
      }
    }

    throw new Error(message);
  }
}
```

**5. Frontend - Hook useLongPress (Customizado)**

```typescript
// hooks/useLongPress.ts
import { useCallback, useRef } from 'react';

interface UseLongPressOptions {
  onLongPress: () => void;
  delay?: number; // milliseconds (default: 1000ms = 1 segundo)
}

export function useLongPress({ onLongPress, delay = 1000 }: UseLongPressOptions) {
  const timeoutRef = useRef<NodeJS.Timeout>();

  const start = useCallback(() => {
    timeoutRef.current = setTimeout(() => {
      onLongPress();
    }, delay);
  }, [onLongPress, delay]);

  const clear = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
  }, []);

  return {
    onMouseDown: start,
    onMouseUp: clear,
    onMouseLeave: clear,
    onTouchStart: start,
    onTouchEnd: clear,
  };
}
```

**6. Frontend - Hook useItems com deleteItem (Optimistic Update)**

```typescript
// hooks/useItems.ts
const deleteItem = useCallback(
  async (itemId: string): Promise<void> => {
    // Encontrar item atual
    const item = items.find(i => i.id === itemId);
    if (!item) throw new Error('Item não encontrado');

    // Criar cópia para revert
    const originalItems = [...items];

    // Optimistic update: remover do estado local imediatamente
    setItems(prev => prev.filter(i => i.id !== itemId));

    try {
      // Fazer request para backend
      await itemsApi.deleteItem(listId, itemId);
    } catch (err) {
      // Reverter em caso de erro
      setItems(originalItems);
      throw err;
    }
  },
  [items, listId]
);

return { items, addItem, updateItem, deleteItem, toggleCheck };
```

**7. Frontend - ItemOptionsMenu Component**

```typescript
// components/ItemOptionsMenu.tsx
interface ItemOptionsMenuProps {
  isOpen: boolean;
  onClose: () => void;
  onEdit: () => void;
  onDelete: () => void;
  position: { x: number; y: number };
}

export function ItemOptionsMenu({
  isOpen,
  onClose,
  onEdit,
  onDelete,
  position
}: ItemOptionsMenuProps) {
  if (!isOpen) return null;

  return (
    <>
      {/* Backdrop para fechar ao clicar fora */}
      <div
        className="fixed inset-0 z-40"
        onClick={onClose}
      />

      {/* Menu dropdown */}
      <div
        className="absolute z-50 glass-card rounded-lg shadow-lg p-2 min-w-[120px]"
        style={{ top: position.y, left: position.x }}
      >
        <button
          onClick={() => {
            onEdit();
            onClose();
          }}
          className="w-full text-left px-4 py-2 hover:bg-gray-100 rounded-lg transition-colors"
        >
          Editar
        </button>
        <button
          onClick={() => {
            onDelete();
            onClose();
          }}
          className="w-full text-left px-4 py-2 hover:bg-red-50 text-red-600 rounded-lg transition-colors"
        >
          Remover
        </button>
      </div>
    </>
  );
}
```

**8. Frontend - DeleteConfirmModal Component**

```typescript
// components/DeleteConfirmModal.tsx
interface DeleteConfirmModalProps {
  isOpen: boolean;
  itemName: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export function DeleteConfirmModal({
  isOpen,
  itemName,
  onConfirm,
  onCancel
}: DeleteConfirmModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="glass-card w-full max-w-md mx-4 p-6">
        <h2 className="text-xl font-semibold mb-2">Remover item?</h2>
        <p className="text-gray-600 mb-6">
          Tem certeza que deseja remover <strong>'{itemName}'</strong>?
        </p>

        <div className="flex gap-2 justify-end">
          <button
            onClick={onCancel}
            className="px-4 py-2 rounded-lg border hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            onClick={onConfirm}
            className="px-4 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600"
          >
            Confirmar
          </button>
        </div>
      </div>
    </div>
  );
}
```

**9. Frontend - ListItem com Long-Press**

```typescript
// components/ListItem.tsx
import { useLongPress } from '../hooks/useLongPress';

interface ListItemProps {
  item: ListItem;
  onToggle: (itemId: string) => void;
  onEdit: (item: ListItem) => void;
  onDelete: (item: ListItem) => void;
}

export function ListItem({ item, onToggle, onEdit, onDelete }: ListItemProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });

  const handleLongPress = useCallback((event: React.MouseEvent | React.TouchEvent) => {
    // Capturar posição do clique/touch
    const x = 'clientX' in event ? event.clientX : event.touches[0].clientX;
    const y = 'clientY' in event ? event.clientY : event.touches[0].clientY;

    setMenuPosition({ x, y });
    setMenuOpen(true);
  }, []);

  const longPressProps = useLongPress({
    onLongPress: handleLongPress,
    delay: 1000 // 1 segundo
  });

  return (
    <div
      className="flex items-center gap-3 p-3 hover:bg-gray-50 rounded-lg transition-colors"
      {...longPressProps}
    >
      {/* Checkbox */}
      <input
        type="checkbox"
        checked={item.checked}
        onChange={() => onToggle(item.id)}
        className="w-5 h-5"
      />

      {/* Item content */}
      <div className="flex-1">
        <span className={item.checked ? 'line-through text-gray-400' : ''}>
          {item.name}
        </span>
      </div>

      {/* Menu dropdown */}
      <ItemOptionsMenu
        isOpen={menuOpen}
        onClose={() => setMenuOpen(false)}
        onEdit={() => onEdit(item)}
        onDelete={() => onDelete(item)}
        position={menuPosition}
      />
    </div>
  );
}
```

**10. Frontend - ListView Integration**

```typescript
// pages/ListView.tsx
const [deleteModalOpen, setDeleteModalOpen] = useState(false);
const [deletingItem, setDeletingItem] = useState<ListItem | null>(null);

const handleDeleteItem = async () => {
  if (!deletingItem) return;

  try {
    await deleteItem(deletingItem.id);
    toast.success('Item removido');
    setDeleteModalOpen(false);
    setDeletingItem(null);
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Erro ao remover item';
    toast.error(message);
  }
};

// Handler para abrir modal de confirmação
const onDelete = (item: ListItem) => {
  setDeletingItem(item);
  setDeleteModalOpen(true);
};

// Renderização do ListItem
{items.map(item => (
  <ListItem
    key={item.id}
    item={item}
    onToggle={handleToggleItem}
    onEdit={onEdit}
    onDelete={onDelete}
  />
))}

// DeleteConfirmModal (fora do map)
<DeleteConfirmModal
  isOpen={deleteModalOpen}
  itemName={deletingItem?.name || ''}
  onConfirm={handleDeleteItem}
  onCancel={() => {
    setDeleteModalOpen(false);
    setDeletingItem(null);
  }}
/>
```

### 🧪 Testes e Validação

**Testes Unitários (ListItemServiceTest):**

```java
@Test
void shouldDeleteItem() {
    // Given
    UUID listId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    User user = createTestUser();
    List list = createTestList(user, ListType.GENERIC);
    ListItem item = createItem(list, "Item to delete", 1);

    when(listRepository.findById(listId)).thenReturn(Optional.of(list));
    when(listItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    // When
    service.deleteItem(listId, itemId, user);

    // Then
    verify(listItemRepository).delete(item);
    verify(listItemRepository).saveAll(any()); // Reordenação
}

@Test
void shouldReorderPositionsAfterDelete() {
    // Given: Lista com items [0, 1, 2, 3], deletar position 1
    UUID listId = UUID.randomUUID();
    ListItem item2 = createItem(list, "Item 2", 2);
    ListItem item3 = createItem(list, "Item 3", 3);

    when(listItemRepository.findByListIdAndPositionGreaterThanOrderByPositionAsc(listId, 1))
        .thenReturn(List.of(item2, item3));

    // When
    service.reorderPositions(listId, 1);

    // Then
    assertEquals(1, item2.getPosition());
    assertEquals(2, item3.getPosition());
    verify(listItemRepository).saveAll(List.of(item2, item3));
}

@Test
void shouldThrow403WhenNotParticipant() {
    // Given: Usuário não é participante
    User otherUser = createTestUser("other@example.com");

    // When/Then
    assertThrows(ForbiddenException.class, () ->
        service.deleteItem(listId, itemId, otherUser));
}

@Test
void shouldThrow404WhenItemNotFound() {
    // Given: Item não existe
    when(listItemRepository.findById(itemId)).thenReturn(Optional.empty());

    // When/Then
    assertThrows(ItemNotFoundException.class, () ->
        service.deleteItem(listId, itemId, user));
}

@Test
void shouldThrow404WhenItemNotBelongsToList() {
    // Given: Item pertence a outra lista
    List otherList = createTestList(user, ListType.GENERIC);
    ListItem item = createItem(otherList, "Item");

    // When/Then
    assertThrows(ItemNotFoundException.class, () ->
        service.deleteItem(listId, itemId, user));
}
```

**Testes de Integração (ListItemControllerTest):**

```java
@Test
void shouldDeleteItemEndpoint() throws Exception {
    mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", listId, itemId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
}

@Test
void shouldReturn404ForNonExistentItem() throws Exception {
    UUID fakeItemId = UUID.randomUUID();

    mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", listId, fakeItemId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
}

@Test
void shouldReturn403WhenNotParticipant() throws Exception {
    String otherUserToken = generateTokenForOtherUser();

    mockMvc.perform(delete("/api/lists/{listId}/items/{itemId}", listId, itemId)
            .header("Authorization", "Bearer " + otherUserToken))
        .andExpect(status().isForbidden());
}
```

**Testes Frontend:**

```typescript
describe('DeleteConfirmModal', () => {
  it('renders item name in confirmation message', () => {
    const { getByText } = render(
      <DeleteConfirmModal
        isOpen={true}
        itemName="Arroz"
        onConfirm={jest.fn()}
        onCancel={jest.fn()}
      />
    );

    expect(getByText(/Arroz/)).toBeInTheDocument();
    expect(getByText(/Tem certeza que deseja remover/)).toBeInTheDocument();
  });

  it('calls onConfirm when Confirmar is clicked', () => {
    const onConfirm = jest.fn();
    const { getByText } = render(
      <DeleteConfirmModal
        isOpen={true}
        itemName="Arroz"
        onConfirm={onConfirm}
        onCancel={jest.fn()}
      />
    );

    fireEvent.click(getByText('Confirmar'));
    expect(onConfirm).toHaveBeenCalled();
  });

  it('calls onCancel when Cancelar is clicked', () => {
    const onCancel = jest.fn();
    const { getByText } = render(
      <DeleteConfirmModal
        isOpen={true}
        itemName="Arroz"
        onConfirm={jest.fn()}
        onCancel={onCancel}
      />
    );

    fireEvent.click(getByText('Cancelar'));
    expect(onCancel).toHaveBeenCalled();
  });
});

describe('useLongPress', () => {
  it('triggers onLongPress after 1 second', async () => {
    const onLongPress = jest.fn();
    const { result } = renderHook(() => useLongPress({ onLongPress, delay: 1000 }));

    // Simular mouse down
    act(() => {
      result.current.onMouseDown();
    });

    // Aguardar 1 segundo
    await waitFor(() => {
      expect(onLongPress).toHaveBeenCalled();
    }, { timeout: 1100 });
  });

  it('does not trigger onLongPress if released early', async () => {
    const onLongPress = jest.fn();
    const { result } = renderHook(() => useLongPress({ onLongPress, delay: 1000 }));

    // Simular mouse down e up antes de 1 segundo
    act(() => {
      result.current.onMouseDown();
    });

    await new Promise(resolve => setTimeout(resolve, 500)); // 0.5 segundo

    act(() => {
      result.current.onMouseUp();
    });

    await new Promise(resolve => setTimeout(resolve, 600)); // Total 1.1 segundo

    expect(onLongPress).not.toHaveBeenCalled();
  });
});

describe('useItems deleteItem', () => {
  it('removes item optimistically and calls API', async () => {
    const { result } = renderHook(() => useItems(listId));

    // Assumir items iniciais
    act(() => {
      result.current.setItems([
        { id: '1', name: 'Item 1' },
        { id: '2', name: 'Item 2' }
      ]);
    });

    // Mock API call
    jest.spyOn(itemsApi, 'deleteItem').mockResolvedValue(undefined);

    // Deletar item
    await act(async () => {
      await result.current.deleteItem('1');
    });

    // Verificar que item foi removido do estado
    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].id).toBe('2');
  });

  it('reverts state on API error', async () => {
    const { result } = renderHook(() => useItems(listId));

    const originalItems = [
      { id: '1', name: 'Item 1' },
      { id: '2', name: 'Item 2' }
    ];

    act(() => {
      result.current.setItems(originalItems);
    });

    // Mock API call com erro
    jest.spyOn(itemsApi, 'deleteItem').mockRejectedValue(new Error('Network error'));

    // Tentar deletar item
    try {
      await act(async () => {
        await result.current.deleteItem('1');
      });
    } catch (err) {
      // Esperado
    }

    // Verificar que estado foi revertido
    expect(result.current.items).toHaveLength(2);
    expect(result.current.items).toEqual(originalItems);
  });
});
```

### 🚨 Armadilhas Comuns a Evitar

1. **Não fazer hard delete** - Arquitetura define hard delete, não soft delete
2. **Esquecer de reordenar positions** - Positions devem ser recalculadas após delete
3. **Não validar item pertence à lista** - Item pode existir mas pertencer a outra lista
4. **Usar HTTP 200 em vez de 204** - DELETE bem-sucedido deve retornar 204 No Content
5. **Não fazer optimistic update** - UI deve responder imediatamente
6. **Não reverter em caso de erro** - Estado deve voltar ao original se falhar
7. **Não mostrar modal de confirmação** - Ação destrutiva requer confirmação explícita
8. **Long-press muito curto ou longo** - Deve ser exatamente 1 segundo (1000ms)
9. **Cancelar long-press não funciona** - Deve limpar timeout se mouse/touch sair antes de 1s
10. **Não registrar activity log antes de deletar** - Log deve ser criado ANTES do delete para auditoria

### 🔗 Relacionamento com Outras Stories

**Depende de:**
- ✅ Story 3.1: Modelagem de dados (ListItem entity com position field)
- ✅ Story 3.2: Adicionar item (padrões de validação, permissões)
- ✅ Story 3.3: Listar itens (componente ListItem, hook useItems)
- ✅ Story 3.4: Marcar/desmarcar item (padrão de permissão isParticipant)
- ✅ Story 3.5: Editar item (modal pattern, optimistic UI)

**Próximas Stories Usarão:**
- Story 5.2: Broadcast de itens - ITEM_DELETED será broadcastado em tempo real
- Story 6.2: Registro de atividades de itens - Ação de delete será registrada no activity log

**Esta Story Habilita:**
- ✅ CRUD completo de itens (Create, Read, Update, Delete)
- ✅ Epic 3 completamente finalizado
- ✅ Base para sincronização real-time de deletes (Epic 5)
- ✅ Base para auditoria de deletes (Epic 6)

### 📊 Checklist de Implementação

Antes de marcar esta story como completa:

**Backend:**
- [ ] Método deleteItem no ListItemService com validação completa
- [ ] Método reorderPositions para recalcular positions
- [ ] Query customizada no ListItemRepository para reordenação
- [ ] Validação de permissão (participante da lista)
- [ ] Validação de item pertencente à lista
- [ ] Endpoint DELETE /api/lists/{id}/items/{itemId} funcionando
- [ ] RFC 7807 error responses configurados (403, 404)
- [ ] HTTP 204 No Content para sucesso

**Frontend:**
- [ ] API call deleteItem no itemsApi.ts com tratamento de erros
- [ ] Hook useLongPress criado com delay de 1 segundo
- [ ] Função deleteItem no hook useItems com optimistic update e rollback
- [ ] Componente ItemOptionsMenu criado com Editar/Remover
- [ ] Componente DeleteConfirmModal criado com confirmação
- [ ] Modal mostra nome do item dinamicamente
- [ ] Modal fecha em sucesso ou cancelar
- [ ] Animação fade-out de 200ms
- [ ] Toast "Item removido" após sucesso
- [ ] Toast específico para erros (403, 404)

**Integração:**
- [ ] ListItem chama onDelete ao long-press de 1 segundo
- [ ] Long-press cancela se soltar antes de 1 segundo
- [ ] ListView gerencia estado do modal (isOpen, deletingItem)
- [ ] Menu dropdown posicionado corretamente

**Testes:**
- [ ] Testes unitários backend: 5 testes (delete, reorder, erros 403/404)
- [ ] Testes integração backend: 3 testes (204, 403, 404)
- [ ] Testes frontend: 6 testes (modal, long-press, hooks)
- [ ] Todos os testes passando

### Project Structure Notes

**Alinhamento com Estrutura de Projeto Unificada:**

Esta story segue o padrão já estabelecido:
- `listitem/` módulo (DDD-lite)
- Controller → Service → Repository → Entity
- Componentes React com TypeScript
- Hooks customizados para estado e interações
- Modais glassmorphism (padrão visual já existente)

**Padrões de Código Estabelecidos (Stories Anteriores):**

- **Constructor injection:** Sem @Autowired
- **@Transactional:** Operações de escrita
- **RFC 7807:** Erros padronizados
- **Optimistic UI:** Atualização local imediata + revert em erro
- **HTTP 204:** DELETE bem-sucedido retorna No Content (sem body)
- **Testes:** @DataJpaTest, @WebMvcTest, @SpringBootTest
- **Frontend:** Hooks customizados, componentes funcionais, Tailwind CSS
- **Modals:** Padrão glassmorphism com backdrop-blur

### References

**Epics e Stories:**
- [Fonte: _bmad-output/planning-artifacts/epics.md#Story-3.6]
  - Epic 3: Gestão de Itens
  - Story 3.6: Remover Item
  - FR17: Participante da lista pode remover itens

**Story Anterior:**
- [Fonte: _bmad-output/implementation-artifacts/3-5-editar-item.md]
  - Story 3.5: Editar Item
  - Padrão de permissão (isParticipant)
  - Padrão de validação (403, 404)
  - Optimistic UI + rollback em erro
  - Modal pattern com glassmorphism

**Decisões Arquiteturais:**
- [Fonte: _bmad-output/planning-artifacts/architecture.md]
  - **Decision #004:** RFC 7807 Problem Details
  - **Decision #002:** Data Model (hard delete, positions)
  - **HTTP 204:** DELETE retorna No Content
  - **Reordenação:** Positions recalculadas após delete

**Git Intelligence (Recent Commits):**
```
d37b6db fix(listitem): add missing test helper and fix test assertion
c0eca50 feat(listitem): implement edit item with code review fixes (story 3-5)
a71c0f8 feat(listitem): implement toggle item check with code review fixes (story 3-4)
fc64fd1 feat(listitem): implement list items view with code review fixes (story 3-3)
```

**Padrões de Commit:**
- Formato: `feat(scope): description`
- Referência à story: `(story 3-6)`

## Dev Agent Record

### Agent Model Used

Claude Code (claude-opus-4-6)

### Debug Log References

Nenhum problema crítico encontrado durante a implementação.

### Completion Notes List

- **Backend (Java/Spring Boot):**
  - Implementado método `deleteItem` no ListItemService com validações de permissão (403), existência (404) e reordenação automática de positions
  - Adicionado query method `findByListIdAndPositionGreaterThanOrderByPositionAsc` no ListItemRepository para reordenação
  - Criado endpoint DELETE `/api/lists/{listId}/items/{itemId}` no ListItemController com documentação OpenAPI completa
  - Todos os testes unitários (6) e de integração (7) passando

- **Frontend (React/TypeScript):**
  - Criado hook `useLongPress` para detectar pressionamento longo (1 segundo) com prevenção de click após long-press
  - Criado componente `ItemOptionsMenu` com opções Editar/Remover (glassmorphism)
  - Criado componente `DeleteConfirmModal` com confirmação de exclusão
  - Modificado `ListItem` para suportar long-press, abrir menu e aplicar animação fade-out
  - Adicionado método `deleteItem` no hook `useItems` com animação fade-out de 200ms antes de remover
  - Adicionada animação CSS `fadeOut` em `index.css` conforme AC6
  - Integrado modal de confirmação no `ListView` com Toast de sucesso/erro e prop `isDeleting`
  - Build do frontend passando sem erros TypeScript

- **Testes:**
  - Backend: 37 testes passando (6 novos de deleteItem)
  - Frontend: 20 testes passando
    - 5 testes de `useLongPress` (novo arquivo)
    - 8 testes de `DeleteConfirmModal` (novo arquivo)
    - 7 testes de `useItems` (3 anteriores + 4 novos de deleteItem)

- **Code Review Fixes Aplicados (2026-02-14):**
  - ✅ Fix 1 (HIGH): Implementada animação fade-out de 200ms antes de remover item
  - ✅ Fix 2 (HIGH): File List atualizado com 3 arquivos não documentados
  - ✅ Fix 3 (MEDIUM): useLongPress corrigido para prevenir click após long-press
  - ✅ Fix 4 (MEDIUM): Adicionados 17 testes frontend faltantes

### Change Log

- 2026-02-14: Story 3.6 implementada - Remover Item (backend + frontend completo)
  - Backend: deleteItem service, DELETE endpoint, reordenação de positions, 13 testes
  - Frontend: useLongPress hook, ItemOptionsMenu, DeleteConfirmModal, integração ListView

### File List

**Arquivos a Serem Modificados/Criados:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── controller/
│   │   └── ListItemController.java       # [MODIFICAR] +DELETE endpoint
│   ├── service/
│   │   └── ListItemService.java          # [MODIFICAR] +deleteItem + reorderPositions
│   └── repository/
│       └── ListItemRepository.java       # [MODIFICAR] +query para reordenação

test/java/br/com/leoferolive/nossalista/listitem/
├── service/
│   └── ListItemServiceTest.java          # [MODIFICAR] +5 testes delete
└── controller/
    └── ListItemControllerTest.java       # [MODIFICAR] +3 testes DELETE endpoint

frontend/src/
├── api/
│   └── itemsApi.ts                       # [MODIFICAR] +deleteItem
├── hooks/
│   ├── useItems.ts                        # [MODIFICAR] +deleteItem com fade-out
│   ├── useItems.test.ts                   # [MODIFICAR] +testes deleteItem
│   └── useLongPress.ts                    # [CRIAR] Hook para long-press
├── components/
│   ├── ListItem.tsx                       # [MODIFICAR] Integrar long-press + isDeleting
│   ├── ItemOptionsMenu.tsx                # [CRIAR] Menu Editar/Remover
│   └── DeleteConfirmModal.tsx             # [CRIAR] Modal de confirmação
├── pages/
│   └── ListView.tsx                       # [MODIFICAR] Handler + modal state + isDeleting prop
├── types/
│   └── Item.ts                            # [MODIFICAR] +isDeleting em ListItemProps
└── index.css                              # [MODIFICAR] +animação fade-out
```

**Arquivos de Referência (NÃO modificar):**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── domain/ListItem.java              # Campos: name, position, checked, etc
│   ├── domain/ListType.java              # Enum: SHOPPING, TASK, WISHLIST, GENERIC
│   ├── dto/ListItemResponseDTO.java
│   └── exception/
│       ├── ListNotFoundException.java
│       ├── ItemNotFoundException.java
│       └── ForbiddenException.java

frontend/src/
├── components/EditItemModal.tsx          # [REFERÊNCIA] Padrão de modal glassmorphism
├── components/ListItem.tsx               # Checkbox e texto separados
├── index.css                             # [REFERÊNCIA] Animações, glassmorphism
└── hooks/useToast.ts                     # [REFERÊNCIA] Toast system
```
