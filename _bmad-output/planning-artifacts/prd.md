---
stepsCompleted: ['step-01-init', 'step-02-discovery', 'step-03-success', 'step-04-journeys', 'step-05-domain', 'step-06-innovation', 'step-07-project-type', 'step-08-scoping', 'step-09-functional', 'step-10-nonfunctional', 'step-11-polish', 'step-12-complete']
inputDocuments:
  - docs/NossaLista — Documento de Escopo MVP.txt
  - docs/nossalista-layout.jsx
workflowType: 'prd'
documentCounts:
  briefs: 0
  research: 0
  brainstorming: 0
  projectDocs: 2
classification:
  projectType: web_app
  domain: general
  complexity: low
  projectContext: brownfield
---

# Product Requirements Document - NossaLista

**Author:** Leo
**Date:** 2026-02-09T20:59:33-03:00
**Status:** Draft
**Version:** 1.0

---

## Executive Summary

NossaLista é um aplicativo web de **listas compartilhadas em tempo real** para famílias e amigos. Usuários criam diferentes tipos de lista (Compras, Tarefas, Wishlist, Genérica), convidam outras pessoas e colaboram com sincronização instantânea via WebSocket.

**Diferencial:** Sincronização real-time visível + interface minimalista. Quando dois usuários editam a mesma lista simultaneamente, as alterações aparecem instantaneamente para ambos - sem recarregar a página.

**Stack Técnica:** React 19 + TypeScript + Vite (frontend) | Java 25 + Spring Boot 4 (backend) | WebSocket STOMP/SockJS (real-time) | PostgreSQL | K3s no Raspberry Pi 4.

**Contexto do Projeto:** Projeto de aprendizado com objetivo tangível. Foco em praticar tecnologias modernas, ter controle total de uma aplicação autossustentada em infra própria.

---

## Success Criteria

### User Success

**Momento "Aha!":** Dois usuários editam a mesma lista simultaneamente e veem as alterações aparecerem em tempo real - sem recarregar a página.

**Sinais de sucesso:**
- Usuário convida alguém e essa pessoa entra na lista sem fricção
- Item adicionado aparece instantaneamente na tela de outra pessoa
- Item marcado como concluído sincroniza para todos os participantes
- Interface intuitiva o suficiente para não precisar de tutorial

### Business Success

Como projeto pessoal/familiar, sucesso = **estabilidade e utilidade**:

- Sistema estável funcionando continuamente no Raspberry Pi
- Lista sendo usada ativamente por pelo menos 2-3 pessoas
- Feedback positivo: "isto é melhor que WhatsApp/Notas para listas compartilhadas"
- Deploy automatizado funcionando (GitHub Actions → K3s)

### Technical Success

**Confiabilidade do WebSocket é crítica:**

| Métrica | Alvo |
|---------|------|
| Entrega de mensagens WebSocket | 100% (sem perda) |
| Latência de sincronização | < 500ms entre usuários |
| Uptime do serviço | > 95% (home server realista) |
| Taxa de erro da API | < 1% |

### Measurable Outcomes

- [ ] WebSocket conectado sem queda durante sessão típica (30min)
- [ ] Convite por username funciona em < 5 segundos
- [ ] Checkbox marca/desmarca sincroniza instantaneamente
- [ ] Deploy via GitHub Actions completa sem intervenção manual

---

## User Journeys

### Persona 1: Mariana - A Dono da Lista

**Perfil:** Mariana, 34 anos, mãe de dois. Trabalha fora, estuda à noite, precisa organizar a casa com eficiência.

**Dor:** "Estou cansada de mandar lista no WhatsApp e ninguém atualizar. Compro coisa que já têm ou esqueço o que precisa."

**Objetivo:** Criar lista de compras e compartilhar com o marido Pedro para que ele passe no mercado e não erre.

**Jornada:**

*Cena Abertura:* Mariana está no almoço do trabalho, lembrando que acabou o arroz. Pega o celular, abre o WhatsApp, manda mensagem para Pedro: "Compra arroz, leite, ovos". Pedro lê 2 horas depois... mas não vai no mercado hoje.

*Ação Ascendente:* Mariana conhece o NossaLista por indicação. Faz login com Google. Toca "+ Nova Lista". Escolhe "🛒 Compras", digita "Mercado Semanal". Digita "pedro" na busca e ele aparece. Também gera um link e manda no WhatsApp.

*Clímax:* Pedro entra na lista. Mariana adiciona "Azeite". **Mágica acontece:** na tela do Pedro, o item aparece instantaneamente. Ele marca "Arroz" como comprado. Mariana vê o check verde em tempo real. Ela sorri: "finalmente!"

