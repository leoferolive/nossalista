# T3 · Race de `position` (UNIQUE + retry)

**Branch:** `feat/onda2-t3-race-position` · **Origem:** P1 (add concorrente corrompe ordem) + follow-up da Onda 1

## Problema

`ListItemService` calcula `position` via `findMaxPositionByListId + save`. Dois `add_items`
concorrentes na mesma lista leem o mesmo `maxPosition` e gravam itens com `position`
duplicada. A Onda 1 adiou isso (exigia tocar `ListItemService`, que colidia com o T3 da Onda 1).

## Objetivo

Tornar a ordenação robusta a inserções concorrentes: constraint de unicidade + retry que
recalcula a posição, transformando a race em operação correta (não em erro nem em corrupção).

## Escopo (arquivos que PODE tocar)

- `backend/src/main/resources/db/migration/V18__unique_list_item_position.sql` (novo — confira que `V18` é o próximo número real; o `ls` não ordena numérico, o max atual é `V17`).
- `backend/src/main/java/br/com/leoferolive/nossalista/listitem/service/ListItemService.java` — retry manual (sem `spring-retry`, que não está no pom) em torno da inserção: ao pegar `DataIntegrityViolationException` na constraint de posição, recalcular `maxPosition` e tentar de novo (com um teto de tentativas).
- Novo teste (concorrência) provando que dois adds concorrentes resultam em posições distintas, sem exceção propagada ao chamador.

**NÃO** tocar: `pom.xml` (use retry manual, não adicione dependência), `docs/DECISIONS.md`/`quality-gate*.md` (orquestrador registra o D-032), entidades (a coluna `position` já existe). Justificativa no corpo do PR.

## Passos

1. Migration: `ALTER TABLE list_items ADD CONSTRAINT uq_list_items_list_position UNIQUE (list_id, position);` — **atenção**: se houver posições duplicadas pré-existentes, a constraint falha; a migration deve normalizar antes (ex.: renumerar por `id`/`created_at` dentro de cada lista) e então criar a constraint. Valide contra Postgres (idealmente após rebase sobre a T1/Testcontainers) e H2.
2. `ListItemService`: encapsular o cálculo de posição + insert num pequeno loop de retry (ex.: até 3–5 tentativas) que, ao violar a unicidade, relê `maxPosition` e reinsere. Fora do teto, propaga erro claro.
3. Teste: simular concorrência (`ExecutorService`/`CountDownLatch`) de vários adds na mesma lista e asseverar posições únicas e contíguas, sem erro ao chamador.
4. `./mvnw test` e `./scripts/quality.sh --pre-commit` verdes.

## Critérios de aceite

- [ ] Migration `V18` cria `UNIQUE(list_id, position)` (com normalização de duplicatas pré-existentes, se houver).
- [ ] Retry manual no `ListItemService` resolve a race sem propagar erro ao chamador (dentro do teto).
- [ ] Teste de concorrência de add prova posições únicas.
- [ ] Suíte verde; `backend-quality` verde.
- [ ] Justificativa no corpo do PR (orquestrador registra D-032).

## Notas

- Independente da T1/T2 em arquivos (não toca pom nem baseline). Se rebasear sobre a T1 (Testcontainers) já mergeada, aproveite para rodar o teste de concorrência contra Postgres real.

## Restrições de commit

- Sem atribuição de IA. Mensagens em PT-BR.
