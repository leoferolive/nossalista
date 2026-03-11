# NossaLista

Aplicativo web de listas compartilhadas em tempo real.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![CI](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml)

## Sobre o projeto

NossaLista permite criar, compartilhar e editar listas colaborativas com sincronizacao em tempo real.
Usuarios novos recebem um tutorial guiado no primeiro login para aprender criacao, compartilhamento, edicao e acompanhamento realtime.

Status atual:
- MVP em desenvolvimento ativo
- Backend e frontend implementados no monorepo
- Onboarding guiado no primeiro login (com replay manual no menu da conta)
- Redesign global do frontend com linguagem `paper tech editorial`, paridade light/dark e switch de tema exposto na UI
- Landing page publica com CTAs separados para cadastro e login em modais distintos
- CI ativo para frontend e backend
- Release automática em `main` com tag SemVer patch e deploy automático em `dev`
- Deploy manual em `dev` para branches/SHAs não mergeados via RC tag auditável
- Deploy manual em `prod` com aprovação de environment e tag estável existente

## Stack

| Camada | Tecnologia |
| --- | --- |
| Frontend | React 19 + TypeScript + Vite |
| Backend | Java 25 + Spring Boot 4 |
| Real-time | Spring WebSocket (STOMP + SockJS) |
| Auth | Google OAuth2 + email/senha + JWT |
| BD Producao | PostgreSQL |
| BD Dev | PostgreSQL |
| BD Testes | H2 (MODE=PostgreSQL) |
| Migrations | Flyway |
| Infra | Raspberry Pi 4 + K3s + Cloudflare Tunnel |

## Estrutura

```text
nossalista/
|- backend/
|- frontend/
|- docs/
|- contracts/
|- k8s/
|- docker-compose.yml
|- CLAUDE.md
|- AGENTS.md -> CLAUDE.md
```

## Como executar

### Pre-requisitos

- Java 25
- Node.js 22+
- Docker e Docker Compose

### 1) Banco local (PostgreSQL)

```bash
docker compose up -d
```

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

## Qualidade e testes

## Experiencia de interface

- Tema visual oficial do frontend: `paper tech editorial`
- Modos `light` e `dark` compartilham a mesma linguagem visual e os mesmos componentes
- A landing usa dois fluxos distintos:
  - CTA principal abre cadastro
  - CTA secundario abre login
- Modais e formularios principais seguem o mesmo padrao de textbox, label, helper text, feedback e hierarquia de botoes

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
npm run test:e2e
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

- Frontend: ESLint, Prettier, Stylelint, TypeScript (`tsc --noEmit`), Vitest com coverage >= 80%, build, bundle budget e smoke E2E com Playwright.
- Backend: `verify` com Checkstyle, PMD, SpotBugs, ArchUnit, JaCoCo (>= 80% linhas e >= 75% branches), build e suite de regressao.
- Seguranca e compliance: EditorConfig check, gitleaks, semgrep, npm audit (high+), licencas e OWASP Dependency-Check.
- Smoke backend: subida do jar com profile `ci` e validacao de `/actuator/health`.

## Deploy

- Manifests Kubernetes em `k8s/`
- Ambientes:
  - Dev: `release.yml` cria `vX.Y.Z` e dispara `deploy-on-tag.yml`; para branches/SHAs não mergeados, usar `deploy-branch-dev.yml`, que gera `vX.Y.Z-rc.<sha>`
  - Prod: `deploy-prod.yml` promove uma tag estável existente (`vX.Y.Z`) com aprovação obrigatória no environment `production`
- Fonte de verdade da versão implantada:
  - imagem do Deployment recebe explicitamente `ghcr.io/...:<tag>`
  - annotations `deploy.nossalista/tag` e `deploy.nossalista/sha` são atualizadas no cluster
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