*Resolução:* Mariana chega em casa, vê que Pedro já comprou 5 dos 8 itens. No jantar: "Compras em 10 minutos, sem discussão!" O NossaLista entrou na rotina da família.

---

### Persona 2: Pedro - O Participante Convidado

**Perfil:** Pedro, 36 anos, marido da Mariana. Trabalha fora, passa no mercado algumas vezes por semana.

**Dor:** "Esqueço o que ela pediu no WhatsApp. A mensagem some lá em cima."

**Objetivo:** Acessar a lista rapidamente e marcar o que comprou.

**Jornada:**

*Cena Abertura:* Pedro está no mercado, carrinho na mão. Pega o celular, rola o WhatsApp... "Onde foi que ela mandou a lista?"

*Ação Ascendente:* Recebe mensagem: "Use o NossaLista". Clica no link, abre no navegador. Login com Google em 2 toques. Lista carrega: "Mercado Semanal" com 8 itens.

*Clímax:* Pedro pega o arroz, toca no checkbox. Check verde. Pega o leite, vê "Quantidade: 3", pega 3. Mariana adiciona "Tomate" em tempo real. Ele vê a notificação.

*Resolução:* Pedro passa no caixa com exatamente o que precisava. Chega em casa: "Trouxe tudo!" Mariana: "Eu sei, vi você marcando!"

---

### Persona 3: Leo - O Admin/Sysadmin

**Perfil:** Leo, desenvolvedor e dono do sistema. Roda NossaLista em Raspberry Pi com K3s em casa.

**Objetivo:** Manter o sistema estável e saber o que está acontecendo.

**Jornada:**

*Cena Abertura:* Leo recebe mensagem: "O app parou de atualizar". WebSocket caindo? Backend crashou?

*Ação Ascendente:* Leo acessa o dashboard K3s. Frontend OK, Backend restartou 3 vezes. Logs mostram: "WebSocket connection timeout". PostgreSQL consumindo muita memória.

*Clímax:* Leo identifica que Cloudflare Tunnel derruba WebSocket após 5 minutos de inatividade. Ajusta keep-alive, commit no GitHub, CI/CD faz deploy automático.

*Resolução:* Sistema volta ao normal em 5 minutos. Leo adiciona monitoramento. Próxima vez, sabe antes dos usuários.

---

### Journey Requirements Summary

| Jornada | Capacidades Reveladas |
|---------|----------------------|
| Mariana - Dono | Onboarding simples, criação de lista, busca de usuário, geração de link, sincronização real-time visível |
| Pedro - Convidado | Login rápido, acesso via link, checkbox intuitivo, visualização de quantidade, notificações de atualização |
| Leo - Admin | Logs de aplicação, métricas de WebSocket, alertas de crash, CI/CD funcional, monitoramento de pods |

---

## Web App Specific Requirements

### Project-Type Overview

NossaLista é uma **Single Page Application (SPA)** moderna construída com React 19 + TypeScript + Vite, focada em colaboração em tempo real para famílias e amigos.

### Technical Architecture

- **SPA completa:** Todo o fluxo sem recarregar página
- **Client-side routing:** React Router para navegação
- **State management:** React Context + hooks (sem Redux para MVP)
- **Real-time core:** WebSocket (STOMP sobre SockJS) como camada primária de sincronização

### Browser Matrix

| Browser | Versão Mínima | Status |
|---------|---------------|--------|
| Chrome/Edge | Últimos 2 anos | ✅ Primário |
| Safari (desktop/mobile) | Últimos 2 anos | ⚠️ Best effort |
| Firefox | Últimos 2 anos | ⚠️ Best effort |

**Estratégia:** Chrome-first para velocidade. Outros browsers testados conforme relatos de usuários.

### Real-Time Architecture

**100% das atualizações são em tempo real:**
- Adicionar item → broadcast para todos conectados
- Marcar/desmarcar checkbox → sincronização imediata
- Editar item → atualização instantânea
- Novo participante → entrada visível na lista

**Protocolo:** STOMP sobre WebSocket com fallback SockJS

### SEO Strategy

**Fase 1 (MVP):** SEO não é crítico - app autenticado não é indexável
**Fase 2 (Futuro):** Meta tags para compartilhamento social (Open Graph)

### Accessibility Level

**Nível inicial: Básico**

✅ **Implementar no MVP:**
- Contraste WCAG AA
- Navegação por teclado
- Labels em inputs e botões
- Touch targets ≥ 44px

⚠️ **Melhorias futuras:** ARIA labels avançados, leitor de tela otimizado

### Responsive Design

**Mobile-first:**
- Mobile: < 640px
- Tablet: 640px - 1024px
- Desktop: > 1024px

O protótipo `nossalista-layout.jsx` demonstra design responsivo com glassmorphism adaptável.

