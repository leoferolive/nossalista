# Story 5.3: Sincronização de Checkbox (Item Check)

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário,
I want ver quando alguém marca/desmarca item em tempo real,
so that eu saiba o que já foi feito/comprado.

## Acceptance Criteria

**AC1 — Pop animation no checkbox ao receber ITEM_CHECKED via WebSocket:**
**Given** mensagem `ITEM_CHECKED` recebida (de qualquer usuário)
**Then** animação "pop" (300ms) é aplicada ao checkbox do item correspondente
**And** a animação funciona tanto para marcar (false → true) quanto para desmarcar (true → false)

**AC2 — Visual de item marcado:**
**Given** item com `checked: true`
**Then** texto exibe `line-through` e `opacity-50`
**And** checkbox exibe fundo azul com ícone de check (✓)
*(Nota: Já implementado em Story 3.4/5.2, manter comportamento)*

**AC3 — Yellow pulse highlight para ITEM_CHECKED de OUTRO usuário:**
**Given** mensagem `ITEM_CHECKED` recebida de outro usuário (não o próprio)
**Then** highlight amarelo (pulse 200ms) é aplicado ao item inteiro por 300ms
**And** Toast `"{username} marcou {itemName}"` exibido se `payload.checked === true`
**And** Toast `"{username} desmarcou {itemName}"` exibido se `payload.checked === false`

**AC4 — Sem eco de toast para ação própria:**
**Given** mensagem `ITEM_CHECKED` recebida onde `userId === currentUser.id`
**Then** Toast de outros usuários NÃO é exibido (sem duplicação)
**And** Toast "Sincronizado" já exibido via `handleToggleItem()` é suficiente

**AC5 — Latência (NFR-P1):**
**Given** 3+ clientes conectados ao mesmo `/topic/list/{listId}`
**Then** todos recebem o broadcast em < 500ms após o toggle REST
*(Nota: Garantido pela infraestrutura existente — sem código novo)*

**AC6 — Last-write-wins:**
**Given** dois usuários marcam/desmarcam o mesmo item simultaneamente
**Then** o estado final é o do último broadcast recebido (last-write-wins)
**And** a UI reflete o estado final sem inconsistências
*(Nota: Garantido pelo `setItems` no handler existente — sem código novo)*

## Tasks / Subtasks

### Frontend — Tipos

- [x] Atualizar `frontend/src/types/Item.ts`: adicionar `isWsChecked?: boolean` em `ListItemProps` (AC: 1, 3)

### Frontend — CSS

- [x] Adicionar animação `ws-item-checked` em `frontend/src/index.css` (AC: 3)
  - [x] Definir `@keyframes wsCheckedPulse` com yellow highlight (fundo amarelo-âmbar que desaparece)
  - [x] Definir `.ws-item-checked { animation: wsCheckedPulse 300ms ease-out forwards; }`

### Frontend — ListItem Component

- [x] Atualizar `frontend/src/components/ListItem.tsx` para suportar `isWsChecked` (AC: 1, 3)
  - [x] Adicionar prop `isWsChecked?: boolean` (já declarado no tipo acima)
  - [x] Aplicar classe `ws-item-checked` ao container do item quando `isWsChecked === true`
  - [x] Substituir inline style do checkbox por classe CSS `animate-pop` condicionada a `isWsChecked`
  - [x] Manter a lógica visual existente: fundo azul/check icon quando `item.checked`, `line-through`/`opacity-50` no texto

### Frontend — ListView

- [x] Atualizar `frontend/src/pages/ListView.tsx` (AC: 1, 3, 4)
  - [x] Adicionar estado: `const [wsCheckedItemIds, setWsCheckedItemIds] = useState<Set<string>>(new Set())`
  - [x] No handler `ITEM_CHECKED`: adicionar item ID ao `wsCheckedItemIds`, remover após 300ms via `setTimeout`
  - [x] O `setTimeout` de limpeza deve ser executado **independentemente** de `isOwnAction` (pop funciona para todos)
  - [x] A lógica de toast (yellow highlight context) permanece dentro do `!isOwnAction` gate existente
  - [x] Passar `isWsChecked={wsCheckedItemIds.has(item.id)}` ao `ListItemComponent` (junto com `isWsAdded` existente)

### Testes Frontend

