# T2 · WebSocket na cobertura

**Branch:** `feat/onda2-t2-websocket-cobertura` · **Origem:** P0-3 (camada real-time excluída da métrica)

> **Merge serializado:** esta tarefa compartilha `pom.xml` e `.quality-baseline/backend.json`
> com a T1. O orquestrador vai pedir rebase sobre a T1 já mergeada antes do merge desta.

## Problema

`backend/pom.xml:389` exclui `websocket/**` do JaCoCo e `:462` do Pitest. Os 86%/77%
reportados **não incluem** a camada real-time — a mais sensível a concorrência. Existem
arquivos de teste de WS que não contam para o gate nem para o ratchet.

## Objetivo

Trazer a camada `websocket/**` para dentro da métrica de cobertura (JaCoCo e Pitest),
adicionando os testes necessários para o gate continuar verde, e recalibrar o baseline.

## Escopo (arquivos que PODE tocar)

- `backend/pom.xml` — **apenas a config de plugins** (remover/estreitar o `<exclude>` de `websocket/**` no JaCoCo, linha ~389; e o `<param>` no Pitest, ~462). Estreitar em vez de remover tudo é aceitável (ex.: excluir só DTOs/gerados), mas o publisher/interceptor/controllers de WS DEVEM entrar. NÃO tocar a seção de dependências (T1).
- `backend/src/test/java/.../websocket/**` — adicionar/reforçar testes para cobrir o publisher, interceptor e controllers de WS até o gate passar.
- `.quality-baseline/backend.json` — recalibrar o baseline após a inclusão.

**NÃO** editar: dependências do `pom.xml` (T1), `ListItemService.java` (T3), `docs/DECISIONS.md`/`quality-gate*.md` (orquestrador). Justificativa no corpo do PR.

## Passos

1. Ver o que `websocket/**` contém e o que já tem teste (a Onda-0 mapeou ~6 arquivos de teste de WS existentes).
2. Estreitar o `<exclude>` do JaCoCo para deixar entrar publisher/interceptor/controllers (excluir só DTOs/records triviais se necessário). Idem no Pitest.
3. Rodar cobertura (`./scripts/quality.sh --full` ou o goal jacoco) e ver o que falta; adicionar testes de comportamento (não mocks vazios) até o gate de 80%/75% passar na camada incluída.
4. Recalibrar `.quality-baseline/backend.json` para o novo número real (para cima — o ratchet exige não-decréscimo).
5. `./scripts/quality.sh --pre-commit` e a suíte verde.

## Critérios de aceite

- [ ] `websocket/**` (publisher/interceptor/controllers) dentro da métrica JaCoCo e Pitest.
- [ ] Testes adicionados cobrem comportamento real; gate 80%/75% passa com a camada incluída.
- [ ] `.quality-baseline/backend.json` recalibrado (não-decréscimo respeitado).
- [ ] `backend-quality` verde no CI.
- [ ] Justificativa no corpo do PR (orquestrador não registra D novo — é mudança de gate; a prosa vai no quality-gate consolidado).

## Restrições de commit

- Sem atribuição de IA. Mensagens em PT-BR.
