# Matriz de Autenticacao de Endpoints

Esta matriz registra os endpoints consumidos pelo frontend atual e o contrato esperado de autenticacao.

## Endpoints publicos intencionais

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | fluxo de cadastro | Publico; dispara e-mail de verificacao (Q2.7) |
| `POST` | `/api/auth/login` | `frontend/src/components/LoginModal.tsx` (landing), `frontend/src/pages/Login.tsx` (legado sem rota principal) | Publico; emite cookie de sessao HttpOnly e retorna somente perfil; 403 quando `app.auth.require-email-verification=true` e conta EMAIL nao-verificada (Q2.7) |
| `GET` | `/api/auth/google` | `frontend/src/components/LoginModal.tsx`, `frontend/src/pages/Login.tsx` (legado), `frontend/src/pages/JoinListPage.tsx` | Publico; responde `302 Location: /oauth2/authorization/google` relativo |
| `POST` | `/api/auth/oauth/exchange` | `frontend/src/pages/AuthCallback.tsx` via `authApi.exchangeOAuthCode` | Publico; troca one-time code por cookie de sessao HttpOnly e perfil. Code single-use, TTL 60s |
| `GET` | `/api/auth/csrf` | interceptor Axios | Publico; materializa `XSRF-TOKEN`, `Cache-Control: no-store` |
| `POST` | `/api/auth/logout` | `AuthContext` via `usersApi.logout` | Expira o cookie de sessao; mutacao com sessao exige `X-XSRF-TOKEN` |
| `GET` | `/api/auth/verify-email?token=` | `frontend/src/pages/VerifyEmail.tsx` via `authApi.verifyEmail` | Publico; consome token e marca `email_verified=true` (Q2.7) |
| `POST` | `/api/auth/resend-verification` | `authApi.resendVerification` | Publico; rate-limited (3/email/h, 10/IP/15min); sempre 200 (anti-enumeracao) (Q2.7) |
| `POST` | `/api/auth/forgot-password` | `authApi.forgotPassword` | Publico; rate-limited; sempre 200 (anti-enumeracao) |
| `POST` | `/api/auth/reset-password` | `authApi.resetPassword` | Publico; rate-limited; consome token de reset |
| `POST` | `/api/auth/magic-link` | `frontend/src/components/LoginModal.tsx` via `authApi.requestMagicLink` | Publico; rate-limited (5/e-mail/1h + 15/IP/15min); sempre 200 (anti-enumeracao); 400 em corpo invalido (D-025) |
| `POST` | `/api/auth/magic-login` | `frontend/src/pages/MagicLogin.tsx` via `authApi.magicLogin` | Publico; rate-limited (10/IP/15min); consome token (uso unico, 10min), marca `email_verified=true`, emite cookie de sessao HttpOnly e retorna somente perfil; 400 se token invalido/expirado/usado (D-025) |
| `GET` | `/api/health` | monitoramento e auditoria operacional | Publico; retorna status, version, gitSha, gitTag, environment e buildTime |
| `GET` | `/api/lists/join/{inviteCode}` | `frontend/src/api/listsApi.ts` | Publico para preview do convite |
| `WS` | `/ws/**` | `frontend/src/api/websocket.ts` | Handshake exige sessao cookie; `CONNECT` herda o usuario e nao aceita JWT |

