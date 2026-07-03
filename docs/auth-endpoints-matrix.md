# Matriz de Autenticacao de Endpoints

Esta matriz registra os endpoints consumidos pelo frontend atual e o contrato esperado de autenticacao.

## Endpoints publicos intencionais

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | fluxo de cadastro | Publico; dispara e-mail de verificacao (Q2.7) |
| `POST` | `/api/auth/login` | `frontend/src/components/LoginModal.tsx` (landing), `frontend/src/pages/Login.tsx` (legado sem rota principal) | Publico; 403 quando `app.auth.require-email-verification=true` e conta EMAIL nao-verificada (Q2.7) |
| `GET` | `/api/auth/google` | `frontend/src/components/LoginModal.tsx`, `frontend/src/pages/Login.tsx` (legado), `frontend/src/pages/JoinListPage.tsx` | Publico |
| `POST` | `/api/auth/oauth/exchange` | `frontend/src/pages/AuthCallback.tsx` via `authApi.exchangeOAuthCode` | Publico; troca one-time code OAuth2 por JWT (Q2.3). Code single-use, TTL 60s |
| `GET` | `/api/auth/verify-email?token=` | `frontend/src/pages/VerifyEmail.tsx` via `authApi.verifyEmail` | Publico; consome token e marca `email_verified=true` (Q2.7) |
| `POST` | `/api/auth/resend-verification` | `authApi.resendVerification` | Publico; rate-limited (3/email/h, 10/IP/15min); sempre 200 (anti-enumeracao) (Q2.7) |
| `POST` | `/api/auth/forgot-password` | `authApi.forgotPassword` | Publico; rate-limited; sempre 200 (anti-enumeracao) |
| `POST` | `/api/auth/reset-password` | `authApi.resetPassword` | Publico; rate-limited; consome token de reset |
| `GET` | `/api/health` | monitoramento e auditoria operacional | Publico; retorna status, version, gitSha, gitTag, environment e buildTime |
| `GET` | `/api/lists/join/{inviteCode}` | `frontend/src/api/listsApi.ts` | Publico para preview do convite |
| `WS` | `/ws/**` | `frontend/src/api/websocket.ts` | Handshake permissivo; autenticacao exigida no CONNECT STOMP |

