# Napkin Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Execution & Validation (Highest Priority)
0. **[2026-06-10] Validar deploy direto no Pi via kubectl (canal Tailscale), independente da API GitHub**
   Do instead: o kubeconfig local tem o contexto `default` apontando para o cluster do Pi (`https://100.117.57.82:6443`, CA+cert válidos). Use `kubectl --context default -n nossalista-dev ...` (dev) / `-n nossalista` (prod). zsh não expande `K="kubectl ..."`; escreva o comando completo. Quando `api.github.com` (IP Azure `4.228.31.149`) cai (timeouts `http=000`, inclusive de dentro dos pods), CI/release/deploy e `git push` continuam — só a observação via `gh`/`gh workflow run` quebra. Confirme o resultado do deploy pela imagem do Deployment e saúde do pod, não pelo `gh`.
1. **[2026-07-07] Contagem de testes = `<testcase>` nos XML do surefire, nunca o console**
   Do instead: `grep -c '<testcase' backend/target/surefire-reports/*.xml`; classes com `@Nested` fazem o console reportar `Tests run: 0` (regra canônica em `backend/QUALITY.md`).
2. **[2026-07-07] Gate NVD falha rápido com cache frio — não é bug da sua mudança**
   Do instead: disparar `gh workflow run nvd-cache-warmer.yml`, aguardar ~15min de seed e re-rodar o CI. Override admin SÓ se o pom.xml não tiver diff E com autorização do usuário registrada naquele PR (não se estende a outros PRs).
3. **[2026-06-10] Boot do Spring Boot no Pi ARM ~180-215s — dimensionar startupProbe/rollout com folga**
   Do instead: o cold-start mede ~180-215s ("Started ... in 182s"; rollout observado chegou a 214s). `startupProbe` precisa de janela folgada (`initialDelay + periodSeconds*failureThreshold`): dev 315s (15+10*30), prod 360s (60+10*30). `kubectl rollout status --timeout` ≥ 540s. Janela curta → SIGTERM (exit 143) no boot → crash-loop + falso negativo de CI. Container JRE slim não tem `jar`/`unzip`/`wget`: tem `curl`; liste libs aninhadas com `grep -a -o 'BOOT-INF/lib/[A-Za-z0-9._-]*\.jar' /app/app.jar`.
4. **[2026-06-10] `./scripts/quality.sh` falha com Permission denied em worktrees**
   Do instead: os scripts em `scripts/quality/` estão versionados como `100644` (sem bit de exec). Rode o gate via `bash scripts/quality/run-backend.sh pre-commit` e `bash scripts/quality/run-frontend.sh pre-commit` (EXIT=0 = ok). Para commitar use `git commit --no-verify` quando o hook husky esbarrar no mesmo bit.
5. **[2026-06-10] Worktrees novas não têm dependências instaladas**
   Do instead: antes de testar, rode `npm ci` em `frontend/` e deixe o Maven baixar deps (`./mvnw -o test` usa cache offline). Sem isso o gate de frontend falha com `eslint: not found` (artefato de ambiente, não regressão).
6. **[2026-07-07] Teste flaky conhecido: UserControllerTest.shouldUpdateUpdatedAtAutomatically**
   Do instead: re-rodar antes de atribuir a falha à sua mudança.
7. **[2026-03-19] UFW on Raspberry Pi blocks K3s API port 6443 for Tailscale traffic**
   Do instead: Ensure `sudo ufw allow in on tailscale0 to any port 6443 proto tcp` is active. If deploy times out on `kubectl apply`, check UFW first.
8. **[2026-03-19] deploy-on-tag.yml requires commit SHA as `ref`, not tag name**
   Do instead: Use `git rev-list -n 1 <tag>` to get the commit SHA and pass it as `ref`.

## CI/CD Fragilidades Conhecidas
0. **[2026-07-06] `deploy-prod.yml` NÃO tem aprovação de environment — README/RUNBOOK estão errados**
   Do instead: o job `deploy` em `deploy-environment.yml` não declara `jobs.<id>.environment:`; o `environment: prod` em `deploy-prod.yml`/`rollback-prod.yml` é só input do `with:` (seleciona `k8s/prod`), sem protection rules. Qualquer `workflow_dispatch` deploya prod sem revisão. Não confie no README (`README.md:27,207`) nem no RUNBOOK (`docs/RUNBOOK.md:111,134,142`) nesse ponto; o CLAUDE.md ("sem gate de aprovação manual") é o correto. Antes do dispatch, validar tag↔ref (skill `verify-deploy`). Ao corrigir: ou criar o environment `production` com required reviewers e declará-lo no job, ou consertar os docs.
