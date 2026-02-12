---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
lastStep: 8
status: 'complete'
completedAt: '2026-02-10'
inputDocuments:
  - _bmad-output/planning-artifacts/prd.md
  - _bmad-output/planning-artifacts/prd-validation-report.md
  - _bmad-output/planning-artifacts/ux-design-specification.md
  - docs/NossaLista — Documento de Escopo MVP.txt
workflowType: 'architecture'
project_name: 'nossalista'
user_name: 'Leo'
date: '2026-02-10'
communication_language: 'Portuguese'
document_output_language: 'Portuguese'
classification:
  projectType: web_app
  domain: general
  complexity: low
  projectContext: brownfield
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

---

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**

O projeto define **45 Functional Requirements** organizados em 6 domínios principais:

1. **Authentication & User Management (FR1-7):** Sistema de autenticação dual com Google OAuth2 (primário) e email/senha (fallback), usando JWT stateless. Username único serve como identificador para convites.

2. **List Management (FR8-14):** CRUD completo de listas com 4 tipos pré-definidos (Compras, Tarefas, Wishlist, Genérica). Cada tipo define quais campos estão disponíveis nos itens. Distinção clara entre dono (OWNER) e participante (MEMBER).

3. **Item Management (FR15-22):** Gestão completa de itens com campos dinâmicos baseados no tipo de lista. Suporte a checkbox de conclusão, quantidade, prazos e URLs. Rastreamento de criador de cada item.

4. **Sharing & Collaboration (FR23-30):** Sistema de convites por username e links com expiração. Todos os participantes têm permissão total para gerenciar itens. Dono pode remover participantes; membros podem sair da lista.

5. **Real-time Synchronization (FR31-38):** ⚡ **CRÍTICO** - 100% das atualizações são sincronizadas via WebSocket (STOMP/SockJS) com latência < 500ms. Sistema mantém conexão com reconexão automática e indicação de membros online.

6. **Activity & History (FR39-45):** Timeline de atividades rastreando quem fez o quê e quando. Todas as ações (adicionar, editar, remover, marcar) são registradas com autor e timestamp.

**Implicações Arquiteturais dos FRs:**
- **Stateless auth necessário:** JWT é obrigatório para funcionar com WebSocket
- **Broadcast pattern:** WebSocket precisa enviar atualizações para todos os participantes conectados
- **Dynamic fields:** Schema flexível ou campos nullable para diferentes tipos de lista
- **Multi-tenancy simples:** Listas pertencem a usuários mas têm múltiplos participantes

**Non-Functional Requirements:**

**Performance (NFR-P1 a NFR-P3):**
- Latência WebSocket < 500ms entre usuários
- Time to Interactive < 3 segundos (4G)
- First Contentful Paint < 1.5 segundos
- Bundle size inicial < 200KB gzipped

**Security (NFR-S1 a NFR-S7):**
- HTTPS via Cloudflare Tunnel
- Senhas hasheadas (bcrypt/argon2)
- JWT com expiração máxima de 7 dias
- OAuth2 com fluxo PKCE
- CORS configurado para domínio único
- Links de convite expiram em 24 horas

**Reliability (NFR-R1 a NFR-R5):**
- Uptime > 95% mensal (realista para home server)
- Logs de aplicação por 30 dias
- Backup diário do PostgreSQL
- Pods K3s restartam automaticamente
- Deploy sem downtime > 2 minutos

**Accessibility (NFR-A1 a NFR-A5):**
- WCAG AA (contraste mínimo 4.5:1)
- Navegação completa por teclado
- Labels descritivos em inputs/botões
- Touch targets ≥ 44×44 pixels
- Chrome últimos 2 anos (primário), Safari/Firefox best-effort

**Integration (NFR-I1 a NFR-I4):**
- Google OAuth2 em produção
- WebSocket (STOMP/SockJS) através do Cloudflare Tunnel
- GitHub Actions deploy automático para K3s
- PostgreSQL persiste em volume do cluster

**NFRs que Moldarão a Arquitetura:**
1. **Real-time performance:** Arquitetura precisa ser otimizada para latência < 500ms
2. **Stateless authentication:** JWT essencial para WebSocket funcionar
3. **Infrastructure constraints:** Recursos limitados do Raspberry Pi requerem arquitetura eficiente
4. **Tunnel compatibility:** WebSocket precisa sobreviver através do Cloudflare Tunnel

**Scale & Complexity:**

- **Complexidade do Projeto:** BAIXA
  - Requisitos bem definidos e delimitados
  - Stack tecnológica moderna mas padrão da indústria
  - Sem requisitos regulatórios complexos
  - Single-domain com multi-tenancy simples
  - Projeto de aprendizado com objetivos claros

- **Domínio Técnico Primário:** Full-Stack Web App com Real-time Collaboration
  - Frontend: Single Page Application (React 19)
  - Backend: REST API + WebSocket Server
  - Banco de dados relacional (PostgreSQL)
  - Autenticação stateless (JWT)

- **Componentes Arquiteturais Estimados:** 12-15 componentes principais
  - Backend: 6 serviços (Auth, User, List, Item, Member, Activity)
  - Integration: WebSocket Handler, API Gateway
  - Frontend: 3-4 contexts/hooks, 5-7 componentes principais

### Technical Constraints & Dependencies

**Constraints Infraestruturais:**
- **Hardware:** Raspberry Pi 4 com recursos limitados (CPU/memória)
- **Orquestração:** K3s (Kubernetes lightweight)
- **Networking:** Cloudflare Tunnel como única entrada (não possui IP público)
- **Deploy:** GitHub Actions → K3s automatizado

**Dependencies Técnicas:**
- Google OAuth2 para autenticação primária
- Cloudflare Tunnel para exposição do serviço
- STOMP sobre SockJS para WebSocket
- PostgreSQL para persistência (H2 para dev/test)

**Restrições de Deploy:**
- Aplicação deve ser "home-server friendly" - auto-contida
- Monitoramento de recursos crítico (PostgreSQL no Pi)
- CI/CD deve funcionar sem intervenção manual

### Cross-Cutting Concerns Identified

1. **Real-time Synchronization:**
   - **AFETA:** Todos os componentes de lista e itens
   - **IMPLEMENTAÇÃO:** WebSocket broadcast para todos os participantes online
   - **CRITICIDADE:** Alta - é o diferencial principal do produto

2. **Authentication & Authorization:**
   - **AFETA:** Todas as APIs REST e endpoints WebSocket
   - **IMPLEMENTAÇÃO:** JWT stateless com validação em cada request
   - **CRITICIDADE:** Alta - proteção de dados pessoais e controle de acesso

3. **Error Handling & Recovery:**
   - **AFETA:** Cliente WebSocket (reconexão), APIs (retry), UI (feedback)
   - **IMPLEMENTAÇÃO:** Reconexão automática, mensagens de erro user-friendly
   - **CRITICIDADE:** Alta - queda de WebSocket é comum em tunnels

4. **Activity Logging:**
   - **AFETA:** Todas as operações de escrita (create/update/delete)
   - **IMPLEMENTAÇÃO:** Event listeners que registram ações em todas as entidades
   - **CRITICIDADE:** Média - importante para UX, não crítico para funcionalidade

5. **Mobile Responsiveness:**
   - **AFETA:** Todas as telas e componentes da UI
   - **IMPLEMENTAÇÃO:** Mobile-first design com 3 breakpoints (<640px, 640-1024px, >1024px)
   - **CRITICIDADE:** Alta - uso primário será mobile

6. **Performance Optimization:**
   - **AFETA:** Bundle size, lazy loading, cache strategies
   - **IMPLEMENTAÇÃO:** Code splitting, cache de APIs, otimização de renders
   - **CRITICIDADE:** Média - NFRs definem metas específicas

---

## Architectural Decision #001: Repository Structure

### Decision

**Adotar estrutura de Monorepo para o NossaLista.**

### Context

Decisão tomada durante a fase de análise de contexto do projeto. A questão surgiu sobre organizar o código em dois repositórios separados (frontend e backend) ou um único monorepo.

### Options Considered

**Option A: Monorepo** ✅ **ESCOLHIDO**
- Único repositório contendo frontend, backend e configuração de deploy
- CI/CD incremental com path filters
- Deploy coordenado via tags

**Option B: Multi-Repo**
- `nossalista-api/` para backend Spring Boot
- `nossalista-web/` para frontend React
- Repositórios independentes com sincronização manual

### Decision Drivers

| Factor | Weight | Monorepo | Multi-Repo |
|--------|--------|----------|------------|
| Coordinated changes | High | ✅ Excellent | ❌ Poor |
| CI/CD simplicity | Medium | ⚠️ Moderate | ✅ Excellent |
| Deploy coordination | High | ✅ Excellent | ❌ Manual |
| Shared types | High | ✅ Possible | ❌ Duplicated |
| Team independence | Low | ❌ Not needed | ✅ N/A |
| Release versioning | Medium | ✅ Single tag | ❌ Coordinated tags |

### Rationale

**Por que Monorepo para NossaLista:**

1. **Projeto de aprendizado com 1 desenvolvedor**
   - Independência de repos não traz benefício real
   - Menor contexto para gerenciar

2. **Mudanças coordenadas são frequentes**
   - Real-time WebSocket implica API e frontend evoluem juntos
   - Novo campo na API = mudança em ambos

3. **Deploy coordenado é valioso**
   - GitHub Actions → K3s é mais simples com tag único
   - Garante que frontend e backend estão sync

4. **Shared types é HUGE**
   - Pacote `shared/` com interfaces TypeScript da API
   - Backend gera OpenAPI/Swagger
   - Frontend gera clientes TypeScript do contrato
   - Zero divergência de contratos!

5. **Git history conta a história completa**
   - Bug que envolve ambos = visto no mesmo commit
   - `git bisect` funciona através da stack completa

### Consequences

**Positive:**
- ✅ Simplicidade mental: Um repo = um projeto
- ✅ Deploy coordenado com tags únicas (`v1.0.0` aplica a ambos)
- ✅ Shared types entre frontend e backend
- ✅ Debugging cross-stack mais fácil
- ✅ Mudanças que afetam ambos ficam no mesmo commit

**Negative:**
- ❌ CI/CD ligeiramente mais complexo (path filters necessários)
- ❌ Build times maiores ao clonar repo completo (mitigado com caches)
- ❌ Tooling conflicts possíveis (mitigado com Docker)

**Neutral:**
- ↔️ Frontend e backend podem ter releases em tempos diferentes via branches

### Implementation

**Estrutura do Repositório:**

```
nossalista/
├── backend/                 # Spring Boot 4 + Java 25
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # React 19 + TypeScript + Vite
│   ├── src/
│   ├── package.json
│   └── Dockerfile
├── shared/                  # Tipos compartilhados (opcional)
│   ├── types.ts            # Interfaces TypeScript da API
│   └── openapi.json        # Gerado do backend
├── deploy/                  # K3s manifests
│   ├── backend-deployment.yaml
│   ├── frontend-deployment.yaml
│   └── ingress.yaml
├── docker-compose.dev.yml   # Dev local
└── .github/
    └── workflows/
        ├── backend-ci.yml      # Path: backend/**
        ├── frontend-ci.yml     # Path: frontend/**
        └── deploy.yml          # On: tags ['v*']
```

**CI/CD Strategy:**

```yaml
# .github/workflows/backend-ci.yml
on:
  push:
    paths: ['backend/**']

# .github/workflows/frontend-ci.yml
on:
  push:
    paths: ['frontend/**']

# .github/workflows/deploy.yml
on:
  push:
    tags: ['v*']  # Deploy coordenado em tagged releases
```

**Workflow de Deploy:**
1. Desenvolvimento: `git push origin main` → CI roda, não deploya
2. Release: `git tag v1.0.0 && git push origin v1.0.0` → Deploy ambos

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- TBD: CI/CD Pipeline Architecture
- TBD: Shared Types Strategy (OpenAPI/Swagger)

---

## Starter Template Evaluation

### Primary Technology Domain

**Full-Stack Web App com Real-time Collaboration** baseado na análise do contexto do projeto.

- Frontend: Single Page Application (React 19)
- Backend: REST API + WebSocket Server (Spring Boot 4)
- Banco de dados relacional (PostgreSQL)
- Autenticação stateless (JWT)

### Starter Options Considered

**Frontend Starters Analisados:**
- Vite oficial (create-vite) - ✅ Escolhido
- vite-react-ts-tailwind-starter (terceiros) - Rejeitado: menos manutenção
- Vite + Shadcn + Tailwind - Rejeitado: complexidade desnecessária

**Backend Starters Analisados:**
- Spring Initializr (Spring Boot 4.0.2 + Java 25) - ✅ Escolhido
- Spring Boot 3.5.x - Rejeitado: usuário prefere stack LTS mais moderna
- Templates customizados - Rejeitado: Spring Initializr é padrão da indústria

### Selected Starter: Vite (Frontend) + Spring Initializr (Backend)

**Rationale for Selection:**

**Frontend - Vite Oficial:**
- Mantido oficialmente pela equipe Vite/Evan You
- Build dev extremamente rápido (ESBuild-based)
- HMR instantâneo - crítico para desenvolvimento ágil
- Zero configuração desnecessária
- Totalmente compatível com React 19 + TypeScript
- Tailwind CSS adicionado manualmente em 3 minutos - controle total

**Backend - Spring Initializr (Spring Boot 4.0.2 + Java 25):**
- Ferramenta oficial do ecossistema Spring
- Spring Boot 4 + Java 25 = ambos LTS, projetados para trabalhar juntos
- Baseado no Spring Framework 7.0 - nova geração modular
- Documentação oficial robusta e crescente
- Perfect fit para projeto de aprendizado com stack moderna
- Complexidade BAIXA do projeto = ideal para stack de ponta
- Todas as dependencies necessárias disponíveis (WebSocket, Security, JPA, Flyway)

**Monorepo Ready:**
- Ambos starters criam estruturas que funcionam perfeitamente em monorepo
- Build scripts independentes (npm para frontend, mvn para backend)
- CI/CD incremental com path filters já definidos na Decision #001

### Initialization Commands

**Frontend (Vite + React 19 + TypeScript):**

```bash
# Criar projeto frontend
npm create vite@latest frontend -- --template react-ts

cd frontend
npm install

# Adicionar Tailwind CSS
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Configurar tailwind.config.js
# Adicionar @tailwind directives ao src/index.css
```

**Backend (Spring Boot 4.0.2 + Java 25):**

```bash
# Via cURL
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=4.0.2 \
  -d groupId=br.com.leoferolive \
  -d artifactId=nossalista-api \
  -d name=nossalista-api \
  -d packageName=br.com.leoferolive.nossalista \
  -d dependencies=web,data-jpa,postgresql,websocket,security,validation,flyway \
  -d javaVersion=25 \
  -o backend.zip

unzip backend.zip
mv backend nossalista-api

# Ou via https://start.spring.io/ (interface web)
```

**Estrutura do Monorepo:**

```
nossalista/
├── frontend/           # Criado via Vite
├── nossalista-api/     # Criado via Spring Initializr
├── deploy/
├── docker-compose.dev.yml
└── .github/workflows/
```

### Architectural Decisions Provided by Starter

**Frontend (Vite):**

**Language & Runtime:**
- TypeScript 5.x com configuração otimizada
- React 19 (última versão)
- Target: ES2020, navegador moderno

**Styling Solution:**
- Tailwind CSS 3.x (via npm install manual)
- PostCSS + Autoprefixer incluídos
- Configuração minimalista: `tailwind.config.js`

**Build Tooling:**
- Vite 5.x (dev server + build)
- ESBuild para transpilação (Go-based, ultra-rápido)
- HMR (Hot Module Replacement) instantâneo
- Production bundle otimizado com Rollup

**Testing Framework:**
- Vitest incluído (test runner compatível com Vite)
- JS DOM para testes de componente
- Configuração testes incluída

