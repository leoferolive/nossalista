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

Total no levantamento inicial: **66 violações** distribuídas em **43 arquivos**.
As regras infringidas são `complexity`, `max-lines-per-function` e `max-lines`.

| Domínio | Arquivos afetados |
|---|---|
| Mock / config | `mock/mockServer.ts`, `vite.config.ts` |
| Components | `ActivityTimeline`, `AppHeader`, `AuthLayout`, `CreateListModal`, `DeleteConfirmModal`, `DeleteListModal`, `EditItemModal`, `EditListNameModal`, `InviteModal`, `ItemOptionsMenu`, `ListCard`, `ListItem`, `LoginModal`, `MembersModal`, `ModalShell`, `NotificationBell`, `OnboardingTourOverlay`, `RegisterModal`, `ResponsiveActionMenu`, `ResponsiveSheet`, `UserProfile` |
| Contexts | `AuthContext`, `NotificationContext`, `OnboardingContext`, `WebSocketContext` |
| Hooks | `useActivities`, `useItems`, `useLists`, `usePushNotifications` |
| Pages | `AuthCallback`, `ForgotPassword`, `Home`, `JoinListPage`, `LandingPage`, `ListView`, `Login`, `Profile`, `Register`, `ResetPassword` |
| Types | `WebSocketMessage` |

**TODO até:** 2026-08-31. A lista canônica de arquivos é o bloco de override
em `frontend/eslint.config.js` (último bloco do array exportado).

### Plano de refatoração sugerido

- **Modais e páginas grandes** (`ListView.tsx`, `JoinListPage.tsx`, `AppHeader.tsx`):
  extrair sub-componentes de seção (header, body, footer) e mover lógica de
  estado para hooks customizados (`useListView`, `useListMembers`, ...).
- **Hooks de fetch** (`useItems`, `useLists`, `useActivities`): quebrar o
  `useEffect` central em handlers menores ou usar `useReducer`.
- **`mock/mockServer.ts`**: já é um arquivo de mock — aceitável crescer,
  mas pode ser fatiado por domínio (`mock/lists.ts`, `mock/items.ts`, ...).
- **`WebSocketMessage.ts` (`parseListWebSocketMessage`)**: substituir o
  `switch` gigante por um mapa `type → parser`.

---

## Remediação de CVEs — gate `security-and-compliance` (registrada em 2026-06-10)

