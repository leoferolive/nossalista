# NossaLista

Aplicativo web de listas compartilhadas em tempo real.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![CI](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml)

## Sobre o projeto

NossaLista permite criar, compartilhar e editar listas colaborativas com sincronizacao em tempo real.
Usuarios novos recebem um tutorial guiado no primeiro login para aprender criacao, compartilhamento, edicao e acompanhamento realtime.
O sistema suporta notificacoes online em tempo real e push do navegador opcional (ativacao/desativacao pelo usuario no menu da conta).

Status atual:

- MVP em desenvolvimento ativo
- Backend e frontend implementados no monorepo
- Onboarding guiado no primeiro login (com replay manual no menu da conta)
- Personal Access Tokens (PAT) para autenticar clientes MCP/API externos, gerenciaveis em "Conexoes (API/Assistentes)" no menu da conta
- Rebranding global do frontend com linguagem `Fresh Lists`, paridade light/dark e switch de tema exposto na UI
- Shell autenticado mobile refeito com headers compactos, sheets para conta/notificacoes e acoes de lista reorganizadas
- Landing page publica com CTAs separados para cadastro e login em modais distintos
- Fluxo deslogado centralizado na landing (`/`), com `/login` mantido apenas como redirecionamento legado para `/?auth=login`
- CI ativo para frontend e backend
- Release automática em `main` com tag SemVer patch e deploy automático em `dev`
- Deploy manual em `dev` para branches/SHAs não mergeados via RC tag auditável
- Deploy manual em `prod` com aprovação de environment e tag estável existente

## Stack

| Camada      | Tecnologia                               |
| ----------- | ---------------------------------------- |
| Frontend    | React 19 + TypeScript + Vite             |
| Backend     | Java 25 + Spring Boot 4                  |
| Real-time   | Spring WebSocket (STOMP + SockJS)        |
| Auth        | Google OAuth2 + email/senha + JWT        |
| BD Producao | PostgreSQL                               |
| BD Dev      | PostgreSQL                               |
| BD Testes   | H2 (MODE=PostgreSQL)                     |
| Migrations  | Flyway                                   |
| Infra       | Raspberry Pi 4 + K3s + Cloudflare Tunnel |

## Estrutura

```text
nossalista/
|- backend/
|- frontend/
|- docs/
|- .agents/skills/
|- contracts/
|- k8s/
|- docker-compose.yml
|- CLAUDE.md
|- AGENTS.md -> CLAUDE.md
```

## Servidor MCP

