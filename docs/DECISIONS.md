# Decisoes Arquiteturais (Resumo)

## D-001 Monorepo

- **Decisao:** manter frontend e backend no mesmo repositorio.
- **Motivo:** mudancas coordenadas de API/tempo real e simplificacao operacional.

## D-002 Autenticacao

- **Decisao:** JWT stateless + Google OAuth2 (principal) + email/senha (fallback).
- **Motivo:** compatibilidade com SPA e WebSocket autenticado.

## D-003 Sincronizacao real-time

- **Decisao:** STOMP sobre SockJS em `/ws/**`.
- **Motivo:** suporte nativo no ecossistema Spring e padrao de pub/sub por lista.

## D-004 Dados de itens por tipo de lista

- **Decisao:** colunas nullable para campos opcionais no MVP.
- **Motivo:** simplicidade inicial; possibilidade de migracao futura para JSONB.

## D-005 Qualidade backend

- **Decisao:** gate com Checkstyle + JaCoCo >= 70% linhas + suite de regressao.
- **Motivo:** manter baseline tecnico minimo durante evolucao do MVP.

## D-006 CI bloqueante no PR

- **Decisao:** tornar todos os gates de qualidade, seguranca, cobertura e build bloqueantes em PR.
- **Motivo:** impedir regressao funcional/arquitetural e reduzir risco de deploy com falhas conhecidas.

## D-007 Cobertura minima atualizada

- **Decisao:** frontend com threshold global >= 80% (lines/branches/functions/statements) e backend com >= 80% line + >= 75% branch.
- **Motivo:** elevar padrao tecnico com controle de regressao por no-decrease entre branch base e branch do PR.

## D-008 Onboarding inicial por conta

- **Decisao:** tutorial guiado no primeiro login com persistencia por conta (`users.onboarding_completed_at`), endpoint idempotente de conclusao e replay manual no menu da conta.
- **Motivo:** reduzir friccao para novos usuarios sem repetir onboarding em todos os acessos/dispositivos.

## D-009 Estrategia E2E em duas camadas

- **Decisao:** separar E2E Playwright em duas suites com tags:
  - `@pr`: deterministic/mockado, bloqueante no PR.
  - `@fullstack`: navegador + backend real, execucao noturna e manual.
- **Motivo:** elevar cobertura de fluxo critico sem estourar tempo de feedback no PR.

## D-010 OAuth2 one-time code (Q2.3)

- **Decisao:** o sucesso do OAuth2 nao coloca mais o JWT na URL de redirect. Emite
  um one-time code opaco (SecureRandom + Base64 URL-safe, 256 bits) guardado
  in-memory (`OAuthCodeStore`, TTL 60s, single-use, varrido pelo scheduler de
  cleanup existente) e redireciona para `/auth/callback?code=<code>`. O frontend
  troca o code pelo JWT em `POST /api/auth/oauth/exchange` e persiste no
  `localStorage`.
- **Motivo:** evitar vazamento do JWT em historico do browser, logs de servidor e
  header `Referer`, mantendo a arquitetura `localStorage` existente (sem migrar
  para cookie HttpOnly — decisao Q2.9 do dono).

## D-011 Verificacao de e-mail no registro (Q2.7)

- **Decisao:** registro email/senha gera token de verificacao
  (`email_verification_tokens`, validade 24h) e envia e-mail
  (`email-verification.html`). `GET /api/auth/verify-email?token=` consome o token
  e marca `users.email_verified=true`; `POST /api/auth/resend-verification`
  reenvia (rate-limited). Usuarios Google entram com `email_verified=true`.
- **Decisao (gating configuravel):** o enforcement estrito de login e controlado
  por `app.auth.require-email-verification` (default `false`). Desligado: status
  registrado, login nao bloqueado. Ligado: login de conta EMAIL nao-verificada
  retorna 403. O gating nunca afeta contas Google.
- **DECISAO PENDENTE DO DONO:** ligar o enforcement estrito so e seguro apos
  decidir o tratamento das contas pre-existentes (todas com `email_verified=false`
  apos a migracao `V10`): backfill explicito para `true`, campanha de
  reverificacao, ou manter desligado. A migracao **nao** desloga ninguem por si so
  (default desligado).
- **Motivo:** reduzir abuso/contas falsas sem quebrar contas legadas nem usuarios
  OAuth no momento da implantacao.