1. **[2026-06-10] CI quebra em pane do `api.github.com`: action EditorConfig sem cache nem versão pinada**
   Do instead: o job `security-and-compliance` usa `editorconfig-checker/action-editorconfig-checker@v2`, que busca `/releases/latest` no `api.github.com` **a cada run, sem cache** (≠ Gitleaks, que tem cache). Em apagão do `api.github.com` (IP Azure `4.228.31.149`), o step falha com `ETIMEDOUT` → CI vermelha → `release.yml` vira `skipped` → sem tag nova. Não é o commit; é infra. Hardening: pinar versão do ec ou cachear o binário (como o Gitleaks). Re-run só adianta quando o `api.github.com` voltar **de verdade** (o runner também precisa alcançá-lo). Trigger de rerun/`gh` também depende da mesma API; `git push` não.
2. **[2026-07-07] Gate de cobertura do CI é MAIS estrito que o baseline local**
   Do instead: o CI compara contra a cobertura REAL da main (cache `base-*-coverage-<sha>`) + diff-cover ≥80% nas linhas alteradas — passar no `.quality-baseline` local não garante o gate. Critérios completos e citáveis em `docs/goal-criteria.md`.

## Shell & Command Reliability
1. **[2026-07-23] WSL local sem libasound.so.2 quebra `playwright test` (chromium headless shell)**
   Do instead: `apt-get download libasound2t64` funciona sem root; extrair com `dpkg -x libasound2t64_*.deb <dir>` e rodar os testes com `LD_LIBRARY_PATH=<dir>/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH npx playwright test`. Sem isso o erro é `error while loading shared libraries: libasound.so.2`.
2. **[2026-07-23] `gh run view --job <id> --log` pode dizer "still in progress" mesmo com o job já concluído (`conclusion` preenchido na API)**
   Do instead: baixar direto via `gh api repos/<owner>/<repo>/actions/jobs/<id>/logs > file.log` e grepar o arquivo; não confiar no comando `gh run view --log` para jobs cujo workflow run ainda tem outro job pendente.
3. **[2026-03-19] SSH with password requires pty — no sshpass/expect in this env**
   Do instead: Use Python `pty.fork()` pattern to handle password prompts for SSH to Pi (`leoferolive@100.117.57.82`).

## Domain Behavior Guardrails
0. **[2026-07-23] `frontend/mock/mockServer.ts` precisa espelhar o contrato real de auth — ele não segue sozinho quando o backend/front mudam**
   Do instead: ao alterar fluxo de autenticação real (ex.: JWT/localStorage → cookie HttpOnly + CSRF), atualizar também o mock server e `e2e/support/mock-auth.ts` no mesmo PR. Sintomas de drift: `/api/users/me` sempre "logado" mesmo anônimo (mock caía num fallback de usuário demo), e falha de qualquer POST/PUT/PATCH/DELETE com alerta "Mock route não encontrada" na UI (preflight `ensureCsrfToken()` batendo em `/api/auth/csrf` inexistente no mock). `npm run test:e2e:pr` roda contra `preview:mock`, não contra um backend real.
1. **[2026-06-11] Prod `/actuator/health` agregado = DOWN por MailHealthIndicator (SMTP 535)**
   Do instead: o SMTP de prod rejeita login (`AuthenticationFailedException: 535 5.7.8 Authentication failed`) → `mail` health DOWN → `/actuator/health` agregado DOWN, MAS liveness/readiness UP (mail não está nesses grupos), então o pod serve normalmente e a URL pública `/api/health` (controller próprio) = UP. Não é regressão de deploy (o deploy não toca `nossalista-secrets`). Para resolver: corrigir credencial SMTP no secret de prod, ou excluir `mail` do health agregado. Features de e-mail (verificação/reset) ficam quebradas em prod até lá. Probes usam os endpoints de grupo (`/actuator/health/liveness|readiness`), não o agregado — por isso o mail DOWN não derruba o pod.
2. **[2026-07-07] MCP_OAUTH_SIGNING_KEY ≥32 bytes, única por ambiente (dev/prod)**
   Do instead: gerar com `openssl rand -hex 32`; sem ela o pod entra em crash-loop (fail-fast do OAuth). Secret `nossalista-secrets` nos ns `nossalista` e `nossalista-dev`.
3. **[2026-07-07] Spring AOP é incompatível com o scanner @McpTool**
   Do instead: rate limit/métricas do MCP são chamadas explícitas no código, nunca aspecto (CGLIB quebra o transporte async). Ver D-023 em `docs/DECISIONS.md`.
4. **[2026-07-07] Frontend: `src/pages/**` e `src/main.tsx` são EXCLUÍDOS da cobertura**
   Do instead: lógica testável vai para `src/components/**` (contam no gate); ver `vitest.config.ts`.
5. **[2026-03-19] GitHub Actions billing limit blocks scheduled workflows (e2e fullstack)**
   Do instead: Check GitHub Billing & Plans before investigating workflow failures that never started.

## User Directives
1. **[2026-06-10] Implementação sempre em worktree isolada; não integrar sem revisão**
   Do instead: cada área de trabalho em sua `git worktree`/branch; nunca `git push`/PR/merge em `main` sem confirmação explícita do dono. Branches de fix ficam locais e prontas para review.
