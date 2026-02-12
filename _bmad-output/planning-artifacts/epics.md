---
stepsCompleted: ['step-01-validate-prerequisites', 'step-02-design-epics', 'step-03-create-stories', 'step-04-final-validation']
inputDocuments:
  - _bmad-output/planning-artifacts/prd.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/ux-design-specification.md
---

# NossaLista - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for NossaLista, decomposing the requirements from the PRD, UX Design, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

**Authentication & User Management (FR1-7):**
- FR1: Usuário pode fazer login usando Google OAuth2
- FR2: Usuário pode fazer login usando email e senha
- FR3: Usuário pode se registrar com email, senha e username único
- FR4: Usuário pode acessar seu próprio perfil
- FR5: Usuário pode atualizar informações do próprio perfil
- FR6: Usuário pode buscar outros usuários por username
- FR7: Sistema mantém sessão do usuário autenticado via token JWT

**List Management (FR8-14):**
- FR8: Usuário pode criar uma nova lista escolhendo tipo e nome
- FR9: Usuário pode visualizar todas as listas que possui ou participa
- FR10: Usuário pode visualizar detalhes de uma lista específica
- FR11: Dono da lista pode editar o nome da lista
- FR12: Dono da lista pode excluir a lista
- FR13: Sistema suporta 4 tipos de lista: Compras, Tarefas, Wishlist, Genérica
- FR14: Tipo de lista define quais campos estão disponíveis nos itens

**Item Management (FR15-22):**
- FR15: Participante da lista pode adicionar itens
- FR16: Participante da lista pode editar itens existentes
- FR17: Participante da lista pode remover itens
- FR18: Participante da lista pode marcar/desmarcar item como concluído
- FR19: Itens do tipo Compras suportam campo de quantidade
- FR20: Itens do tipo Tarefas suportam campo de data de prazo
- FR21: Itens do tipo Wishlist suportam campo de URL/link
- FR22: Sistema registra quem criou cada item

**Sharing & Collaboration (FR23-30):**
- FR23: Dono da lista pode convidar usuários por username
- FR24: Dono da lista pode gerar link de convite com expiração
- FR25: Usuário pode aceitar convite via link de convite
- FR26: Participante da lista pode visualizar outros membros
- FR27: Dono da lista pode remover participantes
- FR28: Participante da lista pode sair da lista
- FR29: Todos os participantes têm permissão para gerenciar itens
- FR30: Sistema distingue dono (OWNER) de participante (MEMBER)

**Real-time Synchronization (FR31-38):**
- FR31: Participantes online recebem atualizações de itens em tempo real
- FR32: Participantes online recebem notificação quando item é adicionado
- FR33: Participantes online recebem notificação quando item é editado
- FR34: Participantes online recebem notificação quando item é removido
- FR35: Participantes online recebem notificação quando item é marcado/desmarcado
- FR36: Participantes online recebem notificação quando novo membro entra
- FR37: Sistema indica quais membros estão online na lista
- FR38: Sistema mantém conexão WebSocket com reconexão automática

**Activity & History (FR39-45):**
- FR39: Participante da lista pode visualizar histórico de atividades
- FR40: Sistema registra quando item é adicionado com autor e timestamp
- FR41: Sistema registra quando item é marcado/desmarcado com autor e timestamp
- FR42: Sistema registra quando item é editado com autor e timestamp
- FR43: Sistema registra quando item é removido com autor e timestamp
- FR44: Sistema registra quando membro entra na lista
- FR45: Sistema registra quando membro sai da lista

**Total: 45 Functional Requirements**

### NonFunctional Requirements

**Performance (NFR-P1 a NFR-P3):**
- NFR-P1: Ações do usuário refletem para outros participantes em menos de 500ms
- NFR-P2: Tela inicial carrega em menos de 3 segundos em conexão 4G
- NFR-P3: WebSocket reconecta automaticamente em caso de queda

**Security (NFR-S1 a NFR-S7):**
- NFR-S1: Todas as conexões utilizam HTTPS via Cloudflare Tunnel
- NFR-S2: Senhas são hasheadas usando bcrypt ou argon2
- NFR-S3: Tokens JWT têm expiração máxima de 7 dias
- NFR-S4: OAuth2 segue fluxo PKCE para segurança adicional
- NFR-S5: CORS está configurado para nossalista.leoferolive.com.br apenas
- NFR-S6: Links de convite expiram em 24 horas
- NFR-S7: Dono da lista é o único que pode excluí-la

**Reliability (NFR-R1 a NFR-R5):**
- NFR-R1: Sistema mantém uptime > 95% mensal
- NFR-R2: Logs de aplicação são mantidos por 30 dias
- NFR-R3: Backup do PostgreSQL é realizado diariamente
- NFR-R4: Pods do K3s restartam automaticamente em caso de crash
- NFR-R5: Deploy via GitHub Actions não causa downtime > 2 minutos

**Accessibility (NFR-A1 a NFR-A5):**
- NFR-A1: Contraste de cores atende WCAG AA (mínimo 4.5:1)
- NFR-A2: Todas as funcionalidades são acessíveis por teclado
- NFR-A3: Inputs e botões possuem labels descritivos
- NFR-A4: Touch targets têm mínimo de 44×44 pixels
- NFR-A5: Interface funciona em navegadores Chrome últimos 2 anos

**Integration (NFR-I1 a NFR-I4):**
- NFR-I1: Google OAuth2 integra corretamente com produção
- NFR-I2: WebSocket (STOMP/SockJS) funciona através do Cloudflare Tunnel
- NFR-I3: GitHub Actions deploya automaticamente para K3s no push para main
- NFR-I4: PostgreSQL persiste dados corretamente em volume do cluster

**Total: 19 Non-Functional Requirements**

### Additional Requirements

**Starter Template (Architecture):**
- Frontend: Vite (create-vite) com React 19 + TypeScript
- Backend: Spring Initializr com Spring Boot 4.0.2 + Java 25
- Adicionar Tailwind CSS manualmente ao frontend

