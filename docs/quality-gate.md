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
- **Race conditions / concorrência** — não há análise estática confiável.
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
