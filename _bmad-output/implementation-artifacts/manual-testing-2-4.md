# Manual Testing Guide - Story 2.4: Ver Detalhes de uma Lista

**Data:** 2026-02-12
**Reviewer:** Leo
**Story:** 2-4-ver-detalhes-de-uma-lista

---

## ✅ PRÉ-REQUISITOS

Antes de iniciar os testes, certifique-se que:

1. ✅ Backend está rodando (`./mvnw spring-boot:run` no diretório backend)
2. ✅ Frontend está rodando (`npm run dev` no diretório frontend)
3. ✅ PostgreSQL está ativo (via Docker Compose ou local)
4. ✅ Você tem um usuário cadastrado e pode fazer login
5. ✅ Você tem pelo menos uma lista criada na home

---

## 🔧 SETUP INICIAL

Execute estas ações antes de começar os testes:

```bash
# 1. Iniciar backend
cd backend
./mvnw spring-boot:run

# 2. Iniciar frontend (em outro terminal)
cd frontend
npm run dev

# 3. Acessar aplicação
# Abrir navegador em: http://localhost:5173

# 4. Fazer login e criar lista de teste
# Login → Home → Criar Nova Lista → "Lista de Teste Manual"
```

---

## 🧪 TESTES BACKEND (Swagger UI)

**URL Swagger:** http://localhost:8080/swagger-ui/index.html

### Teste 1: GET /api/lists/{id} - Happy Path (200 OK)

**Objetivo:** Verificar que endpoint retorna dados completos quando lista existe e usuário é owner.

**Passos:**
1. Abrir Swagger UI
2. Fazer login via POST /api/auth/login (obter JWT token)
3. Clicar em "Authorize" no topo e colar token (formato: `Bearer <token>`)
4. Criar uma lista via POST /api/lists com body:
   ```json
   {
     "name": "Teste Detalhes",
     "typeId": 1
   }
   ```
5. Copiar o `id` retornado (UUID)
6. Executar GET /api/lists/{id} com o ID copiado

**Resultado Esperado:**
- ✅ Status: 200 OK
- ✅ Response contém:
  - `id`: UUID da lista
  - `name`: "Teste Detalhes"
  - `type.id`: 1
  - `type.slug`: "compras"
  - `type.name`: "Compras"
  - `owner.id`: seu user ID
  - `owner.username`: seu username
  - `owner.name`: seu nome
  - `inviteCode`: código de 12 caracteres alfanuméricos
  - `isOwner`: true
  - `itemsCount`: 0
  - `createdAt`: timestamp ISO 8601
  - `updatedAt`: timestamp ISO 8601

---

### Teste 2: GET /api/lists/{id} - Lista Não Existe (404)

**Objetivo:** Verificar que retorna 404 quando ID não existe.

**Passos:**
1. No Swagger, executar GET /api/lists/{id}
2. Usar um UUID aleatório que não existe (ex: `00000000-0000-0000-0000-000000000000`)

**Resultado Esperado:**
- ✅ Status: 404 Not Found
- ✅ Response RFC 7807 Problem Details:
  ```json
  {
    "type": "https://api.nossalista.com/docs/errors/list-not-found",
    "title": "Lista Não Encontrada",
    "detail": "Lista não encontrada",
    "instance": "/api/lists/{id}"
  }
  ```

---

### Teste 3: GET /api/lists/{id} - Sem Permissão (403)

**Objetivo:** Verificar que usuário não-owner não pode acessar lista de outro.

**Passos:**
1. Criar lista como User A (via Swagger)
2. Copiar ID da lista
3. Fazer logout
4. Criar novo usuário User B via POST /api/auth/register
5. Fazer login como User B
6. Tentar GET /api/lists/{id} com ID da lista do User A

