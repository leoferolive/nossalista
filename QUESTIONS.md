# QUESTIONS.md — Revisao Tecnica Completa do NossaLista

> Revisao realizada em marco de 2026, cobrindo **todo** o codigo-fonte do projeto: backend (Java/Spring Boot), frontend (React/TypeScript), CI/CD (GitHub Actions), infra (Docker/K8s) e documentacao.
>
> Cada pergunta e independente. Responda diretamente abaixo de cada uma com a decisao ou explicacao.

---

## Status de Implementacao (atualizado 2026-06-10)

Execucao dos itens acionaveis ("corrigir agora" / "sim") em branches isoladas (sem push/PR). Cada area em sua worktree:

| Branch | Itens entregues | Commit(s) |
| ------ | --------------- | --------- |
| `fix/rate-limit-ip-spoofing` | Bypass de rate limit via `X-Forwarded-For` (relacionado a Q2.6) — IP resolvido via `CF-Connecting-IP` | `6f71e03` |
| `fix/auth-hardening` | Q2.2 (JWT secret fail-fast), Q2.4 (cache TTL no filtro), Q2.5 (RBAC), Q2.3 (OAuth one-time code), Q2.7 (verificacao de email) | `0c762bf`, `4d1ffb3` |
| `fix/rename-sharedlist` | Q1.1 (`List` -> `SharedList`) | `df944f7` |
| `fix/docker-hardening` | Q15.2 (healthcheck/curl), Q15.3 (jar especifico), Q15.5 (env vars + bind 127.0.0.1) | `78f3678` |
| `fix/ci-infra-hardening` | Q14.5, Q14.6, Q14.8, Q14.9, Q14.10, Q16.2, Q16.3, Q16.5, Q16.6 | `35178c6` |
| `fix/frontend-apierror` | Q11.2 (`authApi` lanca `ApiError`) | `25337d1` |

Ja em `main` antes desta rodada: Q14.1 (`needs: changes` presente em `ci.yml`). Q9.4: resolvido na pratica (comparacao por `type.slug`).

**Decisao pendente do dono:** enforcement estrito de verificacao de email (`app.auth.require-email-verification`) esta **desligado por padrao** — ligar exige tratar contas legadas (backfill de `email_verified`). Ver Q2.7 e `docs/DECISIONS.md`.

Itens marcados ⚪ Pos-MVP no corpo abaixo seguem deliberadamente adiados.

---

## Indice