**Code Organization:**
```
frontend/
├── src/
│   ├── main.tsx          # Entry point
│   ├── App.tsx           # Root component
│   ├── assets/           # Static assets
│   └── vite-env.d.ts     # TypeScript definitions
├── index.html
├── vite.config.ts        # Config Vite
├── tsconfig.json         # Config TypeScript
├── tailwind.config.js    # Config Tailwind (adicionado)
└── package.json
```

**Development Experience:**
- Dev server na porta 5173
- HMR automático para mudanças em código/estilos
- TypeScript estrito habilitado
- Import de arquivos `.css`, `.ts`, `.tsx` sem configuração extra

**Backend (Spring Boot 4):**

**Language & Runtime:**
- Java 25 (LTS)
- Spring Boot 4.0.2 (LTS)
- Maven como build tool
- OpenJDK ou Oracle JDK

**Styling Solution:**
- N/A (backend sem UI)

**Build Tooling:**
- Maven 3.9+ (pom.xml)
- Spring Boot DevTools (hot reload)
- Flyway para migrations de banco

**Testing Framework:**
- JUnit 5 (Jupiter)
- Spring Boot Test
- Spring Web Test (MockMvc)
- Testcontainers para integração com PostgreSQL

**Code Organization:**
```
nossalista-api/
├── src/main/java/br/com/leoferolive/nossalista/
│   ├── NossaListaApiApplication.java
│   ├── config/           # Security, WebSocket, CORS
│   ├── auth/
│   ├── user/
│   ├── list/
│   ├── item/
│   ├── member/
│   └── activity/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/     # Flyway migrations
└── pom.xml
```

**Development Experience:**
- Dev tools com hot reload
- Actuator endpoints para monitoring
- Logs estruturados
- Profiles: dev (H2), prod (PostgreSQL)

### Note

Project initialization using these commands should be the first implementation story.

### Sources

