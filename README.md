# NossaLista

Aplicativo web de listas compartilhadas em tempo real.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![CI](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml)

## Sobre o projeto

NossaLista permite criar, compartilhar e editar listas colaborativas com sincronizacao em tempo real.

Status atual:
- MVP em desenvolvimento ativo
- Backend e frontend implementados no monorepo
- CI ativo para frontend e backend
- Workflow de deploy remoto temporariamente desativado em `.github/workflows/deploy.yml`

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
npm run test -- --run
npm run build
```

### Backend

```bash
cd backend
./mvnw -B verify
./mvnw -B -Pstrict-quality verify
./mvnw -B -Pregression-tests test
```

Detalhes de quality gate do backend em `backend/QUALITY.md`.

## Deploy

- Manifests Kubernetes em `k8s/`
- Deploy manual:

```bash
kubectl apply -f k8s/
kubectl get pods -n nossalista
kubectl logs -f deployment/nossalista -n nossalista
```

- Observacao: o workflow `.github/workflows/deploy.yml` esta comentado (desativado temporariamente).

## Documentacao canonica

- `README.md`
- `CLAUDE.md` e `AGENTS.md`
- `docs/**`
- `backend/QUALITY.md`
- `frontend/README.md`

Indice rapido de docs em `docs/README.md`.
