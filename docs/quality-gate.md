# Quality Gate

Gate local unificado que mede o código por métricas objetivas, falhando o build se alguma regredir. Inspirado em "Pare de ler código de IA, comece a medi-lo" (Codeminer42).

## Como rodar

    ./scripts/quality.sh --full              # roda tudo + ratchet
    ./scripts/quality.sh --pre-commit        # subset rápido (lint + types + static)
    ./scripts/quality.sh --full --update-baseline   # promove métricas atuais a piso

## Dimensões medidas

| Camada   | Dimensão                  | Ferramenta             |
|----------|---------------------------|------------------------|
| Backend  | Cobertura linha/branch    | JaCoCo                 |
| Backend  | Complexidade ciclomática  | PMD `CyclomaticComplexity` |
| Backend  | Complexidade cognitiva    | PMD `CognitiveComplexity` |
| Backend  | NPath                     | PMD `NPathComplexity`  |
| Backend  | Tamanho de método/classe  | PMD `NcssCount` (substitui `ExcessiveMethodLength`/`ExcessiveClassLength` no PMD 7) |
| Backend  | Estilo                    | Checkstyle             |
| Backend  | Bugs estáticos            | SpotBugs (High)        |
| Backend  | Arquitetura               | ArchUnit (já existente)|
| Frontend | Cobertura linha/branch/fn | Vitest v8              |
| Frontend | Complexidade ciclomática  | ESLint `complexity`    |
| Frontend | Tamanho de função/arquivo | ESLint `max-lines-per-function`, `max-lines` |
| Frontend | Tipos                     | tsc strict             |
| Frontend | Lint                      | ESLint flat config     |

### Escopo de cobertura (ampliado na Onda 2 — honestidade de métrica)

A cobertura reportada agora conta código que antes era excluído da métrica:

- **Backend:** a camada `websocket/**` (publisher, interceptors, controllers, scheduler,
  `PresenceService`) entrou no JaCoCo/Pitest — só DTOs/records triviais (`websocket/dto/**`,
  `WebSocketActor`) seguem fora. Cobertura backend passou de ~86%/77% para **93%/84%**
  (linha/branch) com a camada real-time incluída. Ver D-031 (Testcontainers) e o PR #81.
- **Frontend:** `src/api/**` e `src/pages/**` saíram do `exclude` do `vitest.config.ts`
  (já tinham testes, mas eram invisíveis ao ratchet). O baseline caiu de forma **honesta**
  (o denominador cresceu, o gate de 80% não foi afrouxado). O que continua excluído
  (entrypoints, tipos puros, hooks/componentes ainda sem teste) está marcado como dívida
  no próprio `vitest.config.ts`. Ver PR #82.
- **Testes sensíveis ao banco** rodam contra **PostgreSQL real** via Testcontainers (opt-in
  `AbstractPostgresIT`), não mais só H2 — ver D-031.

## Thresholds atuais

Ver tabela completa em `docs/superpowers/plans/2026-05-11-quality-gate.md`, seção "Thresholds escolhidos".

Resumo:

- **Backend**: ciclomática <= 10, cognitiva <= 15, NPath <= 200, NcssCount método <= 40 / classe <= 250.
- **Frontend**: ciclomática <= 10, max-lines-per-function <= 60, max-lines <= 400, max-depth <= 4, max-params <= 5.

## Política de ratchet

- Primeira execução grava `.quality-baseline/*.json`.
- Execuções seguintes só passam se nenhuma métrica regredir.
- Promover novo piso é um commit explícito: `./scripts/quality.sh --full --update-baseline && git commit`.

## Limitações reconhecidas

O gate **NÃO** mede:

- **Segurança runtime** (SQLi, XSS, auth bypass) — coberto parcialmente por Semgrep no CI.
- **Performance** (latência, throughput, N+1 queries) — exige observabilidade.
- **Race conditions / concorrência** — não há análise estática confiável. (A Onda 2 fechou
  dois casos concretos com guardas estruturais: lock otimista `@Version` (D-029) e
  `UNIQUE(list_id, position)` + retry (D-032), ambos com testes de concorrência multi-thread
  rodando contra Postgres real.)
- **Memory leaks** — exige profiling.
- **Intenção da feature** — só revisão humana garante que o código faz o que o ticket pediu.
- **Qualidade dos testes** — só mede cobertura, não se o teste afirma algo útil.

> **Princípio:** o gate mecaniza o que pode ser mecanizado. A revisão humana fica livre para focar no que máquinas não fazem.

## Dívida técnica

Violações pré-existentes (> 5 por dimensão) estão documentadas em
[`docs/quality-gate-debt.md`](./quality-gate-debt.md), com prazos de refatoração.
Os arquivos da dívida são excluídos do ruleset bloqueante via:

- **Backend (PMD):** `<exclude-pattern>` em `backend/pmd/ruleset.xml`.
- **Frontend (ESLint):** bloco de override com `rules: 'off'` no final de `frontend/eslint.config.js`.

## FAQ

**Por que ciclomática <= 10 e não <= 6 como no post Rails?**
Java tem mais boilerplate (declarações de tipo, exceções verificadas). 10 é o padrão McCabe original e o default do SonarSource. 6 produziria ruído sem ganho de qualidade.

**Por que não mutation testing?**
Custo alto (Pitest demora minutos por módulo) e ROI baixo sem CI dedicado. Pode entrar em iteração futura.

**Por que `NcssCount` em vez de `ExcessiveMethodLength`/`ExcessiveClassLength`?**
O PMD 7 removeu essas duas regras e as unificou em `NcssCount`, que conta
statements (não linhas brutas). Os thresholds (`methodReportLevel=40`,
`classReportLevel=250`) foram calibrados para o equivalente do plano original.

**Onde roda o scan de vulnerabilidades de dependências (SCA)?**
No CI, via **OSV-Scanner** (`.github/workflows/osv-scanner.yml`), que consulta a
base agregada do OSV.dev e cobre backend (Maven) e frontend (npm) num único passo.
O antigo OWASP Dependency-Check/NVD foi removido — exigia `NVD_API_KEY` e baixar a
base inteira da NVD, o que causava falhas espúrias de "cache frio" no CI. Ver
**D-027** em `docs/DECISIONS.md` e a issue #70. O run local (`./scripts/quality.sh`)
não faz SCA.

Além do OSV-Scanner, o job `security-and-compliance` do CI roda `npm audit` no frontend
("Frontend audit"). Na Onda 2 esse gate acusou uma vuln **crítica** pré-existente em
`websocket-driver` (transitiva via `sockjs-client → faye-websocket`,
GHSA-mp7j-qc5w-4988) — corrigida com bump não-breaking de lockfile
(`websocket-driver` 0.7.4 → 0.7.5, `npm audit fix` sem `--force`, PR #82).