**Resultado Esperado:**
- ✅ Status: 403 Forbidden
- ✅ Response RFC 7807 Problem Details:
  ```json
  {
    "type": "https://api.nossalista.com/docs/errors/access-forbidden",
    "title": "Acesso Negado",
    "detail": "Você não tem permissão para acessar esta lista",
    "instance": "/api/lists/{id}"
  }
  ```

---

### Teste 4: GET /api/lists/{id} - Não Autenticado (401)

**Objetivo:** Verificar que endpoint requer autenticação.

**Passos:**
1. No Swagger, clicar em "Authorize" e fazer logout (remover token)
2. Tentar executar GET /api/lists/{id} com qualquer ID válido

**Resultado Esperado:**
- ✅ Status: 401 Unauthorized
- ✅ Acesso negado sem JWT token

---

### Teste 5: GET /api/lists/{id} - Diferentes Tipos de Lista

**Objetivo:** Verificar que endpoint funciona com todos os tipos de lista.

**Passos:**
1. Criar 3 listas, uma de cada tipo:
   - POST /api/lists com `typeId: 1` (Compras)
   - POST /api/lists com `typeId: 2` (Tarefas)
   - POST /api/lists com `typeId: 3` (Wishlist)
2. Para cada lista criada, executar GET /api/lists/{id}

**Resultado Esperado:**
- ✅ Todas retornam 200 OK
- ✅ Campo `type.slug` correto:
  - typeId 1 → "compras"
  - typeId 2 → "tarefas"
  - typeId 3 → "wishlist"

---

## 🎨 TESTES FRONTEND (Aplicação Web)

**URL:** http://localhost:5173

### Teste 6: Navegação da Home para ListView

**Objetivo:** Verificar que clicar em ListCard navega para detalhes.

**Passos:**
1. Fazer login na aplicação
2. Estar na Home (deve ver grid de listas)
3. Clicar em qualquer ListCard

**Resultado Esperado:**
- ✅ URL muda para `/lists/{id}`
- ✅ ListView carrega e exibe dados da lista
- ✅ Não há erro no console do navegador

---

### Teste 7: ListView - Exibição Correta de Dados

**Objetivo:** Verificar que ListView mostra todos os elementos conforme AC3.

**Passos:**
1. Navegar para qualquer lista (clicar em ListCard)
2. Observar os elementos da página

**Resultado Esperado:**
- ✅ **Header:**
  - Botão voltar (seta ←) visível no canto esquerdo
  - Nome da lista exibido ao lado
- ✅ **Info da lista:**
  - Emoji do tipo correto (🛒 para Compras, ✓ para Tarefas, etc)
  - Nome do tipo da lista
  - Texto "Criada por {username}"
  - Badge "Você é o dono" ou "Lista compartilhada"
- ✅ **Seção Itens:**
  - Título "Itens (0)"
  - Ícone de lista vazia
  - Mensagem "Esta lista ainda não tem itens."
  - Texto "Adicione o primeiro!"
  - Botão "Adicionar Item" presente mas DISABLED
  - Texto "(Funcionalidade disponível no Epic 3)"

---

### Teste 8: ListView - Botão Voltar

**Objetivo:** Verificar que botão voltar retorna para Home.

**Passos:**
1. Navegar para qualquer lista
2. Clicar no botão de voltar (seta ←)

**Resultado Esperado:**
- ✅ Retorna para Home (URL: `/`)
- ✅ Grid de listas é exibido novamente
- ✅ Lista que estava visualizando ainda aparece no grid

---

### Teste 9: ListView - Loading State

**Objetivo:** Verificar skeleton/loading durante carregamento.

**Passos:**
1. Abrir DevTools do navegador (F12)
2. Ir para Network tab e ativar "Slow 3G" ou "Fast 3G"
3. Navegar para uma lista clicando em ListCard
4. Observar o estado de loading

**Resultado Esperado:**
- ✅ Skeleton UI é exibido enquanto carrega
- ✅ Skeleton mostra:
  - Header com placeholder de botão + título
  - Info card com placeholder circular + linhas
  - Items section com placeholders
