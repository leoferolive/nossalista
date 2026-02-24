# Story 4.5: convidar-por-username

Status: review

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a dono de uma lista,
I want convidar usuários por username,
So that possa adicionar pessoas diretamente sem depender de link.

## Acceptance Criteria

**Given** endpoint POST /api/lists/{id}/invite disponível  
**When** request JWT válido, sou dono, body { "username": "pedro" }, usuário existe  
**Then** 201 Created, list_members criado (role = 'MEMBER'), Toast "{username} adicionado!"

**Given** endpoint invite  
**When** username não existe  
**Then** 404 Not Found "Usuário não encontrado"

**Given** endpoint invite  
**When** usuário já é membro  
**Then** 409 Conflict "Usuário já é membro"

**Given** InviteModal (dono)  
**When** aberto  
**Then** seção "Convidar por Username" com campo busca "Buscar usuário", botão "Convidar", autocomplete (GET /api/users/search)

**Given** campo busca, digito "leo"  
**Then** dropdown com resultados: avatar, username, name

**Given** resultado selecionado, toco "Convidar"  
**Then** POST invite enviado, Toast "{username} convidado!", modal fecha, membro aparece na lista

## Tasks / Subtasks

### Backend (Java/Spring)

- [x] Implementar endpoint `POST /api/lists/{id}/invite` em `MemberController` (AC: 1, 2, 3)
  - [x] Validar autenticação JWT e obter usuário autenticado
  - [x] Validar que solicitante é OWNER da lista
  - [x] Validar payload com `username` obrigatório
  - [x] Retornar 201/404/409 com RFC 7807 para erros

- [x] Implementar serviço de convite por username em `MemberService` (AC: 1, 2, 3)
  - [x] Buscar lista por `id` com controle de acesso
  - [x] Buscar usuário alvo por `username` (case-insensitive)
  - [x] Validar membro existente (idempotência rejeitada com 409)
  - [x] Criar `ListMember` com role `MEMBER` e timestamps de membership (`created_at`/`updated_at`)

- [x] Garantir regras de domínio e segurança (AC: 1, 2, 3)
  - [x] OWNER pode convidar, MEMBER não pode convidar
  - [x] OWNER não pode “convidar” a si mesmo (retornar 409 com mensagem clara)
  - [x] Erros padronizados em `ProblemDetail` (RFC 7807)

### Frontend (React/TypeScript)

- [x] Evoluir `InviteModal` para fluxo de convite por username (AC: 4, 5, 6)
  - [x] Campo de busca com debounce para `GET /api/users/search?q=...`
  - [x] Dropdown com avatar, username e name
  - [x] Seleção de usuário e habilitação do botão `Convidar`

- [x] Integrar chamada de API de convite por username (AC: 1, 6)
  - [x] Adicionar função em `frontend/src/api/listsApi.ts`
  - [x] Tratar estados de loading, erro e sucesso
  - [x] Atualizar lista de membros na UI após sucesso

- [x] UX e feedback consistentes (AC: 6)
  - [x] Toast de sucesso com username convidado
  - [x] Toast de erro para 404/409 com mensagem amigável
  - [x] Fechar modal no sucesso e preservar estado no erro

### Testing

- [x] Testes unitários backend para serviço de convite
  - [x] convite bem-sucedido (201)
  - [x] usuário inexistente (404)
  - [x] usuário já membro (409)
  - [x] solicitante não OWNER (403)

- [x] Testes de integração backend para endpoint `POST /api/lists/{id}/invite`
  - [x] fluxo completo com JWT válido e OWNER
  - [x] 401 sem token
  - [x] 403 para MEMBER tentando convidar

- [x] Testes frontend (component + integração)
  - [x] autocomplete renderiza resultados corretamente
  - [x] clique em convidar envia request correto
  - [x] erros 404/409 exibem feedback adequado

## Dev Notes

### Contexto da story

- Esta story é a continuação direta da Story 4.4: agora que o ingresso por link autenticado está pronto, falta o fluxo de convite direto por username para reduzir fricção do dono.
- Requisitos de negócio principais do Epic 4: OWNER gerencia participação; MEMBER colabora em itens, mas não administra acesso.

### Reuso obrigatório (evitar reinventar)

- Reusar entidades e repositórios já existentes de membresia (`ListMember`, `ListMemberRepository`) ao invés de criar novas estruturas.
- Reusar endpoint de busca de usuários já existente (`GET /api/users/search`) para autocomplete no modal.
- Reusar padrões de erro já adotados (RFC 7807 em `GlobalExceptionHandler`).

### Requisitos técnicos

- API deve seguir padrão REST do projeto: `/api/lists/{id}/invite`.
- Campos JSON em snake_case no contrato de API.
- Datas em ISO 8601 UTC nas respostas quando aplicável.
- Controle de autorização no backend: somente OWNER pode convidar.

### Compliance de arquitetura

- Backend em estrutura por domínio (`member/`, `user/`, `list/`) sem criar pacotes fora do padrão.
- Frontend em organização por tipo (`components/`, `api/`, `types/`, `hooks/`).
- Tratamento de erro frontend: log técnico + mensagem amigável ao usuário.

### Library/Framework requirements

- Manter stack definida no projeto: Spring Boot 4.x, Spring Security 7.x, React 19.x.
- Sem introduzir bibliotecas novas para autocomplete neste escopo; usar o padrão já existente de hooks + client API.

### File structure requirements