## Endpoints autenticados

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | `frontend/src/contexts/AuthContext.tsx`, `frontend/src/api/usersApi.ts` | Requer sessao web por cookie |
| `PATCH` | `/api/users/me` | `frontend/src/api/usersApi.ts` | Requer sessao web por cookie |
| `POST` | `/api/users/me/onboarding/complete` | `frontend/src/contexts/OnboardingContext.tsx` | Requer sessao web por cookie |
| `POST` | `/api/users/me/tokens` | `frontend/src/api/tokensApi.ts` | Requer sessao web por cookie; PAT/access token OAuth MCP nao podem acessar — PAT nao pode acessar (403) |
| `GET` | `/api/users/me/tokens` | `frontend/src/api/tokensApi.ts` | Requer sessao web por cookie; PAT/access token OAuth MCP nao podem acessar — PAT nao pode acessar (403) |
| `DELETE` | `/api/users/me/tokens/{id}` | `frontend/src/api/tokensApi.ts` | Requer sessao web por cookie; PAT/access token OAuth MCP nao podem acessar — PAT nao pode acessar (403); idempotente; 404 se de outro usuario |
| `GET` | `/api/users/search` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `POST` | `/api/lists` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `GET` | `/api/lists` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `GET` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `GET` | `/api/lists/{id}/state` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `PATCH` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `DELETE` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `POST` | `/api/lists/{id}/invite-link` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `POST` | `/api/lists/join/{inviteCode}` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `POST` | `/api/lists/{id}/invite` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `GET` | `/api/lists/{id}/members` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `DELETE` | `/api/lists/{id}/members/{userId}` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `POST` | `/api/lists/{id}/leave` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `GET` | `/api/lists/{id}/activity` | `frontend/src/api/listsApi.ts` | Requer sessao web por cookie |
| `POST` | `/api/lists/{listId}/items` | `frontend/src/api/itemsApi.ts` | Requer sessao web por cookie |
| `GET` | `/api/lists/{listId}/items` | `frontend/src/api/itemsApi.ts` | Requer sessao web por cookie |
| `PATCH` | `/api/lists/{listId}/items/{itemId}/check` | `frontend/src/api/itemsApi.ts` | Requer sessao web por cookie |
| `PATCH` | `/api/lists/{listId}/items/{itemId}` | `frontend/src/api/itemsApi.ts` | Requer sessao web por cookie |
| `DELETE` | `/api/lists/{listId}/items/{itemId}` | `frontend/src/api/itemsApi.ts` | Requer sessao web por cookie |
| `POST` | `/mcp` | clientes MCP externos (claude.ai, Claude Code/Desktop, Cursor) | Requer cookie de sessao web, PAT (`nlmcp_...`) ou access token OAuth do MCP; JWT de sessao em Bearer e rejeitado. Enforcement de escopo READ e por tool, nao por metodo HTTP — ver `docs/mcp.md` e `docs/DECISIONS.md` (D-020, D-022) |

## Endpoints do servidor de autorizacao OAuth do MCP (Fase C, D-022)

| Metodo | Endpoint | Contrato |
| --- | --- | --- |
| `GET` | `/oauth/authorize` | Publico. `client_id`/`redirect_uri` invalidos -> 400 direto (nunca redirect); demais erros de validacao (PKCE, resource, scope) -> redirect `?error=` para o `redirect_uri`. Sucesso -> redirect para a tela de consentimento da SPA |
| `GET` | `/api/oauth/consent/{requestId}` | Requer sessao web por cookie; PAT e access token OAuth MCP nao podem acessar (403). Retorna os dados do pedido pendente para a tela de consentimento |
| `POST` | `/api/oauth/consent/{requestId}/approve` | Requer sessao web por cookie; PAT/access token OAuth MCP nao podem acessar. Emite o authorization code e devolve `{redirectUrl}` |
| `POST` | `/api/oauth/consent/{requestId}/deny` | Requer sessao web por cookie; PAT/access token OAuth MCP nao podem acessar. Devolve `{redirectUrl}` com `error=access_denied` |
| `POST` | `/oauth/token` | Publico (`application/x-www-form-urlencoded`). `grant_type=authorization_code` (+ PKCE) ou `refresh_token` (rotacao obrigatoria). Erros no formato OAuth padrao (`{"error", "error_description"}`, nao RFC 7807) |
| `POST` | `/oauth/revoke` | Publico. Revoga a familia inteira do refresh token informado; sempre 200 (RFC 7009 — nunca revela se o token era valido) |
| `POST` | `/oauth/register` | Publico (Dynamic Client Registration, RFC 7591 — Fase C.1, D-024). Endurecido: rate limit por IP (10/h padrao), `redirect_uris` deve ser https ou loopback com porta explicita, teto global de clientes. Erros no formato OAuth padrao. Desativavel via `app.mcp-oauth.dcr.enabled` |
| `GET` | `/api/oauth/connections` | Requer sessao web por cookie; PAT/access token OAuth MCP nao podem acessar. Lista assistentes conectados via OAuth |
| `POST` | `/api/oauth/connections/{clientId}/revoke` | Requer sessao web por cookie; PAT/access token OAuth MCP nao podem acessar. Desconecta um assistente (revoga a familia inteira) |
| `GET` | `/.well-known/oauth-authorization-server` | Publico (RFC 8414). Anuncia `registration_endpoint` quando `app.mcp-oauth.dcr.enabled=true` (default) |
| `GET` | `/.well-known/oauth-protected-resource` | Publico (RFC 9728) |

## Observacoes