- ✅ Skeleton tem animação de "pulse"
- ✅ Após carregamento, skeleton desaparece e dados reais aparecem

---

### Teste 10: ListView - Error 404 (Lista Não Encontrada)

**Objetivo:** Verificar tratamento de erro quando lista não existe.

**Passos:**
1. Copiar URL de uma lista válida (ex: `/lists/abc123...`)
2. Modificar o ID na URL para UUID inválido
3. Pressionar Enter para navegar

**Resultado Esperado:**
- ✅ Página exibe card de erro vermelho
- ✅ Mensagem: "Lista não encontrada"
- ✅ Botão "Voltar para Home" presente
- ✅ Clicar no botão retorna para `/`

---

### Teste 11: ListView - Error 403 (Sem Permissão)

**Objetivo:** Verificar que usuário vê erro apropriado ao tentar acessar lista de outro.

**Passos:**
1. Como User A, criar lista e copiar ID da URL
2. Fazer logout
3. Criar novo usuário User B
4. Como User B, navegar manualmente para `/lists/{id-do-user-a}`

**Resultado Esperado:**
- ✅ Página exibe card de erro vermelho
- ✅ Mensagem: "Você não tem permissão para acessar esta lista"
- ✅ Botão "Voltar para Home" presente
- ✅ Console não mostra erros não tratados

---

### Teste 12: ListView - Error de Conexão

**Objetivo:** Verificar tratamento de erro quando backend está offline.

**Passos:**
1. Parar o backend (`Ctrl+C` no terminal do backend)
2. No frontend, tentar navegar para uma lista

**Resultado Esperado:**
- ✅ Página exibe card de erro vermelho
- ✅ Mensagem de erro genérica (ex: "Erro de conexão. Verifique sua internet.")
- ✅ Botão "Tentar Novamente" presente
- ✅ Ao reiniciar backend e clicar "Tentar Novamente", dados carregam corretamente

---

## 🔄 TESTE DE INTEGRAÇÃO COMPLETO

### Teste 13: Fluxo End-to-End

**Objetivo:** Validar todo o fluxo de criar lista → visualizar → voltar.

**Passos:**
1. Login na aplicação
2. Na Home, clicar em "+ Nova Lista"
3. Criar lista "E2E Test" do tipo "Tarefas"
4. Fechar modal (lista aparece no grid)
5. Clicar no ListCard da lista recém-criada
6. Verificar que ListView carrega corretamente
7. Verificar que nome é "E2E Test" e tipo é "Tarefas"
8. Clicar em voltar
9. Verificar que Home ainda mostra a lista

**Resultado Esperado:**
- ✅ Todo o fluxo funciona sem erros
- ✅ Dados permanecem consistentes
- ✅ Navegação é fluida

---

## ✅ CHECKLIST FINAL

Após executar todos os testes, marque:

- [X] Todos os 5 testes backend (Swagger) passaram
- [X] Todos os 8 testes frontend (UI) passaram
- [X] Teste de integração E2E passou
- [X] Nenhum erro não tratado apareceu no console
- [X] Performance está adequada (loading < 2s em 4G)
- [X] Acessibilidade: navegação por teclado funciona
- [X] Mobile: tela funciona em viewport de 375px (iPhone SE)

---

## 🐛 REGISTRO DE BUGS ENCONTRADOS

Se encontrar bugs durante os testes, registre aqui:

| # | Teste | Descrição | Severidade | Status |
|---|-------|-----------|------------|--------|
|   |       |           |            |        |

---

## 📝 NOTAS ADICIONAIS

- **Browsers testados:** Chrome, Firefox, Safari
- **Dispositivos testados:** Desktop, Mobile (emulado)
- **Performance observada:** [preencher após testes]
- **Observações:** [preencher se houver]

---

**Testado por:** Leonardo Oliveira
**Data:** 12/02/2026
**Aprovado:** [X] Sim [ ] Não - Motivo: _______________