- Backend (esperado):
  - `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/MemberController.java`
  - `backend/src/main/java/br/com/leoferolive/nossalista/member/service/MemberService.java`
  - `backend/src/main/java/br/com/leoferolive/nossalista/member/repository/ListMemberRepository.java`
  - `backend/src/main/java/br/com/leoferolive/nossalista/user/repository/UserRepository.java`
- Frontend (esperado):
  - `frontend/src/components/InviteModal.tsx`
  - `frontend/src/api/listsApi.ts`
  - `frontend/src/types/List.ts`
  - `frontend/src/pages/ListView.tsx`

### Testing requirements

- Backend: priorizar unit tests de regra de negócio + integration tests de contrato HTTP.
- Frontend: validar seleção/autocomplete, request de convite e feedback visual.
- Cobrir cenários de regressão: convite duplicado, usuário inexistente, convite por não OWNER.

## Previous Story Intelligence (4.4)

- Padrão de robustez da Story 4.4 deve ser mantido: validação de segurança explícita, idempotência tratada com cuidado e mensagens claras.
- Em 4.4 houve correção importante de semântica HTTP (201 vs 200): aqui manter semântica consistente para criação de membership (201 no sucesso de criação).
- Limpeza de estado e mensagens de feedback no frontend foram pontos críticos em 4.4; não regressar nesses pontos no InviteModal.

## Git Intelligence Summary

- Commits recentes mostram concentração no domínio `member` e reforço pós code-review (`feat(member): ... with code review fixes`).
- Arquivos mais tocados recentemente para este fluxo: `ListJoinController`, `ListJoinService`, `SecurityConfig`, `InviteModal`, `listsApi.ts`, `ListView.tsx`.
- Padrão de commit recente: `feat({scope}): ... (story X.Y)`; manter consistência quando for commitar esta story.

## Latest Tech Information

- Spring Boot: página oficial indica série 4.0.x atual (4.0.3); projeto usa 4.0.2 e pode seguir nessa série sem mudança estrutural para esta story.
- Spring Security: versão atual 7.0.x (7.0.3), alinhada com abordagem de segurança por configuração declarativa e filtros já adotada no projeto.
- React: série 19.x continua recomendada; há alertas recentes de segurança relacionados a React Server Components, não impactando diretamente este fluxo (SPA cliente + API REST), mas vale manter dependências atualizadas no ciclo normal.

## Project Context Reference

- Não foi encontrado `project-context.md` no workspace.
- Fontes usadas para contexto desta story:
  - `_bmad-output/planning-artifacts/epics.md`
  - `_bmad-output/planning-artifacts/architecture.md`
  - `_bmad-output/planning-artifacts/prd.md`
  - `_bmad-output/planning-artifacts/ux-design-specification.md`
  - `_bmad-output/implementation-artifacts/4-4-entrar-na-lista-autenticado.md`

## Story Completion Status

- Status final definido para `review`.
- Implementação concluída com validação de testes backend/frontend.

## Dev Agent Record

### Agent Model Used

openai/gpt-5.3-codex

### Debug Log References

- Sprint status analisado para seleção automática da primeira story em backlog.
- Artefatos PRD/Arquitetura/UX/Epics analisados integralmente para extração de contexto.

### Implementation Plan

- Implementação backend focada em `POST /api/lists/{id}/invite` com validações de ownership, resolução case-insensitive de username e conflitos de convite via exceções específicas mapeadas para RFC 7807.
- Implementação frontend focada em evolução do `InviteModal` com autocomplete debounce, seleção de usuário e fluxo de convite com feedback de erro/sucesso sem regressão no convite por link.
- Cobertura de testes adicionada em backend (unit + integração) e frontend (component) com execução completa dos testes do projeto.

### Completion Notes List

- Endpoint `POST /api/lists/{id}/invite` implementado com retorno `201 Created` e payload de confirmação (`invited_username`, `message`).
- Regras de domínio aplicadas: apenas OWNER convida, auto-convite bloqueado (409), usuário inexistente (404), usuário já membro (409).
- `InviteModal` evoluído com seção "Convidar por Username", busca com debounce e dropdown de autocomplete com avatar/username/nome.
- `listsApi` ampliada com `searchUsers` e `inviteByUsername` com tratamento de erros 401/403/404/409.
- Testes adicionados: `MemberServiceTest`, `MemberControllerIntegrationTest`, `InviteModal.test.tsx`.
- Validações executadas: `./mvnw test`, `npm test -- --run`, `npm run build`.
- Pendência de ambiente: `npm run lint` falha por dependência ausente `typescript-eslint` no `eslint.config.js`.

### File List

- `_bmad-output/implementation-artifacts/4-5-convidar-por-username.md`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/MemberController.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/service/MemberService.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/dto/InviteByUsernameRequest.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/dto/InviteByUsernameResponse.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/exception/MemberInvitationConflictException.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/exception/UserNotFoundForInviteException.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/user/repository/UserRepository.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/service/MemberServiceTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/MemberControllerIntegrationTest.java`
- `frontend/src/components/InviteModal.tsx`
- `frontend/src/components/InviteModal.test.tsx`
- `frontend/src/api/listsApi.ts`
- `frontend/src/pages/ListView.tsx`
- `frontend/src/types/List.ts`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

## Change Log

- 2026-02-23: Implementado convite por username no backend e frontend, com testes unitários/integração/componentes e atualização de status da story para review.
