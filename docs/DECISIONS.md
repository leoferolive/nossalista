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

## D-010 Resolucao de IP do cliente para rate limiting

- **Decisao:** resolver o IP do cliente (chave de rate limit em `forgot-password`, `reset-password`
  e `resend-verification`) via o header confiavel `CF-Connecting-IP`, com fallback seguro para
  `request.getRemoteAddr()`. O header `X-Forwarded-For` deixa de ser usado por ser controlado pelo
  cliente. A logica fica isolada no componente `ClientIpResolver` (`config/`).
- **Motivo:** na topologia `Cloudflare Tunnel -> Traefik -> pod`, o `CF-Connecting-IP` e reescrito
  pela borda Cloudflare e nao e spoofavel pelo cliente. Confiar no primeiro valor do
  `X-Forwarded-For` permitia a um atacante enviar um valor diferente por requisicao e ganhar um
  bucket novo a cada chamada, anulando o rate limit por IP (em especial no brute-force de token do
  `reset-password`, que so tem bucket por IP). Nao adotamos `server.forward-headers-strategy` porque
  ela deriva o IP do proprio `X-Forwarded-For`/`Forwarded`, reintroduzindo o vetor de spoof nesta
  topologia.

## D-011 OAuth2 one-time code (Q2.3)

- **Decisao:** o sucesso do OAuth2 nao coloca mais o JWT na URL de redirect. Emite
  um one-time code opaco (SecureRandom + Base64 URL-safe, 256 bits) guardado
  **no banco** (tabela `oauth_authorization_codes` via `OAuthCodeStore`, TTL 60s,
  single-use, varrido pelo scheduler de cleanup existente) e redireciona para
  `/auth/callback?code=<code>`. O frontend troca o code pelo JWT em
  `POST /api/auth/oauth/exchange` e persiste no `localStorage`.
- **Motivo:** evitar vazamento do JWT em historico do browser, logs de servidor e
  header `Referer`, mantendo a arquitetura `localStorage` existente (sem migrar
  para cookie HttpOnly — decisao Q2.9 do dono).
- **Store persistido (correcao):** originalmente o code vivia in-memory por
  instancia (`ConcurrentHashMap`). Como o code e EMITIDO na requisicao de callback
  do Google e TROCADO numa segunda requisicao (XHR do SPA), com store por
  instancia essas duas requisicoes caindo em pods diferentes (≥1 replica/HPA) — ou
  um restart do pod entre elas — faziam o `oauth/exchange` responder **400** e o
  login Google nunca completar (usuario sem JWT => 401 em tudo). Persistir no banco
  compartilhado (migration `V11`) deixa qualquer instancia validar o code e o fluxo
  sobreviver a restart/escala. Mesmo padrao de `password_reset_tokens`.

## D-012 Verificacao de e-mail no registro (Q2.7)

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

## D-013 Override de versoes para remediar CVEs (gate security-and-compliance)

- **Decisao:** sobrescrever no `backend/pom.xml` duas versoes geridas/transitivas para
  passar o gate OWASP dependency-check (`failBuildOnCVSS=7`), mantendo o
  `spring-boot-starter-parent` em **4.0.6** (nao existe Spring Boot 4.0.7 no Maven Central):
  - `spring-framework` fixado em **7.0.8** via propriedade `<spring-framework.version>7.0.8</spring-framework.version>`
    (mecanismo oficial do BOM do Spring Boot para sobrescrever a versao do Spring Framework
    sem trocar o parent). Corrige **CVE-2026-41842**, **CVE-2026-41850** e **CVE-2026-41851**
    (CVSS 7.5, DoS em recursos estaticos do MVC/WebFlux). 7.0.8 e patch — nao quebra contrato.
  - `async-http-client` fixado em **2.15.0** via `<dependencyManagement>` (vinha em **2.10.4**
    como transitivo de `nl.martijndwars:web-push:5.1.1`). Corrige **CVE-2026-45300**
    (vazamento de Cookie em redirect cross-origin). Mesmo major 2.x, compativel; o
    `async-http-client-netty-utils` acompanha para 2.15.0 automaticamente.
- **Motivo:** desbloquear o gate de seguranca sem subir o parent (indisponivel) nem trocar o
  major do async-http-client.
- **Nota de manutencao:** o override de `spring-framework.version` deve ser **removido** quando
  sair o **Spring Boot 4.0.7+**, que ja gerenciara o Spring Framework 7.0.8 (ou superior) pelo
  proprio parent. O override de `async-http-client` permanece ate `web-push` atualizar seu
  transitivo.

## D-014 Dimensionamento de startupProbe e rollout timeout para o boot lento no Pi ARM

- **Contexto:** o deploy automatico do release v0.0.49 em dev falhou no job `deploy`
  (`kubectl rollout status --timeout=300s` deu *timeout*) embora o pod tenha se estabilizado
  sozinho minutos depois. Investigacao mostrou exit code **143 (SIGTERM)** e 5 restarts no boot.
- **Causa-raiz:** o boot real do Spring Boot 4 / Java 25 no **Raspberry Pi 4 (ARM)** mede
  **~180-195s** ("Started NossaListaApplication in 182.032 seconds"). A janela do `startupProbe`
  era `15 + 10*18 = 195s` (dev) e `60 + 10*18 = 240s` (prod); em dev o container morto rodou
  **195.224s** antes de ficar Ready e o startupProbe o matou por ~0,2s de margem, gerando
  crash-loop. Em paralelo, o `kubectl rollout status --timeout=300s` da CI nao cobria boot lento
  + pull da imagem + ciclos de restart, virando **falso negativo** de deploy.
