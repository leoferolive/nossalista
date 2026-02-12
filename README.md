# NossaLista

> Aplicativo web de listas compartilhadas em tempo real

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![CI](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml/badge.svg)](https://github.com/leoferolive/nossalista/actions/workflows/ci.yml)

## 📋 Sobre o Projeto

NossaLista é um aplicativo web colaborativo que permite criar e compartilhar listas em tempo real. Ideal para listas de compras compartilhadas em família, tarefas domésticas, wishlists ou qualquer tipo de lista que precise ser gerenciada por múltiplas pessoas simultaneamente.

### Status do Projeto

⚠️ **Em Desenvolvimento** - O projeto está atualmente em fase de planejamento MVP.

## 🚀 Funcionalidades Principais

### MVP Planejado

- **Autenticação Flexível**
  - Login com Google OAuth2 (método principal)
  - Email/senha como alternativa

- **Tipos de Lista**
  - Lista de Compras (com quantidade e unidade)
  - Lista de Tarefas (com prioridade e prazo)
  - Wishlist (com links e preço estimado)
  - Lista Genérica (personalizável)

- **Compartilhamento**
  - Convite por username
  - Convite por link único
  - Controle de permissões (owner, editor, viewer)

- **Sincronização Real-time**
  - Atualizações instantâneas via WebSocket
  - Indicadores de usuários ativos
  - Notificações de alterações

- **Activity Log**
  - Timeline completa de ações na lista
  - Rastreabilidade de alterações

## 🛠️ Stack Tecnológico

| Camada       | Tecnologia                           |
|-------------|--------------------------------------|
| Frontend    | React 19 + TypeScript + Vite        |
| Backend     | Java 25 + Spring Boot 4              |
| Real-time   | Spring WebSocket (STOMP + SockJS)    |
| Autenticação| JWT + OAuth2 (Google)                |
| BD Produção | PostgreSQL                           |
| BD Dev      | PostgreSQL (Docker Compose)          |
| BD Testes   | H2 (MODE=PostgreSQL)                 |
| Migrations  | Flyway                               |
| Infra       | Raspberry Pi 4 + K3s + Cloudflare Tunnel |

## 📁 Estrutura do Projeto (Monorepo)

```
nossalista/
├── backend/                 # Backend Spring Boot 4
│   └── src/main/java/br/com/leoferolive/nossalista/
│       ├── config/          # Security, WebSocket, CORS
│       ├── auth/            # AuthController, AuthService, JWT
│       ├── user/            # User entity e CRUD
│       ├── list/            # Lista, ListType, CRUD
│       ├── item/            # ListItem entity
│       ├── member/          # ListMember, convites
│       ├── activity/        # ActivityLog
│       └── websocket/       # STOMP controllers
├── frontend/                # Frontend React 19
│   └── src/
│       ├── api/             # Axios client, endpoints
│       ├── hooks/           # useAuth, useLists, useWebSocket
│       ├── pages/           # Login, Home, ListView
│       ├── components/      # Componentes reutilizáveis
│       ├── contexts/        # AuthContext
│       └── types/           # TypeScript types
├── deploy/                  # Scripts e configs de deploy
├── k8s/                     # Manifests Kubernetes
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   └── namespace.yaml
├── docs/                    # Documentação do projeto
│   └── NossaLista — Documento de Escopo MVP.txt
└── .github/workflows/       # CI/CD
```

## 🏃 Como Executar

### Pré-requisitos

- Java 25 (LTS)
- Maven 3.9+
- Node.js 18+ e npm
- Docker (opcional, para containerização)
- Kubernetes K3s (para deploy em produção)
- kubectl (para gerenciar cluster)

### Backend (Spring Boot 4)

```bash
# 1. Subir PostgreSQL para desenvolvimento
docker compose up -d

# 2. Executar com Maven (perfil dev usa PostgreSQL local)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O backend estará disponível em `http://localhost:8080`

Endpoints úteis:
- Health check: `http://localhost:8080/api/health`
- API docs: `http://localhost:8080/api` (quando disponível)

### Frontend (React 19 + Vite)

```bash
cd frontend

# Instalar dependências
npm install

# Executar em modo dev
npm run dev
```

O frontend estará disponível em `http://localhost:5173`

### Docker Compose (PostgreSQL dev)

```bash
# Subir PostgreSQL para desenvolvimento
docker compose up -d

# Ver logs do banco
docker compose logs -f postgres

# Parar e manter dados
docker compose down

# Parar e apagar dados (reset completo)
docker compose down -v
```

## 🚢 Deploy

### Kubernetes (Produção)

O projeto está configurado para deploy em cluster Kubernetes usando K3s:

```bash
# Aplicar manifests
kubectl apply -f k8s/

# Ver status dos pods
kubectl get pods -n nossalista

# Ver logs
kubectl logs -f deployment/nossalista -n nossalista

# Restart do deployment
kubectl rollout restart deployment/nossalista -n nossalista
```

### CI/CD

O pipeline em `.github/workflows/deploy.yml` automatiza:
1. Build da imagem Docker
2. Push para GitHub Container Registry
3. Deploy no cluster K3s via kubectl

### Infraestrutura

- **Hardware**: Raspberry Pi 4
- **Cluster**: K3s (Kubernetes leve)
- **Tunnel**: Cloudflare Tunnel para exposição externa
- **Domínio**: nossalista.leoferolive.com.br

## 📖 Documentação

Para informações detalhadas sobre:
- Arquitetura do sistema
- Modelo de dados (ERD)
- Especificação de APIs REST
- Protocolos WebSocket
- Roadmap de implementação

Consulte `docs/NossaLista — Documento de Escopo MVP.txt`

## 🗺️ Roadmap de Implementação

1. **Fundação Backend** - Setup, modelo de dados, Spring Security, CRUD básico
2. **Compartilhamento** - Convites, membros, activity log
3. **Real-time** - WebSocket config, broadcast de alterações
4. **Frontend** - React app, auth flow, telas principais
5. **Infra & Deploy** - Docker, K3s, CI/CD, Cloudflare Tunnel

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 👤 Autor

**Leonardo Oliveira**

- GitHub: [@leoferolive](https://github.com/leoferolive)

---

⭐ Se este projeto foi útil para você, considere dar uma estrela!
