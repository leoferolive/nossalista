# Arquitetura de Deploy

## 1. Estratégia de Imagem Única (Monolítica)

O frontend React é compilado e embutido no JAR do Spring Boot. Um único container serve tanto a API quanto o SPA.

```
Docker multi-stage build:
  Stage 1 (node:22-alpine):
    - npm ci
    - npm run build → /app/dist

  Stage 2 (maven:3.9-eclipse-temurin-25):
    - COPY backend/pom.xml
    - mvn dependency:go-offline
    - COPY backend/src
    - COPY --from=stage1 /app/dist → src/main/resources/static/
    - mvn clean package -DskipTests

  Stage 3 (eclipse-temurin:25-jre):
    - apt-get install curl (para o HEALTHCHECK; base não traz curl/wget)
    - COPY --from=stage2 /app/app.jar app.jar  (jar executável selecionado no builder)
    - USER appuser
    - ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Resultado:** Spring Boot serve `index.html` e assets estáticos em `/`, e a API em `/api/**`.

## 2. Tabela de Ambientes

| Atributo                | Dev                                      | Prod                                          |
|-------------------------|------------------------------------------|-----------------------------------------------|
| Domínio                 | `nossalista.home`                        | `nossalista.leoferolive.com.br`               |
| Namespace K8s           | `nossalista-dev`                         | `nossalista`                                  |
| Image tag               | `ghcr.io/leoferolive/nossalista-dev:latest` | `ghcr.io/leoferolive/nossalista:latest` + `vX.Y.Z` |
| Trigger deploy          | `workflow_dispatch` (`deploy-branch-dev.yml` ou `deploy-on-tag.yml`) | `workflow_dispatch` (`deploy-prod.yml`) + approval no environment `production` |
| Spring profile          | `dev`                                    | `prod`                                        |
| Banco de dados          | `nossalista_dev` (postgres.database)     | `nossalista` (postgres.database)              |
| Acesso                  | Rede local + Tailnet (`:8443`)           | Internet (Cloudflare Tunnel)                  |
| HTTPS                   | Não (HTTP apenas)                        | Sim (via Cloudflare)                          |
| Google OAuth URI        | `http://nossalista.home/api/auth/google/callback` | `https://nossalista.leoferolive.com.br/api/auth/google/callback` |

## 3. Fluxo de Deploy

```
Dev:
  workflow_dispatch deploy-branch-dev.yml (branch/SHA)
    → cria RC tag vX.Y.Z-rc.<sha>
    → GitHub Actions (deploy-environment.yml)
    → Docker build ARM64 (multi-stage)
    → push ghcr.io/leoferolive/nossalista-dev:latest
    → kubectl apply k8s/dev/
    → Pod reiniciado com nova imagem

  workflow_dispatch deploy-on-tag.yml (tag estavel + sha)
    → valida alinhamento tag/ref
    → GitHub Actions (deploy-environment.yml)
    → Docker build ARM64 (multi-stage)
    → push ghcr.io/leoferolive/nossalista-dev:latest + ghcr.io/leoferolive/nossalista-dev:vX.Y.Z
    → kubectl apply k8s/dev/
    → Pod reiniciado com nova imagem

Prod:
  workflow_dispatch deploy-prod.yml (tag estavel)
    → valida formato/existencia da tag
    → aguarda approval no environment production
    → GitHub Actions (deploy-environment.yml)
    → Docker build ARM64 (multi-stage)
    → push ghcr.io/leoferolive/nossalista:latest + ghcr.io/leoferolive/nossalista:vX.Y.Z
    → kubectl apply k8s/prod/
    → Pod reiniciado com nova imagem
```

## 4. Diagrama de Rede

```
Internet
  │
  └─ Cloudflare Tunnel ─────► cloudflared (ns: cloudflared)
                                   │
                                   ▼
                          nossalista.nossalista.svc:80
                                   │
                                   ▼
                          Pod: nossalista:8080 (ns: nossalista)
                                   │
                          ┌────────┴────────┐
                          │                 │
                       GET /             GET /api/**
                       (React SPA)       (Spring Boot API)

Rede local
  │
  └─ DNS nossalista.home → 192.168.3.63
         │
         ▼
    Traefik (porta 80, entry: web)
         │
         ▼
    nossalista-dev.nossalista-dev.svc:80
         │
         ▼
    Pod: nossalista-dev:8080 (ns: nossalista-dev)
```

## 5. Componentes da Infraestrutura

### Cluster K3s
- **Versão:** k3s v1.34.3
- **Nó:** `leo-ubuntu` (Raspberry Pi 4)
- **Arquitetura:** ARM64

### Traefik
- **Namespace:** `traefik-system`
- **IngressClass:** `traefik`
- **Entrypoint:** `web` (porta 80)

### PostgreSQL
- **Namespace:** `database`
- **Imagem:** `postgres:17`
- **NodePort:** 30001 (acesso externo/dev local: `192.168.3.63:30001`)
- **DNS interno:** `postgres.database.svc.cluster.local:5432`
- **Secret:** `postgres-secret` (user/db: `root`)

### Cloudflare Tunnel
- **Namespace:** `cloudflared`
- **Rota atual:** `demo-api.leoferolive.com.br`
- **Nova rota a adicionar:** `nossalista.leoferolive.com.br`

### GHCR (Registry)
- **Imagens:** `ghcr.io/leoferolive/nossalista` e `ghcr.io/leoferolive/nossalista-dev`
- **Secret de pull:** `ghcr-secret` (precisa ser replicado em `nossalista-dev` e `nossalista`)
