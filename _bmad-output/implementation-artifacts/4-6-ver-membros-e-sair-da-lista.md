# Story 4.6: ver-membros-e-sair-da-lista

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a participante de uma lista,
I want ver quem sao os membros e poder sair da lista,
So that eu tenha autonomia sobre minha participacao.

## Acceptance Criteria

**Given** endpoint GET /api/lists/{id}/members disponivel  
**When** request JWT valido, sou membro  
**Then** 200 OK com array de membros: user { id, username, name, avatar_url }, role, joined_at  
**And** ordenado: OWNER primeiro, depois MEMBERs por joined_at ASC

**Given** endpoint POST /api/lists/{id}/leave disponivel  
**When** request JWT valido, sou MEMBER  
**Then** 204 No Content, registro list_members removido, nao acesso mais a lista

**Given** endpoint leave  
**When** sou OWNER  
**Then** 403 Forbidden "O dono nao pode sair. Transfira ou exclua a lista."

**Given** ListView  
**When** carregada  
**Then** header tem botao "Membros" com contador "👥 3", ao tocar abre painel/modal com lista

**Given** painel membros aberto  
**When** renderizado  
**Then** cada membro: avatar, username, name, badge "Dono"/"Membro", dono primeiro

**Given** painel membros, sou MEMBER  
**When** visivel  
**Then** botao "Sair da Lista" visivel (cor alerta)

**Given** painel membros, sou OWNER  
**When** visivel  
**Then** botao "Sair" NAO aparece, aviso "Voce e o dono"

**Given** toco "Sair da Lista"  
**Then** modal confirmacao: "Sair da lista? Voce perdera acesso.", botoes "Cancelar"/"Sair"  
**And** ao confirmar: POST leave, Toast "Voce saiu", redireciona para Home, lista some da Home

## Tasks / Subtasks

### Backend (Java/Spring)

- [x] Implementar `GET /api/lists/{id}/members` em `MemberController` (AC: 1, 4, 5)
  - [x] Validar JWT e participacao na lista (OWNER ou MEMBER)
  - [x] Retornar membros com shape de contrato (`user`, `role`, `joined_at`)
  - [x] Garantir ordenacao OWNER primeiro e MEMBER por `joined_at ASC`
- [x] Implementar `POST /api/lists/{id}/leave` em `MemberController` (AC: 2, 3)
  - [x] Permitir saida apenas para role MEMBER
  - [x] Bloquear OWNER com 403 e mensagem definida
  - [x] Remover membership e retornar 204 sem payload
- [x] Consolidar regras no `MemberService` (AC: 1, 2, 3)
  - [x] Reusar verificacoes de ownership/membership ja existentes
  - [x] Garantir que usuario removido perca acesso imediato a endpoints da lista
  - [x] Preparar gancho para activity log (Epic 6) sem bloquear esta entrega

### Frontend (React/TypeScript)

- [x] Adicionar acao de abrir painel/modal de membros na `ListView` (AC: 4, 5)
  - [x] Exibir contador de membros no header (`👥 N`)
  - [x] Listar membros com avatar, username, name, badge de role
  - [x] Respeitar ordenacao recebida da API
- [x] Implementar CTA de saida da lista para MEMBER (AC: 6, 8)
  - [x] Mostrar botao de alerta apenas para MEMBER
  - [x] Exibir modal de confirmacao antes do POST leave
  - [x] Ao sucesso: toast, fechar modal e redirecionar para Home
- [x] Tratar estado OWNER no painel (AC: 7)
  - [x] Ocultar botao de saida para OWNER
  - [x] Exibir aviso explicito "Voce e o dono"

### Testing

- [x] Backend unit tests em `MemberService` para listagem e leave
  - [x] MEMBER sai com 204 e membership removido
  - [x] OWNER recebe 403 ao tentar sair
  - [x] Nao membro nao consegue listar membros
- [x] Backend integration tests em `MemberController`
  - [x] `GET /members` com ordenacao correta e contrato esperado
  - [x] `POST /leave` sucesso para MEMBER e 403 para OWNER
  - [x] 401 para requests sem token
- [x] Frontend tests (component/integration)
  - [x] Renderizacao do painel com membros e badges
  - [x] Visibilidade condicional do botao "Sair da Lista"
  - [x] Fluxo de confirmacao + redirecionamento apos leave

## Dev Notes

### Contexto da story

