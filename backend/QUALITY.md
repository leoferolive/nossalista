# Qualidade de codigo e testes (Backend)

Este documento define os gates obrigatorios de qualidade do backend.

## Gates obrigatorios

- **Checkstyle**: validacao em `verify`.
- **PMD**: regras bloqueantes em `backend/pmd/ruleset.xml`.
- **SpotBugs**: analise estatica com falha em achados de severidade alta.
- **ArchUnit**: validacao de regras arquiteturais em testes.
- **Cobertura JaCoCo**: minimo de **80% em linhas** e **75% em branches** no bundle monitorado.
- **Suíte de regressao**: testes criticos marcados com `@RegressionTest`.

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

## Smoke test de execucao

```bash
./mvnw -B -DskipTests package
java -jar target/nossalista-0.0.1-SNAPSHOT.jar --spring.profiles.active=ci
```

- O profile `ci` usa H2 e permite validar `GET /actuator/health` localmente.
