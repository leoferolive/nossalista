# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Sobre o Projeto

NossaLista é um aplicativo web de listas compartilhadas em tempo real. O projeto está atualmente em fase de planejamento MVP e ainda não possui código implementado.

## Stack Técnico Planejada

| Camada       | Tecnologia                           |
|-------------|--------------------------------------|
| Frontend    | React 19 + TypeScript + Vite        |
| Backend     | Java 25 + Spring Boot 4              |
| Real-time   | Spring WebSocket (STOMP + SockJS)    |
| Auth        | Google OAuth2 + email/senha          |
| BD Produção | PostgreSQL                           |
| BD Local    | H2 (testes/dev)                      |
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
├── list/            (Lista, ListType, controller, service, repository)
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

## Deploy e Infraestrutura

### Kubernetes (k8s/)
- `deployment.yaml`: Deployment com 1 réplica, health checks em `/actuator/health`
- `service.yaml`: Service tipo ClusterIP
- `ingress.yaml`: Ingress Traefik para `nossalista.leoferolive.com.br`
- `namespace.yaml`: Namespace dedicado `nossalista`

### CI/CD
O pipeline em `.github/workflows/deploy.yml` utiliza um workflow reutilizável de `self-workflows` que:
- Build da imagem Docker (Java 21)
- Push para GitHub Container Registry (`ghcr.io/leoferolive/nossalista`)
- Deploy no cluster K3s via kubectl

### Comandos de Deploy
```bash
# Deploy manual (se necessário)
kubectl apply -f k8s/

# Ver status do deployment
kubectl get pods -n nossalista
kubectl logs -f deployment/nossalista -n nossalista

# Restart do deployment
kubectl rollout restart deployment/nossalista -n nossalista
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
