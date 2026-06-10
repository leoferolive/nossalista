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

Referencia de exemplo:
- `backend/.env.example`

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
