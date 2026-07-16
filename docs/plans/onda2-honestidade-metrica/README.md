# Onda 2 — Honestidade de métrica e robustez de teste

Segunda onda da avaliação total. Foco: fazer a cobertura contar o que hoje esconde,
testar contra o banco real e fechar a race de `position` deixada pela Onda 1.

> Pipeline: `~/.claude/skills/orchestrated-worktree-delivery/SKILL.md`.
> Escopo travado: **auto-merge**, **agrupar por localidade de arquivo**.

## Tarefas

| # | Tarefa | Arquivos (ownership) | Origem |
|---|--------|----------------------|--------|
| [T1](./T1-testcontainers.md) | Testcontainers-PostgreSQL | `backend/pom.xml` (deps), base class de IT, testes de repo/migration/MCP, resources de teste | P0-2 (H2 ≠ prod) |
| [T2](./T2-websocket-cobertura.md) | WebSocket na cobertura | `backend/pom.xml` (jacoco/pitest), testes de `websocket/**`, `.quality-baseline/backend.json` | P0-3 (camada oculta) |
| [T3](./T3-race-position.md) | Race de `position` (UNIQUE + retry) | `listitem/service/ListItemService.java`, migration `V18`, novo teste | P1 (add concorrente) |
| [T4](./T4-cobertura-frontend.md) | Honestidade de cobertura frontend | `frontend/vitest.config.ts`, `.quality-baseline/frontend.json`, `src/types/WebSocketMessage.test.ts`, novos testes | P1 (excludes inflados + contrato WS) |

## Regras de não-conflito (lições da Onda 1)

- **`pom.xml`**: tocado por T1 (seção de dependências) e T2 (plugin jacoco/pitest). Merge **serializado** T1 → T2 (T2 rebaseia sobre T1 antes do merge).
- **`.quality-baseline/backend.json`**: T2 (e T1 se necessário) → serializa junto com o pom.
- **`.quality-baseline/frontend.json`**: só T4.
- **Migration `V18`**: só T3 (o próximo número livre é 18; o `ls` não ordena numérico — confira o max real).
- **`ListItemService.java`**: só T3.
- **`docs/DECISIONS.md` e `docs/quality-gate*.md`**: **NENHUM agente edita.** O orquestrador consolida D-031 (Testcontainers) e D-032 (race de position) e a prosa do quality-gate num commit único após os merges — elimina o conflito de append que atrasou a Onda 1. Cada tarefa põe a justificativa da decisão no **corpo do PR**.

## Design opt-in do Testcontainers (T1)

T1 é **aditivo**: introduz uma base class (ex.: `AbstractPostgresIT`) com container singleton e `@ServiceConnection`, e migra para ela apenas os testes de repositório, a validação de migration e o `McpServerIntegrationTest`. **Não** troca o default H2 global — assim T2/T3 (e o resto da suíte) seguem em H2 e permanecem independentes de T1.

## Fora do escopo desta onda

- **e2e de colaboração A→B**: movido para o trabalho de CD (issue #77 — religar a suíte fullstack como gate de e2e críticos).
- Onda 3 (UX/ops) e Onda 4 (IA de produto): ver [[nossalista-avaliacao-roadmap]].
