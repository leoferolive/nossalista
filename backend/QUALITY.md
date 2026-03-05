# Qualidade de codigo e testes (Backend)

Este documento define os gates obrigatorios de qualidade do backend.

## Gates obrigatorios

- **Checkstyle**: validacao em `verify`.
- **Cobertura JaCoCo**: minimo de **70% em linhas** (`LINE COVEREDRATIO`) no bundle do backend.
- **Suíte de regressao**: testes criticos marcados com `@RegressionTest`.

## Comandos principais

- Executar gate completo:

```bash
./mvnw -B verify
```

- Executar apenas regressao:

```bash
./mvnw -B -Pregression-tests test
```

## Checkstyle

- Configuracao: `backend/checkstyle/checkstyle.xml`
- Supressoes: `backend/checkstyle/suppressions.xml`

## Cobertura

- Relatorio local: `backend/target/site/jacoco/index.html`
- O CI publica o relatorio como artefato da pipeline.

## Marcacao de regressao

- Anotacao: `backend/src/test/java/br/com/leoferolive/nossalista/support/RegressionTest.java`
- A anotacao equivale a `@Tag("regression")`.
- Usar em testes de funcionalidades criticas (auth, listas, itens, membros e realtime).