- Esta story fecha o ciclo de colaboracao do Epic 4 no lado do participante: visibilidade de quem esta na lista e autonomia para sair sem acao do dono.
- Dependencia direta de fluxos ja entregues: entrada por link autenticado (Story 4.4) e convite por username (Story 4.5), que alimentam `list_members`.

### Developer Context Section

- Nao reinventar contratos de membresia: reusar DTOs, repositorios e regras de `member/` ao inves de criar novo modulo paralelo.
- O endpoint de membros e de leitura autenticada; o endpoint de leave altera estado e deve seguir semantica REST (204 sem corpo).
- UX deve manter transparencia e simplicidade: painel enxuto, estado de role claro, confirmacao antes de sair e feedback imediato.

### Technical Requirements

- Contratos REST:
  - `GET /api/lists/{id}/members` retorna array com `user`, `role`, `joined_at`.
  - `POST /api/lists/{id}/leave` retorna `204 No Content` para MEMBER.
- Regras de autorizacao:
  - Apenas membro (OWNER/MEMBER) lista membros.
  - Apenas MEMBER pode sair; OWNER recebe `403 Forbidden`.
- Regras de ordenacao:
  - OWNER primeiro.
  - MEMBERs por `joined_at ASC`.
- Erros padronizados em RFC 7807 com mensagens amigaveis e consistentes.

### Architecture Compliance

- Manter backend em estrutura feature-based existente: `member/controller`, `member/service`, `member/repository`, `member/dto`.
- Manter frontend em estrutura por tipo: `pages`, `components`, `api`, `types`.
- Seguir padroes globais:
  - JSON em `snake_case`
  - datas ISO 8601 UTC
  - erro RFC 7807

### Library Framework Requirements

- Spring Boot 4.x e Spring Security 7.x (stack ja adotada no projeto).
- React 19 + TypeScript + Vite no frontend.
- Nao introduzir nova biblioteca de estado/modal/toast para esta story; reutilizar padrao ja existente.

### File Structure Requirements

- Backend (alvos esperados):
  - `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/MemberController.java`
  - `backend/src/main/java/br/com/leoferolive/nossalista/member/service/MemberService.java`
  - `backend/src/main/java/br/com/leoferolive/nossalista/member/repository/ListMemberRepository.java`
  - `backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java`
- Frontend (alvos esperados):
  - `frontend/src/pages/ListView.tsx`
  - `frontend/src/components/InviteModal.tsx` (se painel de membros permanecer integrado ao modal atual)
  - `frontend/src/api/listsApi.ts`
  - `frontend/src/types/List.ts`

### Testing Requirements

- Backend: combinar unit tests de regra de dominio com integration tests de contrato HTTP.
- Frontend: validar controle por role (OWNER vs MEMBER), confirmacao de saida e navegacao pos-leave.
- Regressao obrigatoria: usuario removido/que saiu nao deve continuar acessando dados da lista.

### Previous Story Intelligence

- Story 4.5 consolidou padrao de ownership no `MemberService`; reutilizar a mesma estrategia de validacao para evitar divergencia de regra.
- Story 4.4 reforcou semantica de status HTTP (201/200 em join) e limpeza de estado no frontend; aqui manter consistencia com `204` em leave e UI sem estado residual apos redirecionamento.
- Ultimas stories concentraram mudancas em `MemberController`, `MemberService`, `listsApi.ts`, `ListView.tsx` e `InviteModal.tsx`; priorizar continuidade nesses pontos.

### Git Intelligence Summary

- Ultimos titulos: `feat(member)` dominando as stories 4.3, 4.4 e 4.5, com foco em convite/join/membership.
- Arquivos recorrentes nos commits relevantes: `MemberController`, `MemberService`, `ListJoinService`, `listsApi.ts`, `ListView.tsx`, `InviteModal.tsx`, `GlobalExceptionHandler`.
- Padrao recente de commit: `feat(member): ... (story X.Y)` com ciclo posterior de review fixes; seguir o mesmo padrao para rastreabilidade.

### Latest Tech Information

- Spring Boot pagina oficial indica serie atual `4.0.3`; projeto em `4.0.2` permanece alinhado para esta story.
- Spring Security pagina oficial indica `7.0.3`; sem necessidade de mudanca estrutural para implementar members/leave.
- React releases publicos apontam `19.2.4` como latest; principal nota recente destaca hardening de React Server Components (impacto baixo no fluxo SPA desta story).
- STOMP JS release `v7.3.0` e SockJS Client `1.6.1` permanecem referencias atuais para fase real-time (Epic 5), sem bloqueio para esta story.