- [Complete Guide to Setting Up React with TypeScript and Vite 2025](https://medium.com/@robinviktorssonsson/complete-guide-to-setting-up-react-with-typescript-and-vite-2025-468f6556aaf2)
- [How to build a React + TypeScript app with Vite](https://blog.logrocket.com/how-to-build-react-typescript-app-vite/)
- [Installing Tailwind CSS with Vite](https://tailwindcss.com/docs)
- [vite-react-ts-tailwind-starter](https://github.com/0kzh/vite-react-ts-tailwind-starter)
- [Spring Boot 4.0.0 available now](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now)
- [Spring Initializr Official](https://start.spring.io/)

---

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
- Monorepo structure (#001)
- Data model com colunas nullable (#002)
- RFC 7807 error handling (#004)
- WebSocket message format (#005)
- Spring Boot 4 + Java 25 LTS stack (Starter)
- React 19 + TypeScript + Vite stack (Starter)

**Important Decisions (Shape Architecture):**
- SpringDoc OpenAPI 3 documentation (#003)
- React Context + useReducer state management (#006)
- Axios + custom hooks data fetching (#007)
- SLF4J + Logback logging (#008)
- Spring Boot Actuator monitoring (#009)
- GitHub Container Registry (#010)
- 3-camada testing strategy (#011)
- Testcontainers PostgreSQL (#012)

**Deferred Decisions (Post-MVP):**
- Caching strategy (Redis futuro se necessário)
- Rate limiting (Cloudflare pode cuidar disso)
- Distributed tracing (complexidade desnecessária agora)
- Message queue (WebSocket é suficiente para MVP)

---

## Decision #002: Data Model for Dynamic List Item Fields

### Decision

**Adotar colunas nullable na tabela `list_items` para suportar campos específicos por tipo de lista.**

### Context

O PRD define 4 tipos de lista (Compras, Tarefas, Wishlist, Genérica), cada um com campos diferentes nos itens. Precisamos decidir como modelar essa flexibilidade no banco de dados.

### Options Considered

**Option A: Colunas Nullable** ✅ **ESCOLHIDO**
```sql
CREATE TABLE list_items (
  id UUID PRIMARY KEY,
  list_id UUID NOT NULL,
  name TEXT NOT NULL,
  checked BOOLEAN DEFAULT FALSE,
  quantity INTEGER,              -- NULL para não-Compras
  due_date TIMESTAMP,            -- NULL para não-Tarefas
  url TEXT,                      -- NULL para não-Wishlist
  position INTEGER,
  created_by UUID NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);
```

**Option B: JSONB para Dados Flexíveis**
- Coluna `metadata JSONB` armazena campos dinâmicos
- Consultas com operadores JSON do PostgreSQL
- Flexibilidade máxima mas complexidade maior

**Option C: Schema Híbrido (Type-Specific Tables)**
- Tabela principal + tabelas específicas por tipo
- JOINs necessários para cada query
- Type-safe mas overkill para 4 tipos

### Rationale

**Por que colunas nullable para NossaLista:**

1. **Simplicidade do MVP:** 4 tipos fixos, não tipos customizáveis ilimitados
2. **Performance:** Queries diretas sem JSON parsing overhead
3. **Aprendizado:** Padrão relacional tradicional é melhor para aprender fundamentals
4. **Type safety:** Hibernate/JPA mapeia para tipos Java claramente
5. **Migração futura:** Se precisar de flexibilidade, migra para JSONB é straightforward
6. **Queries SQL:** Simples, legíveis, otimizáveis

### Consequences

**Positive:**
- ✅ Schema SQL simples e legível
- ✅ Performance ótima (índices em colunas primitivas)
- ✅ JPA/Hibernate mapeamento direto
- ✅ Queries intuitivas (`WHERE quantity > 0`)
- ✅ Validação em tempo de compilação

**Negative:**
- ❌ Adicionar novo campo requer migration Flyway
- ❌ Colunas nullable podem ser confusas (qual campo é obrigatório para qual tipo?)
- ❌ Schema menos flexível para customização futura

**Neutral:**
- ↔️ Requer validação na aplicação (business sabe quais campos usar por tipo)

### Implementation

**Validação por tipo na aplicação:**

```java
public enum ListType {
    SHOPPING,
    TASK,
    WISHLIST,
    GENERIC;
}

// Service valida quais campos são obrigatórios
if (listType == ListType.SHOPPING && item.getQuantity() == null) {
    throw new ValidationException("Itens de Compras requerem quantity");
}
```

**Constraint check (opcional):**
```sql
-- Trigger para validar que quantity só tem valor se tipo = SHOPPING
-- Ou validação na aplicação (mais simples para MVP)
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- Flyway migrations (#Starter)
- Spring Data JPA (#Starter)

---

## Decision #003: API Documentation Strategy

### Decision

**Adotar SpringDoc OpenAPI 3 (OpenAPI 3.0) para documentação automática da API REST.**

### Context

Backend Spring Boot precisa ter documentação de API para:
- Frontend consumir contratos de forma type-safe
- Manter API sincronizada com documentação
- Swagger UI para testes manuais
- Export OpenAPI JSON para gerar clientes TypeScript

### Options Considered

**Option A: SpringDoc OpenAPI 3** ✅ **ESCOLHIDO**
- Biblioteca nativa para Spring Boot
- Gera documentação automaticamente dos controllers
- Swagger UI embutida (`/swagger-ui.html`)
- Exporta OpenAPI 3.0 JSON/YAML
- Integração com Spring Security

**Option B: Manual README com exemplos**
- Simples, mas desatualizado facilmente
- Sem validação automática de contratos
- Não existe single source of truth

**Option C: Postman Collection**
- Bom para testes manuais
- Source-of-truth separado do código
- Sincronização manual necessária

### Rationale

**Por que SpringDoc OpenAPI:**

1. **Single source of truth:** Código = documentação
2. **Zero manutenção:** Atualiza automaticamente com código
3. **Type-safe frontend:** Gera clientes TypeScript do OpenAPI
4. **Swagger UI:** Testes rápidos sem Postman
5. **Spring nativo:** Integração perfeita com Spring Boot 4
6. **Padrão da indústria:** OpenAPI é amplamente adotado

### Consequences

**Positive:**
- ✅ Documentação sempre sincronizada com código
- ✅ Swagger UI para debugging (`/swagger-ui/index.html`)
- ✅ OpenAPI JSON exportável para frontend
- ✅ Anotações simples (`@Tag`, `@Operation`, `@ApiResponse`)

**Negative:**
- ⚠️ Levemente verboso em anotações (mas vale a pena)
- ⚠️ Precisa configurar para endpoints autenticados

### Implementation

**Dependency (pom.xml):**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.7.0</version>
</dependency>
```

**Configuration:**
```java
@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NossaLista API")
                .version("1.0.0")
                .description("API de listas compartilhadas em tempo real"));
    }
}
```

**Exemplo Controller:**
```java
@Tag(name = "Listas", description = "Gerenciamento de listas")
@RestController
@RequestMapping("/api/lists")
public class ListController {

    @Operation(summary = "Criar nova lista")
    @ApiResponse(responseCode = "201", description = "Lista criada com sucesso")
    @PostMapping
    ResponseEntity<ListDTO> createList(@RequestBody CreateListRequest request) {
        // ...
    }
}
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- RFC 7807 Problem Details (#004)
- Shared Types Strategy (future)

---

## Decision #004: API Error Handling Standard

### Decision

**Adotar RFC 7807 Problem Details for HTTP APIs como padrão de respostas de erro.**

### Context

API REST precisa retornar erros de forma:
- Consistente em todos os endpoints
- Type-safe para frontend TypeScript
- Machine-readable para ferramentas
- Human-readable para developers

### Options Considered

**Option A: RFC 7807 Problem Details** ✅ **ESCOLHIDO**
```json
{
  "type": "https://api.nossalista.com/docs/errors/list-not-found",
  "title": "Lista não encontrada",
  "status": 404,
  "detail": "Lista com ID 'abc-123' não existe ou você não tem permissão para acessá-la.",
  "instance": "/api/lists/abc-123"
}
```

**Option B: JSON Simples Customizado**
```json
{
  "error": "Lista não encontrada",
  "message": "Lista com ID 'abc-123' não existe"
}
```
- Simples mas proprietário e inconsistente

**Option C: Custom com Code**
```json
{
  "code": "LIST_NOT_FOUND",
  "message": "Lista não encontrada",
  "timestamp": "2026-02-10T12:00:00",
  "path": "/api/lists/abc-123"
}
```
- Reinventando a roda, não segue padrão

### Rationale

**Por que RFC 7807 para NossaLista:**

1. **Padrão formal:** RFC respeitada mundialmente
2. **Spring nativo:** `ProblemDetail` no Spring 6+ (Spring Boot 4)
3. **Type-safe frontend:** TypeScript pode tipar erros corretamente
4. **Skill profissional:** Habilidade valorizada no mercado
5. **Ferramentas:** Clientes HTTP conhecem o padrão
6. **Interoperabilidade:** Máquinas podem parsear e interpretar

### Consequences

**Positive:**
- ✅ Consistência garantida por padrão
- ✅ Type-safe errors no frontend TypeScript
- ✅ Spring Boot fornece `ProblemDetail` class
- ✅ Internacionalização via `type` URI
- ✅ Profissional e enterprise-ready

**Negative:**
- ❌ Respostas mais verbosas que JSON simples
- ⚠️ Requer configurar `@RestControllerAdvice`
- ⚠️ Precisa manter documentação de tipos (se usar URIs)

### Implementation

**Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ListNotFoundException.class)
    ResponseEntity<ProblemDetail> listNotFound(ListNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/list-not-found"));
        problem.setTitle("Lista não encontrada");
        problem.setInstance(URI.create("/api/lists/" + ex.getListId()));

        return ResponseEntity.status(404).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validationError(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Erro de validação"
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/validation-error"));
        problem.setTitle("Validation Error");
        // Add field errors to detail

        return ResponseEntity.badRequest().body(problem);
    }
}
```

**Frontend TypeScript:**
```typescript
interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
}

// Type-safe error handling
if (error.response?.data instanceof ProblemDetail) {
  const problem = error.response.data as ProblemDetail;

  switch (problem.status) {
    case 404:
      showNotification(`Erro: ${problem.title}`);
      break;
    case 403:
      showNotification(`Acesso negado: ${problem.detail}`);
      break;
  }
}
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- SpringDoc OpenAPI 3 (#003)
- Spring Security (Starter)

---

## Decision #005: WebSocket Message Format

### Decision

**Adotar "Event-Type Envelope" para mensagens WebSocket broadcast.**

### Context

O PRD define STOMP sobre WebSocket para sincronização real-time. Precisamos padronizar o formato das mensagens que são broadcastadas para todos os participantes de uma lista.

### Options Considered

**Option A: Event-Type Envelope** ✅ **ESCOLHIDO**
```json
{
  "type": "ITEM_ADDED",
  "payload": {
    "id": "uuid-123",
    "name": "Arroz",
    "quantity": 2,
    "checked": false
  },
  "userId": "uuid-456",
  "username": "leo",
  "timestamp": "2026-02-10T12:00:00Z"
}
```

**Option B: Direct Entity**
```json
{
  "id": "uuid-123",
  "name": "Arroz",
  "quantity": 2
}
```
- Cliente não sabe se foi add/update/delete
- Sem contexto de quem fez a ação

**Option C: STOMP Headers + Body**
```stomp
SEND
destination:/app/list/123/item
type:ITEM_ADDED
userId:uuid-456

{payload json}
```
- Headers STOMP são menos visíveis no frontend
- Mais complexo de implementar

### Rationale

**Por que Event-Type Envelope:**

1. **Type-safe:** Frontend discrimina ações por `type`
2. **Extensível:** Fácil adicionar novos eventos
3. **Audit trail:** Quem fez o quê e quando
4. **Debugging:** Timestamp ajuda a troubleshooting
5. **UX:** Mostra "Leo adicionou Arroz" em tempo real
6. **STOMP natural:** Funciona perfeitamente com STOMP

### Consequences

**Positive:**
- ✅ Frontend pode reagir diferente por tipo (add vs update vs delete)
- ✅ Experiência rica com "quem fez o quê"
- ✅ Timeline de atividade fácil de implementar
- ✅ Debugging com timestamp

**Negative:**
- ⚠️ Mensagens ligeiramente maiores (mas insignificante para WebSocket)

### Implementation

**Backend (Broadcast):**
```java
@Controller
public class ListWebSocketController {

    @MessageMapping("/list/{listId}/item.add")
    @SendTo("/topic/list/{listId}")
    public WebSocketMessage itemAdded(
        @DestinationVariable String listId,
        @Payload AddItemRequest request,
        Principal principal
    ) {
        ListItem item = itemService.addItem(listId, request, principal);
        User user = userService.getByUsername(principal.getName());

        return WebSocketMessage.builder()
            .type("ITEM_ADDED")
            .payload(mapToDTO(item))
            .userId(user.getId())
            .username(user.getUsername())
            .timestamp(Instant.now())
            .build();
    }
}
```

**Frontend (Subscribe):**
```typescript
interface WebSocketMessage {
  type: 'ITEM_ADDED' | 'ITEM_UPDATED' | 'ITEM_DELETED' | 'ITEM_CHECKED';
  payload: ListItemDTO;
  userId: string;
  username: string;
  timestamp: string;
}

function subscribeToList(listId: string) {
  client.subscribe(`/topic/list/${listId}`, (message) => {
    const event: WebSocketMessage = JSON.parse(message.body);

    switch (event.type) {
      case 'ITEM_ADDED':
        onItemAdded(event.payload, event.username);
        break;
      case 'ITEM_CHECKED':
        onItemChecked(event.payload, event.username);
        break;
      // ...
    }
  });
}
```

**Event Types Definidos:**
- `ITEM_ADDED` - Novo item criado
- `ITEM_UPDATED` - Item editado
- `ITEM_DELETED` - Item removido
- `ITEM_CHECKED` - Checkbox marcado/desmarcado
- `MEMBER_JOINED` - Novo participante entrou
- `MEMBER_LEFT` - Participante saiu

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- STOMP sobre SockJS (PRD)
- Activity Logging requirement (Cross-cutting concern)

---

## Decision #006: Frontend State Management

### Decision

**Adotar React Context + useReducer para gerenciamento de estado global.**

### Context

Frontend React precisa gerenciar estado de:
- Usuário autenticado
- Listas carregadas
- Estado de WebSocket (conectado/desconectado)
- Notificações e mensagens de erro

### Options Considered

**Option A: React Context + useReducer** ✅ **ESCOLHIDO**
- Built-in do React, sem dependências externas
- Simples para escala pequena/média
- Perfeito para app single-user com real-time
- Boilerplate mínimo

**Option B: Zustand**
- API minimalista, ótimo DX
- DevTools integradas
- Escalável se crescer
- Requer biblioteca externa

**Option C: Redux Toolkit**
- Overkill para este tamanho
- Muito boilerplate para pouco benefício
- Curva de aprendizado mais íngreme

### Rationale

**Por que Context + useReducer para NossaLista:**

1. **Built-in:** Sem dependências externas
2. **Simples:** Para escala pequena/média que o projeto tem
3. **Suficiente:** App single-user com real-time não precisa de Redux
4. **Aprendizado:** Entender Context/useReducer é fundamental React
5. **Bundle size:** Zero bytes adicionais
6. **Migrável:** Se crescer, migra para Zustand é fácil

### Consequences

**Positive:**
- ✅ Zero dependências externas
- ✅ Curva de aprendizado suave
- ✅ Suficiente para needs do MVP
- ✅ Type-safe com TypeScript

**Negative:**
- ⚠️ Re-render otimização manual (useMemo, useCallback)
- ⚠️ Sem DevTools nativas (mas React DevTools ajudam)

### Implementation

**Auth Context:**
```typescript
interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}

interface AuthActions {
  login: (user: User, token: string) => void;
  logout: () => void;
}

type AuthContextValue = AuthState & AuthActions;

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  const value = useMemo(() => ({
    ...state,
    login: (user, token) => dispatch({ type: 'LOGIN', user, token }),
    logout: () => dispatch({ type: 'LOGOUT' })
  }), [state]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
```

**WebSocket Context:**
```typescript
interface WebSocketState {
  connected: boolean;
  lists: Map<string, ListItem[]>;
}

interface WebSocketActions {
  subscribe: (listId: string) => void;
  unsubscribe: (listId: string) => void;
}
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- React 19 + TypeScript (Starter)
- Real-time Synchronization (Cross-cutting concern)

---

## Decision #007: Frontend Data Fetching Pattern

### Decision

**Adotar Axios + Custom Hooks para consumo da API REST.**

### Context

Frontend precisa consumir API REST de forma:
- Type-safe
- Com error handling (RFC 7807)
- Com loading states
- Com retry logic opcional

### Options Considered

**Option A: Axios + Custom Hooks** ✅ **ESCOLHIDO**
```typescript
function useLists() {
  const [lists, setLists] = useState<ListDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<ProblemDetail | null>(null);

  const fetchLists = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get<ListDTO[]>('/lists');
      setLists(response.data);
    } catch (err) {
      setError(err.response?.data);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchLists(); }, [fetchLists]);

  return { lists, loading, error, refetch: fetchLists };
}
```
- Controle total do código
- Simples, sem mágica
- Type-safe com TypeScript

**Option B: React Query (TanStack Query)**
- Caching automático
- Background refetch
- DevTools excelentes
- Overkill para app pequeno?

**Option C: SWR**
- Similar ao React Query
- Mais simples, menos features
- Menos popular

### Rationale

**Por que Axios + Custom Hooks:**

1. **Controle total:** Você entende cada linha de código
2. **Simples:** Sem mágica de biblioteca
3. **Type-safe:** TypeScript coverage total
4. **Aprendizado:** Entender promises, async/await, hooks é fundamental
5. **Leve:** Axios é pequeno
6. **Suficiente:** Para MVP não precisa de caching avançado

**Nota:** React Query pode ser adicionado no futuro se precisar de:
- Caching sofisticado
- Background refetch
- Optimistic updates
- Infinite scrolling

### Consequences

**Positive:**
- ✅ Código transparente e compreensível
- ✅ Type-safe com interfaces TypeScript
- ✅ Error handling customizado (RFC 7807)
- ✅ Loading states controlados

**Negative:**
- ⚠️ Mais boilerplate que React Query
- ⚠️ Sem caching automático (mas não é crítico para MVP)
- ⚠️ Retry logic manual se precisar

### Implementation

**API Client (axios instance):**
```typescript
// api/client.ts
import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 10000,
});

// Request interceptor - adiciona JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor - trata erros RFC 7807
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data instanceof ProblemDetail) {
      return Promise.reject(error);
    }
    // Transform other errors to ProblemDetail format
    return Promise.reject(transformToProblemDetail(error));
  }
);
```

**Custom Hook:**
```typescript
// hooks/useLists.ts
export function useLists() {
  const [lists, setLists] = useState<ListDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<ProblemDetail | null>(null);

  const fetchLists = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<ListDTO[]>('/lists');
      setLists(response.data);
    } catch (err) {
      setError(err.response?.data || defaultError);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchLists(); }, [fetchLists]);

  return { lists, loading, error, refetch: fetchLists };
}
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- RFC 7807 Problem Details (#004)
- React Context + useReducer (#006)

---

## Decision #008: Logging Strategy

### Decision

**Adotar SLF4J + Logback com rotação automática e retenção de 30 dias.**

### Context

Home server em Raspberry Pi precisa de logs eficientes para:
- Troubleshooting de problemas
- Atender NFR-R2 (logs por 30 dias)
- Não consumir muito disco
- Performance aceitável

### Options Considered

**Option A: SLF4J + Logback (Padrão Spring)** ✅ **ESCOLHIDO**
```yaml
logging:
  level:
    br.com.leoferolive.nossalista: INFO
    org.springframework.web: WARN
  file:
    name: /var/log/nossalista/app.log
    max-size: 100MB
    max-history: 30
```
- Padrão Spring Boot
- Logback é rápido e eficiente
- Rotation automática gerencia arquivos

**Option B: Log4j2**
- Similar ao Logback
- Mais complexo configurar
- Desnecessário para este projeto

### Rationale

**Por que SLF4J + Logback:**

1. **Padrão Spring:** Já vem configurado no Spring Boot
2. **Eficiente:** Logback é rápido (assíncrono)
3. **Rotação automática:** Gerencia tamanho/história
4. **30 dias retention:** Atende NFR-R2
5. **Zero config:** Funciona out-of-the-box

### Consequences

**Positive:**
- ✅ Config zero ou mínima
- ✅ Rotação automática de logs
- ✅ Retenção configurável (30 dias)
- ✅ Formato de log customizável

### Implementation

**application-prod.yml:**
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  level:
    root: WARN
    br.com.leoferolive.nossalista: INFO
    org.springframework.web: WARN
    org.springframework.security: WARN
  file:
    name: /var/log/nossalista/app.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 1GB
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- Spring Boot 4 (Starter)
- NFR-R2: Logs por 30 dias

---

## Decision #009: Monitoring & Health Checks

### Decision

**Adotar Spring Boot Actuator com Kubernetes Probes para health checks.**

### Context

K3s precisa saber se os pods estão saudáveis para:
- Restart automático se travar
- Não rotear tráfego para pods não-ready
- PostgreSQL health check
- Atender NFR-R4 (pods restartam automaticamente)

### Options Considered

**Option A: Spring Boot Actuator + K8s Probes** ✅ **ESCOLHIDO**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true
```
```yaml
# K8s deployment
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

**Option B: Custom health endpoints**
- Você escreve código customizado
- Overkill para NossaLista

### Rationale

**Por que Actuator + K8s Probes:**

1. **K8s native:** Probes funcionam nativamente com K3s
2. **PostgreSQL health:** Checa conexão com BD automaticamente
3. **Zero código:** Apenas configuração
4. **Metrics expostas:** Prometheus pode scrape (futuro)
5. **Padrão Spring:** Funciona out-of-the-box

### Consequences

**Positive:**
- ✅ K3s reinicia pods travados automaticamente
- ✅ Não roteia tráfego para pods não-ready
- ✅ Database connectivity check
- ✅ Metrics expostas para monitoring futuro

### Implementation

**pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**k8s/deployment.yaml:**
```yaml
spec:
  containers:
  - name: nossalista-api
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- K3s deployment (PRD)
- NFR-R4: Pods restartam automaticamente

---

## Decision #010: Container Registry Strategy

### Decision

**Adotar GitHub Container Registry (ghcr.io) para armazenamento de imagens Docker.**

### Context

GitHub Actions precisa onde pushar as imagens Docker após build. O projeto é público, sem orçamento para registry privado.

### Options Considered

**Option A: GitHub Container Registry (ghcr.io)** ✅ **ESCOLHIDO**
- Integrado ao GitHub
- Free para públicos
- Código + imagens no mesmo lugar

**Option B: Docker Hub**
- Popular, mas registro separado
- Rate limits em free tier

**Option C: Registry próprio no K3s**
- Complexidade desnecessária
- Precisa gerenciar storage

### Rationale

**Por que ghcr.io:**

1. **Integrado:** Já está no GitHub
2. **Free:** Para públicos (que é seu caso)
3. **Simétrico:** Código + imagens no mesmo lugar
4. **CI/CD simples:** Sem credenciais extras (uses GITHUB_TOKEN)
5. **Performance:** CDN do GitHub

### Consequences

**Positive:**
- ✅ Zero configuração de credenciais
- ✅ Integrado com GitHub Actions
- ✅ Alto desempenho com CDN

### Implementation

**.github/workflows/backend-ci.yml:**
```yaml
- name: Log in to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}

- name: Build and push
  uses: docker/build-push-action@v5
  with:
    push: true
    tags: ghcr.io/leoferolive/nossalista-api:latest
    context: ./nossalista-api
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- GitHub Actions CI/CD (PRD)
- Monorepo structure (#001)

---

## Decision #011: Testing Pyramid Strategy

### Decision

**Adotar estratégia de 3 camadas: 70-80% unit, 20-30% integration, E2E minimal.**

### Context

Definir quanto teste é suficiente para MVP de aprendizado, garantindo confiança sem over-engineering.

### Options Considered

**Option A: Três Camadas Balanceadas** ✅ **ESCOLHIDO**

**Backend:**
- **Unit Tests (70-80%):**
  - Services layer (lógica de negócio)
  - Repositories (com H2 in-memory)
  - JWT util, validators
- **Integration Tests (20-30%):**
  - Controllers (MockMvc)
  - WebSocket handlers
  - Flyway migrations (Testcontainers PostgreSQL)

**Frontend:**
- **Unit Tests:** Vitest para componentes críticos
  - ListItem, ListCard components
  - Auth flow hooks
- **Integration Tests:** Testing Library
  - List creation flow
  - Item add/remove
- **E2E:** Playwright (opcional)
  - Apenas um smoke test crítico

**Option B: Cobertura Completa (90%+)**
- Overkill para MVP de aprendizado
- CI demora muito

**Option C: Apenas Unit Tests**
- Sem confiança em integração
- Bugs escapam na camada de API/frontend

### Rationale

**Por que 3 camadas balanceadas:**

1. **Cobertura boa:** Sem over-engineering
2. **Confiança:** Críticos cobertos
3. **Velocidade:** CI roda em < 2 min
4. **E2E minimal:** Caro e lento, apenas smoke test
5. **Aprendizado:** Aprende diferentes tipos de teste

### Consequences

**Positive:**
- ✅ Feedback rápido (unit tests < 30s)
- ✅ Confiança em integração (integration tests)
- ✅ Smoke test em fluxo crítico (E2E)

**Negative:**
- ⚠️ Alguns bugs podem escapar (não é 100% coverage)

### Implementation

**Backend Unit Test:**
```java
@ExtendWith(MockitoExtension.class)
class ListServiceTest {
    @Mock
    private ListRepository listRepository;

    @InjectMocks
    private ListService listService;

    @Test
    void shouldCreateList() {
        // Test logic isolada
    }
}
```

**Backend Integration Test:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class ListControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateListEndpoint() {
        mockMvc.perform(post("/api/lists"))
            .andExpect(status().isCreated());
    }
}
```

**Frontend Unit Test:**
```typescript
describe('ListItem', () => {
  it('renders item name', () => {
    render(<ListItem name="Arroz" />);
    expect(screen.getByText('Arroz')).toBeInTheDocument();
  });
});
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- Testcontainers PostgreSQL (#012)

---

## Decision #012: Integration Test Database Strategy

### Decision

**Adotar Testcontainers com PostgreSQL real para integration tests.**

### Context

Integration tests de backend precisam de database real para testar:
- Queries JPA/Hibernate
- Flyway migrations
- PostgreSQL-specific features

### Options Considered

**Option A: Testcontainers (PostgreSQL real)** ✅ **ESCOLHIDO**
```java
@Testcontainers
class ListRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");

    @Test
    void shouldCreateList() {
        // Testa com PostgreSQL real, não H2
    }
}
```
- PostgreSQL real em Docker during tests
- Fidelidade total com produção

**Option B: H2 In-Memory**
- Rápido mas H2 ≠ Postgres
- Algumas features não funcionam
- Queries podem falhar em prod

**Option C: H2 dev + Testcontainers CI**
- Complexidade adicional
- Inconsistência entre ambientes

### Rationale

**Por que Testcontainers:**

1. **Fidelidade:** PostgreSQL real = produção
2. **Confiança:** Queries H2 ≠ Postgres às vezes
3. **Migrations:** Flyway testado de verdade
4. **Aprendizado:** Testcontainers é habilidade valiosa
5. **Raspberry Pi:** Descobrir bugs antes de deployar para produção

### Consequences

**Positive:**
- ✅ PostgreSQL real durante tests
- ✅ Bugs descobertos antes do deploy
- ✅ Migrations validadas

**Negative:**
- ⚠️ Lento: 3-5s por teste que precisa BD
- ⚠️ Requer Docker durante CI

### Implementation

**pom.xml:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

**Test:**
```java
@Testcontainers
@SpringBootTest
class ListRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("nossalista_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldCreateAndFetchList() {
        // Real PostgreSQL being tested
    }
}
```

### Status

✅ **APPROVED** - Decision recorded 2026-02-10

### Related Decisions

- Testing Strategy (#011)
- PostgreSQL (PRD)

---

## Decision Impact Analysis

### Implementation Sequence

Ordem recomendada para implementação das decisões:

1. **Setup Inicial (Starter + Estrutura)**
   - #001: Monorepo structure
   - Starter: Vite + Spring Initializr

2. **Fundations Backend**
   - #002: Data Model (colunas nullable)
   - #008: Logging (Logback)
   - #009: Actuator health checks
   - #012: Testcontainers setup

3. **API Layer**
   - #003: SpringDoc OpenAPI
   - #004: RFC 7807 error handling
   - #007: Axios client (frontend)

4. **Real-time**
   - #005: WebSocket Event-Type Envelope
   - STOMP configuration

5. **Frontend**
   - #006: React Context + useReducer
   - #007: Custom hooks

6. **Testing**
   - #011: Testing pyramid

7. **CI/CD**
   - #010: GitHub Container Registry

### Cross-Component Dependencies

```
#002 (Data Model)
  ↓ afeta
#003 (OpenAPI) - contratos gerados do model
  ↓ afeta
#007 (Axios) - clientes gerados do OpenAPI

#004 (RFC 7807)
  ↓ afeta
#007 (Axios) - error handling de ProblemDetails

#005 (WebSocket)
  ↓ afeta
#006 (Context) - WebSocket state management

#009 (Actuator)
  ↓ afeta
K8s deployment - probes dependem de /actuator/health

#012 (Testcontainers)
  ↓ afeta
#011 (Testing) - integration tests dependem de BD real
```

### Deferred Decisions (Post-MVP)

As seguintes decisões foram propositalmente adiadas para após o MVP:

- **Caching Strategy:** Redis não é necessário inicialmente
- **Rate Limiting:** Cloudflare Tunnel pode fornecer isso
- **Distributed Tracing:** Complexidade desnecessária para single-service
- **Message Queue:** WebSocket é suficiente para real-time do MVP
- **CDN:** Assets estáticos não são críticos para MVP
- **Feature Flags:** Não necessário sem canary deployments

---

## Implementation Patterns & Consistency Rules

### Pattern Categories Defined

**Critical Conflict Points Identified:**
- 10 categorias onde AI agents poderiam fazer escolhas diferentes
- 50+ decisões de padronização para garantir consistência
- Padrões aplicam a backend (Java/Spring) e frontend (React/TypeScript)

**Por que estes padrões são críticos:**

Diferentes agentes de AI implementando o mesmo projeto poderiam:
- Nomear tabelas como `users`, `Users` ou `user`
- Usar `/api/lists` ou `/api/Lists` ou endpoints inconsistentes
- Retornar JSON com `user_id` ou `userId` ou `USER_ID`
- Organizar testes em `tests/` ou co-localizados com código
- Gerenciar loading states de formas completamente diferentes

Estes padrões eliminam ambiguidade e garantem que qualquer agente (humano ou AI) produza código consistente e compatível.

---

## Naming Patterns

### Database Naming Conventions

**Regra:** Usar **snake_case** para todos os objetos no PostgreSQL

**Tabelas:**
- **Formato:** Plural, snake_case, minúsculo
- **Exemplos:**
  ```sql
  users
  list_items
  activity_log
  list_members
  ```

**Colunas:**
- **Formato:** snake_case, minúsculo
- **Exemplos:**
  ```sql
  user_id
  created_at
  list_id
  is_checked
  ```

**Foreign Keys:**
- **Formato:** `{tabela_referenciada}_id`
- **Exemplos:**
  ```sql
  user_id        -- FK para users.id
  list_id        -- FK para lists.id
  created_by     -- FK para users.id
  ```

**Índices:**
- **Formato:** `idx_{tabela}_{coluna(s)}`
- **Exemplos:**
  ```sql
  idx_users_email
  idx_list_items_list_id
  idx_activity_log_list_id_created_at
  ```

**Constraints/Unique:**
- **Formato:** `uq_{tabela}_{coluna(s)}` ou `fk_{tabela}_{coluna}`
- **Exemplos:**
  ```sql
  uq_users_username
  uq_list_members_user_list
  fk_list_items_list_id
  ```

**Migrations (Flyway):**
- **Formato:** `V{version}__{description}.sql`
- **Exemplos:**
  ```sql
  V1__create_users_table.sql
  V2__create_lists_table.sql
  V3__add_foreign_key_list_items.sql
  ```

**Implementação JPA/Hibernate:**
```java
@Table(name = "list_items")
public class ListItem {

    @Column(name = "list_id")
    private UUID listId;

    @Column(name = "created_at")
    private Instant createdAt;
}
```

---

### API Naming Conventions

**Regra:** REST endpoints seguem padrão RESTful com **plural, kebab-case, lowercase**

**Endpoint Structure:**
```
/api/{resource}[/{id}][/{sub-resource}]
```

**Regras:**
1. **Recursos:** Sempre **plural** (coleção)
2. **Case:** **kebab-case** para multi-palavra
3. **Lowercase:** Tudo minúsculo
4. **IDs:** Path parameter `{id}` sem tipo

**Exemplos:**

```
# Lists
GET    /api/lists                          # Listar todas
POST   /api/lists                          # Criar nova
GET    /api/lists/{id}                     # Detalhe
PATCH  /api/lists/{id}                     # Atualizar
DELETE /api/lists/{id}                     # Excluir

# List Items (sub-resource)
GET    /api/lists/{id}/items               # Listar itens
POST   /api/lists/{id}/items               # Adicionar item
PATCH  /api/lists/{id}/items/{itemId}      # Atualizar item
DELETE /api/lists/{id}/items/{itemId}      # Remover item

# Members
GET    /api/lists/{id}/members             # Listar membros
POST   /api/lists/{id}/invite              Convidar usuário
DELETE /api/lists/{id}/members/{userId}    # Remover membro
POST   /api/lists/{id}/leave               Sair da lista

# Activity Log
GET    /api/lists/{id}/activity            # Timeline

# Auth
POST   /api/auth/register                  Registro
POST   /api/auth/login                     Login
GET    /api/auth/google                    OAuth redirect
GET    /api/auth/google/callback           OAuth callback

# Users
GET    /api/users/me                       Perfil próprio
PATCH  /api/users/me                       Atualizar perfil
GET    /api/users/search?q={username}      Buscar usuário
```

**Query Parameters:**
- **Formato:** **snake_case**
- **Exemplos:**
  ```
  GET /api/lists?page=0&size=20&sort=created_at,desc
  GET /api/lists/{id}/activity?limit=50
  GET /api/users/search?q=leo&active=true
  ```

**Headers:**
- **Formato:** PascalCase para custom headers
- **Exemplos:**
  ```
  Authorization: Bearer {jwt_token}
  X-Request-ID: uuid
  ```

---

### Code Naming Conventions

**Backend (Java):**

**Classes:**
- **Formato:** PascalCase, substantivo
- **Exemplos:**
  ```java
  public class ListService {}
  public class ListItem {}
  public class ListNotFoundException {}
  public class WebSocketConfig {}
  ```

**Interfaces:**
- **Formato:** PascalCase, opcionalmente prefixo `I` (não recomendado)
- **Exemplos:**
  ```java
  public interface ListRepository extends JpaRepository<List, UUID> {}
  public interface JwtService {}  // Prefira sem prefixo I
  ```

**Methods:**
- **Formato:** camelCase, verbo + substantivo
- **Exemplos:**
  ```java
  public List<ListItem> getItemsByListId(String listId) {}
  public void addMemberToList(UUID listId, UUID userId) {}
  public boolean isListOwner(UUID listId, UUID userId) {}
  ```

**Variáveis:**
- **Formato:** camelCase
- **Exemplos:**
  ```java
  String listId = "abc";
  List<ListItem> items = new ArrayList<>();
  Instant createdAt = Instant.now();
  boolean isChecked = true;
  ```

**Constants:**
- **Formato:** SCREAMING_SNAKE_CASE
- **Exemplos:**
  ```java
  public static final String DEFAULT_LIST_TYPE = "GENERIC";
  public static final int MAX_ITEMS_PER_LIST = 1000;
  public static final Duration TOKEN_EXPIRATION = Duration.ofDays(7);
  ```

**Packages:**
- **Formato:** lowercase, separado por pontos
- **Exemplos:**
  ```java
  br.com.leoferolive.nossalista.config
  br.com.leoferolive.nossalista.auth
  br.com.leoferolive.nossalista.list
  br.com.leoferolive.nossalista.item
  ```

---

**Frontend (TypeScript/React):**

**Components:**
- **Formato:** PascalCase
- **Exemplos:**
  ```typescript
  function ListCard() {}
  function ListItem() {}
  function CreateListModal() {}
  function ActivityTimeline() {}
  ```

**Component Files:**
- **Formato:** PascalCase.tsx
- **Exemplos:**
  ```
  ListCard.tsx
  ListItem.tsx
  CreateListModal.tsx
  ```

**Custom Hooks:**
- **Formato:** camelCase com prefixo `use`
- **Exemplos:**
  ```typescript
  function useLists() {}
  function useAuth() {}
  function useWebSocket() {}
  function useLocalStorage(key: string) {}
  ```

**Hook Files:**
- **Formato:** camelCase.ts
- **Exemplos:**
  ```
  useLists.ts
  useAuth.ts
  useWebSocket.ts
  ```

**Utility Functions:**
- **Formato:** camelCase, verbo + substantivo
- **Exemplos:**
  ```typescript
  function formatDate(isoString: string) {}
  function validateEmail(email: string) {}
  function truncateText(text: string, maxLength: number) {}
  ```

**Type Interfaces/Types:**
- **Formato:** PascalCase
- **Exemplos:**
  ```typescript
  interface ListDTO {}
  interface ListItemDTO {}
  type ListType = 'SHOPPING' | 'TASK' | 'WISHLIST' | 'GENERIC';
  ```

**Enums:**
- **Formato:** PascalCase (enum), SCREAMING_SNAKE_CASE (values)
- **Exemplos:**
  ```typescript
  enum ListType {
    SHOPPING = 'shopping',
    TASK = 'task',
    WISHLIST = 'wishlist',
    GENERIC = 'generic'
  }
  ```

---

## Structure Patterns

### Project Organization

**Backend Structure (Feature-based):**

```
nossalista-api/
├── src/main/java/br/com/leoferolive/nossalista/
│   ├── NossaListaApiApplication.java    # Classe principal
│   │
│   ├── config/                          # Configurações globais
│   │   ├── SecurityConfig.java
│   │   ├── WebSocketConfig.java
│   │   ├── CorsConfig.java
│   │   └── OpenApiConfig.java
│   │
│   ├── auth/                            # Domínio: autenticação
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   └── OAuth2SuccessHandler.java
│   │
│   ├── user/                            # Domínio: usuários
│   │   ├── UserController.java
│   │   ├── UserService.java
│   │   ├── UserRepository.java
│   │   └── User.java
│   │
│   ├── list/                            # Domínio: listas
│   │   ├── ListController.java
│   │   ├── ListService.java
│   │   ├── ListRepository.java
│   │   └── List.java
│   │
│   ├── item/                            # Domínio: itens
│   │   ├── ItemController.java
│   │   ├── ItemService.java
│   │   ├── ItemRepository.java
│   │   └── ListItem.java
│   │
│   ├── member/                          # Domínio: membros
│   │   ├── MemberController.java
│   │   ├── MemberService.java
│   │   ├── MemberRepository.java
│   │   └── ListMember.java
│   │
│   ├── activity/                        # Domínio: activity log
│   │   ├── ActivityController.java
│   │   ├── ActivityService.java
│   │   ├── ActivityRepository.java
│   │   └── ActivityLog.java
│   │
│   ├── websocket/                       # WebSocket handlers
│   │   ├── ListWebSocketController.java
│   │   └── WebSocketAuthInterceptor.java
│   │
│   ├── exception/                       # Error handling
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ListNotFoundException.java
│   │   └── ValidationException.java
│   │
│   └── dto/                             # Data Transfer Objects
│       ├── request/                     # Request DTOs
│       │   ├── CreateListRequest.java
│       │   ├── AddItemRequest.java
│       │   └── InviteMemberRequest.java
│       └── response/                    # Response DTOs
│           ├── ListDTO.java
│           ├── ListItemDTO.java
│           └── UserDTO.java
│
├── src/main/resources/
│   ├── application.yml                   # Config principal
│   ├── application-dev.yml               # Profile dev (H2)
│   └── application-prod.yml              # Profile prod (PostgreSQL)
│
├── src/test/java/br/com/leoferolive/nossalista/
│   └── [espelha estrutura main]         # Tests seguem estrutura main
│
└── pom.xml
```

**Princípios:**
- **Feature-based:** Cada domínio em seu pacote
- **Tests espelham main:** Mesma estrutura em `src/test`
- **DTOs separados:** `request` e `response` em pacotes próprios

---

**Frontend Structure (Type-based organizado):**

```
frontend/
├── src/
│   ├── main.tsx                         # Entry point
│   ├── App.tsx                          # Root component
│   ├── vite-env.d.ts                    # Vite types
│   │
│   ├── pages/                           # Componentes de página (rotas)
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   ├── Home.tsx                     # Dashboard de listas
│   │   ├── ListView.tsx                 # Detalhe da lista
│   │   └── JoinList.tsx                 # Aceitar convite
│   │
│   ├── components/                      # Componentes reutilizáveis
│   │   ├── layout/
│   │   │   ├── Header.tsx
│   │   │   ├── MainLayout.tsx
│   │   │   └── Footer.tsx
│   │   ├── lists/
│   │   │   ├── ListCard.tsx
│   │   │   ├── CreateListModal.tsx
│   │   │   └── InviteModal.tsx
│   │   ├── items/
│   │   │   ├── ListItem.tsx
│   │   │   ├── AddItemForm.tsx
│   │   │   └── ItemCheckbox.tsx
│   │   └── common/
│   │       ├── Button.tsx
│   │       ├── Input.tsx
│   │       ├── Modal.tsx
│   │       └── LoadingSpinner.tsx
│   │
│   ├── hooks/                           # Custom hooks
│   │   ├── useLists.ts
│   │   ├── useAuth.ts
│   │   ├── useWebSocket.ts
│   │   └── useLocalStorage.ts
│   │
│   ├── contexts/                        # React contexts
│   │   ├── AuthContext.tsx
│   │   ├── WebSocketContext.tsx
│   │   └── NotificationContext.tsx
│   │
│   ├── api/                             # API client
│   │   ├── client.ts                    # Axios instance configurado
│   │   ├── auth.ts                      # Auth endpoints
│   │   ├── lists.ts                     # Lists endpoints
│   │   ├── items.ts                     # Items endpoints
│   │   └── websocket.ts                 # WebSocket client
│   │
│   ├── types/                           # TypeScript interfaces
│   │   ├── api.ts                       # API DTOs
│   │   ├── models.ts                    # Domain models
│   │   └── index.ts                     # Barrel export
│   │
│   ├── utils/                           # Funções utilitárias
│   │   ├── date.ts                      # Formatação de datas
│   │   ├── validation.ts                # Validações
│   │   └── format.ts                    # Formatação geral
│   │
│   ├── styles/                          # Estilos globais
│   │   ├── global.css                   # CSS global
│   │   └── tailwind.css                 # Tailwind directives
│   │
│   └── assets/                          # Assets estáticos
│       ├── images/
│       ├── icons/
│       └── fonts/
│
├── public/
│   ├── vite.svg
│   └── index.html
│
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── package.json
```

**Princípios:**
- **Pages:** Componentes de nível de rota em `pages/`
- **Components:** Reutilizáveis em `components/` organizados por domínio
- **Type-based:** Separação clara entre pages, components, hooks, contexts
- **Barrel exports:** `index.ts` para imports limpos

---

### File Structure Patterns

**Configuration Files:**

**Backend:**
```
nossalista-api/
├── pom.xml                              # Maven config
├── .gitignore
├── Dockerfile
└── src/main/resources/
    ├── application.yml                   # Config base
    ├── application-dev.yml               # Dev profile
    └── application-prod.yml              # Prod profile
```

**Frontend:**
```
frontend/
├── package.json                         # Dependencies
├── vite.config.ts                       # Vite config
├── tsconfig.json                        # TypeScript config
├── tsconfig.node.json                   # TypeScript (node scripts)
├── tailwind.config.js                   # Tailwind config
├── postcss.config.js                    # PostCSS config
├── .eslintrc.cjs                        # ESLint config
└── .gitignore
```

**Environment Files:**

**Backend:**
```yaml
# application-{profile}.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Frontend:**
```bash
# .env (gitignored)
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws

# .env.production
VITE_API_URL=https://nossalista.leoferolive.com.br/api
VITE_WS_URL=wss://nossalista.leoferolive.com.br/ws
```

---

## Format Patterns

### API Response Formats

**Success Response Structure:**

**Regra:** Respostas de sucesso retornam **o recurso diretamente** (sem envelope)

**GET Single Resource:**
```http
GET /api/lists/{id}

Response 200:
{
  "id": "abc-123",
  "name": "Mercado Semanal",
  "type": "SHOPPING",
  "owner_id": "user-456",
  "created_at": "2026-02-10T12:00:00Z"
}
```

**GET Collection:**
```http
GET /api/lists

Response 200:
[
  {
    "id": "abc-123",
    "name": "Mercado Semanal",
    "type": "SHOPPING"
  },
  {
    "id": "def-456",
    "name": "Tarefas Casa",
    "type": "TASK"
  }
]
```

**POST Create:**
```http
POST /api/lists

Response 201:
{
  "id": "new-uuid",
  "name": "Nova Lista",
  "type": "SHOPPING",
  "created_at": "2026-02-10T12:00:00Z"
}

Headers:
Location: /api/lists/new-uuid
```

**PATCH Update:**
```http
PATCH /api/lists/{id}

Response 200:
{
  "id": "abc-123",
  "name": "Nome Atualizado",
  "type": "SHOPPING",
  "updated_at": "2026-02-10T12:30:00Z"
}
```

**DELETE:**
```http
DELETE /api/lists/{id}

Response 204: No Content
```

---

**Paginated Response (quando aplicável):**

```http
GET /api/lists?page=0&size=20

Response 200:
{
  "content": [
    {"id": "abc-123", "name": "Lista 1"},
    {"id": "def-456", "name": "Lista 2"}
  ],
  "page": 0,
  "size": 20,
  "total_elements": 45,
  "total_pages": 3,
  "first": true,
  "last": false
}
```

---

### Data Exchange Formats

**JSON Field Naming:**

**Regra:** Usar **snake_case** para todos os campos JSON

**Rationale:**
- ✅ **Consistente com DB:** Mesmo padrão do PostgreSQL
- ✅ **Type-safe:** TypeScript interfaces seguem mesmo padrão
- ✅ **Spring Boot:** `PropertyNamingStrategies.SnakeCaseStrategy` configurado globalmente

**Exemplo:**
```json
{
  "list_id": "abc-123",
  "user_id": "user-456",
  "created_at": "2026-02-10T12:00:00Z",
  "is_checked": false,
  "due_date": null
}
```

**TypeScript Interface:**
```typescript
interface ListItemDTO {
  list_id: string;
  user_id: string;
  created_at: string;
  is_checked: boolean;
  due_date: string | null;
}
```

**Spring Configuration:**
```yaml
# application.yml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

---

**Date/Time Format:**

**Regra:** Usar **ISO 8601 string** com timezone UTC

**Formato:**
```json
{
  "created_at": "2026-02-10T12:00:00Z",
  "updated_at": "2026-02-10T12:30:00Z",
  "due_date": "2026-02-15T23:59:59Z"
}
```

**Backend (Java):**
```java
// Entidade
@Column(name = "created_at")
private Instant createdAt;

// DTO retorna ISO 8601 automaticamente
```

**Frontend (TypeScript):**
```typescript
// ISO string → Date
const date = new Date(dto.created_at);

// Date → ISO string
const isoString = date.toISOString();
```

**Boolean Representation:**
- **Formato:** `true` / `false` (JSON nativo)
- **Exemplo:**
  ```json
  {
    "is_checked": true,
    "is_owner": false,
    "is_active": true
  }
  ```

**Null Handling:**
- **Missing fields:** Omitir se `null`
- **Exemplo:**
  ```json
  {
    "name": "Arroz",
    "quantity": 2,
    "checked": false,
    "due_date": null           ← Omitir se não aplicável
  }

  // Melhor:
  {
    "name": "Arroz",
    "quantity": 2,
    "checked": false
  }
  ```

**Array vs Object:**
- **Collection:** Sempre array, mesmo se vazio/único
- **Exemplo:**
  ```json
  {
    "items": [
      {"id": "1", "name": "Arroz"}
    ]
  }
  ```

---

## Communication Patterns

### Event System Patterns

**WebSocket Event Naming:**

**Regra:** Event types em **SCREAMING_SNAKE_CASE**, payload fields em **snake_case**

**Event Envelope Structure:**
```typescript
interface WebSocketMessage {
  type: EventType;
  payload: unknown;
  user_id: string;
  username: string;
  timestamp: string;  // ISO 8601
}

type EventType =
  | 'ITEM_ADDED'
  | 'ITEM_UPDATED'
  | 'ITEM_DELETED'
  | 'ITEM_CHECKED'
  | 'MEMBER_JOINED'
  | 'MEMBER_LEFT'
  | 'LIST_UPDATED';
```

**Exemplos:**
```json
{
  "type": "ITEM_ADDED",
  "payload": {
    "id": "item-123",
    "list_id": "list-456",
    "name": "Arroz",
    "quantity": 2,
    "is_checked": false
  },
  "user_id": "user-789",
  "username": "leo",
  "timestamp": "2026-02-10T12:00:00Z"
}

{
  "type": "ITEM_CHECKED",
  "payload": {
    "id": "item-123",
    "is_checked": true
  },
  "user_id": "user-999",
  "username": "ana",
  "timestamp": "2026-02-10T12:05:00Z"
}

{
  "type": "MEMBER_JOINED",
  "payload": {
    "list_id": "list-456",
    "member": {
      "id": "user-999",
      "username": "ana",
      "avatar_url": "https://..."
    }
  },
  "user_id": "user-999",
  "username": "ana",
  "timestamp": "2026-02-10T12:10:00Z"
}
```

---

### State Management Patterns

**React Context + useReducer Patterns:**

**State Structure:**
```typescript
// Estado imutável
interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}

// Actions são objetos plain
type AuthAction =
  | { type: 'LOGIN'; payload: { user: User; token: string } }
  | { type: 'LOGOUT' }
  | { type: 'REFRESH_TOKEN'; payload: string };

// Reducer retorna novo estado (imutável)
function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN':
      return {
        ...state,
        user: action.payload.user,
        token: action.payload.token,
        isAuthenticated: true
      };
    case 'LOGOUT':
      return {
        ...state,
        user: null,
        token: null,
        isAuthenticated: false
      };
    default:
      return state;
  }
}
```

**State Update Rules:**
1. **Sempre imutável:** Nunca modifique state diretamente
2. **Dispatch actions:** Sempre via reducer/action
3. **Type-safe actions:** Use discriminated unions

---

## Process Patterns

### Error Handling Patterns

**Frontend Error Handling:**

**Regra:** Log técnico no console + User-friendly via notification

**Implementation:**
```typescript
try {
  await api.post('/api/lists', data);
} catch (error) {
  if (error.response?.data) {
    const problem = error.response.data as ProblemDetail;

    // 1. Log técnico (para debugging)
    console.error('[API Error]', {
      type: problem.type,
      title: problem.title,
      status: problem.status,
      detail: problem.detail,
      instance: problem.instance
    });

    // 2. User-friendly notification
    showNotification({
      type: 'error',
      title: problem.title,           // "Lista não encontrada"
      message: problem.detail         // "Lista com ID 123 não existe"
        || 'Ocorreu um erro. Tente novamente.'  // Fallback
    });
  } else {
    // Erro sem ProblemDetail (network, timeout, etc)
    console.error('[Unknown Error]', error);
    showNotification({
      type: 'error',
      title: 'Erro de conexão',
      message: 'Verifique sua internet e tente novamente.'
    });
  }
}
```

**Error Boundary Pattern:**
```typescript
interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[Error Boundary]', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-page">
          <h1>Algo deu errado</h1>
          <p>Recarregue a página para tentar novamente.</p>
        </div>
      );
    }

    return this.props.children;
  }
}
```

---

### Loading State Patterns

**Regra:** `loading` boolean + `error` ProblemDetail + `finally` sempre executa

**Implementation:**
```typescript
function useLists() {
  const [lists, setLists] = useState<ListDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<ProblemDetail | null>(null);

  const fetchLists = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<ListDTO[]>('/lists');
      setLists(response.data);
    } catch (err) {
      const problem = err.response?.data as ProblemDetail;
      setError(problem);
    } finally {
      setLoading(false);  // Sempre executa, mesmo com erro
    }
  };

  useEffect(() => {
    fetchLists();
  }, []);

  return { lists, loading, error, refetch: fetchLists };
}
```

**Component Usage:**
```typescript
function Home() {
  const { lists, loading, error } = useLists();

  if (loading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorMessage error={error} />;
  }

  return (
    <div>
      {lists.map(list => <ListCard key={list.id} list={list} />)}
    </div>
  );
}
```

**Loading UI Pattern:**
```typescript
// Global loading
{isLoading && <GlobalLoadingSpinner />}

// Local loading (inline)
<Button disabled={loading}>
  {loading ? <Spinner /> : 'Salvar'}
</Button>

// Skeleton loading
{loading ? <ListCardSkeleton /> : <ListCard list={list} />}
```

---

## Enforcement Guidelines

### All AI Agents MUST:

**✓ Database:**
- Usar snake_case para tabelas, colunas, índices, constraints
- Nomear FKs como `{tabela}_id`
- Usar migrations Flyway numeradas

**✓ API:**
- Seguir RESTful padrão: `/api/{plural-resource}/{id}`
- Usar kebab-case em endpoints
- Retornar RFC 7807 para erros
- Retornar recurso diretamente (sem envelope) para sucesso

**✓ Código:**
- Java: PascalCase classes, camelCase methods/vars
- React: PascalCase components, camelCase hooks com `use` prefixo
- TypeScript: PascalCase types/interfaces

**✓ Formatos:**
- JSON: snake_case para todos os fields
- DateTime: ISO 8601 strings (UTC)
- Boolean: `true`/`false` nativos

**✓ Estrutura:**
- Backend: Feature-based (pacotes por domínio)
- Frontend: Type-based (pages, components, hooks separados)
- Tests: Espelham estrutura de `src/main` ou `src`

**✓ Processos:**
- Error handling: Console log + User notification
- Loading states: Boolean + ProblemDetail error
- State updates: Sempre imutável via reducer

---

### Pattern Verification

**Automated:**
- ESLint/Prettier para code style frontend
- Checkstyle/SpotBugs para code style backend
- Linting no CI/CD (GitHub Actions)

**Manual:**
- Code review checklist baseado nestes padrões
- Documento sempre disponível para referência

**Pattern Violations:**
- Se padrão precisar mudar: Atualizar este documento primeiro
- Discussão antes de mudar padrões estabelecidos
- Documentar razão da mudança

---

### Pattern Examples

**Good Examples:**

**Backend:**
```java
// ✅ Correto: snake_case no DB, camelCase no Java
@Table(name = "list_items")
public class ListItem {

    @Column(name = "list_id")
    private UUID listId;

    @Column(name = "created_at")
    private Instant createdAt;
}

// ✅ Correto: PascalCase class, camelCase method
public class ListService {
    public List<ListItem> getItemsByListId(UUID listId) {
        return repository.findByListId(listId);
    }
}

// ✅ Correto: RFC 7807 error response
@ExceptionHandler(ListNotFoundException.class)
ProblemDetail listNotFound(ListNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problem.setType(URI.create("https://api.nossalista.com/errors/list-not-found"));
    return problem;
}
```

**Frontend:**
```typescript
// ✅ Correto: PascalCase component
function ListCard({ list }: { list: ListDTO }) {
  const { loading, error } = useListItems(list.id);

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;

  return <div>{list.name}</div>;
}

// ✅ Correto: camelCase hook, snake_case API response
function useLists() {
  const [lists, setLists] = useState<ListDTO[]>([]);

  const fetchLists = async () => {
    const response = await api.get<ListDTO[]>('/lists');
    setLists(response.data);  // snake_case fields
  };

  return { lists, fetchLists };
}

// ✅ Correto: Error handling pattern
try {
  await api.post('/api/lists', data);
} catch (error) {
  const problem = error.response?.data as ProblemDetail;
  console.error('[API Error]', problem);  // Log técnico
  showNotification({ title: problem.title, message: problem.detail });  // User-friendly
}
```

---

**Anti-Patterns (NÃO FAZER):**

```java
// ❌ ERRADO: camelCase no DB
@Table(name = "listItems")  // Deveria ser "list_items"

// ❌ ERRADO: PascalCase method
public List<ListItem> GetItemsByListId() {}  // Deveria ser getItemsByListId()

// ❌ ERRADO: JSON envelope desnecessário
return ResponseEntity.ok(Map.of("data", list));  // Deveria retornar list direto
```

```typescript
// ❌ ERRADO: camelCase component
function listCard({ list }: { list: ListDTO }) {}  // Deveria ser ListCard

// ❌ ERRADO: camelCase hook sem "use"
function getLists() {}  // Deveria ser useLists

// ❌ ERRADO: Modificar state diretamente
function addItem(item: ListItem) {
  lists.push(item);  // ❌ Deveria ser setLists([...lists, item])
}
```

---

## Project Structure & Boundaries

### Complete Project Directory Structure

```
nossalista/                                  # Root do monorepo
│
├── README.md                                # Documentação principal
├── .gitignore                               # Git ignore rules
├── docker-compose.dev.yml                   # Dev local (frontend + backend + db)
│
├── frontend/                                # Frontend: React 19 + Vite + TypeScript
│   ├── public/
│   │   ├── vite.svg
│   │   └── index.html
│   │
│   ├── src/
│   │   ├── main.tsx                        # Entry point
│   │   ├── App.tsx                         # Root component com Router
│   │   ├── vite-env.d.ts                   # Vite type declarations
│   │   │
│   │   ├── pages/                          # Componentes de página (rotas)
│   │   │   ├── Login.tsx                    # FR1, FR2: Login
│   │   │   ├── Register.tsx                 # FR3: Registro
│   │   │   ├── Home.tsx                     # FR9: Listar listas do usuário
│   │   │   ├── ListView.tsx                 # FR10: Detalhe da lista + itens
│   │   │   └── JoinList.tsx                 # FR25: Aceitar convite via link
│   │   │
│   │   ├── components/                     # Componentes reutilizáveis
│   │   │   │
│   │   │   ├── layout/                     # Layout components
│   │   │   │   ├── Header.tsx               # Cabeçalho da app
│   │   │   │   ├── MainLayout.tsx           # Layout principal
│   │   │   │   └── Footer.tsx               # Rodapé
│   │   │   │
│   │   │   ├── lists/                      # FR8-14: List-related components
│   │   │   │   ├── ListCard.tsx             # Card de lista na Home
│   │   │   │   ├── CreateListModal.tsx      # FR8: Modal criar lista
│   │   │   │   ├── InviteModal.tsx          # FR23, FR24: Modal convidar
│   │   │   │   └── ListSettings.tsx         # Configurações da lista
│   │   │   │
│   │   │   ├── items/                      # FR15-22: Item-related components
│   │   │   │   ├── ListItem.tsx             # Renderiza item da lista
│   │   │   │   ├── AddItemForm.tsx          # FR15: Form adicionar item
│   │   │   │   ├── EditItemModal.tsx        # FR16: Editar item
│   │   │   │   └── ItemCheckbox.tsx         # FR18: Checkbox de conclusão
│   │   │   │
│   │   │   └── common/                     # Shared UI components
│   │   │       ├── Button.tsx
│   │   │       ├── Input.tsx
│   │   │       ├── Modal.tsx
│   │   │       ├── LoadingSpinner.tsx
│   │   │       ├── ErrorMessage.tsx         # RFC 7807 error display
│   │   │       ├── Notification.tsx         # Toast notifications
│   │   │       └── ActivityTimeline.tsx     # FR39-45: Timeline de atividades
│   │   │
│   │   ├── hooks/                          # Custom React hooks
│   │   │   ├── useLists.ts                 # Hook para listar/buscar listas
│   │   │   ├── useAuth.ts                  # Hook para auth state
│   │   │   ├── useWebSocket.ts             # Hook para WebSocket (FR31-38)
│   │   │   ├── useListItems.ts             # Hook para itens de lista específica
│   │   │   ├── useLocalStorage.ts          # Hook para localStorage
│   │   │   └── useNotification.ts          # Hook para notificações
│   │   │
│   │   ├── contexts/                       # React Context providers
│   │   │   ├── AuthContext.tsx             # Auth state global
│   │   │   ├── WebSocketContext.tsx        # WebSocket state global
│   │   │   └── NotificationContext.tsx     # Notification state
│   │   │
│   │   ├── api/                            # API client layer
│   │   │   ├── client.ts                   # Axios configurado com interceptors
│   │   │   ├── auth.ts                     # FR1-7: Auth endpoints
│   │   │   ├── lists.ts                    # FR8-14: List endpoints
│   │   │   ├── items.ts                    # FR15-22: Item endpoints
│   │   │   ├── members.ts                  # FR23-30: Member endpoints
│   │   │   ├── activity.ts                 # FR39-45: Activity endpoints
│   │   │   └── websocket.ts                # STOMP WebSocket client
│   │   │
│   │   ├── types/                          # TypeScript type definitions
│   │   │   ├── api.ts                      # API DTOs (snake_case fields)
│   │   │   │   ├── ListDTO.ts
│   │   │   │   ├── ListItemDTO.ts
│   │   │   │   ├── UserDTO.ts
│   │   │   │   └── ProblemDetail.ts        # RFC 7807
│   │   │   ├── models.ts                   # Domain models
│   │   │   └── index.ts                    # Barrel export
│   │   │
│   │   ├── utils/                          # Utility functions
│   │   │   ├── date.ts                     # Format data ISO 8601
│   │   │   ├── validation.ts               # Form validation
│   │   │   └── format.ts                   # Formatação geral
│   │   │
│   │   ├── styles/                         # Estilos globais
│   │   │   ├── global.css                  # CSS global
│   │   │   └── tailwind.css                # Tailwind directives
│   │   │
│   │   └── assets/                         # Static assets
│   │       ├── images/
│   │       ├── icons/
│   │       └── fonts/
│   │
│   ├── index.html
│   ├── vite.config.ts                       # Vite config
│   ├── tsconfig.json                        # TypeScript config
│   ├── tsconfig.node.json                   # TypeScript (node scripts)
│   ├── tailwind.config.js                   # Tailwind config
│   ├── postcss.config.js                    # PostCSS config
│   ├── .eslintrc.cjs                        # ESLint config
│   ├── package.json                         # Dependencies
│   └── .env.example                         # Environment variables template
│
├── nossalista-api/                          # Backend: Spring Boot 4 + Java 25
│   │
│   ├── src/main/java/br/com/leoferolive/nossalista/
│   │   ├── NossaListaApiApplication.java   # @SpringBootApplication
│   │   │
│   │   ├── config/                         # Configuration classes
│   │   │   ├── SecurityConfig.java         # FR1-7: JWT + OAuth2
│   │   │   ├── WebSocketConfig.java        # FR31-38: STOMP config
│   │   │   ├── CorsConfig.java             # CORS configuration
│   │   │   ├── OpenApiConfig.java          # #003: SpringDoc config
│   │   │   └── JacksonConfig.java          # #003: snake_case JSON
│   │   │
│   │   ├── auth/                           # FR1-7: Authentication domain
│   │   │   ├── AuthController.java         # POST /api/auth/*
│   │   │   ├── AuthService.java
│   │   │   ├── JwtService.java             # JWT stateless tokens
│   │   │   └── OAuth2SuccessHandler.java   # Google OAuth2 callback
│   │   │
│   │   ├── user/                           # FR4-7, FR6: User domain
│   │   │   ├── UserController.java         # GET/PATCH /api/users/me, GET /api/users/search
│   │   │   ├── UserService.java
│   │   │   ├── UserRepository.java         # Spring Data JPA
│   │   │   └── User.java                   # @Entity JPA
│   │   │
│   │   ├── list/                           # FR8-14: List domain
│   │   │   ├── ListController.java         # GET/POST/PATCH/DELETE /api/lists/*
│   │   │   ├── ListService.java
│   │   │   ├── ListRepository.java         # Spring Data JPA
│   │   │   ├── List.java                   # @Entity JPA
│   │   │   └── ListType.java               # @Enum (SHOPPING, TASK, WISHLIST, GENERIC)
│   │   │
│   │   ├── item/                           # FR15-22: Item domain
│   │   │   ├── ItemController.java         # GET/POST/PATCH/DELETE /api/lists/{id}/items/*
│   │   │   ├── ItemService.java
│   │   │   ├── ItemRepository.java         # Spring Data JPA
│   │   │   └── ListItem.java               # @Entity JPA
│   │   │
│   │   ├── member/                         # FR23-30: Member domain
│   │   │   ├── MemberController.java       # POST /api/lists/{id}/invite, etc.
│   │   │   ├── MemberService.java
│   │   │   ├── MemberRepository.java       # Spring Data JPA
│   │   │   ├── ListMember.java             # @Entity JPA
│   │   │   └── MemberRole.java            # @Enum (OWNER, MEMBER)
│   │   │
│   │   ├── activity/                       # FR39-45: Activity log domain
│   │   │   ├── ActivityController.java     # GET /api/lists/{id}/activity
│   │   │   ├── ActivityService.java
│   │   │   ├── ActivityRepository.java     # Spring Data JPA
│   │   │   ├── ActivityLog.java            # @Entity JPA
│   │   │   └── ActionType.java            # @Enum (ADDED, CHECKED, REMOVED, etc.)
│   │   │
│   │   ├── websocket/                      # FR31-38: WebSocket handlers
│   │   │   ├── ListWebSocketController.java # @MessageMapping /app/list/*
│   │   │   └── WebSocketAuthInterceptor.java  # JWT validation for WS
│   │   │
│   │   ├── exception/                      # Global error handling
│   │   │   ├── GlobalExceptionHandler.java  # #004: RFC 7807 responses
│   │   │   ├── ListNotFoundException.java
│   │   │   ├── ValidationException.java
│   │   │   └── UnauthorizedException.java
│   │   │
│   │   └── dto/                            # Data Transfer Objects
│   │       ├── request/                    # Request DTOs
│   │       │   ├── CreateListRequest.java
│   │       │   ├── UpdateListRequest.java
│   │       │   ├── AddItemRequest.java
│   │       │   ├── UpdateItemRequest.java
│   │       │   ├── InviteMemberRequest.java
│   │       │   └── RegisterRequest.java
│   │       │
│   │       └── response/                   # Response DTOs
│   │           ├── ListDTO.java
│   │           ├── ListItemDTO.java
│   │           ├── UserDTO.java
│   │           └── ActivityDTO.java
│   │
│   ├── src/main/resources/
│   │   ├── application.yml                 # Base config
│   │   ├── application-dev.yml             # Profile dev: H2 database
│   │   ├── application-prod.yml            # Profile prod: PostgreSQL
│   │   └── db/migration/                   # Flyway migrations
│   │       ├── V1__create_users_table.sql
│   │       ├── V2__create_lists_table.sql
│   │       ├── V3__create_list_items_table.sql
│   │       ├── V4__create_list_members_table.sql
│   │       ├── V5__create_activity_log_table.sql
│   │       ├── V6__add_foreign_keys.sql
│   │       └── V7__add_indexes.sql
│   │
│   ├── src/test/java/br/com/leoferolive/nossalista/
│   │   ├── config/                         # Test configs
│   │   ├── auth/                           # Auth tests (unit + integration)
│   │   ├── user/                           # User tests
│   │   ├── list/                           # List tests
│   │   ├── item/                           # Item tests
│   │   ├── member/                         # Member tests
│   │   └── activity/                       # Activity tests
│   │
│   ├── pom.xml                             # Maven dependencies
│   ├── Dockerfile                          # Backend image
│   └── .env.example                        # Environment variables template
│
├── deploy/                                 # K3s deployment manifests
│   ├── backend-deployment.yaml             # Backend K8s deployment
│   ├── backend-service.yaml                # Backend service
│   ├── frontend-deployment.yaml            # Frontend K8s deployment
│   ├── frontend-service.yaml               # Frontend service
│   ├── postgres-deployment.yaml            # PostgreSQL deployment
│   ├── postgres-persistent-volume.yaml     # PostgreSQL storage
│   ├── ingress.yaml                       # Traefik ingress (Cloudflare Tunnel)
│   └── configmap.yaml                     # Shared config
│
├── .github/
│   └── workflows/
│       ├── backend-ci.yml                 # Path: backend/** → test + build
│       ├── frontend-ci.yml                # Path: frontend/** → test + build
│       └── deploy.yml                     # On: tags ['v*'] → deploy ambos
│
└── docs/                                   # Additional project docs
    ├── NossaLista — Documento de Escopo MVP.txt
    └── API-GUIDE.md                        # #003: API documentation guide
```

### Architectural Boundaries

**API Boundaries:**

**Public Endpoints (sem autenticação):**
```
POST /api/auth/register              # FR3
POST /api/auth/login                 # FR2
GET  /api/auth/google                # FR1: OAuth redirect
GET  /api/auth/google/callback       # FR1: OAuth callback
POST /api/lists/join/{inviteCode}    # FR25: Aceitar convite
```

**Authenticated Endpoints (JWT required):**
```
# User Management (FR4-7)
GET    /api/users/me                  # FR4
PATCH  /api/users/me                  # FR5
GET    /api/users/search?q={username} # FR6

# List Management (FR8-14)
GET    /api/lists                     # FR9
POST   /api/lists                     # FR8
GET    /api/lists/{id}                # FR10
PATCH  /api/lists/{id}                # FR11
DELETE /api/lists/{id}                # FR12

# Item Management (FR15-22)
GET    /api/lists/{id}/items           # Listar itens
POST   /api/lists/{id}/items           # FR15
PATCH  /api/lists/{id}/items/{itemId}  # FR16
DELETE /api/lists/{id}/items/{itemId}  # FR17
PATCH  /api/lists/{id}/items/check    # FR18 (checkbox)

# Members (FR23-30)
POST   /api/lists/{id}/invite          # FR23: Username invite
POST   /api/lists/{id}/invite-link     # FR24: Link invite
DELETE /api/lists/{id}/members/{uid}   # FR27: Remove member
POST   /api/lists/{id}/leave           # FR28: Leave list

# Activity (FR39-45)
GET    /api/lists/{id}/activity        # FR39
```

**WebSocket Endpoints (JWT required via handshake):**
```
SUBSCRIBE  /topic/list/{listId}          # Receber atualizações (FR31)
SEND       /app/list/{listId}/item.add    # FR32
SEND       /app/list/{listId}/item.update # FR33
SEND       /app/list/{listId}/item.delete # FR34
SEND       /app/list/{listId}/item.check  # FR35
```

**Component Boundaries:**

**Frontend Component Communication:**
- **Pages → Components:** Props down, events up
- **Components → API:** Via custom hooks (`useLists`, `useListItems`)
- **Global State:** React Context (Auth, WebSocket, Notifications)
- **Real-time:** WebSocket Context broadcasts to subscribed components

**State Management Boundaries:**
- **AuthContext:** Global, single source of truth for user + token
- **WebSocketContext:** Global, manages STOMP connection per list
- **NotificationContext:** Global, toast/snackbar notifications
- **Local State:** Component-level state (modals open/close, form inputs)

**Service Communication Patterns:**
- **Frontend → Backend:** Axios HTTP client (REST)
- **Backend → Frontend (real-time):** STOMP WebSocket (broadcast)
- **Backend Services:** @Autowired dependency injection
- **No frontend-to-backend WebSocket:** Frontend only subscribes, doesn't send

**Service Boundaries:**

**Backend Layer Boundaries:**
```
Controller Layer → Service Layer → Repository Layer → Database
      ↓                ↓               ↓
   DTOs           Domain Logic    JPA Entities
   Validation     Business Rules  ORM Mapping
```

**Integration Points:**
- **Controllers:** 1 controller por domain (auth, user, list, item, member, activity)
- **Services:** 1 service por domain, com @Transactional
- **Repositories:** Spring Data JPA, 1 repository per entity
- **DTOs:** Separate request/response DTOs per operation

**Cross-Cutting:**
- **Security:** JWT validation via SecurityConfig (filter level)
- **Error Handling:** GlobalExceptionHandler catches all exceptions
- **WebSocket:** ListWebSocketController broadcasts to /topic/list/{id}

**Data Boundaries:**

**Database Schema Boundaries:**
```
users (user_id PK)
  ↓ 1:N
lists (list_id PK, owner_id FK)
  ↓ 1:N
list_items (item_id PK, list_id FK)
  ↓ 1:N
activity_log (activity_id PK, list_id FK)

lists ↔ list_members (N:M via junction table)
```

**Data Access Patterns:**
- **Read-Only:** Repository methods (findBy*, findAll)
- **Write:** Service methods with @Transactional
- **Validation:** Service layer validates before persisting
- **Audit:** ActivityService logs all writes

**No Caching Layer (MVP):**
- Direct database access (no Redis between)
- WebSocket provides real-time cache for clients
- Future: Add Redis if performance issues arise

### Requirements to Structure Mapping

**Feature Domain Mapping:**

| FR Category | Backend Package | Frontend Location | API Base |
|-------------|-----------------|-------------------|----------|
| **Auth (FR1-7)** | `auth/`, `user/` | `pages/Login.tsx`, `pages/Register.tsx`, `contexts/AuthContext.tsx` | `/api/auth/*`, `/api/users/*` |
| **Lists (FR8-14)** | `list/` | `components/lists/`, `pages/Home.tsx` | `/api/lists/*` |
| **Items (FR15-22)** | `item/` | `components/items/`, `pages/ListView.tsx` | `/api/lists/{id}/items/*` |
| **Members (FR23-30)** | `member/` | `components/lists/InviteModal.tsx` | `/api/lists/{id}/members/*`, `/api/lists/{id}/invite*` |
| **Real-time (FR31-38)** | `websocket/` | `hooks/useWebSocket.ts`, `contexts/WebSocketContext.tsx` | WS: `/topic/list/{id}`, `/app/list/{id}/*` |
| **Activity (FR39-45)** | `activity/` | `components/common/ActivityTimeline.tsx` | `/api/lists/{id}/activity` |

**Cross-Cutting Concerns:**

| Concern | Backend Location | Frontend Location | Integration |
|---------|------------------|-------------------|-------------|
| **Authentication** | `config/SecurityConfig.java`, `auth/JwtService.java` | `contexts/AuthContext.tsx`, `hooks/useAuth.ts` | JWT in Authorization header |
| **Error Handling** | `exception/GlobalExceptionHandler.java` | `components/common/ErrorMessage.tsx`, `api/client.ts` | RFC 7807 responses |
| **Validation** | Service layer, `@Valid` annotations | `utils/validation.ts`, form libraries | BeanValidation → 422 status |
| **Real-time** | `config/WebSocketConfig.java`, `websocket/` | `contexts/WebSocketContext.tsx`, `hooks/useWebSocket.ts` | STOMP subscriptions |
| **Logging** | SLF4J (everywhere) | `console.error` in error handlers | Não integrado |
| **Notifications** | N/A | `contexts/NotificationContext.tsx` | Toast/snackbar UI |

### Integration Points

**Internal Communication:**

**Frontend → Backend:**
```
Component → Custom Hook → Axios Client → Controller → Service → Repository → DB
  ↓            ↓             ↓              ↓            ↓            ↓
ListCard  useListItems   api/lists.ts   ListController ListService ListRepository PostgreSQL
```

**Backend → Frontend (Real-time):**
```
Service → WebSocketController → STOMP Broker → /topic/list/{id} → WebSocket Client → Context → Component
  ↓                             ↓                    ↓                      ↓                ↓
ItemService broadcasts      ITEM_ADDED event      Stomp over WS     useWebSocket    ListItem re-renders
```

**Frontend Component Communication:**
```
Parent Component (ListView)
  ↓ props
Child Component (ListItem)
  ↑ callback event
Parent updates state → triggers API call → WebSocket broadcast → other clients update
```

**External Integrations:**

| Third-party | Integration Point | Purpose |
|-------------|-------------------|---------|
| **Google OAuth2** | `auth/OAuth2SuccessHandler.java` | FR1: Google login |
| **Cloudflare Tunnel** | `deploy/ingress.yaml` | Expose service to internet |
| **GitHub Actions** | `.github/workflows/*` | CI/CD automated deploy |
| **PostgreSQL** | `application-prod.yml`, `deploy/postgres-deployment.yaml` | Production database |
| **H2 Database** | `application-dev.yml` | Development database |

**Data Flow:**

**Create Item Flow:**
```
1. User types "Arroz" in AddItemForm
2. onSubmit → useListItems().add()
3. Axios POST /api/lists/{id}/items
4. ListController → ItemService.create()
5. ItemRepository.save() → PostgreSQL INSERT
6. ActivityService.log() → PostgreSQL INSERT
7. WebSocketController broadcasts ITEM_ADDED to /topic/list/{id}
8. All subscribed clients receive event via WebSocket
9. Each client's WebSocketContext updates state
10. All ListItems re-render with new item
```

**WebSocket Reconnection Flow:**
```
1. Connection drops (Cloudflare Tunnel timeout, network issue)
2. SockJS detects disconnect → onClose event
3. useWebSocket hook's onError/onClose
4. Exponential backoff: reconnect(1s) → reconnect(2s) → reconnect(4s)...
5. On success: re-subscribe to /topic/list/{id}, request current list state
6. User sees brief "Reconnecting..." indicator, then back to normal
```

### File Organization Patterns

**Configuration Files:**

**Root Level:**
```
nossalista/
├── .gitignore                          # Git ignore patterns
├── docker-compose.dev.yml              # Dev: frontend + backend + postgres
├── README.md                           # Project documentation
└── docs/                               # Additional docs
```

**Backend Config:**
```
nossalista-api/
├── pom.xml                             # Maven dependencies & build config
├── Dockerfile                          # Container image
├── .env.example                        # DB_URL, JWT_SECRET, OAuth client ID
└── src/main/resources/
    ├── application.yml                 # Base config (port, jackson, actuator)
    ├── application-dev.yml             # H2, debug logging, dev features
    └── application-prod.yml            # PostgreSQL, production logging
```

**Frontend Config:**
```
frontend/
├── package.json                        # Dependencies, scripts
├── vite.config.ts                      # Vite plugins, build config
├── tsconfig.json                       # TypeScript compiler options
├── tailwind.config.js                  # Tailwind theme, content paths
├── .eslintrc.cjs                       # ESLint rules
└── .env.example                        # VITE_API_URL, VITE_WS_URL
```

**Source Organization:**

**Backend Source:**
- **Entry:** `NossaListaApiApplication.java` (main method)
- **Domains:** One package per domain (auth, user, list, item, member, activity, websocket)
- **Cross-cutting:** `config/`, `exception/`, `dto/`
- **Layering:** Controller → Service → Repository → Entity

**Frontend Source:**
- **Entry:** `main.tsx` (ReactDOM.createRoot)
- **Routing:** `App.tsx` (React Router setup)
- **Pages:** Route components in `pages/`
- **Features:** Domain-specific components in `components/{domain}/`
- **Shared:** Reusable UI in `components/common/`
- **State:** Contexts in `contexts/`, hooks in `hooks/`

**Test Organization:**

**Backend Tests:**
```
src/test/java/.../
├── auth/AuthServiceTest.java           # Unit tests (@ExtendWith(MockitoExtension))
├── auth/AuthControllerIntegrationTest.java # Integration (@SpringBootTest, @AutoConfigureMockMvc)
├── list/ListRepositoryTest.java        # Testcontainers (@Testcontainers)
└── ...mirrors main structure...
```

**Frontend Tests:**
```
frontend/src/
├── components/__tests__/               # Component tests (Vitest + Testing Library)
│   ├── lists/ListCard.test.tsx
│   └── items/ListItem.test.tsx
├── hooks/__tests__/                     # Hook tests
│   ├── useLists.test.ts
│   └── useAuth.test.ts
└── e2e/                                 # E2E tests (Playwright)
    └── flows/create-list.spec.ts
```

**Asset Organization:**

**Static Assets:**
```
frontend/public/
├── vite.svg                             # Favicon
└── index.html                           # HTML template

frontend/src/assets/
├── images/                              # App images (logos, illustrations)
├── icons/                               # Icon files (if not using icon library)
└── fonts/                               # Custom fonts (if not using web fonts)
```

### Development Workflow Integration

**Development Server Structure:**

**Backend Dev Server:**
```bash
cd nossalista-api
mvn spring-boot:run
# Runs on :8080
# Uses H2 in-memory database (application-dev.yml)
# Hot reload via Spring Boot DevTools
# Actuator at http://localhost:8080/actuator/health
# Swagger UI at http://localhost:8080/swagger-ui/index.html
```

**Frontend Dev Server:**
```bash
cd frontend
npm run dev
# Runs on :5173
# HMR instantâneo
# Proxies API requests to :8080 via vite.config.ts proxy
```

**Docker Compose (Dev):**
```bash
docker-compose -f docker-compose.dev.yml up
# Spins up: frontend (:3000) + backend (:8080) + postgres (:5432)
# All interconnected via Docker network
# Volumes mounted for hot-reload
```

**Build Process Structure:**

**Backend Build:**
```bash
cd nossalista-api
mvn clean package
# Generates: target/nossalista-api-1.0.0.jar
# Runs tests during build
# Dockerfile copies jar into image
```

**Frontend Build:**
```bash
cd frontend
npm run build
# Generates: dist/ folder with optimized static assets
# Vite optimizes, minifies, tree-shakes
# Dockerfile copies dist into nginx image
```

**Deployment Structure:**

**K3s Deployment:**
```bash
# GitHub Actions triggers on tag (e.g., v1.0.0)
1. Build backend image → push to ghcr.io/leoferolive/nossalista-api:v1.0.0
2. Build frontend image → push to ghcr.io/leoferolive/nossalista-web:v1.0.0
3. kubectl apply -f deploy/backend-deployment.yaml (updates image tag)
4. kubectl apply -f deploy/frontend-deployment.yaml (updates image tag)
5. K3s rolling update: zero-downtime deployment
6. Traefik Ingress routes nossalista.leoferolive.com.br → services
```

**Production Config:**
```
PostgreSQL: Persistent volume in cluster
Backend: Replicas: 1 (sufficient for home server)
Frontend: Replicas: 1
Resources: CPU/memory limits configured for Raspberry Pi 4
```

---

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:**

Todas as decisões arquiteturais trabalham juntas de forma coerente:

| Aspecto | Avaliação | Detalhes |
|---------|-----------|----------|
| **Stack Compatibility** | ✅ Excelente | Java 25 + Spring Boot 4 + React 19 + Vite totalmente compatíveis |
| **Database Strategy** | ✅ Excelente | PostgreSQL (prod) + H2 (dev) com Flyway migrations |
| **Real-time Architecture** | ✅ Boa | STOMP/SockJS via Cloudflare Tunnel (requer teste de estabilidade) |
| **Authentication** | ✅ Padrão | JWT stateless + Google OAuth2 bem definidos |
| **Monorepo Structure** | ✅ Funcional | CI/CD incremental com path filters funciona |
| **Build Tools** | ✅ Adequado | Maven (backend) + Vite (frontend) são padrões da indústria |

**Riscos Identificados:**
- ⚠️ **Cloudflare Tunnel WebSocket:** Pode derrubar conexões após inatividade. **Mitigação:** Keep-alive configurável, reconexão automática no cliente

**Pattern Consistency:**

Padrões de implementação suportam todas as decisões:

- ✅ **Naming:** snake_case (DB) ↔️ snake_case (JSON) ↔️ snake_case (TypeScript) - 100% consistente
- ✅ **Error Handling:** RFC 7807 (#004) ↔️ ProblemDetail TypeScript ↔️ Error boundary pattern - coerente
- ✅ **WebSocket:** Event-Type Envelope (#005) ↔️ STOMP ↔️ WebSocketContext - alinhado
- ✅ **State Management:** React Context + useReducer (#006) ↔️ State update patterns - consistente

**Structure Alignment:**

Estrutura do projeto habilita todas as decisões:

- ✅ **Backend:** Feature-based packages (auth, user, list, item, member, activity) suportam domínios do negócio
- ✅ **Frontend:** Type-based organization (pages, components, hooks, contexts) habilita padrões React
- ✅ **Tests:** Espelham estrutura main - garante cobertura
- ✅ **Integration Points:** Frontend ↔️ Backend (REST) + Backend → Frontend (WebSocket) bem definidos

---

### Requirements Coverage Validation ✅

**Epic/Feature Coverage:**

Todos os domínios funcionais do NossaLista têm suporte arquitetural completo:

| Domínio | FRs | Suporte Arquitetural | Status |
|---------|-----|----------------------|--------|
| **Authentication** | FR1-FR7 (7) | auth/ package + SecurityConfig + JWT + OAuth2 | ✅ Completo |
| **List Management** | FR8-FR14 (7) | list/ package + ListController + List entity | ✅ Completo |
| **Item Management** | FR15-FR22 (8) | item/ package + ItemController + ListItem entity | ✅ Completo |
| **Sharing/Collaboration** | FR23-FR30 (8) | member/ package + ListMember entity + roles | ✅ Completo |
| **Real-time Sync** | FR31-FR38 (8) | websocket/ package + STOMP + Event-Type Envelope | ✅ Completo |
| **Activity/History** | FR39-FR45 (7) | activity/ package + ActivityLog entity | ✅ Completo |

**Total:** 45 FRs → 100% cobertura arquitetural

**Functional Requirements Coverage:**

Todas as categorias de FR estão cobertas por decisões arquiteturais específicas:

- ✅ **FR1-7 (Auth):** Decision #007 (JWT stateless), SecurityConfig, OAuth2 integration
- ✅ **FR8-14 (Lists):** Decision #002 (colunas nullable), list/ domain, CRUD endpoints
- ✅ **FR15-22 (Items):** Decision #002, item/ domain, sub-resource endpoints `/api/lists/{id}/items`
- ✅ **FR23-30 (Members):** member/ domain, ListMember entity, OWNER/MEMBER roles
- ✅ **FR31-38 (Real-time):** Decision #005 (Event-Type Envelope), websocket/ package, STOMP
- ✅ **FR39-45 (Activity):** activity/ domain, ActionType enum, audit trail

**Non-Functional Requirements Coverage:**

Todos os NFRs foram endereçados arquiteturalmente:

| NFR Categoria | Requisitos | Decisões Arquiteturais | Status |
|---------------|-----------|-------------------------|--------|
| **Performance (P1-P3)** | Latência < 500ms, TTI < 3s | Vite (HMR rápido), STOMP WebSocket, otimizações | ✅ Endereçado |
| **Security (S1-S7)** | HTTPS, JWT, OAuth2, bcrypt | #007 JWT + SecurityConfig + #004 RFC 7807 | ✅ Coberto |
| **Reliability (R1-R5)** | Uptime > 95%, logs 30 dias | #008 Logback (30 dias), #009 Actuator, backup diário | ✅ Coberto |
| **Accessibility (A1-A5)** | WCAG AA, teclado, touch 44px | Tailwind components, contraste, navegação | ✅ Endereçado |
| **Integration (I1-I4)** | Google OAuth, WebSocket, K3s | Integrações definidas em fronteiras externas | ✅ Coberto |

---

### Implementation Readiness Validation ✅

**Decision Completeness:**

- ✅ **12 decisões documentadas:** Todas com contexto, opções consideradas, rationale, consequências
- ✅ **Versões especificadas:** Java 25, Spring Boot 4.0.2, React 19, PostgreSQL 16
- ✅ **Trade-offs analisados:** Monorepo vs multi-repo, colunas nullable vs JSONB, etc.
- ✅ **Código exemplo:** Cada decisão importante vem com exemplo de implementação

**Structure Completeness:**

- ✅ **Monorepo completo:** frontend/ + nossalista-api/ + deploy/ + .github/workflows/
- ✅ **Backend:** 7 domínios (auth, user, list, item, member, activity, websocket)
- ✅ **Frontend:** 5 pages, 15+ components, 5 hooks, 3 contexts, 4 categorias API
- ✅ **Tests:** Estrutura espelha main, Testcontainers PostgreSQL
- ✅ **Integration Points:** API boundaries, WebSocket subscriptions, event flows

**Pattern Completeness:**

- ✅ **10 categorias de padrões:** 50+ regras de consistência documentadas
- ✅ **Naming conventions:** Database (snake_case), API (kebab-case), Code (PascalCase/camelCase)
- ✅ **Format standards:** JSON (snake_case), DateTime (ISO 8601), Boolean (true/false)
- ✅ **Communication patterns:** WebSocket events, REST responses, error handling
- ✅ **Process patterns:** Loading states, error recovery, state updates
- ✅ **Anti-padrões:** Exemplos do que NÃO fazer para cada categoria

**Enforcement:**

- ✅ **Automated:** ESLint/Prettier (frontend), Checkstyle (backend), CI/CD linting
- ✅ **Manual:** Code review checklist baseado nos padrões
- ✅ **Document-driven:** Este documento é single source of truth

---

### Gap Analysis Results

**Critical Gaps:** **NENHUM** ✅

Tudo que é crítico para implementação foi definido.

**Important Gaps:** **4 áreas identificadas**

1. **Flyway Migrations SQL**
   - **Status:** Estrutura de migrations definida (V1__*, V2__*, etc.), mas SQL não gerado
   - **Impacto:** Agentes AI precisam gerar SQL consistente
   - **Mitigação:** Padrões de naming bem definidos (#002) + schema completo descrito reduzem risco significativamente
   - **Ação:** SQL será gerado durante implementação (fase 1 do roadmap)

2. **Docker Configuration**
   - **Status:** Dockerfiles mencionados na estrutura, mas não definidos
   - **Impacto:** Imagens podem variar levemente entre agentes
   - **Mitigação:** Docker multi-stage é padrão da indústria, baixo risco
   - **Ação:** Dockerfiles serão criados durante fase 5 (Infra & Deploy)

3. **K3s Manifests**
   - **Status:** Estrutura definida, mas YAMLs Kubernetes não gerados
   - **Impacto:** Deploy pode variar
   - **Mitigação:** Estrutura bem documentada, padrões K8s são estabelecidos
   - **Ação:** Manifests serão criados durante fase 5 (Infra & Deploy)

4. **CI/CD Workflows**
   - **Status:** Estrutura de paths definida, mas workflows YAML não gerados
   - **Impacto:** Deploy automatizado pode variar na configuração
   - **Mitigação:** GitHub Actions é padrão, documentação é clara
   - **Ação:** Workflows serão criados durante fase 5 (Infra & Deploy)

**Nice-to-Have Gaps:** **3 melhorias futuras**

1. **OpenAPI/TypeScript Shared Types** (#003)
   - SpringDoc gera OpenAPI, mas workflow de geração de clientes TypeScript não definido
   - **Quando:** Após backend estar funcional e estável
   - **Benefício:** Type safety total entre frontend e backend

2. **Monitoring Dashboard**
   - Actuator expõe métricas, mas dashboard visual não definido
   - **Quando:** Se monitoramento manual via logs não for suficiente
   - **Benefício:** Visualização em tempo real de métricas

3. **Playwright E2E Tests**
   - Estrutura definida (e2e/), mas testes não escritos
   - **Quando:** Após MVP funcional estar estável
   - **Benefício:** Confiança em fluxos críticos de ponta a ponta

---

### Validation Issues Addressed

**Nenhum issue crítico encontrado.**

Arquitetura está coerente, completa e pronta para implementação. Gaps identificados são:
- **Important:** Serão endereçados durante fases apropriadas do roadmap (migrations SQL na Fase 1, Docker/K3s/CI-CD na Fase 5)
- **Nice-to-have:** Podem ser adicionados posteriormente sem impacto na arquitetura base

---

### Architecture Completeness Checklist

**✅ Requirements Analysis**

- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed (BAIXA, web app real-time)
- [x] Technical constraints identified (Raspberry Pi, Cloudflare Tunnel, K3s)
- [x] Cross-cutting concerns mapped (6 categorias críticas)

**✅ Architectural Decisions**

- [x] Critical decisions documented with versions (#001-#012)
- [x] Technology stack fully specified (Java 25, Spring Boot 4.0.2, React 19, PostgreSQL 16)
- [x] Integration patterns defined (REST API, STOMP WebSocket, OAuth2)
- [x] Performance considerations addressed (Vite HMR, < 500ms WebSocket latency)

**✅ Implementation Patterns**

- [x] Naming conventions established (snake_case DB/API/JSON)
- [x] Structure patterns defined (feature-based backend, type-based frontend)
- [x] Communication patterns specified (REST, WebSocket events, state updates)
- [x] Process patterns documented (error handling, loading states, validation)

**✅ Project Structure**

- [x] Complete directory structure defined (monorepo completo)
- [x] Component boundaries established (API, Service, Data layers)
- [x] Integration points mapped (45 FRs → arquivos específicos)
- [x] Requirements to structure mapping complete (categorizado por domínio)

---

### Architecture Readiness Assessment

**Overall Status:** **✅ READY FOR IMPLEMENTATION**

**Confidence Level:** **ALTA**

Baseado em:
- ✅ Coerência completa entre decisões
- ✅ Cobertura 100% dos requisitos (45 FRs + 17 NFRs)
- ✅ Padrões abrangentes (50+ regras de consistência)
- ✅ Estrutura clara e específica
- ✅ Gaps não-críticos bem mitigados

**Key Strengths:**

1. **Stack Tecnológico Moderno:** Java 25 + Spring Boot 4 + React 19 representa o estado da arte (2025-2026)
2. **Real-time First:** Arquitetura otimizada para o diferencial principal (sincronização instantânea)
3. **Consistência Garantida:** Padrões abrangentes asseguram implementação uniforme por múltiplos agentes
4. **Home Server Ready:** Decisões consideram constraints do Raspberry Pi (recursos limitados)
5. **Escalabilidade:** Embora BAIXA complexidade, arquitetura suporta crescimento futuro
6. **Documentação Completa:** 3000+ linhas de decisões, padrões, estrutura, exemplos
7. **Pragmática:** Decisões balançadas (ex: colunas nullable ao invés de JSONB para simplicidade)

**Areas for Future Enhancement:**

1. **Post-MVP:** Shared types entre frontend/backend via OpenAPI
2. **Post-MVP:** Monitoring com Prometheus/Grafana se necessário
3. **Post-MVP:** Testes E2E Playwright automatizados
4. **Post-MVP:** Redis caching se performance tornar-se problema
5. **Post-MVP:** Rate limiting se necessário (Cloudflare pode prover)

---

### Implementation Handoff

**Para AI Agents (e Desenvolvedores):**

🎯 **Guia de Implementação - SIGA ESTES PRINCÍPIOS:**

1. **Follow Decisions Exactly:**
   - Todas as 12 decisões arquiteturais (#001-#012) são obrigatórias
   - Stack tecnológica não pode ser alterada (Java 25, Spring Boot 4.0.2, React 19, Vite, PostgreSQL)
   - Monorepo structure é fixa

2. **Use Patterns Consistently:**
   - Naming: SEMPRE snake_case no DB, API JSON, TypeScript fields
   - API: SEMPRE plural, kebab-case endpoints (/api/lists, não /api/list)
   - Error Handling: SEMPRE RFC 7807 ProblemDetails para erros de API
   - WebSocket: SEMPRE Event-Type Envelope (#005)

3. **Respect Project Structure:**
   - Backend: Feature-based packages (auth/, user/, list/, item/, member/, activity/)
   - Frontend: Type-based (pages/, components/, hooks/, contexts/)
   - Tests: Espelham estrutura de src/main ou src/
   - Não invente novos pacotes/diretórios sem consultar arquitetura

4. **Refer to This Document:**
   - Antes de tomar qualquer decisão técnica, consulte este documento
   - Se o documento não cobre, discuta antes de decidir
   - Padrões neste documento são "single source of truth"

**First Implementation Priority:**

1. **Initialize Project (Starter Template):**
   ```bash
   # Backend (Fase 1 - Fundação)
   curl https://start.spring.io/starter.zip \
     -d type=maven-project \
     -d language=java \
     -d bootVersion=4.0.2 \
     -d groupId=br.com.leoferolive \
     -d artifactId=nossalista-api \
     -d packageName=br.com.leoferolive.nossalista \
     -d dependencies=web,data-jpa,postgresql,websocket,security,validation,flyway \
     -d javaVersion=25 \
     -o backend.zip
   
   # Frontend
   npm create vite@latest frontend -- --template react-ts
   cd frontend && npm install
   npm install -D tailwindcss postcss autoprefixer
   npx tailwindcss init -p
   ```

2. **Setup Database Model (Decision #002):**
   - Criar migrations Flyway V1-V7 com schema definido
   - Seguir padrão snake_case para todas as tabelas/colunas
   - Implementar entidades JPA com @Column(name="snake_case")

3. **Implement Authentication (Decision #007):**
   - Configurar SecurityConfig para JWT + OAuth2
   - Criar JwtService para geração/validação de tokens
   - Implementar endpoints /api/auth/*

4. **Implement Real-time (Decision #005):**
   - Configurar WebSocketConfig (STOMP + SockJS)
   - Criar ListWebSocketController
   - Implementar Event-Type Envelope para mensagens

5. **Implement Frontend (Patterns):**
   - Criar contexts (AuthContext, WebSocketContext)
   - Criar hooks (useAuth, useWebSocket, useLists)
   - Implementar componentes seguindo estrutura definida

**Success Criteria:**

- ✅ AI agents podem implementar features consistentemente seguindo este documento
- ✅ Zero ambiguidade em decisões técnicas críticas
- ✅ 45 FRs implementáveis com arquitetura definida
- ✅ Múltiplos agentes podem trabalhar em paralelo sem conflitos

---

**Próximo:** Completar workflow e fornecer orientações finais de implementação.

---

## 🎯 Implementation Guidance

### Quick Start - Next Steps

**1. Initialize Projects (Starter Templates):**

```bash
# Backend (Fase 1 - Fundação)
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=4.0.2 \
  -d groupId=br.com.leoferolive \
  -d artifactId=nossalista-api \
  -d packageName=br.com.leoferolive.nossalista \
  -d dependencies=web,data-jpa,postgresql,websocket,security,validation,flyway \
  -d javaVersion=25 \
  -o backend.zip

unzip backend.zip && mv backend nossalista-api

# Frontend
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

**2. Follow Roadmap Phases:**

| Fase | Focus | Arquitetura Base |
|------|-------|-----------------|
| **Fase 1** | Fundação Backend | Decisões #002, #007, #008, #009 |
| **Fase 2** | Compartilhamento | Decision #002 (members), package `member/` |
| **Fase 3** | Real-time | Decisão #005, package `websocket/` |
| **Fase 4** | Frontend | Decisão #006, patterns React/TypeScript |
| **Fase 5** | Infra & Deploy | Decisão #001, #010, K3s manifests, CI/CD |

**3. Reference This Document:**

- Antes de qualquer decisão técnica → **consulte architecture.md**
- Padrões de naming → **seção Implementation Patterns**
- Estrutura de arquivos → **seção Project Structure**
- Decisões arquiteturais → **seções Decision #XXX**

---

## 🎓 Architecture Highlights for Quick Reference

**Key Architectural Decisions:**

| # | Decision | Impact |
|---|----------|--------|
| #001 | Monorepo | Frontend + Backend juntos, deploy coordenado |
| #002 | Colunas Nullable | Simples, performático, type-safe |
| #003 | SpringDoc OpenAPI 3 | Documentação automática da API |
| #004 | RFC 7807 Problem Details | Erros consistentes e type-safe |
| #005 | Event-Type Envelope | WebSocket mensagens estruturadas |
| #006 | React Context + useReducer | State management simples e suficiente |
| #007 | JWT stateless | Auth sem servidor de sessão |
| #008 | SLF4J + Logback | Logging eficiente com 30 dias retention |
| #009 | Spring Boot Actuator | Health checks para K3s |
| #010 | GitHub Container Registry | Imagens Docker no GitHub |
| #011 | Testing Pyramid (70/20/10) | Cobertura equilibrada |
| #012 | Testcontainers PostgreSQL | Integration tests realistas |

**Critical Patterns:**

- **Naming:** `snake_case` em DB, API, JSON (100% consistente)
- **API:** `/api/{plural-resource}/{id}` (RESTful padrão)
- **WebSocket:** Event types em `SCREAMING_SNAKE_CASE`, payload `snake_case`
- **Error Handling:** RFC 7807 sempre, log técnico + user-friendly notification
- **Loading States:** Boolean loading + ProblemDetail error + finally sempre executa

---

## 📚 Document Summary

**File:** `_bmad-output/planning-artifacts/architecture.md`

**Sections:**
1. ✅ Project Context Analysis (FRs, NFRs, Constraints, Cross-cutting concerns)
2. ✅ Architectural Decision #001: Repository Structure (Monorepo)
3. ✅ Starter Template Evaluation (Vite + Spring Initializr)
4. ✅ Core Architectural Decisions (#001-#012)
5. ✅ Implementation Patterns & Consistency Rules (50+ regras)
6. ✅ Project Structure & Boundaries (Monorepo completo)
7. ✅ Architecture Validation Results (Coerência ✅, Cobertura ✅, Prontidão ✅)

**Total Lines:** ~3900 lines

**Total Decisions:** 12

**Total Patterns:** 10 categorias

**Total Examples:** 30+ code examples

---

## ✨ Success Criteria - All Met

- ✅ Complete architecture document delivered
- ✅ All architectural decisions documented with rationale
- ✅ Implementation patterns prevent AI agent conflicts
- ✅ Project structure supports all requirements
- ✅ Validation confirms 100% FR coverage
- ✅ Ready for AI agent implementation

---

**Leo, sua arquitetura está completa e pronta para guiar a implementação do NossaLista!**

Qualquer dúvida sobre o documento, estou à disposição. Você pode me perguntar sobre qualquer decisão, padrão ou estrutura definida aqui.

**Próximos passos sugeridos:**

1. **Executar comando de inicialização** (seção Quick Start acima)
2. **Criar primeiro commit** com estrutura inicial
3. **Seguir roadmap** do Documento de Escopo MVP (Fases 1-5)
4. **Referir este documento** sempre que tiver dúvidas técnicas

**Parabéns pela conclusão deste workflow de arquitetura!** 🚀

---
**Workflow Status:** COMPLETED ✅

**Document Location:** `_bmad-output/planning-artifacts/architecture.md`

**Last Updated:** 2026-02-10

---

## 🎯 Implementation Guidance

### Quick Start - Next Steps

**1. Initialize Projects (Starter Templates):**

```bash
# Backend (Fase 1 - Fundação)
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=4.0.2 \
  -d groupId=br.com.leoferolive \
  -d artifactId=nossalista-api \
  -d packageName=br.com.leoferolive.nossalista \
  -d dependencies=web,data-jpa,postgresql,websocket,security,validation,flyway \
  -d javaVersion=25 \
  -o backend.zip

unzip backend.zip && mv backend nossalista-api

# Frontend
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

**2. Follow Roadmap Phases:**

| Fase | Focus | Arquitetura Base |
|------|-------|-----------------|
| **Fase 1** | Fundação Backend | Decisões #002, #007, #008, #009 |
| **Fase 2** | Compartilhamento | Decision #002 (members), package `member/` |
| **Fase 3** | Real-time | Decisão #005, package `websocket/` |
| **Fase 4** | Frontend | Decisão #006, patterns React/TypeScript |
| **Fase 5** | Infra & Deploy | Decisão #001, #010, K3s manifests, CI/CD |

**3. Reference This Document:**

- Antes de qualquer decisão técnica → **consulte architecture.md**
- Padrões de naming → **seção Implementation Patterns**
- Estrutura de arquivos → **seção Project Structure**
- Decisões arquiteturais → **seções Decision #XXX**

---

## 🎓 Architecture Highlights for Quick Reference

**Key Architectural Decisions:**

| # | Decision | Impact |
|---|----------|--------|
| #001 | Monorepo | Frontend + Backend juntos, deploy coordenado |
| #002 | Colunas Nullable | Simples, performático, type-safe |
| #003 | SpringDoc OpenAPI 3 | Documentação automática da API |
| #004 | RFC 7807 Problem Details | Erros consistentes e type-safe |
| #005 | Event-Type Envelope | WebSocket mensagens estruturadas |
| #006 | React Context + useReducer | State management simples e suficiente |
| #007 | JWT stateless | Auth sem servidor de sessão |
| #008 | SLF4J + Logback | Logging eficiente com 30 dias retention |
| #009 | Spring Boot Actuator | Health checks para K3s |
| #010 | GitHub Container Registry | Imagens Docker no GitHub |
| #011 | Testing Pyramid (70/20/10) | Cobertura equilibrada |
| #012 | Testcontainers PostgreSQL | Integration tests realistas |

**Critical Patterns:**

- **Naming:** `snake_case` em DB, API, JSON (100% consistente)
- **API:** `/api/{plural-resource}/{id}` (RESTful padrão)
- **WebSocket:** Event types em `SCREAMING_SNAKE_CASE`, payload `snake_case`
- **Error Handling:** RFC 7807 sempre, log técnico + user-friendly notification
- **Loading States:** Boolean loading + ProblemDetail error + finally sempre executa

---

## 📚 Document Summary

**File:** `_bmad-output/planning-artifacts/architecture.md`

**Sections:**
1. ✅ Project Context Analysis (FRs, NFRs, Constraints, Cross-cutting concerns)
2. ✅ Architectural Decision #001: Repository Structure (Monorepo)
3. ✅ Starter Template Evaluation (Vite + Spring Initializr)
4. ✅ Core Architectural Decisions (#001-#012)
5. ✅ Implementation Patterns & Consistency Rules (50+ regras)
6. ✅ Project Structure & Boundaries (Monorepo completo)
7. ✅ Architecture Validation Results (Coerência ✅, Cobertura ✅, Prontidão ✅)

**Total Lines:** ~4200 lines

**Total Decisions:** 12

**Total Patterns:** 10 categorias

**Total Examples:** 30+ code examples

---

## ✨ Success Criteria - All Met

- ✅ Complete architecture document delivered
- ✅ All architectural decisions documented with rationale
- ✅ Implementation patterns prevent AI agent conflicts
- ✅ Project structure supports all requirements
- ✅ Validation confirms 100% FR coverage
- ✅ Ready for AI agent implementation

---

**Leo, sua arquitetura está completa e pronta para guiar a implementação do NossaLista!**

Qualquer dúvida sobre o documento, estou à disposição. Você pode me perguntar sobre qualquer decisão, padrão ou estrutura definida aqui.

**Próximos passos sugeridos:**

1. **Executar comando de inicialização** (seção Quick Start acima)
2. **Criar primeiro commit** com estrutura inicial
3. **Seguir roadmap** do Documento de Escopo MVP (Fases 1-5)
4. **Referir este documento** sempre que tiver dúvidas técnicas

**Parabéns pela conclusão deste workflow de arquitetura!** 🚀

---
**Workflow Status:** COMPLETED ✅

**Document Location:** `_bmad-output/planning-artifacts/architecture.md`

**Last Updated:** 2026-02-10
