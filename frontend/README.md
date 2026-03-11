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

## Scripts

- `npm run dev`: inicia servidor de desenvolvimento
- `npm run build`: gera build de producao
- `npm run test`: executa testes com Vitest
- `npm run test:coverage`: executa testes com cobertura (threshold >= 80%)
- `npm run test:e2e`: executa smoke E2E com Playwright
- `npm run lint`: executa lint
- `npm run stylelint`: valida CSS
- `npm run typecheck`: executa `tsc --noEmit`
- `npm run format:check`: valida formatacao com Prettier
- `npm run preview`: sobe build local para validacao
- `npm run bundle:check`: valida budget do maior chunk JS

## Integracao com backend

- A API esperada e servida pelo backend em `http://localhost:8080`
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

## Padrao visual de formularios

- Inputs em telas/modais do tema principal devem explicitar contraste:
  - Fundo: `bg-nl-surface-strong` (ou equivalente do tema)
  - Texto digitado: `text-nl-text`
  - Placeholder: `placeholder:text-nl-muted/70`
  - Cursor de texto: `caret-nl-accent`
- Evitar depender do estilo padrao do navegador para `input`, pois isso pode resultar em campo claro com fonte clara.

## Onboarding Guiado (Primeiro Login)

- O tour inicia automaticamente na Home quando `user.onboardingCompletedAt` ainda e `null`.
- Ao concluir ou pular, o frontend chama `POST /api/users/me/onboarding/complete` (idempotente).
- Em caso de falha para persistir essa preferencia, o tour nao reabre na sessao atual e um aviso leve e exibido.
- O usuario pode reabrir o fluxo completo a qualquer momento pelo menu da conta em `Ver tutorial`.
- O spotlight recalcula automaticamente quando o alvo entra no DOM apos o passo iniciar (ex.: modal de criacao), evitando overlay opaco sem destaque.
- Todos os passos mantem acao `Pular`; no passo de criacao, `Proximo` fica desabilitado ate a lista ser criada.

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
