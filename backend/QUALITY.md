# Qualidade de codigo e testes (Backend)

Este documento define os gates obrigatorios de qualidade do backend.

## Gates obrigatorios

- **Checkstyle**: validacao em `verify`.
- **PMD**: regras bloqueantes em `backend/pmd/ruleset.xml`.
- **SpotBugs**: analise estatica com falha em achados de severidade alta.
- **ArchUnit**: validacao de regras arquiteturais em testes.
- **Cobertura JaCoCo**: minimo de **80% em linhas** e **75% em branches** no bundle monitorado.
- **Suíte de regressao**: testes criticos marcados com `@RegressionTest`.
- **Detector N+1 (n1-detector)**: testes selecionados registram `N1DetectorExtension` para coletar consultas executadas. O CI roda `n1-detector-cli check-report` sobre `target/n1-report.json` ao final da suite e falha se houver alertas acima do threshold configurado em `backend/src/test/resources/.n1-detector.yml`.
- **SCA / OWASP Dependency-Check**: dependencias com CVEs bloqueantes falham o pipeline. Em Spring Boot 4, manter o SpringDoc na linha **3.x**; a linha `2.8.x` nao e compativel com Boot 4 e quebra a inicializacao dos testes.
- **Supressoes de SCA**: registrar em `backend/dependency-check-suppressions.xml` somente com justificativa objetiva, prazo de remocao e referencia ao fornecedor upstream. Atualmente existe uma supressao temporaria para `CVE-2026-29062` porque o Spring Boot `4.0.3` ainda gerencia `tools.jackson.core:3.0.4`.

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

## SpotBugs

- Execucao no `verify` via `spotbugs-maven-plugin`.
- Configurado para falhar com achados de severidade alta.

## Cobertura

- Relatorio local: `backend/target/site/jacoco/index.html`
- O CI publica o relatorio como artefato da pipeline.

### Escopo monitorado no MVP

- Exclui o pacote `websocket` e o mapper `listitem/dto/ListItemMapper` do gate de cobertura.

## Marcacao de regressao

- Anotacao: `backend/src/test/java/br/com/leoferolive/nossalista/support/RegressionTest.java`
- A anotacao equivale a `@Tag("regression")`.
- Usar em testes de funcionalidades criticas (auth, listas, itens, membros e realtime).

## Detector N+1

- Configuracao: `backend/src/test/resources/.n1-detector.yml`
- Wiring Hibernate: `backend/src/test/java/br/com/leoferolive/nossalista/N1DetectorTestConfig.java` (registra `N1StatementInspector` via `HibernatePropertiesCustomizer`).
- Os jars `io.n1detector:*:0.1.0` ainda nao estao em Maven Central. O CI clona `https://github.com/leoferolive/n1-detector.git` e executa `mvn install` antes do `verify`. Localmente, instale uma vez com:

```bash
git clone https://github.com/leoferolive/n1-detector.git /tmp/n1-detector
cd /tmp/n1-detector/clients/java && mvn -q install -DskipTests
```

- O relatorio `backend/target/n1-report.json` e publicado como artefato no CI junto ao JaCoCo.

## Smoke test de execucao

```bash
./mvnw -B -DskipTests package
java -jar target/nossalista-0.0.1-SNAPSHOT.jar --spring.profiles.active=ci
```

- O profile `ci` usa H2 e permite validar `GET /actuator/health` localmente.
- O endpoint tecnico `GET /api/health` tambem deve responder com `version`, `gitSha`, `gitTag`, `environment` e `buildTime` coerentes com os metadados de build/deploy.