---

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**MVP Approach:** Problem-solving MVP - resolver a dor real de listas compartilhadas com sincronização instantânea.

**Resource Requirements:**
- **Time:** 1 desenvolvedor full-stack
- **Skills:** React, TypeScript, Spring Boot, WebSocket, K3s/Docker
- **Infra:** Raspberry Pi 4 + Cloudflare Tunnel (já disponível)

### MVP Feature Set (Phase 1)

**Core User Journeys Supported:**
- Mariana cria lista e convida Pedro
- Pedro acessa via link e colabora
- Sincronização real-time visível

**Must-Have Capabilities:**

| Categoria | Feature |
|-----------|---------|
| Autenticação | Google OAuth2 + email/senha |
| Listas | 4 tipos pré-definidos (Compras, Tarefas, Wishlist, Genérica) |
| Compartilhamento | Convite por username + link com expiração |
| Real-time | WebSocket (STOMP) para sincronização |
| Itens | CRUD completo + checkbox |
| Histórico | Timeline de atividade por lista |

**5 Fases de Desenvolvimento:**

```
Fase 1 — Fundação (Backend)
├── Setup Spring Boot 4 + Java 25
├── Modelo de dados + Flyway migrations
├── Spring Security + JWT
├── OAuth2 Google + registro email/senha
├── CRUD de listas e itens
└── Profiles dev (H2) / prod (PostgreSQL)

Fase 2 — Compartilhamento
├── Sistema de convites (username + link)
├── Membros da lista (join/leave/remove)
└── Activity log

Fase 3 — Real-time
├── WebSocket config (STOMP + SockJS)
├── Broadcast de alterações em itens
└── Auth no WebSocket (JWT)

Fase 4 — Frontend
├── Setup React + Vite + Tailwind
├── Auth flow (Google + email/senha)
├── Tela Home (listar listas)
├── Tela ListView (itens + real-time)
├── Modais de criação e convite
└── Timeline de atividade

Fase 5 — Infra & Deploy
├── Dockerfiles (frontend + backend)
├── Manifests K3s (Deployment, Service, Ingress)
├── GitHub Actions pipeline
├── Cloudflare Tunnel → nossalista.leoferolive.com.br
└── PostgreSQL no cluster
```

### Post-MVP Features

**Phase 2 (Growth):** App mobile nativo, criação/customização de tipos de lista, notificações push, drag & drop, temas personalizáveis.

**Phase 3 (Expansion):** Offline mode com sync, exportar lista (PDF), integração com assistentes de voz, listas recorrentes automáticas.

### Risk Mitigation Strategy

| Risco | Mitigação |
|-------|-----------|
| WebSocket instável no Cloudflare Tunnel | Configurar keep-alive, testar extensivamente |
| PostgreSQL consumo no Pi | Monitorar recursos, considerar volume externo |
| Conexões simultâneas limitando o Pi | Testar carga, implementar reconexão automática |
| Usuários preferirem WhatsApp/Notas | Testar com família/amigos, coletar feedback |
| Tempo limitado para desenvolvimento | Fases podem ser estendidas, features não-críticas postergadas |

---

## Functional Requirements

⚠️ **Capability Contract:** Esta lista é o contrato de capacidades do produto. Qualquer feature não listada aqui não existirá no produto final a menos que seja explicitamente adicionada.

### Authentication & User Management

- **FR1:** Usuário pode fazer login usando Google OAuth2
- **FR2:** Usuário pode fazer login usando email e senha
- **FR3:** Usuário pode se registrar com email, senha e username único
- **FR4:** Usuário pode acessar seu próprio perfil
- **FR5:** Usuário pode atualizar informações do próprio perfil
- **FR6:** Usuário pode buscar outros usuários por username
- **FR7:** Sistema mantém sessão do usuário autenticado via token JWT

### List Management

- **FR8:** Usuário pode criar uma nova lista escolhendo tipo e nome
- **FR9:** Usuário pode visualizar todas as listas que possui ou participa
- **FR10:** Usuário pode visualizar detalhes de uma lista específica
- **FR11:** Dono da lista pode editar o nome da lista
- **FR12:** Dono da lista pode excluir a lista
- **FR13:** Sistema suporta 4 tipos de lista: Compras, Tarefas, Wishlist, Genérica
- **FR14:** Tipo de lista define quais campos estão disponíveis nos itens

### Item Management

- **FR15:** Participante da lista pode adicionar itens
- **FR16:** Participante da lista pode editar itens existentes
- **FR17:** Participante da lista pode remover itens
- **FR18:** Participante da lista pode marcar/desmarcar item como concluído
- **FR19:** Itens do tipo Compras suportam campo de quantidade
- **FR20:** Itens do tipo Tarefas suportam campo de data de prazo
- **FR21:** Itens do tipo Wishlist suportam campo de URL/link
- **FR22:** Sistema registra quem criou cada item

