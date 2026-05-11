# Quality Gate — Dívida Técnica

Este documento lista violações **pré-existentes** detectadas pelo quality gate
unificado e formalmente reconhecidas como dívida técnica. Cada item tem um
prazo para refatoração; até lá, o arquivo correspondente é excluído do
ruleset bloqueante.

> **Política:** novos commits não podem **adicionar** violações. Apenas reduzir
> ou refatorar. As exclusões temporárias abaixo devem ser removidas quando o
> arquivo for refatorado.

## Backend (PMD) — registrada em 2026-05-11

Total no levantamento inicial: **13 violações** distribuídas em **4 arquivos**.

| Arquivo | Regras infringidas | Métodos / contexto | TODO até |
|---|---|---|---|
| `backend/src/main/java/br/com/leoferolive/nossalista/auth/OAuth2SuccessHandler.java` | CyclomaticComplexity | `onAuthenticationSuccess` | 2026-08-31 |
| `backend/src/main/java/br/com/leoferolive/nossalista/listitem/service/ListItemService.java` | CyclomaticComplexity (classe + 3 métodos), CognitiveComplexity (3 métodos), NPathComplexity, NcssCount | `updateItem`, `validateDynamicFieldsByType`, `validateUpdateFields` e a própria classe | 2026-08-31 |
| `backend/src/main/java/br/com/leoferolive/nossalista/member/service/MemberService.java` | CyclomaticComplexity | `inviteByUsername` | 2026-08-31 |
| `backend/src/main/java/br/com/leoferolive/nossalista/websocket/WebSocketSubscriptionInterceptor.java` | CyclomaticComplexity, CognitiveComplexity | `preSend` | 2026-08-31 |

### Plano de refatoração sugerido

- **`OAuth2SuccessHandler.onAuthenticationSuccess`**: extrair o branching
  (login novo vs. login existente, com/sem username) em métodos privados ou em
  uma estratégia separada.
- **`ListItemService.updateItem` / `validateDynamicFieldsByType` /
  `validateUpdateFields`**: a lógica de "campos dinâmicos por tipo de lista"
  cresceu além do limite. Extrair validadores polimórficos por
  `ListType` (cada tipo conhece seus campos), reduzindo o `switch` central.
- **`MemberService.inviteByUsername`**: cadeia de validações encadeadas pode
  virar pipeline (`Optional` / `Result`) ou pré-checagens em helpers.
- **`WebSocketSubscriptionInterceptor.preSend`**: o `switch` por comando
  STOMP pode ser quebrado em handlers por tipo (`SUBSCRIBE`, `SEND`, ...).

## Frontend (ESLint) — registrada em 2026-05-11

A definir após Tarefa 2.

---

## Como reduzir a dívida

1. Refatore o arquivo.
2. Remova o `<exclude-pattern>` correspondente em `backend/pmd/ruleset.xml`.
3. Rode `./scripts/quality.sh --full` e confirme exit 0.
4. Commit em PR separado, com referência a esta linha da dívida.