- [x] Atualizar `frontend/src/components/ListItem.test.tsx` (AC: 1, 3)
  - [x] Teste: `isWsChecked=true` aplica classe `ws-item-checked` no container
  - [x] Teste: `isWsChecked=true` aplica classe `animate-pop` no checkbox
  - [x] Teste: `isWsChecked=false` (default) não aplica essas classes

- [x] Atualizar `frontend/src/pages/ListView.test.tsx` (AC: 1, 3, 4)
  - [x] Simular mensagem `ITEM_CHECKED` de OUTRO usuário (checked: true): verificar toast "marcou"
  - [x] Simular mensagem `ITEM_CHECKED` de OUTRO usuário (checked: false): verificar toast "desmarcou"
  - [x] Simular mensagem `ITEM_CHECKED` do PRÓPRIO usuário: verificar que toast de "marcou/desmarcou" NÃO é exibido

## Dev Notes

### Contexto da Story

Esta story é **frontend-only**. O backend já implementou `ITEM_CHECKED` broadcast em `toggleItemCheck()` na Story 5.2. A infra WebSocket (Story 5.1) e o handler básico de `ITEM_CHECKED` no `ListView.tsx` (Story 5.2) já existem.

O objetivo desta story é **enriquecer o feedback visual** para marcar/desmarcar itens de outros usuários:
1. **Yellow highlight** no item inteiro (feedback de quem foi o alvo da alteração)
2. **Pop animation no checkbox** para desmarcar (atualmente só ocorre no sentido check→true)

### Developer Context Section

#### Estado atual do ListView.tsx — ITEM_CHECKED handler (linha ~154)

O handler já existe e funciona:
```typescript
case 'ITEM_CHECKED':
  if (!isOwnAction) {
    setItems((prev) =>
      prev.map((i) =>
        i.id === message.payload.id ? { ...i, checked: message.payload.checked } : i
      )
    );
    const action = message.payload.checked ? 'marcou' : 'desmarcou';
    showToast(`${message.username} ${action} ${message.payload.name}`, 'info');
  }
  break;
```

**Modificação necessária:** adicionar gerenciamento de `wsCheckedItemIds` **fora** do `if (!isOwnAction)`:
```typescript
case 'ITEM_CHECKED':
  // Pop animation funciona para todos (inclusive próprio usuário)
  setWsCheckedItemIds((prev) => new Set([...prev, message.payload.id]));
  setTimeout(() => {
    setWsCheckedItemIds((prev) => {
      const next = new Set(prev);
      next.delete(message.payload.id);
      return next;
    });
  }, 300);

  if (!isOwnAction) {
    setItems((prev) =>
      prev.map((i) =>
        i.id === message.payload.id ? { ...i, checked: message.payload.checked } : i
      )
    );
    const action = message.payload.checked ? 'marcou' : 'desmarcou';
    showToast(`${message.username} ${action} ${message.payload.name}`, 'info');
  }
  break;
```

#### Pop animation no ListItem.tsx — problema com uncheck

**Situação atual** (linha ~89):
```typescript
style={{
  animation: item.checked
    ? 'pop 300ms cubic-bezier(0.175, 0.885, 0.32, 1.275)'
    : 'none',
}}
```

**Problema:** quando `checked` vai de `true` → `false` (desmarcar via WS), o animation é setado para `'none'`, sem efeito visual. Somente o sentido check tem pop.

**Solução:** usar a prop `isWsChecked` para adicionar classe `animate-pop` ao invés de depender do estado `item.checked`. O `animate-pop` já está definido em `index.css`:
```css
.animate-pop {
  animation: pop 300ms cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
```

No `ListItem.tsx`:
```typescript
<button
  onClick={handleCheckboxClick}
  className={`w-6 h-6 rounded border-2 flex items-center justify-center transition-all duration-300 ${
    item.checked
      ? 'bg-blue-500 border-blue-500'
      : 'border-gray-300 hover:border-blue-400'
  } ${isWsChecked ? 'animate-pop' : ''}`}
  // Remover o inline style de animation
  aria-label={item.checked ? 'Marcar como não concluído' : 'Marcar como concluído'}
  aria-checked={item.checked}
  role="checkbox"
>
```

**CRÍTICO:** remover o `style={{ animation: ... }}` inline do checkbox ao adicionar a classe.

#### CSS para yellow highlight