### Sharing & Collaboration

- **FR23:** Dono da lista pode convidar usuários por username
- **FR24:** Dono da lista pode gerar link de convite com expiração
- **FR25:** Usuário pode aceitar convite via link de convite
- **FR26:** Participante da lista pode visualizar outros membros
- **FR27:** Dono da lista pode remover participantes
- **FR28:** Participante da lista pode sair da lista
- **FR29:** Todos os participantes têm permissão para gerenciar itens
- **FR30:** Sistema distingue dono (OWNER) de participante (MEMBER)

### Real-time Synchronization

- **FR31:** Participantes online recebem atualizações de itens em tempo real
- **FR32:** Participantes online recebem notificação quando item é adicionado
- **FR33:** Participantes online recebem notificação quando item é editado
- **FR34:** Participantes online recebem notificação quando item é removido
- **FR35:** Participantes online recebem notificação quando item é marcado/desmarcado
- **FR36:** Participantes online recebem notificação quando novo membro entra
- **FR37:** Sistema indica quais membros estão online na lista
- **FR38:** Sistema mantém conexão WebSocket com reconexão automática

### Activity & History

- **FR39:** Participante da lista pode visualizar histórico de atividades
- **FR40:** Sistema registra quando item é adicionado com autor e timestamp
- **FR41:** Sistema registra quando item é marcado/desmarcado com autor e timestamp
- **FR42:** Sistema registra quando item é editado com autor e timestamp
- **FR43:** Sistema registra quando item é removido com autor e timestamp
- **FR44:** Sistema registra quando membro entra na lista
- **FR45:** Sistema registra quando membro sai da lista

**Total: 45 Functional Requirements**

---

## Non-Functional Requirements

### Performance

NossaLista depende de sincronização em tempo real. A latência afeta diretamente a percepção de "instantaneidade".

| Métrica | Alvo |
|---------|------|
| Latência WebSocket | < 500ms entre usuários |
| Time to Interactive (TTI) | < 3 segundos |
| First Contentful Paint | < 1.5 segundos |
| Bundle size inicial | < 200KB gzipped |
| Login OAuth2 | < 2 segundos |

**NFR-P1:** Ações do usuário (adicionar, editar, marcar item) refletem para outros participantes em menos de 500ms
**NFR-P2:** Tela inicial carrega em menos de 3 segundos em conexão 4G
**NFR-P3:** WebSocket reconecta automaticamente em caso de queda, sem intervenção do usuário

### Security

Proteção de dados pessoais e controle de acesso são essenciais.

**Dados Protegidos:** Email, nome, username; Conteúdo das listas; Tokens de sessão

**NFR-S1:** Todas as conexões utilizam HTTPS via Cloudflare Tunnel
**NFR-S2:** Senhas são hasheadas usando bcrypt ou argon2
**NFR-S3:** Tokens JWT têm expiração máxima de 7 dias
**NFR-S4:** OAuth2 segue fluxo PKCE para segurança adicional
**NFR-S5:** CORS está configurado para `nossalista.leoferolive.com.br` apenas
**NFR-S6:** Links de convite expiram em 24 horas
**NFR-S7:** Dono da lista é o único que pode excluí-la

### Reliability

Para um home server, 95% de uptime é realista e aceitável.

**NFR-R1:** Sistema mantém uptime > 95% mensal
**NFR-R2:** Logs de aplicação são mantidos por 30 dias para troubleshooting
**NFR-R3:** Backup do PostgreSQL é realizado diariamente
**NFR-R4:** Pods do K3s restartam automaticamente em caso de crash
**NFR-R5:** Deploy via GitHub Actions não causa downtime > 2 minutos

### Accessibility

Nível básico de acessibilidade para inclusão familiar.

**NFR-A1:** Contraste de cores atende WCAG AA (mínimo 4.5:1)
**NFR-A2:** Todas as funcionalidades são acessíveis por teclado
**NFR-A3:** Inputs e botões possuem labels descritivos
**NFR-A4:** Touch targets têm mínimo de 44×44 pixels
**NFR-A5:** Interface funciona em navegadores Chrome últimos 2 anos

### Integration

Integrações externas necessárias para o funcionamento.

**NFR-I1:** Google OAuth2 integra corretamente com produção
**NFR-I2:** WebSocket (STOMP/SockJS) funciona através do Cloudflare Tunnel
**NFR-I3:** GitHub Actions deploya automaticamente para K3s no push para main
**NFR-I4:** PostgreSQL persiste dados corretamente em volume do cluster

---

*End of PRD v1.0*