## Endpoints autenticados

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | `frontend/src/contexts/AuthContext.tsx`, `frontend/src/api/usersApi.ts` | Requer JWT |
| `PATCH` | `/api/users/me` | `frontend/src/api/usersApi.ts` | Requer JWT |
| `POST` | `/api/users/me/onboarding/complete` | `frontend/src/contexts/OnboardingContext.tsx` | Requer JWT |
| `POST` | `/api/users/me/tokens` | `frontend/src/api/tokensApi.ts` | Requer JWT de sessao normal — PAT nao pode acessar (403) |
| `GET` | `/api/users/me/tokens` | `frontend/src/api/tokensApi.ts` | Requer JWT de sessao normal — PAT nao pode acessar (403) |
| `DELETE` | `/api/users/me/tokens/{id}` | `frontend/src/api/tokensApi.ts` | Requer JWT de sessao normal — PAT nao pode acessar (403); idempotente; 404 se de outro usuario |
| `GET` | `/api/users/search` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}/state` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `PATCH` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `DELETE` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{id}/invite-link` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/join/{inviteCode}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{id}/invite` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}/members` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `DELETE` | `/api/lists/{id}/members/{userId}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{id}/leave` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}/activity` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{listId}/items` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{listId}/items` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `PATCH` | `/api/lists/{listId}/items/{itemId}/check` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `PATCH` | `/api/lists/{listId}/items/{itemId}` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `DELETE` | `/api/lists/{listId}/items/{itemId}` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `POST` | `/mcp` | clientes MCP externos (claude.ai, Claude Code/Desktop, Cursor) | Requer JWT, PAT (`nlmcp_...`) ou access token OAuth do MCP. Enforcement de escopo READ e por tool, nao por metodo HTTP — ver `docs/mcp.md` e `docs/DECISIONS.md` (D-020, D-022) |

## Endpoints do servidor de autorizacao OAuth do MCP (Fase C, D-022)

| Metodo | Endpoint | Contrato |
| --- | --- | --- |
| `GET` | `/oauth/authorize` | Publico. `client_id`/`redirect_uri` invalidos -> 400 direto (nunca redirect); demais erros de validacao (PKCE, resource, scope) -> redirect `?error=` para o `redirect_uri`. Sucesso -> redirect para a tela de consentimento da SPA |
| `GET` | `/api/oauth/consent/{requestId}` | Requer JWT de sessao normal — PAT/token OAuth nao podem acessar (403). Retorna os dados do pedido pendente para a tela de consentimento |
| `POST` | `/api/oauth/consent/{requestId}/approve` | Requer JWT de sessao normal. Emite o authorization code e devolve `{redirectUrl}` |
| `POST` | `/api/oauth/consent/{requestId}/deny` | Requer JWT de sessao normal. Devolve `{redirectUrl}` com `error=access_denied` |
| `POST` | `/oauth/token` | Publico (`application/x-www-form-urlencoded`). `grant_type=authorization_code` (+ PKCE) ou `refresh_token` (rotacao obrigatoria). Erros no formato OAuth padrao (`{"error", "error_description"}`, nao RFC 7807) |
| `POST` | `/oauth/revoke` | Publico. Revoga a familia inteira do refresh token informado; sempre 200 (RFC 7009 — nunca revela se o token era valido) |
| `GET` | `/api/oauth/connections` | Requer JWT de sessao normal. Lista assistentes conectados via OAuth |
| `POST` | `/api/oauth/connections/{clientId}/revoke` | Requer JWT de sessao normal. Desconecta um assistente (revoga a familia inteira) |
| `GET` | `/.well-known/oauth-authorization-server` | Publico (RFC 8414) |
| `GET` | `/.well-known/oauth-protected-resource` | Publico (RFC 9728) |

## Observacoes

- O endpoint de atividades agora existe no backend e atende o contrato paginado esperado pelo frontend.
- `POST /api/auth/login` e `GET /api/users/me` retornam `onboardingCompletedAt` para controlar o tutorial de primeiro login por conta.
- O frontend centraliza auth na landing (`/`) e usa query params (`auth`, `registered`, `email`) para abrir modal e prefill.
- `/login` permanece apenas para compatibilidade de links antigos e redireciona para `/?auth=login`.
- **OAuth2 one-time code (Q2.3):** o `OAuth2SuccessHandler` NAO coloca mais o JWT na URL. Ele emite um code opaco (SecureRandom + Base64 URL-safe, 256 bits) guardado **no banco** (tabela `oauth_authorization_codes` via `OAuthCodeStore`, TTL 60s, single-use, varrido pelo scheduler de cleanup) e redireciona para `/auth/callback?code=<code>`. O frontend troca o code pelo JWT em `POST /api/auth/oauth/exchange` e persiste no `localStorage` como antes (arquitetura `localStorage` mantida — decisao Q2.9). Isso evita vazamento do JWT em historico do browser, logs e header `Referer`. **O store e persistido (nao in-memory)**: o code e emitido na requisicao de callback do Google e trocado numa segunda requisicao (XHR do SPA); com store por instancia, essas duas requisicoes caindo em pods diferentes (≥1 replica) ou um restart entre elas faziam o `oauth/exchange` responder 400 e o login Google nunca completar. Ver `docs/DECISIONS.md` (D-011).
- **Verificacao de e-mail (Q2.7):** registro EMAIL gera token (`email_verification_tokens`, validade 24h) e envia e-mail (`email-verification.html`). `GET /api/auth/verify-email?token=` consome o token e marca `email_verified=true`. Usuarios Google entram com `email_verified=true` (e-mail ja verificado pelo provedor). O enforcement estrito de login e configuravel via `app.auth.require-email-verification` (default `false`) — ver `docs/ENVIRONMENT.md` e `docs/DECISIONS.md`.
- **Personal Access Tokens (PAT):** alem do JWT de sessao, `/api/**` aceita `Authorization: Bearer nlmcp_...` — tokens de longa duracao gerenciados pelo proprio usuario em "Conexoes (API/Assistentes)" no menu da conta (`frontend/src/pages/Connections.tsx`), pensados para clientes MCP/API externos. Diferencas em relacao ao JWT: (1) um PAT de escopo `READ` so pode usar metodos HTTP seguros (GET/HEAD/OPTIONS) — qualquer mutacao responde `403`; (2) um PAT, de qualquer escopo, nunca acessa `/api/auth/**` nem `/api/users/me/tokens/**` (`403`); (3) token revogado ou expirado responde `401`. Ver `docs/DECISIONS.md` (D-018).
- **Servidor MCP (`/mcp`):** aceita JWT, PAT ou access token OAuth do MCP, mas a regra de escopo READ/READ_WRITE e aplicada dentro de cada tool (`McpSecurityContext`), nao pelo metodo HTTP — todo o protocolo MCP trafega via `POST`. Ver `docs/mcp.md` e `docs/DECISIONS.md` (D-020).
- **OAuth 2.1 do servidor MCP (Fase C):** claude.ai e Claude Code conectam via Authorization Code + PKCE (S256 obrigatorio) em vez de copiar um PAT manualmente. Clientes registrados estaticamente (sem DCR); consentimento na SPA (`/oauth/consent`, autenticado por JWT de sessao); access token JWT assinado com `MCP_OAUTH_SIGNING_KEY` (chave propria, nunca `JWT_SECRET`), audience validada (RFC 8707); refresh token com rotacao e deteccao de reuso; escopo OAuth vale SOMENTE para `/mcp`, nunca para `/api/**`. Ver `docs/mcp.md` e `docs/DECISIONS.md` (D-022).
