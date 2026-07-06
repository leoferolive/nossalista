# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Git Commit Guidelines

**CRITICAL:** Do NOT include "Co-Authored-By: Claude" or any AI attribution in commit messages. Keep commits clean and professional.

## Sobre o Projeto

NossaLista e um aplicativo web de listas compartilhadas em tempo real. O projeto esta em desenvolvimento ativo de MVP, com backend e frontend ja implementados no monorepo.

## Governanca de Documentacao (Mandatorio)

Ao final de **toda task**, e obrigatorio revisar e atualizar a documentacao canonica impactada pela mudanca.

Escopo de "toda documentacao" neste repositorio:

- `README.md`
- `CLAUDE.md` (e `AGENTS.md`, que deve espelhar este arquivo via symlink)
- `docs/**`
- `backend/QUALITY.md`
- `frontend/README.md`

Regras obrigatorias:

- Nenhuma task e considerada concluida sem atualizar a documentacao impactada.
- Se a implementacao mudar contrato, fluxo, comando, arquitetura, ambiente ou operacao, a documentacao correspondente deve ser atualizada na mesma task.
- Em caso de duvida, atualizar ao inves de adiar.
- Nao usar `_bmad-output/**` como fonte canonica operacional do projeto.

## Quality Gate (Obrigatório antes de commit)

Antes de qualquer `git commit`, rode:

    ./scripts/quality.sh --pre-commit

Ele faz lint + typecheck + static analysis nos dois ecossistemas em <= 30s. Se passar, commite. Se falhar, **corrija a regressão antes**, não relaxe thresholds.

Para uma verificação completa (cobertura + ratchet), antes de abrir PR:

    ./scripts/quality.sh --full

Detalhes, thresholds e limitações: `docs/quality-gate.md`. Dívida técnica pré-existente: `docs/quality-gate-debt.md`.

## Stack Técnico Planejada

| Camada      | Tecnologia                               |
| ----------- | ---------------------------------------- |
| Frontend    | React 19 + TypeScript + Vite             |
| Backend     | Java 25 + Spring Boot 4                  |
| Real-time   | Spring WebSocket (STOMP + SockJS)        |
| Auth        | Google OAuth2 + email/senha              |
| BD Produção | PostgreSQL                               |
| BD Dev      | PostgreSQL (Docker Compose)              |
| BD Testes   | H2 (MODE=PostgreSQL)                     |
| Infra       | Raspberry Pi 4 + K3s + Cloudflare Tunnel |

## Documentação de Referência

O documento de escopo completo está em `docs/NossaLista — Documento de Escopo MVP.txt` e contém:

- Arquitetura detalhada do sistema
- Modelo de dados (ERD)
- Estrutura de pastas planejada para backend e frontend
- Especificação de APIs REST
- Protocolos WebSocket (STOMP)
- Roadmap de implementação em 5 fases

## Estrutura de Pastas Planejada

### Backend (nossalista-api/)

```
src/main/java/br/com/leoferolive/nossalista/
├── config/          (Security, WebSocket, CORS)
├── auth/            (AuthController, AuthService, JWT, OAuth2)
├── user/            (User entity, controller, service, repository)
├── list/            (SharedList entity [tabela `lists`], ListType, controller, service, repository)
├── listitem/        (ListItem entity, CRUD)
├── member/          (ListMember, convites)
├── activity/        (ActivityLog, histórico)
└── websocket/       (STOMP controllers, interceptors)
```

### Frontend (nossalista-web/)

```
src/
├── api/             (axios client, endpoints, websocket)
├── hooks/           (useAuth, useLists, useWebSocket)
├── pages/           (Login, Home, ListView, etc.)
├── components/      (ListCard, ListItem, modais)
├── contexts/        (AuthContext)
└── types/           (TypeScript types)
```

## Principais Funcionalidades do MVP

- **Autenticação**: Google OAuth2 (principal) + email/senha (fallback)
- **Tipos de lista**: Compras, Tarefas, Wishlist, Genérica
- **Campos dinâmicos**: Cada tipo de lista possui campos específicos (quantidade, prazo, URL, etc.)
- **Compartilhamento**: Convite por username ou link único
- **Sincronização real-time**: WebSocket com STOMP para atualizações instantâneas
- **Activity log**: Timeline de ações na lista

