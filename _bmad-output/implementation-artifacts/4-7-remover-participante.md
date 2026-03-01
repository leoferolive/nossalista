# Story 4.7: Remover Participante

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a dono de uma lista,
I want remover participantes da minha lista,
So that eu possa controlar quem tem acesso.

## Acceptance Criteria

**AC1 — Endpoint DELETE disponível:**
**Given** endpoint DELETE /api/lists/{id}/members/{userId} disponível
**When** request com JWT válido, requester é OWNER, userId é MEMBER da lista
**Then** 204 No Content, registro `list_members` removido, membro não acessa mais a lista

**AC2 — Bloquear remoção do dono:**
**Given** endpoint DELETE /api/lists/{id}/members/{userId}
**When** userId é o OWNER da lista
**Then** 403 Forbidden com body RFC 7807: `"O dono não pode ser removido"`

**AC3 — Apenas OWNER pode remover:**
**Given** endpoint DELETE /api/lists/{id}/members/{userId}
**When** requester NÃO é OWNER (é MEMBER ou não é membro)
**Then** 403 Forbidden com body RFC 7807: `"Apenas o dono pode remover participantes"`

**AC4 — Botão Remover no painel de membros:**
**Given** painel de membros aberto, usuário logado é OWNER
**When** visualiza a lista de membros
**Then** cada MEMBER tem botão de lixeira "Remover" ao lado
**And** o próprio OWNER não tem botão de remoção ao lado do seu nome

**AC5 — Modal de confirmação:**
**Given** OWNER toca "Remover" ao lado de um membro
**Then** aparece modal de confirmação: "Remover {username}? Ação não pode ser desfeita."
**And** botões "Cancelar" e "Confirmar Remoção" visíveis

**AC6 — Fluxo de remoção confirmada:**
**Given** OWNER confirma remoção no modal
**Then** DELETE /api/lists/{id}/members/{userId} é chamado
**And** Toast "{username} removido da lista" exibido
**And** modal fecha
**And** membro removido desaparece da lista de membros imediatamente

**AC7 — Hook de activity log:**
**Given** `MemberService.removeMember` chamado com sucesso
**When** remoção concluída
**Then** prepara gancho para activity log (Epic 6) sem bloquear esta entrega (stub/comment)

## Tasks / Subtasks

### Backend (Java/Spring)

- [x] Implementar `DELETE /api/lists/{id}/members/{userId}` em `MemberController` (AC: 1, 2, 3)
  - [x] Adicionar método `removeMember` no controller com `@DeleteMapping("/{listId}/members/{userId}")`
  - [x] Anotar com `@Operation`, `@ApiResponses` e `@SecurityRequirement` (padrão Swagger do projeto)
  - [x] Retornar `ResponseEntity<Void>` com status 204 em caso de sucesso
- [x] Implementar `removeMember` em `MemberService` (AC: 1, 2, 3, 7)
  - [x] Verificar se requester é OWNER da lista (403 se não for)
  - [x] Verificar se targetUserId não é o OWNER (403 se for)
  - [x] Verificar se targetUser é membro da lista (404 se não encontrado)
  - [x] Deletar o registro `ListMember` via `ListMemberRepository`
  - [x] Adicionar stub/comentário para hook de activity log (Epic 6)

### Frontend (React/TypeScript)

- [x] Adicionar suporte a remoção de membro em `MembersModal.tsx` (AC: 4, 5, 6)
  - [x] Adicionar prop `onRemoveMember: (userId: string, username: string) => void`
  - [x] Adicionar props de estado: `removingMemberId: string | null`, `removeConfirmMemberId: string | null`
  - [x] Adicionar props de controle: `onConfirmRemove: () => void`, `onCancelRemove: () => void`
  - [x] Renderizar botão lixeira em cada MEMBER row (somente quando `isOwner === true`)
  - [x] Renderizar modal de confirmação inline: "Remover {username}? Ação não pode ser desfeita."
