# Design — Login por Magic Link

- **Data:** 2026-07-03
- **Status:** Aprovado (design), pronto para plano de implementação
- **Branch:** `worktree-feat+magic-link-login`
- **Decisão relacionada:** D-024 (a registrar em `docs/DECISIONS.md`)

## 1. Contexto e motivação

A camada de e-mail transacional (commit `0b9ddc9`, 2026-03-29) introduziu os três templates
`password-reset`, `email-verification` e `magic-link`, além do método `EmailService.sendMagicLink`
implementado em `SmtpEmailService` e `ConsoleEmailService`. Os fluxos de **reset de senha** e
**verificação de e-mail** foram integrados ponta a ponta (o segundo em `4d1ffb3`, Q2.7), mas o
**magic link nunca foi conectado**: não existe service, entidade de token, endpoint nem página no
frontend que o dispare. Hoje `sendMagicLink` é **código órfão** — implementado, testado no nível de
envio, mas sem nenhum chamador em produção. O template inclusive aponta para a rota
`/magic-login?token=…`, que **não existe** no frontend.

Este design especifica a implementação do fluxo completo, espelhando o padrão já consolidado dos
outros dois fluxos de token, e finalmente conectando `sendMagicLink`.

## 2. Objetivo

Permitir que um usuário com **conta já existente** faça login sem senha: informa o e-mail, recebe um
link de acesso de uso único (validade 10 min) e, ao clicar, é autenticado (recebe JWT) e tem o
e-mail marcado como verificado.

## 3. Decisões de produto (fechadas no brainstorming)

1. **Escopo: login-only de contas existentes.** Se o e-mail não tem conta, é um no-op silencioso
   (anti-enumeração). Magic link **não** cria conta — cadastro continua por registro e Google OAuth.
2. **Solicitação embutida na superfície de login.** Não há rota nova de solicitação. A superfície de
   login **viva** é o `components/LoginModal.tsx` — a rota `/login` apenas redireciona via
   `LegacyLoginRedirect` para `/?auth=login`, que abre o modal na landing. `pages/Login.tsx` é
   **código morto** (importado só pelo próprio teste) e **não** faz parte do escopo. No `LoginModal`, o
   usuário reusa o campo de e-mail e aciona uma ação **secundária** "Enviar link mágico". O botão
   "Entrar" (senha) permanece o CTA **primário** — a ação de link mágico é visualmente secundária para
   não competir (o CLAUDE.md pede evitar CTAs ambíguos). A página de **consumo** `/magic-login` é uma
   rota nova.
3. **Consumo marca `email_verified=true` e loga.** Clicar no link comprova posse do e-mail (mesma
   prova que a verificação exige). Vale para **qualquer** conta existente, inclusive contas Google/OAuth
   e contas de e-mail ainda não verificadas. Se `REQUIRE_EMAIL_VERIFICATION=true`, o magic link também
   serve para a pessoa destravar a conta.
4. **Rate limits:** iguais aos do reset de senha (ver §5.3).
5. **Token em plaintext no banco:** mantido consistente com `password_reset_tokens` e
   `email_verification` (mesmo nível dos outros dois fluxos). Hashear os três é uma melhoria futura
   separada, fora do escopo.
6. **Escopo de UI restrito ao `LoginModal`.** Como `pages/Login.tsx` é inalcançável (dead code), o
   botão de magic link é adicionado **apenas** ao `LoginModal` (a superfície pública real), evitando
   editar UI morta e produzir testes de falsa confiança.

## 4. Abordagem

Token **stateful** persistido em tabela própria, uso único, com expiração — idêntico ao padrão de
`PasswordResetToken` / `EmailVerificationToken`. Descartada a alternativa *stateless* (o token ser um
JWT curto assinado, sem tabela): dificulta uso-único e invalidação e divergiria dos outros dois
fluxos, aumentando a carga cognitiva de manutenção.

## 5. Design — Backend

### 5.1. Dados e domínio

- **Migration `V15__create_magic_link_tokens.sql`** (V14 já existe — `V14__create_mcp_oauth_registered_clients.sql`;
  V15 é o próximo número livre). Tabela `magic_link_tokens` espelhando **exatamente** o DDL de
  `V8__create_password_reset_tokens.sql`:
  ```sql
  CREATE TABLE magic_link_tokens (
      id UUID PRIMARY KEY,
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      token VARCHAR(255) NOT NULL UNIQUE,
      expires_at TIMESTAMP NOT NULL,
      used BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMP NOT NULL DEFAULT NOW()
  );
  CREATE INDEX idx_magic_link_tokens_token ON magic_link_tokens(token);
  CREATE INDEX idx_magic_link_tokens_user_id ON magic_link_tokens(user_id);
  ```
