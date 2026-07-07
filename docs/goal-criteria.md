# Critérios de sucesso verificáveis (para `/goal` e agentes)

Lista canônica dos critérios que definem "CI verde / tarefa concluída" neste
repositório, no formato que um loop goal-based (ex.: `/goal` do Claude Code)
consegue verificar mecanicamente. Detalhes e justificativas dos gates:
[`quality-gate.md`](quality-gate.md) e [`../backend/QUALITY.md`](../backend/QUALITY.md).

## Critérios

1. **Quality gate local completo**: `./scripts/quality.sh --full` termina com
   exit 0. Em worktrees, use `bash scripts/quality/run-backend.sh` e
   `bash scripts/quality/run-frontend.sh` (os scripts não têm bit de exec).
2. **Diff coverage ≥ 80% nas linhas alteradas** (gate BLOQUEANTE do CI):
   `diff-cover` sobre `backend/target/site/jacoco/jacoco.xml` com
   `--compare-branch=origin/main --fail-under=80`.
3. **Cobertura sem regressão vs main** (backend e frontend): o CI compara
   contra a cobertura REAL da main (cache `base-*-coverage-<sha>`) — mais
   estrito que o `.quality-baseline` local. Frontend: `src/pages/**` e
   `src/main.tsx` são excluídos; lógica testável vive em `src/components/**`.
4. **Gate NVD / dependency-check verde**. Se falhar por cache frio (o guard
   avisa explicitamente): disparar `gh workflow run nvd-cache-warmer.yml`,
   aguardar ~15 min e re-executar o CI — **uma vez**. Nunca fazer override de
   merge sem autorização registrada no PR (e só se `pom.xml` não tiver diff).
5. **Contagem de testes reportada corretamente**: contar `<testcase>` nos XML —
   `grep -c '<testcase' backend/target/surefire-reports/*.xml`. Nunca usar a
   linha "Tests run" do console (classes `@Nested` reportam 0).
6. **Todos os checks do PR verdes**: `gh pr checks <nº> --watch` sem falhas
   (`frontend-quality`, `backend-quality`, `security-and-compliance`, e2e
   quando aplicável).

## Exemplo de uso com `/goal`

> /goal deixar o CI do PR #NN verde, respeitando docs/goal-criteria.md;
> máximo de 5 tentativas; nunca usar merge admin nem relaxar thresholds.

## Anti-critérios (nunca fazem parte do sucesso)

- Relaxar thresholds ou baselines para "passar".
- Override admin de gate sem autorização explícita registrada no PR.
- Marcar concluído com teste flaky conhecido sem re-execução
  (`UserControllerTest.shouldUpdateUpdatedAtAutomatically`).
