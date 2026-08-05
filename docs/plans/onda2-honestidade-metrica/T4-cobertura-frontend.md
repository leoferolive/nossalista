# T4 · Honestidade de cobertura frontend + contrato WS

**Branch:** `feat/onda2-t4-cobertura-frontend` · **Origem:** P1 (excludes inflados do vitest) + P1 (contrato WS só do produtor)

## Problema

1. `frontend/vitest.config.ts` exclui `src/pages/**` e `src/api/**` inteiros (além de vários
   componentes/hooks), embora **já existam** testes de página e de api — os ~89% de cobertura
   são medidos sobre denominador reduzido e o ratchet não protege essas áreas.
2. O parser do frontend (`src/types/WebSocketMessage.test.ts`) **hardcoda** expectativas e nunca
   lê `contracts/websocket-envelope-v2.json` — o frontend pode divergir do contrato em silêncio.

## Objetivo

Tornar a métrica de cobertura frontend honesta (remover excludes que já têm teste) e amarrar o
teste do parser WS ao arquivo de contrato canônico.

## Escopo (arquivos que PODE tocar)

- `frontend/vitest.config.ts` — remover `src/api/**` e `src/pages/**` do `exclude` (eles têm testes). Para os hooks/componentes excluídos individualmente que **não** têm teste (ex.: `useActivities.ts`, `useLists.ts`, `useWebSocket.ts`), OU adicionar teste, OU mantê-los excluídos com um comentário justificando — mas `src/api` e `src/pages` devem sair do exclude.
- `frontend/src/**` — adicionar os testes necessários para o gate/ratchet continuar verde após a inclusão de `api`/`pages`.
- `frontend/src/types/WebSocketMessage.test.ts` — passar a **ler** `contracts/websocket-envelope-v2.json` e derivar as asserções dele (`actorRequiredEventTypes`, `notificationEventTypes`, `revisionRequiredChannels`), em vez de hardcodar.
- `.quality-baseline/frontend.json` — recalibrar após a mudança de denominador.

**NÃO** tocar backend, nem `docs/DECISIONS.md`/`quality-gate*.md` (orquestrador consolida a prosa). Justificativa no corpo do PR.

## Passos

1. Remover `src/api/**` e `src/pages/**` do `exclude` do `vitest.config.ts`.
2. Rodar `npm run test:coverage` e ver o que caiu abaixo do limite; adicionar testes de comportamento nas áreas recém-incluídas até o gate passar (priorizar `src/api` — client axios/handleApiError — e páginas com pouca cobertura).
3. Reescrever `WebSocketMessage.test.ts` para importar/ler o JSON de contrato e asseverar contra ele (o teste deve falhar se o contrato e o parser divergirem).
4. Recalibrar `.quality-baseline/frontend.json`.
5. `npm run lint`, `npm run typecheck`, `npm run test:coverage` e `./scripts/quality.sh --pre-commit` verdes.

## Critérios de aceite

- [ ] `src/api/**` e `src/pages/**` fora do `exclude`; gate/ratchet verdes com o novo denominador.
- [ ] `WebSocketMessage.test.ts` lê `contracts/websocket-envelope-v2.json` e falharia se o parser divergir.
- [ ] `.quality-baseline/frontend.json` recalibrado (não-decréscimo).
- [ ] `frontend-quality` verde no CI.
- [ ] Justificativa no corpo do PR.

## Restrições de commit

- Sem atribuição de IA. Mensagens em PT-BR.