## Diretriz Visual Atual do Frontend

- A linguagem oficial do frontend e `Fresh Lists`
- A direcao base do sistema visual e `Playful Editorial`, com a identidade unificada **leoferolive design**: paleta `violeta + teal` (`--nl-accent` = violeta `#7c3aed` para acao/CTA; `--nl-primary` = teal `#14b8a6` para status/progresso)
- As fontes sao **self-hosted** (`Fraunces` + `Plus Jakarta Sans`, variaveis) em `frontend/src/styles/fonts/` — nao usar o CDN do Google Fonts
- A marca vive em tokens `--nl-*` (`frontend/src/index.css`); o override da identidade `leoferolive design` esta em `frontend/src/styles/leoferolive-tokens.css`, importado por ultimo em `frontend/src/main.tsx` para vencer na cascata. Re-skins futuros trocam **valores** de tokens, nunca componentes
- `light` e `dark` devem ser tratados como temas de primeira classe, com o mesmo nivel de refinamento visual
- A landing publica deve manter dois fluxos distintos:
  - CTA principal: cadastro
  - CTA secundario: login
- A landing publica deve permanecer minimalista, com hero curto, preview principal unico e pouco texto concorrente
- Evitar componentes publicos ou modais com comportamento ambiguo (ex.: dois links abrindo o mesmo fluxo)
- Sempre priorizar primitives globais de tema/formulario/modal antes de criar estilos locais ad hoc

## Skills Locais

- `.agents/skills/interface-design`: skill local para design de interfaces de produto, com foco em dashboards, apps e paineis autenticados.
- A skill pode persistir decisoes em `.interface-design/system.md` para manter consistencia visual entre sessoes.
- Ao usar essa skill neste repositorio, respeitar a diretriz `Fresh Lists`, a minimalidade da landing publica e a paridade entre temas `light` e `dark`.
- O frontend pode rodar em modo `mock` via `npm run dev:mock`, servindo `/api/**` em memoria e mantendo o WebSocket em modo no-op conectado para desenvolvimento visual.
## Deploy e Infraestrutura

### Kubernetes (k8s/)

- `deployment.yaml`: Deployment com 1 réplica, health checks em `/actuator/health`
- `service.yaml`: Service tipo ClusterIP
- `ingress.yaml`: Ingress Traefik para `nossalista.leoferolive.com.br`
- `namespace.yaml`: Namespace dedicado `nossalista`

### CI/CD

O pipeline usa `deploy-environment.yml` como único workflow reutilizável central. A lógica de build/deploy está **internalizada** nele (sem dependência de repos externos). O `release.yml` usa `GHCR_PAT` para disparar `deploy-on-tag.yml` via `workflow_dispatch` — necessário porque `GITHUB_TOKEN` não pode disparar outros workflows e `workflow_call` dentro de `workflow_run` não é suportado pelo GitHub.

**Runners (regra):**

- **Todos os workflows** (CI **e** deploy, dev e prod) rodam em `ubuntu-latest` (GitHub-hosted). Nenhum workflow depende mais do runner self-hosted do Pi. Ver `docs/DECISIONS.md` D-016 (CI) e D-017 (deploy).
- **Deploy → cluster:** o kube-apiserver do K3s **não** é exposto na internet; o `KUBECONFIG` aponta para o **IP Tailscale** do nó. Os jobs de deploy entram na tailnet via `tailscale/github-action` (secret `TAILSCALE_AUTHKEY`) **antes** do `kubectl`.
- **Build ARM:** o `Dockerfile` é cross-build — os stages de compilação (npm/maven) usam `--platform=$BUILDPLATFORM` (nativos em x86); só o stage de runtime é `linux/arm64`, emulado via `docker/setup-qemu-action`. Por isso a migração não exige build nativo ARM.
- **Secrets exigidos pelo deploy:** `GHCR_PAT` (push GHCR), `KUBECONFIG` (com IP Tailscale do nó), `TAILSCALE_AUTHKEY` (entrar na tailnet). O `TAILSCALE_AUTHKEY` deve estar válido (auth keys expiram) — se o deploy falhar no step "Conectar na Tailscale", renove o secret.

