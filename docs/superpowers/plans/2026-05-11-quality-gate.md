# Plano de Implementação — Quality Gate Local Unificado

**Data:** 2026-05-11
**Branch:** `quality-gate-plan`
**Worktree:** `/home/leoferolive/projetos/nossalista-wt-quality-gate`
**Autor do plano:** agente independente, sob orientação do usuário

---

## Goal

Instalar um *quality gate* local unificado no monorepo NossaLista (Java 25 + Spring Boot 4 no backend, React 19 + TS no frontend) inspirado no post "Pare de ler código de IA, comece a medi-lo" da Codeminer42. O gate deve:

- Ser invocável por um único comando (`./scripts/quality.sh`).
- Imprimir uma tabela `✓ / ✗` com cada dimensão medida.
- Falhar (exit code não-zero) quando qualquer dimensão regredir.
- Seguir padrão *ratchet*: a primeira execução produz a *baseline*; execuções futuras só podem manter ou melhorar.
- Servir como **subset rápido** do CI (alvo: rodar em ≤ 30 s para o subset `pre-commit`, ≤ 5 min para o `full`).
- Ser executável manualmente pelo agente Claude antes de qualquer commit (instruído no `CLAUDE.md`).

**Não-objetivos** (limitações reconhecidas, ver seção final):

- Não cobre segurança runtime, performance, race conditions, memory leaks, correção da feature.
- Não inclui *mutation testing* nesta iteração (custo alto, ROI baixo sem CI dedicado).
- Não substitui a revisão humana de PRs — substitui apenas a *parte mecânica* dela.

---

## Architecture

```
nossalista/
├── scripts/
│   ├── quality.sh                  ← orquestrador único (NOVO)
│   ├── quality/
│   │   ├── run-backend.sh          ← Maven + extração de métricas (NOVO)
│   │   ├── run-frontend.sh         ← npm lint+typecheck+test (NOVO)
│   │   ├── render-table.sh         ← formata saída ✓/✗ (NOVO)
│   │   └── ratchet.py              ← compara métricas vs baseline (NOVO)
│   └── coverage/                    ← scripts Python existentes (REUSAR)
│       ├── compare_backend_coverage.py
│       └── compare_frontend_coverage.py
├── .quality-baseline/               ← baseline ratchet versionada (NOVO, no git)
│   ├── backend.json
│   └── frontend.json
├── .husky/                          ← hooks de pre-commit (NOVO)
│   └── pre-commit
├── .github/
│   └── dependabot.yml               ← (NOVO)
├── backend/pmd/ruleset.xml          ← MODIFICAR (adicionar complexidade)
├── frontend/eslint.config.js        ← MODIFICAR (adicionar complexity)
├── frontend/package.json            ← MODIFICAR (husky + lint-staged + script quality)
├── docs/quality-gate.md             ← documentação (NOVO)
├── CLAUDE.md                        ← MODIFICAR (instruir agente)
└── README.md                        ← MODIFICAR (seção quality)
```

**Fluxo de execução do `./scripts/quality.sh`:**

```
quality.sh
  ├── parse args: --pre-commit | --full | --update-baseline
  ├── invoca run-backend.sh
  │     └── ./mvnw -P strict-quality verify -DskipITs (subset rápido)
  │     └── lê target/jacoco-report/jacoco.xml
  │     └── parseia PMD report (CyclomaticComplexity, NPath, etc.)
  │     └── emite backend.metrics.json
  ├── invoca run-frontend.sh
  │     └── npm run lint
  │     └── npm run typecheck
  │     └── npm run test:coverage
  │     └── parseia ESLint JSON report + coverage-summary.json
  │     └── emite frontend.metrics.json
  ├── invoca ratchet.py com baseline + métricas atuais
  ├── render-table.sh imprime tabela ✓/✗
  └── exit 0 (tudo ok) / exit 1 (regressão)
```

---

## Tech Stack

| Camada | Ferramenta | Versão atual | Uso no gate |
|---|---|---|---|
| Backend test runner | Maven + Surefire | já presente | `verify` |
| Backend coverage | JaCoCo 0.8.14 | já em `pom.xml:287` | extrair `target/jacoco-report/jacoco.xml` |
| Backend lint | Checkstyle 3.6.0 | já em `pom.xml:186` | exit code |
| Backend complexity | PMD 3.28.0 | já presente, mas regra única | **adicionar regras** (`CyclomaticComplexity`, `NPathComplexity`, `CognitiveComplexity`, `ExcessiveMethodLength`, `ExcessiveClassLength`) |
| Backend bugs | SpotBugs 4.9.8.1 | já em `pom.xml:230`, threshold High | exit code |
| Frontend lint | ESLint 9 (flat config) | já em `frontend/eslint.config.js` | **adicionar `complexity`, `max-lines-per-function`, `max-lines`** |
| Frontend types | tsc strict | já em `tsconfig.json:18` | `npm run typecheck` |
| Frontend coverage | Vitest v8 80%/80% | já em `vitest.config.ts:37-42` | `coverage-summary.json` |
| Pre-commit | Husky 9 + lint-staged 15 | **AUSENTE — instalar** | dispara subset rápido |
| Dependabot | n/a | **AUSENTE — criar** | maven + npm + github-actions |
| Ratchet | Python 3 (já usado em `scripts/coverage/`) | já presente | estender pattern existente |
| Tabela | bash + cores ANSI | n/a | renderizador puro shell |

**Decisão de empacotamento:** **shell + python**, sem `Makefile`. Justificativa: já há precedente Python em `scripts/coverage/`; um `Makefile` adicionaria uma terceira linguagem (bash + python + make) para pouco ganho. Um único entry-point `./scripts/quality.sh` é mais discoverable.

**Decisão de profile Maven:** **reusar `strict-quality`** existente (não criar `quality-local`). O gate local é definido por *qual subset* o shell roda, não por um profile separado. Evita drift entre `pom.xml` profiles.

---

## File Structure

### Arquivos a CRIAR

