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
- `npm run lint`: executa lint
- `npm run preview`: sobe build local para validacao

## Integracao com backend

- A API esperada e servida pelo backend em `http://localhost:8080`
- Endpoints e contrato de autenticacao: `docs/auth-endpoints-matrix.md`
- Sincronizacao realtime: STOMP/SockJS em `/ws/**`

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
npm run test -- --run
```
