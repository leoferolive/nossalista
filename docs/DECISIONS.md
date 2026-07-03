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
- **Nota de manutencao:** o override de `spring-framework.version` foi **removido em 2026-07-02**
  com o bump do parent para **Spring Boot 4.0.7** — ver D-019. O override de `async-http-client`
  permanece ate `web-push` atualizar seu transitivo.

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

## D-016 CI em runner GitHub-hosted (deploy migrado depois — ver D-017)

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
  - **Download do Gitleaks:** o asset x86_64 do Gitleaks (GoReleaser) chama-se `x64`, nao
    `amd64`; `dpkg --print-architecture` retorna `amd64` no GitHub-hosted, entao o step mapeia
    `amd64 → x64` (arm64 ja casava). O EditorConfig (`ec-linux-amd64`) ja usava o nome correto.
  - **Baseline de cobertura:** o gate `no-decrease` compara contra um cache medido no ambiente
    anterior (self-hosted ARM). A medicao da V8/Node difere por fracoes entre ARM e x64, gerando
    falso-negativo na primeira comparacao apos a migracao. Mitigacao one-time: invalidar o cache
    `base-frontend-coverage-<sha>` obsoleto; o proximo push na `main` re-mede a baseline em
    `ubuntu-latest` e PRs seguintes voltam a comparar no mesmo ambiente.
  - Para voltar a usar o runner self-hosted (ex.: economizar minutos hospedados), basta
    reverter `runs-on` para `[self-hosted, linux, ARM64]` nos jobs do `ci.yml` com o runner online.

## D-017 Deploy (dev e prod) em runner GitHub-hosted via Tailscale + QEMU

- **Contexto:** apos D-016, a CI ja rodava em `ubuntu-latest`, mas todos os workflows de **deploy**
  (`deploy-environment.yml` e orquestradores: `deploy-branch-dev`, `deploy-on-tag`, `deploy-prod`,
  `rollback-prod`, `release`) ainda exigiam o runner self-hosted ARM64 do Pi. Com o runner
  offline/instavel, o deploy ficava preso na fila — inclusive o **auto-deploy em dev** disparado
  por `release.yml` apos cada merge na `main`. O acoplamento "deploy depende do Pi ligado"
  bloqueava tanto restaurar dev quanto promover prod.
- **Decisao:** mover **todos** os jobs de deploy para `runs-on: ubuntu-latest`, resolvendo os dois
  acoplamentos que prendiam o deploy ao Pi:
  - **Acesso ao cluster:** o kube-apiserver do K3s nao e exposto na internet; o `KUBECONFIG` ja
    aponta para o **IP Tailscale** do no. O job `deploy` entra na tailnet via
    `tailscale/github-action@v2` (secret `TAILSCALE_AUTHKEY`, ja existente) antes do `kubectl`.
  - **Build ARM:** o `Dockerfile` ja e cross-build — os stages de compilacao (`node`, `maven`)
    usam `FROM --platform=$BUILDPLATFORM` e rodam nativos em x86; so o stage 3 (runtime
    `eclipse-temurin:25-jre` arm64, que faz `apt-get curl` + copia o jar) e arm64. Adicionado
    `docker/setup-qemu-action@v3` no `build-and-push` para o binfmt do stage emulado. O resultado
    final continua `linux/arm64`, compativel com o cluster.
- **Motivo:** desacopla completamente revisao, merge **e** deploy da disponibilidade do hardware
  caseiro. Os orquestradores (criar tag, chamar o reusable, limpar imagens via
  `actions/delete-package-versions`, publicar summary) sao tarefas de `git`/`gh`/API e nao
  precisam de ARM nem de rede local.
- **Secrets exigidos:** `GHCR_PAT`, `KUBECONFIG` (com IP Tailscale do no) e `TAILSCALE_AUTHKEY`.
  Auth keys da Tailscale expiram — se o deploy falhar no step "Conectar na Tailscale", renove o
  secret. Recomendado usar uma auth key **efemera** para nao acumular nos na tailnet a cada run.
- **Pre-condicao operacional:** o destino do deploy continua sendo o cluster K3s no Pi. Mover o
  runner para a nuvem **nao** substitui o cluster: se o Pi/cluster estiver desligado, o `kubectl`
  falha (timeout) — GitHub-hosted apenas remove a dependencia do *runner*, nao do *cluster*.
- **Reversao:** voltar `runs-on` para `[self-hosted, linux, ARM64]` nos workflows de deploy (e
  remover os steps QEMU/Tailscale) com o runner online.

## D-018 Personal Access Tokens (PAT) como mecanismo de auth para MCP/integracoes

- **Contexto:** o futuro servidor MCP do NossaLista (e outros clientes de API externos, ex.:
  assistentes de IA) precisa autenticar em nome de um usuario sem usar o fluxo de login
  interativo (JWT de sessao com expiracao curta, pensado para o SPA). E necessario um mecanismo
  de credencial de longa duracao, gerenciavel pelo proprio usuario, com granularidade de acesso.
- **Decisao:** introduzir Personal Access Tokens (`personal_access_tokens`), com as seguintes
  regras:
  - **Formato:** `nlmcp_` + 256 bits de entropia (`SecureRandom`, hex), mesmo padrao de forca do
    `OAuthCodeStore` (D-011). O prefixo identifica o token como PAT no filtro de autenticacao
    (`PersonalAccessTokenAuthenticationFilter`), distinguindo-o de um JWT sem custo de parsing.
  - **Hash-only:** so o hash SHA-256 do token e persistido (`token_hash`, coluna `UNIQUE`). O
    valor em claro e devolvido **uma unica vez**, na resposta de criacao — nunca mais recuperavel,
    nem pelo proprio dono. Mesma logica de "nunca guardar segredo em claro" do hashing de senha.
  - **Escopo:** `READ` ou `READ_WRITE`. Um PAT `READ` so pode usar metodos HTTP seguros
    (GET/HEAD/OPTIONS) em `/api/**` — reforcado via `AuthorizationManager` dedicado em
    `SecurityConfig`, nao apenas por convencao do cliente.
  - **Superficie restrita:** um PAT nunca pode gerenciar tokens (`/api/users/me/tokens/**`) nem
    acessar `/api/auth/**` — essas rotas exigem JWT de sessao normal. Evita que um token vazado
    seja usado para emitir novos tokens ou orquestrar o fluxo de auth.
  - **Limite por conta:** maximo de 10 tokens ativos (nao revogados) por usuario, com erro de
    negocio claro (`409 Conflict`) ao exceder — evita acumulo descontrolado de credenciais.
  - **`last_used_at` com throttle:** atualizado no maximo a cada 60s por token, para nao gerar um
    `UPDATE` a cada requisicao autenticada via PAT em uso intenso.
  - **Rate limiting de tentativas invalidas:** o filtro verifica primeiro, sem incrementar
    (`RateLimiterService.isBlocked`), se o IP ja esta bloqueado por tentativas anteriores; se
    estiver, responde `429` direto, **antes de qualquer lookup no banco**. So depois disso o
    token e resolvido por `token_hash` (indexado, `UNIQUE`); se o lookup falhar, a tentativa e
    registrada (`RateLimiterService.isAllowed`) para contar no bloqueio das proximas requisicoes
    desse IP. Um token valido nunca e contado como tentativa. Camada extra, nao defesa primaria —
    os 256 bits de entropia do segredo ja tornam forca bruta inviavel por si so.