| Caminho | Propósito |
|---|---|
| `scripts/quality.sh` | Orquestrador principal, parseia flags, chama sub-scripts |
| `scripts/quality/run-backend.sh` | Roda Maven e extrai métricas para JSON |
| `scripts/quality/run-frontend.sh` | Roda npm scripts e extrai métricas para JSON |
| `scripts/quality/render-table.sh` | Renderiza tabela ✓/✗ com cores ANSI |
| `scripts/quality/ratchet.py` | Compara métricas atuais vs `.quality-baseline/*.json` |
| `.quality-baseline/backend.json` | Baseline backend (committada após primeiro run) |
| `.quality-baseline/frontend.json` | Baseline frontend (committada após primeiro run) |
| `.quality-baseline/README.md` | Explica o que é, como atualizar, quando ratchet sobe |
| `.husky/pre-commit` | Hook que chama `./scripts/quality.sh --pre-commit` |
| `.github/dependabot.yml` | Configuração Dependabot |
| `docs/quality-gate.md` | Documentação canônica do gate |

### Arquivos a MODIFICAR

| Caminho | Mudança |
|---|---|
| `backend/pmd/ruleset.xml` | Adicionar regras de complexidade |
| `frontend/eslint.config.js` | Adicionar `complexity`, `max-lines-per-function`, `max-lines` |
| `frontend/package.json` | Adicionar deps `husky`, `lint-staged`; script `prepare`; bloco `lint-staged` |
| `package.json` (raiz, se existir; senão criar mínimo) | Apenas para hospedar `prepare: husky` no monorepo |
| `CLAUDE.md` | Seção "Quality Gate" instruindo agente a rodar antes de commitar |
| `README.md` | Linkar `docs/quality-gate.md` na seção apropriada |
| `backend/QUALITY.md` | Anexar referência ao gate unificado |
| `.gitignore` | Garantir que `target/`, `coverage/`, `node_modules/` continuam ignorados; **NÃO** ignorar `.quality-baseline/` |

### Arquivos a NÃO TOCAR nesta iteração

- `.github/workflows/ci.yml` — o gate local é subset do CI, mas o CI já é maduro. Modificá-lo é escopo separado.
- `scripts/coverage/compare_*_coverage.py` — pattern reusado pelo `ratchet.py`, não mexer no código existente.

---

## Thresholds escolhidos (com justificativa)

> **Princípio:** thresholds iniciais devem ser *atingíveis hoje* pelo código existente. O ratchet sobe daí. Adotar números do post Rails cegamente ("≤ 6 ciclomática") quebraria o build no primeiro commit.

### Backend (Java)

| Métrica | Threshold inicial | Justificativa |
|---|---|---|
| Cobertura linha (JaCoCo) | ≥ 80 % (já existe em `pom.xml:297-348`) | manter o gate atual, ratchet sobe |
| Cobertura branch (JaCoCo) | ≥ 75 % (já existe) | idem |
| Cobertura no-decrease | sempre | já implementado em `scripts/coverage/compare_backend_coverage.py` |
| PMD `CyclomaticComplexity` por método | ≤ **10** | padrão McCabe original. O post Rails usa 6, mas é convenção Ruby/Rubocop. Para Java mainstream (Spring controllers, DTO mappers), 6 produz ruído. SonarSource e Google Java Style usam 10–15. **10 é defensável e atingível.** |
| PMD `NPathComplexity` por método | ≤ 200 | default PMD; serve como teto duro contra explosão combinatória |
| PMD `CognitiveComplexity` por método | ≤ 15 | métrica de Sonar; 15 é o default oficial |
| PMD `ExcessiveMethodLength` | ≤ 40 linhas | O post Rails usa 15 — adequado a Ruby idiomático. Java tem mais boilerplate (declarações de tipo, exceções). 40 mantém o espírito sem flagging falso positivo |
| PMD `ExcessiveClassLength` | ≤ 250 linhas | idem; classes Spring com `@Service` + DTOs internos passam de 100 sem ser "ruins". 250 é o default PMD ajustado |
| Checkstyle | já configurado, manter | gate existente |
| SpotBugs threshold | High (já configurado) | manter |
| ArchUnit | já presente | manter |

### Frontend (TypeScript)

| Métrica | Threshold inicial | Justificativa |
|---|---|---|
| Cobertura linha (Vitest) | ≥ 80 % (já em `vitest.config.ts:37-42`) | manter |
| Cobertura branch (Vitest) | ≥ 80 % | manter |
| Cobertura no-decrease | sempre | já em `scripts/coverage/compare_frontend_coverage.py` |
| ESLint `complexity` | ≤ **10** | mesma justificativa do backend; alinhado a `eslint-plugin-sonarjs` defaults |
| ESLint `max-lines-per-function` | ≤ 60 linhas (excluindo blank/comments) | hooks React + componentes funcionais comuns ficam em 30–50. 60 é teto sem virar policial |
| ESLint `max-lines` por arquivo | ≤ 400 linhas | componentes de página podem crescer; 400 sinaliza para extrair sub-componentes |
| ESLint `max-depth` | ≤ 4 | default ESLint |
| ESLint `max-params` | ≤ 5 | default; sinaliza para usar objeto de opções |
| TypeScript strict | já habilitado em `tsconfig.json:18` | manter |
| Prettier / Stylelint | já gates do CI | manter |

### Política de ratchet

- **Baseline:** valor obtido na primeira execução em `main`, congelado em `.quality-baseline/*.json`.
- **Subir:** se uma execução produz métrica melhor, o script imprime um aviso `[ratchet candidate] cobertura subiu de 80% para 83%` e o desenvolvedor pode rodar `./scripts/quality.sh --update-baseline` para fixar o novo piso.
- **Nunca descer automaticamente.** Updates de baseline são commits explícitos, revisáveis em PR.

---

## Tarefas

> Cada tarefa termina com um commit. Use mensagens no padrão `<tipo>(<escopo>): <descrição>` (ver `git log`). Todo passo deve ser executável em 2–5 min.

---

### Tarefa 1 — Adicionar regras de complexidade ao PMD do backend

**Objetivo:** transformar `backend/pmd/ruleset.xml` em ruleset com gates de complexidade.

**Passos:**

