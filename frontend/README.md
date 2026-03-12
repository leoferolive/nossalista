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
- `npm run test:e2e`: executa cenarios E2E com Playwright (smoke, auth/redirect e onboarding)
- `npm run lint`: executa lint
- `npm run stylelint`: valida CSS
- `npm run typecheck`: executa `tsc --noEmit`
- `npm run format:check`: valida formatacao com Prettier
- `npm run preview`: sobe build local para validacao
- `npm run bundle:check`: valida budget do maior chunk JS

## Integracao com backend

- A API esperada e servida pelo backend em `http://localhost:8080`
- Em modo `mock`, o Vite intercepta `/api/**` com respostas em memoria para login, perfil, listas, itens e convites
- Endpoints e contrato de autenticacao: `docs/auth-endpoints-matrix.md`
- Sincronizacao realtime: STOMP/SockJS em `/ws/**`

## Notificacoes (online + push)

- Notificacoes online no sino ficam sempre ativas para usuario autenticado (conexao WebSocket global enquanto estiver logado).
- Push do navegador e opcional (opt-in/opt-out) pelo menu da conta:
  - `Ativar notificacoes push`: pede permissao e registra subscricao.
  - `Desativar notificacoes push`: remove subscricao e para push do browser.
- O menu da conta exibe status explicito de push (`ativado`, `desativado`, `nao suportado` ou `indisponivel no ambiente`).
- Push depende de VAPID configurado no backend (`VAPID_PUBLIC_KEY` e `VAPID_PRIVATE_KEY`).
- Em ambiente de desenvolvimento, o Service Worker/PWA fica habilitado para validar fluxo de push localmente.

## Sistema visual

- Linguagem visual oficial: `Fresh Lists`
- Direcao visual base: `Playful Editorial`
- Paleta principal: `coral + teal`
- Fonte display: `Fraunces`
- Fonte de interface: `Plus Jakarta Sans`
- Tokens globais vivem em `src/index.css` e cobrem `light` e `dark` com paridade de superficie, borda, foco, sombra e overlays
- O switch de tema e parte do produto e deve aparecer nas telas publicas e na area autenticada principal
- Em mobile autenticado, o switch de tema sai do topo e vai para o sheet da conta para reduzir ruído visual
- A landing publica deve ficar mais leve que o produto autenticado: um hero, um preview principal e apoio curto
- Headers autenticados no mobile seguem um shell compacto de 3 zonas:
  - linha 1 com contexto e utilidades
  - linha 2 com titulo/subtitulo
  - linha 3 com no maximo uma acao primaria
- Menus de conta, notificacoes e overflows operacionais usam `ResponsiveSheet` no mobile e dropdown apenas no desktop

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

## Shell autenticado

- `AppHeader` agora expõe slots semanticos (`primaryAction`, `secondaryActions`, `accountActions`) em vez de um bloco generico de acoes.
- `Home` prioriza primeira dobra com titulo, CTA principal e inicio dos cards.
- `ListView` separa acao primaria (`Convidar`) das acoes secundarias (`Membros`, `Historico`) e move acoes destrutivas para overflow.
- `NotificationBell` e menu da conta usam sheets no mobile para evitar popovers cortados.
- `ListCard` foi encurtado para mostrar status e entrada com menos texto repetitivo.

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
npm run test:e2e
```

## Gates de CI (PR)

- Coverage minima do frontend: 80% para linhas, branches, funcoes e statements.
- Build e runtime smoke com `vite build` + `vite preview`.
- Smoke E2E obrigatorio com Playwright.
