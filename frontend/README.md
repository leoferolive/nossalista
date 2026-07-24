# Frontend NossaLista

Aplicacao React 19 + TypeScript + Vite do NossaLista.

## Requisitos

- Node.js 22+
- npm

## Executar localmente

```bash
npm install
npm run dev
```

App local: `http://localhost:5173`

Para rodar sem backend, use o mockserver embutido:

```bash
npm install
npm run dev:mock
```

## Scripts

- `npm run dev`: inicia servidor de desenvolvimento
- `npm run build`: gera build de producao
- `npm run dev:mock`: inicia o frontend com API mock em memoria
- `npm run test`: executa testes com Vitest
- `npm run test:coverage`: executa testes com cobertura (threshold >= 80%)
- `npm run test:e2e`: executa todos os cenarios E2E com Playwright
- `npm run test:e2e:pr`: executa apenas cenarios `@pr` (deterministic/mockado, gate bloqueante de PR)
- `npm run test:e2e:fullstack`: executa apenas cenarios `@fullstack` (frontend + backend reais)
- `npm run lint`: executa lint
- `npm run stylelint`: valida CSS
- `npm run typecheck`: executa `tsc --noEmit`
- `npm run format:check`: valida formatacao com Prettier
- `npm run preview`: sobe build local para validacao
- `npm run bundle:check`: valida budget do maior chunk JS

## Integracao com backend

- A API esperada e servida pelo backend em `http://localhost:8080`
- Em modo `mock`, o Vite intercepta `/api/**` com respostas em memoria para login, perfil, listas, itens, convites e Personal Access Tokens (`/api/users/me/tokens`)
- Endpoints e contrato de autenticacao: `docs/auth-endpoints-matrix.md`
- Sincronizacao realtime: STOMP/SockJS em `/ws/**`

### Sessao, CSRF e realtime

- A sessao web e restaurada por `GET /api/users/me`; o JWT fica em cookie HttpOnly e nao e lido pelo React nem enviado como Bearer. Na primeira carga, `authToken` e `user` legados sao removidos do `localStorage`.
- Axios usa `withCredentials` e busca `GET /api/auth/csrf` antes de `POST`, `PUT`, `PATCH` e `DELETE`, encaminhando `X-XSRF-TOKEN`.
- Login por senha e troca do code Google retornam somente o perfil e escrevem o cookie no backend. Logout chama `POST /api/auth/logout`.
- SockJS autentica pelo cookie do handshake; nao envie JWT nos query params ou no frame STOMP `CONNECT`.

## Notificacoes (online + push)

- Notificacoes online no sino ficam sempre ativas para usuario autenticado (conexao WebSocket global enquanto estiver logado).
- Push do navegador e opcional (opt-in/opt-out) pelo menu da conta:
  - `Ativar notificacoes push`: pede permissao e registra subscricao.
  - `Desativar notificacoes push`: remove subscricao e para push do browser.
- O menu da conta exibe status explicito de push (`ativado`, `desativado`, `nao suportado` ou `indisponivel no ambiente`).
- Push depende de VAPID configurado no backend (`VAPID_PUBLIC_KEY` e `VAPID_PRIVATE_KEY`).
- Em ambiente de desenvolvimento, o Service Worker/PWA fica habilitado para validar fluxo de push localmente.
- O dropdown do menu da conta no desktop deve renderizar sem clipping no card do header (padrao via `nl-card-unclipped` no `AppHeader`).

## Sistema visual

- Linguagem visual oficial: `Fresh Lists`
- Direcao visual base: `Playful Editorial`
- Identidade unificada: `leoferolive design`
- Paleta principal: `violeta + teal` (`--nl-accent` violeta `#7c3aed` = acao/CTA; `--nl-primary` teal `#14b8a6` = status/progresso)
- Tokens de apoio: `--nl-info` (azul `#2f6fed` / dark `#6398f0`, ex.: "convite por link") e `--nl-text-faint` (texto/ícones sutis; light `#968fa8` / dark `#6f6982`)
- Fonte display: `Fraunces` (self-hosted, `woff2` variavel com subset latino)
- Fonte de interface: `Plus Jakarta Sans` (self-hosted, `woff2` variavel com subset latino)
- Tokens globais vivem em `src/index.css` e cobrem `light` e `dark` com paridade de superficie, borda, foco, sombra e overlays
- O override da identidade `leoferolive design` (valores dos tokens + `@font-face` self-hosted) fica em `src/styles/leoferolive-tokens.css` com as fontes em `src/styles/fonts/`, importado por ultimo em `src/main.tsx` para vencer na cascata sem tocar componentes. Reverter = remover esse import (e reativar o `@import` do Google Fonts em `src/index.css`)
- O switch de tema e parte do produto e deve aparecer nas telas publicas e na area autenticada principal
- Em mobile autenticado, o switch de tema sai do topo e vai para o sheet da conta para reduzir ruído visual
- A landing publica deve ficar mais leve que o produto autenticado: um hero, um preview principal e apoio curto
- Na landing (`LandingPage.tsx`), o hero e o CTA final sao **sempre escuros** (hex fixos, nao respondem ao tema); apenas as secoes claras "Como funciona", "Recursos" e o rodape mudam com `data-theme`
- Cada secao da landing tem **um unico CTA solido** (`nl-btn-primary`) + um link de texto secundario; os recursos aparecem como lista de icone+texto, nunca como botoes
- Headers autenticados no mobile seguem um shell compacto de 3 zonas:
  - linha 1 com contexto e utilidades
  - linha 2 com titulo/subtitulo
  - linha 3 com no maximo uma acao primaria