- [ ] **1.1 (impl)** Substituir `backend/pmd/ruleset.xml` por:
  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <ruleset name="NossaLista PMD"
           xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.github.io/schema/ruleset_2_0_0.xsd">
      <description>
          Regras de PMD usadas como gate bloqueante no CI e no quality gate local.
          Foca em complexidade e tamanho — outras dimensões ficam em Checkstyle/SpotBugs.
      </description>

      <!-- Errorprone (mantém o que já existia) -->
      <rule ref="category/java/errorprone.xml/EmptyCatchBlock"/>

      <!-- Complexidade: thresholds calibrados para Java mainstream (não Rails) -->
      <rule ref="category/java/design.xml/CyclomaticComplexity">
          <properties>
              <property name="methodReportLevel" value="10"/>
              <property name="classReportLevel"  value="60"/>
          </properties>
      </rule>

      <rule ref="category/java/design.xml/NPathComplexity">
          <properties>
              <property name="reportLevel" value="200"/>
          </properties>
      </rule>

      <rule ref="category/java/design.xml/CognitiveComplexity">
          <properties>
              <property name="reportLevel" value="15"/>
          </properties>
      </rule>

      <!-- Tamanho: thresholds calibrados para Java (mais verboso que Ruby) -->
      <rule ref="category/java/design.xml/ExcessiveMethodLength">
          <properties>
              <property name="minimum" value="40"/>
          </properties>
      </rule>

      <rule ref="category/java/design.xml/ExcessiveClassLength">
          <properties>
              <property name="minimum" value="250"/>
          </properties>
      </rule>
  </ruleset>
  ```

- [ ] **1.2 (teste)** Rodar PMD localmente para descobrir violações pré-existentes:
  ```bash
  cd backend && ./mvnw -P strict-quality pmd:pmd -DskipTests
  ```
  **Saída esperada:** ou `BUILD SUCCESS` (caso o código já atenda), ou lista de violações em `target/pmd.xml`. Anotar contagem.

- [ ] **1.3 (decisão)** Se houver > 5 violações:
  - Não relaxar os thresholds.
  - Listar violações em `docs/quality-gate-debt.md` (sem committar code refactor agora) e marcar como **TODO ratchet phase 2**.
  - Aplicar `<exclude-pattern>` temporários no ruleset apontando para os arquivos da dívida (com comentário `<!-- TODO: refatorar até YYYY-MM-DD -->`).
  - Se ≤ 5: corrigir antes de seguir.

- [ ] **1.4 (validação)** Rodar `./mvnw -P strict-quality verify -DskipITs` no backend e confirmar `BUILD SUCCESS`.

- [ ] **1.5 (commit)**
  ```bash
  git add backend/pmd/ruleset.xml docs/quality-gate-debt.md
  git commit -m "feat(backend): regras de complexidade no PMD ruleset"
  ```

---

### Tarefa 2 — Adicionar regras de complexidade ao ESLint do frontend

**Objetivo:** ativar `complexity`, `max-lines-per-function`, `max-lines`, `max-depth`, `max-params` no `frontend/eslint.config.js`.

**Passos:**

- [ ] **2.1 (impl)** Em `frontend/eslint.config.js`, dentro do bloco de regras TS/React principal, adicionar:
  ```js
  rules: {
      // ... regras existentes preservadas ...

      // Complexidade — gates calibrados para React 19 + TS estrito
      'complexity': ['error', { max: 10 }],
      'max-lines-per-function': ['error', {
          max: 60,
          skipBlankLines: true,
          skipComments: true,
          IIFEs: true,
      }],
      'max-lines': ['error', {
          max: 400,
          skipBlankLines: true,
          skipComments: true,
      }],
      'max-depth': ['error', 4],
      'max-params': ['error', 5],
  }
  ```

- [ ] **2.2 (impl)** Adicionar override permissivo para testes (E2E e unit), pois fixtures comuns excedem `max-lines-per-function`:
  ```js
  {
      files: ['**/*.test.{ts,tsx}', 'e2e/**/*.ts', 'src/test/**/*.ts'],
      rules: {
          'max-lines-per-function': 'off',
          'complexity': 'off',
      },
  }
  ```

- [ ] **2.3 (teste)** Rodar lint:
  ```bash
  cd frontend && npm run lint
  ```
  **Saída esperada:** ou ok, ou contagem de violações. Anotar.

- [ ] **2.4 (decisão)** Mesma política da tarefa 1: > 5 → adicionar à dívida; ≤ 5 → corrigir agora.

- [ ] **2.5 (commit)**
  ```bash
  git add frontend/eslint.config.js docs/quality-gate-debt.md
  git commit -m "feat(frontend): regras de complexidade no ESLint config"
  ```

---

### Tarefa 3 — Script `scripts/quality/ratchet.py` (núcleo do ratchet)

**Objetivo:** ferramenta única que compara métricas atuais com `.quality-baseline/*.json` e retorna exit code.

**Passos:**

- [ ] **3.1 (impl)** Criar `scripts/quality/ratchet.py` com:
  ```python
  #!/usr/bin/env python3
  """Compara métricas atuais com a baseline armazenada.

  Uso:
    ratchet.py check <metrics.json> <baseline.json>
    ratchet.py update <metrics.json> <baseline.json>

  Retorna 0 se todas as métricas atendem ou superam a baseline,
  1 se alguma regrediu, 2 em erro de uso.

  Cada métrica é declarada com direção ('higher_is_better' | 'lower_is_better').
  """
  import json
  import sys
  from pathlib import Path


  def load(path: Path) -> dict:
      if not path.exists():
          return {}
      return json.loads(path.read_text(encoding="utf-8"))


  def check(current: dict, baseline: dict) -> list[str]:
      failures: list[str] = []
      for name, meta in current.items():
          if name not in baseline:
              continue  # nova métrica, será incorporada no próximo update
          cur = meta["value"]
          base = baseline[name]["value"]
          direction = meta.get("direction", "higher_is_better")
          if direction == "higher_is_better" and cur + 1e-9 < base:
              failures.append(f"{name}: {cur} < baseline {base}")
          elif direction == "lower_is_better" and cur > base + 1e-9:
              failures.append(f"{name}: {cur} > baseline {base}")
      return failures


  def main() -> int:
      if len(sys.argv) != 4:
          print(__doc__)
          return 2
      mode, metrics_path, baseline_path = sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])
      current = load(metrics_path)
      baseline = load(baseline_path)

      if mode == "update":
          baseline_path.parent.mkdir(parents=True, exist_ok=True)
          baseline_path.write_text(json.dumps(current, indent=2, sort_keys=True), encoding="utf-8")
          print(f"Baseline atualizada: {baseline_path}")
          return 0

      if mode == "check":
          if not baseline:
              print(f"Baseline ausente em {baseline_path} — rodando como primeira execução, gravando.")
              baseline_path.parent.mkdir(parents=True, exist_ok=True)
              baseline_path.write_text(json.dumps(current, indent=2, sort_keys=True), encoding="utf-8")
              return 0
          failures = check(current, baseline)
          if failures:
              print("Regressões detectadas:")
              for f in failures:
                  print(f"  - {f}")
              return 1
          return 0

      print(f"Modo desconhecido: {mode}")
      return 2


  if __name__ == "__main__":
      sys.exit(main())
  ```

- [ ] **3.2 (teste)** Criar `scripts/quality/test_ratchet.sh` (smoke test inline):
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail
  TMP=$(mktemp -d)
  cat > "$TMP/cur.json" <<'EOF'
  {
    "coverage_line":   {"value": 82.5, "direction": "higher_is_better"},
    "pmd_violations":  {"value": 3,    "direction": "lower_is_better"}
  }
  EOF
  cat > "$TMP/base.json" <<'EOF'
  {
    "coverage_line":   {"value": 80.0, "direction": "higher_is_better"},
    "pmd_violations":  {"value": 5,    "direction": "lower_is_better"}
  }
  EOF
  python3 scripts/quality/ratchet.py check "$TMP/cur.json" "$TMP/base.json"
  echo "OK: smoke test ratchet passou"
  rm -rf "$TMP"
  ```
  Tornar executável e rodar:
  ```bash
  chmod +x scripts/quality/test_ratchet.sh
  ./scripts/quality/test_ratchet.sh
  ```
  **Saída esperada:** `OK: smoke test ratchet passou` e exit 0.

- [ ] **3.3 (teste de regressão)** Inverter os valores e confirmar exit 1:
  ```bash
  python3 -c "
  import json, subprocess, tempfile, os
  with tempfile.TemporaryDirectory() as t:
      cur = {'coverage_line': {'value': 70.0, 'direction': 'higher_is_better'}}
      base = {'coverage_line': {'value': 80.0, 'direction': 'higher_is_better'}}
      open(f'{t}/c.json','w').write(json.dumps(cur))
      open(f'{t}/b.json','w').write(json.dumps(base))
      r = subprocess.run(['python3','scripts/quality/ratchet.py','check',f'{t}/c.json',f'{t}/b.json'])
      assert r.returncode == 1, f'expected 1 got {r.returncode}'
      print('OK: regressão detectada')
  "
  ```

- [ ] **3.4 (commit)**
  ```bash
  chmod +x scripts/quality/ratchet.py
  git add scripts/quality/ratchet.py scripts/quality/test_ratchet.sh
  git commit -m "feat(scripts): ratchet.py para gate de regressão de métricas"
  ```

---

### Tarefa 4 — Script `scripts/quality/run-backend.sh`

**Objetivo:** rodar Maven e produzir `target/quality-metrics/backend.json`.

**Passos:**

- [ ] **4.1 (impl)** Criar `scripts/quality/run-backend.sh`:
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail

  SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
  REPO_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
  BACKEND_DIR="$REPO_ROOT/backend"
  OUT_DIR="$REPO_ROOT/.quality-output"
  mkdir -p "$OUT_DIR"

  MODE="${1:-full}"   # full | pre-commit

  cd "$BACKEND_DIR"

  if [[ "$MODE" == "pre-commit" ]]; then
      # subset rápido — sem cobertura, só lint/static analysis
      ./mvnw -q -P strict-quality \
          checkstyle:check pmd:check spotbugs:check \
          -DskipTests -DskipITs
      exit 0
  fi

  # full: roda tudo
  ./mvnw -q -P strict-quality verify -DskipITs

  JACOCO_XML="$BACKEND_DIR/target/jacoco-report/jacoco.xml"
  PMD_XML="$BACKEND_DIR/target/pmd.xml"

  python3 - <<PY
  import json, sys, os
  from pathlib import Path
  import defusedxml.ElementTree as ET

  jacoco = ET.parse("$JACOCO_XML").getroot()
  metrics = {}

  def counter(name):
      el = jacoco.find(f"counter[@type='{name}']")
      if el is None: return 0.0
      missed = int(el.attrib.get("missed", "0"))
      covered = int(el.attrib.get("covered", "0"))
      total = missed + covered
      return (covered / total * 100.0) if total else 0.0

  metrics["backend_coverage_line"]   = {"value": round(counter("LINE"),   2), "direction": "higher_is_better"}
  metrics["backend_coverage_branch"] = {"value": round(counter("BRANCH"), 2), "direction": "higher_is_better"}

  pmd_violations = 0
  pmd_path = Path("$PMD_XML")
  if pmd_path.exists():
      pmd_root = ET.parse(str(pmd_path)).getroot()
      ns = {"p": "http://pmd.sourceforge.net/report/2.0.0"}
      pmd_violations = len(pmd_root.findall(".//p:violation", ns))
  metrics["backend_pmd_violations"] = {"value": pmd_violations, "direction": "lower_is_better"}

  out = Path("$OUT_DIR") / "backend.json"
  out.write_text(json.dumps(metrics, indent=2, sort_keys=True))
  print(f"Wrote {out}")
  PY
  ```

- [ ] **4.2 (teste)** Tornar executável e rodar em modo pre-commit:
  ```bash
  chmod +x scripts/quality/run-backend.sh
  ./scripts/quality/run-backend.sh pre-commit
  ```
  **Saída esperada:** Maven roda checkstyle+pmd+spotbugs, exit 0, sem rodar testes. Tempo alvo: < 25s.

- [ ] **4.3 (teste)** Rodar em modo full:
  ```bash
  ./scripts/quality/run-backend.sh full
  cat .quality-output/backend.json
  ```
  **Saída esperada:** JSON com 3 chaves (`backend_coverage_line`, `backend_coverage_branch`, `backend_pmd_violations`).

- [ ] **4.4 (commit)**
  ```bash
  git add scripts/quality/run-backend.sh
  git commit -m "feat(scripts): run-backend.sh extrai métricas para JSON"
  ```

---

### Tarefa 5 — Script `scripts/quality/run-frontend.sh`

**Objetivo:** rodar npm scripts e produzir `target/quality-metrics/frontend.json`.

**Passos:**

- [ ] **5.1 (impl)** Criar `scripts/quality/run-frontend.sh`:
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail

  SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
  REPO_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
  FRONTEND_DIR="$REPO_ROOT/frontend"
  OUT_DIR="$REPO_ROOT/.quality-output"
  mkdir -p "$OUT_DIR"

  MODE="${1:-full}"
  cd "$FRONTEND_DIR"

  if [[ "$MODE" == "pre-commit" ]]; then
      npm run lint
      npm run typecheck
      exit 0
  fi

  # full
  ESLINT_REPORT="$OUT_DIR/eslint-report.json"
  npx eslint . --ext ts,tsx --format json --output-file "$ESLINT_REPORT" || true
  npm run typecheck
  npm run test:coverage

  python3 - <<PY
  import json
  from pathlib import Path

  metrics = {}

  # ESLint violations
  report = json.loads(Path("$ESLINT_REPORT").read_text())
  errors = sum(f["errorCount"] for f in report)
  warnings = sum(f["warningCount"] for f in report)
  metrics["frontend_eslint_errors"]   = {"value": errors,   "direction": "lower_is_better"}
  metrics["frontend_eslint_warnings"] = {"value": warnings, "direction": "lower_is_better"}

  # Vitest coverage-summary.json
  cov_path = Path("$FRONTEND_DIR/coverage/coverage-summary.json")
  if cov_path.exists():
      cov = json.loads(cov_path.read_text())["total"]
      metrics["frontend_coverage_line"]   = {"value": round(cov["lines"]["pct"],     2), "direction": "higher_is_better"}
      metrics["frontend_coverage_branch"] = {"value": round(cov["branches"]["pct"],  2), "direction": "higher_is_better"}
      metrics["frontend_coverage_func"]   = {"value": round(cov["functions"]["pct"], 2), "direction": "higher_is_better"}

  out = Path("$OUT_DIR") / "frontend.json"
  out.write_text(json.dumps(metrics, indent=2, sort_keys=True))
  print(f"Wrote {out}")
  PY

  # ESLint deve falhar build se houver erros (lint-staged já cobre pre-commit)
  if grep -q '"errorCount":[1-9]' "$ESLINT_REPORT"; then
      echo "ESLint encontrou errors — falhando."
      exit 1
  fi
  ```

- [ ] **5.2 (teste)** Executar e validar:
  ```bash
  chmod +x scripts/quality/run-frontend.sh
  ./scripts/quality/run-frontend.sh pre-commit
  ```
  **Saída esperada:** lint + typecheck passam, exit 0.

- [ ] **5.3 (teste)** Modo full:
  ```bash
  ./scripts/quality/run-frontend.sh full
  cat .quality-output/frontend.json
  ```
  **Saída esperada:** JSON com 5 chaves de métricas.

- [ ] **5.4 (commit)**
  ```bash
  git add scripts/quality/run-frontend.sh
  git commit -m "feat(scripts): run-frontend.sh extrai métricas para JSON"
  ```

---

### Tarefa 6 — Renderizador de tabela `scripts/quality/render-table.sh`

**Objetivo:** imprimir a tabela ✓/✗ idêntica em espírito ao post Codeminer42.

**Passos:**

- [ ] **6.1 (impl)** Criar `scripts/quality/render-table.sh`:
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail

  GREEN="\033[0;32m"
  RED="\033[0;31m"
  RESET="\033[0m"

  METRICS_FILE="$1"
  BASELINE_FILE="$2"

  python3 - "$METRICS_FILE" "$BASELINE_FILE" <<'PY'
  import json, sys
  from pathlib import Path

  cur  = json.loads(Path(sys.argv[1]).read_text())
  base = json.loads(Path(sys.argv[2]).read_text()) if Path(sys.argv[2]).exists() else {}

  G, R, Z = "\033[0;32m", "\033[0;31m", "\033[0m"
  print(f"{'Métrica':<35} {'Atual':>10} {'Baseline':>10}  Status")
  print("-" * 70)
  any_fail = False
  for name, meta in sorted(cur.items()):
      v = meta["value"]
      direction = meta.get("direction", "higher_is_better")
      b = base.get(name, {}).get("value", "—")
      if b == "—":
          status = f"{G}✓ novo{Z}"
      elif direction == "higher_is_better":
          ok = v + 1e-9 >= b
          status = f"{G}✓{Z}" if ok else f"{R}✗ regrediu{Z}"
          any_fail |= not ok
      else:
          ok = v <= b + 1e-9
          status = f"{G}✓{Z}" if ok else f"{R}✗ regrediu{Z}"
          any_fail |= not ok
      print(f"{name:<35} {v:>10} {b:>10}  {status}")
  print("-" * 70)
  sys.exit(1 if any_fail else 0)
  PY
  ```

- [ ] **6.2 (teste)** Smoke test com JSONs fake:
  ```bash
  chmod +x scripts/quality/render-table.sh
  ./scripts/quality/render-table.sh .quality-output/backend.json /dev/null
  ```
  **Saída esperada:** tabela impressa com coluna `Baseline` mostrando `—` e status `✓ novo` em todas as linhas.

- [ ] **6.3 (commit)**
  ```bash
  git add scripts/quality/render-table.sh
  git commit -m "feat(scripts): render-table.sh imprime tabela ✓/✗"
  ```

---

### Tarefa 7 — Orquestrador `scripts/quality.sh`

**Objetivo:** entry-point único.

**Passos:**

- [ ] **7.1 (impl)** Criar `scripts/quality.sh`:
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail

  SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
  REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
  BASELINE_DIR="$REPO_ROOT/.quality-baseline"
  OUT_DIR="$REPO_ROOT/.quality-output"

  MODE="full"
  UPDATE_BASELINE=0
  for arg in "$@"; do
      case "$arg" in
          --pre-commit) MODE="pre-commit" ;;
          --full)       MODE="full" ;;
          --update-baseline) UPDATE_BASELINE=1 ;;
          -h|--help)
              cat <<USAGE
  Uso: ./scripts/quality.sh [--pre-commit | --full] [--update-baseline]

    --pre-commit       subset rápido (≤ 30s alvo); lint + typecheck + static analysis
    --full             roda tudo (default); cobertura + ratchet
    --update-baseline  grava a baseline atual após um --full bem-sucedido
  USAGE
              exit 0 ;;
          *) echo "Arg desconhecido: $arg"; exit 2 ;;
      esac
  done

  echo "▶ Backend ($MODE)…"
  "$SCRIPT_DIR/quality/run-backend.sh" "$MODE"
  echo "▶ Frontend ($MODE)…"
  "$SCRIPT_DIR/quality/run-frontend.sh" "$MODE"

  if [[ "$MODE" == "pre-commit" ]]; then
      echo "✓ pre-commit gate ok"
      exit 0
  fi

  # Full: ratchet
  FAILED=0
  for stack in backend frontend; do
      METRICS="$OUT_DIR/$stack.json"
      BASELINE="$BASELINE_DIR/$stack.json"

      echo
      echo "── $stack ──"
      "$SCRIPT_DIR/quality/render-table.sh" "$METRICS" "$BASELINE" || FAILED=1

      if [[ $UPDATE_BASELINE -eq 1 ]]; then
          python3 "$SCRIPT_DIR/quality/ratchet.py" update "$METRICS" "$BASELINE"
      else
          python3 "$SCRIPT_DIR/quality/ratchet.py" check "$METRICS" "$BASELINE" || FAILED=1
      fi
  done

  exit $FAILED
  ```

- [ ] **7.2 (teste)** Rodar primeira vez (cria baseline):
  ```bash
  chmod +x scripts/quality.sh
  ./scripts/quality.sh --full
  ls .quality-baseline/
  ```
  **Saída esperada:** tabela ✓ em todas linhas (status `✓ novo`); `.quality-baseline/backend.json` e `frontend.json` criados.

- [ ] **7.3 (teste)** Rodar segunda vez (gate ativo):
  ```bash
  ./scripts/quality.sh --full
  ```
  **Saída esperada:** todas linhas `✓`, exit 0.

- [ ] **7.4 (teste)** Rodar modo pre-commit isolado:
  ```bash
  time ./scripts/quality.sh --pre-commit
  ```
  **Saída esperada:** exit 0, tempo total ≤ 30s.

- [ ] **7.5 (commit)**
  ```bash
  git add scripts/quality.sh .quality-baseline/
  git commit -m "feat(scripts): quality.sh orquestrador unificado + baseline inicial"
  ```

---

### Tarefa 8 — README e doc da baseline

**Passos:**

- [ ] **8.1 (impl)** Criar `.quality-baseline/README.md`:
  ```markdown
  # Quality Baseline

  Arquivos JSON congelados que servem de piso para o ratchet em `./scripts/quality.sh`.

  ## Quando atualizar

  - Quando uma métrica subiu legitimamente (mais cobertura, menos violações) e o time quer fixar o novo piso.
  - **Nunca** para "destravar" um build vermelho. Se o build falhou, a feature deve melhorar a métrica, não relaxar o piso.

  ## Como atualizar

  ```bash
  ./scripts/quality.sh --full --update-baseline
  git add .quality-baseline/
  git commit -m "chore(quality): ratchet baseline up"
  ```

  Updates de baseline são commits explícitos, revisáveis em PR.
  ```

- [ ] **8.2 (commit)**
  ```bash
  git add .quality-baseline/README.md
  git commit -m "docs(quality): explicar política de atualização da baseline"
  ```

---

### Tarefa 9 — Husky + lint-staged + hook pre-commit

**Objetivo:** disparar o subset rápido automaticamente antes de cada commit.

**Passos:**

- [ ] **9.1 (impl)** No `frontend/package.json`, adicionar em `devDependencies` (rodar `npm i -D` para resolver versões):
  ```bash
  cd frontend
  npm install -D husky@^9 lint-staged@^15
  ```

- [ ] **9.2 (impl)** Adicionar em `frontend/package.json`:
  ```json
  "scripts": {
      "...": "...",
      "prepare": "cd .. && husky frontend/.husky"
  },
  "lint-staged": {
      "frontend/**/*.{ts,tsx}": ["eslint --max-warnings 0", "prettier --check"],
      "frontend/**/*.{css,scss}": ["stylelint"]
  }
  ```

- [ ] **9.3 (impl)** Criar `.husky/pre-commit` na **raiz do monorepo**:
  ```bash
  #!/usr/bin/env bash
  set -euo pipefail

  REPO_ROOT="$(git rev-parse --show-toplevel)"
  cd "$REPO_ROOT"

  # 1. lint-staged só nos arquivos alterados (rápido)
  if [ -d "$REPO_ROOT/frontend/node_modules" ]; then
      (cd frontend && npx lint-staged)
  fi

  # 2. Quality gate subset (≤ 30s alvo)
  ./scripts/quality.sh --pre-commit
  ```

  Tornar executável: `chmod +x .husky/pre-commit`.

- [ ] **9.4 (teste)** Forçar instalação dos hooks e validar:
  ```bash
  cd frontend && npm run prepare
  cd ..
  # Cria commit de no-op para disparar hook
  echo "" >> docs/quality-gate.md
  git add docs/quality-gate.md
  git commit -m "test: trigger pre-commit hook"
  ```
  **Saída esperada:** hook roda lint-staged + `./scripts/quality.sh --pre-commit`, commit acontece. Se algum gate falha, commit é abortado.

- [ ] **9.5 (decisão)** Se o hook ultrapassar 30s:
  - Aceitar até 45s para a primeira iteração.
  - Caso contrário, mover `typecheck` para fora do pre-commit (deixar só lint).

- [ ] **9.6 (commit)**
  ```bash
  git add .husky/pre-commit frontend/package.json frontend/package-lock.json
  git commit -m "feat(quality): pre-commit hook com husky + lint-staged"
  ```

---

### Tarefa 10 — `.github/dependabot.yml`

**Passos:**

- [ ] **10.1 (impl)** Criar `.github/dependabot.yml`:
  ```yaml
  version: 2
  updates:
    # Backend Java/Maven
    - package-ecosystem: "maven"
      directory: "/backend"
      schedule:
        interval: "weekly"
        day: "monday"
        time: "06:00"
        timezone: "America/Sao_Paulo"
      open-pull-requests-limit: 5
      labels: ["dependencies", "backend"]
      ignore:
        # Spring Boot major bumps são manuais
        - dependency-name: "org.springframework.boot:*"
          update-types: ["version-update:semver-major"]

    # Frontend npm
    - package-ecosystem: "npm"
      directory: "/frontend"
      schedule:
        interval: "weekly"
        day: "monday"
        time: "06:00"
        timezone: "America/Sao_Paulo"
      open-pull-requests-limit: 5
      labels: ["dependencies", "frontend"]
      groups:
        eslint:
          patterns: ["eslint*", "@typescript-eslint/*", "typescript-eslint"]
        react:
          patterns: ["react", "react-dom", "@types/react*"]
        vitest:
          patterns: ["vitest", "@vitest/*"]
      ignore:
        - dependency-name: "react"
          update-types: ["version-update:semver-major"]
        - dependency-name: "react-dom"
          update-types: ["version-update:semver-major"]

    # GitHub Actions
    - package-ecosystem: "github-actions"
      directory: "/"
      schedule:
        interval: "weekly"
        day: "monday"
        time: "06:00"
        timezone: "America/Sao_Paulo"
      open-pull-requests-limit: 5
      labels: ["dependencies", "ci"]
  ```

- [ ] **10.2 (validação)** Validar YAML:
  ```bash
  python3 -c "import yaml; yaml.safe_load(open('.github/dependabot.yml'))" && echo "YAML válido"
  ```

- [ ] **10.3 (commit)**
  ```bash
  git add .github/dependabot.yml
  git commit -m "chore(ci): adicionar dependabot para maven, npm e github-actions"
  ```

---

### Tarefa 11 — Documentação `docs/quality-gate.md`

**Passos:**

- [ ] **11.1 (impl)** Criar `docs/quality-gate.md` com seções: Visão geral, Como rodar, Dimensões medidas, Thresholds, Política de ratchet, Limitações reconhecidas, FAQ.

  Conteúdo de referência:
  ```markdown
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
  | Backend  | Tamanho de método/classe  | PMD `ExcessiveMethodLength`, `ExcessiveClassLength` |
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

  ## Política de ratchet

  - Primeira execução grava `.quality-baseline/*.json`.
  - Execuções seguintes só passam se nenhuma métrica regredir.
  - Promover novo piso é um commit explícito: `./scripts/quality.sh --full --update-baseline && git commit`.

  ## Limitações reconhecidas

  O gate **NÃO** mede:

  - **Segurança runtime** (SQLi, XSS, auth bypass) — coberto parcialmente por Semgrep/OWASP-DC no CI.
  - **Performance** (latência, throughput, N+1 queries) — exige observabilidade.
  - **Race conditions / concorrência** — não há análise estática confiável.
  - **Memory leaks** — exige profiling.
  - **Intenção da feature** — só revisão humana garante que o código faz o que o ticket pediu.
  - **Qualidade dos testes** — só mede cobertura, não se o teste afirma algo útil.

  > **Princípio:** o gate mecaniza o que pode ser mecanizado. A revisão humana fica livre para focar no que máquinas não fazem.

  ## FAQ

  **Por que ciclomática ≤ 10 e não ≤ 6 como no post Rails?**
  Java tem mais boilerplate (declarações de tipo, exceções verificadas). 10 é o padrão McCabe original e o default do SonarSource. 6 produziria ruído sem ganho de qualidade.

  **Por que não mutation testing?**
  Custo alto (Pitest demora minutos por módulo) e ROI baixo sem CI dedicado. Pode entrar em iteração futura.
  ```

- [ ] **11.2 (commit)**
  ```bash
  git add docs/quality-gate.md
  git commit -m "docs(quality): documentar quality gate e suas limitações"
  ```

---

### Tarefa 12 — Atualizar `CLAUDE.md` e `README.md`

**Objetivo:** instruir o agente Claude a rodar `./scripts/quality.sh --pre-commit` antes de commitar; linkar a doc no README.

**Passos:**

- [ ] **12.1 (impl)** Em `CLAUDE.md`, antes da seção "Stack Técnico Planejada", inserir:
  ```markdown
  ## Quality Gate (Obrigatório antes de commit)

  Antes de qualquer `git commit`, rode:

      ./scripts/quality.sh --pre-commit

  Ele faz lint + typecheck + static analysis nos dois ecossistemas em ≤ 30s. Se passar, commite. Se falhar, **corrija a regressão antes**, não relaxe thresholds.

  Para uma verificação completa (cobertura + ratchet), antes de abrir PR:

      ./scripts/quality.sh --full

  Detalhes, thresholds e limitações: `docs/quality-gate.md`.
  ```

- [ ] **12.2 (impl)** Em `README.md`, na seção apropriada (após instruções de setup), adicionar:
  ```markdown
  ## Quality Gate

  Validação unificada de qualidade: `./scripts/quality.sh --pre-commit` (rápido) ou `./scripts/quality.sh --full` (completo). Documentação em [docs/quality-gate.md](docs/quality-gate.md).
  ```

- [ ] **12.3 (impl)** Em `backend/QUALITY.md`, anexar nota final referenciando `docs/quality-gate.md` como entry-point unificado.

- [ ] **12.4 (commit)**
  ```bash
  git add CLAUDE.md README.md backend/QUALITY.md
  git commit -m "docs: instruir agente a rodar quality gate antes de commit"
  ```

---

### Tarefa 13 — Validação end-to-end

**Objetivo:** rodar o ciclo completo e provar que regressões são detectadas.

**Passos:**

- [ ] **13.1 (teste)** Limpar baseline e rodar do zero:
  ```bash
  rm -f .quality-baseline/*.json
  ./scripts/quality.sh --full
  # Deve criar baseline e passar
  ```

- [ ] **13.2 (teste)** Rodar de novo, sem mudanças — deve passar:
  ```bash
  ./scripts/quality.sh --full
  echo "exit: $?"
  ```
  **Saída esperada:** `exit: 0`.

- [ ] **13.3 (teste de regressão induzida)** Adicionar temporariamente uma função frontend com complexidade 12:
  ```bash
  # Criar arquivo de teste local (NÃO committar)
  cat > frontend/src/_complexity_probe.ts <<'EOF'
  export function probe(n: number): number {
    let r = 0;
    if (n > 0) r++; if (n > 1) r++; if (n > 2) r++; if (n > 3) r++;
    if (n > 4) r++; if (n > 5) r++; if (n > 6) r++; if (n > 7) r++;
    if (n > 8) r++; if (n > 9) r++; if (n > 10) r++;
    return r;
  }
  EOF
  ./scripts/quality.sh --full
  echo "exit: $?"
  rm frontend/src/_complexity_probe.ts
  ```
  **Saída esperada:** `exit: 1`, ESLint report violation de `complexity`, tabela mostra `✗ regrediu` em `frontend_eslint_errors`.

- [ ] **13.4 (teste de pre-commit)** Simular commit com regressão:
  ```bash
  cat > frontend/src/_complexity_probe.ts <<'EOF'
  // mesmo conteúdo do passo 13.3
  EOF
  git add frontend/src/_complexity_probe.ts
  git commit -m "test: deve falhar" || echo "Hook bloqueou (esperado)"
  git rm -f frontend/src/_complexity_probe.ts
  ```

- [ ] **13.5 (commit final)** Limpeza:
  ```bash
  git status # confirmar workspace limpo
  ```

---

## Checklist final antes de abrir PR

- [ ] `./scripts/quality.sh --full` passa em main após merge mental.
- [ ] `.quality-baseline/*.json` committadas com baseline real.
- [ ] Pre-commit hook funciona em commits novos.
- [ ] Dependabot.yml validou como YAML.
- [ ] CLAUDE.md instrui agente.
- [ ] docs/quality-gate.md tem seção de limitações.
- [ ] CI atual continua verde (gate local é subset, não duplica).

---

## Limitações reconhecidas (inspiradas no post)

Este gate **não substitui** revisão humana. Ele mecaniza o que pode ser mecanizado para que o revisor humano fique livre para o que importa.

**O que o gate captura:**

- Complexidade excessiva (métrica McCabe, NPath, cognitive).
- Tamanho excessivo de método/classe/função/arquivo.
- Cobertura de teste insuficiente ou regressão de cobertura.
- Estilo inconsistente (Checkstyle, ESLint, Prettier, Stylelint).
- Bugs estáticos óbvios (SpotBugs High).
- Violações arquiteturais declaradas (ArchUnit).
- Type errors (tsc strict).

**O que o gate NÃO captura:**

1. **Segurança runtime** — SQL injection, XSS, auth bypass, CSRF, IDOR. Coberto parcialmente por Semgrep/OWASP-DC/Gitleaks no CI, mas não por este gate local.
2. **Performance** — latência, throughput, N+1 queries, queries faltando índice. Exige observabilidade ou benchmarks.
3. **Race conditions e concorrência** — Spring `@Transactional` propagation, WebSocket message ordering, otimistic locking. Não há análise estática confiável.
4. **Memory leaks** — exige profiling, fora do escopo de métricas estáticas.
5. **Intenção da feature** — o código pode estar tecnicamente perfeito e fazer a coisa errada. Só revisão humana resolve.
6. **Qualidade dos testes** — cobertura mede *qual linha foi executada*, não *qual asserção foi feita*. Um teste sem `assert` ainda cobre. Mutation testing endereçaria isso, mas foi adiado.
7. **UX / acessibilidade** — fora do escopo deste gate. Existe a skill `web-design-guidelines` para isso.
8. **Decisões arquiteturais erradas mas legais** — escolher REST quando WebSocket seria melhor, ou vice-versa. Métrica não vê.
9. **Documentação correta** — gate não valida se README, CLAUDE.md e docs/ refletem o código.
10. **Dependabot e licenças** — Dependabot é parte do plano, mas não é parte do `quality.sh` em si.

> Se você implementar este plano e parar de revisar PRs, **você está usando errado**. O gate é o piso, não o teto.

---

## Decisões controversas que o usuário deve revisar

1. **Ciclomática ≤ 10 (não ≤ 6 como no post Rails).** Justificado acima por idiomática Java. Se o usuário quiser ser mais agressivo, baixar para 8 — mas espera-se mais ruído inicial.
2. **Não incluir mutation testing nesta iteração.** Pitest existe para Java, Stryker para JS, mas custo é alto. Pode entrar em fase 2.
3. **`prepare: husky` em `frontend/package.json` em vez de package.json raiz.** Como o monorepo não tem `package.json` raiz hoje, evita-se criar um só para hospedar husky. O hook em si fica na raiz (`.husky/`), só a instalação parte do frontend.
4. **Reusar profile Maven `strict-quality`** em vez de criar `quality-local`. Evita drift, mas significa que o gate local depende do mesmo profile que o CI usa.
5. **`.quality-baseline/` versionada.** Alternativa: armazenar em cache CI (como `scripts/coverage/` já faz via SHA). Optei por versionar porque ratchet local precisa funcionar offline e baseline em cache CI complica o workflow do dev.
6. **Dívida técnica em `docs/quality-gate-debt.md`** com exclusões PMD temporárias se houver > 5 violações. Alternativa mais purista: corrigir tudo antes de ativar gate. Pragmaticamente, criar a dívida documentada destrava a entrega.
