# Qualidade de codigo e testes (Backend)

Este documento define os gates obrigatorios de qualidade do backend.

> **Entry-point unificado:** Para rodar o gate dos dois ecossistemas (backend
> + frontend) de uma vez, com tabela final e ratchet de regressão, use
> `./scripts/quality.sh --full` na raiz. Documentação: `docs/quality-gate.md`.

## Gates obrigatorios

- **Checkstyle**: validacao em `verify`.
- **PMD**: regras bloqueantes em `backend/pmd/ruleset.xml`.
- **SpotBugs**: analise estatica com falha em achados de severidade alta.
- **ArchUnit**: validacao de regras arquiteturais em testes.
- **Cobertura JaCoCo**: minimo de **80% em linhas** e **75% em branches** no bundle monitorado.
- **Suíte de regressao**: testes criticos marcados com `@RegressionTest`.
- **SCA / OWASP Dependency-Check**: dependencias com CVEs bloqueantes falham o pipeline. Em Spring Boot 4, manter o SpringDoc na linha **3.x**; a linha `2.8.x` nao e compativel com Boot 4 e quebra a inicializacao dos testes.
- **Supressoes de SCA**: registrar em `backend/dependency-check-suppressions.xml` somente com justificativa objetiva, prazo de remocao e referencia ao fornecedor upstream. Hoje a unica supressao ativa cobre CVEs de dependencias npm transitivas detectadas via `backend/package-lock.json` residual (o backend e Java/Maven; remover o arquivo elimina a supressao). As supressoes de `CVE-2026-29062` (Jackson 3.0.4) e dos CVEs de DOMPurify do swagger-ui foram removidas em 2026-06-03 porque os bumps abaixo as tornaram obsoletas.
- **Baseline de versoes (bump de seguranca 2026-06-03)**: Spring Boot `4.0.6` (BOM sobe spring-framework `7.0.7`, spring-security `7.0.5`, tomcat `11.0.21`, jackson `3.1.2`); overrides via `<properties>`: tomcat `11.0.22`, netty `4.2.15.Final`, postgresql `42.7.11`; swagger-ui `5.32.6` (DOMPurify `3.4.0`). Esse conjunto zera os CVEs CVSS>=7 do gate sem novas supressoes.

## Comandos principais

- Executar gate completo:

```bash
./mvnw -B -Pstrict-quality verify
```

- Executar gate sem SCA (rodada local rapida):

```bash
./mvnw -B -Pstrict-quality -Ddependency-check.skip=true verify
```

- Executar apenas regressao:

```bash
./mvnw -B -Pregression-tests test
```

## Checkstyle

- Configuracao: `backend/checkstyle/checkstyle.xml`
- Configuracao estrita progressiva: `backend/checkstyle/checkstyle-strict.xml`
- Supressoes: `backend/checkstyle/suppressions.xml`

## PMD

- Regras bloqueantes: `backend/pmd/ruleset.xml`
- Execucao no `verify` via `maven-pmd-plugin`.
- Inclui gates de complexidade: `CyclomaticComplexity` (<= 10/metodo),
  `NPathComplexity` (<= 200), `CognitiveComplexity` (<= 15) e tamanho via
  `NcssCount` (metodo <= 40, classe <= 250).
- Violacoes pre-existentes em arquivos legados estao listadas em
  `docs/quality-gate-debt.md` e excluidas via `<exclude-pattern>` no ruleset
  (com prazo de refatoracao).
- **CPD (duplicacao):** execucao `cpd-report` (goal `cpd`, WARNING / report-only)
  com `minimumTokens=80`. Gera `target/cpd.xml`. IA copia-cola logica em vez de
  reusar. Promover a `cpd-check` (blocker) apos medir o baseline e excluir
  DTOs/mappers que inflam falso-positivo.

## SpotBugs

- Execucao no `verify` via `spotbugs-maven-plugin`.
- Configurado para falhar com achados de severidade alta (`spotbugs-check`, blocker).
- **FindSecBugs (SAST):** execucao adicional `spotbugs-security` com threshold
  `Medium` e `failOnError=false` (WARNING / report-only). Gera
  `target/spotbugs-security.xml` para decoracao de PR. Promover a blocker apos
  triagem dos achados (FindSecBugs tem falso-positivo em Spring Security/JPA).