- Menus de conta, notificacoes e overflows operacionais usam `ResponsiveSheet` no mobile e dropdown apenas no desktop
- Home mobile inclui busca local (debounce) por nome e tipo da lista, sem chamadas extras de API
- Cards de lista na Home seguem padrao de 3 zonas (cabecalho, estado informativo, rodape) e o card inteiro e o alvo primario de toque
- Indicadores informativos nunca devem reutilizar visuais de controles interativos (ex.: checkbox decorativo)
- Microcopy em portugues deve manter acentuacao PT-BR consistente (`você`, `Histórico`, `sessão`)

## Padrao visual de formularios e modais

- Use as primitives globais sempre que possivel:
  - Campo: `nl-input`
  - Label: `nl-label`
  - Helper text: `nl-helper`
  - Erro: `nl-alert` ou `nl-helper nl-helper-error`
  - Botoes: `nl-btn-primary`, `nl-btn-secondary`, `nl-btn-ghost`, `nl-btn-danger`
  - Estrutura de modal: `ModalShell`
  - Estrutura de sheet/drawer mobile: `ResponsiveSheet`
- Landing publica:
  - CTA principal abre cadastro
  - CTA secundario abre login
  - Nao reutilizar o mesmo modal para os dois CTAs
- Fluxo de auth da landing:
  - Usuario deslogado em rota protegida volta para `/?auth=login&redirect=...`
  - `/login` e rota legada e redireciona para `/?auth=login`
  - Abertura de modal por URL: `/?auth=login|register`
  - Login pela landing preserva `redirect` e retorna ao destino protegido apos autenticar
  - Pos-cadastro: `/?auth=login&registered=1&email=...` (prefill + mensagem de sucesso)
  - Convite pendente usa `sessionStorage.pendingInviteCode` e tenta auto-join apos login por email
- Evitar depender do estilo padrao do navegador para `input`, `button` ou `dialog`; o padrao oficial deve vir dos tokens/classes do projeto

## Onboarding Guiado (Primeiro Login)

- O tour inicia automaticamente na Home quando `user.onboardingCompletedAt` ainda e `null`.
- Ao concluir ou pular, o frontend chama `POST /api/users/me/onboarding/complete` (idempotente).
- Em caso de falha para persistir essa preferencia, o tour nao reabre na sessao atual e um aviso leve e exibido.
- O usuario pode reabrir o fluxo completo a qualquer momento pelo menu da conta em `Ver tutorial`.
- O spotlight recalcula automaticamente quando o alvo entra no DOM apos o passo iniciar (ex.: modal de criacao), evitando overlay opaco sem destaque.
- Todos os passos mantem acao `Pular`; no passo de criacao, `Proximo` fica desabilitado ate a lista ser criada.
- Em mobile, o painel do tutorial fica menor e mais leve, para nao competir com o entendimento inicial da tela.
- O overlay do onboarding segue os tokens globais `Fresh Lists` (`nl-*`) para manter paridade visual entre `light` e `dark` e evitar paleta legado.

## Shell autenticado

- `AppHeader` agora expõe slots semanticos (`primaryAction`, `secondaryActions`, `accountActions`) em vez de um bloco generico de acoes.
- `Home` prioriza primeira dobra com titulo, CTA principal e inicio dos cards.
- `ListView` separa acao primaria (`Convidar`) das acoes secundarias (`Membros`, `Histórico`) e move acoes destrutivas para overflow.
- Em `ListView`, as acoes de dono podem aparecer inline (larguras maiores) ou dentro do overflow (`Mais`) em layouts compactos; testes E2E devem cobrir ambos os caminhos.
- Em `ListView`, membros tambem possuem overflow de acoes com opcoes permitidas por papel (ex.: `Sair da lista`).
- Em mobile, o topo autenticado e os sheets usam escala compacta (titulo menor, subtitulo truncado, paddings reduzidos e action rows mais baixas) para diminuir scroll e friccao de navegacao.
- Feedbacks de navegacao entre telas (ex.: saida de lista) usam `location.state` com fallback em `sessionStorage` para evitar perda do toast em transicoes rapidas.
- `NotificationBell` e menu da conta usam sheets no mobile para evitar popovers cortados.
- `ListCard` usa variante `mobile compact` (altura menor, tipografia ajustada e menos linhas auxiliares), preservando a versao mais editorial no desktop.
- `ListItem` manteve long-press, mas ganhou acao explicita de overflow (`•••`) e tempo de long-press menor para reduzir esforco no celular.

## Estrutura principal

```text
src/
|- api/
|- components/
|- contexts/
|- hooks/
|- pages/
|- types/
```

## Testes

```bash
npm run lint
npm run format:check
npm run stylelint
npm run typecheck
npm run test:coverage
npm run build
npm run bundle:check
npm run test:e2e:pr
npm run test:e2e:fullstack
```

## Gates de CI (PR)

- Coverage minima do frontend: 80% para linhas, branches, funcoes e statements.
- Build e runtime smoke com `vite build` + `vite preview`.
- Suite E2E `@pr` obrigatoria com Playwright.
- Suite `@fullstack` roda separadamente no workflow `frontend-e2e-fullstack.yml` (cron diario + disparo manual).