- [x] Implementar lógica de remoção em `ListView.tsx` (AC: 4, 5, 6)
  - [x] Adicionar estado: `removeConfirmMemberId`, `removingMemberId`
  - [x] Implementar `handleRemoveMember(userId, username)` — abre modal de confirmação
  - [x] Implementar `handleConfirmRemove()` — chama `deleteListMember`, exibe toast, atualiza lista
  - [x] Implementar `handleCancelRemove()` — fecha modal sem ação
  - [x] Atualizar lista local de membros removendo o membro após sucesso (sem re-fetch completo)
- [x] Adicionar `deleteListMember` em `listsApi.ts` (AC: 1)
  - [x] `DELETE /api/lists/{listId}/members/{userId}` retornando `Promise<void>`

### Testing

- [x] Backend unit tests em `MemberServiceTest` para `removeMember` (AC: 1, 2, 3)
  - [x] OWNER remove MEMBER com sucesso (delete chamado, 204 implícito)
  - [x] Não-OWNER tenta remover → 403 "Apenas o dono pode remover participantes"
  - [x] OWNER tenta remover o próprio OWNER → 403 "O dono não pode ser removido"
  - [x] OWNER tenta remover usuário que não é membro → 404
- [x] Backend integration tests em `MemberControllerIntegrationTest` (AC: 1, 2, 3)
  - [x] `DELETE /members/{userId}` com OWNER removendo MEMBER → 204
  - [x] `DELETE /members/{userId}` sem autenticação → 401
  - [x] `DELETE /members/{userId}` por MEMBER (não OWNER) → 403
  - [x] `DELETE /members/{ownerId}` tentando remover o OWNER → 403
  - [x] `DELETE /members/{userId}` com userId não membro → 404
- [x] Frontend tests em `MembersModal.test.tsx` (AC: 4, 5, 6)
  - [x] Botão de remoção visível para MEMBER quando `isOwner === true`
  - [x] Botão de remoção NÃO visível quando `isOwner === false`
  - [x] OWNER não tem botão de remoção ao lado do próprio nome
  - [x] Modal de confirmação aparece ao clicar no botão de remoção
  - [x] `onConfirmRemove` chamado ao confirmar; `onCancelRemove` ao cancelar

## Dev Notes

### Contexto da Story

Esta story fecha o Epic 4 (Compartilhamento e Colaboração) do lado do OWNER: após poder convidar (4.2, 4.5) e ver membros (4.6), o dono agora pode remover participantes indesejados. Complementa a saída voluntária (Story 4.6 — `POST /leave`) com a remoção forçada pelo OWNER.

Dependências diretas:
- **Story 4.6** entregou `GET /members`, `POST /leave`, `MembersModal.tsx` — reutilizar toda essa estrutura.
- **Story 4.5** entregou `POST /invite` e padrões de validação de ownership no `MemberService` — reutilizar os mesmos guards.

### Developer Context Section

- **NÃO criar novo módulo**: toda lógica de remoção vai em `MemberService.removeMember` (análogo ao `leaveList` já existente).
- **NÃO modificar o OWNER**: a regra "OWNER não pode ser removido" é análoga ao "OWNER não pode sair" — mesma estratégia de verificação.
- **Reutilizar `MembersModal`**: o componente é puramente apresentacional (props-driven). Adicionar novas props de remoção sem refatorar a estrutura.
- **Activity log**: apenas preparar hook (comentário/stub) — Epic 6 não está implementado ainda. Não bloquear a entrega.

### Technical Requirements

- Contrato REST:
  - `DELETE /api/lists/{listId}/members/{userId}` → 204 No Content (sem body) em sucesso
  - Erros em RFC 7807 (ProblemDetail): 401, 403, 404
- Regras de autorização:
  - Somente OWNER da lista pode remover membros
  - OWNER não pode ser removido (nem por si mesmo)
  - Usuário não-membro retorna 404 (não 403) para não vazar informações
