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

## Padrao visual de formularios

- Inputs em telas/modais do tema principal devem explicitar contraste:
  - Fundo: `bg-nl-surface-strong` (ou equivalente do tema)
  - Texto digitado: `text-nl-text`
  - Placeholder: `placeholder:text-nl-muted/70`
  - Cursor de texto: `caret-nl-accent`
- Evitar depender do estilo padrao do navegador para `input`, pois isso pode resultar em campo claro com fonte clara.

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