- O endpoint de atividades agora existe no backend e atende o contrato paginado esperado pelo frontend.
- `POST /api/auth/login` e `GET /api/users/me` retornam `onboardingCompletedAt` para controlar o tutorial de primeiro login por conta.
- O frontend centraliza auth na landing (`/`) e usa query params (`auth`, `registered`, `email`) para abrir modal e prefill.
- `/login` permanece apenas para compatibilidade de links antigos e redireciona para `/?auth=login`.
- **OAuth2 one-time code (Q2.3):** `OAuth2SuccessHandler` nao coloca JWT na URL. Emite code opaco de uso unico (256 bits, banco, TTL 60s) e redireciona para `/auth/callback?code=<code>`. `POST /api/auth/oauth/exchange` grava o JWT somente no cookie HttpOnly; a SPA recebe apenas perfil. O store persistido permite troca em pods diferentes. Ver `docs/DECISIONS.md` (D-011).
- **Verificacao de e-mail (Q2.7):** registro EMAIL gera token (`email_verification_tokens`, validade 24h) e envia e-mail (`email-verification.html`). `GET /api/auth/verify-email?token=` consome o token e marca `email_verified=true`. Usuarios Google entram com `email_verified=true` (e-mail ja verificado pelo provedor). O enforcement estrito de login e configuravel via `app.auth.require-email-verification` (default `false`) — ver `docs/ENVIRONMENT.md` e `docs/DECISIONS.md`.
- **Login por magic link (D-025):** `POST /api/auth/magic-link` gera um token opaco (`magic_link_tokens`, migration V15, validade 10min, uso unico) para uma conta EXISTENTE (qualquer `AuthProvider`, inclusive Google) e envia por e-mail; NUNCA cria conta nova. `POST /api/auth/magic-login` consome o token, marca `email_verified=true`, emite o cookie de sessao HttpOnly e retorna somente o perfil. UI: acao secundaria "Entrar com link magico" no `LoginModal` e pagina publica `/magic-login` para consumir o link. Ver `docs/DECISIONS.md` (D-025).
- **Personal Access Tokens (PAT):** alem do cookie de sessao, `/api/**` aceita `Authorization: Bearer nlmcp_...` — tokens de longa duracao gerenciados pelo proprio usuario em "Conexoes (API/Assistentes)" no menu da conta (`frontend/src/pages/Connections.tsx`), pensados para clientes MCP/API externos. Diferencas em relacao ao JWT: (1) um PAT de escopo `READ` so pode usar metodos HTTP seguros (GET/HEAD/OPTIONS) — qualquer mutacao responde `403`; (2) um PAT, de qualquer escopo, nunca acessa `/api/auth/**` nem `/api/users/me/tokens/**` (`403`); (3) token revogado ou expirado responde `401`. Ver `docs/DECISIONS.md` (D-018).
- **Servidor MCP (`/mcp`):** aceita cookie de sessao web, PAT ou access token OAuth do MCP, mas a regra de escopo READ/READ_WRITE e aplicada dentro de cada tool (`McpSecurityContext`), nao pelo metodo HTTP — todo o protocolo MCP trafega via `POST`. Ver `docs/mcp.md` e `docs/DECISIONS.md` (D-020).
- **OAuth 2.1 do servidor MCP (Fase C):** claude.ai e Claude Code conectam via Authorization Code + PKCE (S256 obrigatorio) em vez de copiar um PAT manualmente. Consentimento na SPA (`/oauth/consent`, autenticado por cookie de sessao); access token JWT assinado com `MCP_OAUTH_SIGNING_KEY` (chave propria, nunca `JWT_SECRET`), audience validada (RFC 8707); refresh token com rotacao e deteccao de reuso; escopo OAuth vale SOMENTE para `/mcp`, nunca para `/api/**`. Ver `docs/mcp.md` e `docs/DECISIONS.md` (D-022).
- **Dynamic Client Registration (Fase C.1):** `POST /oauth/register` complementa os clientes estaticos (`claude-ai`/`claude-code`) — destrava o "Add connector" do claude.ai sem configuracao manual. `McpOAuthClientRegistry` resolve estaticos e dinamicos de forma transparente para authorize/token (estaticos tem precedencia por id); um cliente dinamico passa pelas MESMAS protecoes de consentimento (cookie + trava por usuario) que um estatico. Ver `docs/mcp.md` e `docs/DECISIONS.md` (D-024).

## Contrato de sessao web

- O JWT de sessao nao aparece no JSON, `localStorage`, query string ou `Authorization`. Em producao ele e `__Host-nl_session` (`Secure; HttpOnly; SameSite=Lax; Path=/`, sem `Domain`); dev/teste usam `nl_session` nao-Secure apenas para HTTP local.
- Mutacoes autenticadas pela sessao exigem `XSRF-TOKEN` + `X-XSRF-TOKEN`. Axios obtem o valor por `GET /api/auth/csrf`; os transportes `/ws/**`, `/mcp/**`, `/oauth/token`, `/oauth/revoke` e `/oauth/register` ficam fora desse mecanismo.
- WebSocket/SockJS usa o cookie no handshake e o frame STOMP `CONNECT` nao carrega JWT.
