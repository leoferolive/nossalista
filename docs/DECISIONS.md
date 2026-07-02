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
- **Motivo:** entrega a Fase B do roadmap MCP — usuarios do NossaLista podem conectar
  assistentes de IA as proprias listas com credenciais de longa duracao, escopo de leitura
  ou leitura/escrita, e broadcast em tempo real automatico (as tools reusam os services
  existentes, que ja publicam eventos STOMP).