O backend expoe um servidor [MCP](https://modelcontextprotocol.io) embutido em `POST /mcp`
(Streamable HTTP), autenticado por OAuth 2.1 (Authorization Code + PKCE — claude.ai e
Claude Code conectam via "Add connector" sem copiar credencial manualmente), Personal
Access Token (`nlmcp_...`) ou JWT, para conectar assistentes de IA as listas do usuario.
Guia de conexao, tools disponiveis e modelo de seguranca em `docs/mcp.md`.

## Skills locais

- `.agents/skills/interface-design`: skill local para projetar e auditar interfaces de produto com memoria em `.interface-design/system.md`, mantendo consistencia de espacamento, profundidade, superficies e padroes de componentes.

## Como executar

### Pre-requisitos

- Java 25
- Node.js 22+
- Docker e Docker Compose

### 1) Banco local (PostgreSQL)

```bash
docker compose up -d
```

O `docker-compose.yml` sobe apenas o Postgres de desenvolvimento. As credenciais sao
parametrizadas por variaveis de ambiente com defaults de dev, e a porta e exposta
somente no loopback (`127.0.0.1:5432`), nunca em toda a rede:

| Variavel            | Default (dev)   |
| ------------------- | --------------- |
| `POSTGRES_DB`       | `nossalista_dev` |
| `POSTGRES_USER`     | `nossalista`     |
| `POSTGRES_PASSWORD` | `nossalista`     |

`docker compose up` funciona sem configuracao adicional (usa os defaults). Para
sobrescrever, copie `.env.example` para `.env` na raiz e ajuste os valores — o
`.env` e ignorado pelo git e pelo build da imagem (`.dockerignore`).

### 2) Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend em `http://localhost:8080`.

### 3) Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend em `http://localhost:5173`.

Para subir apenas o frontend com API mock em memoria:

```bash
cd frontend
npm install
npm run dev:mock
```

## Quality Gate

Validação unificada de qualidade (lint + types + cobertura + complexidade + ratchet):

```bash
./scripts/quality.sh --pre-commit   # subset rápido (<= 30s)
./scripts/quality.sh --full         # tudo + ratchet de baseline
```

Roda nos dois ecossistemas (Maven + npm), produz uma tabela `OK/regrediu`, e
falha se alguma métrica regrediu vs. `.quality-baseline/*.json`. Disparado
automaticamente via Husky no `git commit`. Detalhes em
[`docs/quality-gate.md`](docs/quality-gate.md). Dívida pré-existente em
[`docs/quality-gate-debt.md`](docs/quality-gate-debt.md).

## Qualidade e testes

## Experiencia de interface

- Tema visual oficial do frontend: `Fresh Lists`
- Direcao visual: `Playful Editorial` na identidade unificada `leoferolive design`, com paleta base `violeta + teal`
- Modos `light` e `dark` compartilham a mesma linguagem visual e os mesmos componentes
- A landing usa dois fluxos distintos:
  - CTA principal abre cadastro
  - CTA secundario abre login
- A landing publica deve permanecer minimalista: hero curto, preview principal unico e apoio enxuto
- Modais e formularios principais seguem o mesmo padrao de textbox, label, helper text, feedback e hierarquia de botoes
- A area autenticada mobile agora prioriza conteudo acima da dobra:
  - headers compactos
  - menu da conta e notificacoes em bottom sheet
  - acoes secundarias em overflow quando necessario
  - cards da Home com alvo de toque primario no card inteiro, busca local por nome/tipo e estados informativos sem checkbox decorativo
  - escala compacta em cards/sheets/action rows para reduzir scroll e friccao
- Rotas protegidas com usuario deslogado redirecionam para `/` (nao mais para `/login?redirect=...`)
- Contrato de URL para orquestrar auth na landing:
  - `/?auth=login|register`
  - `/?auth=login&registered=1&email=...`

### Frontend

```bash
cd frontend
npm run lint
npm run format:check
npm run stylelint
npm run typecheck
npm run test:coverage
npm run build
npm run bundle:check
npm run test:e2e:pr
npm run test:e2e:fullstack
```

### Backend

```bash
cd backend
./mvnw -B -Pstrict-quality verify
./mvnw -B -Pregression-tests test
./mvnw -B -DskipTests package
java -jar target/nossalista-0.0.1-SNAPSHOT.jar --spring.profiles.active=ci
```

Detalhes de quality gate do backend em `backend/QUALITY.md`.

## Gates de PR (bloqueantes)

- Frontend: ESLint, Prettier, Stylelint, TypeScript (`tsc --noEmit`), Vitest com coverage >= 80%, build, bundle budget e suite E2E Playwright `@pr` bloqueante.
- Backend: `verify` com Checkstyle, PMD, SpotBugs, ArchUnit, JaCoCo (>= 80% linhas e >= 75% branches), build e suite de regressao.
- Seguranca e compliance: EditorConfig check, gitleaks, semgrep, npm audit (high+), licencas e OWASP Dependency-Check.
- Smoke backend: subida do jar com profile `ci` e validacao de `/actuator/health`.
- Full-stack E2E: workflow `frontend-e2e-fullstack.yml` roda diariamente (06:00 UTC / 03:00 America/Sao_Paulo) e tambem via `workflow_dispatch`.

## Deploy

- Manifests Kubernetes em `k8s/`
- Ambientes:
  - Dev: `release.yml` cria `vX.Y.Z` e dispara `deploy-on-tag.yml`; para branches/SHAs não mergeados, usar `deploy-branch-dev.yml`, que gera `vX.Y.Z-rc.<sha>`
  - Prod: `deploy-prod.yml` promove uma tag estável existente (`vX.Y.Z`) com aprovação obrigatória no environment `production`
- Fonte de verdade da versão implantada:
  - imagem do Deployment recebe explicitamente `ghcr.io/...:<tag>`
  - annotations `deploy.nossalista/tag` e `deploy.nossalista/sha` são atualizadas no cluster
  - cada workflow de deploy publica `Deployment Summary` no GitHub Actions com `Tag version deployada`, imagem, SHA e ambiente
  - `GET /api/health` retorna `version`, `gitSha`, `gitTag`, `environment` e `buildTime`
- Imagens publicadas:
  - Dev: `ghcr.io/leoferolive/nossalista-dev:latest` + tags RC/estáveis para rastreabilidade
  - Prod: `ghcr.io/leoferolive/nossalista:latest` + tag estável `vX.Y.Z`
- Guardrails de deploy:
  - `deploy-branch-dev.yml`: `ref` é obrigatório (sem default), evitando deploy acidental de `main`
  - `deploy-on-tag.yml`: valida formato de tag estável e se `tag` e `ref` apontam para o mesmo commit
  - `deploy-prod.yml`: aceita apenas tag estável `vX.Y.Z` existente no repositório

- Operação manual (fallback):

```bash
kubectl apply -f k8s/dev/
kubectl apply -f k8s/prod/
kubectl get pods -n nossalista-dev
kubectl get pods -n nossalista
kubectl get deployment nossalista-dev -n nossalista-dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
curl http://nossalista.home/api/health
curl https://nossalista.leoferolive.com.br/api/health
```

## Documentacao canonica

- `README.md`
- `CLAUDE.md` e `AGENTS.md`
- `docs/**`
- `backend/QUALITY.md`
- `frontend/README.md`

Indice rapido de docs em `docs/README.md`.