- **Entidade `MagicLinkToken`** (`auth/domain/`) — cópia estrutural de `PasswordResetToken`
  (`@Table(name = "magic_link_tokens")`, `@PrePersist` gerando `id` e `created_at`).
- **`MagicLinkTokenRepository`** (`auth/repository/`) — métodos:
  - `Optional<MagicLinkToken> findByTokenAndUsedFalse(String token)`
  - `void deleteByUserIdAndUsedFalse(UUID userId)`
- **`InvalidMagicLinkTokenException`** (`auth/exception/`) — espelha `InvalidResetTokenException`.
  Registrar o mapeamento para HTTP 400 em **`config/GlobalExceptionHandler`** (linha ~115), que já
  trata `InvalidResetTokenException` e `InvalidVerificationTokenException` da mesma forma — basta
  adicionar um `@ExceptionHandler` análogo.

### 5.2. Serviço — `MagicLinkService` (`auth/service/`)

- `void requestMagicLink(String email)` — `@Transactional`:
  1. Normaliza (`trim().toLowerCase()`).
  2. `userService.findByEmailOptional(email)`; se vazio → `log.debug` + `return` (no-op,
     anti-enumeração). **Não** filtra por `AuthProvider` (diferente de `resendVerification`): qualquer
     conta existente pode logar por magic link.
  3. `tokenRepository.deleteByUserIdAndUsedFalse(userId)` (limpa tokens não usados anteriores).
  4. Gera `UUID.randomUUID().toString()`, cria `MagicLinkToken` com `expiresAt = now + 10 min`, salva.
  5. `emailService.sendMagicLink(user.getEmail(), user.getName(), tokenValue)` em `try/catch`
     best-effort (falha de SMTP é logada, não propaga) — mesmo padrão dos outros services.
- `User consume(String token)` — `@Transactional`:
  1. `findByTokenAndUsedFalse(token)` → senão `throw InvalidMagicLinkTokenException("Token inválido ou já utilizado")`.
  2. Se `expiresAt.isBefore(now)` → `throw InvalidMagicLinkTokenException("Token expirado")`.
  3. `setUsed(true)`, salva.
  4. `userService.markEmailVerified(userId)` (método já existente; idempotente para quem já é verificado).
  5. Retorna o `User` (o controller emite o JWT).

Constante `MAGIC_LINK_EXPIRATION_MINUTES = 10` — deve coincidir com o valor já usado em
`SmtpEmailService` (o template exibe "expira em 10 minutos"). Manter as duas em sincronia (nota no
código; não há fonte única compartilhada hoje, assim como reset usa 60 em dois lugares).

### 5.3. Endpoints — `AuthController` (`/api/auth`)

| Método | Rota | Corpo | Resposta | Observações |
|--------|------|-------|----------|-------------|
| POST | `/magic-link` | `MagicLinkRequest { email }` | **200 sempre** | Anti-enumeração; rate-limited |
| POST | `/magic-login` | `MagicLoginRequest { token }` | `LoginResponse` (JWT + user) | Emite JWT; rate-limited |

- **DTOs novos** (`auth/dto/`): `MagicLinkRequest(@Email @NotBlank String email)` e
  `MagicLoginRequest(@NotBlank String token)` — records com Bean Validation, espelhando
  `ForgotPasswordRequest` / `ResetPasswordRequest`.
- **`/magic-login` emite o JWT** no mesmo formato de `/login` e `/oauth/exchange`:
  `User user = magicLinkService.consume(token); String jwt = jwtService.generateToken(user);
  LocalDateTime exp = jwtService.getExpirationTime(); return userMapper.toLoginResponse(user, jwt, exp);`
- **Rate limiting** (constantes no controller, via `RateLimiterService` + `ClientIpResolver`, como os
  fluxos existentes):
  - `/magic-link`: **5 por e-mail / 1h** + **15 por IP / 15min** (= `forgot-password`).
  - `/magic-login`: **10 por IP / 15min** (= `reset-password`).
  - Excesso → `RateLimitExceededException` (HTTP 429).

## 6. Design — Frontend

- **`api/authApi.ts`**: `requestMagicLink(email: string): Promise<void>` (POST `/api/auth/magic-link`)
  e `magicLogin(token: string): Promise<OAuthExchangeResponse>` (POST `/api/auth/magic-login`).
  **Não existe** um tipo `LoginResponse` compartilhado no frontend (é uma `interface` privada duplicada
  em `Login.tsx`/`LoginModal.tsx`). O tipo correto a reusar é **`OAuthExchangeResponse`** — já
  exportado em `authApi.ts` e usado por `exchangeOAuthCode`, carregando `token` + `expiresAt` + user,
  exatamente o formato do `LoginResponse` do backend.