- **Motivo:** e o pre-requisito de autenticacao do servidor MCP (Fase B do plano) — sem isso, o
  servidor MCP nao teria como autenticar clientes externos em nome de um usuario de forma segura
  e auditavel (revogavel, com expiracao e escopo).

## D-019 Bump para Spring Boot 4.0.7 (fecha CVEs de jackson-databind e spring-security)

- **Contexto:** o gate `security-and-compliance` (OWASP dependency-check) passou a falhar no PR
  #50 por CVEs novas no feed NVD, sem relacao com o conteudo do PR: `jackson-databind` 2.21.2 e
  `tools.jackson.core:jackson-databind` 3.1.2 (CVE-2026-54512, CVE-2026-54513, CVSS 8.1) e
  `spring-security-{core,web,oauth2-core,config,crypto}` 7.0.5 (CVE-2026-40988, CVE-2026-40993,
  CVSS 7.5/7.2). Todas geridas pelo BOM do `spring-boot-starter-parent` (nao sao dependencias
  adicionadas pelo PR).
- **Decisao:** subir o parent de **4.0.6** para **4.0.7** (ja disponivel no Maven Central). O BOM
  4.0.7 passa a gerenciar `jackson-2-bom` **2.21.4**, `jackson-bom` (tools.jackson) **3.1.4** e
  `spring-security.version` **7.0.6** — todas acima do minimo corrigido (2.21.3+/3.1.3+/7.0.6+),
  resolvendo as 4 CVEs sem overrides individuais.
- **Efeito colateral (positivo):** o 4.0.7 tambem passa a gerenciar `spring-framework` em
  **7.0.8** nativamente — o override manual `<spring-framework.version>7.0.8</spring-framework.version>`
  de D-013 ficou redundante e foi **removido**. Confirmado via `dependency:tree`:
  `spring-core:7.0.8` e `spring-web:7.0.8` inalterados.
- **Motivo:** patch de parent (4.0.6 -> 4.0.7) e de menor risco que overrides individuais e ainda
  reduz divida tecnica (fecha o item pendente de D-013 sobre remover o override de
  `spring-framework` "quando sair Spring Boot 4.0.7+").