Adicionar em `frontend/src/index.css`:
```css
/* Animação para itens marcados/desmarcados via WebSocket */
@keyframes wsCheckedPulse {
  0% { background-color: rgba(251, 191, 36, 0.4); } /* amber-400 com 40% opacidade */
  100% { background-color: transparent; }
}

.ws-item-checked {
  animation: wsCheckedPulse 300ms ease-out forwards;
}
```

#### Aplicação do ws-item-checked no ListItem.tsx

```typescript
<div
  id={`list-item-${item.id}`}
  className={`flex items-center gap-3 p-3 rounded-lg border transition-all cursor-pointer hover:bg-gray-50 ${
    item.checked ? 'opacity-50' : ''
  } ${isDeleting ? 'animate-fade-out' : ''} ${isWsAdded ? 'ws-item-added' : ''} ${isWsChecked ? 'ws-item-checked' : ''}`}
  ...
>
```

#### Padrão de props existente — isWsAdded

A prop `isWsChecked` segue o mesmo padrão de `isWsAdded` (já existente):
- Declarada em `ListItemProps` como `isWsChecked?: boolean`
- Defaultada para `false` em `ListItemComponent`
- Passada de `ListView.tsx` como `isWsChecked={wsCheckedItemIds.has(item.id)}`
- Gerenciada por `Set<string>` com cleanup via `setTimeout`

### Technical Requirements

- **Apenas frontend** — backend inalterado (Story 5.2 já implementou tudo no backend)
- **Set mutável:** usar pattern funcional `new Set([...prev, id])` para evitar mutação direta do Set no estado React
- **Cleanup do timeout:** o `setTimeout` em `wsCheckedItemIds` não precisa de cleanup no unmount (opera sobre state, não refs — React lida com isso)
- **Duração 300ms:** consistente com `wsAddedItemIds` (Story 5.2) e `animate-fade-out` (200ms)
- **animate-pop:** a classe CSS já existe em `index.css` (linha ~56-58) — não duplicar

### Architecture Compliance