- **`components/LoginModal.tsx`** (única superfície de login viva; **não** editar `pages/Login.tsx`):
  adicionar ação secundária **"Enviar link mágico"** que reusa o e-mail digitado. Fluxo: valida que o
  e-mail não está vazio → chama `requestMagicLink` → exibe mensagem genérica *"Se existe uma conta com
  esse e-mail, enviamos um link de acesso."* (sempre, independente de o e-mail existir). "Entrar"
  continua primário; o link mágico é secundário (link/botão-texto). Paridade `light`/`dark` usando os
  primitives/tokens existentes.
- **`pages/MagicLogin.tsx`** (rota nova `/magic-login` registrada em `main.tsx`): espelha
  `pages/AuthCallback.tsx`.
  1. Lê `?token=` da query string.
  2. Estado `loading`: chama `authApi.magicLogin(token)`.
  3. Sucesso: injeta a sessão no `AuthContext` (mesmo mecanismo que o `AuthCallback` usa para o
     `OAuthExchangeResponse` — `login()` + `persistAuthToken`) e redireciona para a Home.
  4. Erro (token ausente/inválido/expirado, ou 400/429): estado de erro com mensagem amigável e link
     para a tela de login.

## 7. Testes (TDD)

**Backend:**
- `MagicLinkServiceTest`:
  - `requestMagicLink` com e-mail existente cria token e chama `sendMagicLink`.
  - `requestMagicLink` com e-mail inexistente é no-op (não cria token, não envia).
  - `requestMagicLink` funciona para conta OAuth (não filtra provider).
  - `consume` válido: marca `used=true`, chama `markEmailVerified`, retorna o `User`.
  - `consume` com token expirado lança `InvalidMagicLinkTokenException`.
  - `consume` com token já usado lança `InvalidMagicLinkTokenException`.
  - Falha de envio de e-mail não propaga (best-effort).
- `AuthController` (camada web, espelhando os testes de forgot/reset):
  - `/magic-link` retorna 200 mesmo para e-mail inexistente.
  - `/magic-link` retorna 429 ao estourar o rate limit (e-mail ou IP).
  - `/magic-login` válido retorna 200 com `LoginResponse` contendo JWT.
  - `/magic-login` com token inválido/expirado retorna 400.
  - `/magic-login` retorna 429 ao estourar o rate limit por IP.
- (Se houver testes de repositório no padrão do projeto) teste de `MagicLinkTokenRepository`.

**Frontend:**
- `MagicLogin.test.tsx`: token válido → chama `magicLogin` → seta sessão → redireciona; token
  ausente/erro → estado de erro + link para login.
- Teste do botão "Enviar link mágico" **no `LoginModal`** (não em `Login.tsx`): chama `requestMagicLink`
  e exibe a mensagem genérica; não vaza se o e-mail existe.

## 8. Documentação (governança obrigatória)

- **`docs/DECISIONS.md`**: registrar **D-024** (login por magic link — escopo login-only, marca
  e-mail verificado, token stateful de 10 min).
- Atualizar a documentação de auth/fluxos de e-mail que enumere os fluxos existentes (localizar a doc
  canônica de autenticação em `docs/**`; incluir o magic link junto de reset e verificação).
- Revisar `backend/.env.example` / `README.md` se listarem os fluxos de e-mail transacional.
- Atualizar `frontend/README.md` se enumerar as rotas/páginas públicas (adicionar `/magic-login`).

## 9. Fora de escopo (YAGNI)

- Cadastro/signup por magic link (decisão: login-only).
- Hashear tokens no banco (melhoria transversal aos três fluxos, tratada à parte).
- Rota dedicada de solicitação (`/magic-link` no frontend) — a solicitação é embutida no login.
- "Lembrar deste dispositivo", múltiplos links simultâneos, ou expiração configurável por ambiente.
- Unificar a fonte da constante de expiração entre service e template (dívida pré-existente análoga ao
  reset; não piora nada).

## 10. Riscos e considerações

- **Sincronia da constante de expiração (10 min)** entre `MagicLinkService` e `SmtpEmailService` — se
  divergirem, o e-mail exibe um tempo errado. Mitigação: comentário cruzado; teste que verifica o
  valor.
- **Mapeamento da exceção para 400** — feito em `config/GlobalExceptionHandler`, que já converte
  `InvalidResetTokenException` / `InvalidVerificationTokenException`; adicionar um `@ExceptionHandler`
  análogo para `InvalidMagicLinkTokenException`.
- **Injeção de sessão no `AuthContext`** — reusar exatamente o caminho do `AuthCallback` para evitar
  divergência de como o JWT é persistido (localStorage vs contexto).
- **Quality gate**: rodar `./scripts/quality.sh --pre-commit` antes de cada commit e
  `npm run format:check` no frontend (o pre-commit não cobre Prettier).