### References

- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/planning-artifacts/architecture.md`
- `_bmad-output/planning-artifacts/prd.md`
- `_bmad-output/planning-artifacts/ux-design-specification.md`
- `_bmad-output/implementation-artifacts/4-5-convidar-por-username.md`
- `_bmad-output/implementation-artifacts/4-4-entrar-na-lista-autenticado.md`

## Project Context Reference

- `project-context.md` nao encontrado no workspace durante a descoberta de insumos.
- Contexto de implementacao derivado de epics, arquitetura, PRD, UX e stories anteriores do Epic 4.

## Story Completion Status

- Status final da story: `done`.
- Completion note: `Ultimate context engine analysis completed - comprehensive developer guide created`.

## Senior Developer Review (AI)

### Outcome

Approve

### Findings Resolution

- Corrigido AC de UI no header: botao de membros com contador movido para o header da `ListView`.
- Corrigido gap de testes unitarios backend: adicionado caso "Nao membro nao consegue listar membros" em `MemberServiceTest`.
- Corrigido gap de testes de integracao backend: adicionado caso `401` para `POST /api/lists/{id}/leave` sem autenticacao.
- Corrigido gap de testes frontend: adicionado teste de fluxo completo de confirmacao e redirecionamento apos leave.
- Melhorada robustez pos-leave: navegacao para Home agora envia estado para refetch e feedback consistente.
- Melhorada confiabilidade visual do contador de membros: fallback agora evita exibir `0` enganoso em erro de carregamento.
- Ajustado copy do estado OWNER para consistencia: "Voce e o dono".

### Review Notes

- ACs revisados contra codigo e testes: implementados.
- Tasks marcadas como concluidas e auditadas: alinhadas apos os ajustes.
- Sem pendencias HIGH/MEDIUM apos os fixes.

## Dev Agent Record

### Agent Model Used

openai/gpt-5.3-codex

### Debug Log References

- Selecao automatica da primeira story em backlog no `sprint-status.yaml`.
- Descoberta e leitura completa de artefatos de planejamento e da story anterior do epic.
- Coleta de inteligencia de git para padroes de implementacao recentes.
- Pesquisa externa de versoes estaveis das tecnologias criticas.
- Implementacao dos endpoints `GET /members` e `POST /leave` com validacao por membership no `MemberService`.
- Ajuste de autorizacao em `ListService.getListById` para owner ou membro.
- Implementacao do painel de membros na `ListView` com CTA condicional de saida e confirmacao.
- Execucao de suites: `./mvnw test` (backend) e `npm test -- --run` (frontend).

### Completion Notes List

- Endpoint `GET /api/lists/{id}/members` entregue com contrato `user/role/joined_at` em snake_case e ordenacao OWNER primeiro.
- Endpoint `POST /api/lists/{id}/leave` entregue com 204 para MEMBER e 403 para OWNER com mensagem definida na AC.
- `MemberService` consolidou regras de membership para listagem/leave e reuso para verificacao de acesso.
- `ListView` ganhou botao `Membros`, contador `👥 N`, painel de membros e fluxo de sair da lista com confirmacao e toast de sucesso.
- Testes backend e frontend atualizados e executados com sucesso; lint frontend indisponivel por dependencia ausente (`typescript-eslint` no `eslint.config.js`).
- 2026-02-24: Review adversarial aplicada com fixes em AC de header, gaps de testes (backend/frontend), fluxo pos-leave e feedback de contador.

### File List

- `_bmad-output/implementation-artifacts/4-6-ver-membros-e-sair-da-lista.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/controller/MemberController.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/service/MemberService.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/repository/ListMemberRepository.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/member/dto/ListMemberResponse.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/list/service/ListService.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/service/MemberServiceTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/member/controller/MemberControllerIntegrationTest.java`
- `frontend/src/types/List.ts`
- `frontend/src/api/listsApi.ts`
- `frontend/src/pages/ListView.tsx`
- `frontend/src/pages/Home.tsx`
- `frontend/src/pages/ListView.test.tsx`

### Change Log

- 2026-02-24: Implementada Story 4.6 com endpoints de membros/leave, painel de membros no frontend e cobertura de testes backend/frontend.
- 2026-02-24: Code review adversarial concluido; corrigidos issues HIGH/MEDIUM e story promovida para `done`.
