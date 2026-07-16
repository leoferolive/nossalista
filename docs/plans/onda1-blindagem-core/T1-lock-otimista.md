# T1 · Lock otimista em listas/itens

**Branch:** `feat/onda1-t1-lock-otimista` · **Origem:** P0-1 (concorrência em listas compartilhadas)

## Problema

O core do produto é edição simultânea por múltiplos membros, mas nenhuma entidade tem
controle de concorrência (`grep @Version` no `src/main` = vazio). Dois membros editando/
marcando o mesmo item podem causar **lost update** silencioso. O campo `revision` é token
de ordenação de broadcast, **não** guarda de conflito de escrita.

## Objetivo

Transformar escrita concorrente conflitante em **erro detectável (HTTP 409)** em vez de
sobrescrita silenciosa, via optimistic locking do JPA.

## Escopo (arquivos que esta tarefa PODE tocar)

- `backend/src/main/java/br/com/leoferolive/nossalista/listitem/domain/ListItem.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/list/domain/SharedList.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/common/exception/GlobalExceptionHandler.java`
- `backend/src/main/resources/db/migration/V*__add_optimistic_lock_version.sql` (novo — usar o próximo número livre)
- `backend/src/test/java/.../` — novo teste de concorrência

**NÃO** editar `ListItemService.java`, `notification/**`, `push/**`, `application.yml`,
`pom.xml` (são de outras tarefas). Se achar que precisa, PARE e registre no PR como follow-up.

## Passos

1. Ler as duas entidades para seguir o estilo (Lombok? getters manuais? JPA annotations existentes).
2. Adicionar `@Version` (coluna `version`, tipo `Long`/`bigint`) em `ListItem` e `SharedList`.
3. Migration Flyway nova (confira o próximo `V*` livre com `ls backend/src/main/resources/db/migration`):
   adicionar coluna `version bigint not null default 0` em `list_items` e `lists`.
4. No `GlobalExceptionHandler`, mapear `org.springframework.orm.ObjectOptimisticLockingFailureException`
   (e/ou `jakarta.persistence.OptimisticLockException`) para **409 Conflict** em formato RFC 7807
   (seguir o padrão dos handlers existentes — mesmo `ProblemDetail`/estrutura), com mensagem clara
   tipo "o item foi alterado por outra pessoa; recarregue e tente novamente".
5. Teste de concorrência novo: provar que edição concorrente do mesmo item/lista dispara o conflito.
   Padrão sugerido: carregar a entidade em dois contextos/transações separados, salvar ambos,
   asseverar que o segundo lança `ObjectOptimisticLockingFailureException`. O teste deve **falhar**
   se o `@Version` for removido.

## Critérios de aceite

- [ ] `@Version` presente e funcional em `ListItem` e `SharedList`.
- [ ] Migration adiciona `version` nas duas tabelas; `./scripts/quality.sh --full` / boot valida (Flyway `validate`).
- [ ] `GlobalExceptionHandler` retorna 409 RFC 7807 para o conflito de lock.
- [ ] Teste de concorrência que comprova detecção de lost update (falharia sem `@Version`).
- [ ] `./scripts/quality.sh --pre-commit` verde.
- [ ] Documentação impactada atualizada (governança do `CLAUDE.md`): se houver, uma linha em `docs/DECISIONS.md` ou `docs/quality-gate*.md` sobre o novo comportamento 409.

## Notas / limitações conhecidas

- Testes rodam em **H2** nesta onda (Testcontainers é Onda 2). Documente no PR que o teste de
  concorrência é indicativo e será fortalecido contra PostgreSQL real na Onda 2.
- A race de `position` no `add_items` **não** é resolvida aqui (exigiria tocar `ListItemService`,
  conflitando com T3) — fica registrada para a Onda 2 com `@Retryable`.

## Restrições de commit

- **Sem** `Co-Authored-By: Claude` ou qualquer atribuição de IA (regra do `CLAUDE.md`).
- Commits pequenos e descritivos, mensagem em PT-BR.