**Repository Structure (Architecture Decision #001):**
- Monorepo com frontend/, backend/, deploy/, e shared/ (opcional)
- CI/CD incremental com path filters (backend/**, frontend/**)
- Deploy coordenado via tags (v* -> deploy ambos)

**Authentication & Security (Architecture):**
- JWT stateless com expiração de 7 dias
- Google OAuth2 com fluxo PKCE
- Spring Security config para REST + WebSocket
- Senhas hasheadas com bcrypt/argon2

**API Design (Architecture Decisions #003, #004):**
- SpringDoc OpenAPI 3 para documentação automática
- RFC 7807 Problem Details para respostas de erro
- OpenAPI JSON exportável para gerar clientes TypeScript

**WebSocket (Architecture Decision #005):**
- STOMP sobre SockJS para real-time sync
- "Event-Type Envelope" para formato de mensagens
- Tópicos: /topic/list/{listId} para broadcasts
- Destinations: /app/list/{listId}/item.*, /app/list/{listId}/item.check

**Frontend State Management (Architecture Decision #006):**
- React Context + hooks (sem Redux no MVP)
- Contextos: AuthContext, ListContext, WebSocketContext

**Data Model (Architecture Decision #002):**
- Colunas nullable em list_items para campos dinâmicos (quantidade, due_date, url)
- Flyway migrations para versionamento de schema
- PostgreSQL em produção, H2 para dev/test

**Logging & Monitoring (Architecture Decisions #008, #009):**
- SLF4J + Logback para logging estruturado
- Logs por 30 dias (NFR-R2)
- Spring Boot Actuator para health checks
- Liveness e readiness probes no K3s

**Testing (Architecture Decision #011):**
- Backend: JUnit 5 + Spring Boot Test + Testcontainers
- Frontend: Vitest + React Testing Library
- Testes E2E com Playwright (opcional)

**UX/UI Requirements:**
- Responsive design: mobile < 640px, tablet 640-1024px, desktop > 1024px
- Touch targets ≥ 44px (NFR-A4)
- Animações sutis: pulse (300ms) quando item aparece
- Toast notifications para feedback de sincronização
- Checkbox customizado com animação "pop"
- Progressive disclosure para features avançadas

**Componentes UI Principais (UX):**
- Layout principal com glassmorphism
- ListCard para mostrar listas na Home
- ListItem com checkbox customizado
- CreateListModal com cards visuais para tipo de lista
- InviteModal com busca por username + link copiável
- ActivityTimeline (📜) expansível
- Toast notifications com tipos: success, error, info

**Interações Especiais (UX):**
- Campo de adição SEMPRE visível no bottom (crítico para UX)
- Swipe gestures futuros (swipe right para marcar, left para deletar)
- Voice input futuro para adição rápida
- Progressive disclosure baseada em uso

### FR Coverage Map

```
FR1: Epic 1 - Google OAuth2 Login
FR2: Epic 1 - Email/Senha Login
FR3: Epic 1 - Registro com username único
FR4: Epic 1 - Acessar próprio perfil
FR5: Epic 1 - Atualizar perfil
FR6: Epic 1 - Buscar usuários por username
FR7: Epic 1 - Sessão JWT stateless

FR8: Epic 2 - Criar lista com tipo e nome
FR9: Epic 2 - Listar todas as listas do usuário
FR10: Epic 2 - Ver detalhes de lista específica
FR11: Epic 2 - Editar nome da lista
FR12: Epic 2 - Excluir lista (dono)
FR13: Epic 2 - 4 tipos de lista pré-definidos
FR14: Epic 2 - Tipo define campos disponíveis

FR15: Epic 3 - Adicionar itens
FR16: Epic 3 - Editar itens
FR17: Epic 3 - Remover itens
FR18: Epic 3 - Marcar/desmarcar concluído
FR19: Epic 3 - Campo quantidade (Compras)
FR20: Epic 3 - Campo prazo (Tarefas)
FR21: Epic 3 - Campo URL (Wishlist)
FR22: Epic 3 - Registrar criador do item

FR23: Epic 4 - Convidar por username
FR24: Epic 4 - Gerar link de convite
FR25: Epic 4 - Aceitar convite via link
FR26: Epic 4 - Ver membros da lista
FR27: Epic 4 - Remover participante (dono)
FR28: Epic 4 - Sair da lista
FR29: Epic 4 - Permissão igualitária para itens
FR30: Epic 4 - Distinção OWNER/MEMBER

FR31: Epic 5 - Atualizações em tempo real
FR32: Epic 5 - Notificação de item adicionado
FR33: Epic 5 - Notificação de item editado
FR34: Epic 5 - Notificação de item removido
FR35: Epic 5 - Notificação de item marcado
FR36: Epic 5 - Notificação de novo membro
FR37: Epic 5 - Indicadores online
FR38: Epic 5 - Reconexão automática

FR39: Epic 6 - Ver histórico de atividades
FR40: Epic 6 - Registrar item adicionado
FR41: Epic 6 - Registrar item marcado
FR42: Epic 6 - Registrar item editado
FR43: Epic 6 - Registrar item removido
FR44: Epic 6 - Registrar membro entrou
FR45: Epic 6 - Registrar membro saiu
```

## Epic List

### Epic 1: Autenticação e Perfis de Usuário

**Valor Entregue:** Usuários podem acessar o sistema de forma segura e gerenciar suas identidades.

**O que os usuários conseguem:**
- Fazer login com Google OAuth2 (primário) ou email/senha (fallback)
- Criar conta com username único
- Buscar outros usuários por username (prepara para convites)
- Ver e editar seu próprio perfil

**FRs cobertos:** FR1, FR2, FR3, FR4, FR5, FR6, FR7 (7 requisitos)

**Status:** ✅ Standalone - Sistema completo de autenticação

---

### Epic 2: Gestão de Listas Pessoais

**Valor Entregue:** Usuários podem criar e gerenciar suas próprias listas.

**O que os usuários conseguem:**
- Criar listas escolhendo tipo (Compras, Tarefas, Wishlist, Genérica)
- Ver todas as suas listas na Home
- Ver detalhes de uma lista específica
- Editar nome da lista
- Excluir lista (apenas dono)

**FRs cobertos:** FR8, FR9, FR10, FR11, FR12, FR13, FR14 (7 requisitos)

**Status:** ✅ Usa Epic 1 - Listas funcionam sem compartilhamento ou itens

---

### Epic 3: Gestão de Itens

**Valor Entregue:** Usuários podem adicionar e gerenciar itens dentro de suas listas.

**O que os usuários conseguem:**
- Adicionar itens às listas
- Editar itens existentes
- Remover itens
- Marcar/desmarcar itens como concluídos
- Campos dinâmicos por tipo: quantidade (Compras), prazo (Tarefas), URL (Wishlist)
- Sistema registra quem criou cada item

**FRs cobertos:** FR15, FR16, FR17, FR18, FR19, FR20, FR21, FR22 (8 requisitos)

**Status:** ✅ Usa Epic 1-2 - Itens funcionam sem compartilhamento ou real-time

---

### Epic 4: Compartilhamento e Colaboração

**Valor Entregue:** Usuários podem convidar outras pessoas e colaborar em listas.

**O que os usuários conseguem:**
- Convidar pessoas por username
- Gerar link de convite com expiração (24h)
- Aceitar convite via link
- Ver quem são os membros da lista
- Dono pode remover participantes
- Participante pode sair da lista
- Todos podem gerenciar itens (permissão igualitária)
- Distinção entre dono (OWNER) e participante (MEMBER)

**FRs cobertos:** FR23, FR24, FR25, FR26, FR27, FR28, FR29, FR30 (8 requisitos)

**Status:** ✅ Usa Epic 1-3 - Compartilhamento funciona sem real-time (polling possível)

---

### Epic 5: Sincronização em Tempo Real

**Valor Entregue:** Múltiplos usuários veem alterações instantaneamente - o "momento Aha!"

**O que os usuários conseguem:**
- Ver alterações de itens em tempo real (< 500ms)
- Receber notificações quando itens são adicionados/editados/removidos
- Ver quando alguém marca/desmarca item
- Ver quando novo membro entra
- Indicadores de quem está online
- Reconexão automática se cair

**FRs cobertos:** FR31, FR32, FR33, FR34, FR35, FR36, FR37, FR38 (8 requisitos)

**Status:** ✅ Usa Epic 1-4 - Sistema completo de colaboração real-time

---

### Epic 6: Histórico e Atividades

**Valor Entregue:** Transparência total - usuários veem "quem fez o quê e quando".

**O que os usuários conseguem:**
- Ver timeline de atividades da lista
- Registro completo: quem adicionou/marcou/editou/removeu itens
- Registro de entrada/saída de membros
- Todas as ações com autor e timestamp

**FRs cobertos:** FR39, FR40, FR41, FR42, FR43, FR44, FR45 (7 requisitos)

**Status:** ✅ Usa Epic 1-5 - Sistema completo de auditoria

---

## Epic 1: Autenticação e Perfis de Usuário

Usuários podem acessar o sistema de forma segura e gerenciar suas identidades.

### Story 1.1: Setup do Projeto e Configuração de Segurança

As a desenvolvedor,
I want configurar a fundação técnica do projeto (monorepo, backend, frontend, database, security),
So that tenhamos uma base sólida para implementar as funcionalidades de autenticação.

**Acceptance Criteria:**

**Given** um projeto vazio do NossaLista
**When** executo o setup inicial
**Then** devo ter uma estrutura de monorepo funcionando

**And** a estrutura de pastas deve seguir o padrão aprovado:

```
nossalista/
├── backend/          # Spring Boot 4 + Java 25
├── frontend/         # React 19 + TypeScript + Vite
├── deploy/           # Manifests K3s (vazio por enquanto)
└── .github/
    └── workflows/    # CI/CD (vazio por enquanto)
```

**Given** o backend criado via Spring Initializr
**When** configuro as dependências
**Then** o pom.xml deve incluir: Spring Boot 4.0.2, Spring Security, Spring Data JPA, PostgreSQL driver, H2, Validation, Flyway, JWT library (jjwt)

**Given** o backend configurado
**When** configuro o database
**Then** application-dev.yml deve usar H2 em memória
**And** application-prod.yml deve usar PostgreSQL
**And** Flyway deve estar habilitado para migrations
**And** a url de conexão deve ser configurável via environment variable

**Given** o backend configurado
**When** configuro Spring Security
**Then** SecurityConfig deve: Desabilitar CSRF para APIs stateless, Configurar CORS para nossalista.leoferolive.com.br, Habilitar JWT authentication filter, Permitir endpoints públicos (/api/auth/**), Exigir autenticação para demais endpoints (/api/**)

**Given** o frontend criado via Vite
**When** configuro o projeto
**Then** package.json deve incluir: React 19, TypeScript 5+, Tailwind CSS 3+, Axios, React Router

**Given** o projeto configurado
**When** verifico os ambientes
**Then** deve ser possível rodar `npm run dev` (frontend na porta 5173)
**And** deve ser possível rodar `mvn spring-boot:run` (backend na porta 8080)
**And** ambos devem iniciar sem erros

---

### Story 1.2: Registro com Email/Senha

As a novo usuário,
I want me registrar usando email, senha e username,
So that possa acessar o NossaLista sem usar Google.

**Acceptance Criteria:**

**Given** o endpoint POST /api/auth/register está disponível
**When** envio request com email válido, senha e username único
**Then** sistema deve criar novo usuário
**And** senha deve ser hasheada com bcrypt
**And** usuário deve ter role USER
**And** response deve ser 201 Created com dados do usuário (sem senha)

**Given** o endpoint de registro
**When** envio email já cadastrado
**Then** response deve ser 409 Conflict
**And** body deve seguir RFC 7807 Problem Details
**And** mensagem deve indicar que email já existe

**Given** o endpoint de registro
**When** envio username já cadastrado
**Then** response deve ser 409 Conflict
**And** body deve indicar que username já existe

**Given** o endpoint de registro
**When** envio senha com menos de 6 caracteres
**Then** response deve ser 400 Bad Request
**And** body deve listar erros de validação

**Given** o endpoint de registro
**When** envio email inválido
**Then** response deve ser 400 Bad Request
**And** body deve indicar email inválido

**Given** migração Flyway V1__create_users_table.sql
**When** executada
**Then** tabela users deve ter colunas: id (UUID), username (unique), email (unique), password (nullable), name (nullable), avatar_url (nullable), auth_provider, created_at, updated_at

---

### Story 1.3: Login com Email/Senha

As a usuário cadastrado,
I want fazer login com meu email e senha,
So that possa acessar minhas listas.

**Acceptance Criteria:**

**Given** o endpoint POST /api/auth/login está disponível
**When** envio credenciais válidas (email + senha)
**Then** response deve ser 200 OK
**And** body deve conter JWT token
**And** token deve ter expiração de 7 dias
**And** body deve conter dados do usuário (id, username, email, name, avatar_url)

**Given** o endpoint de login
**When** envio email inexistente
**Then** response deve ser 401 Unauthorized
**And** body deve seguir RFC 7807
**And** mensagem não deve revelar se email existe (segurança)

**Given** o endpoint de login
**When** envio senha incorreta
**Then** response deve ser 401 Unauthorized
**And** mensagem deve indicar credenciais inválidas

**Given** JWT token gerado
**When** decodificado
**Then** deve conter: user_id, email, username, exp (7 dias)
**And** deve ser assinado com chave secreta configurável

**Given** JWT token válido
**When** incluído no header Authorization: Bearer {token}
**Then** requests autenticados devem ser aceitos
**And** sistema deve extrair user_id do token

---

### Story 1.4: Integração Google OAuth2

As a novo usuário,
I want fazer login usando minha conta Google,
So that possa acessar o NossaLista sem criar outra senha.

**Acceptance Criteria:**

**Given** o endpoint GET /api/auth/google está disponível
**When** acesso via browser
**Then** deve redirecionar para Google OAuth2 consent screen
**And** scope deve incluir email e profile

**Given** usuário autorizou no Google
**When** Google redireciona para /api/auth/google/callback
**Then** sistema deve trocar code por tokens
**And** deve extrair email, name, picture do Google
**And** deve criar novo usuário se não existir
**And** deve atualizar usuário existente se já existir
**And** deve gerar JWT token
**And** deve redirecionar para frontend com token

**Given** usuário criado via Google OAuth2
**When** verificado no database
**Then** auth_provider deve ser 'GOOGLE'
**And** password deve ser NULL
**And** email deve ser único
**And** avatar_url deve conter URL do Google
**And** username deve ser gerado automaticamente (email prefix + número se necessário)

**Given** Google OAuth2 configurado
**When** verifico application.yml
**Then** deve conter: spring.security.oauth2.client.registration.google.client-id, client-secret, redirect-uri

**Given** PKCE habilitado para OAuth2
**When** fluxo é executado
**Then** code verifier e challenge devem ser gerados
**And** state parameter deve ser validado (segurança CSRF)

---

### Story 1.5: Perfil e Busca de Usuários

As a usuário autenticado,
I want ver meu perfil e buscar outros usuários por username,
So that possa gerenciar minhas informações e convidar pessoas para minhas listas.

**Acceptance Criteria:**

**Given** o endpoint GET /api/users/me está disponível
**When** faço request com JWT válido
**Then** response deve ser 200 OK
**And** body deve conter: id, username, email, name, avatar_url, auth_provider, created_at
**And** NÃO deve conter password

**Given** o endpoint GET /api/users/me
**When** faço request sem autenticação
**Then** response deve ser 401 Unauthorized

**Given** o endpoint PATCH /api/users/me está disponível
**When** faço request com campos para atualizar (name ou avatar_url)
**Then** response deve ser 200 OK com dados atualizados
**And** updated_at deve ser atualizado automaticamente
**And** username e email NÃO podem ser alterados

**Given** o endpoint PATCH /api/users/me
**When** tento alterar username ou email
**Then** response deve ser 400 Bad Request
**And** body deve indicar que esses campos são somente leitura

**Given** o endpoint GET /api/users/search?q={query} está disponível
**When** faço request com termo de busca
**Then** response deve ser 200 OK
**And** body deve conter array de usuarios (username, name, avatar_url)
**And** busca deve ser case-insensitive
**And** busca deve encontrar parciais (ex: "leo" encontra "leonardo")
**And** NÃO deve retornar email ou password

**Given** o endpoint de busca
**When** faço request sem query parameter
**Then** response deve ser 400 Bad Request
**And** body deve indicar que 'q' é obrigatório

**Given** o endpoint de busca
**When** faço request com termo que não existe
**Then** response deve ser 200 OK
**And** array deve estar vazio

**Given** JWT token válido no header
**When** extraio user_id do token
**Then** ApplicationUserDetailsService deve carregar usuário do database
**And** deve retornar UserDetails para Spring Security
**And** UsernamePasswordAuthenticationToken deve ser criado
**And** SecurityContext deve ser populado

---

## Epic 2: Gestão de Listas Pessoais

Usuários podem criar e gerenciar suas próprias listas.

### Story 2.1: Modelagem de Dados de Listas e Tipos

As a desenvolvedor,
I want criar a estrutura de dados para listas e tipos,
So that o sistema possa armazenar e gerenciar listas pessoais.

**Acceptance Criteria:**

**Given** migração Flyway V2__create_list_types_and_lists.sql
**When** executada
**Then** tabela list_types deve ter colunas: id (SERIAL), name (VARCHAR), slug (VARCHAR, unique), created_at
**And** deve ter 4 tipos pré-inseridos: (1, "Compras", "compras"), (2, "Tarefas", "tarefas"), (3, "Wishlist", "wishlist"), (4, "Genérica", "generica")

**Given** migração V2 executada
**When** tabela lists é criada
**Then** tabela lists deve ter colunas: id (UUID), name, type_id (FK), owner_id (FK), invite_code, created_at, updated_at
**And** deve ter índices: idx_lists_owner_id, idx_lists_invite_code
**And** deve ter constraints: fk_lists_type, fk_lists_owner

**Given** entidade List no backend
**When** mapeada via JPA
**Then** deve ter @Entity com campos: id, name, type, owner, inviteCode, createdAt, updatedAt
**And** @PreUpdate para atualizar updated_at automaticamente

---

### Story 2.2: Criar Nova Lista

As a usuário autenticado,
I want criar uma nova lista escolhendo tipo e nome,
So that possa começar a organizar minhas tarefas ou compras.

**Acceptance Criteria:**

**Given** o endpoint POST /api/lists está disponível
**When** faço request com JWT válido e body { "name": "Mercado Semanal", "typeId": 1 }
**Then** response deve ser 201 Created com lista criada
**And** owner_id deve ser o usuário autenticado
**And** invite_code deve ser gerado automaticamente (string alfanumérica 12 chars)

**Given** o endpoint de criação
**When** nome está vazio ou < 3 caracteres ou typeId inválido
**Then** response deve ser 400 Bad Request com erro de validação

**Given** CreateListModal no frontend
**When** aberto
**Then** deve mostrar: campo "Nome da lista", 4 cards visuais (🛒 Compras, ✅ Tarefas, 🎁 Wishlist, 📝 Genérica)
**And** botão "Criar Lista" desabilitado até nome preenchido e tipo selecionado
**And** Enter no campo deve criar lista (se tipo selecionado)

**Given** lista criada com sucesso
**When** response é recebida
**Then** Toast "Lista criada" deve aparecer (success, 300ms)
**And** modal deve fechar
**And** usuário deve ser redirecionado para Home

---

### Story 2.3: Listar Todas as Listas do Usuário

As a usuário autenticado,
I want ver todas as minhas listas na tela inicial,
So that possa acessar rapidamente o que preciso.

**Acceptance Criteria:**

**Given** o endpoint GET /api/lists está disponível
**When** faço request com JWT válido
**Then** response deve ser 200 OK com array de listas
**And** cada lista deve ter: id, name, type, owner_id, isOwner (boolean)
**And** deve vir ordenadas por updated_at DESC

**Given** Home screen no frontend
**When** carregada
**Then** deve mostrar header "Minhas Listas", botão "+ Nova Lista", grid de ListCards (responsive: 1/2/3 colunas)
**And** ListCard deve ter: emoji do tipo, nome, contagem de itens, indicador "minha"/"compartilhada"
**And** touch target ≥ 44px (NFR-A4)

**Given** usuário sem listas
**When** Home screen é carregada
**Then** deve mostrar estado vazio com mensagem e botão "+ Criar Primeira Lista"

---

### Story 2.4: Ver Detalhes de uma Lista

As a usuário autenticado,
I want ver os detalhes de uma lista específica,
So that possa ver suas informações e começar a adicionar itens.

**Acceptance Criteria:**

**Given** o endpoint GET /api/lists/{id} está disponível
**When** faço request com JWT válido e lista existe
**Then** response deve ser 200 OK com: id, name, type, owner, itemsCount: 0, membersCount: 1

**Given** endpoint de detalhe
**When** lista não existe
**Then** response deve ser 404 Not Found com RFC 7807

**Given** ListView screen no frontend
**When** carregada
**Then** header deve mostrar: nome da lista, botão voltar
**And** info da lista: tipo (emoji + nome), dono (avatar + username)
**And** seção "Itens" (vazia), botão "Adicionar Item" (disabled)
**And** estado vazio: "Esta lista ainda não tem itens. Adicione o primeiro!"

---

### Story 2.5: Editar Nome da Lista

As a dono de uma lista,
I want editar o nome da minha lista,
So that possa corrigir erros ou atualizar conforme necessidade.

**Acceptance Criteria:**

**Given** o endpoint PATCH /api/lists/{id} está disponível
**When** faço request com JWT válido, sou dono, body { "name": "Novo Nome" }
**Then** response deve ser 200 OK com lista atualizada
**And** updated_at deve ser maior que valor anterior

**Given** endpoint de edição
**When** não sou dono ou nome inválido ou tento alterar typeId
**Then** response deve ser 403 Forbidden, 400 Bad Request, 400 Bad Request respectivamente

**Given** ListView com lista carregada
**When** toco botão editar (lápis no header)
**Then** modal deve abrir com campo preenchido, botões "Cancelar" e "Salvar"
**And** ao salvar: Toast "Lista atualizada", modal fecha, header atualiza

---

### Story 2.6: Excluir Lista

As a dono de uma lista,
I want excluir minha lista,
So that possa remover listas que não preciso mais.

**Acceptance Criteria:**

**Given** o endpoint DELETE /api/lists/{id} está disponível
**When** faço request com JWT válido, sou dono
**Then** response deve ser 204 No Content
**And** lista deve ser removida com cascade (itens, membros, activity logs)

**Given** endpoint de exclusão
**When** não sou dono (NFR-S7)
**Then** response deve ser 403 Forbidden com "Apenas o dono pode excluir esta lista"

**Given** ListView com lista carregada
**When** toco opções (três pontos) → "Excluir Lista"
**Then** modal confirmação: "Excluir Lista? Tem certeza? Esta ação não pode ser desfeita."
**And** ao confirmar: DELETE request, Toast "Lista excluída", redireciona para Home

---

## Epic 3: Gestão de Itens

Usuários podem adicionar e gerenciar itens dentro de suas listas.

### Story 3.1: Modelagem de Dados de Itens

As a desenvolvedor,
I want criar a estrutura de dados para itens de lista,
So that o sistema possa armazenar itens com campos dinâmicos por tipo.

**Acceptance Criteria:**

**Given** migração Flyway V3__create_list_items.sql
**When** executada
**Then** tabela list_items deve ter: id (UUID), list_id (FK), name, checked (BOOLEAN, default false), quantity (nullable), due_date (nullable), url (nullable), position (INTEGER), created_by (FK), created_at, updated_at
**And** índices: idx_list_items_list_id, idx_list_items_position
**And** constraints: fk_list_items_list (CASCADE), fk_list_items_creator

**Given** entidade ListItem no backend
**When** mapeada via JPA
**Then** deve ter campos: id, list, name, checked, quantity, dueDate, url, position, createdBy, createdAt, updatedAt
**And** @PreUpdate para atualizar updated_at

**Given** lista do tipo Compras/Tarefas/Wishlist/Genérica
**When** item é criado
**Then** campos nullable correspondentes devem ser aplicados (quantity/due_date/url ou NULL)

---

### Story 3.2: Adicionar Item à Lista

As a participante de uma lista,
I want adicionar itens à lista,
So that possa organizar o que preciso comprar/fazer.

**Acceptance Criteria:**

**Given** o endpoint POST /api/lists/{id}/items está disponível
**When** faço request com JWT válido, body { "name": "Arroz", "quantity": 2 }
**Then** response deve ser 201 Created com item criado
**And** position deve ser atribuído automaticamente (maior + 1)
**And** created_by deve ser o usuário autenticado

**Given** ListView no frontend
**When** carregada
**Then** campo de adição deve estar SEMPRE visível no bottom (CRÍTICO UX)
**And** campo deve ter focus automático, botão "+" ao lado
**And** Enter adiciona item

**Given** campo de adição preenchido
**When** adiciono "Arroz"
**Then** Toast "Sincronizando..." aparece, item aparece com pulse (300ms), Toast "Sincronizado" aparece
**And** campo limpa e mantém focus

**Given** lista do tipo Compras
**When** campo de adição visível
**Then** campo quantidade visível ao lado (padrão 1)

---

### Story 3.3: Listar Itens de uma Lista

As a participante de uma lista,
I want ver todos os itens da lista,
So that possa ver o que precisa ser feito/comprado.

**Acceptance Criteria:**

**Given** o endpoint GET /api/lists/{id}/items está disponível
**When** faço request com JWT válido
**Then** response deve ser 200 OK com array ordenado por position ASC
**And** cada item tem: id, name, checked, quantity, due_date, url, position, created_by, created_at, updated_at

**Given** ListView no frontend
**When** itens carregados
**Then** cada item renderizado como ListItem com: checkbox customizado, nome, campos extras, criador (avatar + username)

**Given** ListItem com checked = true/false
**When** renderizado
**Then** checkbox marcado/desmarcado, texto com/sem line-through e opacidade

**Given** ListItem do tipo Compras/Tarefas/Wishlist
**When** renderizado
**Then** mostra campos extras: quantity/due_date/url conforme aplicável

---

### Story 3.4: Marcar/Desmarcar Item como Concluído

As a participante de uma lista,
I want marcar itens como concluídos,
So that possa controlar o que já fiz/comprei.

**Acceptance Criteria:**

**Given** o endpoint PATCH /api/lists/{id}/items/{itemId}/check está disponível
**When** faço request com JWT válido
**Then** response deve ser 200 OK com checked invertido (toggle)

**Given** ListItem component
**When** toco no checkbox
**Then** request PATCH enviado, animação "pop" (300ms), checkbox muda, Toast "Sincronizado" aparece
**And** optimistic UI: estado visual muda imediatamente

**Given** checkbox animação "pop"
**When** executada
**Then** keyframes: scale(0.8) → scale(1.2) → scale(1.0), 300ms, cubic-bezier

**Given** ListItem
**When** toco no texto (não checkbox)
**Then** modal de edição abre (Story 3.5), checkbox NÃO alterado

---

### Story 3.5: Editar Item

As a participante de uma lista,
I want editar itens existentes,
So that possa corrigir erros ou atualizar informações.

**Acceptance Criteria:**

**Given** o endpoint PATCH /api/lists/{id}/items/{itemId} está disponível
**When** faço request com JWT válido, body com campos para atualizar
**Then** response deve ser 200 OK com item atualizado

**Given** lista do tipo Compras/Tarefas/Wishlist
**When** edito item
**Then** posso alterar campos específicos do tipo

**Given** ListItem na ListView
**When** toco no texto do item
**Then** EditItemModal abre com campos preenchidos conforme tipo de lista

**Given** EditItemModal aberto
**When** altero campos e salvo
**Then** request PATCH enviado, Toast "Sincronizando...", modal fecha, item atualiza, Toast "Sincronizado"

---

### Story 3.6: Remover Item

As a participante de uma lista,
I want remover itens da lista,
So that possa deletar o que não preciso mais.

**Acceptance Criteria:**

**Given** o endpoint DELETE /api/lists/{id}/items/{itemId} está disponível
**When** faço request com JWT válido
**Then** response deve ser 204 No Content, item removido do database

**Given** ListItem na ListView
**When** faço long-press (1 segundo)
**Then** menu opções aparece com "Editar", "Remover"

**Given** menu opções aberto
**When** toco "Remover"
**Then** modal confirmação: "Remover item? Tem certeza que deseja remover '{nome}'?"
**And** ao confirmar: DELETE request, fade-out (200ms), item desaparece, Toast "Item removido", outros itens reordenam

**Given** lista com item removido
**When** positions verificadas
**Then** positions dos itens restantes reordenadas automaticamente (0, 1, 2...)

---

## Epic 4: Compartilhamento e Colaboração

Usuários podem convidar outras pessoas e colaborar em listas.

### Story 4.1: Modelagem de Dados de Membros e Convites

As a desenvolvedor,
I want criar a estrutura de dados para membros e convites,
So that o sistema possa gerenciar quem tem acesso a cada lista.

**Acceptance Criteria:**

**Given** migração Flyway V4__create_list_members.sql
**When** executada
**Then** tabela list_members deve ter: id (UUID), list_id (FK), user_id (FK), role ('OWNER'/'MEMBER'), joined_at
**And** índice único: uk_list_members (list_id, user_id), índices: idx_list_members_list, idx_list_members_user
**And** constraints: fk_list_members_list (CASCADE), fk_list_members_user (CASCADE)

**Given** tabela lists já existe
**When** verifico coluna invite_code
**Then** invite_code (VARCHAR(20), unique, nullable) existe
**And** coluna invite_expires_at (TIMESTAMP, nullable) adicionada para expiração de link (24h)

**Given** entidade ListMember no backend
**When** mapeada via JPA
**Then** campos: id, list, user, role (enum: OWNER, MEMBER), joinedAt
**And** constraint único em (list, user)

**Given** enum ListMemberRole
**When** definida
**Then** valores: OWNER (permissões completas), MEMBER (gerenciar itens, sair)

**Given** lista recém-criada
**When** dono cria a lista
**Then** registro em list_members criado automaticamente com role = OWNER

---

### Story 4.2: Gerar Link de Convite

As a dono de uma lista,
I want gerar um link de convite para compartilhar minha lista,
So that outras pessoas possam acessá-la.

**Acceptance Criteria:**

**Given** endpoint POST /api/lists/{id}/invite-link disponível
**When** faço request com JWT válido, sou dono
**Then** response 200 OK com: inviteCode, inviteLink (nossalista.../join/{code}), expiresAt (24h)
**And** se invite_code válido existe, reutiliza; se expirou, gera novo

**Given** InviteModal no frontend (dono)
**When** aberto
**Then** seção "Convidar por Link" com botão "Gerar Link"/"Copiar Link"
**And** quando gerado: link completo, botão "Copiar", ícone share, tempo expiração "24 horas"

**Given** botão "Copiar Link"
**When** tocado
**Then** link copiado para clipboard, Toast "Link copiado!", botão muda para "Copiado!"

**Given** link copiado
**When** colo no WhatsApp
**Then** preview: "Convite para lista: {nome}", URL completa

---

### Story 4.3: Aceitar Convite via Link (Read-Only Mode)

As a pessoa sem conta,
Iwant visualizar uma lista via link de convite sem criar conta,
So that possa decidir se quero participar antes de me comprometer.

**Acceptance Criteria:**

**Given** endpoint GET /api/lists/join/{inviteCode} disponível
**When** request SEM autenticação, invite_code válido e não expirou
**Then** response 200 OK com: id, name, type, owner, items (read-only), inviteCode, expiresAt, modo = "READ_ONLY"

**Given** endpoint join
**When** invite_code não existe ou expirou
**Then** 404 Not Found ou 410 Gone com mensagem apropriada

**Given** JoinList screen (read-only)
**When** carregada via link
**Then** header: "Modo Leitura - Entre para Editar", lista visível, itens SEM interação (checkbox disabled, sem campo adição, botões ocultos)
**And** aviso: "Você está visualizando em modo leitura. Entre para colaborar!"
**And** botões "Entrar com Google" e "Entrar com Email" proeminentes

**Given** link com 5 min restantes
**When** JoinList carregada
**Then** aviso: "Este link expira em breve! Entre agora ou peça um novo link."

---

### Story 4.4: Entrar na Lista (Autenticado)

As a usuário autenticado,
Iwant entrar em uma lista via link de convite,
So that possa colaborar nela.

**Acceptance Criteria:**

**Given** endpoint POST /api/lists/join/{inviteCode} disponível
**When** request JWT válido, invite_code válido, não sou membro
**Then** 201 Created, registro list_members criado (role = 'MEMBER'), Toast "Bem-vindo à lista {nome}!"

**Given** endpoint join
**When** já sou membro
**Then** 200 OK com "Você já é membro", lista completa

**Given** endpoint join
**When** sou dono
**Then** 200 OK com "Você é o dono", lista completa

**Given** usuário JoinList (read-only)
**When** clica "Entrar com Google"
**Then** OAuth2 inicia, após auth volta para JoinList, POST join chamado automaticamente, se sucesso redireciona para ListView (full access)

**Given** POST join com sucesso
**Then** transição: modo leitura → completo, botão "Entrar" some, campo adição aparece, checkboxes habilitados, Toast "Bem-vindo!"

---

### Story 4.5: Convidar por Username

As a dono de uma lista,
Iwant convidar usuários por username,
So that possa adicionar pessoas diretamente sem depender de link.

**Acceptance Criteria:**

**Given** endpoint POST /api/lists/{id}/invite disponível
**When** request JWT válido, sou dono, body { "username": "pedro" }, usuário existe
**Then** 201 Created, list_members criado (role = 'MEMBER'), Toast "{username} adicionado!"

**Given** endpoint invite
**When** username não existe
**Then** 404 Not Found "Usuário não encontrado"

**Given** endpoint invite
**When** usuário já é membro
**Then** 409 Conflict "Usuário já é membro"

**Given** InviteModal (dono)
**When** aberto
**Then** seção "Convidar por Username" com campo busca "Buscar usuário", botão "Convidar", autocomplete (GET /api/users/search)

**Given** campo busca, digito "leo"
**Then** dropdown com resultados: avatar, username, name

**Given** resultado selecionado, toco "Convidar"
**Then** POST invite enviado, Toast "{username} convidado!", modal fecha, membro aparece na lista

---

### Story 4.6: Ver Membros e Sair da Lista

As a participante de uma lista,
Iwant ver quem são os membros e poder sair da lista,
So that eu tenha autonomia sobre minha participação.

**Acceptance Criteria:**

**Given** endpoint GET /api/lists/{id}/members disponível
**When** request JWT válido, sou membro
**Then** 200 OK com array de membros: user { id, username, name, avatar_url }, role, joined_at
**And** ordenado: OWNER primeiro, depois MEMBERs por joined_at ASC

**Given** endpoint POST /api/lists/{id}/leave disponível
**When** request JWT válido, sou MEMBER
**Then** 204 No Content, registro list_members removido, não acesso mais a lista

**Given** endpoint leave
**When** sou OWNER
**Then** 403 Forbidden "O dono não pode sair. Transfira ou exclua a lista."

**Given** ListView
**When** carregada
**Then** header tem botão "Membros" com contador "👥 3", ao tocar abre painel/modal com lista

**Given** painel membros aberto
**When** renderizado
**Then** cada membro: avatar, username, name, badge "Dono"/"Membro", dono primeiro

**Given** painel membros, sou MEMBER
**When** visível
**Then** botão "Sair da Lista" visível (cor alerta)

**Given** painel membros, sou OWNER
**When** visível
**Then** botão "Sair" NÃO aparece, aviso "Você é o dono"

**Given** toco "Sair da Lista"
**Then** modal confirmação: "Sair da lista? Você perderá acesso.", botões "Cancelar"/"Sair"
**And** ao confirmar: POST leave, Toast "Você saiu", redireciona para Home, lista some da Home

---

### Story 4.7: Remover Participante

As a dono de uma lista,
Iwant remover participantes da minha lista,
So that eu possa controlar quem tem acesso.

**Acceptance Criteria:**

**Given** endpoint DELETE /api/lists/{id}/members/{userId} disponível
**When** request JWT válido, sou OWNER, userId é MEMBER
**Then** 204 No Content, registro list_members removido, membro não acessa mais

**Given** endpoint remove
**When** tento remover dono
**Then** 403 Forbidden "O dono não pode ser removido"

**Given** endpoint remove
**When** não sou OWNER
**Then** 403 Forbidden "Apenas o dono pode remover"

**Given** painel membros aberto, sou OWNER
**When** visualizo
**Then** cada MEMBER tem botão "Remover" (lixeira), OWNER não tem

**Given** toco "Remover" ao lado de membro
**Then** modal confirmação: "Remover {username}? Ação não pode ser desfeita."
**And** ao confirmar: DELETE remove, Toast "{username} removido", modal fecha, membro some da lista

**Given** ListaService.removeMember
**When** chamado
**Then** verifica requestUser é OWNER, verifica targetUser não é OWNER, remove registro, registra activity log (Epic 6)

---

## Epic 5: Sincronização em Tempo Real

Múltiplos usuários veem alterações instantaneamente - o momento "Aha!" do NossaLista.

### Story 5.1: Setup e Configuração de WebSocket

As a desenvolvedor,
Iwant configurar WebSocket com STOMP sobre SockJS,
So that o sistema possa suportar sincronização em tempo real.

**Acceptance Criteria:**

**Given** dependência WebSocket no pom.xml
**Then** spring-boot-starter-websocket e spring-messaging incluídos

**Given** WebSocketConfig configurado
**Then** @EnableWebSocketMessageBroker anotado
**And** configureMessageBroker: STOMP simple broker (/topic), app prefix (/app), STOMP endpoint /ws com SockJS, CORS configurado

**Given** WebSocketAuthInterceptor
**Then** extrai JWT do header/query, valida, autentica no SecurityContext, rejeita sem token (401)

**Given** ChannelInterceptor em subscribe /topic/list/{listId}
**Then** verifica permissão (dono ou membro), SecurityException se não autorizado, desconecta cliente

**Given** frontend WebSocketContext
**Then** @stomp/stompjs e sockjs-client instalados, hook useWebSocket ou WebSocketContext existe
**And** cria conexão STOMP (SockJS /ws), headers com Authorization: Bearer {token}, gerencia estado: CONNECTED/CONNECTING/DISCONNECTED

---

### Story 5.2: Formato de Mensagem e Broadcast de Itens

As a desenvolvedor,
Iwant implementar formato "Event-Type Envelope" e broadcast de itens,
So that clientes recebam atualizações de itens em tempo real.

**Acceptance Criteria:**

**Given** formato Event-Type Envelope
**Then** JSON: { type, payload, userId, username, timestamp }
**And** types: ITEM_ADDED, ITEM_UPDATED, ITEM_REMOVED, ITEM_CHECKED

**Given** ListService.addItem/updateItem/removeItem
**When** executado com sucesso
**Then** broadcast ITEM_*/ITEM_CHECKED para /topic/list/{listId}, payload com item, userId/username do criador

**Given** WebSocketContext.subscribeToList(listId)
**Then** subscribe /topic/list/{listId}, callback onMessage registrado, parse JSON

**Given** mensagem ITEM_ADDED recebida
**Then** payload.item extraído, adicionado ao estado, pulse animation (300ms), Toast "{username} adicionou {itemName}"

**Given** mensagem ITEM_UPDATED recebida
**Then** item existente atualizado (match id), Toast "{username} editou {itemName}"

**Given** mensagem ITEM_REMOVED recebida
**Then** item removido do estado, fade-out (200ms), outros reordenam, Toast "{username} removeu {itemName}"

**Given** broadcasting
**Then** TODOS os clientes conectados recebem, latência < 500ms (NFR-P1)

---

### Story 5.3: Sincronização de Checkbox (Item Check)

As a usuário,
Iwant ver quando alguém marca/desmarca item em tempo real,
So that eu saiba o que já foi feito/comprado.

**Acceptance Criteria:**

**Given** ListService.toggleItemCheck
**When** executado
**Then** broadcast ITEM_CHECKED para /topic/list/{listId}, payload com item atualizado

**Given** mensagem ITEM_CHECKED recebida
**Then** item checked atualizado, animação "pop" no checkbox (300ms), se true: line-through + opacidade 50%, se false: normal

**Given** ITEM_CHECKED recebida de outro usuário
**Then** highlight no item (pulse amarelo 200ms), Toast "{username} marcou/desmarcou {itemName}"

**Given** ITEM_CHECKED recebida de mim mesmo
**Then** Toast "Sincronizado" aparece (confirmação eco)

**Given** latência medida
**Then** tempo entre clique A e atualização B < 500ms (NFR-P1)

**Given** múltiplos usuários marcam simultaneamente
**Then** last-write-wins, estado final consistente

---

### Story 5.4: Indicadores Online e Membros

As a usuário,
Iwant ver quem está online na lista em tempo real,
So that eu saiba com quem estou colaborando.

**Acceptance Criteria:**

**Given** WebSocketSession subscribe em /topic/list/{listId}
**Then** sessão registrada no listId, mapa sessões ativas por lista mantido

**Given** primeiro usuário subscribe
**Then** evento MEMBER_ONLINE enviado para /topic/list/{listId}, payload: { userId, username, name, avatar_url }

**Given** cliente desconecta
**Then** evento MEMBER_OFFLINE enviado, payload: { userId, username }

**Given** MEMBER_ONLINE recebido
**Then** avatar aparece em seção "Online", bolinha verde (●), nome exibido, contador "X online" atualizado

**Given** MEMBER_OFFLINE recebido
**Then** avatar removido da seção "Online", bolinha some, contador decrementado

**Given** ListView com membros online
**Then** seção "Online agora: 3" visível, avatares horizontalmente com overlap, bolinhas verdes

**Given** ListView sem outros online
**Then** "Apenas você online agora" ou seção não aparece, contador "1 online"

**Given** heartbeat mechanism
**Then** cliente envia HEARTBEAT a cada 30s, servidor timeout em 60s (2 heartbeats perdidos), sessão encerrada

---

### Story 5.5: Reconexão Automática

As a usuário,
Iwant que o sistema reconecte automaticamente se a conexão cair,
So that eu não precise recarregar ou fazer login novamente.

**Acceptance Criteria:**

**Given** conexão WebSocket perdida
**Then** onError acionado, estado RECONNECTING, toast "Sem conexão. Reconectando...", tentativa reconexão iniciada

**Given** tentativa reconexão
**Then** backoff exponencial: imediato, 2s, 5s, 10s (máximo), tentativas infinitas

**Given** reconexão bem-sucedida
**Then** toast "Conectado novamente!", estado CONNECTED, subscrições re-estabelecidas, dados recarregados (GET /api/lists/{id}/items)

**Given** reconexão após falha
**Then** re-subscrible tópicos anteriores, recarrega estado, mescla para evitar duplicatas

**Given** Cloudflare Tunnel desconexão (5 min inatividade)
**Then** reconexão automática detecta e reconecta, usuário não percebe (além toast)

**Given** perda de rede móvel
**Then** onError detecta, ao voltar reconecta, toast "Conexão restaurada"

**Given** reconexão em progresso, usuário offline
**Then** operações enfileiradas, ao reconectar enviadas em ordem, falhas notificadas

**Given** WebSocketContext implementado
**Then** métodos: connect(), disconnect(), reconnect(), isConnecting(), isConnected(), gerencia fila mensagens, timer backoff

**Given** reconexão servidor fora
**Then** tentativas continuam indefinidamente, toast não spam, indicador visual: 🔴 Offline → 🟡 Reconectando → 🟢 Online

**Given** heartbeat cliente
**Then** PING a cada 30s, se PONG não em 60s conexão morta, onClose acionado, reconexão inicia

---

### Story 5.6: Performance e Otimizações

As a desenvolvedor,
Iwant garantir performance e otimizações do WebSocket,
So that latência seja < 500ms (NFR-P1) e o sistema escale.

**Acceptance Criteria:**

**Given** latência medida mesma rede
**Then** média < 100ms, p95 < 300ms, p99 < 500ms (NFR-P1)

**Given** latência redes diferentes (4G)
**Then** média < 300ms, p95 < 500ms (NFR-P1), p99 < 1s

**Given** teste 10 usuários, 10 itens em 10s
**Then** todas mensagens entregues, nenhuma perdida, ordem preservada, latência média < 500ms

**Given** usuário edita rapidamente (5x em 1s)
**Then** throttling aplicado, apenas último broadcasted ou batching a cada 500ms

**Given** SimpMessagingTemplate.convertAndSend
**Then** Jackson JSON serializado, payload minimizado, timestamp ISO 8601

**Given** frontend handler mensagem
**Then** JSON.parse nativo, React otimizado (useCallback/useMemo), render seletivo

**Given** múltiplas listas subscribe
**Then** única conexão reusada, múltiplos subscribes mesma conexão

**Given** /actuator/health
**Then** status WebSocket incluído, métricas: conexões ativas, msg/s, latência média

**Given** memória servidor monitorada
**Then** sessões WebSocket não crescem indefinidamente, expiradas limpas (GC), heartbeat limpa órfãs

**Given** stress 100 usuários
**Then** servidor suporta sem crash, latência < 1s, CPU/memória estáveis

**Given** STOMP frontend configurado
**Then** options: heartbeat in/out 10000ms, reconnectDelay automático backoff, debug false produção

---

## Epic 6: Histórico e Atividades

Transparência total - usuários veem "quem fez o quê e quando".

### Story 6.1: Modelagem de Dados de Activity Log

As a desenvolvedor,
Iwant criar a estrutura de dados para activity log,
So that o sistema possa registrar todas as ações das listas.

**Acceptance Criteria:**

**Given** migração Flyway V6__create_activity_log.sql
**When** executada
**Then** tabela activity_log: id (UUID), list_id (FK), user_id (FK), action, target_type, target_id (UUID nullable), target_name, details (JSONB nullable), created_at
**And** índices: idx_activity_log_list_id, idx_activity_log_created_at (list_id, created_at DESC)
**And** constraints: fk_activity_log_list (CASCADE), fk_activity_log_user (CASCADE)

**Given** campo details JSONB
**When** usado
**Then** formato flexível: { oldValue, newValue, changedFields }

**Given** entidade ActivityLog
**Then** @Entity com campos: id, list, user, action, targetType, targetId, targetName, details, createdAt

**Given** ActivityLogRepository
**Then** findByListIdOrderByCreatedAtDesc(Long listId) retorna Page<ActivityLog>, paginação suportada

---

### Story 6.2: Registro de Atividades de Itens

As a sistema,
Iwant registrar automaticamente todas as ações de itens,
So that haja uma trilha de auditoria completa.

**Acceptance Criteria:**

**Given** ActivityLogEventListener configurado
**When** ItemCreatedEvent/ItemCheckedEvent/ItemUncheckedEvent/ItemUpdatedEvent/ItemDeletedEvent disparado
**Then** ActivityLog criado com action apropriado, target_type = "ITEM", target_id/nome do item, details

**Given** ListService.addItem/toggleItemCheck/updateItem/removeItem
**When** executado com sucesso
**Then** evento de domínio publicado, ActivityLogEventListener captura e registra

**Given** @TransactionalEventListener (AFTER_COMMIT)
**When** configurado
**Then** log só registrado se transação principal commitou, idempotência garantida

**Given** ActivityLogEventListener exception
**When** lançada
**Then** logada mas não quebra operação principal

---

### Story 6.3: Registro de Atividades de Membros

As a sistema,
Iwant registrar automaticamente todas as ações de membros,
So that haja uma trilha completa de participação.

**Acceptance Criteria:**

**Given** ActivityLogEventListener
**When** MemberJoinedEvent disparado
**Then** ActivityLog criado: action = "MEMBER_JOINED", target_type = "MEMBER", details { method, invitedBy }

**Given** ActivityLogEventListener
**When** MemberLeftEvent disparado
**Then** ActivityLog criado: action = "MEMBER_LEFT", details { wasKicked: false }

**Given** ActivityLogEventListener
**When** MemberRemovedEvent disparado
**Then** ActivityLog criado: action = "MEMBER_REMOVED", details { removedBy }

**Given** ListMemberService.joinList/leaveList/removeMember
**When** executado
**Then** evento de domínio publicado, ActivityLogEventListener registra

---

### Story 6.4: API e Frontend de Timeline

As a usuário,
Iwant ver o histórico de atividades da lista,
So that eu possa acompanhar o que aconteceu.

**Acceptance Criteria:**

**Given** endpoint GET /api/lists/{id}/activity disponível
**When** request JWT válido, sou membro, page=0, size=50
**Then** 200 OK com: content (activities), totalPages, totalElements, activities ordenadas created_at DESC

**Given** ActivityTimeline component
**When** activity ITEM_ADDED
**Then** mostra: avatar, "{username} adicionou {itemName}", tempo relativo ("há 5 min")

**Given** ActivityTimeline
**When** activity ITEM_CHECKED/UNCHECKED/UPDATED/REMOVED
**Then** mostra formato apropriado com username, itemName, detalhes

**Given** ActivityTimeline
**When** activity MEMBER_JOINED/LEFT/REMOVED
**Then** mostra: avatar, "{username} entrou/saiu/removido", detalhes

**Given** formatação tempo relativo
**When** < 1min, < 1h, < 24h, < 7d, >= 7d
**Then** "agora mesmo", "há X min", "há X horas", "ontem/terça", "15/01/2026"

**Given** ActivityTimeline na ListView
**When** ícone 📜 tocado
**Then** painel abre/colapsa, carrega GET /api/lists/{id}/activity, infinite scroll para mais

---

### Story 6.5: Integração com Real-Time e Limpeza

As a desenvolvedor,
Iwant integrar activity log com real-time e gerenciar retenção,
So that o sistema não cresça indefinidamente e usuários vejam atividades em tempo real.

**Acceptance Criteria:**

**Given** ActivityLogEventListener cria log
**When** sucesso
**Then** ActivityCreatedEvent publicado, broadcast ACTIVITY_LOGGED para /topic/list/{listId}

**Given** mensagem ACTIVITY_LOGGED recebida frontend
**When** ActivityTimeline aberta
**Then** nova atividade adicionada ao topo com fade-in (200ms)

**Given** ActivityTimeline fechada
**When** ACTIVITY_LOGGED recebida
**Then** indicador 📜 atualizado com badge "X novas", Toast sutil "Nova atividade"

**Given** retenção 30 dias (NFR-R2)
**When** scheduler @Scheduled(cron="0 0 2 * * ?") executa
**Then** deleteOldLogs(30 dias) chamado, logs antigos deletados, espaço liberado

**Given** performance query
**When** tabela tem 10.000+ logs
**Then** índice idx_activity_log_created_at usado, query < 100ms, paginação limita resultados