> **Nota (2026-07-06):** o SCA de dependências migrou do OWASP dependency-check/NVD
> para o **OSV-Scanner** (ver **D-027** e issue #70). Os overrides de versão abaixo
> continuam válidos (corrigem o CVE na dependência resolvida, independente do scanner);
> as menções ao "dependency-check"/"Feed NVD" nesta seção são registro histórico.

Override de versões no `backend/pom.xml` para passar o gate de vulnerabilidades. Detalhe
arquitetural em `docs/DECISIONS.md` (D-013, D-019).

| Dependência | De → Para | Mecanismo no pom | CVEs corrigidas | Status |
|---|---|---|---|---|
| `org.springframework:spring-framework` | gerido pelo Boot 4.0.6 → **7.0.8** | propriedade `<spring-framework.version>` | CVE-2026-41842, CVE-2026-41850, CVE-2026-41851 (CVSS 7.5, DoS recursos estáticos MVC/WebFlux) | **Resolvido em 2026-07-02** — override removido; Spring Boot 4.0.7 passou a gerir 7.0.8 nativamente pelo parent (D-019) |
| `org.asynchttpclient:async-http-client` | 2.10.4 (transitivo de `web-push:5.1.1`) → **2.16.1** | `<dependencyManagement>` | CVE-2026-45300 (vazamento de Cookie em redirect cross-origin) | Pendente — remover quando `web-push` atualizar o transitivo |

- `async-http-client-netty-utils` sobe junto (dependência interna do próprio
  `async-http-client`) — não precisa de entrada extra. Atualizado para 2.16.1
  em 2026-08-10 (rotina semanal de dependências, patch/minor).
- Validação (2026-06-10): `dependency:tree` confirma `spring-core:7.0.8`, `spring-web:7.0.8`,
  `async-http-client:2.16.1` e `async-http-client-netty-utils:2.16.1`, sem
  resíduo asynchttpclient em 2.10.4. Suite de testes: 484 testes, 0 falhas.

### Bump para Spring Boot 4.0.7 (registrado em 2026-07-02, ver D-019)

Feed NVD passou a reportar CVSS ≥ 7.0 em dependências geridas pelo BOM do Spring Boot 4.0.6
(nenhuma delas adicionada pelo PR que disparou o alerta): `jackson-databind` 2.21.2 /
`tools.jackson.core:jackson-databind` 3.1.2 (CVE-2026-54512, CVE-2026-54513) e
`spring-security-{core,web,oauth2-core,config,crypto}` 7.0.5 (CVE-2026-40988, CVE-2026-40993).

Resolvido subindo `spring-boot-starter-parent` de **4.0.6** para **4.0.7**, que já gerencia
`jackson-2-bom` 2.21.4, `jackson-bom` 3.1.4 e `spring-security.version` 7.0.6 — sem overrides
individuais. Validação: `dependency:tree` confirma `jackson-databind:2.21.4`,
`tools.jackson.core:jackson-databind:3.1.4`, `spring-security-*:7.0.6`. Suite de testes completa
sem regressão (ver resultado real no PR #50).

---

## Frontend npm audit — GHSA-qwww-vcr4-c8h2 (registrada em 2026-07-28)

`npm audit --omit=dev` no frontend reporta **2 high** — ambos o mesmo advisory,
propagado de `react-router` para `react-router-dom`:

| Advisory | Pacote | Range afetado | Severidade | Status |
|---|---|---|---|---|
| [GHSA-qwww-vcr4-c8h2](https://github.com/advisories/GHSA-qwww-vcr4-c8h2) — RSC Mode CSRF Bypass | `react-router` / `react-router-dom` | `>=7.12.0 <8.3.0` | High (CVSS 7.1, CWE-352) | Aceito temporariamente — sem fix não-breaking disponível |

**Por que aceito e não corrigido:**

- O advisory só afeta quem usa as **APIs instáveis de RSC** (React Server
  Components) do react-router — feature opt-in. O frontend do NossaLista usa
  `<BrowserRouter>` clássico (`src/main.tsx`) e não referencia nenhuma API
  `unstable_RSC*` em nenhum lugar do código (SPA via Vite, sem framework RSC).
  **Não é explorável nesta aplicação.**
- `package-lock.json` já está travado na versão mais recente publicada
  (`7.18.1`); a versão corrigida (`8.3.0`) ainda não foi lançada no registry
  no momento do registro desta dívida. O único "fix" que
  `npm audit fix --force` oferece hoje é downgrade para `7.11.0`, uma
  regressão real sem ganho de segurança.

**Mecanismo de exceção:** o job `security-and-compliance` (`.github/workflows/ci.yml`,
step "Frontend audit") não usa mais `npm audit --audit-level=high` puro — passou a
filtrar via `jq`, ignorando apenas o advisory source `1124282` (GHSA-qwww-vcr4-c8h2).
Qualquer outro high/critical continua bloqueando o CI normalmente.

**Como reduzir esta dívida:** quando `react-router-dom@8.3.0` (ou uma versão
`7.x` patched) for publicada, rodar `npm update react-router react-router-dom`,
confirmar `npm audit --omit=dev` limpo, remover o filtro `jq` do step "Frontend
audit" (voltar para `npm audit --audit-level=high --omit=dev` puro) e apagar
esta seção.

---

## Como reduzir a dívida

1. Refatore o arquivo.
2. Remova a entrada correspondente do `violationSuppressXPath` em
   `backend/pmd/ruleset.xml` (regra afetada). Os XPath são qualificados por
   `ClassDeclaration[@SimpleName='...']` para evitar colisão de nomes entre
   service e controller (ex.: `updateItem`, `inviteByUsername`).
3. Rode `./scripts/quality.sh --full` e confirme exit 0.
4. Commit em PR separado, com referência a esta linha da dívida.
