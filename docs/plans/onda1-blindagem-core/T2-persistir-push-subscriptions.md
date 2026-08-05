# T2 · Persistir push subscriptions

**Branch:** `feat/onda1-t2-persistir-push` · **Origem:** P1 (push in-memory)

## Problema

`PushSubscriptionStore` é um `ConcurrentHashMap` in-memory (sem tabela/migration). Como cada
deploy substitui o pod (`replicas: 1`), **toda inscrição de push é apagada a cada release** e
push notifications param silenciosamente até o usuário reabrir o app e reinscrever. Também não
sobrevive a mais de uma réplica.

## Objetivo

Persistir as inscrições de push no PostgreSQL (via Flyway + JPA), mantendo a **API pública do
store idêntica**, para que as inscrições sobrevivam a restart/deploy.

## Escopo (arquivos que esta tarefa PODE tocar)

- `backend/src/main/java/br/com/leoferolive/nossalista/push/**` (o pacote inteiro)
  - `PushSubscriptionStore.java` — passa a delegar a um repository JPA, **preservando as assinaturas públicas**.
  - Nova entity (ex.: `PushSubscription.java`) + novo `PushSubscriptionRepository.java`.
- `backend/src/main/resources/db/migration/V*__create_push_subscriptions.sql` (novo — próximo número livre)
- `backend/src/test/java/.../push/**` — testes novos/ajustados

**NÃO** editar `PushNotificationService.java` (é de T3) — mantenha a assinatura pública do store
para que `PushNotificationService` continue compilando sem alteração. Não tocar outros pacotes,
`application.yml` nem `pom.xml`.

## Passos

1. Ler `PushSubscriptionStore.java` e todos os seus chamadores (`grep -r PushSubscriptionStore`)
   para inventariar a **API pública exata** (métodos, tipos de retorno) que deve permanecer estável.
2. Modelar a entity `PushSubscription`: chave, `user_id`, `endpoint`, chaves `p256dh`/`auth`,
   timestamps. Índice por `user_id` (o store hoje busca por usuário) e unicidade por `endpoint`.
3. Migration Flyway nova criando a tabela `push_subscriptions` (confira o próximo `V*` livre).
4. `PushSubscriptionRepository extends JpaRepository` com os finders necessários (por `user_id`, por `endpoint`).
5. Refatorar `PushSubscriptionStore` para delegar ao repository, **sem mudar sua interface pública**
   (é um adaptador). Remover o `ConcurrentHashMap`.
6. Testes: salvar → recuperar por usuário; substituir inscrição por `endpoint` (upsert); remover.
   Idealmente um teste que prove persistência entre "sessões" (repository).

## Critérios de aceite

- [ ] Tabela `push_subscriptions` criada por migration Flyway; Flyway `validate` no boot passa.
- [ ] Entity + repository JPA; `PushSubscriptionStore` delega ao repo com **API pública inalterada**.
- [ ] `PushNotificationService` e demais chamadores compilam **sem edição**.
- [ ] Testes cobrindo persistência (save/find-by-user/remove/upsert por endpoint).
- [ ] `./scripts/quality.sh --pre-commit` verde.
- [ ] Documentação impactada atualizada (governança do `CLAUDE.md`): registrar em `docs/DECISIONS.md`
      a migração do store in-memory → tabela (alinhado ao padrão D-011).

## Restrições de commit

- **Sem** atribuição de IA nos commits (regra do `CLAUDE.md`). Mensagens em PT-BR.