- **Sem Redux/Zustand** (Decisão #006) — estado local com `useState` ✅
- **Sem novos hooks** — lógica inline no `handleWebSocketMessage` com `setWsCheckedItemIds` ✅
- **`useCallback` para handler:** o `handleWebSocketMessage` já usa `useCallback` — adicionar `setWsCheckedItemIds` à lista de dependências ✅
- **CSS classes > inline styles:** migrar do inline `style={{ animation }}` para classes CSS (`animate-pop`) — mais idiomático com Tailwind

### Library Framework Requirements

- **React 19 + Vite:** sem novas dependências
- **@stomp/stompjs v7:** infraestrutura WebSocket existente, sem alteração
- **Tailwind CSS:** `ws-item-checked` é uma classe custom em `index.css` (não utilidade Tailwind — correto, seguir padrão de `ws-item-added`)

### File Structure Requirements

**Frontend — arquivos modificados:**
- `frontend/src/types/Item.ts` — adicionar `isWsChecked?: boolean` em `ListItemProps`
- `frontend/src/index.css` — adicionar `@keyframes wsCheckedPulse` e `.ws-item-checked`
- `frontend/src/components/ListItem.tsx` — aplicar `ws-item-checked` e `animate-pop` via prop, remover inline style
- `frontend/src/pages/ListView.tsx` — adicionar `wsCheckedItemIds` state e gerenciamento no handler
- `frontend/src/components/ListItem.test.tsx` — novos testes para `isWsChecked`
- `frontend/src/pages/ListView.test.tsx` — testes para toast de marcou/desmarcou

**Sem novos arquivos.**

### Testing Requirements

**Frontend — novos testes em `ListItem.test.tsx`:**
```typescript
it('deve aplicar classe ws-item-checked quando isWsChecked=true', () => {
  render(
    <ListItemComponent
      item={mockItem}
      onToggle={vi.fn()}
      onEdit={vi.fn()}
      isWsChecked={true}
    />
  );
  const itemContainer = screen.getByTestId(`list-item-${mockItem.id}`);
  expect(itemContainer).toHaveClass('ws-item-checked');
});

it('deve aplicar classe animate-pop no checkbox quando isWsChecked=true', () => {
  render(
    <ListItemComponent
      item={mockItem}
      onToggle={vi.fn()}
      onEdit={vi.fn()}
      isWsChecked={true}
    />
  );
  const checkbox = screen.getByRole('checkbox');
  expect(checkbox).toHaveClass('animate-pop');
});

it('não deve aplicar ws-item-checked por padrão', () => {
  render(
    <ListItemComponent item={mockItem} onToggle={vi.fn()} onEdit={vi.fn()} />
  );
  const itemContainer = screen.getByTestId(`list-item-${mockItem.id}`);
  expect(itemContainer).not.toHaveClass('ws-item-checked');
});
```

**Frontend — novos testes em `ListView.test.tsx`:**
```typescript
it('deve exibir toast "marcou" ao receber ITEM_CHECKED de outro usuário', () => {
  // Mock subscribe que chama callback com ITEM_CHECKED { checked: true }
  const mockSubscribe = vi.fn((listId, callback) => {
    callback({
      type: 'ITEM_CHECKED',
      payload: { ...existingItem, checked: true },
      userId: 'other-user-id',
      username: 'maria',
      timestamp: '2026-03-02T00:00:00Z',
    });
  });
  // ... setup mocks, render ListView
  // Assert: toast com "maria marcou {itemName}" aparece
});

it('deve exibir toast "desmarcou" ao receber ITEM_CHECKED (checked=false) de outro usuário', () => {
  // Mesma estrutura, payload.checked = false
  // Assert: toast com "maria desmarcou {itemName}" aparece
});

it('não deve exibir toast de "marcou/desmarcou" para ação do próprio usuário', () => {
  // callback com userId === currentUser.id
  // Assert: sem toast de "marcou" ou "desmarcou"
  // Assert: estado checked atualizado via setItems NÃO ocorre (isOwnAction)
});
```

**Executar antes de marcar done:**
- Frontend: `npm test -- --run` (123 testes existentes + novos devem passar, 0 falhas)
- Backend: `./mvnw test` (sem alterações — só para garantir que nada quebrou)

### Previous Story Intelligence (Story 5.2)

**Padrão de wsAddedItemIds** (referência direta para wsCheckedItemIds):
```typescript
// Estado para animação pulse de itens adicionados via WS
const [wsAddedItemIds, setWsAddedItemIds] = useState<Set<string>>(new Set());

// No handler ITEM_ADDED:
setWsAddedItemIds((prev) => new Set([...prev, message.payload.id]));
setTimeout(() => {
  setWsAddedItemIds((prev) => {
    const next = new Set(prev);
    next.delete(message.payload.id);
    return next;
  });
}, 300);
```

**`handleWebSocketMessage` usa `useCallback`** (linha ~118 de ListView.tsx):
- Dependências atuais: `[currentUser?.id, setItems, showToast]`
- Após esta story: adicionar `setWsCheckedItemIds` às dependências (ou deixar fora — é um state setter estável no React)
- **Nota:** setters de `useState` são estáveis (não mudam entre renders), não precisam ser declarados nas dependências do useCallback

**`isWsAdded` prop passada ao ListItemComponent** (linha ~763-764):
```tsx
<ListItemComponent
  key={item.id}
  item={item}
  onToggle={handleToggleItem}
  onEdit={handleEditItem}
  onDelete={handleDeleteItem}
  isDeleting={deletingItemId === item.id}
  isWsAdded={wsAddedItemIds.has(item.id)}
/>
```
Adicionar `isWsChecked={wsCheckedItemIds.has(item.id)}` neste mesmo bloco.

**325 backend + 123 frontend** testes passando antes desta story — manter verde.

**Aprendizados relevantes de Story 5.2:**
- Dois `useEffect` separados (connect vs subscribe) — padrão correto e estável ✅
- `handleWebSocketMessage` com `useCallback` e dependências estáveis — seguir ✅
- Inline style para animação no ListItem foi abordagem simples mas limitada — migrar para classe CSS ✅
- `isWsAdded` tem cleanup correto com `Set` funcional — replicar o mesmo padrão ✅

### Git Intelligence Summary

**Padrão de commit estabelecido:**
- `feat(websocket): <descrição> (story 5.3)` ou `feat(frontend): <descrição> (story 5.3)`
- Commits anteriores relevantes:
  - `a05706f chore(sprint): mark story 5.2 as done after code review fixes`
  - `1f0a67b test(frontend): add ITEM_UPDATED and ITEM_CHECKED WebSocket handler tests`
  - `49312f5 fix(frontend): apply ws-item-added pulse animation and guard connect with isAuthenticated`

**Arquivos recentemente modificados relevantes:**
- `frontend/src/pages/ListView.tsx` — modificado em múltiplos commits da Story 5.2
- `frontend/src/components/ListItem.tsx` — última modificação significativa em Story 3.x
- `frontend/src/index.css` — animações adicionadas progressivamente

### Latest Tech Information

**React 19 + useState com Set:**
- Sets não são primitivos — React detecta mudança apenas por referência
- `new Set([...prev, id])` cria novo Set (referência nova) → trigger de re-render correto ✅
- `const next = new Set(prev); next.delete(id); return next;` também cria referência nova ✅
- **NUNCA:** `prev.add(id); return prev;` — React não detectaria mudança (mesma referência)

**CSS Animation + React:**
- Quando `isWsChecked` muda de `false` → `true` → `false`, a classe `ws-item-checked` é adicionada e removida
- O browser re-aplica a animação quando a classe é re-adicionada (mesmo item)
- Para garantir re-play: pode ser necessário forçar reflow — mas com Set removal após 300ms isso não é problema pois o Set é limpo antes do próximo evento

**Tailwind CSS + Custom Animations:**
- Classes custom em `index.css` coexistem com Tailwind utilities ✅
- `animation: wsCheckedPulse 300ms ease-out forwards` → `forwards` preserva o estado final (background transparent)

### References

- Story 5.1: `_bmad-output/implementation-artifacts/5-1-setup-e-configuracao-de-websocket.md`
- Story 5.2: `_bmad-output/implementation-artifacts/5-2-formato-de-mensagem-e-broadcast-de-itens.md`
- Epic 5, Story 5.3: `_bmad-output/planning-artifacts/epics.md`
- `ListView.tsx`: `frontend/src/pages/ListView.tsx`
- `ListItem.tsx`: `frontend/src/components/ListItem.tsx`
- `Item.ts` (tipos): `frontend/src/types/Item.ts`
- `index.css`: `frontend/src/index.css`
- `ListItem.test.tsx`: `frontend/src/components/ListItem.test.tsx`
- `ListView.test.tsx`: `frontend/src/pages/ListView.test.tsx`

## Project Context Reference

- `project-context.md` não encontrado no workspace. Contexto derivado de epics.md, architecture.md e stories anteriores do Epic 5.

## Story Completion Status

- Status: `review`
- Completion note: Implementação concluída, testes atualizados e suíte de regressão executada (frontend + backend).

## Dev Agent Record

### Agent Model Used

openai/gpt-5.3-codex

### Debug Log References

- Frontend (RED): `npm test -- --run src/components/ListItem.test.tsx src/pages/ListView.test.tsx`
- Frontend (GREEN): `npm test -- --run src/components/ListItem.test.tsx src/pages/ListView.test.tsx`
- Frontend (regressão): `npm test -- --run`
- Backend (regressão): `./mvnw test`

### Completion Notes List

- Implementado suporte a `isWsChecked` em `ListItemProps` e no `ListItemComponent`, incluindo `ws-item-checked` no container e `animate-pop` no checkbox.
- Removido o `style` inline de animação do checkbox e migrada a animação para classe CSS condicional, preservando visual de checked/unchecked existente.
- Adicionada animação `ws-item-checked` (`@keyframes wsCheckedPulse`) em `frontend/src/index.css` para highlight amarelo de 300ms.
- Atualizado `ListView` para manter `wsCheckedItemIds` e aplicar pop/highlight em toda mensagem `ITEM_CHECKED`, com cleanup de 300ms independente de `isOwnAction`.
- Mantida a regra de toast apenas para ações de outros usuários (`!isOwnAction`) e coberta com testes.
- Testes atualizados e validados: frontend 127/127 passando; backend 327 testes sem falhas (1 skipped já existente).

### File List

- `frontend/src/types/Item.ts`
- `frontend/src/index.css`
- `frontend/src/components/ListItem.tsx`
- `frontend/src/pages/ListView.tsx`
- `frontend/src/components/ListItem.test.tsx`
- `frontend/src/pages/ListView.test.tsx`

### Change Log

- 2026-03-02: Implementada sincronização visual de `ITEM_CHECKED` (pop + highlight), ajustes em `ListView` e cobertura de testes para cenários de outros usuários e ação própria.
