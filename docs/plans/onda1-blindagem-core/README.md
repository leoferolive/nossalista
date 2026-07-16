# Onda 1 — Blindar o core

Plano de execução derivado da avaliação total do NossaLista (5 agentes Opus 4.8).
Onda 1 = os itens de **maior risco de dados/robustez**, atacados primeiro.

> Escopo desta onda travado com o dono: **só Onda 1**, **auto-merge** após review,
> **tarefas agrupadas por localidade de arquivo** (arquivos disjuntos → worktrees
> paralelas sem conflito de merge). `backend/pom.xml` fica intocado nas três tarefas.

## Tarefas

| # | Tarefa | Arquivos (ownership) | Achado origem |
|---|--------|----------------------|---------------|
| [T1](./T1-lock-otimista.md) | Lock otimista em listas/itens | `listitem/domain/ListItem.java`, `list/domain/SharedList.java`, `common/exception/GlobalExceptionHandler.java`, nova migration, novo teste | P0-1 (concorrência) |
| [T2](./T2-persistir-push-subscriptions.md) | Persistir push subscriptions | `push/**`, nova migration, testes | P1 (push in-memory) |
| [T3](./T3-notificacoes-async-timeouts.md) | Notificações fora da transação + timeouts | `listitem/service/ListItemService.java`, `notification/**`, `push/PushNotificationService.java`, `email/service/SmtpEmailService.java`, `application.yml`, novo `config/AsyncConfig.java` | P1 (chamadas externas síncronas) |

### Garantia de não-conflito (mapa de arquivos por tarefa)

- **T1**: entidades (`ListItem`, `SharedList`), `GlobalExceptionHandler`, migration nova, teste novo. **Não** toca `ListItemService`.
- **T2**: pacote `push/**` — `PushSubscriptionStore` (internals), nova entity + repository, migration nova. **Não** toca `PushNotificationService`.
- **T3**: `ListItemService`, `notification/**`, `PushNotificationService`, `SmtpEmailService`, `application.yml`, `AsyncConfig` novo.

Interseção de arquivos entre tarefas: **vazia**. As migrations usam números de versão distintos (cada implementador confere o próximo `V*` livre em `db/migration/`).

## Pipeline de execução (por tarefa)

Ver a skill do fluxo em `~/.claude/skills/orchestrated-worktree-delivery/SKILL.md`.

1. Orquestrador (modelo principal) cria worktree + branch `feat/onda1-<slug>` a partir de `main`.
2. **Sonnet 5** implementa na worktree (TDD, `./scripts/quality.sh --pre-commit`, commit **sem** atribuição de IA), abre PR via `gh`.
3. **Opus 4.8** revisa o diff do PR → veredito estruturado.
4. **Orquestrador avalia**: revisão × implementado × pedido. Gap → devolve ao implementador → re-review.
5. Aprovado → **auto-merge na `main`** + remoção da worktree/branch.

## Fora do escopo desta onda (registrado, não executar agora)

- Testcontainers-PostgreSQL, WebSocket na cobertura, excludes do `vitest` (→ Onda 2).
- `@Retryable` no `add_items` para a race de `position` (exigiria tocar `ListItemService`,
  colidindo com T3 — vai para Onda 2 junto com Testcontainers).
- Gate de aprovação de prod: **removido de propósito** pelo dono → issue
  [#77](https://github.com/leoferolive/nossalista/issues/77) trata a evolução do CD.
