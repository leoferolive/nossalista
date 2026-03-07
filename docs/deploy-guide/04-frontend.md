# Frontend — Verificações para Build de Produção

## 1. vite.config.ts — Sem Mudanças Necessárias

O proxy `/api` configurado no `vite.config.ts` só é ativo durante `npm run dev` (servidor de desenvolvimento Vite). No build de produção (`npm run build`), o proxy não existe — as requisições vão diretamente para o mesmo servidor que serve o HTML (o Spring Boot).

```typescript
// vite.config.ts (trecho atual — ok para dev local)
server: {
  proxy: {
    '/api': 'http://localhost:8080',
    '/ws': 'http://localhost:8080',
  }
}
```

**Em produção:** o browser faz `fetch('/api/lists')` e o Spring Boot (mesmo servidor) responde. Nenhuma mudança necessária.

## 2. Variáveis de Ambiente Frontend (`VITE_*`)

Verificar se o código frontend usa `import.meta.env.VITE_*`:

```bash
# Buscar uso de variáveis de ambiente no código
grep -r "import.meta.env" frontend/src/
```

### Se o frontend usa URLs absolutas (problema)

Exemplos de código problemático:
```typescript
// ❌ Errado — URL hardcoded
const API_URL = 'http://localhost:8080/api'
fetch(`${API_URL}/lists`)

// ❌ Errado — variável de ambiente com URL completa
const API_URL = import.meta.env.VITE_API_URL
fetch(`${API_URL}/lists`)
```

### Como deve estar (correto para embedded)

```typescript
// ✅ Correto — path relativo, funciona com frontend embutido
fetch('/api/lists')

// ✅ Correto — sem VITE_API_URL, sem VITE_WS_URL
const ws = new WebSocket(`ws://${window.location.host}/ws`)
```

### Variáveis de ambiente para o build

Se for necessário passar variáveis em tempo de build (via GitHub Actions), adicionar no `vite.config.ts`:

```typescript
// Nenhuma variável VITE_* necessária para frontend embutido
// O frontend usa paths relativos (/api, /ws) que resolvem para o mesmo host
```

**Conclusão:** com frontend embutido, **não são necessárias** variáveis `VITE_API_URL` ou `VITE_WS_URL`.

## 3. Verificação do Build

```bash
cd frontend

# Instalar dependências (se necessário)
npm ci

# Build de produção
npm run build

# Verificar output
ls dist/
# Esperado: index.html, assets/ (com .js e .css hasheados)

# Verificar tamanho
du -sh dist/
```

### Output esperado do `dist/`

```
dist/
├── index.html
└── assets/
    ├── index-AbCdEf12.js
    ├── index-GhIjKl34.css
    └── ... (outros chunks)
```

## 4. WebSocket — Conexão Relativa

O WebSocket deve usar o mesmo host que serve a aplicação. Verificar que o código usa conexão relativa:

```bash
grep -r "WebSocket\|SockJS\|ws://" frontend/src/
```

### Padrão correto para embedded

```typescript
// ✅ Usa o mesmo host e porta da aplicação
const wsUrl = `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`

// ✅ SockJS com path relativo
const socket = new SockJS('/ws')
```

## 5. Checklist de Verificação do Frontend

```bash
# 1. Sem URLs hardcoded no código
grep -r "localhost:8080\|192.168" frontend/src/
# Esperado: sem resultados (ou apenas em comentários)

# 2. Sem VITE_API_URL ou VITE_WS_URL necessários
grep -r "VITE_API_URL\|VITE_WS_URL" frontend/src/
# Se encontrar: avaliar se é necessário ou pode usar path relativo

# 3. Build funciona
npm run build
# Esperado: BUILD SUCCESS, dist/ gerado

# 4. TypeScript sem erros
npm run type-check 2>/dev/null || npx tsc --noEmit
# Esperado: sem erros
```