- **Decisao:**
  - `startupProbe.failureThreshold` **18 → 30** em `k8s/dev` e `k8s/prod` (janela de boot:
    dev **315s**, prod **360s**), dando folga confortavel sobre o pior caso medido (~195s).
    Prod usa `initialDelaySeconds: 60` porque no rollout o pod novo sobe ao lado do antigo
    (maxSurge) competindo por CPU no mesmo no, podendo passar de 240s.
  - `kubectl rollout status --timeout` **300s → 540s** em `deploy-environment.yml` (cobre boot
    + pull + janela do startupProbe de ambos os ambientes).
- **Motivo:** o codigo (CVE bump + PRs de QUESTIONS) roda saudavel em runtime (`/actuator/health`
  = UP em dev com v0.0.49); a falha era de **dimensionamento de infra**, nao regressao. Ajustar
  probe/timeout para a realidade do hardware ARM elimina o crash-loop e o falso negativo de CI.
- **Nota:** se o boot for otimizado (ex.: AOT/CDS, lazy init) e cair bem abaixo de 195s, os
  valores podem ser reduzidos; ate la, manter a folga.

## D-015 EditorConfig check via binario direto + cache (resiliente a pane do api.github.com)

- **Contexto:** durante uma indisponibilidade do `api.github.com` (IP Azure `4.228.31.149`,
  2026-06-10), o job `security-and-compliance` falhou repetidamente no step `EditorConfig check`.
  A action `editorconfig-checker/action-editorconfig-checker@v2` consulta
  `api.github.com/.../releases/latest` em **todo run, sem cache**; com o endpoint fora, dava
  `ETIMEDOUT` → CI vermelha → `release.yml` `skipped` → nenhuma tag nova. Os demais steps que
  baixam de `github.com` (Gitleaks, actions) seguiram funcionando — só a chamada REST quebrava.
- **Decisao:** substituir a action por **download direto do binario** de
  `github.com/editorconfig-checker/editorconfig-checker/releases/download/v${EC_VERSION}/ec-linux-${ARCH}.tar.gz`
  + `actions/cache`, espelhando o padrao ja usado para o Gitleaks. Versao pinada em
  `EC_VERSION` (env do workflow, hoje `3.7.0`); `ARCH` via `dpkg --print-architecture` (arm64).
- **Motivo:** `github.com/releases/download` (objects.githubusercontent.com) e independente do
  `api.github.com`, entao o step passa mesmo durante panes da API e fica cacheado. Remove um
  ponto unico de falha externo do gate de CI.
- **Config explicita (`.editorconfig-checker.json`):** o binario `ec` cru tem defaults mais
  estritos que a action (a action efetivamente desabilitava o check de `IndentSize` e ignorava
  conteudo vendored/gerado). Sem config, `ec` v3.7.0 acusava **476 violacoes** pre-existentes no
  checkout (461 `IndentSize` "multiple of 2" em `_bmad/`, `_bmad-output/`, `backend/`, `docs/`;
  + 15 de line-ending/newline em `_bmad/` e `.quality-baseline/`). Para reproduzir o comportamento
  verde anterior, foi adicionado `.editorconfig-checker.json` na raiz: `Disable.IndentSize=true`
  (check ruidoso em markdown/yaml, ja tolerado antes) e `Exclude` de `^_bmad/`, `^_bmad-output/`,
  `^\.quality-baseline/` (artefatos vendored/gerados, nao canonicos — `_bmad-output/**` ja e
  declarado nao-canonico no CLAUDE.md). Com isso `ec` valida o checkout com **0 violacoes**.
- **Manutencao:** bumpar `EC_VERSION` periodicamente (a action fazia isso implicitamente com
  `latest`). Violacoes reais de estilo em codigo canonico (`backend/`, `frontend/`, `docs/`) que
  nao sejam `IndentSize` continuam barrando a CI — a config so neutraliza o ruido conhecido.

## D-016 CI em runner GitHub-hosted (deploy permanece self-hosted)

- **Contexto:** o `ci.yml` rodava todos os jobs em `runs-on: [self-hosted, linux, ARM64]`
  (runner `leo-ubuntu-nossalista`, no proprio Raspberry Pi). Quando esse runner ficou
  **offline**, os jobs de CI ficaram presos em `queued` indefinidamente — o check obrigatorio
  `security-and-compliance` nunca completava e **bloqueava o merge** de qualquer PR (branch
  protection da `main`). Ponto unico de falha: sem o Pi ligado, nenhum PR avanca.
- **Decisao:** mover **apenas o `ci.yml`** (jobs `changes`, `frontend-quality`, `frontend-e2e`,
  `backend-quality`, `security-and-compliance`) para `runs-on: ubuntu-latest` (GitHub-hosted).
  Os workflows de **deploy** (`deploy-environment.yml` e orquestradores) **permanecem
  self-hosted ARM64** — precisam de acesso de rede ao cluster K3s local e do build nativo ARM.
- **Motivo:** a CI (testes, lint, build, scans) nao depende de arquitetura ARM nem de acesso ao
  cluster — `actions/setup-node@v4` e `actions/setup-java@v4` (Temurin 25) instalam as
  toolchains no runner hospedado, e os downloads de binarios ja se adaptam via
  `dpkg --print-architecture` (retorna `amd64` no ubuntu-latest). Tirar a CI do runner caseiro
  remove o acoplamento entre disponibilidade do Pi e a capacidade de revisar/mergear codigo.
- **Notas:**
  - Caches do `actions/cache` tem chave por `runner.arch`: a troca ARM64 → X64 gera um
    cache-miss unico na primeira execucao (Maven/NVD/Playwright re-populam), sem quebra.
  - Para voltar a usar o runner self-hosted (ex.: economizar minutos hospedados), basta
    reverter `runs-on` para `[self-hosted, linux, ARM64]` nos jobs do `ci.yml` com o runner online.
