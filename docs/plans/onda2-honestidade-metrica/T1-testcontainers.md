# T1 · Testcontainers-PostgreSQL

**Branch:** `feat/onda2-t1-testcontainers` · **Origem:** P0-2 (integração testada em H2, nunca em Postgres)

## Problema

Todos os `@SpringBootTest`/repositório rodam em H2 (`MODE=PostgreSQL`). As migrations Flyway
são validadas **só contra H2**, não contra o Postgres de produção — classe de bug "passa no
teste, quebra em prod" (locking, SQL nativo, tipos, collation).

## Objetivo

Introduzir Testcontainers-PostgreSQL de forma **aditiva (opt-in)** e apontar os testes mais
sensíveis ao banco (repositório, validação de migration, `McpServerIntegrationTest`) para um
Postgres real, mantendo o resto da suíte em H2 (rápido).

## Escopo (arquivos que PODE tocar)

- `backend/pom.xml` — **apenas a seção de `<dependencies>`** (adicionar `org.testcontainers:postgresql` + `junit-jupiter`, scope test; e `spring-boot-testcontainers`). NÃO tocar a config de plugins (jacoco/pitest é da T2).
- Nova base class de teste, ex.: `backend/src/test/java/.../support/AbstractPostgresIT.java` (container singleton reutilizável + `@ServiceConnection`).
- Os testes que serão migrados para Postgres: testes de repositório (`*RepositoryTest`), um teste de validação de migration, `McpServerIntegrationTest`, e (bônus) os testes de concorrência da Onda 1 (`ListItemOptimisticLockingTest`/`SharedListOptimisticLockingTest`) — fortalecê-los contra Postgres real.
- Recursos de teste específicos do container (se necessário, um `application-testcontainers.yml` próprio — NÃO alterar o `src/test/resources/application.yml` global do H2, para não afetar T2/T3).

**NÃO** editar: config de plugins do `pom.xml` (T2), `.quality-baseline/*` (T2 cuida do backend), `ListItemService.java` (T3), `docs/DECISIONS.md`/`quality-gate*.md` (orquestrador consolida). Ponha a justificativa da decisão (Testcontainers) no corpo do PR — o orquestrador registra o D-031.

## Passos

1. Confirmar Docker disponível (`docker info`) — está.
2. Adicionar dependências Testcontainers ao `pom.xml` (só dependencies).
3. Criar `AbstractPostgresIT` com container **singleton** (padrão static + start manual ou singleton container) e `@ServiceConnection` (Spring Boot 3.1+/4) para autoconfigurar o datasource; Flyway roda as migrations reais no container.
4. Migrar os testes-alvo para estender/ativar a base Postgres. Garantir isolamento entre testes (limpeza ou `@Transactional`/truncate) para não vazar estado no container compartilhado.
5. Rodar `./mvnw test` (o CI e o local têm Docker). A suíte inteira deve ficar verde; os testes migrados devem subir o container e validar as migrations no Postgres real.
6. `./scripts/quality.sh --pre-commit` verde.

## Critérios de aceite

- [ ] Testcontainers-PostgreSQL configurado de forma aditiva (base class opt-in); default H2 global inalterado.
- [ ] Testes de repositório + validação de migration + `McpServerIntegrationTest` rodam contra Postgres real (Flyway aplica as 18 migrations no container sem erro).
- [ ] (Bônus) testes de concorrência da Onda 1 rodando em Postgres.
- [ ] Suíte completa verde; `./scripts/quality.sh --pre-commit` verde; CI (`backend-quality`) verde (tem Docker).
- [ ] Justificativa da decisão no corpo do PR (o orquestrador registra o D-031).

## Notas

- Não converter a suíte inteira — só os testes sensíveis ao banco. Container singleton para não pagar boot por classe.
- Se o CI precisar de ajuste (ex.: garantir Docker/serviço), documente no PR; o runner ubuntu-latest já tem Docker.

## Restrições de commit

- Sem atribuição de IA (regra do CLAUDE.md). Mensagens em PT-BR.