- UUID como tipo de ID para `listId` e `userId` (padrão do projeto)

### Architecture Compliance

- **Backend estrutura feature-based**: novos métodos em `member/controller/MemberController.java` e `member/service/MemberService.java` — não criar novos arquivos desnecessários.
- **Frontend estrutura por tipo**: lógica em `pages/ListView.tsx`, UI em `components/MembersModal.tsx`, chamada HTTP em `api/listsApi.ts`.
- Padrões globais obrigatórios:
  - JSON em `snake_case`
  - Datas em ISO 8601 UTC
  - Erros em RFC 7807 (ProblemDetail)
  - `@AuthenticationPrincipal User authenticatedUser` no controller para obter usuário logado
  - `ResponseEntity<Void>` com `.noContent().build()` para 204

### Library Framework Requirements

- Spring Boot 4.x + Spring Security 7.x — sem novas dependências necessárias.
- React 19 + TypeScript — sem novas bibliotecas. Reutilizar padrão de toast, modal e estado já existentes em `ListView.tsx`.
- **Não introduzir** nova biblioteca de UI ou estado para esta story.

### File Structure Requirements

Backend (arquivos a modificar):
- `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/MemberController.java` — adicionar método `removeMember`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/service/MemberService.java` — adicionar método `removeMember`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/service/MemberServiceTest.java` — adicionar testes de `removeMember`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/MemberControllerIntegrationTest.java` — adicionar testes de integração

Frontend (arquivos a modificar):
- `frontend/src/api/listsApi.ts` — adicionar `deleteListMember`
- `frontend/src/pages/ListView.tsx` — adicionar lógica de remoção
- `frontend/src/components/MembersModal.tsx` — adicionar UI de remoção
- `frontend/src/components/MembersModal.test.tsx` — adicionar testes de remoção

### Testing Requirements

- **Backend**: unit tests com Mockito para regras de negócio + integration tests com MockMvc para contrato HTTP.
- **Frontend**: component tests em `MembersModal.test.tsx` para visibilidade condicional e fluxo de confirmação.
- **Regressão obrigatória**: membro removido não deve continuar acessando dados da lista (verificar com GET /api/lists/{id} após remoção).
- Executar suites completas antes de marcar como done:
  - Backend: `./mvnw test`
  - Frontend: `npm test -- --run`

### Previous Story Intelligence (Story 4.6)

- `MembersModal.tsx` é **totalmente presentacional** (props-driven, sem estado próprio). Toda lógica de controle vive em `ListView.tsx` como estado elevado. Manter este padrão — adicionar novas props de remoção.
- `MemberService.leaveList` usa `findByListIdAndUserId` para encontrar o membership e depois `delete`. Seguir exatamente o mesmo padrão em `removeMember`.
- Story 4.6 consolidou verificação de ownership comparando `list.getOwner().getId()` com `requesterId`. Reutilizar exatamente essa verificação.
- Último commit de 4.6 (`feat(review): apply code review fixes...`) incluiu fixes em `ActivityTimeline`, `UserProfile/Profile` — atenção para não interferir com essas mudanças não documentadas formalmente.

### Git Intelligence Summary

- Padrão de commit: `feat(member): <descrição> (story 4.7)` — manter rastreabilidade.
- Arquivos recorrentes: `MemberController.java`, `MemberService.java`, `listsApi.ts`, `ListView.tsx`, `MembersModal.tsx` — exatamente os mesmos desta story.
- Ciclo observado: `feat` → code review fixes. Implementar já seguindo o padrão revisado para reduzir ciclos.
- Commits recentes incluem `feat(review): apply code review fixes and add new frontend features (story 4.6)` — verificar se há código não commitado relevante antes de começar.

### Latest Tech Information

- Spring Boot 4.x: `@DeleteMapping` com `@PathVariable` UUID segue o mesmo padrão já usado em `ListController` para `DELETE /api/lists/{id}`.
- Spring Security 7.x: `@AuthenticationPrincipal User` para obter usuário autenticado no controller (padrão já adotado).
- React 19: sem mudanças que afetam este fluxo de CRUD.

### References

- `_bmad-output/planning-artifacts/epics.md` — Story 4.7 (linha ~1107)
- `_bmad-output/planning-artifacts/architecture.md` — Seção de Sharing & Collaboration (FR23-30)
- `_bmad-output/implementation-artifacts/4-6-ver-membros-e-sair-da-lista.md` — story anterior com padrões estabelecidos
- `_bmad-output/implementation-artifacts/4-5-convidar-por-username.md` — padrões de ownership validation

## Project Context Reference

- `project-context.md` não encontrado no workspace durante a descoberta de insumos.
- Contexto de implementação derivado de epics, arquitetura, PRD e stories anteriores do Epic 4.

## Story Completion Status

- Status: `ready-for-dev`
- Completion note: `Ultimate context engine analysis completed - comprehensive developer guide created`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

Nenhum bloqueio crítico. Bug pré-existente detectado e corrigido: `MemberControllerIntegrationTest` usava `.with(user(...))` do Spring Security Test (que cria UserDetails simples) em vez do padrão correto do projeto com `SecurityContextHolder.getContext().setAuthentication()`. Corrigido para usar `authenticateUser(user)` consistente com `ListControllerIntegrationTest`.

### Completion Notes List

- Implementado `DELETE /api/lists/{listId}/members/{userId}` no backend (MemberController + MemberService)
- Implementado `deleteListMember` na listsApi.ts
- Atualizado MembersModal.tsx com 5 novas props para UI de remoção (botão lixeira + confirmação inline)
- Atualizado ListView.tsx com estado e handlers de remoção
- 4 novos testes unitários backend (MemberServiceTest), 5 novos testes de integração (MemberControllerIntegrationTest)
- 5 novos testes frontend (MembersModal.test.tsx)
- Corrigido bug pré-existente nos testes de integração de membros (autenticação via SecurityContextHolder)
- Suite backend: 301 testes, 0 falhas | Suite frontend: 112 testes, 0 falhas

### File List

- `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/MemberController.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/service/MemberService.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/service/MemberServiceTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/MemberControllerIntegrationTest.java`
- `frontend/src/api/listsApi.ts`
- `frontend/src/pages/ListView.tsx`
- `frontend/src/components/MembersModal.tsx`
- `frontend/src/components/MembersModal.test.tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/4-7-remover-participante.md`

### Change Log

- 2026-03-01: Story 4.7 criada com análise exaustiva de contexto. Cobre remoção de participante pelo OWNER via DELETE endpoint + UI de confirmação no painel de membros.
- 2026-03-01: Story 4.7 implementada. Backend: DELETE /api/lists/{listId}/members/{userId} com todas as regras de autorização. Frontend: botão lixeira + modal de confirmação inline no MembersModal. Todos os testes passando.
- 2026-03-01: Code review aplicado. Correções: (1) Criada MemberNotFoundException para substituir UserNotFoundForInviteException no contexto de remoção; (2) Adicionado handler no GlobalExceptionHandler; (3) Path variable `listId` → `id` no MemberController para consistência; (4) Removido estado redundante `removeConfirmMemberUsername` em ListView.tsx (username derivado de members); (5) Corrigido double-emoji 👥 no botão de Membros; (6) Substituído emoji 🗑️ por ícone SVG em MembersModal; (7) Adicionado teste unitário `shouldThrow404WhenListNotFoundInRemoveMember`; (8) Adicionado teste de integração `shouldRemoveMemberFromMembersListAfterDeletion` (regressão obrigatória). Suite: 303 testes backend, 112 frontend, 0 falhas.
