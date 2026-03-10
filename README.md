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
- CI ativo para frontend e backend
- Deploy dev automático para branches `release/*`
- Release automática em `main` com tag SemVer patch e deploy prod com aprovação de environment

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
  - Dev:
    - `deploy-branch-dev.yml` (manual): testa branch/SHA com RC tag rastreável em `nossalista-dev`
    - `deploy-on-tag.yml` (manual/automático via `release.yml`): deploy em dev de tag estável `vX.Y.Z`
  - Prod:
    - `deploy-prod.yml` (manual): deploy de tag estável `vX.Y.Z` com aprovação obrigatória no environment `production`
  - Release:
    - `release.yml` (automático após CI em `main`) cria/reutiliza tag estável e dispara `deploy-on-tag.yml`

- Imagens:
  - Dev: `ghcr.io/leoferolive/nossalista-dev:latest` (e tags RC/estáveis para rastreabilidade)
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
```

## Documentacao canonica

- `README.md`
- `CLAUDE.md` e `AGENTS.md`
- `docs/**`
- `backend/QUALITY.md`
- `frontend/README.md`

Indice rapido de docs em `docs/README.md`.