- **Validacao:** `spring-security-{core,web,oauth2-core,config,crypto}:7.0.6`,
  `jackson-databind:2.21.4`, `tools.jackson.core:jackson-databind:3.1.4` confirmados via
  `dependency:tree`. Suite de testes completa sem regressao (ver resultado no PR #50).

## D-020 Servidor MCP embutido no backend (Streamable HTTP)

- **Contexto:** Fase B do plano MCP — expor as listas do NossaLista como tools MCP para
  Claude Code/Desktop/Cursor, autenticadas pelos PATs da Fase A (D-018) ou por JWT.
- **Decisao de dependencia (Passo 0):** usar o starter oficial
  `org.springframework.ai:spring-ai-starter-mcp-server-webmvc:2.0.0` (via
  `spring-ai-bom:2.0.0`), protocolo `STREAMABLE`, montado em `/mcp`. Spring AI 2.0.0 GA
  (lancado 2026-06-12, poucas semanas antes desta Fase) e o primeiro trem estavel
  desenhado especificamente para Spring Boot 4.0/4.1 + Spring Framework 7 — confirmado
  publicando de verdade contra Boot 4.0.7 deste projeto (`dependency:tree` sem conflito de
  versao: `spring-boot-starter-web` fica mediado em 4.0.7 mesmo com o starter do MCP
  pedindo 4.1.0 transitivamente, por ser dependencia mais proxima). O SDK Java oficial puro
  (`io.modelcontextprotocol.sdk:mcp`) ficou como fallback nao usado — o starter Spring AI
  cobriu tudo via anotacao `@McpTool` sem necessidade de registrar transporte manualmente.
  Tools implementadas como `@Component` com metodos `@McpTool` em `mcp/tool/*McpTools`,
  descobertas automaticamente pelo annotation scanner do starter (nenhum registro manual).
- **Roteamento — exclusao do SpaController:** `SpaController` (fallback do SPA) usava um
  regex catch-all (`^(?!api|ws|actuator|v3|swagger-ui|assets)[^\.]*`) que capturava `/mcp`
  via `@GetMapping` antes do `RouterFunction` do MCP ser considerado, causando 405 em toda
  chamada `POST /mcp`. Corrigido excluindo `mcp` do regex negativo. Mantido mesmo o
  `RouterFunctionMapping` (order -1) ja tendo prioridade teorica sobre
  `RequestMappingHandlerMapping` (order 0) — defesa em profundidade.
- **Seguranca:** `/mcp/**` exige autenticacao (`authenticated()`) em `SecurityConfig`, sem
  aplicar o `apiAccessManager()` de `/api/**` — todo o protocolo MCP trafega via `POST
  /mcp`, entao a restricao de metodos seguros para PAT `READ` bloquearia 100% das chamadas.
  O enforcement de escopo passou para a camada de tool: `McpSecurityContext.requireWriteAccess()`
  chamado no inicio de toda tool de mutacao, lancando `McpScopeException` (mensagem
  acionavel) se o PAT for `READ`-only. Os filtros `JwtAuthenticationFilter`/
  `PersonalAccessTokenAuthenticationFilter` da Fase A ja rodam para toda requisicao (nao sao
  scoped por path), entao cobrem `/mcp` sem alteracao. Identidade sempre resolvida por
  requisicao via `SecurityContextHolder` (`McpSecurityContext.currentUser()`) — nenhum
  estado de usuario e cacheado no processo do servidor MCP.
- **Tratamento de erro:** o `SyncMcpToolMethodCallback` do SDK ja converte qualquer
  excecao lancada por um metodo `@McpTool` em `CallToolResult.isError(true)` (texto:
  `"Error invoking method: <nome>\n<mensagem da causa raiz>"`) — confirmado lendo o
  fonte do SDK (`spring-ai-mcp-annotations:2.0.0`). Por isso as tools deste modulo apenas
  deixam suas excecoes de negocio (`ForbiddenException`, `ListNotFoundException`,
  `ValidationException`, `InvalidInputException`, `McpScopeException` etc.) propagarem
  normalmente — nunca stack trace, sempre mensagem acionavel do dominio.
- **Schema de saida:** `@McpTool(generateOutputSchema = true)` gera `outputSchema` a partir
  do tipo de retorno do metodo (quando nao e `CallToolResult`/void/tipo primitivo), e o
  `CallToolResult` resultante ja vem, por padrao do SDK, com `structuredContent` (o objeto
  tipado) **e** um bloco `content` de texto redundante (JSON serializado) — confirmado
  capturando uma chamada real de tool em teste. As 13 tools deste modulo tem `outputSchema`
  + `structuredContent` + texto simultaneamente, sem necessidade de construir
  `CallToolResult` manualmente. Efeito colateral descoberto e corrigido: todo componente de
  record e tratado como obrigatorio por padrao pelo gerador; campos opcionais precisam de
  `@Nullable` (jspecify) para sair do `required`, e `@JsonInclude(NON_NULL)` para nao
  serializar `null` explicito (que falha a validacao de
  tipo do schema mesmo fora do `required`).
- **Fix TOCTOU (MENOR-1 da Fase A):** `PersonalAccessTokenService.create` tinha uma janela
  entre contar tokens ativos e inserir um novo — duas requisicoes concorrentes do mesmo
  usuario podiam ambas passar pela checagem de limite (10) antes de qualquer insercao,
  ultrapassando o limite. Corrigido com `UserRepository.findByIdForUpdate` (`SELECT ... FOR
  UPDATE` via `@Lock(PESSIMISTIC_WRITE)`), travando a linha do usuario antes de contar —
  serializa chamadas concorrentes do mesmo usuario (usuarios diferentes nao se bloqueiam).
  Funciona identicamente em H2 e PostgreSQL.
- **Regressao de teste causada pela nova dependencia (achado e corrigido nesta fase):**
  adicionar `spring-ai-starter-mcp-server-webmvc` traz `reactor-core` transitivamente pela
  primeira vez neste projeto (SDK MCP usa `Mono`/`Flux` internamente mesmo no client/server
  sincrono). A mera presenca de `reactor-core` no classpath — bisseccionado e confirmado
  isoladamente, sem nenhuma outra mudanca de codigo — quebrou o padrao de teste usado em 5
  suites (`ListControllerIntegrationTest`, `ListItemControllerTest`,
  `MemberControllerIntegrationTest`, `PushControllerTest`, `UserControllerTest`) que
  populam `SecurityContextHolder.getContext().setAuthentication(...)` manualmente antes de
  `mockMvc.perform(...)`: o `SecurityContextHolderFilter` moderno passou a descartar essa
  autenticacao pre-definida, autenticando a requisicao como anonima (401). O idioma correto
  para isso e `TestSecurityContextHolder` (do `spring-security-test`), que integra
  corretamente com `springSecurity()` independentemente da presenca de `reactor-core` — as
  5 suites foram migradas para `TestSecurityContextHolder.getContext().setAuthentication(...)`
  (mantendo `SecurityContextHolder.clearContext()` onde existia, agora espelhado com
  `TestSecurityContextHolder.clearContext()`). Nao foi feita nenhuma mudanca em
  `SecurityConfig` de producao para "consertar" isso — o problema era so nos testes.
  Excluida tambem a dependencia transitiva `io.micrometer:context-propagation` do starter
  do MCP (nao usada pelo client/server sincrono deste projeto; sua mera presenca registra
  um `ThreadLocalAccessor` de `SecurityContext` no `ContextRegistry` global do Micrometer,
  reduzindo superficie de interacao nao intencional sem necessidade funcional).
- **Bug bloqueante encontrado no QA e corrigido (commit `1af9d07`):** `application.yml`
  de producao nao tinha `spring.ai.mcp.server.protocol: STREAMABLE` — so
  `src/test/resources/application.yml` tinha. Como esse arquivo de teste SUBSTITUI (nao
  mescla com) o de producao no classpath de teste, `McpServerIntegrationTest` sempre
  passou normalmente enquanto qualquer execucao real (`mvnw spring-boot:run` ou o jar
  empacotado, fora do `@SpringBootTest`) resolvia o transporte SSE legado (endpoint
  `POST /mcp/message`, nao `/mcp`) e respondia `500` em toda chamada real ao endpoint
  documentado. O QA confirmou o fix com um cliente MCP real
  (`@modelcontextprotocol/sdk`) contra o jar empacotado, antes e depois da correcao.
  **Licao registrada:** toda config nova de producao precisa existir nos DOIS arquivos
  (`src/main/resources/application.yml` e `src/test/resources/application.yml`) — a
  suite de testes, rodando exclusivamente sob `@SpringBootTest`, nao detecta ausencia no
  arquivo de producao. Nenhum smoke test do jar empacotado roda hoje em CI; adicionar um
  (build do jar + subida real + `curl`/chamada MCP minima contra `/mcp` antes do deploy)
  fica registrado aqui como follow-up para fechar essa lacuna estruturalmente, em vez de
  depender de revisao manual de "toda config nova esta nos dois arquivos".
- **Motivo:** entrega a Fase B do roadmap MCP — usuarios do NossaLista podem conectar
  assistentes de IA as proprias listas com credenciais de longa duracao, escopo de leitura
  ou leitura/escrita, e broadcast em tempo real automatico (as tools reusam os services
  existentes, que ja publicam eventos STOMP).
- **Apontamentos do review senior do PR aplicados (duas rodadas):**
  - **IMPORTANTE (pre-merge) — lote/pagina sem teto (DoS por payload gigante):**
    `add_items`/`set_items_checked`/`remove_items` nao limitavam o numero de itens por
    chamada, e `get_list`/`get_list_activity` nao limitavam `limit`/`size` — num backend de
    1 replica, uma unica chamada grande (de um token valido ou de um modelo induzido por
    prompt injection) executaria N inserts/toggles/deletes/broadcasts ou uma consulta
    pesada, sem nenhum rate limit cobrindo requisicoes autenticadas validas. Corrigido com
    `McpLimits` (`mcp/support/McpLimits.java`): teto de 200 itens por lote
    (`add_items`/`set_items_checked`/`remove_items`), teto de 500 por pagina em `get_list`
    (`limit`) e teto de 100 por pagina em `get_list_activity` (`size`) — tetos diferentes por
    chamador (ajustado na segunda rodada do review; a primeira versao usava 500 para os
    dois). `InvalidInputException` acionavel quando excedido, informando o teto exato e
    orientando dividir a chamada em varias. Constantes nomeadas e documentadas nas
    descricoes das proprias tools. `McpLimitsTest` + teste parametrizado em
    `McpServerIntegrationTest` cobrindo os 5 casos (um por tool/teto).
  - **MINOR — contrato de ordenacao implicito em `McpSecurityContext`:**
    `requireWriteAccess()` so era seguro porque `currentUser()` era sempre chamado antes; com
    autenticacao `null`/anonima, `isPersonalAccessToken(null)` retornava `false` e o metodo
    liberava a escrita por omissao. Sem impacto real hoje (`/mcp` ja exige `authenticated()`
    no HTTP antes de qualquer tool ser despachada), mas era footgun latente para uma tool
    futura que chamasse `requireWriteAccess()` sem `currentUser()` antes. Corrigido:
    `requireWriteAccess()` agora assevera autenticacao por conta propria (lanca
    `McpAuthenticationException` se ausente/anonima, antes de checar escopo).
    `McpSecurityContextTest` cobre auth ausente/anonima/JWT/PAT READ/PAT READ_WRITE.
  - **MINOR — prompt injection via conteudo de terceiros:** registrado como limitacao
    conhecida em `docs/mcp.md` (nao corrigivel nesta camada — inerente ao MCP). Descricoes de
    `remove_items` e `remove_member` passaram a orientar confirmacao com o usuario antes de
    chamar, no mesmo padrao ja usado por `delete_list`.
  - **`add_items` com elemento nulo na lista `items`:** confirmado empiricamente (capturada a
    resposta real de uma chamada de teste) que o proprio schema de entrada do SDK MCP ja
    rejeita um elemento `null` num array de objetos antes de a tool ser invocada
    (`isError: true`, "null found, object expected" — nunca chega ao Java). Mesmo assim,
    adicionado um guard defensivo em `ListItemMcpTools.addOneItem` (retorna outcome
    `"item must not be null"` em vez de propagar NPE) para robustez caso essa validacao de
    schema mude de comportamento no SDK.
  - **`update_item` com todos os campos nulos:** antes era um no-op silencioso (broadcast +
    activity log sem nenhuma mudanca real). Agora lanca `InvalidInputException`
    ("Nothing to update: provide at least one field...") quando `name`, `quantity`,
    `dueDate` e `url` vêm todos nulos.
  - **Idioma (aplicado na segunda rodada):** mensagens de erro geradas pelo proprio modulo
    `mcp` (`McpIds`, `McpLimits`, `ListNameResolver`, e as validacoes de
    `add_items`/`update_item`/`share_list` acima) foram traduzidas para ingles, consistente
    com as descricoes/parametros das tools (convencao MCP). Deliberadamente NAO tocado:
    mensagens de excecoes de negocio vindas dos services (`ForbiddenException`,
    `ListNotFoundException` lancada por `ListService`, `ValidationException`,
    `@NotBlank`/`@Size` dos DTOs compartilhados como `CreateListRequest`/
    `CreateItemRequestDTO`) — essas permanecem em portugues porque sao os mesmos
    services/DTOs usados pela API REST/SPA; traduzir so para o MCP exigiria uma camada de
    mapeamento de mensagens especifica do modulo, fora de escopo. Documentado em
    `docs/mcp.md` (secao "Idioma") para quem for adicionar uma tool nova saber onde a
    mensagem deve ficar em cada idioma.
  - **NIT — nao aplicado (decisao explicita, nao esquecimento):** o regex catch-all do
    `SpaController` (`(?!api|ws|...|mcp)`) nao tem boundary de segmento de path, entao tambem
    exclui rotas SPA que comecem com esses prefixos (ex.: `/mcpx`) — pre-existente para
    `api`/`assets`/etc. mesmo antes desta fase, risco baixo (nenhuma rota do SPA hoje comeca
    com esses prefixos), registrado para follow-up caso uma rota assim seja criada. O
    reviewer confirmou explicitamente que este item fica como esta.
- **CI vermelho pos-push, dois bugs de isolamento de teste encontrados e corrigidos:**
  o pipeline do PR reprovou `ListControllerIntegrationTest` (14 falhas de
  `listRepository.count()`) porque a ordem do Surefire no CI difere da local. Dois
  problemas distintos, ambos so visiveis com ordem de execucao diferente:
  1. **Poluicao de dados entre classes:** `McpServerIntegrationTest` (`@SpringBootTest`
     `RANDOM_PORT`) cria dezenas de listas via chamadas HTTP reais (sem `@Transactional`,
     que nao ajudaria mesmo — a chamada roda em outra thread) e compartilhava o mesmo H2
     em memoria (`testdb`) usado pelas suites de `MockMvc`. Corrigido isolando a classe
     com uma URL de H2 dedicada via `@TestPropertySource`
     (`jdbc:h2:mem:mcp-it;MODE=PostgreSQL;...`), o que forca o Spring a criar um
     `ApplicationContext` (e portanto um datasource/Flyway) proprio, nao compartilhado com
     nenhuma outra classe de teste.
  2. **Vazamento de autenticacao entre classes:** `PushControllerTest` (migrada para
     `TestSecurityContextHolder` no fix do `reactor-core` desta mesma fase, ver acima) seta
     autenticacao no `@BeforeEach` mas nao tinha `@AfterEach` de limpeza, ao contrario das
     outras 4 suites migradas. Como `TestSecurityContextHolder.getContext()` retorna o
     mesmo objeto `SecurityContext` que `SecurityContextHolder` usa internamente (confirmado
     lendo o fonte de `spring-security-test`), mutar esse objeto sem limpar depois deixa uma
     autenticacao "presa" no `SecurityContextHolder` do processo — visivel quando outra
     classe roda depois na mesma JVM/thread e espera nenhuma autenticacao (`McpSecurityContextTest`).
     Corrigido adicionando `@AfterEach` com `SecurityContextHolder.clearContext()` +
     `TestSecurityContextHolder.clearContext()`, no mesmo padrao das outras 4 suites.
  Validado localmente com `./mvnw clean test` em ordem normal, `-Dsurefire.runOrder=reversealphabetical`
  e `-Dsurefire.runOrder=random` (duas seeds diferentes) — 608/608 nas quatro execucoes.
  **Licao:** qualquer teste `@SpringBootTest` que persista dados via HTTP real, ou que
  manipule `SecurityContextHolder`/`TestSecurityContextHolder` diretamente, precisa de
  isolamento explicito (datasource dedicado ou limpeza simetrica) — a ordem padrao do
  Surefire local pode mascarar o problema indefinidamente.

## D-021 Cache do banco NVD aquecido no main (dependency-check rápido nas PRs)

- **Contexto:** o job `security-and-compliance` roda o OWASP dependency-check e as PRs de
  backend levavam ~14 min (às vezes *timeout*/`cancelled`). Os logs do Actions mostraram o
  step baixando o banco NVD inteiro — `NVD API has 363.026 records in this update` — em
  quase toda PR, em vez de uma atualização incremental.
- **Causa-raiz:** isolamento de cache do GitHub Actions. O banco NVD era cacheado por chave
  semanal criada **na branch da PR** (`refs/pull/NN/merge`); PRs irmãs não enxergam o cache
  uma da outra, e **não havia cache no `main`** (branch default — a única herdável por todas
  as PRs). Resultado: cache frio em quase toda PR de backend → pull completo (~363k
  registros, ~12-15 min). A abordagem "resumível" por-run (PR #55) só ajudava re-runs da
  mesma PR, não PRs novas.
- **Decisão:**
  - **Workflow agendado `nvd-cache-warmer.yml`** (cron diário 06:00 UTC + `workflow_dispatch`
    + push em `backend/pom.xml`): roda **no `main`** o goal `dependency-check:update-only` e
    salva o banco NVD num cache com chave por data (`nvd-db-<os>-<AAAA-MM-DD>`). Rodando no
    main, o cache é herdável por **todas** as PRs.
  - **PRs (`security-and-compliance`)**: restauram o cache por prefixo
    (`restore-keys: nvd-db-<os>-`, só leitura) e rodam `dependency-check:check` com
    **`-DautoUpdate=false`** — nunca tocam a rede da NVD, só analisam contra o banco quente.
    A propriedade `autoUpdate` foi confirmada via `dependency-check:help` no plugin 12.1.8.
  - **Guard de cache frio**: se o banco NVD não estiver presente (diretório ausente ou sem
    `*.mv.db`), o job dispara o warmer (`gh workflow run`, via `GHCR_PAT` — o `GITHUB_TOKEN`
    não pode disparar `workflow_dispatch` em outro workflow) e falha com mensagem acionável,
    pedindo re-execução após ~15 min.
- **Motivo:** o dependency-check em si (goal, `failBuildOnCVSS=7`, suppressions — ver
  **D-013**) fica **inalterado**; muda só o mecanismo de cache. A checagem de vulnerabilidade
  permanece no gate, mas o download pesado sai do caminho crítico das PRs (~14 min → <1 min).
- **Rollout / operação:** semear o cache uma vez após o merge (rodar `nvd-cache-warmer.yml`
  via `workflow_dispatch`) — ver RUNBOOK. O warmer mantém o banco quente diariamente; um
  cache *evicted* (limite de 10 GB do repo) é ressemeado automaticamente pelo guard na
  próxima PR de backend.
- **Nota de tolerância a versão:** o guard procura `*.mv.db` (wildcard) em vez do nome fixo
  `odc.mv.db`, para não quebrar se o dependency-check renomear o banco H2 numa versão futura.
- **Resiliência a NVD throttled:** em dias de rate-limit severo da NVD (visto em 02-03/07/2026)
  o download completo passa de 30 min. O warmer tem `timeout-minutes: 60` no step de update
  e salva o cache com `if: always()` sob chave por-run (`...-<run_id>-<run_attempt>`), de modo
  que o progresso parcial é preservado e o `restore-keys` (prefixo) o retoma no run seguinte,
  convergindo em poucas execuções (cron noturno + re-dispatch). As PRs restauram o mesmo cache
  por prefixo `nvd-db-<os>-`, então a chave por-run é transparente para elas.

## D-022 OAuth 2.1 (Authorization Code + PKCE) para clientes do servidor MCP (Fase C)

- **Contexto:** Fase C do roadmap MCP — destravar o botao "Add connector" do
  claude.ai (web e app mobile) e um fluxo OAuth nativo no Claude Code, evitando
  que o usuario precise copiar manualmente um PAT (Fase A/D-018) para conectar
  um assistente de IA ao servidor MCP (`/mcp`, Fase B/D-020).

- **Passo 0 — o que o claude.ai exige de fato (pesquisado em 2026-07, fontes abaixo):**
  - **Dynamic Client Registration (DCR, RFC 7591) NAO e obrigatoria.** O claude.ai
    aceita 3 formas de credencial num custom connector: DCR automatico, Client ID
    Metadata Documents (CIMD), e — a que importa aqui — **client_id/client_secret
    estaticos informados manualmente em "Advanced settings"** ao adicionar o
    conector (client_secret e opcional, cobrindo clientes publicos PKCE-only).
    Confirmado por multiplas fontes independentes, incluindo um issue do proprio
    tracker `anthropics/claude-ai-mcp` que descreve exatamente esse campo.
    O Claude Code CLI tem o mesmo suporte explicito via
    `claude mcp add --transport http --client-id <id> --client-secret <secret>
    --callback-port <porta>`. **Decisao:** registrar clientes ESTATICAMENTE em
    config (`app.mcp-oauth.clients` em `application.yml`), sem implementar DCR —
    exatamente o caminho default previsto no plano desta fase.
  - **Redirect URIs oficiais do claude.ai:** `https://claude.ai/api/mcp/auth_callback`
    e `https://claude.com/api/mcp/auth_callback` (os dois dominios sao usados pela
    Anthropic; registrados ambos para o cliente `claude-ai`).
  - **Claude Code (CLI) usa redirect loopback de porta variavel:**
    `http://localhost:<porta aleatoria>/callback` por padrao a cada conexao (fixavel
    com `--callback-port`, mas isso exige coordenacao manual do usuario a cada
    setup). **Decisao:** o cliente `claude-code` tem `allow-loopback-redirect: true`
    — `McpOAuthClientRegistry` aceita qualquer porta em
    `http://localhost|127.0.0.1/callback` para esse cliente especifico (RFC 8252
    §7.3, excecao de loopback para apps nativos — nunca um wildcard de host ou
    path). Nenhum outro cliente tem essa flag; match e sempre EXATO por padrao.
  - **OAuth 2.1 exige PKCE S256** (implicit grant removido, match exato de
    redirect_uri) — confirmado como requisito do proprio MCP Authorization spec.
    `code_challenge_method=plain` e rejeitado explicitamente em `GET /oauth/authorize`
    (redirect com `error=invalid_request`), nunca aceito silenciosamente.
  - **Descoberta:** claude.ai consulta `/.well-known/oauth-authorization-server`
    (RFC 8414) e `/.well-known/oauth-protected-resource` (RFC 9728) na raiz do
    dominio antes de iniciar o fluxo — os dois sao publicos e implementados em
    `WellKnownOAuthController`.
  - **Fontes:** [Claude.ai docs — Authentication for connectors](https://claude.com/docs/connectors/building/authentication),
    [Claude Help Center — Custom connectors via remote MCP](https://support.claude.com/en/articles/11175166-get-started-with-custom-connectors-using-remote-mcp),
    [anthropics/claude-ai-mcp#112 — client id/secret em advanced settings](https://github.com/anthropics/claude-ai-mcp/issues/112),
    [Claude Code Docs — MCP](https://code.claude.com/docs/en/mcp),
    [sunpeak.ai — Claude Connector Authentication (mai/2026)](https://sunpeak.ai/blogs/claude-connector-oauth-authentication/).

- **Passo 0 — Spring Authorization Server vs. implementacao propria enxuta:**
  **Decisao: implementacao propria enxuta** (Authorization Code + PKCE em
  controllers/services dedicados no pacote `mcpoauth/`), NAO Spring Authorization
  Server. Motivos, todos confirmados na pesquisa:
  - O modulo de authorization server do Spring foi absorvido pelo proprio Spring
    Security a partir da serie **7.1** (`org.springframework.security:
    spring-security-oauth2-authorization-server`), NAO na 7.0.x. Este projeto fixa
    `spring-security` em **7.0.6** via `spring-boot-starter-parent:4.0.7` (D-019);
    puxar um artefato `7.1.0` sobre um `spring-security-core:7.0.6` seria um
    skew de versao nao gerenciado pelo BOM do Boot atual — exatamente o tipo de
    "integracao tortuosa" que o plano desta fase pede para evitar. Confirmado
    consultando o Maven Central: a versao mais recente do artefato e `7.1.0`, sem
    nenhuma `7.0.x` GA publicada.
  - Relatos de terceiros descrevem o Authorization Server do Spring como exigindo
    "esforco e tinkering" consideraveis para producao, com pagina de login como
    SPA funcionando mas a tela de **consentimento sendo "mais dificil"**, e a
    propria equipe do Spring Security recomendando **clientes confidenciais**
    (nao publicos) — o oposto do que precisamos aqui (claude.ai/Claude Code sao
    OBRIGATORIAMENTE clientes publicos, PKCE-only, sem `client_secret` verificado
    no servidor).
  - A superficie OAuth deste projeto e deliberadamente pequena (2 escopos, poucos
    clientes estaticos, uma unica audience) — o overhead de adotar um
    authorization server de proposito geral, orientado a sessao/formulario de
    login, supera o beneficio. Uma implementacao propria enxuta, reaproveitando o
    padrao ja revisado/testado de `OAuthCodeStore` (D-011) e `PersonalAccessToken`
    (D-018) — code opaco de uso unico + TTL curto + hash-only para segredos —
    mante controle total sobre a arquitetura stateless (JWT em `localStorage`,
    sem `HttpSession`) sem reconciliar dois modelos de autenticacao concorrentes.

- **Design da implementacao (pacote `br.com.leoferolive.nossalista.mcpoauth`):**
  - **Fluxo de authorize sem exigir sessao no browser:** `GET /oauth/authorize` e
    PUBLICO (o JWT de sessao vive em `localStorage`, que NAO acompanha uma
    navegacao top-level de pagina inteira vinda do cliente OAuth). O endpoint
    valida `client_id`/`redirect_uri` (erros aqui sao 400 DIRETO, nunca redirect —
    evita open redirect com um redirect_uri nao confiavel), depois valida
    `response_type=code`/`code_challenge_method=S256`/`resource` (erros aqui
    redirecionam com `?error=` para o redirect_uri, ja confiavel nesse ponto),
    persiste um `PendingAuthorization` (tabela `mcp_oauth_pending_authorizations`,
    TTL 10min) e redireciona para `{FRONTEND_URL}/oauth/consent?request_id=...`.
  - **Tela de consentimento na SPA** (`frontend/src/pages/OAuthConsent.tsx`,
    protegida por `ProtectedRoute` — reusa o fluxo de login existente com retorno
    automatico via `redirect`/`postLoginRedirect`): busca o pedido pendente via
    `GET /api/oauth/consent/{requestId}` (autenticado por JWT normal) e decide via
    `POST /api/oauth/consent/{requestId}/approve|deny`, ambos restritos a sessao
    JWT normal (`sessionOnlyManager()`, mesma regra ja usada para
    `/api/users/me/tokens/**` — um PAT ou um access token OAuth do MCP nunca pode
    aprovar um consentimento em nome do usuario). Na aprovacao, emite um
    authorization code opaco (`mcp_oauth_codes`, SecureRandom 256 bits, TTL 60s) e
    devolve `{redirectUrl}` para a SPA navegar o browser de volta ao cliente OAuth.
  - **`POST /oauth/token` publico** (clientes suportados sao PUBLICOS — a prova de
    posse e o `code_verifier` ou a posse do refresh token opaco, nao um
    `client_secret`). Erros seguem o formato OAuth padrao
    (`{"error": "...", "error_description": "..."}`, RFC 6749 §5.2) via
    `McpOAuthExceptionHandler` — um `@RestControllerAdvice` ISOLADO do
    `GlobalExceptionHandler` compartilhado, com `@Order(HIGHEST_PRECEDENCE)`
    (achado de implementacao: quando dois beans `@RestControllerAdvice` existem, o
    Spring resolve o handler dentro do PRIMEIRO bean, por ordem, que tiver
    QUALQUER match — nao necessariamente o mais especifico entre todos os beans;
    sem o `@Order`, o catch-all `Exception.class` do `GlobalExceptionHandler`
    vencia antes deste bean ser sequer consultado, convertendo todo erro OAuth em
    500 generico).
  - **Access token:** JWT HS256 assinado com `MCP_OAUTH_SIGNING_KEY` — uma chave
    PROPRIA, nunca `JWT_SECRET` (fail-fast identico ao `JwtService`, min. 32 bytes).
    Claims: `sub` (userId), `scope` (`READ`/`READ_WRITE`, mesmos valores do PAT),
    `aud` (resource/audience canonica, RFC 8707), `client_id`, `iss`, `iat`, `exp`
    (30min default). `McpOAuthJwtService.validate()` rejeita assinatura invalida,
    expiracao e AUDIENCE DIFERENTE da canonica configurada (`app.mcp-oauth.resource`).
  - **Refresh token:** opaco (256 bits), hash-only (mesmo padrao PAT), com
    ROTACAO obrigatoria por `family_id`: cada uso revoga o token atual e emite um
    novo na MESMA familia. Reuso de um refresh JA revogado (rotacionado ou ja
    detectado antes) revoga a familia INTEIRA — sinal classico de vazamento
    (RFC 6749 §10.4). Reenvio do MESMO authorization code apos consumido (replay)
    tambem revoga a familia de tokens que ele originou.
  - **Achado de implementacao (`@Transactional` + `noRollbackFor`):** a revogacao
    de seguranca nos ramos de replay/reuso acontece IMEDIATAMENTE ANTES de lancar
    `OAuthTokenException` — como essa excecao e uma `RuntimeException`, o rollback
    padrao do Spring para `@Transactional` desfazia a propria revogacao junto com
    o resto da transacao, permitindo ao atacante continuar usando o token
    supostamente revogado. Corrigido com
    `@Transactional(noRollbackFor = OAuthTokenException.class)` em
    `exchangeAuthorizationCode`/`refresh` — descoberto e confirmado com um teste de
    integracao que falhava silenciosamente sem lancar excecao (o refresh
    "revogado" continuava funcionando).
  - **Enforcement de escopo reaproveita `PatAuthorizationSupport.PAT_AUTHORITY`:**
    por instrucao do plano ("mapear para as MESMAS authorities `SCOPE_*` que o
    PAT — `McpSecurityContext` nao deve precisar mudar"), o access token OAuth
    recebe as MESMAS tres authorities que um PAT (`ROLE_*`, `PAT_AUTHORITY`,
    `SCOPE_READ`/`SCOPE_READ_WRITE`) — isso faz `McpSecurityContext.requireWriteAccess()`
    bloquear escopo READ tentando mutar SEM nenhuma mudanca nessa classe. Uma
    authority adicional e exclusiva, `MCP_OAUTH_AUTHORITY`, identifica
    positivamente a origem OAuth (usada para bloquear `/api/**` — ver abaixo).
    Reinterpretacao documentada no Javadoc de `PatAuthorizationSupport`: a
    authority `PAT_AUTHORITY` agora significa "credencial nao-sessao com escopo
    explicito" (PAT OU OAuth), nao apenas "Personal Access Token" no sentido restrito.
  - **`/mcp/**` como unico escopo valido do token OAuth:**
    `McpOAuthTokenAuthenticationFilter` so tenta validar o Bearer como JWT OAuth
    quando `request.getRequestURI()` comeca com `/mcp` — fora dai, o token nem
    chega a ser autenticado (segue como anonimo). Defesa em profundidade em
    `SecurityConfig.apiAccessManager()`: nega explicitamente qualquer autenticacao
    com `MCP_OAUTH_AUTHORITY` em `/api/**`, mesmo que o filtro um dia deixe de
    restringir por path. Testado: usar o access token OAuth em `/api/lists`
    retorna **401** (nao 403 — o token nem e reconhecido como credencial fora de
    `/mcp`, entao a negacao de autorizacao de um anonimo vira 401 pelo
    `ExceptionTranslationFilter` do Spring Security, nao 403).
  - **`WWW-Authenticate` no 401 de `/mcp` sem tocar `Http401UnauthorizedEntryPoint`:**
    a exigencia de descoberta do MCP (RFC 9728) pede esse header apontando para
    `/.well-known/oauth-protected-resource`. Implementado com
    `DelegatingAuthenticationEntryPoint` (`McpWwwAuthenticateEntryPoint` para
    `/mcp/**`, delegando ao `Http401UnauthorizedEntryPoint` global depois de
    setar o header) passado como o UNICO `authenticationEntryPoint(...)` de
    `SecurityConfig`. **Achado de implementacao:** `defaultAuthenticationEntryPointFor(...)`
    (a API que parece feita para "adicionar" um entry point por path) e
    SILENCIOSAMENTE IGNORADO quando `authenticationEntryPoint(...)` explicito
    tambem esta configurado — confirmado lendo o fonte de
    `ExceptionHandlingConfigurer.getAuthenticationEntryPoint(H)`, que retorna o
    explicito e nunca chega a construir o `DelegatingAuthenticationEntryPoint`
    interno nesse caso. `Http401UnauthorizedEntryPoint` continua INTOCADO (Fase D
    mexe nesse arquivo em paralelo).
  - **Migration `V13__create_mcp_oauth_tables.sql`:** `mcp_oauth_pending_authorizations`,
    `mcp_oauth_codes` (nao apaga ao consumir — precisa do registro pos-consumo
    para detectar replay), `mcp_oauth_refresh_tokens`. Limpeza agendada em
    `McpOAuthCleanupScheduler` (scheduler PROPRIO, nao reaproveitando
    `RateLimiterCleanupScheduler` — Fase D mexe em rate limiting em paralelo).
  - **Tela "Conexoes":** nova secao "Assistentes conectados via OAuth"
    (`OAuthConnectionsPanel`, autocontido) ao lado dos PATs existentes — lista uma
    conexao por `client_id` (familia de refresh token mais recente ainda ativa) e
    permite desconectar (revoga TODA a familia daquele cliente).
  - **`mcp_oauth_codes.code` em claro (unica excecao ao padrao hash-only):**
    diferente de refresh token e PAT (D-018, hash-only, sao a credencial
    completa sozinhos), o authorization code sobrevive no maximo 60s (TTL
    curto), e uso unico, e SOZINHO nao autentica nada — a troca por token exige
    o `code_verifier` correto (PKCE), que nunca trafega nem e persistido no
    banco. Mesma decisao de `OAuthCodeStore` (D-011). Um vazamento do banco
    exporia o code, mas nao o verifier necessario para resgata-lo.

- **CRITICO — sequestro de consentimento cross-user (achado do QA, corrigido
  antes do merge):** sem vinculo nenhum a um browser/usuario, um atacante
  NAO-LOGADO podia chamar `GET /oauth/authorize` com o PROPRIO
  `code_challenge` (o endpoint e publico por design — ver acima), copiar a URL
  de consentimento resultante (`{FRONTEND_URL}/oauth/consent?request_id=...`)
  e envia-la por phishing para a vitima. A vitima, logada, abria o link,
  aprovava o que parecia um pedido de conexao legitimo, e o authorization code
  emitido saia com o `userId` da VITIMA mas o `code_challenge` do ATACANTE —
  que so ele sabia resgatar com o `code_verifier` correspondente, obtendo
  tokens OAuth (inclusive `READ_WRITE`) da conta da vitima. Reproduzido pelo QA
  com PoC completo (curl + SDK). Corrigido com DUAS camadas independentes
  (qualquer uma sozinha ja bloquearia o ataque descrito; as duas juntas cobrem
  variantes onde uma das duas nao se aplicasse):
  1. **Cookie de vinculo ao browser** (`PendingAuthorization.nonce` +
     `McpOAuthAuthorizationService.CONSENT_COOKIE_NAME`): `GET /oauth/authorize`
     gera um nonce de 256 bits, persiste no pedido, e devolve como cookie
     `HttpOnly; Secure; SameSite=Lax` (TTL = TTL do pedido, 10min) no MESMO
     browser que chamou o endpoint. `approve`/`deny` exigem esse cookie batendo
     (comparacao em tempo constante) com o nonce persistido — o browser da
     vitima, que nunca visitou `/oauth/authorize` (só recebeu o link do
     `/oauth/consent`), nao possui esse cookie.
  2. **Trava por usuario** (`PendingAuthorization.claimedByUserId`): o primeiro
     `GET /api/oauth/consent/{id}` autenticado reivindica o pedido para aquele
     `userId`; qualquer chamada subsequente (GET, approve ou deny) de um
     usuario DIFERENTE responde 403 (`OAuthConsentForbiddenException`) — mesmo
     que, por algum outro vetor, essa segunda conta tivesse o cookie certo.
  Nenhuma trava exige o cookie no `GET` de visualizacao (so em `approve`/
  `deny`) — ver dados do pedido (nome do cliente, escopo, host de retorno) nao
  e sensivel e simplifica a UX (a SPA sempre faz `GET` antes de decidir).
  Migration `V13` alterada (ainda nao mergeada) com as colunas `nonce`
  (`NOT NULL`) e `claimed_by_user_id` (nullable, `FK users`). Testes de
  integracao dedicados: `crossUserConsentHijackBlockedWithoutCookie`
  (reproduz o PoC do QA ponta-a-ponta e confirma o bloqueio) e
  `approveByDifferentUserThanClaimedIsForbidden` (trava por usuario isolada).

- **MEDIUM — access token OAuth sobrevive a revogacao de familia ate expirar
  (achado do QA, mitigado, nao eliminavel sem infra adicional):** o access
  token e um JWT stateless (`McpOAuthJwtService`) — quando uma familia de
  refresh tokens e revogada (replay de code, reuso de refresh, revoke manual,
  ou desconectar em "Conexoes"), qualquer access token JA EMITIDO daquela
  familia continua validando normalmente ate a propria expiracao, porque a
  validacao (`McpOAuthTokenAuthenticationFilter`) so verifica assinatura +
  `exp` + `aud`, nunca consulta o banco. **Mitigacao adotada:** TTL do access
  token reduzido de 30min para **10min** (`McpOAuthProperties.DEFAULT_ACCESS_TOKEN_TTL`,
  constante nomeada — nao mais um literal solto), limitando a janela de
  exposicao. **Nao eliminavel nesta fase** sem introduzir estado por chamada
  (introspeccao de token contra o banco, ou uma blacklist de JTIs revogados)
  — o que reintroduziria uma consulta ao banco em toda chamada `/mcp`,
  trade-off deliberadamente evitado pelo design stateless desta fase.
  Registrado como **follow-up conhecido**: se o caso de uso exigir revogacao
  instantanea de access tokens (nao so de refresh), avaliar introspeccao
  (RFC 7662) ou reduzir ainda mais o TTL. Teste dedicado
  `revokedFamilyAccessTokenRemainsValidUntilNaturalExpiry` documenta
  EXPLICITAMENTE esse comportamento esperado (para nao ser lido como bug
  nao-intencional numa leitura futura do codigo).

- **Testes:** `McpOAuthFlowIntegrationTest` (23 casos, H2 dedicado
  `mcp-oauth-it` — mesmo padrao de isolamento de `McpServerIntegrationTest`,
  D-020) cobre o fluxo completo autorize→consentimento→code→token→chamada MCP
  real via SDK, PKCE incorreto, code replay, redirect_uri/resource divergentes,
  refresh rotation + reuso, escopo READ bloqueando mutacao via token OAuth,
  bloqueio em `/api/**`, audience errada, endpoints well-known, o header
  `WWW-Authenticate`, o PoC de sequestro de consentimento cross-user (bloqueado
  com e sem cookie), a trava por usuario, `client_id` desconhecido em
  `/oauth/token` (formato `invalid_client`, ver review abaixo), e a
  sobrevivencia do access token a revogacao de familia (limitacao documentada,
  nao bug). Mais `PkceValidatorTest`, `McpOAuthClientRegistryTest` (inclui a
  regra de loopback), `McpOAuthJwtServiceTest` (fail-fast da chave, expiracao,
  audience, chave errada), `McpOAuthAtomicUpdatesRepositoryTest` e
  `McpOAuthTokenServiceTest` (ver review abaixo) isolados. Suite completa do
  backend: **657 testes, 0 falhas** (`./mvnw clean test`, com `clean`
  explicito). Numero contra-verificado contando os elementos `<testcase>` nos
  XML do Surefire (`grep -o "<testcase " target/surefire-reports/*.xml | wc -l`
  = 657, identico ao total agregado do Maven) — a linha "Tests run" do
  `.txt`/stdout de uma classe isolada pode reportar 0 para classes com metodos
  `@ParameterizedTest`/`@Nested`, o que já gerou contagem incorreta numa
  medicao anterior; contar os `<testcase>` do XML é o metodo confiavel.
  Frontend: `OAuthConsent.test.tsx` (novo) e `Connections.test.tsx`
  (ampliado com a secao OAuth) — suite completa 494 testes/51 arquivos,
  0 falhas (`npx vitest run`).

- **Review senior pos-implementacao (aplicado antes do merge do PR #56):**
  - **I-1/M-1 — consumo/rotacao/reivindicacao ATOMICOS (achado do review):** o
    padrao original de "ler o estado em Java, decidir, gravar" para consumir um
    authorization code, rotacionar um refresh token, e reivindicar um
    `PendingAuthorization` tinha uma janela de corrida sob READ COMMITTED —
    duas transacoes concorrentes liam o MESMO snapshot "ainda nao
    consumido/revogado/reivindicado" e ambas prosseguiam, emitindo DOIS pares
    de token do mesmo code (ou deixando uma conta diferente roubar uma
    reivindicacao) sem disparar a revogacao de replay. Corrigido substituindo
    cada leitura-decisao-gravacao por um UPDATE condicional
    (`WHERE consumed_at/revoked_at/claimed_by_user_id IS NULL`) que devolve o
    numero de linhas afetadas: `McpOAuthCodeRepository#markConsumed`,
    `McpOAuthRefreshTokenRepository#markRotated`,
    `PendingAuthorizationRepository#claimIfUnclaimed`. Quando o UPDATE afeta 0
    linhas, o SERVICO trata como corrida perdida (replay/reuso/sequestro) e
    revoga a familia certa, exatamente como no ramo de replay sequencial ja
    existente. **Achado de implementacao (cache de 1o nivel do Hibernate):** ao
    reler o `token_family_id`/`claimed_by_user_id` VENCEDOR apos o UPDATE
    condicional falhar, um `findByCode`/`findById` comum devolveria a MESMA
    entidade ja gerenciada nesta transacao (carregada antes do UPDATE em
    massa), com os campos NUNCA mutados em Java — nao o valor persistido pela
    transacao vencedora. Corrigido com projecoes ESCALARES dedicadas
    (`findTokenFamilyIdByCode`, `findClaimedByUserId`), que sempre disparam um
    SELECT novo contra o banco. Testado em dois niveis:
    `McpOAuthAtomicUpdatesRepositoryTest` prova, chamando cada UPDATE
    condicional duas vezes em sequencia, que a segunda chamada SEMPRE afeta 0
    linhas (a propria query e o compare-and-swap); `McpOAuthTokenServiceTest`
    (mocks) forca o UPDATE condicional a "perder a corrida" mesmo com o
    snapshot inicial "nao consumido", provando que o SERVICO revoga a familia
    VENCEDORA (nao a local) e nunca emite um segundo par de tokens — cenario
    inatingivel chamando o endpoint HTTP duas vezes em sequencia, porque o
    atalho de replay sequencial (baseado no snapshot ja lido) sempre
    intercepta antes.
  - **M-2 — `client_id` desconhecido em `/oauth/token`:** respondia um
    `ProblemDetail` RFC 7807 (formato usado por `/oauth/authorize`), nao o
    formato de erro OAuth padrao (`{"error": "invalid_client"}`, RFC 6749 §5.2)
    esperado por SDKs de cliente OAuth genericos. Corrigido convertendo
    `OAuthUnknownClientException` para `OAuthTokenException.invalidClient` em
    `McpOAuthTokenService` (`requireKnownClient`), mantendo o `ProblemDetail`
    intocado em `/oauth/authorize` (onde faz sentido — o erro e antes de haver
    um redirect_uri confiavel). Teste dedicado:
    `tokenRejectsUnknownClientWithOAuthErrorFormat`.
  - **M-3 — cliente de teste local (`nossalista-local`) NAO deve existir em
    prod:** estava registrado em `application.yml` (base, compartilhada por
    TODOS os profiles, inclusive prod). Corrigido movendo o registro para
    `application-dev.yml` (listas em `@ConfigurationProperties` NAO mesclam
    entre profiles — o bloco em `application-dev.yml` redeclara a lista
    INTEIRA, claude-ai/claude-code + nossalista-local); `application.yml`
    (base, herdada por prod) mantem so claude-ai/claude-code.
    `application-prod.yml` comentado explicitamente para nao reintroduzir esse
    cliente ali.
  - **M-4 — summary de `/oauth/revoke` superestimava o efeito:** dizia revogar
    "um refresh (ou access) token"; na pratica so revoga a familia de refresh
    tokens — o access token OAuth, por ser JWT stateless, sobrevive ate expirar
    (ver limitacao MEDIUM acima). Summary/descricao do endpoint corrigidos para
    refletir isso.
  - **NITs:** `McpOAuthJwtService.validate()` envolve a checagem de
    `getAudience()` num try/catch para um NPE teorico (token malformado de
    origem externa); `McpOAuthConsentController` limpa o cookie de
    consentimento (Max-Age=0) apos approve/deny (ja sem uso depois da
    decisao); `McpOAuthAuthorizeController` valida o tamanho de `state`
    (limite da coluna VARCHAR(512)) antes de persistir, evitando um 500 de
    truncamento do banco.

- **Novas variaveis de ambiente:** `MCP_OAUTH_SIGNING_KEY` (obrigatoria,
  fail-fast, >= 32 bytes, DIFERENTE de `JWT_SECRET`), `MCP_OAUTH_ISSUER` e
  `MCP_OAUTH_RESOURCE` (defaults localhost em dev/test, dominio real
  hardcoded como default em `application-prod.yml`, mesmo padrao de
  `frontend.url`). Ver `docs/ENVIRONMENT.md`.

- **Pendencias/riscos conhecidos:**
  - O redirect URI do app MOBILE do claude.ai nao foi confirmado
    separadamente na pesquisa (assume-se o mesmo `claude.ai/api/mcp/auth_callback`
    via deep link/webview do sistema, pratica padrao de OAuth em apps moveis) —
    validar com um teste manual real do "Add connector" no app quando possivel.
  - `createdAt` exibido na tela de Conexoes para uma conexao OAuth reflete a
    ultima ROTACAO do refresh token, nao a conexao original (limitacao aceita:
    `lastUsedAt` e a informacao mais acionavel de qualquer forma).
  - DCR nao implementado: se um cliente OAuth generico (nao claude.ai/Claude
    Code) precisar se conectar sem suporte a client_id estatico, ele nao
    funcionara ate um DCR minimo ser adicionado — fora do escopo desta fase por
    decisao explicita do Passo 0.
