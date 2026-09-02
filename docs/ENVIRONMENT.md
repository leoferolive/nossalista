# Variaveis de Ambiente

## Backend

As principais variaveis sao lidas em `backend/src/main/resources/application.yml` e perfis.

Obrigatorias em producao:
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

> **`JWT_SECRET` e fail-fast (obrigatorio em todos os ambientes):** `application.yml`
> nao tem mais default para `jwt.secret`. Na inicializacao, `JwtService` valida o
> secret e **a aplicacao nao sobe** (lanca `IllegalStateException`) se o secret
> estiver ausente/vazio, for o placeholder de exemplo, ou tiver menos de 32 bytes
> (256 bits, minimo do HS256). Gere um secret aleatorio com pelo menos 32 bytes
> (ver `docs/deploy-guide/06-secrets.md`). Os perfis de teste (`application-test.yml`,
> `application-ci.yml` e `src/test/resources/application.yml`) ja trazem um secret
> de teste valido.

Importantes (com default em alguns cenarios):
- `DATABASE_URL` (default: `jdbc:postgresql://localhost:5432/nossalista`)
- `DATABASE_USER` (default: `nossalista`)
- `FRONTEND_URL` (default: `http://localhost:5173`)
- `REQUIRE_EMAIL_VERIFICATION` -> `app.auth.require-email-verification` (default: `false`)
- `MCP_OAUTH_SIGNING_KEY`, `MCP_OAUTH_ISSUER`, `MCP_OAUTH_RESOURCE` — ver secao dedicada abaixo

> **`MCP_OAUTH_SIGNING_KEY` e fail-fast (obrigatoria em todos os ambientes):**
> assina os access tokens OAuth do servidor de autorizacao do MCP (Fase C —
> ver `docs/DECISIONS.md` D-022 e `docs/mcp.md`). `McpOAuthJwtService` valida o
> secret na inicializacao e **a aplicacao nao sobe** se estiver ausente ou tiver
> menos de 32 bytes (256 bits, minimo do HS256) — mesmo mecanismo de
> `JWT_SECRET`, porem com uma chave PROPRIA e obrigatoriamente DIFERENTE (nunca
> reuse o valor de `JWT_SECRET`: seriam dois tipos de token com o mesmo
> segredo). Os perfis de teste/CI ja trazem uma chave de teste valida.
>
> `MCP_OAUTH_ISSUER` e `MCP_OAUTH_RESOURCE` tem default de desenvolvimento
> (`http://localhost:8080` e `http://localhost:8080/mcp`) e sao sobrescritos
> com o dominio real em `application-prod.yml` (mesmo padrao de `FRONTEND_URL`).
> `MCP_OAUTH_RESOURCE` e a audience (RFC 8707) validada em todo access token
> OAuth apresentado a `/mcp` — um token emitido para outro resource e rejeitado
> com `401`.

> **`MCP_OAUTH_DCR_ENABLED` (Fase C.1 — Dynamic Client Registration, D-024):**
> liga/desliga `POST /oauth/register` e o anuncio de `registration_endpoint`
> no discovery (`/.well-known/oauth-authorization-server`). Default `true`.
> Quando `false`, o endpoint responde `403 {"error":"access_denied"}` e o
> discovery para de anunciar o endpoint — util para desativar DCR num
> ambiente sem desativar o resto do OAuth. Demais parametros de DCR
> (rate limit por IP, teto de clientes registrados, TTL de cliente nunca
> usado) nao tem env var dedicada nesta fase — ajustar via
> `app.mcp-oauth.dcr.*` em `application*.yml` se necessario.

> **`REQUIRE_EMAIL_VERIFICATION` (Q2.7) — enforcement de verificacao de e-mail:**
> Controla se o login email/senha bloqueia contas EMAIL ainda nao verificadas.
> Default `false`: o status `email_verified` e registrado, mas o login **nao** e
> bloqueado — evita deslogar contas pre-existentes (criadas antes da feature) e
> nao afeta usuarios Google (que entram com `email_verified=true`). Quando `true`,
> login de conta EMAIL nao-verificada retorna **403** (ProblemDetail orientando a
> verificar o e-mail). **Decisao pendente do dono:** so ligue o enforcement estrito
> depois de decidir o tratamento das contas pre-existentes (backfill de
> `email_verified` ou campanha de reverificacao) — ver `docs/DECISIONS.md`.

Referencia de exemplo:
- `backend/.env.example`

## Cookie de sessao e CSRF

`app.auth.session-cookie.*` fica em `application*.yml`, nao em secret de ambiente:

- `prod`: `name=__Host-nl_session`, `secure=true`, `same-site=Lax`, `Path=/`, sem `Domain`. A aplicacao falha no boot se esse profile nao estiver nessa configuracao segura.
- `dev` e `test`: `name=nl_session`, `secure=false`, para permitir HTTP local. Nunca promover esse override para producao.
- A duracao do cookie e `jwt.expiration` (atualmente 7 dias).
- `XSRF-TOKEN` nao e credencial: e legivel pela SPA e deve acompanhar mutacoes autenticadas pela sessao como `X-XSRF-TOKEN`. Os transportes MCP/OAuth e `/ws/**` sao excluidos do CSRF de sessao.

Em producao, configure no Cloudflare uma regra de Redirect (ou Always Use HTTPS) para `http://nossalista.leoferolive.com.br/*` -> HTTPS, status **308**, antes do deploy da aplicacao. Nao habilite `server.forward-headers-strategy`: ver D-010, pois isso reabriria spoof de IP por headers forwarded.

## Frontend

No estado atual, o frontend usa a configuracao padrao de consumo da API definida no codigo.
Se novos `VITE_*` forem introduzidos, registrar aqui como obrigatorios/opcionais.

## Seguranca

- Nunca commitar `.env` com secrets reais.
- Em PRs, mascarar valores sensiveis em logs e capturas.

### Autorizacao (RBAC)

- O role do usuario (`USER`/`ADMIN`, enum `Role`) e propagado para as authorities
  do Spring Security pelo `JwtAuthenticationFilter` como `ROLE_USER` / `ROLE_ADMIN`.
- Method security esta habilitado (`@EnableMethodSecurity`): endpoints podem ser
  protegidos com `@PreAuthorize("hasRole('ADMIN')")`. Nao ha funcionalidade admin
  exposta no MVP; a infraestrutura de authorities apenas fica pronta para uso.
- Negacao de autorizacao retorna **403 Forbidden** em RFC 7807 (Problem Details),
  via `Http403AccessDeniedHandler` (cadeia de filtros) e do handler de
  `AccessDeniedException` no `GlobalExceptionHandler` (method security em MVC).
  Falta de autenticacao continua retornando **401**.