**Limitações conhecidas do GitHub Actions (não contornar):**

- `GITHUB_TOKEN` não pode disparar `workflow_dispatch` em outros workflows (403)
- `workflow_call` dentro de `workflow_run` causa `startup_failure`
- `permissions:` dentro de jobs de reusable workflow (`workflow_call`) causa `startup_failure`
- Push de tag via `GITHUB_TOKEN` não dispara eventos de outros workflows

### Arquitetura de Workflows

```
ci.yml (push/PR) ──────────────────────────────────────────── testes, lint, segurança
osv-scanner.yml (PR/push main/cron) ───────────────────────── SCA de dependências

release.yml (workflow_run após CI) ─── cria tag + release
                                    └─ gh workflow run deploy-on-tag.yml (GHCR_PAT)
                                                          ↓
deploy-branch-dev.yml (manual) ───────────────────────────┤
deploy-prod.yml (manual) ─────────────────────────────────┤
rollback-prod.yml (manual) ───────────────────────────────┘
                                              ↓
                                   deploy-environment.yml
                                   (build Docker + push GHCR + kubectl)
```

- **`deploy-environment.yml`**: Reusable workflow com jobs `build-and-push` e `deploy`. Sem `permissions:` em jobs (causa startup_failure). Imagens: `nossalista` (prod) / `nossalista-dev` (dev).
- **`release.yml`**: Cria tag semântica e GitHub Release. Usa `GHCR_PAT` (escopo `workflow`) para disparar deploy em dev.
- **`deploy-on-tag.yml`**: Deploya tag estável em dev — chamado pelo release automático ou manualmente.
- **`deploy-branch-dev.yml`**: Para branches/SHAs não mergeados. Cria RC tag rastreável, deploya em dev, limpa imagens RC antigas do `nossalista-dev` (mantém 3).
- **`deploy-prod.yml`**: Deploy em prod, disparado manualmente via `workflow_dispatch` com uma tag semântica estável (valida formato `vX.Y.Z` e existência da tag antes do deploy). Sem gate de aprovação manual.
- **`rollback-prod.yml`**: Rollback de prod para uma tag semântica estável **anterior**. Espelha o `deploy-prod.yml` (valida tag `vX.Y.Z`, reusa `deploy-environment.yml`) — só muda a semântica: reimplanta uma release já conhecida. Compartilha o `concurrency: group: deploy-prod` com o `deploy-prod.yml` para serializar operações em prod.
- **`frontend-e2e-fullstack.yml`**: Suíte E2E navegador↔backend, hoje só por `workflow_dispatch` (cron noturno DESABILITADO por billing — ver abaixo). Possui notificação de falha (`if: failure()` abre issue rotulada `ci-failure` via `gh`) e cache dos browsers Playwright (`~/.cache/ms-playwright`, key por hash do `package-lock.json`).
- **`osv-scanner.yml`**: SCA de dependências via **OSV-Scanner** (base OSV.dev), cobrindo backend (Maven, `backend/pom.xml`) e frontend (npm, `frontend/package-lock.json`) num único scan recursivo. Substitui o antigo OWASP dependency-check/NVD, removido por falhas espúrias de "cache frio" (ver `docs/DECISIONS.md` D-027 e issue #70). Em **PR** reporta só vulnerabilidades **novas** do diff; em **push na `main` + cron semanal** faz scan completo; publica **SARIF** na aba Security. Complementado por Dependabot (`.github/dependabot.yml`, Maven + npm + github-actions). Os demais checks de segurança (gitleaks, editorconfig-checker, Semgrep, `npm audit`, license-checker) seguem no job `security-and-compliance` do `ci.yml`. Fase de validação: **não** é required check em branch protection ainda (decisão do dono após comparar findings).
- Todos os workflows de deploy publicam um `Deployment Summary` ao final da execução no GitHub Actions.
- `tag` em workflows de deploy significa **tag da imagem implantada**; `ref` significa **ref do checkout que será reconstruído**.
- O workflow de deploy aplica manifestos estruturais e depois força a imagem do Deployment com `kubectl set image`, além de registrar `deploy.nossalista/tag` e `deploy.nossalista/sha` via annotations.
- **`GHCR_PAT` é obrigatório** no `deploy-environment.yml`: o job `build-and-push` falha cedo, com mensagem clara, se o secret estiver vazio (sem fallback silencioso para `github.token`, que não tem `write:packages`).
- **Fonte única da metadata de build**: `APP_VERSION`/`APP_GIT_TAG`/`APP_GIT_SHA`/`APP_BUILD_TIME`/`APP_ENVIRONMENT` são embutidos como `ENV` na **imagem** (Dockerfile, a partir dos build-args). O deploy **não** reinjeta via `kubectl set env` (eliminada a dupla fonte de verdade) e os manifestos **não** declaram placeholders estáticos dessa metadata. As annotations `deploy.nossalista/tag|sha` seguem como registro auditável.
- **Profile Spring por ambiente**: ambos os Deployments setam `SPRING_PROFILES_ACTIVE` declarativamente (`dev` em `k8s/dev`, `prod` em `k8s/prod`).

### Fluxo de Deploy (Regra Obrigatória)

```
push main → CI passa → release.yml
              └─ cria tag semântica v1.2.x
              └─ gh workflow run deploy-on-tag.yml (GHCR_PAT) → deploy dev

workflow_dispatch → deploy-branch-dev.yml (para branches/SHAs não mergeados)
              └─ cria RC tag v1.2.x-rc.{sha} (pre-release)
              └─ deploy-environment(dev, v1.2.x-rc.{sha})
              └─ limpa RC tags antigas (mantém 10 tags / 3 imagens nossalista-dev)

workflow_dispatch → deploy-prod.yml (com tag semântica estável)
              └─ deploy-environment(prod, v1.2.x)

workflow_dispatch → rollback-prod.yml (com tag semântica estável ANTERIOR)
              └─ deploy-environment(prod, v1.2.x-anterior)  # reimplanta release conhecida
```

**Regras:**

- `deploy-environment.yml` é o único lugar com lógica de build/deploy — não duplicar.
- `deploy-branch-dev.yml` é para testar branches/SHAs ainda **não** mergeados — sempre gera uma RC tag rastreável.
- Prod **sempre** recebe uma tag semântica estável (`v1.2.x`), nunca uma RC.
- RC tags são pre-releases e **não** aparecem como "Latest" no GitHub Releases.
- **Nunca** usar `deploy-branch-dev.yml` para promover código a prod — ele não gera tag semântica.

### Comandos de Deploy

```bash
# Deploy manual (se necessário)
kubectl apply -f k8s/

# Ver status do deployment
kubectl get pods -n nossalista
kubectl logs -f deployment/nossalista -n nossalista
kubectl get deployment/nossalista -n nossalista -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
curl https://nossalista.leoferolive.com.br/api/health

# Restart do deployment
kubectl rollout restart deployment/nossalista -n nossalista
```

### Workflows de Deploy

```bash
# Testar branch/SHA em dev (gera RC tag auditável)
gh workflow run deploy-branch-dev.yml --field ref=<branch-ou-sha>

# Deploy em produção (requer tag semântica estável; sem aprovação manual)
gh workflow run deploy-prod.yml --field tag=v1.2.3

# Rollback de produção para uma release estável anterior (sem aprovação manual)
gh workflow run rollback-prod.yml --field tag=v1.2.2
```

## Decisões Arquiteturais Importantes

- **Campos por tipo de lista**: Colunas nullable em `list_items` (simples para MVP, migrar para JSONB se necessário)
- **Auth**: JWT stateless (compatível com WebSocket)
- **Estado frontend**: React Context + hooks (sem Redux no MVP)
- **Migrations**: Flyway para versionamento de schema
- **Roteamento**:
  - `/` → Frontend (SPA)
  - `/api/**` → Backend REST
  - `/ws/**` → Backend WebSocket

## Fases de Implementação

O roadmap define 5 fases sequenciais:

1. **Fundação Backend**: Setup, modelo de dados, Spring Security, CRUD básico
2. **Compartilhamento**: Convites, membros, activity log
3. **Real-time**: WebSocket config, broadcast de alterações
4. **Frontend**: React app, auth flow, telas principais
5. **Infra & Deploy**: Docker, K3s, CI/CD, Cloudflare Tunnel