## Cobertura

- Relatorio local: `backend/target/site/jacoco/index.html`
- O CI publica o relatorio como artefato da pipeline.

### Escopo monitorado no MVP

- Exclui o pacote `websocket` e o mapper `listitem/dto/ListItemMapper` do gate de cobertura.

## Gates de codigo gerado por IA (diff do PR)

Estes gates avaliam **o codigo novo de cada PR** (nao o projeto inteiro),
evitando herdar divida. Rodam no job `backend-quality` do CI.

- **Cobertura diferencial (`diff-cover`) — BLOCKER.** Exige **80% de cobertura
  nas linhas alteradas** do PR. Le `backend/target/site/jacoco/jacoco.xml` e
  compara com `origin/<base>`. Complementa (nao substitui) o gate JaCoCo de
  bundle 80/75 e o script no-decrease. Comando local:

  ```bash
  diff-cover backend/target/site/jacoco/jacoco.xml \
    --compare-branch=origin/main --src-roots backend/src/main/java --fail-under=80
  ```

  > Em monorepo, `--src-roots` e obrigatorio: o `jacoco.xml` registra paths por
  > package (sem prefixo) e sem isso o diff-cover reporta "No lines with
  > coverage information" (falso-positivo). Validar que a saida mostra
  > "% of diff lines covered".

- **Mutation testing (PIT) — ADIADO (incompativel com Java 25).** Smoke em
  2026-06-03 confirmou `Unsupported class file major version 69` (PIT 1.19.1 nao
  le bytecode JDK 25 via ASM; ver hcoles/pitest#1439). O step de CI esta
  desativado (`if: false`) e o plugin fica declarado no `pom.xml` pronto para
  reativar quando o PIT suportar JDK 25 (bumpar versao + religar o step). Quando
  ativo, rodaria so nas classes alteradas via `scmMutationCoverage`
  (`continue-on-error`), com threshold de mutation score 70 no codigo novo, para
  matar testes "fantasma" que inflam cobertura sem validar comportamento.
  Comando previsto (apos suporte a JDK 25):

  ```bash
  ./mvnw -B org.pitest:pitest-maven:scmMutationCoverage \
    -DoriginBranch=origin/main -DdestinationBranch=HEAD -Dinclude=ADDED,MODIFIED
  ```

- **Higiene de dependencias — WARNING.** `dependency:analyze` (declaradas-nao-usadas
  / usadas-nao-declaradas) + `dependency:resolve`. No Maven, coordenada
  inexistente ja falha o build na resolucao (cobre "package hallucination").

- **Error Prone — ADIADO.** Efetivamente bloqueado no Java 25 (roda dentro do
  javac e quebraria a compilacao). PMD + SpotBugs/FindSecBugs cobrem boa parte do
  escopo. Reavaliar quando uma release de Error Prone declarar suporte a JDK 25.

### Decoracao de PR (nao-bloqueante)

- **reviewdog** (Checkstyle, `filter-mode=added`): comenta inline so nas linhas
  do diff.
- **dorny/test-reporter**: publica resultados JUnit como Check Run.
- **sticky-pull-request-comment**: posta o relatorio do `diff-cover` como
  comentario fixo.

Todas as etapas de decoracao usam `continue-on-error` — nunca quebram o pipeline.

## Marcacao de regressao

- Anotacao: `backend/src/test/java/br/com/leoferolive/nossalista/support/RegressionTest.java`
- A anotacao equivale a `@Tag("regression")`.
- Usar em testes de funcionalidades criticas (auth, listas, itens, membros e realtime).

## Smoke test de execucao

```bash
./mvnw -B -DskipTests package
java -jar target/nossalista-0.0.1-SNAPSHOT.jar --spring.profiles.active=ci
```

- O profile `ci` usa H2 e permite validar `GET /actuator/health` localmente.
- Metricas Prometheus expostas em `GET /actuator/prometheus` (Micrometer; HTTP + JVM, tag `application=nossalista`) para scrape do kube-prometheus-stack.
- O endpoint tecnico `GET /api/health` tambem deve responder com `version`, `gitSha`, `gitTag`, `environment` e `buildTime` coerentes com os metadados de build/deploy.