1. [Arquitetura Backend](#1-arquitetura-backend)
2. [Seguranca](#2-seguranca)
3. [Banco de Dados e Performance](#3-banco-de-dados-e-performance)
4. [WebSocket e Real-time](#4-websocket-e-real-time)
5. [Tratamento de Erros (Backend)](#5-tratamento-de-erros-backend)
6. [API Design (Backend)](#6-api-design-backend)
7. [Testes (Backend)](#7-testes-backend)
8. [Arquitetura Frontend](#8-arquitetura-frontend)
9. [Componentes e UX](#9-componentes-e-ux)
10. [Hooks e State Management](#10-hooks-e-state-management)
11. [Camada de API (Frontend)](#11-camada-de-api-frontend)
12. [Contextos (Frontend)](#12-contextos-frontend)
13. [CSS e Design System](#13-css-e-design-system)
14. [CI/CD (GitHub Actions)](#14-cicd-github-actions)
15. [Docker e Build](#15-docker-e-build)
16. [Kubernetes e Infraestrutura](#16-kubernetes-e-infraestrutura)
17. [Funcionalidades Ausentes](#17-funcionalidades-ausentes)
18. [Qualidade de Codigo / Code Smells](#18-qualidade-de-codigo--code-smells)
19. [PWA e Service Worker](#19-pwa-e-service-worker)
20. [Mock Server](#20-mock-server)

---

## 1. Arquitetura Backend

### Q1.1 — Entidade `List` sombreia `java.util.List`

A entidade `List.java` tem o mesmo nome de `java.util.List`, forcando imports fully-qualified (`java.util.List<List>`) em todo o codebase. Isso reduz a legibilidade significativamente. Devemos renomear para `SharedList` ou `ListEntity`?

**Resposta:** Vamos renomear para SharedList.

---

### Q1.2 — `PresenceService` e `PushSubscriptionStore` sao in-memory

Ambos usam `ConcurrentHashMap`. Dados de presenca e subscricoes push sao perdidos ao reiniciar o servidor. Isso impede escalabilidade horizontal (multiplas instancias). Isso e uma limitacao aceita para o MVP, ou devemos migrar para Redis/banco?

**Resposta:** Podemos deixar.

---

### Q1.3 — Mapeamento de tipo duplicado em 4+ lugares

O mapeamento entre `typeId`, `slug` e `name` de tipos de lista aparece em: `List.getType()`, `ListMapper.toListResponse()`, `ListJoinService.mapToJoinListResponse()`, `ListJoinService.buildListJoinedResponse()`. Isso viola DRY e e fragil (adicionar um novo tipo exige mudanca em 4 lugares). Devemos centralizar em um unico lugar (ex: `ListTypeEntity` ou um utilitario)?

**Resposta:** Sim, vamos centralizar esse mapeamento em um unico lugar. Podemos criar uma classe utilitaria `ListTypeMapper` que tem metodos para converter entre `typeId`, `slug` e `name`. Isso reduziria a duplicacao, facilitaria a manutencao e garantiria consistencia em todo o codigo.

---

### Q1.4 — `ListMapper` faz query no banco

`ListMapper.toListResponse()` chama `listItemRepository.countByListId()` dentro da camada de mapeamento. Mappers devem ser funcoes de transformacao puras. O count deveria ser buscado no service e passado ao mapper. Corrigir?

**Resposta:** Sim.

---

### Q1.5 — `OAuth2SuccessHandler` roda fora de `@Transactional`

O handler de sucesso OAuth2 faz `findByEmail` + `save` via chamadas diretas ao repositorio, sem boundary transacional. Se o save falhar, nao ha rollback. Adicionar `@Transactional`?

**Resposta:** Sim.

---

### Q1.6 — `NotificationService.notifyListMembers()` e sincrono

A notificacao itera por membros e chama `messagingTemplate.convertAndSend()` + `pushNotificationService.sendToUser()` sincronamente, dentro do boundary de transacao. Para listas com muitos membros, isso pode causar timeouts. Migrar para `@Async` ou Spring Events?

**Resposta:** Pos-MVP. Com uma unica instancia e listas pequenas no MVP, o impacto e minimo. Quando o numero de membros por lista crescer, migrar para `@Async` ou Spring Events.

---

### Q1.7 — Sem interface de servico (service layer)

Todos os services sao classes concretas sem interface. Isso e aceitavel para o MVP? Ou devemos criar interfaces para facilitar testes unitarios puros (sem Mockito)?

**Resposta:** Pos-MVP. Classes concretas sao aceitaveis para o MVP. Interfaces de servico so agregam valor real quando ha multiplas implementacoes — Mockito funciona bem com classes concretas.

---

## 2. Seguranca

### Q2.1 — JWT com expiracao de 7 dias sem refresh token

O token JWT expira em 7 dias (604800000 ms) sem mecanismo de refresh. O usuario deve re-logar apos expirar. Devemos implementar refresh tokens com access tokens de vida curta (ex: 15 min)?

**Resposta:** Sim, implementar refresh tokens e access tokens de vida curta e uma pratica recomendada para melhorar a seguranca. Access tokens de vida curta limitam a janela de ataque se um token for comprometido, enquanto refresh tokens permitem que os usuarios obtenham novos access tokens sem re-logar. Para o MVP, podemos implementar um mecanismo simples de refresh token, armazenando-o com HttpOnly cookie e criando um endpoint `/api/auth/refresh` para trocar por um novo access token.

---

### Q2.2 — Secret JWT padrao fraco no `application.yml`

O `application.yml` tem um secret padrao (`change-this-secret-key-in-production...`). Se alguem rodar em producao sem configurar `JWT_SECRET`, o sistema fica comprometido. Devemos remover o default e forcar falha rapida se a variavel nao estiver definida?

**Resposta:** Sim, remover o valor padrao do secret JWT e forcar uma falha rapida se `JWT_SECRET` nao estiver definido e uma boa pratica de seguranca. Isso previne que o sistema seja implantado com um secret fraco ou conhecido, reduzindo o risco de comprometimento. Podemos adicionar uma checagem na inicializacao do aplicativo para verificar se `JWT_SECRET` esta presente e lançar uma excecao clara se nao estiver.

---

### Q2.3 — Token OAuth2 passado via query parameter na URL

O `OAuth2SuccessHandler` redireciona com `?token=...` na URL. O JWT aparece no historico do browser, logs do servidor e headers Referer. Devemos usar um codigo temporario one-time que e trocado por token, ou cookie HTTP-only?

**Resposta:** Sim, passar o token JWT via query parameter e uma pratica insegura. Usar um codigo temporario one-time ou um cookie HTTP-only seria mais seguro. O codigo temporario pode ser armazenado em cache no servidor por alguns minutos e trocado por um token JWT via endpoint seguro. Alternativamente, usar um cookie HTTP-only para armazenar o token evita que ele seja exposto em URLs, historico do browser e logs, protegendo contra ataques de XSS e vazamento acidental.

---

### Q2.4 — DB lookup em toda requisicao no `JwtAuthenticationFilter`

O filtro JWT faz `userService.findById(userId)` em toda requisicao autenticada. Isso adiciona uma query ao banco por request. Devemos implementar cache (ex: cache in-memory com TTL curto) ou aceitar o custo para o MVP?

**Resposta:** Vamos implementar um cache in-memory simples com TTL curto para armazenar os usuarios autenticados. Isso pode ser feito usando `ConcurrentHashMap` com timestamps para expirar entradas antigas. O cache reduziria significativamente a carga no banco de dados para requisicoes autenticadas frequentes, melhorando a performance sem adicionar complexidade significativa ao MVP.

---

### Q2.5 — Role `ADMIN` existe mas nunca e usada

O enum `Role` tem `USER` e `ADMIN`, mas `ADMIN` nunca e atribuido, verificado, ou usado em `@PreAuthorize`. As authorities no `JwtAuthenticationFilter` sao sempre `emptyList()`. Remover o role ADMIN por enquanto, ou implementar RBAC?

**Resposta:** Pode implementar RBAC basico usando o role `ADMIN`, atribuindo-o a usuarios especificos e protegendo endpoints sensiveis com `@PreAuthorize("hasRole('ADMIN')")`. Isso adicionaria uma camada de seguranca para funcionalidades administrativas futuras. Se nao houver necessidade imediata, podemos remover o role `ADMIN` para evitar confusao, mas implementar RBAC desde o inicio pode ser benéfico a longo prazo.

---

### Q2.6 — Sem rate limiting em endpoints de autenticacao

Nao ha rate limiting em login, registro, busca de usuarios ou geracao de convites. O sistema e suscetivel a ataques de brute-force. Implementar rate limiting (ex: Bucket4j, Spring Cloud Gateway)?

**Resposta:** Sim, implementar rate limiting nos endpoints de autenticacao e uma medida importante para prevenir ataques de brute-force. Podemos usar uma biblioteca como Bucket4j para limitar o numero de tentativas por IP ou por usuario em um periodo de tempo. Isso ajudaria a proteger contra ataques automatizados e reduziria o risco de comprometimento de contas.

---

### Q2.7 — Sem verificacao de email no registro

Usuarios podem se registrar com qualquer email sem verificacao. Alguem pode usar o email de outra pessoa. Implementar verificacao por email e necessario para o MVP?

**Resposta:** Sim, vamos fazer isso. Implementar verificacao de email e uma pratica recomendada para garantir a autenticidade dos usuarios e prevenir o uso de emails falsos ou de terceiros. Podemos enviar um email com um link de verificacao contendo um token unico, e o usuario so pode acessar funcionalidades completas apos verificar seu email. Isso melhoraria a seguranca e a confiabilidade do sistema desde o inicio.

---

### Q2.8 — `LoginRequest` nao valida formato de email

O DTO `LoginRequest` usa `@NotBlank` no campo email mas nao tem `@Email`. Um usuario pode enviar strings que nao sao emails. Adicionar `@Email`?

**Resposta:** Sim, adicionar `@Email` ao campo de email em `LoginRequest` e uma validacao simples que melhora a qualidade dos dados e a experiencia do usuario. Isso previne que usuarios enviem entradas claramente invalidas e pode ajudar a reduzir erros e confusao. E uma adicao de baixo custo que traz beneficios significativos para o MVP.

---

### Q2.9 — JWT armazenado em `localStorage` (frontend)

O token JWT e armazenado em `localStorage`, que e vulneravel a XSS (qualquer script injetado pode ler). Cookies HTTP-only seriam mais seguros. Isso e um risco aceito para o MVP?

**Resposta:** Não precisa por agora.

---

### Q2.10 — CORS com origens de desenvolvimento em producao

`SecurityConfig` tem origens de desenvolvimento (`localhost:5173`, `localhost:8080`) hardcoded junto com producao. Remover origens dev do profile de producao?

**Resposta:** Sim, corrigir agora. Mover origens de desenvolvimento para um profile Spring separado (`dev`) e manter apenas a origem de producao no profile `prod`.

---

## 3. Banco de Dados e Performance

### Q3.1 — N+1 query em `ListMapper.toListResponse()`

Ao listar todas as listas do usuario, `listItemRepository.countByListId()` e chamado por lista, gerando N queries de COUNT separadas. Devemos trazer o count na query principal (JOIN ou subselect)?

**Resposta:** Sim

---

### Q3.2 — `UserService.searchByUsername()` faz full table scan

`findByUsernameContainingIgnoreCase` traduz para `LIKE '%query%'` — sem uso de indice. Performance degrada com tabela grande. Adicionar indice ou limitar resultados?

**Resposta:** Pos-MVP. Com base de usuarios pequena no MVP, o impacto e negligivel. Combinado com Q3.3 (adicionar limite), o risco e mitigado. Indice trigram (`pg_trgm`) pode ser adicionado quando a tabela crescer.

---

### Q3.3 — `/api/users/search` sem limite de resultados

O endpoint de busca de usuarios nao tem paginacao nem limite. Uma query como `q=a` pode retornar milhares de resultados. Adicionar `@Max` ou paginacao?

**Resposta:** Sim, corrigir agora. Adicionar `LIMIT 20` (ou paginacao com `Pageable`) no endpoint. Mudanca simples que previne abuso e problemas de performance.

---

### Q3.4 — `AuthService.generateUniqueUsername()` faz queries em loop

Na criacao de usuario OAuth2, o metodo tenta usernames sequenciais com queries individuais (`while (userRepository.findByUsername(...).isPresent())`). Para prefixos populares, isso pode gerar muitas queries. Otimizar?

**Resposta:** Pos-MVP. O cenario de muitas colisoes e raro. Uma otimizacao simples seria usar `SELECT username FROM users WHERE username LIKE 'prefix%'` e calcular o proximo numero em memoria.

---

### Q3.5 — Activity log cresce indefinidamente

Nao ha politica de retencao ou limpeza para logs de atividade. O volume cresce sem limites. Implementar retention policy (ex: manter ultimos 90 dias)?

**Resposta:** Pos-MVP. O volume de dados no MVP sera pequeno. Implementar retention policy (ex: manter ultimos 90 dias ou N registros por lista) quando o volume justificar.

---

### Q3.6 — Sem paginacao em `getItemsByListId`

O endpoint retorna todos os itens da lista sem paginacao. Para listas muito grandes, isso pode ser problematico. Adicionar paginacao ou limitar resultados?

**Resposta:** Pos-MVP. Listas de compras/tarefas raramente excedem centenas de itens. Adicionar paginacao com `Pageable` e scroll infinito no frontend quando necessario.

---

## 4. WebSocket e Real-time

### Q4.1 — Race condition no `PushSubscriptionStore`

O `PushSubscriptionStore` usa `ConcurrentHashMap` com `compute()`, mas os valores sao `ArrayList` (nao thread-safe). A `ArrayList` pode ser lida via `findByUserId()` enquanto esta sendo modificada dentro de `compute()`. Mesmo com `List.copyOf()` no retorno, ha uma janela de corrida. Corrigir para `CopyOnWriteArrayList` ou sincronizar?

**Resposta:** Corrigir agora. A correcao e trivial: trocar `ArrayList` por `CopyOnWriteArrayList` dentro do `compute()`. Previne corrupcao de dados sem impacto de performance relevante.

---

### Q4.2 — Sem protocolo de reconexao/sync para eventos perdidos

Se o cliente desconecta e reconecta, nao ha mecanismo para determinar quais eventos foram perdidos. O campo `revision` no `WebSocketMessage` sugere que isso foi planejado mas nao implementado. Implementar sync baseado em revision?

**Resposta:** Pos-MVP. Implementar sync baseado em revision e complexo. Para o MVP, o cliente pode fazer um fetch completo ao reconectar. Documentar a limitacao.

---

### Q4.3 — JSON manual no `PushNotificationService`

`PushNotificationService.serialize()` usa `String.format` com escaping manual em vez de `ObjectMapper`. Isso e fragil e pode quebrar com caracteres especiais (newlines, tabs, unicode). Migrar para Jackson?

**Resposta:** Corrigir agora. Migrar para `ObjectMapper` e uma mudanca simples que elimina risco de quebra com caracteres especiais. O `ObjectMapper` ja esta disponivel via Spring.

---

### Q4.4 — `WebSocketEventPublisher.publishItemsEvent()` nome enganoso

O metodo e usado para todos os tipos de evento (items, members, list updates), nao apenas itens. Renomear para `publishEvent()` ou similar?

**Resposta:** Corrigir agora. Renomear para `publishEvent()`. Mudanca trivial que melhora legibilidade.

---

## 5. Tratamento de Erros (Backend)

### Q5.1 — `IllegalArgumentException` e capturada de forma muito ampla

O `GlobalExceptionHandler` captura `IllegalArgumentException` e retorna 400. Qualquer biblioteca que lance IAE tera seus erros internos expostos como 400 para o cliente. Restringir a casos conhecidos?

**Resposta:** Corrigir agora. Criar excecoes de dominio especificas (ex: `InvalidInputException`) e usar essas no handler. Remover o catch generico de `IllegalArgumentException` para evitar expor erros internos de bibliotecas.

---

### Q5.2 — Sem catch-all para excecoes nao tratadas

Nao ha handler para `Exception.class` ou `RuntimeException.class`. Um NPE inesperado ou erro de banco resultara na pagina de erro padrao do Spring em vez de uma resposta RFC 7807 consistente. Adicionar?

**Resposta:** Corrigir agora. Adicionar handler para `Exception.class` que retorna 500 com corpo RFC 7807 consistente. Previne vazamento de stack traces e garante respostas uniformes.

---

### Q5.3 — `UserService` lanca `RuntimeException` generico

`UserService.findByEmail()`, `findByUsername()` e outros metodos lancam `RuntimeException` com mensagens genericas. Essas excecoes nao recebem tratamento adequado no `GlobalExceptionHandler`. Criar excecoes de dominio especificas?

**Resposta:** Corrigir agora. Criar excecoes de dominio: `ResourceNotFoundException`, `DuplicateResourceException`, etc. Adicionar handlers correspondentes no `GlobalExceptionHandler`. Trabalhar junto com Q5.1 e Q5.2.

---

## 6. API Design (Backend)

### Q6.1 — Convencao de nomes de DTO inconsistente

Alguns DTOs usam sufixo `DTO` (`CreateItemRequestDTO`, `ListItemResponseDTO`), outros nao (`UpdateItemRequest`, `ListResponse`). Padronizar?

**Resposta:** Pos-MVP. Padronizar para formato sem sufixo `DTO` (ex: `CreateItemRequest`, `ListItemResponse`) quando houver janela de refatoracao. A inconsistencia nao causa bugs.

---

### Q6.2 — Dois DTOs para update de item: `UpdateItemRequest` e `UpdateItemRequestDTO`

Existem dois DTOs de update: `UpdateItemRequest` (com validacao `@NotBlank` em `name`) e `UpdateItemRequestDTO` (com campos opcionais). Qual e o canonico? O outro deve ser removido?

**Resposta:** Corrigir agora. Investigar qual DTO e realmente usado nos controllers e remover o outro. Dois DTOs para a mesma operacao e confuso e pode causar bugs.

---

### Q6.3 — `ListMember` usa `@CreationTimestamp`/`@UpdateTimestamp` enquanto outras entidades usam `@PrePersist`/`@PreUpdate`

Inconsistencia no mecanismo de timestamp entre entidades. Pode causar diferencas sutis de timing. Padronizar em um unico mecanismo?

**Resposta:** Pos-MVP. Padronizar para `@PrePersist`/`@PreUpdate` (mais explicito e independente de Hibernate) quando houver refatoracao de entidades.

---

### Q6.4 — `ListItemService.touchListRevision()` seta `updatedAt` duas vezes

O metodo chama `list.setUpdatedAt(LocalDateTime.now())` e depois `listRepository.save(list)`, mas a entidade tambem tem `@PreUpdate` que seta `updatedAt` novamente. O set manual e sobrescrito pelo `@PreUpdate`, e o revision retornado pode nao corresponder ao que esta no banco. Corrigir?

**Resposta:** Corrigir agora. Remover o `setUpdatedAt` manual e deixar o `@PreUpdate` cuidar. O set manual e redundante e pode causar inconsistencia entre o revision retornado e o valor no banco.

---

## 7. Testes (Backend)

### Q7.1 — JaCoCo exclui todo o pacote `websocket`

A exclusao de cobertura do JaCoCo inclui o pacote `websocket` inteiro. Isso e uma lacuna significativa. Reduzir o escopo da exclusao?

**Resposta:** Pos-MVP. Quando testes de WebSocket forem adicionados (Q7.4), reduzir o escopo da exclusao para cobrir apenas classes de config.

---

### Q7.2 — Sem teste para `GlobalExceptionHandler`

Nao ha teste dedicado verificando que todos os tipos de excecao produzem respostas RFC 7807 corretas. Adicionar?

**Resposta:** Pos-MVP. Adicionar testes quando o handler for refatorado (Q5.1, Q5.2, Q5.3).

---

### Q7.3 — Sem teste para `SpaController`

O controller que encaminha rotas de frontend para `index.html` nao tem teste. Adicionar?

**Resposta:** Pos-MVP. O SpaController e trivial e coberto indiretamente pelos testes E2E.

---

### Q7.4 — Sem testes de integracao WebSocket

Existem apenas testes unitarios para interceptors e presence. Nao ha teste de integracao end-to-end do WebSocket (connect, subscribe, receive message). Adicionar?

**Resposta:** Pos-MVP. Testes de integracao WebSocket sao complexos de configurar. Os testes unitarios existentes cobrem a logica critica.

---

### Q7.5 — TODO no `ListRepository` nunca enderecado

Linha 76 do `ListRepository`: `// TODO: Adicionar countItems()`. Esta funcionalidade planejada nunca foi implementada. Resolver ou remover o TODO?

**Resposta:** Corrigir agora. Relacionado a Q3.1 — implementar o `countItems()` como parte da correcao do N+1 e remover o TODO.

---

## 8. Arquitetura Frontend

### Q8.1 — `App.tsx` e `App.css` sao dead code

Esses arquivos sao o scaffold padrao do Vite (contador). Nunca sao importados — `main.tsx` gerencia todo o roteamento. Deletar?

**Resposta:** Sim, corrigir agora. Deletar ambos. Dead code do scaffold Vite que nunca e usado.

---

### Q8.2 — `ListView.tsx` e um God Component (1201 linhas, 37+ states)

`ListView.tsx` tem 37+ variaveis de estado, 20+ handlers, 10+ useEffects em um unico componente. Qualquer mudanca de state (toast, modal, WebSocket) causa re-render completo. Decompo-lo em sub-componentes menores?

**Resposta:** Pos-MVP. E a maior divida tecnica do frontend, mas decompo-lo agora e arriscado e demorado. Planejar decomposicao em sub-componentes (`ListHeader`, `ItemList`, `ListActions`, modais) como tarefa dedicada pos-MVP.

---

### Q8.3 — Sem `AbortController` em nenhum hook

Nenhum dos hooks (`useItems`, `useLists`, `useActivities`) cancela requests em andamento ao desmontar o componente. Isso pode causar state updates em componentes desmontados e requests redundantes. Implementar?

**Resposta:** Pos-MVP. O risco de state updates em componentes desmontados gera warnings no console mas nao causa crashes. Implementar quando os hooks forem refatorados.

---

### Q8.4 — Sem camada de normalizacao de dados

Cada hook gerencia seu proprio estado independentemente. Se a mesma lista aparece em `useLists.lists` e `useLists.currentList`, elas podem ficar dessincronizadas. Implementar normalizacao centralizada?

**Resposta:** Pos-MVP. Para o MVP com poucas telas e dados limitados, a dessincronizacao e rara. Considerar TanStack Query pos-MVP em vez de normalizacao manual.

---

### Q8.5 — `useToast()` cria instancias independentes

Cada componente que chama `useToast()` obtem seu proprio estado de toast isolado. `EditItemModal`, `InviteModal`, `UserProfile` e `ListView` todos tem sistemas de toast separados. Toasts de um componente nao sao visiveis em outro. Centralizar via Context?

**Resposta:** Corrigir agora. Centralizar via `ToastContext` global. Toasts devem ser visiveis independente de qual componente os disparou. Mudanca relativamente simples que melhora a UX significativamente.

---

### Q8.6 — `useWebSocket.ts` e um wrapper trivial sem valor agregado

O hook apenas desestrutura e re-retorna o contexto, sem transformacao ou logica adicional. `useWebSocketContext` ja fornece o mesmo comportamento. Remover a indireção desnecessaria?

**Resposta:** Corrigir agora. Remover `useWebSocket.ts` e usar `useWebSocketContext` diretamente. Elimina indireção desnecessaria.

---

### Q8.7 — Provider nesting complexo e fragil em `main.tsx`

`OnboardingProvider` envolve `WebSocketProvider`, que depende de `AuthProvider`, e `NotificationWrapper` depende de ambos. O grafo de dependencias e complexo e fragil. Documentar a ordem ou simplificar?

**Resposta:** Pos-MVP. Documentar a ordem de dependencia dos providers como comentario em `main.tsx`. A complexidade e inerente ao pattern de Context API — simplificar com Jotai/Zustand e escopo pos-MVP.

---

## 9. Componentes e UX

### Q9.1 — Multiplos modais nao usam `ModalShell`

`EditListNameModal`, `DeleteListModal`, `DeleteConfirmModal`, `EditItemModal` e `MembersModal` implementam seus proprios backdrops, key handlers e layouts em vez de usar o componente compartilhado `ModalShell`. Isso cria inconsistencia visual e comportamental. Migrar todos para `ModalShell`?

**Resposta:** Pos-MVP. Migrar incrementalmente — quando qualquer modal for modificado, migra-lo para `ModalShell`. Nao fazer migracao big-bang.

---

### Q9.2 — `ModalShell` nao tem focus trap nem body scroll lock

O modal tem `role="dialog"` e `aria-modal="true"`, mas nao implementa focus trap real (Tab pode sair do modal) nem lock de scroll do background. Implementar?

**Resposta:** Pos-MVP. Importante para acessibilidade. Usar `focus-trap-react` em vez de implementar manualmente. Priorizar quando trabalhar em acessibilidade.

---

### Q9.3 — `DeleteConfirmModal` foca no botao destrutivo

O auto-focus vai para o botao "Confirmar" (destrutivo) em vez do botao "Cancelar" (seguro). Isso e anti-pattern de UX — acoes destrutivas nao devem ser focadas automaticamente. Inverter?

**Resposta:** Corrigir agora. Mover `autoFocus` para o botao "Cancelar". Mudanca de uma linha que previne exclusoes acidentais.

---

### Q9.4 — `EditItemModal` — comparacao de `listType` provavelmente quebrada

Linhas 85-91 comparam `listType` (que recebe `String(currentList.type.id)`, um numero) contra strings como `'SHOPPING'`, `'TASK'`, `'WISHLIST'`. A menos que os IDs sejam essas strings, os campos condicionais nunca renderizam. Isso e um bug? Deveria comparar com `type.slug`?

**Resposta:**

---

### Q9.5 — `EditItemModal` — delay de 500ms antes do save

O save e envolvido em `setTimeout(500)`. O usuario clica "Salvar", nada acontece por 500ms, depois a API e chamada. O botao fica clicavel durante o delay, permitindo double-submit. Remover o delay?

**Resposta:** Corrigir agora. Remover o `setTimeout(500)`. Desabilitar o botao durante o save (loading state) para prevenir double-submit em vez de usar delay artificial.

---

### Q9.6 — `ThemeToggle` usa texto "Sun" e "Moon" em vez de icones

As labels do toggle de tema sao texto literal (`Sun`, `Moon`) com `aria-hidden="true"`. Parecem placeholders onde icones SVG deveriam estar. Substituir por icones?

**Resposta:** Pos-MVP. Substituir por icones SVG de sol/lua quando houver polish visual. Funcionalidade nao e afetada.

---

### Q9.7 — `navigate(-1)` pode sair do app

Em `ListView.tsx` linha 849, `navigate(-1)` e usado para voltar. Se o usuario acessou a URL diretamente (link compartilhado), isso pode navegar para um site externo ou pagina em branco. Usar `navigate('/home')` como fallback seguro?

**Resposta:** Corrigir agora. Usar `navigate('/home')` como fallback seguro. Mudanca simples que previne UX quebrada para links diretos/compartilhados.

---

### Q9.8 — Double toast ao sair da lista

`handleConfirmLeaveList` em `ListView.tsx` armazena toast no `sessionStorage` E passa via `navigation state`. A pagina Home le AMBAS as fontes, podendo mostrar o toast duas vezes. Corrigir?

**Resposta:** Corrigir agora. Usar apenas uma das fontes (preferir `navigation state`). Remover a duplicacao com `sessionStorage`.

---

### Q9.9 — `LoginResponse` interface duplicada em `Login.tsx` e `LoginModal.tsx`

A mesma interface e definida identicamente em dois arquivos. Extrair para um arquivo de tipos compartilhado?

**Resposta:** Pos-MVP. Extrair para `types/Auth.ts` quando houver refatoracao de tipos.

---

### Q9.10 — `Login.tsx` standalone nao processa `pendingInviteCode`

A pagina standalone de login nao verifica `sessionStorage` para `pendingInviteCode`, enquanto o `LoginModal.tsx` verifica. Um usuario logando via pagina standalone nao tera seu convite processado. Bug?

**Resposta:** Corrigir agora. Bug real — usuario que logar pela pagina standalone perdera o convite pendente. Adicionar a mesma logica de `pendingInviteCode` do `LoginModal.tsx`.

---

### Q9.11 — `InviteModal` search dropdown sem navegacao por teclado

O dropdown de resultados de busca de usuarios nao tem `role="listbox"`, navegacao por setas, nem `aria-activedescendant`. Usuarios que dependem de teclado nao conseguem selecionar resultados. Corrigir?

**Resposta:** Pos-MVP. Importante para acessibilidade, mas nao bloqueia funcionalidade core. Implementar quando trabalhar em a11y.

---

### Q9.12 — `MembersModal` sem handler de Escape nem click-to-close no backdrop

Diferente de outros modais, este nao pode ser fechado com Escape ou clicando fora. Apenas o botao X funciona. Consistir com outros modais?

**Resposta:** Corrigir agora. Adicionar handler de Escape e click no backdrop para consistencia com outros modais. Mudanca simples.

---

### Q9.13 — `ItemOptionsMenu` sem roles ARIA e posicionamento fragil

O menu dropdown nao tem `role="menu"`/`role="menuitem"`. O posicionamento fixo nao conta para mudancas de scroll apos abertura. Corrigir?

**Resposta:** Pos-MVP. Adicionar `role="menu"`/`role="menuitem"` e corrigir posicionamento quando trabalhar em acessibilidade.

---

### Q9.14 — `AppHeader` mostra `ThemeToggle` duplicado no desktop

No desktop, o toggle de tema aparece na barra do header E dentro do menu dropdown. O usuario tem dois lugares para mudar o tema. Remover um?

**Resposta:** Corrigir agora. Remover o toggle do dropdown no desktop, manter apenas o da barra do header. No mobile, manter apenas no menu.

---

### Q9.15 — `OnboardingTourOverlay` usa `MutationObserver` no `document.body` inteiro

Observar o body inteiro para mutations (childList, subtree, attributes) e custoso. Cada mudanca no DOM do app causa recompute da posicao do spotlight. Limitar escopo ou throttle?

**Resposta:** Pos-MVP. O onboarding roda uma unica vez por usuario. O impacto de performance e temporario.

---

### Q9.16 — `TypeCard` usa `aria-pressed` em vez de `aria-checked` dentro de radiogroup

Dentro de um `radiogroup`, itens devem usar `role="radio"` com `aria-checked`, nao `aria-pressed` (que e para toggle buttons). Corrigir?

**Resposta:** Pos-MVP. Corrigir para `role="radio"` com `aria-checked` quando trabalhar em acessibilidade.

---

### Q9.17 — `JoinListPage` — `quantity` check com `&&` falha para `quantity === 0`

Linha 343: `{item.quantity && ...}` nao renderiza o badge quando quantidade e 0. Deveria usar `item.quantity != null`. Corrigir?

**Resposta:** Corrigir agora. Bug real — `quantity === 0` e um valor valido. Trocar para `item.quantity != null`.

---

### Q9.18 — Hardcoded `bg-yellow-100 text-yellow-800` ignora dark theme em `JoinListPage`

Classes Tailwind raw usadas em vez de tokens do design system (`nl-*`). No dark theme, isso fica visualmente quebrado. Corrigir?

**Resposta:** Corrigir agora. Substituir classes Tailwind hardcoded por tokens do design system (`nl-*`) que respeitam o tema. Bug visual no dark mode.

---

### Q9.19 — `Profile.tsx` e `JoinListPage` usam `window.location.reload()` para retry

Um full reload perde todo o estado SPA. Deveria re-chamar a funcao de load em vez de recarregar a pagina. Corrigir?

**Resposta:** Pos-MVP. O reload funciona, so nao e elegante. Substituir por re-chamada da funcao de load quando os componentes forem refatorados.

---

### Q9.20 — Logout inconsistente entre `Profile.tsx` e `AppHeader.tsx`

`Profile.tsx` chama `usersApi.logout()` antes de `logout()`, enquanto `AppHeader.tsx` chama apenas `logout()` do contexto sem hit na API. Se o endpoint invalida o token server-side, o AppHeader deixa o token valido. Qual e o comportamento correto?

**Resposta:** Corrigir agora. Padronizar: ambos devem chamar `usersApi.logout()` antes de `logout()` do contexto. Mover a logica para dentro do `logout()` do `AuthContext` para garantir consistencia.

---

### Q9.21 — `AuthCallback.tsx` persiste token antes de validar

`persistAuthToken(token)` e chamado antes de `/api/users/me` validar o token. Se o token for invalido, ha uma janela onde estado invalido esta no storage. O catch limpa, mas a janela existe. Reordenar?

**Resposta:** Pos-MVP. A janela de estado invalido e muito curta e o catch limpa corretamente. Reordenar quando refatorar o fluxo de auth.

---

### Q9.22 — `JoinListPage` auto-join pode disparar multiplas vezes

O `useEffect` que auto-join quando autenticado tem `listData` como dependencia. Se `listData` mudar, o join pode disparar novamente. No React 18 Strict Mode, o efeito pode executar duas vezes antes do guard `joining` ser setado. Corrigir?

**Resposta:** Corrigir agora. Usar `useRef` como guard em vez de state (`joining`) para evitar execucao dupla no Strict Mode. Ex: `const joiningRef = useRef(false)`.

---

## 10. Hooks e State Management

### Q10.1 — Stale closure em `useItems` — `toggleItem`, `updateItem`, `deleteItem`

Esses callbacks fecham sobre o valor de `items` no momento da criacao. Como `items` esta no array de dependencias, os callbacks sao recriados a cada mudanca, invalidando memoizacao downstream. Pior: entre invocacao e resolucao async, `items` pode ter mudado, causando rollback com dados stale. Devemos usar o updater funcional `setItems(prev => ...)` exclusivamente?

**Resposta:** Corrigir agora. Migrar todos os setters para o pattern funcional `setItems(prev => ...)`. Elimina a dependencia de `items` no array de dependencias e previne rollbacks com dados stale.

---

### Q10.2 — Delay de 200ms hardcoded em `useItems.deleteItem`

`await new Promise(resolve => setTimeout(resolve, 200))` e um delay de animacao no hook de dados. Isso acopla a camada de dados ao UI. Se o componente desmontar antes de 200ms, a execucao continua. Mover a animacao para a camada de componente?

**Resposta:** Corrigir agora. Mover o delay de animacao para a camada de componente (CSS transition ou callback). O hook de dados nao deve ter conhecimento de animacoes.

---

### Q10.3 — Sem deduplicacao de requests / race condition em `useItems`

Nao ha `AbortController` nem deduplicacao. Se `fetchItems` for chamado duas vezes rapidamente, ambos requests completam e o segundo pode sobrescrever o primeiro com dados stale. Implementar?

**Resposta:** Pos-MVP. Considerar migrar para TanStack Query, que resolve deduplicacao, caching e race conditions de forma elegante.

---

### Q10.4 — `useLists` — state `loading` compartilhado entre `fetchLists` e `createList`

Ambas operacoes setam o mesmo `loading`. Se o usuario cria uma lista enquanto a listagem ainda esta carregando, o loading fica incorreto quando a primeira operacao completa. Separar os states de loading?

**Resposta:** Pos-MVP. Separar em `listLoading` e `createLoading` quando refatorar os hooks.

---

### Q10.5 — `useActivities` — race condition entre `refresh` e `loadMore`

Se `refresh` e chamado enquanto `loadMore` esta em voo, a resposta antiga pode ser appendada a atividades que ja foram resetadas por `refresh`, resultando em entradas duplicadas. Usar `AbortController`?

**Resposta:** Pos-MVP. O cenario e raro na pratica. Corrigir com `AbortController` quando refatorar hooks.

---

### Q10.6 — `useLongPress` — sem cleanup ao desmontar

O timeout ref nunca e limpo ao desmontar. Se o usuario inicia um long press e o componente desmonta antes do timeout, `onLongPress()` sera chamado em componente desmontado. Adicionar cleanup?

**Resposta:** Corrigir agora. Adicionar `useEffect` de cleanup que limpa `timeoutRef.current` ao desmontar. Mudanca de 3 linhas que previne memory leak.

---

### Q10.7 — `usePushNotifications` — inscricao orfã ao falhar subscribe

Se `pushApi.subscribe(subscription.toJSON())` falha, a inscricao no browser ja foi criada (via `registration.pushManager.subscribe`), mas o servidor nao sabe. O usuario fica inscrito no browser sem o servidor saber. Desfazer inscricao no browser em caso de falha?

**Resposta:** Pos-MVP. Adicionar `subscription.unsubscribe()` no catch quando refatorar push notifications.

---

### Q10.8 — `handleWebSocketMessage` em `ListView` tem 8 dependencias

O callback e recriado sempre que qualquer dependencia muda, causando unsubscribe/resubscribe no WebSocket. Isso pode perder mensagens durante a reconexao. Reduzir dependencias usando refs?

**Resposta:** Pos-MVP. Resolver junto com a decomposicao de `ListView.tsx` (Q8.2). Usar refs para callbacks estaveis.

---

### Q10.9 — `useItems` — optimistic updates ignoram resposta do servidor

O hook faz update otimista e ignora a resposta do servidor (`updated`). Se o servidor retorna dados diferentes (ex: `updatedAt` diferente), o estado local diverge. Reconciliar com dados do servidor?

**Resposta:** Pos-MVP. Reconciliar com dados do servidor apos a resposta. Considerar TanStack Query que tem esse pattern built-in.

---

### Q10.10 — `OnboardingContext` — `finishTour` silencia erros de API

Se `usersApi.completeOnboarding()` falha, o tour termina visualmente mas o servidor nao sabe. Na proxima sessao, o tour recomeça. Tratar o erro e mostrar feedback?

**Resposta:** Pos-MVP. O pior caso e o tour reiniciar na proxima sessao — inconveniente mas nao critico.

---

## 11. Camada de API (Frontend)

### Q11.1 — Duplicacao massiva de error handling nos API files

Cada metodo em `listsApi.ts`, `itemsApi.ts`, `usersApi.ts` repete o mesmo padrao try/catch com `AxiosError<ProblemDetail>`, if/else por status code, ~15 vezes no total. Uma funcao utilitaria `handleApiError` eliminaria centenas de linhas duplicadas. Refatorar?

**Resposta:** Corrigir agora. Criar funcao utilitaria `handleApiError(error): never` que centraliza o pattern try/catch. Reduz centenas de linhas duplicadas e garante tratamento consistente.

---

### Q11.2 — Alguns metodos lancam `Error`, outros lancam `ApiError`

`getListById` lanca `Error` (sem status), `deleteList` lanca `ApiError` (com status). Os hooks consomem com `err instanceof Error`, perdendo o status code do `ApiError`. Padronizar?

**Resposta:** Corrigir agora. Padronizar para sempre lancar `ApiError` (com status code). Corrigir junto com Q11.1.

---

### Q11.3 — `searchUsers` usa parametro `q`, mock espera `query`

Em `listsApi.ts` linha 318: `params: { q: query }`. No mock server linha 379: `url.searchParams.get('query')`. O mock sempre recebe `null` para o parametro de busca. Bug no mock ou na API?

**Resposta:** Corrigir agora. Bug real — o mock nunca recebe o parametro de busca. Alinhar o mock para usar `q` (que e o que a API real envia).

---

### Q11.4 — `client.ts` — redirect duro no 401

`window.location.href = '/'` no interceptor de 401 causa full page reload, perdendo todo o estado SPA. Multiple requests simultaneos com 401 disparam multiplos redirects. Migrar para dispatch via AuthContext?

**Resposta:** Pos-MVP. Migrar para dispatch via `AuthContext` para manter o estado SPA e evitar multiplos redirects. Complexidade media.

---

### Q11.5 — `pushApi.getVapidPublicKey` engole todos os erros

O metodo catch tudo e retorna `null`. Erros de rede, erros do servidor e endpoints faltando sao tratados como "push nao configurado". Isso dificulta debug em producao. Pelo menos logar o erro?

**Resposta:** Pos-MVP. Adicionar `console.error` no minimo para facilitar debug. O comportamento de retornar `null` e aceitavel como fallback.

---

### Q11.6 — Tipos `snake_case` vs `camelCase` misturados nos DTOs frontend

`ListResponse` usa camelCase (`inviteCode`, `isOwner`), `InviteLinkResponse` usa snake_case (`invite_code`, `invite_link`), `JoinListResponse` usa snake_case (`type_slug`, `owner_username`). Isso sugere que o backend retorna formatos diferentes por endpoint. O frontend nao normaliza. Padronizar?

**Resposta:** Corrigir agora. Padronizar no backend para retornar sempre camelCase (configurar `ObjectMapper` com `PropertyNamingStrategies.LOWER_CAMEL_CASE`). Corrigir os endpoints inconsistentes.

---

### Q11.7 — `UpdateItemRequest` nao permite setar campos como `null`

`CreateItemRequest` tem `quantity?: number | null` (nullable), mas `UpdateItemRequest` tem `quantity?: number` (nao nullable). Isso significa que voce pode setar `quantity` no create mas nao pode limpa-lo no update. Alinhar?

**Resposta:** Corrigir agora. Alinhar `UpdateItemRequest` com `CreateItemRequest` permitindo `quantity?: number | null`. Caso contrario, usuarios nao conseguem limpar campos opcionais.

---

## 12. Contextos (Frontend)

### Q12.1 — `AuthContext` — `isAuthenticated` le `localStorage` em todo render

`const isAuthenticated = !!user && !!getStoredAuthToken()` chama `localStorage.getItem` em todo render do provider (e de qualquer filho). Derivar do state ou memoizar?

**Resposta:** Corrigir agora. Derivar `isAuthenticated` do state (`!!user && !!token`) em vez de ler `localStorage` a cada render. Mudanca simples com impacto positivo na performance.

---

### Q12.2 — `AuthContext` — bootstrap faz fetch `/api/users/me` em todo carregamento

Mesmo se o usuario e token armazenados sao validos, o app faz request de rede em toda carga de pagina. Isso atrasa o render inicial. Considerar stale-while-revalidate?

**Resposta:** Pos-MVP. Implementar stale-while-revalidate: renderizar com dados do storage imediatamente e validar em background.

---

### Q12.3 — `WebSocketContext` — `doReconnect` referencia a si mesmo em `useCallback`

`doReconnect` agenda a si mesmo via `setTimeout`. Se `doSubscribe` (dependencia) mudar, o timer antigo ainda chama a versao antiga. Usar `useRef` para a funcao de reconnect?

**Resposta:** Pos-MVP. Migrar para `useRef` quando refatorar o WebSocket context.

---

### Q12.4 — `WebSocketContext` — sem limite maximo de tentativas de reconexao

O backoff cap em 10s, mas nao ha limite de tentativas. Se o servidor estiver permanentemente fora, o cliente reconecta para sempre a cada 10s. Pode ser ruim para dispositivos moveis em conexao limitada. Adicionar limite?

**Resposta:** Corrigir agora. Adicionar limite maximo de tentativas (ex: 30 = ~5 min com backoff). Apos o limite, mostrar mensagem ao usuario. Previne drain de bateria em mobile.

---

### Q12.5 — `WebSocketContext` — acessa `localStorage` diretamente em vez de `session.ts`

Linha 134: `localStorage.getItem('authToken')` em vez de usar `getStoredAuthToken()`. Bypasseia qualquer mudanca futura na estrategia de session storage. Corrigir?

**Resposta:** Corrigir agora. Usar `getStoredAuthToken()` de `session.ts`. Mudanca de uma linha que garante consistencia.

---

### Q12.6 — `NotificationContext` — `unreadCount` e context value nao memoizados

`unreadCount` roda `.filter()` em todo render. O value object e recriado em todo render, causando re-renders em todos os consumidores. Envolver em `useMemo`?

**Resposta:** Corrigir agora. Envolver `unreadCount` em `useMemo` e o value object do context tambem. Previne re-renders desnecessarios.

---

### Q12.7 — `NotificationContext` — `handleMessage` usa type assertions inseguras

`raw` e convertido para `Record<string, unknown>` e campos sao assertados como `as string`, sem validacao runtime. Uma notificacao malformada pode causar comportamento indefinido. Adicionar validacao como `parseListWebSocketMessage`?

**Resposta:** Pos-MVP. Adicionar validacao runtime (ex: Zod ou verificacao manual) quando refatorar o contexto de notificacoes.

---

### Q12.8 — `session.ts` — `getStoredUser` tem side effect de limpar sessao

Se `JSON.parse` falha ou dados sao invalidos, `clearStoredSession()` e chamado como efeito colateral de uma funcao "getter". Isso pode deslogar um usuario autenticado se o objeto user estiver malformado mas o token valido. Separar a logica?

**Resposta:** Pos-MVP. Separar a logica de limpeza em funcao dedicada (`validateStoredSession`). O getter nao deve ter side effects.

---

## 13. CSS e Design System

### Q13.1 — Google Fonts via `@import` bloqueia renderizacao

Fontes sao carregadas via `@import url(...)` no CSS, que e render-blocking. Migrar para `<link rel="preload">` no HTML ou self-host?

**Resposta:** Pos-MVP. Migrar para `<link rel="preload">` no `index.html` ou self-host. Melhora LCP mas nao bloqueia funcionalidade.

---

### Q13.2 — `color-mix()` sem fallback para browsers antigos

A funcao CSS `color-mix()` nao e suportada em Safari < 16.4 e browsers antigos. Sem fallback. Isso e aceitavel para o publico-alvo?

**Resposta:** Pos-MVP. `color-mix()` e suportado por 95%+ dos browsers atuais. Aceitavel para o publico-alvo do MVP.

---

### Q13.3 — Classe `nl-glass` usada em `JoinListPage` mas nao definida no CSS

A classe `nl-glass` e referenciada em `JoinListPage.tsx` (linha 133) mas nao existe em `index.css`. Estilo faltando ou residuo de design anterior?

**Resposta:** Corrigir agora. Definir a classe `nl-glass` no CSS ou remover a referencia em `JoinListPage.tsx`. Provavelmente residuo de design anterior.

---

### Q13.4 — `persistAuthToken` exportado de `session.ts` mas nunca usado

A funcao `persistAuthToken` e exportada mas nunca importada em lugar algum do codebase. Dead code? Remover?

**Resposta:** Corrigir agora. Remover. Dead code.

---

## 14. CI/CD (GitHub Actions)

### Q14.1 — `security-and-compliance` nao declara `needs: changes` — steps condicionais sao dead code

O job `security-and-compliance` referencia `needs.changes.outputs.frontend` e `needs.changes.outputs.backend`, mas nao declara `needs: changes`. Os outputs serao strings vazias, entao as steps condicionais (frontend audit, license policy, backend dependency check) **nunca executam**. Bug significativo. Corrigir?

**Resposta:**

---

### Q14.2 — `pip install semgrep` e `defusedxml` sem version pin

Instalar sem versao fixa pode causar builds nao-reprodutiveis ou quebras inesperadas. Pinar versoes?

**Resposta:** Pos-MVP. Pinar versoes para reproducibilidade. Baixo risco no curto prazo.

---

### Q14.3 — Sem `timeout-minutes` nos jobs de CI

O padrao e 360 minutos (6 horas). Um processo travado pode consumir muito runner time. Adicionar limites explicitos (ex: 20 min)?

**Resposta:** Corrigir agora. Adicionar `timeout-minutes: 20` em todos os jobs. Previne consumo excessivo de runner time em caso de travamento.

---

### Q14.4 — `release.yml` so bumpa PATCH, nunca MINOR ou MAJOR

O auto-bump sempre incrementa patch. Nao ha mecanismo para minor ou major bumps. Implementar convencao de commits (Conventional Commits) ou override manual?

**Resposta:** Pos-MVP. Para o MVP, patch bumps sao suficientes. Implementar Conventional Commits quando o projeto tiver releases mais estruturados.

---

### Q14.5 — `deploy-environment.yml` — fallback `GHCR_PAT || github.token` da falsa seguranca

Se `GHCR_PAT` nao estiver setado, o fallback para `github.token` provavelmente nao tem `packages:write`. O push para GHCR falha silenciosamente. Falhar explicitamente se `GHCR_PAT` nao existir?

**Resposta:**

---

### Q14.6 — `frontend-e2e-fullstack.yml` sem cache de browsers Playwright

Diferente do `ci.yml`, este workflow nao cacheia browsers Playwright. Roda diariamente baixando Chromium toda vez. Adicionar cache?

**Resposta:** Sim

---

### Q14.7 — `frontend-e2e-fullstack.yml` usa profile `ci` (H2) para testes "fullstack"

O backend roda com perfil `ci` que usa H2 in-memory. O nome "fullstack" implica stack completa, mas nao testa contra PostgreSQL. Isso e intencional? Documentar?

**Resposta:** Sim, documentar que o profile `ci` e usado para testes rapidos, mas nao reflete o ambiente de producao. Considerar renomear o workflow para algo como `frontend-e2e-ci-profile.yml`?

---

### Q14.8 — Sem notificacao de falha no workflow agendado (nightly)

Se o workflow noturno `frontend-e2e-fullstack.yml` falha, nao ha notificacao (Slack, email). Falhas podem passar despercebidas. Adicionar notificacao?

**Resposta:** Sim, adicionar notificacao de falhas para o workflow noturno.

---

### Q14.9 — Sem workflow/mecanismo de rollback documentado para producao

Se um deploy de producao da errado, nao ha workflow de rollback. O operador precisa disparar `deploy-prod.yml` manualmente com a tag anterior. Documentar ou criar workflow?

**Resposta:** Criar workflow de rollback que pode ser disparado manualmente, e documentar o processo.

---

### Q14.10 — `java -jar target/*.jar` no smoke test pode matchear multiplos JARs

O glob `*.jar` pode pegar JAR principal + sources JAR. Usar nome especifico?

**Resposta:** Sim, usar nome especifico para evitar ambiguidades.

---

## 15. Docker e Build

### Q15.1 — Sem `.dockerignore`

Nao existe `.dockerignore`. O build context inclui `.git/`, `node_modules/`, `.worktrees/`, arquivos de teste, configs de IDE, etc. Isso torna builds dramaticamente mais lentos e pode vazar arquivos sensiveis. Criar `.dockerignore`?

**Resposta:** Sim, criar `.dockerignore` para excluir arquivos e pastas desnecessarios do build context.

---

### Q15.2 — `HEALTHCHECK` usa `curl` que pode nao estar disponivel na imagem

`eclipse-temurin:25-jre` pode nao incluir `curl`. Se nao tiver, o healthcheck sempre falha e o Kubernetes nunca considera o pod saudavel. Usar `wget --spider` ou instalar `curl`?

**Resposta:** Pode instalar `curl` na imagem ou migrar para `wget --spider` que tem mais chances de estar presente. Verificar qual e a melhor abordagem para a imagem base escolhida.

---

### Q15.3 — `COPY --from=backend-builder /app/target/*.jar` glob fragil

Se o Maven build produzir multiplos JARs (original + repackaged), o `COPY` pode falhar ou copiar o errado. Usar nome especifico?

**Resposta:**Sim, usar nome especifico para garantir que o JAR correto seja copiado para a imagem final.

---

### Q15.4 — `maven:3.9-eclipse-temurin-25` pode nao existir no Docker Hub

Java 25 e muito recente. A combinacao Maven 3.9 + Temurin 25 pode nao ter imagem publicada. Verificar e considerar alternativa?

**Resposta:** Não, vamos continuar no java 25.

---

### Q15.5 — `docker-compose.yml` — credenciais hardcoded e porta exposta em todas interfaces

Username/password `nossalista` hardcoded. Porta 5432 bind em `0.0.0.0` (acessivel na rede). Usar variaveis de ambiente e bind em `127.0.0.1`?

**Resposta:**Sim, usar variaveis de ambiente para credenciais e bind em `

---

### Q15.6 — `docker-compose.yml` — nenhum servico de aplicacao definido

O compose so define PostgreSQL. Backend e frontend devem ser rodados manualmente. Adicionar servicos para experiencia `docker compose up` completa?

**Resposta:** Pos-MVP. O fluxo atual (postgres via compose + backend/frontend manual) funciona para desenvolvimento. Adicionar servicos de app quando o onboarding de novos devs se tornar prioridade.

---

## 16. Kubernetes e Infraestrutura

### Q16.1 — `k8s/legacy/` conflita com producao e esta desatualizado

O namespace `nossalista` em `k8s/legacy/` e igual ao de `k8s/prod/`. Se alguem acidentalmente rodar `kubectl apply -f k8s/legacy/`, pode sobrescrever recursos de producao com manifests desatualizados (sem startup probe, sem envFrom, sem imagePullPolicy). Deletar ou mover para arquivo?

**Resposta:** Deletar `k8s/legacy/` para evitar confusao e riscos de sobrescrita acidental. Manter apenas os manifests atualizados em `k8s/prod/` e `k8s/dev/`.

---

### Q16.2 — Dev deployment sem `SPRING_PROFILES_ACTIVE`

Diferente de prod (que seta `SPRING_PROFILES_ACTIVE: prod`), o deployment dev nao seta o profile. A aplicacao usa o profile padrao, que pode nao estar configurado para o ambiente dev. Adicionar `SPRING_PROFILES_ACTIVE: dev`?

**Resposta:**Sim.

---

### Q16.3 — `startupProbe.initialDelaySeconds: 60` no dev pode ser excessivo

Combinado com `periodSeconds: 10` e `failureThreshold: 18`, o tempo maximo de startup e 240 segundos. Se o app inicia mais rapido, o delay inicial adiciona latencia desnecessaria aos rollouts. Reduzir?

**Resposta:**Sim, vamos reduzir o `initialDelaySeconds` para algo mais apropriado (ex: 10-20 segundos) para o ambiente de desenvolvimento, onde tempos de startup geralmente são menores.

---

### Q16.4 — Sem TLS explicito nos ingresses

Producao e dev usam HTTP (`entrypoints: web`). Para prod, o Cloudflare Tunnel criptografa o tunel, mas o trafego entre Cloudflare e Traefik pode nao ser criptografado. Documentar explicitamente que o Tunnel cuida da criptografia?

**Resposta:** Sim, documentar que o Cloudflare Tunnel cuida da criptografia do trafego externo, e que o trafego interno entre Cloudflare e Traefik e HTTP. Para uma seguranca adicional, poderiamos considerar configurar TLS entre Cloudflare e Traefik, mas isso adiciona complexidade. Avaliar trade-offs?

---

### Q16.5 — Hostname Tailscale hardcoded no ingress dev

`leo-ubuntu.tail7485fb.ts.net` esta hardcoded. Se o hostname Tailscale mudar, o ingress quebra. Parametrizar?

**Resposta:**SIm, parametrizar o hostname do Tailscale usando variaveis de ambiente ou ConfigMap para evitar hardcoding e facilitar mudancas futuras.

---

### Q16.6 — `kubectl set env` cria source dupla de verdade para metadata

O deploy seta `APP_VERSION`, `APP_GIT_SHA`, etc. via `kubectl set env`, mas esses valores ja estao baked na imagem via build args. Duas fontes de verdade. O `set env` tambem dispara rolling restart mesmo sem mudanca de imagem. Isso e intencional?

**Resposta:** POde ser alterado isso.

---

## 17. Funcionalidades Ausentes

### Q17.1 — Sem endpoint de troca/reset de senha

Nao existe endpoint para mudar ou resetar senha. O `ForgotPassword.tsx` existe no frontend mas e apenas um placeholder visual. Implementar?

**Resposta:** Corrigir agora. O `ForgotPassword.tsx` ja existe como placeholder. Implementar endpoint de reset de senha (envio de email com token + endpoint de troca). Funcionalidade basica esperada pelos usuarios.

---

### Q17.2 — Sem endpoint de transferencia de ownership

`MemberService.leaveList()` previne o owner de sair. A mensagem diz "Transfira ou exclua a lista", mas nao existe endpoint de transferencia. Implementar?

**Resposta:** Pos-MVP. O workaround (excluir a lista) existe. Implementar transferencia quando trabalhar em gerenciamento avancado de listas.

---

### Q17.3 — Sem endpoint de exclusao de conta

Nao ha endpoint para deletar conta do usuario (possivel requisito de LGPD/GDPR). Implementar?

**Resposta:** Corrigir agora. Requisito de LGPD — usuarios brasileiros tem direito a exclusao de dados. Implementar soft-delete ou hard-delete com cascata. Prioridade legal.

---

### Q17.4 — Sem endpoint de reordenacao de itens

Itens tem campo `position`, mas nao ha endpoint para reordenar (drag-and-drop). So ha calculo automatico de posicao ao adicionar. Implementar?

**Resposta:** Pos-MVP. O campo `position` ja existe. Implementar endpoint de reordenacao + drag-and-drop quando polir a UX.

---

### Q17.5 — Sem suporte offline

O service worker faz precache de assets mas nao cacheia responses de API. O app e completamente nao-funcional offline. Implementar caching de runtime?

**Resposta:** Pos-MVP. Suporte offline e complexo e nao e expectativa de um MVP. O service worker ja cacheia assets estaticos.

---

### Q17.6 — No proxy de `/ws` no `vite.config.ts`

O proxy so configura `/api`. Conexoes WebSocket em `/ws` em modo desenvolvimento (nao-mock) falham a menos que o backend rode na mesma porta. Adicionar proxy de `/ws`?

**Resposta:** Corrigir agora. Adicionar proxy de `/ws` no `vite.config.ts`. Mudanca simples que desbloqueia testes locais de WebSocket.

---

## 18. Qualidade de Codigo / Code Smells

### Q18.1 — `LIST_TYPES` hardcoded no frontend duplica dados do backend

As definicoes de tipos de lista (nomes, icones, descricoes) estao hardcoded em `types/List.ts` (linhas 141-170). Se o backend adicionar ou modificar tipos, precisa atualizar em dois lugares. Buscar do API?

**Resposta:** Pos-MVP. Para o MVP com tipos fixos, hardcoded e aceitavel. Buscar da API quando tipos forem configuraveis.

---

### Q18.2 — String matching para classificacao de erros no frontend

`ListView.tsx` e `EditListNameModal.tsx` usam `errorMessage.includes('nao encontrada')` e `.includes('permissao')` para classificar erros. Se a mensagem mudar (acentos, i18n), a classificacao quebra. Usar codigos de erro estruturados?

**Resposta:** Corrigir agora. Usar codigos de erro estruturados do backend (RFC 7807 `type` field) em vez de string matching. Corrigir junto com Q5.1/Q5.3.

---

### Q18.3 — `ListItemProps` definido em arquivo de tipo de dados

`types/Item.ts` mistura tipos de dados (`ListItem`, `CreateItemRequest`) com props de componente (`ListItemProps`). Props deveriam ficar perto do componente que os usa. Mover?

**Resposta:** Pos-MVP. Mover `ListItemProps` para junto do componente `ListItem` quando refatorar tipos.

---

### Q18.4 — `Date.now()` para IDs de toast pode colidir

Se dois toasts sao criados no mesmo milissegundo, terao o mesmo ID. Isso causa conflitos de key React. Usar `crypto.randomUUID()` ou contador?

**Resposta:** Corrigir agora. Trocar por `crypto.randomUUID()`. Mudanca de uma linha que elimina possibilidade de colisao.

---

### Q18.5 — `document.getElementById` em vez de `useRef` em multiplos componentes

`EditListNameModal` e `ListItem` usam `document.getElementById` para acessar elementos DOM em vez do sistema de refs do React. Isso e fragil e bypassa o React. Migrar para refs?

**Resposta:** Pos-MVP. Migrar para `useRef` quando os componentes forem modificados. Funciona, mas bypassa o React.

---

### Q18.6 — External avatar URL sem sanitizacao + envio para servico terceiro

`ListItem.tsx` usa `item.createdBy.avatarUrl` diretamente como `img src`. O fallback para `ui-avatars.com` envia o username para servico externo (privacidade). Validar URLs e gerar avatares localmente?

**Resposta:** Corrigir agora. Validar que `avatarUrl` comeca com `https://` e vem de dominios permitidos. Gerar avatares localmente com iniciais em vez de enviar dados para `ui-avatars.com` (privacidade).

---

### Q18.7 — `vitest.config.ts` exclui extensivamente da cobertura

Toda a camada de API (`src/api/**`), todas as paginas (`src/pages/**`), `main.tsx`, e varios componentes sao excluidos do coverage. O threshold de 80% so e atingido porque o codigo mais complexo e excluido. Melhorar?

**Resposta:** Pos-MVP. Expandir cobertura gradualmente, comecando pela camada de API (com mocks de axios) e paginas criticas.

---

### Q18.8 — `workbox-precaching` nao listado como dependencia explicita

O service worker importa de `workbox-precaching`, mas nao esta em `dependencies` ou `devDependencies`. E fornecido transitivamente por `vite-plugin-pwa`. Dependencia implicita que pode quebrar. Listar explicitamente?

**Resposta:** Corrigir agora. Adicionar `workbox-precaching` em `devDependencies`. Previne quebra se `vite-plugin-pwa` mudar suas dependencias transitivas.

---

### Q18.9 — Google OAuth ausente na pagina de registro

A pagina de Login oferece Google Auth, mas a pagina de Registro oferece apenas email/senha. Experiencia assimetrica. Adicionar Google Auth no registro?

**Resposta:** Corrigir agora. Assimetria confusa para o usuario. Adicionar botao de Google OAuth na pagina de registro, redirecionando para o mesmo fluxo OAuth2.

---

### Q18.10 — Unsafe `as` casts na camada de API

`error as AxiosError<ProblemDetail>` e usado sem verificar se o erro e realmente Axios. Se um TypeError for lancado, o cast silenciosamente produz um objeto sem `.response`, caindo na mensagem generica. Verificar tipo antes de cast?

**Resposta:** Pos-MVP. Adicionar `axios.isAxiosError(error)` antes do cast. Corrigir junto com Q11.1 na refatoracao de error handling.

---

## 19. PWA e Service Worker

### Q19.1 — `event.data?.json()` pode lancar excecao no push handler

Se os dados do push nao forem JSON valido, `json()` lanca excecao e a notificacao falha silenciosamente. Adicionar try/catch?

**Resposta:** Corrigir agora. Envolver em try/catch com fallback para notificacao generica. Previne falha silenciosa.

---

### Q19.2 — Sem tratamento de acoes de notificacao

O handler `notificationclick` so abre a URL de `data.url`. Nao ha tratamento de acoes como "Marcar como feito" ou "Ver lista". Implementar?

**Resposta:** Pos-MVP. O comportamento atual (abrir URL) e funcional. Acoes adicionais sao enhancement.

---

### Q19.3 — `registerSW({ immediate: true })` sem tratamento de erro

Registracao do service worker ocorre no top-level do modulo sem try/catch. Se falhar, pode crashar o app. Envolver em try/catch?

**Resposta:** Corrigir agora. Envolver em try/catch. Se falhar, o app deve funcionar normalmente sem service worker.

---

### Q19.4 — PWA dev options habilitadas em desenvolvimento

`devOptions: { enabled: true }` no `vite.config.ts` habilita o service worker em dev. Pode causar problemas de cache durante desenvolvimento. Desabilitar?

**Resposta:** Pos-MVP. Pode causar cache issues, mas e util para testar PWA. Manter e documentar.

---

## 20. Mock Server

### Q20.1 — Search parameter mismatch entre API real e mock

API real envia `q=...`, mock espera `query=...`. O mock sempre recebe `null` para busca, retornando todos os usuarios. Corrigir o mock ou a API?

**Resposta:** Corrigir agora. Mesmo bug que Q11.3. Alinhar o mock para usar `q` (parametro que a API real envia).

---

### Q20.2 — Mock server nao valida autenticacao

Qualquer request com ou sem auth retorna dados do usuario demo. Bugs de autenticacao nao podem ser detectados em desenvolvimento mock. Adicionar validacao basica?

**Resposta:** Pos-MVP. O mock server e para desenvolvimento visual rapido. Validacao de auth no mock adicionaria complexidade sem beneficio significativo.

---

### Q20.3 — `readJsonBody` nao trata content-type nao-JSON

Se o body nao for JSON valido, `JSON.parse` lanca excecao nao tratada que crasha o middleware. Adicionar try/catch?

**Resposta:** Corrigir agora. Envolver `JSON.parse` em try/catch e retornar `{}` ou erro 400. Previne crash do middleware.

---

### Q20.4 — Sem mock de WebSocket para `/ws`

O mock server cuida de REST mas nao ha mock para STOMP/WebSocket. Em mock mode, subscricoes ficam em `pendingSubscriptionsRef` sem nunca receber dados mock. Features de real-time nao podem ser testadas. Implementar mock de WebSocket?

**Resposta:** Pos-MVP. Implementar mock de STOMP/WebSocket quando funcionalidades de real-time precisarem ser testadas visualmente.

---

## Resumo por Prioridade

### Criticos (corrigir imediatamente)

| # | Issue |
|---|-------|
| Q14.1 | `security-and-compliance` — steps condicionais sao dead code (needs: changes faltando) |
| Q15.1 | Sem `.dockerignore` — build context inclui `.git/`, `node_modules/` |
| Q16.1 | `k8s/legacy/` pode sobrescrever producao acidentalmente |
| Q9.4 | `EditItemModal` comparacao de `listType` provavelmente quebrada |
| Q8.1 | `App.tsx` e `App.css` sao dead code |

### Altos (corrigir em breve)

| # | Issue |
|---|-------|
| Q2.2 | Secret JWT padrao fraco |
| Q2.3 | Token OAuth2 na URL |
| Q3.1 | N+1 query em `ListMapper` |
| Q4.1 | Race condition no `PushSubscriptionStore` |
| Q5.2 | Sem catch-all para excecoes nao tratadas |
| Q5.3 | `UserService` lanca `RuntimeException` generico |
| Q8.2 | `ListView.tsx` God Component |
| Q9.3 | `DeleteConfirmModal` foca botao destrutivo |
| Q9.5 | `EditItemModal` delay 500ms |
| Q10.1 | Stale closures em `useItems` |
| Q11.1 | Duplicacao massiva de error handling |
| Q15.2 | `HEALTHCHECK` com `curl` possivelmente inexistente |

### Medios (melhorias importantes)

| # | Issue |
|---|-------|
| Q1.1 | Entidade `List` sombreia `java.util.List` |
| Q1.3 | Mapeamento de tipo duplicado |
| Q1.4 | `ListMapper` faz query no banco |
| Q1.6 | Notificacoes sincronas |
| Q2.1 | JWT 7 dias sem refresh |
| Q2.4 | DB lookup por request |
| Q2.6 | Sem rate limiting |
| Q3.2 | Full table scan em busca de usuarios |
| Q3.3 | Busca sem limite de resultados |
| Q8.3 | Sem AbortController |
| Q8.5 | Toast nao centralizado |
| Q9.1 | Modais nao usam ModalShell |
| Q9.2 | ModalShell sem focus trap |
| Q9.7 | `navigate(-1)` pode sair do app |
| Q9.8 | Double toast ao sair |
| Q9.10 | Login standalone nao processa invite |
| Q9.20 | Logout inconsistente |
| Q11.6 | snake_case vs camelCase nos DTOs |
| Q12.6 | Context value nao memoizado |
| Q14.5 | Fallback GHCR_PAT falsa seguranca |

---

**Total: 107 perguntas cobrindo todo o projeto.**

Responda cada pergunta acima e me chame novamente para comecarmos as melhorias.
